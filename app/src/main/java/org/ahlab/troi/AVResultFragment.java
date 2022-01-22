package org.ahlab.troi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import org.ahlab.troi.databinding.FragmentAVResultBinding;

public class AVResultFragment extends Fragment {
	private String strArousal;
	private String strValence;
	private FragmentAVResultBinding binding;

	public AVResultFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			int arousal = getArguments().getInt(getString(R.string.key_pred_arousal));
			if (arousal > 0) {
				strArousal = "Arousal Level: High";
			} else if (arousal < 0) {
				strArousal = "Arousal Level: Low";
			} else {
				strArousal = "Arousal Level: Neutral";
			}
			int valence = getArguments().getInt(getString(R.string.key_pred_valence));
			if (valence > 0) {
				strValence = "Valence Level: Positive";
			} else if (valence < 0) {
				strValence = "Valence Level: Negative";
			} else {
				strValence = "Valence Level: Neutral";
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		binding = FragmentAVResultBinding.inflate(inflater, container, false);
		binding.txtArousal.setText(strArousal);
		binding.txtValence.setText(strValence);
		return binding.getRoot();
	}
}