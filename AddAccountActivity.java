package com.example.financeapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * AddAccount activity allows the user to create a new financial account.
 * This activity retrieves the current username from shared preferences, initializes the UI elements,
 * sets up the account type spinner, and assigns click listeners.
 */
public class AddAccountActivity extends AppCompatActivity {

    private String username;
    private Button addAccountReturnButton, saveAccountButton;
    private EditText editAccountName, editAccountBank, editCustomAccountType;
    private Spinner accountTypeSpinner;
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
        setContentView(R.layout.activity_add_account);

        SharedPreferences sharedPreferences = getSharedPreferences("FinanceAppPreferences", Context.MODE_PRIVATE);
        username = sharedPreferences.getString("username", null);

        db = new DatabaseHelper(this);

        // Bind views and set up account type spinner.
        initViews();
        setupAccountTypeSpinner();

        // Set up button listeners.
        initListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Binds XML views to member variables.
     */
    private void initViews() {
        addAccountReturnButton = findViewById(R.id.addAccountReturnButton);
        saveAccountButton = findViewById(R.id.saveAccountButton);
        editAccountName = findViewById(R.id.editAccountName);
        editAccountBank = findViewById(R.id.editAccountBank);
        accountTypeSpinner = findViewById(R.id.accountTypeSpinner);
        editCustomAccountType = findViewById(R.id.editCustomAccountType);
    }

    /**
     * Sets up the account type spinner with predefined account types.
     * Displays a custom input field (editCustomAccountType) if the user selects "Other".
     */
    private void setupAccountTypeSpinner() {
        ArrayList<String> accountTypes = new ArrayList<>(Arrays.asList("Personal", "Business", "Other"));
        ArrayAdapter<String> accountTypeAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, accountTypes);
        accountTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accountTypeSpinner.setAdapter(accountTypeAdapter);

        accountTypeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = accountTypeSpinner.getSelectedItem().toString();
                if ("Other".equals(selected)) {
                    // Show the custom account type EditText when "Other" is selected.
                    editCustomAccountType.setVisibility(View.VISIBLE);
                } else {
                    // Hide the custom account type input when a predefined type is selected.
                    editCustomAccountType.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // No action needed.
            }
        });
    }

    /**
     * Sets up the click listeners for the buttons by calling dedicated functions.
     */
    private void initListeners() {
        addAccountReturnButton.setOnClickListener(view -> navigateToAccounts());
        saveAccountButton.setOnClickListener(view -> processAndSaveAccount());
    }

    /**
     * Navigates back to the Accounts activity.
     */
    private void navigateToAccounts() {
        Intent intent = new Intent(AddAccountActivity.this, AccountsActivity.class);
        startActivity(intent);
    }

    /**
     * Processes user input for the new account, validates the details, and saves the account in the database.
     * If "Other" is selected as the account type, the custom type input is used.
     */
    private void processAndSaveAccount() {
        String accountName = editAccountName.getText().toString().trim();
        String accountBank = editAccountBank.getText().toString().trim();
        String accountType = accountTypeSpinner.getSelectedItem().toString();

        // If "Other" is selected, use the text from the custom account type field.
        if ("Other".equals(accountType)) {
            accountType = editCustomAccountType.getText().toString().trim();
            if (accountType.isEmpty()) {
                Toast.makeText(AddAccountActivity.this, "Enter a custom type or select a valid type", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Validate mandatory fields.
        if (accountName.isEmpty()) {
            Toast.makeText(AddAccountActivity.this, "Enter an account name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (accountBank.isEmpty()) {
            Toast.makeText(AddAccountActivity.this, "Enter bank/organization name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save the new account in the database.
        if (db.addAccount(username, accountName, accountType, accountBank)) {
            Toast.makeText(AddAccountActivity.this, "Account saved", Toast.LENGTH_SHORT).show();
            navigateToAccounts();
        } else {
            Toast.makeText(AddAccountActivity.this, "Failed to save the account", Toast.LENGTH_SHORT).show();
        }
    }
}