package com.example.cookingassistant.ui;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.cookingassistant.R;
import com.example.cookingassistant.databinding.FragmentHomeBinding;
import com.example.cookingassistant.db.AppDatabase;
import com.example.cookingassistant.db.Item;
import com.example.cookingassistant.db.ItemDao;
import java.util.List;
import java.util.concurrent.Executors;
import android.content.Intent;
import android.widget.Toast;
import com.example.cookingassistant.api.ApiClient;
import com.example.cookingassistant.api.model.MealResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private ItemDao dao;

    public HomeFragment() { super(R.layout.fragment_home); }

    @Override public void onViewCreated(android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentHomeBinding.bind(view);
        dao = AppDatabase.getInstance(requireContext()).itemDao();

        binding.btnToPantry.setOnClickListener(v ->
                ((MainActivity) requireActivity()).selectTab(R.id.nav_pantry));
        binding.btnToRecipes.setOnClickListener(v ->
                ((MainActivity) requireActivity()).selectTab(R.id.nav_recipes));
        binding.btnSurprise.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Picking a random recipe…", Toast.LENGTH_SHORT).show();
            ApiClient.get().random().enqueue(new Callback<MealResponse>() {
                @Override public void onResponse(Call<MealResponse> call, Response<MealResponse> resp) {
                    if (resp.body() != null && resp.body().meals != null && !resp.body().meals.isEmpty()) {
                        String id = resp.body().meals.get(0).idMeal;
                        Intent i = new Intent(requireContext(), RecipeDetailActivity.class);
                        i.putExtra("id", id);
                        startActivity(i);
                    } else {
                        Toast.makeText(requireContext(), "No recipe found. Try again.", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onFailure(Call<MealResponse> call, Throwable t) {
                    Toast.makeText(requireContext(), "Network error. Try again.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        refreshStats();
    }

    @Override public void onResume() {
        super.onResume();
        refreshStats();
    }

    private void refreshStats() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Item> items = dao.getAllSync();
            int total = items.size();
            int lowCount = 0;
            for (Item i : items) if (i.quantity <= 1) lowCount++;

            final int totalFinal = total;
            final int lowFinal = lowCount;
            final int pct = totalFinal == 0 ? 0 : (int) (((totalFinal - lowFinal) * 100f) / totalFinal);

            requireActivity().runOnUiThread(() -> {
                binding.gauge.setPercentage(pct);
                binding.txtItemsTotal.setText("Items: " + totalFinal);
                binding.txtItemsLow.setText("Low: " + lowFinal);
            });
        });
    }
}
