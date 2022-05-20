package org.ahlab.troi.ui.selfreport;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.ahlab.troi.databinding.FragmentCategoricalSelfReportBinding;

public class CategoricalSelfReportFragment extends Fragment {

    private static final String TAG = "### CATEGORICAL_SELF_REPORT ###";
    private FragmentCategoricalSelfReportBinding binding;

    public int getCategoricalEmotion() {
        return categoricalEmotion;
    }

    public String getCustomEmotion() {
        return customEmotion;
    }

    private int categoricalEmotion;
    private String customEmotion = "";

    public CategoricalSelfReportFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCategoricalSelfReportBinding.inflate(inflater, container, false);
        binding.txtSelfEmotion.setVisibility(View.INVISIBLE);
        binding.chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Log.i(TAG, "onCreateView: check changed: " + checkedId);
            if (checkedId == binding.chipNon.getId()) {
                binding.txtSelfEmotion.clearComposingText();
                binding.txtSelfEmotion.setVisibility(View.VISIBLE);
                categoricalEmotion = -1;
            } else {
                binding.txtSelfEmotion.setVisibility(View.INVISIBLE);
                if (checkedId == binding.chipCheerful.getId()) {
                    categoricalEmotion = 0;
                } else if (checkedId == binding.chipHappy.getId()) {
                    categoricalEmotion = 1;
                } else if (checkedId == binding.chipAngry.getId()) {
                    categoricalEmotion = 2;
                } else if (checkedId == binding.chipNervous.getId()) {
                    categoricalEmotion = 3;
                } else if (checkedId == binding.chipSad.getId()) {
                    categoricalEmotion = 4;
                } else if (checkedId == binding.chipNeutral.getId()) {
                    categoricalEmotion = 5;
                }

            }
        });

        binding.txtSelfEmotion.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                customEmotion = editable.toString();
                Log.i(TAG, "afterTextChanged: custom emotion " + customEmotion);
            }
        });

        return binding.getRoot();
    }
}