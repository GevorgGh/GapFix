package com.example.gapfix;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
public class CalendarHomeworkAdapter extends RecyclerView.Adapter<CalendarHomeworkAdapter.HomeworkViewHolder> {
    private final List<FirestoreMessage> homeworkList;
    private final Context context;
    private final OnHomeworkActionListener listener;
    public interface OnHomeworkActionListener {
        void onViewFile(String url);
        void onUploadSolution(FirestoreMessage homework);
        void onArchiveHomework(FirestoreMessage homework);
        void onCouldnDoIt(FirestoreMessage homework);
    }
    public CalendarHomeworkAdapter(Context context, List<FirestoreMessage> homeworkList, OnHomeworkActionListener listener) {
        this.context = context;
        this.homeworkList = homeworkList;
        this.listener = listener;
    }
    @NonNull
    @Override
    public HomeworkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_homework, parent, false);
        return new HomeworkViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull HomeworkViewHolder holder, int position) {
        FirestoreMessage homework = homeworkList.get(position);
        holder.tvSubject.setText(homework.subject != null ? homework.subject : "General");
        holder.tvTitle.setText(homework.text != null ? homework.text : "Untitled Homework");
        if (homework.lessonTimestamp != 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd @ HH:mm", Locale.getDefault());
            holder.tvSubtitle.setText(sdf.format(new Date(homework.lessonTimestamp)));
        } else {
            holder.tvSubtitle.setText(R.string.ext_click_to_view_file);
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && homework.fileUrl != null) listener.onViewFile(homework.fileUrl);
        });
        holder.divider.setVisibility(View.GONE);
        holder.tvHomeworkStatusBadge.setVisibility(View.GONE);
        holder.layoutStudentActions.setVisibility(View.GONE);
        holder.layoutSolutionDetails.setVisibility(View.GONE);
        holder.layoutTutorFeedbackActions.setVisibility(View.GONE);
        holder.btnAddToArchive.setVisibility(View.VISIBLE);
        if (homework.solutionUrl != null) {
            holder.divider.setVisibility(View.VISIBLE);
            holder.layoutSolutionDetails.setVisibility(View.VISIBLE);
            holder.btnViewSolution.setOnClickListener(v -> {
                if (listener != null) listener.onViewFile(homework.solutionUrl);
            });
            if (homework.tutorFeedback != null) {
                boolean isCorrect = "correct".equals(homework.tutorFeedback);
                holder.tvFeedbackBadge.setText(isCorrect ? "Correct" : "Incorrect");
                holder.tvFeedbackBadge.setTextColor(ContextCompat.getColor(context, 
                    isCorrect ? R.color.color_success : R.color.color_error));
            } else {
                holder.tvFeedbackBadge.setText("Awaiting Review");
                holder.tvFeedbackBadge.setTextColor(ContextCompat.getColor(context, R.color.color_warning));
            }
        } else if ("failed".equals(homework.homeworkStatus)) {
            holder.tvHomeworkStatusBadge.setVisibility(View.VISIBLE);
            holder.tvHomeworkStatusBadge.setText("Couldn't do it");
            holder.tvHomeworkStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.color_error));
        } else {
            holder.divider.setVisibility(View.VISIBLE);
            holder.layoutStudentActions.setVisibility(View.VISIBLE);
            holder.btnDone.setOnClickListener(v -> {
                if (listener != null) listener.onUploadSolution(homework);
            });
            holder.btnFailed.setOnClickListener(v -> {
                if (listener != null) listener.onCouldnDoIt(homework);
            });
        }
        holder.btnAddToArchive.setOnClickListener(v -> {
            if (listener != null) listener.onArchiveHomework(homework);
        });
        holder.btnEditHomework.setVisibility(View.GONE);
        holder.btnDeleteHomework.setVisibility(View.GONE);
    }
    @Override
    public int getItemCount() {
        return homeworkList.size();
    }
    public static class HomeworkViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvTitle, tvSubtitle, tvHomeworkStatusBadge, tvFeedbackBadge;
        View divider;
        LinearLayout layoutStudentActions, layoutSolutionDetails, layoutTutorFeedbackActions;
        MaterialButton btnDone, btnFailed, btnViewSolution, btnAddToArchive;
        ImageButton btnEditHomework, btnDeleteHomework;
        public HomeworkViewHolder(@NonNull View v) {
            super(v);
            tvSubject = v.findViewById(R.id.tvHomeworkSubject);
            tvTitle = v.findViewById(R.id.tvHomeworkTitle);
            tvSubtitle = v.findViewById(R.id.tvHomeworkSubtitle);
            tvHomeworkStatusBadge = v.findViewById(R.id.tvHomeworkStatusBadge);
            tvFeedbackBadge = v.findViewById(R.id.tvFeedbackBadge);
            divider = v.findViewById(R.id.homeworkDivider);
            layoutStudentActions = v.findViewById(R.id.layoutStudentActions);
            layoutSolutionDetails = v.findViewById(R.id.layoutSolutionDetails);
            layoutTutorFeedbackActions = v.findViewById(R.id.layoutTutorFeedbackActions);
            btnDone = v.findViewById(R.id.btnDone);
            btnFailed = v.findViewById(R.id.btnFailed);
            btnViewSolution = v.findViewById(R.id.btnViewSolution);
            btnAddToArchive = v.findViewById(R.id.btnAddToArchive);
            btnEditHomework = v.findViewById(R.id.btnEditHomework);
            btnDeleteHomework = v.findViewById(R.id.btnDeleteHomework);
        }
    }
}
