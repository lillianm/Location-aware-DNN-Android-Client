package edu.sv.cmu.datacollectiononline;


import java.util.ArrayList;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import static ti.android.ble.sensortag.SensorTag.UUID_KEY_DATA;
import ti.android.ble.common.BluetoothLeService;
import ti.android.ble.common.GattInfo;
import ti.android.ble.sensortag.BarometerCalibrationCoefficients;
import ti.android.ble.sensortag.MagnetometerCalibrationCoefficients;
import ti.android.ble.sensortag.PreferencesActivity;
import ti.android.ble.sensortag.PreferencesFragment;
import ti.android.ble.sensortag.Sensor;
import ti.android.ble.sensortag.SensorTag;
import ti.android.ble.sensortag.SimpleKeysStatus;
import ti.android.util.Point3D;
import edu.sv.cmu.datacollectiononline.data.TXTParser;
import edu.sv.cmu.datacollectiononline.handler.GPSTracker;
import edu.sv.cmu.datacollectiononline.handler.ModePagerAdpater;
import edu.sv.cmu.datacollectiononline.handler.RecordButtonListener;
import edu.sv.cmu.datacollectiononline.handler.UIHandler;
import edu.sv.cmu.datacollectiononline.thread.AudioThread;
import edu.sv.cmu.datacollectiononline.thread.ContextThread;
import edu.sv.cmu.datacollectiononline.thread.PollThread;
import edu.sv.cmu.datacollectiononline.thread.WriteThread;
import edu.sv.cmu.datacollectiononline.util.XMLWriter;
import android.annotation.SuppressLint;
//import edu.sv.cmu.datacollectiononline.view.BarChartView;
//import edu.sv.cmu.datacollectiononline.view.MyCompassView;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
//import android.support.v7.app.ActionBar;
//import android.app.FragmentTransaction;
//import android.support.v7.app.ActionBarActivity;
//import android.support.v7.app.ActionBar; //Fragment Activity doesn't have getSupportActionBar()
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
public class MainActivity extends FragmentActivity {

	public static final UUID SECURE_UUID = UUID.fromString("F15CC914-E4BC-45CE-9930-CB7695385850");
	
	/*
	 * constants
	 */
	public static final String EXTRA_DEVICE = "EXTRA_DEVICE";

	public static String ANDROID_ID ;
	private final String TAG = "MainActivity";

	public final static int DEFAULT_SERVER_PORT = 7000;
	public final static int DEFAULT_SERVER_PORT_CN = 5011;
	public final static String DEFAULT_SERVER_ADDR = "209.129.244.7";

	/* BE SURE TO SET DEBUG TO FALSE TO CONNECT TO SERVER */
	//public static boolean DEBUG_MODE = true;

	/* APP MODE*/
	public final static int MODE_SPEECHTEST = 0;
	public final static int MODE_DATA_COLLECTION = 1000;

	/* Default preference*/
	public final static boolean DEFAULT_AUTOSTART_STATE = false;
	public final static boolean DEFAULT_AUTOSTOP_STATE = false;
	public final static boolean DEFAULT_PUNCTUATION_STATE = true;
	public final static boolean DEFAULT_CHINESE_STATE = false;

	/*
	 * static constants
	 */
	public final static int WRITETHREAD = 100;
	public final static int POLLTHREAD = 200;
	public final static int AUDIOTHREAD = 300;
	public final static int CONTEXTTHREAD = 400;
	public final static int BLUETOOTH = 500;


	public final static int socket_success = 0 + WRITETHREAD;
	public final static int socket_fail = 1 + WRITETHREAD;

	public final static int update_response_text = 0 + POLLTHREAD;
	public final static int start_pollthread = 1 + POLLTHREAD;
	public final static int update_prompt_text = 2 + POLLTHREAD;
	public final static int server_done = 3 + POLLTHREAD;

	public final static int update_energy_params_background = 10 + AUDIOTHREAD;
	public final static int update_energy_params_speech = 11 + AUDIOTHREAD;
	public final static int update_zc_params_background = 12 + AUDIOTHREAD;
	public final static int update_zc_params_speech = 13 + AUDIOTHREAD;
	public final static int update_running_average = 14 + AUDIOTHREAD;

	public final static int update_context_proximity = 0 + CONTEXTTHREAD;
	public final static int set_gps = 1 + CONTEXTTHREAD;


	//fragment argument constants
	public static final String ARG_SECTION_NUMBER = "section_number";
	//preferences constants
	public static final String PREFS_NAME = "edu.cmu.sv.speech_client_preferences";
	public static final String PREFS_MODE = "mode?client:data collection";
	public static final String PREFS_SERVER_IP = "server_ip";
	public static final String PREFS_SERVER_PORT = "server_port";
	public static final String PREFS_SERVER_PORT_CN = "server_port_cn";
	public static final String PREFS_AUTOSTART = "autostart_recording";
	public static final String PREFS_AUTOSTOP = "autostop_recording";
	public static final String PREFS_CHINESE = "chinese_formatting";
	public static final String PREFS_PUNCTUATION = "english_punctuation";

	/* statics for sharedprefereces that indicates the current status of the application */
	public static String CLIENTMODE = "speech_client";
	public static String DATACOLLECTIONMODE = "data_collection";
	public static String DEFAULT_PREFS_MODE = CLIENTMODE;

	/* Used by blueTooth Button to skip to next or do back */
	public static final int next_prompt = 0 + MODE_DATA_COLLECTION;
	public static final int prev_prompt = 1 + MODE_DATA_COLLECTION;


	/* fragments and adapters*/

	public Fragment currentFragment;
	public FragmentParamsWrapper pf_wrapper = null;
	public FragmentUserModeWrapper usr_wrapper = null;
	public ModePagerAdpater mPagerAdapter;
	private ViewPager mViewPager;

	private FragmentManager fm = getSupportFragmentManager();
	public FragmentManager fm_wrapper = null;


	/* threads */
	public AudioThread t_audio;
	public ContextThread t_context;
	public PollThread t_poll;
	public WriteThread t_write;

	/* Buffer */
	public List<byte[]> buffer = Collections.synchronizedList(new LinkedList<byte[]>());
	//private HashMap<String, Long> threads; // thread name is the key, thread id is value

	public TXTParser txtParser;
	/* Views */
	//public Button record_button;
	//public MyCompassView compassView;

	public Button btn_manual_start;
	//public BarChartView barchart;

	public GPSTracker gpsTracker;
	public XMLWriter xmlWriter;
	/* keep track of the socket status */
	public boolean socket_status = false;
	public boolean recording_status = false;

	/** BLE: BEGIN of BLE Code */

	private static final int PREF_ACT_REQ = 0;
	private static final int FWUPDATE_ACT_REQ = 1;

	// SensorTag
	private List<Sensor> mEnabledSensors = new ArrayList<Sensor>();
	private BluetoothGattService mOadService = null;
	private BluetoothGattService mConnControlService = null;
	private static boolean mMagCalibrateRequest = true;
	private static boolean mHeightCalibrateRequest = true;



	// BLE
	private BluetoothLeService mBtLeService = null;
	private BluetoothDevice mBluetoothDevice = null;
	private BluetoothGatt mBtGatt = null;
	private List<BluetoothGattService> mServiceList = null;
	private static final int GATT_TIMEOUT = 100; // milliseconds
	private boolean mServicesRdy = false;
	private boolean mIsReceiving = false;
	protected static final  String blePressString = "<Press BLE button for the next Prompt>";
	protected static final  String recordPressString = "<Press Record to begin>";

	private PowerManager pm;
	private PowerManager.WakeLock wl;
	@Override 
	protected void onResume()
	{
		if (!wl.isHeld())
			wl.acquire();
		Log.d(TAG,"onResume");
		super.onResume();
		if (!mIsReceiving) {
			registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
			Log.e(TAG,"mIsReceiving = true");
			mIsReceiving = true;
		}
	}

	@Override
	protected void onPause() {
		Log.d(TAG,"onPause");
		super.onPause();
		if (mIsReceiving) {
			unregisterReceiver(mGattUpdateReceiver);
			mIsReceiving = false;
		}
		if (wl.isHeld())
			wl.release();
	}





	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.e(TAG, "onCreate Called");



		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);
		if (!wl.isHeld())
			wl.acquire();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Intent intent = getIntent();


		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		/** BLE: Start of BLE related code */
		// BLE
		mBtLeService = BluetoothLeService.getInstance();
		mBluetoothDevice = intent.getParcelableExtra(EXTRA_DEVICE);
		mServiceList = new ArrayList<BluetoothGattService>();
		
		// GATT database
		Resources res = getResources();
		XmlResourceParser xpp = res.getXml(R.xml.gatt_uuid);
		new GattInfo(xpp);

		// Initialize sensor list
		updateSensorList();
		/** BLE: end of BLE related code */
		/** BLE: Start of BLE related code */
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		onViewInflated(getCurrentFocus());
		/** BLE: Start of BLE related code */


		ANDROID_ID = Secure.getString(getContentResolver(),
				Secure.ANDROID_ID); 


		txtParser = new TXTParser(this);
		txtParser.populatePromptData();


		/* Set up the action bar. */
		final android.app.ActionBar actionBar = getActionBar();
		//actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		//actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowHomeEnabled(true);
		//actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		actionBar.setCustomView(R.layout.actionbar_view);



		/* init fragmentManager, viewPager and pagerAdapter*/
		fm = getSupportFragmentManager();
		mPagerAdapter = new ModePagerAdpater(fm);
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mPagerAdapter);

		/* Remove invalid socket */
		if(t_write!=null && t_write.isAlive()){
			Log.e(TAG,"SocketThread not terminated previously");
		}

		if(usr_wrapper == null){
			usr_wrapper = mPagerAdapter.usr_wrapper;
		}


		/* record button */
		btn_manual_start = (Button) findViewById(R.id.btn_start);
		btn_manual_start.setText("Click to Connect to Server");
		btn_manual_start.setBackgroundResource(R.drawable.btn_yellow);
		btn_manual_start.setOnClickListener(new RecordButtonListener(this));


		//threads = new HashMap<String, Long>();

		/* collect GPS data */
		gpsTracker = new GPSTracker(this);

		/* other senser listeners in the context thread */
		t_context = new ContextThread(this);
		xmlWriter = new XMLWriter();
		t_context.start();

		/* Connect to Google Glass*/
		




	}
	/**
	 * callback class for the data service
	 */
	private ServiceConnection sp_proc_connection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			//called when the connection with the service has been established

			//sp_service = ((SPBinder)service).getService();

			Log.i(TAG, "main activity bound to speech processing service");
		}

		public void onServiceDisconnected(ComponentName className) {
			//called when connection with service is unexpectedly disconnected
			//should never be called for us because it's running in our process

			Log.e(TAG, "service unexpectedly disconnected");
		}
	};

	private ServiceConnection vlc_proc_connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			//called when the connection with the service has been established
			//vlc_service = ((VLCBinder)service).getService();
			Log.i(TAG, "main activity bound to vlc service");
		}
		public void onServiceDisconnected(ComponentName className) {
			//called when connection with service is unexpectedly disconnected
			//should never be called for us because it's running in our process

			Log.e(TAG, "service unexpectedly disconnected");
		}

	};
	protected static long record_start_time;
	/* Install UI handler */
	public final UIHandler ui_handler = new UIHandler(this);

	@Override
	public void onStart() {
		super.onStart();
		Log.v(TAG, "onStart() called");


		/*
		 * set up the audio data collection thread
		 */
		t_audio = new AudioThread(0, this);
		t_audio.start();


		/* Remove invalid socket */
		if(t_write!=null && t_write.isAlive()){
			Log.e(TAG,"SocketThread not terminated previously");
		}

		if(pf_wrapper == null){
			pf_wrapper = mPagerAdapter.pf_wrapper;
		}
		if(usr_wrapper == null){
			usr_wrapper = mPagerAdapter.usr_wrapper;
		}
		ui_handler.removeCallbacksAndMessages(null);

		if(txtParser==null){
			txtParser = new TXTParser(this);
			txtParser.populatePromptData();
		}
		if (!wl.isHeld())
			wl.acquire();


		Log.d(TAG, "about to call bind service");




	}

	@Override
	public void onStop() {
		super.onStop();

		Log.i(TAG, "onStop");

		if(t_write!=null)
			t_write.kill();
		if(t_poll!=null)
			t_poll.kill();
		if(t_audio!=null)
			t_audio.kill();
		if(t_context!=null)
			t_context.kill();
		/* clear the message queue */
		if(ui_handler!=null){
			ui_handler.removeCallbacksAndMessages(null);
		}
		


	}
	@Override 
	public void onDestroy(){
		super.onDestroy();

		Log.d(TAG,"onDestroy");
		if(t_write!=null)
			t_write.kill();
		if(t_poll!=null)
			t_poll.kill();
		if(t_audio!=null)
			t_audio.kill();
		if(t_context!=null)
			t_context.kill();
		/* clear the message queue */
		if(ui_handler!=null){
			ui_handler.removeCallbacksAndMessages(null);
		}
		if(mBtLeService!=null){
			mBtLeService.close();
		}
		mBtLeService = null;
		/* maybe you should set an alarm to kill all the threads after a few minutes */
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		/*
		 * get ready to make an alert dialog to get feedback from the user when
		 * they choose something in the settings menu
		 */
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		AlertDialog alert;

		//set the cancel button
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		//get the preferences for the app
		final SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		//make an editable field for the user to possibly use (I know it's weird but select case
		//doesn't have scope)
		final EditText input = new EditText(this);

		//figure out which menu item was clicked
		switch(item.getItemId()) {
		case R.id.server_ip_settings:

			//set the title and message of the alert
			builder.setTitle("Server IP");
			builder.setMessage("Set the IP address of the server:");

			//make an EditText field for the alert to use
			builder.setView(input);

			/*
			 * limit the input type to valid IP format
			 * 
			 * copied from http://stackoverflow.com/questions/5798140/press-many-times-validate-ip-address-in-edittext-while-typing
			 */
			InputFilter[] filters = new InputFilter[1];
			filters[0] = new InputFilter() {

				@Override
				public CharSequence filter(CharSequence source, int start,
						int end, Spanned dest, int dstart, int dend) {
					//if there is text
					if (end > start) {
						String destTxt = dest.toString();
						String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);

						if (!resultingTxt.matches ("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) { 
							return "";
						} else {
							String[] splits = resultingTxt.split("\\.");

							for (int i=0; i<splits.length; i++) {
								if (Integer.valueOf(splits[i]) > 255) {
									return "";
								}
							}
						}
					}
					return null;
				}

			};
			input.setFilters(filters);
			input.setRawInputType(InputType.TYPE_CLASS_NUMBER);

			//set the initial text to whatever prefs is set to
			input.setText(prefs.getString(PREFS_SERVER_IP, DEFAULT_SERVER_ADDR));

			//set the confirm button
			builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {

					//get the user input
					String ip_addr = input.getText().toString();

					//get the preferences and commit the new value
					SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
					prefs.edit().putString(PREFS_SERVER_IP, ip_addr).commit();
				}
			});

			//make and show the dialog
			alert = builder.create();
			alert.show();

			return true;

		case R.id.server_port_settings:

			//set the title and message of the alert
			builder.setTitle("Server Port");
			builder.setMessage("Set the port to communicate to the server on:");

			//make an EditText field for the alert to use
			builder.setView(input);

			//limit the input type to just numbers
			input.setInputType(InputType.TYPE_CLASS_NUMBER);

			//set the initial text to whatever prefs is set to
			input.setText(String.valueOf(prefs.getInt(PREFS_SERVER_PORT, DEFAULT_SERVER_PORT)));

			//set the confirm button
			builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {

					//get the port number provided by the user
					String port_string = input.getText().toString();
					int port = Integer.parseInt(port_string);

					//TODO check if port is in proper range

					//get the preferences and commit the new value
					SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
					prefs.edit().putInt(PREFS_SERVER_PORT, port).apply();
				}
			});

			Log.e(TAG,""+prefs.getInt(PREFS_SERVER_PORT, 0));
			//make and show the dialog
			alert = builder.create();
			alert.show();


			return true;


			// Put this back when Chinese version is ready	

			/*
		case R.id.server_port_settings_cn:

			//set the title and message of the alert
			builder.setTitle("Server Port");
			builder.setMessage("Set the port to communicate to the server on:");

			//make an EditText field for the alert to use
			builder.setView(input);

			//limit the input type to just numbers
			input.setInputType(InputType.TYPE_CLASS_NUMBER);

			//set the initial text to whatever prefs is set to
			input.setText(String.valueOf(prefs.getInt(PREFS_SERVER_PORT_CN, DEFAULT_SERVER_PORT_CN)));

			//set the confirm button
			builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {

					//get the port number provided by the user
					String port_string = input.getText().toString();
					int port = Integer.parseInt(port_string);

					//TODO check if port is in proper range

					//get the preferences and commit the new value
					SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
					prefs.edit().putInt(PREFS_SERVER_PORT_CN, port).commit();
				}
			});

			//make and show the dialog
			alert = builder.create();
			alert.show();

			return true;
			 */

		case R.id.autostop_settings:

			Log.i(TAG, "changing autostop settings");

			//set the title and message of the alert
			builder.setTitle(R.string.autostop_string);
			builder.setMessage("Stop recording automatically when a voice isn't detected:");

			//set the on button
			builder.setPositiveButton("On", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//get the preferences and commit the new value
					SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
					prefs.edit().putBoolean(PREFS_AUTOSTOP, true).commit();
				}
			});

			//set the off button
			builder.setNegativeButton("Off", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//get the preferences and commit the new value
					SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
					prefs.edit().putBoolean(PREFS_AUTOSTOP, false).commit();
				}
			});

			//make and show the dialog
			alert = builder.create();
			alert.show();

			/* Highlight the current state */
			if( prefs.getBoolean(PREFS_AUTOSTOP, DEFAULT_AUTOSTOP_STATE)){
				alert.getButton(DialogInterface.BUTTON_POSITIVE).setBackgroundColor(Color.BLUE);
			}
			else{
				alert.getButton(DialogInterface.BUTTON_NEGATIVE).setBackgroundColor(Color.BLUE);
			}
			return true;

		case R.id.autostart_setting:

			Log.i(TAG, "changing autostart settings");

			//set the title and message of the alert
			builder.setTitle(R.string.autostart_string);
			builder.setMessage("Stop recording automatically when a voice isn't detected:");

			//set the on button
			builder.setPositiveButton("On", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//get the preferences and commit the new value
					SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
					prefs.edit().putBoolean(PREFS_AUTOSTART, true).commit();
				}
			});

			//set the off button
			builder.setNegativeButton("Off", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//get the preferences and commit the new value
					SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
					prefs.edit().putBoolean(PREFS_AUTOSTART, false).commit();
				}
			});

			//make and show the dialog
			alert = builder.create();
			alert.show();
			//record_button.setVisibility(View.VISIBLE);
			/* Highlight the current state */
			if( prefs.getBoolean(PREFS_AUTOSTART, DEFAULT_AUTOSTART_STATE)){
				alert.getButton(DialogInterface.BUTTON_POSITIVE).setBackgroundColor(Color.BLUE);
			}
			else{
				alert.getButton(DialogInterface.BUTTON_NEGATIVE).setBackgroundColor(Color.BLUE);
			}
			return true;



		case R.id.punctuation_settings:

			Log.i(TAG, "changing punctuation settings");

			//set the title and message of the alert
			builder.setTitle(R.string.punctuation_string);
			builder.setMessage("Try and add appropriate punctuation to the transcription:");

			//set the on button
			builder.setPositiveButton("On", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//get the preferences and commit the new value
					SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
					prefs.edit().putBoolean(PREFS_PUNCTUATION, true).commit();
				}
			});

			//set the off button
			builder.setNegativeButton("Off", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//get the preferences and commit the new value
					SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
					prefs.edit().putBoolean(PREFS_PUNCTUATION, false).commit();
				}
			});

			//make and show the dialog
			alert = builder.create();
			alert.show();
			/* Highlight the current state */
			if( prefs.getBoolean(PREFS_PUNCTUATION, DEFAULT_PUNCTUATION_STATE)){
				alert.getButton(DialogInterface.BUTTON_POSITIVE).setBackgroundColor(Color.BLUE);
			}
			else{
				alert.getButton(DialogInterface.BUTTON_NEGATIVE).setBackgroundColor(Color.BLUE);
			}
			return true;


		default:
			//I don't recognize what was clicked so just call the superclass and hope it handles it
			return super.onOptionsItemSelected(item);
		}
	}

	//	@Override
	//	public void onTabSelected(ActionBar.Tab tab,
	//			FragmentTransaction fragmentTransaction) {
	//		// When the given tab is selected, switch to the corresponding page in
	//		// the ViewPager.
	//		//mViewPager.setCurrentItem(tab.getPosition());
	//
	//		/*
	//		 * stop the old fragment from recording if it is
	//		 */
	//		//		
	//		/*
	//		 * update the current fragment
	//		 */
	//		//currentFragment = getSupportFragmentManager().findFragmentByTag(
	//		//	mSectionsPagerAdapter.makeFragmentName(R.id.pager, tab.getPosition()));
	//
	//	}
	//
	//	@Override
	//	public void onTabUnselected(ActionBar.Tab tab,
	//			FragmentTransaction fragmentTransaction) {
	//	}
	//
	////	@Override
	////	public void onTabReselected(ActionBar.Tab tab,
	////			FragmentTransaction fragmentTransaction) {
	//	}
	/**
	 * Retreives an AudioThread connected to the requested IP address and port combination. If one already
	 * exists then it returns that, otherwise it creates the requested connection and returns the new thread
	 * already started.
	 * 
	 * @param server_addr the IP address to connect to the server at
	 * @param server_port the port to connect to the server on
	 * @param frag_id the id of the fragment to send feedback from the connection to
	 * @param reset whether to reset the connection if it's already alive
	 * @return a data thread connected to the proper transcription server and mongo db for that type of fragment
	 */
	//	public AudioThread getAudioThread(String server_addr, int server_port, boolean reset) {
	//
	//
	//		//search the thread map for a matching thread
	//		//SocketThread socket = thread_map.get(key);
	//
	//		//check if the out thread doesn't exist yet or has died
	//		if(t_write == null || !t_write.isAlive()) {
	//
	//			//set up connection threads with server
	//			t_write = new WriteThread(this, buffer,DEFAULT_SERVER_ADDR, DEFAULT_SERVER_PORT, ui_handler);
	//			t_write.start();
	//
	//			//put the new thread in the map
	//			//thread_map.put(key, t_out);
	//			threads.put(socket_thread, t_write.getId());
	//
	//			Log.v(TAG, "had to start up a new data thread");
	//		} else if(reset) {
	//			//kill the connection
	//			t_write.kill();
	//
	//			//make the new connection
	//			t_write = new WriteThread(this, buffer,DEFAULT_SERVER_ADDR, DEFAULT_SERVER_PORT, ui_handler);
	//			t_write.start();
	//
	//			//put the new thread in the map
	//			//thread_map.put(key, t_out);
	//			threads.put(socket_thread,t_write.getId());
	//
	//			Log.v(TAG, "reset a live connection");
	//		}
	//
	//
	//		/*
	//		 * TODO this is causing severe lag when there is no connection to the server.
	//		 * I need to change my reconnection strategy for if a connection dies so I
	//		 * can not block up the UI thread waiting on a connection to time out.
	//		 */
	//
	//		//update the data thread with the new transmission info
	//		t_audio.updateTarget(t_write);
	//
	//		//return the thread
	//		return t_audio;
	//	}

	/* debugging */
	private void listAllThreads(){
		Set<Thread> allThreads = Thread.getAllStackTraces().keySet();
		for(Thread t: allThreads){
			Log.e("THREADS",t.getName());
		}
	}




	/**
	 * A fragment representing the prompted speech portion of the app
	 */
	//	public static class PromptFragment extends Fragment {
	//
	//		/*
	//		 * constants
	//		 */
	//		private static final String TAG = "PromptFragment";
	//		public static final int PROMPT_ID = 0;
	//		public static final int RESPONSE_ID = 1;
	//
	//		/*
	//		 * globals
	//		 */
	//		public Spannable currentPrompt;
	//		private int frag_id;
	//		private int server_port;
	//		private String server_addr;
	//		private CMUCollectActivity act;
	//		private long s_time = -1;
	//		private long e_time = -1;
	//		private String text = "";
	//		
	//		public PromptFragment() {
	//			Log.d(TAG, "constructor for prompt fragment");
	//		}
	//		/** Begin BLE Code */
	//		/** When there is any change received by the Bluetooth message handler */
	private boolean isBLEPressed = false;
	@SuppressLint("DefaultLocale")
	private void onCharacteristicChanged(String uuidStr, byte[] value) {
		Log.d(TAG,uuidStr);
		if (mMagCalibrateRequest) {
			if (uuidStr.equals(SensorTag.UUID_MAG_DATA.toString())) {
				Point3D v = Sensor.MAGNETOMETER.convert(value);

				MagnetometerCalibrationCoefficients.INSTANCE.val = v;
				mMagCalibrateRequest = false;
				Toast.makeText(this, "Magnetometer calibrated", Toast.LENGTH_SHORT)
				.show();
			}
		}

		if (mHeightCalibrateRequest) {
			if (uuidStr.equals(SensorTag.UUID_BAR_DATA.toString())) {
				Point3D v = Sensor.BAROMETER.convert(value);

				BarometerCalibrationCoefficients.INSTANCE.heightCalibration = v.x;
				mHeightCalibrateRequest = false;
				Toast.makeText(this, "Height measurement calibrated", Toast.LENGTH_SHORT)
				.show();
			}
		}

		/* case OFF_TO_ON : start recording */

		/* CASE;
		 * 	ON_OFF:  left key down 
		 *  OFF_OFF: left key up 
		 *  OFF_ON:  right key down
		 *  OFF_OFF: right key up
		 *  */

		if (uuidStr.equals(UUID_KEY_DATA.toString())) {
			SimpleKeysStatus s;
			s = Sensor.SIMPLE_KEYS.convertKeys(value);


			switch (s) {
			/* left key down */
			case ON_OFF:
				Log.d(TAG,"LEFT;ON_OFF");
				if(this.socket_status == false){
					Toast.makeText(this, "Click Button to Connect to Server First!", Toast.LENGTH_SHORT).show();
				}
				else{
					/* Disable button now*/
					btn_manual_start.setEnabled(false);
					btn_manual_start.setText("Recording");
					btn_manual_start.setBackgroundResource(R.drawable.btn_green);



					/* resetConnection() creates a new WriteThread */
					if(t_write!=null && t_write.isAlive()){
						t_write.kill();
					}
					if(t_poll!=null && t_poll.isAlive()){
						t_poll.kill();
					}
					t_write = new WriteThread(this,buffer,MainActivity.DEFAULT_SERVER_ADDR, MainActivity.DEFAULT_SERVER_PORT,ui_handler); 


					t_write.start();

					recording_status = true;
					t_audio.audio_handler.sendMessage(t_audio.audio_handler.obtainMessage(AudioThread.start_recording_from_audio));
					btn_manual_start.setEnabled(true);

				}
				break;

			case OFF_OFF:
				Log.e(TAG,"OFF_OFF");

				if(this.socket_status == false){
					Toast.makeText(this, "Click Button to Connect to Server First!", Toast.LENGTH_SHORT).show();
				}

				else{
					btn_manual_start.setEnabled(false);

					btn_manual_start.setText("Click to Start");
					btn_manual_start.setBackgroundResource(R.drawable.btn_red);
					//btn_manual_start.setBackgroundColor(Color.RED);
					recording_status = false;

					//stop capturing and send empty package
					t_audio.capturing = false;

					t_audio.audio_handler.sendMessage(t_audio.audio_handler.obtainMessage(AudioThread.stop_recording_from_audio));
					//ctx.t_context.cp.end_time = System.currentTimeMillis();
					btn_manual_start.setEnabled(true);
				}
				break;
			case OFF_ON:
				Log.e(TAG, "OFF_ON");
				ui_handler.sendMessage(ui_handler.obtainMessage(next_prompt));
				break;
			case ON_ON:
				Log.e(TAG,"ON_ON");
				break;
			default:
				throw new UnsupportedOperationException();
			}

			//mButton.setImageResource(img);
		}
	}
	/**
	 *  BLE: receiver receiver
	 */
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			int status = intent.getIntExtra(BluetoothLeService.EXTRA_STATUS, BluetoothGatt.GATT_SUCCESS);

			if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				if (status == BluetoothGatt.GATT_SUCCESS) {
					displayServices();
					checkOad();
				} else {
					Toast.makeText(getApplication(), "Service discovery failed", Toast.LENGTH_LONG).show();
					return;
				}
			} else if (BluetoothLeService.ACTION_DATA_NOTIFY.equals(action)) {
				// Notification
				byte  [] value = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
				String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
				// Send the value and UUID to the current fragment. The fragment should implement 
				// onCharacteristicChanged function that has a handler for the BLE button
				onCharacteristicChanged(uuidStr, value);
			} else if (BluetoothLeService.ACTION_DATA_WRITE.equals(action)) {
				// Data written
				String uuidStr = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
				onCharacteristicWrite(uuidStr,status);
			} 

			if (status != BluetoothGatt.GATT_SUCCESS) {
				setError("GATT error code: " + status);
			}
		}
	};

	/**
	 * BLE: Write data to sensortag?? Not used or implemented in the source. Just added for completeness sake
	 * @param uuidStr
	 * @param status
	 */
	private void onCharacteristicWrite(String uuidStr, int status) {
		Log.d(TAG,"onCharacteristicWrite: " + uuidStr);
	}

	/**
	 * Display the status of services from the BLE sensortag
	 */
	private void displayServices() {
		mServicesRdy = true;

		try {
			mServiceList = mBtLeService.getSupportedGattServices();
		} catch (Exception e) {
			e.printStackTrace();
			mServicesRdy = false;
		}

		// Characteristics descriptor readout done
		if (mServicesRdy) {
			setStatus("Service discovery complete");
			enableSensors(true);
			enableNotifications(true);
		} else {
			setError("Failed to read services");
		}
	}

	/**
	 * BLE: Show any error from the sensortag
	 * @param txt
	 */
	private void setError(String txt) {

		Toast.makeText(this, "Error:" + txt, Toast.LENGTH_SHORT).show();
	}

	/**
	 * BLE: Set the status of sensortag
	 * @param txt
	 */
	private void setStatus(String txt) {
		Toast.makeText(this, "Status:" + txt, Toast.LENGTH_SHORT).show();
	}

	/**
	 * BLE: Enables the Sensors on the SensorTag
	 * @param enable
	 */
	@SuppressLint("NewApi")
	private void enableSensors(boolean enable) {
		for (Sensor sensor : mEnabledSensors) {
			UUID servUuid = sensor.getService();
			UUID confUuid = sensor.getConfig();

			// Skip keys 
			if (confUuid == null)
				break;



			BluetoothGattService serv = mBtGatt.getService(servUuid);
			BluetoothGattCharacteristic charac = serv.getCharacteristic(confUuid);
			byte value =  enable ? sensor.getEnableSensorCode() : Sensor.DISABLE_SENSOR_CODE;
			mBtLeService.writeCharacteristic(charac, value);
			mBtLeService.waitIdle(GATT_TIMEOUT);
		}

	}

	/**
	 * BLE: Enable notifications from BLE Sensors
	 * @param enable
	 */

	@SuppressLint("NewApi")
	private void enableNotifications(boolean enable) {
		for (Sensor sensor : mEnabledSensors) {
			UUID servUuid = sensor.getService();
			UUID dataUuid = sensor.getData();
			BluetoothGattService serv = mBtGatt.getService(servUuid);
			Log.d(TAG,serv.toString());
			BluetoothGattCharacteristic charac = serv.getCharacteristic(dataUuid);
			Log.e(TAG, "mBtleService:" + mBtLeService);
			mBtLeService.setCharacteristicNotification(charac,enable);
			mBtLeService.waitIdle(GATT_TIMEOUT);
		}
	}

	/**
	 *   BLE: Start an activity to change the preferences for the BLE sensortag
	 */
	private void startPreferenceActivity() {
		// Disable sensors and notifications when settings dialog is open
		if (mBluetoothDevice != null) {
			enableSensors(false);
			enableNotifications(false);

			final Intent i = new Intent(this, PreferencesActivity.class);
			i.putExtra(PreferencesActivity.EXTRA_SHOW_FRAGMENT, PreferencesFragment.class.getName());
			i.putExtra(PreferencesActivity.EXTRA_NO_HEADERS, true);
			i.putExtra(EXTRA_DEVICE, mBluetoothDevice);
			startActivityForResult(i,PREF_ACT_REQ);		
		}
	}

	/**
	 * BLE: Check if there is an over the air update for the SensorTag
	 */
	@SuppressLint("NewApi")
	private void checkOad() {
		// Check if OAD is supported (needs OAD and Connection Control service)
		mOadService = null;
		mConnControlService = null;

		for (int i = 0; i < mServiceList.size() && (mOadService == null || mConnControlService == null); i++) {
			BluetoothGattService srv = mServiceList.get(i);
			if (srv.getUuid().equals(GattInfo.OAD_SERVICE_UUID)) {
				mOadService = srv;
			}
			if (srv.getUuid().equals(GattInfo.CC_SERVICE_UUID)) {
				mConnControlService = srv;
			}
		}
	}

	/**
	 * BLE: Check if a sensor in the sensor tag is enabled by the preferences
	 * @param sensor
	 * @return
	 */
	boolean isEnabledByPrefs(final Sensor sensor) {
		String preferenceKeyString = "pref_" + sensor.name().toLowerCase(Locale.ENGLISH) + "_on";

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		Boolean defaultValue = true;
		return prefs.getBoolean(preferenceKeyString, defaultValue);
	}

	/**
	 * BLE: Get the Over the Air discovery service
	 * @return
	 */
	BluetoothGattService getOadService() {
		return mOadService;
	}

	/**
	 * BLE: Get the connection control service for bluetooth
	 * @return
	 */
	BluetoothGattService getConnControlService() {
		return mConnControlService;
	}
	//
	// Application implementation
	//
	/**
	 * Update the list of sensors based on preferences.
	 */
	private void updateSensorList() {
		mEnabledSensors.clear();

		for (int i=0; i<Sensor.SENSOR_LIST.length; i++) {
			Sensor sensor = Sensor.SENSOR_LIST[i];
			if (isEnabledByPrefs(sensor)) {
				mEnabledSensors.add(sensor);
			}
		}
		ArrayList<Integer> a = new ArrayList<Integer>();
		Collections.sort(a);
	}

	// Activity result handling
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case PREF_ACT_REQ:
			if (mBluetoothDevice != null) {
				Toast.makeText(this, "Applying preferences", Toast.LENGTH_SHORT).show();
				if (!mIsReceiving) {
					mIsReceiving = true;
					registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
				}

				updateSensorList();
				enableSensors(true);
				enableNotifications(true);
			}
			break;
		case FWUPDATE_ACT_REQ:
			// FW update cancelled so resume
			if (mBluetoothDevice != null) {
				enableSensors(true);
				enableNotifications(true);
			}
			break;
		default:
			Log.e(TAG, "Unknown request code");
			break;
		}
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter fi = new IntentFilter();
		fi.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		fi.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
		fi.addAction(BluetoothLeService.ACTION_DATA_WRITE);
		fi.addAction(BluetoothLeService.ACTION_DATA_READ);
		return fi;
	}
	/**
	 * when the main activity view is inflated
	 * @param view
	 */
	void onViewInflated(View view) {
		Log.d(TAG, "Gatt view ready");

		// Create GATT object
		if (mBluetoothDevice != null) {
			mBtGatt = BluetoothLeService.getBtGatt();
			// Start service discovery
			if (!mServicesRdy && mBtGatt != null) {
				if (mBtLeService.getNumServices() == 0)
					discoverServices();
				else
					displayServices();
			}
		}
	}

	/**
	 * Discover services available by Bluetooth
	 */
	@SuppressLint("NewApi")
	private void discoverServices() {
		if (mBtGatt.discoverServices()) {
			Log.i(TAG, "START SERVICE DISCOVERY");
			mServiceList.clear();
			setStatus("Service discovery started");
		} else {
			setError("Service discovery start failed");
		}
	}

	/** END of BLE Code */



}


