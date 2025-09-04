package com.example.financeapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * DatabaseHelper is responsible for managing the SQLite database for the application.
 * It creates/updates tables for users, goals, accounts, cards, and transactions, and provides
 * comprehensive methods to access and modify the stored data.
 * Helper methods include inserting and updating users, retrieving user items,
 * performing operations on goals, accounts, cards, and transactions, as well as
 * specialized methods for handling recurring subscription transactions.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    //=======================================================================
    // Database configuration constants.
    //=======================================================================
    public static final String DATABASE_NAME = "UserDB.db";
    public static final int DATABASE_VERSION = 1;

    //=======================================================================
    // Users table constants.
    //=======================================================================
    public static final String USERS_TABLE = "users";
    public static final String USERS_USERNAME = "USERNAME";
    public static final String USERS_FIRST_NAME = "FIRSTNAME";
    public static final String USERS_LAST_NAME = "LASTNAME";
    public static final String USERS_BIRTH_DATE = "BIRTHDATE";
    public static final String USERS_PASSWORD = "PASSWORD";

    //=======================================================================
    // Goals table constants.
    //=======================================================================
    public static final String GOALS_TABLE = "Goals";
    public static final String GOALS_ID = "GOAL_ID";
    public static final String GOALS_NAME = "GOAL_NAME";
    public static final String GOALS_DESCRIPTION = "GOAL_DESCRIPTION";
    public static final String GOALS_AMOUNT = "GOAL_AMOUNT";
    public static final String GOALS_CATEGORY = "GOAL_CATEGORY";
    public static final String GOALS_DUE_DATE = "GOAL_DUE_DATE";
    public static final String GOALS_VOICE_NOTE_PATH = "VOICE_NOTE_PATH";
    public static final String GOALS_USERNAME_FOREIGN_KEY = "USERNAME";

    //=======================================================================
    // Accounts table constants.
    //=======================================================================
    public static final String ACCOUNTS_TABLE = "accounts";
    public static final String ACCOUNTS_ID = "ACCOUNT_ID";
    public static final String ACCOUNTS_NAME = "ACCOUNT_NAME";
    public static final String ACCOUNTS_TYPE = "ACCOUNT_TYPE";
    public static final String ACCOUNTS_BANK = "ACCOUNT_BANK";
    public static final String ACCOUNTS_USERNAME_FOREIGN_KEY = "USER_ID";

    //=======================================================================
    // Cards table constants.
    //=======================================================================
    public static final String CARDS_TABLE = "cards";
    public static final String CARDS_ID = "CARD_ID";
    public static final String CARDS_NUMBER = "CARD_NUMBER";
    public static final String CARDS_EXPIRATION_DATE = "CARD_EXPIRATION_DATE";
    public static final String CARDS_ACCOUNT_ID_FOREIGN_KEY = "ACCOUNT_ID";

    //=======================================================================
    // Transactions table constants.
    //=======================================================================
    public static final String TRANSACTIONS_TABLE = "transactions";
    public static final String TRANSACTIONS_ID = "TRANSACTION_ID";
    public static final String TRANSACTIONS_AMOUNT = "TRANSACTION_AMOUNT";
    public static final String TRANSACTIONS_TYPE = "TRANSACTION_TYPE";
    public static final String TRANSACTIONS_CATEGORY = "TRANSACTION_CATEGORY";
    public static final String TRANSACTIONS_DESCRIPTION = "TRANSACTIONS_DESCRIPTION";
    public static final String TRANSACTIONS_DATETIME = "TRANSACTION_DATE";
    public static final String TRANSACTIONS_ACCOUNT_ID_FOREIGN_KEY = "ACCOUNT_ID";
    public static final String TRANSACTIONS_CARD_ID_FOREIGN_KEY = "CARD_ID";
    public static final String TRANSACTIONS_IS_SUBSCRIPTION = "TRANSACTION_IS_SUBSCRIPTION";
    public static final String TRANSACTIONS_INTERVAL = "TRANSACTION_INTERVAL";
    public static final String TRANSACTIONS_NEXT_RUN = "TRANSACTION_NEXT_RUN";
    public static final String PARENT_SUBSCRIPTION_ID = "PARENT_SUBSCRIPTION_ID";

    //=======================================================================
    // Constructor
    //=======================================================================

    /**
     * Constructs a new DatabaseHelper.
     *
     * @param context the Context in which the database is operating.
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //=======================================================================
    // onCreate, onUpgrade, onConfigure
    //=======================================================================

    /**
     * Called when the database is created for the first time.
     * This method creates the Users, Goals, Accounts, Cards, and Transactions tables.
     *
     * @param db the SQLiteDatabase in which to create the tables.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Users table.
        String createUsersTable = "CREATE TABLE " + USERS_TABLE + " (" +
                USERS_USERNAME + " TEXT PRIMARY KEY, " +
                USERS_PASSWORD + " TEXT, " +
                USERS_FIRST_NAME + " TEXT, " +
                USERS_LAST_NAME + " TEXT, " +
                USERS_BIRTH_DATE + " DATE)";
        db.execSQL(createUsersTable);

        // Create Goals table.
        String createGoalsTable = "CREATE TABLE " + GOALS_TABLE + " (" +
                GOALS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                GOALS_NAME + " TEXT NOT NULL, " +
                GOALS_DESCRIPTION + " TEXT, " +
                GOALS_AMOUNT + " REAL, " +
                GOALS_CATEGORY + " TEXT, " +
                GOALS_DUE_DATE + " DATETIME, " +
                GOALS_VOICE_NOTE_PATH + " TEXT, " +
                GOALS_USERNAME_FOREIGN_KEY + " TEXT, " +
                "FOREIGN KEY(" + GOALS_USERNAME_FOREIGN_KEY + ") REFERENCES " +
                USERS_TABLE + "(" + USERS_USERNAME + "))";
        db.execSQL(createGoalsTable);

        // Create Accounts table.
        String createAccountsTable = "CREATE TABLE " + ACCOUNTS_TABLE + " (" +
                ACCOUNTS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ACCOUNTS_NAME + " TEXT, " +
                ACCOUNTS_TYPE + " TEXT, " +
                ACCOUNTS_BANK + " TEXT, " +
                ACCOUNTS_USERNAME_FOREIGN_KEY + " TEXT, " +
                "FOREIGN KEY(" + ACCOUNTS_USERNAME_FOREIGN_KEY + ") REFERENCES " +
                USERS_TABLE + "(" + USERS_USERNAME + "))";
        db.execSQL(createAccountsTable);

        // Create Cards table.
        String createCardsTable = "CREATE TABLE " + CARDS_TABLE + " (" +
                CARDS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                CARDS_NUMBER + " TEXT, " +
                CARDS_EXPIRATION_DATE + " TEXT, " +
                CARDS_ACCOUNT_ID_FOREIGN_KEY + " INTEGER, " +
                "FOREIGN KEY(" + CARDS_ACCOUNT_ID_FOREIGN_KEY + ") REFERENCES " +
                ACCOUNTS_TABLE + "(" + ACCOUNTS_ID + ") ON UPDATE CASCADE)";
        db.execSQL(createCardsTable);

        // Create Transactions table.
        String createTransactionsTable = "CREATE TABLE " + TRANSACTIONS_TABLE + " (" +
                TRANSACTIONS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TRANSACTIONS_AMOUNT + " REAL, " +
                TRANSACTIONS_TYPE + " TEXT, " +
                TRANSACTIONS_CATEGORY + " TEXT, " +
                TRANSACTIONS_DESCRIPTION + " TEXT, " +
                TRANSACTIONS_DATETIME + " TEXT, " +
                TRANSACTIONS_ACCOUNT_ID_FOREIGN_KEY + " INTEGER, " +
                TRANSACTIONS_CARD_ID_FOREIGN_KEY + " INTEGER, " +
                TRANSACTIONS_IS_SUBSCRIPTION + " BOOLEAN, " +
                TRANSACTIONS_INTERVAL + " TEXT, " +
                TRANSACTIONS_NEXT_RUN + " TEXT, " +
                PARENT_SUBSCRIPTION_ID + " INTEGER, " +
                "FOREIGN KEY(" + TRANSACTIONS_ACCOUNT_ID_FOREIGN_KEY + ") REFERENCES " +
                ACCOUNTS_TABLE + "(" + ACCOUNTS_ID + ") ON UPDATE CASCADE, " +
                "FOREIGN KEY(" + TRANSACTIONS_CARD_ID_FOREIGN_KEY + ") REFERENCES " +
                CARDS_TABLE + "(" + CARDS_ID + ") ON UPDATE CASCADE, " +
                "FOREIGN KEY(" + PARENT_SUBSCRIPTION_ID + ") REFERENCES " +
                TRANSACTIONS_TABLE + "(" + TRANSACTIONS_ID + ") ON DELETE SET NULL)";
        db.execSQL(createTransactionsTable);
    }

    /**
     * Called when the database needs to be upgraded.
     * For simplicity, this implementation drops existing tables and calls onCreate() again.
     *
     * @param db the SQLiteDatabase.
     * @param oldVersion the old database version.
     * @param newVersion the new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + USERS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + GOALS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + ACCOUNTS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + CARDS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + TRANSACTIONS_TABLE);
        onCreate(db);
    }

    /**
     * Called before onCreate or onUpgrade, this method enables foreign key constraints.
     *
     * @param db the SQLiteDatabase.
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    //=========================================================================================
    // Users methods
    //=========================================================================================

    /**
     * Inserts a new user into the Users table.
     *
     * @param firstName the user's first name.
     * @param lastName the user's last name.
     * @param birthDate the user's birthdate.
     * @param username the username.
     * @param password the user's password.
     * @return true if the insertion was successful, false otherwise.
     */
    public boolean insertUser(String firstName, String lastName, String birthDate,
                              String username, String password) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(USERS_FIRST_NAME, firstName);
            values.put(USERS_LAST_NAME, lastName);
            values.put(USERS_BIRTH_DATE, birthDate);
            values.put(USERS_USERNAME, username);
            values.put(USERS_PASSWORD, password);
            long result = db.insert(USERS_TABLE, null, values);
            return result != -1;
        }
    }

    /**
     * Checks if the provided username exists in the Users table.
     *
     * @param username the username to check.
     * @return true if the username exists, false otherwise.
     */
    public boolean isUsernameExists(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            String query = "SELECT * FROM " + USERS_TABLE +
                    " WHERE " + USERS_USERNAME + "=?";
            cursor = db.rawQuery(query, new String[]{username});
            return (cursor != null && cursor.getCount() > 0);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    /**
     * Checks if a user with the specified username and password exists.
     *
     * @param username the username.
     * @param password the password.
     * @return true if the user exists and the password matches, false otherwise.
     */
    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            String query = "SELECT * FROM " + USERS_TABLE +
                    " WHERE " + USERS_USERNAME + "=? AND " +
                    USERS_PASSWORD + "=?";
            cursor = db.rawQuery(query, new String[]{username, password});
            return (cursor != null && cursor.getCount() > 0);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    /**
     * Retrieves the value of a specific column for a user.
     *
     * @param username the username.
     * @param columnName the column to retrieve.
     * @return the value from the specified column, or null if not found.
     */
    public String getUserItem(String username, String columnName) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        String returnedItem = null;
        try {
            String query = "SELECT " + columnName + " FROM " + USERS_TABLE +
                    " WHERE " + USERS_USERNAME + "=?";
            cursor = db.rawQuery(query, new String[]{username});
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndexOrThrow(columnName);
                returnedItem = cursor.getString(idx);
            }
        } catch (IllegalArgumentException e) {
            Log.e("DatabaseHelper", "Column error: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return returnedItem;
    }

    /**
     * Updates the first name for a specific user.
     *
     * @param username the username.
     * @param newFirstName the new first name.
     * @return true if at least one row was updated, false otherwise.
     */
    public boolean updateUserFirstName(String username, String newFirstName) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(USERS_FIRST_NAME, newFirstName);
            int rows = db.update(USERS_TABLE, contentValues, USERS_USERNAME + "=?", new String[]{username});
            return rows > 0;
        }
    }

    /**
     * Updates the last name for a specific user.
     *
     * @param username the username.
     * @param newLastName the new last name.
     * @return true if the update was successful, false otherwise.
     */
    public boolean updateUserLastName(String username, String newLastName) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(USERS_LAST_NAME, newLastName);
            int rows = db.update(USERS_TABLE, contentValues, USERS_USERNAME + "=?", new String[]{username});
            return rows > 0;
        }
    }

    /**
     * Retrieves the password for a specific user.
     *
     * @param username the username.
     * @return the password, or null if not found.
     */
    public String getUserPassword(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + USERS_PASSWORD + " FROM " + USERS_TABLE +
                " WHERE " + USERS_USERNAME + "=?", new String[]{username});
        String password = null;
        if (cursor != null) {
            try {
                int index = cursor.getColumnIndexOrThrow(USERS_PASSWORD);
                if (cursor.moveToFirst()) {
                    password = cursor.getString(index);
                }
            } catch (IllegalArgumentException e) {
                Log.e("DatabaseHelper", "Column not found: " + e.getMessage());
            } finally {
                cursor.close();
                db.close();
            }
        }
        return password;
    }

    /**
     * Updates the password for a specific user.
     *
     * @param username the username.
     * @param newPassword the new password.
     * @return true if the update was successful, false otherwise.
     */
    public boolean updateUserPassword(String username, String newPassword) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(USERS_PASSWORD, newPassword);
            int rows = db.update(USERS_TABLE, contentValues, USERS_USERNAME + "=?", new String[]{username});
            return rows > 0;
        }
    }

    //=========================================================================================
    // Goals methods
    //=========================================================================================

    /**
     * Inserts a new goal into the Goals table.
     *
     * @param username        the user associated with the goal.
     * @param goalName        the name of the goal.
     * @param goalDescription the description of the goal.
     * @param goalAmount      the monetary amount set for the goal.
     * @param category        the goal category.
     * @param dueDate         the due date for the goal (optional, formatted as DD/MM/YYYY).
     * @param voiceNotePath   the path to any associated voice note.
     * @return true if the goal was inserted successfully, false otherwise.
     */
    public boolean insertGoal(String username, String goalName, String goalDescription,
                              double goalAmount, String category, String dueDate, String voiceNotePath) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues cv = new ContentValues();
            cv.put(GOALS_USERNAME_FOREIGN_KEY, username);
            cv.put(GOALS_NAME, goalName);
            cv.put(GOALS_DESCRIPTION, goalDescription);
            cv.put(GOALS_AMOUNT, goalAmount);
            cv.put(GOALS_CATEGORY, category);
            cv.put(GOALS_DUE_DATE, dueDate);
            cv.put(GOALS_VOICE_NOTE_PATH, voiceNotePath);
            long result = db.insert(GOALS_TABLE, null, cv);
            return result != -1;
        }
    }

    /**
     * Retrieves all goals for a specific user.
     *
     * @param username the username.
     * @return a Cursor over the user's goals. The caller must close the Cursor.
     */
    public Cursor getUserGoals(String username) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + GOALS_TABLE +
                " WHERE " + GOALS_USERNAME_FOREIGN_KEY + "=?";
        return db.rawQuery(query, new String[]{username});
    }

    /**
     * Deletes a specific goal by its ID.
     *
     * @param goalID the ID of the goal to delete.
     * @return true if the deletion was successful, false otherwise.
     */
    public boolean deleteGoal(int goalID) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            int result = db.delete(GOALS_TABLE,
                    GOALS_ID + "=?",
                    new String[]{String.valueOf(goalID)});
            return result > 0;
        }
    }

    /**
     * Updates an existing goal.
     *
     * @param goalId          the ID of the goal.
     * @param goalName        the new goal name.
     * @param goalDescription the new description.
     * @param goalAmount      the new goal amount.
     * @param goalCategory    the new goal category.
     * @param dueDate         the new due date (optional, formatted as DD/MM/YYYY).
     *                        If the due date is cleared (empty), a null is stored in the database.
     * @param voiceNotePath   the new voice note path.
     * @return true if the update was successful, false otherwise.
     */
    public boolean updateGoal(int goalId, String goalName, String goalDescription,
                              double goalAmount, String goalCategory, String dueDate, String voiceNotePath) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues cv = new ContentValues();
            cv.put(GOALS_NAME, goalName);
            cv.put(GOALS_DESCRIPTION, goalDescription);
            cv.put(GOALS_AMOUNT, goalAmount);
            cv.put(GOALS_CATEGORY, goalCategory);
            // Convert an empty due date to null so that it is cleared in the database.
            cv.put(GOALS_DUE_DATE, dueDate.isEmpty() ? null : dueDate);
            cv.put(GOALS_VOICE_NOTE_PATH, voiceNotePath);
            int rowsAffected = db.update(GOALS_TABLE, cv,
                    GOALS_ID + "=?",
                    new String[]{String.valueOf(goalId)});
            return rowsAffected > 0;
        }
    }

    //=========================================================================================
    // Accounts methods
    //=========================================================================================

    /**
     * Inserts a new account for a user.
     *
     * @param username the username.
     * @param accountName the account name.
     * @param accountType the account type.
     * @param accountBank the bank associated with the account.
     * @return true if the account was added successfully, false otherwise.
     */
    public boolean addAccount(String username, String accountName, String accountType, String accountBank) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(ACCOUNTS_USERNAME_FOREIGN_KEY, username);
            values.put(ACCOUNTS_NAME, accountName);
            values.put(ACCOUNTS_TYPE, accountType);
            values.put(ACCOUNTS_BANK, accountBank);
            long result = db.insert(ACCOUNTS_TABLE, null, values);
            return result != -1;
        }
    }

    /**
     * Retrieves all accounts for a specific user.
     *
     * @param username the username.
     * @return a Cursor over the accounts for the user.
     */
    public Cursor getUserAccounts(String username) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + ACCOUNTS_TABLE +
                " WHERE " + ACCOUNTS_USERNAME_FOREIGN_KEY + "=?";
        return db.rawQuery(query, new String[]{username});
    }

    /**
     * Updates an account's details.
     *
     * @param accountID the ID of the account.
     * @param newName the new account name.
     * @param newType the new account type.
     * @param newBank the new bank name.
     */
    public void updateAccount(int accountID, String newName, String newType, String newBank) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(ACCOUNTS_NAME, newName);
            contentValues.put(ACCOUNTS_TYPE, newType);
            contentValues.put(ACCOUNTS_BANK, newBank);
            db.update(ACCOUNTS_TABLE, contentValues, ACCOUNTS_ID + " = ?", new String[]{String.valueOf(accountID)});
        }
    }

    //=========================================================================================
    // Cards methods
    //=========================================================================================

    /**
     * Inserts a new card associated with an account.
     *
     * @param accountID the ID of the account.
     * @param cardNumber the card number.
     * @param expirationDate the card's expiration date.
     * @return true if inserted successfully, false otherwise.
     */
    public boolean addCard(int accountID, String cardNumber, String expirationDate) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues cv = new ContentValues();
            cv.put(CARDS_ACCOUNT_ID_FOREIGN_KEY, accountID);
            cv.put(CARDS_NUMBER, cardNumber);
            cv.put(CARDS_EXPIRATION_DATE, expirationDate);
            long result = db.insert(CARDS_TABLE, null, cv);
            return result != -1;
        }
    }

    /**
     * Retrieves all cards for a given account.
     *
     * @param accountID the account ID.
     * @return a Cursor over the cards for the specified account.
     */
    public Cursor getUserCards(int accountID) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + CARDS_TABLE +
                " WHERE " + CARDS_ACCOUNT_ID_FOREIGN_KEY + "=?";
        return db.rawQuery(query, new String[]{String.valueOf(accountID)});
    }

    /**
     * Updates a card's number and expiration date.
     *
     * @param cardID the card ID.
     * @param newNumber the new card number.
     * @param newDate the new expiration date.
     */
    public void updateCard(int cardID, String newNumber, String newDate) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(CARDS_NUMBER, newNumber);
            contentValues.put(CARDS_EXPIRATION_DATE, newDate);
            db.update(CARDS_TABLE, contentValues, CARDS_ID + " = ?", new String[]{String.valueOf(cardID)});
        }
    }

    //=========================================================================================
    // Transactions methods
    //=========================================================================================

    /**
     * Adds a new transaction. For subscription transactions, it calculates the next run using the provided datetime and interval.
     *
     * @param accountID the account ID.
     * @param cardID the card ID (or -1 if none).
     * @param amount the transaction amount.
     * @param type the transaction type (e.g., "Income", "Expense").
     * @param category the category of the transaction.
     * @param description a description; if null or empty, an empty string is used.
     * @param datetime the transaction datetime in "dd/MM/yyyy HH:mm" format.
     * @param isSubscription whether the transaction is a subscription.
     * @param interval the subscription interval (if applicable).
     * @param parentSubscriptionID the parent subscription ID, or null if this is an original subscription.
     * @return true if inserted successfully, false otherwise.
     */
    public boolean addTransaction(int accountID, int cardID, double amount, String type, String category,
                                  String description, String datetime, boolean isSubscription,
                                  String interval, Integer parentSubscriptionID) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues cv = new ContentValues();
            cv.put(TRANSACTIONS_ACCOUNT_ID_FOREIGN_KEY, accountID);
            if (cardID != -1) {
                cv.put(TRANSACTIONS_CARD_ID_FOREIGN_KEY, cardID);
            }
            cv.put(TRANSACTIONS_AMOUNT, amount);
            cv.put(TRANSACTIONS_TYPE, type);
            cv.put(TRANSACTIONS_CATEGORY, category);
            cv.put(TRANSACTIONS_DESCRIPTION, (description == null || description.trim().isEmpty()) ? "" : description);
            cv.put(TRANSACTIONS_DATETIME, datetime);
            cv.put(TRANSACTIONS_IS_SUBSCRIPTION, isSubscription ? 1 : 0);
            if (isSubscription) {
                cv.put(TRANSACTIONS_INTERVAL, interval);
                cv.put(TRANSACTIONS_NEXT_RUN, calculateNextRun(datetime, interval));
            } else {
                cv.put(TRANSACTIONS_INTERVAL, (String) null);
                cv.put(TRANSACTIONS_NEXT_RUN, (String) null);
            }
            cv.put(PARENT_SUBSCRIPTION_ID, parentSubscriptionID);
            long result = db.insert(TRANSACTIONS_TABLE, null, cv);
            return result != -1;
        }
    }


    /**
     * Retrieves a list of transactions for the given user by joining the transactions, accounts, and cards tables.
     *
     * @param username the username whose transactions are to be fetched.
     * @return an ArrayList of Transaction objects.
     */
    public ArrayList<Transaction> getTransactionsFromDB(String username) {
        SQLiteDatabase db = getReadableDatabase();
        ArrayList<Transaction> transactionList = new ArrayList<>();
        String query = "SELECT t." + TRANSACTIONS_ID +
                ", t." + TRANSACTIONS_TYPE +
                ", t." + TRANSACTIONS_AMOUNT +
                ", t." + TRANSACTIONS_CATEGORY +
                ", t." + TRANSACTIONS_DESCRIPTION +
                ", t." + TRANSACTIONS_DATETIME +
                ", a." + ACCOUNTS_NAME +
                ", c." + CARDS_NUMBER +
                ", t." + TRANSACTIONS_ACCOUNT_ID_FOREIGN_KEY +
                ", t." + TRANSACTIONS_CARD_ID_FOREIGN_KEY +
                ", t." + TRANSACTIONS_IS_SUBSCRIPTION +
                ", t." + TRANSACTIONS_INTERVAL +
                ", t." + TRANSACTIONS_NEXT_RUN +
                ", t." + PARENT_SUBSCRIPTION_ID +
                " FROM " + TRANSACTIONS_TABLE + " t " +
                "LEFT JOIN " + ACCOUNTS_TABLE + " a ON t." + TRANSACTIONS_ACCOUNT_ID_FOREIGN_KEY +
                " = a." + ACCOUNTS_ID + " " +
                "LEFT JOIN " + CARDS_TABLE + " c ON t." + TRANSACTIONS_CARD_ID_FOREIGN_KEY +
                " = c." + CARDS_ID + " " +
                "WHERE a." + ACCOUNTS_USERNAME_FOREIGN_KEY + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{username});
        if (cursor.moveToFirst()) {
            do {
                int transactionID = cursor.getInt(cursor.getColumnIndexOrThrow(TRANSACTIONS_ID));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(TRANSACTIONS_TYPE));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(TRANSACTIONS_AMOUNT));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(TRANSACTIONS_CATEGORY));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(TRANSACTIONS_DESCRIPTION));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(TRANSACTIONS_DATETIME));
                String accountName = cursor.isNull(cursor.getColumnIndexOrThrow(ACCOUNTS_NAME))
                        ? "N/A" : cursor.getString(cursor.getColumnIndexOrThrow(ACCOUNTS_NAME));
                String cardNumber = cursor.isNull(cursor.getColumnIndexOrThrow(CARDS_NUMBER))
                        ? "N/A" : cursor.getString(cursor.getColumnIndexOrThrow(CARDS_NUMBER));
                int accountID = cursor.getInt(cursor.getColumnIndexOrThrow(TRANSACTIONS_ACCOUNT_ID_FOREIGN_KEY));
                int cardID = cursor.isNull(cursor.getColumnIndexOrThrow(TRANSACTIONS_CARD_ID_FOREIGN_KEY))
                        ? -1 : cursor.getInt(cursor.getColumnIndexOrThrow(TRANSACTIONS_CARD_ID_FOREIGN_KEY));
                boolean isSubscription = cursor.getInt(cursor.getColumnIndexOrThrow(TRANSACTIONS_IS_SUBSCRIPTION)) == 1;
                String interval = cursor.isNull(cursor.getColumnIndexOrThrow(TRANSACTIONS_INTERVAL))
                        ? null : cursor.getString(cursor.getColumnIndexOrThrow(TRANSACTIONS_INTERVAL));
                String nextRun = cursor.isNull(cursor.getColumnIndexOrThrow(TRANSACTIONS_NEXT_RUN))
                        ? null : cursor.getString(cursor.getColumnIndexOrThrow(TRANSACTIONS_NEXT_RUN));
                Integer parentSubscriptionId = cursor.isNull(cursor.getColumnIndexOrThrow(PARENT_SUBSCRIPTION_ID))
                        ? null : cursor.getInt(cursor.getColumnIndexOrThrow(PARENT_SUBSCRIPTION_ID));

                Transaction transaction = new Transaction(
                        transactionID, type, amount, category, description, date,
                        accountName, cardNumber, accountID, cardID,
                        isSubscription, interval, nextRun, parentSubscriptionId
                );
                transactionList.add(transaction);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactionList;
    }

    /**
     * Calculates the net total for transactions in a specific category for a user.
     *
     * @param username the username.
     * @param category the category to calculate for.
     * @return the net total (Income minus Expense).
     */
    public double getNetTransactionsForCategory(String username, String category) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        double netTotal = 0;
        try {
            String query = "SELECT SUM(CASE " +
                    "WHEN " + TRANSACTIONS_TYPE + " = 'Income' THEN " + TRANSACTIONS_AMOUNT + " " +
                    "WHEN " + TRANSACTIONS_TYPE + " = 'Expense' THEN -" + TRANSACTIONS_AMOUNT + " " +
                    "ELSE 0 END) as netTotal " +
                    "FROM " + TRANSACTIONS_TABLE + " t " +
                    "JOIN " + ACCOUNTS_TABLE + " a ON t." + TRANSACTIONS_ACCOUNT_ID_FOREIGN_KEY +
                    " = a." + ACCOUNTS_ID + " " +
                    "WHERE a." + ACCOUNTS_USERNAME_FOREIGN_KEY + " = ? " +
                    "AND t." + TRANSACTIONS_CATEGORY + " = ?";
            cursor = db.rawQuery(query, new String[]{username, category});
            if (cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex("netTotal");
                if (idx != -1) {
                    netTotal = cursor.getDouble(idx);
                }
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return netTotal;
    }

    /**
     * Deletes a transaction with the specified ID.
     *
     * @param transactionId the ID of the transaction to delete.
     * @return true if deletion was successful, false otherwise.
     */
    public boolean deleteTransaction(int transactionId) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            int result = db.delete(TRANSACTIONS_TABLE, TRANSACTIONS_ID + " = ?",
                    new String[]{String.valueOf(transactionId)});
            return result > 0;
        }
    }

    /**
     * Deletes a subscription and all related transactions.
     *
     * @param subscriptionId The unique identifier of the subscription to be deleted.
     * @return true if at least one row was affected, false otherwise.
     */
    public boolean deleteSubscriptionWithTransactions(int subscriptionId) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            // Delete both the parent subscription and all child transactions (whose PARENT_SUBSCRIPTION_ID matches the parent's ID)
            int rowsAffected = db.delete(
                    TRANSACTIONS_TABLE,
                    TRANSACTIONS_ID + " = ? OR " + PARENT_SUBSCRIPTION_ID + " = ?",
                    new String[]{String.valueOf(subscriptionId), String.valueOf(subscriptionId)}
            );
            return rowsAffected > 0;
        }
    }

    /**
     * Updates an existing transaction in the database using the provided Transaction object.
     *
     * @param transaction the Transaction object containing updated values.
     * @param flag to check if the interval was updated.
     * @return true if the update was successful, false otherwise.
     */
    public boolean updateTransaction(Transaction transaction, boolean flag) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(TRANSACTIONS_AMOUNT, transaction.getAmount());
            values.put(TRANSACTIONS_TYPE, transaction.getType());
            values.put(TRANSACTIONS_CATEGORY, transaction.getCategory());
            values.put(TRANSACTIONS_DESCRIPTION, transaction.getDescription());
            values.put(TRANSACTIONS_DATETIME, transaction.getDate());
            values.put(TRANSACTIONS_ACCOUNT_ID_FOREIGN_KEY, transaction.getAccountID());
            if (transaction.getCardID() == -1) {
                values.putNull(TRANSACTIONS_CARD_ID_FOREIGN_KEY);
            } else {
                values.put(TRANSACTIONS_CARD_ID_FOREIGN_KEY, transaction.getCardID());
            }
            values.put(TRANSACTIONS_IS_SUBSCRIPTION, transaction.isSubscription() ? 1 : 0);
            if (transaction.isSubscription()) {
                values.put(TRANSACTIONS_INTERVAL, transaction.getInterval());
                if (transaction.getNextRun() != null && !transaction.getNextRun().trim().isEmpty()) {
                    if (flag) {
                        values.put(TRANSACTIONS_NEXT_RUN, calculateNextRun(transaction.getDate(), transaction.getInterval()));
                    } else {
                        values.put(TRANSACTIONS_NEXT_RUN, transaction.getNextRun());
                    }
                } else {
                    values.put(TRANSACTIONS_NEXT_RUN, calculateNextRun(transaction.getDate(), transaction.getInterval()));
                }
            } else {
                values.putNull(TRANSACTIONS_INTERVAL);
                values.putNull(TRANSACTIONS_NEXT_RUN);
            }
            values.put(PARENT_SUBSCRIPTION_ID, transaction.getParentSubscriptionId());

            int rowsAffected = db.update(TRANSACTIONS_TABLE, values,
                    TRANSACTIONS_ID + " = ?", new String[]{String.valueOf(transaction.getTransactionId())});
            return rowsAffected > 0;
        }
    }

    /**
     * Inserts a new transaction spawned from a subscription.
     * The new transaction is added as a non-subscription transaction with the parent subscription ID.
     *
     * @param subscriptionTransaction the original subscription Transaction.
     * @return true if the insertion was successful, false otherwise.
     */
    public boolean spawnNewTransaction(Transaction subscriptionTransaction) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(TRANSACTIONS_AMOUNT, subscriptionTransaction.getAmount());
            values.put(TRANSACTIONS_TYPE, subscriptionTransaction.getType());
            values.put(TRANSACTIONS_CATEGORY, subscriptionTransaction.getCategory());
            values.put(TRANSACTIONS_DESCRIPTION, subscriptionTransaction.getDescription());
            values.put(TRANSACTIONS_DATETIME, subscriptionTransaction.getDate());
            values.put(TRANSACTIONS_ACCOUNT_ID_FOREIGN_KEY, subscriptionTransaction.getAccountID());
            if (subscriptionTransaction.getCardID() != -1) {
                values.put(TRANSACTIONS_CARD_ID_FOREIGN_KEY, subscriptionTransaction.getCardID());
            } else {
                values.putNull(TRANSACTIONS_CARD_ID_FOREIGN_KEY);
            }
            values.put(TRANSACTIONS_IS_SUBSCRIPTION, 0);
            values.put(TRANSACTIONS_INTERVAL, (String) null);
            values.put(TRANSACTIONS_NEXT_RUN, (String) null);

            // Determine the parent subscription's ID.
            Integer parentId = subscriptionTransaction.getParentSubscriptionId();
            if (parentId == null || parentId == 0) {
                parentId = subscriptionTransaction.getTransactionId();
            }
            if (parentId != 0) {
                values.put(PARENT_SUBSCRIPTION_ID, parentId);
            } else {
                values.putNull(PARENT_SUBSCRIPTION_ID);
            }

            long result = db.insert(TRANSACTIONS_TABLE, null, values);
            if (result == -1) {
                Log.e("spawnNewTransaction", "Failed to insert transaction for subscription ID: "
                        + subscriptionTransaction.getTransactionId());
            } else {
                Log.d("spawnNewTransaction", "Transaction inserted successfully with ID: " + result);
            }
            return result != -1;
        }
    }


    //=========================================================================================
    // Subscription helper methods
    //=========================================================================================

    /**
     * Determines the shortest (highest priority) subscription interval from active subscriptions.
     *
     * @return the shortest subscription interval ("minutely", "daily", "weekly", "monthly", "yearly"),
     *         or null if no active subscriptions are found.
     */
    public String getShortestSubscriptionInterval() {
        SQLiteDatabase db = getReadableDatabase();
        String[] priorityOrder = {"Minutely", "Daily", "Weekly", "Monthly", "Yearly"};
        for (String interval : priorityOrder) {
            Cursor cursor = db.rawQuery("SELECT 1 FROM " + TRANSACTIONS_TABLE +
                    " WHERE " + TRANSACTIONS_IS_SUBSCRIPTION + " = 1 AND " +
                    TRANSACTIONS_INTERVAL + " = ?", new String[]{interval});
            if (cursor.moveToFirst()) {
                cursor.close();
                db.close();
                return interval;
            }
            cursor.close();
        }
        db.close();
        return null;
    }

    /**
     * Retrieves all subscription transactions that are ready to run as of the specified current date.
     * The current date is formatted into "yyyy-MM-dd HH:mm" to allow proper comparison
     * with the stored next run datetime. Only subscription transactions (where TRANSACTIONS_IS_SUBSCRIPTION = 1)
     * with a next run datetime less than or equal to the current date are returned.
     * @param currentDate the current date.
     * @return an ArrayList of Transaction objects representing subscriptions that are ready to execute.
     */
    public ArrayList<Transaction> getSubscriptionsReadyToRun(Date currentDate) {
        SQLiteDatabase db = getReadableDatabase();
        ArrayList<Transaction> subscriptions = new ArrayList<>();

        String currentDateString = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(currentDate);

        String query = "SELECT t." + TRANSACTIONS_ID +
                ", t." + TRANSACTIONS_TYPE +
                ", t." + TRANSACTIONS_AMOUNT +
                ", t." + TRANSACTIONS_CATEGORY +
                ", t." + TRANSACTIONS_DESCRIPTION +
                ", t." + TRANSACTIONS_DATETIME +
                ", t." + TRANSACTIONS_ACCOUNT_ID_FOREIGN_KEY +
                ", t." + TRANSACTIONS_CARD_ID_FOREIGN_KEY +
                ", t." + TRANSACTIONS_INTERVAL +
                ", t." + TRANSACTIONS_NEXT_RUN +
                ", t." + PARENT_SUBSCRIPTION_ID +
                " FROM " + TRANSACTIONS_TABLE + " t " +
                "WHERE t." + TRANSACTIONS_IS_SUBSCRIPTION + " = 1 AND t." + TRANSACTIONS_NEXT_RUN + " <= ?";

        Cursor cursor = db.rawQuery(query, new String[]{currentDateString});
        if (cursor.moveToFirst()) {
            do {
                int transactionID = cursor.getInt(cursor.getColumnIndexOrThrow(TRANSACTIONS_ID));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(TRANSACTIONS_TYPE));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(TRANSACTIONS_AMOUNT));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(TRANSACTIONS_CATEGORY));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(TRANSACTIONS_DESCRIPTION));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(TRANSACTIONS_DATETIME));
                int accountID = cursor.getInt(cursor.getColumnIndexOrThrow(TRANSACTIONS_ACCOUNT_ID_FOREIGN_KEY));
                int cardID = cursor.isNull(cursor.getColumnIndexOrThrow(TRANSACTIONS_CARD_ID_FOREIGN_KEY))
                        ? -1 : cursor.getInt(cursor.getColumnIndexOrThrow(TRANSACTIONS_CARD_ID_FOREIGN_KEY));
                String interval = cursor.isNull(cursor.getColumnIndexOrThrow(TRANSACTIONS_INTERVAL))
                        ? null : cursor.getString(cursor.getColumnIndexOrThrow(TRANSACTIONS_INTERVAL));
                String nextRun = cursor.isNull(cursor.getColumnIndexOrThrow(TRANSACTIONS_NEXT_RUN))
                        ? null : cursor.getString(cursor.getColumnIndexOrThrow(TRANSACTIONS_NEXT_RUN));
                Integer parentSubscriptionId = cursor.isNull(cursor.getColumnIndexOrThrow(PARENT_SUBSCRIPTION_ID))
                        ? null : cursor.getInt(cursor.getColumnIndexOrThrow(PARENT_SUBSCRIPTION_ID));

                // For subscription transactions, account and card details are set to null.
                Transaction subscription = new Transaction(
                        transactionID, type, amount, category, description, date,
                        null, null, accountID, cardID,
                        true, interval, nextRun, parentSubscriptionId
                );
                subscriptions.add(subscription);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return subscriptions;
    }

    /**
     * Calculates the next run datetime for a subscription transaction based on its current datetime and interval.
     * The input datetime is expected in "dd/MM/yyyy HH:mm" format, and the calculated next run will also be formatted as
     * "dd/MM/yyyy HH:mm". Supported intervals include "minutely", "daily", "weekly", "monthly", and "yearly".
     * @param datetime the current datetime of the subscription in "dd/MM/yyyy HH:mm" format.
     * @param interval the subscription interval (minutely, daily, weekly, monthly, or yearly).
     * @return the next run datetime as a formatted string, or null if the parsing fails or the interval is unsupported.
     */
    public String calculateNextRun(String datetime, String interval) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        try {
            // Parse the input datetime.
            Date parsedDate = inputFormat.parse(datetime);
            if (parsedDate != null) {
                calendar.setTime(parsedDate);
            } else {
                System.out.println("The provided datetime string could not be parsed.");
            }
        } catch (ParseException e) {
            Log.e("DatabaseHelper", "Error parsing datetime: " + datetime, e);
            return null;
        }
        // Adjust the calendar based on the specified interval.
        switch (interval.toLowerCase()) {
            case "minutely":
                calendar.add(Calendar.MINUTE, 1);
                break;
            case "daily":
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case "weekly":
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case "monthly":
                calendar.add(Calendar.MONTH, 1);
                break;
            case "yearly":
                calendar.add(Calendar.YEAR, 1);
                break;
            default:
                Log.e("DatabaseHelper", "Unsupported interval: " + interval);
                return null;
        }
        // Return the formatted next run datetime.
        return outputFormat.format(calendar.getTime());
    }

    /**
     * Computes the total income for a user by aggregating all transactions of type "income"
     * across all accounts associated with the specified username.
     * This method retrieves all account IDs linked to the given user and then iterates through
     * each account's transactions. Only amounts corresponding to income transactions are added
     * to the total income.
     * @param username the username for which to compute the total income.
     * @return the total income amount.
     */
    public double sumUserIncomes(String username) {
        double totalIncome = 0.0;
        SQLiteDatabase db = getReadableDatabase();

        // Retrieve all account IDs for the specified user.
        Cursor accountCursor = db.rawQuery(
                "SELECT " + ACCOUNTS_ID +
                        " FROM " + ACCOUNTS_TABLE +
                        " WHERE " + ACCOUNTS_USERNAME_FOREIGN_KEY + " = ?",
                new String[]{username});

        if (accountCursor.moveToFirst()) {
            do {
                int accountId = accountCursor.getInt(0);
                // Retrieve transactions for each account.
                Cursor transactionCursor = db.rawQuery(
                        "SELECT " + TRANSACTIONS_AMOUNT + ", " + TRANSACTIONS_TYPE +
                                " FROM " + TRANSACTIONS_TABLE +
                                " WHERE " + TRANSACTIONS_ACCOUNT_ID_FOREIGN_KEY + " = ?",
                        new String[]{String.valueOf(accountId)});

                if (transactionCursor.moveToFirst()) {
                    do {
                        double amount = transactionCursor.getDouble(0);
                        String type = transactionCursor.getString(1);
                        if (type.equalsIgnoreCase("income")) {
                            totalIncome += amount;
                        }
                    } while (transactionCursor.moveToNext());
                }
                transactionCursor.close();
            } while (accountCursor.moveToNext());
        }
        accountCursor.close();
        db.close();
        return totalIncome;
    }

    /**
     * Computes the total expenses for a user by aggregating all transactions of type "expense"
     * across all accounts associated with the specified username.
     * This method retrieves all account IDs linked to the given user and then iterates through
     * each account's transactions. Only the amounts corresponding to expense transactions
     * are added to the total expenses.
     * @param username the username for which to compute the total expenses.
     * @return the total expense amount.
     */
    public double sumUserExpenses(String username) {
        double totalExpenses = 0.0;
        SQLiteDatabase db = getReadableDatabase();

        // Retrieve all account IDs for the specified user.
        Cursor accountCursor = db.rawQuery(
                "SELECT " + ACCOUNTS_ID +
                        " FROM " + ACCOUNTS_TABLE +
                        " WHERE " + ACCOUNTS_USERNAME_FOREIGN_KEY + " = ?",
                new String[]{username});

        if (accountCursor.moveToFirst()) {
            do {
                int accountId = accountCursor.getInt(0);
                // Retrieve transactions for each account.
                Cursor transactionCursor = db.rawQuery(
                        "SELECT " + TRANSACTIONS_AMOUNT + ", " + TRANSACTIONS_TYPE +
                                " FROM " + TRANSACTIONS_TABLE +
                                " WHERE " + TRANSACTIONS_ACCOUNT_ID_FOREIGN_KEY + " = ?",
                        new String[]{String.valueOf(accountId)});

                if (transactionCursor.moveToFirst()) {
                    do {
                        double amount = transactionCursor.getDouble(0);
                        String type = transactionCursor.getString(1);
                        if (type.equalsIgnoreCase("expense")) {
                            totalExpenses += amount;
                        }
                    } while (transactionCursor.moveToNext());
                }
                transactionCursor.close();
            } while (accountCursor.moveToNext());
        }
        accountCursor.close();
        db.close();
        return totalExpenses;
    }

    /**
     * Retrieves the balance of a specific account.
     * This method calculates the current balance for the given account by summing up all related transaction amounts.
     *
     * @param accountID the unique identifier of the account whose balance is being calculated.
     * @return the current balance for the account as a double.
     */
    public double getAccountBalance(int accountID) {
        double balance = 0.0;
        SQLiteDatabase db = getReadableDatabase();

        // Construct the query to sum incomes and subtract expenses.
        String query = "SELECT SUM(CASE " +
                "WHEN " + TRANSACTIONS_TYPE + " = 'Income' THEN " + TRANSACTIONS_AMOUNT + " " +
                "WHEN " + TRANSACTIONS_TYPE + " = 'Expense' THEN -" + TRANSACTIONS_AMOUNT + " " +
                "ELSE 0 END) as total_balance " +
                "FROM " + TRANSACTIONS_TABLE + " " +
                "WHERE " + TRANSACTIONS_ACCOUNT_ID_FOREIGN_KEY + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(accountID)});
        if (cursor.moveToFirst()) {
            int index = cursor.getColumnIndex("total_balance");
            if (!cursor.isNull(index)) {
                balance = cursor.getDouble(index);
            }
        }
        cursor.close();
        return balance;
    }

    /**
     * Deletes an item from a specified table based on the given column and item ID.
     *
     * @param table the table from which to delete the item.
     * @param column_name the column used to match the item ID.
     * @param itemID the ID of the item to delete.
     * @return true if the deletion was successful, false otherwise.
     */
    public boolean deleteItem(String table, String column_name, int itemID) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(table, column_name + "=?", new String[]{String.valueOf(itemID)});
        db.close();
        return result > 0;
    }

}
