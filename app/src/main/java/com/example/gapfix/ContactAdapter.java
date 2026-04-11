package com.example.gapfix;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    private List<User> contacts;
    private OnContactClickListener listener;

    public interface OnContactClickListener {
        void onContactClick(User contact, String userId);
    }

    private List<String> userIds;

    public ContactAdapter(List<User> contacts, List<String> userIds, OnContactClickListener listener) {
        this.contacts = contacts;
        this.userIds = userIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User contact = contacts.get(position);
        String userId = userIds.get(position);

        holder.tvName.setText(contact.getName());
        holder.tvLastMessage.setText(contact.getEmail()); // Show email as secondary info
        holder.tvTime.setVisibility(View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onContactClick(contact, userId));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
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
