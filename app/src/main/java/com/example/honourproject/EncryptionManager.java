package com.example.honourproject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

//Summary of class
//Encrypts and Decrypts data using AES method

//Handles encrypting/decrypting data with AES/GCM
public class EncryptionManager {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding"; //encryption methid
    private SharedPreferences prefs; //stores passwords securely

    public EncryptionManager(Context context) {
        //get saved preferences for storing passwords
        this.prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
    }

    //turns plaintext to ciphertext
    public String encrypt(String data) throws Exception {
        if (data == null) return null;

        String password = getOrCreatePassword(); //gets app secret key
        SecretKey secretKey = generateKey(password); //converts password to encryption key

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] iv = cipher.getIV(); //random data for decryption
        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        //Combine IV (random data for encryption to ensure that encrypted text doesnt encrypt the same way twice) and encrypted data
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.encodeToString(combined, Base64.DEFAULT); //makes text-safe
    }

    //turn ciphertext into plaintext
    public String decrypt(String encryptedData) throws Exception {
        if (encryptedData == null) return null;

        byte[] combined = Base64.decode(encryptedData, Base64.DEFAULT); //reverse encoding

        String password = getOrCreatePassword();
        SecretKey secretKey = generateKey(password);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);

        //split IV and encrypted data
        byte[] iv = Arrays.copyOfRange(combined, 0, 12);
        byte[] encrypted = Arrays.copyOfRange(combined, 12, combined.length);

        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
        byte[] decrypted = cipher.doFinal(encrypted);

        return new String(decrypted, StandardCharsets.UTF_8);
    }

    //convert password string into AES key
    private SecretKey generateKey(String password) {
        byte[] keyBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] finalKey = Arrays.copyOf(keyBytes, 32); //make string 32 bytes
        return new SecretKeySpec(finalKey, "AES");
    }

    //get app secret key, if its doesnt exist, create one
    private String getOrCreatePassword() {
        String password = prefs.getString("app_key", null);
        if (password == null) {
            password = "SecretKey123!" + System.currentTimeMillis(); //generates default key
            prefs.edit().putString("app_key", password).apply();
        }
        return password;
    }

    //save users password
    public void setAppPassword(String password) {
        prefs.edit().putString("user_password", password).apply();
    }

    //check if password is correct
    public boolean verifyPassword(String inputPassword) {
        String storedPassword = prefs.getString("user_password", null);
        if (storedPassword == null) {
            setAppPassword(inputPassword); //if its first time entering password, save it
            return true;
        }
        return inputPassword.equals(storedPassword);
    }
}
