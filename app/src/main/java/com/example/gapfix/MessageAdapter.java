package com.example.gapfix;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_DATE_HEADER = 3;

    private List<FirestoreMessage> messages;
    private String currentUserId;

    public MessageAdapter(List<FirestoreMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        FirestoreMessage message = messages.get(position);
        if (message.senderId.equals("DATE_HEADER")) {
            return VIEW_TYPE_DATE_HEADER;
        }
        return currentUserId.equals(message.senderId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_RECEIVED) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FirestoreMessage message = messages.get(position);

        if (holder instanceof DateHeaderViewHolder) {
            ((DateHeaderViewHolder) holder).bind(message.text);
            return;
        }

        long timeMillis = message.timestamp != null
                ? message.timestamp.toDate().getTime()
                : System.currentTimeMillis();

        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message.text, timeMillis);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message.text, timeMillis);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(String message, long time) {
            tvMessage.setText(message);
            tvTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(time)));
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(String message, long time) {
            tvMessage.setText(message);
            tvTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(time)));
        }
    }

    static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateHeader;

        DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateHeader = itemView.findViewById(R.id.tvDateHeader);
        }

        void bind(String dateText) {
            tvDateHeader.setText(dateText);
        }
    }

    public static String getFormattedDate(long smsTimeInMilis) {
        Calendar smsTime = Calendar.getInstance();
        smsTime.setTimeInMillis(smsTimeInMilis);

        Calendar now = Calendar.getInstance();

        if (now.get(Calendar.DATE) == smsTime.get(Calendar.DATE) &&
                now.get(Calendar.MONTH) == smsTime.get(Calendar.MONTH) &&
                now.get(Calendar.YEAR) == smsTime.get(Calendar.YEAR)) {
            return "Today";
        } else if (now.get(Calendar.DATE) - smsTime.get(Calendar.DATE) == 1 &&
                now.get(Calendar.MONTH) == smsTime.get(Calendar.MONTH) &&
                now.get(Calendar.YEAR) == smsTime.get(Calendar.YEAR)) {
            return "Yesterday";
        } else {
            return new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(new Date(smsTimeInMilis));
        }
    }
}
