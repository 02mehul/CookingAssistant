package com.example.cookingassistant.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.cookingassistant.R;
import com.example.cookingassistant.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNav.setOnItemSelectedListener(item -> {
            Fragment f;
            int id = item.getItemId();
            if (id == R.id.nav_pantry) f = new PantryFragment();
            else if (id == R.id.nav_recipes) f = new RecipesFragment();
            else if (id == R.id.nav_sensors) f = new SensorsLauncherFragment();
            else f = new HomeFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, f)
                    .commit();
            return true;
        });

        // Select default AFTER listener is set so it loads HomeFragment immediately
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
    }

    public void selectTab(int menuId) {
        binding.bottomNav.setSelectedItemId(menuId);
    }

}
