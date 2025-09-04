package com.example.financeapp;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * ChartView is a custom view for drawing various types of charts (BAR, PIE, DOT)
 * based on provided data and labels. It manages all data animations, axis drawing,
 * and interactive touch events for dot charts.
 */
public class ChartView extends View {

    /**
     * Enum for supported chart types.
     */
    public enum ChartType {
        BAR, PIE, DOT
    }

    private ChartType chartType = ChartType.BAR;
    private List<Float> data;
    private List<String> labels;
    private Paint linePaint;    // For drawing connecting lines (dot chart)

    // Paint objects for drawing axes, bars, dots, pie slices, and text.
    private Paint axisPaint, barPaint, dotPaint, piePaint, textPaint;
    private float animatedSweepAngle = 0;
    private int hoveredIndex = -1;
    // For bar charts: animatedData is used for smooth animation.
    private List<Float> animatedData = new ArrayList<>();

    // Margins for drawing (ensuring no labels get clipped)
    private final int leftMargin = 120;
    private final int rightMargin = 100;
    private final int topMargin = 50;
    private final int bottomMargin = 100;

    /**
     * Constructor for ChartView.
     *
     * @param context The Context the view is running in.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public ChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Initializes paint objects used for drawing various chart elements.
     */
    private void init() {
        // Initialize axis paint.
        axisPaint = new Paint();
        axisPaint.setColor(Color.GRAY);
        axisPaint.setStrokeWidth(3);

        // Initialize bar paint.
        barPaint = new Paint();
        barPaint.setColor(Color.BLUE);
        barPaint.setStyle(Paint.Style.FILL);

        // Initialize dot paint.
        dotPaint = new Paint();
        dotPaint.setColor(Color.GREEN);
        dotPaint.setStyle(Paint.Style.FILL);

        // Initialize pie paint.
        piePaint = new Paint();
        piePaint.setStyle(Paint.Style.FILL);

        // Initialize text paint.
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Initialize line paint for connecting dots.
        linePaint = new Paint();
        linePaint.setColor(Color.RED);
        linePaint.setStrokeWidth(4);
        linePaint.setStyle(Paint.Style.STROKE);
    }

    /**
     * Sets the data and labels for the chart.
     *
     * @param data   A List of Float values representing the chart data.
     * @param labels A List of String labels corresponding to the data.
     * @throws IllegalArgumentException if data and labels are null or of different sizes.
     */
    public void setData(List<Float> data, List<String> labels) {
        if (data == null || labels == null || data.size() != labels.size()) {
            throw new IllegalArgumentException("Data and labels must be non-null and have the same size.");
        }
        this.data = data;
        this.labels = labels;
        hoveredIndex = -1;
        animatedData = new ArrayList<>(Collections.nCopies(data.size(), 0f));
        invalidate();
    }

    /**
     * Sets the type of chart to display.
     *
     * @param chartType The chart type (BAR, PIE, DOT).
     */
    public void setChartType(ChartType chartType) {
        this.chartType = chartType;
        invalidate();
    }

    /**
     * Draws the chart based on the selected type (BAR, PIE, DOT).
     *
     * @param canvas The Canvas on which to draw the chart.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        // Clear the canvas with a white background.
        canvas.drawColor(Color.WHITE);

        if (data == null || data.isEmpty()) {
            canvas.drawText("No data available", getWidth() / 2f, getHeight() / 2f, textPaint);
            return;
        }

        // Draw selected chart type.
        switch (chartType) {
            case BAR:
                drawBarChart(canvas);
                break;
            case PIE:
                drawPieChart(canvas);
                drawPieLegend(canvas);
                break;
            case DOT:
                drawDotChart(canvas);
                break;
        }
    }

    // ----------------------------
    // Animation Methods
    // ----------------------------

    /**
     * Animates the bar chart by interpolating the data values.
     */
    public void animateBars() {
        if (data == null || data.isEmpty()) return;
        animatedData = new ArrayList<>(Collections.nCopies(data.size(), 0f));
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            for (int i = 0; i < data.size(); i++) {
                animatedData.set(i, data.get(i) * progress);
            }
            invalidate();
        });
        animator.start();
    }

    /**
     * Animates the pie chart by incrementing the sweep angle.
     */
    public void animatePieChart() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 360f);
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> {
            animatedSweepAngle = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    // ----------------------------
    // Axis Drawing (for Bar and Dot Charts)
    // ----------------------------

    /**
     * Draws the X and Y axes along with tick marks and labels for bar and dot charts.
     *
     * @param canvas The Canvas on which to draw the axes.
     */
    private void drawAxis(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        // Draw Y-axis.
        canvas.drawLine(leftMargin, topMargin, leftMargin, height - bottomMargin, axisPaint);
        // Draw X-axis.
        canvas.drawLine(leftMargin, height - bottomMargin, width - rightMargin, height - bottomMargin, axisPaint);

        // Determine maximum data value for scaling.
        float maxVal = Collections.max(data);
        int steps;
        if (maxVal <= 5) {
            steps = (int) Math.ceil(maxVal);
            if (steps < 1) steps = 1;
        } else {
            steps = 5;
        }

        // Draw tick marks (the '-' things) along the Y-axis.
        textPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= steps; i++) {
            float val = (maxVal * i) / steps;
            float yPos = height - bottomMargin - ((maxVal > 0 ? (val / maxVal) : 0) * (height - topMargin - bottomMargin));
            canvas.drawLine(leftMargin, yPos, leftMargin - 8, yPos, axisPaint);
            canvas.drawText(formatYAxisValue(val), leftMargin - 10, yPos + 8, textPaint);
        }
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * Formats a given Y-axis value. For large numbers it converts them to "K" notation.
     *
     * @param value The value to format.
     * @return A String representation of the value.
     */
    private String formatYAxisValue(float value) {
        if (value >= 1000) {
            return String.format(Locale.getDefault(), "%.1fK", value / 1000f);
        }
        return String.valueOf(Math.round(value));
    }

    // ----------------------------
    // Bar Chart Drawing
    // ----------------------------

    /**
     * Draws a bar chart based on the current data and animated data.
     *
     * @param canvas The Canvas on which to draw the bar chart.
     */
    private void drawBarChart(Canvas canvas) {
        drawAxis(canvas);
        int width = getWidth();
        int height = getHeight();
        float maxVal = Collections.max(data);
        int barCount = data.size();
        float availableWidth = width - leftMargin - rightMargin;
        float barWidth = availableWidth / barCount;

        for (int i = 0; i < barCount; i++) {
            float value = animatedData.get(i);
            float barHeight = (value / maxVal) * (height - topMargin - bottomMargin);
            float left = leftMargin + i * barWidth + barWidth * 0.1f;
            float right = leftMargin + (i + 1) * barWidth - barWidth * 0.1f;
            float top = height - bottomMargin - barHeight;
            float bottom = height - bottomMargin;
            canvas.drawRect(left, top, right, bottom, barPaint);
            float barCenter = leftMargin + i * barWidth + barWidth / 2f;
            canvas.drawText(labels.get(i), barCenter, height - bottomMargin + 25, textPaint);
        }
    }

    // ----------------------------
    // Pie Chart Drawing and Legend
    // ----------------------------

    /**
     * Draws a pie chart using the provided data and animated sweep angle.
     *
     * @param canvas The Canvas on which to draw the pie chart.
     */
    private void drawPieChart(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        int radius = Math.min(width - leftMargin - rightMargin, height - topMargin - bottomMargin) / 3;
        float cx = width / 2f;
        float cy = height / 2f;
        float total = 0;
        for (Float v : data) {
            total += v;
        }
        float startAngle = 0;
        for (int i = 0; i < data.size(); i++) {
            float sweepAngle = (data.get(i) / total) * animatedSweepAngle;
            piePaint.setColor(getColorForIndex(i));
            canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius,
                    startAngle, sweepAngle, true, piePaint);
            startAngle += sweepAngle;
        }
    }

    /**
     * Draws the legend for the pie chart, displaying labels and percentages.
     *
     * @param canvas The Canvas on which to draw the legend.
     */
    private void drawPieLegend(Canvas canvas) {
        int boxSize = 30;
        int gap = 10;
        float legendStartX = 10;
        float legendStartY = topMargin + 10;

        textPaint.setTextAlign(Paint.Align.LEFT);
        float total = 0;
        for (Float v : data) {
            total += v;
        }
        for (int i = 0; i < labels.size(); i++) {
            piePaint.setColor(getColorForIndex(i));
            float itemY = legendStartY + i * (boxSize + gap);
            canvas.drawRect(legendStartX, itemY, legendStartX + boxSize, itemY + boxSize, piePaint);
            float percent = (data.get(i) / total) * 100;
            String percentStr = (percent < 0.1f) ? "<0.1%" : String.format(Locale.getDefault(), "%.1f", percent) + "%";
            String legendText = labels.get(i) + " (" + percentStr + ")";
            canvas.drawText(legendText, legendStartX + boxSize + 10, itemY + boxSize * 0.8f, textPaint);
        }
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    // ----------------------------
    // Dot Chart Drawing
    // ----------------------------

    /**
     * Draws a dot chart by plotting data points, connecting them with lines,
     * and displaying a tooltip when a dot is hovered.
     *
     * @param canvas The Canvas on which to draw the dot chart.
     */
    private void drawDotChart(Canvas canvas) {
        drawAxis(canvas);

        int width = getWidth();
        int height = getHeight();
        float maxVal = Collections.max(data);
        int pointCount = data.size();
        float availableWidth = width - leftMargin - rightMargin;

        // Draw x-axis tick marks for each data point.
        for (int i = 0; i < pointCount; i++) {
            float x = (pointCount == 1)
                    ? (leftMargin + availableWidth / 2f)
                    : leftMargin + (availableWidth * i / (pointCount - 1));
            canvas.drawLine(x, height - bottomMargin, x, height - bottomMargin + 8, axisPaint);
        }

        ArrayList<Float> dotXs = new ArrayList<>();
        ArrayList<Float> dotYs = new ArrayList<>();

        // Compute coordinates for the dots.
        for (int i = 0; i < pointCount; i++) {
            float x = (pointCount == 1)
                    ? (leftMargin + availableWidth / 2f)
                    : leftMargin + (availableWidth * i / (pointCount - 1));
            float y = height - bottomMargin - ((data.get(i) / maxVal) * (height - topMargin - bottomMargin));
            dotXs.add(x);
            dotYs.add(y);
            float radius = (hoveredIndex == i) ? 15 : 8;
            canvas.drawCircle(x, y, radius, dotPaint);
        }

        // Draw lines connecting the dots.
        for (int i = 0; i < dotXs.size() - 1; i++) {
            canvas.drawLine(dotXs.get(i), dotYs.get(i), dotXs.get(i + 1), dotYs.get(i + 1), linePaint);
        }

        // Draw a tooltip if a dot is hovered.
        if (hoveredIndex != -1 && hoveredIndex < data.size()) {
            float x = (pointCount == 1)
                    ? (leftMargin + availableWidth / 2f)
                    : leftMargin + (availableWidth * hoveredIndex / (pointCount - 1));
            float y = height - bottomMargin - ((data.get(hoveredIndex) / maxVal) * (height - topMargin - bottomMargin));
            String tooltip = "(" + labels.get(hoveredIndex) + ", " + data.get(hoveredIndex) + ")";
            float textWidth = textPaint.measureText(tooltip);
            float textHeight = textPaint.getTextSize();
            float padding = 8;
            float bubbleWidth = textWidth + 2 * padding;
            float bubbleHeight = textHeight + 2 * padding;
            float bubbleX = x + 10;
            float bubbleY = y - bubbleHeight - 10;
            if (bubbleX + bubbleWidth > width - rightMargin) {
                bubbleX = x - bubbleWidth - 10;
            }
            if (bubbleY < topMargin) {
                bubbleY = y + 10;
            }
            Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bubblePaint.setColor(Color.argb(200, 255, 255, 255));
            bubblePaint.setStyle(Paint.Style.FILL);
            Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            borderPaint.setColor(Color.BLACK);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(2);
            RectF bubbleRect = new RectF(bubbleX, bubbleY, bubbleX + bubbleWidth, bubbleY + bubbleHeight);
            canvas.drawRoundRect(bubbleRect, 8, 8, bubblePaint);
            canvas.drawRoundRect(bubbleRect, 8, 8, borderPaint);
            float textX = bubbleX + bubbleWidth / 2;
            float textY = bubbleY + bubbleHeight / 2 + textHeight / 3;
            canvas.drawText(tooltip, textX, textY, textPaint);
        }
    }

    /**
     * Returns a color from a predefined palette based on the given index.
     *
     * @param index The index used to select the color.
     * @return The color corresponding to the provided index.
     */
    private int getColorForIndex(int index) {
        int[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.CYAN, Color.YELLOW};
        return colors[index % colors.length];
    }

    // ----------------------------
    // Touch Handling for Dot Chart Interactivity
    // ----------------------------

    /**
     * Handles touch events for the DOT chart, detecting touch proximity to data points.
     *
     * @param event The MotionEvent representing the touch.
     * @return True if a dot was touched and the event was handled, otherwise the default behavior.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (chartType != ChartType.DOT) {
            return super.onTouchEvent(event);
        }
        float tx = event.getX();
        float ty = event.getY();
        int width = getWidth();
        int height = getHeight();
        float availableWidth = width - leftMargin - rightMargin;
        for (int i = 0; i < data.size(); i++) {
            float x = (data.size() == 1)
                    ? (leftMargin + availableWidth / 2f)
                    : leftMargin + (availableWidth * i / (data.size() - 1));
            float y = height - bottomMargin - ((data.get(i) / Collections.max(data)) * (height - topMargin - bottomMargin));
            if (Math.abs(tx - x) < 20 && Math.abs(ty - y) < 20) {
                hoveredIndex = i;
                invalidate();
                performClick();
                return true;
            }
        }
        hoveredIndex = -1;
        invalidate();
        return super.onTouchEvent(event);
    }

    /**
     * Ensures proper click event handling for accessibility.
     */
    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
