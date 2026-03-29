package com.example.gapfix;

import java.io.Serializable;

public class Booking implements Serializable {
    private String bookingId;
    private String studentId;
    private String tutorId;
    private String lessonDate;
    private String lessonTime;
    private String subject;
    private String status; // pending, confirmed, completed, cancelled
    private String tutorName;
    private String tutorImage;
    public Booking() {}

    public Booking(String bookingId, String studentId, String tutorId,
                   String date, String time, String subject) {
        this.bookingId = bookingId;
        this.studentId = studentId;
        this.tutorId = tutorId;
        this.lessonDate = date;
        this.lessonTime = time;
        this.subject = subject;
        this.status = "pending";
    }

    public String getBookingId() { return bookingId; }
    public String getStudentId() { return studentId; }
    public String getTutorId() { return tutorId; }
    public String getLessonDate() { return lessonDate; }
    public String getLessonTime() { return lessonTime; }
    public String getSubject() { return subject; }
    public String getStatus() { return status; }
    public String getTutorName() { return tutorName; }
    public String getTutorImage() { return tutorImage; }

    public void setBookingId(String bookingId) { this.bookingId = bookingId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setTutorId(String tutorId) { this.tutorId = tutorId; }
    public void setLessonDate(String lessonDate) { this.lessonDate = lessonDate; }
    public void setLessonTime(String lessonTime) { this.lessonTime = lessonTime; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setStatus(String status) { this.status = status; }
    public void setTutorName(String tutorName) { this.tutorName = tutorName; }
    public void setTutorImage(String tutorImage) { this.tutorImage = tutorImage; }
}