package com.example.gapfix;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TutorStudentsFragment extends Fragment {

    private RecyclerView rvStudents;
    private TextView tvNoStudents;
    private StudentAdapter adapter;
    private final List<StudentModel> studentList = new ArrayList<>();
    private final Map<String, StudentModel> studentMap = new HashMap<>();
    private String currentUserId;
    private DatabaseReference bookingsRef;
    private DatabaseReference studentsRef;

    private FirebaseFirestore db;
    private BottomSheetDialog homeworkBottomSheet;
    private HomeworkAdapter sheetHomeworkAdapter;
    private final List<FirestoreMessage> sheetHomeworkList = new ArrayList<>();
    private ListenerRegistration sheetHomeworkListener;
    private ActivityResultLauncher<String[]> documentPickerLauncher;
    private Uri pendingHomeworkUri = null;
    private String selectedOtherUserId, selectedChatId;

    public TutorStudentsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = com.google.firebase.firestore.FirebaseFirestore.getInstance("gapfix");
        documentPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        pendingHomeworkUri = uri;
                        if (homeworkBottomSheet != null && homeworkBottomSheet.isShowing()) {
                            TextView tvStatus = homeworkBottomSheet.findViewById(R.id.tvHomeworkUploadStatus);
                            if (tvStatus != null) {
                                tvStatus.setText(R.string.file_uploaded);
                            }
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tutor_students, container, false);

        rvStudents = view.findViewById(R.id.rvStudents);
        tvNoStudents = view.findViewById(R.id.tvNoStudents);
        currentUserId = FirebaseAuth.getInstance().getUid();
        bookingsRef = FirebaseDatabase.getInstance().getReference("Bookings");
        studentsRef = FirebaseDatabase.getInstance().getReference("Users").child("Student");

        setupRecyclerView();
        loadStudents();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new StudentAdapter(studentList, student -> {
            
        }, (student, action) -> {
            if ("chat".equals(action)) {
                Intent intent = new Intent(getContext(), ChatActivity.class);
                intent.putExtra("CHAT_USER_ID", student.getUid());
                intent.putExtra("CHAT_USER_NAME", student.getName());
                if (student.getChatId() != null) {
                    intent.putExtra("CHAT_ID", student.getChatId());
                }
                startActivity(intent);
            } else if ("homework".equals(action)) {
                showHomeworkBottomSheet(student);
            } else if ("delete".equals(action)) {
                confirmAndDeleteStudent(student);
            }
        });
        rvStudents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvStudents.setAdapter(adapter);
    }

    private void loadStudents() {
        if (currentUserId == null) return;

        
        FirebaseDatabase.getInstance().getReference("Users").child("Tutor").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            
                            studentMap.clear();
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
                    String dsTutorId = ds.child("tutorId").getValue(String.class);
                    if (dsTutorId == null) dsTutorId = ds.child("teacherId").getValue(String.class);
                    if (dsTutorId == null) dsTutorId = ds.child("teacherID").getValue(String.class);

                    if (currentUserId.equals(dsTutorId)) {
                        String dsStudentId = ds.child("studentId").getValue(String.class);
                        if (dsStudentId == null) dsStudentId = ds.child("studentID").getValue(String.class);
                        
                        if (dsStudentId != null) {
                            Long ts = ds.child("timestamp").getValue(Long.class);
                            if (ts != null && ts > now) {
                                String status = ds.child("status").getValue(String.class);
                                if (!"cancelled".equalsIgnoreCase(status)) {
                                    futureLessonMap.put(dsStudentId, true);
                                }
                            }
                            if (!futureLessonMap.containsKey(dsStudentId)) {
                                futureLessonMap.put(dsStudentId, false);
                            }

                            if (!studentMap.containsKey(dsStudentId)) {
                                addStudentToMap(dsStudentId);
                            }
                        }
                    }
                }

                
                for (Map.Entry<String, Boolean> entry : futureLessonMap.entrySet()) {
                    StudentModel sm = studentMap.get(entry.getKey());
                    if (sm != null) {
                        sm.setCanDelete(!entry.getValue());
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
                        @SuppressWarnings("unchecked")
                        List<String> participants = (List<String>) doc.get("participants");
                        if (participants != null) {
                            for (String id : participants) {
                                if (!id.equals(currentUserId)) {
                                    StudentModel sm = studentMap.get(id);
                                    if (sm == null) {
                                        addStudentToMap(id);
                                        sm = studentMap.get(id);
                                    }
                                    if (sm != null) {
                                        sm.setChatId(doc.getId());

                                        
                                        Map<String, Object> unreadMap = (Map<String, Object>) doc.get("unreadCount");
                                        if (unreadMap != null && unreadMap.containsKey(currentUserId)) {
                                            Object count = unreadMap.get(currentUserId);
                                            sm.setUnreadCount(count instanceof Number ? ((Number) count).intValue() : 0);
                                        } else {
                                            sm.setUnreadCount(0);
                                        }

                                        
                                        Map<String, Object> chatUnreadMap = (Map<String, Object>) doc.get("unreadChatCount");
                                        if (chatUnreadMap != null && chatUnreadMap.containsKey(currentUserId)) {
                                            Object count = chatUnreadMap.get(currentUserId);
                                            sm.setUnreadChatCount(count instanceof Number ? ((Number) count).intValue() : 0);
                                        } else {
                                            sm.setUnreadChatCount(0);
                                        }

                                        Map<String, Object> hwUnreadMap = (Map<String, Object>) doc.get("unreadHomeworkCount");
                                        if (hwUnreadMap != null && hwUnreadMap.containsKey(currentUserId)) {
                                            Object count = hwUnreadMap.get(currentUserId);
                                            sm.setUnreadHomeworkCount(count instanceof Number ? ((Number) count).intValue() : 0);
                                        } else {
                                            sm.setUnreadHomeworkCount(0);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    updateList();
                });
    }

    private void addStudentToMap(String studentId) {
        StudentModel sm = new StudentModel();
        sm.setUid(studentId);
        sm.setCanDelete(true); 
        studentMap.put(studentId, sm);
        fetchStudentDetails(studentId);
    }

    private void fetchStudentDetails(String studentId) {
        studentsRef.child(studentId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null && studentMap.containsKey(studentId)) {
                        StudentModel sm = studentMap.get(studentId);
                        if (sm != null) {
                            sm.setName(user.getName());
                            sm.setEmail(user.getEmail());
                            sm.setProfileImage(user.getImageResourceLink());
                            updateList();
                        }
                    }
                } else {
                    
                    studentMap.remove(studentId);
                    updateList();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showHomeworkBottomSheet(StudentModel student) {
        if (homeworkBottomSheet != null && homeworkBottomSheet.isShowing()) return;

        selectedOtherUserId = student.getUid();

        
        if (student.getChatId() != null && !student.getChatId().isEmpty()) {
            selectedChatId = student.getChatId();
        } else {
            List<String> ids = new ArrayList<>(Arrays.asList(currentUserId, selectedOtherUserId));
            ids.sort(null);
            selectedChatId = ids.get(0) + "_" + ids.get(1);
        }

        homeworkBottomSheet = new BottomSheetDialog(requireContext());

        
        Map<String, Object> clear = new HashMap<>();
        clear.put("unreadHomeworkCount." + currentUserId, 0);
        db.collection("chats").document(selectedChatId).update(clear);

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_homework_bottom_sheet, null);

        TextView tvSheetTitle = view.findViewById(R.id.tvTitle);
        TextInputEditText etTitle = view.findViewById(R.id.etHomeworkTitle);
        AutoCompleteTextView autoCompleteLesson = view.findViewById(R.id.actvLessonSelector);
        AutoCompleteTextView autoCompleteSubject = view.findViewById(R.id.actvSubjectSelector);
        MaterialCardView btnUpload = view.findViewById(R.id.btnUploadHomeworkFile);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveHomework);
        RecyclerView rvHomeworks = view.findViewById(R.id.rvHomeworks);

        pendingHomeworkUri = null;

        if (tvSheetTitle != null) tvSheetTitle.setText(R.string.ext_add_homework);
        Map<String, List<Booking>> bookingsBySubject = new HashMap<>();
        List<String> subjectStrings = new ArrayList<>();

        bookingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long now = System.currentTimeMillis();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Booking b = ds.getValue(Booking.class);
                    if (b != null) {
                        
                        String sId = ds.child("studentId").getValue(String.class);
                        if (sId == null) sId = ds.child("studentID").getValue(String.class);
                        
                        String tId = ds.child("tutorId").getValue(String.class);
                        if (tId == null) tId = ds.child("teacherId").getValue(String.class);
                        if (tId == null) tId = ds.child("teacherID").getValue(String.class);

                        boolean match = (tId != null && tId.equals(currentUserId) && sId != null && sId.equals(selectedOtherUserId));

                        if (match) {
                            
                            Long ts = ds.child("timestamp").getValue(Long.class);
                            String status = ds.child("status").getValue(String.class);
                            String subject = ds.child("subject").getValue(String.class);
                            
                            if (ts != null && ts > 0 && !"cancelled".equalsIgnoreCase(status)) {
                                if (subject == null || subject.isEmpty()) {
                                    subject = getString(R.string.unspecified_subject);
                                }

                                
                                b.setTimestamp(ts);
                                b.setStatus(status);
                                b.setSubject(subject);
                                b.setStudentId(sId);
                                b.setTutorId(tId);

                                List<Booking> list = bookingsBySubject.computeIfAbsent(subject, k -> new ArrayList<>());
                                list.add(b);
                                if (!subjectStrings.contains(subject)) {
                                    subjectStrings.add(subject);
                                }
                            }
                        }
                    }
                }

                subjectStrings.sort(null);
                if (!isAdded()) return;

                ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, subjectStrings);
                if (autoCompleteSubject != null) {
                    autoCompleteSubject.setAdapter(subjectAdapter);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        final Booking[] selectedBooking = {null};
        final String[] selectedSubjectName = {null};
        SimpleDateFormat displayFormat = new SimpleDateFormat("EEEE, MMM dd @ HH:mm", Locale.getDefault());

        if (autoCompleteSubject != null) {
            autoCompleteSubject.setOnItemClickListener((parent, v, position, id) -> {
                selectedSubjectName[0] = subjectStrings.get(position);
                selectedBooking[0] = null;
                if (autoCompleteLesson != null) autoCompleteLesson.setText("");

                List<Booking> filteredLessons = bookingsBySubject.get(selectedSubjectName[0]);
                if (filteredLessons != null) {
                    filteredLessons.sort((b1, b2) -> Long.compare(b2.getTimestamp(), b1.getTimestamp()));
                    List<String> lessonStrings = new ArrayList<>();
                    for (Booking b : filteredLessons) {
                        lessonStrings.add(displayFormat.format(new Date(b.getTimestamp())));
                    }
                    ArrayAdapter<String> lessonAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, lessonStrings);
                    if (autoCompleteLesson != null) {
                        autoCompleteLesson.setAdapter(lessonAdapter);
                        autoCompleteLesson.showDropDown();
                    }
                }
            });
        }

        if (autoCompleteLesson != null) {
            autoCompleteLesson.setOnItemClickListener((parent, v, position, id) -> {
                if (selectedSubjectName[0] != null) {
                    List<Booking> filtered = bookingsBySubject.get(selectedSubjectName[0]);
                    if (filtered != null && position < filtered.size()) {
                        selectedBooking[0] = filtered.get(position);
                    }
                }
            });
        }

        if (btnUpload != null) {
            btnUpload.setOnClickListener(v -> documentPickerLauncher.launch(new String[]{"image/*", "application/pdf"}));
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String titleText = (etTitle != null && etTitle.getText() != null) ? etTitle.getText().toString().trim() : "";
                if (titleText.isEmpty()) {
                    Toast.makeText(getContext(), R.string.err_enter_title, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (selectedBooking[0] == null) {
                    Toast.makeText(getContext(), R.string.err_select_lesson, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (pendingHomeworkUri == null) {
                    Toast.makeText(getContext(), R.string.err_upload_file_first, Toast.LENGTH_SHORT).show();
                    return;
                }
                uploadHomeworkImage(pendingHomeworkUri, titleText, selectedBooking[0].getTimestamp(), selectedBooking[0].getSubject());
            });
        }

        sheetHomeworkAdapter = new HomeworkAdapter(sheetHomeworkList, "Tutor", new HomeworkAdapter.OnHomeworkActionListener() {
            @Override public void onViewFile(String url) { openFile(url); }
            @Override public void onDeleteHomework(FirestoreMessage msg) { deleteHomework(msg); }
            @Override public void onEditHomework(FirestoreMessage msg) { }
            @Override public void onMarkRight(FirestoreMessage msg) { updateFeedback(msg, "correct"); }
            @Override public void onMarkWrong(FirestoreMessage msg) { updateFeedback(msg, "incorrect"); }
            @Override public void onUploadSolution(FirestoreMessage msg) {}
            @Override public void onCouldnDoIt(FirestoreMessage msg) {}
            @Override public void onArchiveHomework(FirestoreMessage msg) {}
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

    private void deleteHomework(FirestoreMessage msg) {
        db.collection("chats").document(selectedChatId).collection("messages").document(msg.documentId).delete();
    }

    private void updateFeedback(FirestoreMessage msg, String feedback) {
        db.collection("chats").document(selectedChatId).collection("messages").document(msg.documentId)
                .update("tutorFeedback", feedback).addOnSuccessListener(a -> {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("lastMessage", "[Homework Reviewed: " + feedback + "]");
                    meta.put("lastMessageTime", com.google.firebase.firestore.FieldValue.serverTimestamp());
                    if (selectedOtherUserId != null) {
                        meta.put("unreadCount." + selectedOtherUserId, com.google.firebase.firestore.FieldValue.increment(1));
                        meta.put("unreadHomeworkCount." + selectedOtherUserId, com.google.firebase.firestore.FieldValue.increment(1));
                    }
                    db.collection("chats").document(selectedChatId).set(meta, com.google.firebase.firestore.SetOptions.merge());
                });
    }

    private void uploadHomeworkImage(Uri uri, String title, long lessonTs, String subject) {
        Toast.makeText(getContext(), R.string.uploading, Toast.LENGTH_SHORT).show();
        MediaManager.get().upload(uri).unsigned("ml_default").option("folder", "Homeworks/" + selectedChatId).callback(new UploadCallback() {
            @Override public void onSuccess(String requestId, Map resultData) {
                String url = (String) resultData.get("secure_url");
                if (isAdded()) requireActivity().runOnUiThread(() -> sendHomeworkMessage(url, title, lessonTs, subject));
            }
            @Override public void onError(String r, ErrorInfo e) {}
            @Override public void onStart(String r) {}
            @Override public void onProgress(String r, long b, long t) {}
            @Override public void onReschedule(String r, ErrorInfo e) {}
        }).dispatch();
    }

    private void sendHomeworkMessage(String url, String title, long lessonTs, String subject) {
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("receiverId", selectedOtherUserId);
        message.put("text", title);
        message.put("type", "homework");
        message.put("fileUrl", url);
        message.put("timestamp", FieldValue.serverTimestamp());
        message.put("homeworkStatus", "pending");
        message.put("lessonTimestamp", lessonTs);
        message.put("subject", subject);

        db.collection("chats").document(selectedChatId).collection("messages").add(message).addOnSuccessListener(a -> {
            Map<String, Object> meta = new HashMap<>();
            meta.put("lastMessage", "[Assigned: " + title + "]");
            meta.put("lastMessageType", "homework");
            meta.put("lastMessageTime", FieldValue.serverTimestamp());
            meta.put("participants", Arrays.asList(currentUserId, selectedOtherUserId));
            if (selectedOtherUserId != null) {
                meta.put("unreadCount." + selectedOtherUserId, FieldValue.increment(1));
                meta.put("unreadHomeworkCount." + selectedOtherUserId, FieldValue.increment(1));
            }
            db.collection("chats").document(selectedChatId).set(meta, SetOptions.merge());
            Toast.makeText(getContext(), R.string.homework_assigned, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateList() {
        studentList.clear();
        for (StudentModel sm : studentMap.values()) {
            
            if (!sm.isCanDelete() || (sm.getChatId() != null && !sm.getChatId().isEmpty())) {
                studentList.add(sm);
            }
        }
        
        if (isAdded()) {
            tvNoStudents.setVisibility(studentList.isEmpty() ? View.VISIBLE : View.GONE);
            adapter.notifyDataSetChanged();
        }
    }

    private void confirmAndDeleteStudent(StudentModel student) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Student Connection")
                .setMessage("This will delete the chat history with " + student.getName() + ". You will no longer see this student in your list unless there are future lessons scheduled.")
                .setPositiveButton("Delete", (dialog, which) -> deleteStudentChat(student))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteStudentChat(StudentModel student) {
        String cid = student.getChatId();
        if (cid == null || cid.isEmpty()) {
            
            studentMap.remove(student.getUid());
            updateList();
            return;
        }

        db.collection("chats").document(cid).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Student removed from list", Toast.LENGTH_SHORT).show();
                    
                    
                    student.setChatId(null);
                    updateList();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete chat", Toast.LENGTH_SHORT).show());
    }

    public static class StudentModel {
        private String uid;
        private String name;
        private String email;
        private String profileImage;
        private String chatId;
        private boolean isExpanded = false;
        private boolean canDelete = false;
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
