package com.example.gapfix;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hbb20.CountryCodePicker;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
public class EditProfileActivity extends AppCompatActivity {
    private EditText etFirstName, etLastName, etBio, etPhone, etEmail, etDOB;
    private Spinner genderSpinner;
    private ImageView ivProfile;
    private ProgressBar progressBar;
    private CountryCodePicker ccp;
    private View layoutPhone, tvBioLabel;
    private Uri profileUri;
    private String currentImageUrl = "";
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private String userId, userRole;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.backFr, new BackFragment())
                .commit();
        initCloudinary();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        userId = user.getUid();
        userRole = getIntent().getStringExtra("ROLE");
        if (userRole == null) userRole = "Student";
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(userRole).child(userId);
        etFirstName = findViewById(R.id.editFirstName);
        etLastName = findViewById(R.id.editLastName);
        etEmail = findViewById(R.id.editEmail);
        etBio = findViewById(R.id.editBio);
        etPhone = findViewById(R.id.editPhone);
        etDOB = findViewById(R.id.editDOB);
        genderSpinner = findViewById(R.id.spinnerGender);
        ivProfile = findViewById(R.id.profileImage);
        progressBar = findViewById(R.id.editProgress);
        ccp = findViewById(R.id.ccp);
        layoutPhone = findViewById(R.id.layoutPhone);
        tvBioLabel = findViewById(R.id.tvBioLabel);
        if ("Student".equals(userRole)) {
            if (tvBioLabel != null) tvBioLabel.setVisibility(View.GONE);
            if (etBio != null) etBio.setVisibility(View.GONE);
        }
        ccp.registerCarrierNumberEditText(etPhone);
        setupGenderSpinner();
        etDOB.setOnClickListener(v -> showDatePicker());
        findViewById(R.id.fabEditPhoto).setOnClickListener(v -> showImageSourceDialog());
        findViewById(R.id.btnGoToChangePassword).setOnClickListener(v -> 
                startActivity(new Intent(this, ChangePasswordActivity.class)));
        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfile());
        findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> showDeleteAccountDialog());
        loadUserData();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void loadUserData() {
        progressBar.setVisibility(View.VISIBLE);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                if (snapshot.exists()) {
                    String fullName = snapshot.child("name").getValue(String.class);
                    String bio = snapshot.child("bio").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String dob = snapshot.child("dob").getValue(String.class);
                    String gender = snapshot.child("gender").getValue(String.class);
                    currentImageUrl = snapshot.child("imageResourceLink").getValue(String.class);
                    if (fullName != null) {
                        String[] parts = fullName.split(" ", 2);
                        etFirstName.setText(parts[0]);
                        if (parts.length > 1) etLastName.setText(parts[1]);
                    }
                    if (email != null) etEmail.setText(email);
                    if (bio != null) etBio.setText(bio);
                    if (dob != null) etDOB.setText(dob);
                    if (phone != null) {
                        ccp.setFullNumber(phone);
                    }
                    if (gender != null) {
                        ArrayAdapter<String> adapter = (ArrayAdapter<String>) genderSpinner.getAdapter();
                        int position = adapter.getPosition(gender);
                        if (position >= 0) genderSpinner.setSelection(position);
                    }
                    if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                        Glide.with(EditProfileActivity.this)
                                .load(currentImageUrl)
                                .placeholder(R.drawable.person_circle)
                                .circleCrop()
                                .into(ivProfile);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }
    private void setupGenderSpinner() {
        String[] genders = {getString(R.string.select_gender), getString(R.string.male), getString(R.string.female), getString(R.string.other)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genders);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);
    }
    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.date_of_birth))
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setTheme(R.style.MyDatePickerTheme)
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String date = sdf.format(new Date(selection));
            etDOB.setText(date);
        });
        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }
    private void showImageSourceDialog() {
        String[] options = {getString(R.string.camera), getString(R.string.gallery)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.select_image_source)
                .setItems(options, (dialog, index) -> {
                    if (index == 0) takePicture.launch(null);
                    else pickMedia.launch(new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
                }).show();
    }
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    profileUri = uri;
                    ivProfile.setImageURI(uri);
                }
            });
    private final ActivityResultLauncher<Void> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    ivProfile.setImageBitmap(bitmap);
                }
            });
    private void saveProfile() {
        resetErrors();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String bio = etBio.getText().toString().trim();
        String phone = ccp.getFullNumberWithPlus();
        String dob = etDOB.getText().toString().trim();
        String gender = genderSpinner.getSelectedItem().toString();
        boolean hasError = false;
        if (firstName.isEmpty()) { setError(etFirstName); hasError = true; }
        if (lastName.isEmpty()) { setError(etLastName); hasError = true; }
        if (dob.isEmpty()) { setError(etDOB); hasError = true; }
        if ("Tutor".equals(userRole)) {
            if (bio.isEmpty()) { setError(etBio); hasError = true; }
            if (currentImageUrl.isEmpty() && profileUri == null) {
                Toast.makeText(this, "Profile picture is required for Tutors", Toast.LENGTH_SHORT).show();
                hasError = true;
            }
        }
        if (genderSpinner.getSelectedItemPosition() == 0) {
            setError(genderSpinner);
            hasError = true;
        }
        if (hasError) {
            Toast.makeText(this, R.string.err_fill_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ccp.isValidFullNumber()) {
            setError(layoutPhone);
            Toast.makeText(this, R.string.err_invalid_phone, Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.btnSaveProfile).setEnabled(false);
        String fullName = firstName + " " + lastName;
        proceedToSaveProfile(fullName, email, bio, phone, dob, gender);
    }
    private void proceedToSaveProfile(String name, String email, String bio, String phone, String dob, String gender) {
        if (profileUri != null) {
            uploadImageAndSave(name, email, bio, phone, dob, gender);
        } else {
            updateFirebaseProfile(name, email, bio, phone, currentImageUrl, dob, gender);
        }
    }
    private void uploadImageAndSave(String name, String email, String bio, String phone, String dob, String gender) {
        String folderPath = "Users/" + userId;
        MediaManager.get().upload(profileUri)
                .unsigned("ml_default")
                .option("folder", folderPath)
                .callback(new UploadCallback() {
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        updateFirebaseProfile(name, email, bio, phone, url, dob, gender);
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        updateFirebaseProfile(name, email, bio, phone, currentImageUrl, dob, gender);
                    }
                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }
    private void updateFirebaseProfile(String name, String email, String bio, String phone, String imageUrl, String dob, String gender) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("bio", bio);
        updates.put("phone", phone);
        updates.put("dob", dob);
        updates.put("gender", gender);
        updates.put("imageResourceLink", imageUrl);
        userRef.updateChildren(updates).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            findViewById(R.id.btnSaveProfile).setEnabled(true);
            if (task.isSuccessful()) {
                Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, R.string.err_profile_update_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void setError(View view) {
        view.setBackgroundResource(R.drawable.rounded_input_field_error);
    }
    private void resetErrors() {
        etFirstName.setBackgroundResource(R.drawable.rounded_input_field);
        etLastName.setBackgroundResource(R.drawable.rounded_input_field);
        etEmail.setBackgroundResource(R.drawable.rounded_input_field);
        etDOB.setBackgroundResource(R.drawable.rounded_input_field);
        layoutPhone.setBackgroundResource(R.drawable.rounded_input_field);
        etBio.setBackgroundResource(R.drawable.rounded_input_field);
        genderSpinner.setBackgroundResource(R.drawable.rounded_input_field);
    }
    private void initCloudinary() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dbugqpl3m");
            MediaManager.init(this, config);
        } catch (Exception ignored) {}
    }
    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account_confirm_title)
                .setMessage(R.string.delete_account_confirm_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteAccount())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    private void deleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        progressBar.setVisibility(View.VISIBLE);
        userRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                user.delete().addOnCompleteListener(deleteTask -> {
                    progressBar.setVisibility(View.GONE);
                    if (deleteTask.isSuccessful()) {
                        Toast.makeText(this, R.string.msg_account_deleted, Toast.LENGTH_LONG).show();
                        navigateToMain();
                    } else {
                        if (deleteTask.getException() instanceof com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                            Toast.makeText(this, R.string.msg_reauth_required, Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                            navigateToMain();
                        } else {
                            String error = deleteTask.getException() != null ? deleteTask.getException().getMessage() : "Unknown error";
                            Toast.makeText(this, getString(R.string.err_delete_account_failed, error), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to delete database records", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
