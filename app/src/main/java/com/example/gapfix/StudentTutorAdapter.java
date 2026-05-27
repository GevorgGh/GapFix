package com.example.gapfix;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gapfix.StudentTutorsFragment.TutorModel;

import java.util.List;

public class StudentTutorAdapter extends RecyclerView.Adapter<StudentTutorAdapter.TutorViewHolder> {

    private final List<TutorModel> tutorList;
    private final OnTutorClickListener listener;
    private final OnTutorActionClickListener actionListener;

    public interface OnTutorClickListener {
        void onTutorClick(TutorModel tutor);
    }

    public interface OnTutorActionClickListener {
        void onActionClick(TutorModel tutor, String action);
    }

    public StudentTutorAdapter(List<TutorModel> tutorList, OnTutorClickListener listener, OnTutorActionClickListener actionListener) {
        this.tutorList = tutorList;
        this.listener = listener;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public TutorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_tutor, parent, false);
        return new TutorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TutorViewHolder holder, int position) {
        TutorModel tutor = tutorList.get(position);
        holder.bind(tutor, listener, actionListener);
    }

    @Override
    public int getItemCount() {
        return tutorList.size();
    }

    public static class TutorViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName, tvEmail, tvUnreadBadge, tvChatBadge, tvHomeworkBadge;
        private final ImageView ivProfile, ivArrow;
        private final LinearLayout tutorHeader, dropdownContainer;
        private final View btnChat, btnHomework, btnDelete;

        public TutorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTutorName);
            tvEmail = itemView.findViewById(R.id.tvTutorEmail);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
            tvChatBadge = itemView.findViewById(R.id.tvChatBadge);
            tvHomeworkBadge = itemView.findViewById(R.id.tvHomeworkBadge);
            ivProfile = itemView.findViewById(R.id.ivTutorProfile);
            ivArrow = itemView.findViewById(R.id.ivDropdownArrow);
            tutorHeader = itemView.findViewById(R.id.tutorHeader);
            dropdownContainer = itemView.findViewById(R.id.dropdownContainer);
            btnChat = itemView.findViewById(R.id.btnOpenChat);
            btnHomework = itemView.findViewById(R.id.btnOpenHomework);
            btnDelete = itemView.findViewById(R.id.btnDeleteTutor);
        }

        public void bind(TutorModel tutor, OnTutorClickListener listener, OnTutorActionClickListener actionListener) {
            tvName.setText(tutor.getName() != null ? tutor.getName() : "Loading...");
            tvEmail.setText(tutor.getEmail() != null ? tutor.getEmail() : "");

            if (tutor.getUnreadCount() > 0) {
                tvUnreadBadge.setVisibility(View.VISIBLE);
                tvUnreadBadge.setText(String.valueOf(tutor.getUnreadCount()));
                
                
                if (tutor.getUnreadChatCount() > 0) {
                    tvChatBadge.setVisibility(View.VISIBLE);
                    tvChatBadge.setText(String.valueOf(tutor.getUnreadChatCount()));
                } else {
                    tvChatBadge.setVisibility(View.GONE);
                }

                if (tutor.getUnreadHomeworkCount() > 0) {
                    tvHomeworkBadge.setVisibility(View.VISIBLE);
                    tvHomeworkBadge.setText(String.valueOf(tutor.getUnreadHomeworkCount()));
                } else {
                    tvHomeworkBadge.setVisibility(View.GONE);
                }
            } else {
                tvUnreadBadge.setVisibility(View.GONE);
                tvChatBadge.setVisibility(View.GONE);
                tvHomeworkBadge.setVisibility(View.GONE);
            }

            if (tutor.getProfileImage() != null && !tutor.getProfileImage().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(tutor.getProfileImage())
                        .placeholder(R.drawable.person_circle)
                        .into(ivProfile);
            } else {
                ivProfile.setImageResource(R.drawable.person_circle);
            }

            dropdownContainer.setVisibility(tutor.isExpanded() ? View.VISIBLE : View.GONE);
            if (ivArrow != null) {
                ivArrow.setRotation(tutor.isExpanded() ? 180 : 0);
            }

            tutorHeader.setOnClickListener(v -> {
                tutor.setExpanded(!tutor.isExpanded());
                dropdownContainer.setVisibility(tutor.isExpanded() ? View.VISIBLE : View.GONE);
                if (ivArrow != null) {
                    ivArrow.setRotation(tutor.isExpanded() ? 180 : 0);
                }
                if (listener != null) listener.onTutorClick(tutor);
            });

            btnChat.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onActionClick(tutor, "chat");
            });

            btnHomework.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onActionClick(tutor, "homework");
            });

            btnDelete.setVisibility(tutor.isCanDelete() ? View.VISIBLE : View.GONE);
            btnDelete.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onActionClick(tutor, "delete");
            });
        }
    }
}
