package com.example.cookingassistant.ui.mood;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cookingassistant.R;
import com.google.android.material.chip.ChipGroup;

public class MoodCaptureActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sm;
    private Sensor accel, light;
    private long lastSampleMs = 0;
    private float energySmoothed = 0f; // 0..1
    private float lastLux = -1f;

    private MoodMeterView meter;
    private TextView txtEnv, txtEnergy, txtMoodTitle;
    private ChipGroup chips;

    private String selectedMood = "Calm";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_capture);
        setSupportActionBar(findViewById(R.id.toolbar));

        meter = findViewById(R.id.moodMeter);
        txtEnv = findViewById(R.id.txtEnv);
        txtEnergy = findViewById(R.id.txtEnergy);
        txtMoodTitle = findViewById(R.id.txtMoodTitle);
        chips = findViewById(R.id.chipsMood);

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sm != null) {
            accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            light = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        // default selection & color
        findViewById(R.id.chipCalm).performClick(); // triggers listener below

        chips.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids == null || ids.isEmpty()) return;
            int id = ids.get(0);
            if (id == R.id.chipHappy)      setMood("Happy",     0xFFFBC02D, "😊");
            else if (id == R.id.chipCalm)  setMood("Calm",      0xFF42A5F5, "🙂");
            else if (id == R.id.chipStressed) setMood("Stressed", 0xFFEF5350, "😵");
            else if (id == R.id.chipSad)   setMood("Sad",       0xFF5C6BC0, "😔");
            else if (id == R.id.chipEnergetic) setMood("Energetic", 0xFFFB8C00, "🔥");
        });

        findViewById(R.id.btnNext).setOnClickListener(v -> {
            Intent i = new Intent(this, IngredientInputActivity.class);
            i.putExtra("mood", selectedMood);
            i.putExtra("energy", energySmoothed);
            i.putExtra("env", inferEnv());
            startActivity(i);
        });
    }

    private void setMood(String mood, int color, String emoji) {
        selectedMood = mood;
        meter.setMoodColor(color);
        txtMoodTitle.setText("Feeling: " + mood + " " + emoji);
    }

    @Override protected void onResume() {
        super.onResume();
        if (sm != null) {
            if (accel != null) sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI);
            if (light != null) sm.registerListener(this, light, SensorManager.SENSOR_DELAY_UI);
        }
    }
    @Override protected void onPause() {
        super.onPause();
        if (sm != null) sm.unregisterListener(this);
    }

    @Override public void onSensorChanged(SensorEvent e) {
        long now = System.currentTimeMillis();
        if (e.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float ax = e.values[0], ay = e.values[1], az = e.values[2];
            float mag = (float)Math.sqrt(ax*ax + ay*ay + az*az) - SensorManager.GRAVITY_EARTH;
            mag = Math.max(0f, mag);
            energySmoothed = 0.9f*energySmoothed + 0.1f * Math.min(1f, mag/4f);
            meter.setEnergy(energySmoothed);
            if (now - lastSampleMs > 300) {
                txtEnergy.setText("Energy: " + (int)(energySmoothed*100));
                lastSampleMs = now;
            }
        } else if (e.sensor.getType() == Sensor.TYPE_LIGHT) {
            lastLux = e.values[0];
            txtEnv.setText("Env: " + (int)lastLux + " lx (" + inferEnv() + ")");
        }
    }
    private String inferEnv() {
        if (lastLux < 0) return "unknown";
        if (lastLux < 20) return "night";
        if (lastLux < 150) return "dim";
        if (lastLux < 1000) return "day";
        return "bright";
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
