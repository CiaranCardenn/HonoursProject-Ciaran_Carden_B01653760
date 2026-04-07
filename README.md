# Secret Notes - Encrypted Note Taking App

A secure note-taking Android app with self-destructing messages, user authentication, and disguise mode.

## Features

- **User Authentication**
  - Create local account (username/password)
  - Sign in with Google account
  
- **Secure Notes**
  - Create, edit, and delete notes
  - Encrypt individual notes with AES encryption
  - Set self-destruct timers (30 seconds, 1 minute, 5 minutes, 1 hour)
  - Get notifications before notes self-destruct

- **Disguise Mode**
  - Hide the app as a different app:
    - Calculator
    - Weather app
    - Todo List
    - Expense Tracker
  - Long press to reveal the real app with password

- **User Profile**
  - View current user info
  - Sign out option

## Tech Stack

- **Language:** Java
- **Database:** Room (SQLite)
- **Authentication:** Firebase Auth (Google Sign-In)
- **Encryption:** AES/GCM (256-bit)
- **UI:** XML layouts, RecyclerView, Material Design
- **Architecture:** MVVM pattern with LiveData

## Setup Instructions

### Prerequisites

- Android Studio Hedgehog or newer
- Minimum SDK: API 24 (Android 7.0)
- Google Services account (for Google Sign-In)

### Installation

1. Clone the repository:
```bash
git clone https://github.com/CiaranCardenn/HonoursProject-Ciaran_Carden_B01653760.git
```

2. Open the project in Android Studio
3. Sync the project with Gradle
4. Run the app on an emulator or physical device

### Firebase Setup
1. Create a project on Firebase Console
2. Register your android app with your package name
3. Download google-services.json and place it in the app/ folder
4. Enable Google Sign-In in Firebase Authentication

### How it Works
1. Login: User creates account or signs in with google
2. Create Note: Tap the + button to create a new note
3. Encrypt: Check the encryption box to encrypt the note
4. Self-Destruct: Choose how long until the note destructs
5. Disguise Mode: Enable in the settings

### Security Notes
1. User passwords are stored as plaintext in this version
2. Encryption keys are derived from a stored password
3. Notes are encrypted using AES-236 in GCM mode
4. Self-destruct timers are checked every 10 seconds

### Future Improvements
1. Add password hashing
2. Fully implement biometric authentication (currently just for display in settings)
3. Export and import notes
4. Cloud backup
5. Dark mode support

### Known Issues
1. Self-destruct notifications may not be pushed if app is force closes
2. Long press in disguise mode uses the users login password

### Contact
Ciaran Carden 
Education - B0163760@studentmail.uws.ac.uk
Personal - ciarancarden233@icloud.com
