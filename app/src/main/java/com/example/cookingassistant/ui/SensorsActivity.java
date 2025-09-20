package com.example.cookingassistant.ui;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cookingassistant.R;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

public class SensorsActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sm;
    private Sensor light, prox, accel;

    private TextView txtLight, txtProx, txtWaves, txtShake, txtTimer, txtOrientation;
    private MaterialSwitch switchHaptics;

    private int waves = 0, shakes = 0;
    private long lastShakeMs = 0, lastWaveMs = 0;

    // Low-pass gravity for accel
    private static final float ALPHA = 0.9f;
    private float[] gravity = new float[]{0,0,0};

    // Timer (30s). Shake toggles start/pause.
    private CountDownTimer timer;
    private long remainingMs = 30_000;
    private boolean timerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensors);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        light = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
        prox  = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        txtLight = findViewById(R.id.txtLight);
        txtProx  = findViewById(R.id.txtProx);
        txtWaves = findViewById(R.id.txtWaves);
        txtShake = findViewById(R.id.txtShake);
        txtTimer = findViewById(R.id.txtTimer);
        txtOrientation = findViewById(R.id.txtOrientation);
        switchHaptics = findViewById(R.id.switchHaptics);

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        // If any sensor missing, show it
        if (light == null) txtLight.setText("Light sensor not available");
        if (prox  == null) txtProx.setText("Proximity sensor not available");
        if (accel == null) txtShake.setText("Accelerometer not available");

        updateTimerLabel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (light != null) sm.registerListener(this, light, SensorManager.SENSOR_DELAY_UI);
        if (prox  != null) sm.registerListener(this, prox,  SensorManager.SENSOR_DELAY_UI);
        if (accel != null) sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sm.unregisterListener(this);
        // keep timer state
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (e.sensor.getType() == Sensor.TYPE_LIGHT) {
            handleLight(e.values[0]);
        } else if (e.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            handleProximity(e.values[0], e.sensor.getMaximumRange());
        } else if (e.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            handleAccel(e.values);
        }
    }

    private void handleLight(float lux) {
        txtLight.setText("Light: " + (int) lux + " lx");
        // Night kitchen mode if too dim
        boolean night = lux < 20f;
        int bg = night ? Color.BLACK : Color.TRANSPARENT;
        int fg = night ? Color.WHITE : Color.BLACK;

        // Change background/foreground for quick high contrast
        findViewById(android.R.id.content).getRootView().setBackgroundColor(bg);
        txtLight.setTextColor(fg);
        txtProx.setTextColor(fg);
        txtWaves.setTextColor(fg);
        txtShake.setTextColor(fg);
        txtTimer.setTextColor(fg);
        txtOrientation.setTextColor(fg);
    }

    private void handleProximity(float value, float maxRange) {
        // "Near" typically 0.0 on most devices
        boolean near = value < maxRange;
        txtProx.setText(near ? "Proximity: NEAR" : "Proximity: FAR");

        long now = System.currentTimeMillis();
        // debounce 600ms to avoid multiple waves in one pass
        if (near && now - lastWaveMs > 600) {
            lastWaveMs = now;
            waves++;
            txtWaves.setText("Waves: " + waves);
            haptic(20);
            Snackbar.make(findViewById(android.R.id.content), "Wave detected (next step)", Snackbar.LENGTH_SHORT).show();
            // here you could advance recipe step UI if you want
        }
    }

    private void handleAccel(float[] v) {
        // Low-pass gravity
        gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * v[0];
        gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * v[1];
        gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * v[2];

        // Linear acceleration
        float lx = v[0] - gravity[0];
        float ly = v[1] - gravity[1];
        float lz = v[2] - gravity[2];
        double mag = Math.sqrt(lx*lx + ly*ly + lz*lz);

        // Orientation (rough)
        double pitch = Math.toDegrees(Math.atan2(-v[0], Math.sqrt(v[1]*v[1] + v[2]*v[2])));
        double roll  = Math.toDegrees(Math.atan2(v[1], v[2]));
        String orient = Math.abs(pitch) < 25 ? "Flat" : (pitch > 0 ? "Tilt Up" : "Tilt Down");
        txtOrientation.setText("Orientation: " + orient);

        // Shake detect (debounced)
        long now = System.currentTimeMillis();
        if (mag > 2.2 && now - lastShakeMs > 500) { // tweak threshold if needed
            lastShakeMs = now;
            shakes++;
            txtShake.setText("Shakes: " + shakes);
            haptic(25);
            toggleTimer();
        }
    }

    private void toggleTimer() {
        if (!timerRunning) {
            startTimer();
            Snackbar.make(findViewById(android.R.id.content), "Timer started", Snackbar.LENGTH_SHORT).show();
        } else {
            pauseTimer();
            Snackbar.make(findViewById(android.R.id.content), "Timer paused", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void startTimer() {
        timerRunning = true;
        timer = new CountDownTimer(remainingMs, 1000) {
            @Override public void onTick(long ms) {
                remainingMs = ms;
                updateTimerLabel();
            }
            @Override public void onFinish() {
                timerRunning = false;
                remainingMs = 30_000;
                updateTimerLabel();
                haptic(120);
                Snackbar.make(findViewById(android.R.id.content), "Timer done!", Snackbar.LENGTH_LONG).show();
            }
        }.start();
    }

    private void pauseTimer() {
        if (timer != null) timer.cancel();
        timerRunning = false;
        updateTimerLabel();
    }

    private void updateTimerLabel() {
        long s = remainingMs / 1000;
        String m = String.format("Timer: %02d:%02d (shake to start/pause)", (s/60), (s%60));
        txtTimer.setText(m);
    }

    private void haptic(int ms) {
        if (!switchHaptics.isChecked()) return;
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(ms);
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
