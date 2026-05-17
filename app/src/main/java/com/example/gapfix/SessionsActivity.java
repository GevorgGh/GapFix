package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sessions);

        userRole = getIntent().getStringExtra("role");

        currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            finish();
            return;
        }

        initUI();
        determineRoleAndLoadData();
    }

    private void initUI() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Sessions");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tabLayout = findViewById(R.id.tabLayout);
        rvSessions = findViewById(R.id.rvSessions);
        emptyState = findViewById(R.id.emptyState);

        // Determine which adapter to use based on role
        if ("Tutor".equals(userRole)) {
            // If we are a tutor, we need the Tutor adapter
            // Note: If you have a separate TutorBookingAdapter, use that here.
            // Assuming BookingTutorAdapter is intended for tutors in this screen too.
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
                filterData(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
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
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        String field = "Tutor".equals(userRole) ? "tutorId" : "studentId";

        ref.orderByChild(field).equalTo(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Booking b = data.getValue(Booking.class);
                    if (b != null) fullList.add(b);
                }
                filterData(tabLayout.getSelectedTabPosition());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filterData(int position) {
        filteredList.clear();
        long now = System.currentTimeMillis();
        Set<String> addedPackages = new HashSet<>();

        for (Booking b : fullList) {
            String status = b.getStatus() != null ? b.getStatus().toLowerCase() : "";
            
            boolean matchTab = false;
            if (position == 0) { // Current
                if (status.equals("confirmed") && (now < b.getTimestamp() + (60 * 60 * 1000))) matchTab = true;
            } else if (position == 1) { // Pending
                if (status.contains("pending")) matchTab = true;
            } else if (position == 2) { // History
                if (status.equals("finished") || status.equals("done") || status.equals("cancelled")) matchTab = true;
            }

            if (matchTab) {
                // Logic: If it's a package, only show one "entry" in the Pending/Sessions tab to avoid clutter
                if (b.isPackage() && b.getPackageId() != null) {
                    if (!addedPackages.contains(b.getPackageId())) {
                        filteredList.add(b);
                        addedPackages.add(b.getPackageId());
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
