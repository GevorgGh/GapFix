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

public class TutorSettingsFragment extends Fragment {

    private TextView tvName, tvEmail;
    private MaterialButton btnLogout;
    private ImageButton btnEditProfile;
    private TextView btnManageSubjects, btnCertificates, btnNotifications, btnPrivacy;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    public TutorSettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tutor_settings, container, false);

        // Initialize UI components
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnManageSubjects = view.findViewById(R.id.btnManageSubjects);
        btnCertificates = view.findViewById(R.id.btnCertificates);
        btnNotifications = view.findViewById(R.id.btnNotifications);
        btnPrivacy = view.findViewById(R.id.btnPrivacy);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            tvEmail.setText(currentUser.getEmail());
            userRef = FirebaseDatabase.getInstance().getReference("Users")
                    .child("Tutor").child(currentUser.getUid());
            
            loadUserData();
        }

        // Set up click listeners
        btnLogout.setOnClickListener(v -> logout());

        btnEditProfile.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Edit Profile coming soon", Toast.LENGTH_SHORT).show();
        });

        btnManageSubjects.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new TutorSubjectFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        btnCertificates.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddCertificatesActivity.class);
            startActivity(intent);
        });

        btnNotifications.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Notification settings coming soon", Toast.LENGTH_SHORT).show();
        });

        btnPrivacy.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Privacy settings coming soon", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void loadUserData() {
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded() && snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    tvName.setText(name != null ? name : "Tutor");
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
