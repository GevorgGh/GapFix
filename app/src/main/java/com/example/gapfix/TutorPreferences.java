package com.example.gapfix;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TutorPreferences extends AppCompatActivity {

    private ChipGroup chipGroupSelected;
    private ListView listSuggestions;
    private NestedScrollView chipScroll;
    private List<String> selectedSubjects = new ArrayList<>();
    private List<String> allSubjectsList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tutor_preferences);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.backFr, new BackFragment())
                .commit();

        chipGroupSelected = findViewById(R.id.chipGroupSelected);
        listSuggestions = findViewById(R.id.listSuggestions);
        chipScroll = findViewById(R.id.chipScroll);
        SearchView searchView = findViewById(R.id.searchView);


        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, allSubjectsList);
        listSuggestions.setAdapter(adapter);

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
            String subject = adapter.getItem(position);
            if (subject != null) {
                addChip(subject);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> saveToFirebase());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void addChip(String text) {
        if (selectedSubjects.contains(text)) {
            Toast.makeText(this, "Already added", Toast.LENGTH_SHORT).show();
            return;
        }

        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setTextColor(ContextCompat.getColor(this, R.color.gapfix_text_dark));
        chip.setChipBackgroundColorResource(R.color.bg_tint);

        chip.setOnCloseIconClickListener(v -> {
            chipGroupSelected.removeView(chip);
            selectedSubjects.remove(text);
        });

        chipGroupSelected.addView(chip);
        selectedSubjects.add(text);

        chipScroll.post(() -> chipScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void loadSubjectsFromFirebase() {
        DatabaseReference subjectsRef = FirebaseDatabase.getInstance().getReference("Subjects");
        subjectsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allSubjectsList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String s = data.getValue(String.class);
                    if (s != null) allSubjectsList.add(s);
                }
                Collections.sort(allSubjectsList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TutorPreferences.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void saveToFirebase() {
        FirebaseDatabase.getInstance().getReference("Users")
                .child("Tutor")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("preferences")
                .setValue(selectedSubjects)
                .addOnSuccessListener(aVoid -> {
                    startActivity(new Intent(this, AddCertificatesActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
