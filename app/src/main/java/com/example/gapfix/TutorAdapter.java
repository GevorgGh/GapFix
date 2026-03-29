package com.example.gapfix;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
            String displayName = tutor.getName();
            holder.nameText.setText(displayName);

            holder.bioText.setText(tutor.getBio());

            if (tutor.getImageResourceLink() != null) {
                Glide.with(holder.itemView.getContext())
                        .load(tutor.getImageResourceLink())
                        .into(holder.profileImage);
            } else{
                holder.profileImage.setImageResource(R.drawable.person_circle);
            }

            holder.priceText.setText(String.format("From $%d to $%d", tutor.getMinPrice(), tutor.getMaxPrice()));

            holder.chipGroup.removeAllViews();
            if (tutor.getSubjects() != null) {
                for (String subject : tutor.getSubjects()) {
                    Chip chip = new Chip(holder.itemView.getContext());
                    chip.setClickable(false);
                    chip.setFocusable(false);
                    chip.setCheckable(false);
                    chip.setText(subject);
                    holder.chipGroup.addView(chip);
                }
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), TutorActivity.class);
                intent.putExtra("tutor", tutor);
                v.getContext().startActivity(intent);
            });
        }

        if (holder.nameText == null) {
            android.util.Log.e("TUTOR_DEBUG", "nameText is NULL - check view ID in tutor_card.xml");
            return;
        }
        android.util.Log.d("TUTOR_DEBUG", "Setting name: " + tutor.getName());
        holder.nameText.setText(tutor.getName());
    }

    @Override
    public int getItemCount() { return tutorList.size(); }

    public static class TutorViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, bioText, priceText;
        ChipGroup chipGroup;
        ImageView profileImage;
        public TutorViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tutor_name);
            bioText = itemView.findViewById(R.id.tutor_bio);
            chipGroup = itemView.findViewById(R.id.tutor_subjects_chips);
            profileImage = itemView.findViewById(R.id.tutor_image);
            priceText = itemView.findViewById(R.id.priceRange);
        }
    }
}