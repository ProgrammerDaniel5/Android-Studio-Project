package com.example.financeapp;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

/**
 * Entrance activity presents the user with two options:
 * navigate to the sign-up screen or the sign-in screen.
 * This activity binds the Sign Up and Sign In buttons, and assigns
 * click listeners that launch the corresponding activities.
 */
public class EntranceActivity extends AppCompatActivity {

    /**
     * Forces the activity's locale to English.
     * This method ensures that all displayed text uses English regardless of any device or user settings.
     *
     * @param newBase the new base context for this activity
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        Locale englishLocale = new Locale("en");
        Locale.setDefault(englishLocale);
        Configuration config = newBase.getResources().getConfiguration();
        config.setLocale(englishLocale);
        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }

    /**
     * Called when the Entrance activity is created.
     * Sets up the layout, initializes buttons, assigns click listeners
     * for navigation, and applies window insets to accommodate system bars.
     *
     * @param savedInstanceState Bundle containing the activity's previous state, or null if new.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrance);

        Button signUpButton = findViewById(R.id.toSignUpButton);
        Button signInButton = findViewById(R.id.toSignInButton);

        signUpButton.setOnClickListener(view -> {
            Intent intent = new Intent(EntranceActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        signInButton.setOnClickListener(view -> {
            Intent intent = new Intent(EntranceActivity.this, SignInActivity.class);
            startActivity(intent);
        });

        // Apply window insets to the main view so the content properly avoids system bars.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}