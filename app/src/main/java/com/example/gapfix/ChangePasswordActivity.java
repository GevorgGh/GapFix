package com.example.gapfix;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etOldPassword, etNewPassword, etConfirmNewPassword;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.backFr, new BackFragment())
                .commit();

        etOldPassword = findViewById(R.id.editOldPassword);
        etNewPassword = findViewById(R.id.editNewPassword);
        etConfirmNewPassword = findViewById(R.id.editConfirmNewPassword);
        progressBar = findViewById(R.id.passwordProgress);

        findViewById(R.id.btnUpdatePassword).setOnClickListener(v -> updatePassword());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void updatePassword() {
        String oldPass = etOldPassword.getText().toString();
        String newPass = etNewPassword.getText().toString();
        String confirmPass = etConfirmNewPassword.getText().toString();

        if (oldPass.isEmpty()) {
            Toast.makeText(this, "Please enter your current password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.isEmpty()) {
            Toast.makeText(this, "Please enter a new password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.btnUpdatePassword).setEnabled(false);

        
        user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), oldPass))
                .addOnCompleteListener(reauthTask -> {
                    if (reauthTask.isSuccessful()) {
                        user.updatePassword(newPass).addOnCompleteListener(task -> {
                            progressBar.setVisibility(View.GONE);
                            findViewById(R.id.btnUpdatePassword).setEnabled(true);
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Password Updated Successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(this, "Error: " + (task.getException() != null ? task.getException().getMessage() : "Update failed"), Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        findViewById(R.id.btnUpdatePassword).setEnabled(true);
                        Toast.makeText(this, "Authentication failed: Incorrect old password", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
