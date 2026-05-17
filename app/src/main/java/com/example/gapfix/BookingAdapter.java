package com.example.gapfix;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private static final int TYPE_SINGLE = 0;
    private static final int TYPE_PACKAGE = 1;

    private List<Booking> bookingList;
    private Context context;

    public BookingAdapter(Context context, List<Booking> bookingList) {
        this.context = context;
        this.bookingList = bookingList;
    }

    @Override
    public int getItemViewType(int position) {
        Booking b = bookingList.get(position);
        return (b.isPackage() && b.getPackageId() != null) ? TYPE_PACKAGE : TYPE_SINGLE;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == TYPE_PACKAGE) ? R.layout.item_booking_package_student : R.layout.item_booking;
        View view = LayoutInflater.from(context).inflate(layout, parent, false);
        return new BookingViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);
        boolean isPackage = holder.viewType == TYPE_PACKAGE;

        // Common Fields
        holder.tvSubject.setText(booking.getSubject());
        String status = booking.getStatus() != null ? booking.getStatus() : "";

        // Status Colors & logic
        if (isPackage) {
            bindPackageView(holder, booking);
        } else {
            bindSingleView(holder, booking);
        }

        // Fetch Student/Tutor info
        // In Student view, we usually show Tutor info.
        // The screenshot shows "Gevorg" as the title, who is likely the Tutor in this context.
        fetchUserInfo(booking.getTutorId(), holder.tvName, holder.ivPhoto);
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
            holder.tvStatus.setText("• CANCELLED");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error));
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnAction.setVisibility(View.GONE);
        } else if ("suggestion_pending".equalsIgnoreCase(status)) {
            holder.tvStatus.setText("• ACTION REQUIRED: TUTOR SUGGESTED CHANGES");
            holder.tvStatus.setTextColor(Color.parseColor("#C53030"));
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnAction.setVisibility(View.GONE);
            holder.itemView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF5F5")));
            holder.layoutSuggestion.setVisibility(View.VISIBLE);
            String source = booking.getSuggestedSourceDay() != null ? booking.getSuggestedSourceDay() : "Current";
            String dest = booking.getSuggestedDestDay() != null ? booking.getSuggestedDestDay() : "New Day";
            String time = booking.getSuggestedTime() != null ? booking.getSuggestedTime() : "New Time";
            holder.tvSuggestionDetails.setText("Move " + source + " to " + dest + " at " + time);
            holder.tvSuggestionMsg.setText("\"" + (booking.getSuggestionMessage() != null ? booking.getSuggestionMessage() : "") + "\"");
            holder.btnAcceptSuggestion.setOnClickListener(v -> acceptSuggestion(booking));
            holder.btnRejectSuggestion.setOnClickListener(v -> rejectSuggestion(booking));
        } else {
            holder.tvStatus.setText("• " + status.toUpperCase());
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.gapfix_text_secondary));
            holder.btnCancel.setVisibility(("pending".equals(status) || "confirmed".equals(status)) ? View.VISIBLE : View.GONE);
            holder.btnCancel.setOnClickListener(v -> showCancelDialog(booking));
            if ("confirmed".equals(status)) {
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.gapfix_green));
                holder.btnAction.setVisibility(View.VISIBLE);
                holder.btnAction.setText("Join");
                holder.btnAction.setOnClickListener(v -> joinLesson(booking));
            } else {
                holder.btnAction.setVisibility(View.GONE);
            }
        }
    }

    private void bindPackageView(BookingViewHolder holder, Booking booking) {
        String status = booking.getStatus() != null ? booking.getStatus() : "";
        
        // Header Status text matching screenshot
        String statusDisplay = booking.getSubject() + " (Package · " + booking.getPackageTotalLessons() + " Lessons)";
        if ("suggestion_pending".equalsIgnoreCase(status)) {
            statusDisplay += " [REVIEW CHANGES]";
            holder.tvSubject.setTextColor(Color.parseColor("#C53030"));
        } else if ("confirmed".equalsIgnoreCase(status)) {
            statusDisplay += " [CONFIRMED]";
            holder.tvSubject.setTextColor(ContextCompat.getColor(context, R.color.gapfix_green));
        } else {
            statusDisplay += " [AWAITING TUTOR]";
            holder.tvSubject.setTextColor(ContextCompat.getColor(context, R.color.gapfix_text_secondary));
        }
        holder.tvSubject.setText(statusDisplay);

        // Fetch package data for pattern and list
        loadPackageDetails(booking, holder);

        // Buttons & Suggestions
        if ("suggestion_pending".equalsIgnoreCase(status)) {
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.btnReviewChanges.setText("Review / Change");
            holder.btnReviewChanges.setVisibility(View.VISIBLE);
            holder.btnReviewChanges.setOnClickListener(v -> showReviewChangesSheet(booking));

            holder.btnAcceptProposal.setVisibility(View.VISIBLE);
            holder.btnAcceptProposal.setText("Accept Proposal");
            holder.btnAcceptProposal.setOnClickListener(v -> acceptSuggestion(booking));

            holder.btnRejectProposal.setVisibility(View.VISIBLE);
            holder.btnRejectProposal.setText("Reject");
            holder.btnRejectProposal.setOnClickListener(v -> rejectSuggestion(booking));

            // Populate suggestion content
            holder.layoutSuggestion.setVisibility(View.VISIBLE);
            String src = booking.getSuggestedSourceDay() != null ? booking.getSuggestedSourceDay() : "Current";
            String dst = booking.getSuggestedDestDay() != null ? booking.getSuggestedDestDay() : "New Day";
            String tm = booking.getSuggestedTime() != null ? booking.getSuggestedTime() : "New Time";
            holder.tvSuggestionDetails.setText("Move all " + src + " to " + dst + " at " + tm);
            holder.tvSuggestionMsg.setText("\"" + (booking.getSuggestionMessage() != null ? booking.getSuggestionMessage() : "No message") + "\"");

        } else if ("pending".equalsIgnoreCase(status) || "free_trial_pending".equalsIgnoreCase(status)) {
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.btnReviewChanges.setText("EDIT PROPOSAL");
            holder.btnReviewChanges.setVisibility(View.VISIBLE);
            holder.btnReviewChanges.setOnClickListener(v -> showReviewChangesSheet(booking));

            holder.btnAcceptProposal.setVisibility(View.GONE);

            holder.btnRejectProposal.setVisibility(View.VISIBLE);
            holder.btnRejectProposal.setText("CANCEL PROPOSAL");
            holder.btnRejectProposal.setOnClickListener(v -> performCancellation(booking, "Student cancelled proposal"));
        } else if ("confirmed".equalsIgnoreCase(status)) {
            holder.layoutActions.setVisibility(View.GONE);
            holder.btnCancelStudent.setVisibility(View.VISIBLE);
            holder.btnCancelStudent.setOnClickListener(v -> showReviewChangesSheet(booking));
        } else {
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.btnReviewChanges.setVisibility(View.GONE);
            holder.btnAcceptProposal.setVisibility(View.GONE);
            holder.btnRejectProposal.setVisibility(View.VISIBLE);
            holder.btnRejectProposal.setText("CANCEL PROPOSAL");
            holder.btnRejectProposal.setOnClickListener(v -> performCancellation(booking, "Student cancelled proposal"));

            holder.btnCancelStudent.setVisibility(View.GONE);
        }

        // Toggle
        holder.btnExpandPackage.setOnClickListener(v -> {
            if (holder.expanded) {
                holder.expanded = false;
                holder.rvPackageLessons.setVisibility(View.GONE);
                holder.btnExpandPackage.setText("▼ View all lessons in package");
            } else {
                holder.expanded = true;
                holder.rvPackageLessons.setVisibility(View.VISIBLE);
                holder.btnExpandPackage.setText("▲ Hide all lessons in package");
            }
        });
    }

    private void loadPackageDetails(Booking booking, BookingViewHolder holder) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        ref.orderByChild("packageId").equalTo(booking.getPackageId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Booking> list = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Booking b = ds.getValue(Booking.class);
                            if (b != null) list.add(b);
                        }
                        list.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                        if (list.isEmpty()) return;

                        SimpleDateFormat shortDate = new SimpleDateFormat("MMM dd", Locale.getDefault());
                        SimpleDateFormat fullDate  = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                        SimpleDateFormat dayFmt = new SimpleDateFormat("EEE", Locale.getDefault());

                        int total = list.size();
                        long startTs = list.get(0).getTimestamp();
                        long endTs   = list.get(total - 1).getTimestamp();

                        // Pattern calculation
                        Set<String> days = new TreeSet<>();
                        for (Booking b : list) days.add(dayFmt.format(new Date(b.getTimestamp())));
                        String pattern = "Proposed Pattern:\n" + String.join("/", days) + "\n(View List)";
                        holder.tvSummaryPattern.setText(pattern);

                        // Summary dates
                        holder.tvSummaryStart.setText("📅 Start: " + fullDate.format(new Date(startTs)) + ".");
                        holder.tvSummaryEnd.setText("✅ End: " + fullDate.format(new Date(endTs)) + ".");

                        // Dates preview
                        StringBuilder preview = new StringBuilder();
                        int previewCount = Math.min(4, total);
                        for (int i = 0; i < previewCount; i++) {
                            preview.append("• ").append(shortDate.format(new Date(list.get(i).getTimestamp())));
                            if (i < previewCount - 1) preview.append("\n");
                        }
                        if (total > previewCount) {
                            preview.append("\n+ ").append(total - previewCount).append(" more");
                        }
                        holder.tvDatesPreview.setText(preview.toString());

                        // Nested List
                        holder.rvPackageLessons.setLayoutManager(new LinearLayoutManager(context));
                        holder.rvPackageLessons.setAdapter(new PackageLessonsAdapter(list));
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void joinLesson(Booking booking) {
        if (LessonTimeHelper.isJoinable(booking, "student")) {
            Intent i = new Intent(context, VideoCallActivity.class);
            i.putExtra("BOOKING_ID", booking.getBookingId());
            i.putExtra("IS_INCOMING", false);
            context.startActivity(i);
        } else {
            Toast.makeText(context, "Join button active 5 mins before lesson.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showReviewChangesSheet(Booking booking) {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
        View v = LayoutInflater.from(context).inflate(R.layout.layout_suggest_reject_sheet, null);
        sheet.setContentView(v);

        // Customize for Student
        TextView tvTitle = v.findViewById(R.id.tvSheetTitle);
        if (tvTitle != null) tvTitle.setText("Review & Suggest Alternative");
        
        TextView tvName = v.findViewById(R.id.tvSheetStudentName);
        tvName.setText(booking.getTutorName());
        
        v.findViewById(R.id.btnSheetRejectProposal).setVisibility(View.GONE); // Student has Reject on the main card

        // Bulk Change Logic (Spinners)
        List<Booking> cachedLessons = new ArrayList<>();
        List<String> lessonStrings = new ArrayList<>();
        String[] defaultDays = {"Mondays", "Tuesdays", "Wednesdays", "Thursdays", "Fridays", "Saturdays", "Sundays"};
        List<String> activeDaysList = new ArrayList<>(java.util.Arrays.asList(defaultDays));
        
        android.widget.RadioGroup rgScope = v.findViewById(R.id.rgChangeScope);
        TextView tvSourceLabel = v.findViewById(R.id.tvSourcePatternLabel);

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
                .setHour(12).setMinute(0).setTitleText("Select Preferred Time").build();
            tp.addOnPositiveButtonClickListener(v2 -> btnDestTime.setText(String.format(Locale.getDefault(), "%02d:%02d", tp.getHour(), tp.getMinute())));
            tp.show(((androidx.fragment.app.FragmentActivity)context).getSupportFragmentManager(), "STUDENT_SUGGEST_TIME");
        });

        v.findViewById(R.id.btnSheetSubmitCounter).setOnClickListener(btn -> {
            String msg = ((com.google.android.material.textfield.TextInputEditText)v.findViewById(R.id.etSheetMessage)).getText().toString().trim();
            String source = spinSource.getSelectedItem() != null ? spinSource.getSelectedItem().toString() : "";
            String dest = spinDest.getSelectedItem().toString();
            String time = btnDestTime.getText().toString();
            int selectedPos = spinSource.getSelectedItemPosition();

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
            if (rgScope.getCheckedRadioButtonId() == R.id.rbScopeSingle) {
                if (selectedPos >= 0 && selectedPos < cachedLessons.size()) {
                    Booking targetLesson = cachedLessons.get(selectedPos);
                    DatabaseReference dr = ref.child(targetLesson.getBookingId());
                    dr.child("suggestedSourceDay").setValue("This Lesson");
                    dr.child("suggestedDestDay").setValue(dest.replace("s", "")); // "Tuesdays" -> "Tuesday"
                    dr.child("suggestedTime").setValue(time);
                    dr.child("suggestionMessage").setValue("Student: " + msg);
                    dr.child("status").setValue("pending"); // Reset to pending for tutor to review
                    
                    Toast.makeText(context, "Alternative suggestion sent for specific lesson!", Toast.LENGTH_SHORT).show();
                    sheet.dismiss();
                }
            } else {
                ref.orderByChild("packageId").equalTo(booking.getPackageId()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            DatabaseReference dr = ds.getRef();
                            dr.child("suggestedSourceDay").setValue(source);
                            dr.child("suggestedDestDay").setValue(dest);
                            dr.child("suggestedTime").setValue(time);
                            dr.child("suggestionMessage").setValue("Student: " + msg);
                            dr.child("status").setValue("pending"); // Reset to pending for tutor to review
                        }
                        Toast.makeText(context, "Alternative suggestion sent!", Toast.LENGTH_SHORT).show();
                        sheet.dismiss();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
            }
        });

        // Pre-fetch lessons for single scope
        FirebaseDatabase.getInstance().getReference("Bookings")
                .orderByChild("packageId").equalTo(booking.getPackageId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Booking> lessons = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Booking b = ds.getValue(Booking.class);
                            if (b != null) lessons.add(b);
                        }
                        lessons.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                        
                        cachedLessons.clear();
                        lessonStrings.clear();
                        SimpleDateFormat fullDayName = new SimpleDateFormat("EEEE", Locale.getDefault());
                        Set<String> uniqueDays = new TreeSet<>();
                        SimpleDateFormat lessonFormat = new SimpleDateFormat("MMM dd, yyyy @ HH:mm", Locale.getDefault());
                        for (Booking b : lessons) {
                            cachedLessons.add(b);
                            lessonStrings.add("Lesson: " + lessonFormat.format(new Date(b.getTimestamp())));
                            uniqueDays.add(fullDayName.format(new Date(b.getTimestamp())) + "s");
                        }
                        
                        activeDaysList.clear();
                        activeDaysList.addAll(uniqueDays);

                        if (rgScope.getCheckedRadioButtonId() == R.id.rbScopeSingle) {
                            adapterSource.clear();
                            adapterSource.addAll(lessonStrings);
                            adapterSource.notifyDataSetChanged();
                        } else {
                            adapterSource.clear();
                            adapterSource.addAll(activeDaysList);
                            adapterSource.notifyDataSetChanged();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
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
                            Glide.with(context).load(img).placeholder(R.drawable.person_circle).circleCrop().into(ivPhoto);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void acceptSuggestion(Booking booking) {
        String sourceDayStr = booking.getSuggestedSourceDay();
        String destDayStr = booking.getSuggestedDestDay();
        String newTime = booking.getSuggestedTime();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        ref.orderByChild("packageId").equalTo(booking.getPackageId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Booking b = ds.getValue(Booking.class);
                            if (b == null) continue;

                            DatabaseReference dr = ds.getRef();
                            
                            // If it's a single suggestion or the day matches the source day, we shift it
                            // Note: sourceDayStr might be "Wednesdays" or "This Lesson"
                            boolean shouldShift = false;
                            if ("This Lesson".equals(sourceDayStr)) {
                                shouldShift = true;
                            } else if (sourceDayStr != null) {
                                java.util.Calendar cal = java.util.Calendar.getInstance();
                                cal.setTimeInMillis(b.getTimestamp());
                                String currentDay = new SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.getTime()) + "s"; // "Wednesday" -> "Wednesdays"
                                if (currentDay.equalsIgnoreCase(sourceDayStr)) {
                                    shouldShift = true;
                                }
                            }

                            if (shouldShift && destDayStr != null && newTime != null) {
                                applyShift(dr, b, sourceDayStr, destDayStr, newTime);
                            } else {
                                // Just confirm and clear
                                dr.child("status").setValue("confirmed");
                                dr.child("suggestedSourceDay").removeValue();
                                dr.child("suggestedDestDay").removeValue();
                                dr.child("suggestedTime").removeValue();
                                dr.child("suggestionMessage").removeValue();
                            }
                        }
                        Toast.makeText(context, "Suggestion accepted! Schedule updated.", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void applyShift(DatabaseReference dr, Booking b, String source, String dest, String time) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(b.getTimestamp());

        if (!"This Lesson".equals(source)) {
            int sourceDay = getDayOfWeek(source);
            int destDay = getDayOfWeek(dest);
            int diff = destDay - sourceDay;
            cal.add(java.util.Calendar.DATE, diff);
        } else {
            // For single lessons, we just move it to the next occurrence of 'dest' if needed, 
            // but usually 'dest' is a specific day name like "Tuesday".
            int destDay = getDayOfWeek(dest + "s"); // "Tuesday" -> "Tuesdays"
            while (cal.get(java.util.Calendar.DAY_OF_WEEK) != destDay) {
                cal.add(java.util.Calendar.DATE, 1);
            }
        }

        // Apply new time
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
                            Toast.makeText(context, "Cancelled.", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onCancelled(@NonNull DatabaseError e) {}
                    });
        } else {
            ref.child(booking.getBookingId()).child("status").setValue("cancelled");
            ref.child(booking.getBookingId()).child("cancellationReason").setValue(reason);
            Toast.makeText(context, "Cancelled.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCancelDialog(Booking booking) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Cancel Lesson").setMessage("Are you sure?").setPositiveButton("Yes", (d, w) -> performCancellation(booking, "Student cancelled")).setNegativeButton("No", null).show();
    }

    @Override public int getItemCount() { return bookingList.size(); }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        int viewType;
        ImageView ivPhoto;
        TextView tvName, tvSubject, tvTime, tvDate, tvStatus;
        MaterialButton btnCancel, btnAction;
        
        // Single Suggestion
        View layoutSuggestion;
        TextView tvSuggestionDetails, tvSuggestionMsg;
        MaterialButton btnAcceptSuggestion, btnRejectSuggestion;

        // Package specific
        TextView tvSummaryStart, tvSummaryEnd, tvSummaryPattern, tvDatesPreview;
        TextView btnExpandPackage;
        RecyclerView rvPackageLessons;
        View layoutActions;
        MaterialButton btnReviewChanges, btnAcceptProposal, btnRejectProposal, btnReviewCalendar, btnCancelStudent;

        boolean expanded = false;

        public BookingViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;
            if (viewType == TYPE_PACKAGE) {
                ivPhoto = itemView.findViewById(R.id.ivTutorPhoto);
                tvName = itemView.findViewById(R.id.tvTutorName);
                tvSubject = itemView.findViewById(R.id.tvSubject);
                tvSummaryStart = itemView.findViewById(R.id.tvSummaryStart);
                tvSummaryEnd = itemView.findViewById(R.id.tvSummaryEnd);
                tvSummaryPattern = itemView.findViewById(R.id.tvSummaryPattern);
                tvDatesPreview = itemView.findViewById(R.id.tvDatesPreview);
                btnReviewCalendar = itemView.findViewById(R.id.btnReviewCalendar);
                btnExpandPackage = itemView.findViewById(R.id.btn_expand_package);
                rvPackageLessons = itemView.findViewById(R.id.rv_package_lessons);
                layoutActions = itemView.findViewById(R.id.layout_actions);
                btnReviewChanges = itemView.findViewById(R.id.btn_review_changes);
                btnAcceptProposal = itemView.findViewById(R.id.btn_accept_proposal);
                btnRejectProposal = itemView.findViewById(R.id.btn_reject_proposal);
                btnCancelStudent = itemView.findViewById(R.id.btnCancelStudent);
                layoutSuggestion = itemView.findViewById(R.id.layout_suggestion);
                tvSuggestionDetails = itemView.findViewById(R.id.tv_suggestion_details);
                tvSuggestionMsg = itemView.findViewById(R.id.tv_suggestion_msg);
            } else {
                ivPhoto = itemView.findViewById(R.id.tutor_image);
                tvName = itemView.findViewById(R.id.tv_tutor_name);
                tvSubject = itemView.findViewById(R.id.tv_subject);
                tvTime = itemView.findViewById(R.id.tv_time);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvStatus = itemView.findViewById(R.id.tv_status);
                btnCancel = itemView.findViewById(R.id.btn_cancel);
                btnAction = itemView.findViewById(R.id.btn_action);
                layoutSuggestion = itemView.findViewById(R.id.layout_suggestion);
                tvSuggestionDetails = itemView.findViewById(R.id.tv_suggestion_details);
                tvSuggestionMsg = itemView.findViewById(R.id.tv_suggestion_msg);
                btnAcceptSuggestion = itemView.findViewById(R.id.btn_accept_suggestion);
                btnRejectSuggestion = itemView.findViewById(R.id.btn_reject_suggestion);
                rvPackageLessons = itemView.findViewById(R.id.rv_package_lessons);
                btnExpandPackage = itemView.findViewById(R.id.btn_expand_package);
            }
        }
    }

    static class PackageLessonsAdapter extends RecyclerView.Adapter<PackageLessonsAdapter.VH> {
        private List<Booking> list;
        private SimpleDateFormat fmt = new SimpleDateFormat("EEE, MMM dd, HH:mm", Locale.getDefault());
        PackageLessonsAdapter(List<Booking> list) { this.list = list; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_package_lesson, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int p) {
            Booking b = list.get(p);
            h.tvNum.setText("#" + (p + 1));
            h.tvInfo.setText("Lesson #" + (p+1) + ": " + fmt.format(new Date(b.getTimestamp())) + " (" + b.getSubject() + ")");
            h.tvStatus.setText("SUBMITTED"); // Match screenshot
        }
        @Override public int getItemCount() { return list.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView tvNum, tvInfo, tvStatus;
            VH(View v) {
                super(v);
                tvNum = v.findViewById(R.id.tvLessonNumber);
                tvInfo = v.findViewById(R.id.tvLessonDateTime);
                tvStatus = v.findViewById(R.id.tvLessonItemStatus);
            }
        }
    }
}
