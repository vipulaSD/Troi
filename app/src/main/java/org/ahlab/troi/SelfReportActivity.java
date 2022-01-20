package org.ahlab.troi;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.ahlab.troi.databinding.ActivitySelfReportBinding;

import java.util.Random;

public class SelfReportActivity extends AppCompatActivity {
	private String TAG = "%%%%%%%%";
	private ActivitySelfReportBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		binding = ActivitySelfReportBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initFragment();
		initButtons();
	}

	private void initButtons() {
//		binding.
	}

	private void initFragment() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		Fragment fragment;
		Random random = new Random();
		float rnd = random.nextFloat();
		Log.i(TAG, "onCreate: randomVal: " + rnd);
		if (rnd > 0.5) {
			fragment = new CategoricalSelfReportFragment();
		} else {
			fragment = new NPSelfReportFragment();
		}

		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(binding.selfReportStub.getId(), fragment);
		transaction.commit();
	}
}