package com.example.gapfix;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;
public class CertificateAdapter extends RecyclerView.Adapter<CertificateAdapter.CertViewHolder> {
    private final List<Map<String, Object>> certificates;
    private final OnCertActionListener listener;
    public interface OnCertActionListener {
        void onViewCert(String url, String title);
        void onDeleteCert(int position, String certId);
        void onEditCert(int position, String certId, String currentTitle);
    }
    public CertificateAdapter(List<Map<String, Object>> certificates, OnCertActionListener listener) {
        this.certificates = certificates;
        this.listener = listener;
    }
    @NonNull
    @Override
    public CertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_certificate, parent, false);
        return new CertViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull CertViewHolder holder, int position) {
        Map<String, Object> cert = certificates.get(position);
        String title = (String) cert.get("title");
        String url = (String) cert.get("fileUrl");
        String id = (String) cert.get("id");
        holder.tvTitle.setText(title != null ? title : "Untitled Certificate");
        if (url != null && url.toLowerCase().contains(".pdf")) {
            holder.ivIcon.setImageResource(R.drawable.outline_assignment_24);
        } else {
            holder.ivIcon.setImageResource(R.drawable.baseline_check_circle_24_small);
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && url != null) listener.onViewCert(url, title);
        });
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditCert(position, id, title);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteCert(position, id);
        });
    }
    @Override
    public int getItemCount() {
        return certificates.size();
    }
    public static class CertViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvStatus;
        ImageButton btnEdit, btnDelete;
        ImageView ivIcon;
        public CertViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvCertTitle);
            tvStatus = itemView.findViewById(R.id.tvCertStatus);
            btnEdit = itemView.findViewById(R.id.btnEditCert);
            btnDelete = itemView.findViewById(R.id.btnDeleteCert);
            ivIcon = itemView.findViewById(R.id.ivCertIcon);
        }
    }
}
