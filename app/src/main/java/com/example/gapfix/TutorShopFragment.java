package com.example.gapfix;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.SearchView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class TutorShopFragment extends Fragment {

    private DatabaseReference mDatabase;
    private String currentUserId;
    private ArrayList<Tutor> filteredTutors = new ArrayList<>();
    private ArrayList<Tutor> allMatchedTutors = new ArrayList<>();
    private TutorAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tutor_shop, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewTutors);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        SearchView searchView = view.findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                filterByName(newText);
                return true;
            }
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterByName(query);
                return true;
            }
        });

        mDatabase = FirebaseDatabase.getInstance().getReference();
        adapter = new TutorAdapter(filteredTutors);
        recyclerView.setAdapter(adapter);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadStudentPreferences();
        }

        return view;
    }

    private void loadStudentPreferences() {
        // Fetching what the Student wants to learn (e.g., "Accounting", "Math")
        mDatabase.child("Users").child("Student").child(currentUserId).child("preferences")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ArrayList<String> studentSelectedSubjects = new ArrayList<>();
                        if (snapshot.exists()) {
                            for (DataSnapshot prefSnapshot : snapshot.getChildren()) {
                                String pref = prefSnapshot.getValue(String.class);
                                if (pref != null) {
                                    studentSelectedSubjects.add(pref.trim().toLowerCase());
                                }
                            }
                            fetchMatchingTutors(studentSelectedSubjects);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void fetchMatchingTutors(ArrayList<String> studentSubjects) {
        mDatabase.child("Users").child("Tutor").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allMatchedTutors.clear();

                for (DataSnapshot tutorSnapshot : snapshot.getChildren()) {
                    Tutor tutor = new Tutor();
                    tutor.setId(tutorSnapshot.getKey());
                    tutor.setName(tutorSnapshot.child("name").getValue(String.class));
                    tutor.setBio(tutorSnapshot.child("Bio").getValue(String.class));
                    tutor.setImageResourceLink(tutorSnapshot.child("imageResourceLink").getValue(String.class));

                    ArrayList<Tutor.SubjectPreference> tutorPrefs = new ArrayList<>();
                    DataSnapshot prefsSnapshot = tutorSnapshot.child("preferences");

                    boolean isMatch = false;
                    if (prefsSnapshot.exists()) {
                        for (DataSnapshot subSnapshot : prefsSnapshot.getChildren()) {
                            Tutor.SubjectPreference pref = subSnapshot.getValue(Tutor.SubjectPreference.class);
                            if (pref != null) {
                                tutorPrefs.add(pref);

                                // Check if this specific tutor subject matches student interest
                                if (studentSubjects.contains(pref.name.trim().toLowerCase())) {
                                    isMatch = true;
                                }
                            }
                        }
                    }
                    tutor.setPreferences(tutorPrefs);

                    // Only add to list if they teach something the student likes
                    if (isMatch) {
                        allMatchedTutors.add(tutor);
                    }
                }

                // Initial display
                filterByName("");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filterByName(String query) {
        filteredTutors.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredTutors.addAll(allMatchedTutors);
        } else {
            String lower = query.toLowerCase().trim();
            for (Tutor t : allMatchedTutors) {
                if (t.getName() != null && t.getName().toLowerCase().contains(lower)) {
                    filteredTutors.add(t);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}