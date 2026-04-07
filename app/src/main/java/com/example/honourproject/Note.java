package com.example.honourproject;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

//Class Summary
//Defines note attributes

//Defines note table in db
@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true) //autp incrementing id for each note
    public int id; //unique identiier for each note

    // User ID to associate note with specific user
    public String userId;

    public String noteTitle; //note title
    public String noteContent; //content of note
    public long noteCreated; //timestamp when note is created
    public long noteDelete; //timestamp when note should self-destruct if enabled
    public boolean isEncrypted; //checks if note is encrypted or note

    public Note() {} //empty constructor for db
}