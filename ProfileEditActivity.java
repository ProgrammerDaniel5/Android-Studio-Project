package com.example.financeapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * ProfileEditActivity allows users to edit their profile information,
 * including first and last name. Changes are saved in the database.
 */
public class ProfileEditActivity extends AppCompatActivity {

    // UI components for profile editing
    private EditText editFirstName, editLastName;
    private Button saveFirstNameButton, saveLastNameButton, profileEditReturnButton;

    // Database helper instance for interacting with user data
    private DatabaseHelper db;

    // The current user's username retrieved from SharedPreferences
    private String username;

    /**
     * Initializes the activity, sets up UI components, retrieves user data,
     * and registers event listeners.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down, this Bundle contains
     *                           the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        initDatabase();
        initViews();
        populateFields();
        initListeners();
    }

    /**
     * Initializes the database helper and retrieves the current username.
     */
    private void initDatabase() {
        db = new DatabaseHelper(this);
        SharedPreferences sharedPreferences = getSharedPreferences("FinanceAppPreferences", Context.MODE_PRIVATE);
        username = sharedPreferences.getString("username", null);
    }

    /**
     * Binds UI components with their corresponding views in the layout.
     */
    private void initViews() {
        editFirstName = findViewById(R.id.editFirstName);
        editLastName = findViewById(R.id.editLastName);
        saveFirstNameButton = findViewById(R.id.saveFirstNameBtn);
        saveLastNameButton = findViewById(R.id.saveLastNameBtn);
        profileEditReturnButton = findViewById(R.id.profileEditReturnButton);
    }

    /**
     * Pre-populates input fields with existing user data retrieved from the database.
     */
    private void populateFields() {
        String currentFirstName = db.getUserItem(username, DatabaseHelper.USERS_FIRST_NAME);
        String currentLastName = db.getUserItem(username, DatabaseHelper.USERS_LAST_NAME);
        if (currentFirstName != null) {
            editFirstName.setText(currentFirstName);
        }
        if (currentLastName != null) {
            editLastName.setText(currentLastName);
        }
    }

    /**
     * Registers event listeners for user interactions.
     */
    private void initListeners() {
        profileEditReturnButton.setOnClickListener(view -> {
            Intent intent = new Intent(ProfileEditActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Handle first name update
        saveFirstNameButton.setOnClickListener(view -> updateFirstName());

        // Handle last name update
        saveLastNameButton.setOnClickListener(view -> updateLastName());
    }

    /**
     * Updates the first name in the database after validating input.
     */
    private void updateFirstName() {
        String newFirstName = editFirstName.getText().toString().trim();
        if (TextUtils.isEmpty(newFirstName)) {
            editFirstName.setError(getResources().getString(R.string.first_name_cannot_be_empty));
            return;
        }
        boolean success = db.updateUserFirstName(username, newFirstName);
        showToast(success ? R.string.first_name_updated_successfully : R.string.error_updating_first_name);
    }

    /**
     * Updates the last name in the database. Last name can be left empty.
     */
    private void updateLastName() {
        String newLastName = editLastName.getText().toString().trim();
        boolean success = db.updateUserLastName(username, newLastName);
        showToast(success ? R.string.last_name_updated_successfully : R.string.error_updating_last_name);
    }

    /**
     * Displays a short Toast message.
     *
     * @param messageResId Resource ID of the message to be displayed.
     */
    private void showToast(int messageResId) {
        Toast.makeText(this, getResources().getString(messageResId), Toast.LENGTH_SHORT).show();
    }
}
