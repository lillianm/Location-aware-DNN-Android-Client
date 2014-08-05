package edu.sv.cmu.datacollectiononline.thread;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import edu.sv.cmu.datacollectiononline.MainActivity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class WriteThread extends Thread{

	private final int cp_header_size = 4;
	/* running indicates whether the socket is running */
	public volatile boolean running;
	public volatile boolean initialized;
	public volatile boolean recording;
	//	static public int PACKET_SIZE = 128;
	//	static public int BUFFER_SIZE = 10;
	static int TIMEOUT = 300;
	static String TAG = "WriteThread";
	/* 0 indicates the open socket using button - connect to server
	 * 1 indicates reopen with each sentence */
	private int mode = 0;

	private Socket socket;
	private String serverAddr;
	private int serverPort;

	private MainActivity ctx;
	private OutputStream out;
	private Handler ui_handler;
	private List<byte[]> buffer;
	

	public WriteThread(MainActivity ctx, List<byte[]> buf, String serverAddr, int serverPort, Handler handler){
		this.ctx = ctx;
		this.buffer = buf;
		this.serverAddr = ctx.getSharedPreferences(MainActivity.PREFS_NAME, MainActivity.MODE_PRIVATE)
				.getString(MainActivity.PREFS_SERVER_IP, serverAddr);
		this.serverPort = ctx.getSharedPreferences(MainActivity.PREFS_NAME, MainActivity.MODE_PRIVATE)
				.getInt(MainActivity.PREFS_SERVER_PORT, serverPort);
		this.ui_handler = handler;
		//this.mode = mode;
		/* Initialize buffer */
		running = false;
		buffer = Collections.synchronizedList(new LinkedList<byte[]>());
	}

	/* Send Data from buffer to the Server */
	@Override
	public void run(){

		try{

			socket = new Socket();
			socket.connect(new InetSocketAddress(serverAddr, serverPort), TIMEOUT);
			Log.d(TAG,serverAddr+serverPort);
			out = socket.getOutputStream();

			/* start the polling thread , listening to server*/
			ctx.t_poll = new PollThread(socket, serverAddr, serverPort, ctx);
			ctx.t_poll.start();

			running = true;
			/* If sent to check the socket status */
			if(ctx.socket_status == false){
				Message msg = ui_handler.obtainMessage(MainActivity.socket_success);
				ui_handler.sendMessage(msg);
			}

			//showToastInActivity(ctx,"start recording");
			/* successfully connected to server*/
			
			if(running == true ){
				/* begin sending data */
				/* send context data before audio data, context header = 4 Bytes */
				String jsonString = ctx.t_context.cp.returnJSONString();
				
				Log.d(TAG,jsonString);
				byte[] context_header = new byte[cp_header_size];
				byte[] context_packet = new byte[cp_header_size + jsonString.length()];
				
				ByteBuffer.wrap(context_header).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(0xffff0000+jsonString.length());
				
				for(int i=0;i<context_header.length;i++){
					context_packet[i] = context_header[i];
				}
				for(int i=0;i<jsonString.length();i++){
					context_packet[i+4] = (byte) jsonString.charAt(i); 
				}
				
				Log.d(TAG,"context_packet sent"+context_packet.length);
				
				out.write(context_packet);
				out.flush();
				
				byte[] packet;
				while(running) {
					if(!buffer.isEmpty()) {

						try {
							//remove the next packet from the queue and send it
							packet = buffer.remove(0);
							out.write(packet);
							out.flush();
							Log.d(TAG, "packet_sent"+packet.length);
						} catch(IOException e) {
							Log.e(TAG, "failed to transmit packet. " + e.getMessage());
							return;
						}
					}

					//pause a smidge so we don't overload the cpu
					try {
						synchronized(this) {
							this.wait(1);
						}
					} catch (InterruptedException e) {
						Log.e(TAG, "queue loop wait interrupted: " + e.getMessage());
						break;
					}
				}

				Log.d(TAG,"send Context Package at the end");
				
				/* end sending data, update UI */
				//showToastInActivity(ctx, "Sending data to server done");

			}

		} catch (UnknownHostException e) {
			Log.e(TAG, "unable to find the server");
			recording = false;
			running = false;
			ui_handler.sendMessage(ui_handler.obtainMessage(MainActivity.socket_fail, "Unable to find server"));

			return;

		} catch (IOException e) {
			Log.e(TAG, "error setting up the connection with the server: " + e.getMessage());
			recording = false;
			running = false;
			ui_handler.sendMessage(ui_handler.obtainMessage(MainActivity.socket_fail, "Unable to connect to server" + e.getMessage()));
			return;
		}

	}

	/* Stop sending packages and wait for pollThread to close the socket */
	public void kill(){
		running =  false;

	}
	protected void addToList(byte[] new_pack) {
		buffer.add(new_pack);
	}	
	
	/* Show Connection Information in Main Activity */
	private void showToastInActivity(final MainActivity ctx, final String message){
		ctx.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
			}

		});
	}
	
	/* get JSON format of context package */
	
	public byte[] getContextPackage(){
		String jsonString = ctx.t_context.cp.returnJSONString();
		
		Log.d(TAG,jsonString);
		byte[] context_header = new byte[cp_header_size];
		byte[] context_packet = new byte[cp_header_size + jsonString.length()];
		
		ByteBuffer.wrap(context_header).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(0xffff0000+jsonString.length());
		
		for(int i=0;i<context_header.length;i++){
			context_packet[i] = context_header[i];
		}
		for(int i=0;i<jsonString.length();i++){
			context_packet[i+4] = (byte) jsonString.charAt(i); 
		}
		return context_packet;
	}
	
	

}
