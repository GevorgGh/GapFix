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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class AddCertificatesActivity extends AppCompatActivity {

    private Uri fileUri;
    private TextView tvFileName;
    private ProgressBar uploadProgressBar;
    private String uploadedUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_certificates);

        tvFileName = findViewById(R.id.tvFileName);
        uploadProgressBar = findViewById(R.id.uploadProgressBar);
        Button btnSelectFile = findViewById(R.id.btnSelectFile);
        Button btnUpload = findViewById(R.id.btnUpload);

        ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        fileUri = result.getData().getData();
                        tvFileName.setText("File Selected: " + fileUri.getLastPathSegment());
                    }
                }
        );

        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            filePickerLauncher.launch(intent);
        });

        btnUpload.setOnClickListener(v -> {
            if (fileUri != null) {
                uploadProgressBar.setVisibility(View.VISIBLE);
                btnUpload.setEnabled(false); // Prevent double clicks
                uploadFileToFirebase(fileUri);
            } else {
                Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadFileToFirebase(Uri uri) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("certificates")
                .child(uid)
                .child(System.currentTimeMillis() + "_cert");

        storageRef.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        uploadedUrl = downloadUri.toString();
                        saveUrlToDatabase(uploadedUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    uploadProgressBar.setVisibility(View.GONE);
                    findViewById(R.id.btnUpload).setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUrlToDatabase(String url) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference("Users")
                .child("Teacher")
                .child(uid)
                .child("certificates")
                .push()
                .setValue(url)
                .addOnSuccessListener(aVoid -> {
                    uploadProgressBar.setVisibility(View.GONE);
                    startActivity(new Intent(this, HomeActivity.class));
                    Toast.makeText(this, "Certificate Uploaded!", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}