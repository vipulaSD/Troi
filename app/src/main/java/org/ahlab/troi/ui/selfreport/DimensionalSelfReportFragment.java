package org.ahlab.troi.ui.selfreport;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.slider.Slider;

import org.ahlab.troi.databinding.FragmentDimensionalSelfReportBinding;

public class DimensionalSelfReportFragment extends Fragment {

    private static final String TAG = "### DIMENSIONAL_SELF_REPORT_FRAGMENT ###";

    private double arousal;
    private double valence;
    private FragmentDimensionalSelfReportBinding binding;

    public DimensionalSelfReportFragment() {
        // Required empty public constructor
    }


    public double getArousal() {
        return arousal;
    }

    public double getValence() {
        return valence;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDimensionalSelfReportBinding.inflate(inflater, container, false);

        binding.slideArousal.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                arousal = slider.getValue();
            }
        });

        binding.slideValence.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                valence = slider.getValue();
            }
        });
        return binding.getRoot();
    }
}