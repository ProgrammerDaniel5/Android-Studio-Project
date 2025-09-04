package com.example.financeapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * ChangePassword activity allows the user to update their account password.
 * Upon creation, the activity sets up the layout, retrieves the username from shared preferences,
 * and binds the views. It then registers dedicated functions for each button.
 */
public class ChangePasswordActivity extends AppCompatActivity {

    // UI components.
    private EditText currentPasswordEdit, newPasswordEdit, confirmNewPasswordEdit;
    private Button changePasswordReturnButton, changePasswordButton;

    // Database helper instance.
    private DatabaseHelper db;

    // Username retrieved from shared preferences.
    private String username;

    /**
     * Called when the activity is created.
     * Enables edge-to-edge display, sets the layout, initializes the database helper and username,
     * and then binds the views and sets up button listeners.
     *
     * @param savedInstanceState Bundle containing the previous state, or null if new.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        // Instantiate the database helper.
        db = new DatabaseHelper(this);

        // Retrieve the current username from shared preferences.
        SharedPreferences sharedPreferences = getSharedPreferences("FinanceAppPreferences", Context.MODE_PRIVATE);
        username = sharedPreferences.getString("username", null);

        // Initialize the views and set up listeners.
        initViews();
        initListeners();
    }

    /**
     * Binds XML views to member variables.
     */
    private void initViews() {
        currentPasswordEdit = findViewById(R.id.currentPasswordEdit);
        newPasswordEdit = findViewById(R.id.newPasswordEdit);
        confirmNewPasswordEdit = findViewById(R.id.confirmNewPasswordEdit);
        changePasswordReturnButton = findViewById(R.id.changePasswordReturnButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);
    }

    /**
     * Sets up button click listeners by calling dedicated helper functions.
     */
    private void initListeners() {
        changePasswordReturnButton.setOnClickListener(view -> navigateToSettings());
        changePasswordButton.setOnClickListener(view -> validateAndUpdatePassword());
    }

    /**
     * Navigates back to the Settings activity.
     */
    private void navigateToSettings() {
        Intent intent = new Intent(ChangePasswordActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Validates input, hashes the current and new passwords, compares the current password hash with the stored hash,
     * and updates the password in the database if the validation passes.
     */
    private void validateAndUpdatePassword() {
        String currentPassword = currentPasswordEdit.getText().toString().trim();
        String newPassword = newPasswordEdit.getText().toString().trim();
        String confirmPassword = confirmNewPasswordEdit.getText().toString().trim();

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(ChangePasswordActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        // Verify that the password meets minimum length requirements
        if (newPassword.length() < 6) {
            Toast.makeText(ChangePasswordActivity.this, "The new password has to contain at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(ChangePasswordActivity.this, "New passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Hash the current password using SHA-256.
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] currentHashBytes = md.digest(currentPassword.getBytes());
            StringBuilder currentHex = new StringBuilder();
            for (byte b : currentHashBytes) {
                currentHex.append(String.format("%02x", b));
            }
            String hashedCurrentPassword = currentHex.toString();

            // Retrieve the stored hashed password from the database.
            String storedPassword = db.getUserPassword(username);
            if (!hashedCurrentPassword.equals(storedPassword)) {
                Toast.makeText(ChangePasswordActivity.this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                return;
            }

            // Hash the new password.
            byte[] newHashBytes = md.digest(newPassword.getBytes());
            StringBuilder newHex = new StringBuilder();
            for (byte b : newHashBytes) {
                newHex.append(String.format("%02x", b));
            }
            String hashedNewPassword = newHex.toString();

            // Update the password in the database.
            boolean success = db.updateUserPassword(username, hashedNewPassword);
            if (success) {
                Toast.makeText(ChangePasswordActivity.this, "Password changed successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(ChangePasswordActivity.this, "Password change failed", Toast.LENGTH_SHORT).show();
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
