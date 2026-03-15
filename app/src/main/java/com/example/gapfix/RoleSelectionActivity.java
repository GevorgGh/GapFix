package com.example.gapfix;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class RoleSelectionActivity extends AppCompatActivity {

    private ConstraintLayout tutorCard, studentCard;
    private EditText dobField;
    private Button continueBtn;
    private String selectedRole = ""; // Stores "Tutor" or "Student"

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_role_selection);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize Views
        tutorCard = findViewById(R.id.tutorCard);
        studentCard = findViewById(R.id.studentCard);
        dobField = findViewById(R.id.editTextDate2);
        continueBtn = findViewById(R.id.button2);

        // Role Selection Logic
        tutorCard.setOnClickListener(v -> selectRole("Tutor"));
        studentCard.setOnClickListener(v -> selectRole("Student"));

        // Date Picker Logic
        dobField.setOnClickListener(v -> showDatePicker());

        // Continue Button Logic
        continueBtn.setOnClickListener(v -> saveUserRoleAndProceed());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void selectRole(String role) {
        selectedRole = role;

        // Update UI visuals
        if (role.equals("Tutor")) {
            tutorCard.setSelected(true);
            studentCard.setSelected(false);
        } else {
            tutorCard.setSelected(false);
            studentCard.setSelected(true);
        }
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String date = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                    dobField.setText(date);
                }, year, month, day);
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
            String uid = user.getUid();

            // Create a user profile map
            Map<String, Object> userUpdates = new HashMap<>();
            userUpdates.put("role", selectedRole);
            userUpdates.put("dob", dob);
            userUpdates.put("email", user.getEmail());
            userUpdates.put("name", user.getDisplayName());

            // Save to: Users -> [Role] -> [UID]
            mDatabase.child("Users").child(selectedRole).child(uid).setValue(userUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (selectedRole.equals("Student"))
                                startActivity(new Intent(RoleSelectionActivity.this, StudentPreferences.class));
                            else
                                startActivity(new Intent(RoleSelectionActivity.this, TutorPreferences.class));
                        } else {
                            Toast.makeText(RoleSelectionActivity.this, "Database Error: " +
                                    task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}