package com.example.gapfix;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
public class TutorSubjectAdapter extends RecyclerView.Adapter<TutorSubjectAdapter.ViewHolder> {
    private List<Subject> list;
    public TutorSubjectAdapter(List<Subject> list) { this.list = list; }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tutor_subject, parent, false);
        return new ViewHolder(v);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Subject sub = list.get(position);
        holder.name.setText(SubjectHelper.getTranslatedSubject(sub.name));
        String displayInfo = String.format("%s %.0f / %d mins", sub.currency, sub.price, sub.duration);
        holder.price.setText(displayInfo);
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                list.remove(pos);
                notifyItemRemoved(pos);
            }
        });
    }
    @Override
    public int getItemCount() {
        return list.size();
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, price;
        ImageButton btnDelete;
        public ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.tvSubjectName);
            price = v.findViewById(R.id.tvPriceDisplay);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
