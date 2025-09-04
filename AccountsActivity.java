package com.example.financeapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

/**
 * The Accounts activity displays the user’s accounts and provides options to add, edit,
 * or delete an account. This class handles both the user interface and business logic by loading
 * account data from the database, updating the list view, handling context menus, and performing
 * actions based on user interaction.
 */
public class AccountsActivity extends AppCompatActivity {

    // UI components
    private Button accountsReturnButton, newAccountButton;
    private ListView listViewAccounts;

    // Data storage fields for username, database helper, and account data.
    private String username;
    private DatabaseHelper db;

    // Lists to hold account display strings and corresponding IDs.
    private ArrayList<String> accountsList;
    private ArrayAdapter<String> accountsAdapter;
    private ArrayList<Integer> accountsIDList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_accounts);

        // Initialize the UI components.
        initViews();
        initData();
        initListeners();
    }

    /**
     * Binds the UI components defined in the layout to their corresponding instance variables.
     */
    private void initViews() {
        listViewAccounts = findViewById(R.id.listViewAccounts);
        accountsReturnButton = findViewById(R.id.accountsReturnButton);
        newAccountButton = findViewById(R.id.newAccountButton);
    }

    /**
     * Initializes the DatabaseHelper, account data lists and sets up the adapter for the ListView.
     */
    private void initData() {
        // Initialize the DatabaseHelper.
        db = new DatabaseHelper(this);

        // Retrieve the username from SharedPreferences.
        SharedPreferences sharedPreferences = getSharedPreferences("FinanceAppPreferences", Context.MODE_PRIVATE);
        username = sharedPreferences.getString("username", null);

        accountsList = new ArrayList<>();
        accountsIDList = new ArrayList<>();
        accountsAdapter = new ArrayAdapter<>(this, R.layout.list_view_item, accountsList);
        listViewAccounts.setAdapter(accountsAdapter);
        loadAccounts();
    }

    /**
     * Sets up all the listeners for UI components including button clicks, item selection,
     * and long-click context menu registration.
     */
    private void initListeners() {
        // Return to the MainActivity when the return button is clicked.
        accountsReturnButton.setOnClickListener(view -> {
            Intent intent = new Intent(AccountsActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // Open the Cards activity when an account is selected.
        listViewAccounts.setOnItemClickListener((adapterView, view, position, id) -> {
            int accountID = accountsIDList.get(position);
            Intent intent = new Intent(AccountsActivity.this, CardsActivity.class);
            intent.putExtra("accountID", accountID);
            startActivity(intent);
        });

        // Launch the AddAccount activity when the new account button is clicked.
        newAccountButton.setOnClickListener(view -> {
            Intent intent = new Intent(AccountsActivity.this, AddAccountActivity.class);
            startActivity(intent);
        });

        // Register the ListView for a context menu.
        registerForContextMenu(listViewAccounts);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Loads account data from the database for the current user and updates the ListView.
     * For each account, it retrieves details such as name, account type and bank, then
     * calculates the balance dynamically. The final display string for each account
     * is updated to include the current balance.
     */
    private void loadAccounts() {
        Cursor cursor = db.getUserAccounts(username);
        if (cursor != null) {
            try {
                // Get column indices for each required field.
                int accountIDIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.ACCOUNTS_ID);
                int accountNameIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.ACCOUNTS_NAME);
                int accountTypeIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.ACCOUNTS_TYPE);
                int accountBankIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.ACCOUNTS_BANK);

                // Clear previous data in case this method is called multiple times.
                accountsList.clear();
                accountsIDList.clear();

                // Iterate through each record and build the display string including balance.
                if (cursor.moveToFirst()) {
                    do {
                        int accountID = cursor.getInt(accountIDIndex);
                        String accountName = cursor.getString(accountNameIndex);
                        String accountType = cursor.getString(accountTypeIndex);
                        String accountBank = cursor.getString(accountBankIndex);

                        // Calculate the account balance by using the DatabaseHelper function.
                        double balance = db.getAccountBalance(accountID);
                        String formattedBalance = String.format(Locale.getDefault(), "%.2f", balance);

                        // Construct the display string: "AccountName (AccountType) - Bank | Balance: $X.XX"
                        String accountDisplay = accountName + " (" + accountType + ") - " + accountBank +
                                " | Balance: " + formattedBalance;
                        accountsList.add(accountDisplay);
                        accountsIDList.add(accountID);
                    } while (cursor.moveToNext());
                    // Notify the adapter that data has changed.
                    accountsAdapter.notifyDataSetChanged();
                }
            } catch (IllegalArgumentException e) {
                Log.e("Accounts", "Column not found: " + e.getMessage());
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Creates a context menu for account list items with options to edit or delete the account.
     *
     * @param menu The context menu to populate.
     * @param view The view the menu is being created for.
     * @param menuInfo Extra information about the view for which the context menu should be shown.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        menu.setHeaderTitle("Select an option");
        menu.add(0, view.getId(), 0, "Edit");
        menu.add(0, view.getId(), 0, "Delete");
    }

    /**
     * Handles user selections from the context menu, performing either an edit or delete operation.
     *
     * @param item The selected context menu item.
     * @return true after handling the selection.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (info == null) {
            return false;
        }
        int position = info.position;

        if (Objects.equals(item.getTitle(), "Edit")) {
            int accountID = accountsIDList.get(position);
            String accountDetails = accountsList.get(position);
            showEditAccountDialog(accountID, accountDetails);
        } else if (Objects.equals(item.getTitle(), "Delete")) {
            int accountID = accountsIDList.get(position);
            String accountDetails = accountsList.get(position);

            // Extract the account name from the display string.
            String accountName = accountDetails.split(" \\(")[0].trim();

            // Delete associated transactions and cards, then the account itself.
            db.deleteItem(DatabaseHelper.TRANSACTIONS_TABLE, DatabaseHelper.TRANSACTIONS_ACCOUNT_ID_FOREIGN_KEY, accountID);
            db.deleteItem(DatabaseHelper.CARDS_TABLE, DatabaseHelper.CARDS_ACCOUNT_ID_FOREIGN_KEY, accountID);
            db.deleteItem(DatabaseHelper.ACCOUNTS_TABLE, DatabaseHelper.ACCOUNTS_ID, accountID);

            // Remove the account from the lists and update the adapter.
            accountsList.remove(position);
            accountsIDList.remove(position);
            accountsAdapter.notifyDataSetChanged();

            Toast.makeText(AccountsActivity.this, "Account \"" + accountName + "\" was deleted.", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    /**
     * Displays an AlertDialog allowing the user to edit account details.
     *
     * @param accountID      The unique ID of the account to edit.
     * @param accountDetails The current details of the account in the format "AccountName (AccountType) - Bank".
     */
    private void showEditAccountDialog(int accountID, String accountDetails) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.edit_account_layout, null);

        EditText editAccountName = dialogView.findViewById(R.id.editAccountName);
        EditText editAccountType = dialogView.findViewById(R.id.editAccountType);
        EditText editAccountBank = dialogView.findViewById(R.id.editAccountBank);

        // Parse the display string "AccountName (AccountType) - Bank/Organization"
        String[] parts = accountDetails.split(" \\(");
        String accountName = parts[0].trim();
        if (parts.length > 1) {
            String remaining = parts[1];
            String[] subParts = remaining.split("\\) - ");
            if (subParts.length >= 2) {
                String accountType = subParts[0].trim();
                String accountBank = subParts[1].trim().split(" \\| ")[0].trim();
                editAccountName.setText(accountName);
                editAccountType.setText(accountType);
                editAccountBank.setText(accountBank);
            } else {
                editAccountName.setText(accountName);
            }
        } else {
            editAccountName.setText(accountName);
        }

        builder.setView(dialogView)
                .setTitle(getResources().getString(R.string.edit_account))
                .setPositiveButton(getResources().getString(R.string.save), (dialog, which) -> {
                    String newAccountName = editAccountName.getText().toString().trim();
                    String newAccountType = editAccountType.getText().toString().trim();
                    String newAccountBank = editAccountBank.getText().toString().trim();

                    // Update the account information in the database.
                    db.updateAccount(accountID, newAccountName, newAccountType, newAccountBank);

                    double balance = db.getAccountBalance(accountID);
                    String formattedBalance = String.format(Locale.getDefault(), "%.2f", balance);

                    // Update the display string in our list and refresh the adapter.
                    int index = accountsIDList.indexOf(accountID);
                    accountsList.set(index, newAccountName + " (" + newAccountType + ") - " + newAccountBank + " | Balance: " + formattedBalance);
                    accountsAdapter.notifyDataSetChanged();

                    Toast.makeText(AccountsActivity.this, "Account updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getResources().getString(R.string.cancel), (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}