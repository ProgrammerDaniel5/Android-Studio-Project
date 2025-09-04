package com.example.financeapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * ContactMe activity allows the user to send feedback or a message to the developer.
 * The activity retrieves the current username from shared preferences, initializes
 * the UI views, and sets up listeners for submitting the message (via an email intent)
 * or returning to the MainActivity.
 */
public class ContactMeActivity extends AppCompatActivity {

    // UI components
    private EditText editContactTopic, editContactMessage;
    private Button contactSubmitButton, contactReturnButton;

    // Database helper instance for user operations
    private DatabaseHelper db;

    // Currently logged-in user's username
    private String username;

    /**
     * Called when the ContactMe activity is created.
     * Sets the layout, initializes the database and shared preferences,
     * and calls helper methods for view binding and listener setup.
     *
     * @param savedInstanceState Bundle containing the activity's previous state, or null if new.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_contact_me);

        // Instantiate DatabaseHelper
        db = new DatabaseHelper(this);

        // Retrieve username from shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences("FinanceAppPreferences", Context.MODE_PRIVATE);
        username = sharedPreferences.getString("username", null);

        // Initialize UI views and event listeners
        initViews();
        initListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Binds the UI elements from the layout to the member variables.
     */
    private void initViews() {
        editContactTopic = findViewById(R.id.editContactTopic);
        editContactMessage = findViewById(R.id.editContactMessage);
        contactSubmitButton = findViewById(R.id.contactSubmitButton);
        contactReturnButton = findViewById(R.id.contactReturnButton);
    }

    /**
     * Sets up the click listeners for the UI buttons.
     * When the return button is clicked, MainActivity is launched.
     * When the submit button is clicked, an email intent is created and started.
     */
    private void initListeners() {
        contactReturnButton.setOnClickListener(view -> {
            Intent intent = new Intent(ContactMeActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // Create and launch an email intent when the submit button is clicked
        contactSubmitButton.setOnClickListener(view -> {
            // Retrieve user's first name from the database using the username
            String firstName = db.getUserItem(username, DatabaseHelper.USERS_FIRST_NAME);
            String subject = editContactTopic.getText().toString();
            String message = editContactMessage.getText().toString();

            // Construct the mailto URI with subject and body.
            String uriText = "mailto:NotchOdin@gmail.com" +
                    "?subject=" + Uri.encode(subject) +
                    "&body=" + Uri.encode("Name: " + firstName + " (username: " + username + ")" + "\nMessage:\n" + message);
            Uri uri = Uri.parse(uriText);
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(uri);

            // Launch email client if available
            if (emailIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(emailIntent);
            }
        });
    }
}