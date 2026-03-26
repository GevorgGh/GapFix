package com.example.gapfix;

import java.util.ArrayList;

public class Tutor extends User {
    private String name;
    private String bio;
    private int imageResourceId;
    private ArrayList<String> subjects;

    public Tutor(String name, String bio, int imageResourceId, ArrayList<String> subjects) {
        this.name = name;
        this.bio = bio;
        this.imageResourceId = imageResourceId;
        this.subjects = subjects;
    }

    public Tutor(){}

    public String getBio() {
        return bio;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }

    public ArrayList<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(ArrayList<String> subjects) {
        this.subjects = subjects;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
