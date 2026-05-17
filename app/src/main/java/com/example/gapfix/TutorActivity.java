package com.example.gapfix;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public class TutorActivity extends AppCompatActivity {

    private ReviewAdapter adapter;
    private RecyclerView reviewsRv;
    private List<Review> reviewList;
    private Button btnBookTrial, btnBookLesson;
    private Tutor tutor;
    
    private TextView tvAvgRating, tvReviewCount, tvMemberSince, tvAboutTitle;
    private RatingBar summaryRatingBar;
    private ImageView tutorBanner;

    // Selection fields for the booking process
    private Tutor.SubjectPreference selectedPref;
    private String selectedSubjectName;
    private long selectedDateMs = -1;
    private int selectedHour = -1;
    private int selectedMinute = -1;
    
    // Package Logic Fields
    private boolean isPackageMode = false;
    private int packageQuantity = 12;
    private long packageStartDateMs = -1;
    private List<WeeklySlot> weeklySlots = new ArrayList<>();
    private WeeklySlotAdapter weeklySlotAdapter;

    // Beautiful subject colors cycling
    private final String[] SUBJECT_COLORS = {"#7E22CE", "#1D4ED8", "#059669", "#B91C1C", "#C2410C"};
    private final String[] DAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tutor);

        tutor = (Tutor) getIntent().getSerializableExtra("tutor");

        if (tutor == null) {
            finish();
            return;
        }

        initUI();
        fetchReviewsByTutorId(tutor.getId());
    }

    private void initUI() {
        TextView tutorName = findViewById(R.id.tutor_name);
        TextView tutorBio = findViewById(R.id.tutor_bio);
        tvAboutTitle = findViewById(R.id.tv_about_title);
        ChipGroup tutorSubjectsChips = findViewById(R.id.tutor_subjects_chips);
        reviewsRv = findViewById(R.id.reviews);
        ImageView profileImage = findViewById(R.id.tutor_image);
        tutorBanner = findViewById(R.id.tutor_banner);
        btnBookTrial = findViewById(R.id.btnBookTrial);
        btnBookLesson = findViewById(R.id.btnBookLesson);
        
        tvAvgRating = findViewById(R.id.tv_avg_rating);
        tvReviewCount = findViewById(R.id.tv_review_count);
        tvMemberSince = findViewById(R.id.tv_member_since);
        summaryRatingBar = findViewById(R.id.summary_rating_bar);

        reviewList = new ArrayList<>();
        adapter = new ReviewAdapter(reviewList);
        reviewsRv.setLayoutManager(new LinearLayoutManager(this));
        reviewsRv.setAdapter(adapter);

        tutorName.setText(tutor.getName());
        tutorBio.setText(tutor.getBio());
        tvAboutTitle.setText("About " + tutor.getName());
        tvMemberSince.setText("Member since 2024"); 

        tutorSubjectsChips.removeAllViews();
        if (tutor.getPreferences() != null) {
            for (int i = 0; i < tutor.getPreferences().size(); i++) {
                Tutor.SubjectPreference pref = tutor.getPreferences().get(i);
                Chip chip = new Chip(this);
                chip.setText(getShortSubjectInfo(pref));
                
                String colorStr = SUBJECT_COLORS[i % SUBJECT_COLORS.length];
                chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor(colorStr)));
                chip.setTextColor(Color.WHITE);
                chip.setChipStrokeWidth(0f);
                chip.setClickable(false);
                tutorSubjectsChips.addView(chip);
            }
        }

        Glide.with(this)
                .load(tutor.getImageResourceLink() != null ? tutor.getImageResourceLink() : R.drawable.person_circle)
                .placeholder(R.drawable.person_circle)
                .centerCrop()
                .into(profileImage);
        
        Glide.with(this)
                .load("https://images.unsplash.com/photo-1507842217343-583bb7270b66?q=80&w=1000&auto=format&fit=crop")
                .centerCrop()
                .into(tutorBanner);

        btnBookTrial.setOnClickListener(v -> showBookingBottomSheet(true));
        btnBookLesson.setOnClickListener(v -> showBookingBottomSheet(false));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private String getShortSubjectInfo(Tutor.SubjectPreference pref) {
        return String.format(Locale.getDefault(), "%s %s%d • %dm", 
                pref.name, pref.currency, pref.price, pref.duration);
    }

    private void showBookingBottomSheet(boolean isTrial) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_booking_bottom_sheet, null);
        dialog.setContentView(sheetView);

        // Bind Sheet Views
        TextView tvTitle = sheetView.findViewById(R.id.tvSheetTitle);
        ChipGroup chipGroup = sheetView.findViewById(R.id.sheet_subjects_chips);
        MaterialButtonToggleGroup toggleType = sheetView.findViewById(R.id.toggleLessonType);
        View layoutSingle = sheetView.findViewById(R.id.layoutSingleLesson);
        View layoutPackage = sheetView.findViewById(R.id.layoutPackage);
        
        MaterialButton btnDate = sheetView.findViewById(R.id.btnSheetDate);
        MaterialButton btnTime = sheetView.findViewById(R.id.btnSheetTime);
        
        TextView tvQty = sheetView.findViewById(R.id.tvQuantity);
        MaterialButton btnPlus = sheetView.findViewById(R.id.btnPlus);
        MaterialButton btnMinus = sheetView.findViewById(R.id.btnMinus);
        CheckBox cbProDiscount = sheetView.findViewById(R.id.cbProDiscount);
        ChipGroup groupDays = sheetView.findViewById(R.id.groupRecurringDays);
        RecyclerView rvSlots = sheetView.findViewById(R.id.rvWeeklySlots);
        TextView tvDurationInfo = sheetView.findViewById(R.id.tvPackageDurationInfo);

        TextView tvSummarySubjLine = sheetView.findViewById(R.id.tvSummarySubjectLine);
        TextView tvSummarySubtotal = sheetView.findViewById(R.id.tvSummarySubtotal);
        TextView tvSummaryFees = sheetView.findViewById(R.id.tvSummaryFees);
        TextView tvSummaryTotal = sheetView.findViewById(R.id.tvSummaryTotal);
        View layoutDiscount = sheetView.findViewById(R.id.layoutDiscount);
        TextView tvSummaryDiscount = sheetView.findViewById(R.id.tvSummaryDiscount);
        
        CheckBox cbPolicy = sheetView.findViewById(R.id.cbPolicy);
        MaterialButton btnConfirm = sheetView.findViewById(R.id.btnSheetConfirm);

        // Initial State
        selectedPref = null;
        selectedSubjectName = null;
        isPackageMode = false;
        packageQuantity = 12;
        packageStartDateMs = -1;
        weeklySlots.clear();
        tvQty.setText(String.valueOf(packageQuantity));
        
        tvTitle.setText(isTrial ? "Book Trial Lesson" : "Book Regular Lesson");
        toggleType.setVisibility(isTrial ? View.GONE : View.VISIBLE);
        layoutSingle.setVisibility(View.VISIBLE);
        layoutPackage.setVisibility(View.GONE);
        cbPolicy.setText("I agree to tutor's 24-hour Cancellation Policy");

        // Populate Subjects in Sheet
        if (tutor.getPreferences() != null) {
            for (int i = 0; i < tutor.getPreferences().size(); i++) {
                Tutor.SubjectPreference pref = tutor.getPreferences().get(i);
                Chip chip = new Chip(this);
                chip.setText(getShortSubjectInfo(pref));
                chip.setCheckable(true);
                chip.setClickable(true);
                chip.setChipBackgroundColor(createChipColorStateList(SUBJECT_COLORS[i % SUBJECT_COLORS.length]));
                chip.setTextColor(Color.WHITE);
                chip.setChipStrokeWidth(0f);
                chip.setId(View.generateViewId());
                chipGroup.addView(chip);
            }
        }

        // Setup Recurring Day Chips
        for (String day : DAYS) {
            Chip chip = new Chip(this);
            chip.setText(day);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChipBackgroundColor(createChipColorStateList("#10B981"));
            chip.setTextColor(Color.WHITE);
            chip.setId(View.generateViewId());
            groupDays.addView(chip);
        }

        rvSlots.setLayoutManager(new LinearLayoutManager(this));
        weeklySlotAdapter = new WeeklySlotAdapter(weeklySlots, getSupportFragmentManager());
        rvSlots.setAdapter(weeklySlotAdapter);

        Runnable updateSummary = () -> {
            if (selectedPref == null) {
                tvSummarySubjLine.setText("No subject selected");
                tvSummarySubtotal.setText("$0.00");
                tvSummaryFees.setText("$0.00");
                tvSummaryTotal.setText("$0.00");
                btnConfirm.setText("Secure Payment & Confirm Booking");
                return;
            }

            int count = isPackageMode ? packageQuantity : 1;
            // CORRECTED MONEY LOGIC: Price in DB is for the specified duration (e.g. $60 for 90m)
            double subtotal = selectedPref.price * count;
            if (isTrial) subtotal = 0;

            double discount = (isPackageMode && cbProDiscount.isChecked()) ? subtotal * 0.05 : 0;
            double fees = (subtotal - discount) * 0.05; 
            double total = subtotal - discount + fees;

            String currency = selectedPref.currency != null ? selectedPref.currency : "$";
            // Summary format matching image: "Subject (90m) x 1: $90.00"
            tvSummarySubjLine.setText(String.format(Locale.getDefault(), "%s (%dm) x %d", 
                    selectedPref.name, selectedPref.duration, count));
            
            tvSummarySubtotal.setText(String.format(Locale.getDefault(), "%s%.2f", currency, subtotal));
            
            if (discount > 0) {
                layoutDiscount.setVisibility(View.VISIBLE);
                tvSummaryDiscount.setText(String.format(Locale.getDefault(), "-%s%.2f", currency, discount));
            } else {
                layoutDiscount.setVisibility(View.GONE);
            }

            tvSummaryFees.setText(String.format(Locale.getDefault(), "%s%.2f", currency, fees));
            tvSummaryTotal.setText(String.format(Locale.getDefault(), "Total: %s%.2f", currency, total));
            
            btnConfirm.setText(String.format(Locale.getDefault(), "Secure Payment & Confirm Booking (%s%.2f)", currency, total));

            if (isPackageMode && !weeklySlots.isEmpty()) {
                int slotsPerWeek = weeklySlots.size();
                int weeksNeeded = (int) Math.ceil((double) packageQuantity / slotsPerWeek);
                tvDurationInfo.setText(String.format(Locale.getDefault(), "These %d weekly slots will be booked for %d weeks to complete your %d-lesson package.", slotsPerWeek, weeksNeeded, packageQuantity));
            }
        };

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int index = -1;
                View checkedChip = sheetView.findViewById(checkedIds.get(0));
                for (int i = 0; i < group.getChildCount(); i++) {
                    if (group.getChildAt(i) == checkedChip) { index = i; break; }
                }
                if (index != -1) {
                    selectedPref = tutor.getPreferences().get(index);
                    selectedSubjectName = selectedPref.name;
                    if (isTrial) checkTrialAvailability(selectedSubjectName, btnConfirm);
                    else { btnConfirm.setEnabled(true); btnConfirm.setAlpha(1.0f); }
                }
            } else {
                selectedPref = null;
                selectedSubjectName = null;
            }
            updateSummary.run();
        });

        toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                isPackageMode = (checkedId == R.id.btnPackage);
                layoutSingle.setVisibility(isPackageMode ? View.GONE : View.VISIBLE);
                layoutPackage.setVisibility(isPackageMode ? View.VISIBLE : View.GONE);
                updateSummary.run();
            }
        });

        btnPlus.setOnClickListener(v -> { packageQuantity++; tvQty.setText(String.valueOf(packageQuantity)); updateSummary.run(); });
        btnMinus.setOnClickListener(v -> { if (packageQuantity > 1) { packageQuantity--; tvQty.setText(String.valueOf(packageQuantity)); updateSummary.run(); } });
        cbProDiscount.setOnCheckedChangeListener((v, c) -> updateSummary.run());

        groupDays.setOnCheckedStateChangeListener((group, checkedIds) -> {
            List<WeeklySlot> newSlots = new ArrayList<>();
            for (Integer id : checkedIds) {
                Chip c = sheetView.findViewById(id);
                String day = c.getText().toString();
                boolean exists = false;
                for (WeeklySlot s : weeklySlots) {
                    if (s.dayName.equals(day)) {
                        newSlots.add(s);
                        exists = true;
                        break;
                    }
                }
                if (!exists) newSlots.add(new WeeklySlot(day));
            }
            weeklySlots.clear();
            weeklySlots.addAll(newSlots);
            // If a global start date was already chosen, compute dates for any new slots
            if (packageStartDateMs != -1) {
                weeklySlotAdapter.setGlobalStartDate(packageStartDateMs);
            } else {
                weeklySlotAdapter.notifyDataSetChanged();
            }
            updateSummary.run();
        });

        MaterialButton btnPackageStartDate = sheetView.findViewById(R.id.btnPackageStartDate);
        btnPackageStartDate.setOnClickListener(v -> showDatePicker(date -> {
            // Convert UTC picker result to local midnight
            java.util.Calendar utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
            utcCal.setTimeInMillis(date);
            java.util.Calendar localCal = java.util.Calendar.getInstance();
            localCal.set(utcCal.get(java.util.Calendar.YEAR),
                         utcCal.get(java.util.Calendar.MONTH),
                         utcCal.get(java.util.Calendar.DAY_OF_MONTH),
                         0, 0, 0);
            localCal.set(java.util.Calendar.MILLISECOND, 0);
            packageStartDateMs = localCal.getTimeInMillis();
            String label = new java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(localCal.getTime());
            btnPackageStartDate.setText(label);
            // Propagate to all selected slots
            weeklySlotAdapter.setGlobalStartDate(packageStartDateMs);
            updateSummary.run();
        }));

        btnDate.setOnClickListener(v -> showDatePicker(date -> { 
            selectedDateMs = date; 
            btnDate.setText(new java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new java.util.Date(date))); 
        }));
        btnTime.setOnClickListener(v -> showTimePicker((h, m) -> { 
            selectedHour = h; 
            selectedMinute = m; 
            btnTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)); 
        }));

        btnConfirm.setOnClickListener(v -> {
            if (selectedSubjectName == null) { Toast.makeText(this, "Select a subject", Toast.LENGTH_SHORT).show(); return; }
            if (!cbPolicy.isChecked()) { Toast.makeText(this, "Please agree to the policy", Toast.LENGTH_SHORT).show(); return; }

            if (isPackageMode) {
                if (packageStartDateMs == -1) { Toast.makeText(this, "Please choose a start date", Toast.LENGTH_SHORT).show(); return; }
                if (weeklySlots.isEmpty()) { Toast.makeText(this, "Select at least one recurring day", Toast.LENGTH_SHORT).show(); return; }
                for (WeeklySlot slot : weeklySlots) {
                    if (slot.hour == -1) {
                        Toast.makeText(this, "Please set a time for all selected days", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                performPackageBooking(dialog);
            } else {
                if (selectedDateMs == -1 || selectedHour == -1) { Toast.makeText(this, "Select date and time", Toast.LENGTH_SHORT).show(); return; }
                long ts = calculateTimestamp(selectedDateMs, selectedHour, selectedMinute);
                if (ts <= System.currentTimeMillis()) {
                    Toast.makeText(this, "Cannot book in the past", Toast.LENGTH_SHORT).show();
                } else {
                    performSingleBooking(selectedSubjectName, ts, isTrial, dialog);
                }
            }
        });

        updateSummary.run();
        dialog.show();
    }

    private void performPackageBooking(BottomSheetDialog dialog) {
        String studentId = FirebaseAuth.getInstance().getUid();
        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("Bookings");
        
        int createdCount = 0;
        int weeksOffset = 0;
        String packageId = UUID.randomUUID().toString();
        
        while (createdCount < packageQuantity) {
            for (WeeklySlot slot : weeklySlots) {
                if (createdCount >= packageQuantity) break;
                
                // slot.startDateMs is already local-midnight for the correct weekday
                Calendar localCal = Calendar.getInstance();
                localCal.setTimeInMillis(slot.startDateMs);
                localCal.set(Calendar.HOUR_OF_DAY, slot.hour);
                localCal.set(Calendar.MINUTE, slot.minute);
                localCal.set(Calendar.SECOND, 0);
                localCal.set(Calendar.MILLISECOND, 0);
                localCal.add(Calendar.WEEK_OF_YEAR, weeksOffset);
                
                DatabaseReference newRef = bookingsRef.push();
                Booking b = new Booking(newRef.getKey(), studentId, tutor.getId(), "", "", selectedSubjectName, localCal.getTimeInMillis());
                b.setPackage(true);
                b.setPackageId(packageId);
                b.setPackageTotalLessons(packageQuantity);
                b.setTutorName(tutor.getName());
                b.setDuration(selectedPref.duration);
                newRef.setValue(b);
                
                createdCount++;
            }
            weeksOffset++;
        }
        
        Toast.makeText(this, "Package Booked Successfully (" + packageQuantity + " lessons)!", Toast.LENGTH_LONG).show();
        dialog.dismiss();
        startActivity(new Intent(this, HomeStudentActivity.class));
        finish();
    }

    private void performSingleBooking(String subject, long timestamp, boolean isTrial, BottomSheetDialog dialog) {
        String studentId = FirebaseAuth.getInstance().getUid();
        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("Bookings");
        
        DatabaseReference newRef = bookingsRef.push();
        Booking newBooking = new Booking(newRef.getKey(), studentId, tutor.getId(), "", "", subject, timestamp);
        newBooking.setStatus(isTrial ? "free_trial_pending" : "pending");
        newBooking.setFree(isTrial);
        newBooking.setTutorName(tutor.getName());
        newBooking.setDuration(selectedPref.duration);

        newRef.setValue(newBooking).addOnSuccessListener(aVoid -> {
            if (isTrial) {
                FirebaseDatabase.getInstance().getReference("FreeLessonsUsed")
                        .child(studentId).child(tutor.getId()).child(subject).setValue(true);
            }
            sendNewBookingNotification(tutor.getId(), subject);
            Toast.makeText(TutorActivity.this, "Booking Successful!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            startActivity(new Intent(TutorActivity.this, HomeStudentActivity.class));
            finish();
        });
    }

    private void showDatePicker(OnDateSelectedListener listener) {
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now());
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setCalendarConstraints(constraintsBuilder.build())
                .build();
        picker.addOnPositiveButtonClickListener(listener::onDateSelected);
        picker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void showTimePicker(OnTimeSelectedListener listener) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(12).setMinute(0)
                .setTitleText("Select Time")
                .build();
        picker.addOnPositiveButtonClickListener(v -> listener.onTimeSelected(picker.getHour(), picker.getMinute()));
        picker.show(getSupportFragmentManager(), "TIME_PICKER");
    }

    private long calculateTimestamp(long dateMs, int hour, int minute) {
        Calendar dateCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        dateCal.setTimeInMillis(dateMs);
        Calendar localCal = Calendar.getInstance(); 
        localCal.set(dateCal.get(Calendar.YEAR), dateCal.get(Calendar.MONTH), dateCal.get(Calendar.DAY_OF_MONTH), hour, minute, 0);
        localCal.set(Calendar.MILLISECOND, 0);
        return localCal.getTimeInMillis();
    }

    private ColorStateList createChipColorStateList(String color) {
        int baseColor = Color.parseColor(color);
        int[][] states = new int[][] {
            new int[] {android.R.attr.state_checked}, 
            new int[] {} 
        };
        int[] colors = new int[] {
            baseColor, 
            Color.argb(128, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)) 
        };
        return new ColorStateList(states, colors);
    }

    private void fetchReviewsByTutorId(String tutorId) {
        if (tutorId == null) return;
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("Reviews").child(tutorId);
        reviewsRv.setLayoutManager(new LinearLayoutManager(this));
        reviewsRv.setAdapter(adapter);

        reviewsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                reviewList.clear();
                float totalRating = 0;
                int count = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Review review = snapshot.getValue(Review.class);
                    if (review != null) {
                        totalRating += review.getRating();
                        count++;
                        fetchStudentName(review);
                    }
                }
                if (count > 0) {
                    float avg = totalRating / count;
                    tvAvgRating.setText(String.format(Locale.US, "%.1f", avg));
                    summaryRatingBar.setRating(avg);
                    tvReviewCount.setText(String.format(Locale.US, "%d %s", count, count == 1 ? "Review" : "Reviews"));
                } else {
                    tvAvgRating.setText("0.0");
                    summaryRatingBar.setRating(0);
                    tvReviewCount.setText("0 Reviews");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void fetchStudentName(Review review) {
        DatabaseReference studentRef = FirebaseDatabase.getInstance().getReference("Users").child("Student").child(review.getStudentId());
        studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) review.setStudentName(snapshot.child("name").getValue(String.class));
                else review.setStudentName("Anonymous");
                if (!reviewList.contains(review)) {
                    reviewList.add(review);
                    adapter.notifyDataSetChanged();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendNewBookingNotification(String targetTutorId, String subject) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(targetTutorId).push();
        Map<String, Object> data = new HashMap<>();
        data.put("title", "New Lesson Booked! 📅");
        data.put("message", "A student booked a lesson for " + subject);
        data.put("timestamp", System.currentTimeMillis());
        data.put("isCall", false);
        notifRef.setValue(data);
    }

    private void checkTrialAvailability(String subject, MaterialButton btn) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseDatabase.getInstance().getReference("FreeLessonsUsed").child(uid).child(tutor.getId()).child(subject)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getValue(Boolean.class)) {
                            Toast.makeText(TutorActivity.this, "Trial already used.", Toast.LENGTH_SHORT).show();
                            btn.setEnabled(false); btn.setAlpha(0.5f);
                        } else {
                            btn.setEnabled(true);
                            btn.setAlpha(1.0f);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    interface OnDateSelectedListener {
        void onDateSelected(long date);
    }

    interface OnTimeSelectedListener {
        void onTimeSelected(int hour, int minute);
    }
}
