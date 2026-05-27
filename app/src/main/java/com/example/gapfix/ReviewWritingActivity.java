package com.example.gapfix;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ReviewWritingActivity extends AppCompatActivity {

    private RatingBar ratingBar;
    private TextInputEditText etReview;
    private TextView tvTutorName, tvSubject;
    private Button btnSubmit, btnSkip;
    private String bookingId, tutorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_writing);

        bookingId = getIntent().getStringExtra("BOOKING_ID");
        tutorId = getIntent().getStringExtra("TUTOR_ID");

        tvTutorName = findViewById(R.id.tvTutorName);
        tvSubject = findViewById(R.id.tvSubject);
        ratingBar = findViewById(R.id.ratingBar);
        etReview = findViewById(R.id.etReview);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnSkip = findViewById(R.id.btnSkip);

        loadLessonDetails();

        btnSubmit.setOnClickListener(v -> submitReview());
        btnSkip.setOnClickListener(v -> finish());
    }

    private void loadLessonDetails() {
        if (bookingId == null || tutorId == null) return;

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        
        
        rootRef.child("Users").child("Tutor").child(tutorId).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    tvTutorName.setText(snapshot.getValue(String.class));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        
        rootRef.child("Bookings").child(bookingId).child("subject")
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    tvSubject.setText(snapshot.getValue(String.class));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void submitReview() {
        float rating = ratingBar.getRating();
        String reviewText = etReview.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        if (tutorId == null) {
            finish();
            return;
        }

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Reviews").child(tutorId).push();
        String studentId = FirebaseAuth.getInstance().getUid();

        Map<String, Object> reviewData = new HashMap<>();
        reviewData.put("rating", rating);
        reviewData.put("review", reviewText);
        reviewData.put("studentId", studentId);
        reviewData.put("bookingId", bookingId);
        reviewData.put("timestamp", System.currentTimeMillis());

        dbRef.setValue(reviewData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(ReviewWritingActivity.this, "Review submitted! Thank you.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(ReviewWritingActivity.this, "Failed to submit review", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
