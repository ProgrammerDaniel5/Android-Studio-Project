package com.example.financeapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

/**
 * Cards activity displays a list of cards related to a given account and allows users
 * to edit or delete cards via a context menu. It also provides navigation options to add a new card or return to accounts.
 */
public class CardsActivity extends AppCompatActivity {

    // UI components.
    private Button cardsReturnButton, newCardButton;
    private ListView listViewCards;

    private int accountID;
    private DatabaseHelper db;

    // Data structures for cards.
    private ArrayList<String> cardsList;
    private ArrayList<Integer> cardsIDList;
    private ArrayAdapter<String> cardsAdapter;

    /**
     * Called when the activity is first created.
     * This method  sets the content view, and calls helper methods to initialize data,
     * bind views, set up the adapter and listeners, load cards from the database
     * and register the context menu.
     *
     * @param savedInstanceState if the activity is being reinitialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cards);

        initData();           // Initialize database, username, accountID, and lists.
        initViews();          // Bind XML views.
        initAdapter();        // Set up the adapter for the ListView.
        initListeners();      // Assign click listeners for navigation.
        loadCardsFromDB();    // Load cards from the database.
        registerForContextMenu(listViewCards); // Register the ListView for the context menu.

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Initializes core data required by this activity.
     */
    private void initData() {
        db = new DatabaseHelper(this);
        // Data fields.
        accountID = getIntent().getIntExtra("accountID", -1);
        cardsList = new ArrayList<>();
        cardsIDList = new ArrayList<>();
    }

    /**
     * Binds the XML views from the layout to member variables.
     */
    private void initViews() {
        cardsReturnButton = findViewById(R.id.cardsReturnButton);
        newCardButton = findViewById(R.id.newCardButton);
        listViewCards = findViewById(R.id.listViewCards);
    }

    /**
     * Sets up the ArrayAdapter for displaying the list of cards.
     */
    private void initAdapter() {
        cardsAdapter = new ArrayAdapter<>(this, R.layout.list_view_item, cardsList);
        listViewCards.setAdapter(cardsAdapter);
    }

    /**
     * Sets up click listeners for the navigation buttons.
     */
    private void initListeners() {
        cardsReturnButton.setOnClickListener(view -> navigateBackToAccounts());
        newCardButton.setOnClickListener(view -> navigateToAddCard());
    }

    /**
     * Navigates back to the Accounts activity.
     */
    private void navigateBackToAccounts() {
        Intent intent = new Intent(CardsActivity.this, AccountsActivity.class);
        startActivity(intent);
    }

    /**
     * Navigates to the AddCard activity and passes the current accountID.
     */
    private void navigateToAddCard() {
        Intent intent = new Intent(CardsActivity.this, AddCardActivity.class);
        intent.putExtra("accountID", accountID);
        startActivity(intent);
    }

    /**
     * Loads the cards associated with this account from the database and updates the adapter.
     */
    private void loadCardsFromDB() {
        Cursor cursor = db.getUserCards(accountID);
        if (cursor != null) {
            try {
                int cardIDIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.CARDS_ID);
                int cardNumberIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.CARDS_NUMBER);
                int cardExpirationDateIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.CARDS_EXPIRATION_DATE);
                if (cursor.moveToFirst()) {
                    do {
                        int cardID = cursor.getInt(cardIDIndex);
                        String cardNumber = cursor.getString(cardNumberIndex);
                        String cardExpirationDate = cursor.getString(cardExpirationDateIndex);
                        cardsList.add(cardNumber + " (" + cardExpirationDate + ")");
                        cardsIDList.add(cardID);
                    } while (cursor.moveToNext());
                    cardsAdapter.notifyDataSetChanged();
                }
            } catch (IllegalArgumentException e) {
                Log.e("CardsActivity", "Column not found: " + e.getMessage());
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Called to create a context menu for the ListView.
     *
     * @param menu    The context menu that is being built.
     * @param view    The view for which the context menu is being created.
     * @param menuInfo Extra information about the view for which the menu is being created.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        menu.setHeaderTitle("Select an option");
        menu.add(0, view.getId(), 0, "Edit");
        menu.add(0, view.getId(), 0, "Delete");
    }

    /**
     * Called when a context menu item is selected.
     *
     * @param item The selected menu item.
     * @return True if the item selection is handled, false otherwise.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (menuInfo == null) {
            Log.e("CardsActivity", "menuInfo is null, cannot process menu action.");
            return false;
        }
        int position = menuInfo.position;
        if (Objects.equals(item.getTitle(), "Edit")) {
            int cardId = cardsIDList.get(position);
            String cardDetails = cardsList.get(position);
            openEditCardDialog(cardId, cardDetails);
        } else if (Objects.equals(item.getTitle(), "Delete")) {
            int cardId = cardsIDList.get(position);
            // Extract the card number from details.
            String cardNumber = cardsList.get(position).split(" \\(")[0].trim();
            deleteCard(cardId, position, cardNumber);
        }
        return true;
    }

    /**
     * Opens an edit dialog for updating the card details.
     *
     * @param cardId      The unique ID of the card to edit.
     * @param cardDetails The current card details, formatted as "CardNumber (ExpirationDate)".
     */
    private void openEditCardDialog(int cardId, String cardDetails) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.edit_card_layout, null);

        EditText editCardNumber = dialogView.findViewById(R.id.dialogEditCardNumber);
        EditText editCardExpirationDate = dialogView.findViewById(R.id.dialogEditExpirationDate);

        // TextWatcher: format card number by grouping digits with spaces.
        editCardNumber.addTextChangedListener(new TextWatcher() {
            String currentFormatted = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                String newText = s.toString();
                if (!newText.equals(currentFormatted)) {
                    String cleanString = newText.replaceAll("\\s", "");
                    StringBuilder formatted = new StringBuilder();
                    for (int i = 0; i < cleanString.length(); i++) {
                        if (i > 0 && i % 4 == 0) {
                            formatted.append(" ");
                        }
                        formatted.append(cleanString.charAt(i));
                    }
                    currentFormatted = formatted.toString();
                    editCardNumber.removeTextChangedListener(this);
                    editCardNumber.setText(currentFormatted);
                    editCardNumber.setSelection(currentFormatted.length());
                    editCardNumber.addTextChangedListener(this);
                }
            }
        });

        // Show month-year picker when expiration date is clicked.
        editCardExpirationDate.setOnClickListener(view -> showMonthYearPicker(editCardExpirationDate));

        // Pre-populate fields from the current card details.
        String[] details = cardDetails.split(" \\(");
        editCardNumber.setText(details[0].trim());
        editCardExpirationDate.setText(details[1].replace(")", "").trim());

        builder.setView(dialogView)
                .setTitle("Edit Card")
                .setPositiveButton("Save", (dialog, which) -> {
                    String updatedCardNumber = editCardNumber.getText().toString();
                    String updatedExpirationDate = editCardExpirationDate.getText().toString();

                    db.updateCard(cardId, updatedCardNumber, updatedExpirationDate);

                    int index = cardsIDList.indexOf(cardId);
                    cardsList.set(index, updatedCardNumber + " (" + updatedExpirationDate + ")");
                    cardsAdapter.notifyDataSetChanged();

                    Toast.makeText(CardsActivity.this, "Card updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    /**
     * Deletes the card from the database and removes it from the displayed list.
     *
     * @param cardId     The unique ID of the card to delete.
     * @param position   The position of the card in the list.
     * @param cardNumber The card number (used for the confirmation message).
     */
    private void deleteCard(int cardId, int position, String cardNumber) {
        // Delete any associated transactions first.
        db.deleteItem(DatabaseHelper.TRANSACTIONS_TABLE, DatabaseHelper.TRANSACTIONS_CARD_ID_FOREIGN_KEY, cardId);
        // Delete the card itself.
        db.deleteItem(DatabaseHelper.CARDS_TABLE, DatabaseHelper.CARDS_ID, cardId);
        cardsList.remove(position);
        cardsIDList.remove(position);
        cardsAdapter.notifyDataSetChanged();
        Toast.makeText(CardsActivity.this, "Card \"" + cardNumber + "\" was deleted.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Displays a month-year picker dialog for selecting a new expiration date.
     *
     * @param editCardExpirationDate The EditText view that should display the selected expiration date.
     */
    private void showMonthYearPicker(EditText editCardExpirationDate) {
        LayoutInflater inflater = LayoutInflater.from(CardsActivity.this);
        View pickerView = inflater.inflate(R.layout.month_year_picker, null);

        NumberPicker monthPicker = pickerView.findViewById(R.id.month_picker);
        NumberPicker yearPicker = pickerView.findViewById(R.id.year_picker);

        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setDisplayedValues(new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"});
        monthPicker.setWrapSelectorWheel(true);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(currentYear);
        yearPicker.setMaxValue(currentYear + 20);
        yearPicker.setWrapSelectorWheel(true);

        AlertDialog.Builder builder = new AlertDialog.Builder(CardsActivity.this);
        builder.setView(pickerView);
        builder.setPositiveButton("OK", (dialog, which) -> {
            int selectedMonth = monthPicker.getValue();
            int selectedYear = yearPicker.getValue();
            String formattedDate = String.format(Locale.getDefault(), "%02d/%d", selectedMonth, selectedYear);
            editCardExpirationDate.setText(formattedDate);
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }
}