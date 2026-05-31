package com.example.gapfix;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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
    private void loginUser(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.err_fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            GapFixApplication.fetchTokenAndLogin(user.getUid());
                            checkDatabaseForProfile(user.getUid());
                        }
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, getString(R.string.err_login_failed, error), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void checkDatabaseForProfile(String uid) {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(tokenTask -> {
                    String freshToken = tokenTask.isSuccessful() ? tokenTask.getResult() : null;
                    db.child("Users").child("Student").child(uid).get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            Boolean isComplete = task.getResult().child("isComplete").getValue(Boolean.class);
                            if (isComplete != null && isComplete) {
                                if (freshToken != null) {
                                    db.child("Users").child("Student").child(uid).child("fcmToken").setValue(freshToken);
                                }
                                editor.putString("user_role", "Student").apply();
                                startActivity(new Intent(LoginActivity.this, HomeStudentActivity.class));
                                finishAffinity();
                            } else {
                                editor.putString("user_role", "Student").apply();
                                startActivity(new Intent(LoginActivity.this, StudentPreferences.class));
                                finishAffinity();
                            }
                        } else {
                            db.child("Users").child("Tutor").child(uid).get().addOnCompleteListener(tutorTask -> {
                                if (tutorTask.isSuccessful() && tutorTask.getResult().exists()) {
                                    Boolean isComplete = tutorTask.getResult().child("isComplete").getValue(Boolean.class);
                                    if (isComplete != null && isComplete) {
                                        if (freshToken != null) {
                                            db.child("Users").child("Tutor").child(uid).child("fcmToken").setValue(freshToken);
                                        }
                                        editor.putString("user_role", "Tutor").apply();
                                        startActivity(new Intent(LoginActivity.this, HomeTutorActivity.class));
                                        finishAffinity();
                                    } else {
                                        editor.putString("user_role", "Tutor").apply();
                                        startActivity(new Intent(LoginActivity.this, TutorSubjectActivity.class));
                                        finishAffinity();
                                    }
                                } else {
                                    editor.putString("user_role", "None").apply();
                                    startActivity(new Intent(LoginActivity.this, RoleSelectionActivity.class));
                                    finishAffinity();
                                }
                            });
                        }
                    });
                });
    }
}
