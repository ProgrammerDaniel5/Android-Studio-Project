package com.example.financeapp;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;

/**
 * GoalsAdapter is a custom ArrayAdapter that binds Goal objects to a ListView.
 * It displays goal details including description, an optional due date, a dynamic visual progress indicator,
 * and a voice note playback control with animated feedback.
 */
public class GoalsAdapter extends ArrayAdapter<Goal> {
    private final DatabaseHelper dbHelper;
    private final String username;

    /**
     * Constructs a new GoalsAdapter.
     *
     * @param context   The current context.
     * @param goals     The list of Goal objects to display.
     * @param dbHelper  The helper instance for database operations (e.g., net transaction calculations).
     * @param username  The username used to filter database queries.
     */
    public GoalsAdapter(Context context, ArrayList<Goal> goals, DatabaseHelper dbHelper, String username) {
        super(context, 0, goals);
        this.dbHelper = dbHelper;
        this.username = username;
    }

    /**
     * ViewHolder is a static inner class that caches the views for each goal item.
     * This pattern improves performance and reinforces the separation between UI layout and logic.
     */
    private static class ViewHolder {
        TextView txtGoalItem;
        TextView txtGoalDueDate;
        View progressFill;
        ImageButton btnPlayVoice;
        androidx.cardview.widget.CardView cardView;
    }

    /**
     * Provides a view for an individual Goal item in the ListView.
     * Uses the ViewHolder pattern for efficient view recycling and maintenance.
     *
     * @param position    The position of the item within the adapter's data set.
     * @param convertView The recycled view to populate.
     * @param parent      The parent view that this view will eventually be attached to.
     * @return A fully configured view displaying the given Goal's information.
     */
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final Goal goal = getItem(position);
        ViewHolder holder;

        // Inflate and cache view components if needed.
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.goals_list_view_item, parent, false);
            holder = new ViewHolder();
            holder.txtGoalItem = convertView.findViewById(R.id.txtGoalItem);
            holder.txtGoalDueDate = convertView.findViewById(R.id.txtGoalDueDate);
            holder.progressFill = convertView.findViewById(R.id.progressFill);
            holder.btnPlayVoice = convertView.findViewById(R.id.btnPlayVoice);
            holder.cardView = (androidx.cardview.widget.CardView) convertView;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (goal != null) {
            // 1. Bind textual goal information (name, amount, and description).
            StringBuilder goalText = new StringBuilder();
            goalText.append(goal.getGoalName()).append(" - ").append(goal.getGoalAmount());
            if (goal.getGoalDescription() != null && !goal.getGoalDescription().isEmpty()) {
                goalText.append("\n").append(goal.getGoalDescription());
            }
            holder.txtGoalItem.setText(goalText.toString());

            // 2. Display due date separately.
            if (goal.getGoalDueDate() != null && !goal.getGoalDueDate().trim().isEmpty()) {
                String dueText = "Due: " + goal.getGoalDueDate();
                holder.txtGoalDueDate.setText(dueText);
                holder.txtGoalDueDate.setVisibility(View.VISIBLE);
            } else {
                holder.txtGoalDueDate.setVisibility(View.GONE);
            }

            // 3. Calculate progress percentage and update related UI elements.
            int progressPercentage = calculateProgressPercentage(goal);
            updateProgressDisplay(convertView, holder.progressFill, progressPercentage);
            updateCardBackground(holder.cardView, progressPercentage);

            // 4. Set up voice note playback (if available) with animated feedback.
            holder.btnPlayVoice.setOnClickListener(view -> handleVoiceNotePlayback(holder.btnPlayVoice, goal));
        }

        return convertView;
    }

    /**
     * Calculates the progress percentage for a goal by comparing net transactions to the goal amount.
     *
     * @param goal A Goal object containing the target amount and category.
     * @return An integer value between 0 and 100 indicating the progress percentage.
     */
    private int calculateProgressPercentage(Goal goal) {
        int percentage = 0;
        if (goal.getGoalAmount() > 0) {
            double netTotal = dbHelper.getNetTransactionsForCategory(username, goal.getGoalCategory());
            percentage = (int) Math.min(100, (netTotal / goal.getGoalAmount()) * 100);
        }
        return percentage;
    }

    /**
     * Updates the width of the progress fill view to visually indicate the goal's progress.
     *
     * @param parentView       The parent view containing the progress indicator.
     * @param progressFillView The view whose width represents the current progress.
     * @param percentage       The percentage (0–100) the progress fill should cover.
     */
    private void updateProgressDisplay(final View parentView, final View progressFillView, final int percentage) {
        parentView.post(() -> {
            int totalWidth = parentView.getWidth();
            int newWidth = (totalWidth * percentage) / 100;
            progressFillView.getLayoutParams().width = newWidth;
            progressFillView.requestLayout();
        });
    }

    /**
     * Updates the CardView background color based on the current progress.
     * Changes to a distinctive color if the goal is reached (100% or more); otherwise, uses a default color.
     *
     * @param cardView   The CardView displaying the goal.
     * @param percentage The current progress percentage.
     */
    private void updateCardBackground(androidx.cardview.widget.CardView cardView, int percentage) {
        int colorResId = (percentage >= 100) ? R.color.goalReachedColor : R.color.defaultCardBackgroundColor;
        cardView.setCardBackgroundColor(ContextCompat.getColor(getContext(), colorResId));
    }

    /**
     * Handles the voice note playback process for a goal. If a voice note is already playing, it stops playback;
     * otherwise, it initializes and starts media playback with UI animation feedback.
     *
     * @param btnPlayVoice The button that triggers voice note playback.
     * @param goal         The associated Goal object containing the voice note file path.
     */
    private void handleVoiceNotePlayback(final ImageButton btnPlayVoice, Goal goal) {
        MediaPlayer currentPlayer = (MediaPlayer) btnPlayVoice.getTag();
        if (currentPlayer != null && currentPlayer.isPlaying()) {
            // Stop and clean up any active playback.
            currentPlayer.stop();
            currentPlayer.release();
            btnPlayVoice.clearAnimation();
            btnPlayVoice.setImageResource(R.drawable.play_arrow_icon);
            btnPlayVoice.setTag(null);
            return;
        }

        String voicePath = goal.getVoiceNotePath();
        if (voicePath != null && !voicePath.isEmpty()) {
            MediaPlayer mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(voicePath);
                mediaPlayer.prepare();
                mediaPlayer.start();
                // Store the MediaPlayer instance using the button's tag.
                btnPlayVoice.setTag(mediaPlayer);
                btnPlayVoice.setImageResource(R.drawable.pause_icon);
                startPulsatingAnimation(btnPlayVoice);
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    btnPlayVoice.clearAnimation();
                    btnPlayVoice.setImageResource(R.drawable.play_arrow_icon);
                    btnPlayVoice.setTag(null);
                });
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Unable to play recording", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "No recording available", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Starts a pulsating scale animation on the voice playback button to visually indicate active audio playback.
     *
     * @param btnPlayVoice The ImageButton to animate.
     */
    private void startPulsatingAnimation(ImageButton btnPlayVoice) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1.0f, 1.2f,
                1.0f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(500);
        scaleAnimation.setRepeatMode(Animation.REVERSE);
        scaleAnimation.setRepeatCount(Animation.INFINITE);
        btnPlayVoice.startAnimation(scaleAnimation);
    }
}
