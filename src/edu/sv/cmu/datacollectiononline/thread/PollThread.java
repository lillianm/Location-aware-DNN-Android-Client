package edu.sv.cmu.datacollectiononline.thread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Locale;

import edu.sv.cmu.datacollectiononline.MainActivity;
import android.content.SharedPreferences;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class PollThread extends Thread {
	/*
	 * constants
	 */
	private final String TAG = "PollThread"+this.getId();
	//public static String KALDI_ENCODING = "CP1250";
	protected static String SERVER_ENCODING = "utf-8";

	enum OutputFormat {
		WORDS, WORDS_ALIGNED
	}

	/*
	 * globals
	 */
	private volatile boolean running = true;
	private BufferedReader in = null;
	private MainActivity ctx;
	private volatile int frag_id = 0;
	private String serverAddr;
	private int serverPort;
	private Socket socket;
	/**
	 * constructor for the thread
	 */
	public PollThread(Socket serv, String addr, int port, MainActivity ctx) {
		/*
		 * set the thread priority high so other threads (like networking
		 * thread) don't get in our way
		 */
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);

		Log.i(TAG, "starting polling thread");

		//showToastInActivity(ctx,"starting to connecting to server");

		serverAddr = addr;
		serverPort = port;
		this.ctx = ctx;
		this.socket = serv;

		//get an input stream from the socket
		try {
			in = new BufferedReader(new InputStreamReader(serv.getInputStream(), SERVER_ENCODING));
		} catch (IOException e) {
			Log.e(TAG, "unable to establish input stream from server: " + e.getMessage());
			return;	
		}
	}

	/**
	 * gracefully stop the thread
	 * be responsible to close the socket
	 * kill WriteThread if it's alive
	 */
	public void kill() {
		running = false;

		Log.i(TAG, "killing poll thread");
		if(ctx.t_write!=null && ctx.t_write.isAlive()){
			ctx.t_write.kill();
		}
		/* the socket should be closed right after receiving SERVER DONE 
		 * close the socket if unexpectely terminated */
		
		if(!socket.isClosed()){
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG,"Unable to close the socket");
			}
		}
	}

	@Override
	public void run() {

		//check if initialization was successful
		if(ctx.ui_handler == null)
			return;
		
		String prev_response = "";
		String response = "";
		while(running && in != null) {

			//poll the socket for incoming strings
			try {
				if(in.ready()) {
					try {
						response = in.readLine();
						prev_response = response;
						Log.e(TAG,"response :"+response);
					} catch (IOException e) {
						Log.e(TAG, "error reading in line from server: " + e.getMessage());

						continue;
					}
				} else {
					//if the socket isn't ready to read from pause a bit and try again
					try {
						synchronized(this) {
							wait(1);
						}
					} catch(InterruptedException e) {
						Log.e(TAG, "wait cycle interrupted");
					}

					continue;
				}

				//check if transmit is dead now (would happen here if someone else closed socket)
				if(in == null)
					break;

				//check the response string
				if(response == null) {
					Log.e(TAG, "response was null");
				} else if(response.equals("")) {
					Log.e(TAG, "nothing received from server");
				} else {
					if(response.startsWith("@")){
						response = response.replace("@","");
					}
					if(response.toLowerCase().equals("back")){
						Message msg = ctx.ui_handler.obtainMessage();
						msg.what = MainActivity.prev_prompt;
						ctx.ui_handler.sendMessage(msg);
						break;
					}
					/*
					 * parse the string from Kaldi format
					 */

					//check the header for errors and completion
					String reco_words = "";
					if (!response.startsWith("RESULT:"))
						Log.e(TAG, "Improper header to start server response");

					if (response.substring(7).equals("DONE")) {
						Log.e(TAG, "SERVER DONE");

						//send done message back to UI thread to kill connection/recording
						Message msg = ctx.ui_handler.obtainMessage();
						msg.what = MainActivity.server_done;
						msg.obj = prev_response;
						msg.arg1 = serverPort;
						msg.arg2 = frag_id;
						
						ctx.ui_handler.sendMessage(msg);
						
						/* close the socket after receiving SERVER DONE */
						if(!socket.isClosed()){
							try {
								socket.close();
								Log.d(TAG,"socket is closed by audio thread");
							} catch (IOException e) {
								e.printStackTrace();
								Log.e(TAG,"Unable to close the socket");
							}
						}
						
					} else {

						//split the response up into its individual fields
						String params[] = response.substring(7).split(",");

						//set up the initial values of the response fields
						int num = 0;
						float reco_dur = 0, input_dur = 0;
						OutputFormat format = OutputFormat.WORDS;

						//loop through the data fields
						for (String param : params) {
							//split the field name from its value
							String ptok[] = param.split("=");

							//scan for recognized field names
							if (ptok[0].equals("NUM"))
								//number of recognized words
								num = Integer.parseInt(ptok[1]);
							else if (ptok[0].equals("RECO-DUR"))
								//how long it took to process the words?
								reco_dur = Float.parseFloat(ptok[1]);
							else if (ptok[0].equals("INPUT-DUR"))
								//how long the audio sample this is for was?
								input_dur = Float.parseFloat(ptok[1]);
							else if (ptok[0].equals("FORMAT")) {
								//indicate formatting of the words in the response
								if (ptok[1].equals("WSE"))
									//following words are aligned
									format = OutputFormat.WORDS_ALIGNED;
								else if (ptok[1].equals("W"))
									//following words are not aligned?
									format = OutputFormat.WORDS;
								else
									Log.e(TAG, "Output format not supported");
							} else
								Log.w(TAG, "WARNING: unknown parameter in header: " + ptok[0]);
						}

						//keep reading from the input stream until all the recognized words have been retrieved
						for (int i = 0; i < num; i++) {
							String line = in.readLine();
							if (format == OutputFormat.WORDS_ALIGNED) {
								//words have timestamp pairs with them
								String word[] = line.split(",");

								//add the new word to the recognized word chain
								if(reco_words.equals(""))
									reco_words = word[0];
								else
									reco_words += " " + word[0];

								//								//start/end of the word timestamps
								//								float start = Float.parseFloat(word[1]);
								//								float end = Float.parseFloat(word[2]);

							} else if (format == OutputFormat.WORDS) {
								//words don't have timestamp pairs with them
								if(reco_words.equals(""))
									reco_words = line;
								else
									reco_words += " " + line;
							}
						}

						final SharedPreferences prefs = ctx.getSharedPreferences(
								MainActivity.PREFS_NAME, MainActivity.MODE_PRIVATE);

						//sometimes underscores are used server side that we don't want to display
						reco_words = reco_words.replace('_', ' ');

						if(prefs.getBoolean(MainActivity.PREFS_PUNCTUATION, 
								MainActivity.DEFAULT_PUNCTUATION_STATE)) {

							//make the formatting of the output prettier
							String initChar = reco_words.substring(0, 1).toUpperCase(Locale.ENGLISH);
							reco_words = initChar + reco_words.substring(1, reco_words.length());

							//check if sentence should end in a question mark or period
							reco_words = addPunctuation(reco_words);
						}

						if(prefs.getBoolean(MainActivity.PREFS_CHINESE, 
								MainActivity.DEFAULT_CHINESE_STATE)) {

							reco_words = reco_words.replace(" ", "");
						}

						//send the string back to the UI thread
						Message msg = ctx.ui_handler.obtainMessage();
						msg.what = MainActivity.update_response_text;
						msg.obj = reco_words;
						ctx.ui_handler.sendMessage(msg);
						Log.e(TAG, reco_words);
					}

				}
			} catch(IOException e) {
				Log.e(TAG, "error while reading from server socket, closing Poll thread");
				break;
			}
		}

		Log.i(TAG, "stopped polling for server responses");
	}


	
	private static ArrayList<String> QUESTION_WORDS;
	static {
		QUESTION_WORDS = new ArrayList<String>(25);
		final String[] words = {"who", "what", "where", "when", "why", "how", "which", "wherefore",
				"whatever", "whom", "whose", "wherewith", "whither", "whence"};

		for(String s : words) {
			QUESTION_WORDS.add(s);
		}
	}

	/**
	 * Add puntuation to the end of the end of a sentence, attempting to guess whether it should be a
	 * period or question mark.
	 * @param sentence The sentence to add punctuation to
	 * @return a new string with appropriate punctuation added to the end.
	 */
	protected String addPunctuation(String sentence) {
		if(sentence == null || sentence.length() < 1) {
			Log.w(TAG, "did not provide a sentence to add punctuation to");
			return sentence;
		}

		//get the first word
		int firstSpace = sentence.indexOf(' ');
		if(firstSpace < 0) {
			Log.w(TAG, "did not find a space to indicate where the first word ends");
			return sentence;
		}
		String firstWord = sentence.substring(0, firstSpace);

		if(QUESTION_WORDS.contains(firstWord.toLowerCase(Locale.ENGLISH))) {
			Log.i(TAG, "sentence started with a question indicating word");

			return sentence + "?";
		} else if(sentence.charAt(sentence.length() - 1) != '.') {
			return sentence + ".";
		} else {
			return sentence;
		}
	}

	private void showToastInActivity(final MainActivity ctx, final String message){
		ctx.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
			}

		});
	}
}
