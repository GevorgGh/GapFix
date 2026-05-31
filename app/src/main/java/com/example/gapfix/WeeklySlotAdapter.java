package com.example.gapfix;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import androidx.fragment.app.FragmentManager;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
public class WeeklySlotAdapter extends RecyclerView.Adapter<WeeklySlotAdapter.ViewHolder> {
    private List<WeeklySlot> slots;
    private FragmentManager fragmentManager;
    private long globalStartDateMs = -1;
    public WeeklySlotAdapter(List<WeeklySlot> slots, FragmentManager fragmentManager) {
        this.slots = slots;
        this.fragmentManager = fragmentManager;
    }
    public void setGlobalStartDate(long localStartDateMs) {
        this.globalStartDateMs = localStartDateMs;
        for (WeeklySlot slot : slots) {
            slot.startDateMs = computeFirstOccurrence(localStartDateMs, slot.dayName);
        }
        notifyDataSetChanged();
    }
    private long computeFirstOccurrence(long baseMs, String dayName) {
        int targetDay = getDayOfWeek(dayName);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(baseMs);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        int currentDay = cal.get(Calendar.DAY_OF_WEEK);
        int daysToAdd = (targetDay - currentDay + 7) % 7;
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd);
        return cal.getTimeInMillis();
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weekly_slot, parent, false);
        return new ViewHolder(v);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeeklySlot slot = slots.get(position);
        holder.tvDay.setText(slot.dayName);
        if (slot.startDateMs != -1) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            holder.tvStartDate.setText(sdf.format(new java.util.Date(slot.startDateMs)));
        } else {
            holder.tvStartDate.setText("— pick start date above —");
        }
        if (slot.hour != -1) {
            holder.btnTime.setText(String.format(Locale.getDefault(), "%02d:%02d", slot.hour, slot.minute));
        } else {
            holder.btnTime.setText("Time");
        }
        holder.btnTime.setOnClickListener(v -> {
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(12).setMinute(0)
                    .setTitleText("Select Time for " + slot.dayName)
                    .build();
            timePicker.addOnPositiveButtonClickListener(tp -> {
                slot.hour = timePicker.getHour();
                slot.minute = timePicker.getMinute();
                notifyItemChanged(position);
            });
            timePicker.show(fragmentManager, "TIME_PICKER_" + position);
        });
    }
    private int getDayOfWeek(String dayName) {
        switch (dayName) {
            case "Sun": return Calendar.SUNDAY;
            case "Mon": return Calendar.MONDAY;
            case "Tue": return Calendar.TUESDAY;
            case "Wed": return Calendar.WEDNESDAY;
            case "Thu": return Calendar.THURSDAY;
            case "Fri": return Calendar.FRIDAY;
            case "Sat": return Calendar.SATURDAY;
            default:    return Calendar.MONDAY;
        }
    }
    @Override
    public int getItemCount() { return slots.size(); }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvStartDate;
        MaterialButton btnTime;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay       = itemView.findViewById(R.id.tvDayName);
            tvStartDate = itemView.findViewById(R.id.tvSlotStartDate);
            btnTime     = itemView.findViewById(R.id.btnSlotTime);
        }
    }
}
