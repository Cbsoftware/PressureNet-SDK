package ca.cumulonimbus.pressurenetsdk;

import java.util.ArrayList;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.IBinder;

/**
 * Represent developer-facing pressureNET API
 * Background task; manage and run everything
 * Handle Intents
 * 
 * @author jacob
 * 
 */
public class CbService extends Service implements SensorEventListener  {
	
	private CbDataCollector dataCollector;
	
	/**
	 * Find all the data for an observation group.
	 * 
	 * Location, Measurement values, etc.
	 * 
	 * @return
	 */
	public CbObservationGroup collectNewObservationGroup() {
		ArrayList<CbObservation> observations = new ArrayList<CbObservation>();
		CbObservation pressureObservation = new CbObservation();		
		
		// Location values
		
		// Measurement values
		dataCollector = new CbDataCollector();
		pressureObservation = dataCollector.getPressureObservation();
		
		// Put everything together
		observations.add(pressureObservation);
		CbObservationGroup newGroup = new CbObservationGroup();
		newGroup.setGroup(observations);
		return newGroup;
	}
	
	/**
	 * Use HTTPS to send the observation group to the server
	 * 
	 * @param group
	 * @return
	 */
	
	public boolean sendCbObservationGroup(CbObservationGroup group) {
		// TODO: Implement
		return false;
	}
	
	public void start() {
		log("CbService: Starting to collect data.");
		
		// use AlarmManager or postDelayed tasks
		// to periodically collect and send data
		// according to values in CbSettings.
		
		// One group
		CbObservationGroup fullGroup = collectNewObservationGroup();
		sendCbObservationGroup(fullGroup);
		
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		start();
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public void log(String message) {
		System.out.println(message);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO: Implement
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO: Implement
	}
	
}
