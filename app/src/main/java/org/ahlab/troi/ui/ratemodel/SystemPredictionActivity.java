package org.ahlab.troi.ui.ratemodel;

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

import org.ahlab.troi.MainActivity;
import org.ahlab.troi.R;
import org.ahlab.troi.databinding.ActivitySystemPredictionBinding;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SystemPredictionActivity extends AppCompatActivity {
  private static final String TAG = "##### SYSTEM_REPORT #####";
  private FirebaseFirestore db;
  private ActivitySystemPredictionBinding binding;
  private int selfReportMode;
  private double selfArousal;
  private double selfValence;
  private int selfCategorical;
  private String customCategory;
  private int predictedArousal;
  private int predictedValence;
  private int predictedCategory;
  private int systemMode;
  private int degreeOfAgree;
  private int confidence;
  private String comment = "";
  private String pid;
  private int triggerMode;
  private Random random;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivitySystemPredictionBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    db = FirebaseFirestore.getInstance();
    FirebaseFirestoreSettings settings =
        new FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build();
    db.setFirestoreSettings(settings);

    SharedPreferences preferences =
        getSharedPreferences(getString(R.string.shared_preference), Context.MODE_PRIVATE);
    pid = preferences.getString(getString(R.string.key_pid), "");

    random = new Random();
    processExtra();
    initRatingFragment();
    initButton();
  }

  private void initButton() {
    binding.systemPredictComment.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            /*
            no action require before text change
             */
          }

          @Override
          public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            /*
            no additional action require during text change
             */
          }

          @Override
          public void afterTextChanged(Editable editable) {
            comment = editable.toString();
          }
        });
    binding.btnSubmitFeedback.setOnClickListener(
        view -> {
          degreeOfAgree = (int) binding.seekAgreeable.getValue();
          confidence = (int) binding.seekConfidence.getValue();

          Log.i(
              TAG,
              String.format(
                  "pid: %s, agreement: %d, confidence: %d, comment: %s",
                  pid, degreeOfAgree, confidence, comment));
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
            entry.put(getString(R.string.key_predicted_category), predictedCategory);
          } else {
            entry.put(getString(R.string.key_pred_arousal), predictedArousal);
            entry.put(getString(R.string.key_pred_valence), predictedValence);
          }
          entry.put(getString(R.string.key_trigger_mode), triggerMode);
          entry.put(getString(R.string.key_agree), degreeOfAgree);
          entry.put(getString(R.string.key_confidence), confidence);
          entry.put(getString(R.string.key_comment), comment);
          entry.put("key_ts", new Date());

          db.collection(pid)
              .add(entry)
              .addOnSuccessListener(
                  documentReference ->
                      Log.i(
                          TAG,
                          String.format("document added with id: %s", documentReference.getId())))
              .addOnFailureListener(e -> Log.e(TAG, "Error while saving the entry", e));

          Intent intent = new Intent(getApplicationContext(), MainActivity.class);
          intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          startActivity(intent);
        });
  }

  private void processExtra() {
    Bundle extras = getIntent().getExtras();
    Log.i(TAG, "processExtra: " + extras);
    selfReportMode = extras.getInt(getString(R.string.key_self_report_mode));
    selfArousal = extras.getDouble(getString(R.string.key_self_arousal));
    selfValence = extras.getDouble(getString(R.string.key_self_valence));
    selfCategorical = extras.getInt(getString(R.string.key_self_category));
    customCategory = extras.getString(getString(R.string.key_self_category_custom), "");
    predictedArousal = extras.getInt(getString(R.string.key_pred_arousal));
    predictedValence = extras.getInt(getString(R.string.key_pred_valence));
    predictedCategory = extras.getInt(getString(R.string.key_predicted_category));
    triggerMode = extras.getInt(getString(R.string.key_trigger_mode));

    StringBuilder extraBuilder = new StringBuilder("Extras: ");
    extraBuilder.append("\nself report mode: ").append(selfReportMode);
    extraBuilder.append("\n self report arousal: ").append(selfArousal);
    extraBuilder.append("\n self report valence: ").append(selfValence);
    extraBuilder.append("\n self report categorical: ").append(selfCategorical);
    extraBuilder.append("\n self report custom: ").append(customCategory);
    extraBuilder.append("\n predicted arousal: ").append(predictedArousal);
    extraBuilder.append("\n predicated valence: ").append(predictedValence);
    extraBuilder.append("\n predicated category: ").append(predictedCategory);

    Log.i(TAG, String.format("processExtra: %s", extraBuilder));
  }

  private void initRatingFragment() {

    Fragment fragment;
    float rnd = random.nextFloat();
    Log.i(TAG, String.format("onCreate: randomVal: %f", rnd));
    if (rnd > 0.5) {
      fragment = new CategoricalResultsFragment();
      fragment.setArguments(getIntent().getExtras());
      systemMode = 0;
    } else {
      fragment = new DimensionalResultsFragment();
      fragment.setArguments(getIntent().getExtras());
      systemMode = 1;
    }

    replaceFragment(fragment);
  }

  private void replaceFragment(Fragment fragment) {
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction transaction = fragmentManager.beginTransaction();
    transaction.replace(binding.systemResultFragment.getId(), fragment);
    transaction.commit();
  }
}
