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
import java.util.Map;
import java.util.TimeZone;

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

        Tutor tutor = (Tutor) getIntent().getSerializableExtra("tutor");
        if (tutor == null) {
            finish();
            return;
        }

        String tutorId = tutor.getId();
        String studentId = mAuth.getCurrentUser().getUid();

        TextView tutorName = findViewById(R.id.tutor_name);
        tutorName.setText(tutor.getName());

        ImageView tutorImage = findViewById(R.id.tutor_image);
        Glide.with(this)
                .load(tutor.getImageResourceLink() != null ? tutor.getImageResourceLink() : R.drawable.person_circle)
                .placeholder(R.drawable.person_circle)
                .circleCrop()
                .into(tutorImage);

        ChipGroup tutorSubjectsGroup = findViewById(R.id.tutor_subjects_chips);
        tutorSubjectsGroup.removeAllViews();

        if (tutor.getPreferences() != null) {
            for (Tutor.SubjectPreference pref : tutor.getPreferences()) {
                Chip chip = new Chip(this);
                String chipLabel = String.format("%s (%s%d)", pref.name, pref.currency, pref.price);
                chip.setText(chipLabel);
                chip.setCheckable(true);
                chip.setClickable(true);
                chip.setId(View.generateViewId());
                tutorSubjectsGroup.addView(chip);
            }
        }

        tutorSubjectsGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip selectedChip = findViewById(checkedIds.get(0));
                String fullText = selectedChip.getText().toString();
                selectedSubject = fullText.split(" \\(")[0];
            } else {
                selectedSubject = null;
            }
        });

        MaterialButton btnSelectDate = findViewById(R.id.btnSelectDate);
        MaterialButton btnSelectTime = findViewById(R.id.btnSelectTime);
        MaterialButton btnBook = findViewById(R.id.btnConfirmBooking);

        setupPickers(btnSelectDate, btnSelectTime);

        btnBook.setOnClickListener(v -> {
            if (selectedSubject == null) {
                Toast.makeText(this, "Please select a subject", Toast.LENGTH_SHORT).show();
            } else if (selectedDateMs == -1 || selectedHour == -1) {
                Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show();
            } else {
                long finalTimestamp = calculateTimestamp(selectedDateMs, selectedHour, selectedMinute);
                
                if (finalTimestamp < System.currentTimeMillis() + (1 * 60_000L)) {
                    Toast.makeText(this, "Bookings must be at least 30 minutes from now", Toast.LENGTH_LONG).show();
                } else {
                    String dateStr = btnSelectDate.getText().toString();
                    String timeStr = btnSelectTime.getText().toString();
                    bookLesson(selectedSubject, dateStr, timeStr, finalTimestamp, tutorId, studentId);
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupPickers(MaterialButton btnDate, MaterialButton btnTime) {
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

    private long calculateTimestamp(long dateMs, int hour, int minute) {
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(dateMs);
        int year = utcCal.get(Calendar.YEAR);
        int month = utcCal.get(Calendar.MONTH);
        int day = utcCal.get(Calendar.DAY_OF_MONTH);

        Calendar localCal = Calendar.getInstance(); 
        localCal.set(year, month, day, hour, minute, 0);
        localCal.set(Calendar.MILLISECOND, 0);
        
        return localCal.getTimeInMillis();
    }

    public void bookLesson(String subject, String date, String time, long timestamp, String tId, String sId) {
        DatabaseReference bookingsRef = mDatabase.getReference("Bookings");

        bookingsRef.orderByChild("studentId").equalTo(sId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean hasPreviousLesson = false;
                        boolean hasActiveBooking = false;

                        for (DataSnapshot data : snapshot.getChildren()) {
                            Booking existingBooking = data.getValue(Booking.class);
                            if (existingBooking != null && tId.equals(existingBooking.getTutorId())) {
                                String status = existingBooking.getStatus();
                                
                                if ("done".equals(status) || "finished".equals(status)) {
                                    hasPreviousLesson = true;
                                }
                                
                                // Logic: Cannot book if there's an active booking (pending, confirmed, etc.)
                                if ("pending".equals(status) || "free_trial_pending".equals(status) || "confirmed".equals(status)) {
                                    hasActiveBooking = true;
                                }
                            }
                        }

                        if (hasActiveBooking) {
                            Toast.makeText(BookFreeLessonActivity.this, "You already have a pending or active lesson with this tutor.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        DatabaseReference newBookingRef = bookingsRef.push();
                        String bId = newBookingRef.getKey();

                        Booking newBooking = new Booking(bId, sId, tId, date, time, subject, timestamp);

                        if (!hasPreviousLesson) {
                            newBooking.setStatus("free_trial_pending");
                        } else {
                            newBooking.setStatus("pending");
                        }

                        newBookingRef.setValue(newBooking)
                                .addOnSuccessListener(aVoid -> {
                                    sendNewBookingNotification(tId, subject); // Send notification to Tutor
                                    Toast.makeText(BookFreeLessonActivity.this, "Booking sent!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(BookFreeLessonActivity.this, HomeStudentActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(BookFreeLessonActivity.this, "Failed to book", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Error checking history: " + error.getMessage());
                    }
                });
    }

    private void sendNewBookingNotification(String targetTutorId, String subject) {
        DatabaseReference notifRef = mDatabase.getReference("Notifications").child(targetTutorId).push();
        Map<String, Object> data = new HashMap<>();
        data.put("title", "New Lesson Booked! 📅");
        data.put("message", "A student has booked a new lesson for " + subject + ".");
        data.put("timestamp", System.currentTimeMillis());
        data.put("isCall", false); // Distinguish from calling
        notifRef.setValue(data);
    }
}
