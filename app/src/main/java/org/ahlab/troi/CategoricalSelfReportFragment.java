package org.ahlab.troi;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import org.ahlab.troi.databinding.FragmentCategoricalSelfReportBinding;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CategoricalSelfReportFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CategoricalSelfReportFragment extends Fragment {

	// TODO: Rename parameter arguments, choose names that match
	// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
	private static final String ARG_PARAM1 = "param1";
	private static final String ARG_PARAM2 = "param2";
	private static final String TAG = "###CAT_SELF_REPOT###";
	private FragmentCategoricalSelfReportBinding binding;

	// TODO: Rename and change types of parameters
	private String mParam1;
	private String mParam2;

	public int getCategoricalEmotion() {
		return categoricalEmotion;
	}

	public String getCustomEmotion() {
		return customEmotion;
	}

	private int categoricalEmotion;
	private String customEmotion="";

	public CategoricalSelfReportFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param param1 Parameter 1.
	 * @param param2 Parameter 2.
	 * @return A new instance of fragment CategoricalSelfReportFragment.
	 */
	// TODO: Rename and change types and number of parameters
	public static CategoricalSelfReportFragment newInstance(String param1, String param2) {
		CategoricalSelfReportFragment fragment = new CategoricalSelfReportFragment();
		Bundle args = new Bundle();
		args.putString(ARG_PARAM1, param1);
		args.putString(ARG_PARAM2, param2);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			mParam1 = getArguments().getString(ARG_PARAM1);
			mParam2 = getArguments().getString(ARG_PARAM2);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
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