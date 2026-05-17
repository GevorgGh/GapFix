package com.example.gapfix;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class NavTutorFragment extends Fragment {

    private ImageView dashboard, calendar, subjects, chat, profile;
    private View notificationDot;
    private DatabaseReference notifRef;
    private Query notifQuery;
    private ValueEventListener notifListener;
    private long startTime;

    public NavTutorFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nav_tutor, container, false);

        // Record startup time to ignore old notifications
        startTime = System.currentTimeMillis();

        dashboard = view.findViewById(R.id.dashboard);
        calendar = view.findViewById(R.id.calendar);
        subjects = view.findViewById(R.id.subjects);
        chat = view.findViewById(R.id.chat);
        profile = view.findViewById(R.id.profile);
        notificationDot = view.findViewById(R.id.notification_dot);

        View.OnClickListener navClickListener = v -> {
            if (v.getId() == R.id.calendar || v.getId() == R.id.calendar_container) {
                updateMenu(R.id.calendar);
            } else {
                updateMenu(v.getId());
            }
        };

        dashboard.setOnClickListener(navClickListener);
        calendar.setOnClickListener(navClickListener);
        View calContainer = view.findViewById(R.id.calendar_container);
        if (calContainer != null) calContainer.setOnClickListener(navClickListener);
        
        subjects.setOnClickListener(navClickListener);
        chat.setOnClickListener(navClickListener);
        profile.setOnClickListener(navClickListener);

        updateMenu(R.id.dashboard);
        listenForNotifications();

        return view;
    }

    private void listenForNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(uid);
        
        // Listen only for NEW notifications
        notifQuery = notifRef.orderByChild("timestamp").startAt(startTime + 1);
        
        notifListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    if (notificationDot != null) notificationDot.setVisibility(View.VISIBLE);
                } else {
                    if (notificationDot != null) notificationDot.setVisibility(View.GONE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        notifQuery.addValueEventListener(notifListener);
    }

    public void updateMenu(int clickedId) {
        dashboard.setImageResource(R.drawable.user_act);
        calendar.setImageResource(R.drawable.cal);
        subjects.setImageResource(R.drawable.journal);
        chat.setImageResource(R.drawable.chat_unfilled);
        profile.setImageResource(R.drawable.person);

        if (clickedId == R.id.dashboard) {
            dashboard.setImageResource(R.drawable.user_act_clicked);
            switchFragment(new TutorDashboardFragment());
        } else if (clickedId == R.id.calendar) {
            calendar.setImageResource(R.drawable.cal_clicked);
            if (notificationDot != null) notificationDot.setVisibility(View.GONE); 
            if (notifRef != null) notifRef.removeValue();
            switchFragment(new TutorCalendarFragment());
        } else if (clickedId == R.id.subjects) {
            subjects.setImageResource(R.drawable.journal_clicked);
            switchFragment(new TutorSubjectFragment());
        } else if (clickedId == R.id.chat) {
            chat.setImageResource(R.drawable.chat_filled);
            switchFragment(new ChatListFragment());
        } else if (clickedId == R.id.profile) {
            profile.setImageResource(R.drawable.person_clicked);
            switchFragment(new TutorSettingsFragment());
        }
    }

    private void switchFragment(Fragment fragment) {
        if (getActivity() != null) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (notifQuery != null && notifListener != null) {
            notifQuery.removeEventListener(notifListener);
        }
    }
}
