package com.example.gapfix;

import android.os.Bundle;
import android.util.Log;
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
        if (tutor.getImageResourceLink() != null) {
            Glide.with(this).load(tutor.getImageResourceLink()).into(tutorImage);
        } else {
            tutorImage.setImageResource(R.drawable.person_circle);
        }

        ChipGroup tutorSubjects = findViewById(R.id.tutor_subjects_chips);
        tutorSubjects.removeAllViews();
        for (String subject : tutor.getSubjects()) {
            Chip chip = new Chip(this);
            chip.setText(subject);
            chip.setCheckable(true);
            tutorSubjects.addView(chip);
        }

        tutorSubjects.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip selectedChip = findViewById(checkedIds.get(0));
                selectedSubject = selectedChip.getText().toString();
            } else {
                selectedSubject = null;
            }
        });

        MaterialButton btnSelectDate = findViewById(R.id.btnSelectDate);
        MaterialButton btnSelectTime = findViewById(R.id.btnSelectTime);
        MaterialButton btnBook = findViewById(R.id.btnConfirmBooking);

        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now());

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Lesson Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraintsBuilder.build())
                .build();

        btnSelectDate.setOnClickListener(v -> datePicker.show(getSupportFragmentManager(), "DATE_PICKER"));

        datePicker.addOnPositiveButtonClickListener(selection -> {
            selectedDateMs = selection;
            btnSelectDate.setText(datePicker.getHeaderText());
        });

        btnSelectTime.setOnClickListener(v -> {
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
                    .setMinute(Calendar.getInstance().get(Calendar.MINUTE))
                    .setTitleText("Select Lesson Time")
                    .build();

            timePicker.addOnPositiveButtonClickListener(tp -> {
                selectedHour = timePicker.getHour();
                selectedMinute = timePicker.getMinute();
                String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                btnSelectTime.setText(formattedTime);
            });
            timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
        });

        btnBook.setOnClickListener(v -> {
            if (selectedSubject == null) {
                Toast.makeText(this, "Please select a subject", Toast.LENGTH_SHORT).show();
            } else if (selectedDateMs == -1 || selectedHour == -1) {
                Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show();
            } else if (!isDateTimeValid(selectedDateMs, selectedHour, selectedMinute)) {
                Toast.makeText(this, "Bookings must be at least 30 minutes from now", Toast.LENGTH_LONG).show();
            } else {
                bookLesson(selectedSubject, btnSelectDate.getText().toString(), btnSelectTime.getText().toString(), tutorId, studentId);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private boolean isDateTimeValid(long dateMs, int hour, int minute) {
        Calendar selectedCal = Calendar.getInstance();
        selectedCal.setTimeInMillis(dateMs);
        selectedCal.set(Calendar.HOUR_OF_DAY, hour);
        selectedCal.set(Calendar.MINUTE, minute);
        selectedCal.set(Calendar.SECOND, 0);
        selectedCal.set(Calendar.MILLISECOND, 0);

        Calendar minAllowed = Calendar.getInstance();
        minAllowed.add(Calendar.MINUTE, 30);

        return selectedCal.after(minAllowed);
    }

    public void bookLesson(String subject, String date, String time, String tId, String sId) {
        DatabaseReference bookingsRef = mDatabase.getReference("Bookings").push();
        String bId = bookingsRef.getKey();

        Booking booking = new Booking(bId, sId, tId, date, time, subject);

        bookingsRef.setValue(booking)
                .addOnSuccessListener(aVoid -> {
                    fetchTutorTokenAndNotify(tId, subject);
                    Toast.makeText(this, "Booking successful", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Booking failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void fetchTutorTokenAndNotify(String tutorId, String subject) {
        mDatabase.getReference("Users").child("Tutor").child(tutorId).child("fcmToken")
                .get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String token = snapshot.getValue(String.class);
                        DatabaseReference notifRef = mDatabase.getReference("Notifications").child(tutorId).push();

                        HashMap<String, String> data = new HashMap<>();
                        data.put("title", "New Lesson Request!");
                        data.put("message", "A student booked a lesson for: " + subject);
                        data.put("timestamp", String.valueOf(System.currentTimeMillis()));
                        data.put("fcmToken", token);

                        notifRef.setValue(data);
                    }
                })
                .addOnFailureListener(e -> Log.e("GapFix", "Token fetch failed", e));
    }
}