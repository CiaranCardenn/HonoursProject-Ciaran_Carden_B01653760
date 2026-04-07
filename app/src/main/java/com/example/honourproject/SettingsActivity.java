package com.example.honourproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

//Class Summary
//Shows app settigs
//Lets user enable disguise mode
//Show what user is logged in
//Signout

//Settings screen
public class SettingsActivity extends AppCompatActivity {

    //UI elements
    private Switch disguiseSwitch; //enable/dispable disguise mode
    private Spinner disguiseSpinner; //select which app for disguise mode
    private SharedPreferences prefs; //saves user preferences
    private Button signOutButton; //logout button
    private TextView currentUserInfo; //shows who is logged in

    //Managers and auth
    private UserManager userManager;
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize UserManager
        userManager = new UserManager(this);
        firebaseAuth = FirebaseAuth.getInstance();

        // Setup Google Sign-In client
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); //shows back arrow
            getSupportActionBar().setTitle("Settings");
        }

        // Initialize views connect to UI elements
        disguiseSwitch = findViewById(R.id.disguise_switch);
        disguiseSpinner = findViewById(R.id.disguise_spinner);
        signOutButton = findViewById(R.id.sign_out_button);
        currentUserInfo = findViewById(R.id.current_user_info);

        // Get preferences
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        // Display current user info
        displayUserInfo();

        // Setup disguise switch
        boolean isDisguised = prefs.getBoolean("is_disguised", false);
        disguiseSwitch.setChecked(isDisguised);

        disguiseSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("is_disguised", isChecked).apply();
            disguiseSpinner.setEnabled(isChecked);

            String status = isChecked ? "enabled" : "disabled";
            Toast.makeText(this, "Disguise mode " + status + ". Restart app to see changes.", Toast.LENGTH_SHORT).show();
        });

        // Setup disguise dropdown
        String[] disguiseApps = {"Calculator", "Weather", "Todo List", "Expense Tracker"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, disguiseApps);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        disguiseSpinner.setAdapter(adapter);

        // Load saved disguise type
        String currentDisguise = prefs.getString("disguise_type", "Calculator");
        for (int i = 0; i < disguiseApps.length; i++) {
            if (disguiseApps[i].equals(currentDisguise)) {
                disguiseSpinner.setSelection(i);
                break;
            }
        }

        // Save disguise selection
        disguiseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = disguiseApps[position];
                prefs.edit().putString("disguise_type", selected).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Set disguise enabled state based on switch
        disguiseSpinner.setEnabled(isDisguised);

        // Setup Sign Out button
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOutUser();
            }
        });
    }

    //display current user info
    private void displayUserInfo() {
        if (userManager.isUserLoggedIn()) {
            String username = userManager.getCurrentUsername();
            String userType = userManager.getUserType();

            String displayText;
            if ("google".equals(userType)) {
                displayText = "Logged in with Google\n" + username;
            } else {
                displayText = "Logged in as: " + username;
            }
            currentUserInfo.setText(displayText);
        } else {
            currentUserInfo.setText("Not logged in");
        }
    }

    //shows confirmation for signing out
    private void signOutUser() {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out? Your notes will remain saved.")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    performSignOut();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    //signs out user
    private void performSignOut() {
        // Clear ALL user preferences including disguise mode
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();  // Clear all preferences
        editor.apply();

        // Sign out from Firebase (Google)
        if (firebaseAuth.getCurrentUser() != null) {
            firebaseAuth.signOut();
        }

        // Sign out from Google client
        googleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                // User manager logout
                userManager.logout();

                Toast.makeText(SettingsActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();

                // go back to login screen
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }


    //handles back button
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}