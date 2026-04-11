package com.example.gapfix;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StudentSettingsFragment extends Fragment {

    private TextView tvName, tvEmail;
    private MaterialButton btnLogout;
    private ImageButton btnEditProfile;
    private TextView btnNotifications, btnPrivacy, btnHelp, btnAbout;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    public StudentSettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_settings, container, false);

        // Initialize UI components
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnNotifications = view.findViewById(R.id.btnNotifications);
        btnPrivacy = view.findViewById(R.id.btnPrivacy);
        btnHelp = view.findViewById(R.id.btnHelp);
        btnAbout = view.findViewById(R.id.btnAbout);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            tvEmail.setText(currentUser.getEmail());
            userRef = FirebaseDatabase.getInstance().getReference("Users")
                    .child("Student").child(currentUser.getUid());
            
            loadUserData();
        }

        // Set up click listeners
        btnLogout.setOnClickListener(v -> logout());

        btnEditProfile.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Edit Profile coming soon", Toast.LENGTH_SHORT).show();
        });

        btnNotifications.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Notification settings coming soon", Toast.LENGTH_SHORT).show();
        });

        btnPrivacy.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Privacy settings coming soon", Toast.LENGTH_SHORT).show();
        });

        btnHelp.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Help center coming soon", Toast.LENGTH_SHORT).show();
        });

        btnAbout.setOnClickListener(v -> {
            Toast.makeText(getContext(), "GapFix v1.0", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void loadUserData() {
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded() && snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    tvName.setText(name != null ? name : "Student");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void logout() {
        mAuth.signOut();
        
        // Clear saved role in SharedPreferences
        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            prefs.edit().clear().apply();

            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }
}
