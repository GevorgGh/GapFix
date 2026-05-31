package com.example.gapfix;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class HomeworkAdapter extends RecyclerView.Adapter<HomeworkAdapter.HomeworkViewHolder> {
    private final List<FirestoreMessage> homeworks;
    private final String currentUserRole;
    private final OnHomeworkActionListener listener;
    public interface OnHomeworkActionListener {
        void onViewFile(String url);
        void onDeleteHomework(FirestoreMessage msg);
        void onEditHomework(FirestoreMessage msg);
        void onMarkRight(FirestoreMessage msg);
        void onMarkWrong(FirestoreMessage msg);
        void onUploadSolution(FirestoreMessage msg);
        void onCouldnDoIt(FirestoreMessage msg);
        void onArchiveHomework(FirestoreMessage msg);
    }
    public HomeworkAdapter(List<FirestoreMessage> homeworks, String currentUserRole, OnHomeworkActionListener listener) {
        this.homeworks = homeworks;
        this.currentUserRole = currentUserRole;
        this.listener = listener;
    }
    @NonNull
    @Override
    public HomeworkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_homework, parent, false);
        return new HomeworkViewHolder(v);
    }
    @Override
    public void onBindViewHolder(@NonNull HomeworkViewHolder holder, int position) {
        FirestoreMessage msg = homeworks.get(position);
        holder.tvSubject.setVisibility(View.VISIBLE);
        if (msg.subject != null && !msg.subject.isEmpty()) {
            holder.tvSubject.setText(msg.subject.trim());
        } else {
            holder.tvSubject.setText("General");
        }
        holder.tvTitle.setText(msg.text != null ? msg.text : "Untitled Assignment");
        StringBuilder subtitle = new StringBuilder();
        if (msg.lessonTimestamp != 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd @ HH:mm", Locale.getDefault());
            subtitle.append(sdf.format(new Date(msg.lessonTimestamp)));
        }
        if (subtitle.length() > 0) {
            holder.tvSubtitle.setText(subtitle.toString());
        } else {
            holder.tvSubtitle.setText(R.string.ext_click_to_view_file);
        }
        boolean isTutor = "Tutor".equalsIgnoreCase(currentUserRole);
        if (msg.fileUrl != null && msg.fileUrl.toLowerCase().endsWith(".pdf")) {
            holder.ivFileIcon.setImageResource(R.drawable.outline_assignment_24);
            holder.ivFileIcon.setColorFilter(android.graphics.Color.RED);
        } else {
            holder.ivFileIcon.setImageResource(R.drawable.outline_assignment_24);
            holder.ivFileIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.gapfix_green));
        }
        holder.itemView.setOnClickListener(v -> {
            if (msg.fileUrl != null && listener != null) listener.onViewFile(msg.fileUrl);
        });
        holder.divider.setVisibility(View.GONE);
        holder.layoutStudentActions.setVisibility(View.GONE);
        holder.layoutSolutionDetails.setVisibility(View.GONE);
        holder.layoutTutorFeedbackActions.setVisibility(View.GONE);
        holder.tvHomeworkStatusBadge.setVisibility(View.GONE);
        holder.tvFeedbackBadge.setVisibility(View.GONE);
        holder.btnDelete.setVisibility(View.GONE);
        holder.btnEdit.setVisibility(View.GONE);
        holder.ivArrow.setVisibility(View.VISIBLE);
        if (holder.btnAddToArchive != null) {
            if (isTutor) {
                holder.btnAddToArchive.setVisibility(View.GONE);
            } else {
                holder.btnAddToArchive.setVisibility(View.VISIBLE);
                holder.btnAddToArchive.setOnClickListener(v -> {
                    if (listener != null) listener.onArchiveHomework(msg);
                });
            }
        }
        if (isTutor && msg.solutionUrl == null && !"failed".equals(msg.homeworkStatus)) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.ivArrow.setVisibility(View.GONE);
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteHomework(msg);
            });
            holder.btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditHomework(msg);
            });
        }
        if (msg.solutionUrl != null) {
            holder.divider.setVisibility(View.VISIBLE);
            holder.layoutSolutionDetails.setVisibility(View.VISIBLE);
            holder.btnViewSolution.setOnClickListener(v -> {
                if (listener != null) listener.onViewFile(msg.solutionUrl);
            });
            holder.tvFeedbackBadge.setVisibility(View.VISIBLE);
            if (msg.tutorFeedback != null) {
                boolean isCorrect = "correct".equalsIgnoreCase(msg.tutorFeedback);
                holder.tvFeedbackBadge.setText(isCorrect ? "Correct" : "Incorrect");
                holder.tvFeedbackBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), 
                    isCorrect ? R.color.color_success : R.color.color_error));
            } else if (isTutor) {
                holder.tvFeedbackBadge.setText("Review Required");
                holder.tvFeedbackBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_warning));
                holder.layoutTutorFeedbackActions.setVisibility(View.VISIBLE);
                holder.btnMarkRight.setOnClickListener(v -> {
                    if (listener != null) listener.onMarkRight(msg);
                });
                holder.btnMarkWrong.setOnClickListener(v -> {
                    if (listener != null) listener.onMarkWrong(msg);
                });
            } else {
                holder.tvFeedbackBadge.setText("Awaiting Review");
                holder.tvFeedbackBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_warning));
            }
        } else {
            if (!isTutor) {
                if ("failed".equals(msg.homeworkStatus)) {
                    holder.tvHomeworkStatusBadge.setVisibility(View.VISIBLE);
                    holder.tvHomeworkStatusBadge.setText("Couldn't do it");
                    holder.tvHomeworkStatusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_error));
                } else {
                    holder.divider.setVisibility(View.VISIBLE);
                    holder.layoutStudentActions.setVisibility(View.VISIBLE);
                    holder.btnDone.setOnClickListener(v -> {
                        if (listener != null) listener.onUploadSolution(msg);
                    });
                    holder.btnFailed.setOnClickListener(v -> {
                        if (listener != null) listener.onCouldnDoIt(msg);
                    });
                }
            } else {
                holder.tvHomeworkStatusBadge.setVisibility(View.VISIBLE);
                boolean isFailed = "failed".equals(msg.homeworkStatus);
                holder.tvHomeworkStatusBadge.setText(isFailed ? "Couldn't do it" : "Pending");
                holder.tvHomeworkStatusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), 
                    isFailed ? R.color.color_error : R.color.color_info));
            }
        }
    }
    @Override public int getItemCount() { return homeworks.size(); }
    public static class HomeworkViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubject, tvSubtitle, tvFeedbackBadge, tvHomeworkStatusBadge;
        View divider;
        LinearLayout layoutStudentActions, layoutSolutionDetails, layoutTutorFeedbackActions;
        MaterialButton btnDone, btnFailed, btnViewSolution, btnMarkRight, btnMarkWrong, btnAddToArchive;
        ImageView ivFileIcon, ivArrow;
        ImageButton btnDelete, btnEdit;
        public HomeworkViewHolder(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvHomeworkTitle);
            tvSubject = v.findViewById(R.id.tvHomeworkSubject);
            tvSubtitle = v.findViewById(R.id.tvHomeworkSubtitle);
            tvFeedbackBadge = v.findViewById(R.id.tvFeedbackBadge);
            tvHomeworkStatusBadge = v.findViewById(R.id.tvHomeworkStatusBadge);
            divider = v.findViewById(R.id.homeworkDivider);
            layoutStudentActions = v.findViewById(R.id.layoutStudentActions);
            layoutSolutionDetails = v.findViewById(R.id.layoutSolutionDetails);
            layoutTutorFeedbackActions = v.findViewById(R.id.layoutTutorFeedbackActions);
            btnDone = v.findViewById(R.id.btnDone);
            btnFailed = v.findViewById(R.id.btnFailed);
            btnViewSolution = v.findViewById(R.id.btnViewSolution);
            btnMarkRight = v.findViewById(R.id.btnMarkRight);
            btnMarkWrong = v.findViewById(R.id.btnMarkWrong);
            btnAddToArchive = v.findViewById(R.id.btnAddToArchive);
            ivFileIcon = v.findViewById(R.id.ivFileIcon);
            ivArrow = v.findViewById(R.id.ivArrowForward);
            btnDelete = v.findViewById(R.id.btnDeleteHomework);
            btnEdit = v.findViewById(R.id.btnEditHomework);
        }
    }
}
