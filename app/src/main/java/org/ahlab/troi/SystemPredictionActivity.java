package org.ahlab.troi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import org.ahlab.troi.databinding.ActivitySystemPredictionBinding;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SystemPredictionActivity extends AppCompatActivity {
	FirebaseFirestore db;
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
	private String pid;
	private int triggerMode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivitySystemPredictionBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		db = FirebaseFirestore.getInstance();
		FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED).build();
		db.setFirestoreSettings(settings);

		SharedPreferences preferences = getSharedPreferences(getString(R.string.shared_preference), Context.MODE_PRIVATE);
		pid = preferences.getString(getString(R.string.key_pid), "");

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

			Log.i(TAG, "pid : " + pid + ", agree: " + degreeOfAgree + ", confidence: " + confidence + ", comment: " + comment);
			Map<String, Object> entry = new HashMap<>();
			entry.put(getString(R.string.key_self_report_mode), selfReportMode);
			if (selfReportMode == 0) {
				entry.put(getString(R.string.key_self_category), selfCategorical);
				entry.put(getString(R.string.key_self_category_custom), customCategory);
			} else {
				entry.put(getString(R.string.key_self_arousal), selfArousal);
				entry.put(getString(R.string.key_self_valence), selfValence);
			}
			entry.put(getString(R.string.key_pred_mode), systemMode);
			if (systemMode == 0) {
				entry.put(getString(R.string.key_predicted_category), predCategorical);
			} else {
				entry.put(getString(R.string.key_pred_arousal), predArousal);
				entry.put(getString(R.string.key_pred_valence), predValence);
			}
			entry.put(getString(R.string.key_trigger_mode), triggerMode);
			entry.put(getString(R.string.key_agree), degreeOfAgree);
			entry.put(getString(R.string.key_confidence), confidence);
			entry.put(getString(R.string.key_comment), comment);
			entry.put("key_ts", new Date());

			db.collection(pid).add(entry).addOnSuccessListener(documentReference -> {
				Log.i(TAG, "document added with id: " + documentReference.getId());

			}).addOnFailureListener(e -> {
				Log.e(TAG, "Error while saving the entry", e);
			});

			Intent intent = new Intent(getApplicationContext(), MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);

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
		triggerMode = extras.getInt(getString(R.string.key_trigger_mode));

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