package com.example.cookingassistant.ui.mood;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cookingassistant.R;

import java.util.ArrayList;
import java.util.Locale;

public class IngredientInputActivity extends AppCompatActivity {

    private EditText edit;
    private String mood, env;
    private float energy;

    private final ActivityResultLauncher<Intent> speechLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), res -> {
                if (res.getResultCode() == RESULT_OK && res.getData() != null) {
                    ArrayList<String> list = res.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (list != null && !list.isEmpty()) {
                        String existing = edit.getText().toString().trim();
                        String add = list.get(0);
                        edit.setText(existing.isEmpty() ? add : existing + ", " + add);
                    }
                }
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingredient_input);
        setSupportActionBar(findViewById(R.id.toolbar));

        mood = getIntent().getStringExtra("mood");
        env = getIntent().getStringExtra("env");
        energy = getIntent().getFloatExtra("energy", 0f);

        edit = findViewById(R.id.editIngredients);

        findViewById(R.id.btnMic).setOnClickListener(v -> startSpeech());
        findViewById(R.id.btnDone).setOnClickListener(v -> {
            String txt = edit.getText().toString();
            Intent i = new Intent(this, MoodMatchActivity.class);
            i.putExtra("mood", mood);
            i.putExtra("env", env);
            i.putExtra("energy", energy);
            i.putExtra("ingredients", txt);
            startActivity(i);
        });
    }

    private void startSpeech() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your ingredients");
            speechLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Speech not available on this device", Toast.LENGTH_SHORT).show();
        }
    }
}
