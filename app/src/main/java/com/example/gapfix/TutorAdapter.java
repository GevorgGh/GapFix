package com.example.gapfix;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Locale;

public class TutorAdapter extends RecyclerView.Adapter<TutorAdapter.TutorViewHolder> {

    private final ArrayList<Tutor> tutorList;

    public TutorAdapter(ArrayList<Tutor> tutorList) {
        this.tutorList = tutorList;
    }

    public void refresh() {
        notifyDataSetChanged();
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
        holder.bind(tutor);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), TutorActivity.class);
            intent.putExtra("tutor", tutor);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return tutorList.size();
    }

    public static class TutorViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName, tvBio;
        private final ImageView ivProfile;
        private final LinearLayout subjectsContainer;

        public TutorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tutor_name);
            tvBio = itemView.findViewById(R.id.tutor_bio);
            ivProfile = itemView.findViewById(R.id.tutor_image);
            subjectsContainer = itemView.findViewById(R.id.subjects_container);
        }

        public void bind(Tutor tutor) {
            tvName.setText(tutor.getName());
            tvBio.setText(tutor.getBio());

            Glide.with(itemView.getContext())
                    .load(tutor.getImageResourceLink() != null ? tutor.getImageResourceLink() : R.drawable.person_circle)
                    .placeholder(R.drawable.person_circle)
                    .into(ivProfile);

            subjectsContainer.removeAllViews();
            if (tutor.getPreferences() != null) {
                for (Tutor.SubjectPreference pref : tutor.getPreferences()) {
                    View subjectView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.item_tutor_subject_shop, subjectsContainer, false);
                    
                    TextView tvSubjectName = subjectView.findViewById(R.id.tvSubjectName);
                    TextView tvPriceTime = subjectView.findViewById(R.id.tvPriceTime);

                    tvSubjectName.setText(SubjectHelper.getTranslatedSubject(pref.name));
                    
                    String currency = (pref.currency != null) ? pref.currency : "$";
                    int duration = pref.duration > 0 ? pref.duration : 60;
                    
                    tvPriceTime.setText(String.format(Locale.getDefault(), "%s%d • %d mins", 
                            currency, (int) pref.price, duration));
                    
                    if (subjectsContainer.getChildCount() > 0) {
                        
                        subjectView.setPadding(0, 4, 0, 0);
                    }

                    subjectsContainer.addView(subjectView);
                }
            }
        }
    }
}