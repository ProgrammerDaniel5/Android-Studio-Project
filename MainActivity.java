package com.example.financeapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * MainActivity is the central hub of the app. It displays the user's greeting, balance, and charts
 * that visualize the transaction data. The user can filter data by time and select different chart types.
 * Additionally, navigation buttons and an options menu allow moving to other sections (Goals Tracker,
 * ReportsActivity, ProfileActivity, Accounts, etc.) and handling user actions like logout.
 */
public class MainActivity extends AppCompatActivity {

    // UI components.
    private Button toGoalsTrackerButton, toReportsButton, barChartButton, pieChartButton, dotChartButton, filterButton;
    private TextView welcomeTextView, balanceTextView;
    private Spinner timeFilterSpinner, filterModeSpinner;
    private ChartView chartView;

    // Data and helper members.
    private DatabaseHelper db;
    private String username;
    private SharedPreferences sharedPreferences;
    private ChartView.ChartType currentChartType = ChartView.ChartType.BAR;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 123;

    // Enum for Time Filters
    private enum TimeFilter {
        ALL_TIME,
        TODAY,
        THIS_WEEK,
        THIS_MONTH,
        THIS_YEAR
    }

    // Enum for Filter Modes
    private enum FilterMode {
        AMOUNT,
        COUNT
    }

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
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize shared preferences, username, and database.
        initData();

        // Bind UI elements.
        initViews();

        // Setup spinner adapters.
        initSpinnerAdapters();

        // Setup button listeners.
        initListeners();

        // Initial chart update (default: Bar Chart by Amount).
        updateBarChartDataByAmount();

        // Set welcome and balance text.
        updateUserInfo();

        // Check and request notification permission.
        checkAndRequestNotificationPermission();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });
    }

    /**
     * Initializes shared preferences, username, and database helper.
     */
    private void initData() {
        sharedPreferences = getSharedPreferences("FinanceAppPreferences", Context.MODE_PRIVATE);
        username = sharedPreferences.getString("username", null);
        db = new DatabaseHelper(this);
    }

    /**
     * Binds XML views from the layout to the corresponding member variables.
     */
    private void initViews() {
        chartView = findViewById(R.id.ChartView);
        barChartButton = findViewById(R.id.buttonBarChart);
        pieChartButton = findViewById(R.id.buttonPieChart);
        dotChartButton = findViewById(R.id.buttonDotsChart);
        filterButton = findViewById(R.id.buttonFilter);
        filterModeSpinner = findViewById(R.id.filterModeSpinner);
        timeFilterSpinner = findViewById(R.id.timeFilterSpinner);
        toGoalsTrackerButton = findViewById(R.id.MainActivityToGoalsTrackerButton);
        toReportsButton = findViewById(R.id.MainActivityToReportsButton);
        welcomeTextView = findViewById(R.id.welcomeTextView);
        balanceTextView = findViewById(R.id.balanceTextView);
    }

    /**
     * Sets up adapters for the filter mode and time filter spinners.
     */
    private void initSpinnerAdapters() {
        ArrayAdapter<CharSequence> modeAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.filter_modes,
                R.layout.spinner_item);
        modeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        filterModeSpinner.setAdapter(modeAdapter);

        ArrayAdapter<CharSequence> timeAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.time_filters,
                R.layout.spinner_item);
        timeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        timeFilterSpinner.setAdapter(timeAdapter);
    }

    /**
     * Sets up listeners for button clicks and other UI interactions.
     */
    private void initListeners() {
        // Chart type button listeners.
        barChartButton.setOnClickListener(v -> {
            currentChartType = ChartView.ChartType.BAR;
            FilterMode mode = FilterMode.values()[filterModeSpinner.getSelectedItemPosition()];
            if (mode == FilterMode.COUNT) {
                updateBarChartDataByCount();
            } else {
                updateBarChartDataByAmount();
            }
        });

        pieChartButton.setOnClickListener(v -> {
            currentChartType = ChartView.ChartType.PIE;
            FilterMode mode = FilterMode.values()[filterModeSpinner.getSelectedItemPosition()];
            if (mode == FilterMode.COUNT) {
                updatePieChartDataByCount();
            } else {
                updatePieChartDataByAmount();
            }
        });

        dotChartButton.setOnClickListener(v -> {
            currentChartType = ChartView.ChartType.DOT;
            FilterMode mode = FilterMode.values()[filterModeSpinner.getSelectedItemPosition()];
            if (mode == FilterMode.COUNT) {
                updateDotChartDataByCount();
            } else {
                updateDotChartDataByIndex();
            }
        });

        // Filter button listener.
        filterButton.setOnClickListener(v -> {
            FilterMode mode = FilterMode.values()[filterModeSpinner.getSelectedItemPosition()];
            switch (currentChartType) {
                case BAR:
                    if (mode == FilterMode.COUNT) {
                        updateBarChartDataByCount();
                    } else {
                        updateBarChartDataByAmount();
                    }
                    break;
                case PIE:
                    if (mode == FilterMode.COUNT) {
                        updatePieChartDataByCount();
                    } else {
                        updatePieChartDataByAmount();
                    }
                    break;
                case DOT:
                    if (mode == FilterMode.COUNT) {
                        updateDotChartDataByCount();
                    } else {
                        updateDotChartDataByIndex();
                    }
                    break;
            }
        });

        // Navigation button listeners.
        toGoalsTrackerButton.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, GoalsTrackerActivity.class)));

        toReportsButton.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, ReportsActivity.class)));
    }

    /**
     * Updates the welcome message and user balance display.
     */
    private void updateUserInfo() {
        String firstName = db.getUserItem(username, DatabaseHelper.USERS_FIRST_NAME);
        String lastName = db.getUserItem(username, DatabaseHelper.USERS_LAST_NAME);
        String welcomeMessage = getResources().getString(R.string.welcome) + ", " + firstName + " " + lastName;
        welcomeTextView.setText(welcomeMessage);
        double balance = db.sumUserIncomes(username) - db.sumUserExpenses(username);
        String balanceMessage = getResources().getString(R.string.balance) + ": " + String.format(Locale.getDefault(), "%,.2f", balance);
        balanceTextView.setText(balanceMessage);
        if (balance < 0 && balance > -1000) {
            balanceTextView.setTextColor(ContextCompat.getColor(this, R.color.orange));
        } else if (balance <= -1000) {
            balanceTextView.setTextColor(ContextCompat.getColor(this, R.color.red));
        }
    }

    // -----------------------------------------------------------------------------------
    // Options Menu Methods
    // -----------------------------------------------------------------------------------

    /**
     * Inflates the options menu.
     *
     * @param menu The menu to populate.
     * @return true if the menu is created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Handles selection of items in the options menu.
     *
     * @param item The selected menu item.
     * @return True if the selection is handled.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        if (id == R.id.action_accounts) {
            startActivity(new Intent(this, AccountsActivity.class));
            return true;
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.action_guide) {
            showGuideContentDialog();
            return true;
        }
        if (id == R.id.action_credit) {
            showCreditContentDialog();
            return true;
        }
        if (id == R.id.action_contact_me) {
            startActivity(new Intent(this, ContactMeActivity.class));
            return true;
        }
        if (id == R.id.action_logout) {
            new AlertDialog.Builder(this)
                    .setMessage(getResources().getString(R.string.are_you_sure_you_want_to_log_out))
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.yes), (dialogInterface, i) -> {
                        sharedPreferences.edit().clear().apply();
                        startActivity(new Intent(MainActivity.this, EntranceActivity.class));
                        Toast.makeText(MainActivity.this, getResources().getString(R.string.logged_out_successfully), Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton(getResources().getString(R.string.no), (dialogInterface, i) -> dialogInterface.dismiss())
                    .create()
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // -----------------------------------------------------------------------------------
    // Data Filtering Method
    // -----------------------------------------------------------------------------------

    /**
     * Filters a list of transactions based on the selected time filter.
     * Allowed values: "Today", "This Week", "This Month", "This Year", "All Time".
     *
     * @param transactions The original list of transactions.
     * @param timeFilter   The selected time filter.
     * @return The filtered list of transactions.
     */
    private ArrayList<Transaction> filterTransactionsByTime(ArrayList<Transaction> transactions, TimeFilter timeFilter) {
        if (timeFilter == TimeFilter.ALL_TIME) {
            return transactions;
        }
        ArrayList<Transaction> filtered = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        Date startDate;
        switch (timeFilter) {
            case TODAY:
                cal.setTime(now);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                startDate = cal.getTime();
                break;
            case THIS_WEEK:
                cal.setTime(now);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                startDate = cal.getTime();
                break;

            case THIS_MONTH:
                cal.setTime(now);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                startDate = cal.getTime();
                break;
            case THIS_YEAR:
                cal.setTime(now);
                cal.set(Calendar.DAY_OF_YEAR, 1);
                startDate = cal.getTime();
                break;
            default:
                return transactions;
        }
        for (Transaction t : transactions) {
            try {
                Date tDate = sdf.parse(t.getDate());
                if (tDate != null &&
                        (tDate.equals(startDate) || (tDate.after(startDate) && tDate.before(now)) || tDate.equals(now))) {
                    filtered.add(t);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return filtered;
    }

    // -----------------------------------------------------------------------------------
    // Chart Update Methods
    // -----------------------------------------------------------------------------------

    /** Updates the Pie Chart using total transaction amounts grouped by type. */
    private void updatePieChartDataByAmount() {
        ArrayList<Transaction> transactions = db.getTransactionsFromDB(username);
        TimeFilter timeFilter = TimeFilter.values()[timeFilterSpinner.getSelectedItemPosition()];
        transactions = filterTransactionsByTime(transactions, timeFilter);
        Map<String, Double> typeAmount = new LinkedHashMap<>();
        for (Transaction t : transactions) {
            String type = t.getType();
            double sum = typeAmount.getOrDefault(type, 0.0);
            typeAmount.put(type, sum + t.getAmount());
        }
        List<Float> pieData = new ArrayList<>();
        List<String> pieLabels = new ArrayList<>();
        for (Map.Entry<String, Double> entry : typeAmount.entrySet()) {
            pieLabels.add(entry.getKey());
            pieData.add(entry.getValue().floatValue());
        }
        chartView.setData(pieData, pieLabels);
        chartView.setChartType(ChartView.ChartType.PIE);
        chartView.animatePieChart();
    }

    /** Updates the Pie Chart using transaction counts grouped by type. */
    private void updatePieChartDataByCount() {
        ArrayList<Transaction> transactions = db.getTransactionsFromDB(username);
        TimeFilter timeFilter = TimeFilter.values()[timeFilterSpinner.getSelectedItemPosition()];
        transactions = filterTransactionsByTime(transactions, timeFilter);

        Map<String, Integer> typeCount = new LinkedHashMap<>();
        for (Transaction t : transactions) {
            String type = t.getType();
            int count = typeCount.getOrDefault(type, 0);
            typeCount.put(type, count + 1);
        }
        List<Float> pieData = new ArrayList<>();
        List<String> pieLabels = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
            pieLabels.add(entry.getKey());
            pieData.add((float) entry.getValue());
        }
        chartView.setData(pieData, pieLabels);
        chartView.setChartType(ChartView.ChartType.PIE);
        chartView.animatePieChart();
    }

    /**
     * Updates the Dot Chart based on sequential (index-based) order after sorting transactions
     * chronologically. Each dot represents a transaction amount.
     */
    private void updateDotChartDataByIndex() {
        ArrayList<Transaction> transactions = db.getTransactionsFromDB(username);
        TimeFilter timeFilter = TimeFilter.values()[timeFilterSpinner.getSelectedItemPosition()];
        transactions = filterTransactionsByTime(transactions, timeFilter);
        transactions.sort((t1, t2) -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            try {
                Date d1 = sdf.parse(t1.getDate());
                Date d2 = sdf.parse(t2.getDate());

                // Handle potential null values
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return -1;
                if (d2 == null) return 1;

                return d1.compareTo(d2);
            } catch (ParseException e) {
                return 0;
            }
        });
        List<Float> dotData = new ArrayList<>();
        List<String> dotLabels = new ArrayList<>();
        for (Transaction t : transactions) {
            dotData.add((float) t.getAmount());
            dotLabels.add(t.getDate());
        }
        chartView.setData(dotData, dotLabels);
        chartView.setChartType(ChartView.ChartType.DOT);
        chartView.invalidate();
    }

    /** Updates the Dot Chart by counting transactions grouped per day. */
    private void updateDotChartDataByCount() {
        ArrayList<Transaction> transactions = db.getTransactionsFromDB(username);
        TimeFilter timeFilter = TimeFilter.values()[timeFilterSpinner.getSelectedItemPosition()];
        transactions = filterTransactionsByTime(transactions, timeFilter);
        Map<String, Integer> dailyCounts = new TreeMap<>((s1, s2) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date d1 = sdf.parse(s1);
                Date d2 = sdf.parse(s2);

                // Handle potential null values
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return -1;
                if (d2 == null) return 1;

                return d1.compareTo(d2);
            } catch (ParseException e) {
                return s1.compareTo(s2);
            }
        });
        for (Transaction t : transactions) {
            String fullDate = t.getDate();
            String day = fullDate.contains(" ") ? fullDate.split(" ")[0] : fullDate;
            int count = dailyCounts.getOrDefault(day, 0);
            dailyCounts.put(day, count + 1);
        }
        List<Float> dotData = new ArrayList<>();
        List<String> dotLabels = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : dailyCounts.entrySet()) {
            dotLabels.add(entry.getKey());
            dotData.add(entry.getValue().floatValue());
        }
        chartView.setData(dotData, dotLabels);
        chartView.setChartType(ChartView.ChartType.DOT);
        chartView.invalidate();
    }

    /** Updates the Bar Chart using transaction counts grouped by type. */
    private void updateBarChartDataByCount() {
        ArrayList<Transaction> transactions = db.getTransactionsFromDB(username);
        TimeFilter timeFilter = TimeFilter.values()[timeFilterSpinner.getSelectedItemPosition()];
        transactions = filterTransactionsByTime(transactions, timeFilter);
        Map<String, Integer> typeCount = new LinkedHashMap<>();
        for (Transaction t : transactions) {
            String type = t.getType();
            int count = typeCount.getOrDefault(type, 0);
            typeCount.put(type, count + 1);
        }
        List<Float> barData = new ArrayList<>();
        List<String> barLabels = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
            barLabels.add(entry.getKey());
            barData.add((float) entry.getValue());
        }
        chartView.setData(barData, barLabels);
        chartView.setChartType(ChartView.ChartType.BAR);
        chartView.animateBars();
    }

    /** Updates the Bar Chart by summing transaction amounts grouped by type. */
    private void updateBarChartDataByAmount() {
        ArrayList<Transaction> transactions = db.getTransactionsFromDB(username);
        TimeFilter timeFilter = TimeFilter.values()[timeFilterSpinner.getSelectedItemPosition()];
        transactions = filterTransactionsByTime(transactions, timeFilter);
        Map<String, Double> typeAmount = new LinkedHashMap<>();
        for (Transaction t : transactions) {
            String type = t.getType();
            double sum = typeAmount.getOrDefault(type, 0.0);
            typeAmount.put(type, sum + t.getAmount());
        }
        List<Float> barData = new ArrayList<>();
        List<String> barLabels = new ArrayList<>();
        for (Map.Entry<String, Double> entry : typeAmount.entrySet()) {
            barLabels.add(entry.getKey());
            barData.add(entry.getValue().floatValue());
        }
        chartView.setData(barData, barLabels);
        chartView.setChartType(ChartView.ChartType.BAR);
        chartView.animateBars();
    }

    // -----------------------------------------------------------------------------------
    // Dialog Methods and Raw Content Reader
    // -----------------------------------------------------------------------------------

    /**
     * Displays the Credit Information dialog, dismissible on tapping text or image.
     */
    private void showCreditContentDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.credit_layout);
        TextView textView = dialog.findViewById(R.id.creditLayoutTextView);
        textView.setOnClickListener(v -> dialog.dismiss());
        ImageView imageView = dialog.findViewById(R.id.creditLayoutImageView);
        imageView.setOnClickListener(v -> dialog.dismiss());
        textView.setText(readContent("credit"));
        dialog.setCancelable(true);
        Window window = dialog.getWindow();
        if (window != null) {
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
    }

    /**
     * Displays the Guide dialog, dismissible on tapping the text view.
     */
    private void showGuideContentDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.guide_layout);
        TextView textView = dialog.findViewById(R.id.guideLayoutTextView);
        textView.setOnClickListener(v -> dialog.dismiss());
        textView.setText(readContent("guide"));
        dialog.setCancelable(true);

        Window window = dialog.getWindow();
        if (window != null) {
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
    }

    /**
     * Reads text content from a raw resource file.
     *
     * @param filename The name of the resource file (without extension).
     * @return The file content as a String or an error message if not found.
     */
    private String readContent(String filename) {
        StringBuilder text = new StringBuilder();

        // Explicitly map filenames to resource IDs
        int resourceId;
        switch (filename) {
            case "guide":
                resourceId = R.raw.guide;
                break;
            case "credit":
                resourceId = R.raw.credit;
                break;
            default:
                return "Resource not found: " + filename;
        }

        try {
            InputStream inputStream = getResources().openRawResource(resourceId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
            }
            reader.close();
            inputStream.close();
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
            return "Resource not found: " + filename;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return text.toString();
    }

    // -----------------------------------------------------------------------------------
    // Notification and Lifecycle Handling
    // -----------------------------------------------------------------------------------

    /**
     * Checks for and requests notification permissions.
     */
    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if we have already asked for this permission.
            boolean alreadyAsked = sharedPreferences.getBoolean("notificationPermissionAsked", false);
            if (!alreadyAsked) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_REQUEST_CODE
                    );
                    // Mark that we've asked for permission, so we don't ask again.
                    sharedPreferences.edit().putBoolean("notificationPermissionAsked", true).apply();
                }
            }
        }
    }

    /**
     * Handles the result of the notification permission request.
     *
     * @param requestCode  The request code.
     * @param permissions  The requested permissions.
     * @param grantResults The results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Notification permission granted.");
            } else {
                Log.w("MainActivity", "Notification permission denied.");
            }
        }
    }
}