package com.example.gapfix;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class SessionsActivity extends AppCompatActivity {
    private TabLayout tabLayout;
    private RecyclerView rvSessions;
    private LinearLayout emptyState;
    private BookingAdapter adapter;
    private List<Booking> fullList = new ArrayList<>();
    private List<Booking> filteredList = new ArrayList<>();
    private String currentUserId;
    private String userRole;
    private final Set<String> seenCancelledIds = new HashSet<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sessions);
        userRole = getIntent().getStringExtra("role");
        currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            finish();
            return;
        }
        initUI();
        determineRoleAndLoadData();

        View mainView = findViewById(R.id.main);
        View appBar = findViewById(R.id.appBarLayout);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
                if (appBar != null) {
                    appBar.setPadding(0, systemBars.top, 0, 0);
                }
                return insets;
            });
        }
    }
    private void initUI() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.my_sessions);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        tabLayout = findViewById(R.id.tabLayout);
        rvSessions = findViewById(R.id.rvSessions);
        emptyState = findViewById(R.id.emptyState);
        int[] titles = {R.string.ext_current, R.string.ext_pending, R.string.ext_history};
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null && i < titles.length) {
                View badgeView = LayoutInflater.from(this).inflate(R.layout.tab_badge, null);
                TextView tvTitle = badgeView.findViewById(R.id.tab_title);
                tvTitle.setText(getString(titles[i]));
                tab.setCustomView(badgeView);
            }
        }
        updateTabColors();
        if ("Tutor".equals(userRole)) {
            BookingTutorAdapter tutorAdapter = new BookingTutorAdapter(filteredList, this);
            rvSessions.setAdapter(tutorAdapter);
        } else {
            adapter = new BookingAdapter(this, filteredList);
            rvSessions.setAdapter(adapter);
        }
        rvSessions.setLayoutManager(new LinearLayoutManager(this));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                filterData(position);
                updateTabColors();
                if (position == 2) { 
                    markCancelledAsSeen();
                    setupTabBadge(2, 0);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {
                updateTabColors();
            }
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    private void updateTabColors() {
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null && tab.getCustomView() != null) {
                TextView tvTitle = tab.getCustomView().findViewById(R.id.tab_title);
                if (tab.isSelected()) {
                    tvTitle.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.gapfix_green));
                } else {
                    tvTitle.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.gapfix_text_secondary));
                }
            }
        }
    }
    private void markCancelledAsSeen() {
        for (Booking b : fullList) {
            if ("cancelled".equalsIgnoreCase(b.getStatus())) {
                seenCancelledIds.add(b.getBookingId());
            }
        }
        getSharedPreferences("SeenBookings", MODE_PRIVATE).edit()
                .putStringSet("seen_cancelled_" + currentUserId, seenCancelledIds)
                .apply();
    }
    private void setupTabBadge(int tabIndex, int count) {
        TabLayout.Tab tab = tabLayout.getTabAt(tabIndex);
        if (tab == null || tab.getCustomView() == null) return;
        if (tabLayout.getSelectedTabPosition() == tabIndex) {
            count = 0;
        }
        TextView tvBadge = tab.getCustomView().findViewById(R.id.tab_badge);
        if (count > 0) {
            tvBadge.setText(String.valueOf(count));
            tvBadge.setVisibility(View.VISIBLE);
        } else {
            tvBadge.setVisibility(View.GONE);
        }
    }
    private void determineRoleAndLoadData() {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference("Users");
        root.child("Tutor").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userRole = snapshot.exists() ? "Tutor" : "Student";
                loadSessions();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void loadSessions() {
        Set<String> savedSeen = getSharedPreferences("SeenBookings", MODE_PRIVATE)
                .getStringSet("seen_cancelled_" + currentUserId, new HashSet<>());
        seenCancelledIds.clear();
        seenCancelledIds.addAll(savedSeen);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        String field = "Tutor".equals(userRole) ? "tutorId" : "studentId";
        final boolean isTutor = "Tutor".equals(userRole);
        ref.orderByChild(field).equalTo(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullList.clear();
                long now = System.currentTimeMillis();
                int pendingCount = 0;
                int cancelledCount = 0;
                Set<String> countedPendingPkgs = new HashSet<>();
                Set<String> countedCancelledPkgs = new HashSet<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Booking b = data.getValue(Booking.class);
                    if (b != null) {
                        b.setBookingId(data.getKey());
                        String s = b.getStatus();
                        long duration = b.getDuration() > 0 ? b.getDuration() : LessonTimeHelper.DEFAULT_DURATION_MINUTES;
                        long endTime = b.getTimestamp() + (duration * 60 * 1000L);
                        if (now > endTime && ("confirmed".equals(s) || "pending".equals(s) || "free_trial_pending".equals(s) || "suggestion_pending".equals(s))) {
                            data.getRef().child("status").setValue("cancelled");
                            data.getRef().child("cancellationReason").setValue("Time expired");
                            b.setStatus("cancelled");
                        }
                        fullList.add(b);
                        String status = b.getStatus() != null ? b.getStatus().toLowerCase() : "";
                        boolean isPkg = b.isPackage() && b.getPackageId() != null;
                        
                        if (isTutor) {
                            if (status.equals("pending") || status.equals("free_trial_pending")) {
                                if (isPkg) {
                                    if (!countedPendingPkgs.contains(b.getPackageId())) {
                                        countedPendingPkgs.add(b.getPackageId());
                                        pendingCount++;
                                    }
                                } else {
                                    pendingCount++;
                                }
                            }
                        } else {
                            if (status.equals("suggestion_pending")) {
                                if (isPkg) {
                                    if (!countedPendingPkgs.contains(b.getPackageId())) {
                                        countedPendingPkgs.add(b.getPackageId());
                                        pendingCount++;
                                    }
                                } else {
                                    pendingCount++;
                                }
                            }
                        }
                        
                        if (status.equals("cancelled")) {
                            if (!seenCancelledIds.contains(b.getBookingId())) {
                                if (isPkg) {
                                    if (!countedCancelledPkgs.contains(b.getPackageId())) {
                                        countedCancelledPkgs.add(b.getPackageId());
                                        cancelledCount++;
                                    }
                                } else {
                                    cancelledCount++;
                                }
                            }
                        }
                    }
                }
                setupTabBadge(1, pendingCount);  
                setupTabBadge(2, cancelledCount); 
                filterData(tabLayout.getSelectedTabPosition());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void filterData(int position) {
        filteredList.clear();
        long now = System.currentTimeMillis();
        Set<String> pendingPackages = new HashSet<>();
        for (Booking b : fullList) {
            if (b.isPackage() && b.getPackageId() != null) {
                String s = b.getStatus() != null ? b.getStatus().toLowerCase() : "";
                if (s.contains("pending") || s.contains("suggestion")) {
                    pendingPackages.add(b.getPackageId());
                }
            }
        }
        Set<String> addedPackages = new HashSet<>();
        for (Booking b : fullList) {
            String status = b.getStatus() != null ? b.getStatus().toLowerCase() : "";
            String pkgId = b.getPackageId();
            boolean isPkg = b.isPackage() && pkgId != null;
            long duration = b.getDuration() > 0 ? b.getDuration() : LessonTimeHelper.DEFAULT_DURATION_MINUTES;
            boolean matchTab = false;
            if (position == 0) { 
                if (isPkg && pendingPackages.contains(pkgId)) {
                    matchTab = false;
                } else if (status.equals("confirmed") && (now < b.getTimestamp() + (duration * 60 * 1000L))) {
                    matchTab = true;
                }
            } else if (position == 1) { 
                if (isPkg) {
                    if (pendingPackages.contains(pkgId) && (status.contains("pending") || status.contains("suggestion"))) {
                        matchTab = true;
                    }
                } else {
                    if (status.contains("pending") || status.contains("suggestion")) matchTab = true;
                }
            } else if (position == 2) { 
                if (isPkg) {
                    boolean hasActive = pendingPackages.contains(pkgId);
                    if (!hasActive) {
                        for (Booking other : fullList) {
                            if (pkgId.equals(other.getPackageId())) {
                                String s = other.getStatus() != null ? other.getStatus().toLowerCase() : "";
                                long dur = other.getDuration() > 0 ? other.getDuration() : LessonTimeHelper.DEFAULT_DURATION_MINUTES;
                                if (s.equals("confirmed") && (now < other.getTimestamp() + (dur * 60 * 1000L))) {
                                    hasActive = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!hasActive && (status.equals("finished") || status.equals("done") || status.equals("cancelled") || status.equals("completed"))) {
                        matchTab = true;
                    }
                } else {
                    if (status.equals("finished") || status.equals("done") || status.equals("cancelled") || status.equals("completed")) {
                        matchTab = true;
                    }
                }
            }
            if (matchTab) {
                if (isPkg) {
                    if (!addedPackages.contains(pkgId)) {
                        filteredList.add(b);
                        addedPackages.add(pkgId);
                    }
                } else {
                    filteredList.add(b);
                }
            }
        }
        updateUI();
    }
    private void updateUI() {
        if (filteredList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rvSessions.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvSessions.setVisibility(View.VISIBLE);
        }
        if (rvSessions.getAdapter() != null) {
            rvSessions.getAdapter().notifyDataSetChanged();
        }
    }
}
