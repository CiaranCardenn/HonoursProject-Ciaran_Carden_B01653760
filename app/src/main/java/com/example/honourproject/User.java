package com.example.honourproject;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

//Class Summary
//Defines user attributes

//Define users table in DB
@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true) //auto incrementing ID
    public int id; //unique identifier for each user

    public String username;      // Unique username
    public String passwordHash;  // Stored encrypted password
    public String email;         // Optional email
    public long createdAt;       // When account was created

    //empty constructor for DB
    public User() {}

    //Constructor to create a new user
    public User(String username, String passwordHash, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.createdAt = System.currentTimeMillis();
    }
}