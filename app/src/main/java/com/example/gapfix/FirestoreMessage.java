package com.example.gapfix;
import com.google.firebase.Timestamp;
import java.util.Objects;
public class FirestoreMessage {
    public String senderId;
    public String receiverId; 
    public String text;
    public Timestamp timestamp;
    public String type; 
    public String fileUrl;
    public String documentId;
    public String homeworkStatus; 
    public long lessonTimestamp; 
    public String subject; 
    public String solutionUrl; 
    public String tutorFeedback; 
    public String chatId; 
    public FirestoreMessage() {} 
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FirestoreMessage that = (FirestoreMessage) o;
        return Objects.equals(senderId, that.senderId) &&
               Objects.equals(text, that.text) &&
               Objects.equals(timestamp, that.timestamp);
    }
    @Override
    public int hashCode() {
        return Objects.hash(senderId, text, timestamp);
    }
}
