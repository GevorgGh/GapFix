package com.example.gapfix;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TutorDashboardFragment extends Fragment {

    private FirebaseUser user;
    private TextView tvWelcome, tvEarnings, tvLessonCount, tvNoLessons, tvLessonSubject, tvLessonTime, tvStudentName;
    private DatabaseReference datRef, lesRef;
    private RelativeLayout layoutLessonDetails;

    public TutorDashboardFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tutor_dashboard, container, false);

        // Initialize Firebase
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Reference to Tutor Profile
            datRef = FirebaseDatabase.getInstance().getReference("Users")
                    .child("Tutor")
                    .child(user.getUid());

            // Reference to all Bookings
            lesRef = FirebaseDatabase.getInstance().getReference("Bookings");
        }

        // Initialize UI Elements
        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvEarnings = view.findViewById(R.id.tvEarnings);
        tvLessonCount = view.findViewById(R.id.tvLessonCount);
        tvNoLessons = view.findViewById(R.id.tvNoLessons);
        layoutLessonDetails = view.findViewById(R.id.layoutLessonDetails);
        tvLessonSubject = view.findViewById(R.id.tvLessonSubject);
        tvLessonTime = view.findViewById(R.id.tvLessonTime);
        tvStudentName = view.findViewById(R.id.tvStudentName);

        loadDashboardData();

        return view;
    }

    private void loadDashboardData() {
        if (datRef == null || user == null) return;

        // 1. Load Tutor Profile Stats (Name, Earnings, Lesson Count)
        loadTutorStats();

        // 2. Load Today's Lesson
        String todayString = getTodayDateString(); // Formats to "Apr 5, 2026"

        // Query only bookings belonging to THIS tutor
        Query tutorQuery = lesRef.orderByChild("tutorId").equalTo(user.getUid());

        tutorQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                boolean foundToday = false;

                for (DataSnapshot data : snapshot.getChildren()) {
                    Booking b = data.getValue(Booking.class);

                    // Check if lesson is for today
                    if (b != null && todayString.equals(b.getLessonDate())) {
                        Log.d("Firebase", "Found today's lesson: " + b.getLessonDate());
                        updateUIWithBooking(b);
                        foundToday = true;
                        break;
                    }
                }

                // Toggle visibility if no lesson is found for today
                if (!foundToday) {
                    tvNoLessons.setVisibility(View.VISIBLE);
                    layoutLessonDetails.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Error fetching bookings: " + error.getMessage());
            }
        });
    }

    private void loadTutorStats() {
        // Name
        datRef.child("name").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded() && snapshot.exists()) {
                    tvWelcome.setText(String.format("Hello, %s!", snapshot.getValue(String.class)));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Earnings
        datRef.child("earnedMoney").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    Double money = snapshot.getValue(Double.class);
                    tvEarnings.setText(String.format("$%.2f", money != null ? money : 0.0));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Total Lessons Count
        datRef.child("lessonsCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    Integer count = snapshot.getValue(Integer.class);
                    tvLessonCount.setText(String.valueOf(count != null ? count : 0));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateUIWithBooking(Booking booking) {
        tvNoLessons.setVisibility(View.GONE);
        layoutLessonDetails.setVisibility(View.VISIBLE);

        tvLessonSubject.setText(booking.getSubject());
        tvLessonTime.setText(booking.getLessonTime());

        // Fetch Student name dynamically based on studentId in the booking
        DatabaseReference studentRef = FirebaseDatabase.getInstance().getReference("Users")
                .child("Student")
                .child(booking.getStudentId());

        studentRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded() && snapshot.exists()) {
                    tvStudentName.setText(snapshot.getValue(String.class));
                } else {
                    tvStudentName.setText("Unknown Student");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String getTodayDateString() {
        // Use "MMM d, yyyy" to match "Apr 5, 2026" (not "Apr 05")
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        return sdf.format(new Date());
    }
}