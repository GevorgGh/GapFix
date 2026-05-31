package com.example.gapfix;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class ArchiveSubjectAdapter extends RecyclerView.Adapter<ArchiveSubjectAdapter.ArchiveSubjectViewHolder> {
    private List<SubjectArchiveModel> subjects = new ArrayList<>();
    private OnSubjectClickListener clickListener;
    private final java.util.Map<String, String> subjectsTranslationMap = new java.util.HashMap<>();
    private boolean isTranslating = false;
    public interface OnSubjectClickListener {
        void onSubjectClick(String subject);
    }
    public void setOnSubjectClickListener(OnSubjectClickListener clickListener) {
        this.clickListener = clickListener;
    }
    public void setSubjects(List<SubjectArchiveModel> subjects) {
        this.subjects = subjects;
        notifyDataSetChanged();
    }
    private String getTranslatedSubject(android.content.Context context, String canonical) {
        if (canonical == null) return "";
        if (subjectsTranslationMap.isEmpty() && !isTranslating) {
            loadSubjectTranslations(context);
        }
        String translated = subjectsTranslationMap.get(canonical);
        return translated != null ? translated : canonical;
    }
    private void loadSubjectTranslations(android.content.Context context) {
        isTranslating = true;
        FirebaseDatabase.getInstance().getReference("Subjects").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                subjectsTranslationMap.clear();
                String lang = LocaleHelper.getLanguage(context);
                for (DataSnapshot data : snapshot.getChildren()) {
                    Object value = data.getValue();
                    if (value instanceof String) {
                        String s = (String) value;
                        subjectsTranslationMap.put(s, s);
                    } else if (value instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, String> translations = (Map<String, String>) value;
                        String canonical = translations.get("en");
                        String translated = translations.get(lang);
                        if (translated == null) translated = canonical;
                        if (canonical != null) subjectsTranslationMap.put(canonical, translated);
                    }
                }
                notifyDataSetChanged();
                isTranslating = false;
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                isTranslating = false;
            }
        });
    }
    @NonNull
    @Override
    public ArchiveSubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_archive_subject, parent, false);
        return new ArchiveSubjectViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ArchiveSubjectViewHolder holder, int position) {
        SubjectArchiveModel model = subjects.get(position);
        String subjectCanonical = model.getSubjectName();
        String subjectDisplay = getTranslatedSubject(holder.itemView.getContext(), subjectCanonical);
        holder.tvSubjectName.setText(subjectDisplay);
        applySubjectStyle(holder, subjectCanonical);
        if (holder.tvFileCount != null) {
            holder.tvFileCount.setText(holder.itemView.getContext().getString(R.string.ext_materials_reviewed, model.getReviewedFiles(), model.getTotalFiles()));
        }
        if (holder.pbProgress != null) {
            holder.pbProgress.setProgress(model.getProgress());
        }
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onSubjectClick(subjectCanonical);
            }
        });
    }
    private void applySubjectStyle(ArchiveSubjectViewHolder holder, String subject) {
        String name = subject.toLowerCase();
        int color;
        int iconRes = R.drawable.archive;
        if (name.contains("math") || name.contains("calc") || name.contains("algebra") || name.contains("stat") || name.contains("physic")) {
            color = 0xFF1976D2; 
        } else if (name.contains("science") || name.contains("chem") || name.contains("biol") || name.contains("medicine") || name.contains("health")) {
            color = 0xFF008253; 
        } else if (name.contains("english") || name.contains("lang") || name.contains("armenian") || name.contains("russian") || name.contains("french") || name.contains("german") || name.contains("spanish")) {
            color = 0xFF388E3C; 
        } else if (name.contains("history") || name.contains("social") || name.contains("politic") || name.contains("philosophy") || name.contains("geography")) {
            color = 0xFFF57C00; 
        } else if (name.contains("tech") || name.contains("computer") || name.contains("code") || name.contains("program") || name.contains("software") || name.contains("ai")) {
            color = 0xFF303F9F; 
        } else if (name.contains("art") || name.contains("music") || name.contains("paint") || name.contains("design") || name.contains("draw") || name.contains("photo")) {
            color = 0xFF689F38; 
        } else if (name.contains("busines") || name.contains("econom") || name.contains("finan") || name.contains("market") || name.contains("manage")) {
            color = 0xFF00796B; 
        } else if (name.contains("sport") || name.contains("gym") || name.contains("danc") || name.contains("fit")) {
            color = 0xFFD32F2F; 
        } else {
            int[] vibrantPalette = {
                    0xFF0288D1, 
                    0xFFFBC02D, 
                    0xFF008253, 
                    0xFFE64A19, 
                    0xFF689F38, 
                    0xFF0097A7, 
                    0xFF5D4037, 
                    0xFF455A64, 
                    0xFF008253, 
                    0xFFFFA000, 
                    0xFF1976D2, 
                    0xFF004D40, 
                    0xFF388E3C, 
                    0xFFD32F2F  
            };
            int hash = Math.abs(subject.hashCode());
            color = vibrantPalette[hash % vibrantPalette.length];
        }
        if (holder.ivIconContainer != null) {
            holder.ivIconContainer.setCardBackgroundColor(color);
        }
        if (holder.ivSubjectIcon != null) {
            holder.ivSubjectIcon.setImageResource(iconRes);
        }
    }
    @Override
    public int getItemCount() {
        return subjects.size();
    }
    static class ArchiveSubjectViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubjectName, tvFileCount;
        ImageView ivSubjectIcon;
        com.google.android.material.card.MaterialCardView ivIconContainer;
        android.widget.ProgressBar pbProgress;
        public ArchiveSubjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubjectName = itemView.findViewById(R.id.tvSubjectName);
            tvFileCount = itemView.findViewById(R.id.tvFileCount);
            ivSubjectIcon = itemView.findViewById(R.id.ivSubjectIcon);
            ivIconContainer = itemView.findViewById(R.id.ivIconContainer);
            pbProgress = itemView.findViewById(R.id.pbSubjectProgress);
        }
    }
}