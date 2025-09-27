package com.example.cookingassistant.ui.mood;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.cookingassistant.R;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MoodCaptureActivity extends AppCompatActivity implements SensorEventListener {

    // Sensors (existing)
    private SensorManager sm;
    private Sensor accel, light;
    private long lastSampleMs = 0;
    private float energySmoothed = 0f; // 0..1
    private float lastLux = -1f;

    private long lastAnalyzeMs = 0;

    // UI
    private MoodMeterView meter;
    private TextView txtEnv, txtEnergy, txtMoodTitle, txtCamStatus;
    private ChipGroup chips;
    private PreviewView previewView;
    private MaterialSwitch switchCamera;

    private String selectedMood = "Calm";

    private static final int MOOD_WINDOW = 8;        // last N frames
    private final java.util.ArrayDeque<String> moodVotes = new java.util.ArrayDeque<>(MOOD_WINDOW);
    private long lastCommittedMs = 0;
    private String committedMood = "Calm";

    // CameraX / ML Kit
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private FaceDetector faceDetector;
    private boolean cameraRunning = false;
    private long lastAppliedMs = 0; // debounce mood updates

    // Permission launcher
    private final ActivityResultLauncher<String> cameraPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    switchCamera.setChecked(false);
                    txtCamStatus.setText("Permission denied");
                    Snackbar.make(findViewById(android.R.id.content), "Camera permission denied", Snackbar.LENGTH_SHORT).show();
                }
            });

    private void voteMood(String m) {
        if (moodVotes.size() == MOOD_WINDOW) moodVotes.removeFirst();
        moodVotes.addLast(m);
    }
    private String majorityMood() {
        java.util.HashMap<String,Integer> cnt = new java.util.HashMap<>();
        for (String m : moodVotes) cnt.put(m, cnt.getOrDefault(m, 0) + 1);
        String best = committedMood; int bestN = -1;
        for (var e : cnt.entrySet()) if (e.getValue() > bestN) { bestN = e.getValue(); best = e.getKey(); }
        return best;
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_capture);
        setSupportActionBar(findViewById(R.id.toolbar));

        meter = findViewById(R.id.moodMeter);
        txtEnv = findViewById(R.id.txtEnv);
        txtEnergy = findViewById(R.id.txtEnergy);
        txtMoodTitle = findViewById(R.id.txtMoodTitle);
        txtCamStatus = findViewById(R.id.txtCamStatus);
        chips = findViewById(R.id.chipsMood);
        previewView = findViewById(R.id.previewView);
        switchCamera = findViewById(R.id.switchCamera);

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sm != null) {
            accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            light = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        // Default mood
        findViewById(R.id.chipCalm).performClick();

        // Chip changes -> update mood/color
        chips.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids == null || ids.isEmpty()) return;
            int id = ids.get(0);
            if (id == R.id.chipHappy)      setMood("Happy",     0xFFFBC02D, "😊");
            else if (id == R.id.chipCalm)  setMood("Calm",      0xFF42A5F5, "🙂");
            else if (id == R.id.chipStressed) setMood("Stressed", 0xFFEF5350, "😵");
            else if (id == R.id.chipSad)   setMood("Sad",       0xFF5C6BC0, "😔");
            else if (id == R.id.chipEnergetic) setMood("Energetic", 0xFFFB8C00, "🔥");
        });

        // Camera switch
        switchCamera.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                ensureCameraPermissionThenStart();
            } else {
                stopCamera();
            }
        });

        // Next button
        findViewById(R.id.btnNext).setOnClickListener(v -> {
            Intent i = new Intent(this, IngredientInputActivity.class);
            i.putExtra("mood", selectedMood);
            i.putExtra("energy", energySmoothed);
            i.putExtra("env", inferEnv());
            startActivity(i);
        });

        // Prepare ML Kit Face Detector (fast + classification)
        FaceDetectorOptions opts = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE) // more stable
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f) // ignore tiny faces
                .enableTracking()
                .build();
        faceDetector = FaceDetection.getClient(opts);
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void setMood(String mood, int color, String emoji) {
        selectedMood = mood;
        meter.setMoodColor(color);
        txtMoodTitle.setText("Feeling: " + mood + " " + emoji);
    }

    // ===== Sensors =====
    @Override protected void onResume() {
        super.onResume();
        if (sm != null) {
            if (accel != null) sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI);
            if (light != null) sm.registerListener(this, light, SensorManager.SENSOR_DELAY_UI);
        }
        if (switchCamera.isChecked()) ensureCameraPermissionThenStart();
    }

    @Override protected void onPause() {
        super.onPause();
        if (sm != null) sm.unregisterListener(this);
        stopCamera(); // release camera when not visible
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (faceDetector != null) faceDetector.close();
        if (cameraExecutor != null) cameraExecutor.shutdown();
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

    // ===== Camera / ML Kit =====
    private void ensureCameraPermissionThenStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        if (cameraRunning) return;
        previewView.setVisibility(android.view.View.VISIBLE);
        txtCamStatus.setText("Starting camera…");

        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

                CameraSelector selector;
                try {
                    selector = CameraSelector.DEFAULT_FRONT_CAMERA;
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(this, selector, preview, analysis);
                } catch (Exception noFront) {
                    selector = CameraSelector.DEFAULT_BACK_CAMERA;
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(this, selector, preview, analysis);
                }

                cameraRunning = true;
                txtCamStatus.setText("Detecting face…");
            } catch (Exception ex) {
                cameraRunning = false;
                previewView.setVisibility(android.view.View.GONE);
                txtCamStatus.setText("Camera error");
                Snackbar.make(findViewById(android.R.id.content), "Camera error: " + ex.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void stopCamera() {
        if (!cameraRunning) return;
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        previewView.setVisibility(android.view.View.GONE);
        txtCamStatus.setText("Camera off");
        cameraRunning = false;
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeFrame(ImageProxy proxy) {
        try {
            if (proxy.getImage() == null) { proxy.close(); return; }
            long now = System.currentTimeMillis();
            if (now - lastAnalyzeMs < 70) { // ~14 fps cap; use 90 for ~11 fps
                proxy.close();
                return;
            }
            lastAnalyzeMs = now;
            InputImage image = InputImage.fromMediaImage(proxy.getImage(), proxy.getImageInfo().getRotationDegrees());

            faceDetector.process(image)
                    .addOnSuccessListener(faces -> {
                        if (faces == null || faces.isEmpty()) {
                            runOnUiThread(() -> txtCamStatus.setText("No face"));
                        } else {
                            Face f = faces.get(0); // primary face
                            applyDetectedMoodFromFace(f);
                        }
                    })
                    .addOnFailureListener(e ->
                            runOnUiThread(() -> txtCamStatus.setText("Detection error")))
                    .addOnCompleteListener(task -> proxy.close());
        } catch (Exception ex) {
            proxy.close();
        }
    }

    private void applyDetectedMoodFromFace(Face f) {
        Float smile = f.getSmilingProbability();
        Float leftOpen = f.getLeftEyeOpenProbability();
        Float rightOpen = f.getRightEyeOpenProbability();
        float yaw = f.getHeadEulerAngleY();
        float pitch = f.getHeadEulerAngleX();

        // Classify this single frame
        String frameMood = "Calm";
        if (smile != null && smile >= 0.70f) {
            frameMood = "Happy";
        } else if (smile != null && smile <= 0.25f) {
            boolean eyesClosed = (leftOpen != null && leftOpen < 0.40f) && (rightOpen != null && rightOpen < 0.40f);
            if (eyesClosed) frameMood = "Calm";
            else if (Math.abs(yaw) > 20f || Math.abs(pitch) > 20f) frameMood = "Stressed";
            else frameMood = "Sad";
        } else if (energySmoothed > 0.65f) {
            frameMood = "Energetic";
        }

        // Vote and compute majority for stability
        voteMood(frameMood);
        String stable = majorityMood();

        // Only commit if:
        //  (a) majority differs from committed OR it’s been a while, AND
        //  (b) we debounce to avoid flicker
        long now = System.currentTimeMillis();
        boolean changed = !stable.equals(committedMood);
        if (changed && (now - lastCommittedMs) > 700) {
            committedMood = stable;
            lastCommittedMs = now;

            // Map to color/emoji and update UI
            int color = 0xFF42A5F5; String emoji = "🙂";
            int chipId = R.id.chipCalm;
            switch (committedMood) {
                case "Happy": color = 0xFFFBC02D; emoji = "😊"; chipId = R.id.chipHappy; break;
                case "Stressed": color = 0xFFEF5350; emoji = "😵"; chipId = R.id.chipStressed; break;
                case "Sad": color = 0xFF5C6BC0; emoji = "😔"; chipId = R.id.chipSad; break;
                case "Energetic": color = 0xFFFB8C00; emoji = "🔥"; chipId = R.id.chipEnergetic; break;
            }

            final int cFinal = color; final String eFinal = emoji; final int chipFinal = chipId;
            runOnUiThread(() -> {
                setMood(committedMood, cFinal, eFinal);
                chips.check(chipFinal);
            });
        }

        // Status readout (optional, keep as-is)
        runOnUiThread(() -> txtCamStatus.setText(statusLine(smile, leftOpen, rightOpen)));
    }


    private String statusLine(Float smile, Float l, Float r) {
        String s = smile == null ? "—" : String.format(Locale.US, "%.0f%%", smile * 100f);
        String le = l == null ? "—" : String.format(Locale.US, "%.0f%%", l * 100f);
        String re = r == null ? "—" : String.format(Locale.US, "%.0f%%", r * 100f);
        return "Face ok • Smile " + s + " • Eyes " + le + "/" + re;
    }
}
