package com.example.gapfix;

import android.app.DatePickerDialog;
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


        getSupportFragmentManager().beginTransaction()
                .replace(R.id.backFr, new BackFragment()).commit();

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        emailField = findViewById(R.id.editEmail);
        passField = findViewById(R.id.editPassword);
        repeatPassField = findViewById(R.id.editPassword2);
        nameField = findViewById(R.id.editName);
        dateField = findViewById(R.id.editTextDate);

        View.OnFocusChangeListener focusListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                v.setSelected(hasFocus);
            }
        };

        emailField.setOnFocusChangeListener(focusListener);
        passField.setOnFocusChangeListener(focusListener);
        repeatPassField.setOnFocusChangeListener(focusListener);
        nameField.setOnFocusChangeListener(focusListener);
        dateField.setOnFocusChangeListener(focusListener);

        calendar = Calendar.getInstance();
        simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

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
        // Create the dialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                SignUpActivity.this,
                R.style.MyDatePickerTheme,
                (view, year, month, dayOfMonth) -> {
                    // Update calendar object
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // Force the text into the field immediately
                    String selectedDate = simpleDateFormat.format(calendar.getTime());
                    dateField.setText(selectedDate);

                    dateField.clearFocus();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, "Done", datePickerDialog);
        datePickerDialog.show();
    }

    private boolean isOldEnough(Calendar dob) {
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

        // Adjust age if birthday hasn't happened yet this year
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
                            saveUserInfo(firebaseUser.getUid(), name, email, role, dob);
                            firebaseUser.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                                if (verifyTask.isSuccessful()) {
                                    Toast.makeText(this, "Registration successful! Check email.", Toast.LENGTH_LONG).show();
                                    mAuth.signOut();
                                    finish();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserInfo(String userId, String name, String email, String role, String dob) {
        User userProfile = new User(name, email, role);
        userProfile.setDob(dob);

        mDatabase.child("Users").child(role).child(userId).setValue(userProfile);

        String cleanUsername = name.replaceAll("\\s+", "").toLowerCase();
        mDatabase.child("usernames").child(cleanUsername).setValue(email);
    }
}