package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private boolean redirected = false;

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
            }, 5000);

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

        if (googleBtn != null) googleBtn.setOnClickListener(v -> signIn());
        if (login != null) login.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
        if (signup != null) signup.setOnClickListener(v -> startActivity(new Intent(this, SignUpRole.class)));

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
        googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent());
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                checkDatabaseForProfile(mAuth.getCurrentUser().getUid());
            } else {
                Toast.makeText(this, "Auth Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkDatabaseForProfile(String uid) {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        
        // We look for the 'email' field specifically.
        // A 'ghost' profile created by the token logic will NOT have an email.
        db.child("Users").child("Student").child(uid).get().addOnCompleteListener(task -> {
            if (redirected) return;
            DataSnapshot snapshot = task.getResult();
            if (task.isSuccessful() && snapshot.exists() && snapshot.hasChild("email")) {
                redirected = true;
                startActivity(new Intent(this, HomeStudentActivity.class));
                finish();
            } else {
                db.child("Users").child("Tutor").child(uid).get().addOnCompleteListener(tutorTask -> {
                    if (redirected) return;
                    DataSnapshot tutorSnap = tutorTask.getResult();
                    if (tutorTask.isSuccessful() && tutorSnap.exists() && tutorSnap.hasChild("email")) {
                        redirected = true;
                        startActivity(new Intent(this, HomeTutorActivity.class));
                    } else {
                        redirected = true;
                        startActivity(new Intent(this, RoleSelectionActivity.class));
                    }
                    finish();
                });
            }
        });
    }
}
