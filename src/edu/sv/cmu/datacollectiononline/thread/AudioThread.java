//package edu.sv.cmu.datacollectiononline.thread;
package edu.sv.cmu.datacollectiononline.thread;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.sv.cmu.datacollectiononline.MainActivity;
import edu.sv.cmu.datacollectiononline.R;
import edu.sv.cmu.datacollectiononline.R.drawable;
import edu.sv.cmu.datacollectiononline.R.id;
import edu.sv.cmu.datacollectiononline.util.Energy;
import edu.sv.cmu.datacollectiononline.util.ZeroCross;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioSource;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;

/**
 * This thread constantly listens to mic input in a small (~1-2s) buffer
 * then proceeds to store all speech when triggered until told to stop.
 * 
 * @author Hans Reichenbach
 */
public class AudioThread extends Thread {
	/*
	 * constants
	 */
	private final String TAG = "AudioThread";

	//audio constants
	public final static int BUFFER_SIZE = 256; //currently just arbitrarily copied from sample code
	public final int BUFFER_MULTIPLE = 10; //how much bigger than the min buffer size we want recorder to use
	public final int BUFFER_TIME = 1000; //how much time we should buffer before button press in millis
	public final int SAMPLE_RATE = 16000; // in hz
	public final int CHANNEL_ENCODING = AudioFormat.CHANNEL_IN_MONO;
	public final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	public final static int ZERO_THRESH = 60;
	private final int ZC_FRAME_THRESH = 10;
	private final int ENERGY_THRESH = 300;//100;
	private final int E_FRAME_THRESH = 75;

	public final int RUNNING_FRAME_NUMBER = 62;
	/* Dynamic Threshold for testing */
	public volatile int threshold_testing = 300;
	public int zc_threshold_testing = 10;
	/*
	 * Symbols
	 */
	public final static int start_recording_from_audio = 2 + MainActivity.AUDIOTHREAD;
	public final static int stop_recording_from_audio = 3 + MainActivity.AUDIOTHREAD;
	public final static int enable_auto_start = 4 + MainActivity.AUDIOTHREAD;
	public final static int disable_auto_start = 5 + MainActivity.AUDIOTHREAD;
	public final static int ALARM = 6 + MainActivity.AUDIOTHREAD;
	public final static int RESET = 7 + MainActivity.AUDIOTHREAD;

	/* states: 
	 * runningL  the audio_threading working
	 * capturing: begin to capture and send data;
	 * auto_start: automatically start capturing */
	private volatile boolean running = true;
	public volatile boolean capturing = false;
	private volatile boolean auto_start = false;

	/*
	 * globals
	 */

	
	private int packet_size = BUFFER_SIZE*4; // keep it small so there's a high packet send rate
	//private OutThread t_out;
	//public WriteThread ctx.t_write;
	private MainActivity ctx;
	private short dc_offset;

	/* Energy Params Classes */	
	Energy bg_energy = new Energy();
	Energy speech_energy = new Energy();

	/* Zero Crossing*/
	ZeroCross bg_zc = new ZeroCross(zc_threshold_testing,50);
	ZeroCross speech_zc = new ZeroCross(zc_threshold_testing,50);
	private LinkedList<Float> runningAvg = null;
	private int rAvg = 0;

	/* Handler */
	public Handler audio_handler = new Handler() {
		@Override
		public void handleMessage(Message msg){
			if (msg.what == start_recording_from_audio){
				capturing = true;
				ctx.ui_handler.sendMessage(ctx.ui_handler.obtainMessage(start_recording_from_audio));
			}
			if (msg.what == stop_recording_from_audio){
				capturing = false;
				ctx.ui_handler.sendMessage(ctx.ui_handler.obtainMessage(stop_recording_from_audio));
			}
			if (msg.what == enable_auto_start){
				auto_start = true;
			}
			if (msg.what == disable_auto_start){
				auto_start = false;
			}
			if(msg.what == ALARM){
				//sendMessageToUI(ctx.update_running_average,null,rAvg,0);
			}
			if(msg.what == RESET){
				bg_energy.reset();
				speech_energy.reset();
				bg_zc.reset();
				speech_zc.reset();
				runningAvg = new LinkedList<Float>();
				sendMessageToUI(MainActivity.update_energy_params_speech,speech_energy,0,0);
				sendMessageToUI(MainActivity.update_energy_params_background,bg_energy,0,0);
				sendMessageToUI(MainActivity.update_running_average,null,(int) rAvg, 0);
				

			}

		}
	};

	/**
	 * constructor
	 * 
	 * @param packetSize Maximum size of audio packets to send to the server. Smaller packets means faster send rates.
	 * Packet sizes < 1 use the default packet size.
	 * @param ctx Activity context this is being run in
	 */
	public AudioThread(int packetSize, MainActivity ctx) {

		//check for a custom packet size
		if(packetSize > 0) packet_size = packetSize;

		//grab the necessary variables
		this.ctx = ctx;
		this.auto_start = false;//ctx.getSharedPreferences(ctx.PREFS_NAME, ctx.MODE_PRIVATE).getBoolean(ctx.PREFS_AUTOSTART, false);
	}

	@Override
	public void run() {
		Log.i(TAG, "starting main portion of audio thread");

		/*
		 * set up the audio stuff
		 */

		//set up the buffers and other variables
		AudioRecord recorder = null;
		List<short[]> runningBuffer = new LinkedList<short[]>();
		long bufferStartTime;

		//initialize the recorder
		int minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_ENCODING, AUDIO_ENCODING);
		recorder = new AudioRecord(AudioSource.VOICE_RECOGNITION, SAMPLE_RATE,
				CHANNEL_ENCODING, AUDIO_ENCODING, 
				minBufSize*BUFFER_MULTIPLE);

		recorder.startRecording();
		Log.v(TAG,"recorder starts recording");
		/*
		 * outer loop keeps a running 1 second buffer to account for people talking
		 * before they actually hit the record button but still expecting that to
		 * be heard.
		 * 
		 * inner loop keeps a full record of the audio stream until it's told to stop,
		 * which then allows the outer loop to take back over and maintain a new 1 sec
		 * buffer.
		 */
		bufferStartTime = System.currentTimeMillis();
		short[] buffer;
		dc_offset = 0;
		int runningSum = 0;
		int zcCountSum = 0;
		int zcCountSize = 50;
		int energyThreshCount = 0;
		int startEnergyThreshCount = 0;
		ArrayList<Short> dcQueue = new ArrayList<Short>(packet_size);
		ArrayList<Integer> counterQueue = new ArrayList<Integer>(zcCountSize);
		SharedPreferences prefs = 
				ctx.getSharedPreferences(MainActivity.PREFS_NAME, MainActivity.MODE_PRIVATE);

		//alarmRunningAvg();

		while(running) {
			//make sure the recorder is still running
			if(recorder.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
				recorder.startRecording();
			}

			//get a fresh buffer because you'll overwrite the other buffers in the list otherwise
			buffer = new short[BUFFER_SIZE];

			//get the audio segment from the recorder
			int bytes_recorded = recorder.read(buffer, 0, buffer.length);

			//check the bytes recorded to see if the recorder crashed (it tends to do this)
			if(bytes_recorded != buffer.length) {
				Log.e(TAG, "error, didn't fill a buffer section. Only " + bytes_recorded + " bytes recorded");
				//Log.v(TAG, "restarting recorder");

				//make sure the old recorder is fully closed out
				recorder.stop();
				recorder.release();

				//set up a new recorder and start it
				recorder = new AudioRecord(AudioSource.VOICE_RECOGNITION, SAMPLE_RATE,
						CHANNEL_ENCODING, AUDIO_ENCODING, 
						minBufSize*BUFFER_MULTIPLE);
				recorder.startRecording();

				continue;
			}



			//update the buffer with the value
			if(System.currentTimeMillis() - bufferStartTime > BUFFER_TIME && !runningBuffer.isEmpty()) {
				/*
				 * past the buffer time so buffer should be as big as we need it
				 * so we'll drop the first element then add the new values to maintain size
				 */
				runningBuffer.remove(0);
				runningBuffer.add(buffer);


			} else {
				//not at proper buffer length yet so just add to end
				runningBuffer.add(buffer);
			}
			/* Calculate ZeroCrossing */
			bg_zc.update(buffer, packet_size);
			//sendMessageToUI(MainActivity.update_zc_params_background,bg_zc,0, 0);

			/* Updating global background energy*/
			double frame_energy = energyCalculation(buffer);
			bg_energy.update(frame_energy);		

			/* update UI */
			rAvg = (int) updateRunningAvg(runningAvg,(float) frame_energy);
			sendMessageToUI(MainActivity.update_energy_params_background,bg_energy,rAvg,0);
			//
			/* update running avg */
			
			//Log.d("frequency",""+zcCountSum);
						//Log.e("BACKGROUND_ENERGY",bg_energy_sum+":"+bg_energy_count+":"+bg_average_energy);


			/* only begin to background*/
			if(!capturing && ((auto_start && ctx.getSharedPreferences(ctx.PREFS_NAME, ctx.MODE_PRIVATE).getBoolean(ctx.PREFS_AUTOSTART, false))))
			{checkToStart(buffer, dcQueue, runningSum, zcCountSum, 
					zcCountSize, startEnergyThreshCount, counterQueue);
			}
			/*
			 * nest the while inside an if so that we can perform a few actions
			 * before/after the capture loop without doing it while the 1 sec buffer
			 * loop is executing and skipping the capture loop
			 */
			if(capturing) {

				Log.d(TAG,"BEGIN CAPTURING");
				// update UI
				//ctx.ui_handler.sendMessage(ctx.ui_handler.obtainMessage(start_recording));
				//if was provided a means of transmission then set up networking data
				short[] audio;
				int audio_rem;
				//if(t_out != null) {

				if(ctx.t_write != null){
					//set up an initial audio packet
					audio = new short[packet_size];
					audio_rem = packet_size;
				} else {
					/*
					 * this should never trigger when the packet needs to be used,
					 * I'm just putting it in here to make the compiler shut up about
					 * uninitialized variables.
					 */
					audio = new short[1];
					audio_rem = 0;
				}

				int frameTot = 0;
				while(capturing) {
					//refresh the buffer
					buffer = new short[BUFFER_SIZE];

					//get the current reading from the recorder
					bytes_recorded = recorder.read(buffer, 0, buffer.length);

					//check the bytes recorded to see if the recorder crashed (it tends to do that)
					if(bytes_recorded != buffer.length) {
						//make sure the old recorder is fully closed out
						recorder.stop();
						recorder.release();

						//set up a new recorder and start it
						recorder = new AudioRecord(AudioSource.VOICE_RECOGNITION, SAMPLE_RATE,
								CHANNEL_ENCODING, AUDIO_ENCODING, 
								minBufSize*BUFFER_MULTIPLE);
						recorder.startRecording();

						Log.e(TAG, "error while recording, didn't fill a buffer section. Only " + 
								bytes_recorded + " bytes recorded");
						continue;
					}

					//if(prefs.getBoolean(MainActivity.PREFS_AUTOSTOP, 
					//	MainActivity.DEFAULT_AUTOSTOP_STATE)) {
					if(true || prefs.getBoolean(MainActivity.PREFS_AUTOSTOP, MainActivity.DEFAULT_AUTOSTOP_STATE)) {

						/* Calculate ZeroCrossing */
						speech_zc.update(buffer, packet_size);
						//sendMessageToUI(MainActivity.update_zc_params_speech,speech_zc,0, 0);

						/* Calculate Energy */
						double energy = energyCalculation(buffer);
						speech_energy.update(energy);
						
						/* update running Avg */
						rAvg = (int) updateRunningAvg(runningAvg,(float) energy);
						sendMessageToUI(MainActivity.update_energy_params_speech,speech_energy, rAvg, 0);
						//check if the energy was below our speaking threshold
						//if(energy < ENERGY_THRESH-average_energy) {
						if(energy < threshold_testing) {
							energyThreshCount++;
						} else {
							energyThreshCount = 0;
						}

						if (prefs.getBoolean(MainActivity.PREFS_AUTOSTOP, MainActivity.DEFAULT_AUTOSTOP_STATE) && zcCountSum < ZC_FRAME_THRESH && energyThreshCount > E_FRAME_THRESH) {
							Log.d(TAG, "PAUSE!!");

							stopCapture();

							//reset the counters
							energyThreshCount = 0;
							zcCountSum = 0;
						} else {
							//Log.v(TAG, "zero-crossing count = " + zcCount);
							//Log.v(TAG, "zero-crossing count sum = " + zcCountSum);
							//Log.v(TAG, "energy count = " + energyThreshCount);
						}
					}

					//add the current buffer to the capture
					runningBuffer.add(buffer);

					if(frameTot + buffer.length > packet_size) {
						frameTot = buffer.length;
					} else {
						frameTot += buffer.length;
					}

					//if the user provided a means of transmission then send off data as we go
					if(ctx.t_write != null) {
						//send the audio chunk off to the server
						if(audio_rem >= buffer.length) {
							//if there's more space left than the buffer size then add it to the packet
							for(int i = 0; i < buffer.length; i++) {
								audio[audio.length - audio_rem] = buffer[i];
								audio_rem--;
							}
						} else {
							//less than the full buffer of space left in the packet so trim the excess and send it

							//trim the packet down to size
							short[] a = new short[audio.length - audio_rem];
							for(int i = 0; i < a.length; i++)
								a[i] = audio[i];

							//convert the short array to a byte array (little endian because Kaldi is weird like that)
							byte[] audioBytes = new byte[a.length*2];
							ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(a);

							//send the audio data
							sendAudio(audioBytes);

							//reset the packet
							audio_rem = packet_size;

							//add the buffer to the new packet
							for(int i = 0; i < buffer.length; i++) {
								audio[audio.length - audio_rem] = buffer[i];
								audio_rem--;
							}
						}
					}
				}

				/*
				 * done capturing
				 */

				//check if we had a partial packet made
				if(audio_rem > 0 && audio_rem < packet_size && ctx.t_write != null) {
					//trim the packet down to size
					short[] a = new short[audio.length - audio_rem];
					for(int i = 0; i < a.length; i++)
						a[i] = audio[i];

					//convert the short array to a byte array (little endian because Kaldi is weird like that)
					byte[] audioBytes = new byte[a.length*2];
					ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(a);

					//send the audio data
					sendAudio(audioBytes);
				}

				if(ctx.t_write != null) {
					//indicate end of the stream to Kaldi
					byte[] end = new byte[4];
					ByteBuffer.wrap(end).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(0);
					// send the context package right before the END package 
					ctx.t_write.addToList(ctx.t_write.getContextPackage());
					// send END package
					ctx.t_write.addToList(end);
					// send context package 
					ctx.t_write.addToList(ctx.t_write.getContextPackage());
					
					Log.d(TAG,"END of Capturing");
					
				}
				else{
					Log.e(TAG,"The ending package header of audio data is not sent");
				}
				//clear out the running buffer
				runningBuffer.clear();

				//reset the buffer start time
				bufferStartTime = System.currentTimeMillis();
				
				// update UI
				//ctx.ui_handler.sendMessage(ctx.ui_handler.obtainMessage(stop_recording));
			}
		}

		/*
		 * after run loop ends
		 */

		//clean up the audio recorder
		recorder.stop();
		recorder.release();
		recorder = null;

		Log.i(TAG, "stopped data thread");

	}

	/**
	 * start capturing audio
	 */
	public void startCapture() {
		Log.i(TAG, "starting capture");

		//allow the capturing loop to trigger
		capturing = true;

		//turn the record button red
		ctx.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				//get the button
				View fragRoot = ctx.currentFragment.getView();
				Button recordBtn = (Button)fragRoot.findViewById(R.id.record_button);

				//update the recording state of the main activity
				ctx.recording_status = true;

				//change the background
				Drawable alt = ctx.getResources().getDrawable(
						R.drawable.button_on_bg);
				recordBtn.setBackgroundDrawable(alt); //for API lvl 15-
				//recordBtn.setBackground(alt); //for API lvl 16+
			}
		});
	}

	/**
	 * stop capturing audio
	 */
	public void stopCapture() {
		Log.i(TAG, "stopping capture");
		/* Use Alarm to stop the recording after 500 ms */
		Runnable alarm = new Runnable(){

			@Override
			public void run() {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				audio_handler.sendMessage(audio_handler.obtainMessage(stop_recording_from_audio));
			}

		};
		alarm.run();


		//stop the capturing loop
		capturing = false;
	}
	
	
	/**
	 * gracefully stop the thread
	 */
	public void kill() {
		capturing = false;
		Log.i(TAG, "stopping capture");

		running = false;
	}

	/**
	 * update the connection that the data thread sends information over
	 * 
	 * @param out the outbound thread to the server to send audio stream on
	 * @param mongo_addr the address of the mongo server to store data in
	 */
	public synchronized void updateTarget(WriteThread write) {

		// update the WriteThread to the current Thread
		ctx.t_write = write;
		Log.d(TAG,"Get SocketThread");
	}

	/**
	 * context data collection methods
	 */

	/**
	 * converts a list of short[] into a wav file
	 * 
	 * @param buffer the list of short arrays
	 * @return a byte[] containing the bytes of the audio stream in proper wav file format
	 */
	private byte[] shortListToWav(List<short[]> buffer) {
		//convert some strings to bytes for the file format
		byte[] s_riff = "RIFF".getBytes();
		byte[] s_wave = "WAVE".getBytes();
		byte[] s_fmt = "fmt ".getBytes();
		byte[] s_data = "data".getBytes();

		//calculate file size in bytes
		int filesize = 28 + s_riff.length + s_wave.length + s_fmt.length + s_data.length
				+ buffer.size()*2*BUFFER_SIZE;

		//make a new byte buffer to play with
		ByteBuffer buf = ByteBuffer.allocate(filesize);

		/*
		 * build the header
		 */
		buf.put(s_riff);
		buf.putInt(Integer.reverseBytes(filesize)); //final file size
		buf.put(s_wave);
		buf.put(s_fmt);
		buf.putInt(Integer.reverseBytes(16)); //sub-chunk size is 16 for PCM
		buf.putShort(Short.reverseBytes((short)1)); //audio format 1 for PCM
		buf.putShort(Short.reverseBytes((short)1)); //num of channels. hardcoded to 1 for mono for now
		buf.putInt(Integer.reverseBytes(SAMPLE_RATE)); //sample rate
		buf.putInt(Integer.reverseBytes(
				SAMPLE_RATE*1*16/8)); //byte rate; sampleRate*numChannels*bitsPerSample/8
		buf.putShort(Short.reverseBytes((short)(1*16/8))); //block align; numChannels*bitsPerSample/8
		buf.putShort(Short.reverseBytes((short)16)); //bits per sample, we're doing 16
		buf.put(s_data);
		buf.putInt(Integer.reverseBytes(buffer.size()*2*BUFFER_SIZE)); //data chunk size

		//put the audio shorts into the byte buffer
		for(int i = 0; i < buffer.size(); i++) {
			//have to loop through the buffer at each location too
			for(int j = 0; j < BUFFER_SIZE; j++) {
				buf.putShort(Short.reverseBytes(buffer.get(i)[j]));
			}
		}

		return buf.array();
	}


	/**
	 * send the audio waveform to the server to be processed
	 * 
	 * @param wav a byte array with the raw audio data
	 */
	private void sendAudio(byte[] wav) {
		//check if we still have a socket
		if(ctx.t_write == null)
			return;

		/*
		 * make the packet header
		 */
		int header_size = 4; //file size only so 4 bytes (file size is int)

		//make a packet of the right size
		byte[] packet = new byte[wav.length + header_size];

		//get the data length bytes (little endian because Kaldi is weird like that)
		byte[] d_len_bytes;
		d_len_bytes = new byte[4];
		ByteBuffer.wrap(d_len_bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(wav.length);

		/*
		 * put the header into the packet.
		 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		 * MAKE SURE TO CHANGE THIS IF YOU EVER CHANGE THE HEADER!!!!!!!
		 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		 */
		for(int i = 0; i < d_len_bytes.length; i++)
			packet[i] = d_len_bytes[i];

		//put the data in the packet
		for(int i = header_size; i < packet.length; i++)
			packet[i] = wav[i-header_size];

		//send the packet
		ctx.t_write.addToList(packet);
	}
	private void checkToStart(short[] buffer, ArrayList<Short> dcQueue, int runningSum, int zcCountSum, 
			int zcCountSize, int startEnergyThreshCount, ArrayList<Integer> counterQueue  ){

		for(int i = 0; i < buffer.length; i++) {
			if(dcQueue.size() < packet_size) {
				dcQueue.add(buffer[i]);

				runningSum += buffer[i];
			} else {
				short sub = dcQueue.remove(0);
				dcQueue.add(buffer[i]);

				runningSum = runningSum - sub + buffer[i];
			}
		}

		dc_offset = (short) (runningSum / packet_size);

		int zcCount = 0;
		int sgn_old = (buffer[0] > dc_offset) ? 1 : -1;
		for (int i=1; i < BUFFER_SIZE; ++i) {
			int sgn =  (buffer[i] > dc_offset) ? 1 : -1;
			if ((sgn_old - sgn) != 0) {
				//Log.v(TAG, "ZERO CROSSED");
				zcCount++;
			}
			sgn_old = sgn;				
		}


		if(counterQueue.size() < zcCountSize) {
			counterQueue.add(zcCount);
			int add = (zcCount > ZERO_THRESH) ? 1 : 0;
			zcCountSum+=add;
		} else {
			counterQueue.add(zcCount);
			int sub = (counterQueue.remove(0) > ZERO_THRESH) ? 1 : 0;
			int add = (zcCount > ZERO_THRESH) ? 1 : 0;
			zcCountSum = zcCountSum - sub + add;
		}
		//Log.d(TAG,"zzCountSum:"+ zcCountSum);
		/*
		 * energy level calculations
		 */
		double energy = energyCalculation(buffer);

		Log.v(TAG,"not capturing, energy for this frame " + energy);

		


		//check if the energy was below our speaking threshold
		//if(energy > ENERGY_THRESH) {
		if(energy > threshold_testing) {
			startEnergyThreshCount++;
		}
		//	} else {
		//		startEnergyThreshCount = 0;
		//	}
		//Log.d(TAG,""+startEnergyThreshCount);
		if ((zcCountSum > ZC_FRAME_THRESH || startEnergyThreshCount > 0)) {
			Log.d(TAG, "START AUTO!!");

			ctx.ui_handler.sendMessage(ctx.ui_handler.obtainMessage(start_recording_from_audio));
			capturing = true;

			//reset the counters
			startEnergyThreshCount = 0;
			zcCountSum = 0;
		} 
	}

	/* Helper functions */
	private double energyCalculation(short[] buffer){
		double energy = 0.0;
		for(int i = 0; i < buffer.length; i++) {

			energy += buffer[i] * buffer[i];
		}
		double energy_avg = Math.sqrt(energy / buffer.length);
		return energy_avg;
	}

	private float updateRunningAvg(LinkedList<Float> runningAvg, float frame_energy){
		if(runningAvg == null)
			runningAvg = new LinkedList<Float>();
		runningAvg.add(frame_energy);

		if(runningAvg.size() > RUNNING_FRAME_NUMBER)
			runningAvg.remove();
		float s = 0;

		Iterator<Float> it = runningAvg.iterator();
		while(it.hasNext()){
			s+=it.next();
		}
		//Log.d(TAG,"updating Running Average");
		return s/runningAvg.size();
	}
	private void sendMessageToUI(int what, Object obj, int arg1, int arg2){
		Message msg = ctx.ui_handler.obtainMessage();
		msg.what = what;
		msg.obj = obj;
		msg.arg1 = arg1;
		msg.arg2 = arg2;
		ctx.ui_handler.sendMessage(msg);
	}
}

//	public void alarmRunningAvg(){
//		new Alarm(this).start();
//	}}	
//

