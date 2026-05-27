package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private boolean redirected = false;
    private boolean isAutoChecking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            new Handler().postDelayed(() -> {
                if (!redirected) {
                    setupUI();
                }
            }, 3000);

            isAutoChecking = true;
            checkDatabaseForProfile(currentUser.getUid());
        } else {
            setupUI();
        }

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    }
                }
        );

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupUI() {
        setContentView(R.layout.activity_main);

        Button login = findViewById(R.id.btn_goto_login);
        Button signup = findViewById(R.id.btn_goto_signup);
        ConstraintLayout googleBtn = findViewById(R.id.google_button);
        View btnTestStudent = findViewById(R.id.btn_test_student);
        View btnTestTutor = findViewById(R.id.btn_test_tutor);

        if (googleBtn != null) googleBtn.setOnClickListener(v -> signIn());
        if (login != null) login.setOnClickListener(v -> {
            isAutoChecking = false;
            startActivity(new Intent(this, LoginActivity.class));
        });
        if (signup != null) signup.setOnClickListener(v -> {
            isAutoChecking = false;
            startActivity(new Intent(this, SignUpRole.class));
        });

        if (btnTestStudent != null) {
            btnTestStudent.setOnClickListener(v -> performTestLogin("gapfix00@gmail.com", "044238228Gg."));
        }
        if (btnTestTutor != null) {
            btnTestTutor.setOnClickListener(v -> performTestLogin("innovationcampus26@gmail.com", "044238228Gg."));
        }

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    }

    private void signIn() {
        isAutoChecking = false;
        googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent());
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                checkDatabaseForProfile(mAuth.getCurrentUser().getUid());
            } else {
                
            }
        });
    }

    private void performTestLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        isAutoChecking = true;
                        checkDatabaseForProfile(mAuth.getCurrentUser().getUid());
                    }
                });
    }

    private void checkDatabaseForProfile(String uid) {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        db.child("Users").child("Student").child(uid).get().addOnCompleteListener(task -> {
            if (redirected) return;
            DataSnapshot snapshot = task.getResult();
            if (task.isSuccessful() && snapshot.exists() && snapshot.hasChild("email")) {
                Boolean isComplete = snapshot.child("isComplete").getValue(Boolean.class);
                if (isComplete != null && isComplete) {
                    redirected = true;
                    startActivity(new Intent(this, HomeStudentActivity.class));
                    finish();
                } else if (!isAutoChecking) {
                    
                    redirected = true;
                    if (currentUser != null && !currentUser.isEmailVerified() && 
                        currentUser.getProviderData().stream().anyMatch(p -> p.getProviderId().equals("password"))) {
                        Intent intent = new Intent(this, IsVerifiedActivity.class);
                        intent.putExtra("ROLE", "Student");
                        startActivity(intent);
                    } else {
                        startActivity(new Intent(this, StudentPreferences.class));
                    }
                    finish();
                }
            } else {
                db.child("Users").child("Tutor").child(uid).get().addOnCompleteListener(tutorTask -> {
                    if (redirected) return;
                    DataSnapshot tutorSnap = tutorTask.getResult();
                    if (tutorTask.isSuccessful() && tutorSnap.exists() && tutorSnap.hasChild("email")) {
                        Boolean isComplete = tutorSnap.child("isComplete").getValue(Boolean.class);
                        if (isComplete != null && isComplete) {
                            redirected = true;
                            startActivity(new Intent(this, HomeTutorActivity.class));
                            finish();
                        } else if (!isAutoChecking) {
                            redirected = true;
                            if (currentUser != null && !currentUser.isEmailVerified() && 
                                currentUser.getProviderData().stream().anyMatch(p -> p.getProviderId().equals("password"))) {
                                Intent intent = new Intent(this, IsVerifiedActivity.class);
                                intent.putExtra("ROLE", "Tutor");
                                startActivity(intent);
                            } else {
                                startActivity(new Intent(this, TutorSubjectActivity.class));
                            }
                            finish();
                        }
                    } else if (!isAutoChecking) {
                        
                        redirected = true;
                        Intent intent = new Intent(this, SignUpRole.class);
                        intent.putExtra("isGoogle", true);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        });
    }
}
