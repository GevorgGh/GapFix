package com.example.gapfix;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.List;

@IgnoreExtraProperties
public class FirestoreConversation {
    public List<String> participants;
    public String lastMessage;
    public String lastMessageType;
    public Timestamp lastMessageTime;
    public String otherUserId;         
    public String otherUserName;
    public String otherUserImage;
    public java.util.Map<String, Long> unreadCount;
    public String chatId;
    public FirestoreConversation() {}
}
