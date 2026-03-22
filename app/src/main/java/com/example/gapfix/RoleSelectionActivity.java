package com.example.gapfix;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RoleSelectionActivity extends AppCompatActivity {

    private ConstraintLayout tutorCard, studentCard;
    private EditText dobField;
    private Button continueBtn;
    private String selectedRole = "";

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_role_selection);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        tutorCard = findViewById(R.id.tutorCard);
        studentCard = findViewById(R.id.studentCard);
        dobField = findViewById(R.id.editTextDate2);
        continueBtn = findViewById(R.id.button2);

        // --- Role Selection Logic ---
        tutorCard.setOnClickListener(v -> selectRole("Tutor"));
        studentCard.setOnClickListener(v -> selectRole("Student"));

        dobField.setOnClickListener(v -> showDatePicker());

        dobField.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showDatePicker();
                v.clearFocus();
            }
        });

        continueBtn.setOnClickListener(v -> saveUserRoleAndProceed());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void selectRole(String role) {
        selectedRole = role;

        tutorCard.setSelected(role.equals("Tutor"));
        studentCard.setSelected(role.equals("Student"));

        if (role.equals("Tutor")) {
            tutorCard.setAlpha(1.0f);
            studentCard.setAlpha(0.5f);
        } else {
            studentCard.setAlpha(1.0f);
            tutorCard.setAlpha(0.5f);
        }
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();

        Calendar maxDateCalendar = Calendar.getInstance();
        maxDateCalendar.add(Calendar.YEAR, -12);

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, R.style.MyDatePickerTheme,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%d", selectedDay, (selectedMonth + 1), selectedYear);
                    dobField.setText(date);
                }, year, month, day);

        datePickerDialog.getDatePicker().setMaxDate(maxDateCalendar.getTimeInMillis());

        datePickerDialog.show();
    }

    private void saveUserRoleAndProceed() {
        String dob = dobField.getText().toString().trim();
        FirebaseUser user = mAuth.getCurrentUser();

        if (selectedRole.isEmpty()) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dob.isEmpty()) {
            Toast.makeText(this, "Please enter your date of birth", Toast.LENGTH_SHORT).show();
            return;
        }

        if (user != null) {
            continueBtn.setEnabled(false); // Prevent double clicks
            String uid = user.getUid();

            Map<String, Object> userUpdates = new HashMap<>();
            userUpdates.put("role", selectedRole);
            userUpdates.put("dob", dob);
            userUpdates.put("email", user.getEmail());
            userUpdates.put("name", user.getDisplayName());

            mDatabase.child("Users").child(selectedRole).child(uid).setValue(userUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Class<?> targetClass = selectedRole.equals("Student") ?
                                    StudentPreferences.class : TutorPreferences.class;
                            startActivity(new Intent(RoleSelectionActivity.this, targetClass));
                            finish(); // Close this activity
                        } else {
                            continueBtn.setEnabled(true);
                            Toast.makeText(RoleSelectionActivity.this, "Error: " +
                                    task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}