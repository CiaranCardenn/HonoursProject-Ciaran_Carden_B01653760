package com.example.honourproject;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

//Class Summary
//Provides all SQL querys for notes

//Data access object to handle all db operations for notes
@Dao
public interface NoteDao {
    //insert a new note and return ID
    @Insert
    long insert(Note note);

    //update an existing note
    @Update
    void update(Note note);

    //delete a note
    @Delete
    void delete(Note note);

    // Get notes for specific user and sort newest to oldest
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY noteCreated DESC")
    List<Note> getNotesForUser(String userId);

    // Same as above but auto updates when data changes
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY noteCreated DESC")
    LiveData<List<Note>> getNotesForUserLive(String userId);

    // Get notes to delete for specific user
    @Query("SELECT * FROM notes WHERE userId = :userId AND noteDelete > 0 AND noteDelete <= :currentTime")
    List<Note> getNotesToDeleteForUser(String userId, long currentTime);

    // Get all expired notes across all users (for cleanup worker)
    @Query("SELECT * FROM notes WHERE noteDelete > 0 AND noteDelete <= :currentTime")
    List<Note> getNotesToDelete(long currentTime);

    //get all notes for all users sorted new to old
    @Query("SELECT * FROM notes ORDER BY noteCreated DESC")
    List<Note> getAllNotes();

    //same as above but auto updates
    @Query("SELECT * FROM notes ORDER BY noteCreated DESC")
    LiveData<List<Note>> getAllNotesLive();
}