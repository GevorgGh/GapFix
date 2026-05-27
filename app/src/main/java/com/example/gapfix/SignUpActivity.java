package com.example.gapfix;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hbb20.CountryCodePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class SignUpActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private EditText emailField, passField, passConfirmField, firstNameField, lastNameField, phoneField, dateField, bioField;
    private Spinner genderSpinner;
    private CheckBox termsCheckbox;
    private CountryCodePicker ccp;
    private ImageView profileImage;
    private ProgressBar signUpProgress;
    private View layoutPhone, tvBioLabel, layoutEmail, layoutPassword, tvAccountSecurityLabel, tvPasswordRequirements;
    private boolean isGoogle = false;
    private Uri profileUri;
    private String profilePicUrl = "";
    private static final String UPLOAD_PRESET = "ml_default";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.backFr, new BackFragment())
                .commit();

        initCloudinary();
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        emailField = findViewById(R.id.editEmail);
        passField = findViewById(R.id.editPassword);
        passConfirmField = findViewById(R.id.editPasswordConfirm);
        firstNameField = findViewById(R.id.editFirstName);
        lastNameField = findViewById(R.id.editLastName);
        phoneField = findViewById(R.id.editPhone);
        dateField = findViewById(R.id.editTextDate);
        bioField = findViewById(R.id.editBio);
        genderSpinner = findViewById(R.id.spinnerGender);
        termsCheckbox = findViewById(R.id.checkboxTerms);
        ccp = findViewById(R.id.ccp);
        profileImage = findViewById(R.id.profileImage);
        signUpProgress = findViewById(R.id.signUpProgress);
        layoutPhone = findViewById(R.id.layoutPhone);
        tvBioLabel = findViewById(R.id.tvBioLabel);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);
        tvAccountSecurityLabel = findViewById(R.id.tvAccountSecurityLabel);
        tvPasswordRequirements = findViewById(R.id.tvPasswordRequirements);
        
        isGoogle = getIntent().getBooleanExtra("isGoogle", false);
        if (isGoogle) {
            if (layoutEmail != null) layoutEmail.setVisibility(View.GONE);
            if (layoutPassword != null) layoutPassword.setVisibility(View.GONE);
            if (tvAccountSecurityLabel != null) tvAccountSecurityLabel.setVisibility(View.GONE);
            if (tvPasswordRequirements != null) tvPasswordRequirements.setVisibility(View.GONE);

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                if (emailField != null) emailField.setText(currentUser.getEmail());
                String displayName = currentUser.getDisplayName();
                if (displayName != null && !displayName.isEmpty()) {
                    String[] parts = displayName.split(" ", 2);
                    if (firstNameField != null) firstNameField.setText(parts[0]);
                    if (parts.length > 1 && lastNameField != null) lastNameField.setText(parts[1]);
                }
            }
        }

        findViewById(R.id.fabAddPhoto).setOnClickListener(v -> showImageSourceDialog());

        ccp.registerCarrierNumberEditText(phoneField);

        setupGenderSpinner();

        dateField.setOnClickListener(v -> showDatePicker());

        final String selectedRole = getIntent().getStringExtra("ROLE");
        if ("Student".equals(selectedRole)) {
            if (tvBioLabel != null) tvBioLabel.setVisibility(View.GONE);
            if (bioField != null) bioField.setVisibility(View.GONE);
        }

        Button reg = findViewById(R.id.button3);

        reg.setOnClickListener(view -> {
            resetErrors();

            String email = emailField.getText().toString().trim();
            String pass = passField.getText().toString().trim();
            String passConfirm = passConfirmField.getText().toString().trim();
            String firstName = firstNameField.getText().toString().trim();
            String lastName = lastNameField.getText().toString().trim();
            String phone = ccp.getFullNumberWithPlus();
            String dateStr = dateField.getText().toString().trim();
            String bio = bioField.getText().toString().trim();
            
            boolean hasError = false;

            if (firstName.isEmpty()) { setError(firstNameField); hasError = true; }
            if (lastName.isEmpty()) { setError(lastNameField); hasError = true; }
            if (dateStr.isEmpty()) { setError(dateField); hasError = true; }
            
            if ("Tutor".equals(selectedRole)) {
                if (bio.isEmpty()) { setError(bioField); hasError = true; }
                if (profileUri == null) {
                    Toast.makeText(this, "Profile picture is required for Tutors", Toast.LENGTH_SHORT).show();
                    hasError = true;
                }
            }

            if (genderSpinner.getSelectedItemPosition() == 0) {
                setError(genderSpinner);
                hasError = true;
            }
            
            if (!isGoogle) {
                if (email.isEmpty()) { setError(emailField); hasError = true; }
                if (pass.isEmpty()) { setError(passField); hasError = true; }
                if (passConfirm.isEmpty()) { setError(passConfirmField); hasError = true; }
            }

            if (hasError) {
                Toast.makeText(this, R.string.err_fill_required, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isGoogle && !pass.equals(passConfirm)) {
                setError(passField);
                setError(passConfirmField);
                Toast.makeText(this, R.string.err_passwords_dont_match, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!ccp.isValidFullNumber()) {
                setError(layoutPhone);
                Toast.makeText(this, R.string.err_invalid_phone, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!termsCheckbox.isChecked()) {
                Toast.makeText(this, R.string.err_agree_terms, Toast.LENGTH_SHORT).show();
                return;
            }

            String fullName = firstName + " " + lastName;
            if (isGoogle) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    if (profileUri != null) {
                        uploadProfileImage(user.getUid(), fullName, email, selectedRole, dateStr, phone, genderSpinner.getSelectedItem().toString(), bio);
                    } else {
                        finalizeSignUp(user.getUid(), fullName, email, selectedRole, dateStr, phone, genderSpinner.getSelectedItem().toString(), bio, "");
                    }
                }
            } else {
                startRegistration(email, pass, fullName, selectedRole, dateStr, phone, genderSpinner.getSelectedItem().toString(), bio);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setError(View view) {
        view.setBackgroundResource(R.drawable.rounded_input_field_error);
    }

    private void resetErrors() {
        firstNameField.setBackgroundResource(R.drawable.rounded_input_field);
        lastNameField.setBackgroundResource(R.drawable.rounded_input_field);
        dateField.setBackgroundResource(R.drawable.rounded_input_field);
        emailField.setBackgroundResource(R.drawable.rounded_input_field);
        passField.setBackgroundResource(R.drawable.rounded_input_field);
        passConfirmField.setBackgroundResource(R.drawable.rounded_input_field);
        layoutPhone.setBackgroundResource(R.drawable.rounded_input_field);
        bioField.setBackgroundResource(R.drawable.rounded_input_field);
        genderSpinner.setBackgroundResource(R.drawable.rounded_input_field);
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
                    profileImage.setImageURI(uri);
                    profileImage.setPadding(0, 0, 0, 0);
                }
            });

    private final ActivityResultLauncher<Void> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    profileImage.setImageBitmap(bitmap);
                    profileImage.setPadding(0, 0, 0, 0);
                }
            });

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
            dateField.setText(date);
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void startRegistration(String email, String password, String name, String role, String dob, String phone, String gender, String bio) {
        signUpProgress.setVisibility(View.VISIBLE);
        findViewById(R.id.button3).setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            if (profileUri != null) {
                                uploadProfileImage(firebaseUser.getUid(), name, email, role, dob, phone, gender, bio);
                            } else {
                                finalizeSignUp(firebaseUser.getUid(), name, email, role, dob, phone, gender, bio, "");
                            }
                        }
                    } else {
                        signUpProgress.setVisibility(View.GONE);
                        findViewById(R.id.button3).setEnabled(true);
                        }
                });
    }

    private void uploadProfileImage(String userId, String name, String email, String role, String dob, String phone, String gender, String bio) {
        String folderPath = "Users/" + userId;
        MediaManager.get().upload(profileUri)
                .unsigned(UPLOAD_PRESET)
                .option("folder", folderPath)
                .callback(new UploadCallback() {
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        finalizeSignUp(userId, name, email, role, dob, phone, gender, bio, url);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        finalizeSignUp(userId, name, email, role, dob, phone, gender, bio, "");
                    }

                    @Override public void onStart(String requestId) {}
                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void finalizeSignUp(String userId, String name, String email, String role, String dob, String phone, String gender, String bio, String profileUrl) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(tokenTask -> {
                    String token = tokenTask.isSuccessful() ? tokenTask.getResult() : "";
                    saveUserInfo(userId, name, email, role, dob, token, phone, gender, bio, profileUrl);
                });
    }

    private void saveUserInfo(String userId, String name, String email, String role, String dob, String fcmToken, String phone, String gender, String bio, String profileUrl) {
        User userProfile = new User(name, email, role);
        userProfile.setDob(dob);
        userProfile.setFcmToken(fcmToken);
        userProfile.setPhone(phone);
        userProfile.setGender(gender);
        userProfile.setImageResourceLink(profileUrl);
        userProfile.setBio(bio);
        
        DatabaseReference userRef = mDatabase.child("Users").child(role).child(userId);
        userRef.setValue(userProfile).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                userRef.child("isComplete").setValue(false);
                
                GapFixApplication.fetchTokenAndLogin(userId);

                if (isGoogle) {
                    signUpProgress.setVisibility(View.GONE);
                    Intent intent;
                    if ("Student".equals(role)) {
                        intent = new Intent(SignUpActivity.this, StudentPreferences.class);
                    } else {
                        intent = new Intent(SignUpActivity.this, TutorSubjectActivity.class);
                    }
                    startActivity(intent);
                    finish();
                } else {
                    FirebaseAuth.getInstance().getCurrentUser().sendEmailVerification().addOnCompleteListener(verifyTask -> {
                        signUpProgress.setVisibility(View.GONE);
                        Intent intent = new Intent(SignUpActivity.this, IsVerifiedActivity.class);
                        intent.putExtra("ROLE", role);
                        startActivity(intent);
                        finish();
                    });
                }
            } else {
                signUpProgress.setVisibility(View.GONE);
                findViewById(R.id.button3).setEnabled(true);
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
