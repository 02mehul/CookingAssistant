package com.example.cookingassistant.ui;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.cookingassistant.R;
import com.example.cookingassistant.api.ApiClient;
import com.example.cookingassistant.api.model.Meal;
import com.example.cookingassistant.api.model.MealResponse;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecipeDetailActivity extends AppCompatActivity implements SensorEventListener {

    private ImageView imgHeader;
    private TextView txtTitle, txtStepHeader, txtStep;
    private ChipGroup chipMeta;
    private LinearLayout ingredientsContainer;
    private MaterialSwitch switchHandsFree, switchReadAloud;

    private List<String> steps = new ArrayList<>();
    private int idx = 0;

    // Proximity for hands-free
    private SensorManager sm;
    private Sensor prox;
    private long lastWaveMs = 0;

    // TTS
    private TextToSpeech tts;

    private String youtubeUrl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imgHeader = findViewById(R.id.imgHeader);
        txtTitle = findViewById(R.id.txtTitle);
        chipMeta = findViewById(R.id.chipMeta);
        ingredientsContainer = findViewById(R.id.ingredientsContainer);
        txtStepHeader = findViewById(R.id.txtStepHeader);
        txtStep = findViewById(R.id.txtStep);
        switchHandsFree = findViewById(R.id.switchHandsFree);
        switchReadAloud = findViewById(R.id.switchReadAloud);

        findViewById(R.id.btnPrev).setOnClickListener(v -> prevStep());
        findViewById(R.id.btnNext).setOnClickListener(v -> nextStep());
        findViewById(R.id.fabYoutube).setOnClickListener(v -> openYoutube());

        // Proximity setup
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        prox = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (prox == null) {
            switchHandsFree.setChecked(false);
            switchHandsFree.setEnabled(false);
            Snackbar.make(findViewById(android.R.id.content), "Proximity not available", Snackbar.LENGTH_SHORT).show();
        }

        // TTS setup
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.getDefault());
                if (!steps.isEmpty() && switchReadAloud.isChecked()) speak(steps.get(idx));
            }
        });

        // Load by ID
        String id = getIntent().getStringExtra("id");
        if (id != null && !id.isEmpty()) {
            loadRecipe(id);
        } else {
            txtTitle.setText("Demo Recipe");
            steps = Arrays.asList("Chop onions.", "Heat oil.", "Add onions and sauté.", "Serve hot.");
            populateIngredientsDemo();
            showStep(0);
        }
    }

    private void loadRecipe(String id) {
        ApiClient.get().lookup(id).enqueue(new Callback<MealResponse>() {
            @Override public void onResponse(Call<MealResponse> call, Response<MealResponse> resp) {
                MealResponse body = resp.body();
                if (body == null || body.meals == null || body.meals.isEmpty()) {
                    Toast.makeText(RecipeDetailActivity.this, "Recipe not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                Meal m = body.meals.get(0);

                // Title + image (via reflective getter to avoid compile issues)
                String title = getField(m, "strMeal");
                String thumb = getField(m, "strMealThumb");
                txtTitle.setText(title != null ? title : "Recipe");
                if (thumb != null && !thumb.trim().isEmpty()) {
                    Glide.with(RecipeDetailActivity.this).load(thumb).into(imgHeader);
                }

                // Meta chips (Category / Area / Tags) — all via getField
                chipMeta.removeAllViews();
                addChip(getField(m, "strCategory"));
                addChip(getField(m, "strArea"));
                String tags = getField(m, "strTags");
                if (tags != null && !tags.trim().isEmpty()) {
                    for (String t : tags.split(",")) addChip(t.trim());
                }

                // Ingredients
                populateIngredients(m);

                // Steps
                steps = parseSteps(getField(m, "strInstructions"));
                showStep(0);

                // YouTube
                youtubeUrl = getField(m, "strYoutube");
                findViewById(R.id.fabYoutube).setVisibility(
                        youtubeUrl != null && !youtubeUrl.trim().isEmpty() ? View.VISIBLE : View.GONE
                );
            }
            @Override public void onFailure(Call<MealResponse> call, Throwable t) {
                Toast.makeText(RecipeDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addChip(String text) {
        if (text == null || text.trim().isEmpty()) return;
        Chip c = new Chip(this);
        c.setText(text);
        c.setClickable(false);
        c.setCheckable(false);
        chipMeta.addView(c);
    }

    private List<String> parseSteps(String instr) {
        if (instr == null) return Arrays.asList("No instructions available.");
        List<String> out = new ArrayList<>();
        for (String line : instr.split("\\r?\\n")) {
            if (!line.trim().isEmpty()) out.add(line.trim());
        }
        if (out.isEmpty()) {
            for (String s : instr.split("\\.(\\s|$)")) {
                if (!s.trim().isEmpty()) out.add(s.trim() + ".");
            }
        }
        if (out.isEmpty()) out.add("No instructions available.");
        return out;
    }

    private void populateIngredients(Meal m) {
        ingredientsContainer.removeAllViews();
        // TheMealDB has strIngredient1..20 and strMeasure1..20
        for (int i = 1; i <= 20; i++) {
            String ing = getField(m, "strIngredient" + i);
            String mea = getField(m, "strMeasure" + i);
            if (ing == null) continue;
            ing = ing.trim();
            if (ing.isEmpty() || ing.equalsIgnoreCase("null")) continue;

            String line = "• " + ing + (mea != null && !mea.trim().isEmpty() ? " — " + mea.trim() : "");
            TextView tv = new TextView(this);
            tv.setText(line);
            tv.setPadding(0, dp(4), 0, dp(4));
            ingredientsContainer.addView(tv);
        }
    }

    private void populateIngredientsDemo() {
        ingredientsContainer.removeAllViews();
        addIngredientLine("Onion", "1");
        addIngredientLine("Oil", "2 tbsp");
        addIngredientLine("Salt", "to taste");
    }

    private void addIngredientLine(String ing, String measure) {
        TextView tv = new TextView(this);
        tv.setText("• " + ing + (measure == null ? "" : " — " + measure));
        tv.setPadding(0, dp(4), 0, dp(4));
        ingredientsContainer.addView(tv);
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    // Reflective getter for Meal fields like strIngredient1, strArea, etc.
    private String getField(Meal m, String name) {
        try {
            Field f = m.getClass().getDeclaredField(name);
            f.setAccessible(true);
            Object val = f.get(m);
            return val == null ? null : String.valueOf(val);
        } catch (Exception e) {
            return null;
        }
    }

    private void showStep(int index) {
        if (index < 0 || index >= steps.size()) return;
        idx = index;
        txtStepHeader.setText("Step " + (idx + 1) + " / " + steps.size());
        txtStep.setText(steps.get(idx));
        if (switchReadAloud.isChecked() && tts != null) speak(steps.get(idx));
    }

    private void nextStep() { if (idx < steps.size() - 1) showStep(idx + 1); }
    private void prevStep() { if (idx > 0) showStep(idx - 1); }

    private void speak(String text) {
        if (tts == null) return;
        tts.stop();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "step");
    }

    private void openYoutube() {
        if (youtubeUrl == null || youtubeUrl.trim().isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "No video link", Snackbar.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl));
        startActivity(i);
    }

    // ===== Proximity hands-free =====
    @Override protected void onResume() {
        super.onResume();
        if (prox != null) sm.registerListener(this, prox, SensorManager.SENSOR_DELAY_UI);
    }

    @Override protected void onPause() {
        super.onPause();
        sm.unregisterListener(this);
        if (tts != null) tts.stop();
    }

    @Override protected void onDestroy() {
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_PROXIMITY) return;
        if (!switchHandsFree.isChecked()) return;
        boolean near = event.values[0] < event.sensor.getMaximumRange();
        long now = System.currentTimeMillis();
        if (near && now - lastWaveMs > 600) { // debounce
            lastWaveMs = now;
            nextStep();
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
