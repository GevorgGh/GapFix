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

    private List<Booking> bookingList;
    private Context context;

    public BookingTutorAdapter(List<Booking> bookingList, Context context) {
        this.bookingList = bookingList;
        this.context = context;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking_tutor, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);
        boolean isPackage = booking.isPackage() && booking.getPackageId() != null;

        // ── Date + Time ────────────────────────────────────────────────
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
        holder.tvTime.setText(timeFormat.format(new Date(booking.getTimestamp())));
        holder.tvDate.setText(dateFormat.format(new Date(booking.getTimestamp())));

        // ── Construct Subject + Status ───────────────────────────────────
        String status = booking.getStatus() != null ? booking.getStatus() : "pending";
        String subjectText = booking.getSubject();
        if (isPackage) {
            subjectText += " (Package · " + booking.getPackageTotalLessons() + " Lessons)";
        }
        
        if ("cancelled".equalsIgnoreCase(status)) {
            String r = booking.getCancellationReason();
            subjectText += " [CANCELLED" + (r != null && !r.isEmpty() ? ": " + r : "") + "]";
            holder.tvSubject.setTextColor(ContextCompat.getColor(context, R.color.error));
        } else if ("confirmed".equalsIgnoreCase(status)) {
            subjectText += " [CONFIRMED]";
            holder.tvSubject.setTextColor(ContextCompat.getColor(context, R.color.gapfix_green));
        } else if ("suggestion_pending".equalsIgnoreCase(status)) {
            subjectText += " [WAITING FOR STUDENT]";
            holder.tvSubject.setTextColor(Color.GRAY);
        } else {
            subjectText += " [" + status.toUpperCase() + "]";
            holder.tvSubject.setTextColor(Color.parseColor("#FFA000"));
        }
        holder.tvSubject.setText(subjectText);

        // ── Reset all panels ───────────────────────────────────────────
        holder.layoutLessonActions.setVisibility(View.GONE);
        holder.layoutPackagePendingActions.setVisibility(View.GONE);
        holder.btnJoin.setVisibility(View.GONE);
        holder.btnCancelTutor.setVisibility(View.GONE);
        holder.layoutPackageSummary.setVisibility(View.GONE);
        holder.btnExpandPackage.setVisibility(View.GONE);
        holder.rvPackageLessons.setVisibility(View.GONE);
        holder.expanded = false;
        holder.btnExpandPackage.setText("▼  View all lessons in package");

        // ── Student name ───────────────────────────────────────────────
        fetchStudentName(booking.getStudentId(), holder.tvStudentName);

        // ── Package-specific UI ────────────────────────────────────────
        if (isPackage) {
            holder.layoutPackageSummary.setVisibility(View.VISIBLE);
            holder.btnExpandPackage.setVisibility(View.VISIBLE);

            // Auto-load package lessons to build the summary
            loadPackageData(booking, holder);

            // Expand toggle
            holder.btnExpandPackage.setOnClickListener(v -> {
                if (holder.expanded) {
                    holder.expanded = false;
                    holder.rvPackageLessons.setVisibility(View.GONE);
                    holder.btnExpandPackage.setText("▼  View all lessons in package");
                } else {
                    holder.expanded = true;
                    holder.btnExpandPackage.setText("▲  Hide lessons");
                    holder.rvPackageLessons.setVisibility(View.VISIBLE);
                }
            });

            // Review Calendar = same as expand
            holder.btnReviewCalendar.setOnClickListener(v -> {
                holder.expanded = true;
                holder.btnExpandPackage.setText("▲  Hide lessons");
                holder.rvPackageLessons.setVisibility(View.VISIBLE);
            });

            // Package pending actions
            if ("pending".equalsIgnoreCase(status) || "free_trial_pending".equalsIgnoreCase(status)) {
                holder.layoutPackagePendingActions.setVisibility(View.VISIBLE);
                holder.btnConfirmSchedule.setOnClickListener(v -> updatePackageStatus(booking, "confirmed"));
                holder.btnSuggestOrReject.setOnClickListener(v -> showSuggestRejectSheet(booking));
            } else if ("confirmed".equalsIgnoreCase(status)) {
                holder.btnCancelTutor.setVisibility(View.VISIBLE);
                holder.btnCancelTutor.setText("Cancel / Change");
                holder.btnCancelTutor.setOnClickListener(v -> showSuggestRejectSheet(booking));
            }

        } else {
            // ── Single lesson UI ───────────────────────────────────────
            if ("pending".equalsIgnoreCase(status) || "free_trial_pending".equalsIgnoreCase(status)) {
                holder.layoutLessonActions.setVisibility(View.VISIBLE);
                holder.btnAccept.setOnClickListener(v -> updateSingleStatus(booking, "confirmed"));
                holder.btnReject.setOnClickListener(v -> showSimpleCancelDialog(booking, "Decline Lesson"));
                
                // Add Suggest Change to single lessons (checking tag to avoid duplicates)
                if (holder.layoutLessonActions.findViewWithTag("btn_suggest") == null) {
                    MaterialButton btnSuggest = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                    btnSuggest.setTag("btn_suggest");
                    btnSuggest.setText("Suggest Change");
                    btnSuggest.setTextSize(10);
                    btnSuggest.setAllCaps(false);
                    btnSuggest.setOnClickListener(v -> showSuggestRejectSheet(booking));
                    holder.layoutLessonActions.addView(btnSuggest);
                }

            } else if ("suggestion_pending".equalsIgnoreCase(status)) {
                holder.layoutLessonActions.setVisibility(View.GONE);
                holder.btnJoin.setVisibility(View.GONE);
                holder.btnCancelTutor.setVisibility(View.VISIBLE);
                holder.btnCancelTutor.setText("Cancel / Change");
                holder.btnCancelTutor.setOnClickListener(v -> showSuggestRejectSheet(booking));

            } else if ("confirmed".equalsIgnoreCase(status)) {
                holder.btnJoin.setVisibility(View.VISIBLE);
                holder.btnCancelTutor.setVisibility(View.VISIBLE);
                holder.btnCancelTutor.setText("Cancel / Change");
                holder.btnCancelTutor.setOnClickListener(v -> showSuggestRejectSheet(booking));

                LessonAlarmScheduler.schedule(context, booking.getBookingId(),
                        booking.getTimestamp(), booking.getSubject(), "tutor");

                holder.btnJoin.removeCallbacks(holder.updateRunnable);
                holder.updateRunnable = new Runnable() {
                    @Override public void run() {
                        boolean joinable = LessonTimeHelper.isJoinable(booking, "tutor");
                        if (joinable) {
                            holder.btnJoin.setEnabled(true);
                            holder.btnJoin.setText("JOIN CLASS");
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
                            if (mins > 60) holder.btnJoin.setText("in " + (mins/60) + "h " + (mins%60) + "m");
                            else if (mins > 0) holder.btnJoin.setText("in " + mins + "m");
                            else holder.btnJoin.setText(System.currentTimeMillis() > booking.getTimestamp() ? "EXPIRED" : "WAITING");
                            holder.btnJoin.postDelayed(this, 30_000);
                        }
                    }
                };
                holder.btnJoin.post(holder.updateRunnable);
            }
        }
    }

    // ── Load all package lessons → build summary + lesson list ────────
    private void loadPackageData(Booking representative, BookingViewHolder holder) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        ref.orderByChild("packageId").equalTo(representative.getPackageId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Booking> lessons = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Booking b = ds.getValue(Booking.class);
                            if (b != null) lessons.add(b);
                        }
                        lessons.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                        if (lessons.isEmpty()) return;

                        SimpleDateFormat shortDate = new SimpleDateFormat("MMM dd", Locale.getDefault());
                        SimpleDateFormat fullDate  = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                        SimpleDateFormat dayName   = new SimpleDateFormat("EEE", Locale.getDefault());
                        SimpleDateFormat timeOnly  = new SimpleDateFormat("HH:mm", Locale.getDefault());

                        // Summary: total, start, schedule, end
                        int total = lessons.size();
                        long startTs = lessons.get(0).getTimestamp();
                        long endTs   = lessons.get(total - 1).getTimestamp();

                        // Unique days for pattern
                        Set<String> daysSet = new TreeSet<>();
                        for (Booking b : lessons) daysSet.add(dayName.format(new Date(b.getTimestamp())));
                        String pattern = String.join("/", daysSet);

                        holder.tvSummaryStart.setText("📅 Start: " + fullDate.format(new Date(startTs)) + ".");
                        holder.tvSummaryEnd.setText("✅ End: " + fullDate.format(new Date(endTs)) + ".");
                        holder.tvSummarySchedule.setText("Proposed Pattern:\n" + pattern + "\n(View List)");

                        // Dates preview (first 4 + overflow)
                        StringBuilder preview = new StringBuilder();
                        int previewCount = Math.min(4, total);
                        for (int i = 0; i < previewCount; i++) {
                            preview.append("• ").append(shortDate.format(new Date(lessons.get(i).getTimestamp())));
                            if (i < previewCount - 1) preview.append("\n");
                        }
                        int remaining = total - previewCount;
                        if (remaining > 0) preview.append("\n+ ").append(remaining).append(" more");
                        holder.tvDatesPreview.setText(preview.toString());

                        // Lesson list in RecyclerView
                        PackageLessonsAdapter adapter = new PackageLessonsAdapter(lessons);
                        holder.rvPackageLessons.setLayoutManager(new LinearLayoutManager(context));
                        holder.rvPackageLessons.setAdapter(adapter);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        Log.e("TutorAdapter", "loadPackageData: " + e.getMessage());
                    }
                });
    }

    // ── "Suggest Changes or Reject" bottom sheet ───────────────────────
    private void showSuggestRejectSheet(Booking booking) {
        BottomSheetDialog sheet = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
        View v = LayoutInflater.from(context).inflate(R.layout.layout_suggest_reject_sheet, null);
        sheet.setContentView(v);

        // Header and Summary
        ImageView ivPhoto = v.findViewById(R.id.ivSheetStudentPhoto);
        TextView tvName = v.findViewById(R.id.tvSheetStudentName);
        TextView tvPkgInfo = v.findViewById(R.id.tvSheetPackageInfo);
        TextView tvTotal = v.findViewById(R.id.tvSheetSummaryTotal);
        TextView tvStart = v.findViewById(R.id.tvSheetSummaryStart);
        TextView tvSchedule = v.findViewById(R.id.tvSheetSummarySchedule);
        TextView tvEnd = v.findViewById(R.id.tvSheetSummaryEnd);
        
        tvName.setText(booking.getTutorName()); // Fallback or fetch
        fetchStudentName(booking.getStudentId(), tvName);
        tvPkgInfo.setText(booking.getSubject() + " (Package - " + booking.getPackageTotalLessons() + " lessons)");

        // Populate summary from Firebase
        List<Booking> cachedLessons = new ArrayList<>();
        List<String> lessonStrings = new ArrayList<>();
        String[] defaultDays = {"Mondays", "Tuesdays", "Wednesdays", "Thursdays", "Fridays", "Saturdays", "Sundays"};
        List<String> activeDaysList = new ArrayList<>(java.util.Arrays.asList(defaultDays));
        
        android.widget.RadioGroup rgScope = v.findViewById(R.id.rgChangeScope);
        TextView tvSourceLabel = v.findViewById(R.id.tvSourcePatternLabel);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        ref.orderByChild("packageId").equalTo(booking.getPackageId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Booking> lessons = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Booking b = ds.getValue(Booking.class);
                            if (b != null) lessons.add(b);
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
                        SimpleDateFormat fullDayName = new SimpleDateFormat("EEEE", Locale.getDefault());
                        Set<String> uniqueDays = new TreeSet<>();
                        
                        for (Booking b : lessons) {
                            daysSet.add(dayName.format(new Date(b.getTimestamp())));
                            uniqueDays.add(fullDayName.format(new Date(b.getTimestamp())) + "s");
                        }
                        String schedule = String.join("/", daysSet) + " @ " + sharedTime;

                        activeDaysList.clear();
                        activeDaysList.addAll(uniqueDays);

                        tvTotal.setText("• " + total + " sessions total");
                        tvStart.setText("• Start: " + fullDate.format(new Date(startTs)));
                        tvSchedule.setText("• Schedule: " + schedule);
                        tvEnd.setText("• Projected End: " + fullDate.format(new Date(endTs)));

                        cachedLessons.clear();
                        lessonStrings.clear();
                        SimpleDateFormat lessonFormat = new SimpleDateFormat("MMM dd, yyyy @ HH:mm", Locale.getDefault());
                        for (Booking b : lessons) {
                            cachedLessons.add(b);
                            lessonStrings.add("Lesson: " + lessonFormat.format(new Date(b.getTimestamp())));
                        }
                        if (rgScope.getCheckedRadioButtonId() == R.id.rbScopeSingle) {
                            android.widget.ArrayAdapter<String> spinAdapter = (android.widget.ArrayAdapter<String>) ((android.widget.Spinner) v.findViewById(R.id.spinnerSourceDay)).getAdapter();
                            if (spinAdapter != null) {
                                spinAdapter.clear();
                                spinAdapter.addAll(lessonStrings);
                                spinAdapter.notifyDataSetChanged();
                            }
                        } else {
                            android.widget.ArrayAdapter<String> spinAdapter = (android.widget.ArrayAdapter<String>) ((android.widget.Spinner) v.findViewById(R.id.spinnerSourceDay)).getAdapter();
                            if (spinAdapter != null) {
                                spinAdapter.clear();
                                spinAdapter.addAll(activeDaysList);
                                spinAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });

        // Bulk Change Spinners and Time
        android.widget.Spinner spinSource = v.findViewById(R.id.spinnerSourceDay);
        android.widget.Spinner spinDest = v.findViewById(R.id.spinnerDestDay);
        MaterialButton btnDestTime = v.findViewById(R.id.btnDestTime);
        
        android.widget.ArrayAdapter<String> adapterSource = new android.widget.ArrayAdapter<>(context, android.R.layout.simple_spinner_item, new ArrayList<>(activeDaysList));
        adapterSource.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinSource.setAdapter(adapterSource);

        android.widget.ArrayAdapter<String> adapterDest = new android.widget.ArrayAdapter<>(context, android.R.layout.simple_spinner_item, defaultDays);
        adapterDest.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinDest.setAdapter(adapterDest);

        rgScope.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbScopeSingle) {
                if (tvSourceLabel != null) tvSourceLabel.setText(R.string.select_lesson_label);
                adapterSource.clear();
                adapterSource.addAll(lessonStrings);
                adapterSource.notifyDataSetChanged();
            } else {
                if (tvSourceLabel != null) tvSourceLabel.setText(R.string.source_pattern_label);
                adapterSource.clear();
                adapterSource.addAll(activeDaysList);
                adapterSource.notifyDataSetChanged();
            }
        });

        btnDestTime.setOnClickListener(b -> {
            com.google.android.material.timepicker.MaterialTimePicker tp = new com.google.android.material.timepicker.MaterialTimePicker.Builder()
                .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
                .setHour(14).setMinute(0).setTitleText("Select Alternative Time").build();
            tp.addOnPositiveButtonClickListener(v2 -> btnDestTime.setText(String.format(Locale.getDefault(), "%02d:%02d", tp.getHour(), tp.getMinute())));
            tp.show(((androidx.fragment.app.FragmentActivity)context).getSupportFragmentManager(), "COUNTER_TIME");
        });

        // Decision Panel
        TextInputEditText etMessage = v.findViewById(R.id.etSheetMessage);
        MaterialButton btnSubmit = v.findViewById(R.id.btnSheetSubmitCounter);
        MaterialButton btnReject = v.findViewById(R.id.btnSheetRejectProposal);
        MaterialButton btnCancel = v.findViewById(R.id.btnSheetCancelAction);

        btnSubmit.setOnClickListener(btn -> {
            String msg = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
            String source = spinSource.getSelectedItem().toString();
            String dest = spinDest.getSelectedItem().toString();
            String time = btnDestTime.getText().toString();
            int selectedPos = spinSource.getSelectedItemPosition();
            
            DatabaseReference pkgRef = FirebaseDatabase.getInstance().getReference("Bookings");
            if (booking.isPackage() && booking.getPackageId() != null) {
                if (rgScope.getCheckedRadioButtonId() == R.id.rbScopeSingle) {
                    if (selectedPos >= 0 && selectedPos < cachedLessons.size()) {
                        Booking targetLesson = cachedLessons.get(selectedPos);
                        DatabaseReference dr = pkgRef.child(targetLesson.getBookingId());
                        dr.child("suggestedSourceDay").setValue("This Lesson");
                        dr.child("suggestedDestDay").setValue(dest.replace("s", "")); // "Tuesdays" -> "Tuesday"
                        dr.child("suggestedTime").setValue(time);
                        dr.child("suggestionMessage").setValue(msg);
                        dr.child("status").setValue("suggestion_pending");
                        
                        sendSuggestionNotification(booking.getStudentId(), booking.getSubject(), "Tutor suggested a new time for a specific lesson: " + time);
                        Toast.makeText(context, "Suggestion submitted!", Toast.LENGTH_SHORT).show();
                        sheet.dismiss();
                    }
                } else {
                    pkgRef.orderByChild("packageId").equalTo(booking.getPackageId())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot ds : snapshot.getChildren()) {
                                        DatabaseReference dr = ds.getRef();
                                        dr.child("suggestedSourceDay").setValue(source);
                                        dr.child("suggestedDestDay").setValue(dest);
                                        dr.child("suggestedTime").setValue(time);
                                        dr.child("suggestionMessage").setValue(msg);
                                        dr.child("status").setValue("suggestion_pending");
                                    }
                                    sendSuggestionNotification(booking.getStudentId(), booking.getSubject(), "Tutor suggested a schedule change.");
                                    Toast.makeText(context, "Counter-offer submitted!", Toast.LENGTH_SHORT).show();
                                    sheet.dismiss();
                                }
                                @Override public void onCancelled(@NonNull DatabaseError e) {}
                            });
                }
            } else {
                // Single lesson suggestion
                DatabaseReference dr = pkgRef.child(booking.getBookingId());
                dr.child("suggestedSourceDay").setValue("This Lesson");
                dr.child("suggestedDestDay").setValue(dest.replace("s", "")); // "Tuesdays" -> "Tuesday"
                dr.child("suggestedTime").setValue(time);
                dr.child("suggestionMessage").setValue(msg);
                dr.child("status").setValue("suggestion_pending");
                sendSuggestionNotification(booking.getStudentId(), booking.getSubject(), "Tutor suggested a new time: " + time);
                Toast.makeText(context, "Suggestion submitted!", Toast.LENGTH_SHORT).show();
                sheet.dismiss();
            }
        });

        btnReject.setOnClickListener(btn -> {
            String msg = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
            if (msg.isEmpty()) {
                Toast.makeText(context, "Please provide a reason for rejection", Toast.LENGTH_SHORT).show();
                return;
            }
            performCancellation(booking, msg);
            Toast.makeText(context, "Proposal rejected.", Toast.LENGTH_SHORT).show();
            sheet.dismiss();
        });

        btnCancel.setOnClickListener(btn -> sheet.dismiss());

        sheet.show();
    }

    // ── Simple cancel dialog (single lessons) ─────────────────────────
    private void showSimpleCancelDialog(Booking booking, String title) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage("Please provide a reason:");
        final android.widget.EditText input = new android.widget.EditText(context);
        input.setHint("Enter reason here...");
        builder.setView(input);
        builder.setPositiveButton("Confirm", (d, w) -> {
            String reason = input.getText().toString().trim();
            if (reason.isEmpty()) Toast.makeText(context, "Reason is required", Toast.LENGTH_SHORT).show();
            else performCancellation(booking, reason);
        });
        builder.setNegativeButton("Back", (d, w) -> d.cancel());
        builder.show();
    }

    // ── Firebase actions ───────────────────────────────────────────────
    private void updatePackageStatus(Booking booking, String newStatus) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        ref.orderByChild("packageId").equalTo(booking.getPackageId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) ds.getRef().child("status").setValue(newStatus);
                        Toast.makeText(context, "Package confirmed!", Toast.LENGTH_SHORT).show();
                        if ("confirmed".equals(newStatus)) {
                            sendNotification(booking.getStudentId(), "Package Accepted 🎉",
                                    "Your package for " + booking.getSubject() + " has been accepted!");
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void updateSingleStatus(Booking booking, String newStatus) {
        FirebaseDatabase.getInstance().getReference("Bookings")
                .child(booking.getBookingId()).child("status").setValue(newStatus)
                .addOnSuccessListener(a -> {
                    Toast.makeText(context, "Lesson " + newStatus, Toast.LENGTH_SHORT).show();
                    if ("confirmed".equals(newStatus)) {
                        sendNotification(booking.getStudentId(), "Lesson Accepted ✅",
                                "Your lesson for " + booking.getSubject() + " has been accepted!");
                    }
                });
    }

    private void performCancellation(Booking booking, String reason) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        if (booking.isPackage() && booking.getPackageId() != null) {
            ref.orderByChild("packageId").equalTo(booking.getPackageId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                ds.getRef().child("status").setValue("cancelled");
                                ds.getRef().child("cancellationReason").setValue(reason);
                            }
                            sendNotification(booking.getStudentId(), "Package Declined",
                                    "Your package for " + booking.getSubject() + " was declined. Reason: " + reason);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError e) {}
                    });
        } else {
            ref.child(booking.getBookingId()).child("status").setValue("cancelled");
            ref.child(booking.getBookingId()).child("cancellationReason").setValue(reason)
                    .addOnSuccessListener(a -> sendNotification(booking.getStudentId(), "Lesson Cancelled",
                            "Your lesson for " + booking.getSubject() + " was cancelled. Reason: " + reason));
        }
    }

    private void sendSuggestionNotification(String studentId, String subject, String message) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("Notifications").child(studentId).push();
        Map<String, Object> data = new HashMap<>();
        data.put("title", "📅 Schedule Suggestion for " + subject);
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

    private void fetchStudentName(String studentId, TextView tvName) {
        FirebaseDatabase.getInstance().getReference("Users").child("Student").child(studentId)
                .child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        tvName.setText(snap.exists() ? snap.getValue(String.class) : "Unknown Student");
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    @Override public int getItemCount() { return bookingList.size(); }

    // ── ViewHolder ─────────────────────────────────────────────────────
    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvSubject, tvTime, tvDate;
        TextView tvSummaryStart, tvSummarySchedule, tvSummaryEnd, tvDatesPreview;
        LinearLayout layoutLessonActions, layoutPackageSummary, layoutPackagePendingActions;
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
            tvTime                    = itemView.findViewById(R.id.tvLessonTime);
            tvDate                    = itemView.findViewById(R.id.tvLessonDate);
            tvSummaryStart            = itemView.findViewById(R.id.tvSummaryStart);
            tvSummarySchedule         = itemView.findViewById(R.id.tvSummarySchedule);
            tvSummaryEnd              = itemView.findViewById(R.id.tvSummaryEnd);
            tvDatesPreview            = itemView.findViewById(R.id.tvDatesPreview);
            layoutPackageSummary      = itemView.findViewById(R.id.layoutPackageSummary);
            layoutLessonActions       = itemView.findViewById(R.id.layoutLessonActions);
            layoutPackagePendingActions = itemView.findViewById(R.id.layoutPackagePendingActions);
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

        PackageLessonsAdapter(List<Booking> lessons) { this.lessons = lessons; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_package_lesson, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Booking b = lessons.get(pos);
            h.tvNumber.setText("#" + (pos + 1));
            h.tvDateTime.setText(fmt.format(new Date(b.getTimestamp())));
            String s = b.getStatus() != null ? b.getStatus() : "pending";
            h.tvStatus.setText(s);
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
