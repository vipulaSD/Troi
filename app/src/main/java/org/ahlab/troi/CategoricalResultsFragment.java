package org.ahlab.troi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import org.ahlab.troi.databinding.FragmentCategoricalResultsBinding;

public class CategoricalResultsFragment extends Fragment {
	private String systemPrediction;
	private FragmentCategoricalResultsBinding binding;
	public CategoricalResultsFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String[] categories = {getString(R.string.emo_cheerful), getString(R.string.emo_happy), getString(R.string.emo_angry), getString(R.string.emo_nervous), getString(R.string.emo_sad), getString(R.string.emo_neutral)};
		if (getArguments() != null) {
			int pred = getArguments().getInt(getString(R.string.key_predicted_category));
			systemPrediction = categories[pred];
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		binding = FragmentCategoricalResultsBinding.inflate(inflater,container,false);

		binding.txtCategorical.setText(systemPrediction);

		return binding.getRoot();
	}
}