package com.example.gapfix;

public class Subject {
    public String name;
    public double price;
    public String currency;

    public Subject() {} // Needed for Firebase

    public Subject(String name, double price, String currency) {
        this.name = name;
        this.price = price;
        this.currency = currency;
    }
}