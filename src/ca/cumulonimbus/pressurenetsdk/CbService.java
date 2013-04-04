package ca.cumulonimbus.pressurenetsdk;

import java.util.ArrayList;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.widget.Toast;

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
	private CbSettingsHandler settingsHandler;
	private CbLocationManager locationManager;
	
	private Handler mHandler = new Handler();
	
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
		locationManager = new CbLocationManager(this);
		locationManager.startGettingLocations();
		
		// Measurement values
		dataCollector = new CbDataCollector();
		pressureObservation = dataCollector.getPressureObservation();
		
		// Put everything together
		observations.add(pressureObservation);
		CbObservationGroup newGroup = new CbObservationGroup();
		newGroup.setGroup(observations);
		return newGroup;
	}

	private Runnable mSubmitReading = new Runnable() {
		public void run() {
			log("collecting and submitting");
			long base = SystemClock.uptimeMillis();
			
			CbObservationGroup fullGroup = new CbObservationGroup();
			
			// Collect observations
			if(settingsHandler.isCollectingData()) {
				fullGroup = collectNewObservationGroup();
				// Send
				if(settingsHandler.isSharingData()) {
					sendCbObservationGroup(fullGroup);
				}
			}
			
			mHandler.postAtTime(this, base + (settingsHandler.getDataCollectionFrequency()));
		}
	};
	
	/**
	 * Stop all listeners, active sensors, etc, and shut down.
	 * 
	 */
	public void shutDownService() {
		locationManager.stopGettingLocations();
		stopSelf();
	}
	
	/**
	 * Use HTTPS to send the observation group to the server
	 * 
	 * @param group
	 * @return
	 */
	public boolean sendCbObservationGroup(CbObservationGroup group) {
		// TODO: Implement
		log("send observation group");
		return false;
	}
	
	public void start() {
		log("CbService: Starting to collect data.");
		
		// use AlarmManager or postDelayed tasks
		// to periodically collect and send data
		// according to values in CbSettings.
		settingsHandler = new CbSettingsHandler();
	
		mHandler.postDelayed(mSubmitReading, 0);
	}
	
	@Override
	public void onDestroy() {
		log("on destory");
		super.onDestroy();
	}

	@Override
	public void onCreate() {
		log("on create");
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log("on start command");
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
	
	public CbDataCollector getDataCollector() {
		return dataCollector;
	}
	public void setDataCollector(CbDataCollector dataCollector) {
		this.dataCollector = dataCollector;
	}
	public CbSettingsHandler getSettingsHandler() {
		return settingsHandler;
	}
	public void setSettingsHandler(CbSettingsHandler settingsHandler) {
		this.settingsHandler = settingsHandler;
	}
	public CbLocationManager getLocationManager() {
		return locationManager;
	}
	public void setLocationManager(CbLocationManager locationManager) {
		this.locationManager = locationManager;
	}

}
