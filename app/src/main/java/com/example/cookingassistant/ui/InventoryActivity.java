package com.example.cookingassistant.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cookingassistant.databinding.ActivityItemEditBinding; // for ItemEdit binding (if you have it)
import com.example.cookingassistant.databinding.FragmentPantryBinding; // not used here but okay if generated
import com.example.cookingassistant.databinding.ActivityInventoryBinding;
import com.example.cookingassistant.db.AppDatabase;
import com.example.cookingassistant.db.Item;
import com.example.cookingassistant.db.ItemDao;

import java.util.List;
import java.util.concurrent.Executors;

public class InventoryActivity extends AppCompatActivity implements PantryListAdapter.OnClick {

    private ActivityInventoryBinding binding;
    private ItemDao dao;
    private PantryListAdapter adapter;

    private static final int REQ_EDIT = 100;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInventoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dao = AppDatabase.getInstance(this).itemDao();

        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PantryListAdapter(this);
        binding.recycler.setAdapter(adapter);

        binding.btnAdd.setOnClickListener(v -> {
            Intent i = new Intent(this, ItemEditActivity.class);
            startActivityForResult(i, REQ_EDIT);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItems();
    }

    private void loadItems() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Item> all = dao.getAllSync();
            runOnUiThread(() -> adapter.submit(all));
        });
    }

    @Override
    public void onItemClick(Item item) {
        Intent i = new Intent(this, ItemEditActivity.class);
        i.putExtra("id", item.id);
        startActivityForResult(i, REQ_EDIT);
    }
}
