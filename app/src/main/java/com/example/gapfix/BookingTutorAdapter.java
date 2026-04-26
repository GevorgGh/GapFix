package com.example.gapfix;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        
        // --- DISPLAY ONLY LOCAL TIME ---
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String localTime = timeFormat.format(new Date(booking.getTimestamp()));
        holder.tvTime.setText(localTime);
        // ---------------------------

        String status = booking.getStatus();
        holder.tvStatus.setText(status.toUpperCase());

        // Reset visibility
        holder.layoutActions.setVisibility(View.GONE);
        holder.btnJoin.setVisibility(View.GONE);
        holder.btnCancelTutor.setVisibility(View.GONE);

        if ("pending".equals(status) || "free_trial_pending".equals(status)) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
            holder.layoutActions.setVisibility(View.VISIBLE);
        } else if ("confirmed".equals(status)) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.gapfix_green));
            holder.btnJoin.setVisibility(View.VISIBLE);
            holder.btnCancelTutor.setVisibility(View.VISIBLE); // Show cancel for confirmed lessons
            
            holder.btnJoin.removeCallbacks(holder.updateRunnable);

            LessonAlarmScheduler.schedule(context,
                    booking.getBookingId(),
                    booking.getTimestamp(),
                    booking.getSubject(),
                    "tutor");

            holder.updateRunnable = new Runnable() {
                @Override
                public void run() {
                    boolean joinable = LessonTimeHelper.isJoinable(booking, "tutor");

                    if (joinable) {
                        holder.btnJoin.setEnabled(true);
                        holder.btnJoin.setText("JOIN CLASS");
                        holder.btnJoin.setBackgroundColor(Color.parseColor("#4CAF50"));

                        holder.btnJoin.setOnClickListener(v -> {
                            Intent intent = new Intent(context, VideoCallActivity.class);
                            intent.putExtra("BOOKING_ID", booking.getBookingId());
                            intent.putExtra("IS_INCOMING", false);
                            context.startActivity(intent);
                        });
                    } else {
                        long mins = LessonTimeHelper.minutesUntilJoinable(booking, "tutor");
                        holder.btnJoin.setEnabled(false);
                        holder.btnJoin.setBackgroundColor(Color.GRAY);
                        if (mins > 60) {
                            holder.btnJoin.setText("in " + (mins/60) + "h " + (mins%60) + "m");
                        } else if (mins > 0) {
                            holder.btnJoin.setText("in " + mins + "m");
                        } else {
                            if (System.currentTimeMillis() > booking.getTimestamp()) {
                                holder.btnJoin.setText("EXPIRED");
                            } else {
                                holder.btnJoin.setText("WAITING");
                            }
                        }
                        holder.btnJoin.postDelayed(this, 30_000);
                    }
                }
            };
            holder.btnJoin.post(holder.updateRunnable);

        } else if ("cancelled".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error));
        } else {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.gapfix_text_secondary));
        }

        fetchStudentName(booking.getStudentId(), holder.tvStudentName);

        holder.btnAccept.setOnClickListener(v -> updateBookingStatus(booking.getBookingId(), "confirmed"));
        holder.btnReject.setOnClickListener(v -> showCancelDialog(booking.getBookingId(), "Decline Lesson"));
        holder.btnCancelTutor.setOnClickListener(v -> showCancelDialog(booking.getBookingId(), "Cancel Lesson"));
    }

    private void showCancelDialog(String bookingId, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage("Please provide a reason:");

        final EditText input = new EditText(context);
        input.setHint("Enter reason here...");
        builder.setView(input);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String reason = input.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(context, "Reason is required", Toast.LENGTH_SHORT).show();
            } else {
                performCancellation(bookingId, reason);
            }
        });
        builder.setNegativeButton("Back", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void performCancellation(String bookingId, String reason) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings").child(bookingId);
        ref.child("status").setValue("cancelled");
        ref.child("cancellationReason").setValue(reason)
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Action completed", Toast.LENGTH_SHORT).show());
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
            @Override public void onCancelled(@NonNull DatabaseError error) {}
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
        MaterialButton btnAccept, btnReject, btnJoin, btnCancelTutor;
        Runnable updateRunnable;

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
            btnCancelTutor = itemView.findViewById(R.id.btnCancelTutor);
        }
    }
}
