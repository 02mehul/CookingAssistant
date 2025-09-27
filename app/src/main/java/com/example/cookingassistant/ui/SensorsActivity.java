package com.example.cookingassistant.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.cookingassistant.R;
import com.example.cookingassistant.databinding.ActivitySensorsBinding;
import com.google.android.material.card.MaterialCardView;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class SensorsActivity extends AppCompatActivity implements SensorEventListener {

    private ActivitySensorsBinding binding;
    private Vibrator vibrator;

    // --- Shake Timer & Accelerometer Variables ---
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 800;
    private int shakeCount = 0;
    private CountDownTimer shakeCountDownTimer;
    private boolean isShakeTimerRunning = false;
    private long shakeTimerInitialTimeInMillis = 0;
    private long shakeTimerTimeLeftInMillis = 0;

    // --- Proximity Sensor & Alarm Variables ---
    private Sensor proximitySensor;
    private boolean isAlarmActive = false;
    private Ringtone activeRingtone;

    // --- Camera & Flashlight Variables ---
    private CameraManager cameraManager;
    private String cameraId;
    private Handler flashlightHandler;
    private Runnable flashlightRunnable;
    private boolean isFlashing = false;

    // --- New Permission Launcher ---
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Permission denied. Flashlight will not be available.", Toast.LENGTH_LONG).show();
                }
            });

    // --- Multiple Named Timers Variables ---
    private final Map<View, CountDownTimer> activeTimers = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySensorsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        setupSensors();
        setupFlashlight();
        setupUIListeners();

        // Check for camera permission when the screen opens
        checkAndRequestCameraPermission();
    }

    // --- New method to check and request camera permission ---
    private void checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }
    }

    private void setupFlashlight() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager != null) {
                cameraId = cameraManager.getCameraIdList()[0];
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        flashlightHandler = new Handler(Looper.getMainLooper());
    }

    private void setupUIListeners() {
        binding.kitchenToolbar.setNavigationOnClickListener(v -> finish());
        binding.btnSetShakeTimer.setOnClickListener(v -> setShakeTimer());
        binding.btnAddTimer.setOnClickListener(v -> addNamedTimer());
    }

    // =================================================================================
    // Shake Timer Logic
    // =================================================================================

    private void setShakeTimer() {
        String minutesStr = Objects.requireNonNull(binding.editShakeTimerMinutes.getText()).toString();
        String secondsStr = Objects.requireNonNull(binding.editShakeTimerSeconds.getText()).toString();
        int minutes = minutesStr.isEmpty() ? 0 : Integer.parseInt(minutesStr);
        int seconds = secondsStr.isEmpty() ? 0 : Integer.parseInt(secondsStr);

        if (isShakeTimerRunning) {
            pauseShakeTimer();
        }
        shakeTimerInitialTimeInMillis = (minutes * 60L + seconds) * 1000L;
        resetShakeTimer();
        Toast.makeText(this, "Quick Timer set. Shake to start!", Toast.LENGTH_SHORT).show();
    }

    private void handleShakeAction() {
        vibrate(50);
        shakeCount++;
        binding.txtShake.setText(String.format(Locale.getDefault(), "Shakes: %d", shakeCount));

        if (isShakeTimerRunning) {
            pauseShakeTimer();
        } else {
            if (shakeTimerTimeLeftInMillis > 1000) {
                startShakeTimer();
            } else if (shakeTimerInitialTimeInMillis > 0) {
                resetShakeTimer();
                binding.txtShakeTimerStatus.setText("Timer reset. Shake to start.");
            }
        }
    }

    private void startShakeTimer() {
        shakeCountDownTimer = new CountDownTimer(shakeTimerTimeLeftInMillis, 1000) {
            @Override public void onTick(long millis) {
                shakeTimerTimeLeftInMillis = millis;
                updateShakeTimerText();
            }
            @Override public void onFinish() {
                isShakeTimerRunning = false;
                shakeTimerTimeLeftInMillis = 0;
                updateShakeTimerText();
                binding.txtShakeTimerStatus.setText("Finished! Shake to reset.");
                vibrate(500);
                playSound();
            }
        }.start();
        isShakeTimerRunning = true;
        binding.txtShakeTimerStatus.setText("Running...");
    }

    private void pauseShakeTimer() {
        if (shakeCountDownTimer != null) shakeCountDownTimer.cancel();
        isShakeTimerRunning = false;
        binding.txtShakeTimerStatus.setText("Paused. Shake to resume.");
    }

    private void resetShakeTimer() {
        if (shakeCountDownTimer != null) shakeCountDownTimer.cancel();
        shakeTimerTimeLeftInMillis = shakeTimerInitialTimeInMillis;
        isShakeTimerRunning = false;
        updateShakeTimerText();
        binding.txtShakeTimerStatus.setText("Ready. Shake to start.");
    }

    private void updateShakeTimerText() {
        int minutes = (int) (shakeTimerTimeLeftInMillis / 1000) / 60;
        int seconds = (int) (shakeTimerTimeLeftInMillis / 1000) % 60;
        binding.txtShakeTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }


    // =================================================================================
    // Multiple Named Timers Logic
    // =================================================================================

    private void addNamedTimer() {
        String name = Objects.requireNonNull(binding.editTimerName.getText()).toString().trim();
        String minutesStr = Objects.requireNonNull(binding.editTimerMinutes.getText()).toString();
        String secondsStr = Objects.requireNonNull(binding.editTimerSeconds.getText()).toString();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a name for the timer", Toast.LENGTH_SHORT).show();
            return;
        }
        int minutes = minutesStr.isEmpty() ? 0 : Integer.parseInt(minutesStr);
        int seconds = secondsStr.isEmpty() ? 0 : Integer.parseInt(secondsStr);
        long totalMillis = (minutes * 60L + seconds) * 1000L;
        if (totalMillis <= 0) {
            Toast.makeText(this, "Please enter a valid duration", Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View timerView = inflater.inflate(R.layout.row_timer, binding.timersContainer, false);

        TextView txtTimerName = timerView.findViewById(R.id.txtTimerName);
        TextView txtTimerCountdown = timerView.findViewById(R.id.txtTimerCountdown);
        Button btnCancel = timerView.findViewById(R.id.btnCancelTimer);
        MaterialCardView cardView = (MaterialCardView) timerView;
        txtTimerName.setText(name);

        CountDownTimer newTimer = new CountDownTimer(totalMillis, 1000) {
            @Override public void onTick(long millis) {
                txtTimerCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", (millis/1000)/60, (millis/1000)%60));
            }
            @Override public void onFinish() {
                txtTimerCountdown.setText("00:00");
                txtTimerName.setText(name + " - Finished!");
                cardView.setCardBackgroundColor(Color.parseColor("#FFDDC6"));
                vibrate(500);
                playSound();
                btnCancel.setText("Clear");
            }
        };
        btnCancel.setOnClickListener(v -> {
            silenceAlarm();
            newTimer.cancel();
            binding.timersContainer.removeView(timerView);
            activeTimers.remove(timerView);
        });

        binding.timersContainer.addView(timerView);
        activeTimers.put(timerView, newTimer);
        newTimer.start();

        binding.editTimerName.getText().clear();
        binding.editTimerMinutes.getText().clear();
        binding.editTimerSeconds.getText().clear();
    }


    // =================================================================================
    // Alarm, Sound, and Flashlight Logic
    // =================================================================================

    private void playSound() {
        try {
            silenceAlarm(); // Stop any previous alarm
            Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            activeRingtone = RingtoneManager.getRingtone(getApplicationContext(), notificationSound);
            activeRingtone.play();
            isAlarmActive = true;
            startFlashing(); // Start the flashlight
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void silenceAlarm() {
        if (isAlarmActive) {
            if (activeRingtone != null) activeRingtone.stop();
            isAlarmActive = false;
            activeRingtone = null;
            stopFlashing(); // Stop the flashlight
            Toast.makeText(this, "Alarm silenced", Toast.LENGTH_SHORT).show();
        }
    }

    private void startFlashing() {
        if (cameraId == null || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return; // Don't try to flash if we don't have permission or a camera
        }
        isFlashing = true;
        flashlightRunnable = new Runnable() {
            private boolean isOn = false;
            @Override
            public void run() {
                try {
                    cameraManager.setTorchMode(cameraId, isOn);
                    isOn = !isOn;
                    if (isFlashing) {
                        flashlightHandler.postDelayed(this, 500); // Blink every 500ms
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        };
        flashlightHandler.post(flashlightRunnable);
    }

    private void stopFlashing() {
        isFlashing = false;
        if (flashlightRunnable != null) {
            flashlightHandler.removeCallbacks(flashlightRunnable);
        }
        try {
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, false); // Ensure flash is off
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    // =================================================================================
    // General Lifecycle and Sensor Methods
    // =================================================================================

    private void vibrate(long duration) {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(duration);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long curTime = System.currentTimeMillis();
            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;
                float x = event.values[0]; float y = event.values[1]; float z = event.values[2];
                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;
                if (speed > SHAKE_THRESHOLD) {
                    handleShakeAction();
                }
                last_x = x; last_y = y; last_z = z;
            }
        }
        else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (isAlarmActive && event.values[0] < proximitySensor.getMaximumRange()) {
                silenceAlarm();
            }
        }
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            }
            if (proximitySensor != null) {
                sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (isShakeTimerRunning) {
            pauseShakeTimer();
        }
        silenceAlarm();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (CountDownTimer timer : activeTimers.values()) {
            if (timer != null) timer.cancel();
        }
        activeTimers.clear();
        if (shakeCountDownTimer != null) shakeCountDownTimer.cancel();
        silenceAlarm();
    }
}