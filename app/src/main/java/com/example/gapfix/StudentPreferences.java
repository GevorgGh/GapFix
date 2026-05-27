package com.example.gapfix;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StudentPreferences extends AppCompatActivity {

    private ChipGroup chipGroupSelected;
    private NestedScrollView chipScroll;
    private final List<String> selectedSubjects = new ArrayList<>();
    private final List<String> allSubjectsList = new ArrayList<>();
    private final java.util.Map<String, String> canonicalToTranslatedMap = new java.util.HashMap<>();
    private final java.util.Map<String, String> translatedToCanonicalMap = new java.util.HashMap<>();
    private ArrayAdapter<String> adapter;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_preferences);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.backFr, new BackFragment())
                .commit();

        chipGroupSelected = findViewById(R.id.chipGroupSelected);
        ListView listSuggestions = findViewById(R.id.listSuggestions);
        chipScroll = findViewById(R.id.chipScroll);
        SearchView searchView = findViewById(R.id.searchView);

        listSuggestions.setNestedScrollingEnabled(true);

        adapter = new ArrayAdapter<>(this, R.layout.item_subject_suggestion, R.id.tvSubjectName, allSubjectsList);
        listSuggestions.setAdapter(adapter);

        checkIfEditMode();
        loadSubjectsFromFirebase();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });

        listSuggestions.setOnItemClickListener((parent, view, position, id) -> {
            String translatedName = adapter.getItem(position);
            if (translatedName != null) {
                addChip(translatedName);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> saveToFirebase());
        findViewById(R.id.tvSkip).setOnClickListener(v -> skipRegistration());
    }

    private void skipRegistration() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users")
                .child("Student")
                .child(user.getUid());

        userRef.child("isComplete").setValue(true);
        userRef.child("skippedRegistration").setValue(true)
                .addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(this, HomeStudentActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }

    private void checkIfEditMode() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseDatabase.getInstance().getReference("Users")
                .child("Student")
                .child(user.getUid())
                .child("isComplete")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Boolean complete = snapshot.getValue(Boolean.class);
                            if (complete != null && complete) {
                                isEditMode = true;
                                findViewById(R.id.tvSkip).setVisibility(View.GONE);
                                ((TextView) findViewById(R.id.textView11)).setText(R.string.change_your_subjects);
                                ((MaterialButton) findViewById(R.id.btnSave)).setText(R.string.update_preferences);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadSubjectsFromFirebase() {
        DatabaseReference subjectsRef = FirebaseDatabase.getInstance().getReference("Subjects");
        subjectsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allSubjectsList.clear();
                canonicalToTranslatedMap.clear();
                translatedToCanonicalMap.clear();

                String lang = LocaleHelper.getLanguage(StudentPreferences.this);

                for (DataSnapshot data : snapshot.getChildren()) {
                    String canonicalName = null;
                    String translatedName = null;

                    Object value = data.getValue();
                    if (value instanceof String) {
                        canonicalName = (String) value;
                        translatedName = canonicalName;
                    } else if (value instanceof java.util.Map) {
                        java.util.Map<String, String> translations = (java.util.Map<String, String>) value;
                        canonicalName = translations.get("en");
                        translatedName = translations.get(lang);
                        if (translatedName == null) translatedName = canonicalName;
                    }

                    if (canonicalName != null && translatedName != null) {
                        allSubjectsList.add(translatedName);
                        canonicalToTranslatedMap.put(canonicalName, translatedName);
                        translatedToCanonicalMap.put(translatedName, canonicalName);
                    }
                }
                Collections.sort(allSubjectsList);
                adapter.notifyDataSetChanged();

                
                loadExistingPreferences();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                }
        });
    }

    private void loadExistingPreferences() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference prefRef = FirebaseDatabase.getInstance().getReference("Users")
                .child("Student")
                .child(user.getUid())
                .child("preferences");

        prefRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String canonicalName = ds.getValue(String.class);
                        if (canonicalName != null) {
                            String translatedName = canonicalToTranslatedMap.get(canonicalName);
                            addChip(translatedName != null ? translatedName : canonicalName);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addChip(String translatedName) {
        String canonicalName = translatedToCanonicalMap.get(translatedName);
        if (canonicalName == null) canonicalName = translatedName; 

        if (selectedSubjects.contains(canonicalName)) {
            Toast.makeText(this, R.string.msg_already_added, Toast.LENGTH_SHORT).show();
            return;
        }

        Chip chip = new Chip(this);
        chip.setText(translatedName);
        chip.setCloseIconVisible(true);
        chip.setCloseIconTintResource(R.color.gapfix_green);
        chip.setTextColor(ContextCompat.getColor(this, R.color.gapfix_text_dark));
        chip.setChipBackgroundColorResource(R.color.gapfix_green_background);
        chip.setChipStrokeColorResource(R.color.color_input_stroke);
        chip.setChipStrokeWidth(1.0f);

        final String finalCanonicalName = canonicalName;
        chip.setOnCloseIconClickListener(v -> {
            chipGroupSelected.removeView(chip);
            selectedSubjects.remove(finalCanonicalName);
        });

        chipGroupSelected.addView(chip);
        selectedSubjects.add(canonicalName);

        chipScroll.post(() -> chipScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void saveToFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.err_session_expired, Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedSubjects.isEmpty()) {
            Toast.makeText(this, R.string.err_select_at_least_one, Toast.LENGTH_SHORT).show();
            return;
        }

        findViewById(R.id.btnSave).setEnabled(false);

        FirebaseDatabase.getInstance().getReference("Users")
                .child("Student")
                .child(user.getUid())
                .child("preferences")
                .setValue(selectedSubjects)
                .addOnSuccessListener(aVoid -> {
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users")
                            .child("Student")
                            .child(user.getUid());
                    
                    userRef.child("isComplete").setValue(true);
                    userRef.child("skippedRegistration").setValue(false);

                    if (isEditMode) {
                        Toast.makeText(this, R.string.msg_preferences_updated, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, R.string.msg_profile_completed, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, HomeStudentActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    findViewById(R.id.btnSave).setEnabled(true);
                    Toast.makeText(this, getString(R.string.err_save_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }
}