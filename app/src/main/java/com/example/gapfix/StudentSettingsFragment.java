package com.example.gapfix;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
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
    private ImageView ivProfilePic;
    private MaterialButton btnLogout;
    private ImageButton btnEditProfile;
    private TextView btnAbout, btnLanguage, btnSubjects;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    public StudentSettingsFragment() {
        
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_settings, container, false);

        
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        ivProfilePic = view.findViewById(R.id.ivProfilePic);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnAbout = view.findViewById(R.id.btnAbout);
        btnLanguage = view.findViewById(R.id.btnLanguage);
        btnSubjects = view.findViewById(R.id.btnSubjects);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            tvEmail.setText(currentUser.getEmail());
            userRef = FirebaseDatabase.getInstance().getReference("Users")
                    .child("Student").child(currentUser.getUid());
            
            loadUserData();
        }

        
        btnLogout.setOnClickListener(v -> logout());

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            intent.putExtra("ROLE", "Student");
            startActivity(intent);
        });

        btnAbout.setOnClickListener(v -> {
            Toast.makeText(getContext(), "GapFix v 1.0", Toast.LENGTH_SHORT).show();
        });

        btnSubjects.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), StudentPreferences.class);
            startActivity(intent);
        });

        
        String currentLang = LocaleHelper.getLanguage(requireContext());
        btnLanguage.setText("App Language (" + getLanguageName(currentLang) + ")");
        btnLanguage.setOnClickListener(v -> showLanguageBottomSheet());

        return view;
    }

    private String getLanguageName(String lang) {
        if ("hy".equals(lang)) return "Հայերեն";
        if ("ru".equals(lang)) return "Русский";
        return "English";
    }

    private void showLanguageBottomSheet() {
        View sheetView = getLayoutInflater().inflate(R.layout.layout_languages_bottom_sheet, null);
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);

        android.widget.RadioGroup rgLanguages = sheetView.findViewById(R.id.rgLanguages);
        com.google.android.material.radiobutton.MaterialRadioButton rbArmenian = sheetView.findViewById(R.id.rbArmenian);
        com.google.android.material.radiobutton.MaterialRadioButton rbEnglish = sheetView.findViewById(R.id.rbEnglish);
        com.google.android.material.radiobutton.MaterialRadioButton rbRussian = sheetView.findViewById(R.id.rbRussian);
        com.google.android.material.button.MaterialButton btnContinue = sheetView.findViewById(R.id.btnContinue);

        String currentLang = LocaleHelper.getLanguage(requireContext());
        if ("hy".equals(currentLang)) {
            rbArmenian.setChecked(true);
        } else if ("ru".equals(currentLang)) {
            rbRussian.setChecked(true);
        } else {
            rbEnglish.setChecked(true);
        }

        btnContinue.setOnClickListener(v -> {
            String selectedLang = "en";
            int checkedId = rgLanguages.getCheckedRadioButtonId();
            if (checkedId == R.id.rbArmenian) {
                selectedLang = "hy";
            } else if (checkedId == R.id.rbRussian) {
                selectedLang = "ru";
            }

            if (!selectedLang.equals(currentLang)) {
                LocaleHelper.setLocale(requireContext(), selectedLang);
                dialog.dismiss();
                if (getActivity() != null) {
                    getActivity().recreate();
                }
            } else {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void loadUserData() {
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded() && snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    tvName.setText(name != null ? name : "Student");

                    String imageUrl = snapshot.child("imageResourceLink").getValue(String.class);
                    if (imageUrl == null) imageUrl = snapshot.child("profilePicture").getValue(String.class);

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(requireContext())
                                .load(imageUrl)
                                .placeholder(R.drawable.person_circle)
                                .circleCrop()
                                .into(ivProfilePic);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                
            }
        });
    }

    private void logout() {
        
        if (getActivity() != null) {
            getActivity().stopService(new Intent(getActivity(), CallNotificationService.class));
            
            mAuth.signOut();
            
            
            SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            prefs.edit().clear().apply();

            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }
}
