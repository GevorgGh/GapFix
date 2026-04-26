package com.example.gapfix;

public class Subject {
    public String name;
    public double price;
    public String currency;
    public int duration; // Duration in minutes

    public Subject() {} // Needed for Firebase

    public Subject(String name, double price, String currency, int duration) {
        this.name = name;
        this.price = price;
        this.currency = currency;
        this.duration = duration;
    }
}