package edu.sv.cmu.datacollectiononline.thread;

import java.util.HashMap;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import edu.sv.cmu.datacollectiononline.MainActivity;
import edu.sv.cmu.datacollectiononline.handler.GPSTracker;
import edu.sv.cmu.datacollectiononline.util.ContextParams;

public class ContextThread extends Thread{

	MainActivity ctx;
	SensorManager sensorManager;
	Sensor orientation;
	Sensor accelerometer;
	Sensor proximity;
	Sensor gyro;
	Sensor rotate;

	public ContextParams cp;
	//public HashMap<String, Object> cpMap = new HashMap<String, Object>();
	 
	private final static String TAG = "CONTEXTTHREAD";
	private final static int EPSILON = 100;
	private static final float NS2S = 1.0f / 1000000000.0f;


	//private GPSTracker gpsTracker;

//	private float p_proximity = 0;

	private float timestamp = 0;
	float[] gravity = new float[3];
	float[] geomagnetic = new float[3];
	//float[] orient = new float[3];
	private final float[] deltaRotationVector = new float[4];

	public SensorEventListener sensorListener = new SensorEventListener(){

		@Override
		public void onSensorChanged(SensorEvent event) {
			int type = event.sensor.getType();
			switch(type){
			case Sensor.TYPE_PROXIMITY:
								cp.setProximity(event.values[0]);
				break;
			case Sensor.TYPE_ACCELEROMETER:
				final float alpha = (float) 0.8;

				gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
				gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
				gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

				cp.linear_acceleration[0] = event.values[0] - gravity[0];
				cp.linear_acceleration[1] = event.values[1] - gravity[1];
				cp.linear_acceleration[2] = event.values[2] - gravity[2];
				//				ctx.usr_wrapper.compassView.updateData(linear_acceleration[1]);
			 //Log.v(TAG,cp.linear_acceleration[0]+":" +cp.linear_acceleration[1]+":"+cp.linear_acceleration[2]);
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				geomagnetic[0] = event.values[0];
				geomagnetic[1] = event.values[1];
				geomagnetic[2] = event.values[2];
				break;
			case Sensor.TYPE_ORIENTATION:
				
				cp.setOrientation(event.values);
				//Log.d(TAG,"Orientation"+event.values[0]+cp.azimuth);
				break;
			case Sensor.TYPE_GYROSCOPE:
				// This timestep's delta rotation to be multiplied by the current rotation
				// after computing it from the gyro sample data.
				if (timestamp != 0) {
					final float dT = (event.timestamp - timestamp) * NS2S;
					// Axis of the rotation sample, not normalized yet.
					float axisX = event.values[0];
					float axisY = event.values[1];
					float axisZ = event.values[2];

					// Calculate the angular speed of the sample
					float omegaMagnitude = (float) Math.sqrt((double) axisX*axisX + axisY*axisY + axisZ*axisZ);

					// Normalize the rotation vector if it's big enough to get the axis
					if (omegaMagnitude > EPSILON) {
						axisX /= omegaMagnitude;
						axisY /= omegaMagnitude;
						axisZ /= omegaMagnitude;
					}

					// Integrate around this axis with the angular speed by the timestep
					// in order to get a delta rotation from this sample over the timestep
					// We will convert this axis-angle representation of the delta rotation
					// into a quaternion before turning it into the rotation matrix.
					float thetaOverTwo = omegaMagnitude * dT / 2.0f;
					float sinThetaOverTwo = (float) Math.sin((double)thetaOverTwo);
					float cosThetaOverTwo = (float) Math.cos((double)thetaOverTwo);
					deltaRotationVector[0] = sinThetaOverTwo * axisX;
					deltaRotationVector[1] = sinThetaOverTwo * axisY;
					deltaRotationVector[2] = sinThetaOverTwo * axisZ;
					deltaRotationVector[3] = cosThetaOverTwo;
				}
				timestamp = event.timestamp;
				float[] deltaRotationMatrix = new float[9];
				SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
				float[] rotationMatrix = new float[9];
				float[] inclineMatrix = new float[9];
				SensorManager.getRotationMatrix(rotationMatrix, inclineMatrix, cp.linear_acceleration, geomagnetic);

				break;
			}


		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {


		}

	};

	public ContextThread(MainActivity ctx){
		this.ctx =ctx;
		cp = new ContextParams();
		
	}

	public void kill(){
		blockSensors();
	}
	public void init(){
		
		sensorManager = (SensorManager) ctx.getSystemService(ctx.SENSOR_SERVICE);
		proximity = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		accelerometer = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		gyro = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		rotate = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		orientation = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

		sensorManager.registerListener(sensorListener, proximity, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorListener, gyro, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorListener, rotate, SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(sensorListener, orientation, SensorManager.SENSOR_DELAY_NORMAL);
		
		
	}

	public void blockSensors(){
		
		//		sensorManager.unregisterListener(sensorListener, accelerometer);
		sensorManager.unregisterListener(sensorListener);
	}

	@Override
	public void run(){
		init();
		while(true){
			try {
				Thread.sleep(1000);
				populateContextParams();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}	
	}
	
	private void populateContextParams(){
		ctx.ui_handler.sendEmptyMessage(MainActivity.set_gps);
			}
	

}
