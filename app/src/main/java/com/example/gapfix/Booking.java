package com.example.gapfix;

import java.io.Serializable;

public class Booking implements Serializable {
    private String bookingId;
    private String studentId;
    private String tutorId;
    private String lessonDate;
    private String lessonTime;
    private long timestamp; // UTC Timestamp in milliseconds
    private String subject;
    private String status; // pending, confirmed, completed, cancelled, free_trial_pending, finished
    private String tutorName;
    private String tutorImage;
    private boolean isFree; // Track if this is a free lesson
    private String cancellationReason; // New field for reason

    public Booking() {}

    public Booking(String bookingId, String studentId, String tutorId,
                   String date, String time, String subject, long timestamp) {
        this.bookingId = bookingId;
        this.studentId = studentId;
        this.tutorId = tutorId;
        this.lessonDate = date;
        this.lessonTime = time;
        this.subject = subject;
        this.timestamp = timestamp;
        this.status = "pending";
    }

    // Getters and Setters
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getTutorId() { return tutorId; }
    public void setTutorId(String tutorId) { this.tutorId = tutorId; }

    public String getLessonDate() { return lessonDate; }
    public void setLessonDate(String lessonDate) { this.lessonDate = lessonDate; }

    public String getLessonTime() { return lessonTime; }
    public void setLessonTime(String lessonTime) { this.lessonTime = lessonTime; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTutorName() { return tutorName; }
    public void setTutorName(String tutorName) { this.tutorName = tutorName; }

    public String getTutorImage() { return tutorImage; }
    public void setTutorImage(String tutorImage) { this.tutorImage = tutorImage; }

    public boolean isFree() { return isFree; }
    public void setFree(boolean free) { isFree = free; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
}