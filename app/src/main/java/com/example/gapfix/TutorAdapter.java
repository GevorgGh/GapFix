package com.example.gapfix;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;

public class TutorAdapter extends RecyclerView.Adapter<TutorAdapter.TutorViewHolder> {

    private ArrayList<Tutor> tutorList;

    public TutorAdapter(ArrayList<Tutor> tutorList) {
        this.tutorList = tutorList;
    }

    @NonNull
    @Override
    public TutorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.tutor_card, parent, false);
        return new TutorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TutorViewHolder holder, int position) {
        Tutor tutor = tutorList.get(position);

        if (tutor != null) {
            // Set Name and Bio
            holder.nameText.setText(tutor.getName());
            holder.bioText.setText(tutor.getBio());

            // Load Profile Image with Glide
            Glide.with(holder.itemView.getContext())
                    .load(tutor.getImageResourceLink() != null ? tutor.getImageResourceLink() : R.drawable.person_circle)
                    .placeholder(R.drawable.person_circle)
                    .circleCrop()
                    .into(holder.profileImage);

            // Clear old chips before adding new ones
            holder.chipGroup.removeAllViews();

            if (tutor.getPreferences() != null) {
                for (Tutor.SubjectPreference pref : tutor.getPreferences()) {
                    Chip chip = new Chip(holder.itemView.getContext());

                    // Logic: Display "Subject Name - CurrencyPrice" (e.g., Accounting - USD120)
                    String chipLabel = String.format("%s - %s%d", pref.name, pref.currency, pref.price);

                    chip.setText(chipLabel);
                    chip.setClickable(false);
                    chip.setFocusable(false);
                    chip.setCheckable(false);

                    // Optional styling to match GapFix theme
                    chip.setChipStrokeColorResource(R.color.gapfix_green);
                    chip.setChipStrokeWidth(2f);
                    chip.setChipBackgroundColorResource(android.R.color.white);

                    holder.chipGroup.addView(chip);
                }
            }

            // Click listener to open detailed Tutor Activity
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), TutorActivity.class);
                intent.putExtra("tutor", tutor);
                v.getContext().startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return tutorList != null ? tutorList.size() : 0;
    }

    public static class TutorViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, bioText;
        ChipGroup chipGroup;
        ImageView profileImage;

        public TutorViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tutor_name);
            bioText = itemView.findViewById(R.id.tutor_bio);
            chipGroup = itemView.findViewById(R.id.tutor_subjects_chips);
            profileImage = itemView.findViewById(R.id.tutor_image);

            // Note: priceText and priceRange were removed because prices are now in the Chips
        }
    }

    // Helper method to update list from Firebase
    public void updateList(ArrayList<Tutor> newList) {
        this.tutorList = newList;
        notifyDataSetChanged();
    }
}