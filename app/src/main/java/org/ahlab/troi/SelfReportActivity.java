package org.ahlab.troi;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.ahlab.troi.databinding.ActivitySelfReportBinding;

import java.util.Random;

public class SelfReportActivity extends AppCompatActivity {
	private final String TAG = "#####SELF_REPORT#####";
	private ActivitySelfReportBinding binding;
	private int selectedMode = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		binding = ActivitySelfReportBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		Fragment fragment = initFragment();
		initButtons(fragment);
	}

	private void initButtons(Fragment dataFragment) {
		binding.btnSubmitReport.setOnClickListener(view -> {
			Intent intent = new Intent(this, SystemPredictionActivity.class);
			if (selectedMode == 0) {
				int selfEmotionCategory = ((CategoricalSelfReportFragment) dataFragment).getCategoricalEmotion();
				String customEmotionCategory = ((CategoricalSelfReportFragment) dataFragment).getCustomEmotion();
				Log.i(TAG, "category id: " + selfEmotionCategory + ", customEmotion: " + customEmotionCategory);
				intent.putExtra(getString(R.string.key_self_category), selfEmotionCategory);
				intent.putExtra(getString(R.string.key_self_category_custom), customEmotionCategory);
				intent.putExtra(getString(R.string.key_self_report_mode), 0);
			} else if (selectedMode == 1) {
				int arousal = ((NPSelfReportFragment) dataFragment).getArousal();
				int valence = ((NPSelfReportFragment) dataFragment).getValence();
				intent.putExtra(getString(R.string.key_self_arousal), arousal);
				intent.putExtra(getString(R.string.key_self_valence), valence);
				intent.putExtra(getString(R.string.key_self_report_mode), 1);
				Log.i(TAG, "on data: arousal: " + arousal + ", valence: " + valence);
			}

			intent.putExtra(getString(R.string.key_predicted_category), 0); // @TODO replace with model values
			intent.putExtra(getString(R.string.key_pred_arousal), 1); // @TODO replace with model values
			intent.putExtra(getString(R.string.key_pred_valence), 1); //@TODO replace with model values
			startActivity(intent);
		});
	}

	private Fragment initFragment() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		Fragment fragment;
		Random random = new Random();
		float rnd = random.nextFloat();
		if (rnd > 0.5) {
			fragment = new CategoricalSelfReportFragment();
			selectedMode = 0;
		} else {
			fragment = new NPSelfReportFragment();
			selectedMode = 1;
		}

		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(binding.selfReportStub.getId(), fragment);
		transaction.commit();

		return fragment;
	}
}