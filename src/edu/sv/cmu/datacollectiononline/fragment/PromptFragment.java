//package edu.sv.cmu.speechclient.fragment;
//
//import edu.sv.cmu.speechclient.MainActivity;
//import edu.sv.cmu.speechclient.PromptFactory;
//import edu.sv.cmu.speechclient.R;
//import edu.sv.cmu.speechclient.R.drawable;
//import edu.sv.cmu.speechclient.R.id;
//import edu.sv.cmu.speechclient.R.layout;
//import edu.sv.cmu.speechclient.thread.AudioThread;
//import android.content.SharedPreferences;
//import android.graphics.drawable.Drawable;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Message;
//import android.support.v4.app.Fragment;
//import android.text.Spannable;
//import android.text.SpannableStringBuilder;
//import android.text.method.ScrollingMovementMethod;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.View.OnClickListener;
//import android.view.View.OnLongClickListener;
//import android.widget.Button;
//import android.widget.TextView;
//import android.widget.Toast;
//
///**
// * A fragment representing the prompted speech portion of the app
// */
//public class PromptFragment extends Fragment {
//	
//	/*
//	 * constants
//	 */
//	private static final String TAG = "PromptFragment";
//	public static final int PROMPT_ID = 0;
//	public static final int RESPONSE_ID = 1;
//	
//	/*
//	 * globals
//	 */
//	public Spannable currentPrompt;
//	private int frag_id;
//	private MainActivity act;
//	
//	public PromptFragment() {
//	}
//
//	@Override
//	public View onCreateView(LayoutInflater inflater, ViewGroup container,
//			Bundle savedInstanceState) {
//		
//		Log.i(TAG, "onCreateView of Prompt");
//		
//		//inflate the main layout
//		View rootView = inflater.inflate(R.layout.fragment_prompt,
//				container, false);
//		
//		//get the args from the activity
//		frag_id = getArguments().getInt(MainActivity.ARG_SECTION_NUMBER);
//
//		//get the activity
//		act = (MainActivity)getActivity();
//		
//		//get the connection information from preferences
//		/*SharedPreferences prefs = act.getSharedPreferences(MainActivity.PREFS_NAME, 
//				MainActivity.MODE_PRIVATE);
//		int server_port = prefs.getInt(MainActivity.PREFS_SERVER_PORT, MainActivity.DEFAULT_SERVER_PORT);
//		int server_port_cn = prefs.getInt(MainActivity.PREFS_SERVER_PORT_CN, 
//				MainActivity.DEFAULT_SERVER_PORT_CN);
//		String server_addr = prefs.getString(MainActivity.PREFS_SERVER_IP, MainActivity.DEFAULT_SERVER_ADDR);*/
//		
//		//generate a random prompt for the user
//		currentPrompt = new SpannableStringBuilder(PromptFactory.getRandomPrompt());
//		
//		//display the prompt
//		((TextView) rootView.findViewById(R.id.prompt_view)).setText(currentPrompt);
//		
//		//make the response view scrollable
//		((TextView)rootView.findViewById(R.id.response_view)).setMovementMethod(new ScrollingMovementMethod());
//		
//		//pre-initialize the data connection to cut down on button lag time
//		//TODO this doesn't seem to make sense in prompt fragment
//		/*if (frag_id == MainActivity.FREESTYLE_FRAG_ID) 
//			act.getAudioThread(server_addr, server_port, frag_id, false);
//		else if (frag_id == MainActivity.FREESTYLE_CN_FRAG_ID)
//			act.getAudioThread(server_addr, server_port_cn, frag_id, false);
//		else
//			act.getAudioThread(server_addr, server_port, frag_id, false);*/
//		
//		return rootView;
//	}
//	
//	@Override
//	public void onActivityCreated(Bundle savedInstanceState) {
//		super.onActivityCreated(savedInstanceState);
//		
//		Log.i(TAG, "onActivityCreated of Prompt");
//		
//		/*
//		 * set up button listeners
//		 */
//		final Button recordBtn = (Button)this.getView().findViewById(R.id.record_button);
//		Button skipBtn = (Button)this.getView().findViewById(R.id.skip_button);
//		/*
//		 * record listener
//		 */
//		recordBtn.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				Log.i(TAG, "record button clicked");
//				
//				SharedPreferences prefs = act.getSharedPreferences(MainActivity.PREFS_NAME, 
//						MainActivity.MODE_PRIVATE);
//				
//				String server_addr = prefs.getString(MainActivity.PREFS_SERVER_IP, 
//						MainActivity.DEFAULT_SERVER_ADDR);
//				
//				//get the right data thread to use
//				AudioThread data;
//				int port;
//				if (frag_id == MainActivity.FREESTYLE_CN_FRAG_ID) {
//					port = prefs.getInt(MainActivity.PREFS_SERVER_PORT_CN, 
//							MainActivity.DEFAULT_SERVER_PORT_CN);
//					data = act.getAudioThread(server_addr, port,  false);
//				} else {
//					port = prefs.getInt(MainActivity.PREFS_SERVER_PORT, 
//							MainActivity.DEFAULT_SERVER_PORT);
//					data = act.getAudioThread(server_addr, port,  false);
//				}
//				
//				//if not already recording start recording
//				if(!act.recording) {
//					
//					//start capturing
//					data.startCapture();
//					
//					//make sure the prompt is fresh
//					Handler ui = act.ui_handler;
//					Message msg = ui.obtainMessage();
//					msg.what = MainActivity.UPDATE_PROMPT_TEXT;
//					currentPrompt = new SpannableStringBuilder(currentPrompt.toString());
//					msg.obj = currentPrompt;
//					msg.arg2 = frag_id;
//					ui.sendMessage(msg);
//					
//				} else {
//					//already recording so stop recording
//					if(data.isAlive()) {
//						data.stopCapture();
//					} else {
//						act.recording = false;
//						
//						//display a toast to let the user know something is up
//						Toast.makeText(act, "Unable to send recorded data to server", Toast.LENGTH_LONG).show();
//						
//						//make sure the recording button is back to off
//						Drawable alt = act.getResources().getDrawable(
//								R.drawable.button_off_bg);
//						recordBtn.setBackgroundDrawable(alt); //API VERSION < 16
//						//recordBtn.setBackground(alt); //API VERSION >= 16
//						
//						Log.v(TAG, "Data thread was dead while trying to stop the recording");
//					}
//				}
//			}
//		});
//		
//		/*
//		 * set up a long click listener for an alternate way of recording
//		 */
//		recordBtn.setOnLongClickListener(new OnLongClickListener() {
//			@Override
//			public boolean onLongClick(View v) {
//				Log.i(TAG, "record button held");
//				
//				//get the activity
//				MainActivity act = (MainActivity)getActivity();
//				
//				SharedPreferences prefs = act.getSharedPreferences(MainActivity.PREFS_NAME, 
//						MainActivity.MODE_PRIVATE);
//				
//				String server_addr = prefs.getString(MainActivity.PREFS_SERVER_IP, 
//						MainActivity.DEFAULT_SERVER_ADDR);
//				
//				//get the right data thread to use
//				AudioThread data;
//				int port;
//				if (frag_id == MainActivity.FREESTYLE_CN_FRAG_ID) {
//					port = prefs.getInt(MainActivity.PREFS_SERVER_PORT_CN, 
//							MainActivity.DEFAULT_SERVER_PORT_CN);
//					data = act.getAudioThread(server_addr, port,  false);
//				} else {
//					port = prefs.getInt(MainActivity.PREFS_SERVER_PORT, 
//							MainActivity.DEFAULT_SERVER_PORT);
//					data = act.getAudioThread(server_addr, port,  false);
//				}
//				
//				//if not already recording start recording
//				if(!act.recording) {
//					
//					//start capturing
//					data.startCapture();
//					
//					//make sure the prompt is fresh
//					Handler ui = act.ui_handler;
//					Message msg = ui.obtainMessage();
//					msg.what = MainActivity.UPDATE_PROMPT_TEXT;
//					currentPrompt = new SpannableStringBuilder(currentPrompt.toString());
//					msg.obj = currentPrompt;
//					msg.arg2 = frag_id;
//					ui.sendMessage(msg);
//				} else
//					return true;
//				
//				/*
//				 * return true if we want the normal click to NOT execute on release
//				 * return false if we want the normal click TO execute on release
//				 */
//				return false;
//			}
//		});
//		
//		/*
//		 * skip listener
//		 */
//		skipBtn.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				Log.i(TAG, "skip button clicked");
//				
//				//get the activity
//				MainActivity act = (MainActivity)getActivity();
//				
//				SharedPreferences prefs = act.getSharedPreferences(MainActivity.PREFS_NAME, 
//						MainActivity.MODE_PRIVATE);
//				
//				String server_addr = prefs.getString(MainActivity.PREFS_SERVER_IP, 
//						MainActivity.DEFAULT_SERVER_ADDR);
//				
//				//get the right data thread to use
//				AudioThread data;
//				int port;
//				if (frag_id == MainActivity.FREESTYLE_CN_FRAG_ID) {
//					port = prefs.getInt(MainActivity.PREFS_SERVER_PORT_CN, 
//							MainActivity.DEFAULT_SERVER_PORT_CN);
//					data = act.getAudioThread(server_addr, port,  false);
//				} else {
//					port = prefs.getInt(MainActivity.PREFS_SERVER_PORT, 
//							MainActivity.DEFAULT_SERVER_PORT);
//					data = act.getAudioThread(server_addr, port, 
//							false);
//				}
//				
//				//kill any recordings that are ongoing
//				if(act.recording) {
//					data.stopCapture();
//				}
//				
//				//change the current prompt
//				Handler ui = act.ui_handler;
//				Message msg = ui.obtainMessage();
//				msg.what = MainActivity.UPDATE_PROMPT_TEXT;
//				msg.obj = new SpannableStringBuilder(PromptFactory.getRandomPrompt());
//				msg.arg2 = frag_id;
//				ui.sendMessage(msg);
//				
//				//wipe any response text out
//				Message msg1 = ui.obtainMessage();
//				msg1.what = MainActivity.UPDATE_RESPONSE_TEXT;
//				msg1.obj = "";
//				msg1.arg2 = frag_id;
//				ui.sendMessage(msg1);
//			}
//		});
//	}
//}