package edu.sv.cmu.datacollectiononline.handler;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import edu.sv.cmu.datacollectiononline.MainActivity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

public class GPSTracker extends Service implements LocationListener {
	public static final String TAG = "GPSTracker";
	private final Context context;
	private MainActivity ctx;
	private boolean isGPSEnabled;
	private boolean isNetworkEnabled;
	private static final int MIN_TIME_BY_UPDATES = 1000*30;
	private static final int MIN_DISTANCE_BY_UPDATES = 10;
	private Location location;
	public GPSTracker(Context context){
		this.context = context;
		ctx = (MainActivity) context;
		Log.w("Service launched","Yes");
		getLocation();
	}

	public void getLocation(){
		LocationManager lm = (LocationManager) context.getSystemService(LOCATION_SERVICE);
		isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		if(!isGPSEnabled && !isNetworkEnabled){
			Toast.makeText(context, "GPS and Network are not available", Toast.LENGTH_LONG).show();
			enableDialog();
		}
		else{
			if(isNetworkEnabled){
				lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 
						MIN_TIME_BY_UPDATES,MIN_DISTANCE_BY_UPDATES, this);
				if(lm != null){
					location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); 
					if(location !=null) Log.w("Network Enabled",location.toString());
					else{
						Log.e(TAG,"Network error");
					}
				}
			}
			if(isGPSEnabled){
				lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 
						MIN_TIME_BY_UPDATES, MIN_DISTANCE_BY_UPDATES, this);
				if(lm!=null){
					location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); 
					if(location!=null){Log.w("GPS Enabled", location.toString());}
				}
			}
		}
	}
	@Override
	public void onLocationChanged(Location location) {
		ctx.t_context.cp.setGPS(getlongitude(), getlatitude());
//				String jsonString = ctx.t_context.cp.returnJSONString();
//		int cp_header_size = 4;
//		Log.d(TAG,jsonString);
//		byte[] context_header = new byte[cp_header_size];
//		byte[] context_packet = new byte[cp_header_size + jsonString.length()];
//		
//		ByteBuffer.wrap(context_header).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(0xffff0000+jsonString.length());
//		
//		for(int i=0;i<context_header.length;i++){
//			context_packet[i] = context_header[i];
//		}
//		for(int i=0;i<jsonString.length();i++){
//			context_packet[i+4] = (byte) jsonString.charAt(i); 
//		}
//		Log.d(TAG,"context_packet sent"+context_packet.length);

	}

	@Override
	public void onProviderDisabled(String provider) {
		if(context!=null)
		{
			Toast.makeText(context, "Disabled provider " + provider,Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onProviderEnabled(String provider) {
		if(context !=null){
			Toast.makeText(context, "Enabled new provider\n please go back to continue " + provider,
					Toast.LENGTH_SHORT).show();}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if(context!=null){
			Toast.makeText(context,"Provider status changed ", Toast.LENGTH_SHORT).show();
		}

	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	public double getlatitude(){
		if(location !=null){
			return location.getLatitude();
		}
		return 0;
	}
	public double getlongitude(){
		if(location !=null){
			return location.getLongitude();
		}
		return 0;
	}

	public void enableDialog(){

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("GPS Settings");
		builder.setMessage("GPS is not enabled, do you want to enable the GPS Service?");

		builder.setPositiveButton("Enable",new DialogInterface.OnClickListener(){
			@Override 
			public void onClick(DialogInterface dialog, int whichButton){
				Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				context.startActivity(intent);
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int whichButton){
				dialog.dismiss();
			}
		});

		builder.show();
	}
}
