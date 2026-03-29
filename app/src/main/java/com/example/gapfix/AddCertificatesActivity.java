package com.example.gapfix;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
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

    private Uri certUri, profileUri;
    private TextView tvFileName, tvProfileName;
    private ImageView profileImageView;
    private ProgressBar uploadProgressBar;
    private Button btnUpload;
    private int uploadCount = 0;
    private static final String UPLOAD_PRESET = "ml_default";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_certificates);

        initCloudinary();

        tvFileName = findViewById(R.id.tvFileName);
        tvProfileName = findViewById(R.id.tvProfileName);
        profileImageView = findViewById(R.id.profileImageView);
        uploadProgressBar = findViewById(R.id.uploadProgressBar);
        btnUpload = findViewById(R.id.btnBookLesson);
        Button btnSelectCert = findViewById(R.id.btnSelectFile);
        Button btnSelectProfile = findViewById(R.id.btnSelectProfile);

        ActivityResultLauncher<Intent> certPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        certUri = result.getData().getData();
                        tvFileName.setText("Cert Selected: " + (certUri != null ? certUri.getLastPathSegment() : ""));
                    }
                }
        );

        btnSelectCert.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
            certPicker.launch(intent);
        });

        btnSelectProfile.setOnClickListener(v -> showImageSourceDialog());

        btnUpload.setOnClickListener(v -> {
            if (certUri != null && profileUri != null) {
                uploadProgressBar.setVisibility(View.VISIBLE);
                btnUpload.setEnabled(false);
                startUpload(certUri, "certificates");
                startUpload(profileUri, "profilePicture");
            } else {
                Toast.makeText(this, "Please select both files", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showImageSourceDialog() {
        String[] options = {"Camera", "Gallery"};
        new AlertDialog.Builder(this)
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) takePicture.launch(null);
                    else pickMedia.launch(new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
                }).show();
    }

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    profileUri = uri;
                    profileImageView.setImageURI(uri);
                    tvProfileName.setText("Profile Pic Selected");
                }
            });

    private final ActivityResultLauncher<Void> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    profileImageView.setImageBitmap(bitmap);
                    tvProfileName.setText("Photo Taken");
                }
            });

    private void startUpload(Uri uri, String dbKey) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String folderPath = "Users/" + user.getUid();

        MediaManager.get().upload(uri)
                .unsigned(UPLOAD_PRESET)
                .option("folder", folderPath)
                .callback(new UploadCallback() {
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        saveUrlToFirebase((String) resultData.get("secure_url"), dbKey);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> {
                            btnUpload.setEnabled(true);
                            uploadProgressBar.setVisibility(View.GONE);
                            Log.e("Cloudinary", "Upload Error: " + error.getDescription());
                            Toast.makeText(AddCertificatesActivity.this, "Upload Error", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveUrlToFirebase(String url, String key) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseDatabase.getInstance().getReference("Users")
                .child("Tutor")
                .child(user.getUid())
                .child(key)
                .setValue(url)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        uploadCount++;

                        if (uploadCount == 2) {
                            uploadProgressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "All files uploaded successfully!", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(AddCertificatesActivity.this, AddBioActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        btnUpload.setEnabled(true);
                        uploadProgressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Firebase Error: " + key, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void initCloudinary() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dbugqpl3m");
            MediaManager.init(this, config);
        } catch (Exception ignored) {}
    }
}