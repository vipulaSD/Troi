package org.ahlab.troi;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
	private int selfReportMode;
	private int selfArousal;
	private int selfValence;
	private int selfCategorical;
	private String customCategory;
	private int predArousal;
	private int predValence;
	private int predCategorical;

	private int systemMode;
	private int degreeOfAgree;
	private int confidence;
	private String comment = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivitySystemPredictionBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		processExtra();
		initFragment();
		initButton();

	}

	private void initButton() {
		binding.systemPredictComment.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				comment = editable.toString();

			}
		});
		binding.btnSubmitFeedback.setOnClickListener(view -> {
			degreeOfAgree = (int) binding.seekAgreeable.getValue();
			confidence = (int) binding.seekConfidence.getValue();

			Log.i(TAG, "agree: " + degreeOfAgree + ", confidence: " + confidence + ", comment: " + comment);
		});

	}

	private void processExtra() {
		Bundle extras = getIntent().getExtras();
		Log.i(TAG, "processExtra: " + extras);
		selfReportMode = extras.getInt(getString(R.string.key_self_report_mode));
		selfArousal = extras.getInt(getString(R.string.key_self_arousal));
		selfValence = extras.getInt(getString(R.string.key_self_valence));
		selfCategorical = extras.getInt(getString(R.string.key_self_category));
		customCategory = extras.getString(getString(R.string.key_self_category_custom), "");
		predArousal = extras.getInt(getString(R.string.key_pred_arousal));
		predValence = extras.getInt(getString(R.string.key_pred_valence));
		predCategorical = extras.getInt(getString(R.string.key_predicted_category));

		StringBuilder extraBuilder = new StringBuilder("Extras: ");
		extraBuilder.append("\nself report mode: ").append(selfReportMode);
		extraBuilder.append("\n self report arousal: ").append(selfArousal);
		extraBuilder.append("\n self report valence: ").append(selfValence);
		extraBuilder.append("\n self report categorical: ").append(selfCategorical);
		extraBuilder.append("\n self report custom: ").append(customCategory);
		extraBuilder.append("\n predicted arousal: ").append(predArousal);
		extraBuilder.append("\n predicated valence: ").append(predValence);
		extraBuilder.append("\n predicated category: ").append(predCategorical);

		Log.i(TAG, "processExtra: " + extraBuilder.toString());
	}

	private void initFragment() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		Fragment fragment;
		Random random = new Random();
		float rnd = random.nextFloat();
		Log.i(TAG, "onCreate: randomVal: " + rnd);
		if (rnd > 0.5) {
			fragment = new CategoricalResultsFragment();
			fragment.setArguments(getIntent().getExtras());
			systemMode = 0;
		} else {
			fragment = new AVResultFragment();
			fragment.setArguments(getIntent().getExtras());
			systemMode = 1;
		}

		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(binding.systemResultFragment.getId(), fragment);
		transaction.commit();
	}
}