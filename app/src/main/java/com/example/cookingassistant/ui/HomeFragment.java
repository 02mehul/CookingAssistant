package com.example.cookingassistant.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cookingassistant.R;
import com.example.cookingassistant.api.ApiClient;
import com.example.cookingassistant.api.model.MealResponse;
import com.example.cookingassistant.databinding.FragmentHomeBinding;
import com.example.cookingassistant.db.AppDatabase;
import com.example.cookingassistant.db.Item;
import com.example.cookingassistant.db.ItemDao;
import com.example.cookingassistant.ui.mood.MoodCaptureActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ItemDao dao;

    public HomeFragment() { super(R.layout.fragment_home); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentHomeBinding.bind(view);
        dao = AppDatabase.getInstance(requireContext()).itemDao();

        // Quick actions
        binding.cardMoodMeal.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MoodCaptureActivity.class)));

        binding.cardPantry.setOnClickListener(v -> selectBottomTab(getResources().getIdentifier("nav_pantry","id",requireContext().getPackageName())));
        binding.cardRecipes.setOnClickListener(v -> selectBottomTab(getResources().getIdentifier("nav_recipes","id",requireContext().getPackageName())));

        binding.cardSurprise.setOnClickListener(v -> {
            ApiClient.get().random().enqueue(new Callback<MealResponse>() {
                @Override public void onResponse(Call<MealResponse> call, Response<MealResponse> resp) {
                    if (resp.body() == null || resp.body().meals == null || resp.body().meals.isEmpty()) {
                        Toast.makeText(requireContext(), "No recipe found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String id = resp.body().meals.get(0).idMeal; // Meal has public idMeal in your model
                    Intent i = new Intent(requireContext(), RecipeDetailActivity.class);
                    i.putExtra("id", id);
                    startActivity(i);
                }
                @Override public void onFailure(Call<MealResponse> call, Throwable t) {
                    Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });

        loadStats();
    }

    @Override public void onResume() {
        super.onResume();
        loadStats();
    }

    private void loadStats() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Item> all = dao.getAllSync();

            int t = (all == null) ? 0 : all.size();
            int l = 0;
            if (all != null) {
                for (Item it : all) if (it.quantity <= 2) l++;
            }
            int p = (t == 0) ? 0 : Math.max(10, Math.min(100, (int) ((t - l) * 100f / Math.max(1, t))));

            // make effectively-final copies for the lambda
            final int totalFinal = t;
            final int lowFinal = l;
            final int percentFinal = p;

            requireActivity().runOnUiThread(() -> {
                binding.txtPercent.setText(percentFinal + "% stocked");
                binding.chipItems.setText("Items: " + totalFinal);
                binding.chipLow.setText("Low: " + lowFinal);
                try { binding.gauge.setProgress(percentFinal); } catch (Throwable ignored) {}
            });
        });
    }

    private void selectBottomTab(int id) {
        if (id == 0) return; // id not found; ignore
        BottomNavigationView bottom = requireActivity().findViewById(R.id.bottomNav);
        if (bottom != null) bottom.setSelectedItemId(id);
    }
}
