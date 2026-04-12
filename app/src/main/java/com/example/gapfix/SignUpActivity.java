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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private EditText emailField, passField, nameField, dateField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        emailField = findViewById(R.id.editEmail);
        passField = findViewById(R.id.editPassword);
        nameField = findViewById(R.id.editName);
        dateField = findViewById(R.id.editTextDate);

        dateField.setOnClickListener(v -> showDatePicker());

        final String selectedRole = getIntent().getStringExtra("ROLE");
        Button reg = findViewById(R.id.button3);

        reg.setOnClickListener(view -> {
            String email = emailField.getText().toString().trim();
            String pass = passField.getText().toString().trim();
            String name = nameField.getText().toString().trim();
            String dateStr = dateField.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty() || name.isEmpty() || dateStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            signUpUser(email, pass, name, selectedRole, dateStr);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, R.style.MyDatePickerTheme,
                (view, year, month, day) -> {
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year);
                    dateField.setText(date);
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void signUpUser(String email, String password, String name, String role, String dob) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                                    .addOnCompleteListener(tokenTask -> {
                                        String token = tokenTask.isSuccessful() ? tokenTask.getResult() : "";
                                        saveUserInfo(firebaseUser.getUid(), name, email, role, dob, token);
                                    });

                            GapFixApplication.fetchTokenAndLogin(firebaseUser.getUid());

                            firebaseUser.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                                Intent intent = "Student".equals(role) ? 
                                    new Intent(SignUpActivity.this, StudentPreferences.class) : 
                                    new Intent(SignUpActivity.this, TutorSubjectActivity.class);
                                startActivity(intent);
                                finish();
                            });
                        }
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserInfo(String userId, String name, String email, String role, String dob, String fcmToken) {
        User userProfile = new User(name, email, role);
        userProfile.setDob(dob);
        userProfile.setFcmToken(fcmToken);
        mDatabase.child("Users").child(role).child(userId).setValue(userProfile);
    }
}
