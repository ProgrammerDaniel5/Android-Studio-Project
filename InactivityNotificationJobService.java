package com.example.financeapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.concurrent.TimeUnit;

/**
 * InactivityNotificationJobService is a JobService that sends a notification when the app
 * has been inactive for a set period. After sending a notification,
 * it re-schedules itself for the next interval.
 */
public class InactivityNotificationJobService extends JobService {

    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "inactivity_channel";
    private static final long REPEAT_INTERVAL = TimeUnit.SECONDS.toMillis(6); // Needed for re-execution
    public static final int NOTIFICATION_JOB_ID = 123;

    /**
     * Called when the job starts. If the app is visible, the job is finished immediately.
     * Otherwise, a notification is sent and the next job is scheduled.
     *
     * @param params Job parameters.
     * @return true if the work is still running.
     */
    @Override
    public boolean onStartJob(JobParameters params) {
        sendNotification();
        scheduleNextJob();
        jobFinished(params, false);
        return true;
    }

    /**
     * Prepares and sends a notification to the user.
     * Checks for permission on Android TIRAMISU and higher.
     */
    private void sendNotification() {
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Hey!")
                .setContentText("It's been a while since you last used the app. Manage your financial activity by using the app.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check runtime notification permission for Android 13+
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            } else {
                Log.w("NotificationService", "POST_NOTIFICATIONS permission not granted. Cannot send notification.");
            }
        } else {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }

        Log.d("JobService", "Notification sent.");
    }

    /**
     * Creates a notification channel.
     */
    private void createNotificationChannel() {
        CharSequence name = "Inactivity Channel";
        String description = "Channel for inactivity notifications";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Schedules the next notification job to run after the defined interval.
     */
    private void scheduleNextJob() {
        JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) return;
        ComponentName componentName = new ComponentName(this, InactivityNotificationJobService.class);
        JobInfo jobInfo = new JobInfo.Builder(NOTIFICATION_JOB_ID, componentName)
                .setMinimumLatency(REPEAT_INTERVAL)
                .setOverrideDeadline(REPEAT_INTERVAL + 5000)
                .setPersisted(true)
                .build();
        int result = jobScheduler.schedule(jobInfo);
        Log.d("JobService", "Re-scheduled next notification job with result: " + result);
    }

    /**
     * Called when the system stops the job before completion.
     *
     * @param params Job parameters.
     * @return false to indicate no rescheduling.
     */
    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
