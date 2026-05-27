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
import com.example.gapfix.TutorStudentsFragment.StudentModel;

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private final List<StudentModel> studentList;
    private final OnStudentClickListener listener;
    private final OnStudentActionClickListener actionListener;

    public interface OnStudentClickListener {
        void onStudentClick(StudentModel student);
    }

    public interface OnStudentActionClickListener {
        void onActionClick(StudentModel student, String action);
    }

    public StudentAdapter(List<StudentModel> studentList, OnStudentClickListener listener, OnStudentActionClickListener actionListener) {
        this.studentList = studentList;
        this.listener = listener;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tutor_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        StudentModel student = studentList.get(position);
        holder.bind(student, listener, actionListener);
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public static class StudentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName, tvEmail, tvUnreadBadge, tvChatBadge, tvHomeworkBadge;
        private final ImageView ivProfile, ivArrow;
        private final LinearLayout studentHeader, dropdownContainer;
        private final View btnChat, btnHomework, btnDelete;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStudentName);
            tvEmail = itemView.findViewById(R.id.tvStudentEmail);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
            tvChatBadge = itemView.findViewById(R.id.tvChatBadge);
            tvHomeworkBadge = itemView.findViewById(R.id.tvHomeworkBadge);
            ivProfile = itemView.findViewById(R.id.ivStudentProfile);
            ivArrow = itemView.findViewById(R.id.ivDropdownArrow);
            studentHeader = itemView.findViewById(R.id.studentHeader);
            dropdownContainer = itemView.findViewById(R.id.dropdownContainer);
            btnChat = itemView.findViewById(R.id.btnOpenChat);
            btnHomework = itemView.findViewById(R.id.btnOpenHomework);
            btnDelete = itemView.findViewById(R.id.btnDeleteStudent);
        }

        public void bind(StudentModel student, OnStudentClickListener listener, OnStudentActionClickListener actionListener) {
            tvName.setText(student.getName() != null ? student.getName() : "Loading...");
            tvEmail.setText(student.getEmail() != null ? student.getEmail() : "");

            if (student.getUnreadCount() > 0) {
                tvUnreadBadge.setVisibility(View.VISIBLE);
                tvUnreadBadge.setText(String.valueOf(student.getUnreadCount()));

                
                if (student.getUnreadChatCount() > 0) {
                    tvChatBadge.setVisibility(View.VISIBLE);
                    tvChatBadge.setText(String.valueOf(student.getUnreadChatCount()));
                } else {
                    tvChatBadge.setVisibility(View.GONE);
                }

                if (student.getUnreadHomeworkCount() > 0) {
                    tvHomeworkBadge.setVisibility(View.VISIBLE);
                    tvHomeworkBadge.setText(String.valueOf(student.getUnreadHomeworkCount()));
                } else {
                    tvHomeworkBadge.setVisibility(View.GONE);
                }
            } else {
                tvUnreadBadge.setVisibility(View.GONE);
                tvChatBadge.setVisibility(View.GONE);
                tvHomeworkBadge.setVisibility(View.GONE);
            }

            if (student.getProfileImage() != null && !student.getProfileImage().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(student.getProfileImage())
                        .placeholder(R.drawable.person_circle)
                        .into(ivProfile);
            } else {
                ivProfile.setImageResource(R.drawable.person_circle);
            }

            dropdownContainer.setVisibility(student.isExpanded() ? View.VISIBLE : View.GONE);
            if (ivArrow != null) {
                ivArrow.setRotation(student.isExpanded() ? 180 : 0);
            }

            studentHeader.setOnClickListener(v -> {
                student.setExpanded(!student.isExpanded());
                dropdownContainer.setVisibility(student.isExpanded() ? View.VISIBLE : View.GONE);
                if (ivArrow != null) {
                    ivArrow.setRotation(student.isExpanded() ? 180 : 0);
                }
                if (listener != null) listener.onStudentClick(student);
            });

            btnChat.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onActionClick(student, "chat");
            });

            btnHomework.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onActionClick(student, "homework");
            });

            btnDelete.setVisibility(student.isCanDelete() ? View.VISIBLE : View.GONE);
            btnDelete.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onActionClick(student, "delete");
            });
        }
    }
}
