package com.example.gapfix;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.agora.chat.ChatClient;
import io.agora.chat.ChatMessage;
import io.agora.chat.Conversation;
import io.agora.chat.GroupReadAck;
import io.agora.MessageListener;
import io.agora.CallBack;

public class ChatActivity extends AppCompatActivity implements MessageListener {

    private String toChatUsername;
    private String toChatNickname;
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<ChatMessage> messageList = new ArrayList<>();
    private EditText etMessage;
    private ImageButton btnSend, btnBack, btnCall;
    private TextView tvChatName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        toChatUsername = getIntent().getStringExtra("CHAT_USER_ID");
        toChatNickname = getIntent().getStringExtra("CHAT_USER_NAME");

        if (TextUtils.isEmpty(toChatUsername)) {
            Toast.makeText(this, "User ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initUI();
        loadMessages();

        ChatClient.getInstance().chatManager().addMessageListener(this);
    }

    private void initUI() {
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        btnCall = findViewById(R.id.btnCall);
        tvChatName = findViewById(R.id.tvChatName);

        tvChatName.setText(toChatNickname != null ? toChatNickname : toChatUsername);

        adapter = new MessageAdapter(messageList);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());

        btnCall.setOnClickListener(v -> {
            Intent intent = new Intent(this, VideoCallActivity.class);
            intent.putExtra("TARGET_USER_ID", toChatUsername);
            startActivity(intent);
        });
    }

    private void loadMessages() {
        Conversation conversation = ChatClient.getInstance().chatManager().getConversation(toChatUsername);
        if (conversation != null) {
            messageList.clear();
            List<ChatMessage> messages = conversation.getAllMessages();
            messageList.addAll(messages);
            adapter.notifyDataSetChanged();
            rvMessages.scrollToPosition(messageList.size() - 1);
        }
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content)) return;

        ChatMessage message = ChatMessage.createTextSendMessage(content, toChatUsername);

        message.setMessageStatusCallback(new CallBack() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    etMessage.setText("");
                    messageList.add(message);
                    adapter.notifyItemInserted(messageList.size() - 1);
                    rvMessages.scrollToPosition(messageList.size() - 1);
                });
            }

            @Override
            public void onError(int code, String error) {
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Send failed: " + error, Toast.LENGTH_SHORT).show());
            }
        });

        ChatClient.getInstance().chatManager().sendMessage(message);
    }

    @Override
    public void onMessageReceived(List<ChatMessage> messages) {
        for (ChatMessage message : messages) {
            if (message.getFrom().equals(toChatUsername)) {
                runOnUiThread(() -> {
                    messageList.add(message);
                    adapter.notifyItemInserted(messageList.size() - 1);
                    rvMessages.scrollToPosition(messageList.size() - 1);
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ChatClient.getInstance().chatManager().removeMessageListener(this);
    }

    @Override public void onCmdMessageReceived(List<ChatMessage> messages) {}
    @Override public void onMessageRead(List<ChatMessage> messages) {}
    @Override public void onMessageDelivered(List<ChatMessage> messages) {}
    @Override public void onMessageRecalled(List<ChatMessage> messages) {}
    @Override public void onGroupMessageRead(List<GroupReadAck> groupReadAcks) {}
}
