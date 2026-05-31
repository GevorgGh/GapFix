package com.example.gapfix;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private List<Review> reviewList;
    public ReviewAdapter(List<Review> reviewList) {
        this.reviewList = reviewList;
    }
    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.review_card, parent, false);
        return new ReviewViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);
        String name = review.getStudentName() != null ? review.getStudentName() : "Anonymous";
        holder.tvStudentName.setText(name);
        holder.commentText.setText(review.getReview());
        if (holder.ratingBar != null) {
            holder.ratingBar.setRating(review.getRating());
        }
    }
    @Override
    public int getItemCount() {
        return reviewList != null ? reviewList.size() : 0;
    }
    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView commentText;
        TextView tvStudentName;
        RatingBar ratingBar;
        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            commentText = itemView.findViewById(R.id.review_text);
            tvStudentName = itemView.findViewById(R.id.student_name);
            ratingBar = itemView.findViewById(R.id.review_rating_bar);
        }
    }
}
