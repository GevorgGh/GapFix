package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class AddBioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_bio);

        EditText bioField = findViewById(R.id.bioField);
        Button addBio = findViewById(R.id.addBio);

        addBio.setOnClickListener(v -> {
            String bioText = bioField.getText().toString();
            if (bioText.length() < 30){
                bioField.setError("Bio must be at least 30 characters");
            } else{
                addBioToFirebase(bioText);
            }
        });



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void addBioToFirebase(String bioText){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseDatabase.getInstance().getReference("Users")
                .child("Tutor")
                .child(user.getUid())
                .child("bio")
                .setValue(bioText)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Bio added successfully!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(AddBioActivity.this, AddPriceTutorActivity.class));
                        finish();
                    }
                    else{
                        Toast.makeText(this, "Firebase Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}