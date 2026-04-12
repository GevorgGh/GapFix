package com.example.gapfix;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.List;

@IgnoreExtraProperties
public class FirestoreConversation {
    public List<String> participants;
    public String lastMessage;
    public Timestamp lastMessageTime;
    
    // We handle participantNames manually in the Fragment to avoid deserialization crashes
    public String otherUserId;         
    public String otherUserName;       

    public FirestoreConversation() {}
}
