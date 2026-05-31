package com.example.gapfix;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
public class TutorDashboardFragment extends Fragment {
    private FirebaseUser user;
    private TextView tvWelcome, tvEarnings, tvLessonCount, tvNoLessons, tvLessonSubject, tvLessonDate, tvLessonTime, tvStudentName, tvLessonStatus;
    private ImageView ivStudentPhoto, ivMyProfilePic;
    private DatabaseReference datRef, lesRef;
    private View layoutLessonDetails;
    private LinearLayout layoutLessonActions;
    private MaterialButton btnAccept, btnReject, btnLesson, btnGoToSubjects, btnViewClasses;
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
        tvLessonDate = view.findViewById(R.id.tvLessonDate);
        tvLessonTime = view.findViewById(R.id.tvLessonTime);
        tvStudentName = view.findViewById(R.id.tvStudentName);
        ivStudentPhoto = view.findViewById(R.id.ivStudentPhoto);
        layoutLessonActions = view.findViewById(R.id.layoutLessonActions);
        btnAccept = view.findViewById(R.id.btnAccept);
        btnReject = view.findViewById(R.id.btnReject);
        btnLesson = view.findViewById(R.id.btnLesson);
        tvLessonStatus = view.findViewById(R.id.tvLessonStatus);
        ivMyProfilePic = view.findViewById(R.id.ivMyProfilePic);
        btnGoToSubjects = view.findViewById(R.id.btnGoToSubjects);
        btnViewClasses = view.findViewById(R.id.btnViewClasses);
        setupQuickActions();
        SubjectHelper.loadTranslations(getContext(), this::loadDashboardData);
        return view;
    }
    private void setupQuickActions() {
        btnGoToSubjects.setOnClickListener(v -> {
            Fragment nav = getParentFragmentManager().findFragmentById(R.id.navFr);
            if (nav instanceof NavTutorFragment) {
                ((NavTutorFragment) nav).updateMenu(R.id.nav_subjects);
            }
        });
        btnViewClasses.setOnClickListener(v -> {
            Fragment nav = getParentFragmentManager().findFragmentById(R.id.navFr);
            if (nav instanceof NavTutorFragment) {
                ((NavTutorFragment) nav).updateMenu(R.id.nav_calendar);
            }
        });
    }
    private void loadDashboardData() {
        if (datRef == null || user == null) return;
        loadTutorStats();
        long nowTs = System.currentTimeMillis();
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
                    b.setBookingId(data.getKey());
                    long duration = b.getDuration() > 0 ? b.getDuration() : LessonTimeHelper.DEFAULT_DURATION_MINUTES;
                    long endTime = b.getTimestamp() + (duration * 60 * 1000L);
                    String status = b.getStatus();
                    if (nowTs > endTime && ("confirmed".equals(status) || "pending".equals(status) || "free_trial_pending".equals(status) || "suggestion_pending".equals(status))) {
                        data.getRef().child("status").setValue("cancelled");
                        data.getRef().child("cancellationReason").setValue("Time expired");
                        b.setStatus("cancelled");
                    }
                    long ts = b.getTimestamp();
                    if (ts >= windowStart && ts <= windowEnd && !"cancelled".equals(b.getStatus())) {
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
                }
        });
    }
    private void loadTutorStats() {
        datRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                String name = snapshot.child("name").getValue(String.class);
                tvWelcome.setText(getString(R.string.msg_hello_format, name != null ? name : "Tutor"));
                String imageUrl = snapshot.child("imageResourceLink").getValue(String.class);
                if (imageUrl == null) imageUrl = snapshot.child("profilePicture").getValue(String.class);
                if (imageUrl != null && !imageUrl.isEmpty() && isAdded()) {
                    Glide.with(TutorDashboardFragment.this)
                            .load(imageUrl)
                            .placeholder(R.drawable.person_circle)
                            .circleCrop()
                            .into(ivMyProfilePic);
                }
                Object moneyObj = snapshot.child("earnedMoney").getValue();
                double money = 0.0;
                if (moneyObj instanceof Number) {
                    money = ((Number) moneyObj).doubleValue();
                }
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
        tvLessonSubject.setText(SubjectHelper.getTranslatedSubject(booking.getSubject()));
        SimpleDateFormat dateFmt = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        if (tvLessonDate != null) tvLessonDate.setText(dateFmt.format(new Date(booking.getTimestamp())));
        tvLessonTime.setText(timeFmt.format(new Date(booking.getTimestamp())));
        tvLessonStatus.setText(getTranslatedStatus(booking.getStatus()).toUpperCase());
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
            if (LessonTimeHelper.isJoinable(booking, "tutor")) {
                btnLesson.setEnabled(true);
                btnLesson.setText(R.string.ext_join);
                btnLesson.setOnClickListener(v -> {
                    Intent intent = new Intent(getContext(), VideoCallActivity.class);
                    intent.putExtra("BOOKING_ID", booking.getBookingId());
                    startActivity(intent);
                });
            } else {
                btnLesson.setEnabled(false);
                btnLesson.setText(R.string.label_waiting);
            }
        } else {
            tvLessonStatus.setTextColor(getResources().getColor(R.color.error));
            layoutLessonActions.setVisibility(View.GONE);
            btnLesson.setVisibility(View.GONE);
        }
        FirebaseDatabase.getInstance().getReference("Users").child("Student")
                .child(booking.getStudentId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isAdded() && snapshot.exists()) {
                            tvStudentName.setText(snapshot.child("name").getValue(String.class));
                            String imageUrl = snapshot.child("imageResourceLink").getValue(String.class);
                            if (imageUrl == null) imageUrl = snapshot.child("profilePicture").getValue(String.class);
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(TutorDashboardFragment.this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.person_circle)
                                        .circleCrop()
                                        .into(ivStudentPhoto);
                            } else {
                                ivStudentPhoto.setImageResource(R.drawable.person_circle);
                            }
                        } else if (isAdded()) {
                            tvStudentName.setText(getString(R.string.label_unknown_student));
                            ivStudentPhoto.setImageResource(R.drawable.person_circle);
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
    private void updateStatus(String bId, String newStatus) {
        if (bId != null) lesRef.child(bId).child("status").setValue(newStatus);
    }
}