package com.example.financeapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * The ProfileActivity class displays the user's profile information and handles all related interactions.
 */
public class ProfileActivity extends AppCompatActivity {

    // UI components for displaying the profile
    private Button profileReturnButton, editProfileButton;
    private TextView profileUsernameTextView, profileFirstNameTextView, profileLastNameTextView, profileBirthDateTextView;

    private DatabaseHelper db;

    /**
     * Called when the activity starts. This method sets up all needed UI components,
     * loads the user's profile data, assigns event handlers, and adapts the layout to an edge-to-edge display.
     *
     * @param savedInstanceState A Bundle containing the activity's previously saved state or null if none.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        initViews();
        loadUserProfile();
        initListeners();

        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Initializes all UI components and creates a DatabaseHelper object.
     */
    private void initViews() {
        profileReturnButton = findViewById(R.id.profileReturnButton);
        editProfileButton = findViewById(R.id.editProfileButton);
        profileUsernameTextView = findViewById(R.id.profileUsernameTextView);
        profileFirstNameTextView = findViewById(R.id.profileFirstNameTextView);
        profileLastNameTextView = findViewById(R.id.profileLastNameTextView);
        profileBirthDateTextView = findViewById(R.id.profileBirthDateTextView);

        // Create a DatabaseHelper object for data retrieval.
        db = new DatabaseHelper(this);
    }

    /**
     * Loads the user's profile data.
     * Retrieves the username from shared preferences and queries the database for additional details,
     * such as first name, last name, and birth date, then displays this data in the corresponding TextViews.
     */
    private void loadUserProfile() {
        SharedPreferences sharedPreferences = getSharedPreferences("FinanceAppPreferences", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("username", null);

        if (username != null) {
            String usernameText = getResources().getString(R.string.username) + ": " + username;
            profileUsernameTextView.setText(usernameText);
            String FirstNameText = getResources().getString(R.string.first_name) + ": " + db.getUserItem(username, DatabaseHelper.USERS_FIRST_NAME);
            profileFirstNameTextView.setText(FirstNameText);
            String lastNameText = getResources().getString(R.string.last_name) + ": " + db.getUserItem(username, DatabaseHelper.USERS_LAST_NAME);
            profileLastNameTextView.setText(lastNameText);
            String birthDateText = getResources().getString(R.string.birthdate) + ": " + db.getUserItem(username, DatabaseHelper.USERS_BIRTH_DATE);
            profileBirthDateTextView.setText(birthDateText);
        }
    }

    /**
     * Assigns click event handlers to UI elements.
     * Sets up listeners for the return button and the edit profile button.
     * The return button navigates to MainActivity, while the edit profile button opens the ProfileEdit activity.
     */
    private void initListeners() {
        profileReturnButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Opens MainActivity when the button is clicked.
             * @param view The view that was clicked.
             */
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        editProfileButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Opens ProfileEditActivity when the button is clicked.
             * @param view The view that was clicked.
             */
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProfileActivity.this, ProfileEditActivity.class);
                startActivity(intent);
            }
        });
    }
}