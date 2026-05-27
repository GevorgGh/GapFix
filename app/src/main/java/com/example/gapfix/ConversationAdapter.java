package com.example.gapfix;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    private final List<FirestoreConversation> conversations;
    private final OnConversationClickListener listener;
    private String currentUserId;

    public interface OnConversationClickListener {
        void onConversationClick(FirestoreConversation conversation);
    }

    public ConversationAdapter(List<FirestoreConversation> conversations, OnConversationClickListener listener) {
        this.conversations = conversations;
        this.listener = listener;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            this.currentUserId = auth.getUid();
        }
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

        
        holder.tvName.setText(conversation.otherUserName != null ? conversation.otherUserName : holder.itemView.getContext().getString(R.string.ext_user_name));

        if (conversation.lastMessage != null && !conversation.lastMessage.isEmpty()) {
            holder.tvLastMessage.setText(conversation.lastMessage);
        } else {
            holder.tvLastMessage.setText(holder.itemView.getContext().getString(R.string.ext_no_conversations_yet));
        }

        
        if (conversation.lastMessageTime != null) {
            holder.tvTime.setText(getFormattedTime(conversation.lastMessageTime.toDate()));
        } else {
            holder.tvTime.setText("");
        }

        
        if (currentUserId != null && conversation.unreadCount != null && conversation.unreadCount.containsKey(currentUserId)) {
            Object unreadObj = conversation.unreadCount.get(currentUserId);
            long unreadNumber = 0;
            if (unreadObj instanceof Number) {
                unreadNumber = ((Number) unreadObj).longValue();
            }
            
            if (unreadNumber > 0) {
                holder.tvUnreadCount.setText(String.valueOf(unreadNumber));
                holder.tvUnreadCount.setVisibility(View.VISIBLE);
            } else {
                holder.tvUnreadCount.setVisibility(View.GONE);
            }
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onConversationClick(conversation));

        if (conversation.otherUserImage != null && !conversation.otherUserImage.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(conversation.otherUserImage)
                    .placeholder(R.drawable.person_circle)
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else if (conversation.otherUserId != null) {
            fetchImageFromRTDB(conversation.otherUserId, holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.person_circle);
        }
    }

    private void fetchImageFromRTDB(String uid, ImageView imageView) {
        FirebaseDatabase.getInstance().getReference("Users").child("Student").child(uid).child("imageResourceLink")
                .get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        loadImage(snapshot.getValue(String.class), imageView);
                    } else {
                        FirebaseDatabase.getInstance().getReference("Users").child("Tutor").child(uid).child("imageResourceLink")
                                .get().addOnSuccessListener(tutorSnap -> {
                                    if (tutorSnap.exists()) {
                                        loadImage(tutorSnap.getValue(String.class), imageView);
                                    }
                                });
                    }
                });
    }

    private void loadImage(String url, ImageView imageView) {
        if (url != null && !url.isEmpty()) {
            Glide.with(imageView.getContext())
                    .load(url)
                    .placeholder(R.drawable.person_circle)
                    .circleCrop()
                    .into(imageView);
        }
    }

    private String getFormattedTime(Date date) {
        if (date == null) return "";

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(date);

        java.util.Calendar now = java.util.Calendar.getInstance();

        if (now.get(java.util.Calendar.YEAR) == cal.get(java.util.Calendar.YEAR) &&
                now.get(java.util.Calendar.DAY_OF_YEAR) == cal.get(java.util.Calendar.DAY_OF_YEAR)) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
        }

        java.util.Calendar yesterday = (java.util.Calendar) now.clone();
        yesterday.add(java.util.Calendar.DATE, -1);
        if (yesterday.get(java.util.Calendar.YEAR) == cal.get(java.util.Calendar.YEAR) &&
                yesterday.get(java.util.Calendar.DAY_OF_YEAR) == cal.get(java.util.Calendar.DAY_OF_YEAR)) {
            return "Yesterday";
        }

        
        if (now.get(java.util.Calendar.YEAR) == cal.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.WEEK_OF_YEAR) == cal.get(java.util.Calendar.WEEK_OF_YEAR)) {
            return new SimpleDateFormat("EEEE", Locale.getDefault()).format(date);
        }

        return new SimpleDateFormat("MMM d", Locale.getDefault()).format(date);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName, tvLastMessage, tvTime, tvUnreadCount;
        public ImageView ivAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
        }
    }
}