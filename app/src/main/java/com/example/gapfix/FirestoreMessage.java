package com.example.gapfix;

import com.google.firebase.Timestamp;
import java.util.Objects;

public class FirestoreMessage {
    public String senderId;
    public String text;
    public Timestamp timestamp;

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
