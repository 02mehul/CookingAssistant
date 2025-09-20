package com.example.cookingassistant.ui;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cookingassistant.R;
import com.example.cookingassistant.db.ItemDao;
import com.example.cookingassistant.db.Item;
import com.example.cookingassistant.db.AppDatabase;

import java.util.List;
import java.util.concurrent.Executors;

public class RecipeActivity extends AppCompatActivity {

    private ItemDao dao;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipes);

        dao = AppDatabase.getInstance(this).itemDao();

        // Example: fetch pantry items in a background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Item> pantryItems = dao.getAllSync();
            runOnUiThread(() -> {
                // TODO: use pantryItems to suggest recipes
            });
        });
    }
}
