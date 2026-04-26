package com.example.gapfix;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

        String currentStatus = booking.getStatus();
        if (!"done".equalsIgnoreCase(currentStatus) && !"cancelled".equalsIgnoreCase(currentStatus) && !"finished".equalsIgnoreCase(currentStatus)) {
            if (System.currentTimeMillis() > booking.getTimestamp() + (60 * 60_000L)) {
                currentStatus = "cancelled";
                autoUpdateStatusInFirebase(booking.getBookingId(), "cancelled");
            }
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String localTime = timeFormat.format(new Date(booking.getTimestamp()));
        
        holder.tvSubject.setText(booking.getSubject());
        holder.tvTime.setText(localTime);
        holder.tvStatus.setText(String.format("• %s", currentStatus));

        String tutorId = booking.getTutorId();
        if (tutorId != null) {
            DatabaseReference tutorRef = FirebaseDatabase.getInstance().getReference("Users").child("Tutor").child(tutorId);
            tutorRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String imageUrl = snapshot.child("profilePicture").getValue(String.class);
                        holder.tvTutorName.setText(name != null ? name : "Unknown Tutor");
                        Glide.with(context).load(imageUrl).placeholder(R.drawable.person_circle).into(holder.tutorImage);
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) { Log.e(TAG, error.getMessage()); }
            });
        }

        holder.btnAction.setVisibility(View.VISIBLE);
        holder.btnAction.setEnabled(true);
        if (holder.updateRunnable != null) holder.btnAction.removeCallbacks(holder.updateRunnable);

        if ("confirmed".equalsIgnoreCase(currentStatus)) {
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
            holder.btnCancel.setVisibility(View.VISIBLE);
            
            holder.btnCancel.setOnClickListener(v -> showCancelDialog(booking.getBookingId()));

            holder.updateRunnable = new Runnable() {
                @Override
                public void run() {
                    boolean joinable = LessonTimeHelper.isJoinable(booking, "student");

                    if (joinable) {
                        holder.btnAction.setEnabled(true);
                        holder.btnAction.setText("JOIN");
                        holder.btnAction.setBackgroundColor(Color.parseColor("#4CAF50"));

                        holder.btnAction.setOnClickListener(v -> {
                            Intent intent = new Intent(context, VideoCallActivity.class);
                            intent.putExtra("BOOKING_ID", booking.getBookingId());
                            intent.putExtra("IS_INCOMING", false);
                            context.startActivity(intent);
                        });
                    } else {
                        long mins = LessonTimeHelper.minutesUntilJoinable(booking, "student");
                        holder.btnAction.setEnabled(false);
                        holder.btnAction.setBackgroundColor(Color.GRAY);
                        if (mins > 60) {
                            holder.btnAction.setText("in " + (mins/60) + "h " + (mins%60) + "m");
                        } else if (mins > 0) {
                            holder.btnAction.setText("in " + mins + "m");
                        } else {
                            if (System.currentTimeMillis() > booking.getTimestamp()) {
                                holder.btnAction.setText("EXPIRED");
                            } else {
                                holder.btnAction.setText("WAITING");
                            }
                        }
                        holder.btnAction.postDelayed(this, 30_000);
                    }
                }
            };
            holder.btnAction.post(holder.updateRunnable);

        } else if ("pending".equalsIgnoreCase(currentStatus) || "free_trial_pending".equalsIgnoreCase(currentStatus)) {
            holder.tvStatus.setTextColor(Color.parseColor("#FFA000"));
            holder.btnAction.setText("WAITING...");
            holder.btnAction.setBackgroundColor(Color.GRAY);
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnAction.setEnabled(false);
            holder.btnCancel.setOnClickListener(v -> showCancelDialog(booking.getBookingId()));
        } else if ("cancelled".equalsIgnoreCase(currentStatus)) {
            holder.tvStatus.setTextColor(Color.RED);
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnAction.setVisibility(View.GONE);
        } else if ("finished".equalsIgnoreCase(currentStatus)) {
            holder.tvStatus.setTextColor(Color.BLUE);
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnAction.setVisibility(View.GONE);
        }
    }

    private void showCancelDialog(String bookingId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Cancel Lesson");
        builder.setMessage("Please provide a reason for cancellation:");

        final EditText input = new EditText(context);
        input.setHint("Enter reason here...");
        builder.setView(input);

        builder.setPositiveButton("Confirm Cancel", (dialog, which) -> {
            String reason = input.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(context, "Reason is required to cancel", Toast.LENGTH_SHORT).show();
            } else {
                performCancellation(bookingId, reason);
            }
        });
        builder.setNegativeButton("Keep Lesson", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void performCancellation(String bookingId, String reason) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings").child(bookingId);
        ref.child("status").setValue("cancelled");
        ref.child("cancellationReason").setValue(reason)
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Lesson cancelled", Toast.LENGTH_SHORT).show());
    }

    private void autoUpdateStatusInFirebase(String bookingId, String newStatus) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Bookings").child(bookingId);
        ref.child("status").setValue(newStatus);
    }

    @Override
    public int getItemCount() { return (bookingList != null) ? bookingList.size() : 0; }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvTutorName, tvSubject, tvTime, tvStatus;
        MaterialButton btnAction, btnCancel;
        ImageView tutorImage;
        Runnable updateRunnable;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvTutorName = itemView.findViewById(R.id.tv_tutor_name);
            tvSubject = itemView.findViewById(R.id.tv_subject);
            tvTime = itemView.findViewById(R.id.tv_time);
            btnAction = itemView.findViewById(R.id.btn_action);
            btnCancel = itemView.findViewById(R.id.btn_cancel);
            tutorImage = itemView.findViewById(R.id.tutor_image);
        }
    }
}
