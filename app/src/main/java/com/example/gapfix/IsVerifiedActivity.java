package com.example.gapfix;
import android.content.Intent;
import android.os.Bundle;
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
        Button btnVerified = findViewById(R.id.btnVerified);
        TextView btnResend = findViewById(R.id.btnResend);
        btnVerified.setOnClickListener(v -> checkEmailVerification());
        btnResend.setOnClickListener(v -> resendVerificationEmail());
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
                        redirectBasedOnRole();
                    } else {
                        Toast.makeText(this, R.string.msg_email_not_verified, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String error = task.getException() != null ? task.getException().getMessage() : "Unknown Error";
                    Toast.makeText(this, getString(R.string.err_refresh_user_failed, error), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    private void resendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, R.string.msg_verification_email_sent_short, Toast.LENGTH_SHORT).show();
                } else {
                    String error = task.getException() != null ? task.getException().getMessage() : "Unknown Error";
                    Toast.makeText(this, getString(R.string.err_send_email_failed, error), Toast.LENGTH_SHORT).show();
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
