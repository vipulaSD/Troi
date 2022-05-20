package org.ahlab.troi.ui.ratemodel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.ahlab.troi.R;
import org.ahlab.troi.databinding.FragmentDimensionalResultBinding;

public class DimensionalResultsFragment extends Fragment {
	private FragmentDimensionalResultBinding binding;

	public DimensionalResultsFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentDimensionalResultBinding.inflate(inflater, container, false);
		if (getArguments() != null) {
			int arousal = getArguments().getInt(getString(R.string.key_pred_arousal));
			if (arousal > 1) {
				binding.txtArousal.setText(getString(R.string.as_high_arousal));
			} else if (arousal < 1) {
				binding.txtArousal.setText(getString(R.string.as_low_arousal));
			} else {
				binding.txtArousal.setText(getString(R.string.as_neutral));
			}
			int valence = getArguments().getInt(getString(R.string.key_pred_valence));
			if (valence > 1) {
				binding.txtValence.setText(getString(R.string.as_positive_valence));
			} else if (valence < 1) {
				binding.txtValence.setText(getString(R.string.as_negative_valence));
			} else {
				binding.txtValence.setText(getString(R.string.as_neutral));
			}
		}
		return binding.getRoot();
	}
}