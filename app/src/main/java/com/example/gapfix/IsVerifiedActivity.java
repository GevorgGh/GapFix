package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class IsVerifiedActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_is_verified);

        mAuth = FirebaseAuth.getInstance();
        role = getIntent().getStringExtra("ROLE");
        
        Log.d("IsVerified", "Role received: " + role);

        Button btnVerified = findViewById(R.id.btnVerified);
        TextView btnResend = findViewById(R.id.btnResend);

        btnVerified.setOnClickListener(v -> {
            Log.d("IsVerified", "Verified button clicked");
            checkEmailVerification();
        });

        btnResend.setOnClickListener(v -> {
            Log.d("IsVerified", "Resend button clicked");
            resendVerificationEmail();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void checkEmailVerification() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (user.isEmailVerified()) {
                        Log.d("IsVerified", "Email is verified, redirecting...");
                        redirectBasedOnRole();
                    } else {
                        Log.d("IsVerified", "Email NOT verified yet");
                        Toast.makeText(this, "Email is not verified yet. Please check your inbox.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("IsVerified", "User reload failed", task.getException());
                    Toast.makeText(this, "Failed to refresh user status: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e("IsVerified", "No current user found");
        }
    }

    private void resendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d("IsVerified", "Verification email sent");
                    Toast.makeText(this, "Verification email sent.", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("IsVerified", "Send verification email failed", task.getException());
                    Toast.makeText(this, "Failed to send email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void redirectBasedOnRole() {
        Intent intent;
        if ("Student".equals(role)) {
            intent = new Intent(IsVerifiedActivity.this, StudentPreferences.class);
        } else {
            intent = new Intent(IsVerifiedActivity.this, TutorSubjectActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
