package com.example.gapfix;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
public class TutorSubjectFragment extends Fragment {
    private AutoCompleteTextView subjectDropdown, currencyDropdown;
    private TextInputEditText etPrice, etDuration;
    private MaterialButton btnAddSubject, btnSaveAll; 
    private RecyclerView rvSubjects;
    private TutorSubjectAdapter adapter;
    private List<Subject> subjectList = new ArrayList<>();
    private List<String> allSubjectsList = new ArrayList<>();
    private final java.util.Map<String, String> canonicalToTranslatedMap = new java.util.HashMap<>();
    private final java.util.Map<String, String> translatedToCanonicalMap = new java.util.HashMap<>();
    private DatabaseReference tutorRef;
    private FirebaseUser user;
    public TutorSubjectFragment() {
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tutor_subject, container, false);
        subjectDropdown = view.findViewById(R.id.subjectDropdown);
        currencyDropdown = view.findViewById(R.id.currencyDropdown);
        etPrice = view.findViewById(R.id.etPrice);
        etDuration = view.findViewById(R.id.etDuration); 
        btnAddSubject = view.findViewById(R.id.btnAddSubject);
        btnSaveAll = view.findViewById(R.id.btnSaveAll);
        rvSubjects = view.findViewById(R.id.rvSubjects);
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tutorRef = FirebaseDatabase.getInstance().getReference("Users")
                    .child("Tutor")
                    .child(user.getUid());
        }
        setupRecyclerView();
        setupInputAdapters();
        loadCurrentSubjects();
        btnAddSubject.setOnClickListener(v -> addSubjectToList());
        btnSaveAll.setOnClickListener(v -> saveToFirebase());
        return view;
    }
    private void setupRecyclerView() {
        adapter = new TutorSubjectAdapter(subjectList);
        rvSubjects.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSubjects.setAdapter(adapter);
    }
    private void setupInputAdapters() {
        loadAvailableSubjectsFromFirebase();
        String[] currencies = {"USD", "AMD", "EUR"};
        if (getContext() != null) {
            ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_dropdown_item_1line, currencies);
            currencyDropdown.setAdapter(currencyAdapter);
            currencyDropdown.setText("USD", false);
        }
    }
    private void loadAvailableSubjectsFromFirebase() {
        DatabaseReference subjectsRef = FirebaseDatabase.getInstance().getReference("Subjects");
        subjectsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                allSubjectsList.clear();
                canonicalToTranslatedMap.clear();
                translatedToCanonicalMap.clear();
                String lang = LocaleHelper.getLanguage(requireContext());
                for (DataSnapshot data : snapshot.getChildren()) {
                    String canonicalName = null;
                    String translatedName = null;
                    Object value = data.getValue();
                    if (value instanceof String) {
                        canonicalName = (String) value;
                        translatedName = canonicalName;
                    } else if (value instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
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
                if (getContext() != null) {
                    ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(
                            getContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            allSubjectsList
                    );
                    subjectDropdown.setAdapter(subjectAdapter);
                }
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void loadCurrentSubjects() {
        if (tutorRef == null) return;
        tutorRef.child("preferences").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                subjectList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Subject s = data.getValue(Subject.class);
                    if (s != null) subjectList.add(s);
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void addSubjectToList() {
        String sub = subjectDropdown.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String curr = currencyDropdown.getText().toString().trim();
        String durationStr = etDuration.getText().toString().trim();
        if (sub.isEmpty() || priceStr.isEmpty() || durationStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields (Subject, Price, Duration)", Toast.LENGTH_SHORT).show();
            return;
        }
        String canonicalName = translatedToCanonicalMap.get(sub);
        if (canonicalName == null) canonicalName = sub; 
        try {
            double price = Double.parseDouble(priceStr);
            int duration = Integer.parseInt(durationStr);
            subjectList.add(new Subject(canonicalName, price, curr, duration));
            adapter.notifyItemInserted(subjectList.size() - 1);
            subjectDropdown.setText("");
            etPrice.setText("");
            etDuration.setText("");
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
        }
    }
    private void saveToFirebase() {
        if (tutorRef == null || subjectList.isEmpty()) {
            Toast.makeText(getContext(), "Please add at least one subject", Toast.LENGTH_SHORT).show();
            return;
        }
        tutorRef.child("preferences").setValue(subjectList)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Subjects saved successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to save subjects", Toast.LENGTH_SHORT).show());
    }
}
