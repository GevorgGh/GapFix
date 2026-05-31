package com.example.gapfix;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public class StudentTutorsFragment extends Fragment {
    private RecyclerView rvTutors;
    private TextView tvNoTutors;
    private StudentTutorAdapter adapter;
    private final List<TutorModel> tutorList = new ArrayList<>();
    private final Map<String, TutorModel> tutorMap = new HashMap<>();
    private String currentUserId;
    private DatabaseReference bookingsRef;
    private DatabaseReference tutorsRef;
    private FirebaseFirestore db;
    private BottomSheetDialog homeworkBottomSheet;
    private HomeworkAdapter sheetHomeworkAdapter;
    private final List<FirestoreMessage> sheetHomeworkList = new ArrayList<>();
    private ListenerRegistration sheetHomeworkListener;
    private ActivityResultLauncher<String[]> solutionPickerLauncher;
    private FirestoreMessage activeSolvingMessage = null;
    private String selectedOtherUserId, selectedChatId;
    public StudentTutorsFragment() {}
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = com.google.firebase.firestore.FirebaseFirestore.getInstance("gapfix");
        solutionPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null && activeSolvingMessage != null) {
                        uploadSolutionImage(uri, activeSolvingMessage);
                    }
                }
        );
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_tutors, container, false);
        rvTutors = view.findViewById(R.id.rvTutors);
        tvNoTutors = view.findViewById(R.id.tvNoTutors);
        currentUserId = FirebaseAuth.getInstance().getUid();
        bookingsRef = FirebaseDatabase.getInstance().getReference("Bookings");
        tutorsRef = FirebaseDatabase.getInstance().getReference("Users").child("Tutor");
        setupRecyclerView();
        loadTutors();
        return view;
    }
    private void setupRecyclerView() {
        adapter = new StudentTutorAdapter(tutorList, tutor -> {
        }, (tutor, action) -> {
            if ("chat".equals(action)) {
                Intent intent = new Intent(getContext(), ChatActivity.class);
                intent.putExtra("CHAT_USER_ID", tutor.getUid());
                intent.putExtra("CHAT_USER_NAME", tutor.getName());
                if (tutor.getChatId() != null) {
                    intent.putExtra("CHAT_ID", tutor.getChatId());
                }
                startActivity(intent);
            } else if ("homework".equals(action)) {
                showHomeworkBottomSheet(tutor);
            } else if ("delete".equals(action)) {
                confirmAndDeleteTutor(tutor);
            }
        });
        rvTutors.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTutors.setAdapter(adapter);
    }
    private void loadTutors() {
        if (currentUserId == null) return;
        FirebaseDatabase.getInstance().getReference("Users").child("Student").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            tutorMap.clear();
                            updateList();
                            return;
                        }
                        startLoadingData();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
    private void startLoadingData() {
        bookingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long now = System.currentTimeMillis();
                Map<String, Boolean> futureLessonMap = new HashMap<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String sId = ds.child("studentId").getValue(String.class);
                    if (sId == null) sId = ds.child("studentID").getValue(String.class);
                    if (currentUserId.equals(sId)) {
                        String tId = ds.child("tutorId").getValue(String.class);
                        if (tId == null) tId = ds.child("teacherId").getValue(String.class);
                        if (tId == null) tId = ds.child("teacherID").getValue(String.class);
                        if (tId != null) {
                            Long ts = ds.child("timestamp").getValue(Long.class);
                            if (ts != null && ts > now) {
                                String status = ds.child("status").getValue(String.class);
                                if (!"cancelled".equalsIgnoreCase(status)) {
                                    futureLessonMap.put(tId, true);
                                }
                            }
                            if (!futureLessonMap.containsKey(tId)) {
                                futureLessonMap.put(tId, false);
                            }
                            if (!tutorMap.containsKey(tId)) {
                                addTutorToMap(tId);
                            }
                        }
                    }
                }
                for (Map.Entry<String, Boolean> entry : futureLessonMap.entrySet()) {
                    TutorModel tm = tutorMap.get(entry.getKey());
                    if (tm != null) {
                        tm.setCanDelete(!entry.getValue());
                    }
                }
                updateList();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
        db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    for (DocumentSnapshot doc : snapshots) {
                        Object participantsObj = doc.get("participants");
                        if (participantsObj instanceof List) {
                            List<?> participants = (List<?>) participantsObj;
                            for (Object idObj : participants) {
                                if (idObj instanceof String) {
                                    String id = (String) idObj;
                                    if (!id.equals(currentUserId)) {
                                        TutorModel tm = tutorMap.get(id);
                                        if (tm == null) {
                                            addTutorToMap(id);
                                            tm = tutorMap.get(id);
                                        }
                                        if (tm != null) {
                                            tm.setChatId(doc.getId());
                                            int chatUnread = getFirestoreCount(doc, "unreadChatCount", currentUserId);
                                            int hwUnread = getFirestoreCount(doc, "unreadHomeworkCount", currentUserId);
                                            tm.setUnreadChatCount(chatUnread);
                                            tm.setUnreadHomeworkCount(hwUnread);
                                            tm.setUnreadCount(chatUnread + hwUnread);
                                            
                                            
                                            if (chatUnread == 0 && hwUnread == 0) {
                                                tm.setUnreadCount(getFirestoreCount(doc, "unreadCount", currentUserId));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    updateList();
                });
    }

    private int getFirestoreCount(DocumentSnapshot doc, String field, String uid) {
        Object obj = doc.get(field);
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            if (map.containsKey(uid)) {
                Object val = map.get(uid);
                if (val instanceof Number) return ((Number) val).intValue();
            }
        }
        Object flat = doc.get(field + "." + uid);
        if (flat instanceof Number) return ((Number) flat).intValue();
        return 0;
    }
    private void addTutorToMap(String tutorId) {
        TutorModel tm = new TutorModel();
        tm.setUid(tutorId);
        tm.setCanDelete(true); 
        tutorMap.put(tutorId, tm);
        fetchTutorDetails(tutorId);
    }
    private void fetchTutorDetails(String tutorId) {
        tutorsRef.child(tutorId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null && tutorMap.containsKey(tutorId)) {
                        TutorModel tm = tutorMap.get(tutorId);
                        if (tm != null) {
                            tm.setName(user.getName());
                            tm.setEmail(user.getEmail());
                            String img = user.getImageResourceLink();
                            if (img == null) {
                                img = snapshot.child("profilePicture").getValue(String.class);
                            }
                            tm.setProfileImage(img);
                            updateList();
                        }
                    }
                } else {
                    tutorMap.remove(tutorId);
                    updateList();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void showHomeworkBottomSheet(TutorModel tutor) {
        if (homeworkBottomSheet != null && homeworkBottomSheet.isShowing()) return;
        selectedOtherUserId = tutor.getUid();
        if (tutor.getChatId() != null && !tutor.getChatId().isEmpty()) {
            selectedChatId = tutor.getChatId();
        } else {
            List<String> ids = new ArrayList<>(Arrays.asList(currentUserId, selectedOtherUserId));
            ids.sort(null);
            selectedChatId = ids.get(0) + "_" + ids.get(1);
        }
        homeworkBottomSheet = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        Map<String, Object> clear = new HashMap<>();
        clear.put("unreadHomeworkCount." + currentUserId, 0);
        db.collection("chats").document(selectedChatId).update(clear);
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_homework_bottom_sheet, null, false);
        TextView tvSheetTitle = view.findViewById(R.id.tvTitle);
        View tilTitle = view.findViewById(R.id.tilHomeworkTitle);
        View tilLesson = view.findViewById(R.id.tilLessonSelector);
        View tilSubject = view.findViewById(R.id.tilSubjectSelector);
        View btnUpload = view.findViewById(R.id.btnUploadHomeworkFile);
        View btnSave = view.findViewById(R.id.btnSaveHomework);
        RecyclerView rvHomeworks = view.findViewById(R.id.rvHomeworks);
        if (tvSheetTitle != null) tvSheetTitle.setText(R.string.ext_homeworks);
        if (tilTitle != null) tilTitle.setVisibility(View.GONE);
        if (tilLesson != null) tilLesson.setVisibility(View.GONE);
        if (tilSubject != null) tilSubject.setVisibility(View.GONE);
        if (btnUpload != null) btnUpload.setVisibility(View.GONE);
        if (btnSave != null) btnSave.setVisibility(View.GONE);
        sheetHomeworkList.clear();
        sheetHomeworkAdapter = new HomeworkAdapter(sheetHomeworkList, "Student", new HomeworkAdapter.OnHomeworkActionListener() {
            @Override public void onViewFile(String url) { openFile(url); }
            @Override public void onDeleteHomework(FirestoreMessage msg) {}
            @Override public void onEditHomework(FirestoreMessage msg) {}
            @Override public void onMarkRight(FirestoreMessage msg) {}
            @Override public void onMarkWrong(FirestoreMessage msg) {}
            @Override public void onUploadSolution(FirestoreMessage msg) { uploadSolution(msg); }
            @Override public void onCouldnDoIt(FirestoreMessage msg) { markFailed(msg); }
            @Override public void onArchiveHomework(FirestoreMessage msg) { archiveHomework(msg); }
        });
        rvHomeworks.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHomeworks.setAdapter(sheetHomeworkAdapter);
        if (sheetHomeworkListener != null) sheetHomeworkListener.remove();
        sheetHomeworkListener = db.collection("chats").document(selectedChatId).collection("messages")
                .whereEqualTo("type", "homework")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    sheetHomeworkList.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        FirestoreMessage msg = doc.toObject(FirestoreMessage.class);
                        if (msg != null) {
                            msg.documentId = doc.getId();
                            sheetHomeworkList.add(msg);
                        }
                    }
                    sheetHomeworkList.sort((m1, m2) -> {
                        long t1 = m1.timestamp != null ? m1.timestamp.toDate().getTime() : 0;
                        long t2 = m2.timestamp != null ? m2.timestamp.toDate().getTime() : 0;
                        return Long.compare(t2, t1);
                    });
                    sheetHomeworkAdapter.notifyDataSetChanged();
                });
        homeworkBottomSheet.setContentView(view);
        homeworkBottomSheet.show();
    }
    private void uploadSolution(FirestoreMessage msg) {
        activeSolvingMessage = msg;
        solutionPickerLauncher.launch(new String[]{"image/*", "application/pdf"});
    }
    private void uploadSolutionImage(android.net.Uri uri, final FirestoreMessage msg) {
        if (selectedChatId == null) {
            return;
        }
        Toast.makeText(getContext(), "Uploading solution...", Toast.LENGTH_SHORT).show();
        com.cloudinary.android.MediaManager.get().upload(uri)
                .unsigned("ml_default")
                .option("folder", "Solutions/" + selectedChatId)
                .callback(new com.cloudinary.android.callback.UploadCallback() {
            @Override public void onSuccess(String requestId, Map resultData) {
                final String url = (String) resultData.get("secure_url");
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("solutionUrl", url);
                        updates.put("homeworkStatus", "done");
                        db.collection("chats").document(selectedChatId).collection("messages").document(msg.documentId).update(updates)
                                .addOnSuccessListener(a -> {
                                    if (isAdded()) Toast.makeText(getContext(), "Solution uploaded", Toast.LENGTH_SHORT).show();
                                    Map<String, Object> meta = new HashMap<>();
                                    meta.put("participants", Arrays.asList(currentUserId, selectedOtherUserId));
                                    meta.put("lastMessage", "[Solution Uploaded]");
                                    meta.put("lastMessageType", "solution");
                                    meta.put("lastMessageTime", com.google.firebase.firestore.FieldValue.serverTimestamp());
                                    if (selectedOtherUserId != null) {
                                        meta.put("unreadCount." + selectedOtherUserId, com.google.firebase.firestore.FieldValue.increment(1));
                                        meta.put("unreadHomeworkCount." + selectedOtherUserId, com.google.firebase.firestore.FieldValue.increment(1));
                                    }
                                    db.collection("chats").document(selectedChatId).set(meta, com.google.firebase.firestore.SetOptions.merge());
                                    activeSolvingMessage = null;
                                });
                    });
                }
            }
            @Override public void onError(String r, com.cloudinary.android.callback.ErrorInfo e) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Upload failed: " + e.getDescription(), Toast.LENGTH_SHORT).show());
            }
            @Override public void onStart(String r) {}
            @Override public void onProgress(String r, long b, long t) {}
            @Override public void onReschedule(String r, com.cloudinary.android.callback.ErrorInfo e) {}
        }).dispatch();
    }
    private void markFailed(FirestoreMessage msg) {
        db.collection("chats").document(selectedChatId).collection("messages").document(msg.documentId)
                .update("homeworkStatus", "failed").addOnSuccessListener(a -> {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("lastMessage", "[Couldn't do homework]");
                    meta.put("lastMessageTime", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    if (selectedOtherUserId != null) {
                        meta.put("unreadCount." + selectedOtherUserId, com.google.firebase.firestore.FieldValue.increment(1));
                        meta.put("unreadHomeworkCount." + selectedOtherUserId, com.google.firebase.firestore.FieldValue.increment(1));
                    }
                    db.collection("chats").document(selectedChatId).set(meta, com.google.firebase.firestore.SetOptions.merge());
                });
    }
    private void archiveHomework(FirestoreMessage msg) {
        if (msg.fileUrl == null) return;
        String title = msg.text != null ? msg.text : "Archived Homework";
        String subject = msg.subject != null ? msg.subject : "General";
        String safeTitle = title.replaceAll("[.#$\\[\\]/]", "_");
        String safeSubject = subject.replaceAll("[.#$\\[\\]/]", "_");
        ArchiveItem item = new ArchiveItem(msg.documentId, currentUserId, safeSubject, msg.fileUrl, safeTitle, System.currentTimeMillis());
        FirebaseDatabase.getInstance().getReference("Users").child("Student").child(currentUserId)
                .child("Archives").child(safeSubject).child(safeTitle).setValue(item)
                .addOnSuccessListener(a -> Toast.makeText(getContext(), "Added to Archive", Toast.LENGTH_SHORT).show());
    }
    private void openFile(String url) {
        if (url == null) return;
        if (url.toLowerCase().contains(".pdf")) {
            PdfHelper.openPdf(requireContext(), url);
        } else {
            android.app.Dialog viewDialog = new android.app.Dialog(requireContext(), android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        viewDialog.setContentView(R.layout.dialog_view_image);
        if (viewDialog.getWindow() != null) {
            viewDialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        }
        ImageView ivFull = viewDialog.findViewById(R.id.ivFullImage);
        ImageButton btnClose = viewDialog.findViewById(R.id.btnClose);
        if (ivFull != null) com.bumptech.glide.Glide.with(this).load(url).into(ivFull);
        if (btnClose != null) btnClose.setOnClickListener(v -> viewDialog.dismiss());
        viewDialog.show();
    }
    }
    private void updateList() {
        tutorList.clear();
        for (TutorModel tm : tutorMap.values()) {
            if (!tm.isCanDelete() || (tm.getChatId() != null && !tm.getChatId().isEmpty())) {
                tutorList.add(tm);
            }
        }
        if (isAdded()) {
            tvNoTutors.setVisibility(tutorList.isEmpty() ? View.VISIBLE : View.GONE);
            adapter.notifyDataSetChanged();
        }
    }
    private void confirmAndDeleteTutor(TutorModel tutor) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Tutor")
                .setMessage("Delete chat history with " + tutor.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (tutor.getChatId() != null) {
                        db.collection("chats").document(tutor.getChatId()).delete()
                                .addOnSuccessListener(a -> {
                                    tutor.setChatId(null);
                                    updateList();
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    public static class TutorModel {
        private String uid, name, email, profileImage, chatId;
        private boolean isExpanded = false, canDelete = false;
        private int unreadCount = 0;
        private int unreadChatCount = 0;
        private int unreadHomeworkCount = 0;
        public String getUid() { return uid; }
        public void setUid(String uid) { this.uid = uid; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getProfileImage() { return profileImage; }
        public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
        public String getChatId() { return chatId; }
        public void setChatId(String chatId) { this.chatId = chatId; }
        public boolean isExpanded() { return isExpanded; }
        public void setExpanded(boolean expanded) { isExpanded = expanded; }
        public boolean isCanDelete() { return canDelete; }
        public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }
        public int getUnreadCount() { return unreadCount; }
        public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
        public int getUnreadChatCount() { return unreadChatCount; }
        public void setUnreadChatCount(int unreadChatCount) { this.unreadChatCount = unreadChatCount; }
        public int getUnreadHomeworkCount() { return unreadHomeworkCount; }
        public void setUnreadHomeworkCount(int unreadHomeworkCount) { this.unreadHomeworkCount = unreadHomeworkCount; }
    }
}
