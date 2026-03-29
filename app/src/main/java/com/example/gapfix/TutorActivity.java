package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

public class TutorActivity extends AppCompatActivity {

    private ReviewAdapter adapter;
    private RecyclerView reviews;
    private List<Review> reviewList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tutor);

        reviews = findViewById(R.id.reviews);
        reviewList = new ArrayList<>();
        adapter = new ReviewAdapter(reviewList);
        if (reviews != null) {
            reviews.setLayoutManager(new LinearLayoutManager(this));
            reviews.setAdapter(adapter);
            reviews.setLayoutManager(new LinearLayoutManager(this));

        } else {
            Log.e("Error", "RecyclerView not found in layout!");
        }

        Tutor tutor = (Tutor) getIntent().getSerializableExtra("tutor");

        TextView tutorName = findViewById(R.id.tutor_name);
        TextView tutorBio = findViewById(R.id.tutor_bio);
        TextView tutorPrice = findViewById(R.id.tutorPrice);
        ChipGroup tutorSubjects = findViewById(R.id.tutor_subjects_chips);
        RecyclerView reviews = findViewById(R.id.reviews);
        ImageView profileImage = findViewById(R.id.tutor_image);
        Button btnBookLesson = findViewById(R.id.btnBookLesson);


        tutorName.setText(tutor.getName());
        tutorBio.setText(tutor.getBio());
        tutorSubjects.removeAllViews();
        tutorPrice.setText("From $" + tutor.getMinPrice() + " to $" + tutor.getMaxPrice());
        for (String subject : tutor.getSubjects()) {
            Chip chip = new Chip(this);
            chip.setText(subject);
            chip.setClickable(false);
            chip.setFocusable(false);
            chip.setCheckable(false);
            tutorSubjects.addView(chip);
        }

        if (tutor.getImageResourceLink() != null) {
            Glide.with(this)
                    .load(tutor.getImageResourceLink())
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.person_circle);
        }

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("Reviews");

        mDatabase.orderByChild("tutorId").equalTo(tutor.getId())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        reviewList.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Review review = snapshot.getValue(Review.class);

                            assert review != null;
                            fetchStudentName(review);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e("RTDB_Error", databaseError.getMessage());
                    }
                });


        btnBookLesson.setOnClickListener(v -> {
            Intent intent = new Intent(TutorActivity.this, BookFreeLessonActivity.class);
            intent.putExtra("tutor", tutor);
            startActivity(intent);
        });





        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void fetchStudentName(Review review) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");

        usersRef.child(review.getStudentId()).get().addOnSuccessListener(dataSnapshot -> {
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