package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class StudentDashboardFragment extends Fragment {

    private FirebaseUser user;
    private TextView tvWelcome, tvHoursDone, tvGoalsCount, tvNoLessons, tvTutorName, tvLessonSubject, tvLessonDate, tvLessonTime, tvLessonStatus;
    private ImageView ivTutorPhoto, ivMyProfilePic;
    private View layoutLessonDetails;
    private MaterialButton btnJoinLesson, btnGoToTutors, btnGoToArchive;
    private DatabaseReference bookingsRef, usersRef;

    public StudentDashboardFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_dashboard, container, false);

        user = FirebaseAuth.getInstance().getCurrentUser();
        bookingsRef = FirebaseDatabase.getInstance().getReference("Bookings");
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvHoursDone = view.findViewById(R.id.tvHoursDone);
        tvGoalsCount = view.findViewById(R.id.tvGoalsCount);
        tvNoLessons = view.findViewById(R.id.tvNoLessons);
        layoutLessonDetails = view.findViewById(R.id.layoutLessonDetails);
        ivTutorPhoto = view.findViewById(R.id.ivTutorPhoto);
        ivMyProfilePic = view.findViewById(R.id.ivMyProfilePic);
        tvTutorName = view.findViewById(R.id.tvTutorName);
        tvLessonSubject = view.findViewById(R.id.tvLessonSubject);
        tvLessonDate = view.findViewById(R.id.tvLessonDate);
        tvLessonTime = view.findViewById(R.id.tvLessonTime);
        tvLessonStatus = view.findViewById(R.id.tvLessonStatus);
        btnJoinLesson = view.findViewById(R.id.btnJoinLesson);
        btnGoToTutors = view.findViewById(R.id.btnGoToTutors);
        btnGoToArchive = view.findViewById(R.id.btnGoToArchive);

        setupButtons();
        loadDashboardData();

        return view;
    }

    private void setupButtons() {
        btnGoToTutors.setOnClickListener(v -> {
            androidx.fragment.app.Fragment nav = getParentFragmentManager().findFragmentById(R.id.navFr);
            if (nav instanceof NavFragment) {
                ((NavFragment) nav).updateMenu(R.id.nav_chat);
            }
        });

        btnGoToArchive.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new StudentArchiveFragment())
                .addToBackStack(null)
                .commit());
    }

    private void loadDashboardData() {
        if (user == null) return;

        
        usersRef.child("Student").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    String name = snapshot.child("name").getValue(String.class);
                    tvWelcome.setText(getString(R.string.msg_hello_format, name != null ? name : "Student"));

                    String image = snapshot.child("imageResourceLink").getValue(String.class);
                    if (image == null) image = snapshot.child("profilePicture").getValue(String.class);

                    if (image != null && !image.isEmpty()) {
                        Glide.with(StudentDashboardFragment.this)
                                .load(image)
                                .placeholder(R.drawable.person_circle)
                                .circleCrop()
                                .into(ivMyProfilePic);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        bookingsRef.orderByChild("studentId").equalTo(user.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                long now = System.currentTimeMillis();
                long totalMinutes = 0;
                Set<String> activePackageIds = new HashSet<>();
                Booking nextBooking = null;
                long minDiff = Long.MAX_VALUE;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Booking b = ds.getValue(Booking.class);
                    if (b == null) continue;
                    b.setBookingId(ds.getKey());

                    String status = b.getStatus() != null ? b.getStatus().toLowerCase() : "";
                    
                    
                    if (status.equals("completed") || status.equals("finished")) {
                        totalMinutes += (b.getDuration() > 0 ? b.getDuration() : (b.isFree() ? 30 : 60));
                    }

                    
                    if (b.isPackage() && b.getPackageId() != null) {
                        if (status.equals("confirmed") || status.equals("pending") || 
                            status.equals("suggestion_pending") || status.equals("free_trial_pending")) {
                            
                            
                            if (b.getTimestamp() > now - (120 * 60 * 1000L)) { 
                                activePackageIds.add(b.getPackageId());
                            }
                        }
                    }

                    
                    if (!status.equals("cancelled") && !status.equals("completed") && !status.equals("finished")) {
                        long diff = b.getTimestamp() - now;
                        
                        if (diff > - (60 * 60 * 1000L) && diff < minDiff) {
                            minDiff = diff;
                            nextBooking = b;
                        }
                    }
                }

                tvHoursDone.setText(String.format(Locale.getDefault(), "%.1fh", totalMinutes / 60.0));
                tvGoalsCount.setText(String.valueOf(activePackageIds.size()));

                if (nextBooking != null) {
                    showNextBooking(nextBooking);
                } else {
                    tvNoLessons.setVisibility(View.VISIBLE);
                    layoutLessonDetails.setVisibility(View.GONE);
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showNextBooking(Booking booking) {
        tvNoLessons.setVisibility(View.GONE);
        layoutLessonDetails.setVisibility(View.VISIBLE);

        tvLessonSubject.setText(SubjectHelper.getTranslatedSubject(booking.getSubject()));
        
        SimpleDateFormat dateFmt = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        if (tvLessonDate != null) tvLessonDate.setText(dateFmt.format(new Date(booking.getTimestamp())));
        tvLessonTime.setText(timeFmt.format(new Date(booking.getTimestamp())));

        tvLessonStatus.setText(getTranslatedStatus(booking.getStatus()));

        if ("confirmed".equalsIgnoreCase(booking.getStatus())) {
            tvLessonStatus.setTextColor(getResources().getColor(R.color.gapfix_green, null));
            if (LessonTimeHelper.isJoinable(booking, "student")) {
                btnJoinLesson.setVisibility(View.VISIBLE);
                btnJoinLesson.setEnabled(true);
                btnJoinLesson.setText(R.string.ext_join);
                btnJoinLesson.setOnClickListener(v -> {
                    Intent intent = new Intent(getContext(), VideoCallActivity.class);
                    intent.putExtra("BOOKING_ID", booking.getBookingId());
                    startActivity(intent);
                });
            } else {
                btnJoinLesson.setVisibility(View.VISIBLE);
                btnJoinLesson.setEnabled(false);
                btnJoinLesson.setText(R.string.label_waiting);
            }
        } else {
            tvLessonStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark, null));
            btnJoinLesson.setVisibility(View.GONE);
        }

        
        usersRef.child("Tutor").child(booking.getTutorId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded() && snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String image = snapshot.child("imageResourceLink").getValue(String.class);
                    if (image == null) image = snapshot.child("profilePicture").getValue(String.class);

                    tvTutorName.setText(name != null ? name : "Tutor");
                    if (image != null && !image.isEmpty() && isAdded()) {
                        Glide.with(requireContext()).load(image).circleCrop().into(ivTutorPhoto);
                    } else if (isAdded()) {
                        ivTutorPhoto.setImageResource(R.drawable.person_circle);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String getTranslatedStatus(String status) {
        if (status == null) return getString(R.string.status_pending);
        switch (status.toLowerCase()) {
            case "confirmed": return getString(R.string.status_confirmed);
            case "completed": return getString(R.string.status_completed);
            case "cancelled": return getString(R.string.status_cancelled);
            case "finished": return getString(R.string.status_finished);
            case "done": return getString(R.string.status_done);
            case "suggestion_pending": return getString(R.string.status_suggestion_pending);
            case "free_trial_pending": return getString(R.string.status_free_trial_pending);
            case "pending":
            default: return getString(R.string.status_pending);
        }
    }
}
