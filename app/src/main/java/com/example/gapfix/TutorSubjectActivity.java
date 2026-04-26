package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
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

public class TutorSubjectActivity extends AppCompatActivity {

    private AutoCompleteTextView subjectDropdown, currencyDropdown;
    private TextInputEditText etPrice, etDuration;
    private MaterialButton btnAddLocal, btnFinalFirebase;
    private RecyclerView rvSubjects;
    private RadioGroup teachMode;
    private TutorSubjectAdapter adapter;
    private List<String> allSubjectsList = new ArrayList<>();

    private List<Subject> subjectList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_subject);

        subjectDropdown = findViewById(R.id.subjectDropdown);
        currencyDropdown = findViewById(R.id.currencyDropdown);
        etPrice = findViewById(R.id.etPrice);
        etDuration = findViewById(R.id.etDuration); // New duration field
        btnAddLocal = findViewById(R.id.btnAddSubjectLocal);
        btnFinalFirebase = findViewById(R.id.btnFinalSaveFirebase);
        rvSubjects = findViewById(R.id.rvSubjects);
        teachMode = findViewById(R.id.teachMode);

        setupInputAdapters();

        adapter = new TutorSubjectAdapter(subjectList);
        rvSubjects.setLayoutManager(new LinearLayoutManager(this));
        rvSubjects.setAdapter(adapter);


        btnAddLocal.setOnClickListener(v -> {
            String sub = subjectDropdown.getText().toString();
            String price = etPrice.getText().toString();
            String curr = currencyDropdown.getText().toString();
            String duration = etDuration.getText().toString();

            if (!sub.isEmpty() && !price.isEmpty() && !duration.isEmpty()) {
                int durationMins = Integer.parseInt(duration);
                subjectList.add(new Subject(sub, Double.parseDouble(price), curr, durationMins));
                adapter.notifyItemInserted(subjectList.size() - 1);

                etPrice.setText("");
                etDuration.setText("");
                subjectDropdown.setText(null);
            } else {
                Toast.makeText(this, "Please fill in all fields (Subject, Price, Duration)", Toast.LENGTH_SHORT).show();
            }
        });

        btnFinalFirebase.setOnClickListener(v -> {
            int checkedId = teachMode.getCheckedRadioButtonId();
            if (checkedId == -1) {
                Toast.makeText(this, "Please select a teaching mode", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String teachModeText = ((RadioButton) findViewById(checkedId)).getText().toString();
            if (subjectList.isEmpty()) {
                Toast.makeText(this, "Please add at least one subject", Toast.LENGTH_SHORT).show();
                return;
            }
            saveTeachModeToFirebase(teachModeText);
        });
    }

    private void setupInputAdapters() {
        loadSubjectsFromFirebase();

        String[] currencies = {"USD", "AMD", "EUR"};
        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, currencies);

        currencyDropdown.setAdapter(currencyAdapter);
        currencyDropdown.setText("USD", false);
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

                ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(
                        TutorSubjectActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        allSubjectsList
                );
                subjectDropdown.setAdapter(subjectAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TutorSubjectActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveTeachModeToFirebase(String teachModeText) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        FirebaseDatabase.getInstance().getReference("Users")
                .child("Tutor")
                .child(user.getUid())
                .child("teachMode")
                .setValue(teachModeText)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveToFirebase();
                    } else {
                        Toast.makeText(this, "Firebase Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void saveToFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase.getInstance().getReference("Users")
                .child("Tutor")
                .child(user.getUid())
                .child("preferences")
                .setValue(subjectList)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseDatabase.getInstance().getReference("Users")
                                .child("Tutor")
                                .child(user.getUid())
                                .child("earnedMoney")
                                .setValue(0)
                                .addOnCompleteListener(moneyTask -> {
                                    if (moneyTask.isSuccessful()) {
                                        FirebaseDatabase.getInstance().getReference("Users")
                                                .child("Tutor")
                                                .child(user.getUid())
                                                .child("lessonsCount")
                                                .setValue(0)
                                                .addOnCompleteListener(lessonTask -> {
                                                    if (lessonTask.isSuccessful()) {
                                                        startActivity(new Intent(TutorSubjectActivity.this, AddCertificatesActivity.class));
                                                        finish();
                                                    } else {
                                                        Toast.makeText(this, "Firebase Error", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                });
                    }
                    else{
                        Toast.makeText(this, "Firebase Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}