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

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class CalendarMonthAdapter extends RecyclerView.Adapter<CalendarMonthAdapter.DayViewHolder> {

    private final List<Date> days;
    private final Date currentMonth;
    private Date selectedDate;
    private final Set<String> lessonDates; 
    private final OnDateClickListener listener;

    public interface OnDateClickListener {
        void onDateClick(Date date);
    }

    public CalendarMonthAdapter(List<Date> days, Date currentMonth, Date selectedDate, Set<String> lessonDates, OnDateClickListener listener) {
        this.days = days;
        this.currentMonth = currentMonth;
        this.selectedDate = selectedDate;
        this.lessonDates = lessonDates;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        Date date = days.get(position);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        
        int day = cal.get(Calendar.DAY_OF_MONTH);
        holder.tvDayNumber.setText(String.valueOf(day));

        Calendar monthCal = Calendar.getInstance();
        monthCal.setTime(currentMonth);
        
        boolean isSameMonth = cal.get(Calendar.MONTH) == monthCal.get(Calendar.MONTH) && 
                             cal.get(Calendar.YEAR) == monthCal.get(Calendar.YEAR);
        
        if (!isSameMonth) {
            holder.tvDayNumber.setAlpha(0.2f);
        } else {
            holder.tvDayNumber.setAlpha(1.0f);
        }

        String dateKey = formatDate(date);
        boolean hasLesson = lessonDates.contains(dateKey);
        boolean isSelected = isSameDay(date, selectedDate);

        if (isSelected) {
            holder.cardDay.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.gapfix_green));
            holder.tvDayNumber.setTextColor(Color.WHITE);
        } else if (hasLesson) {
            
            holder.cardDay.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_success_bg));
            holder.tvDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.gapfix_green));
        } else {
            holder.cardDay.setCardBackgroundColor(Color.TRANSPARENT);
            holder.tvDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.gapfix_text_dark));
        }

        holder.itemView.setOnClickListener(v -> {
            selectedDate = date;
            notifyDataSetChanged();
            listener.onDateClick(date);
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    private String formatDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH);
    }

    private boolean isSameDay(Date d1, Date d2) {
        if (d1 == null || d2 == null) return false;
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(d1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(d2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    public static class DayViewHolder extends RecyclerView.ViewHolder {
        public TextView tvDayNumber;
        public MaterialCardView cardDay;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            cardDay = itemView.findViewById(R.id.cardDay);
        }
    }
}
