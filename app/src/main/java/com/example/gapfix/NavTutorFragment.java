package com.example.gapfix;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.Map;
public class NavTutorFragment extends Fragment {
    private BottomNavigationView bottomNav;
    private com.google.firebase.firestore.ListenerRegistration chatNotifListener;
    public NavTutorFragment() {}
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nav_tutor, container, false);
        bottomNav = view.findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                switchFragment(new TutorDashboardFragment());
            } else if (id == R.id.nav_calendar) {
                switchFragment(new TutorCalendarFragment());
            } else if (id == R.id.nav_subjects) {
                switchFragment(new TutorSubjectFragment());
            } else if (id == R.id.nav_chat) {
                bottomNav.removeBadge(R.id.nav_chat);
                switchFragment(new TutorStudentsFragment());
            } else if (id == R.id.nav_profile) {
                switchFragment(new TutorSettingsFragment());
            }
            return true;
        });
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        }
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
                        int docUnread = getCount(doc, "unreadChatCount", uid) + getCount(doc, "unreadHomeworkCount", uid);
                        if (docUnread == 0) {
                            docUnread = getCount(doc, "unreadCount", uid);
                        }
                        totalUnread += docUnread;
                    }
                    if (totalUnread > 0) {
                        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_chat);
                        badge.setVisible(true);
                        badge.setNumber(totalUnread);
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
        
        Object flatObj = doc.get(field + "." + uid);
        if (flatObj instanceof Number) {
            return ((Number) flatObj).intValue();
        }
        return 0;
    }
    private void listenForNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        
        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("Bookings");
        bookingsRef.orderByChild("tutorId").equalTo(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || bottomNav == null) return;
                int pendingCount = 0;
                java.util.Set<String> countedPackageIds = new java.util.HashSet<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Booking b = ds.getValue(Booking.class);
                    if (b == null) continue;
                    String status = b.getStatus() != null ? b.getStatus().toLowerCase() : "";
                    if (status.equals("pending") || status.equals("free_trial_pending")) {
                        if (b.isPackage() && b.getPackageId() != null) {
                            if (!countedPackageIds.contains(b.getPackageId())) {
                                countedPackageIds.add(b.getPackageId());
                                pendingCount++;
                            }
                        } else {
                            pendingCount++;
                        }
                    }
                }
                
                if (pendingCount > 0) {
                    BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_calendar);
                    badge.setVisible(true);
                    badge.setNumber(pendingCount);
                    badge.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.color_error));
                } else {
                    bottomNav.removeBadge(R.id.nav_calendar);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    public void updateMenu(int clickedId) {
        if (bottomNav == null) return;
        if (clickedId == R.id.nav_dashboard) {
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        } else if (clickedId == R.id.nav_calendar) {
            bottomNav.setSelectedItemId(R.id.nav_calendar);
        } else if (clickedId == R.id.nav_subjects) {
            bottomNav.setSelectedItemId(R.id.nav_subjects);
        } else if (clickedId == R.id.nav_chat) {
            bottomNav.setSelectedItemId(R.id.nav_chat);
        } else if (clickedId == R.id.nav_profile) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
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
        if (chatNotifListener != null) {
            chatNotifListener.remove();
        }
    }
}
