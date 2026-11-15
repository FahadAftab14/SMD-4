package com.example.taskflow.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/**
 * Draws the coloured "Done" / "Delete" / "Remove" area that appears behind a card
 * while it is being swiped. It is drawn UNDER the row, so only the part the card
 * has moved away from is visible.
 */
public final class SwipeBackgroundPainter {

    private final Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float radius;
    private final float padding;

    public SwipeBackgroundPainter(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        radius = 16f * density;
        padding = 20f * density;
        text.setColor(Color.WHITE);
        text.setTextSize(15f * density);
        text.setFakeBoldText(true);
    }

    /**
     * @param itemView    the whole row (the ViewHolder's itemView)
     * @param card        the visible card inside the row (the row also has spacing around it)
     * @param labelOnLeft true when the user swipes right (the label appears on the left edge)
     */
    public void draw(Canvas canvas, View itemView, View card, int color, String label, boolean labelOnLeft) {
        float left = itemView.getLeft() + card.getLeft();
        float right = itemView.getLeft() + card.getRight();
        float top = itemView.getTop() + card.getTop();
        float bottom = itemView.getTop() + card.getBottom();

        background.setColor(color);
        canvas.drawRoundRect(new RectF(left, top, right, bottom), radius, radius, background);

        text.setTextAlign(labelOnLeft ? Paint.Align.LEFT : Paint.Align.RIGHT);
        float x = labelOnLeft ? left + padding : right - padding;
        float y = (top + bottom) / 2f - (text.ascent() + text.descent()) / 2f;
        canvas.drawText(label, x, y, text);
    }
}
