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
    private ArrayList<Tutor> filteredTutors;
    private TutorAdapter adapter;

    private ArrayList<Tutor> allMatchedTutors = new ArrayList<>();

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

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadStudentPreferences();
        } else {
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_LONG).show();
        }

        filteredTutors = new ArrayList<>();
        adapter = new TutorAdapter(filteredTutors);
        recyclerView.setAdapter(adapter);

        return view;
    }

    private void loadStudentPreferences() {
        mDatabase.child("Users").child("Student").child(currentUserId).child("preferences")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ArrayList<String> studentPrefs = new ArrayList<>();
                        if (snapshot.exists()) {
                            for (DataSnapshot prefSnapshot : snapshot.getChildren()) {
                                String pref = String.valueOf(prefSnapshot.getValue());
                                if (pref != null && !pref.equals("null")) {
                                    studentPrefs.add(pref.trim());
                                }
                            }
                            fetchMatchingTutors(studentPrefs);
                        } else {
                            Toast.makeText(getContext(), "Setup preferences first!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchMatchingTutors(ArrayList<String> preferences) {
        mDatabase.child("Users").child("Tutor").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                filteredTutors.clear();
                ArrayList<Tutor> matches = new ArrayList<>();

                for (DataSnapshot tutorSnapshot : snapshot.getChildren()) {
                    try {
                        String tutorName = tutorSnapshot.child("name").getValue() != null ?
                                tutorSnapshot.child("name").getValue().toString() : "Unknown";

                        String tutorBio = tutorSnapshot.child("bio").getValue() != null ?
                                tutorSnapshot.child("bio").getValue().toString() : "";

                        String tutorImage = tutorSnapshot.child("profilePicture").getValue() != null ?
                                tutorSnapshot.child("profilePicture").getValue().toString() : null;

                        int minPrice = 0;
                        int maxPrice = 0;

                        Object minObj = tutorSnapshot.child("minPrice").getValue();
                        Object maxObj = tutorSnapshot.child("maxPrice").getValue();

                        if (minObj != null) {
                            minPrice = (int) Double.parseDouble(minObj.toString());
                        }
                        if (maxObj != null) {
                            maxPrice = (int) Double.parseDouble(maxObj.toString());
                        }

                        ArrayList<String> tutorSubjects = new ArrayList<>();
                        DataSnapshot prefsSnapshot = tutorSnapshot.child("preferences");
                        if (prefsSnapshot.exists()) {
                            for (DataSnapshot sub : prefsSnapshot.getChildren()) {
                                if (sub.getValue() != null) {
                                    tutorSubjects.add(sub.getValue().toString());
                                }
                            }
                        }

                        if (hasMatch(tutorSubjects, preferences)) {
                            Tutor tutor = new Tutor(tutorName, tutorBio, tutorImage, tutorSubjects, minPrice, maxPrice, tutorSnapshot.getKey());
                            matches.add(tutor);
                        }
                    } catch (Exception e) {
                        Log.e("TUTOR_DEBUG", "Error parsing tutor: " + e.getMessage());
                    }
                }

                allMatchedTutors.clear();
                allMatchedTutors.addAll(matches);
                filteredTutors.clear();
                filteredTutors.addAll(matches);
                adapter.notifyDataSetChanged();

                if (matches.isEmpty()) {
                    Toast.makeText(getContext(), "No matches found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private boolean hasMatch(ArrayList<String> tutorSubjects, ArrayList<String> studentPrefs) {
        if (tutorSubjects == null || studentPrefs == null) return false;
        for (String sPref : studentPrefs) {
            for (String tSub : tutorSubjects) {
                if (sPref.equalsIgnoreCase(tSub)) return true;
            }
        }
        return false;
    }

    private void filterByName(String query) {
        filteredTutors.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredTutors.addAll(allMatchedTutors);
        } else {
            String lower = query.toLowerCase().trim();
            for (Tutor t : allMatchedTutors) {
                if (t.getName().toLowerCase().contains(lower)) {
                    filteredTutors.add(t);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}