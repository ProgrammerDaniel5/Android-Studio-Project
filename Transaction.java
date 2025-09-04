package com.example.financeapp;
/**
 * The Transaction class represents a financial transaction with details such
 * as the transaction ID, type, amount, category, date, description, account and
 * card details, as well as subscription-related information.
 *
 * For subscriptions, it stores the interval (e.g., "daily", "weekly"), the next run date,
 * and optionally a parent subscription ID (which is null for the original subscription).
 */
public class Transaction {

    // Private fields for transaction details.
    private int transactionId;
    private String type;
    private double amount;
    private String category;
    private String date;
    private String description;
    private String accountName;
    private String cardNumber;
    private int accountID;
    private int cardID;
    private boolean isSubscription;
    private String interval; // For example: "daily", "weekly", etc.
    private String nextRun;  // Next execution date for subscription
    private Integer parentSubscriptionId; // ID of the parent subscription; null for original

    /**
     * Constructs a new Transaction with the specified details.
     *
     * @param transactionId         the unique identifier for the transaction
     * @param type                  the type of transaction (e.g., "Income", "Expense")
     * @param amount                the monetary amount of the transaction
     * @param category              the category of the transaction
     * @param description           a description of the transaction
     * @param date                  the date and time of the transaction as a String
     * @param accountName           the name of the account associated with the transaction
     * @param cardNumber            the card number used in the transaction
     * @param accountID             the ID of the related account
     * @param cardID                the ID of the related card
     * @param isSubscription        true if this transaction is a subscription, false otherwise
     * @param interval              the subscription interval (if applicable)
     * @param nextRun               the next execution date (if applicable)
     * @param parentSubscriptionId  the parent subscription ID (null for an original subscription)
     */
    public Transaction(int transactionId, String type, double amount, String category, String description, String date,
                       String accountName, String cardNumber, int accountID, int cardID,
                       boolean isSubscription, String interval, String nextRun, Integer parentSubscriptionId) {
        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.description = description;
        this.accountName = accountName;
        this.cardNumber = cardNumber;
        this.accountID = accountID;
        this.cardID = cardID;
        this.isSubscription = isSubscription;
        this.interval = interval;
        this.nextRun = nextRun;
        this.parentSubscriptionId = parentSubscriptionId;
    }

    // Getters and Setters

    /**
     * Returns the transaction ID.
     *
     * @return the transaction ID.
     */
    public int getTransactionId() {
        return transactionId;
    }

    /**
     * Sets the transaction ID.
     *
     * @param transactionId the transaction ID.
     */
    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Returns the type of the transaction.
     *
     * @return the transaction type.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the transaction type.
     *
     * @param type the transaction type.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the amount of the transaction.
     *
     * @return the transaction amount.
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Sets the transaction amount.
     *
     * @param amount the transaction amount.
     */
    public void setAmount(double amount) {
        this.amount = amount;
    }

    /**
     * Returns the category of the transaction.
     *
     * @return the category.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the transaction category.
     *
     * @param category the category.
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Returns the date and time of the transaction.
     *
     * @return the date as a String.
     */
    public String getDate() {
        return date;
    }

    /**
     * Sets the date and time of the transaction.
     *
     * @param date the date as a String.
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * Returns the description of the transaction.
     *
     * @return the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the transaction description.
     *
     * @param description the description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the account name associated with the transaction.
     *
     * @return the account name.
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * Sets the account name.
     *
     * @param accountName the account name.
     */
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    /**
     * Returns the card number used in the transaction.
     *
     * @return the card number.
     */
    public String getCardNumber() {
        return cardNumber;
    }

    /**
     * Sets the card number.
     *
     * @param cardNumber the card number.
     */
    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    /**
     * Returns the account ID.
     *
     * @return the account ID.
     */
    public int getAccountID() {
        return accountID;
    }

    /**
     * Sets the account ID.
     *
     * @param accountID the account ID.
     */
    public void setAccountID(int accountID) {
        this.accountID = accountID;
    }

    /**
     * Returns the card ID.
     *
     * @return the card ID.
     */
    public int getCardID() {
        return cardID;
    }

    /**
     * Sets the card ID.
     *
     * @param cardID the card ID.
     */
    public void setCardID(int cardID) {
        this.cardID = cardID;
    }

    /**
     * Indicates whether this transaction is a subscription.
     *
     * @return true if it is a subscription, false otherwise.
     */
    public boolean isSubscription() {
        return isSubscription;
    }

    /**
     * Sets whether this transaction is a subscription.
     *
     * @param subscription true if it is a subscription.
     */
    public void setSubscription(boolean subscription) {
        isSubscription = subscription;
    }

    /**
     * Returns the subscription interval.
     *
     * @return the interval.
     */
    public String getInterval() {
        return interval;
    }

    /**
     * Sets the subscription interval.
     *
     * @param interval the interval (e.g., "daily", "weekly").
     */
    public void setInterval(String interval) {
        this.interval = interval;
    }

    /**
     * Returns the next execution date for the subscription.
     *
     * @return the next run date.
     */
    public String getNextRun() {
        return nextRun;
    }

    /**
     * Sets the next execution date for the subscription.
     *
     * @param nextRun the next run date.
     */
    public void setNextRun(String nextRun) {
        this.nextRun = nextRun;
    }

    /**
     * Returns the parent subscription ID.
     *
     * @return the parent subscription ID, or null if this is the original.
     */
    public Integer getParentSubscriptionId() {
        return parentSubscriptionId;
    }

    /**
     * Sets the parent subscription ID.
     *
     * @param parentSubscriptionId the parent subscription ID.
     */
    public void setParentSubscriptionId(Integer parentSubscriptionId) {
        this.parentSubscriptionId = parentSubscriptionId;
    }
}