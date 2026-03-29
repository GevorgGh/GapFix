package com.example.gapfix;

import java.io.Serializable;
import java.util.ArrayList;

public class Tutor extends User implements Serializable {
    private String name;
    private String bio;
    private String imageResourceLink;
    private ArrayList<String> subjects;
    private int minPrice;
    private int maxPrice;

    private String id;


    public Tutor(String name, String bio, String imageResourceId, ArrayList<String> subjects, int minPrice, int maxPrice, String id) {
        this.name = name;
        this.bio = bio;
        this.imageResourceLink = imageResourceId;
        this.subjects = subjects;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.id = id;
    }

    public Tutor(){}

    public String getBio() {
        return bio;
    }

    public String getImageResourceLink() {
        return imageResourceLink;
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

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setImageResourceLink(String imageResourceLink) {
        this.imageResourceLink = imageResourceLink;
    }

    public int getMinPrice() {
        return minPrice;
    }

    public int getMaxPrice() {
        return maxPrice;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
