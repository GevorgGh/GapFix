package com.example.gapfix;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class ChatActivity extends AppCompatActivity implements MessageAdapter.OnHomeworkActionListener {
    private static final String TAG = "ChatActivity";
    private static boolean cloudinaryInitialized = false;
    private String currentUserId, otherUserId, otherUserName, chatId;
    private String currentUserName = "Me";
    private String currentUserRole = "Student";
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private final List<FirestoreMessage> messageList = new ArrayList<>();
    private EditText etMessage;
    private TextView tvChatName;
    private FirebaseFirestore db;
    private DatabaseReference rtdbRef;
    private ListenerRegistration messageListener;
    private androidx.activity.result.ActivityResultLauncher<String[]> solutionPickerLauncher;
    private FirestoreMessage activeSolvingMessage = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        otherUserId = getIntent().getStringExtra("CHAT_USER_ID");
        otherUserName = getIntent().getStringExtra("CHAT_USER_NAME");
        currentUserId = FirebaseAuth.getInstance().getUid();
        if (TextUtils.isEmpty(otherUserId) || currentUserId == null) {
            finish();
            return;
        }
        db = com.google.firebase.firestore.FirebaseFirestore.getInstance("gapfix");
        rtdbRef = FirebaseDatabase.getInstance().getReference();
        chatId = getIntent().getStringExtra("CHAT_ID");
        if (chatId == null) {
            List<String> ids = Arrays.asList(currentUserId, otherUserId);
            ids.sort(String::compareTo);
            chatId = ids.get(0) + "_" + ids.get(1);
        }
        initCloudinary();
        initUI();
        initPickers();
        fetchUsersFromRTDB();
        clearUnreadMessagesCount();
    }
    @Override
    protected void onResume() {
        super.onResume();
        clearUnreadMessagesCount();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) messageListener.remove();
    }
    private void initCloudinary() {
        if (!cloudinaryInitialized) {
            try {
                Map<String, String> config = new HashMap<>();
                config.put("cloud_name", "dbugqpl3m");
                MediaManager.init(this, config);
                cloudinaryInitialized = true;
            } catch (IllegalStateException e) {
                cloudinaryInitialized = true;
            } catch (Exception e) {
                }
        }
    }
    private void initUI() {
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        tvChatName = findViewById(R.id.tvChatName);
        tvChatName.setText(otherUserName != null ? otherUserName : "Chat");
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSend).setOnClickListener(v -> sendTextMessage());
        adapter = new MessageAdapter(messageList, currentUserId, currentUserRole, this);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        rvMessages.setAdapter(adapter);
    }
    private void initPickers() {
        solutionPickerLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null && activeSolvingMessage != null) {
                        uploadSolutionImage(uri, activeSolvingMessage);
                        activeSolvingMessage = null;
                    }
                });
    }
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx != -1) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }
    private void fetchUsersFromRTDB() {
        rtdbRef.child("Users").child("Student").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentUserName = snapshot.child("name").getValue(String.class);
                    currentUserRole = "Student";
                    checkProfilesReady();
                } else {
                    rtdbRef.child("Users").child("Tutor").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot tutorSnap) {
                            if (tutorSnap.exists()) {
                                currentUserName = tutorSnap.child("name").getValue(String.class);
                                currentUserRole = "Tutor";
                            }
                            checkProfilesReady();
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) { }
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
        rtdbRef.child("Users").child("Tutor").child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    otherUserName = snapshot.child("name").getValue(String.class);
                    runOnUiThread(() -> tvChatName.setText(otherUserName));
                } else {
                    rtdbRef.child("Users").child("Student").child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot studentSnap) {
                            if (studentSnap.exists()) {
                                otherUserName = studentSnap.child("name").getValue(String.class);
                                runOnUiThread(() -> tvChatName.setText(otherUserName));
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) { }
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });
    }
    private void checkProfilesReady() {
        if (adapter != null) {
            adapter.setUserRole(currentUserRole);
        }
        listenForMessages();
    }
    private void listenForMessages() {
        if (messageListener != null) messageListener.remove();
        messageListener = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    List<FirestoreMessage> incoming = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        FirestoreMessage msg = doc.toObject(FirestoreMessage.class);
                        if (msg != null) {
                            msg.documentId = doc.getId();
                            if ("homework".equalsIgnoreCase(msg.type)) {
                                continue;
                            }
                            incoming.add(msg);
                        }
                    }
                    incoming.sort((m1, m2) -> {
                        long t1 = m1.timestamp != null ? m1.timestamp.toDate().getTime() : Long.MAX_VALUE;
                        long t2 = m2.timestamp != null ? m2.timestamp.toDate().getTime() : Long.MAX_VALUE;
                        return Long.compare(t1, t2);
                    });
                    buildMessageListWithHeaders(incoming);
                });
    }
    private void buildMessageListWithHeaders(List<FirestoreMessage> rawMessages) {
        messageList.clear();
        String lastDateStr = "";
        for (FirestoreMessage msg : rawMessages) {
            long timeMillis;
            if (msg.timestamp != null) {
                timeMillis = msg.timestamp.toDate().getTime();
            } else {
                timeMillis = System.currentTimeMillis();
            }
            String currentDateStr = MessageAdapter.getFormattedDate(timeMillis);
            if (!currentDateStr.equals(lastDateStr)) {
                FirestoreMessage header = new FirestoreMessage();
                header.senderId = "DATE_HEADER";
                header.text = currentDateStr;
                header.timestamp = new Timestamp(new Date(timeMillis - 1));
                messageList.add(header);
                lastDateStr = currentDateStr;
            }
            messageList.add(msg);
        }
        adapter.notifyDataSetChanged();
        if (!messageList.isEmpty()) {
            rvMessages.scrollToPosition(messageList.size() - 1);
        }
    }
    private void clearUnreadMessagesCount() {
        if (chatId == null || currentUserId == null) return;
        Map<String, Object> clear = new HashMap<>();
        clear.put("unreadCount." + currentUserId, 0);
        clear.put("unreadChatCount." + currentUserId, 0);
        db.collection("chats").document(chatId).update(clear);
    }
    private void sendTextMessage() {
        String content = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;
        etMessage.setText("");
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("receiverId", otherUserId);
        message.put("text", content);
        message.put("type", "text");
        message.put("timestamp", FieldValue.serverTimestamp());
        db.collection("chats").document(chatId).collection("messages").add(message);
        updateChatMeta(content);
    }
    private void updateChatMeta(String lastMsg) {
        Map<String, Object> chatMeta = new HashMap<>();
        chatMeta.put("participants", Arrays.asList(currentUserId, otherUserId));
        chatMeta.put("lastMessage", lastMsg);
        chatMeta.put("lastMessageType", "text");
        chatMeta.put("lastMessageTime", FieldValue.serverTimestamp());
        Map<String, String> namesMap = new HashMap<>();
        namesMap.put(currentUserId, currentUserName != null ? currentUserName : "Me");
        namesMap.put(otherUserId, otherUserName != null ? otherUserName : "User");
        chatMeta.put("participantNames", namesMap);
        if (otherUserId != null) {
            chatMeta.put("unreadCount." + otherUserId, FieldValue.increment(1));
            chatMeta.put("unreadChatCount." + otherUserId, FieldValue.increment(1));
        }
        db.collection("chats").document(chatId).set(chatMeta, SetOptions.merge());
    }
    private void uploadSolutionImage(Uri uri, FirestoreMessage msg) {
        Toast.makeText(this, "Uploading solution...", Toast.LENGTH_SHORT).show();
        MediaManager.get().upload(uri).unsigned("ml_default").option("folder", "Solutions/" + chatId).callback(new UploadCallback() {
            @Override public void onSuccess(String requestId, Map resultData) {
                String url = (String) resultData.get("secure_url");
                Map<String, Object> updates = new HashMap<>();
                updates.put("solutionUrl", url);
                updates.put("homeworkStatus", "done");
                db.collection("chats").document(chatId).collection("messages").document(msg.documentId).update(updates)
                        .addOnSuccessListener(a -> runOnUiThread(() -> {
                            Toast.makeText(ChatActivity.this, "Solution uploaded", Toast.LENGTH_SHORT).show();
                            activeSolvingMessage = null;
                        }));
            }
            @Override public void onError(String r, ErrorInfo e) {
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Upload failed: " + e.getDescription(), Toast.LENGTH_SHORT).show());
            }
            @Override public void onStart(String r) {}
            @Override public void onProgress(String r, long b, long t) {}
            @Override public void onReschedule(String r, ErrorInfo e) {}
        }).dispatch();
    }
    @Override
    public void onViewFile(String url) {
        if (url == null) return;
        if (url.toLowerCase().contains(".pdf")) {
            PdfHelper.openPdf(this, url);
            return;
        }
        android.app.Dialog viewDialog = new android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        viewDialog.setContentView(R.layout.dialog_view_image);
        if (viewDialog.getWindow() != null) {
            viewDialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        }
        ImageView ivFull = viewDialog.findViewById(R.id.ivFullImage);
        ImageButton btnClose = viewDialog.findViewById(R.id.btnClose);
        if (ivFull != null) Glide.with(this).load(url).into(ivFull);
        if (btnClose != null) btnClose.setOnClickListener(v -> viewDialog.dismiss());
        viewDialog.show();
    }
    @Override public void onMarkRight(FirestoreMessage message) {
        if (message.documentId == null) return;
        db.collection("chats").document(chatId).collection("messages").document(message.documentId)
                .update("tutorFeedback", "correct");
    }
    @Override public void onMarkWrong(FirestoreMessage message) {
        if (message.documentId == null) return;
        db.collection("chats").document(chatId).collection("messages").document(message.documentId)
                .update("tutorFeedback", "incorrect");
    }
    @Override public void onUploadSolution(FirestoreMessage message) {
        activeSolvingMessage = message;
        solutionPickerLauncher.launch(new String[]{"image/*", "application/pdf"});
    }
    @Override public void onCouldnDoIt(FirestoreMessage message) {
        if (message.documentId == null) return;
        db.collection("chats").document(chatId).collection("messages").document(message.documentId)
                .update("homeworkStatus", "failed");
    }
    @Override public void onArchiveHomework(FirestoreMessage msg) {
        if (msg.fileUrl == null) return;
        String subject = msg.subject;
        if (subject == null || subject.isEmpty()) {
            subject = "General"; 
        }
        String title = msg.text != null ? msg.text : "Archived Homework";
        String safeTitle = title.replaceAll("[.#$\\[\\]/]", "_");
        String safeSubject = subject.replaceAll("[.#$\\[\\]/]", "_");
        ArchiveItem archiveItem = new ArchiveItem(
                msg.documentId,
                currentUserId,
                safeSubject,
                msg.fileUrl,
                safeTitle,
                System.currentTimeMillis()
        );
        DatabaseReference archiveRef = rtdbRef.child("Users").child(currentUserRole).child(currentUserId)
                .child("Archives").child(safeSubject).child(safeTitle);
        archiveRef.setValue(archiveItem)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Added to Archive!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to archive", Toast.LENGTH_SHORT).show());
    }
}