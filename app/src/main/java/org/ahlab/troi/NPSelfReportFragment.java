package org.ahlab.troi;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import org.ahlab.troi.databinding.FragmentNPSelfReportBinding;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NPSelfReportFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NPSelfReportFragment extends Fragment {

	// TODO: Rename parameter arguments, choose names that match
	// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
	private static final String ARG_PARAM1 = "param1";
	private static final String ARG_PARAM2 = "param2";
	private static final String TAG = "###SELF_AV##";

	// TODO: Rename and change types of parameters
	private String mParam1;
	private String mParam2;

	public int getArousal() {
		return arousal;
	}

	public int getValence() {
		return valence;
	}

	private int arousal;
	private int valence;

	private FragmentNPSelfReportBinding binding;

	public NPSelfReportFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param param1 Parameter 1.
	 * @param param2 Parameter 2.
	 * @return A new instance of fragment NPSelfReport.
	 */
	// TODO: Rename and change types and number of parameters
	public static NPSelfReportFragment newInstance(String param1, String param2) {
		NPSelfReportFragment fragment = new NPSelfReportFragment();
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
		binding = FragmentNPSelfReportBinding.inflate(inflater, container, false);

		binding.rgArousal.setOnCheckedChangeListener((radioGroup, i) -> {
			if (i == binding.rArousal0.getId()) {
				arousal = -2;
//			} else if (i == binding.rArousal1.getId()) {
//				arousal = -1;
			} else if (i == binding.rArousal2.getId()) {
				arousal = 0;
//			} else if (i == binding.rArousal3.getId()) {
//				arousal = 1;
			} else if (i == binding.rArousal4.getId()) {
				arousal = 2;
			}
		});

		binding.rgValence.setOnCheckedChangeListener((radioGroup, i) -> {
			if (i == binding.rValence0.getId()) {
				valence = -2;
//			} else if (i == binding.rValence1.getId()) {
//				valence = -1;
			} else if (i == binding.rValence2.getId()) {
				valence = 0;
//			} else if (i == binding.rValence3.getId()) {
//				valence = 1;
			} else if (i == binding.rValence4.getId()) {
				valence = 2;
			}
		});

		return binding.getRoot();
	}
}