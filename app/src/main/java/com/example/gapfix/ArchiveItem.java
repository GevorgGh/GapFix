package com.example.gapfix;

public class ArchiveItem {
    public String documentId;
    public String studentId;
    public String subject;
    public String fileUrl;
    public String fileName; 
    public long timestamp;
    public boolean reviewed;

    public ArchiveItem() {
        
    }

    public ArchiveItem(String documentId, String studentId, String subject, String fileUrl, String fileName, long timestamp) {
        this.documentId = documentId;
        this.studentId = studentId;
        this.subject = subject;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.timestamp = timestamp;
        this.reviewed = false;
    }
}
