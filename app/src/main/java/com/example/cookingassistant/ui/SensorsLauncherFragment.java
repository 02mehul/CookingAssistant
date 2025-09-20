package com.example.cookingassistant.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.cookingassistant.R;
import com.example.cookingassistant.databinding.FragmentSensorsLauncherBinding;

public class SensorsLauncherFragment extends Fragment {
    private FragmentSensorsLauncherBinding binding;
    public SensorsLauncherFragment(){ super(R.layout.fragment_sensors_launcher); }

    @Override public void onViewCreated(android.view.View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        binding = FragmentSensorsLauncherBinding.bind(v);
        binding.btnOpenSensors.setOnClickListener(x ->
                startActivity(new Intent(requireContext(), SensorsActivity.class)));
    }
}
