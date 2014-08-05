package edu.sv.cmu.datacollectiononline.helper;

import android.util.Log;
import edu.sv.cmu.datacollectiononline.MainActivity;
import edu.sv.cmu.datacollectiononline.thread.AudioThread;

public class Alarm extends Thread{
	private MainActivity ctx;
	public Alarm(MainActivity ctx){
		this.ctx = ctx;
	}
	@Override
	public void run(){
		try {
			Log.e("ALARM","ALARM started" );
			sleep(500);
			ctx.ui_handler.sendEmptyMessage(AudioThread.stop_recording_from_audio);
		
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
}
