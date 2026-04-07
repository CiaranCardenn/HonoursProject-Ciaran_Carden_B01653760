package com.example.honourproject;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

//Class Summary
//Checks who is currently logged in
//Saves user session info

//Manages user sessions
public class UserManager {
    //keys for saving user data
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_CURRENT_USER_TYPE = "current_user_type";
    private static final String KEY_CURRENT_USERNAME = "current_username";

    private SharedPreferences prefs;
    private Context context;  // accesses app preferences
    private FirebaseAuth firebaseAuth; //for google users
    private AppDB database; //for local users

    public UserManager(Context context) {
        this.context = context;  // Store context
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.database = AppDB.getInstance(context);
    }

    //get ID for logged in user
    public String getCurrentUserId() {
        // Check if Google user is logged in
        FirebaseUser googleUser = firebaseAuth.getCurrentUser();
        if (googleUser != null) {
            return "google_" + googleUser.getUid();
        }

        // Check if local user is logged in
        String localUsername = prefs.getString(KEY_CURRENT_USERNAME, null);
        if (localUsername != null && !localUsername.isEmpty()) {
            return "local_" + localUsername;
        }

        return null; //no user logged in
    }

    //get display name of user
    public String getCurrentUsername() {
        FirebaseUser googleUser = firebaseAuth.getCurrentUser();
        if (googleUser != null) {
            String name = googleUser.getDisplayName();
            String email = googleUser.getEmail();
            return name != null && !name.isEmpty() ? name : email; //uses name otherwise uses email
        }
        return prefs.getString(KEY_CURRENT_USERNAME, null); //local user
    }

    // alternative method for compatibility
    public String getUserName() {
        return getCurrentUsername();
    }

    //gets user email
    public String getUserEmail() {
        FirebaseUser googleUser = firebaseAuth.getCurrentUser();
        if (googleUser != null) {
            return googleUser.getEmail(); //users google email
        }
        // For local users, return username@local.user
        String username = prefs.getString(KEY_CURRENT_USERNAME, null);
        if (username != null) {
            return username + "@local.user";
        }
        return null;
    }

    //save local user after login
    public void setCurrentLocalUser(String username) {
        prefs.edit()
                .putString(KEY_CURRENT_USERNAME, username)
                .putString(KEY_CURRENT_USER_TYPE, "local")
                .apply();
    }

    //save google user after login
    public void setCurrentGoogleUser(FirebaseUser user) {
        prefs.edit()
                .putString(KEY_CURRENT_USER_TYPE, "google")
                .remove(KEY_CURRENT_USERNAME)
                .apply();
    }

    //removes current user from saved preferences
    public void clearCurrentUser() {
        prefs.edit()
                .remove(KEY_CURRENT_USER_ID)
                .remove(KEY_CURRENT_USER_TYPE)
                .remove(KEY_CURRENT_USERNAME)
                .apply();
    }

    //logouts user
    public void logout() {
        // Clear user preferences - use stored context
        SharedPreferences appPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        appPrefs.edit().clear().apply();  // Clear ALL preferences

        // Sign out from Google if logged in
        if (firebaseAuth.getCurrentUser() != null) {
            firebaseAuth.signOut();
        }

        // Clear current user
        clearCurrentUser();
    }


    //check if any user is logged in
    public boolean isUserLoggedIn() {
        return getCurrentUserId() != null;
    }

    //gets user type, google or local
    public String getUserType() {
        FirebaseUser googleUser = firebaseAuth.getCurrentUser();
        if (googleUser != null) {
            return "google";
        }
        return prefs.getString(KEY_CURRENT_USER_TYPE, null);
    }

    //creates new local user account in background
    public boolean createLocalUser(String username, String password) {
        // Check if username already exists
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean exists = database.userDao().userExists(username);
                if (!exists) {
                    // Create new user
                    User newUser = new User(username, password, username + "@local.user");
                    database.userDao().insert(newUser);
                }
            }
        }).start();

        return true;
    }

    //checks if local user credentials are correct
    public boolean verifyLocalUser(String username, String password) {
        User user = database.userDao().getUserByUsername(username);
        return user != null && user.passwordHash.equals(password);
    }

    //checks if username is taken
    public boolean userExists(String username) {
        return database.userDao().userExists(username);
    }
}