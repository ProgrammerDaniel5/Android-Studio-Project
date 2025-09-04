package com.example.financeapp;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.AlarmManager;
import android.app.PendingIntent;

/**
 * Reports is an activity that displays a list of transactions using a TransactionAdapter,
 * and provides functionalities to add, edit, delete, sort, and filter transactions.
 * It also provides navigation back to MainActivity.
 */
public class ReportsActivity extends AppCompatActivity {

    // UI components.
    private Button reportsReturnButton, reportsAddTransactionButton, sortButton;
    private TextView incomeTextView, expensesTextView;
    private ListView transactionsListview;
    private Spinner filterSpinner, cardsSpinner, typeSpinner, subscriptionIntervalSpinner;

    // Data members.
    private String username;
    private TransactionAdapter transactionAdapter;
    private ArrayList<Transaction> transactionList;
    private DatabaseHelper db;

    private EditText editAmount, editDescription, editDateTime;
    private CheckBox subscriptionCheckBox;
    // Currently selected IDs.
    private int selectedAccountID, selectedCardID;


    // Enums
    private enum FilterType {
        ALL,
        EXPENSES,
        INCOMES,
        PARENT_SUBSCRIPTIONS
    }

    private enum SortOption {
        AMOUNT_HIGH_TO_LOW,
        AMOUNT_LOW_TO_HIGH,
        DATE_NEWEST,
        DATE_OLDEST
    }

    // --- BEGIN: BroadcastReceiver for UI updates ---
    private final BroadcastReceiver transactionsUpdatedReceiver = new BroadcastReceiver() {
        /**
         * Receives broadcast intents when transactions are updated and refreshes the transaction list.
         *
         * @param context The application context.
         * @param intent  The intent containing broadcast data.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshTransactions();
            Log.d("ReportsActivity", "Transactions updated broadcast received; list refreshed.");
        }
    };
    // --- END: BroadcastReceiver for UI updates ---

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
        setContentView(R.layout.activity_reports);

        // Initialize data and UI.
        initData();
        initViews();
        initListeners();
        setupFilterSpinner();

        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(this, transactionList);
        transactionsListview.setAdapter(transactionAdapter);

        // Initialize transaction list and adapter.
        refreshTransactions();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Register the broadcast receiver for transaction updates.
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(transactionsUpdatedReceiver, new IntentFilter("com.example.ZrimaFinanceIt.TRANSACTIONS_UPDATED"));

    }

    /**
     * Called when the activity is destroyed. Performs cleanup operations.
     * Unregisters the broadcast receiver.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the broadcast receiver to avoid leaks.
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(transactionsUpdatedReceiver);
    }

    /**
     * Initializes shared preferences, retrieves the username, and instantiates the database helper.
     */
    private void initData() {
        SharedPreferences sharedPreferences = getSharedPreferences("FinanceAppPreferences", Context.MODE_PRIVATE);
        username = sharedPreferences.getString("username", null);
        db = new DatabaseHelper(this);

        List<Transaction> subscriptions = db.getSubscriptionsReadyToRun(new Date());
        if (!subscriptions.isEmpty()) {
            startImmediateSubscriptionService();
        }
    }

    /**
     * Binds UI views from the layout to member variables.
     */
    private void initViews() {
        reportsReturnButton = findViewById(R.id.reportsReturnButton);
        incomeTextView = findViewById(R.id.incomeTextView);
        expensesTextView = findViewById(R.id.expensesTextView);
        transactionsListview = findViewById(R.id.reportsTransactionsListView);
        reportsAddTransactionButton = findViewById(R.id.reportsAddTransactionButton);
        sortButton = findViewById(R.id.sortButton);
        filterSpinner = findViewById(R.id.filterSpinner);

    }

    /**
     * Sets up listeners for buttons and interactions with the transactions list.
     */
    private void initListeners() {
        reportsReturnButton.setOnClickListener(view -> {
            Intent intent = new Intent(ReportsActivity.this, MainActivity.class);
            startActivity(intent);
        });

        reportsAddTransactionButton.setOnClickListener(view -> newTransaction());

        // Setup long-click for transaction list items for edit and delete.
        transactionsListview.setOnItemLongClickListener((adapterView, view, position, l) -> {
            Transaction transaction = (Transaction) adapterView.getItemAtPosition(position);
            if (transaction == null) {
                Toast.makeText(ReportsActivity.this, getResources().getString(R.string.transaction_not_found), Toast.LENGTH_SHORT).show();
                return true;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(ReportsActivity.this);
            builder.setTitle(getResources().getString(R.string.choose_an_action));
            builder.setItems(new String[]{"Edit", "Delete"}, (dialog, which) -> {
                switch (which) {
                    case 0: // Edit
                        showEditDialog(transaction);
                        break;
                    case 1: // Delete
                        // If the transaction is a subscription, confirm deletion.
                        if (transaction.isSubscription()) {
                            new AlertDialog.Builder(ReportsActivity.this)
                                    .setTitle(getResources().getString(R.string.delete_subscription))
                                    .setMessage(getResources().getString(R.string.deleting_this_subscription_will_))
                                    .setPositiveButton(getResources().getString(R.string.yes), (dialog1, which1) -> {
                                        if (db.deleteSubscriptionWithTransactions(transaction.getTransactionId())) {
                                            updateSubscriptionServiceInterval(ReportsActivity.this);
                                            refreshTransactions();
                                            Toast.makeText(ReportsActivity.this, getResources().getString(R.string.subscription_and_its_sub_subscriptions_were_deleted), Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(ReportsActivity.this, getResources().getString(R.string.failed_to_delete_subscription), Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .setNegativeButton(getResources().getString(R.string.no), null)
                                    .create()
                                    .show();
                        } else {
                            if (db.deleteTransaction(transaction.getTransactionId())) {
                                refreshTransactions();
                                Toast.makeText(ReportsActivity.this, getResources().getString(R.string.transaction_deleted), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ReportsActivity.this, getResources().getString(R.string.failed_to_delete_transaction), Toast.LENGTH_SHORT).show();
                            }
                        }
                        break;
                }
            });
            builder.create().show();
            return true;
        });

        sortButton.setOnClickListener(view -> showSortDialog());
    }

    /**
     * Sets up the filter spinner adapter and listener.
     */
    private void setupFilterSpinner() {
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, R.layout.spinner_item,
                getResources().getStringArray(R.array.transaction_filters));
        filterAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        filterSpinner.setAdapter(filterAdapter);
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Map the spinner index to the FilterType enum.
                FilterType selectedFilter = FilterType.values()[position];
                filterTransactions(selectedFilter);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed.
            }
        });
    }

    /**
     * Launches the "New Transaction" dialog for creating a new transaction.
     */
    private void newTransaction() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.transaction_layout, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Disable cards spinner until an account is selected.
        cardsSpinner = dialogView.findViewById(R.id.transactionCardSpinner);
        cardsSpinner.setEnabled(false);

        // Populate the accounts spinner.
        ArrayList<String> accountsList = new ArrayList<>();
        ArrayList<Integer> accountsIDList = new ArrayList<>();
        Cursor cursor = db.getUserAccounts(username);
        if (cursor != null) {
            try {
                int accountIDIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.ACCOUNTS_ID);
                int accountNameIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.ACCOUNTS_NAME);
                int accountTypeIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.ACCOUNTS_TYPE);
                if (cursor.moveToFirst()) {
                    do {
                        int accountID = cursor.getInt(accountIDIndex);
                        String accountName = cursor.getString(accountNameIndex);
                        String accountType = cursor.getString(accountTypeIndex);
                        accountsList.add(accountName + " (" + accountType + ")");
                        accountsIDList.add(accountID);
                    } while (cursor.moveToNext());
                }
            } catch (IllegalArgumentException e) {
                Log.e("Accounts", "Column not found: " + e.getMessage());
            } finally {
                cursor.close();
            }
        }
        // UI elements for the new transaction dialog.
        Spinner accountsSpinner = dialogView.findViewById(R.id.transactionAccountSpinner);
        ArrayAdapter<String> accountsAdapter = new ArrayAdapter<>(ReportsActivity.this, R.layout.spinner_item, accountsList);
        accountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accountsSpinner.setAdapter(accountsAdapter);

        accountsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                selectedAccountID = accountsIDList.get(position);
                Log.d("Selected Account ID", "Account ID: " + selectedAccountID);
                loadCardsForAccount(selectedAccountID);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // No action needed.
            }
        });

        // Transaction type spinner with Income and Expense options.
        typeSpinner = dialogView.findViewById(R.id.transactionTypeSpinner);
        ArrayList<String> typeOptions = new ArrayList<>(Arrays.asList("Select a type", "Income", "Expense"));
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, typeOptions);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        Spinner transactionCategorySpinner = dialogView.findViewById(R.id.transactionCategorySpinner);
        ArrayList<String> categoryOptions = new ArrayList<>(Arrays.asList(
                "Select a category",
                "Work",
                "Business",
                "Groceries",
                "Fun",
                "Travel",
                "Bills",
                "Education",
                "Health",
                "Miscellaneous"
        ));
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, categoryOptions);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        transactionCategorySpinner.setAdapter(categoryAdapter);
        transactionCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = categoryOptions.get(position);
                Log.d("TransactionCategory", "Selected category: " + selectedCategory);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Bind and set up other transaction fields.
        editAmount = dialogView.findViewById(R.id.editTransactionAmount);
        editDescription = dialogView.findViewById(R.id.editTransactionDescription);
        editDateTime = dialogView.findViewById(R.id.editTransactionDateTime);
        editDateTime.setOnClickListener(view -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    ReportsActivity.this,
                    (datePicker, year, month, dayOfMonth) -> {
                        TimePickerDialog timePickerDialog = new TimePickerDialog(
                                ReportsActivity.this,
                                (timePicker, hourOfDay, minute) -> editDateTime.setText(String.format(Locale.getDefault(), "%d/%d/%d %d:%02d", dayOfMonth, month + 1, year, hourOfDay, minute)),
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true);
                        timePickerDialog.show();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        subscriptionCheckBox = dialogView.findViewById(R.id.subscriptionCheckBox);
        subscriptionIntervalSpinner = dialogView.findViewById(R.id.subscriptionIntervalSpinner);
        ArrayList<String> intervalOptions = new ArrayList<>(Arrays.asList("Select an interval", "Minutely", "Daily", "Weekly", "Monthly", "Yearly"));
        ArrayAdapter<String> intervalAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, intervalOptions);
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        subscriptionIntervalSpinner.setAdapter(intervalAdapter);
        // Show/hide interval spinner based on checkbox state.
        subscriptionCheckBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                subscriptionIntervalSpinner.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        // Setup save and return buttons for the dialog.
        Button buttonSaveTransaction = dialogView.findViewById(R.id.saveTransactionButton);
        Button transactionLayoutReturnButton = dialogView.findViewById(R.id.transactionLayoutReturnButton);
        transactionLayoutReturnButton.setOnClickListener(view -> dialog.dismiss());
        buttonSaveTransaction.setOnClickListener(view -> {
            if (accountsIDList.isEmpty()) {
                Toast.makeText(ReportsActivity.this, getResources().getString(R.string.you_need_to_create_a_financial_account_first), Toast.LENGTH_SHORT).show();
                return;
            }
            String amountText = editAmount.getText().toString();
            if (TextUtils.isEmpty(amountText)) {
                Toast.makeText(ReportsActivity.this, getResources().getString(R.string.select_a_valid_amount), Toast.LENGTH_SHORT).show();
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(amountText);
            } catch (NumberFormatException e) {
                Toast.makeText(ReportsActivity.this, getResources().getString(R.string.invalid_amount), Toast.LENGTH_SHORT).show();
                return;
            }
            String type = typeSpinner.getSelectedItem().toString();
            if (type.equals("Select a type")) {
                Toast.makeText(ReportsActivity.this, getResources().getString(R.string.select_a_valid_type), Toast.LENGTH_SHORT).show();
                return;
            }
            String category = transactionCategorySpinner.getSelectedItem().toString();
            if (category.equals("Select a category")) {
                Toast.makeText(ReportsActivity.this, getResources().getString(R.string.select_a_valid_category), Toast.LENGTH_SHORT).show();
                return;
            }
            String datetime = editDateTime.getText().toString();
            if (TextUtils.isEmpty(datetime)) {
                Toast.makeText(ReportsActivity.this, getResources().getString(R.string.select_a_date_and_a_time), Toast.LENGTH_SHORT).show();
                return;
            }
            boolean isSubscription = subscriptionCheckBox.isChecked();
            String interval = "";
            if (isSubscription) {
                interval = subscriptionIntervalSpinner.getSelectedItem().toString();
                if (interval.equals("Select an interval")) {
                    Toast.makeText(ReportsActivity.this, getResources().getString(R.string.select_a_valid_interval), Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            String description = editDescription.getText().toString();
            // Add the transaction (selectedCardID assumed to be set via loadCardsForAccount).
            if (db.addTransaction(selectedAccountID, selectedCardID, amount, type, category, description, datetime, isSubscription, interval, null)) {
                refreshTransactions();
                Toast.makeText(ReportsActivity.this, getResources().getString(R.string.transaction_saved), Toast.LENGTH_SHORT).show();
                if (isSubscription) {
                    // Schedule future repeats.
                    startSubscriptionService(interval);
                    // And also trigger the service immediately to spawn missed transactions.
                    startImmediateSubscriptionService();
                }
            } else {
                Toast.makeText(ReportsActivity.this, getResources().getString(R.string.transaction_failed), Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
    }

    /**
     * Loads cards for the given account and populates the cards spinner.
     *
     * @param accountID The selected account ID.
     */
    private void loadCardsForAccount(int accountID) {
        ArrayList<String> cardsList = new ArrayList<>();
        ArrayList<Integer> cardsIDList = new ArrayList<>();
        Cursor cursor = db.getUserCards(accountID);
        if (cursor != null) {
            try {
                int cardIDIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.CARDS_ID);
                int cardNumberIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.CARDS_NUMBER);
                if (cursor.moveToFirst()) {
                    do {
                        int cardID = cursor.getInt(cardIDIndex);
                        String cardNumber = cursor.getString(cardNumberIndex);
                        cardsList.add(cardNumber);
                        cardsIDList.add(cardID);
                    } while (cursor.moveToNext());
                }
            } catch (IllegalArgumentException e) {
                Log.e("Cards", "Column not found: " + e.getMessage());
            } finally {
                cursor.close();
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(ReportsActivity.this, R.layout.spinner_item, cardsList);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        cardsSpinner.setAdapter(adapter);
        cardsSpinner.setEnabled(true);
        if (!cardsIDList.isEmpty()) {
            cardsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    selectedCardID = cardsIDList.get(position);
                    Log.d("Selected Card ID", "Card ID: " + selectedCardID);
                }
                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    // No action needed.
                }
            });
        } else {
            selectedCardID = -1;
        }
    }

    /**
     * Displays a dialog to edit an existing transaction.
     *
     * @param transaction The transaction to edit.
     */
    private void showEditDialog(Transaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ReportsActivity.this);
        View dialogView = getLayoutInflater().inflate(R.layout.transaction_layout, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Bind editable fields and set initial values.
        EditText editAmount = dialogView.findViewById(R.id.editTransactionAmount);
        EditText editDescription = dialogView.findViewById(R.id.editTransactionDescription);
        EditText editDateTime = dialogView.findViewById(R.id.editTransactionDateTime);
        editAmount.setText(String.valueOf(transaction.getAmount()));
        editDescription.setText(transaction.getDescription());
        editDateTime.setText(transaction.getDate());

        // Subscription settings.
        CheckBox subscriptionCheckBox = dialogView.findViewById(R.id.subscriptionCheckBox);
        Spinner subscriptionIntervalSpinner = dialogView.findViewById(R.id.subscriptionIntervalSpinner);
        ArrayList<String> intervalOptions = new ArrayList<>(Arrays.asList("Select an interval", "Minutely", "Daily", "Weekly", "Monthly", "Yearly"));
        ArrayAdapter<String> subscriptionIntervalAdapter = new ArrayAdapter<>(ReportsActivity.this, R.layout.spinner_item, intervalOptions);
        subscriptionIntervalAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        subscriptionIntervalSpinner.setAdapter(subscriptionIntervalAdapter);
        subscriptionCheckBox.setChecked(transaction.isSubscription());

        if (transaction.isSubscription()) {
            subscriptionIntervalSpinner.setVisibility(View.VISIBLE);
            String currentInterval = transaction.getInterval();
            if (currentInterval != null) {
                int pos = subscriptionIntervalAdapter.getPosition(currentInterval);
                if (pos >= 0) {
                    subscriptionIntervalSpinner.setSelection(pos);
                }
            }
        } else {
            subscriptionIntervalSpinner.setVisibility(View.GONE);
        }

        subscriptionCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> subscriptionIntervalSpinner.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        // Set up date-time editing.
        // If this transaction is a subscription, disable the DateTime field.
        if (transaction.isSubscription()) {
            editDateTime.setEnabled(false);
            editDateTime.setAlpha(0.5f);
            editDateTime.setClickable(false);
        } else {
            editDateTime.setEnabled(true);
            editDateTime.setAlpha(1.0f);
            editDateTime.setClickable(true);
            editDateTime.setOnClickListener(view -> {
                Calendar calendar = Calendar.getInstance();
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        ReportsActivity.this,
                        (datePicker, year, month, day) -> {
                            TimePickerDialog timePickerDialog = new TimePickerDialog(
                                    ReportsActivity.this,
                                    (timePicker, hourOfDay, minute) -> editDateTime.setText(String.format(Locale.getDefault(), "%d/%d/%d %d:%02d", day, month + 1, year, hourOfDay, minute)),
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true);
                            timePickerDialog.show();
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
            });
        }

        // Setup return button.
        Button transactionLayoutReturnButton = dialogView.findViewById(R.id.transactionLayoutReturnButton);
        transactionLayoutReturnButton.setOnClickListener(view -> dialog.dismiss());

        // Save button.
        Button buttonSaveTransaction = dialogView.findViewById(R.id.saveTransactionButton);
        buttonSaveTransaction.setOnClickListener(view -> {
            String amountText = editAmount.getText().toString();
            if (TextUtils.isEmpty(amountText)) {
                Toast.makeText(ReportsActivity.this, getResources().getString(R.string.enter_a_valid_amount), Toast.LENGTH_SHORT).show();
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(amountText);
            } catch (NumberFormatException e) {
                Toast.makeText(ReportsActivity.this, getResources().getString(R.string.invalid_amount_entered), Toast.LENGTH_SHORT).show();
                return;
            }
            String description = editDescription.getText().toString();
            String datetime = editDateTime.getText().toString();
            boolean isSubscription = subscriptionCheckBox.isChecked();
            String interval = "none";
            if (isSubscription) {
                interval = subscriptionIntervalSpinner.getSelectedItem().toString();
                if (interval.equalsIgnoreCase("Select an interval")) {
                    Toast.makeText(ReportsActivity.this, getResources().getString(R.string.select_a_valid_interval), Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            // Update transaction.
            transaction.setAmount(amount);
            transaction.setDescription(description);
            transaction.setDate(datetime);
            transaction.setSubscription(isSubscription);
            boolean flag = !transaction.getInterval().equals(interval);
            transaction.setInterval(isSubscription ? interval : "none");


            if (db.updateTransaction(transaction, flag)) {
                refreshTransactions();
                Toast.makeText(ReportsActivity.this, getResources().getString(R.string.transaction_updated), Toast.LENGTH_SHORT).show();
                updateSubscriptionServiceInterval(ReportsActivity.this);
            } else {
                Toast.makeText(ReportsActivity.this, getResources().getString(R.string.update_failed), Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
    }

    /**
     * Starts the subscription service based on the provided interval.
     * This method sets up an alarm to trigger the {@link SubscriptionService} at regular intervals.
     * It ensures that the subscription service runs at the specified
     * frequency, using {@link PendingIntent} to manage execution.
     * @param interval The subscription interval ("minutely", "daily", "weekly", "monthly", or "yearly").
     */
    private void startSubscriptionService(String interval) {
        // Check if a valid interval is provided, otherwise log and exit.
        if (interval.equalsIgnoreCase("none")) {
            Log.d("SubscriptionService", "No valid interval provided for subscription.");
            return;
        }
        try {
            // Get the system AlarmManager to schedule periodic execution.
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            // Create an Intent targeting the SubscriptionService.
            Intent alarmIntent = new Intent(this, SubscriptionService.class);

            // Wrap the intent in a PendingIntent to allow AlarmManager execution.
            PendingIntent pendingIntent = PendingIntent.getService(
                    this,
                    0,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Initialize calendar to set the first scheduled trigger time.
            Calendar calendar = Calendar.getInstance();
            long intervalMillis;

            // Determine the time interval for the repeating alarm.
            switch (interval) {
                case "Minutely":
                    calendar.add(Calendar.MINUTE, 1);
                    intervalMillis = 60 * 1000;
                    break;
                case "Daily":
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    intervalMillis = AlarmManager.INTERVAL_DAY;
                    break;
                case "Weekly":
                    calendar.add(Calendar.WEEK_OF_YEAR, 1);
                    intervalMillis = AlarmManager.INTERVAL_DAY * 7;
                    break;
                case "Monthly":
                    calendar.add(Calendar.MONTH, 1);
                    intervalMillis = calendar.getTimeInMillis() - System.currentTimeMillis();
                    break;
                case "Yearly":
                    calendar.add(Calendar.YEAR, 1);
                    intervalMillis = calendar.getTimeInMillis() - System.currentTimeMillis();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported interval: " + interval);
            }

            startNotificationAlarm(interval);
            // Schedule the repeating alarm using AlarmManager.
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP, // Ensures the Android device wakes up to execute the scheduled service.
                    calendar.getTimeInMillis(), // First execution time.
                    intervalMillis, // Repeat interval.
                    pendingIntent // PendingIntent to invoke the service.
            );
            Log.d("SubscriptionService", "Subscription scheduled with interval: " + interval +
                    ", first trigger at: " + calendar.getTimeInMillis());
        } catch (Exception e) {
            Log.e("SubscriptionService", "Error setting up subscription: " + e.getMessage(), e);
        }
    }

    /**
     * Starts the immediate subscription service to spawn the missed transactions.
     * Creates an intent for {@link SubscriptionService} and starts the service using {@code startService()}.
     * Logs an error if the service fails to start.
     */
    private void startImmediateSubscriptionService() {
        try {
            Intent immediateIntent = new Intent(this, SubscriptionService.class);
            // Start the service immediately.
            startService(immediateIntent);
            startNotificationAlarm(db.getShortestSubscriptionInterval());
            Log.d("SubscriptionService", "Immediate subscription service started via startService()");
        } catch (Exception e) {
            Log.e("SubscriptionService", "Error starting immediate service: " + e.getMessage(), e);
        }
    }

    /**
     * Cancels the subscription service.
     *
     * @param context The context used to get the AlarmManager.
     */
    private void cancelSubscriptionService(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, SubscriptionService.class);
        PendingIntent pendingIntent = PendingIntent.getService(
                context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
        cancelNotificationAlarm();
        Log.d("SubscriptionService", "AlarmManager canceled, service will no longer run.");
    }

    /**
     * Updates the subscription service interval based on the shortest active subscription.
     *
     * @param context The application context.
     */
    private void updateSubscriptionServiceInterval(Context context) {
        try (DatabaseHelper db = new DatabaseHelper(context)) {
            String shortestInterval = db.getShortestSubscriptionInterval();
            if (shortestInterval == null) {
                cancelSubscriptionService(context);
                return;
            }
            cancelNotificationAlarm();
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent alarmIntent = new Intent(context, SubscriptionService.class);
            PendingIntent pendingIntent = PendingIntent.getService(
                    context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            long intervalMillis;
            switch (shortestInterval) {
                case "Minutely":
                    intervalMillis = 60 * 1000;
                    break;
                case "Daily":
                    intervalMillis = AlarmManager.INTERVAL_DAY;
                    break;
                case "Weekly":
                    intervalMillis = AlarmManager.INTERVAL_DAY * 7;
                    break;
                case "Monthly":
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MONTH, 1);
                    intervalMillis = calendar.getTimeInMillis() - System.currentTimeMillis();
                    break;
                case "Yearly":
                    Calendar yearlyCalendar = Calendar.getInstance();
                    yearlyCalendar.add(Calendar.YEAR, 1);
                    intervalMillis = yearlyCalendar.getTimeInMillis() - System.currentTimeMillis();
                    break;
                default:
                    Log.e("SubscriptionService", "Unsupported interval: " + shortestInterval);
                    return;
            }
            startNotificationAlarm(shortestInterval);
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis(),
                    intervalMillis,
                    pendingIntent
            );
            Log.d("SubscriptionService", "AlarmManager updated to shortest interval: " + shortestInterval);
        } catch (Exception e) {
            Log.e("SubscriptionService", "Error updating subscription interval", e);
        }
    }

    /**
     * Starts an alarm that triggers notifications using SubscriptionAlarmReceiver.
     *
     * @param interval The notification interval ("Minutely", "Daily", "Weekly", "Monthly", or "Yearly").
     */
    private void startNotificationAlarm(String interval) {
        if (interval.equalsIgnoreCase("none")) {
            Log.d("NotificationAlarm", "No valid interval provided for notifications.");
            return;
        }
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            Intent alarmIntent = new Intent(this, SubscriptionAlarmReceiver.class);
            alarmIntent.putExtra("transactionMessage", "Your scheduled subscription transaction has been processed!");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    1,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Calendar calendar = Calendar.getInstance();
            long intervalMillis;
            switch (interval) {
                case "Minutely":
                    calendar.add(Calendar.MINUTE, 1);
                    intervalMillis = 60 * 1000;
                    break;
                case "Daily":
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    intervalMillis = AlarmManager.INTERVAL_DAY;
                    break;
                case "Weekly":
                    calendar.add(Calendar.WEEK_OF_YEAR, 1);
                    intervalMillis = AlarmManager.INTERVAL_DAY * 7;
                    break;
                case "Monthly":
                    calendar.add(Calendar.MONTH, 1);
                    intervalMillis = calendar.getTimeInMillis() - System.currentTimeMillis();
                    break;
                case "Yearly":
                    calendar.add(Calendar.YEAR, 1);
                    intervalMillis = calendar.getTimeInMillis() - System.currentTimeMillis();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported interval: " + interval);
            }

            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    intervalMillis,
                    pendingIntent
            );
            Log.d("NotificationAlarm", "Notification alarm scheduled with interval: " + interval +
                    ", first trigger at: " + calendar.getTimeInMillis());
        } catch (Exception e) {
            Log.e("NotificationAlarm", "Error setting up notification alarm: " + e.getMessage(), e);
        }
    }

    /**
     * Cancels the notification alarm.
     */
    private void cancelNotificationAlarm() {
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent alarmIntent = new Intent(this, SubscriptionAlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    1,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(pendingIntent);
            Log.d("NotificationAlarm", "Notification alarm canceled.");
        } catch (Exception e) {
            Log.e("NotificationAlarm", "Error canceling notification alarm: " + e.getMessage(), e);
        }
    }

    /**
     * Filters transactions based on the provided filter string.
     *
     * @param filter The filter option ("Expenses", "Income", "Parent Subscriptions", or "All").
     */
    private void filterTransactions(FilterType filter) {
        ArrayList<Transaction> fullList = db.getTransactionsFromDB(username);
        ArrayList<Transaction> filteredList = new ArrayList<>();
        for (Transaction transaction : fullList) {
            switch (filter) {
                case EXPENSES:
                    if ("Expense".equals(transaction.getType())) {
                        filteredList.add(transaction);
                    }
                    break;
                case INCOMES:
                    if ("Income".equals(transaction.getType())) {
                        filteredList.add(transaction);
                    }
                    break;
                case PARENT_SUBSCRIPTIONS:
                    if (transaction.isSubscription()) {
                        filteredList.add(transaction);
                    }
                    break;
                case ALL:
                default:
                    filteredList.add(transaction);
                    break;
            }
        }
        transactionList = filteredList;
        transactionAdapter.updateTransactions(filteredList);
        transactionAdapter.notifyDataSetChanged();
    }

    /**
     * Displays a sort options dialog and sorts transactions based on the selected option.
     */
    private void showSortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.sort_by_title));
        String[] options = getResources().getStringArray(R.array.sort_options);
        builder.setItems(options, (dialog, which) -> {
            SortOption selectedSort = SortOption.values()[which];
            sortTransactions(selectedSort);
        });
        builder.create().show();
    }

    /**
     * Sorts transactions based on the specified sort option.
     *
     * @param sortOption The sort option (e.g., "Amount (High to Low)", "Date (Newest)", etc.).
     */
    private void sortTransactions(SortOption sortOption) {
        ArrayList<Transaction> sortedList = new ArrayList<>(transactionList);
        final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        switch (sortOption) {
            case AMOUNT_HIGH_TO_LOW:
                sortedList.sort((t1, t2) -> Double.compare(t2.getAmount(), t1.getAmount()));
                break;
            case AMOUNT_LOW_TO_HIGH:
                sortedList.sort(Comparator.comparingDouble(Transaction::getAmount));
                break;
            case DATE_NEWEST:
                sortedList.sort((t1, t2) -> {
                    try {
                        Date date1 = formatter.parse(t1.getDate());
                        Date date2 = formatter.parse(t2.getDate());
                        if (date1 == null && date2 == null) return 0;
                        if (date1 == null) return 1;
                        if (date2 == null) return -1;
                        return date2.compareTo(date1);
                    } catch (ParseException e) {
                        e.printStackTrace();
                        return 0;
                    }
                });
                break;
            case DATE_OLDEST:
                sortedList.sort((t1, t2) -> {
                    try {
                        Date date1 = formatter.parse(t1.getDate());
                        Date date2 = formatter.parse(t2.getDate());
                        if (date1 == null && date2 == null) return 0;
                        if (date1 == null) return 1;
                        if (date2 == null) return -1;
                        return date1.compareTo(date2);
                    } catch (ParseException e) {
                        e.printStackTrace();
                        return 0;
                    }
                });
                break;
        }
        transactionAdapter.updateTransactions(sortedList);
        transactionAdapter.notifyDataSetChanged();
    }

    /**
     * Updates the displayed total incomes and total expenses based on the current user's transactions.
     * Retrieves the totals using the sumUserIncome and sumUserExpenses methods from the database.
     */
    private void updateTotals() {
        double totalIncome = db.sumUserIncomes(username);
        double totalExpenses = db.sumUserExpenses(username);
        String incomeMessage = getResources().getString(R.string.income) + " " + String.format(Locale.getDefault(), "%,.2f", totalIncome);
        incomeTextView.setText(incomeMessage);
        String expensesMessage = getResources().getString(R.string.expenses) + " " +String.format(Locale.getDefault(), "%,.2f", totalExpenses);
        expensesTextView.setText(expensesMessage);

        incomeTextView.setTextColor(getResources().getColor(R.color.income_green, null));
        expensesTextView.setTextColor(getResources().getColor(R.color.expense_red, null));
    }

    /**
     * Refreshes the transaction list by re-fetching from the database and reapplying the active filter.
     */
    private void refreshTransactions() {
        transactionList = db.getTransactionsFromDB(username);
        int selectedPosition = filterSpinner.getSelectedItemPosition();
        FilterType currentFilter = FilterType.values()[selectedPosition];
        filterTransactions(currentFilter);
        updateTotals();
    }

}