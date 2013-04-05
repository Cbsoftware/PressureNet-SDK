package ca.cumulonimbus.pressurenetsdk;

import java.security.MessageDigest;
import java.util.ArrayList;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings.Secure;
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
	 * Find all the data for an observation.
	 * 
	 * Location, Measurement values, etc.
	 * 
	 * @return
	 */
	public CbObservation collectNewObservation() {
		CbObservation pressureObservation = new CbObservation();		
		
		// Location values
		locationManager = new CbLocationManager(this);
		locationManager.startGettingLocations();
		
		// Measurement values
		dataCollector = new CbDataCollector(getID());
		pressureObservation = dataCollector.getPressureObservation();
		pressureObservation.setLocation(locationManager.getCurrentBestLocation());
		
		// stop listening for locations
		locationManager.startGettingLocations();
		
		return pressureObservation;
	}
	
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
		dataCollector = new CbDataCollector(getID());
		pressureObservation = dataCollector.getPressureObservation();
		
		// Put everything together
		observations.add(pressureObservation);
		CbObservationGroup newGroup = new CbObservationGroup();
		newGroup.setGroup(observations);
		return newGroup;
	}

	/**
	 * Collect and send data in a different thread.
	 * This runs itself every "settingsHandler.getDataCollectionFrequency()" milliseconds
	 */
	private Runnable mSubmitReading = new Runnable() {
		public void run() {
			log("collecting and submitting");
			long base = SystemClock.uptimeMillis();
			
			CbObservation singleObservation = new CbObservation();
			
			// Collect observations
			if(settingsHandler.isCollectingData()) {
				singleObservation = collectNewObservation();
				// Send
				if(settingsHandler.isSharingData()) {
					log("preferences okay. send cbobvsgr ");
					sendCbObservation(singleObservation);
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
	 * Send the observation group to the server
	 * 
	 * @param group
	 * @return
	 */
	public boolean sendCbObservationGroup(CbObservationGroup group) {
		// TODO: Implement
		log("send observation group");
		return false;
	}
	
	/**
	 * Send the observation to the server
	 * 
	 * @param group
	 * @return
	 */
	public boolean sendCbObservation(CbObservation observation) {
		CbDataSender sender = new CbDataSender();
		sender.setSettings(settingsHandler);
		sender.execute(observation.getObservationAsParams());
		return true;
	}
	
	/**
	 * Start the periodic data collection.
	 */
	public void start() {
		log("CbService: Starting to collect data.");
		mHandler.postDelayed(mSubmitReading, 0);
	}
	
	@Override
	public void onDestroy() {
		log("on destroy");
		shutDownService();
		super.onDestroy();
	}

	@Override
	public void onCreate() {
		log("on create");
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		// Use the intent to initialize Settings
		settingsHandler = new CbSettingsHandler();
		settingsHandler.setServerURL(intent.getStringExtra("serverURL"));
		
		log("on start command");
		start();
		
		return START_STICKY;
	}

	/**
	 * Get a hash'd device ID
	 * 
	 * @return
	 */
	public String getID() {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");

			String actual_id = Secure.getString(getApplicationContext()
					.getContentResolver(), Secure.ANDROID_ID);
			byte[] bytes = actual_id.getBytes();
			byte[] digest = md.digest(bytes);
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < digest.length; i++) {
				hexString.append(Integer.toHexString(0xFF & digest[i]));
			}
			return hexString.toString();
		} catch (Exception e) {
			return "--";
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		System.out.println("on bind");
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
