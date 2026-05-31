package com.example.gapfix;
public class SubjectArchiveModel {
    private String subjectName;
    private int totalFiles;
    private int reviewedFiles;
    public SubjectArchiveModel(String subjectName, int totalFiles, int reviewedFiles) {
        this.subjectName = subjectName;
        this.totalFiles = totalFiles;
        this.reviewedFiles = reviewedFiles;
    }
    public String getSubjectName() { return subjectName; }
    public int getTotalFiles() { return totalFiles; }
    public int getReviewedFiles() { return reviewedFiles; }
    public int getProgress() {
        if (totalFiles == 0) return 0;
        return (int) (((float) reviewedFiles / totalFiles) * 100);
    }
}