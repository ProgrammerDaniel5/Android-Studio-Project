package com.example.financeapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Splash activity displays the splash screen and manages the transition to the next screen.
 *
 * When this activity is created, it enables edge-to-edge display and sets the layout.
 * After a delay of 2 seconds, it checks the user's "remember me" preference and stored username.
 * If "remember me" is selected and a username is available, the app launches the MainActivity.
 * Otherwise, it launches the Entrance activity.
 */
public class SplashActivity extends AppCompatActivity {

    /**
     * Called when the Splash activity is created.
     *
     * @param savedInstanceState Bundle containing the previously saved state of the activity, or null if none.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Delay execution for 2000 milliseconds (2 seconds) before navigating to the next activity.
        new Handler().postDelayed(() -> {
            SharedPreferences preferences = getSharedPreferences("FinanceAppPreferences", Context.MODE_PRIVATE);
            boolean rememberMe = preferences.getBoolean("remember_me", false);
            String storedUsername = preferences.getString("username", null);
            Intent intent;
            // If "remember me" is selected and there is a stored username, go to the main screen.
            if (rememberMe && storedUsername != null) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                // Otherwise, go to the entrance screen.
                intent = new Intent(SplashActivity.this, EntranceActivity.class);
            }
            startActivity(intent);
            finish(); // Finish the splash activity so it is removed from the back stack.
        }, 2000);
    }
}