package edu.sv.cmu.datacollectiononline.handler;


import edu.sv.cmu.datacollectiononline.MainActivity;
import edu.sv.cmu.datacollectiononline.R;
import edu.sv.cmu.datacollectiononline.helper.Alarm;
import edu.sv.cmu.datacollectiononline.thread.AudioThread;
import edu.sv.cmu.datacollectiononline.util.Energy;
import edu.sv.cmu.datacollectiononline.view.MyTextView;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

public class UIHandler extends Handler {

	private final static String TAG = "UI HANDLER";

	private MainActivity ctx;

	/* whether to jump to the next prompt */
	private boolean[] next = new boolean[1];
	/* repeat cnt */
	private int repeat_cnt = 0;
	/* prompt string */
	private int promptId = 0;
	private String prompt = null;
	private String prev_prompt = null;
	private Alarm alarm = null;
	/* constructor */
	public UIHandler(MainActivity ctx){
		this.ctx = ctx;

	}

	public void handleMessage(Message msg){
		if(ctx.isDestroyed()) return;
		//		EnergyParamsFragment pf;
		//		ZCFragment zf;

		switch(msg.what){
		/* THE APP IS SUCCESSFULLY CONNECTED TO SERVER */
		case MainActivity.socket_success:
			/* update socket */
			ctx.t_audio.updateTarget(ctx.t_write);

			ctx.usr_wrapper.status_socket.setText("Connected to Server");
			ctx.usr_wrapper.status_socket.setImage(R.drawable.socket_connected);
			ctx.socket_status = true;
			ctx.recording_status = false;

			ctx.btn_manual_start.setText("Click to Start");
			ctx.btn_manual_start.setBackgroundResource(R.drawable.btn_red);

			if(ctx.usr_wrapper.ll_response!=null){
				ctx.usr_wrapper.ll_response.removeAllViews();
				
				ctx.txtParser.populatePromptData();

				prompt = ctx.txtParser.getRandomePrompt();
				prev_prompt = prompt;
				promptId = Integer.parseInt(prompt.split(":")[0]);
				prompt = "  "+prompt.split(":")[1];

				MyTextView txtView_response = new MyTextView(ctx, 20, Color.BLACK,prompt);
				ctx.usr_wrapper.ll_response.addView(txtView_response);
				
				/* can be used for offline data collection*/
				ctx.t_context.cp.setPrompt(promptId, prompt);
				ctx.xmlWriter.populateXMLFile(ctx.t_context.cp);
			}


			break;
			
		/* SERVER IS NOT AVAILABLE */
		case MainActivity.socket_fail: 

			ctx.usr_wrapper.status_socket.setText("Unable to Connect to Server");
			ctx.usr_wrapper.status_socket.setImage(R.drawable.socket_disconnected);
			/* block auto_starting */
			ctx.t_audio.audio_handler.sendMessage(ctx.t_audio.audio_handler.obtainMessage(AudioThread.disable_auto_start));
			/* do not switch button */
			ctx.socket_status = false;
			ctx.recording_status = false;

			if(ctx.usr_wrapper.ll_response!=null){
				ctx.usr_wrapper.ll_response.removeAllViews();
			}

			/* show stopped recording if socket failed */
			ctx.usr_wrapper.status_recording.setText("Recording Stopped");

			break;
		/* START RECORDING */ 
		case AudioThread.start_recording_from_audio:

			Log.e(TAG, "start recording");
			ctx.usr_wrapper.status_recording.setText("Recording...");
			ctx.usr_wrapper.status_recording.setImage(R.drawable.recorder_on);
			//ctx.recording_status = true;


			ctx.t_context.cp.setPrompt(promptId, prompt);
			ctx.xmlWriter.populateXMLFile(ctx.t_context.cp);




			break;

		/* STOP RECORDING */
		case AudioThread.stop_recording_from_audio:

			ctx.usr_wrapper.status_recording.setText("Recording Stopped");
			ctx.usr_wrapper.status_recording.setImage(R.drawable.mute);


			//ctx.xmlWriter.modifyElement(XMLWriter.timeStamp, "end_time", Long.toString(System.currentTimeMillis()));
			ctx.xmlWriter.writeToXMLFile(MainActivity.ANDROID_ID+System.currentTimeMillis()+".xml");
			break;

		/* SHOW PREVIOUS PROMPT */
		case MainActivity.prev_prompt:
			if(ctx.usr_wrapper.ll_response!=null){
				ctx.usr_wrapper.ll_response.removeAllViews();
				TextView txtView_response = new TextView(ctx);
				ctx.txtParser.populatePromptData();
				if(prev_prompt!=null){
					txtView_response.setText(prev_prompt);
					repeat_cnt++;
					txtView_response.setTextSize(20);
					ctx.usr_wrapper.ll_response.addView(txtView_response);
				}
			}
			break;

		/* Receive Hypothesis from Server */	
		case MainActivity.update_response_text:

			if(ctx.usr_wrapper.ll_response!=null){
				ctx.usr_wrapper.ll_response.removeAllViews();
				MyTextView mytxtView_response = new MyTextView(ctx,20);

				String response = (String) msg.obj;
				String matchWords = ctx.txtParser.matchWordIndex(prompt, response ,next);
				

				mytxtView_response.setText(Html.fromHtml(matchWords));
				mytxtView_response.setTextSize(20);
				ctx.usr_wrapper.ll_response.addView(mytxtView_response);

			}

			break;
			
		/* SKIP AND JUMP TO THE NEXT PROMPT */
		case MainActivity.next_prompt:
			if(ctx.usr_wrapper.ll_response!=null){
				ctx.usr_wrapper.ll_response.removeAllViews();
				

				if(prompt!=null){
					prev_prompt = prompt;
				}
				prompt = ctx.txtParser.getRandomePrompt().split(":")[1];
				repeat_cnt = 0;
				next[0] = false;
				MyTextView txtView_response = new MyTextView(ctx,20, Color.BLACK,prompt);
				ctx.usr_wrapper.ll_response.addView(txtView_response);

			}
			break;


		/* Close Connection 
		 * UPDATE FINAL RESPONSE
		 * */
		case MainActivity.server_done:
			//stop recording
			if(ctx.t_audio!=null)
				ctx.t_audio.stopCapture();


			String response = (String) msg.obj;
			if(ctx.usr_wrapper.ll_response!=null){	
				ctx.usr_wrapper.ll_response.removeAllViews();
				MyTextView mytxtView_response = null;
				response = response.toLowerCase().trim();
				ctx.txtParser.matchWordIndex(prompt, response ,next); 

				if(response.equals("back")){
					Log.e(TAG,"BACK");
					mytxtView_response = new MyTextView(ctx, 20, Color.BLACK, prev_prompt);
					ctx.t_context.cp.setPrompt(promptId, prompt);
					ctx.xmlWriter.populateXMLFile(ctx.t_context.cp);
					repeat_cnt = 0;
					Toast.makeText(ctx, "BACK Detected", Toast.LENGTH_LONG).show();;
				}
				else{
					if(response.trim().equals("next")){
						next[0] = true;
						repeat_cnt = 0;
					}
					if(next[0] || repeat_cnt > 0){
												prev_prompt = prompt;
						prompt = ctx.txtParser.getRandomePrompt();
						promptId = Integer.parseInt(prompt.split(":")[0]);
						prompt = "  " + prompt.split(":")[1];

						mytxtView_response = new MyTextView(ctx, 20, Color.BLACK, prompt);
						ctx.t_context.cp.setPrompt(promptId, prompt);
						ctx.xmlWriter.populateXMLFile(ctx.t_context.cp);
						repeat_cnt = 0;

					}
					else{
						/* Show repeat message */
						Toast toast = Toast.makeText(ctx,"Not correct, Please Repeat",Toast.LENGTH_SHORT);
						toast.setGravity(Gravity.CENTER_HORIZONTAL,0, 300);
						toast.show();

						mytxtView_response = new MyTextView(ctx, 20, Color.BLACK, prompt);
						repeat_cnt++;
					}
				}


				ctx.usr_wrapper.ll_response.addView(mytxtView_response);
			}



			/* detach socket from audio_thread*/
			if(ctx!=null && ctx.t_audio !=null)
				ctx.t_audio.updateTarget(null);
			break;


		default: break;





		/* Debug mode */
		case MainActivity.update_energy_params_background:
			if(ctx.pf_wrapper!=null){
				final int rAvg = msg.arg1;

				final Energy e = (Energy) msg.obj;
				ctx.runOnUiThread(new Runnable(){

					@Override
					public void run() {
						ctx.pf_wrapper.chart.updateEnergy(e,"bg");
						ctx.pf_wrapper.chart.updateRunningAvg(rAvg);

					}

				});
			}

			break;

		case MainActivity.update_energy_params_speech:


			final int rrAvg = msg.arg1;
			final Energy ee = (Energy) msg.obj;
			ctx.runOnUiThread(new Runnable(){

				@Override
				public void run() {
					ctx.pf_wrapper.chart.updateEnergy(ee,"speech");
					ctx.pf_wrapper.chart.updateRunningAvg(rrAvg);

				}

			});


			break;
		}
	}
}

