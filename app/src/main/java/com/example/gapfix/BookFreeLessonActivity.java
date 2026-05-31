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
    private boolean isTrialRequested = false;
    private FirebaseAuth mAuth;
    private FirebaseDatabase mDatabase;
    private MaterialButton btnBook;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_free_lesson);
        mDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        Tutor tutor = (Tutor) getIntent().getSerializableExtra("tutor");
        isTrialRequested = getIntent().getBooleanExtra("isTrial", false);
        if (tutor == null) {
            finish();
            return;
        }
        String tutorId = tutor.getId();
        String studentId = mAuth.getCurrentUser().getUid();
        TextView titleLabel = findViewById(R.id.tvBookingTitle);
        if (titleLabel != null) {
            titleLabel.setText(isTrialRequested ? "Book Trial Lesson" : "Book Regular Lesson");
        }
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
                if (isTrialRequested) {
                    checkIfTrialAvailable(tutorId, studentId, selectedSubject);
                } else {
                    btnBook.setEnabled(true);
                    btnBook.setAlpha(1.0f);
                }
            } else {
                selectedSubject = null;
            }
        });
        MaterialButton btnSelectDate = findViewById(R.id.btnSelectDate);
        MaterialButton btnSelectTime = findViewById(R.id.btnSelectTime);
        btnBook = findViewById(R.id.btnConfirmBooking);
        if (isTrialRequested) {
            btnBook.setText("Confirm Free Trial");
        } else {
            btnBook.setText("Confirm Booking");
        }
        setupPickers(btnSelectDate, btnSelectTime);
        btnBook.setOnClickListener(v -> {
            if (selectedSubject == null) {
                Toast.makeText(this, "Please select a subject", Toast.LENGTH_SHORT).show();
            } else if (selectedDateMs == -1 || selectedHour == -1) {
                Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show();
            } else {
                long finalTimestamp = calculateTimestamp(selectedDateMs, selectedHour, selectedMinute);
                if (finalTimestamp < System.currentTimeMillis() + (1 * 60_000L)) {
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
    private void checkIfTrialAvailable(String tutorId, String studentId, String subject) {
        mDatabase.getReference("FreeLessonsUsed")
                .child(studentId)
                .child(tutorId)
                .child(subject)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getValue(Boolean.class)) {
                            Toast.makeText(BookFreeLessonActivity.this, 
                                    "You have already used your free trial for " + subject + " with this tutor.", 
                                    Toast.LENGTH_LONG).show();
                            btnBook.setEnabled(false);
                            btnBook.setAlpha(0.5f);
                        } else {
                            btnBook.setEnabled(true);
                            btnBook.setAlpha(1.0f);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
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
        if (isTrialRequested) {
            mDatabase.getReference("FreeLessonsUsed").child(sId).child(tId).child(subject)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.getValue(Boolean.class)) {
                            Toast.makeText(BookFreeLessonActivity.this, "Trial already used for this subject.", Toast.LENGTH_SHORT).show();
                        } else {
                            proceedWithBooking(subject, date, time, timestamp, tId, sId, true);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
        } else {
            proceedWithBooking(subject, date, time, timestamp, tId, sId, false);
        }
    }
    private void proceedWithBooking(String subject, String date, String time, long timestamp, String tId, String sId, boolean isTrial) {
        DatabaseReference bookingsRef = mDatabase.getReference("Bookings");
        DatabaseReference newBookingRef = bookingsRef.push();
        String bId = newBookingRef.getKey();
        Booking newBooking = new Booking(bId, sId, tId, date, time, subject, timestamp);
        Tutor tutor = (Tutor) getIntent().getSerializableExtra("tutor");
        double lessonPrice = 0.0;
        int duration = 0;
        if (tutor != null && tutor.getPreferences() != null) {
            for (Tutor.SubjectPreference pref : tutor.getPreferences()) {
                if (pref.name != null && pref.name.equalsIgnoreCase(subject)) {
                    lessonPrice = pref.price;
                    duration = pref.duration;
                    break;
                }
            }
        }
        newBooking.setFree(isTrial);
        newBooking.setPrice(isTrial ? 0 : lessonPrice);
        if (tutor != null) {
            newBooking.setTutorName(tutor.getName());
        }
        if (isTrial) {
            newBooking.setDuration(30); 
            newBooking.setStatus("free_trial_pending");
        } else {
            if (duration > 0) {
                newBooking.setDuration(duration);
            }
            newBooking.setStatus("pending");
        }
        newBookingRef.setValue(newBooking)
                .addOnSuccessListener(aVoid -> {
                    if (isTrial) {
                        mDatabase.getReference("FreeLessonsUsed").child(sId).child(tId).child(subject).setValue(true);
                    }
                    sendNewBookingNotification(tId, subject);
                    Toast.makeText(BookFreeLessonActivity.this, "Booking sent!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(BookFreeLessonActivity.this, HomeStudentActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(BookFreeLessonActivity.this, "Failed to book", Toast.LENGTH_SHORT).show());
    }
    private void sendNewBookingNotification(String targetTutorId, String subject) {
        DatabaseReference notifRef = mDatabase.getReference("Notifications").child(targetTutorId).push();
        Map<String, Object> data = new HashMap<>();
        data.put("title", "New Lesson Booked! 📅");
        data.put("message", "A student has booked a new lesson for " + subject + ".");
        data.put("timestamp", System.currentTimeMillis());
        data.put("isCall", false);
        notifRef.setValue(data);
    }
}
