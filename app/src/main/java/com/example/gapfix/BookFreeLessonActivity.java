package com.example.gapfix;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BookFreeLessonActivity extends AppCompatActivity {

    String selectedSubject;

    FirebaseAuth mAuth;
    FirebaseDatabase mDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_free_lesson);

        mDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Tutor tutor = (Tutor) getIntent().getSerializableExtra("tutor");

        String tutorId = tutor.getId();

        String studentId = mAuth.getCurrentUser().getUid();

        TextView tutorName = findViewById(R.id.tutor_name);
        tutorName.setText(tutor.getName());

        ImageView tutorImage = findViewById(R.id.tutor_image);

        if (tutor.getImageResourceLink() != null) {
            Glide.with(this)
                    .load(tutor.getImageResourceLink())
                    .into(tutorImage);
        } else{
            tutorImage.setImageResource(R.drawable.person_circle);
        }

        ChipGroup tutorSubjects = findViewById(R.id.tutor_subjects_chips);

        tutorSubjects.removeAllViews();
        for (String subject : tutor.getSubjects()) {
            Chip chip = new Chip(this);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setCheckable(true);
            chip.setText(subject);
            tutorSubjects.addView(chip);
        }

        tutorSubjects.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int chipId = checkedIds.get(0);
                Chip selectedChip = findViewById(chipId);
                if (selectedChip != null) {
                    selectedSubject = selectedChip.getText().toString();
                }
            } else {
                selectedSubject = null;
            }
        });
        MaterialButton btnSelectDate = findViewById(R.id.btnSelectDate);
        MaterialButton btnSelectTime = findViewById(R.id.btnSelectTime);
        MaterialButton btnBook = findViewById(R.id.btnConfirmBooking);

        MaterialTimePicker timePicker;
        timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
                .setMinute(Calendar.getInstance().get(Calendar.MINUTE))
                .setTitleText("Select Lesson Time")
                .build();

        btnSelectTime.setOnClickListener(v -> {
            timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
        });


        timePicker.addOnPositiveButtonClickListener(v -> {
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
            btnSelectTime.setText(formattedTime);
        });

        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now());

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Lesson Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraintsBuilder.build())
                .setTheme(com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialCalendar)
                .build();

        btnSelectDate.setOnClickListener(v -> {
            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });


        datePicker.addOnPositiveButtonClickListener(selection -> {
            String dateString = datePicker.getHeaderText();
            btnSelectDate.setText(dateString);
        });

        btnBook.setOnClickListener(v -> {
            if (selectedSubject != null) {
                bookLesson(selectedSubject, btnSelectDate.getText().toString(), btnSelectTime.getText().toString(), tutorId, studentId);
            } else {
                Toast.makeText(this, "Please select a subject", Toast.LENGTH_SHORT).show();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void bookLesson(String subject, String date, String time, String tutorId, String studentId) {
        // 1. Reference the "Bookings" node and get a unique key
        DatabaseReference bookingsRef = mDatabase.getReference("Bookings").push();
        String bId = bookingsRef.getKey();

        // 2. Create the booking object
        Booking booking = new Booking(bId, studentId, tutorId, date, time, subject);

        // 3. Save to Firebase using the ID we just generated
        bookingsRef.setValue(booking)
                .addOnSuccessListener(aVoid -> {
                    fetchTutorTokenAndNotify(tutorId, subject);

                    Toast.makeText(this, "Booking successful", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Booking failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchTutorTokenAndNotify(String tutorId, String subject) {
        mDatabase.getReference("Users").child("Tutor").child(tutorId).child("fcmToken")
                .get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String token = snapshot.getValue(String.class);
                        DatabaseReference notifRef = mDatabase.getReference("Notifications").child(tutorId).push();

                        java.util.HashMap<String, String> notifData = new java.util.HashMap<>();
                        notifData.put("title", "New Lesson Request!");
                        notifData.put("message", "A student booked a lesson for: " + subject);
                        notifData.put("timestamp", String.valueOf(System.currentTimeMillis()));
                        notifData.put("fcmToken", token);

                        notifRef.setValue(notifData);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch token: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}