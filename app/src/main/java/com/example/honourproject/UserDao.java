package com.example.honourproject;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

//Class Summary
//Provides all SQL querys for users

//Data access object, handles all DB operations for users
@Dao
public interface UserDao {
    //add new user to db
    @Insert
    long insert(User user);

    //find and return user by their username
    @Query("SELECT * FROM users WHERE username = :username")
    User getUserByUsername(String username);

    //check if a username is already existing
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE username = :username)")
    boolean userExists(String username);

    //get total number of users
    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();
}