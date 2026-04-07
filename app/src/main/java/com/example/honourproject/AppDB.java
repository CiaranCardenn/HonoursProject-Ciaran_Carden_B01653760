package com.example.honourproject;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

//Summary of class
//Create a room DB to store notes and users.

//Defines data base with 2 tables, note and user
@Database(entities = {Note.class, User.class}, version = 3, exportSchema = false)
public abstract class AppDB extends RoomDatabase {

    //AAccess the data in each table
    public abstract NoteDao noteDao();
    public abstract UserDao userDao();

    //Copy of the DB
    private static AppDB instance;

    //get the db
    public static synchronized AppDB getInstance(Context context){
        if(instance == null){
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDB.class,
                            "notes_database") //db file name
                    .fallbackToDestructiveMigration() //delete old data if db structure changes
                    .build();
        }
        return instance;
    }
}