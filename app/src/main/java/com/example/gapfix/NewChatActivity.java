package com.example.gapfix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class NewChatActivity extends AppCompatActivity {

    private RecyclerView rvContacts;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ContactAdapter adapter;
    private final List<User> contactList = new ArrayList<>();
    private final List<String> contactIds = new ArrayList<>();
    private String currentUserId;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_chat);

        currentUserId = FirebaseAuth.getInstance().getUid();
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userRole = prefs.getString("user_role", null);

        rvContacts = findViewById(R.id.rvContacts);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupRecyclerView();
        
        if (userRole == null) {
            fetchUserRoleAndContacts();
        } else {
            fetchContactsFromBookings();
        }
    }

    private void setupRecyclerView() {
        adapter = new ContactAdapter(contactList, contactIds, (user, userId) -> {
            Intent intent = new Intent(NewChatActivity.this, ChatActivity.class);
            intent.putExtra("CHAT_USER_ID", userId);
            intent.putExtra("CHAT_USER_NAME", user.getName());
            startActivity(intent);
            finish();
        });
        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        rvContacts.setAdapter(adapter);
    }

    private void fetchUserRoleAndContacts() {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("Users");
        db.child("Student").child(currentUserId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                userRole = "Student";
                fetchContactsFromBookings();
            } else {
                db.child("Tutor").child(currentUserId).get().addOnCompleteListener(tutorTask -> {
                    if (tutorTask.isSuccessful() && tutorTask.getResult().exists()) {
                        userRole = "Tutor";
                        fetchContactsFromBookings();
                    } else {
                        progressBar.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void fetchContactsFromBookings() {
        if (currentUserId == null || userRole == null) {
            progressBar.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference("Bookings");
        String myIdField = userRole.equals("Student") ? "studentId" : "tutorId";
        String targetRole = userRole.equals("Student") ? "Tutor" : "Student";

        bookingsRef.orderByChild(myIdField).equalTo(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> uniqueTargetIds = new HashSet<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Booking booking = data.getValue(Booking.class);
                    if (booking != null) {
                        String targetId = userRole.equals("Student") ? booking.getTutorId() : booking.getStudentId();
                        if (targetId != null) {
                            uniqueTargetIds.add(targetId);
                        }
                    }
                }

                if (uniqueTargetIds.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    fetchUserDetails(uniqueTargetIds, targetRole);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(NewChatActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUserDetails(Set<String> targetIds, String targetRole) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users").child(targetRole);
        final int total = targetIds.size();
        final int[] count = {0};

        for (String id : targetIds) {
            usersRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        contactList.add(user);
                        contactIds.add(snapshot.getKey());
                    }
                    
                    count[0]++;
                    if (count[0] == total) {
                        progressBar.setVisibility(View.GONE);
                        if (contactList.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                        } else {
                            adapter.notifyDataSetChanged();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    count[0]++;
                    if (count[0] == total) {
                        progressBar.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }
}
