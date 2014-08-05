package edu.sv.cmu.datacollectiononline;

import edu.sv.cmu.datacollectiononline.thread.AudioThread;
import edu.sv.cmu.datacollectiononline.util.Energy;
//import edu.sv.cmu.datacollectiononline.view.BarChartView;
//import edu.sv.cmu.datacollectiononline.view.MyCompassView;
import edu.sv.cmu.datacollectiononline.view.MyTextView;
import edu.sv.cmu.datacollectiononline.view.StatusView;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FragmentUserModeWrapper extends Fragment{
	
	public final static String TAG = "Fragment_UserMode";
	
	/* views and layouts */
	private View rootView; 
	public LinearLayout ll_msg;
	public LinearLayout ll_params;
	public LinearLayout ll_response;
	public MyTextView mytxtView_socket_status = null;
	public MyTextView mytxtView_recording_status = null;
	public StatusView status_socket = null;
	public StatusView status_recording = null;

	public TextView txtView_response;
	
	private FrameLayout fl_message;
	private FrameLayout fl_params;
	private FrameLayout fl_response;

	/* the reference to object itself */
	private FragmentUserModeWrapper self;

	/* constructor */
	public FragmentUserModeWrapper(){
		super();
		this.self = this;
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState){
		rootView = inflater.inflate(R.layout.fragment_usermode_view, container,false);
		fl_message = (FrameLayout) rootView.findViewById(R.id.message_show);
		ll_msg = new LinearLayout(getActivity());
		ll_msg.setOrientation(LinearLayout.VERTICAL);
		fl_message.addView(ll_msg);

		//mytxtView_socket_status = new MyTextView(getActivity(), 18, Color.BLACK, "socket_status".hashCode());
		//mytxtView_recording_status = new MyTextView(getActivity(), 18, Color.BLACK, "recording_status".hashCode());
		status_socket = new StatusView(getActivity());
		status_recording = new StatusView(getActivity());
		ll_msg.addView(status_socket);
		ll_msg.addView(status_recording);
		//ll_msg.addView(mytxtView_socket_status);
		//ll_msg.addView(mytxtView_recording_status);


		fl_response = (FrameLayout) rootView.findViewById(R.id.response_text_show);
		ll_response = new LinearLayout(getActivity());
		ll_response.setOrientation(LinearLayout.VERTICAL);
		fl_response.addView(ll_response);

		txtView_response = new TextView(getActivity());
		txtView_response.setTextSize(18);
		txtView_response.setTextColor(Color.BLUE);
		ll_response.addView(txtView_response);


		fl_params = (FrameLayout) rootView.findViewById(R.id.context_params_show);
		ll_params = new LinearLayout(getActivity());
		ll_params.setOrientation(LinearLayout.VERTICAL);
		fl_params.addView(ll_params);
		//chart = new BarChartView(self.getActivity(),5);
		//ll_params.addView(chart);

		return rootView;
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		activity.getFragmentManager().beginTransaction().addToBackStack(null);
		if(activity == null || activity.isDestroyed()){
			Log.d(TAG,"MainActivity destroyed");

		}
	}
	@Override
	public void onDetach(){
		super.onDetach();
	}
	public View getRootView(){
		return rootView;
	}
	public FragmentManager getFM(){
		return getFragmentManager();
	}

}
