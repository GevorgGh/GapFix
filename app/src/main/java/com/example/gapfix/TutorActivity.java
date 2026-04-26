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
import java.util.Locale;

public class TutorActivity extends AppCompatActivity {

    private ReviewAdapter adapter;
    private RecyclerView reviewsRv;
    private List<Review> reviewList;
    private Button btnBookLesson;
    private Tutor tutor;
    
    private TextView tvAvgRating, tvReviewCount, tvMemberSince;
    private ImageView tutorBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tutor);

        tutor = (Tutor) getIntent().getSerializableExtra("tutor");

        // Bind Views
        TextView tutorName = findViewById(R.id.tutor_name);
        TextView tutorBio = findViewById(R.id.tutor_bio);
        TextView tutorPriceLabel = findViewById(R.id.tutorPrice);
        ChipGroup tutorSubjectsChips = findViewById(R.id.tutor_subjects_chips);
        reviewsRv = findViewById(R.id.reviews);
        ImageView profileImage = findViewById(R.id.tutor_image);
        tutorBanner = findViewById(R.id.tutor_banner);
        btnBookLesson = findViewById(R.id.btnBookLesson);
        
        tvAvgRating = findViewById(R.id.tv_avg_rating);
        tvReviewCount = findViewById(R.id.tv_review_count);
        tvMemberSince = findViewById(R.id.tv_member_since);

        reviewList = new ArrayList<>();
        adapter = new ReviewAdapter(reviewList);
        if (reviewsRv != null) {
            reviewsRv.setLayoutManager(new LinearLayoutManager(this));
            reviewsRv.setAdapter(adapter);
        }

        if (tutor != null) {
            tutorName.setText(tutor.getName());
            tutorBio.setText(tutor.getBio());
            tutorPriceLabel.setText("Available Subjects & Rates");

            // Display "Member since" if available, else generic
            tvMemberSince.setText("Member since 2024"); // Fallback for now

            tutorSubjectsChips.removeAllViews();
            if (tutor.getPreferences() != null) {
                for (Tutor.SubjectPreference pref : tutor.getPreferences()) {
                    Chip chip = new Chip(this);
                    String info = String.format("%s - %s%d", pref.name, pref.currency, pref.price);
                    chip.setText(info);
                    chip.setChipStrokeColorResource(R.color.gray);
                    chip.setChipStrokeWidth(1f);
                    chip.setChipBackgroundColorResource(android.R.color.white);
                    chip.setClickable(false);
                    tutorSubjectsChips.addView(chip);
                }
            }

            // Load Profile Image
            Glide.with(this)
                    .load(tutor.getImageResourceLink() != null ? tutor.getImageResourceLink() : R.drawable.person_circle)
                    .placeholder(R.drawable.person_circle)
                    .centerCrop()
                    .into(profileImage);
            
            // Load a default banner image (library background)
            // In a real app, this could also be from the tutor profile
            Glide.with(this)
                    .load("https://images.unsplash.com/photo-1507842217343-583bb7270b66?q=80&w=1000&auto=format&fit=crop")
                    .centerCrop()
                    .into(tutorBanner);

            fetchReviewsByTutorId(tutor.getId());
            checkFreeLessonStatus();
        }

        btnBookLesson.setOnClickListener(v -> {
            String currentText = btnBookLesson.getText().toString();
            if (currentText.equalsIgnoreCase("Book a free Lesson") || currentText.equalsIgnoreCase("Book Free Lesson")) {
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
        String tutorId = (tutor != null) ? tutor.getId() : null; 

        if (studentId == null || tutorId == null) return;

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

    private void fetchReviewsByTutorId(String tutorId) {
        if (tutorId == null) return;
        
        DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("Reviews").child(tutorId);
        reviewsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                reviewList.clear();
                float totalRating = 0;
                int count = 0;

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Review review = snapshot.getValue(Review.class);
                    if (review != null) {
                        totalRating += review.getRating();
                        count++;
                        fetchStudentName(review);
                    }
                }

                // Update Summary Stats
                if (count > 0) {
                    float avg = totalRating / count;
                    tvAvgRating.setText(String.format(Locale.US, "%.1f", avg));
                    tvReviewCount.setText(String.format(Locale.US, "%d %s", count, count == 1 ? "Review" : "Reviews"));
                } else {
                    tvAvgRating.setText("0.0");
                    tvReviewCount.setText("0 Reviews");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("RTDB_Error", databaseError.getMessage());
            }
        });
    }

    private void fetchStudentName(Review review) {
        if (review.getStudentId() == null) return;
        
        DatabaseReference studentRef = FirebaseDatabase.getInstance().getReference("Users")
                .child("Student")
                .child(review.getStudentId());

        studentRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    review.setStudentName(snapshot.child("name").getValue(String.class));
                } else {
                    review.setStudentName("Anonymous");
                }
                
                // Add to list and notify only after fetching name to avoid UI flicker/jumps
                if (!reviewList.contains(review)) {
                    reviewList.add(review);
                    adapter.notifyDataSetChanged();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
