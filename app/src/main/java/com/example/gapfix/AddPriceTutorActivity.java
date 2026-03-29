package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.slider.RangeSlider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class AddPriceTutorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_price_tutor);

        RadioGroup teachMode = findViewById(R.id.teachMode);

        RangeSlider priceRangeSlider = findViewById(R.id.priceRangeSlider);

        TextView priceRangeText = findViewById(R.id.priceRangeText);

        Button addInfo = findViewById(R.id.addInfo);

        priceRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            int min = Math.round(values.get(0));
            int max = Math.round(values.get(1));

            priceRangeText.setText(String.format("Price Range: $%d - $%d", min, max));
        });


        addInfo.setOnClickListener(v -> {
            if(teachMode.getCheckedRadioButtonId() == -1){
                Toast.makeText(this, "Please select a teach mode", Toast.LENGTH_SHORT).show();
            } else{
                RadioButton rb = findViewById(teachMode.getCheckedRadioButtonId());
                String teachModeText = rb.getText().toString();
                addInfoToFirebase(teachModeText,  priceRangeSlider.getValues().get(0).intValue(), priceRangeSlider.getValues().get(1).intValue());
            }
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void addInfoToFirebase(String teachMode, int minPrice, int maxPrice) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        FirebaseDatabase.getInstance().getReference("Users")
                .child("Tutor")
                .child(user.getUid())
                .child("teachMode")
                .setValue(teachMode)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseDatabase.getInstance().getReference("Users")
                                .child("Tutor")
                                .child(user.getUid())
                                .child("minPrice")
                                .setValue(minPrice)
                                .addOnCompleteListener(priceTask -> {
                                    FirebaseDatabase.getInstance().getReference("Users")
                                            .child("Tutor")
                                            .child(user.getUid())
                                            .child("maxPrice")
                                            .setValue(maxPrice)
                                            .addOnCompleteListener(priceTask2 -> {
                                                if (priceTask2.isSuccessful()) {
                                                    Toast.makeText(this, "Info added successfully!", Toast.LENGTH_SHORT).show();
                                                    startActivity(new Intent(this, HomeTutorActivity.class));
                                                    finish();
                                                } else {
                                                    Toast.makeText(this, "Firebase Error", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                });
                    } else {
                        Toast.makeText(this, "Firebase Error", Toast.LENGTH_SHORT).show();
                    }
                });

    }
}