package com.example.financeapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * SubscriptionService is a background service that checks and processes recurring subscription transactions.
 * When started, the service retrieves any subscriptions that are due to run, creates the corresponding transactions,
 * and then schedules the next run based on the subscription's interval.
 */
public class SubscriptionService extends Service {

    /**
     * Handles service startup logic.
     * Logs the start event and checks subscriptions.
     *
     * @param intent  The intent that started the service.
     * @param flags   Flags for service startup behavior.
     * @param startId The unique ID for this service instance.
     * @return The restart behavior, set to {@link #START_STICKY}.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SubscriptionService", "Service started.");
        checkSubscriptions();
        return START_STICKY;
    }

    /**
     * Retrieves subscriptions due to run, then processes each one by spawning any missed transactions.
     */
    private void checkSubscriptions() {
        try (DatabaseHelper db = new DatabaseHelper(this)) {
            List<Transaction> subscriptions = db.getSubscriptionsReadyToRun(new Date());
            Log.d("SubscriptionService", "Subscriptions ready to run: " + subscriptions.size());

            for (Transaction subscription : subscriptions) {
                Log.d("SubscriptionService", "Processing subscription ID: " + subscription.getTransactionId());
                spawnMissedTransactions(subscription);
            }
        }
    }

    /**
     * Spawns missed transactions for a subscription. If the stored nextRun is in the past,
     * it loops through every missed interval and spawns a new transaction for each one.
     * Importantly, each spawned transaction is assigned the parent's subscription ID properly.
     *
     * @param subscription the original subscription transaction to process.
     */
    private void spawnMissedTransactions(Transaction subscription) {
        try (DatabaseHelper db = new DatabaseHelper(this)) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date now = new Date();
            try {
                // Parse the stored nextRun value from the subscription.
                Date nextRunDate = sdf.parse(subscription.getNextRun());
                if (nextRunDate == null) {
                    Log.e("SubscriptionService", "nextRunDate is null. Invalid date format.");
                    return;
                }

                // If current time is before nextRun, nothing to do.
                if (now.before(nextRunDate)) {
                    Log.d("SubscriptionService", "No missed transactions. Next run is at: " + sdf.format(nextRunDate));
                    return;
                }

                // Determine the interval length in milliseconds.
                long intervalMillis = getIntervalMillis(subscription.getInterval());
                // Calculate the time difference and the number of missed intervals.
                long diff = now.getTime() - nextRunDate.getTime();
                int missedCount = (int) (diff / intervalMillis) + 1;

                Integer parentId = subscription.getParentSubscriptionId();
                if (parentId == null || parentId == 0) {
                    parentId = subscription.getTransactionId(); // Use subscription’s own ID as parent if not set.
                }

                // For each missed interval, spawn a corresponding missed (child) transaction.
                for (int i = 0; i < missedCount; i++) {
                    // Compute the scheduled time for this missed transaction.
                    Date scheduledTime = new Date(nextRunDate.getTime() + i * intervalMillis);
                    Transaction missedTransaction = new Transaction(
                            0,                   // Let the DB auto-generate an ID.
                            subscription.getType(),
                            subscription.getAmount(),
                            subscription.getCategory(),
                            subscription.getDescription(),
                            sdf.format(scheduledTime),      // Scheduled time for this child transaction.
                            subscription.getAccountName(),
                            subscription.getCardNumber(),
                            subscription.getAccountID(),
                            subscription.getCardID(),
                            true,               // Marked as subscription child.
                            subscription.getInterval(),
                            sdf.format(scheduledTime),
                            parentId                        // Reference to the parent subscription.
                    );
                    db.spawnNewTransaction(missedTransaction);
                    Log.d("SubscriptionService", "Spawned missed transaction for: " + sdf.format(scheduledTime));
                }

                // Updating the subscription's nextRun to be the original nextRun plus missedCount intervals.
                Date newNextRunDate = new Date(nextRunDate.getTime() + missedCount * intervalMillis);
                String newNextRun = sdf.format(newNextRunDate);
                subscription.setNextRun(newNextRun);
                db.updateTransaction(subscription, false);
                Log.d("SubscriptionService", "Updated subscription's nextRun to: " + newNextRun);

                // Send a broadcast so that the UI (ReportsActivity screen) refreshes.
                Intent updateIntent = new Intent("com.example.ZrimaFinanceIt.TRANSACTIONS_UPDATED");
                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(updateIntent);
                Log.d("SubscriptionService", "Broadcast sent: TRANSACTIONS_UPDATED");

            } catch (ParseException e) {
                Log.e("SubscriptionService", "Error parsing nextRun date: " + e.getMessage());
            }
        }
    }

    /**
     * Converts a time interval string into its equivalent duration in milliseconds.
     * This method supports common intervals such as minutely, daily, weekly, monthly, and yearly.
     *
     * @param interval The interval type as a string (e.g., "minutely", "daily", "weekly").
     *                 If null or unsupported, the method will return 0.
     * @return The duration of the interval in milliseconds. Returns 0 if the interval is null or unsupported.
     * @throws IllegalArgumentException if the interval is unsupported.
     */
    private long getIntervalMillis(String interval) {
        if (interval == null)
            return 0;
        switch (interval.toLowerCase()) {
            case "minutely":
                return 60 * 1000;
            case "daily":
                return 24 * 60 * 60 * 1000;
            case "weekly":
                return 7 * 24 * 60 * 60 * 1000;
            case "monthly":
                return 30L * 24 * 60 * 60 * 1000;
            case "yearly":
                return 365L * 24 * 60 * 60 * 1000;
            default:
                throw new IllegalArgumentException("Unsupported interval: " + interval);
        }
    }

    /**
     * Handles binding requests from components.
     * This service does not support binding, so it returns null.
     *
     * @param intent The binding intent.
     * @return Always returns null.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
