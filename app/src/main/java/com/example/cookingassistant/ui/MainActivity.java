package com.example.cookingassistant.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.cookingassistant.R;
import com.example.cookingassistant.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Bottom nav: swap fragments
        binding.bottomNav.setOnItemSelectedListener(item -> {
            Fragment f;
            int id = item.getItemId();
            if (id == R.id.nav_pantry) {
                f = new PantryFragment();
            } else if (id == R.id.nav_recipes) {
                f = new RecipesFragment();
            } else if (id == R.id.nav_sensors) {
                f = new SensorsLauncherFragment();
            } else {
                f = new HomeFragment();
            }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, f)
                    .commit();
            return true;
        });

        // If we were launched with a request to open a specific tab, honor it.
        // Otherwise default to Home.
        if (!handleDeepLink(getIntent())) {
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    // When MainActivity is already on top and gets a new Intent, switch the tab.
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    /** Programmatically select a tab from other parts of the app if needed. */
    public void selectTab(@IdRes int menuId) {
        binding.bottomNav.setSelectedItemId(menuId);
    }

    /** Read the "open_tab" extra and select the appropriate BottomNav item. */
    private boolean handleDeepLink(Intent intent) {
        if (intent == null) return false;
        String tab = intent.getStringExtra("open_tab");
        if (tab == null) return false;

        switch (tab) {
            case "pantry":
                binding.bottomNav.setSelectedItemId(R.id.nav_pantry);
                return true;
            case "recipes":
                binding.bottomNav.setSelectedItemId(R.id.nav_recipes);
                return true;
            case "sensors":
                binding.bottomNav.setSelectedItemId(R.id.nav_sensors);
                return true;
            case "home":
                binding.bottomNav.setSelectedItemId(R.id.nav_home);
                return true;
            default:
                return false;
        }
    }
}
