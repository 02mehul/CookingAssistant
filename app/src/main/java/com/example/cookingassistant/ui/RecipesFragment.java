package com.example.cookingassistant.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.cookingassistant.api.ApiClient;
import com.example.cookingassistant.api.model.Meal;
import com.example.cookingassistant.api.model.MealResponse;
import com.example.cookingassistant.databinding.FragmentRecipesBinding;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecipesFragment extends Fragment implements RecipeAdapter.OnClick {
    private FragmentRecipesBinding binding;
    private RecipeAdapter adapter;

    public RecipesFragment() { super(com.example.cookingassistant.R.layout.fragment_recipes); }

    @Override public void onViewCreated(android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding = FragmentRecipesBinding.bind(view);

        binding.recipeList.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new RecipeAdapter(this);
        binding.recipeList.setAdapter(adapter);

        binding.btnSearch.setOnClickListener(v -> search(binding.edtQuery.getText().toString().trim()));
        search("chicken"); // initial
    }

    private void search(String q) {
        ApiClient.get().search(q.isEmpty() ? "chicken" : q).enqueue(new Callback<MealResponse>() {
            @Override public void onResponse(Call<MealResponse> call, Response<MealResponse> resp) {
                adapter.submit(resp.body() != null ? resp.body().meals : java.util.Collections.emptyList());
            }
            @Override public void onFailure(Call<MealResponse> call, Throwable t) {
                adapter.submit(java.util.Collections.emptyList());
            }
        });
    }

    @Override public void onRecipe(Meal m) {
        Intent i = new Intent(requireContext(), RecipeDetailActivity.class);
        i.putExtra("id", m.idMeal);
        startActivity(i);
    }
}
