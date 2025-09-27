package com.example.cookingassistant.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Ring gauge (0–100%). Call setProgress(int) to update.
 */
public class PantryGaugeView extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arc = new RectF();

    private int progress = 0;         // 0..100
    private float stroke = dp(14);    // ring thickness

    // arc geometry (leave a 90° gap at top-right like the screenshots)
    private static final float START_ANGLE = 135f; // degrees
    private static final float SWEEP_TOTAL = 270f; // degrees

    public PantryGaugeView(Context ctx) { super(ctx); init(); }
    public PantryGaugeView(Context ctx, @Nullable AttributeSet attrs) { super(ctx, attrs); init(); }
    public PantryGaugeView(Context ctx, @Nullable AttributeSet attrs, int defStyleAttr) { super(ctx, attrs, defStyleAttr); init(); }

    private void init() {
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeCap(Paint.Cap.ROUND);
        track.setStrokeWidth(stroke);
        track.setColor(0xFFDFDFDF); // light grey

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setStrokeWidth(stroke);
        progressPaint.setColor(0xFF7E57C2); // purple-ish; change if you like

        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(dp(14));
        text.setColor(0xFF444444);
    }

    /** Public API used by HomeFragment */
    public void setProgress(int value) {
        int clamped = Math.max(0, Math.min(100, value));
        if (clamped != this.progress) {
            this.progress = clamped;
            invalidate();
        }
    }

    /** Optional accessor if you ever need it */
    public int getProgress() { return progress; }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float pad = stroke / 2f + dp(6); // keep ends visible
        arc.set(pad, pad, w - pad, h - pad);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);

        // Track (full arc)
        c.drawArc(arc, START_ANGLE, SWEEP_TOTAL, false, track);

        // Progress
        float sweep = SWEEP_TOTAL * (progress / 100f);
        c.drawArc(arc, START_ANGLE, sweep, false, progressPaint);

        // Center text (optional; harmless if you show percent elsewhere)
        // Comment these two lines out if you don't want text in the ring.
        Paint.FontMetrics fm = text.getFontMetrics();
        float y = getHeight() / 2f - (fm.ascent + fm.descent) / 2f;
        c.drawText(progress + "%", getWidth() / 2f, y, text);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
