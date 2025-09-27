package com.example.cookingassistant.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cookingassistant.R;
import com.example.cookingassistant.databinding.ActivityShoppingListBinding;
import com.example.cookingassistant.db.AppDatabase;
import com.example.cookingassistant.db.ShoppingItem;
import com.example.cookingassistant.db.ShoppingItemDao;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.concurrent.Executors;

public class ShoppingListActivity extends AppCompatActivity {

    private ActivityShoppingListBinding binding;
    private ShoppingItemDao dao;
    private ShoppingListAdapter adapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShoppingListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dao = AppDatabase.getInstance(this).shoppingItemDao();

        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ShoppingListAdapter(new ShoppingListAdapter.Actions() {
            @Override public void onToggle(ShoppingItem item) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    item.checked = !item.checked;
                    dao.update(item);
                    load();
                });
            }

            @Override public void onDelete(ShoppingItem item) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    dao.delete(item);
                    runOnUiThread(() ->
                            Snackbar.make(binding.getRoot(), "Removed", Snackbar.LENGTH_SHORT).show());
                    load();
                });
            }
        });
        binding.recycler.setAdapter(adapter);

        load();
    }

    private void load() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ShoppingItem> all = dao.getAllSync();
            runOnUiThread(() -> {
                adapter.submit(all);
                binding.empty.setVisibility(all == null || all.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.shopping_menu, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        if (item.getItemId() == R.id.action_clear_shopping) {
            Executors.newSingleThreadExecutor().execute(() -> {
                dao.clearAll();
                load();
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
