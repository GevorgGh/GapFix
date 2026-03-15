package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.widget.NestedScrollView;
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
    private ListView listSuggestions;
    private NestedScrollView chipScroll;
    private List<String> selectedSubjects = new ArrayList<>();
    private List<String> allSubjectsList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_preferences);

        chipGroupSelected = findViewById(R.id.chipGroupSelected);
        listSuggestions = findViewById(R.id.listSuggestions);
        chipScroll = findViewById(R.id.chipScroll);
        SearchView searchView = findViewById(R.id.searchView);

        // Initialize adapter with empty list
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, allSubjectsList);
        listSuggestions.setAdapter(adapter);

        // Load 500+ subjects from Firebase
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
            addChip(subject);
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> saveToFirebase());
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
                Toast.makeText(StudentPreferences.this, "Error loading data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addChip(String text) {
        if (selectedSubjects.contains(text)) {
            Toast.makeText(this, "Already added", Toast.LENGTH_SHORT).show();
            return;
        }

        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setTextSize(12);
        chip.setCloseIconVisible(true);
        chip.setTextColor(getResources().getColor(R.color.gapfix_text_dark));

        chip.setOnCloseIconClickListener(v -> {
            chipGroupSelected.removeView(chip);
            selectedSubjects.remove(text);
        });

        chipGroupSelected.addView(chip);
        selectedSubjects.add(text);

        chipScroll.post(() -> chipScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void saveToFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (selectedSubjects.isEmpty()) {
            Toast.makeText(this, "Select at least one subject", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseDatabase.getInstance().getReference("Users")
                .child("Student")
                .child(user.getUid())
                .child("preferences")
                .setValue(selectedSubjects)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                });
    }
}