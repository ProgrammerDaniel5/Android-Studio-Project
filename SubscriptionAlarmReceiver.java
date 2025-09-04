package com.example.financeapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * A BroadcastReceiver that is triggered by the AlarmManager to send a notification.
 */
public class SubscriptionAlarmReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "TRANSACTION_CHANNEL";
    public static final String CHANNEL_NAME = "Transaction Alerts";

    /**
     * Called when the AlarmManager triggers this BroadcastReceiver.
     * Extracts the transaction message from the intent and sends a notification.
     *
     * @param context The application context.
     * @param intent The intent carrying notification data.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String message = intent.getStringExtra("transactionMessage");
        if (message == null) {
            message = "A subscription transaction occurred!";
        }
        sendNotification(context, message);
    }

    /**
     * Creates a notification channel if necessary and sends the notification.
     *
     * @param context The context to use.
     * @param message The message to display.
     */
    private void sendNotification(Context context, String message) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }

        // Build and display the notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Subscription Notification")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        if (notificationManagerCompat.areNotificationsEnabled()) {
            notificationManagerCompat.notify(1555, builder.build());
            Log.d("SubscriptionAlarmReceiver", "Notification sent with message: " + message);
        } else {
            Log.w("SubscriptionAlarmReceiver", "Notifications are disabled by user.");
        }
    }
}
