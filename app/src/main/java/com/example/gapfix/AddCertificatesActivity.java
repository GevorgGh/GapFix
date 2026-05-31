package com.example.gapfix;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class AddCertificatesActivity extends AppCompatActivity implements CertificateAdapter.OnCertActionListener {
    private Uri certUri;
    private TextView tvFileName;
    private EditText etCertTitle;
    private ProgressBar uploadProgressBar;
    private Button btnAddCert;
    private CertificateAdapter adapter;
    private final List<Map<String, Object>> uploadedCertificates = new ArrayList<>();
    private static final String UPLOAD_PRESET = "ml_default";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_certificates);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.backFr, new BackFragment())
                .commit();
        initCloudinary();
        tvFileName = findViewById(R.id.tvFileName);
        etCertTitle = findViewById(R.id.etCertTitle);
        uploadProgressBar = findViewById(R.id.uploadProgressBar);
        Button btnUpload = findViewById(R.id.btnBookLesson);
        btnAddCert = findViewById(R.id.btnAddCert);
        RecyclerView rvCertificates = findViewById(R.id.rvCertificates);
        TextView tvSkip = findViewById(R.id.tvSkipCert);
        boolean fromSettings = getIntent().getBooleanExtra("from_settings", false);
        if (fromSettings) {
            btnUpload.setVisibility(View.GONE);
            tvSkip.setVisibility(View.GONE);
            findViewById(R.id.tvTitle).setVisibility(View.GONE);
            findViewById(R.id.bottomActionsLayout).setVisibility(View.GONE);
        }
        adapter = new CertificateAdapter(uploadedCertificates, this);
        rvCertificates.setLayoutManager(new LinearLayoutManager(this));
        rvCertificates.setAdapter(adapter);
        loadCertificates();
        Button btnSelectCert = findViewById(R.id.btnSelectFile);
        ActivityResultLauncher<Intent> certPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        certUri = result.getData().getData();
                        String fileName = (certUri != null ? certUri.getLastPathSegment() : "");
                        tvFileName.setText(getString(R.string.selected_file_prefix, fileName));
                    }
                }
        );
        btnSelectCert.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
            certPicker.launch(intent);
        });
        btnAddCert.setOnClickListener(v -> {
            String title = etCertTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(this, R.string.cert_title_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (certUri != null) {
                uploadProgressBar.setVisibility(View.VISIBLE);
                btnAddCert.setEnabled(false);
                startUpload(certUri, title);
            } else {
                Toast.makeText(this, R.string.select_cert_file_required, Toast.LENGTH_SHORT).show();
            }
        });
        btnUpload.setOnClickListener(v -> completeProfile());
        tvSkip.setOnClickListener(v -> skipCertificates());
    }
    private void loadCertificates() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        FirebaseDatabase.getInstance().getReference("Users")
                .child("Tutor")
                .child(user.getUid())
                .child("certificates")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        uploadedCertificates.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Object value = child.getValue();
                            if (value instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> cert = new HashMap<>((Map<String, Object>) value);
                                if (!cert.containsKey("id")) {
                                    cert.put("id", child.getKey());
                                }
                                uploadedCertificates.add(cert);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
    private void skipCertificates() {
        completeProfile();
    }
    private void startUpload(Uri uri, String title) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String folderPath = "Users/" + user.getUid();
        MediaManager.get().upload(uri)
                .unsigned(UPLOAD_PRESET)
                .option("folder", folderPath)
                .callback(new UploadCallback() {
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        saveUrlToFirebase((String) resultData.get("secure_url"), title);
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> {
                            btnAddCert.setEnabled(true);
                            uploadProgressBar.setVisibility(View.GONE);
                            Toast.makeText(AddCertificatesActivity.this, R.string.upload_error, Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }
    private void saveUrlToFirebase(String url, String title) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        DatabaseReference tutorCertRef = FirebaseDatabase.getInstance().getReference("Users")
                .child("Tutor")
                .child(user.getUid())
                .child("certificates");
        String certId = tutorCertRef.push().getKey();
        Map<String, Object> certData = new HashMap<>();
        certData.put("id", certId);
        certData.put("title", title);
        certData.put("fileUrl", url);
        if (certId != null) {
            tutorCertRef.child(certId).setValue(certData)
                    .addOnCompleteListener(task -> {
                        btnAddCert.setEnabled(true);
                        uploadProgressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            uploadedCertificates.add(certData);
                            adapter.notifyItemInserted(uploadedCertificates.size() - 1);
                            etCertTitle.setText("");
                            certUri = null;
                            tvFileName.setText(R.string.no_file_selected_label);
                            Toast.makeText(this, R.string.cert_added_success, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, R.string.firebase_error, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
    @Override
    public void onViewCert(String url, String title) {
        if (url == null || url.isEmpty()) return;
        if (url.toLowerCase().contains(".pdf")) {
            PdfHelper.openPdf(this, url);
        } else {
            showImageDialog(url);
        }
    }
    private void showImageDialog(String url) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_view_image);
        ImageView imageView = dialog.findViewById(R.id.ivFullImage);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        Glide.with(this)
                .load(url)
                .into(imageView);
        dialog.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
    @Override
    public void onDeleteCert(int position, String certId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || certId == null) return;
        FirebaseDatabase.getInstance().getReference("Users")
                .child("Tutor")
                .child(user.getUid())
                .child("certificates")
                .child(certId)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        uploadedCertificates.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, uploadedCertificates.size());
                        Toast.makeText(this, "Certificate deleted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    @Override
    public void onEditCert(int position, String certId, String currentTitle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Certificate Title");
        final EditText input = new EditText(this);
        input.setText(currentTitle);
        builder.setView(input);
        builder.setPositiveButton("Update", (dialog, which) -> {
            String newTitle = input.getText().toString().trim();
            if (!newTitle.isEmpty()) {
                updateCertTitle(position, certId, newTitle);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    private void updateCertTitle(int position, String certId, String newTitle) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || certId == null) return;
        FirebaseDatabase.getInstance().getReference("Users")
                .child("Tutor")
                .child(user.getUid())
                .child("certificates")
                .child(certId)
                .child("title")
                .setValue(newTitle)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        uploadedCertificates.get(position).put("title", newTitle);
                        adapter.notifyItemChanged(position);
                        Toast.makeText(this, "Title updated", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void completeProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        DatabaseReference tutorRef = FirebaseDatabase.getInstance().getReference("Users")
                .child("Tutor")
                .child(user.getUid());
        tutorRef.child("isComplete").setValue(true);
        tutorRef.child("skippedRegistration").setValue(uploadedCertificates.isEmpty())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(AddCertificatesActivity.this, HomeTutorActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, R.string.firebase_error, Toast.LENGTH_SHORT).show();
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
