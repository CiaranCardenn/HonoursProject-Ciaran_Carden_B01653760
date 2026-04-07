package com.example.honourproject;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

//Class Summary
//Lets user login with google

//Handles google sign in and firebase auth
public class GoogleSignInHelper {

    private static final String TAG = "GoogleSignInHelper"; //used for debugging
    private static final int RC_SIGN_IN = 9001; //identifies google sign-in result

    private Activity activity; //current screen
    private GoogleSignInClient googleSignInClient; //handles google login
    private FirebaseAuth firebaseAuth; //handles firebase auth
    private GoogleSignInCallback callback; //tell the app what happened

    //interface for getting success/failure results
    public interface GoogleSignInCallback {
        void onSuccess(FirebaseUser user); //login works
        void onFailure(String errorMessage); //login failed
    }

    public GoogleSignInHelper(Activity activity, GoogleSignInCallback callback) {
        this.activity = activity;
        this.callback = callback;

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail() //ask for users email
                .build();

        googleSignInClient = GoogleSignIn.getClient(activity, gso);
        firebaseAuth = FirebaseAuth.getInstance(); //gets firebase auth instance
    }

    //open google login screen
    public void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    //handle result after login with google
    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) { //check if google login requiest
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class); //get google account
                firebaseAuthWithGoogle(account.getIdToken()); //authenticate with firebase
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                callback.onFailure("Google sign in failed: " + e.getMessage());
            }
        }
    }

    //exchange google token for firebase auth
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = firebaseAuth.getCurrentUser(); //get firebase user
                        callback.onSuccess(user); //tell app login worked
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        callback.onFailure("Authentication failed: " + task.getException().getMessage());
                    }
                });
    }

    //sign out from google and firebase
    public void signOut() {
        firebaseAuth.signOut(); //signout of firebase
        googleSignInClient.signOut().addOnCompleteListener(activity, task -> {
            Log.d(TAG, "User signed out");
        });
    }

    //gets current logged in user
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    //checks if someone is logged in
    public boolean isUserSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }
}