package com.example.gapfix;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
public class TutorCalendarFragment extends Fragment {
    private static final String TAG = "TutorCalendar";
    private RecyclerView rvCalendar;
    private TextView tvMonthYear;
    private TextView badgeSessions;
    private RecyclerView rvBookings;
    private View tvNoClasses;
    private DatabaseReference bookingsRef;
    private String currentUserId;
    private final List<Booking> displayedBookings = new ArrayList<>();
    private BookingTutorAdapter bookingAdapter;
    private Calendar currentCalendar;
    private Date selectedDate;
    private final Set<String> bookingDates = new HashSet<>();
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tutor_calendar, container, false);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        ImageButton btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        ImageButton btnNextMonth = view.findViewById(R.id.btnNextMonth);
        rvCalendar = view.findViewById(R.id.rvCalendar);
        badgeSessions = view.findViewById(R.id.badge_sessions);
        rvBookings = view.findViewById(R.id.rv_bookings);
        tvNoClasses = view.findViewById(R.id.tv_no_classes_container);
        currentUserId = FirebaseAuth.getInstance().getUid();
        bookingsRef = FirebaseDatabase.getInstance().getReference("Bookings");
        if (rvBookings != null) {
            rvBookings.setLayoutManager(new LinearLayoutManager(getContext()));
            bookingAdapter = new BookingTutorAdapter(displayedBookings, getContext(), true);
            rvBookings.setAdapter(bookingAdapter);
        }
        view.findViewById(R.id.btn_sessions).setOnClickListener(v -> {
            Intent i = new Intent(requireContext(), SessionsActivity.class);
            i.putExtra("role", "Tutor");
            startActivity(i);
        });
        currentCalendar = Calendar.getInstance();
        selectedDate = new Date();
        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendar();
        });
        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendar();
        });
        fetchAllLessonDates();
        loadSessionBadgeCount();
        updateCalendar();
        loadBookingsForDate(selectedDate);
        return view;
    }
    private void loadSessionBadgeCount() {
        if (currentUserId == null) return;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings");
        ref.orderByChild("tutorId").equalTo(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                int pendingCount = 0;
                Set<String> countedPackageIds = new HashSet<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Booking b = ds.getValue(Booking.class);
                    if (b == null) continue;
                    String s = b.getStatus() != null ? b.getStatus().toLowerCase() : "";
                    if (s.equals("pending") || s.equals("free_trial_pending")) {
                        if (b.isPackage() && b.getPackageId() != null) {
                            if (!countedPackageIds.contains(b.getPackageId())) {
                                countedPackageIds.add(b.getPackageId());
                                pendingCount++;
                            }
                        } else {
                            pendingCount++;
                        }
                    }
                }
                updateBadge(badgeSessions, pendingCount);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void updateBadge(TextView badge, int count) {
        if (badge == null) return;
        if (count > 0) {
            badge.setText(String.valueOf(count));
            badge.setVisibility(View.VISIBLE);
        } else {
            badge.setVisibility(View.GONE);
        }
    }
    private void fetchAllLessonDates() {
        if (currentUserId == null) return;
        bookingsRef.orderByChild("tutorId").equalTo(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookingDates.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Booking b = ds.getValue(Booking.class);
                    if (b != null && !"cancelled".equalsIgnoreCase(b.getStatus())) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(b.getTimestamp());
                        String dateKey = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH);
                        bookingDates.add(dateKey);
                    }
                }
                updateCalendar();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void updateCalendar() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(currentCalendar.getTime()));
        List<Date> days = new ArrayList<>();
        Calendar cal = (Calendar) currentCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysBefore = (firstDayOfWeek + 5) % 7; 
        cal.add(Calendar.DAY_OF_MONTH, -daysBefore);
        for (int i = 0; i < 42; i++) {
            days.add(cal.getTime());
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        CalendarMonthAdapter adapter = new CalendarMonthAdapter(days, currentCalendar.getTime(), selectedDate, bookingDates, date -> {
            selectedDate = date;
            loadBookingsForDate(date);
        });
        rvCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));
        rvCalendar.setAdapter(adapter);
    }
    private void loadBookingsForDate(Date date) {
        if (currentUserId == null) return;
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
        Query query = bookingsRef.orderByChild("tutorId").equalTo(currentUserId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                List<Booking> filteredList = new ArrayList<>();
                Set<String> seenPackageIds = new HashSet<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Booking b = ds.getValue(Booking.class);
                    if (b != null) {
                        b.setBookingId(ds.getKey());
                        long ts = b.getTimestamp();
                        if (ts >= startOfDay && ts <= endOfDay) {
                            if (b.isPackage() && b.getPackageId() != null) {
                                if (!seenPackageIds.contains(b.getPackageId())) {
                                    seenPackageIds.add(b.getPackageId());
                                    filteredList.add(b);
                                }
                            } else {
                                filteredList.add(b);
                            }
                        }
                    }
                }
                updateUI(filteredList);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                }
        });
    }
    private void updateUI(List<Booking> list) {
        displayedBookings.clear();
        if (list.isEmpty()) {
            if (tvNoClasses != null) tvNoClasses.setVisibility(View.VISIBLE);
            if (rvBookings != null) rvBookings.setVisibility(View.GONE);
        } else {
            if (tvNoClasses != null) tvNoClasses.setVisibility(View.GONE);
            if (rvBookings != null) rvBookings.setVisibility(View.VISIBLE);
            displayedBookings.addAll(list);
        }
        if (bookingAdapter != null) bookingAdapter.notifyDataSetChanged();
    }
}
