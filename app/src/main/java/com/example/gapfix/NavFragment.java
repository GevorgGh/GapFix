package com.example.gapfix;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class NavFragment extends Fragment {

    private ImageView imgHome, imgArchive, imgCal, imgChat, imgProfile;
    private View notificationDot, calendarClickable;
    private DatabaseReference notifRef;
    private ValueEventListener notifListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nav, container, false);

        imgHome = view.findViewById(R.id.nav_home);
        imgArchive = view.findViewById(R.id.nav_archive);
        imgCal = view.findViewById(R.id.nav_calendar_icon);
        imgChat = view.findViewById(R.id.nav_chat);
        imgProfile = view.findViewById(R.id.nav_profile);
        notificationDot = view.findViewById(R.id.notification_dot);
        calendarClickable = view.findViewById(R.id.nav_calendar_clickable);

        View.OnClickListener navClickListener = v -> updateMenu(v.getId());

        if (imgHome != null) imgHome.setOnClickListener(navClickListener);
        if (imgArchive != null) imgArchive.setOnClickListener(navClickListener);
        if (calendarClickable != null) calendarClickable.setOnClickListener(navClickListener);
        if (imgChat != null) imgChat.setOnClickListener(navClickListener);
        if (imgProfile != null) imgProfile.setOnClickListener(navClickListener);

        updateMenu(R.id.nav_home);
        listenForNotifications();

        return view;
    }

    private void listenForNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(uid);
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
        notifRef.addValueEventListener(notifListener);
    }

    public void updateMenu(int clickedId) {
        if (imgHome == null || imgArchive == null || imgCal == null || imgChat == null || imgProfile == null) return;

        imgHome.setImageResource(R.drawable.shop);
        imgArchive.setImageResource(R.drawable.archive);
        imgCal.setImageResource(R.drawable.cal);
        imgChat.setImageResource(R.drawable.user_act);
        imgProfile.setImageResource(R.drawable.person);

        if (clickedId == R.id.nav_home) {
            imgHome.setImageResource(R.drawable.shop_clicked);
            switchFragment(new TutorShopFragment());
        } else if (clickedId == R.id.nav_archive) {
            imgArchive.setImageResource(R.drawable.archive_clicked);
        } else if (clickedId == R.id.nav_calendar_clickable) {
            imgCal.setImageResource(R.drawable.cal_clicked);
            if (notificationDot != null) notificationDot.setVisibility(View.GONE);
            if (notifRef != null) notifRef.removeValue();
            switchFragment(new StudentCalendarFragment());
        } else if (clickedId == R.id.nav_chat) {
            imgChat.setImageResource(R.drawable.chat_filled);
            switchFragment(new ChatListFragment());
        } else if (clickedId == R.id.nav_profile) {
            imgProfile.setImageResource(R.drawable.person_clicked);
            switchFragment(new StudentSettingsFragment());
        }
    }

    private void switchFragment(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (notifRef != null && notifListener != null) {
            notifRef.removeEventListener(notifListener);
        }
    }
}
