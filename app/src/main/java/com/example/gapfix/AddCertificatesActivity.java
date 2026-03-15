package com.example.gapfix;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class AddCertificatesActivity extends AppCompatActivity {

    private Uri fileUri;
    private TextView tvFileName;
    private ProgressBar uploadProgressBar;
    private Button btnUpload;

    private static final String CLOUD_NAME = "dbugqpl3m";
    private static final String UPLOAD_PRESET = "ml_default";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_certificates);

        // 1. Initialize Cloudinary (Safe to call multiple times with this check)
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", CLOUD_NAME);
            MediaManager.init(this, config);
        } catch (IllegalStateException e) {
            // MediaManager already initialized
        }

        tvFileName = findViewById(R.id.tvFileName);
        uploadProgressBar = findViewById(R.id.uploadProgressBar);
        btnUpload = findViewById(R.id.btnUpload);
        Button btnSelectFile = findViewById(R.id.btnSelectFile);

        // 2. File Picker Setup
        ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        fileUri = result.getData().getData();
                        tvFileName.setText("Selected: " + fileUri.getLastPathSegment());
                    }
                }
        );

        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*"); // Allows all file types
            filePickerLauncher.launch(intent);
        });

        btnUpload.setOnClickListener(v -> {
            if (fileUri != null) {
                startCloudinaryUpload();
            } else {
                Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startCloudinaryUpload() {
        uploadProgressBar.setVisibility(View.VISIBLE);
        btnUpload.setEnabled(false);

        MediaManager.get().upload(fileUri)
                .unsigned(UPLOAD_PRESET)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String secureUrl = (String) resultData.get("secure_url");
                        saveUrlToFirebase(secureUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        uploadProgressBar.setVisibility(View.GONE);
                        btnUpload.setEnabled(true);
                        Toast.makeText(AddCertificatesActivity.this,
                                "Upload Failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void saveUrlToFirebase(String url) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Using "Tutor" to match your Firebase Rules logic
        FirebaseDatabase.getInstance().getReference("Users")
                .child("Tutor")
                .child(user.getUid())
                .child("certificates")
                .push()
                .setValue(url)
                .addOnSuccessListener(aVoid -> {
                    uploadProgressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    uploadProgressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    Toast.makeText(this, "DB Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}