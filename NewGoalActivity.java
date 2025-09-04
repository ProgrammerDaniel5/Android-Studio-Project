package com.example.financeapp;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * NewGoal is an Activity that provides a user interface for creating a new goal.
 * It allows users to fill in details such as the goal name, amount, description, category,
 * an optional due date (formatted as DD/MM/YYYY), and supports recording a voice note.
 */
public class NewGoalActivity extends AppCompatActivity {
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    // UI Components
    private EditText editGoalName, editGoalAmount, editGoalDescription, editGoalDueDate;
    private Spinner categorySpinner;
    private Button saveGoalButton, recordVoiceNoteButton, returnButton;

    // Audio recording fields
    private String voiceNotePath = "";
    private MediaRecorder recorder;
    private boolean isRecording = false;
    private boolean voiceNoteSaved = false;

    // Permission
    private boolean recordingPermissionGranted = false;

    // Database and user context
    private DatabaseHelper dbHelper;
    private String username;

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_goal);

        // Request audio recording permission.
        requestAudioPermissions();

        // Initialize database helper and user data.
        initDatabaseAndUser();

        // Bind UI components.
        initViews();

        // Setup the category spinner with predefined options.
        setupCategorySpinner();

        // Register event listeners for buttons.
        initListeners();
    }

    /**
     * Cleans up unsaved voice note recordings when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!voiceNoteSaved && voiceNotePath != null && !voiceNotePath.isEmpty()) {
            File tempFile = new File(voiceNotePath);
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Requests the audio recording permissions from the user.
     */
    private void requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            SharedPreferences sharedPreferences = getSharedPreferences("FinanceAppPreferences", Context.MODE_PRIVATE);
            boolean audioPermissionAsked = sharedPreferences.getBoolean("audioPermissionAsked", false);
            if (!audioPermissionAsked) {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
                // Mark that we’ve already asked for the audio permission.
                sharedPreferences.edit().putBoolean("audioPermissionAsked", true).apply();
            }
        } else {
            recordingPermissionGranted = true;
        }
    }

    /**
     * Initializes the DatabaseHelper and retrieves the username from shared preferences.
     */
    private void initDatabaseAndUser() {
        dbHelper = new DatabaseHelper(this);
        SharedPreferences sharedPreferences = getSharedPreferences("FinanceAppPreferences", Context.MODE_PRIVATE);
        username = sharedPreferences.getString("username", "defaultUser");
    }

    /**
     * Binds the UI components using findViewById.
     */
    private void initViews() {
        editGoalName = findViewById(R.id.editGoalName);
        editGoalAmount = findViewById(R.id.editGoalAmount);
        editGoalDescription = findViewById(R.id.editGoalDescription);
        editGoalDueDate = findViewById(R.id.editGoalDueDate);
        categorySpinner = findViewById(R.id.categorySpinner);
        saveGoalButton = findViewById(R.id.saveGoalButton);
        recordVoiceNoteButton = findViewById(R.id.recordVoiceNoteButton);
        returnButton = findViewById(R.id.newGoalReturnButton);
    }

    /**
     * Sets up the spinner with a fixed list of goal categories.
     */
    private void setupCategorySpinner() {
        String[] categories = {
                "Select a category", "Work", "Business", "Groceries", "Fun",
                "Travel", "Bills", "Education", "Health", "Miscellaneous"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, categories);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    /**
     * Displays a DatePicker dialog allowing the user to choose a due date.
     * The selected date is displayed in the editGoalDueDate field in the format "DD/MM/YYYY".
     */
    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year  = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day   = calendar.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    // Format the date as DD/MM/YYYY.
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, monthOfYear + 1, year1);
                    editGoalDueDate.setText(date);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    /**
     * Registers event listeners for recording, saving, and return actions.
     */
    private void initListeners() {
        // Setup Due Date field to trigger a DatePicker dialog.
        editGoalDueDate.setOnClickListener(view -> showDatePickerDialog());

        recordVoiceNoteButton.setOnClickListener(view -> {
            if (!recordingPermissionGranted) {
                Toast.makeText(NewGoalActivity.this, getResources().getString(R.string.microphone_permission_not_granted) + ".", Toast.LENGTH_SHORT).show();
                return;
            }
            toggleRecording();
        });

        saveGoalButton.setOnClickListener(view -> saveGoal());

        returnButton.setOnClickListener(view -> finish());
    }

    /**
     * Toggles the audio recording state between starting and stopping.
     * Also updates the button text accordingly.
     */
    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
            recordVoiceNoteButton.setText(getResources().getString(R.string.record_voice_note));
        } else {
            startRecording();
            recordVoiceNoteButton.setText(getResources().getString(R.string.stop_recording));
        }
        isRecording = !isRecording;
    }

    /**
     * Starts audio recording by preparing the MediaRecorder and creating a file for the voice note.
     */
    private void startRecording() {
        try {
            File outputDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            if (outputDir == null) {
                showToast(getResources().getString(R.string.unable_to_access_storage));
                return;
            }
            // Use a temporary file name for the unsaved recording.
            File outputFile = new File(outputDir, "temp_voice_note.3gp");
            // If an unsaved recording already exists, delete it.
            if (outputFile.exists()) {
                outputFile.delete();
            }
            voiceNotePath = outputFile.getAbsolutePath();
            voiceNoteSaved = false; // This recording is not yet saved as a unique file.

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(voiceNotePath);
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            e.printStackTrace();
            showToast(getResources().getString(R.string.recording_failed) + ": " + e.getMessage());
        }
    }

    /**
     * Stops audio recording and releases the MediaRecorder.
     * Catches potential errors if stop() is called too early.
     */
    private void stopRecording() {
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (RuntimeException stopException) {
                Log.e("RecordingError", "Failed to stop recording: " + stopException.getMessage());
            }
            recorder.release();
            recorder = null;
        }
    }

    /**
     * Validates user input, saves the new goal in the database, and navigates back to the tracker if successful.
     * The due date is optional.
     */
    private void saveGoal() {
        String goalName = editGoalName.getText().toString().trim();
        String goalAmountStr = editGoalAmount.getText().toString().trim();
        String goalDescription = editGoalDescription.getText().toString().trim();
        String selectedCategory = categorySpinner.getSelectedItem().toString();
        String goalDueDate = editGoalDueDate.getText().toString().trim(); // Optional

        if (goalName.isEmpty() || goalAmountStr.isEmpty() || selectedCategory.equals("Select a category")) {
            showToast(getResources().getString(R.string.please_fill_all_required_fields));
            return;
        }

        double goalAmount;
        try {
            goalAmount = Double.parseDouble(goalAmountStr);
        } catch (NumberFormatException e) {
            showToast(getResources().getString(R.string.invalid_amount));
            return;
        }

        // If a voice note was recorded and not yet saved (temp file exists), rename it.
        if (voiceNotePath != null && !voiceNotePath.isEmpty() && !voiceNoteSaved) {
            File tempFile = new File(voiceNotePath);
            if (tempFile.exists()) {
                File outputDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                // Create a unique filename using a timestamp.
                String timeStamp = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.getDefault()).format(new Date());
                String uniqueFilename = "voice_note_" + timeStamp + ".3gp";
                File newFile = new File(outputDir, uniqueFilename);
                boolean renamed = tempFile.renameTo(newFile);
                if (renamed) {
                    voiceNotePath = newFile.getAbsolutePath();
                    voiceNoteSaved = true;
                } else {
                    showToast("Failed to rename the voice note file.");
                }
            }
        }

        boolean inserted = dbHelper.insertGoal(username, goalName, goalDescription, goalAmount, selectedCategory, goalDueDate, voiceNotePath);
        if (inserted) {
            showToast(getResources().getString(R.string.goal_saved_successfully));
            Intent intent = new Intent(NewGoalActivity.this, GoalsTrackerActivity.class);
            startActivity(intent);
            finish();
        } else {
            showToast(getResources().getString(R.string.failed_to_save_the_goal));
        }
    }

    /**
     * Displays a Toast message for a short duration.
     *
     * @param message The message to display.
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Handles the response from the permission request.
     *
     * @param requestCode  the request code passed in requestPermissions().
     * @param permissions  the requested permissions.
     * @param grantResults the results for the requested permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            recordingPermissionGranted = grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED);
            if (!recordingPermissionGranted) {
                recordVoiceNoteButton.setEnabled(false);
            }
        }
    }
}
