package com.example.gapfix;

import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;

@IgnoreExtraProperties
public class Booking implements Serializable {
    private String bookingId;
    private String studentId;
    private String tutorId;
    private String lessonDate;
    private String lessonTime;
    private long timestamp; 
    private String subject;
    private int duration; 
    private String status; 
    private String tutorName;
    private String tutorImage;
    private boolean isFree; 
    private boolean isPackage; 
    private String packageId; 
    private int packageTotalLessons; 
    private String cancellationReason;
    private String suggestedSourceDay;
    private String suggestedDestDay;
    private String suggestedTime;
    private String suggestionMessage;
    private long suggestedTimestamp; 
    private double price;

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

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTutorName() { return tutorName; }
    public void setTutorName(String tutorName) { this.tutorName = tutorName; }

    public String getTutorImage() { return tutorImage; }
    public void setTutorImage(String tutorImage) { this.tutorImage = tutorImage; }

    public boolean isFree() { return isFree; }
    public void setFree(boolean free) { isFree = free; }

    public boolean isPackage() { return isPackage; }
    public void setPackage(boolean aPackage) { isPackage = aPackage; }

    public String getPackageId() { return packageId; }
    public void setPackageId(String packageId) { this.packageId = packageId; }

    public int getPackageTotalLessons() { return packageTotalLessons; }
    public void setPackageTotalLessons(int packageTotalLessons) { this.packageTotalLessons = packageTotalLessons; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public String getSuggestedSourceDay() { return suggestedSourceDay; }
    public void setSuggestedSourceDay(String suggestedSourceDay) { this.suggestedSourceDay = suggestedSourceDay; }

    public String getSuggestedDestDay() { return suggestedDestDay; }
    public void setSuggestedDestDay(String suggestedDestDay) { this.suggestedDestDay = suggestedDestDay; }

    public String getSuggestedTime() { return suggestedTime; }
    public void setSuggestedTime(String suggestedTime) { this.suggestedTime = suggestedTime; }

    public String getSuggestionMessage() { return suggestionMessage; }
    public void setSuggestionMessage(String suggestionMessage) { this.suggestionMessage = suggestionMessage; }

    public long getSuggestedTimestamp() { return suggestedTimestamp; }
    public void setSuggestedTimestamp(long suggestedTimestamp) { this.suggestedTimestamp = suggestedTimestamp; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
