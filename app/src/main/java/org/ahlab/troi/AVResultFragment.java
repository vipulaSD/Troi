package org.ahlab.troi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.ahlab.troi.databinding.FragmentAVResultBinding;

public class AVResultFragment extends Fragment {
	private FragmentAVResultBinding binding;

	public AVResultFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		binding = FragmentAVResultBinding.inflate(inflater, container, false);
		if (getArguments() != null) {
			int arousal = getArguments().getInt(getString(R.string.key_pred_arousal));
			if (arousal > 0) {
				binding.imgArousal.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_arousal_4));
			} else if (arousal < 0) {
				binding.imgArousal.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_arousal_0));
			} else {
				binding.imgArousal.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_arousal_2));
			}
			int valence = getArguments().getInt(getString(R.string.key_pred_valence));
			if (valence > 0) {
				binding.imgValence.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_valence_4));
			} else if (valence < 0) {
				binding.imgValence.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_valence_0));
			} else {
				binding.imgValence.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.ic_valence_2));
			}
		}
		return binding.getRoot();
	}
}