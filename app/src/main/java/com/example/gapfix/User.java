package com.example.gapfix;

public class User {
    public String name;
    public String email;
    public String role;
    public String dob;
    public String fcmToken;


    public User() {
    }

    public User(String name, String email, String role) {
        this.name = name;
        this.email = email;
        this.role = role;
    }


    public User(String name, String email, String role, String dob) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.dob = dob;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public String getFcmToken() { return fcmToken; }

    public String imageResourceLink;
    public String getImageResourceLink() { return imageResourceLink; }
    public void setImageResourceLink(String imageResourceLink) { this.imageResourceLink = imageResourceLink; }
}