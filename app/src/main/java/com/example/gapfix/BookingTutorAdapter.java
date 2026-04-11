package com.example.gapfix;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class BookingTutorAdapter extends RecyclerView.Adapter<BookingTutorAdapter.BookingViewHolder> {

    private List<Booking> bookingList;
    private Context context;

    public BookingTutorAdapter(List<Booking> bookingList, Context context) {
        this.bookingList = bookingList;
        this.context = context;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking_tutor, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        holder.tvSubject.setText(booking.getSubject());
        holder.tvTime.setText(booking.getLessonTime());

        // 1. Handle Status UI
        String status = booking.getStatus();
        holder.tvStatus.setText(status.toUpperCase());

        if ("pending".equals(status) || "free_trial_pending".equals(status)) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.btnJoin.setVisibility(View.GONE);
        } else if ("confirmed".equals(status)) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.gapfix_green));
            holder.layoutActions.setVisibility(View.GONE);
            holder.btnJoin.setVisibility(View.VISIBLE);
        } else {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error));
            holder.layoutActions.setVisibility(View.GONE);
            holder.btnJoin.setVisibility(View.GONE);
        }

        // 2. Fetch Student Name
        fetchStudentName(booking.getStudentId(), holder.tvStudentName);

        // 3. Button Click Listeners
        holder.btnAccept.setOnClickListener(v -> updateBookingStatus(booking.getBookingId(), "confirmed"));
        holder.btnReject.setOnClickListener(v -> updateBookingStatus(booking.getBookingId(), "cancelled"));

        holder.btnJoin.setOnClickListener(v -> {
            // Logic to start the lesson
            Toast.makeText(context, "Starting Lesson...", Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchStudentName(String studentId, TextView tvName) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child("Student").child(studentId);
        ref.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    tvName.setText(snapshot.getValue(String.class));
                } else {
                    tvName.setText("Unknown Student");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateBookingStatus(String bookingId, String newStatus) {
        FirebaseDatabase.getInstance().getReference("Bookings")
                .child(bookingId)
                .child("status")
                .setValue(newStatus)
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Lesson " + newStatus, Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvSubject, tvTime, tvStatus;
        LinearLayout layoutActions;
        MaterialButton btnAccept, btnReject, btnJoin;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvSubject = itemView.findViewById(R.id.tvLessonSubject);
            tvTime = itemView.findViewById(R.id.tvLessonTime);
            tvStatus = itemView.findViewById(R.id.tvLessonStatus);
            layoutActions = itemView.findViewById(R.id.layoutLessonActions);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnJoin = itemView.findViewById(R.id.btnJoin);
        }
    }
}