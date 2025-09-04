package com.example.financeapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Locale;

/**
 * TransactionAdapter is a custom ArrayAdapter that binds Transaction objects to views
 * defined in the transaction_list_view_item layout.
 */
public class TransactionAdapter extends ArrayAdapter<Transaction> {
    private final Context context;

    /**
     * Constructs a new TransactionAdapter.
     *
     * @param context      the current context.
     * @param transactions the list of transactions to display.
     */
    public TransactionAdapter(Context context, List<Transaction> transactions) {
        super(context, 0, transactions);
        this.context = context;
    }

    /**
     * Returns a view for the given position in the list.
     * Inflates the layout if needed and populates UI elements with transaction data.
     *
     * @param position    The position of the item in the list.
     * @param convertView The recycled view to populate.
     * @param parent      The parent ViewGroup.
     * @return The populated view for the transaction.
     */
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        // Inflate the view if not already provided.
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.transaction_list_view_item, parent, false);
        }

        // Retrieve the transaction at the current position.
        Transaction transaction = getItem(position);

        // Bind UI elements.
        TextView account = convertView.findViewById(R.id.transactionAccount);
        TextView card = convertView.findViewById(R.id.transactionCard);
        TextView type = convertView.findViewById(R.id.transactionType);
        TextView category = convertView.findViewById(R.id.transactionCategory);
        TextView amount = convertView.findViewById(R.id.transactionAmount);
        TextView description = convertView.findViewById(R.id.transactionDescription);
        TextView date = convertView.findViewById(R.id.transactionDate);
        TextView subscription = convertView.findViewById(R.id.transactionSubscription);

        if (transaction != null) {
            String accountText = context.getString(R.string.account) + ": " + transaction.getAccountName();
            account.setText(accountText);
            String cardText = context.getString(R.string.card) + ": " + transaction.getCardNumber();
            card.setText(cardText);
            String typeText = context.getString(R.string.type) + ": " + transaction.getType();
            type.setText(typeText);
            String categoryText = context.getString(R.string.category) + ": " + transaction.getCategory();
            category.setText(categoryText);
            String amountText = context.getString(R.string.amount) + ": " + String.format(Locale.getDefault(), "%.2f", transaction.getAmount());
            amount.setText(amountText);
            String dateText = context.getString(R.string.date) + ": " + transaction.getDate();
            date.setText(dateText);

            if (transaction.getDescription() == null || transaction.getDescription().trim().isEmpty()) {
                description.setVisibility(View.GONE);
            } else {
                description.setVisibility(View.VISIBLE);
                String descriptionText = context.getString(R.string.description) + ": " + transaction.getDescription();
                description.setText(descriptionText);
            }

            if (transaction.isSubscription()) {
                subscription.setVisibility(View.VISIBLE);
                String subscriptionText = context.getString(R.string.subscription) + ": " + transaction.getInterval();
                subscription.setText(subscriptionText);
            } else {
                subscription.setVisibility(View.GONE);
            }
        }

        return convertView;
    }

    /**
     * Updates the adapter with a new list of transactions.
     *
     * @param newTransactions the new list of transactions.
     */
    public void updateTransactions(List<Transaction> newTransactions) {
        clear();
        addAll(newTransactions);
        notifyDataSetChanged();
    }
}
