package com.example.gapfix;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TutorDashboardActivity extends AppCompatActivity {

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    TextView tvWelcome, tvEarnings, tvLessonCount, tvNoLessons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tutor_dashboard);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvEarnings = findViewById(R.id.tvEarnings);
        tvLessonCount = findViewById(R.id.tvLessonCount);
        tvNoLessons = findViewById(R.id.tvNoLessons);

        tvWelcome.setText(String.format("Hello, %s!", user.getDisplayName()));

        DatabaseReference datRef = FirebaseDatabase.getInstance().getReference("Users")
                .child("Tutor")
                .child(user.getUid());

        datRef.child("earnedMoney").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double money = snapshot.getValue(Double.class);
                    tvEarnings.setText(String.format("$%s", money));
                } else {
                    tvEarnings.setText(R.string._0_00);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        datRef.child("lessonsCount").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    int lessonsCount = snapshot.getValue(Integer.class);
                    tvLessonCount.setText(String.valueOf(lessonsCount));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}