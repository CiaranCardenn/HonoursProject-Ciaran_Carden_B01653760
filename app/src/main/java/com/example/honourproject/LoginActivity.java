package com.example.honourproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

//Class Summary
//Shows login screen
//Lets user create an account
//Optional disguise mode

//Login screen, normal login, google login and disguise mode
public class LoginActivity extends AppCompatActivity {

    //UI Elements
    private EditText usernameInput;
    private EditText passwordInput;
    private Button loginButton;
    private Button registerButton;
    private SignInButton googleSignInButton;
    private TextView titleText;

    //Managers and Helpers
    private EncryptionManager encryptionManager;
    private SharedPreferences prefs;
    private boolean isDisguised;
    private String disguiseType;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private UserManager userManager;
    private AppDB database;
    private static final int RC_SIGN_IN = 9001; //google signin request code

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize managers
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        encryptionManager = new EncryptionManager(this);
        auth = FirebaseAuth.getInstance();
        userManager = new UserManager(this);
        database = AppDB.getInstance(this);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Check if user is already logged in
        if (userManager.isUserLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Check if disguised mode is on
        isDisguised = prefs.getBoolean("is_disguised", false);
        disguiseType = prefs.getString("disguise_type", "Calculator");

        if (isDisguised) {
            showDisguiseScreen(); //show fake app screen
        } else {
            showLoginScreen(); //show normal login screen
        }
    }

    //shows normal login screen
    private void showLoginScreen() {
        setContentView(R.layout.activity_login);

        //connect to UI elements
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_btn);
        registerButton = findViewById(R.id.register_btn);
        googleSignInButton = findViewById(R.id.google_sign_in_button);
        titleText = findViewById(R.id.app_title);

        // Set normal login appearance
        titleText.setText("Secret Notes");
        usernameInput.setHint("Username");
        usernameInput.setVisibility(View.VISIBLE);
        passwordInput.setHint("Password");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        loginButton.setText("Login");
        registerButton.setText("Create Account");
        registerButton.setVisibility(View.VISIBLE);

        // Login button click listener
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check user credentials in background thread
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        boolean userExists = database.userDao().userExists(username);

                        if (userExists) {
                            User user = database.userDao().getUserByUsername(username);
                            if (user.passwordHash.equals(password)) { //password correct
                                // Login successful
                                userManager.setCurrentLocalUser(username);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(LoginActivity.this, "Welcome " + username + "!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                        finish();
                                    }
                                });
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(LoginActivity.this, "Invalid password", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "User not found. Please create an account.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        // Register button
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();


                //validates inputs
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (username.length() < 3) {
                    Toast.makeText(LoginActivity.this, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 4) {
                    Toast.makeText(LoginActivity.this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                //create new account in background
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        boolean userExists = database.userDao().userExists(username);

                        if (userExists) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "Username already exists. Please choose another.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            // Create new user
                            User newUser = new User(username, password, username + "@local.user");
                            database.userDao().insert(newUser);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "Account created successfully! Please login.", Toast.LENGTH_SHORT).show();
                                    usernameInput.setText("");
                                    passwordInput.setText("");
                                }
                            });
                        }
                    }
                }).start();
            }
        });

        // Setup Google Sign-In button
        if (googleSignInButton != null) {
            googleSignInButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    signInWithGoogle();
                }
            });
            googleSignInButton.setVisibility(View.VISIBLE);
        }
    }

    //start google login
    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    //handle google login request
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    //authenticate with firebase
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Set Google user
                            userManager.setCurrentGoogleUser(auth.getCurrentUser());
                            Toast.makeText(LoginActivity.this, "Welcome " + userManager.getCurrentUsername() + "!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    //shows fake app screen/disguise mode
    private void showDisguiseScreen() {
        setContentView(R.layout.activity_login);

        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_btn);
        registerButton = findViewById(R.id.register_btn);
        googleSignInButton = findViewById(R.id.google_sign_in_button);
        titleText = findViewById(R.id.app_title);

        // Hide Google and register buttons in disguise mode
        if (googleSignInButton != null) {
            googleSignInButton.setVisibility(View.GONE);
        }
        if (registerButton != null) {
            registerButton.setVisibility(View.GONE);
        }

        // Hide username field in disguise mode
        if (usernameInput != null) {
            usernameInput.setVisibility(View.GONE);
        }

        // Change appearance based on disguise type
        switch (disguiseType) {
            case "Calculator":
                titleText.setText("Calculator");
                passwordInput.setHint("Enter number");
                passwordInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                loginButton.setText("Calculate");
                break;
            case "Weather":
                titleText.setText("Weather");
                passwordInput.setHint("Enter city");
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT);
                loginButton.setText("Get Weather");
                break;
            case "Todo List":
                titleText.setText("Todo List");
                passwordInput.setHint("Add task");
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT);
                loginButton.setText("Add");
                break;
            case "Expense Tracker":
                titleText.setText("Expense Tracker");
                passwordInput.setHint("Enter amount");
                passwordInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                loginButton.setText("Add Expense");
                break;
            default:
                titleText.setText("App");
                passwordInput.setHint("Enter text");
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT);
                loginButton.setText("OK");
        }

        // Long press to reveal real app
        loginButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showPasswordPrompt();
                return true;
            }
        });

        // Regular click does nothing (fake functionality)
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = passwordInput.getText().toString().trim();
                if (input.isEmpty()) {
                    String hint = passwordInput.getHint() != null ? passwordInput.getHint().toString() : "input";
                    Toast.makeText(LoginActivity.this, "Please enter " + hint, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Long press the button to access your notes", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    //asks for password to exit disguise mode
    private void showPasswordPrompt() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Enter Password");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String password = input.getText().toString();
            if (encryptionManager.verifyPassword(password)) {
                // For disguise mode, use the password as username (for simplicity)
                userManager.setCurrentLocalUser(password);
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Invalid password", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}