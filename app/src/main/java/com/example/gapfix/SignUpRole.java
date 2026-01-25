package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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
        MaterialCardView card1 = findViewById(R.id.card1);
        MaterialCardView card2 = findViewById(R.id.card2);
        int activeColor = ContextCompat.getColor(this, R.color.gapfix_green);
        int inactiveColor = ContextCompat.getColor(this, R.color.gray);
        cont.setVisibility(View.INVISIBLE);



        card1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                card1.setStrokeColor(activeColor);
                card2.setStrokeColor(inactiveColor);
                cont.setVisibility(View.VISIBLE);
                prof = "tutor";
            }
        });

        card2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                card2.setStrokeColor(activeColor);
                card1.setStrokeColor(inactiveColor);
                cont.setVisibility(View.VISIBLE);
                prof = "student";
            }
        });

        cont.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SignUpRole.this, SignUpActivity.class);
                i.putExtra("ROLE",prof);
                startActivity(i);
            }
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}