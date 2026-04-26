package com.example.gapfix;

import java.io.Serializable;
import java.util.ArrayList;

public class Tutor implements Serializable {
    private String name;
    private String bio;
    public String id; 

    private String imageResourceLink;
    private ArrayList<SubjectPreference> preferences;

    public Tutor() {} // Required for Firebase

    public static class SubjectPreference implements Serializable {
        public String name;
        public int price;
        public String currency;
        public int duration; // Duration in minutes

        public SubjectPreference() {} // Required for Firebase
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getImageResourceLink() { return imageResourceLink; }

    public void setImageResourceLink(String link) { this.imageResourceLink = link; }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<SubjectPreference> getPreferences() { return preferences; }
    public void setPreferences(ArrayList<SubjectPreference> preferences) { this.preferences = preferences; }
}