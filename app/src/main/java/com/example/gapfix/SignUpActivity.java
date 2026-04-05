package com.example.gapfix;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private EditText emailField, passField, repeatPassField, dateField, nameField;
    private Calendar calendar;
    private SimpleDateFormat simpleDateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);


//        getSupportFragmentManager()
//                .beginTransaction()
//                .replace(R.id.backFr, new BackFragment())
//                .commit();

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        emailField = findViewById(R.id.editEmail);
        passField = findViewById(R.id.editPassword);
        repeatPassField = findViewById(R.id.editPassword2);
        nameField = findViewById(R.id.editName);
        dateField = findViewById(R.id.editTextDate);

        View.OnFocusChangeListener focusListener = View::setSelected;

        emailField.setOnFocusChangeListener(focusListener);
        passField.setOnFocusChangeListener(focusListener);
        repeatPassField.setOnFocusChangeListener(focusListener);
        nameField.setOnFocusChangeListener(focusListener);
        dateField.setOnFocusChangeListener(focusListener);

        calendar = Calendar.getInstance();
        simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        dateField.setFocusableInTouchMode(true);

        dateField.setOnClickListener(v -> showDatePicker());

        dateField.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showDatePicker();
            }
        });

        final String selectedRole = getIntent().getStringExtra("ROLE");

        Button reg = findViewById(R.id.button3);

        reg.setOnClickListener(view -> {
            String email = emailField.getText().toString().trim();
            String pass = passField.getText().toString().trim();
            String repeatPass = repeatPassField.getText().toString().trim();
            String name = nameField.getText().toString().trim();
            String dateStr = dateField.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty() || name.isEmpty() || dateStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(repeatPass)) {
                repeatPassField.setError("Passwords do not match");
                return;
            }

            try {
                calendar.setTime(Objects.requireNonNull(simpleDateFormat.parse(dateStr)));
                if (!isOldEnough(calendar)) {
                    Toast.makeText(this, "You must be at least 12 years old", Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (ParseException e) {
                dateField.setError("Invalid date format (dd-mm-yyyy)");
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

        Calendar maxDateCalendar = Calendar.getInstance();
        maxDateCalendar.add(Calendar.YEAR, -12);

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, R.style.MyDatePickerTheme,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%d", selectedDay, (selectedMonth + 1), selectedYear);
                    dateField.setText(date);
                }, year, month, day);

        datePickerDialog.getDatePicker().setMaxDate(maxDateCalendar.getTimeInMillis());

        datePickerDialog.show();
    }

    private boolean isOldEnough(Calendar dob) {
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age >= 12;
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

                            firebaseUser.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                                if (verifyTask.isSuccessful()) {
                                    Intent intent;
                                    if ("Student".equals(role)){
                                        Log.d("SignUp", role);
                                        intent = new Intent(SignUpActivity.this, StudentPreferences.class);
                                    } else {
                                        intent = new Intent(SignUpActivity.this, TutorSubjectActivity.class);
                                    }
                                    startActivity(intent);
                                    finish();
                                }
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

        String cleanUsername = name.replaceAll("\\s+", "").toLowerCase();
        mDatabase.child("usernames").child(cleanUsername).setValue(email);
    }
}