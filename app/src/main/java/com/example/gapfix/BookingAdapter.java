package com.example.gapfix;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {
    private static final String TAG = "BookingAdapter_Debug";
    private List<Booking> bookingList;
    private Context context;

    public BookingAdapter(Context context, List<Booking> bookingList) {
        this.context = context;
        this.bookingList = bookingList;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        // 1. Logic for Auto-Update (Pending -> Cancelled if time passed)
        String currentStatus = booking.getStatus();
        if ("pending".equalsIgnoreCase(currentStatus) && checkIfTimePassed(booking.getLessonDate(), booking.getLessonTime())) {
            currentStatus = "cancelled";
            autoUpdateStatusInFirebase(booking.getBookingId(), "cancelled");
            Log.d(TAG, "Booking " + booking.getBookingId() + " auto-marked as cancelled.");
        }

        holder.tvSubject.setText(booking.getSubject());
        holder.tvTime.setText(booking.getLessonTime());
        holder.tvStatus.setText("• " + currentStatus);

        // 2. Fetch Tutor Data
        String tutorId = booking.getTutorId();
        if (tutorId != null) {
            DatabaseReference tutorRef = FirebaseDatabase.getInstance().getReference("Users").child("Tutor").child(tutorId);
            tutorRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String imageUrl = snapshot.child("profileImage").getValue(String.class);
                        holder.tvTutorName.setText(name != null ? name : "Unknown Tutor");
                        Glide.with(context).load(imageUrl).placeholder(R.drawable.person_circle).into(holder.tutorImage);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) { Log.e(TAG, error.getMessage()); }
            });
        }

        // 3. UI logic based on Status
        holder.btnAction.setVisibility(View.VISIBLE);
        holder.btnAction.setEnabled(true);

        if ("confirmed".equalsIgnoreCase(currentStatus)) {
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            holder.btnAction.setText("JOIN");
        } else if ("pending".equalsIgnoreCase(currentStatus)) {
            holder.tvStatus.setTextColor(Color.parseColor("#FFA000"));
            holder.btnAction.setText("WAITING...");
            holder.btnAction.setEnabled(false);
        } else if ("cancelled".equalsIgnoreCase(currentStatus)) {
            holder.tvStatus.setTextColor(Color.RED);
            holder.btnAction.setVisibility(View.GONE);
        }
    }

    // Helper: Check if the session time has already passed
    private boolean checkIfTimePassed(String dateStr, String timeStr) {
        try {
            // Extracts "14:00" from "14:00 - 15:00"
            String startTime = timeStr.split("-")[0].trim();
            // Matches your Firebase format: "Mar 30, 2026 14:00"
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US);
            Date lessonDate = sdf.parse(dateStr + " " + startTime);

            return new Date().after(lessonDate); // Returns true if 'now' is after lesson start
        } catch (Exception e) {
            Log.e(TAG, "Date Parse Error: " + e.getMessage());
            return false;
        }
    }

    // Helper: Push the status change to Firebase
    private void autoUpdateStatusInFirebase(String bookingId, String newStatus) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings").child(bookingId);
        ref.child("status").setValue(newStatus);
    }

    @Override
    public int getItemCount() { return (bookingList != null) ? bookingList.size() : 0; }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvTutorName, tvSubject, tvTime, tvStatus;
        MaterialButton btnAction;
        ImageView tutorImage;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvTutorName = itemView.findViewById(R.id.tv_tutor_name);
            tvSubject = itemView.findViewById(R.id.tv_subject);
            tvTime = itemView.findViewById(R.id.tv_time);
            btnAction = itemView.findViewById(R.id.btn_action);
            tutorImage = itemView.findViewById(R.id.tutor_image);
        }
    }
}