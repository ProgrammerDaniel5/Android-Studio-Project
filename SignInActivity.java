package com.example.financeapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The SignIn activity handles user login by managing both the user interface and the authentication logic.
 * It validates user input, hashes the password, checks credentials using the DatabaseHelper, and also saves
 * user preferences for auto-login. All operations are organized into private helper methods for clarity.
 */
public class SignInActivity extends AppCompatActivity {

    // UI elements
    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button signInButton;
    private Button signInReturnButton;
    private CheckBox rememberMeCheckBox;

    // Database helper for credential lookup
    private DatabaseHelper db;

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);

        // Initialize the DatabaseHelper instance
        db = new DatabaseHelper(this);

        // Initialize and set up the views and listeners
        initViews();
        initListeners();
    }

    /**
     * Initializes UI components by binding them to their corresponding layout views.
     */
    private void initViews() {
        usernameEditText = findViewById(R.id.editUsernameSignIn);
        passwordEditText = findViewById(R.id.editPasswordSignIn);
        signInButton = findViewById(R.id.signInButton);
        signInReturnButton = findViewById(R.id.signInReturnButton);
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox);
    }

    /**
     * Sets up onClick listeners for the buttons and adjusts the layout for system insets.
     */
    private void initListeners() {
        // Navigate back to the Entrance activity when the return button is clicked
        signInReturnButton.setOnClickListener(view -> navigateToEntrance());
        // Process user sign-in when the sign-in button is clicked
        signInButton.setOnClickListener(view -> processSignIn());

        // Adjust layout padding for proper edge-to-edge display using system window insets.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Handles the sign-in process by validating input, hashing the password,
     * verifying credentials, saving preferences, and navigating to the main activity.
     */
    private void processSignIn() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate that both username and password have been entered
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter both username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hash the plaintext password using SHA-256
        String hashedPassword = hashPassword(password);

        // Check the provided credentials using the DatabaseHelper
        if (checkUserCredentials(username, hashedPassword)) {
            saveUserPreferences(username, rememberMeCheckBox.isChecked());
            Toast.makeText(this, "Sign In Successful", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(SignInActivity.this, MainActivity.class));
        } else {
            Toast.makeText(this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Navigates back to the Entrance activity.
     */
    private void navigateToEntrance() {
        Intent intent = new Intent(SignInActivity.this, EntranceActivity.class);
        startActivity(intent);
    }

    /**
     * Checks the user's credentials by querying the DatabaseHelper.
     *
     * @param username      The entered username.
     * @param hashedPassword The hashed representation of the entered password.
     * @return true if the credentials are valid; false otherwise.
     */
    private boolean checkUserCredentials(String username, String hashedPassword) {
        return db.checkUser(username, hashedPassword);
    }

    /**
     * Hashes the provided plaintext password using the SHA-256 algorithm.
     *
     * @param password The plaintext password.
     * @return A hexadecimal string representing the hashed password.
     * @throws RuntimeException if the SHA-256 algorithm is not available.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = messageDigest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Saves the user's login preferences using SharedPreferences.
     *
     * @param username The username to store.
     * @param remember true if the "Remember Me" option is selected.
     */
    private void saveUserPreferences(String username, boolean remember) {
        SharedPreferences sharedPreferences = getSharedPreferences("FinanceAppPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.putBoolean("remember_me", remember);
        editor.apply();
    }
}
