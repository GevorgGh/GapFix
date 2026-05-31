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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_HOMEWORK = 3;
    private static final int VIEW_TYPE_DATE_HEADER = 4;
    private List<FirestoreMessage> messages;
    private String currentUserId;
    private String currentUserRole;
    private OnHomeworkActionListener homeworkActionListener;
    public interface OnHomeworkActionListener {
        void onUploadSolution(FirestoreMessage message);
        void onCouldnDoIt(FirestoreMessage message);
        void onViewFile(String url);
        void onMarkRight(FirestoreMessage message);
        void onMarkWrong(FirestoreMessage message);
        void onArchiveHomework(FirestoreMessage message);
    }
    public MessageAdapter(List<FirestoreMessage> messages, String currentUserId, String currentUserRole, OnHomeworkActionListener listener) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.currentUserRole = currentUserRole;
        this.homeworkActionListener = listener;
    }
    public void setUserRole(String userRole) {
        this.currentUserRole = userRole;
        notifyDataSetChanged();
    }
    @Override
    public int getItemViewType(int position) {
        FirestoreMessage message = messages.get(position);
        if ("DATE_HEADER".equals(message.senderId)) {
            return VIEW_TYPE_DATE_HEADER;
        }
        if ("homework".equals(message.type)) {
            return VIEW_TYPE_HOMEWORK;
        }
        return currentUserId.equals(message.senderId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            return new SentMessageViewHolder(inflater.inflate(R.layout.item_message_sent, parent, false));
        } else if (viewType == VIEW_TYPE_RECEIVED) {
            return new ReceivedMessageViewHolder(inflater.inflate(R.layout.item_message_received, parent, false));
        } else if (viewType == VIEW_TYPE_HOMEWORK) {
            return new HomeworkViewHolder(inflater.inflate(R.layout.item_homework, parent, false));
        } else {
            return new DateHeaderViewHolder(inflater.inflate(R.layout.item_date_header, parent, false));
        }
    }
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FirestoreMessage message = messages.get(position);
        if (holder instanceof DateHeaderViewHolder) {
            ((DateHeaderViewHolder) holder).bind(message.text);
            return;
        }
        long timeMillis = message.timestamp != null ? message.timestamp.toDate().getTime() : System.currentTimeMillis();
        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message.text, timeMillis);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message.text, timeMillis);
        } else if (holder instanceof HomeworkViewHolder) {
            ((HomeworkViewHolder) holder).bind(message, currentUserRole, homeworkActionListener);
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
    static class HomeworkViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubject, tvSubtitle, tvFeedbackBadge, tvHomeworkStatusBadge;
        View divider;
        LinearLayout layoutStudentActions, layoutSolutionDetails, layoutTutorFeedbackActions;
        MaterialButton btnDone, btnFailed, btnViewSolution, btnMarkRight, btnMarkWrong, btnAddToArchive;
        ImageView ivFileIcon;
        HomeworkViewHolder(@NonNull View v) {
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
        }
        void bind(FirestoreMessage msg, String userRole, OnHomeworkActionListener listener) {
            boolean isTutor = "Tutor".equalsIgnoreCase(userRole);
            tvSubject.setVisibility(View.VISIBLE);
            if (msg.subject != null && !msg.subject.isEmpty()) {
                tvSubject.setText(msg.subject.trim());
            } else {
                tvSubject.setText("General");
            }
            tvTitle.setText(msg.text != null ? msg.text : "Homework");
            StringBuilder subtitle = new StringBuilder();
            if (msg.lessonTimestamp != 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd @ HH:mm", Locale.getDefault());
                subtitle.append(sdf.format(new Date(msg.lessonTimestamp)));
            }
            if (subtitle.length() > 0) {
                tvSubtitle.setText(subtitle.toString());
            } else {
                tvSubtitle.setText(R.string.ext_click_to_view_file);
            }
            if (msg.fileUrl != null && msg.fileUrl.toLowerCase().endsWith(".pdf")) {
                ivFileIcon.setImageResource(R.drawable.outline_assignment_24);
                ivFileIcon.setColorFilter(android.graphics.Color.RED);
            } else {
                ivFileIcon.setImageResource(R.drawable.outline_assignment_24);
                ivFileIcon.setColorFilter(android.graphics.Color.parseColor("#008253"));
            }
            itemView.setOnClickListener(v -> {
                if (msg.fileUrl != null && listener != null) {
                    listener.onViewFile(msg.fileUrl);
                }
            });
            divider.setVisibility(View.GONE);
            layoutStudentActions.setVisibility(View.GONE);
            layoutSolutionDetails.setVisibility(View.GONE);
            layoutTutorFeedbackActions.setVisibility(View.GONE);
            tvHomeworkStatusBadge.setVisibility(View.GONE);
            if (btnAddToArchive != null) {
                if ("Tutor".equalsIgnoreCase(userRole)) {
                    btnAddToArchive.setVisibility(View.GONE);
                } else {
                    btnAddToArchive.setVisibility(View.VISIBLE);
                    btnAddToArchive.setOnClickListener(v -> {
                        if (listener != null) listener.onArchiveHomework(msg);
                    });
                }
            }
            if (msg.solutionUrl != null) {
                divider.setVisibility(View.VISIBLE);
                layoutSolutionDetails.setVisibility(View.VISIBLE);
                btnViewSolution.setOnClickListener(v -> {
                    if (listener != null) listener.onViewFile(msg.solutionUrl);
                });
                if (msg.tutorFeedback != null) {
                    boolean isCorrect = "correct".equals(msg.tutorFeedback);
                    tvFeedbackBadge.setText(isCorrect ? "Correct" : "Incorrect");
                    tvFeedbackBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), 
                        isCorrect ? R.color.color_success : R.color.color_error));
                } else if (isTutor) {
                    tvFeedbackBadge.setText("Review Required");
                    tvFeedbackBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.color_warning));
                    layoutTutorFeedbackActions.setVisibility(View.VISIBLE);
                    btnMarkRight.setOnClickListener(v -> {
                        if (listener != null) listener.onMarkRight(msg);
                    });
                    btnMarkWrong.setOnClickListener(v -> {
                        if (listener != null) listener.onMarkWrong(msg);
                    });
                } else {
                    tvFeedbackBadge.setText("Awaiting Review");
                    tvFeedbackBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.color_warning));
                }
            } else {
                if (!isTutor) {
                    if ("failed".equals(msg.homeworkStatus)) {
                        tvHomeworkStatusBadge.setVisibility(View.VISIBLE);
                        tvHomeworkStatusBadge.setText("Couldn't do it");
                        tvHomeworkStatusBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.color_error));
                    } else {
                        divider.setVisibility(View.VISIBLE);
                        layoutStudentActions.setVisibility(View.VISIBLE);
                        btnDone.setOnClickListener(v -> {
                            if (listener != null) listener.onUploadSolution(msg);
                        });
                        btnFailed.setOnClickListener(v -> {
                            if (listener != null) listener.onCouldnDoIt(msg);
                        });
                    }
                } else {
                    tvHomeworkStatusBadge.setVisibility(View.VISIBLE);
                    boolean isFailed = "failed".equals(msg.homeworkStatus);
                    tvHomeworkStatusBadge.setText(isFailed ? "Couldn't do it" : "Pending");
                    tvHomeworkStatusBadge.setTextColor(ContextCompat.getColor(itemView.getContext(),
                        isFailed ? R.color.color_error : R.color.color_info));
                }
            }
        }
    }
    public static String getFormattedDate(long smsTimeInMilis) {
        Calendar smsTime = Calendar.getInstance();
        smsTime.setTimeInMillis(smsTimeInMilis);
        Calendar now = Calendar.getInstance();
        if (now.get(Calendar.YEAR) == smsTime.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == smsTime.get(Calendar.DAY_OF_YEAR)) {
            return "Today";
        }
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
        if (yesterday.get(Calendar.YEAR) == smsTime.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == smsTime.get(Calendar.DAY_OF_YEAR)) {
            return "Yesterday";
        }
        if (now.get(Calendar.YEAR) == smsTime.get(Calendar.YEAR)) {
            return new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(new Date(smsTimeInMilis));
        } else {
            return new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(new Date(smsTimeInMilis));
        }
    }
}
