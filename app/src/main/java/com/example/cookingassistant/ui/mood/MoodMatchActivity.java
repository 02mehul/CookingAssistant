package com.example.cookingassistant.ui.mood;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.cookingassistant.R;
import com.example.cookingassistant.api.ApiClient;
import com.example.cookingassistant.api.model.Meal;
import com.example.cookingassistant.api.model.MealFilterResponse;
import com.example.cookingassistant.api.model.MealResponse;
import com.example.cookingassistant.api.model.MealSummary;
import com.example.cookingassistant.ui.RecipeDetailActivity;
import com.google.android.material.snackbar.Snackbar;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MoodMatchActivity extends AppCompatActivity {

    private String mood, env, ingredientsRaw;
    private float energy;

    private TextView txtSummary, txtTitle;
    private ImageView img;

    /** Candidate recipe IDs we can cycle through */
    private final List<String> candidateIds = new ArrayList<>();
    private int pos = 0;

    private Meal current;

    // cache parsed ingredients
    private List<String> ingList = new ArrayList<>();

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_match);
        setSupportActionBar(findViewById(R.id.toolbar));

        mood = getIntent().getStringExtra("mood");
        env = getIntent().getStringExtra("env");
        energy = getIntent().getFloatExtra("energy", 0f);
        ingredientsRaw = getIntent().getStringExtra("ingredients");
        if (ingredientsRaw == null) ingredientsRaw = "";
        ingList = parseIngredients(ingredientsRaw);

        txtSummary = findViewById(R.id.txtSummary);
        txtTitle = findViewById(R.id.txtTitle);
        img = findViewById(R.id.img);

        String summary = "Mood: " + mood + "  •  Energy: " + (int)(energy*100) + "  •  Env: " + env +
                "\nIngredients: " + (ingredientsRaw.isEmpty() ? "(none)" : ingredientsRaw);
        txtSummary.setText(summary);

        findViewById(R.id.btnOpen).setOnClickListener(v -> openRecipe());
        findViewById(R.id.btnAnother).setOnClickListener(v -> tryAnother());

        buildCandidates(); // initial load
    }

    /** Build a rich candidate list: all ingredients → mood fallback → random fallback */
    private void buildCandidates() {
        candidateIds.clear();
        pos = 0;

        if (!ingList.isEmpty()) {
            // sequentially fetch for each ingredient and merge (dedupe)
            fetchIngredientAt(0, new LinkedHashSet<>());
        } else {
            buildCandidatesFromMood();
        }
    }

    /** Recursively fetch filter.php?i= for each ingredient and merge into a set */
    private void fetchIngredientAt(int index, Set<String> acc) {
        if (index >= ingList.size()) {
            // done collecting; if empty fallback; else shuffle and load
            if (acc.isEmpty()) {
                buildCandidatesFromMood();
            } else {
                candidateIds.clear();
                candidateIds.addAll(acc);
                Collections.shuffle(candidateIds);
                loadPos(0);
            }
            return;
        }
        String ingredient = ingList.get(index);
        ApiClient.get().filterByIngredient(ingredient).enqueue(new Callback<MealFilterResponse>() {
            @Override public void onResponse(Call<MealFilterResponse> call, Response<MealFilterResponse> resp) {
                List<MealSummary> ms = resp.body() != null ? resp.body().meals : null;
                if (ms != null) {
                    for (MealSummary m : ms) if (m.idMeal != null) acc.add(m.idMeal);
                }
                fetchIngredientAt(index + 1, acc);
            }
            @Override public void onFailure(Call<MealFilterResponse> call, Throwable t) {
                // ignore this ingredient and continue
                fetchIngredientAt(index + 1, acc);
            }
        });
    }

    /** Fallback: search by mood keyword */
    private void buildCandidatesFromMood() {
        String keyword = moodToKeyword(mood, env, energy);
        ApiClient.get().search(keyword).enqueue(new Callback<MealResponse>() {
            @Override public void onResponse(Call<MealResponse> call, Response<MealResponse> resp) {
                List<Meal> meals = (resp.body() != null) ? resp.body().meals : null;
                if (meals != null) {
                    for (Meal m : meals) {
                        String id = getField(m, "idMeal");
                        if (id != null && !id.isEmpty()) candidateIds.add(id);
                    }
                }
                if (candidateIds.isEmpty()) {
                    fetchRandomAlternative(true);
                } else {
                    Collections.shuffle(candidateIds);
                    loadPos(0);
                }
            }
            @Override public void onFailure(Call<MealResponse> call, Throwable t) {
                fetchRandomAlternative(true);
            }
        });
    }

    /** Load details for candidateIds[pos] and bind UI */
    private void loadPos(int p) {
        if (candidateIds.isEmpty()) {
            fetchRandomAlternative(true);
            return;
        }
        pos = (p % candidateIds.size() + candidateIds.size()) % candidateIds.size();
        String id = candidateIds.get(pos);

        ApiClient.get().lookup(id).enqueue(new Callback<MealResponse>() {
            @Override public void onResponse(Call<MealResponse> call, Response<MealResponse> resp) {
                MealResponse body = resp.body();
                if (body == null || body.meals == null || body.meals.isEmpty()) {
                    fetchRandomAlternative(false);
                    return;
                }
                current = body.meals.get(0);
                String title = getField(current, "strMeal");
                String thumb = getField(current, "strMealThumb");
                txtTitle.setText(title != null ? title : "Recipe");
                if (thumb != null && !thumb.trim().isEmpty()) {
                    Glide.with(MoodMatchActivity.this).load(thumb).into(img);
                } else {
                    img.setImageResource(android.R.color.darker_gray);
                }
            }
            @Override public void onFailure(Call<MealResponse> call, Throwable t) {
                fetchRandomAlternative(false);
            }
        });
    }

    /** Try next candidate; if we only have one or none, fetch a random new one */
    private void tryAnother() {
        if (candidateIds.size() <= 1) {
            fetchRandomAlternative(false);
        } else {
            loadPos(pos + 1);
        }
    }

    /** Always adds a new random candidate and shows it */
    private void fetchRandomAlternative(boolean showMsgWhenEmpty) {
        ApiClient.get().random().enqueue(new Callback<MealResponse>() {
            @Override public void onResponse(Call<MealResponse> call, Response<MealResponse> resp) {
                MealResponse b = resp.body();
                if (b == null || b.meals == null || b.meals.isEmpty()) {
                    if (showMsgWhenEmpty)
                        Snackbar.make(findViewById(android.R.id.content), "No recipes found", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                Meal m = b.meals.get(0);
                String id = getField(m, "idMeal");
                if (id != null && !id.isEmpty()) {
                    if (!candidateIds.contains(id)) candidateIds.add(id);
                    loadPos(candidateIds.indexOf(id));
                }
            }
            @Override public void onFailure(Call<MealResponse> call, Throwable t) {
                if (showMsgWhenEmpty)
                    Snackbar.make(findViewById(android.R.id.content), "Network error", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void openRecipe() {
        if (current == null) return;
        String id = getField(current, "idMeal");
        if (id == null || id.trim().isEmpty()) return;
        Intent i = new Intent(this, RecipeDetailActivity.class);
        i.putExtra("id", id);
        startActivity(i);
    }

    // --- helpers ---

    private String moodToKeyword(String mood, String env, float energy) {
        mood = (mood == null) ? "" : mood.toLowerCase(Locale.US);
        env  = (env  == null) ? "" : env.toLowerCase(Locale.US);
        if (mood.contains("calm") || env.equals("night")) return "soup";
        if (mood.contains("sad")) return "chocolate";
        if (mood.contains("stressed")) return "pasta";
        if (mood.contains("energetic") || energy > 0.6f) return "spicy";
        if (mood.contains("happy")) return "grill";
        return "chicken";
    }

    private List<String> parseIngredients(String s) {
        List<String> out = new ArrayList<>();
        if (s == null) return out;
        String[] parts = s.split(",");
        // keep up to 3 non-empty tokens to avoid too many network calls
        for (String p : parts) {
            String t = p.trim().toLowerCase(Locale.US);
            if (!t.isEmpty()) out.add(t);
            if (out.size() == 3) break;
        }
        return out;
    }

    private String getField(Object obj, String name) {
        try {
            Field f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            Object v = f.get(obj);
            return v == null ? null : String.valueOf(v);
        } catch (Exception e) { return null; }
    }
}
