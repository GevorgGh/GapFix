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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private String otherUserId;
    private String otherUserName;
    private String currentUserId;
    private String currentUserName = "Me";
    private String chatId;

    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<FirestoreMessage> messageList = new ArrayList<>();
    private EditText etMessage;
    private ImageButton btnSend, btnBack, btnCall;
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
        fetchNamesFromRTDB();
        listenForMessages();
    }

    private void initUI() {
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        btnCall = findViewById(R.id.btnCall);
        tvChatName = findViewById(R.id.tvChatName);

        tvChatName.setText(otherUserName != null ? otherUserName : "Chat");

        adapter = new MessageAdapter(messageList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
        btnCall.setOnClickListener(v -> {
            Intent intent = new Intent(this, VideoCallActivity.class);
            // Changed: Use chatId instead of otherUserId so both participants join the same channel
            intent.putExtra("TARGET_USER_ID", chatId);
            startActivity(intent);
        });
    }

    private void fetchNamesFromRTDB() {
        // 1. Fetch current user's name
        fetchNameForId(currentUserId, name -> currentUserName = name);
        
        // 2. Fetch other user's name (if missing)
        if (otherUserName == null || otherUserName.isEmpty()) {
            fetchNameForId(otherUserId, name -> {
                otherUserName = name;
                tvChatName.setText(otherUserName);
            });
        }
    }

    private void fetchNameForId(String uid, NameCallback callback) {
        // Check Students first
        FirebaseDatabase.getInstance().getReference("Users").child("Student").child(uid).child("name")
                .get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        callback.onNameFetched(snapshot.getValue(String.class));
                    } else {
                        // Then check Tutors
                        FirebaseDatabase.getInstance().getReference("Users").child("Tutor").child(uid).child("name")
                                .get().addOnSuccessListener(tutorSnap -> {
                                    if (tutorSnap.exists()) {
                                        callback.onNameFetched(tutorSnap.getValue(String.class));
                                    }
                                });
                    }
                });
    }

    interface NameCallback {
        void onNameFetched(String name);
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
                                if (!messageList.contains(msg)) {
                                    messageList.add(msg);
                                    adapter.notifyItemInserted(messageList.size() - 1);
                                    rvMessages.scrollToPosition(messageList.size() - 1);
                                }
                            }
                        }
                    }
                });
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;

        etMessage.setText("");

        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("receiverId", otherUserId); // Required for Cloud Function notifications
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

        db.collection("chats").document(chatId).set(chatMeta, com.google.firebase.firestore.SetOptions.merge());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) messageListener.remove();
    }
}
