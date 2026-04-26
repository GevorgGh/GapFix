package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private String otherUserId;
    private String otherUserName;
    private String currentUserId;
    private String currentUserName = "Me";
    private String currentUserImage = null;
    private String otherUserImage = null;
    private String chatId;

    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<FirestoreMessage> messageList = new ArrayList<>();
    private EditText etMessage;
    private ImageButton btnSend, btnBack;
    private TextView tvChatName;

    private FirebaseFirestore db;
    private ListenerRegistration messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        otherUserId = getIntent().getStringExtra("CHAT_USER_ID");
        otherUserName = getIntent().getStringExtra("CHAT_USER_NAME");
        currentUserId = FirebaseAuth.getInstance().getUid();

        if (TextUtils.isEmpty(otherUserId) || currentUserId == null) {
            Toast.makeText(this, "User ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        List<String> ids = Arrays.asList(currentUserId, otherUserId);
        java.util.Collections.sort(ids);
        chatId = ids.get(0) + "_" + ids.get(1);

        db = FirebaseFirestore.getInstance("gapfix");

        initUI();
        fetchDetailsFromRTDB();
        listenForMessages();
    }

    private void initUI() {
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        tvChatName = findViewById(R.id.tvChatName);

        tvChatName.setText(otherUserName != null ? otherUserName : "Chat");

        adapter = new MessageAdapter(messageList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void fetchDetailsFromRTDB() {
        fetchUserForId(currentUserId, (name, image) -> {
            if (name != null) currentUserName = name;
            currentUserImage = image;
        });
        
        fetchUserForId(otherUserId, (name, image) -> {
            if (name != null) {
                otherUserName = name;
                tvChatName.setText(otherUserName);
            }
            otherUserImage = image;
        });
    }

    private void fetchUserForId(String uid, UserCallback callback) {
        FirebaseDatabase.getInstance().getReference("Users").child("Student").child(uid)
                .get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) callback.onUserFetched(user.getName(), user.getImageResourceLink());
                    } else {
                        FirebaseDatabase.getInstance().getReference("Users").child("Tutor").child(uid)
                                .get().addOnSuccessListener(tutorSnap -> {
                                    if (tutorSnap.exists()) {
                                        User tutor = tutorSnap.getValue(User.class);
                                        if (tutor != null) callback.onUserFetched(tutor.getName(), tutor.getImageResourceLink());
                                    }
                                });
                    }
                });
    }

    interface UserCallback {
        void onUserFetched(String name, String image);
    }

    private void listenForMessages() {
        messageListener = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null) {
                        for (DocumentChange change : snapshots.getDocumentChanges()) {
                            if (change.getType() == DocumentChange.Type.ADDED) {
                                FirestoreMessage msg = change.getDocument().toObject(FirestoreMessage.class);
                                addMessageWithDateHeader(msg);
                            }
                        }
                    }
                });
    }

    private void addMessageWithDateHeader(FirestoreMessage msg) {
        if (msg.timestamp == null) {
            messageList.add(msg);
            adapter.notifyItemInserted(messageList.size() - 1);
            rvMessages.scrollToPosition(messageList.size() - 1);
            return;
        }

        String dateStr = MessageAdapter.getFormattedDate(msg.timestamp.toDate().getTime());
        
        boolean needsHeader = true;
        for (int i = messageList.size() - 1; i >= 0; i--) {
            FirestoreMessage existing = messageList.get(i);
            if (existing.senderId.equals("DATE_HEADER")) {
                if (existing.text.equals(dateStr)) {
                    needsHeader = false;
                }
                break; 
            }
        }

        if (needsHeader) {
            FirestoreMessage header = new FirestoreMessage();
            header.senderId = "DATE_HEADER";
            header.text = dateStr;
            header.timestamp = new Timestamp(new java.util.Date(msg.timestamp.toDate().getTime() - 1));
            messageList.add(header);
            adapter.notifyItemInserted(messageList.size() - 1);
        }

        if (!messageList.contains(msg)) {
            messageList.add(msg);
            adapter.notifyItemInserted(messageList.size() - 1);
            rvMessages.scrollToPosition(messageList.size() - 1);
        }
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;

        etMessage.setText("");

        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("receiverId", otherUserId);
        message.put("text", content);
        message.put("timestamp", FieldValue.serverTimestamp());

        db.collection("chats").document(chatId).collection("messages").add(message);

        Map<String, Object> chatMeta = new HashMap<>();
        chatMeta.put("participants", Arrays.asList(currentUserId, otherUserId));
        chatMeta.put("lastMessage", content);
        chatMeta.put("lastMessageTime", FieldValue.serverTimestamp());
        
        Map<String, String> namesMap = new HashMap<>();
        namesMap.put(currentUserId, currentUserName);
        namesMap.put(otherUserId, otherUserName);
        chatMeta.put("participantNames", namesMap);

        Map<String, String> imagesMap = new HashMap<>();
        if (currentUserImage != null) imagesMap.put(currentUserId, currentUserImage);
        if (otherUserImage != null) imagesMap.put(otherUserId, otherUserImage);
        if (!imagesMap.isEmpty()) chatMeta.put("participantImages", imagesMap);

        db.collection("chats").document(chatId).set(chatMeta, com.google.firebase.firestore.SetOptions.merge());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) messageListener.remove();
    }
}
