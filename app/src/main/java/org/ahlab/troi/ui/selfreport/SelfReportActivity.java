package org.ahlab.troi.ui.selfreport;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.ahlab.troi.R;
import org.ahlab.troi.databinding.ActivitySelfReportBinding;
import org.ahlab.troi.ui.speech.SpeechInputActivity;

import java.util.Random;

public class SelfReportActivity extends AppCompatActivity {
  private static final String TAG = "### SELF_REPORT ###";
  private ActivitySelfReportBinding binding;
  private int selectedMode = -1;
  private Random random;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivitySelfReportBinding.inflate(getLayoutInflater());
    random = new Random();
    setContentView(binding.getRoot());
    Fragment fragment = initFragment();
    initButtons(fragment);
  }

  private void initButtons(Fragment dataFragment) {
    binding.btnSubmitReport.setOnClickListener(view -> goToPrediction(dataFragment));
  }

  private void goToPrediction(Fragment dataFragment) {
    Intent intent = new Intent(this, SpeechInputActivity.class);
    if (selectedMode == 0) {
      int categorical = ((CategoricalSelfReportFragment) dataFragment).getCategoricalEmotion();
      String customEmotion = ((CategoricalSelfReportFragment) dataFragment).getCustomEmotion();
      Log.i(TAG, String.format("category id: %d, custom emotion: %s", categorical, customEmotion));

      intent.putExtra(getString(R.string.key_self_category), categorical);
      intent.putExtra(getString(R.string.key_self_category_custom), customEmotion);
      intent.putExtra(getString(R.string.key_self_report_mode), 0);

    } else if (selectedMode == 1) {
      double arousal = ((DimensionalSelfReportFragment) dataFragment).getArousal();
      double valence = ((DimensionalSelfReportFragment) dataFragment).getValence();
      intent.putExtra(getString(R.string.key_self_arousal), arousal);
      intent.putExtra(getString(R.string.key_self_valence), valence);
      intent.putExtra(getString(R.string.key_self_report_mode), 1);
      Log.i(TAG, String.format("on data: arousal: %f, valence: %f", arousal, valence));
    }
    startActivity(intent);
  }

  private Fragment initFragment() {
    FragmentManager fragmentManager = getSupportFragmentManager();
    Fragment fragment;
    float rnd = random.nextFloat();
    if (rnd > 0.5) {
      fragment = new CategoricalSelfReportFragment();
      selectedMode = 0;
    } else {
      fragment = new DimensionalSelfReportFragment();
      selectedMode = 1;
    }

    FragmentTransaction transaction = fragmentManager.beginTransaction();
    transaction.replace(binding.selfReportStub.getId(), fragment);
    transaction.commit();

    return fragment;
  }
}
