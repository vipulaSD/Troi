package org.ahlab.troi.ui.ratemodel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.ahlab.troi.R;
import org.ahlab.troi.databinding.FragmentCategoricalResultsBinding;

public class CategoricalResultsFragment extends Fragment {
  private String systemPrediction;

  public CategoricalResultsFragment() {
    // Required empty public constructor
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String[] categories = {
      getString(R.string.emo_cheerful),
      getString(R.string.emo_happy),
      getString(R.string.emo_angry),
      getString(R.string.emo_nervous),
      getString(R.string.emo_sad),
      getString(R.string.emo_neutral)
    };
    if (getArguments() != null) {
      int prediction = getArguments().getInt(getString(R.string.key_predicted_category));
      systemPrediction = categories[prediction];
    }
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    FragmentCategoricalResultsBinding binding =
        FragmentCategoricalResultsBinding.inflate(inflater, container, false);

    binding.txtCategorical.setText(systemPrediction);

    return binding.getRoot();
  }
}
