package com.example.gapfix;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.backFr, new BackFragment())
                .commit();

        EditText emailField = findViewById(R.id.editTextText);
        EditText passField = findViewById(R.id.editTextTextPassword);
        Button login = findViewById(R.id.button);
        TextView forgot = findViewById(R.id.textView8);

        View.OnFocusChangeListener focusListener = (v, hasFocus) -> v.setSelected(hasFocus);
        emailField.setOnFocusChangeListener(focusListener);
        passField.setOnFocusChangeListener(focusListener);

        login.setOnClickListener(view -> loginUser(emailField.getText().toString(), passField.getText().toString()));

        forgot.setOnClickListener(view -> startActivity(new Intent(LoginActivity.this, ForgotPass.class)));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    

    private void checkDatabaseForProfile(String uid) {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        db.child("Users").child("Student").child(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    startActivity(new Intent(LoginActivity.this, HomeStudentActivity.class));
                    finish();
                } else {
                    db.child("Users").child("Tutor").child(uid).get().addOnCompleteListener(tutorTask -> {
                        if (tutorTask.isSuccessful() && tutorTask.getResult().exists()) {
                            startActivity(new Intent(LoginActivity.this, HomeTutorActivity.class));
                            finish();
                        } else {
                            startActivity(new Intent(LoginActivity.this, RoleSelectionActivity.class));
                            finish();
                        }
                    });
                }
            } else {
                Log.e("FirebaseError", "Error: " + task.getException().getMessage());
            }
        });
    }


    private void loginUser(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkDatabaseForProfile(user.getUid());
                        }
                    } else {
                        Toast.makeText(this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}