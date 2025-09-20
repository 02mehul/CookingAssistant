package com.example.cookingassistant.ui;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class PantryGaugeView extends View {
    private int percentage = 0;
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    public PantryGaugeView(Context c) { super(c); init(); }
    public PantryGaugeView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        bg.setStyle(Paint.Style.STROKE); bg.setStrokeWidth(36f); bg.setColor(0x22000000);
        fg.setStyle(Paint.Style.STROKE); fg.setStrokeWidth(36f);
        fg.setStrokeCap(Paint.Cap.ROUND); fg.setColor(0xFF6750A4); // M3 primary-ish
        text.setTextAlign(Paint.Align.CENTER); text.setTextSize(52f); text.setColor(0xFF222222);
    }

    public void setPercentage(int p) { percentage = Math.max(0, Math.min(100, p)); invalidate(); }

    @Override protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight(), size = Math.min(w, h);
        int cx = w/2, cy = h/2, r = size/2 - 28;
        oval.set(cx - r, cy - r, cx + r, cy + r);
        c.drawArc(oval, 135, 270, false, bg);
        float sweep = 270f * percentage / 100f;
        c.drawArc(oval, 135, sweep, false, fg);
        c.drawText(percentage + "% stocked", cx, cy + 16, text);
    }
}
