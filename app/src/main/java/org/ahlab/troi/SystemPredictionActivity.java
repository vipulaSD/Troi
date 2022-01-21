package org.ahlab.troi;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.ahlab.troi.databinding.ActivitySystemPredictionBinding;

import java.util.Random;

public class SystemPredictionActivity extends AppCompatActivity {
	private ActivitySystemPredictionBinding binding;
	private String TAG = "#####SYSTEM_REPORT#####";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		binding = ActivitySystemPredictionBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initFragment();
	}

	private void initFragment() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		Fragment fragment;
		Random random = new Random();
		float rnd = random.nextFloat();
		Log.i(TAG, "onCreate: randomVal: " + rnd);
		if (rnd > 0.5) {
			fragment = new CategoricalResultsFragment();
		} else {
			fragment = new AVResultFragment();
		}

		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(binding.systemResultFragment.getId(), fragment);
		transaction.commit();
	}
}