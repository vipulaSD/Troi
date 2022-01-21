package org.ahlab.troi;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
		binding = FragmentCategoricalSelfReportBinding.inflate(inflater,container,false);
		binding.txtSelfEmotion.setVisibility(View.INVISIBLE);
		binding.chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
			Log.i(TAG, "onCreateView: check changed: "+checkedId);
			if(checkedId==binding.chipNon.getId()){
				binding.txtSelfEmotion.clearComposingText();
				binding.txtSelfEmotion.setVisibility(View.VISIBLE);
			}else{
				binding.txtSelfEmotion.setVisibility(View.INVISIBLE);
			}
		});

		return binding.getRoot();
	}
}