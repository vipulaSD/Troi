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
	private int triggerMode;
	private int catPrediction;
	private int arousalPred;
	private int valencePred;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivitySelfReportBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		Fragment fragment = initFragment();
		initButtons(fragment);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			triggerMode = extras.getInt(getString(R.string.key_trigger_mode), 0);
			catPrediction = extras.getInt(getString(R.string.key_predicted_category));
			arousalPred = extras.getInt(getString(R.string.key_pred_arousal));
			valencePred = extras.getInt(getString(R.string.key_pred_valence));
		}
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
				double arousal = ((NPSelfReportFragment) dataFragment).getArousal();
				double valence = ((NPSelfReportFragment) dataFragment).getValence();
				intent.putExtra(getString(R.string.key_self_arousal), arousal);
				intent.putExtra(getString(R.string.key_self_valence), valence);
				intent.putExtra(getString(R.string.key_self_report_mode), 1);
				Log.i(TAG, "on data: arousal: " + arousal + ", valence: " + valence);
			}

			intent.putExtra(getString(R.string.key_predicted_category), catPrediction);
			intent.putExtra(getString(R.string.key_pred_arousal), arousalPred);
			intent.putExtra(getString(R.string.key_pred_valence), valencePred);
			intent.putExtra(getString(R.string.key_trigger_mode), triggerMode);
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