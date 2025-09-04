package com.example.financeapp;

import android.app.Activity;
import android.app.Application;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.concurrent.TimeUnit;

/**
 * MyApplication monitors the app's foreground and background state using
 * ActivityLifecycleCallbacks. When the app goes to the background, it schedules a
 * job to send an inactivity notification after a delay. When the app enters the
 * foreground, any scheduled job is cancelled.
 */
public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static final int NOTIFICATION_JOB_ID = 123;
    private static final long REPEAT_INTERVAL = TimeUnit.SECONDS.toMillis(6);
    private int activityReferences = 0;

    /**
     * Called when the application starts. Registers activity lifecycle callbacks.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    /**
     * Called when any activity in the app is started.
     * Increments the active activity counter. If the first activity is started
     * and the activity is not changing configurations, cancels any scheduled job.
     *
     * @param activity the activity that started.
     */
    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        activityReferences++;
        if (activityReferences == 1 && !activity.isChangingConfigurations() && hasNotificationPermission()) {
            // App enters foreground; cancel any scheduled notification job.
            Log.d("MyApplication", "App entered foreground; cancelling job.");
            JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            if (scheduler != null) {
                scheduler.cancel(NOTIFICATION_JOB_ID);
            }
        }
    }

    /**
     * Called when any activity in the app stops.
     * Decrements the active activity counter and, if the app goes to the background,
     * schedules a notification job to be executed after a delay.
     *
     * @param activity the activity that stopped.
     */
    @Override
    public void onActivityStopped(Activity activity) {
        boolean isActivityChangingConfigurations = activity.isChangingConfigurations();
        activityReferences--;
        if (activityReferences == 0 && !isActivityChangingConfigurations && hasNotificationPermission()) {
            // App went to background; schedule a job for inactivity notification.
            Log.d("MyApplication", "App went to background; scheduling job.");
            JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            if (scheduler != null) {
                ComponentName componentName = new ComponentName(this, InactivityNotificationJobService.class);
                long delayMillis = REPEAT_INTERVAL;
                JobInfo jobInfo = new JobInfo.Builder(NOTIFICATION_JOB_ID, componentName)
                        .setMinimumLatency(delayMillis)
                        .setOverrideDeadline(delayMillis + 5000) // 5-second window to run the job
                        .setPersisted(true) // Persist through reboots if allowed
                        .build();
                int result = scheduler.schedule(jobInfo);
                if (result == JobScheduler.RESULT_SUCCESS) {
                    Log.d("MyApplication", "Notification job scheduled successfully.");
                } else {
                    Log.e("MyApplication", "Failed to schedule notification job.");
                }
            }
        }
    }

    /**
     * Checks whether the application has permission to send notifications.
     *
     * @return {@code true} if the permission is granted, {@code false} otherwise.
     */
    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    // The following lifecycle methods are not used but must be implemented.
    @Override public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) { }
    @Override public void onActivityResumed(@NonNull Activity activity) { }
    @Override public void onActivityPaused(@NonNull Activity activity) { }
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) { }
    @Override public void onActivityDestroyed(@NonNull Activity activity) { }
}