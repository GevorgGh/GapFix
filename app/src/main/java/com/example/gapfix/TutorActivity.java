package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class TutorActivity extends AppCompatActivity {

    private ReviewAdapter adapter;
    private RecyclerView reviewsRv;
    private List<Review> reviewList;
    private Button btnBookLesson;
    private Tutor tutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tutor);

        tutor = (Tutor) getIntent().getSerializableExtra("tutor");

        TextView tutorName = findViewById(R.id.tutor_name);
        TextView tutorBio = findViewById(R.id.tutor_bio);
        TextView tutorPriceLabel = findViewById(R.id.tutorPrice);
        ChipGroup tutorSubjectsChips = findViewById(R.id.tutor_subjects_chips);
        reviewsRv = findViewById(R.id.reviews);
        ImageView profileImage = findViewById(R.id.tutor_image);
        btnBookLesson = findViewById(R.id.btnBookLesson);

        reviewList = new ArrayList<>();
        adapter = new ReviewAdapter(reviewList);
        if (reviewsRv != null) {
            reviewsRv.setLayoutManager(new LinearLayoutManager(this));
            reviewsRv.setAdapter(adapter);
        }

        if (tutor != null) {
            tutorName.setText(tutor.getName());
            tutorBio.setText(tutor.getBio());
            tutorPriceLabel.setText("Available Subjects & Rates:");

            tutorSubjectsChips.removeAllViews();
            if (tutor.getPreferences() != null) {
                for (Tutor.SubjectPreference pref : tutor.getPreferences()) {
                    Chip chip = new Chip(this);
                    String info = String.format("%s - %s %d", pref.name, pref.currency, pref.price);
                    chip.setText(info);
                    chip.setChipStrokeColorResource(R.color.gapfix_green);
                    chip.setChipStrokeWidth(2f);
                    chip.setChipBackgroundColorResource(android.R.color.white);
                    chip.setClickable(false);
                    tutorSubjectsChips.addView(chip);
                }
            }

            Glide.with(this)
                    .load(tutor.getImageResourceLink() != null ? tutor.getImageResourceLink() : R.drawable.person_circle)
                    .placeholder(R.drawable.person_circle)
                    .circleCrop()
                    .into(profileImage);

            fetchReviews(tutor.getName());
            checkFreeLessonStatus();
        }

        btnBookLesson.setOnClickListener(v -> {
            String currentText = btnBookLesson.getText().toString();
            if (currentText.equalsIgnoreCase("Book a free Lesson")) {
                Intent intent = new Intent(TutorActivity.this, BookFreeLessonActivity.class);
                intent.putExtra("tutor", tutor);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Proceeding to regular booking...", Toast.LENGTH_SHORT).show();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void checkFreeLessonStatus() {
        String studentId = FirebaseAuth.getInstance().getUid();
        String tutorId = tutor.getId(); 

        if (studentId == null || tutorId == null) {
            tutorId = tutor.getName();
            Log.w("TutorActivity", "Tutor ID missing, using Name as fallback");
        }

        FirebaseDatabase.getInstance().getReference("FreeLessonsUsed")
                .child(studentId).child(tutorId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue(Boolean.class)) {
                    btnBookLesson.setText("Book Lesson");
                } else {
                    btnBookLesson.setText("Book Free Lesson");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchReviews(String tutorName) {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("Reviews");
        mDatabase.orderByChild("tutorName").equalTo(tutorName)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        reviewList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Review review = snapshot.getValue(Review.class);
                            if (review != null) {
                                fetchStudentName(review);
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("RTDB_Error", databaseError.getMessage());
                    }
                });
    }

    private void fetchStudentName(Review review) {
        DatabaseReference studentRef = FirebaseDatabase.getInstance().getReference("Users")
                .child("Student")
                .child(review.getStudentId());

        studentRef.get().addOnSuccessListener(dataSnapshot -> {
            if (dataSnapshot.exists()) {
                String name = dataSnapshot.child("name").getValue(String.class);
                review.setStudentName(name);
            } else {
                review.setStudentName("Unknown Student");
            }
            reviewList.add(review);
            adapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> {
            Log.e("RTDB_ERROR", "Could not fetch name: " + e.getMessage());
        });
    }
}
