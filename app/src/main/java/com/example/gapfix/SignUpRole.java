package com.example.gapfix;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.card.MaterialCardView;
public class SignUpRole extends AppCompatActivity {
    String prof;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up_role);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.backFr, new BackFragment())
                .commit();
        Button cont = findViewById(R.id.button2);
        cont.setVisibility(View.INVISIBLE);
        cont.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SignUpRole.this, SignUpActivity.class);
                i.putExtra("ROLE", prof);
                i.putExtra("isGoogle", getIntent().getBooleanExtra("isGoogle", false));
                startActivity(i);
            }
        });
        ConstraintLayout cardTutor = findViewById(R.id.tutorCard);
        ConstraintLayout cardStudent = findViewById(R.id.studentCard);
        View.OnClickListener selectionListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardTutor.setSelected(false);
                cardStudent.setSelected(false);
                v.setSelected(true);
                v.setAlpha(1F);
                cont.setVisibility(View.VISIBLE);
                if (v.getId() == R.id.tutorCard) {
                    prof = "Tutor";
                    cardStudent.setAlpha(0.5F);
                } else if (v.getId() == R.id.studentCard){
                    prof = "Student";
                    cardTutor.setAlpha(0.5F);
                }
            }
        };
        cardTutor.setOnClickListener(selectionListener);
        cardStudent.setOnClickListener(selectionListener);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}