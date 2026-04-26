package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TutorDashboardFragment extends Fragment {

    private FirebaseUser user;
    private TextView tvWelcome, tvEarnings, tvLessonCount, tvNoLessons, tvLessonSubject, tvLessonTime, tvStudentName, tvLessonStatus;
    private DatabaseReference datRef, lesRef;
    private RelativeLayout layoutLessonDetails;
    private LinearLayout layoutLessonActions;
    private MaterialButton btnAccept, btnReject, btnLesson;

    public TutorDashboardFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tutor_dashboard, container, false);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            datRef = FirebaseDatabase.getInstance().getReference("Users").child("Tutor").child(user.getUid());
            lesRef = FirebaseDatabase.getInstance().getReference("Bookings");
        }

        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvEarnings = view.findViewById(R.id.tvEarnings);
        tvLessonCount = view.findViewById(R.id.tvLessonCount);
        tvNoLessons = view.findViewById(R.id.tvNoLessons);
        layoutLessonDetails = view.findViewById(R.id.layoutLessonDetails);
        tvLessonSubject = view.findViewById(R.id.tvLessonSubject);
        tvLessonTime = view.findViewById(R.id.tvLessonTime);
        tvStudentName = view.findViewById(R.id.tvStudentName);
        layoutLessonActions = view.findViewById(R.id.layoutLessonActions);
        btnAccept = view.findViewById(R.id.btnAccept);
        btnReject = view.findViewById(R.id.btnReject);
        btnLesson = view.findViewById(R.id.btnLesson);
        tvLessonStatus = view.findViewById(R.id.tvLessonStatus);
        
        loadDashboardData();

        return view;
    }

    private void loadDashboardData() {
        if (datRef == null || user == null) return;

        loadTutorStats();

        long nowTs = System.currentTimeMillis();
        // Window: Any lesson starting within next 24 hours OR started within last 60 minutes
        long windowStart = nowTs - (60 * 60_000L);
        long windowEnd = nowTs + (24 * 60 * 60_000L);

        Query tutorQuery = lesRef.orderByChild("tutorId").equalTo(user.getUid());

        tutorQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                Booking nextBooking = null;
                long closestDiff = Long.MAX_VALUE;

                for (DataSnapshot data : snapshot.getChildren()) {
                    Booking b = data.getValue(Booking.class);
                    if (b == null) continue;

                    long ts = b.getTimestamp();
                    
                    // Logic: Find the closest upcoming lesson (or one that just started)
                    if (ts >= windowStart && ts <= windowEnd) {
                        long diff = Math.abs(ts - nowTs);
                        if (diff < closestDiff) {
                            closestDiff = diff;
                            nextBooking = b;
                        }
                    }
                }

                if (nextBooking != null) {
                    updateUIWithBooking(nextBooking);
                } else {
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
        datRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                
                String name = snapshot.child("name").getValue(String.class);
                tvWelcome.setText(String.format("Hello, %s!", name != null ? name : "Tutor"));

                Object moneyObj = snapshot.child("earnedMoney").getValue();
                double money = 0.0;
                if (moneyObj instanceof Long) money = ((Long) moneyObj).doubleValue();
                else if (moneyObj instanceof Double) money = (Double) moneyObj;
                tvEarnings.setText(String.format(Locale.US, "$%.2f", money));

                Long count = snapshot.child("lessonsCount").getValue(Long.class);
                tvLessonCount.setText(String.valueOf(count != null ? count : 0));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateUIWithBooking(Booking booking) {
        tvNoLessons.setVisibility(View.GONE);
        layoutLessonDetails.setVisibility(View.VISIBLE);

        tvLessonSubject.setText(booking.getSubject());
        
        // Display time in LOCAL time zone based on the timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvLessonTime.setText(sdf.format(new Date(booking.getTimestamp())));
        
        tvLessonStatus.setText(booking.getStatus().toUpperCase());

        String status = booking.getStatus();
        if ("pending".equals(status) || "free_trial_pending".equals(status)) {
            tvLessonStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            layoutLessonActions.setVisibility(View.VISIBLE);
            btnLesson.setVisibility(View.GONE);
            
            btnAccept.setOnClickListener(v -> updateStatus(booking.getBookingId(), "confirmed"));
            btnReject.setOnClickListener(v -> updateStatus(booking.getBookingId(), "cancelled"));

        } else if ("confirmed".equals(status)) {
            tvLessonStatus.setTextColor(getResources().getColor(R.color.gapfix_green));
            layoutLessonActions.setVisibility(View.GONE);
            btnLesson.setVisibility(View.VISIBLE);
            
            // Check if joinable now
            // FIX: Passing "tutor" role to isJoinable
            if (LessonTimeHelper.isJoinable(booking, "tutor")) {
                btnLesson.setEnabled(true);
                btnLesson.setText("JOIN");
                btnLesson.setOnClickListener(v -> {
                    Intent intent = new Intent(getContext(), VideoCallActivity.class);
                    intent.putExtra("BOOKING_ID", booking.getBookingId());
                    startActivity(intent);
                });
            } else {
                btnLesson.setEnabled(false);
                btnLesson.setText("WAITING");
            }
        } else {
            tvLessonStatus.setTextColor(getResources().getColor(R.color.error));
            layoutLessonActions.setVisibility(View.GONE);
            btnLesson.setVisibility(View.GONE);
        }

        FirebaseDatabase.getInstance().getReference("Users").child("Student")
                .child(booking.getStudentId()).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isAdded()) tvStudentName.setText(snapshot.exists() ? snapshot.getValue(String.class) : "Unknown Student");
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateStatus(String bId, String newStatus) {
        if (bId != null) lesRef.child(bId).child("status").setValue(newStatus);
    }
}
