package com.example.gapfix;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.firebase.firestore.*;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    private List<FirestoreConversation> conversations;
    private OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(FirestoreConversation conversation);
    }

    public ConversationAdapter(List<FirestoreConversation> conversations, OnConversationClickListener listener) {
        this.conversations = conversations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FirestoreConversation conversation = conversations.get(position);

        // Show name if available, otherwise fall back to user ID
        holder.tvName.setText(
                conversation.otherUserName != null
                        ? conversation.otherUserName
                        : conversation.otherUserId
        );

        // Show last message
        if (conversation.lastMessage != null && !conversation.lastMessage.isEmpty()) {
            holder.tvLastMessage.setText(conversation.lastMessage);
        } else {
            holder.tvLastMessage.setText("No messages yet");
        }

        // Show timestamp
        if (conversation.lastMessageTime != null) {
            holder.tvTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(conversation.lastMessageTime.toDate()));
        } else {
            holder.tvTime.setText("");
        }

        holder.itemView.setOnClickListener(v -> listener.onConversationClick(conversation));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLastMessage, tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}