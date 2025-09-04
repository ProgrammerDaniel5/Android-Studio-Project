package com.example.financeapp;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat.Type;
import androidx.core.graphics.Insets;

import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

/**
 * GoalsTracker is an activity that displays a list of user goals.
 * It loads goal data from the database, sets up the adapter,
 * and provides navigation and context menu options for editing or deleting goals.
 */
public class GoalsTrackerActivity extends AppCompatActivity {

    // UI components
    private ListView listViewGoals;
    private Button newGoalButton, goalsTrackerReturnButton;

    // Database and adapter related members
    private DatabaseHelper db;
    private GoalsAdapter goalsAdapter;
    private ArrayList<Goal> goalObjects;

    // User information and selection index
    private String username;

    /**
     * Called when the activity is created.
     * Initializes the database helper, loads the username from shared preferences,
     * sets up views, loads goal data, configures the adapter, and assigns event listeners.
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state or null if new.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goals_tracker);

        // Instantiate the database helper and initialize the goals list.
        db = new DatabaseHelper(this);
        goalObjects = new ArrayList<>();

        // Retrieve username from shared preferences.
        SharedPreferences sharedPreferences = getSharedPreferences("FinanceAppPreferences", Context.MODE_PRIVATE);
        username = sharedPreferences.getString("username", "defaultUser");

        // Initialize UI views, load data, set up adapter, and register listeners.
        initViews();
        loadGoals();
        initAdapter();
        initListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Initializes the UI components by binding the views from the layout.
     */
    private void initViews() {
        listViewGoals = findViewById(R.id.listViewGoals);
        newGoalButton = findViewById(R.id.newGoalButton);
        goalsTrackerReturnButton = findViewById(R.id.goalsTrackerReturnButton);
    }

    /**
     * Sets up the custom adapter for the goals ListView.
     * Passes the database helper and username to the adapter for dynamic progress calculations.
     */
    private void initAdapter() {
        goalsAdapter = new GoalsAdapter(this, goalObjects, db, username);
        listViewGoals.setAdapter(goalsAdapter);
    }

    /**
     * Registers event listeners for UI components.
     * This includes navigation buttons, a new goal button, and later context menu operations.
     */
    private void initListeners() {
        goalsTrackerReturnButton.setOnClickListener(view -> {
            Intent intent = new Intent(GoalsTrackerActivity.this, MainActivity.class);
            startActivity(intent);
        });

        newGoalButton.setOnClickListener(view -> {
            Intent intent = new Intent(GoalsTrackerActivity.this, NewGoalActivity.class);
            startActivity(intent);
        });

        // Register the list view for context menu operations.
        registerForContextMenu(listViewGoals);
    }

    /**
     * Loads the user's goals from the database.
     * Clears the current list and repopulates it using the cursor returned from the database query.
     * Also retrieves the optional due date for each goal.
     */
    private void loadGoals() {
        goalObjects.clear();
        Cursor cursor = db.getUserGoals(username);
        if (cursor != null) {
            try {
                int goalIDIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.GOALS_ID);
                int goalNameIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.GOALS_NAME);
                int goalAmountIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.GOALS_AMOUNT);
                int goalDescriptionIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.GOALS_DESCRIPTION);
                int goalCategoryIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.GOALS_CATEGORY);
                int goalDueDateIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.GOALS_DUE_DATE);
                int voiceNoteIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.GOALS_VOICE_NOTE_PATH);

                if (cursor.moveToFirst()) {
                    do {
                        int goalID = cursor.getInt(goalIDIndex);
                        String goalName = cursor.getString(goalNameIndex);
                        double goalAmount = cursor.getDouble(goalAmountIndex);
                        String goalDescription = cursor.getString(goalDescriptionIndex);
                        String goalCategory = cursor.getString(goalCategoryIndex);
                        String goalDueDate = cursor.getString(goalDueDateIndex);
                        String voiceNotePath = cursor.getString(voiceNoteIndex);

                        // Create new Goal instance.
                        Goal goal = new Goal(goalID, goalName, goalDescription, goalAmount, goalCategory, goalDueDate, voiceNotePath);
                        goalObjects.add(goal);
                    } while (cursor.moveToNext());
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Creates the context menu for the ListView.
     *
     * @param menu    The context menu being built.
     * @param view    The view for which the menu is being built.
     * @param menuInfo Additional menu information.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        menu.setHeaderTitle(getResources().getString(R.string.select_an_option));
        menu.add(0, view.getId(), 0, "Edit");
        menu.add(0, view.getId(), 0, "Delete");
    }

    /**
     * Handles selections from the context menu.
     * If "Edit" is selected, shows the edit dialog. If "Delete" is selected, removes the goal.
     *
     * @param item The selected menu item.
     * @return true if the selection is handled, false otherwise.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        if (info == null) {
            Log.e("GoalsTrackerActivity", "menuInfo is null, cannot process menu action.");
            return false;
        }

        int position = info.position;

        if (Objects.equals(item.getTitle(), "Edit")) {
            Goal goalToEdit = goalObjects.get(position);
            showEditDialog(goalToEdit);
            return true;
        } else if (Objects.equals(item.getTitle(), "Delete")) {
            Goal goalToDelete = goalObjects.get(position);
            // Attempt to delete the goal from the database.
            boolean deleted = db.deleteGoal(goalToDelete.getGoalId());
            if (deleted) {
                // Delete the associated voice note file, if it exists.
                String voiceNotePath = goalToDelete.getVoiceNotePath();
                if (voiceNotePath != null && !voiceNotePath.isEmpty()) {
                    File voiceNoteFile = new File(voiceNotePath);
                    if (voiceNoteFile.exists()) {
                        boolean fileDeleted = voiceNoteFile.delete();
                        if (!fileDeleted) {
                            Toast.makeText(GoalsTrackerActivity.this, getResources().getString(R.string.failed_to_delete_voice_note_file), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                goalObjects.remove(position);
                goalsAdapter.notifyDataSetChanged();
                Toast.makeText(GoalsTrackerActivity.this, getResources().getString(R.string.goal) + " \"" + goalToDelete.getGoalName() + "\" " + getResources().getString(R.string.deleted) + ".", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(GoalsTrackerActivity.this, getResources().getString(R.string.failed_to_delete_goal), Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Displays a dialog that allows the user to edit a goal's details.
     * The dialog is pre-populated with the goal's current values.
     * Long-pressing the due date field clears its contents. On confirmation,
     * updated values are validated and saved to the database.
     *
     * @param goalToEdit The goal object to be edited.
     */
    private void showEditDialog(final Goal goalToEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.goal_editing));

        // Inflate the custom dialog layout.
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_goal, null);
        builder.setView(dialogView);

        // Get references to dialog views.
        final EditText editGoalName = dialogView.findViewById(R.id.editGoalName);
        final EditText editGoalAmount = dialogView.findViewById(R.id.editGoalAmount);
        final EditText editGoalDescription = dialogView.findViewById(R.id.editGoalDescription);
        final Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        final EditText editGoalDueDate = dialogView.findViewById(R.id.editGoalDueDate);

        // Pre-populate fields.
        editGoalName.setText(goalToEdit.getGoalName());
        editGoalAmount.setText(String.valueOf(goalToEdit.getGoalAmount()));
        editGoalDescription.setText(goalToEdit.getGoalDescription());
        editGoalDueDate.setText(goalToEdit.getGoalDueDate()); // May be empty if not set

        // Setup spinner with fixed categories.
        String[] categories = {"Select a category", "Work", "Business", "Groceries", "Fun", "Travel", "Bills", "Education", "Health", "Miscellaneous"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);
        // Set the selection based on the current goal category.
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(goalToEdit.getGoalCategory())) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        // Setup the Due Date field:
        // a) It opens a DatePicker when clicked.
        // b) It clears its text when long-pressed.
        editGoalDueDate.setFocusable(false);
        editGoalDueDate.setClickable(true);
        editGoalDueDate.setOnClickListener(view -> {
            Calendar calendar = Calendar.getInstance();
            int year  = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day   = calendar.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    GoalsTrackerActivity.this,
                    (datePicker, year1, monthOfYear, dayOfMonth) -> {
                        // Format the date as DD/MM/YYYY.
                        String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, monthOfYear + 1, year1);
                        editGoalDueDate.setText(date);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });
        // Allow the due date to be cleared by long pressing the field.
        editGoalDueDate.setOnLongClickListener(v -> {
            editGoalDueDate.setText(""); // Clear the due date.
            Toast.makeText(GoalsTrackerActivity.this, getResources().getString(R.string.due_date_cleared), Toast.LENGTH_SHORT).show();
            return true; // Consumed the long click.
        });

        // Set up dialog buttons.
        builder.setPositiveButton(getResources().getString(R.string.update), (dialog, id) -> {
            String newName = editGoalName.getText().toString().trim();
            String newAmountStr = editGoalAmount.getText().toString().trim();
            String newDescription = editGoalDescription.getText().toString().trim();
            String newCategory = spinnerCategory.getSelectedItem().toString();
            String newDueDate = editGoalDueDate.getText().toString().trim(); // May be empty if cleared

            if (newName.isEmpty() || newAmountStr.isEmpty() || newCategory.equals("Select a category")) {
                Toast.makeText(GoalsTrackerActivity.this, getResources().getString(R.string.all_required_fields_must_be_completed), Toast.LENGTH_SHORT).show();
                return;
            }
            double newAmount;
            try {
                newAmount = Double.parseDouble(newAmountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(GoalsTrackerActivity.this, getResources().getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show();
                return;
            }
            // Update the goal in the database. The updateGoal method will convert an empty due date to null.
            boolean updated = db.updateGoal(goalToEdit.getGoalId(), newName, newDescription, newAmount, newCategory, newDueDate, goalToEdit.getVoiceNotePath());
            if (updated) {
                // Update the in-memory Goal object.
                goalToEdit.setGoalName(newName);
                goalToEdit.setGoalDescription(newDescription);
                goalToEdit.setGoalAmount(newAmount);
                goalToEdit.setGoalCategory(newCategory);
                goalToEdit.setGoalDueDate(newDueDate); // This will be an empty string if cleared.
                goalsAdapter.notifyDataSetChanged();
                Toast.makeText(GoalsTrackerActivity.this, getResources().getString(R.string.goal_updated), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(GoalsTrackerActivity.this, getResources().getString(R.string.update_failed), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.cancel), null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}