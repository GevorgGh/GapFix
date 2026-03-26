package com.example.gapfix;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tutor_shop, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewTutors);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));



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
                ArrayList<Tutor> matches = new ArrayList<>();

                for (DataSnapshot tutorSnapshot : snapshot.getChildren()) {
                    String tutorName = String.valueOf(tutorSnapshot.child("name").getValue());

                    ArrayList<String> tutorSubjects = new ArrayList<>();
                    DataSnapshot prefsSnapshot = tutorSnapshot.child("preferences");
                    if (prefsSnapshot.exists()) {
                        for (DataSnapshot sub : prefsSnapshot.getChildren()) {
                            tutorSubjects.add(String.valueOf(sub.getValue()));
                        }
                    }

                    // 3. Perform the match against the student's preferences
                    if (hasMatch(tutorSubjects, preferences)) {
                        Tutor tutor = new Tutor();

                        tutor.setName(tutorName);
                        tutor.setSubjects(tutorSubjects);

                        android.util.Log.d("TUTOR_DEBUG", "tutorName variable = " + tutorName);
                        tutor.setName(tutorName);
                        android.util.Log.d("TUTOR_DEBUG", "tutor.getName() after set = " + tutor.getName());
                        matches.add(tutor);


                    }
                }
                filteredTutors.addAll(matches);
                adapter.notifyDataSetChanged();

                if (matches.isEmpty()) {
                    Toast.makeText(getContext(), "Matches: 0", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Success: Found " + matches.size(), Toast.LENGTH_SHORT).show();
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
}