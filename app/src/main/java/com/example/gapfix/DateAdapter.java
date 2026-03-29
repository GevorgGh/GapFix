package com.example.gapfix;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class DateAdapter extends RecyclerView.Adapter<DateAdapter.DateViewHolder> {
    private List<DateModel> dateList;
    private int selectedPosition = 0;
    private OnDateClickListener listener;

    public interface OnDateClickListener {
        void onDateClick(DateModel date);
    }

    public DateAdapter(List<DateModel> dateList, OnDateClickListener listener) {
        this.dateList = dateList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_date_circle, parent, false);
        return new DateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        DateModel date = dateList.get(position);
        holder.tvDayName.setText(date.getDayName());
        holder.tvDateNumber.setText(date.getDayNumber());

        // Highlight if selected
        if (position == selectedPosition) {
            holder.cardCircle.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.gapfix_green_background)
            );
            holder.tvDateNumber.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.gapfix_green)
            );
        } else {
            holder.cardCircle.setCardBackgroundColor(Color.parseColor("#E0E0E0"));
            holder.tvDateNumber.setTextColor(Color.BLACK);
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            listener.onDateClick(date);
        });
    }

    @Override
    public int getItemCount() { return dateList.size(); }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayName, tvDateNumber;
        MaterialCardView cardCircle;

        public DateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayName = itemView.findViewById(R.id.tv_day_name);
            tvDateNumber = itemView.findViewById(R.id.tv_date_number);
            cardCircle = itemView.findViewById(R.id.date_circle_card);
        }
    }
}
