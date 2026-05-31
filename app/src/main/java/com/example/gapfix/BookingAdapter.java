package com.example.gapfix;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {
    private static final String TAG = "BookingAdapter";
    private static final int TYPE_SINGLE = 0;
    private static final int TYPE_PACKAGE = 1;
    private final List<Booking> bookingList;
    private final Context context;
    private final boolean isCalendarMode;
    public BookingAdapter(Context context, List<Booking> bookingList) {
        this(context, bookingList, false);
    }
    public BookingAdapter(Context context, List<Booking> bookingList, boolean isCalendarMode) {
        this.context = context;
        this.bookingList = bookingList;
        this.isCalendarMode = isCalendarMode;
        SubjectHelper.loadTranslations(context, this::notifyDataSetChanged);
    }
    private static String getTranslatedStatus(Context context, String status) {
        if (status == null) return context.getString(R.string.status_pending);
        switch (status.toLowerCase()) {
            case "confirmed": return context.getString(R.string.status_confirmed);
            case "completed": return context.getString(R.string.status_completed);
            case "cancelled": return context.getString(R.string.status_cancelled);
            case "finished": return context.getString(R.string.status_finished);
            case "done": return context.getString(R.string.status_done);
            case "suggestion_pending": return context.getString(R.string.status_suggestion_pending);
            case "free_trial_pending": return context.getString(R.string.status_free_trial_pending);
            case "pending":
            default: return context.getString(R.string.status_pending);
        }
    }
    @Override
    public int getItemViewType(int position) {
        Booking b = bookingList.get(position);
        return (b.isPackage() && b.getPackageId() != null) ? TYPE_PACKAGE : TYPE_SINGLE;
    }
    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout;
        if (isCalendarMode) {
            layout = R.layout.item_booking_student_calendar;
        } else {
            layout = (viewType == TYPE_PACKAGE) ? R.layout.item_booking_package_student : R.layout.item_booking;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new BookingViewHolder(view, viewType);
    }
    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);
        String status = booking.getStatus() != null ? booking.getStatus() : "pending";
        long now = System.currentTimeMillis();
        long duration = booking.getDuration() > 0 ? booking.getDuration() : LessonTimeHelper.DEFAULT_DURATION_MINUTES;
        long endTime = booking.getTimestamp() + (duration * 60 * 1000L);
        if (now > endTime && ("confirmed".equals(status) || "pending".equals(status) || "free_trial_pending".equals(status) || "suggestion_pending".equals(status))) {
            if (booking.getBookingId() != null) {
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings").child(booking.getBookingId());
                ref.child("status").setValue("cancelled");
                ref.child("cancellationReason").setValue("Time expired");
                booking.setStatus("cancelled");
            }
        }
        if (isCalendarMode) {
            bindCalendarView(holder, booking);
        } else if (holder.viewType == TYPE_PACKAGE) {
            bindPackageView(holder, booking);
        } else {
            bindSingleView(holder, booking);
        }
        fetchUserInfo(booking.getTutorId(), holder.tvName, holder.ivPhoto);
    }
    private void bindCalendarView(BookingViewHolder holder, Booking booking) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
        holder.tvTime.setText(timeFormat.format(new Date(booking.getTimestamp())));
        holder.tvDate.setText(dateFormat.format(new Date(booking.getTimestamp())));
        boolean isPkg = booking.isPackage() && booking.getPackageId() != null;
        if (holder.layoutPackageInfo != null) holder.layoutPackageInfo.setVisibility(View.GONE);
        if (holder.btnExpandPackage != null) holder.btnExpandPackage.setVisibility(View.GONE);
        if (holder.rvPackageLessons != null) holder.rvPackageLessons.setVisibility(View.GONE);
        if (holder.layoutPackageActions != null) holder.layoutPackageActions.setVisibility(View.GONE);
        if (holder.btnCancelStudent != null) holder.btnCancelStudent.setVisibility(View.GONE);
        holder.expanded = false;
        if (holder.btnExpandPackage != null) holder.btnExpandPackage.setText(R.string.ext_view_all_lessons_in_package_2);
        if (isPkg) {
            if (holder.layoutPackageInfo != null) holder.layoutPackageInfo.setVisibility(View.VISIBLE);
            if (holder.tvPackageBadge != null) {
                holder.tvPackageBadge.setText(context.getString(R.string.ext_package_lessons_count_format, booking.getPackageTotalLessons()));
            }
            if (holder.tvDuration != null) {
                long dur = booking.getDuration() > 0 ? booking.getDuration() : 60;
                holder.tvDuration.setText(context.getString(R.string.ext_duration_mins_format, (int) dur));
            }
            if (holder.btnExpandPackage != null) {
                holder.btnExpandPackage.setVisibility(View.VISIBLE);
                holder.btnExpandPackage.setOnClickListener(v -> {
                    if (holder.expanded) {
                        holder.expanded = false;
                        if (holder.rvPackageLessons != null) holder.rvPackageLessons.setVisibility(View.GONE);
                        holder.btnExpandPackage.setText(R.string.btn_view_all_package);
                    } else {
                        holder.expanded = true;
                        if (holder.rvPackageLessons != null) holder.rvPackageLessons.setVisibility(View.VISIBLE);
                        holder.btnExpandPackage.setText(R.string.btn_hide_lessons);
                    }
                });
            }
            if (holder.btnReviewCalendar != null) {
                holder.btnReviewCalendar.setVisibility(View.VISIBLE);
                holder.btnReviewCalendar.setOnClickListener(v -> {
                    holder.expanded = true;
                    if (holder.rvPackageLessons != null) holder.rvPackageLessons.setVisibility(View.VISIBLE);
                    if (holder.btnExpandPackage != null) holder.btnExpandPackage.setText(R.string.btn_hide_lessons);
                });
            }
            loadPackageLessonNumber(booking, holder);
            loadPackageDetails(booking, holder); 
            String curStatus = booking.getStatus() != null ? booking.getStatus() : "";
            if ("suggestion_pending".equalsIgnoreCase(curStatus)) {
                if (holder.layoutPackageActions != null) {
                    holder.layoutPackageActions.setVisibility(View.VISIBLE);
                    if (holder.btnReviewChanges != null) {
                        holder.btnReviewChanges.setText(R.string.ext_review_change_btn);
                        holder.btnReviewChanges.setOnClickListener(v -> showReviewChangesSheet(booking));
                    }
                    if (holder.btnAcceptProposal != null) {
                        holder.btnAcceptProposal.setText(R.string.ext_accept_proposal_btn);
                        holder.btnAcceptProposal.setOnClickListener(v -> acceptSuggestion(booking));
                    }
                }
            } else if ("pending".equalsIgnoreCase(curStatus) || "free_trial_pending".equalsIgnoreCase(curStatus)) {
                if (holder.layoutPackageActions != null) {
                    holder.layoutPackageActions.setVisibility(View.VISIBLE);
                    if (holder.btnReviewChanges != null) {
                        holder.btnReviewChanges.setText(R.string.ext_edit_proposal_btn);
                        holder.btnReviewChanges.setOnClickListener(v -> showReviewChangesSheet(booking));
                    }
                    if (holder.btnAcceptProposal != null) {
                        holder.btnAcceptProposal.setText(R.string.ext_cancel_proposal_btn);
                        holder.btnAcceptProposal.setOnClickListener(v -> performCancellation(booking, context.getString(R.string.msg_student_cancelled_proposal)));
                    }
                }
            } else if ("confirmed".equalsIgnoreCase(curStatus)) {
                if (holder.btnCancelStudent != null) {
                    holder.btnCancelStudent.setVisibility(View.VISIBLE);
                    holder.btnCancelStudent.setOnClickListener(v -> showReviewChangesSheet(booking));
                }
            }
        } else {
            if (holder.layoutPackageInfo != null) holder.layoutPackageInfo.setVisibility(View.GONE);
            holder.tvSubject.setText(SubjectHelper.getTranslatedSubject(booking.getSubject()));
        }
        String curStat = booking.getStatus() != null ? booking.getStatus() : "";
        holder.layoutSuggestion.setVisibility(View.GONE);
        holder.btnCancel.setVisibility(View.GONE);
        holder.btnAction.setVisibility(View.GONE);
        if ("cancelled".equalsIgnoreCase(curStat)) {
            holder.tvStatus.setText(context.getString(R.string.status_format_dot, getTranslatedStatus(context, curStat).toUpperCase(Locale.getDefault())));
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error));
        } else if ("suggestion_pending".equalsIgnoreCase(curStat)) {
            holder.tvStatus.setText(context.getString(R.string.status_format_dot, context.getString(R.string.ext_status_action_required).replace("• ", "").toUpperCase(Locale.getDefault())));
            holder.layoutSuggestion.setVisibility(View.VISIBLE);
            if (booking.getSuggestedTimestamp() > 0) {
                SimpleDateFormat dtFmt = new SimpleDateFormat("EEE, MMM dd 'at' HH:mm", Locale.getDefault());
                holder.tvSuggestionDetails.setText(context.getString(R.string.ext_reschedule_to_format, dtFmt.format(new Date(booking.getSuggestedTimestamp()))));
            } else {
                String source = booking.getSuggestedSourceDay() != null ? booking.getSuggestedSourceDay() : "Current";
                String dest = booking.getSuggestedDestDay() != null ? booking.getSuggestedDestDay() : "New Day";
                String time = booking.getSuggestedTime() != null ? booking.getSuggestedTime() : "New Time";
                holder.tvSuggestionDetails.setText(context.getString(R.string.ext_move_day_format, DayTranslationHelper.translateDay(context, source), DayTranslationHelper.translateDay(context, dest), time));
            }
            String sMsg = booking.getSuggestionMessage() != null ? booking.getSuggestionMessage() : "";
            holder.tvSuggestionMsg.setText(String.format(Locale.getDefault(), "\"%s\"", sMsg));
            holder.btnAcceptSuggestion.setOnClickListener(v -> acceptSuggestion(booking));
            holder.btnRejectSuggestion.setOnClickListener(v -> rejectSuggestion(booking));
        } else {
            holder.tvStatus.setText(context.getString(R.string.status_format_dot, getTranslatedStatus(context, curStat).toUpperCase(Locale.getDefault())));
            if ("confirmed".equals(curStat)) {
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.gapfix_green));
                holder.btnAction.setVisibility(View.VISIBLE);
                holder.btnAction.setText(R.string.ext_join_2);
                holder.btnAction.setOnClickListener(v -> joinLesson(booking));
            } else {
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.gapfix_text_secondary));
            }
            if ("pending".equals(curStat) || "confirmed".equals(curStat) || "free_trial_pending".equals(curStat)) {
                if (!isPkg) { 
                    holder.btnCancel.setVisibility(View.VISIBLE);
                    holder.btnCancel.setOnClickListener(v -> showCancelDialog(booking));
                }
            }
        }
    }
    private void loadPackageLessonNumber(Booking booking, BookingViewHolder holder) {
        FirebaseDatabase.getInstance().getReference("Bookings")
                .orderByChild("packageId").equalTo(booking.getPackageId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Booking> list = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Booking b = ds.getValue(Booking.class);
                            if (b != null) {
                                b.setBookingId(ds.getKey());
                                list.add(b);
                            }
                        }
                        list.sort(Comparator.comparingLong(Booking::getTimestamp));
                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i).getBookingId().equals(booking.getBookingId())) {
                                String sj = context.getString(R.string.ext_lesson_number_format, SubjectHelper.getTranslatedSubject(booking.getSubject()), i + 1);
                                holder.tvSubject.setText(sj);
                                break;
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        }
                });
    }
    private void bindSingleView(BookingViewHolder holder, Booking booking) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
        holder.tvTime.setText(timeFormat.format(new Date(booking.getTimestamp())));
        holder.tvDate.setText(dateFormat.format(new Date(booking.getTimestamp())));
        String status = booking.getStatus() != null ? booking.getStatus() : "";
        holder.layoutSuggestion.setVisibility(View.GONE);
        holder.itemView.setBackgroundTintList(null);
        if ("cancelled".equalsIgnoreCase(status)) {
            holder.tvStatus.setText(context.getString(R.string.status_format_dot, getTranslatedStatus(context, status).toUpperCase(Locale.getDefault())));
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error));
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnAction.setVisibility(View.GONE);
        } else if ("suggestion_pending".equalsIgnoreCase(status)) {
            holder.tvStatus.setText(context.getString(R.string.status_format_dot, context.getString(R.string.ext_status_action_required_tutor).replace("• ", "").toUpperCase(Locale.getDefault())));
            holder.tvStatus.setTextColor(Color.parseColor("#C53030"));
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnAction.setVisibility(View.GONE);
            if (holder.btnReschedule != null) holder.btnReschedule.setVisibility(View.GONE);
            holder.itemView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF5F5")));
            holder.layoutSuggestion.setVisibility(View.VISIBLE);
            if (booking.getSuggestedTimestamp() > 0) {
                SimpleDateFormat dtFmt = new SimpleDateFormat("EEE, MMM dd 'at' HH:mm", Locale.getDefault());
                holder.tvSuggestionDetails.setText(context.getString(R.string.ext_reschedule_to_format, dtFmt.format(new Date(booking.getSuggestedTimestamp()))));
            } else {
                String source = booking.getSuggestedSourceDay() != null ? booking.getSuggestedSourceDay() : "Current";
                String dest = booking.getSuggestedDestDay() != null ? booking.getSuggestedDestDay() : "New Day";
                String time = booking.getSuggestedTime() != null ? booking.getSuggestedTime() : "New Time";
                holder.tvSuggestionDetails.setText(context.getString(R.string.ext_move_day_format, DayTranslationHelper.translateDay(context, source), DayTranslationHelper.translateDay(context, dest), time));
            }
            String sMsg = booking.getSuggestionMessage() != null ? booking.getSuggestionMessage() : "";
            holder.tvSuggestionMsg.setText(String.format(Locale.getDefault(), "\"%s\"", sMsg));
            holder.btnAcceptSuggestion.setOnClickListener(v -> acceptSuggestion(booking));
            holder.btnRejectSuggestion.setOnClickListener(v -> rejectSuggestion(booking));
        } else {
            holder.tvStatus.setText(context.getString(R.string.status_format_dot, getTranslatedStatus(context, status).toUpperCase(Locale.getDefault())));
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.gapfix_text_secondary));
            holder.btnCancel.setVisibility(("pending".equals(status) || "confirmed".equals(status) || "free_trial_pending".equals(status)) ? View.VISIBLE : View.GONE);
            holder.btnCancel.setOnClickListener(v -> showCancelDialog(booking));
            if (holder.btnReschedule != null) {
                holder.btnReschedule.setVisibility(("pending".equals(status) || "confirmed".equals(status) || "free_trial_pending".equals(status)) ? View.VISIBLE : View.GONE);
                holder.btnReschedule.setOnClickListener(v -> showReviewChangesSheet(booking));
            }
            if ("confirmed".equals(status)) {
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.gapfix_green));
                holder.btnAction.setVisibility(View.VISIBLE);
                holder.btnAction.setText(R.string.ext_join_2);
                holder.btnAction.setOnClickListener(v -> joinLesson(booking));
            } else {
                holder.btnAction.setVisibility(View.GONE);
            }
        }
    }
    private void bindPackageView(BookingViewHolder holder, Booking booking) {
        String status = booking.getStatus() != null ? booking.getStatus() : "";
        holder.expanded = false;
        if (holder.rvPackageLessons != null) holder.rvPackageLessons.setVisibility(View.GONE);
        if (holder.btnExpandPackage != null) holder.btnExpandPackage.setText(R.string.ext_view_all_lessons_in_package_2);
        String statusDisplay = SubjectHelper.getTranslatedSubject(booking.getSubject()) + " (" + context.getString(R.string.ext_package) + " · " + booking.getPackageTotalLessons() + " " + context.getString(R.string.ext_lessons) + ")";
        if ("suggestion_pending".equalsIgnoreCase(status)) {
            statusDisplay += " [" + context.getString(R.string.ext_review_change).toUpperCase(Locale.getDefault()) + "]";
            holder.tvSubject.setTextColor(Color.parseColor("#C53030"));
        } else if ("confirmed".equalsIgnoreCase(status)) {
            statusDisplay += " [" + getTranslatedStatus(context, status).toUpperCase(Locale.getDefault()) + "]";
            holder.tvSubject.setTextColor(ContextCompat.getColor(context, R.color.gapfix_green));
        } else if ("cancelled".equalsIgnoreCase(status)) {
            statusDisplay += " [" + getTranslatedStatus(context, status).toUpperCase(Locale.getDefault()) + "]";
            holder.tvSubject.setTextColor(ContextCompat.getColor(context, R.color.error));
        } else {
            statusDisplay += " [" + getTranslatedStatus(context, status).toUpperCase(Locale.getDefault()) + "]";
            holder.tvSubject.setTextColor(ContextCompat.getColor(context, R.color.gapfix_text_secondary));
        }
        holder.tvSubject.setText(statusDisplay);
        loadPackageDetails(booking, holder);
        if ("suggestion_pending".equalsIgnoreCase(status)) {
            if (holder.layoutActions != null) holder.layoutActions.setVisibility(View.VISIBLE);
            if (holder.btnReviewChanges != null) {
                holder.btnReviewChanges.setText(R.string.ext_review_change_btn);
                holder.btnReviewChanges.setVisibility(View.VISIBLE);
                holder.btnReviewChanges.setOnClickListener(v -> showReviewChangesSheet(booking));
            }
            if (holder.btnAcceptProposal != null) {
                holder.btnAcceptProposal.setVisibility(View.VISIBLE);
                holder.btnAcceptProposal.setText(R.string.ext_accept_proposal_btn);
                holder.btnAcceptProposal.setOnClickListener(v -> acceptSuggestion(booking));
            }
            if (holder.btnRejectProposal != null) {
                holder.btnRejectProposal.setVisibility(View.VISIBLE);
                holder.btnRejectProposal.setText(R.string.ext_reject);
                holder.btnRejectProposal.setOnClickListener(v -> rejectSuggestion(booking));
            }
            holder.layoutSuggestion.setVisibility(View.VISIBLE);
            boolean isSingleShifted = context.getString(R.string.label_this_lesson).equals(booking.getSuggestedSourceDay());
            if (isSingleShifted && booking.getSuggestedTimestamp() > 0) {
                SimpleDateFormat dtFmt = new SimpleDateFormat("EEE, MMM dd 'at' HH:mm", Locale.getDefault());
                holder.tvSuggestionDetails.setText(context.getString(R.string.ext_reschedule_lesson_format, dtFmt.format(new Date(booking.getSuggestedTimestamp()))));
            } else {
                String src = booking.getSuggestedSourceDay() != null ? booking.getSuggestedSourceDay() : "Current";
                String dst = booking.getSuggestedDestDay() != null ? booking.getSuggestedDestDay() : "New Day";
                String tm = booking.getSuggestedTime() != null ? booking.getSuggestedTime() : "New Time";
                holder.tvSuggestionDetails.setText(context.getString(R.string.ext_move_all_day_format, DayTranslationHelper.translateDay(context, src), DayTranslationHelper.translateDay(context, dst), tm));
            }
            String sMsg = booking.getSuggestionMessage() != null ? booking.getSuggestionMessage() : context.getString(R.string.ext_no_message);
            holder.tvSuggestionMsg.setText(String.format(Locale.getDefault(), "\"%s\"", sMsg));
        } else if ("pending".equalsIgnoreCase(status) || "free_trial_pending".equalsIgnoreCase(status)) {
            if (holder.layoutActions != null) holder.layoutActions.setVisibility(View.VISIBLE);
            if (holder.btnReviewChanges != null) {
                holder.btnReviewChanges.setText(R.string.ext_edit_proposal_btn);
                holder.btnReviewChanges.setVisibility(View.VISIBLE);
                holder.btnReviewChanges.setOnClickListener(v -> showReviewChangesSheet(booking));
            }
            if (holder.btnAcceptProposal != null) holder.btnAcceptProposal.setVisibility(View.GONE);
            if (holder.btnRejectProposal != null) {
                holder.btnRejectProposal.setVisibility(View.VISIBLE);
                holder.btnRejectProposal.setText(R.string.ext_cancel_proposal_btn);
                holder.btnRejectProposal.setOnClickListener(v -> performCancellation(booking, context.getString(R.string.msg_student_cancelled_proposal)));
            }
        } else if ("confirmed".equalsIgnoreCase(status)) {
            if (holder.layoutActions != null) holder.layoutActions.setVisibility(View.GONE);
            if (holder.btnCancelStudent != null) {
                holder.btnCancelStudent.setVisibility(View.VISIBLE);
                holder.btnCancelStudent.setOnClickListener(v -> showReviewChangesSheet(booking));
            }
        } else if ("cancelled".equalsIgnoreCase(status)) {
            if (holder.layoutActions != null) holder.layoutActions.setVisibility(View.GONE);
            if (holder.btnCancelStudent != null) holder.btnCancelStudent.setVisibility(View.GONE);
        } else {
            if (holder.layoutActions != null) holder.layoutActions.setVisibility(View.VISIBLE);
            if (holder.btnReviewChanges != null) holder.btnReviewChanges.setVisibility(View.GONE);
            if (holder.btnAcceptProposal != null) holder.btnAcceptProposal.setVisibility(View.GONE);
            if (holder.btnRejectProposal != null) {
                holder.btnRejectProposal.setVisibility(View.VISIBLE);
                holder.btnRejectProposal.setText(R.string.ext_cancel_proposal_btn);
                holder.btnRejectProposal.setOnClickListener(v -> performCancellation(booking, context.getString(R.string.msg_student_cancelled_proposal)));
            }
            if (holder.btnCancelStudent != null) holder.btnCancelStudent.setVisibility(View.GONE);
        }
        if (holder.btnExpandPackage != null) {
            holder.btnExpandPackage.setOnClickListener(v -> {
                if (holder.expanded) {
                    holder.expanded = false;
                    if (holder.rvPackageLessons != null) holder.rvPackageLessons.setVisibility(View.GONE);
                    holder.btnExpandPackage.setText(R.string.btn_view_all_package);
                } else {
                    holder.expanded = true;
                    if (holder.rvPackageLessons != null) holder.rvPackageLessons.setVisibility(View.VISIBLE);
                    holder.btnExpandPackage.setText(R.string.btn_hide_lessons);
                }
            });
        }
    }
    private void loadPackageDetails(Booking booking, BookingViewHolder holder) {
        String pkgId = booking.getPackageId();
        if (pkgId == null) return;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        ref.orderByChild("packageId").equalTo(pkgId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Booking> list = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Booking b = ds.getValue(Booking.class);
                            if (b != null) {
                                b.setBookingId(ds.getKey());
                                list.add(b);
                            }
                        }
                        if (list.isEmpty()) {
                            return;
                        }
                        list.sort(Comparator.comparingLong(Booking::getTimestamp));
                        if (holder.rvPackageLessons != null) {
                            if (holder.rvPackageLessons.getLayoutManager() == null) {
                                holder.rvPackageLessons.setLayoutManager(new LinearLayoutManager(context));
                            }
                            holder.rvPackageLessons.setAdapter(new PackageLessonsAdapter(list));
                        }
                        SimpleDateFormat shortDate = new SimpleDateFormat("MMM dd", Locale.getDefault());
                        SimpleDateFormat fullDate  = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                        SimpleDateFormat dayFmt = new SimpleDateFormat("EEE", Locale.getDefault());
                        int total = list.size();
                        long startTs = list.get(0).getTimestamp();
                        long endTs   = list.get(total - 1).getTimestamp();
                        Set<String> days = new TreeSet<>();
                        for (Booking b : list) days.add(dayFmt.format(new Date(b.getTimestamp())));
                        String label = "confirmed".equalsIgnoreCase(booking.getStatus()) ? context.getString(R.string.ext_lesson_pattern_label) : context.getString(R.string.ext_proposed_pattern_label);
                        String pattern = label + String.join("/", days) + context.getString(R.string.ext_view_list_suffix);
                        if (holder.tvSummaryPattern != null) holder.tvSummaryPattern.setText(pattern);
                        if (holder.tvSummaryStart != null) {
                            holder.tvSummaryStart.setText(context.getString(R.string.ext_start_date_format, fullDate.format(new Date(startTs))));
                        }
                        if (holder.tvSummaryEnd != null) {
                            holder.tvSummaryEnd.setText(context.getString(R.string.ext_end_date_format, fullDate.format(new Date(endTs))));
                        }
                        StringBuilder preview = new StringBuilder();
                        int previewCount = Math.min(4, total);
                        for (int i = 0; i < previewCount; i++) {
                            preview.append("• ").append(shortDate.format(new Date(list.get(i).getTimestamp())));
                            if (i < previewCount - 1) preview.append("\n");
                        }
                        if (total > previewCount) {
                            preview.append(context.getString(R.string.ext_plus_more_format, total - previewCount));
                        }
                        if (holder.tvDatesPreview != null) holder.tvDatesPreview.setText(preview.toString());
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        }
                });
    }
    private void joinLesson(Booking booking) {
        if (LessonTimeHelper.isJoinable(booking, "student")) {
            Intent i = new Intent(context, VideoCallActivity.class);
            i.putExtra("BOOKING_ID", booking.getBookingId());
            i.putExtra("IS_INCOMING", false);
            context.startActivity(i);
        } else {
            Toast.makeText(context, R.string.ext_join_active_soon, Toast.LENGTH_SHORT).show();
        }
    }
    private void showReviewChangesSheet(Booking booking) {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
        View v = LayoutInflater.from(context).inflate(R.layout.layout_suggest_reject_sheet, null);
        sheet.setContentView(v);
        TextView tvName = v.findViewById(R.id.tvSheetStudentName);
        tvName.setText(booking.getTutorName());
        v.findViewById(R.id.btnSheetRejectProposal).setVisibility(View.GONE);
        boolean isSingleLesson = !booking.isPackage() || booking.getPackageId() == null;
        if (isSingleLesson) {
            v.findViewById(R.id.rgChangeScope).setVisibility(View.GONE);
            v.findViewById(R.id.layoutChangeOptions).setVisibility(View.GONE);
            v.findViewById(R.id.layoutChangeSingleLesson).setVisibility(View.VISIBLE);
            View summaryTotal = v.findViewById(R.id.tvSheetSummaryTotal);
            if (summaryTotal != null) summaryTotal.setVisibility(View.GONE);
            View summaryStart = v.findViewById(R.id.tvSheetSummaryStart);
            if (summaryStart != null) summaryStart.setVisibility(View.GONE);
            View summarySchedule = v.findViewById(R.id.tvSheetSummarySchedule);
            if (summarySchedule != null) summarySchedule.setVisibility(View.GONE);
            View summaryEnd = v.findViewById(R.id.tvSheetSummaryEnd);
            if (summaryEnd != null) summaryEnd.setVisibility(View.GONE);
            TextView tvPkgInfo = v.findViewById(R.id.tvSheetPackageInfo);
            if (tvPkgInfo != null) {
                String sjInfo = SubjectHelper.getTranslatedSubject(booking.getSubject()) + context.getString(R.string.ext_single_lesson_suffix);
                tvPkgInfo.setText(sjInfo);
            }
            MaterialButton btnDestDate = v.findViewById(R.id.btnDestDateSingle);
            MaterialButton btnDestTimeSingle = v.findViewById(R.id.btnDestTimeSingle);
            final long[] selectedDate = {0};
            final int[] selectedHour = {-1};
            final int[] selectedMinute = {-1};
            btnDestDate.setOnClickListener(b -> {
                com.google.android.material.datepicker.MaterialDatePicker<Long> dp =
                        com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                                .setTitleText(context.getString(R.string.ext_select_date))
                                .setSelection(com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds())
                                .build();
                dp.addOnPositiveButtonClickListener(selection -> {
                    selectedDate[0] = selection;
                    java.util.Calendar utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
                    utcCal.setTimeInMillis(selection);
                    java.util.Calendar localCal = java.util.Calendar.getInstance();
                    localCal.set(utcCal.get(java.util.Calendar.YEAR), utcCal.get(java.util.Calendar.MONTH), utcCal.get(java.util.Calendar.DAY_OF_MONTH));
                    btnDestDate.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(localCal.getTime()));
                });
                dp.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "STUDENT_DATE_PICK");
            });
            btnDestTimeSingle.setOnClickListener(b -> {
                com.google.android.material.timepicker.MaterialTimePicker tp =
                        new com.google.android.material.timepicker.MaterialTimePicker.Builder()
                                .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
                                .setHour(12).setMinute(0)
                                .setTitleText(context.getString(R.string.ext_select_time))
                                .build();
                tp.addOnPositiveButtonClickListener(v2 -> {
                    selectedHour[0] = tp.getHour();
                    selectedMinute[0] = tp.getMinute();
                    btnDestTimeSingle.setText(String.format(Locale.getDefault(), "%02d:%02d", tp.getHour(), tp.getMinute()));
                });
                tp.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "STUDENT_TIME_PICK");
            });
            v.findViewById(R.id.btnSheetSubmitCounter).setOnClickListener(btn -> {
                if (selectedDate[0] == 0 || selectedHour[0] == -1) {
                    Toast.makeText(context, context.getString(R.string.ext_select_date) + " & " + context.getString(R.string.ext_select_time), Toast.LENGTH_SHORT).show();
                    return;
                }
                String msg = ((com.google.android.material.textfield.TextInputEditText) v.findViewById(R.id.etSheetMessage)).getText().toString().trim();
                java.util.Calendar utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
                utcCal.setTimeInMillis(selectedDate[0]);
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(utcCal.get(java.util.Calendar.YEAR), utcCal.get(java.util.Calendar.MONTH),
                        utcCal.get(java.util.Calendar.DAY_OF_MONTH), selectedHour[0], selectedMinute[0], 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                long newTimestamp = cal.getTimeInMillis();
                DatabaseReference dr = FirebaseDatabase.getInstance().getReference("Bookings").child(booking.getBookingId());
                dr.child("suggestedTimestamp").setValue(newTimestamp);
                dr.child("suggestionMessage").setValue(context.getString(R.string.ext_student_msg_prefix, msg));
                dr.child("status").setValue("pending");
                Toast.makeText(context, R.string.ext_reschedule_request_sent, Toast.LENGTH_SHORT).show();
                sheet.dismiss();
            });
            v.findViewById(R.id.btnSheetCancelAction).setOnClickListener(btn -> sheet.dismiss());
            sheet.show();
            return;
        }
        List<Booking> cachedLessons = new ArrayList<>();
        List<String> lessonStrings = new ArrayList<>();
        String[] defaultDays = DayTranslationHelper.getTranslatedDaysArray(context);
        List<String> activeDaysList = new ArrayList<>();
        android.widget.RadioGroup rgScope = v.findViewById(R.id.rgChangeScope);
        TextView tvSourceLabel = v.findViewById(R.id.tvSourcePatternLabel);
        android.widget.Spinner spinSource = v.findViewById(R.id.spinnerSourceDay);
        android.widget.Spinner spinDest = v.findViewById(R.id.spinnerDestDay);
        MaterialButton btnDestTime = v.findViewById(R.id.btnDestTime);
        android.widget.Spinner spinLessons = v.findViewById(R.id.spinnerLessonsInPackage);
        View layoutChangeOptions = v.findViewById(R.id.layoutChangeOptions);
        View layoutChangeSingleLesson = v.findViewById(R.id.layoutChangeSingleLesson);
        MaterialButton btnDestDateSingle = v.findViewById(R.id.btnDestDateSingle);
        MaterialButton btnDestTimeSingle = v.findViewById(R.id.btnDestTimeSingle);
        final long[] selectedDate = {0};
        final int[] selectedHour = {-1};
        final int[] selectedMinute = {-1};
        btnDestDateSingle.setOnClickListener(b -> {
            com.google.android.material.datepicker.MaterialDatePicker<Long> dp =
                    com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                            .setTitleText(context.getString(R.string.ext_select_date))
                            .setSelection(com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds())
                            .build();
            dp.addOnPositiveButtonClickListener(selection -> {
                selectedDate[0] = selection;
                java.util.Calendar utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
                utcCal.setTimeInMillis(selection);
                java.util.Calendar localCal = java.util.Calendar.getInstance();
                localCal.set(utcCal.get(java.util.Calendar.YEAR), utcCal.get(java.util.Calendar.MONTH), utcCal.get(java.util.Calendar.DAY_OF_MONTH));
                btnDestDateSingle.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(localCal.getTime()));
            });
            dp.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "STUDENT_PKG_DATE_PICK");
        });
        btnDestTimeSingle.setOnClickListener(b -> {
            com.google.android.material.timepicker.MaterialTimePicker tp =
                    new com.google.android.material.timepicker.MaterialTimePicker.Builder()
                            .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
                            .setHour(12).setMinute(0)
                            .setTitleText(context.getString(R.string.ext_select_time))
                            .build();
            tp.addOnPositiveButtonClickListener(v2 -> {
                selectedHour[0] = tp.getHour();
                selectedMinute[0] = tp.getMinute();
                btnDestTimeSingle.setText(String.format(Locale.getDefault(), "%02d:%02d", tp.getHour(), tp.getMinute()));
            });
            tp.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "STUDENT_TIME_PICK");
        });
        android.widget.ArrayAdapter<String> adapterSource = new android.widget.ArrayAdapter<>(context, android.R.layout.simple_spinner_item, activeDaysList);
        adapterSource.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinSource.setAdapter(adapterSource);
        android.widget.ArrayAdapter<String> adapterDest = new android.widget.ArrayAdapter<>(context, android.R.layout.simple_spinner_item, defaultDays);
        adapterDest.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinDest.setAdapter(adapterDest);
        android.widget.ArrayAdapter<String> adapterLessons = new android.widget.ArrayAdapter<>(context, android.R.layout.simple_spinner_item, lessonStrings);
        adapterLessons.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinLessons.setAdapter(adapterLessons);
        rgScope.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbScopeSingle) {
                if (tvSourceLabel != null) tvSourceLabel.setText(R.string.select_lesson_label);
                if (layoutChangeOptions != null) layoutChangeOptions.setVisibility(View.GONE);
                if (layoutChangeSingleLesson != null) layoutChangeSingleLesson.setVisibility(View.VISIBLE);
            } else {
                if (tvSourceLabel != null) tvSourceLabel.setText(R.string.source_pattern_label);
                if (layoutChangeOptions != null) layoutChangeOptions.setVisibility(View.VISIBLE);
                if (layoutChangeSingleLesson != null) layoutChangeSingleLesson.setVisibility(View.GONE);
            }
        });
        btnDestTime.setOnClickListener(b -> {
            com.google.android.material.timepicker.MaterialTimePicker tp = new com.google.android.material.timepicker.MaterialTimePicker.Builder()
                    .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
                    .setHour(12).setMinute(0).setTitleText(context.getString(R.string.ext_select_time)).build();
            tp.addOnPositiveButtonClickListener(v2 -> btnDestTime.setText(String.format(Locale.getDefault(), "%02d:%02d", tp.getHour(), tp.getMinute())));
            tp.show(((androidx.fragment.app.FragmentActivity)context).getSupportFragmentManager(), "STUDENT_SUGGEST_TIME");
        });
        v.findViewById(R.id.btnSheetSubmitCounter).setOnClickListener(btn -> {
            String msgText = ((com.google.android.material.textfield.TextInputEditText) v.findViewById(R.id.etSheetMessage)).getText().toString().trim();
            if (rgScope.getCheckedRadioButtonId() == R.id.rbScopeSingle) {
                int selectedPos = spinLessons.getSelectedItemPosition();
                if (selectedPos >= 0 && selectedPos < cachedLessons.size()) {
                    if (selectedDate[0] == 0 || selectedHour[0] == -1) {
                        Toast.makeText(context, R.string.no_image_selected, Toast.LENGTH_SHORT).show(); 
                        return;
                    }
                    java.util.Calendar utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
                    utcCal.setTimeInMillis(selectedDate[0]);
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.set(utcCal.get(java.util.Calendar.YEAR), utcCal.get(java.util.Calendar.MONTH),
                            utcCal.get(java.util.Calendar.DAY_OF_MONTH), selectedHour[0], selectedMinute[0], 0);
                    cal.set(java.util.Calendar.MILLISECOND, 0);
                    long newTimestamp = cal.getTimeInMillis();
                    Booking targetLesson = cachedLessons.get(selectedPos);
                    DatabaseReference dr = FirebaseDatabase.getInstance().getReference("Bookings").child(targetLesson.getBookingId());
                    dr.child("suggestedTimestamp").setValue(newTimestamp);
                    dr.child("suggestedSourceDay").setValue(context.getString(R.string.label_this_lesson));
                    dr.child("suggestionMessage").setValue(context.getString(R.string.ext_student_msg_prefix, msgText));
                    dr.child("status").setValue("pending");
                    Toast.makeText(context, R.string.ext_alt_suggestion_specific_sent, Toast.LENGTH_SHORT).show();
                    sheet.dismiss();
                }
            } else {
                String sourceDayStr = activeDaysList.get(spinSource.getSelectedItemPosition());
                String dbSourceDay = DayTranslationHelper.getEnglishDayFromTranslated(context, sourceDayStr);
                String destDayStr = spinDest.getSelectedItem().toString();
                String dbDestDay = DayTranslationHelper.getEnglishDayFromTranslated(context, destDayStr);
                DatabaseReference dr = FirebaseDatabase.getInstance().getReference("Bookings").child(booking.getBookingId());
                dr.child("suggestedSourceDay").setValue(dbSourceDay);
                dr.child("suggestedDestDay").setValue(dbDestDay);
                dr.child("suggestedTime").setValue(btnDestTime.getText().toString());
                dr.child("suggestionMessage").setValue(context.getString(R.string.ext_student_msg_prefix, msgText));
                dr.child("status").setValue("pending");
                Toast.makeText(context, R.string.ext_alt_suggestion_sent, Toast.LENGTH_SHORT).show();
                sheet.dismiss();
            }
        });
        FirebaseDatabase.getInstance().getReference("Bookings")
                .orderByChild("packageId").equalTo(booking.getPackageId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Booking> lessons = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Booking b = ds.getValue(Booking.class);
                            if (b != null) {
                                b.setBookingId(ds.getKey());
                                lessons.add(b);
                            }
                        }
                        lessons.sort(Comparator.comparingLong(Booking::getTimestamp));
                        cachedLessons.clear();
                        lessonStrings.clear();
                        List<String> rawActive = new ArrayList<>();
                        SimpleDateFormat fullDayName = new SimpleDateFormat("EEEE", Locale.US);
                        SimpleDateFormat lessonFormat = new SimpleDateFormat("MMM dd, yyyy @ HH:mm", Locale.getDefault());
                        for (Booking b : lessons) {
                            cachedLessons.add(b);
                            lessonStrings.add(context.getString(R.string.ext_lesson_item_prefix) + lessonFormat.format(new Date(b.getTimestamp())));
                            String engDay = fullDayName.format(new Date(b.getTimestamp())) + "s";
                            if (!rawActive.contains(engDay)) {
                                rawActive.add(engDay);
                                activeDaysList.add(DayTranslationHelper.translateDay(context, engDay));
                            }
                        }
                        adapterSource.notifyDataSetChanged();
                        adapterLessons.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        }
                });
        v.findViewById(R.id.btnSheetCancelAction).setOnClickListener(btn -> sheet.dismiss());
        sheet.show();
    }
    private void fetchUserInfo(String userId, TextView tvName, ImageView ivPhoto) {
        FirebaseDatabase.getInstance().getReference("Users").child("Tutor").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            tvName.setText(snapshot.child("name").getValue(String.class));
                            String img = snapshot.child("imageResourceLink").getValue(String.class);
                            if (img == null) img = snapshot.child("profilePicture").getValue(String.class);
                            Glide.with(context).load(img).placeholder(R.drawable.person_circle).circleCrop().into(ivPhoto);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        }
                });
    }
    private void acceptSuggestion(Booking booking) {
        if (!booking.isPackage() || booking.getPackageId() == null) {
            DatabaseReference dr = FirebaseDatabase.getInstance().getReference("Bookings").child(booking.getBookingId());
            Map<String, Object> updates = new HashMap<>();
            if (booking.getSuggestedTimestamp() > 0) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
                Date newDate = new Date(booking.getSuggestedTimestamp());
                updates.put("timestamp", booking.getSuggestedTimestamp());
                updates.put("lessonTime", timeFormat.format(newDate));
                updates.put("lessonDate", dateFormat.format(newDate));
                updates.put("suggestedTimestamp", null);
            }
            updates.put("status", "confirmed");
            updates.put("suggestedSourceDay", null);
            updates.put("suggestedDestDay", null);
            updates.put("suggestedTime", null);
            updates.put("suggestionMessage", null);
            dr.updateChildren(updates);
            Toast.makeText(context, R.string.ext_suggestion_accepted_toast, Toast.LENGTH_SHORT).show();
            return;
        }
        final String sourceDayStr = booking.getSuggestedSourceDay();
        final String destDayStr = booking.getSuggestedDestDay();
        final String newTime = booking.getSuggestedTime();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        ref.orderByChild("packageId").equalTo(booking.getPackageId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Booking b = ds.getValue(Booking.class);
                            if (b == null) continue;
                            DatabaseReference dr = ds.getRef();
                            if (context.getString(R.string.label_this_lesson).equals(sourceDayStr)) {
                                if (b.getSuggestedTimestamp() > 0) {
                                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
                                    Date newDate = new Date(b.getSuggestedTimestamp());
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("status", "confirmed");
                                    updates.put("timestamp", b.getSuggestedTimestamp());
                                    updates.put("lessonTime", timeFormat.format(newDate));
                                    updates.put("lessonDate", dateFormat.format(newDate));
                                    updates.put("suggestedTimestamp", null);
                                    updates.put("suggestedSourceDay", null);
                                    updates.put("suggestedDestDay", null);
                                    updates.put("suggestedTime", null);
                                    updates.put("suggestionMessage", null);
                                    dr.updateChildren(updates);
                                } else {
                                    dr.child("status").setValue("confirmed");
                                }
                                continue;
                            }
                            boolean shouldShift = false;
                            if (sourceDayStr != null) {
                                java.util.Calendar cal = java.util.Calendar.getInstance();
                                cal.setTimeInMillis(b.getTimestamp());
                                String currentDay = new SimpleDateFormat("EEEE", Locale.US).format(cal.getTime()) + "s";
                                if (currentDay.equalsIgnoreCase(sourceDayStr)) {
                                    shouldShift = true;
                                }
                            }
                            if (shouldShift && destDayStr != null && newTime != null) {
                                applyShift(dr, b, sourceDayStr, destDayStr, newTime);
                            } else {
                                dr.child("status").setValue("confirmed");
                                dr.child("suggestedSourceDay").removeValue();
                                dr.child("suggestedDestDay").removeValue();
                                dr.child("suggestedTime").removeValue();
                                dr.child("suggestedTimestamp").removeValue();
                                dr.child("suggestionMessage").removeValue();
                            }
                        }
                        Toast.makeText(context, R.string.ext_suggestion_accepted_toast, Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        }
                });
    }
    private void applyShift(DatabaseReference dr, Booking b, String source, String dest, String time) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(b.getTimestamp());
        if (!context.getString(R.string.label_this_lesson).equals(source)) {
            int sourceDay = getDayOfWeek(source);
            int destDay = getDayOfWeek(dest);
            int diff = destDay - sourceDay;
            cal.add(java.util.Calendar.DATE, diff);
        } else {
            int destDay = getDayOfWeek(dest + "s");
            while (cal.get(java.util.Calendar.DAY_OF_WEEK) != destDay) {
                cal.add(java.util.Calendar.DATE, 1);
            }
        }
        try {
            String[] parts = time.split(":");
            cal.set(java.util.Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
            cal.set(java.util.Calendar.MINUTE, Integer.parseInt(parts[1]));
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
        } catch (Exception e) {
            }
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "confirmed");
        updates.put("timestamp", cal.getTimeInMillis());
        updates.put("lessonTime", timeFormat.format(cal.getTime()));
        updates.put("lessonDate", dateFormat.format(cal.getTime()));
        updates.put("suggestedSourceDay", null);
        updates.put("suggestedDestDay", null);
        updates.put("suggestedTime", null);
        updates.put("suggestedTimestamp", null);
        updates.put("suggestionMessage", null);
        dr.updateChildren(updates);
    }
    private int getDayOfWeek(String dayStr) {
        if (dayStr == null) return java.util.Calendar.MONDAY;
        switch (dayStr.toLowerCase(Locale.getDefault())) {
            case "tuesdays": return java.util.Calendar.TUESDAY;
            case "wednesdays": return java.util.Calendar.WEDNESDAY;
            case "thursdays": return java.util.Calendar.THURSDAY;
            case "fridays": return java.util.Calendar.FRIDAY;
            case "saturdays": return java.util.Calendar.SATURDAY;
            case "sundays": return java.util.Calendar.SUNDAY;
            case "mondays":
            default: return java.util.Calendar.MONDAY;
        }
    }
    private void rejectSuggestion(Booking booking) {
        performCancellation(booking, "Student rejected schedule change.");
    }
    private void performCancellation(Booking booking, String reason) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        if (booking.isPackage() && booking.getPackageId() != null) {
            ref.orderByChild("packageId").equalTo(booking.getPackageId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                ds.getRef().child("status").setValue("cancelled");
                                ds.getRef().child("cancellationReason").setValue(reason);
                            }
                            Toast.makeText(context, R.string.file_deleted, Toast.LENGTH_SHORT).show(); 
                        }
                        @Override public void onCancelled(@NonNull DatabaseError e) {
                            }
                    });
        } else {
            if (booking.isFree() && booking.getStudentId() != null && booking.getTutorId() != null && booking.getSubject() != null) {
                FirebaseDatabase.getInstance().getReference("FreeLessonsUsed")
                        .child(booking.getStudentId())
                        .child(booking.getTutorId())
                        .child(booking.getSubject())
                        .removeValue();
            }
            ref.child(booking.getBookingId()).child("status").setValue("cancelled");
            ref.child(booking.getBookingId()).child("cancellationReason").setValue(reason);
            Toast.makeText(context, R.string.file_deleted, Toast.LENGTH_SHORT).show();
        }
    }
    private void showCancelDialog(Booking booking) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle(R.string.delete_archive_title) 
                .setMessage(R.string.delete_archive_message)
                .setPositiveButton(R.string.ext_accept, (d, w) -> performCancellation(booking, "Student cancelled"))
                .setNegativeButton(R.string.cancel, null).show();
    }
    @Override public int getItemCount() { return bookingList.size(); }
    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        final int viewType;
        ImageView ivPhoto;
        TextView tvName, tvSubject, tvTime, tvDate, tvStatus;
        MaterialButton btnCancel, btnAction, btnReschedule;
        View layoutSuggestion, layoutPackageInfo, layoutPackageActions;
        TextView tvSuggestionDetails, tvSuggestionMsg, tvPackageBadge, tvDuration;
        MaterialButton btnAcceptSuggestion, btnRejectSuggestion;
        TextView tvSummaryStart, tvSummaryEnd, tvSummaryPattern, tvDatesPreview;
        TextView btnExpandPackage;
        RecyclerView rvPackageLessons;
        View layoutActions;
        MaterialButton btnReviewChanges, btnAcceptProposal, btnRejectProposal, btnReviewCalendar, btnCancelStudent;
        boolean expanded = false;
        public BookingViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;
            ivPhoto = itemView.findViewById(R.id.ivTutorPhoto);
            if (ivPhoto == null) ivPhoto = itemView.findViewById(R.id.tutor_image);
            tvName = itemView.findViewById(R.id.tvTutorName);
            if (tvName == null) tvName = itemView.findViewById(R.id.tv_tutor_name);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            if (tvSubject == null) tvSubject = itemView.findViewById(R.id.tv_subject);
            tvTime = itemView.findViewById(R.id.tvLessonTime);
            if (tvTime == null) tvTime = itemView.findViewById(R.id.tv_time);
            tvDate = itemView.findViewById(R.id.tvLessonDate);
            if (tvDate == null) tvDate = itemView.findViewById(R.id.tv_date);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            if (tvStatus == null) tvStatus = itemView.findViewById(R.id.tv_status);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            if (btnCancel == null) btnCancel = itemView.findViewById(R.id.btn_cancel);
            btnAction = itemView.findViewById(R.id.btnAction);
            if (btnAction == null) btnAction = itemView.findViewById(R.id.btn_action);
            layoutSuggestion = itemView.findViewById(R.id.layout_suggestion);
            tvSuggestionDetails = itemView.findViewById(R.id.tv_suggestion_details);
            tvSuggestionMsg = itemView.findViewById(R.id.tv_suggestion_msg);
            btnAcceptSuggestion = itemView.findViewById(R.id.btn_accept_suggestion);
            btnRejectSuggestion = itemView.findViewById(R.id.btn_reject_suggestion);
            layoutPackageInfo = itemView.findViewById(R.id.layoutPackageInfo);
            tvPackageBadge = itemView.findViewById(R.id.tvPackageBadge);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            layoutPackageActions = itemView.findViewById(R.id.layout_package_actions);
            btnReviewChanges = itemView.findViewById(R.id.btn_review_changes);
            btnAcceptProposal = itemView.findViewById(R.id.btn_accept_proposal);
            btnRejectProposal = itemView.findViewById(R.id.btn_reject_proposal);
            btnCancelStudent = itemView.findViewById(R.id.btnCancelStudent);
            tvSummaryStart = itemView.findViewById(R.id.tvSummaryStart);
            tvSummaryEnd = itemView.findViewById(R.id.tvSummaryEnd);
            tvSummaryPattern = itemView.findViewById(R.id.tvSummaryPattern);
            tvDatesPreview = itemView.findViewById(R.id.tvDatesPreview);
            btnReviewCalendar = itemView.findViewById(R.id.btnReviewCalendar);
            btnExpandPackage = itemView.findViewById(R.id.btn_expand_package);
            if (btnExpandPackage == null) btnExpandPackage = itemView.findViewById(R.id.btnExpandPackage);
            rvPackageLessons = itemView.findViewById(R.id.rv_package_lessons);
            if (rvPackageLessons == null) rvPackageLessons = itemView.findViewById(R.id.rvPackageLessons);
            layoutActions = itemView.findViewById(R.id.layout_actions);
            if (layoutActions == null) layoutActions = itemView.findViewById(R.id.layoutActions);
            if (viewType == TYPE_SINGLE) {
                btnReschedule = itemView.findViewById(R.id.btn_reschedule);
            }
        }
    }
    static class PackageLessonsAdapter extends RecyclerView.Adapter<PackageLessonsAdapter.VH> {
        private final List<Booking> list;
        private final SimpleDateFormat fmt = new SimpleDateFormat("EEE, MMM dd, HH:mm", Locale.getDefault());
        PackageLessonsAdapter(List<Booking> list) { 
            this.list = list; 
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_package_lesson, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int p) {
            Booking b = list.get(p);
            String s = b.getStatus() != null ? b.getStatus() : "pending";
            long now = System.currentTimeMillis();
            long duration = b.getDuration() > 0 ? b.getDuration() : LessonTimeHelper.DEFAULT_DURATION_MINUTES;
            long endTime = b.getTimestamp() + (duration * 60 * 1000L);
            if (now > endTime && ("confirmed".equals(s) || "pending".equals(s) || "free_trial_pending".equals(s) || "suggestion_pending".equals(s))) {
                if (b.getBookingId() != null) {
                    FirebaseDatabase.getInstance().getReference("Bookings").child(b.getBookingId()).child("status").setValue("cancelled");
                    FirebaseDatabase.getInstance().getReference("Bookings").child(b.getBookingId()).child("cancellationReason").setValue("Time expired");
                    b.setStatus("cancelled");
                    s = "cancelled";
                }
            }
            h.tvNum.setText(String.format(Locale.getDefault(), "#%d", p + 1));
            String subj = b.getSubject();
            String translated = SubjectHelper.getTranslatedSubject(subj);
            h.tvInfo.setText(h.itemView.getContext().getString(R.string.ext_lesson_list_item_format, p + 1, fmt.format(new Date(b.getTimestamp())), translated));
            h.tvStatus.setText(getTranslatedStatus(h.itemView.getContext(), s).toUpperCase(Locale.getDefault()));
            if ("cancelled".equalsIgnoreCase(s)) h.tvStatus.setTextColor(Color.RED);
            else h.tvStatus.setTextColor(Color.parseColor("#008253"));
        }
        @Override public int getItemCount() { return list.size(); }
        static class VH extends RecyclerView.ViewHolder {
            final TextView tvNum;
            final TextView tvInfo;
            final TextView tvStatus;
            VH(View v) {
                super(v);
                tvNum = v.findViewById(R.id.tvLessonNumber);
                tvInfo = v.findViewById(R.id.tvLessonDateTime);
                tvStatus = v.findViewById(R.id.tvLessonItemStatus);
            }
        }
    }
}