package com.example.honourproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseUser;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

//Class Summary
//Shows all notes
//Lets user create, edit or delete notes

//Main screen, shows all notes, lets users create/edit/delete notes
public class MainActivity extends AppCompatActivity implements NoteAdapter.OnNoteClickListener {

    //UI Elements
    private RecyclerView notesRecycler;
    private FloatingActionButton fab;
    private NoteAdapter adapter;

    //Data and managers
    private AppDB database;
    private EncryptionManager encryptionManager;
    private List<Note> notes = new ArrayList<>();
    private UserManager userManager;
    private String currentUserId;

    // Profile UI elements
    private LinearLayout profileLayout;
    private CircleImageView profileImage;
    private TextView userName;
    private TextView userEmail;

    // Auto cleanup for expired notes
    private Handler cleanupHandler;
    private Runnable cleanupRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UserManager
        userManager = new UserManager(this);
        currentUserId = userManager.getCurrentUserId();

        // Check if user is logged in
        if (currentUserId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Secret Notes");
        }

        // Initialize profile views
        profileLayout = findViewById(R.id.profile_layout);
        profileImage = findViewById(R.id.profile_image);
        userName = findViewById(R.id.user_name);
        userEmail = findViewById(R.id.user_email);

        // Display user info
        displayUserInfo();

        // Initialize database and encryption
        database = AppDB.getInstance(this);
        encryptionManager = new EncryptionManager(this);

        // Setup views
        notesRecycler = findViewById(R.id.notes_recycler);
        fab = findViewById(R.id.fab);

        // Setup RecyclerView
        notesRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteAdapter(notes, this);
        notesRecycler.setAdapter(adapter);

        // LiveData observer - only show notes for current user
        LiveData<List<Note>> notesLiveData = database.noteDao().getNotesForUserLive(currentUserId);
        notesLiveData.observe(this, new Observer<List<Note>>() {
            @Override
            public void onChanged(List<Note> newNotes) {
                notes.clear();
                notes.addAll(newNotes);
                adapter.updateNotes(notes);
            }
        });

        // Floating action button to create new note
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoteDialog(null);
            }
        });

        // Start auto deleting expired notes
        startPeriodicCleanup();
    }

    //Shows user name and email in profile
    private void displayUserInfo() {
        if (userManager.isUserLoggedIn()) {
            profileLayout.setVisibility(View.VISIBLE);

            // Display name - works with both getUserName() and getCurrentUsername()
            String name = userManager.getUserName();  // Now this method exists
            if (name == null || name.isEmpty()) {
                name = userManager.getCurrentUsername();  // Fallback
            }
            userName.setText(name != null && !name.isEmpty() ? name : "User");

            // Display email
            String email = userManager.getUserEmail();
            userEmail.setText(email != null && !email.isEmpty() ? email : "");

            // Set default profile icon
            profileImage.setImageResource(R.drawable.ic_default_profile);
        } else {
            profileLayout.setVisibility(View.GONE);
        }
    }

    //checks and deletes expired notes
    private void startPeriodicCleanup() {
        cleanupHandler = new Handler();
        cleanupRunnable = new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            long currentTime = System.currentTimeMillis();
                            // Get notes to delete for ALL users (worker can delete any expired notes)
                            List<Note> expiredNotes = database.noteDao().getNotesToDelete(currentTime);

                            for (Note note : expiredNotes) {
                                database.noteDao().delete(note);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

                cleanupHandler.postDelayed(this, 10000); //runs every 10 seconds
            }
        };

        cleanupHandler.post(cleanupRunnable);
    }

    //notification for expiring notes
    private void scheduleDestructWarning(Note note) {
        long warningTime = note.noteDelete - (5 * 60 * 1000); //send notification 5 mins before deletion

        if (warningTime > System.currentTimeMillis()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    android.app.NotificationManager notificationManager =
                            (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        android.app.NotificationChannel channel = new android.app.NotificationChannel(
                                "warning_channel",
                                "Note Warnings",
                                android.app.NotificationManager.IMPORTANCE_HIGH);
                        notificationManager.createNotificationChannel(channel);
                    }

                    androidx.core.app.NotificationCompat.Builder builder =
                            new androidx.core.app.NotificationCompat.Builder(MainActivity.this, "warning_channel")
                                    .setContentTitle("Note Will Self-Destruct")
                                    .setContentText("'" + note.noteTitle + "' will be destroyed in 5 minutes")
                                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                                    .setAutoCancel(true);

                    notificationManager.notify((int) note.noteDelete, builder.build());
                }
            }, warningTime - System.currentTimeMillis());
        }
    }

    //create or edit a note
    private void showNoteDialog(final Note existingNote) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(existingNote == null ? "New Note" : "Edit Note");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_note, null);
        final EditText titleInput = dialogView.findViewById(R.id.title_input);
        final EditText contentInput = dialogView.findViewById(R.id.content_input);
        final CheckBox encryptCheck = dialogView.findViewById(R.id.encrypt_check);
        final Spinner timeSpinner = dialogView.findViewById(R.id.time_spinner);

        //options for self destruct timer
        String[] timeOptions = {"No self-destruct", "30 seconds", "1 minute", "5 minutes", "1 hour"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, timeOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeSpinner.setAdapter(spinnerAdapter);

        //for editing existing note
        if (existingNote != null) {
            titleInput.setText(existingNote.noteTitle);
            try {
                if (existingNote.isEncrypted) {
                    contentInput.setText(encryptionManager.decrypt(existingNote.noteContent));
                } else {
                    contentInput.setText(existingNote.noteContent);
                }
            } catch (Exception e) {
                contentInput.setText("Error decrypting note");
            }
            encryptCheck.setChecked(existingNote.isEncrypted);
        }

        builder.setView(dialogView);

        //save button
        builder.setPositiveButton("Save", (dialog, which) -> {
            saveNote(
                    titleInput.getText().toString(),
                    contentInput.getText().toString(),
                    encryptCheck.isChecked(),
                    timeSpinner.getSelectedItemPosition(),
                    existingNote
            );
        });

        //cancel button
        builder.setNegativeButton("Cancel", null);

        //delete button, only shows for existing notes
        if (existingNote != null) {
            builder.setNeutralButton("Delete", (dialog, which) -> {
                deleteNote(existingNote);
            });
        }

        builder.show();
    }

    //saves note to db
    private void saveNote(String title, String content, boolean encrypt,
                          int timeOption, Note existingNote) {
        Note note = existingNote != null ? existingNote : new Note();

        // Associate note with current user
        note.userId = currentUserId;
        note.noteTitle = title;

        //ecnrypt note if checked
        try {
            if (encrypt) {
                note.noteContent = encryptionManager.encrypt(content);
            } else {
                note.noteContent = content;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving note", Toast.LENGTH_SHORT).show();
            return;
        }

        note.isEncrypted = encrypt;
        note.noteCreated = existingNote != null ? note.noteCreated : System.currentTimeMillis();

        //sets self destruct based on user selection
        switch (timeOption) {
            case 1:
                note.noteDelete = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
                break;
            case 2:
                note.noteDelete = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
                break;
            case 3:
                note.noteDelete = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
                break;
            case 4:
                note.noteDelete = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);
                break;
            default:
                note.noteDelete = 0; //self destruct not chosen
        }

        //saves in background
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (existingNote != null) {
                    database.noteDao().update(note);
                } else {
                    database.noteDao().insert(note);
                }

                //wanring notification for self destruct
                if (note.noteDelete > 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            scheduleDestructWarning(note);
                        }
                    });
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Note saved!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    //delete note from db
    private void deleteNote(final Note note) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                database.noteDao().delete(note);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    //called for editing a note
    @Override
    public void onNoteClick(Note note) {
        showNoteDialog(note);
    }

    //creates options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        // Add logout option
        menu.add(0, 1000, 0, "Logout"); //add logout
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        } else if (id == 1000) {  // Logout
            userManager.logout();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //shows about app section
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About")
                .setMessage("Secret Notes App v1.0\n\nEncrypted note-taking app with self-destruct feature.")
                .setPositiveButton("OK", null)
                .show();
    }

    //cleans up when activiity ends
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cleanupHandler != null && cleanupRunnable != null) {
            cleanupHandler.removeCallbacks(cleanupRunnable);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user info in case it changed
        displayUserInfo();
    }
}