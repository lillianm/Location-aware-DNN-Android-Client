package edu.sv.cmu.datacollectiononline.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import edu.sv.cmu.datacollectiononline.MainActivity;
import edu.sv.cmu.datacollectiononline.R;
import edu.sv.cmu.datacollectiononline.thread.AudioThread;
import edu.sv.cmu.datacollectiononline.thread.WriteThread;
import android.app.Activity;
import android.graphics.Color;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class RecordButtonListener implements View.OnClickListener{
	private MainActivity ctx;
	Button btn_manual_start = null;

	public RecordButtonListener(MainActivity ctx){
		this.ctx = ctx;
	}
	@Override
	public void onClick(View v) {

		btn_manual_start = (Button) v;
		if(ctx.socket_status == false){
			
			btn_manual_start.setEnabled(false);
			btn_manual_start.setText("Click to Connect to Server");
			btn_manual_start.setBackgroundResource(R.drawable.btn_yellow);

			resetConnection();
			ctx.t_write.start();
			ctx.t_write.kill();

			btn_manual_start.setEnabled(true);
		}
		else{
			if(ctx.recording_status == false){
				//Toast.makeText(getApplicationContext(), "connecting to server, please wait", Toast.LENGTH_SHORT).show();

				/* Disable button now*/
				btn_manual_start.setEnabled(false);
				btn_manual_start.setText("Recording");
				btn_manual_start.setBackgroundResource(R.drawable.btn_green);
				


				//ctx.t_write = new WriteThread(ctx,ctx.buffer,MainActivity.DEFAULT_SERVER_ADDR, MainActivity.DEFAULT_SERVER_PORT,ctx.ui_handler);
				/* resetConnection() creates a new WriteThread */
				resetConnection();
				ctx.t_write.start();

				ctx.recording_status = true;
				ctx.t_audio.audio_handler.sendMessage(ctx.t_audio.audio_handler.obtainMessage(AudioThread.start_recording_from_audio));
				btn_manual_start.setEnabled(true);
				//					
			}
			else{
				//Toast.makeText(ctx, "end recording", Toast.LENGTH_SHORT).show();
				btn_manual_start.setEnabled(false);

				btn_manual_start.setText("Click to Start");
				btn_manual_start.setBackgroundResource(R.drawable.btn_red);
				//btn_manual_start.setBackgroundColor(Color.RED);
				ctx.recording_status = false;
				
				//stop capturing and send empty package
				ctx.t_audio.capturing = false;

				ctx.t_audio.audio_handler.sendMessage(ctx.t_audio.audio_handler.obtainMessage(AudioThread.stop_recording_from_audio));
				//ctx.t_context.cp.end_time = System.currentTimeMillis();
				btn_manual_start.setEnabled(true);
			}


		}



	}
	private void resetConnection(){
		if(ctx.t_write!=null && ctx.t_write.isAlive()){
			ctx.t_write.kill();
		}
		if(ctx.t_poll!=null && ctx.t_poll.isAlive()){
			ctx.t_poll.kill();
		}
		ctx.t_write = new WriteThread(ctx,ctx.buffer,MainActivity.DEFAULT_SERVER_ADDR, MainActivity.DEFAULT_SERVER_PORT,ctx.ui_handler); 
	}

}
