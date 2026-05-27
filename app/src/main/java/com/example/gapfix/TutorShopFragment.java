package com.example.gapfix;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.SearchView;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Locale;

public class TutorShopFragment extends Fragment {

    private DatabaseReference mDatabase;
    private String currentUserId;
    private ArrayList<Tutor> filteredTutors = new ArrayList<>();
    private ArrayList<Tutor> allMatchedTutors = new ArrayList<>();
    private TutorAdapter adapter;
    private final java.util.Map<String, String> subjectsTranslationMap = new java.util.HashMap<>();

    private ArrayList<String> studentSelectedSubjects = new ArrayList<>();
    private ArrayList<String> filterSelectedSubjects = new ArrayList<>();
    private float minPrice = 0, maxPrice = 200;
    private String currentSearchQuery = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tutor_shop, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewTutors);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        SearchView searchView = view.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                applyAllFilters();
                return true;
            }
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                applyAllFilters();
                return true;
            }
        });

        ImageButton btnFilter = view.findViewById(R.id.imageButton);
        btnFilter.setOnClickListener(v -> showFilterBottomSheet());

        mDatabase = FirebaseDatabase.getInstance().getReference();
        adapter = new TutorAdapter(filteredTutors);
        recyclerView.setAdapter(adapter);

        loadSubjectTranslations();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadStudentPreferences();
        }

        return view;
    }

    private void loadStudentPreferences() {
        mDatabase.child("Users").child("Student").child(currentUserId).child("preferences")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        studentSelectedSubjects.clear();
                        if (snapshot.exists()) {
                            for (DataSnapshot prefSnapshot : snapshot.getChildren()) {
                                String pref = prefSnapshot.getValue(String.class);
                                if (pref != null) {
                                    studentSelectedSubjects.add(pref);
                                }
                            }
                        }
                        
                        filterSelectedSubjects.clear();
                        filterSelectedSubjects.addAll(studentSelectedSubjects);
                        
                        fetchTutors();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void fetchTutors() {
        mDatabase.child("Users").child("Tutor").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allMatchedTutors.clear();

                for (DataSnapshot tutorSnapshot : snapshot.getChildren()) {
                    Tutor tutor = new Tutor();
                    tutor.setId(tutorSnapshot.getKey());
                    tutor.setName(tutorSnapshot.child("name").getValue(String.class));
                    tutor.setBio(tutorSnapshot.child("bio").getValue(String.class));
                    
                    String img = tutorSnapshot.child("imageResourceLink").getValue(String.class);
                    if (img == null) img = tutorSnapshot.child("profilePicture").getValue(String.class);
                    tutor.setImageResourceLink(img);

                    ArrayList<Tutor.SubjectPreference> tutorPrefs = new ArrayList<>();
                    DataSnapshot prefsSnapshot = tutorSnapshot.child("preferences");

                    if (prefsSnapshot.exists()) {
                        for (DataSnapshot subSnapshot : prefsSnapshot.getChildren()) {
                            Tutor.SubjectPreference pref = subSnapshot.getValue(Tutor.SubjectPreference.class);
                            if (pref != null) {
                                tutorPrefs.add(pref);
                            }
                        }
                    }
                    tutor.setPreferences(tutorPrefs);

                    allMatchedTutors.add(tutor);
                }

                applyAllFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showFilterBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_filter_tutor_shop, null);
        dialog.setContentView(sheetView);

        RangeSlider priceSlider = sheetView.findViewById(R.id.priceRangeSlider);
        android.widget.TextView tvMin = sheetView.findViewById(R.id.tvMinPrice);
        android.widget.TextView tvMax = sheetView.findViewById(R.id.tvMaxPrice);
        ChipGroup chipGroup = sheetView.findViewById(R.id.chipGroupSubjects);
        MaterialButton btnApply = sheetView.findViewById(R.id.btnApplyFilters);

        priceSlider.setValues(minPrice, maxPrice);
        tvMin.setText(String.format(Locale.getDefault(), "$%d", (int) minPrice));
        tvMax.setText(String.format(Locale.getDefault(), "$%d", (int) maxPrice));

        priceSlider.addOnChangeListener((slider, value, fromUser) -> {
            java.util.List<Float> values = slider.getValues();
            minPrice = values.get(0);
            maxPrice = values.get(1);
            tvMin.setText(String.format(Locale.getDefault(), "$%d", (int) minPrice));
            tvMax.setText(String.format(Locale.getDefault(), "$%d", (int) maxPrice));
        });

        
        for (String subject : studentSelectedSubjects) {
            Chip chip = new Chip(getContext());
            String translated = subjectsTranslationMap.get(subject);
            chip.setText(translated != null ? translated : subject);
            chip.setCheckable(true);
            chip.setCheckable(true);
            chip.setChecked(filterSelectedSubjects.contains(subject));
            
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (!filterSelectedSubjects.contains(subject)) filterSelectedSubjects.add(subject);
                } else {
                    filterSelectedSubjects.remove(subject);
                }
            });
            chipGroup.addView(chip);
        }

        btnApply.setOnClickListener(v -> {
            applyAllFilters();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void applyAllFilters() {
        filteredTutors.clear();
        
        String queryLower = currentSearchQuery.toLowerCase().trim();

        for (Tutor tutor : allMatchedTutors) {
            
            boolean matchesName = true;
            if (!queryLower.isEmpty()) {
                matchesName = tutor.getName() != null && tutor.getName().toLowerCase().contains(queryLower);
            }
            if (!matchesName) continue;

            
            boolean matchesFilters = false;
            
            if (filterSelectedSubjects.isEmpty()) {
                if (studentSelectedSubjects.isEmpty()) {
                    matchesFilters = true;
                }
            } else {
                if (tutor.getPreferences() != null) {
                    for (Tutor.SubjectPreference pref : tutor.getPreferences()) {
                        if (filterSelectedSubjects.contains(pref.name)) {
                            if (pref.price >= minPrice && pref.price <= maxPrice) {
                                matchesFilters = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (matchesFilters) {
                filteredTutors.add(tutor);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadSubjectTranslations() {
        mDatabase.child("Subjects").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                subjectsTranslationMap.clear();
                String lang = LocaleHelper.getLanguage(getContext());
                for (DataSnapshot data : snapshot.getChildren()) {
                    Object value = data.getValue();
                    if (value instanceof String) {
                        String s = (String) value;
                        subjectsTranslationMap.put(s, s);
                    } else if (value instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, String> translations = (java.util.Map<String, String>) value;
                        String canonical = translations.get("en");
                        String translated = translations.get(lang);
                        if (translated == null) translated = canonical;
                        if (canonical != null) subjectsTranslationMap.put(canonical, translated);
                    }
                }
                adapter.refresh();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
