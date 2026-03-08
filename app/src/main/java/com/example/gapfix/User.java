package com.example.gapfix;

public class User {
    public String name;
    public String email;
    public String role;
    public String dob; // Date of Birth

    // Default constructor required for Firebase calls to DataSnapshot.getValue(User.class)
    public User() {
    }

    public User(String name, String email, String role) {
        this.name = name;
        this.email = email;
        this.role = role;
    }

    // Constructor including DOB if you want to create it all at once
    public User(String name, String email, String role, String dob) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.dob = dob;
    }

    // Getter and Setter for DOB
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
}