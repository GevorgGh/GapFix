package com.example.gapfix;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class StudentCalendarFragment extends Fragment {

    private static final String TAG = "StudentCalendar";
    private RecyclerView rvDates, rvBookings;
    private TextView tvNoClasses;
    private DateAdapter dateAdapter;
    private BookingAdapter bookingAdapter;
    private List<DateModel> dateList;
    private List<Booking> bookingList;
    private String currentStudentId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_calendar, container, false);

        currentStudentId = FirebaseAuth.getInstance().getUid();

        rvDates = view.findViewById(R.id.rv_dates);
        rvBookings = view.findViewById(R.id.rv_bookings);
        tvNoClasses = view.findViewById(R.id.tv_no_classes);

        setupDatePicker();

        bookingList = new ArrayList<>();
        bookingAdapter = new BookingAdapter(requireContext(), bookingList);
        rvBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBookings.setAdapter(bookingAdapter);

        // Load today's classes by default
        loadBookingsForDate(new Date());

        return view;
    }

    private void setupDatePicker() {
        dateList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        // Show the next 14 days
        for (int i = 0; i < 14; i++) {
            dateList.add(new DateModel(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        dateAdapter = new DateAdapter(dateList, dateModel -> {
            loadBookingsForDate(dateModel.getFullDate());
        });

        rvDates.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvDates.setAdapter(dateAdapter);
    }

    private void loadBookingsForDate(Date date) {
        if (currentStudentId == null) return;

        // Calculate the range for the entire day in milliseconds (Local Time)
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long endOfDay = cal.getTimeInMillis();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        Query query = ref.orderByChild("studentId").equalTo(currentStudentId);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                bookingList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Booking booking = data.getValue(Booking.class);
                    if (booking != null) {
                        long ts = booking.getTimestamp();
                        // Check if booking falls within the selected 24-hour window
                        if (ts >= startOfDay && ts <= endOfDay) {
                            bookingList.add(booking);
                        }
                    }
                }

                if (bookingList.isEmpty()) {
                    tvNoClasses.setVisibility(View.VISIBLE);
                    rvBookings.setVisibility(View.GONE);
                } else {
                    tvNoClasses.setVisibility(View.GONE);
                    rvBookings.setVisibility(View.VISIBLE);
                }

                bookingAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
