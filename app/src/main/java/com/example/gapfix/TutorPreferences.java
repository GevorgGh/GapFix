package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class TutorPreferences extends AppCompatActivity {

    private ChipGroup chipGroupSelected;
    private ListView listSuggestions;
    private List<String> selectedSubjects = new ArrayList<>();
    private String[] allSubjects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_preferences);

        chipGroupSelected = findViewById(R.id.chipGroupSelected);
        listSuggestions = findViewById(R.id.listSuggestions);
        SearchView searchView = findViewById(R.id.searchView);

        allSubjects = getResources().getStringArray(R.array.teaching_subjects);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, allSubjects);
        listSuggestions.setAdapter(adapter);

        listSuggestions.setVisibility(View.VISIBLE);

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
            String subject = (String) parent.getItemAtPosition(position);
            addChip(subject);

            searchView.setQuery("", false);
            searchView.clearFocus();
        });

        findViewById(R.id.btnSave).setOnClickListener(v -> saveToFirebase());
    }

    private void addChip(String text) {
        if (selectedSubjects.contains(text)) {
            Toast.makeText(this, "Already added", Toast.LENGTH_SHORT).show();
            return;
        }

        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(true);

        chip.setOnCloseIconClickListener(v -> {
            chipGroupSelected.removeView(chip);
            selectedSubjects.remove(text);
        });

        chipGroupSelected.addView(chip);
        selectedSubjects.add(text);
    }

    private void saveToFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (selectedSubjects.isEmpty()) {
            Toast.makeText(this, "Select at least one subject", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseDatabase.getInstance().getReference("Users")
                .child("Tutor")
                .child(user.getUid())
                .child("preferences")
                .setValue(selectedSubjects)
                .addOnSuccessListener(aVoid -> {
                    startActivity(new Intent(this, AddCertificatesActivity.class));
                    finish();
                });
    }
}