package com.example.gapfix;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

public class TutorCalendarFragment extends Fragment {

    private static final String TAG = "CalendarDebug";
    private RecyclerView rvDates, rvBookings;
    private TextView tvNoClasses;
    private DatabaseReference bookingsRef;
    private String currentUserId;

    private List<Booking> displayedBookings = new ArrayList<>();
    private BookingTutorAdapter bookingAdapter;
    private DateAdapter dateAdapter;
    private List<DateModel> dateList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tutor_calendar, container, false);

        rvDates = view.findViewById(R.id.rv_dates);
        rvBookings = view.findViewById(R.id.rv_bookings);
        tvNoClasses = view.findViewById(R.id.tv_no_classes);

        currentUserId = FirebaseAuth.getInstance().getUid();
        bookingsRef = FirebaseDatabase.getInstance().getReference("Bookings");

        rvBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        bookingAdapter = new BookingTutorAdapter(displayedBookings, getContext());
        rvBookings.setAdapter(bookingAdapter);

        populateDateList();
        setupDateList();

        // Load today by default
        loadBookingsForDate(new Date());

        return view;
    }

    private void populateDateList() {
        dateList.clear();
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 30; i++) {
            dateList.add(new DateModel(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void setupDateList() {
        dateAdapter = new DateAdapter(dateList, dateModel -> {
            loadBookingsForDate(dateModel.getFullDate());
        });

        rvDates.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvDates.setAdapter(dateAdapter);
    }

    private void loadBookingsForDate(Date date) {
        if (currentUserId == null) return;

        // Calculate the range for the entire day in milliseconds
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

        Log.d(TAG, "Filtering between: " + startOfDay + " and " + endOfDay);

        Query query = bookingsRef.orderByChild("tutorId").equalTo(currentUserId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                
                List<Booking> filteredList = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Booking b = ds.getValue(Booking.class);
                    if (b != null) {
                        long ts = b.getTimestamp();
                        // Check if booking timestamp falls within the selected day
                        if (ts >= startOfDay && ts <= endOfDay) {
                            filteredList.add(b);
                        }
                    }
                }
                updateUI(filteredList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error: " + error.getMessage());
            }
        });
    }

    private void updateUI(List<Booking> list) {
        displayedBookings.clear();
        if (list.isEmpty()) {
            tvNoClasses.setVisibility(View.VISIBLE);
            rvBookings.setVisibility(View.GONE);
        } else {
            tvNoClasses.setVisibility(View.GONE);
            rvBookings.setVisibility(View.VISIBLE);
            displayedBookings.addAll(list);
        }
        bookingAdapter.notifyDataSetChanged();
    }
}
