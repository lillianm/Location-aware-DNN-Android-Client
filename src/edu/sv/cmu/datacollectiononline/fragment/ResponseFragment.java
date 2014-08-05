package edu.sv.cmu.datacollectiononline.fragment;


import edu.sv.cmu.datacollectiononline.R;
import edu.sv.cmu.datacollectiononline.R.id;
import edu.sv.cmu.datacollectiononline.R.layout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ResponseFragment extends Fragment{
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState){
		View rootView = inflater.inflate(R.layout.fragment_empty, container,false);
		TextView text = (TextView) rootView.findViewById(R.id.txtView_empty_message);
		text.setText("This is the reponse View ");
		return rootView;
	}

}
