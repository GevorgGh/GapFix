package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class BookFreeLessonActivity extends AppCompatActivity {

    private String selectedSubject;
    private long selectedDateMs = -1;
    private int selectedHour = -1;
    private int selectedMinute = -1;

    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_free_lesson);

        mDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 1. Get Tutor from Intent
        Tutor tutor = (Tutor) getIntent().getSerializableExtra("tutor");
        if (tutor == null) {
            finish();
            return;
        }

        // Use tutor.getName() if you don't have a specific ID field yet,
        // but ideally use tutor.getId()
        String tutorId = tutor.getId();
        String studentId = mAuth.getCurrentUser().getUid();

        // 2. Set UI Elements
        TextView tutorName = findViewById(R.id.tutor_name);
        tutorName.setText(tutor.getName());

        ImageView tutorImage = findViewById(R.id.tutor_image);
        Glide.with(this)
                .load(tutor.getImageResourceLink() != null ? tutor.getImageResourceLink() : R.drawable.person_circle)
                .placeholder(R.drawable.person_circle)
                .circleCrop()
                .into(tutorImage);

        // 3. Handle the NEW SubjectPreference structure
        ChipGroup tutorSubjectsGroup = findViewById(R.id.tutor_subjects_chips);
        tutorSubjectsGroup.removeAllViews();

        if (tutor.getPreferences() != null) {
            for (Tutor.SubjectPreference pref : tutor.getPreferences()) {
                Chip chip = new Chip(this);
                // Show name and price so the student knows the cost
                String chipLabel = String.format("%s (%s%d)", pref.name, pref.currency, pref.price);
                chip.setText(chipLabel);

                chip.setCheckable(true);
                chip.setClickable(true);
                // Add a unique ID for the listener to find it
                chip.setId(View.generateViewId());

                tutorSubjectsGroup.addView(chip);
            }
        }

        tutorSubjectsGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip selectedChip = findViewById(checkedIds.get(0));
                // We extract just the subject name (everything before the parenthesis)
                String fullText = selectedChip.getText().toString();
                selectedSubject = fullText.split(" \\(")[0];
            } else {
                selectedSubject = null;
            }
        });

        // 4. Setup Date/Time Pickers (Keep your existing logic)
        MaterialButton btnSelectDate = findViewById(R.id.btnSelectDate);
        MaterialButton btnSelectTime = findViewById(R.id.btnSelectTime);
        MaterialButton btnBook = findViewById(R.id.btnConfirmBooking);

        setupPickers(btnSelectDate, btnSelectTime);

        // 5. Booking Action
        btnBook.setOnClickListener(v -> {
            if (selectedSubject == null) {
                Toast.makeText(this, "Please select a subject", Toast.LENGTH_SHORT).show();
            } else if (selectedDateMs == -1 || selectedHour == -1) {
                Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show();
            } else if (!isDateTimeValid(selectedDateMs, selectedHour, selectedMinute)) {
                Toast.makeText(this, "Bookings must be at least 30 minutes from now", Toast.LENGTH_LONG).show();
            } else {
                // Pass formatted strings to Firebase
                String date = btnSelectDate.getText().toString();
                String time = btnSelectTime.getText().toString();
                bookLesson(selectedSubject, date, time, tutorId, studentId);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupPickers(MaterialButton btnDate, MaterialButton btnTime) {
        // Date Picker logic
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now());

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Lesson Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraintsBuilder.build())
                .build();

        btnDate.setOnClickListener(v -> datePicker.show(getSupportFragmentManager(), "DATE_PICKER"));
        datePicker.addOnPositiveButtonClickListener(selection -> {
            selectedDateMs = selection;
            btnDate.setText(datePicker.getHeaderText());
        });

        // Time Picker logic
        btnTime.setOnClickListener(v -> {
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(12).setMinute(0)
                    .setTitleText("Select Lesson Time")
                    .build();

            timePicker.addOnPositiveButtonClickListener(tp -> {
                selectedHour = timePicker.getHour();
                selectedMinute = timePicker.getMinute();
                String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                btnTime.setText(formattedTime);
            });
            timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
        });
    }

    private boolean isDateTimeValid(long dateMs, int hour, int minute) {
        Calendar selectedCal = Calendar.getInstance();
        selectedCal.setTimeInMillis(dateMs);
        selectedCal.set(Calendar.HOUR_OF_DAY, hour);
        selectedCal.set(Calendar.MINUTE, minute);

        Calendar minAllowed = Calendar.getInstance();
        minAllowed.add(Calendar.MINUTE, 30);
        return selectedCal.after(minAllowed);
    }

    public void bookLesson(String subject, String date, String time, String tId, String sId) {
        // 1. Reference to the Bookings node
        DatabaseReference bookingsRef = mDatabase.getReference("Bookings");

        // 2. Query to check if this student has ever booked THIS tutor before
        bookingsRef.orderByChild("studentId").equalTo(sId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean hasPreviousLesson = false;

                        for (DataSnapshot data : snapshot.getChildren()) {
                            Booking existingBooking = data.getValue(Booking.class);
                            // Check if the tutorId matches in any of the student's previous bookings
                            if (existingBooking != null && tId.equals(existingBooking.getTutorId()) && existingBooking.getStatus().equals("done")) {
                                hasPreviousLesson = true;
                                break;
                            }
                        }

                        // 3. Create the new booking
                        DatabaseReference newBookingRef = bookingsRef.push();
                        String bId = newBookingRef.getKey();

                        Booking newBooking = new Booking(bId, sId, tId, date, time, subject);

                        if (!hasPreviousLesson) {
                            // First time: Mark as Free Trial or set price to 0
                            newBooking.setStatus("free_trial_pending");
                            // If your Booking model has a price field: newBooking.setPrice(0.0);
                            Toast.makeText(BookFreeLessonActivity.this, "First lesson! Applying Free Trial.", Toast.LENGTH_SHORT).show();
                        } else {
                            // Not the first time
                            newBooking.setStatus("pending");
                        }

                        // 4. Save to Firebase
                        newBookingRef.setValue(newBooking)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(BookFreeLessonActivity.this, "Booking sent!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(BookFreeLessonActivity.this, HomeStudentActivity.class));
                                })
                                .addOnFailureListener(e -> Toast.makeText(BookFreeLessonActivity.this, "Failed to book", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Error checking history: " + error.getMessage());
                    }
                });
    }}