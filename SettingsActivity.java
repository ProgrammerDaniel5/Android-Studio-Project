package com.example.financeapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

/**
 * The Settings activity allows the user to change the application language and access password change options.
 * It adjusts the app's locale on the fly based on user selection and manages UI interactions.
 */
public class SettingsActivity extends AppCompatActivity {

    private Button settingsReturnButton, changePasswordButton;
    private Spinner languageSpinner;
    // Flag to skip the spinner's initial callback.
    private boolean isSpinnerInitialized = false;

    /**
     * Attaches a new base context with the appropriate locale configuration.
     * Reads the saved language preference (default "en") and updates the configuration accordingly.
     *
     * @param newBase the new base context for this activity
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("FinanceAppPreferences", Context.MODE_PRIVATE);
        String lang = prefs.getString("lang", "en");
        Locale newLocale;
        if (lang.equals("iw") || lang.equals("he")) {
            newLocale = new Locale("iw");
        } else if (lang.equals("ru")) {
            newLocale = new Locale("ru");
        } else {
            newLocale = new Locale("en");
        }
        Locale.setDefault(newLocale);

        Configuration config = newBase.getResources().getConfiguration();
        config.setLocale(newLocale);
        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }

    /**
     * Called when the activity is starting. Sets up the UI, retrieves saved preferences,
     * initializes views, and sets up necessary listeners.
     *
     * @param savedInstanceState if the activity is being re-initialized after previously being shut down, then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        initViews();
        setupLanguageSpinner();
        initListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Binds the layout views to the corresponding member variables.
     */
    private void initViews() {
        settingsReturnButton = findViewById(R.id.settingsReturnButton);
        changePasswordButton = findViewById(R.id.changePasswordButton);
        languageSpinner = findViewById(R.id.languageSpinner);
    }

    /**
     * Sets up the language spinner with three options ("English", "Hebrew", and "Russian"),
     * and selects the currently saved language.
     */
    private void setupLanguageSpinner() {
        final String[] languages = new String[]{"English", "Hebrew", "Russian"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, languages);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        // Read the currently saved language (default is "en") and set the appropriate spinner position.
        SharedPreferences sharedPreferences = getSharedPreferences("FinanceAppPreferences", Context.MODE_PRIVATE);
        String savedLang = sharedPreferences.getString("lang", "en");
        int selection;
        if (savedLang.equals("iw") || savedLang.equals("he")) {
            selection = 1;
        } else if (savedLang.equals("ru")) {
            selection = 2;
        } else {
            selection = 0;
        }
        languageSpinner.setSelection(selection);

        // Set the listener to react when a user selects a language.
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Skip initial callback.
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true;
                    return;
                }
                String selectedLanguage = languages[position];
                String langCode;
                if (selectedLanguage.equals("Hebrew")) {
                    langCode = "iw";
                } else if (selectedLanguage.equals("Russian")) {
                    langCode = "ru";
                } else {
                    langCode = "en";
                }
                // Only change locale if the new language differs from the saved one.
                String currentLang = getSharedPreferences("FinanceAppPreferences", Context.MODE_PRIVATE)
                        .getString("lang", "en");
                if (!langCode.equals(currentLang)) {
                    changeLocale(langCode);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed.
            }
        });
    }

    /**
     * Initializes click listeners for the return and change password buttons.
     */
    private void initListeners() {
        // Return to MainActivity when the return button is clicked.
        settingsReturnButton.setOnClickListener(view -> {
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // Start the ChangePassword activity.
        changePasswordButton.setOnClickListener(view -> {
            Intent intent = new Intent(SettingsActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Changes the locale of the application.
     * Saves the new language setting to SharedPreferences, shows a toast message,
     * and then recreates the activity to apply the locale change.
     *
     * @param langCode The locale code to switch to ("en", "iw", or "ru").
     */
    private void changeLocale(String langCode) {
        // Save the new language preference.
        SharedPreferences sharedPreferences = getSharedPreferences("FinanceAppPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("lang", langCode);
        editor.apply();

        String langName;
        if (langCode.equals("iw")) {
            langName = "Hebrew";
        } else if (langCode.equals("ru")) {
            langName = "Russian";
        } else {
            langName = "English";
        }
        Toast.makeText(this, "Language changed to " + langName, Toast.LENGTH_SHORT).show();
        // Recreate the activity to apply the new locale settings.
        recreate();
    }
}
