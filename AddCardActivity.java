package com.example.financeapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;
import java.util.Locale;

/**
 * The AddCard activity allows the user to add a new card to a specific account.
 * It retrieves the current username and account ID, binds the views, and applies
 * a text watcher to format the card number input. When the expiration date field is touched,
 * a month-year picker dialog is shown. The activity provides two navigation buttons:
 * one to return to the CardsActivity screen and one to save the new card.
 */
public class AddCardActivity extends AppCompatActivity {

    // Member variables for account info.
    private int accountID;
    private DatabaseHelper db;

    // UI components.
    private Button addCardReturnButton, saveCardButton;
    private EditText editCardNumber, editCardExpirationDate;

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
        setContentView(R.layout.activity_add_card);

        // Initialize essential data.
        initData();

        // Bind views from the layout.
        initViews();

        // Set up event listeners.
        initListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Initializes the database helper and account ID.
     */
    private void initData() {
        accountID = getIntent().getIntExtra("accountID", -1);
        db = new DatabaseHelper(this);
    }

    /**
     * Binds the XML views to the corresponding member variables.
     */
    private void initViews() {
        addCardReturnButton = findViewById(R.id.addCardReturnButton);
        saveCardButton = findViewById(R.id.saveCardButton);
        editCardNumber = findViewById(R.id.editCardNumber);
        editCardExpirationDate = findViewById(R.id.editCardExpirationDate);
    }

    /**
     * Sets up the event listeners for the card number formatting, expiration date picker,
     * and button actions.
     */
    private void initListeners() {
        // Apply formatting to the card number field.
        setupCardNumberFormatting();

        // Set up expiration date field to show a month-year picker.
        editCardExpirationDate.setOnClickListener(view -> showMonthYearPicker(editCardExpirationDate));

        // Set click listener for the return button.
        addCardReturnButton.setOnClickListener(view -> navigateBackToCards());

        // Set click listener for the save button.
        saveCardButton.setOnClickListener(view -> saveCard());
    }

    /**
     * Registers a TextWatcher on the card number EditText to automatically format the number
     * into groups of 4 digits.
     */
    private void setupCardNumberFormatting() {
        editCardNumber.addTextChangedListener(new TextWatcher() {
            String current = "";
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                // No action required.
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                // No action required.
            }
            @Override
            public void afterTextChanged(Editable editable) {
                if (!editable.toString().equals(current)) {
                    String cleanString = editable.toString().replaceAll("\\s", "");
                    StringBuilder formattedString = new StringBuilder();
                    for (int i = 0; i < cleanString.length(); i++) {
                        if (i > 0 && i % 4 == 0) {
                            formattedString.append(" ");
                        }
                        formattedString.append(cleanString.charAt(i));
                    }
                    current = formattedString.toString();
                    editCardNumber.removeTextChangedListener(this);
                    editCardNumber.setText(current);
                    editCardNumber.setSelection(current.length());
                    editCardNumber.addTextChangedListener(this);
                }
            }
        });
    }

    /**
     * Navigates back to the Cards activity while passing the current account ID.
     */
    private void navigateBackToCards() {
        Intent intent = new Intent(AddCardActivity.this, CardsActivity.class);
        intent.putExtra("accountID", accountID);
        startActivity(intent);
    }

    /**
     * Saves the card information using the database helper. If the save is successful,
     * it shows a confirmation Toast and navigates back to the Cards activity.
     */
    private void saveCard() {
        String cardNumber = editCardNumber.getText().toString();
        String cardExpirationDate = editCardExpirationDate.getText().toString();
        if (db.addCard(accountID, cardNumber, cardExpirationDate)) {
            Toast.makeText(AddCardActivity.this, "Card saved", Toast.LENGTH_SHORT).show();
            navigateBackToCards();
        } else {
            Toast.makeText(AddCardActivity.this, "Failed to save the card", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Displays a month-year picker dialog so the user can select the card's expiration date.
     *
     * @param targetEditText The EditText that should display the formatted expiration date.
     */
    private void showMonthYearPicker(EditText targetEditText) {
        LayoutInflater inflater = LayoutInflater.from(AddCardActivity.this);
        View dialogView = inflater.inflate(R.layout.month_year_picker, null);

        NumberPicker monthPicker = dialogView.findViewById(R.id.month_picker);
        NumberPicker yearPicker = dialogView.findViewById(R.id.year_picker);

        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setDisplayedValues(new String[]{
                "01", "02", "03", "04", "05", "06",
                "07", "08", "09", "10", "11", "12"
        });
        monthPicker.setWrapSelectorWheel(true);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(currentYear);
        yearPicker.setMaxValue(currentYear + 20);
        yearPicker.setWrapSelectorWheel(true);

        AlertDialog.Builder builder = new AlertDialog.Builder(AddCardActivity.this);
        builder.setView(dialogView);
        builder.setPositiveButton("OK", (dialog, which) -> {
            int selectedMonth = monthPicker.getValue();
            int selectedYear = yearPicker.getValue();
            String formattedDate = String.format(Locale.getDefault(), "%02d/%d", selectedMonth, selectedYear);
            targetEditText.setText(formattedDate);
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }
}