package com.example.gapfix;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

        holder.tvName.setText(conversation.otherUserName != null ? conversation.otherUserName : "User");

        if (conversation.lastMessage != null && !conversation.lastMessage.isEmpty()) {
            holder.tvLastMessage.setText(conversation.lastMessage);
        } else {
            holder.tvLastMessage.setText("No messages yet");
        }

        if (conversation.lastMessageTime != null) {
            holder.tvTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(conversation.lastMessageTime.toDate()));
        } else {
            holder.tvTime.setText("");
        }

        holder.itemView.setOnClickListener(v -> listener.onConversationClick(conversation));

        // Loading the Image
        if (conversation.otherUserImage != null && !conversation.otherUserImage.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(conversation.otherUserImage)
                    .placeholder(R.drawable.person_circle)
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else if (conversation.otherUserId != null) {
            // Fallback: Fetch from Realtime Database if missing in Firestore metadata
            fetchImageFromRTDB(conversation.otherUserId, holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.person_circle);
        }
    }

    private void fetchImageFromRTDB(String uid, ImageView imageView) {
        // Check Student branch
        FirebaseDatabase.getInstance().getReference("Users").child("Student").child(uid).child("imageResourceLink")
                .get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        loadImage(snapshot.getValue(String.class), imageView);
                    } else {
                        // Check Tutor branch
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

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLastMessage, tvTime;
        ImageView ivAvatar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
        }
    }
}
