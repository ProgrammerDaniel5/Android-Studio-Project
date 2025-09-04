package com.example.financeapp;

import androidx.annotation.NonNull;

/**
 * The {@code Goal} class represents a financial or personal target that a user wants to achieve.
 * It encapsulates details such as the goal's name, description, amount, category, optional due date,
 * and an optional voice note recording path. This class provides standard getters and setters for
 * each field, along with a human-readable {@code toString()} method for display purposes.
 */
public class Goal {
    private int goalId;
    private String goalName;
    private String goalDescription;
    private double goalAmount;
    private String goalCategory;
    private String goalDueDate; // Optional: can be set after creation
    private String voiceNotePath;

    /**
     * Constructs a new {@code Goal} with the specified values.
     *
     * @param goalId          the unique identifier for the goal.
     * @param goalName        the name of the goal.
     * @param goalDescription a brief description of the goal.
     * @param goalAmount      the target amount for the goal.
     * @param goalCategory    the category or type of the goal.
     * @param goalDueDate     the due date for the goal (optional, format: DD/MM/YYYY). Pass an empty string if not applicable.
     * @param voiceNotePath   the file path to an associated voice note (if any).
     */
    public Goal(int goalId, String goalName, String goalDescription, double goalAmount,
                String goalCategory, String goalDueDate, String voiceNotePath) {
        this.goalId = goalId;
        this.goalName = goalName;
        this.goalDescription = goalDescription;
        this.goalAmount = goalAmount;
        this.goalCategory = goalCategory;
        this.goalDueDate = goalDueDate;
        this.voiceNotePath = voiceNotePath;
    }

    /**
     * Returns the unique identifier for the goal.
     *
     * @return the goalId.
     */
    public int getGoalId() {
        return goalId;
    }

    /**
     * Sets the unique identifier for the goal.
     *
     * @param goalId the goalId to set.
     */
    public void setGoalId(int goalId) {
        this.goalId = goalId;
    }

    /**
     * Returns the name of the goal.
     *
     * @return the goalName.
     */
    public String getGoalName() {
        return goalName;
    }

    /**
     * Sets the name of the goal.
     *
     * @param goalName the goalName to set.
     */
    public void setGoalName(String goalName) {
        this.goalName = goalName;
    }

    /**
     * Returns the description of the goal.
     *
     * @return the goalDescription.
     */
    public String getGoalDescription() {
        return goalDescription;
    }

    /**
     * Sets the goal's description.
     *
     * @param goalDescription the goalDescription to set.
     */
    public void setGoalDescription(String goalDescription) {
        this.goalDescription = goalDescription;
    }

    /**
     * Returns the target amount of the goal.
     *
     * @return the goalAmount.
     */
    public double getGoalAmount() {
        return goalAmount;
    }

    /**
     * Sets the target amount for the goal.
     *
     * @param goalAmount the goalAmount to set.
     */
    public void setGoalAmount(double goalAmount) {
        this.goalAmount = goalAmount;
    }

    /**
     * Returns the category of the goal.
     *
     * @return the goalCategory.
     */
    public String getGoalCategory() {
        return goalCategory;
    }

    /**
     * Sets the category for the goal.
     *
     * @param goalCategory the goalCategory to set.
     */
    public void setGoalCategory(String goalCategory) {
        this.goalCategory = goalCategory;
    }

    /**
     * Returns the due date for the goal.
     *
     * @return the goalDueDate.
     */
    public String getGoalDueDate() {
        return goalDueDate;
    }

    /**
     * Sets the due date for the goal.
     *
     * @param goalDueDate the goalDueDate to set.
     */
    public void setGoalDueDate(String goalDueDate) {
        this.goalDueDate = goalDueDate;
    }

    /**
     * Returns the file path for the associated voice note.
     *
     * @return the voiceNotePath.
     */
    public String getVoiceNotePath() {
        return voiceNotePath;
    }

    /**
     * Sets the file path for the associated voice note.
     *
     * @param voiceNotePath the voiceNotePath to set.
     */
    public void setVoiceNotePath(String voiceNotePath) {
        this.voiceNotePath = voiceNotePath;
    }

    /**
     * Returns a human-readable string representation of the goal.
     * The format includes the goal name, target amount, and description (if available).
     *
     * @return a string representation of the goal.
     */
    @NonNull
    @Override
    public String toString() {
        StringBuilder display = new StringBuilder(goalName + " - " + goalAmount);
        if (goalDescription != null && !goalDescription.isEmpty()) {
            display.append("\n").append(goalDescription);
        }
        return display.toString();
    }
}
