//package edu.sv.cmu.datacollectiononline.fragment;
//
//
//import android.os.Bundle;
//import android.support.v4.app.Fragment;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//public class EnergyParamsFragment extends Fragment{
//	private View rootView;
//	@Override
//	public View onCreateView(LayoutInflater inflater, ViewGroup container,
//			Bundle savedInstanceState){
//		rootView = inflater.inflate(R.layout.fragment_energy_params, container,false);
//		TextView txtView1 = (TextView) rootView.findViewById(R.id.txtView_energy_param_background);
//		txtView1.setText("0");
//		
//		TextView txtView2 = (TextView) rootView.findViewById(R.id.txtView_energy_param_speech);
//		txtView2.setText("0");
//		return rootView;
//	}
//	public View getRootView(){
//		return rootView;
//	}
//}
