package com.example.financeapp;

import android.app.DatePickerDialog;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * The SignUp activity handles user registration by combining both the display logic (UI)
 * and the technical logic (input validation, password hashing, age verification, and database operations).
 * It collects required information, validates the input,
 * and registers the user if all conditions are met.
 */
public class SignUpActivity extends AppCompatActivity {

    // UI components
    private EditText editFirstName, editLastName, editBirthDate, editUsername, editPassword;
    private Button signUpButton, signUpReturnButton;
    private CheckBox rememberMeCheckBox;

    // Database helper instance for user-related operations
    private DatabaseHelper db;

    /**
     * Called when the SignUp activity is first created.
     * This method performs the following initialization tasks:
     * - Enables edge-to-edge UI for a modern, full-screen display.
     * - Sets the content view to the activity's layout.
     * - Creates an instance of DatabaseHelper for user-related database operations.
     * - Binds XML views (EditText fields, buttons, and checkboxes) to their corresponding class variables.
     * - Sets up event listeners to handle user interactions, such as signing up and returning to the previous screen.
     *
     * @param savedInstanceState a Bundle containing the activity's previously saved state; if null, this is a new instance.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        db = new DatabaseHelper(this);

        initViews();
        initListeners();
    }

    /**
     * Initializes UI components by binding XML elements to instance variables.
     */
    private void initViews() {
        editFirstName = findViewById(R.id.editFirstNameSignUp);
        editLastName = findViewById(R.id.editLastNameSignUp);
        editBirthDate = findViewById(R.id.editBirthDateSignUp);
        editUsername = findViewById(R.id.editUsernameSignUp);
        editPassword = findViewById(R.id.editPasswordSignUp);
        signUpButton = findViewById(R.id.signUpButton);
        signUpReturnButton = findViewById(R.id.signUpReturnButton);
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBoxSignUp);
    }

    /**
     * Sets up event listeners for UI components.
     * Includes handling the birth date picker, sign up processing, and returning to the entrance.
     */
    private void initListeners() {
        // Open a DatePickerDialog when the birth date field is clicked
        editBirthDate.setOnClickListener(view -> showDatePicker());

        // Navigate back to EntranceActivity if the return button is clicked
        signUpReturnButton.setOnClickListener(view -> navigateToEntrance());

        // Process sign up when the sign up button is clicked
        signUpButton.setOnClickListener(view -> processSignUp());

        // Adjust layout padding for an edge-to-edge display using system window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Displays a DatePickerDialog for the user to select their birth date.
     * Once selected, the date is formatted as "dd/MM/yyyy" and set on the birth date EditText.
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                SignUpActivity.this,
                (datePicker, selectedYear, selectedMonth, selectedDay) ->
                        editBirthDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedDay, (selectedMonth + 1), selectedYear)),
                year, month, day);
        datePickerDialog.show();
    }

    /**
     * Processes the sign up operation by verifying all required inputs,
     * ensuring the user is at least 18 years old, confirming password length,
     * checking for duplicate usernames, hashing the password, and then inserting the user into the database.
     */
    private void processSignUp() {
        // Retrieve user input
        String firstName = editFirstName.getText().toString().trim();
        String lastName = editLastName.getText().toString().trim();
        String birthDate = editBirthDate.getText().toString().trim();
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        // Validate required fields
        if (firstName.isEmpty() || birthDate.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(SignUpActivity.this, "You have to enter all required information", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the user is at least 18 years old
        if (!isAtLeast18YearsOld(birthDate)) {
            Toast.makeText(SignUpActivity.this, "You have to be at least 18 years old", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verify that the password meets minimum length requirements
        if (password.length() < 6) {
            Toast.makeText(SignUpActivity.this, "The password has to contain at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure the username is unique
        if (db.isUsernameExists(username)) {
            Toast.makeText(SignUpActivity.this, "This username is already taken", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hash the plaintext password using SHA-256
        String hashedPassword = hashPassword(password);

        // Attempt to insert the new user into the database
        if (db.insertUser(firstName, lastName, birthDate, username, hashedPassword)) {
            // Save user preferences (username and "Remember Me" state) for auto-login
            saveUserPreferences(username, rememberMeCheckBox.isChecked());
            Toast.makeText(SignUpActivity.this, "Sign Up Successful", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
        } else {
            Toast.makeText(SignUpActivity.this, "Sign Up Failed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Checks if the user is at least 18 years old based on their birth date.
     *
     * @param birthDate A string representing the user's birth date in "dd/MM/yyyy" format.
     * @return true if the user's age is 18 or above; false otherwise.
     */
    private boolean isAtLeast18YearsOld(String birthDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        try {
            Date parsedBirthDate = sdf.parse(birthDate);
            if (parsedBirthDate == null) {
                return false;
            }

            Calendar birthCalendar = Calendar.getInstance();
            birthCalendar.setTime(parsedBirthDate);
            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return age >= 18;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Hashes the provided plaintext password using the SHA-256 algorithm.
     *
     * @param password The plaintext password.
     * @return The hexadecimal string representation of the hashed password.
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
     * Saves the user's preferences (username and "Remember Me" state) into SharedPreferences
     * for auto-login functionality on subsequent app launches.
     *
     * @param username The username to store.
     * @param remember A flag indicating whether the "Remember Me" option was selected.
     */
    private void saveUserPreferences(String username, boolean remember) {
        SharedPreferences sharedPreferences = getSharedPreferences("FinanceAppPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.putBoolean("remember_me", remember);
        editor.apply();
    }

    /**
     * Navigates the user back to the Entrance activity.
     */
    private void navigateToEntrance() {
        Intent intent = new Intent(SignUpActivity.this, EntranceActivity.class);
        startActivity(intent);
    }
}
