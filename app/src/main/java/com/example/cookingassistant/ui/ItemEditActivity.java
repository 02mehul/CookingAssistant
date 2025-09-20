package com.example.cookingassistant.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cookingassistant.databinding.ActivityItemEditBinding;
import com.example.cookingassistant.db.AppDatabase;
import com.example.cookingassistant.db.Item;
import com.example.cookingassistant.db.ItemDao;
import java.util.concurrent.Executors;

public class ItemEditActivity extends AppCompatActivity {
    private ActivityItemEditBinding binding;
    private ItemDao dao;
    private int id = 0;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityItemEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        dao = AppDatabase.getInstance(this).itemDao();

        id = getIntent().getIntExtra("id", 0);
        if (id != 0) {
            Executors.newSingleThreadExecutor().execute(() -> {
                Item it = dao.findByIdSync(id);
                if (it != null) runOnUiThread(() -> {
                    binding.editName.setText(it.name);
                    binding.editQuantity.setText(String.valueOf(it.quantity));
                });
            });
        }

        binding.btnSave.setOnClickListener(v -> {
            String name = binding.editName.getText().toString().trim();
            String qtyStr = binding.editQuantity.getText().toString().trim();
            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(qtyStr)) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show(); return;
            }
            int qty;
            try { qty = Integer.parseInt(qtyStr); } catch (Exception e) {
                Toast.makeText(this, "Quantity must be a number", Toast.LENGTH_SHORT).show(); return;
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                if (id == 0) dao.insert(new Item(name, qty)); else {
                    Item it = dao.findByIdSync(id);
                    if (it != null) { it.name = name; it.quantity = qty; dao.update(it); }
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                    finish(); // return to pantry
                });
            });
        });
    }
}
