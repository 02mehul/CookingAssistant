package com.example.cookingassistant.ui.mood;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class MoodMeterView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color = 0xFF9FA8DA; // default
    private float energy = 0f;      // 0..1

    public MoodMeterView(Context c) { super(c); init(); }
    public MoodMeterView(Context c, @Nullable AttributeSet a) { super(c, a); init(); }
    private void init() { paint.setStyle(Paint.Style.FILL); }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        paint.setColor(color);
        float r = Math.min(getWidth(), getHeight()) / 2f;
        // energy controls inner radius -> donut
        float inner = r * (0.4f + 0.4f * (1f - energy));
        c.translate(getWidth()/2f, getHeight()/2f);
        c.drawCircle(0,0,r, paint);
        paint.setColor(0xFFFFFFFF);
        c.drawCircle(0,0,inner, paint);
    }

    public void setMoodColor(int target) {
        ValueAnimator va = ValueAnimator.ofObject(new ArgbEvaluator(), color, target);
        va.setDuration(300);
        va.addUpdateListener(a -> { color = (int) a.getAnimatedValue(); invalidate(); });
        va.start();
    }

    public void setEnergy(float e) {
        energy = Math.max(0f, Math.min(1f, e));
        invalidate();
    }
}
