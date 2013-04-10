package ca.cumulonimbus.pressurenetsdk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings.Secure;

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
	private CbLocationManager locationManager;

	private CbDb db;
	
	private String mAppDir;
	
	private final Handler mHandler = new Handler();

	/**
	 * Find all the data for an observation.
	 * 
	 * Location, Measurement values, etc.
	 * 
	 * @return
	 */
	public CbObservation collectNewObservation() {
		CbObservation pressureObservation = new CbObservation();		
		log("cb collecting new observation");
		
		// Location values
		locationManager = new CbLocationManager(getApplicationContext());
		locationManager.startGettingLocations();
		
		// Measurement values
		dataCollector = new CbDataCollector(getID());
		pressureObservation = dataCollector.getPressureObservation();
		pressureObservation.setLocation(locationManager.getCurrentBestLocation());
		
		// stop listening for locations
		locationManager.stopGettingLocations();
		
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
	private class ReadingSender implements Runnable {
		private CbSettingsHandler singleAppSettings;
		
		public ReadingSender(CbSettingsHandler settings) {
			this.singleAppSettings = settings;
		}
		
		public void run() {
			log("collecting and submitting " + singleAppSettings.getServerURL());
			long base = SystemClock.uptimeMillis();
			
			CbObservation singleObservation = new CbObservation();
			
			if(singleAppSettings.isCollectingData()) {
				// Collect
				singleObservation = collectNewObservation();
				log("lat" + singleObservation.getLocation().getLatitude());
				if(singleAppSettings.isSharingData()) {
					// Send
					sendCbObservation(singleObservation, singleAppSettings);
				}
			}
			mHandler.postAtTime(this, base + (singleAppSettings.getDataCollectionFrequency()));
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
	public boolean sendCbObservation(CbObservation observation, CbSettingsHandler settings) {
		CbDataSender sender = new CbDataSender(getApplicationContext());
		sender.setSettings(settings,locationManager);
		sender.execute(observation.getObservationAsParams());
		return true;
	}
	
	/**
	 * Start the periodic data collection.
	 */
	public void start(CbSettingsHandler settings) {
		log("CbService: Starting to collect data.");
		//mHandler.postDelayed(mSubmitReading, 0);
		//Thread cbThread = new Thread(mSubmitReading);
		ReadingSender sender = new ReadingSender(settings);
		mHandler.post(sender);
	}
	
	@Override
	public void onDestroy() {
		log("on destroy");
		shutDownService();
		super.onDestroy();
	}

	@Override
	public void onCreate() {
		setUpFiles();
		log("cb on create");
		
		db = new CbDb(getApplicationContext());
		super.onCreate();
	}

	/**
	 * Start running background data collection methods.
	 * 
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log("cb onstartcommand");
		CbSettingsHandler settings = new CbSettingsHandler(getApplicationContext());
		// Check the intent for Settings initialization
		
		if (intent.hasExtra("serverURL")) {
			log( "intent url " + intent.getExtras().getString("serverURL"));
			settings.setServerURL(intent.getStringExtra("serverURL"));

			// Seems like new settings. Try adding to the db.
			settings.saveSettings();
			
			// Start a new thread and return
			start(settings);
			return START_STICKY;
		} else {
			// Check the database for Settings initialization
			db.open();
			//db.clearDb();
			Cursor allSettings = db.fetchAllSettings();
			log("cb intent null; checking db, size " + allSettings.getCount());
			while(allSettings.moveToNext()) {
				settings.setAppID(allSettings.getString(1));
				settings.setDataCollectionFrequency(allSettings.getLong(2));
				start(settings);
				// but just once
				break;
			}
			db.close();
		}
		super.onStartCommand(intent, flags, startId);
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

    // Used to write a log to SD card. Not used unless logging enabled.
    public void setUpFiles() {
    	try {
	    	File homeDirectory = getExternalFilesDir(null);
	    	if(homeDirectory!=null) {
	    		mAppDir = homeDirectory.getAbsolutePath();
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
   
	// Log data to SD card for debug purposes.
	// To enable logging, ensure the Manifest allows writing to SD card.
	public void logToFile(String text) {
		try {
			OutputStream output = new FileOutputStream(mAppDir + "/log.txt", true);
			String logString = (new Date()).toString() + ": " + text + "\n";
			output.write(logString.getBytes());
			output.close();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		System.out.println("on bind");
		return null;
	}

	public void log(String message) {
		logToFile(message);
		//System.out.println(message);
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
	public CbLocationManager getLocationManager() {
		return locationManager;
	}
	public void setLocationManager(CbLocationManager locationManager) {
		this.locationManager = locationManager;
	}
}
