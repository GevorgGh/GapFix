package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    EditText emailField;
    EditText passField;
    EditText nameField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.backFr, new BackFragment())
                .commit();
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        emailField = findViewById(R.id.editEmail);
        passField = findViewById(R.id.editPassword);
        nameField = findViewById(R.id.editName);

        final String selectedRole = getIntent().getStringExtra("ROLE");


        Button reg = findViewById(R.id.button3);

        reg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String emailText = emailField.getText().toString().trim();
                String passText = passField.getText().toString().trim();
                String nameText = nameField.getText().toString().trim();

                if (emailText.isEmpty() || passText.isEmpty() || nameText.isEmpty()) {
                    Toast.makeText(SignUpActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                } else {
                    signUpUser(emailText, passText, nameText, selectedRole);
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void signUpUser(String email, String password, String name, String role) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();

                            saveUserInfo(userId, name, email, role);

                            firebaseUser.sendEmailVerification()
                                    .addOnCompleteListener(verifyTask -> {
                                        if (verifyTask.isSuccessful()) {
                                            Toast.makeText(SignUpActivity.this,
                                                    "Registration successful! Please verify your email.",
                                                    Toast.LENGTH_LONG).show();
                                            mAuth.signOut();
                                            finish();
                                        }
                                    });
                        }
                    } else {
                        emailField.setTextColor(ContextCompat.getColor(this, R.color.error));
                        Toast.makeText(SignUpActivity.this, "Error: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserInfo(String userId, String name, String email, String role) {
        User userProfile = new User(name, email, role);

        mDatabase.child("Users").child(role).child(userId).setValue(userProfile);

        String cleanUsername = name.replaceAll("\\s+", "").toLowerCase();
        mDatabase.child("usernames").child(cleanUsername).setValue(email)
                .addOnFailureListener(e -> {
                    Toast.makeText(SignUpActivity.this, "Failed to reserve username", Toast.LENGTH_SHORT).show();
                });
    }
}