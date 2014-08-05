package edu.sv.cmu.datacollectiononline;

import edu.sv.cmu.datacollectiononline.thread.AudioThread;
import edu.sv.cmu.datacollectiononline.view.BarChartView;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FragmentParamsWrapper extends Fragment{
	private View rootView; 
	public BarChartView chart = null;
	private FragmentParamsWrapper self;
	public FragmentParamsWrapper(){
		super();
		this.self = this;
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState){
		rootView = inflater.inflate(R.layout.fragment_params_view, container,false);
		LinearLayout ll = (LinearLayout) rootView.findViewById(R.id.ll_pfview);
		final EditText editTXT = (EditText) rootView.findViewById(R.id.threshold_test_adjust);
		Button submit = (Button) rootView.findViewById(R.id.submit);
		submit.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if(((MainActivity)self.getActivity()).t_audio != null && ((MainActivity)self.getActivity()).t_audio.isAlive()){
					int new_thre = Integer.parseInt(editTXT.getText().toString());
					((MainActivity)self.getActivity()).t_audio.threshold_testing = new_thre;
				}


			}
		});
		Button reset = (Button) rootView.findViewById(R.id.reset);
		reset.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				((MainActivity)self.getActivity()).t_audio.audio_handler.sendEmptyMessage(AudioThread.RESET);

			}
		})
		;
		chart = new BarChartView(self.getActivity(),5);
		ll.addView(chart);


		return rootView;
	}
	public View getRootView(){
		return rootView;
	}
	public FragmentManager getFM(){
		return getFragmentManager();
	}

}
