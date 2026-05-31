package com.example.gapfix;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public class StudentArchiveFragment extends Fragment {
    private static final String TAG = "StudentArchiveFragment";
    private RecyclerView rvArchiveSubjects;
    private ArchiveSubjectAdapter adapter;
    private ActivityResultLauncher<String[]> filePickerLauncher;
    private Uri pendingFileUri = null;
    private String selectedFileName = null;
    private DatabaseReference baseDbRef;
    private String currentUserId;
    private TextView tvUploadStatusRef = null;
    private ImageView ivUploadIconRef = null;
    private MaterialButton btnSaveArchiveRef = null;
    private ProgressBar uploadProgressBarRef = null;
    private ValueEventListener subjectsListener;
    private final java.util.Map<String, String> translatedToCanonicalMap = new java.util.HashMap<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null && isAdded()) {
                        pendingFileUri = uri;
                        selectedFileName = getFileNameFromUri(uri);
                        if (tvUploadStatusRef != null) {
                            tvUploadStatusRef.setText(getString(R.string.selected_file_prefix, selectedFileName));
                            tvUploadStatusRef.setTextColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.gapfix_green));
                        }
                        if (ivUploadIconRef != null) {
                            ivUploadIconRef.setImageResource(R.drawable.baseline_check_24);
                            ivUploadIconRef.setColorFilter(androidx.core.content.ContextCompat.getColor(getContext(), R.color.gapfix_green));
                        }
                        if (btnSaveArchiveRef != null) {
                            btnSaveArchiveRef.setEnabled(true);
                            btnSaveArchiveRef.setAlpha(1.0f);
                        }
                    }
                }
        );
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_archive, container, false);
        currentUserId = FirebaseAuth.getInstance().getUid();
        Context context = getContext();
        if (currentUserId != null && context != null) {
            String role = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    .getString("user_role", "Student");
            baseDbRef = FirebaseDatabase.getInstance().getReference("Users")
                    .child(role)
                    .child(currentUserId)
                    .child("Archives");
        }
        rvArchiveSubjects = view.findViewById(R.id.rvArchiveSubjects);
        com.google.android.material.floatingactionbutton.FloatingActionButton fabAddArchive = view.findViewById(R.id.fabAddArchive);
        rvArchiveSubjects.setLayoutManager(new LinearLayoutManager(context));
        adapter = new ArchiveSubjectAdapter();
        rvArchiveSubjects.setAdapter(adapter);
        adapter.setOnSubjectClickListener(this::showSubjectArchivesBottomSheet);
        fabAddArchive.setOnClickListener(v -> showAddArchiveBottomSheet());
        SubjectHelper.loadTranslations(getContext(), this::loadSubjects);
        return view;
    }
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        Context context = getContext();
        if (context == null) return "file";
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception e) {
                }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result != null ? result : "file";
    }
    private void uploadFileToCloudinary(Uri uri, String title, String subject, BottomSheetDialog dialog) {
        if (tvUploadStatusRef != null) {
            tvUploadStatusRef.setText(R.string.uploading);
            tvUploadStatusRef.setTextColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.color_text_muted));
        }
        if (ivUploadIconRef != null) {
            ivUploadIconRef.setColorFilter(androidx.core.content.ContextCompat.getColor(getContext(), R.color.color_text_muted));
        }
        if (btnSaveArchiveRef != null) {
            btnSaveArchiveRef.setEnabled(false);
            btnSaveArchiveRef.setAlpha(0.5f);
        }
        
        try {
            MediaManager.get().upload(uri)
                    .unsigned("ml_default")
                    .option("folder", "Archives/" + currentUserId)
                    .option("resource_type", "auto")
                    .callback(new UploadCallback() {
                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String url = (String) resultData.get("secure_url");
                            if (isAdded() && getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    saveArchiveToDatabase(url, title, subject, dialog);
                                });
                            }
                        }
                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            if (isAdded() && getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (tvUploadStatusRef != null) {
                                        tvUploadStatusRef.setText(R.string.upload_failed);
                                        tvUploadStatusRef.setTextColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.color_error));
                                    }
                                    if (ivUploadIconRef != null) {
                                        ivUploadIconRef.setImageResource(R.drawable.ic_close);
                                        ivUploadIconRef.setColorFilter(androidx.core.content.ContextCompat.getColor(getContext(), R.color.color_error));
                                    }
                                    if (btnSaveArchiveRef != null) {
                                        btnSaveArchiveRef.setEnabled(true);
                                        btnSaveArchiveRef.setAlpha(1.0f);
                                    }
                                    String errTxt = getString(R.string.upload_error_prefix) + error.getDescription();
                                    Toast.makeText(getContext(), errTxt, Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                        @Override public void onStart(String requestId) {}
                        @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                        @Override public void onReschedule(String requestId, ErrorInfo error) {}
                    }).dispatch();
        } catch (Exception e) {
            if (isAdded()) {
                Toast.makeText(getContext(), R.string.upload_error_retry, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveArchiveToDatabase(String url, String title, String subject, BottomSheetDialog dialog) {
        if (baseDbRef == null) return;
        
        DatabaseReference subjectRef = baseDbRef.child(subject);
        ArchiveItem newArchive = new ArchiveItem(
                title,
                currentUserId,
                subject,
                url,
                title,
                System.currentTimeMillis()
        );
        
        subjectRef.child(title).setValue(newArchive)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), R.string.archive_save_success, Toast.LENGTH_SHORT).show();
                        if (dialog != null) dialog.dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        if (btnSaveArchiveRef != null) {
                            btnSaveArchiveRef.setEnabled(true);
                            btnSaveArchiveRef.setAlpha(1.0f);
                        }
                        String err = getString(R.string.err_failed_save) + e.getMessage();
                        Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAddArchiveBottomSheet() {
        Context context = getContext();
        if (context == null) return;
        pendingFileUri = null;
        selectedFileName = null;
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
        ViewGroup container = (ViewGroup) getActivity().findViewById(android.R.id.content);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_add_archive_bottom_sheet, container, false);
        bottomSheetDialog.setContentView(sheetView);
        TextInputEditText etArchiveTitle = sheetView.findViewById(R.id.etArchiveTitle);
        AutoCompleteTextView subjectDropdown = sheetView.findViewById(R.id.subjectDropdown);
        
        MaterialButton btnSave = sheetView.findViewById(R.id.btnSaveArchive);
        btnSave.setText(R.string.ext_add_to_archive); 
        
        DatabaseReference subjectsRef = FirebaseDatabase.getInstance().getReference("Subjects");
        subjectsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                Context c = getContext();
                if (c == null) return;
                List<String> allSubjectsDisplayList = new ArrayList<>();
                translatedToCanonicalMap.clear();
                String lang = LocaleHelper.getLanguage(c);
                for (DataSnapshot data : snapshot.getChildren()) {
                    Object value = data.getValue();
                    String canonical = null;
                    String translated = null;
                    if (value instanceof String) {
                        canonical = (String) value;
                        translated = canonical;
                    } else if (value instanceof Map) {
                        Map<String, String> translations = (Map<String, String>) value;
                        canonical = translations.get("en");
                        translated = translations.get(lang);
                        if (translated == null) translated = canonical;
                    }
                    if (canonical != null) {
                        allSubjectsDisplayList.add(translated);
                        translatedToCanonicalMap.put(translated, canonical);
                    }
                }
                Collections.sort(allSubjectsDisplayList);
                ArrayAdapter<String> dropdownAdapter = new ArrayAdapter<>(
                        c,
                        android.R.layout.simple_dropdown_item_1line,
                        allSubjectsDisplayList
                );
                subjectDropdown.setAdapter(dropdownAdapter);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    String msg = getString(R.string.err_fetching_subjects) + error.getMessage();
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        MaterialCardView btnUploadFile = sheetView.findViewById(R.id.btnUploadFile);
        tvUploadStatusRef = sheetView.findViewById(R.id.tvUploadStatus);
        ivUploadIconRef = sheetView.findViewById(R.id.ivUploadIcon);
        btnSaveArchiveRef = btnSave;
        btnSaveArchiveRef.setEnabled(false);
        btnSaveArchiveRef.setAlpha(0.5f);
        
        btnUploadFile.setOnClickListener(v -> filePickerLauncher.launch(new String[]{"image/*", "application/pdf"}));
        
        btnSaveArchiveRef.setOnClickListener(v -> {
            String title = etArchiveTitle.getText() != null ? etArchiveTitle.getText().toString().trim() : "";
            String displaySubject = subjectDropdown.getText().toString().trim();
            String selectedSubject = translatedToCanonicalMap.get(displaySubject);
            if (selectedSubject == null) selectedSubject = displaySubject;
            
            if (title.isEmpty()) {
                Toast.makeText(getContext(), R.string.err_enter_title, Toast.LENGTH_SHORT).show();
                return;
            }
            if (title.contains(".") || title.contains("#") || title.contains("$") || title.contains("[") || title.contains("]")) {
                Toast.makeText(getContext(), R.string.err_invalid_symbols, Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedSubject.isEmpty()) {
                Toast.makeText(getContext(), R.string.err_select_subject, Toast.LENGTH_SHORT).show();
                return;
            }
            if (pendingFileUri == null) {
                Toast.makeText(getContext(), R.string.err_upload_file_first, Toast.LENGTH_SHORT).show();
                return;
            }
            
            uploadFileToCloudinary(pendingFileUri, title, selectedSubject, bottomSheetDialog);
        });

        bottomSheetDialog.show();
    }
    private void loadSubjects() {
        if (baseDbRef == null) return;
        subjectsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                try {
                    List<SubjectArchiveModel> subjectModels = new ArrayList<>();
                    for (DataSnapshot subjectSnap : snapshot.getChildren()) {
                        String subjectName = subjectSnap.getKey();
                        if (subjectName != null && !subjectName.startsWith("-")) {
                            int total = 0;
                            int reviewed = 0;
                            for (DataSnapshot itemSnap : subjectSnap.getChildren()) {
                                try {
                                    ArchiveItem item = itemSnap.getValue(ArchiveItem.class);
                                    if (item != null) {
                                        total++;
                                        if (item.reviewed) {
                                            reviewed++;
                                        }
                                    }
                                } catch (Exception e) {
                                    }
                            }
                            subjectModels.add(new SubjectArchiveModel(subjectName, total, reviewed));
                        }
                    }
                    subjectModels.sort((a, b) -> a.getSubjectName().compareTo(b.getSubjectName()));
                    adapter.setSubjects(subjectModels);
                } catch (Exception e) {
                    }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded() && getContext() != null) {
                    String err = getString(R.string.err_loading_archives) + error.getMessage();
                    Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show();
                }
            }
        };
        baseDbRef.addValueEventListener(subjectsListener);
    }
    private void showSubjectArchivesBottomSheet(String subjectCanonical) {
        Context context = getContext();
        if (context == null) return;
        BottomSheetDialog filesDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
        ViewGroup container = (ViewGroup) getActivity().findViewById(android.R.id.content);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_subject_files_bottom_sheet, container, false);
        filesDialog.setContentView(sheetView);
        TextView tvSubjectTitle = sheetView.findViewById(R.id.tvSubjectTitle);
        String subjectDisplay = SubjectHelper.getTranslatedSubject(subjectCanonical);
        String headerText = subjectDisplay + " " + getString(R.string.archives_suffix);
        tvSubjectTitle.setText(headerText);
        RecyclerView rvArchiveFiles = sheetView.findViewById(R.id.rvArchiveFiles);
        rvArchiveFiles.setLayoutManager(new LinearLayoutManager(context));
        MaterialButton btnClose = sheetView.findViewById(R.id.btnCloseBottomSheet);
        btnClose.setOnClickListener(v -> filesDialog.dismiss());
        if (baseDbRef == null) return;
        baseDbRef.child(subjectCanonical).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                try {
                    List<ArchiveItem> archiveItems = new ArrayList<>();
                    for (DataSnapshot doc : snapshot.getChildren()) {
                        try {
                            ArchiveItem item = doc.getValue(ArchiveItem.class);
                            if (item != null) {
                                item.documentId = doc.getKey();
                                if (item.fileName == null) {
                                    item.fileName = doc.getKey();
                                }
                                archiveItems.add(item);
                            }
                        } catch (Exception e) {
                            }
                    }
                    archiveItems.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                    ArchiveFileAdapter fileAdapter = new ArchiveFileAdapter(getLayoutInflater(), archiveItems, new ArchiveFileAdapter.OnFileClickListener() {
                        @Override
                        public void onFileClick(ArchiveItem item) {
                            viewArchiveFile(item.fileUrl);
                        }
                        @Override
                        public void onDeleteClick(ArchiveItem item, int position) {
                            confirmAndDeleteArchiveItem(item, position, archiveItems, filesDialog);
                        }
                        @Override
                        public void onReviewedClick(ArchiveItem item) {
                            markAsReviewed(item);
                        }
                    });
                    rvArchiveFiles.setAdapter(fileAdapter);
                } catch (Exception e) {
                    }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    String err = getString(R.string.err_fetching_files) + error.getMessage();
                    Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show();
                }
            }
        });
        filesDialog.show();
    }
    private void markAsReviewed(ArchiveItem item) {
        if (baseDbRef == null || item.documentId == null) return;
        baseDbRef.child(item.subject).child(item.documentId).child("reviewed").setValue(!item.reviewed)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        String msg = !item.reviewed ? getString(R.string.msg_marked_reviewed) : getString(R.string.msg_marked_unreviewed);
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void confirmAndDeleteArchiveItem(ArchiveItem item, int position, List<ArchiveItem> list, BottomSheetDialog parentDialog) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.delete_archive_title)
                .setMessage(R.string.delete_archive_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (baseDbRef != null && item.documentId != null) {
                        baseDbRef.child(item.subject).child(item.documentId).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    if (isAdded()) {
                                        Toast.makeText(getContext(), R.string.file_deleted, Toast.LENGTH_SHORT).show();
                                        list.remove(position);
                                        parentDialog.dismiss();
                                        if (!list.isEmpty()) {
                                            showSubjectArchivesBottomSheet(item.subject);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (isAdded()) {
                                        String err = getString(R.string.err_failed_delete) + e.getMessage();
                                        Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    private void viewArchiveFile(String fileUrl) {
        Context context = getContext();
        if (fileUrl == null || context == null) return;
        if (fileUrl.toLowerCase().contains(".pdf")) {
            PdfHelper.openPdf(context, fileUrl);
            return;
        }
        android.app.Dialog viewDialog = new android.app.Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        viewDialog.setContentView(R.layout.dialog_view_image);
        if (viewDialog.getWindow() != null) {
            viewDialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        }
        ImageView ivFull = viewDialog.findViewById(R.id.ivFullImage);
        ImageButton btnClose = viewDialog.findViewById(R.id.btnClose);
        Glide.with(this).load(fileUrl).into(ivFull);
        btnClose.setOnClickListener(v -> viewDialog.dismiss());
        viewDialog.show();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (baseDbRef != null && subjectsListener != null) {
            baseDbRef.removeEventListener(subjectsListener);
        }
    }
    private static class ArchiveFileAdapter extends RecyclerView.Adapter<ArchiveFileAdapter.FileViewHolder> {
        private final LayoutInflater layoutInflater;
        private final List<ArchiveItem> items;
        private final OnFileClickListener fileClickListener;
        public interface OnFileClickListener {
            void onFileClick(ArchiveItem item);
            void onDeleteClick(ArchiveItem item, int position);
            void onReviewedClick(ArchiveItem item);
        }
        public ArchiveFileAdapter(LayoutInflater layoutInflater, List<ArchiveItem> items, OnFileClickListener fileClickListener) {
            this.layoutInflater = layoutInflater;
            this.items = items;
            this.fileClickListener = fileClickListener;
        }
        @NonNull
        @Override
        public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = layoutInflater.inflate(R.layout.item_archive_file, parent, false);
            return new FileViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
            ArchiveItem item = items.get(position);
            if (item.subject != null && !item.subject.isEmpty()) {
                holder.tvFileSubject.setVisibility(View.VISIBLE);
                holder.tvFileSubject.setText(SubjectHelper.getTranslatedSubject(item.subject));
            } else {
                holder.tvFileSubject.setVisibility(View.GONE);
            }
            holder.tvFileName.setText(item.fileName != null ? item.fileName : holder.itemView.getContext().getString(R.string.label_archived_file));
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy @ HH:mm", Locale.getDefault());
            String dateText = holder.itemView.getContext().getString(R.string.uploaded_prefix) + " " + sdf.format(new Date(item.timestamp));
            holder.tvUploadDate.setText(dateText);
            if (item.fileUrl != null && item.fileUrl.toLowerCase().endsWith(".pdf")) {
                holder.ivFileIcon.setImageResource(R.drawable.outline_assignment_24);
                holder.ivFileIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.color_error));
            } else {
                holder.ivFileIcon.setImageResource(R.drawable.outline_assignment_24);
                holder.ivFileIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.gapfix_green));
            }
            if (item.reviewed) {
                holder.btnReviewed.setText(holder.itemView.getContext().getString(R.string.ext_reviewed_done));
                holder.btnReviewed.setAlpha(0.6f);
            } else {
                holder.btnReviewed.setText(holder.itemView.getContext().getString(R.string.ext_i_reviewed_it));
                holder.btnReviewed.setAlpha(1.0f);
            }
            holder.itemView.setOnClickListener(v -> {
                if (fileClickListener != null) fileClickListener.onFileClick(item);
            });
            holder.btnDeleteArchive.setOnClickListener(v -> {
                if (fileClickListener != null) fileClickListener.onDeleteClick(item, position);
            });
            holder.btnReviewed.setOnClickListener(v -> {
                if (fileClickListener != null) fileClickListener.onReviewedClick(item);
            });
        }
        @Override
        public int getItemCount() {
            return items.size();
        }
        static class FileViewHolder extends RecyclerView.ViewHolder {
            ImageView ivFileIcon;
            TextView tvFileName, tvUploadDate, tvFileSubject;
            ImageButton btnDeleteArchive;
            MaterialButton btnReviewed;
            public FileViewHolder(@NonNull View itemView) {
                super(itemView);
                ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
                tvFileSubject = itemView.findViewById(R.id.tvFileSubject);
                tvFileName = itemView.findViewById(R.id.tvFileName);
                tvUploadDate = itemView.findViewById(R.id.tvUploadDate);
                btnDeleteArchive = itemView.findViewById(R.id.btnDeleteArchive);
                btnReviewed = itemView.findViewById(R.id.btnReviewed);
            }
        }
    }
}