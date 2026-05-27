package com.example.gapfix;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class BookingTutorAdapter extends RecyclerView.Adapter<BookingTutorAdapter.BookingViewHolder> {

    private final List<Booking> bookingList;
    private final Context context;
    private boolean isCalendarMode = false;

    public BookingTutorAdapter(List<Booking> bookingList, Context context) {
        this.bookingList = bookingList;
        this.context = context;
        SubjectHelper.loadTranslations(context, this::notifyDataSetChanged);
    }

    public BookingTutorAdapter(List<Booking> bookingList, Context context, boolean isCalendarMode) {
        this.bookingList = bookingList;
        this.context = context;
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

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = isCalendarMode ? R.layout.item_booking_tutor_calendar : R.layout.item_booking_tutor;
        View view = LayoutInflater.from(context).inflate(layoutRes, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);
        boolean isPackage = booking.isPackage() && booking.getPackageId() != null;

        
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
                status = "cancelled";
            }
        }

        
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
        holder.tvTime.setText(timeFormat.format(new Date(booking.getTimestamp())));
        holder.tvDate.setText(dateFormat.format(new Date(booking.getTimestamp())));

        
        String subjectText = SubjectHelper.getTranslatedSubject(booking.getSubject());
        if (isCalendarMode) {
            
            holder.tvSubject.setText(subjectText);
        } else {
            if (isPackage) {
                subjectText += " (" + context.getString(R.string.ext_package) + " · " + booking.getPackageTotalLessons() + " " + context.getString(R.string.ext_lessons) + ")";
            }
            holder.tvSubject.setText(subjectText);
        }

        String statusText = "";
        if ("cancelled".equalsIgnoreCase(status)) {
            String r = booking.getCancellationReason();
            statusText = "[" + getTranslatedStatus(context, status).toUpperCase(Locale.getDefault()) + (r != null && !r.isEmpty() ? ": " + r : "") + "]";
            if (isCalendarMode) {
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error));
            } else {
                holder.tvSubject.setTextColor(ContextCompat.getColor(context, R.color.error));
            }
        } else if ("confirmed".equalsIgnoreCase(status)) {
            statusText = "[" + getTranslatedStatus(context, status).toUpperCase(Locale.getDefault()) + "]";
            if (isCalendarMode) {
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.gapfix_green));
            } else {
                holder.tvSubject.setTextColor(ContextCompat.getColor(context, R.color.gapfix_green));
            }
        } else if ("suggestion_pending".equalsIgnoreCase(status)) {
            statusText = "[" + context.getString(R.string.ext_status_action_required).replace("• ", "").toUpperCase(Locale.getDefault()) + "]";
            if (isCalendarMode) {
                holder.tvStatus.setTextColor(Color.GRAY);
            } else {
                holder.tvSubject.setTextColor(Color.GRAY);
            }
        } else {
            statusText = "[" + getTranslatedStatus(context, status).toUpperCase(Locale.getDefault()) + "]";
            if (isCalendarMode) {
                holder.tvStatus.setTextColor(Color.parseColor("#FFA000"));
            } else {
                holder.tvSubject.setTextColor(Color.parseColor("#FFA000"));
            }
        }
        
        if (isCalendarMode && holder.tvStatus != null) {
            holder.tvStatus.setText(statusText);
        } else if (!isCalendarMode) {
            holder.tvSubject.setText(subjectText + " " + statusText);
        }

        
        holder.layoutLessonActions.setVisibility(View.GONE);
        holder.layoutPackagePendingActions.setVisibility(View.GONE);
        holder.btnJoin.setVisibility(View.GONE);
        holder.btnCancelTutor.setVisibility(View.GONE);
        holder.layoutPackageSummary.setVisibility(View.GONE);
        holder.btnExpandPackage.setVisibility(View.GONE);
        holder.rvPackageLessons.setVisibility(View.GONE);
        if (holder.layoutSummarySchedule != null) holder.layoutSummarySchedule.setVisibility(View.GONE);
        holder.expanded = false;
        holder.btnExpandPackage.setText(R.string.btn_view_all_package);

        
        fetchStudentInfo(booking.getStudentId(), holder.tvStudentName, holder.ivStudentPhoto);

        
        if (isPackage) {
            holder.layoutPackageSummary.setVisibility(View.VISIBLE);
            holder.btnExpandPackage.setVisibility(View.VISIBLE);
            if (holder.layoutSummarySchedule != null) holder.layoutSummarySchedule.setVisibility(View.VISIBLE);

            if (isCalendarMode) {
                if (holder.tvPackageBadge != null) holder.tvPackageBadge.setText(context.getString(R.string.label_package_lessons_format, booking.getPackageTotalLessons()));
                if (holder.tvDuration != null) holder.tvDuration.setText(context.getString(R.string.label_duration_format, (booking.getDuration() > 0 ? booking.getDuration() : 60)));
            }

            loadPackageData(booking, holder);


            holder.btnExpandPackage.setOnClickListener(v -> {
                if (holder.expanded) {
                    holder.expanded = false;
                    holder.rvPackageLessons.setVisibility(View.GONE);
                    holder.btnExpandPackage.setText(R.string.btn_view_all_package);
                } else {
                    holder.expanded = true;
                    holder.btnExpandPackage.setText(R.string.btn_hide_lessons);
                    holder.rvPackageLessons.setVisibility(View.VISIBLE);
                }
            });

            holder.btnReviewCalendar.setOnClickListener(v -> {
                holder.expanded = true;
                holder.btnExpandPackage.setText(R.string.btn_hide_lessons);
                holder.rvPackageLessons.setVisibility(View.VISIBLE);
            });

            if ("pending".equalsIgnoreCase(status) || "free_trial_pending".equalsIgnoreCase(status)) {
                holder.layoutPackagePendingActions.setVisibility(View.VISIBLE);
                holder.btnConfirmSchedule.setOnClickListener(v -> updatePackageStatus(booking, "confirmed"));
                holder.btnSuggestOrReject.setOnClickListener(v -> showSuggestRejectSheet(booking));
            } else if ("confirmed".equalsIgnoreCase(status)) {
                holder.btnCancelTutor.setVisibility(View.VISIBLE);
                holder.btnCancelTutor.setText(R.string.btn_cancel_change);
                holder.btnCancelTutor.setOnClickListener(v -> showSuggestRejectSheet(booking));
            }

        } else {
            
            if ("pending".equalsIgnoreCase(status) || "free_trial_pending".equalsIgnoreCase(status)) {
                holder.layoutLessonActions.setVisibility(View.VISIBLE);
                holder.btnAccept.setOnClickListener(v -> updateSingleStatus(booking, "confirmed"));
                holder.btnReject.setOnClickListener(v -> showSimpleCancelDialog(booking, context.getString(R.string.btn_decline_lesson)));

                if (holder.layoutLessonActions.findViewWithTag("btn_suggest") == null) {
                    MaterialButton btnSuggest = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                    btnSuggest.setTag("btn_suggest");
                    btnSuggest.setText(R.string.btn_suggest_change);
                    btnSuggest.setTextSize(10);
                    btnSuggest.setAllCaps(false);
                    btnSuggest.setOnClickListener(v -> showSuggestRejectSheet(booking));
                    holder.layoutLessonActions.addView(btnSuggest);
                }

            } else if ("suggestion_pending".equalsIgnoreCase(status)) {
                holder.layoutLessonActions.setVisibility(View.GONE);
                holder.btnJoin.setVisibility(View.GONE);
                holder.btnCancelTutor.setVisibility(View.VISIBLE);
                holder.btnCancelTutor.setText(R.string.btn_cancel_change);
                holder.btnCancelTutor.setOnClickListener(v -> showSuggestRejectSheet(booking));

            } else if ("confirmed".equalsIgnoreCase(status)) {
                holder.btnJoin.setVisibility(View.VISIBLE);
                holder.btnCancelTutor.setVisibility(View.VISIBLE);
                holder.btnCancelTutor.setText(R.string.btn_cancel_change);
                holder.btnCancelTutor.setOnClickListener(v -> showSuggestRejectSheet(booking));

                LessonAlarmScheduler.schedule(context, booking.getBookingId(),
                        booking.getTimestamp(), booking.getSubject(), "tutor");

                holder.btnJoin.removeCallbacks(holder.updateRunnable);
                holder.updateRunnable = new Runnable() {
                    @Override public void run() {
                        boolean joinable = LessonTimeHelper.isJoinable(booking, "tutor");
                        if (joinable) {
                            holder.btnJoin.setEnabled(true);
                            holder.btnJoin.setText(R.string.btn_join_class_caps);
                            holder.btnJoin.setBackgroundColor(Color.parseColor("#4CAF50"));
                            holder.btnJoin.setOnClickListener(v -> {
                                Intent i = new Intent(context, VideoCallActivity.class);
                                i.putExtra("BOOKING_ID", booking.getBookingId());
                                i.putExtra("IS_INCOMING", false);
                                context.startActivity(i);
                            });
                        } else {
                            long mins = LessonTimeHelper.minutesUntilJoinable(booking, "tutor");
                            holder.btnJoin.setEnabled(false);
                            holder.btnJoin.setBackgroundColor(Color.GRAY);
                            if (mins > 60) holder.btnJoin.setText(context.getString(R.string.time_in_h_m, (mins/60), (mins%60)));
                            else if (mins > 0) holder.btnJoin.setText(context.getString(R.string.time_in_m, mins));
                            else holder.btnJoin.setText(System.currentTimeMillis() > booking.getTimestamp() ? context.getString(R.string.label_expired) : context.getString(R.string.label_waiting));
                            holder.btnJoin.postDelayed(this, 30_000);
                        }
                    }
                };
                holder.btnJoin.post(holder.updateRunnable);
            }
        }
    }

    private void loadPackageData(Booking representative, BookingViewHolder holder) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        ref.orderByChild("packageId").equalTo(representative.getPackageId())
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
                        lessons.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                        if (lessons.isEmpty()) return;

                        
                        int lessonNum = 1;
                        for (int i = 0; i < lessons.size(); i++) {
                            if (lessons.get(i).getBookingId().equals(representative.getBookingId())) {
                                lessonNum = i + 1;
                                break;
                            }
                        }
                        if (isCalendarMode) {
                            holder.tvSubject.setText(context.getString(R.string.ext_lesson_number_format, SubjectHelper.getTranslatedSubject(representative.getSubject()), lessonNum));
                        }

                        SimpleDateFormat shortDate = new SimpleDateFormat("MMM dd", Locale.getDefault());
                        SimpleDateFormat fullDate  = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                        SimpleDateFormat dayName   = new SimpleDateFormat("EEE", Locale.getDefault());

                        int total = lessons.size();
                        long startTs = lessons.get(0).getTimestamp();
                        long endTs   = lessons.get(total - 1).getTimestamp();

                        Set<String> daysSet = new TreeSet<>();
                        for (Booking b : lessons) daysSet.add(dayName.format(new Date(b.getTimestamp())));
                        String pattern = String.join("/", daysSet);

                        if (holder.tvSummaryStart != null) holder.tvSummaryStart.setText("📅 " + context.getString(R.string.ext_start) + ": " + fullDate.format(new Date(startTs)) + ".");
                        if (holder.tvSummaryEnd != null) holder.tvSummaryEnd.setText("✅ " + context.getString(R.string.ext_end) + ": " + fullDate.format(new Date(endTs)) + ".");
                        String label = "confirmed".equalsIgnoreCase(representative.getStatus()) ? context.getString(R.string.ext_lesson_pattern_label) : context.getString(R.string.ext_proposed_pattern_label);
                        if (holder.tvSummarySchedule != null) holder.tvSummarySchedule.setText(label + pattern + context.getString(R.string.ext_view_list_suffix));

                        StringBuilder preview = new StringBuilder();
                        int previewCount = Math.min(4, total);
                        for (int i = 0; i < previewCount; i++) {
                            preview.append("• ").append(shortDate.format(new Date(lessons.get(i).getTimestamp())));
                            if (i < previewCount - 1) preview.append("\n");
                        }
                        int remaining = total - previewCount;
                        if (remaining > 0) preview.append("\n+ ").append(remaining).append(" more");
                        if (holder.tvDatesPreview != null) holder.tvDatesPreview.setText(preview.toString());

                        PackageLessonsAdapter adapter = new PackageLessonsAdapter(lessons);
                        holder.rvPackageLessons.setLayoutManager(new LinearLayoutManager(context));
                        holder.rvPackageLessons.setAdapter(adapter);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        }
                });
    }

    private void showSuggestRejectSheet(Booking booking) {
        BottomSheetDialog sheet = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
        View v = LayoutInflater.from(context).inflate(R.layout.layout_suggest_reject_sheet, null);
        sheet.setContentView(v);

        TextView tvName = v.findViewById(R.id.tvSheetStudentName);
        TextView tvPkgInfo = v.findViewById(R.id.tvSheetPackageInfo);
        TextView tvTotal = v.findViewById(R.id.tvSheetSummaryTotal);
        TextView tvStart = v.findViewById(R.id.tvSheetSummaryStart);
        TextView tvSchedule = v.findViewById(R.id.tvSheetSummarySchedule);
        TextView tvEnd = v.findViewById(R.id.tvSheetSummaryEnd);
        ImageView ivPhoto = v.findViewById(R.id.ivSheetStudentPhoto);

        fetchStudentInfo(booking.getStudentId(), tvName, ivPhoto);

        
        boolean isSingleLesson = !booking.isPackage() || booking.getPackageId() == null;
        if (isSingleLesson) {
            tvPkgInfo.setText(SubjectHelper.getTranslatedSubject(booking.getSubject()) + context.getString(R.string.ext_single_lesson_suffix));

            v.findViewById(R.id.rgChangeScope).setVisibility(View.GONE);
            v.findViewById(R.id.layoutChangeOptions).setVisibility(View.GONE);
            v.findViewById(R.id.layoutChangeSingleLesson).setVisibility(View.VISIBLE);

            if (tvTotal != null) tvTotal.setVisibility(View.GONE);
            if (tvStart != null) tvStart.setVisibility(View.GONE);
            if (tvSchedule != null) tvSchedule.setVisibility(View.GONE);
            if (tvEnd != null) tvEnd.setVisibility(View.GONE);

            MaterialButton btnDestDate = v.findViewById(R.id.btnDestDateSingle);
            MaterialButton btnDestTimeSingle = v.findViewById(R.id.btnDestTimeSingle);

            final long[] selectedDate = {0};
            final int[] selectedHour = {-1};
            final int[] selectedMinute = {-1};

            btnDestDate.setOnClickListener(b -> {
                com.google.android.material.datepicker.MaterialDatePicker<Long> dp =
                        com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                                .setTitleText(R.string.ext_select_date)
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
                dp.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "TUTOR_DATE_PICK");
            });

            btnDestTimeSingle.setOnClickListener(b -> {
                com.google.android.material.timepicker.MaterialTimePicker tp =
                        new com.google.android.material.timepicker.MaterialTimePicker.Builder()
                                .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
                                .setHour(14).setMinute(0)
                                .setTitleText(R.string.ext_select_time)
                                .build();
                tp.addOnPositiveButtonClickListener(v2 -> {
                    selectedHour[0] = tp.getHour();
                    selectedMinute[0] = tp.getMinute();
                    btnDestTimeSingle.setText(String.format(Locale.getDefault(), "%02d:%02d", tp.getHour(), tp.getMinute()));
                });
                tp.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "TUTOR_TIME_PICK");
            });

            TextInputEditText etMessage = v.findViewById(R.id.etSheetMessage);
            v.findViewById(R.id.btnSheetSubmitCounter).setOnClickListener(btn -> {
                if (selectedDate[0] == 0 || selectedHour[0] == -1) {
                    Toast.makeText(context, context.getString(R.string.ext_select_date) + " & " + context.getString(R.string.ext_select_time), Toast.LENGTH_SHORT).show();
                    return;
                }
                String msg = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";

                java.util.Calendar utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
                utcCal.setTimeInMillis(selectedDate[0]);
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(utcCal.get(java.util.Calendar.YEAR), utcCal.get(java.util.Calendar.MONTH),
                        utcCal.get(java.util.Calendar.DAY_OF_MONTH), selectedHour[0], selectedMinute[0], 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                long newTimestamp = cal.getTimeInMillis();

                DatabaseReference dr = FirebaseDatabase.getInstance().getReference("Bookings").child(booking.getBookingId());
                dr.child("suggestedTimestamp").setValue(newTimestamp);
                dr.child("suggestionMessage").setValue(msg);
                dr.child("status").setValue("suggestion_pending");

                String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour[0], selectedMinute[0]);
                sendSuggestionNotification(booking.getStudentId(), booking.getSubject(),
                        context.getString(R.string.notif_tutor_suggested_time, formattedTime));
                Toast.makeText(context, R.string.msg_suggestion_submitted, Toast.LENGTH_SHORT).show();
                sheet.dismiss();
            });

            v.findViewById(R.id.btnSheetRejectProposal).setOnClickListener(btn -> {
                String msg = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
                if (msg.isEmpty()) {
                    Toast.makeText(context, R.string.msg_reason_required, Toast.LENGTH_SHORT).show();
                    return;
                }
                performCancellation(booking, msg);
                Toast.makeText(context, R.string.msg_proposal_rejected, Toast.LENGTH_SHORT).show();
                sheet.dismiss();
            });

            v.findViewById(R.id.btnSheetCancelAction).setOnClickListener(btn -> sheet.dismiss());
            sheet.show();
            return;
        }

        
        tvPkgInfo.setText(SubjectHelper.getTranslatedSubject(booking.getSubject()) + " (" + context.getString(R.string.ext_package) + " - " + booking.getPackageTotalLessons() + " " + context.getString(R.string.ext_lessons) + ")");

        List<Booking> cachedLessons = new ArrayList<>();
        List<String> lessonStrings = new ArrayList<>();
        String[] defaultDays = DayTranslationHelper.getTranslatedDaysArray(context);
        List<String> activeDaysList = new ArrayList<>();

        android.widget.RadioGroup rgScope = v.findViewById(R.id.rgChangeScope);
        TextView tvSourceLabel = v.findViewById(R.id.tvSourcePatternLabel);

        View layoutChangeOptions = v.findViewById(R.id.layoutChangeOptions);
        View layoutChangeSingleLesson = v.findViewById(R.id.layoutChangeSingleLesson);
        MaterialButton btnDestDateSingle = v.findViewById(R.id.btnDestDateSingle);
        MaterialButton btnDestTimeSingle = v.findViewById(R.id.btnDestTimeSingle);
        android.widget.Spinner spinLessons = v.findViewById(R.id.spinnerLessonsInPackage);

        final long[] selectedDate = {0};
        final int[] selectedHour = {-1};
        final int[] selectedMinute = {-1};

        btnDestDateSingle.setOnClickListener(b -> {
            com.google.android.material.datepicker.MaterialDatePicker<Long> dp =
                    com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                            .setTitleText(R.string.ext_select_date)
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
            dp.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "TUTOR_PKG_DATE_PICK");
        });

        btnDestTimeSingle.setOnClickListener(b -> {
            com.google.android.material.timepicker.MaterialTimePicker tp =
                    new com.google.android.material.timepicker.MaterialTimePicker.Builder()
                            .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
                            .setHour(14).setMinute(0)
                            .setTitleText(R.string.ext_select_time)
                            .build();
            tp.addOnPositiveButtonClickListener(v2 -> {
                selectedHour[0] = tp.getHour();
                selectedMinute[0] = tp.getMinute();
                btnDestTimeSingle.setText(String.format(Locale.getDefault(), "%02d:%02d", tp.getHour(), tp.getMinute()));
            });
            tp.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "TUTOR_PKG_TIME_PICK");
        });

        android.widget.ArrayAdapter<String> adapterLessons = new android.widget.ArrayAdapter<>(context, android.R.layout.simple_spinner_item, lessonStrings);
        adapterLessons.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinLessons.setAdapter(adapterLessons);

        android.widget.Spinner spinSource = v.findViewById(R.id.spinnerSourceDay);
        android.widget.Spinner spinDest = v.findViewById(R.id.spinnerDestDay);
        MaterialButton btnDestTime = v.findViewById(R.id.btnDestTime);

        android.widget.ArrayAdapter<String> adapterSource = new android.widget.ArrayAdapter<>(context, android.R.layout.simple_spinner_item, activeDaysList);
        adapterSource.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinSource.setAdapter(adapterSource);

        android.widget.ArrayAdapter<String> adapterDest = new android.widget.ArrayAdapter<>(context, android.R.layout.simple_spinner_item, defaultDays);
        adapterDest.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinDest.setAdapter(adapterDest);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        ref.orderByChild("packageId").equalTo(booking.getPackageId())
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
                        if (lessons.isEmpty()) return;
                        lessons.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

                        SimpleDateFormat fullDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                        SimpleDateFormat dayName = new SimpleDateFormat("EEE", Locale.getDefault());
                        SimpleDateFormat timeOnly = new SimpleDateFormat("HH:mm", Locale.getDefault());

                        int total = lessons.size();
                        long startTs = lessons.get(0).getTimestamp();
                        long endTs = lessons.get(total - 1).getTimestamp();

                        Set<String> daysSet = new TreeSet<>();
                        String sharedTime = timeOnly.format(new Date(startTs));
                        SimpleDateFormat fullDayName = new SimpleDateFormat("EEEE", Locale.US);
                        Set<String> uniqueDays = new TreeSet<>();
                        List<String> rawActive = new ArrayList<>();

                        for (Booking b : lessons) {
                            daysSet.add(dayName.format(new Date(b.getTimestamp())));
                            String engDay = fullDayName.format(new Date(b.getTimestamp())) + "s";
                            if (!rawActive.contains(engDay)) {
                                rawActive.add(engDay);
                                uniqueDays.add(DayTranslationHelper.translateDay(context, engDay));
                            }
                        }
                        String schedule = String.join("/", daysSet) + " @ " + sharedTime;

                        activeDaysList.clear();
                        activeDaysList.addAll(uniqueDays);

                        tvTotal.setText("• " + total + " " + context.getString(R.string.ext_lessons));
                        tvStart.setText("• " + context.getString(R.string.ext_start) + ": " + fullDate.format(new Date(startTs)));
                        tvSchedule.setText("• " + context.getString(R.string.ext_select_schedule) + ": " + schedule);
                        tvEnd.setText("• " + context.getString(R.string.ext_end) + ": " + fullDate.format(new Date(endTs)));

                        cachedLessons.clear();
                        lessonStrings.clear();
                        SimpleDateFormat lessonFormat = new SimpleDateFormat("MMM dd, yyyy @ HH:mm", Locale.getDefault());
                        for (Booking b : lessons) {
                            cachedLessons.add(b);
                            lessonStrings.add(context.getString(R.string.ext_lesson_item_prefix) + lessonFormat.format(new Date(b.getTimestamp())));
                        }

                        adapterSource.notifyDataSetChanged();
                        adapterLessons.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });

        rgScope.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbScopeSingle) {
                if (tvSourceLabel != null) tvSourceLabel.setText(R.string.label_select_lesson_reschedule);
                if (layoutChangeOptions != null) layoutChangeOptions.setVisibility(View.GONE);
                if (layoutChangeSingleLesson != null) layoutChangeSingleLesson.setVisibility(View.VISIBLE);
            } else {
                if (tvSourceLabel != null) tvSourceLabel.setText(R.string.label_shift_pattern_day);
                if (layoutChangeOptions != null) layoutChangeOptions.setVisibility(View.VISIBLE);
                if (layoutChangeSingleLesson != null) layoutChangeSingleLesson.setVisibility(View.GONE);
            }
        });

        btnDestTime.setOnClickListener(b -> {
            com.google.android.material.timepicker.MaterialTimePicker tp = new com.google.android.material.timepicker.MaterialTimePicker.Builder()
                    .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
                    .setHour(14).setMinute(0).setTitleText(R.string.label_select_alt_time).build();
            tp.addOnPositiveButtonClickListener(v2 -> btnDestTime.setText(String.format(Locale.getDefault(), "%02d:%02d", tp.getHour(), tp.getMinute())));
            tp.show(((androidx.fragment.app.FragmentActivity)context).getSupportFragmentManager(), "COUNTER_TIME");
        });

        TextInputEditText etMessage = v.findViewById(R.id.etSheetMessage);
        v.findViewById(R.id.btnSheetSubmitCounter).setOnClickListener(btn -> {
            String msg = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
            
            DatabaseReference pkgRef = FirebaseDatabase.getInstance().getReference("Bookings");
            if (booking.isPackage() && booking.getPackageId() != null) {
                if (rgScope.getCheckedRadioButtonId() == R.id.rbScopeSingle) {
                    int selectedPos = spinLessons.getSelectedItemPosition();
                    if (selectedPos >= 0 && selectedPos < cachedLessons.size()) {
                        if (selectedDate[0] == 0 || selectedHour[0] == -1) {
                            Toast.makeText(context, context.getString(R.string.ext_select_date) + " & " + context.getString(R.string.ext_select_time), Toast.LENGTH_SHORT).show();
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
                        DatabaseReference dr = pkgRef.child(targetLesson.getBookingId());
                        dr.child("suggestedTimestamp").setValue(newTimestamp);
                        dr.child("suggestedSourceDay").setValue(context.getString(R.string.label_this_lesson));
                        dr.child("suggestionMessage").setValue(msg);
                        dr.child("status").setValue("suggestion_pending");

                        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour[0], selectedMinute[0]);
                        sendSuggestionNotification(booking.getStudentId(), booking.getSubject(), context.getString(R.string.notif_tutor_suggested_time_lesson, formattedTime));
                        Toast.makeText(context, R.string.msg_suggestion_submitted, Toast.LENGTH_SHORT).show();
                        sheet.dismiss();
                    }
                } else {
                    String sourceStr = spinSource.getSelectedItem().toString();
                    String destStr = spinDest.getSelectedItem().toString();
                    String source = DayTranslationHelper.getEnglishDayFromTranslated(context, sourceStr);
                    String dest = DayTranslationHelper.getEnglishDayFromTranslated(context, destStr);
                    String time = btnDestTime.getText().toString();

                    pkgRef.orderByChild("packageId").equalTo(booking.getPackageId())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot ds : snapshot.getChildren()) {
                                        DatabaseReference dr = ds.getRef();
                                        dr.child("suggestedSourceDay").setValue(source);
                                        dr.child("suggestedDestDay").setValue(dest);
                                        dr.child("suggestedTime").setValue(time);
                                        dr.child("suggestedTimestamp").removeValue();
                                        dr.child("suggestionMessage").setValue(msg);
                                        dr.child("status").setValue("suggestion_pending");
                                    }
                                    sendSuggestionNotification(booking.getStudentId(), booking.getSubject(), context.getString(R.string.notif_tutor_suggested_schedule));
                                    Toast.makeText(context, R.string.msg_counter_offer_submitted, Toast.LENGTH_SHORT).show();
                                    sheet.dismiss();
                                }
                                @Override public void onCancelled(@NonNull DatabaseError e) {}
                            });
                }
            }
        });

        v.findViewById(R.id.btnSheetRejectProposal).setOnClickListener(btn -> {
            String msg = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
            if (msg.isEmpty()) {
                Toast.makeText(context, R.string.msg_reason_required, Toast.LENGTH_SHORT).show();
                return;
            }
            performCancellation(booking, msg);
            Toast.makeText(context, R.string.msg_proposal_rejected, Toast.LENGTH_SHORT).show();
            sheet.dismiss();
        });

        v.findViewById(R.id.btnSheetCancelAction).setOnClickListener(btn -> sheet.dismiss());
        sheet.show();
    }

    private void showSimpleCancelDialog(Booking booking, String title) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(R.string.prompt_provide_reason);
        final android.widget.EditText input = new android.widget.EditText(context);
        input.setHint(R.string.hint_enter_reason);
        builder.setView(input);
        builder.setPositiveButton(R.string.ext_accept, (d, w) -> {
            String reason = input.getText().toString().trim();
            if (reason.isEmpty()) Toast.makeText(context, R.string.msg_reason_required, Toast.LENGTH_SHORT).show();
            else performCancellation(booking, reason);
        });
        builder.setNegativeButton(R.string.back, (d, w) -> d.cancel());
        builder.show();
    }

    private void updatePackageStatus(Booking booking, String newStatus) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        ref.orderByChild("packageId").equalTo(booking.getPackageId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Booking b = ds.getValue(Booking.class);
                            if (b == null) continue;
                            DatabaseReference dr = ds.getRef();

                            if ("confirmed".equals(newStatus)) {
                                if (context.getString(R.string.label_this_lesson).equals(b.getSuggestedSourceDay()) && b.getSuggestedTimestamp() > 0) {
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
                                } else if (b.getSuggestedSourceDay() != null && b.getSuggestedDestDay() != null && b.getSuggestedTime() != null) {
                                    applyShift(dr, b, b.getSuggestedSourceDay(), b.getSuggestedDestDay(), b.getSuggestedTime());
                                } else {
                                    dr.child("status").setValue(newStatus);
                                }
                            } else {
                                dr.child("status").setValue(newStatus);
                            }
                        }
                        Toast.makeText(context, R.string.msg_package_confirmed, Toast.LENGTH_SHORT).show();
                        if ("confirmed".equals(newStatus)) {
                            sendNotification(booking.getStudentId(), context.getString(R.string.msg_package_accepted_title),
                                    context.getString(R.string.msg_package_accepted_body, booking.getSubject()));
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void applyShift(DatabaseReference dr, Booking b, String source, String dest, String time) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(b.getTimestamp());

        int sourceDay = getDayOfWeek(source);
        int destDay = getDayOfWeek(dest);
        int diff = destDay - sourceDay;
        cal.add(java.util.Calendar.DATE, diff);

        try {
            String[] parts = time.split(":");
            cal.set(java.util.Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
            cal.set(java.util.Calendar.MINUTE, Integer.parseInt(parts[1]));
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
        } catch (Exception e) { e.printStackTrace(); }

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
        switch (dayStr.toLowerCase()) {
            case "mondays": return java.util.Calendar.MONDAY;
            case "tuesdays": return java.util.Calendar.TUESDAY;
            case "wednesdays": return java.util.Calendar.WEDNESDAY;
            case "thursdays": return java.util.Calendar.THURSDAY;
            case "fridays": return java.util.Calendar.FRIDAY;
            case "saturdays": return java.util.Calendar.SATURDAY;
            case "sundays": return java.util.Calendar.SUNDAY;
            default: return java.util.Calendar.MONDAY;
        }
    }

    private void updateSingleStatus(Booking booking, String newStatus) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings")
                .child(booking.getBookingId());

        if ("confirmed".equals(newStatus) && (booking.getSuggestedTimestamp() > 0 || booking.getSuggestedTime() != null)) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "confirmed");

            if (booking.getSuggestedTimestamp() > 0) {
                Date newDate = new Date(booking.getSuggestedTimestamp());
                updates.put("timestamp", booking.getSuggestedTimestamp());
                updates.put("lessonTime", timeFormat.format(newDate));
                updates.put("lessonDate", dateFormat.format(newDate));
            }

            updates.put("suggestedTimestamp", null);
            updates.put("suggestedSourceDay", null);
            updates.put("suggestedDestDay", null);
            updates.put("suggestedTime", null);
            updates.put("suggestionMessage", null);
            ref.updateChildren(updates)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(context, R.string.msg_lesson_confirmed_new, Toast.LENGTH_SHORT).show();
                        sendNotification(booking.getStudentId(), context.getString(R.string.msg_lesson_accepted_title),
                                context.getString(R.string.msg_lesson_accepted_new_time_body, booking.getSubject()));
                    });
        } else {
            ref.child("status").setValue(newStatus)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(context, context.getString(R.string.msg_lesson_confirmed_new), Toast.LENGTH_SHORT).show();
                        if ("confirmed".equals(newStatus)) {
                            sendNotification(booking.getStudentId(), context.getString(R.string.msg_lesson_accepted_title),
                                    context.getString(R.string.msg_lesson_accepted_body, booking.getSubject()));
                        }
                    });
        }
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
                            sendNotification(booking.getStudentId(), context.getString(R.string.msg_package_declined_title),
                                    context.getString(R.string.msg_package_declined_body, booking.getSubject(), reason));
                        }
                        @Override public void onCancelled(@NonNull DatabaseError e) {}
                    });
        } else {
            ref.child(booking.getBookingId()).child("status").setValue("cancelled");
            ref.child(booking.getBookingId()).child("cancellationReason").setValue(reason)
                    .addOnSuccessListener(a -> sendNotification(booking.getStudentId(), context.getString(R.string.msg_lesson_cancelled_title),
                            context.getString(R.string.msg_lesson_cancelled_body, booking.getSubject(), reason)));
        }
    }

    private void sendSuggestionNotification(String studentId, String subject, String message) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("Notifications").child(studentId).push();
        Map<String, Object> data = new HashMap<>();
        data.put("title", context.getString(R.string.notif_suggestion_title, subject));
        data.put("message", message);
        data.put("timestamp", System.currentTimeMillis());
        notifRef.setValue(data);
    }

    private void sendNotification(String uid, String title, String message) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("Notifications").child(uid).push();
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("message", message);
        data.put("timestamp", System.currentTimeMillis());
        notifRef.setValue(data);
    }

    private void fetchStudentInfo(String studentId, TextView tvName, ImageView ivPhoto) {
        FirebaseDatabase.getInstance().getReference("Users").child("Student").child(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        if (snap.exists()) {
                            tvName.setText(snap.child("name").getValue(String.class));
                            String img = snap.child("imageResourceLink").getValue(String.class);
                            if (img == null) img = snap.child("profilePicture").getValue(String.class);

                            if (img != null && !img.isEmpty()) {
                                Glide.with(context).load(img).placeholder(R.drawable.person_circle).circleCrop().into(ivPhoto);
                            } else {
                                ivPhoto.setImageResource(R.drawable.person_circle);
                            }
                        } else {
                            tvName.setText(context.getString(R.string.label_unknown_student));
                            ivPhoto.setImageResource(R.drawable.person_circle);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    @Override public int getItemCount() { return bookingList.size(); }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvSubject, tvTime, tvDate, tvStatus;
        TextView tvSummaryStart, tvSummarySchedule, tvSummaryEnd, tvDatesPreview;
        TextView tvPackageBadge, tvDuration;
        ImageView ivStudentPhoto;
        LinearLayout layoutLessonActions, layoutPackageSummary, layoutPackagePendingActions, layoutSummarySchedule;
        MaterialButton btnAccept, btnReject;
        MaterialButton btnConfirmSchedule, btnSuggestOrReject;
        MaterialButton btnReviewCalendar;
        TextView btnExpandPackage;
        MaterialButton btnJoin, btnCancelTutor;
        RecyclerView rvPackageLessons;
        Runnable updateRunnable;
        boolean expanded = false;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName             = itemView.findViewById(R.id.tvStudentName);
            tvSubject                 = itemView.findViewById(R.id.tvLessonSubject);
            tvStatus                  = itemView.findViewById(R.id.tvLessonStatus);
            tvTime                    = itemView.findViewById(R.id.tvLessonTime);
            tvDate                    = itemView.findViewById(R.id.tvLessonDate);
            tvSummaryStart            = itemView.findViewById(R.id.tvSummaryStart);
            tvSummarySchedule         = itemView.findViewById(R.id.tvSummarySchedule);
            tvSummaryEnd              = itemView.findViewById(R.id.tvSummaryEnd);
            tvDatesPreview            = itemView.findViewById(R.id.tvDatesPreview);
            tvPackageBadge            = itemView.findViewById(R.id.tvPackageBadge);
            tvDuration                = itemView.findViewById(R.id.tvDuration);
            ivStudentPhoto            = itemView.findViewById(R.id.ivStudentPhoto);
            layoutPackageSummary      = itemView.findViewById(R.id.layoutPackageSummary);
            layoutLessonActions       = itemView.findViewById(R.id.layoutLessonActions);
            layoutPackagePendingActions = itemView.findViewById(R.id.layoutPackagePendingActions);
            layoutSummarySchedule     = itemView.findViewById(R.id.layoutSummarySchedule);
            btnAccept                 = itemView.findViewById(R.id.btnAccept);
            btnReject                 = itemView.findViewById(R.id.btnReject);
            btnConfirmSchedule        = itemView.findViewById(R.id.btnConfirmSchedule);
            btnSuggestOrReject        = itemView.findViewById(R.id.btnSuggestOrReject);
            btnExpandPackage          = itemView.findViewById(R.id.btnExpandPackage);
            btnReviewCalendar         = itemView.findViewById(R.id.btnReviewCalendar);
            btnJoin                   = itemView.findViewById(R.id.btnJoin);
            btnCancelTutor            = itemView.findViewById(R.id.btnCancelTutor);
            rvPackageLessons          = itemView.findViewById(R.id.rvPackageLessons);
        }
    }

    static class PackageLessonsAdapter extends RecyclerView.Adapter<PackageLessonsAdapter.VH> {
        private final List<Booking> lessons;
        private final SimpleDateFormat fmt =
                new SimpleDateFormat("EEE, MMM dd · HH:mm", Locale.getDefault());

        PackageLessonsAdapter(List<Booking> lessons) { 
            this.lessons = lessons; 
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_package_lesson, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Booking b = lessons.get(pos);
            String s = b.getStatus() != null ? b.getStatus() : "pending";

            long now = System.currentTimeMillis();
            long duration = b.getDuration() > 0 ? b.getDuration() : LessonTimeHelper.DEFAULT_DURATION_MINUTES;
            long endTime = b.getTimestamp() + (duration * 60 * 1000L);
            if (now > endTime && ("confirmed".equals(s) || "pending".equals(s) || "suggestion_pending".equals(s))) {
                if (b.getBookingId() != null) {
                    FirebaseDatabase.getInstance().getReference("Bookings").child(b.getBookingId()).child("status").setValue("cancelled");
                    FirebaseDatabase.getInstance().getReference("Bookings").child(b.getBookingId()).child("cancellationReason").setValue("Time expired");
                    b.setStatus("cancelled");
                    s = "cancelled";
                }
            }

            h.tvNumber.setText("#" + (pos + 1));
            
            h.tvDateTime.setText(fmt.format(new Date(b.getTimestamp())));
            h.tvStatus.setText(getTranslatedStatus(h.itemView.getContext(), s).toUpperCase(Locale.getDefault()));
            switch (s.toLowerCase()) {
                case "confirmed": h.tvStatus.setTextColor(Color.parseColor("#4CAF50")); break;
                case "cancelled": h.tvStatus.setTextColor(Color.RED); break;
                case "finished":  h.tvStatus.setTextColor(Color.BLUE); break;
                default:          h.tvStatus.setTextColor(Color.parseColor("#FFA000")); break;
            }
        }

        @Override public int getItemCount() { return lessons.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvNumber, tvDateTime, tvStatus;
            VH(@NonNull View v) {
                super(v);
                tvNumber   = v.findViewById(R.id.tvLessonNumber);
                tvDateTime = v.findViewById(R.id.tvLessonDateTime);
                tvStatus   = v.findViewById(R.id.tvLessonItemStatus);
            }
        }
    }
}