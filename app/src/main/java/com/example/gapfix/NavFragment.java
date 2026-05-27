package com.example.gapfix;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class NavFragment extends Fragment {

    private BottomNavigationView bottomNav;
    private DatabaseReference notifRef;
    private ValueEventListener notifListener;
    private com.google.firebase.firestore.ListenerRegistration chatNotifListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nav, container, false);

        bottomNav = view.findViewById(R.id.bottom_nav);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                switchFragment(new StudentDashboardFragment());
            } else if (id == R.id.nav_home) {
                switchFragment(new TutorShopFragment());
            } else if (id == R.id.nav_calendar) {
                
                bottomNav.removeBadge(R.id.nav_calendar);
                if (notifRef != null) notifRef.removeValue();
                switchFragment(new StudentCalendarFragment());
            } else if (id == R.id.nav_chat) {
                
                bottomNav.removeBadge(R.id.nav_chat);
                switchFragment(new StudentTutorsFragment());
            } else if (id == R.id.nav_profile) {
                switchFragment(new StudentSettingsFragment());
            }
            return true;
        });

        
        bottomNav.setSelectedItemId(R.id.nav_dashboard);
        listenForNotifications();
        listenForChatNotifications();

        return view;
    }

    private void listenForChatNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        chatNotifListener = com.google.firebase.firestore.FirebaseFirestore.getInstance("gapfix")
                .collection("chats")
                .whereArrayContains("participants", uid)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null || !isAdded() || bottomNav == null) return;

                    int totalUnread = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                        
                        totalUnread += getCount(doc, "unreadChatCount", uid);
                        totalUnread += getCount(doc, "unreadHomeworkCount", uid);
                        
                        
                        if (totalUnread == 0) {
                            totalUnread += getCount(doc, "unreadCount", uid);
                        }
                    }

                    if (totalUnread > 0) {
                        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_chat);
                        badge.setVisible(true);
                        badge.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_error));
                    } else {
                        bottomNav.removeBadge(R.id.nav_chat);
                    }
                });
    }

    private int getCount(com.google.firebase.firestore.DocumentSnapshot doc, String field, String uid) {
        Object obj = doc.get(field);
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            if (map.containsKey(uid)) {
                Object val = map.get(uid);
                if (val instanceof Number) {
                    return ((Number) val).intValue();
                }
            }
        }
        return 0;
    }

    private void listenForNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(uid);
        notifListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || bottomNav == null) return;
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_calendar);
                    badge.setVisible(true);
                    badge.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_error));
                } else {
                    bottomNav.removeBadge(R.id.nav_calendar);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        notifRef.addValueEventListener(notifListener);
    }

    public void updateMenu(int clickedId) {
        if (bottomNav == null) return;
        if (clickedId == R.id.nav_dashboard) {
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        } else if (clickedId == R.id.nav_home) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        } else if (clickedId == R.id.nav_calendar) {
            bottomNav.setSelectedItemId(R.id.nav_calendar);
        } else if (clickedId == R.id.nav_chat) {
            bottomNav.setSelectedItemId(R.id.nav_chat);
        } else if (clickedId == R.id.nav_profile) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
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
        if (chatNotifListener != null) {
            chatNotifListener.remove();
        }
    }
}
