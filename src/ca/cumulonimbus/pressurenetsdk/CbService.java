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
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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
public class CbService extends Service  {
	
	private CbDataCollector dataCollector;
	private CbLocationManager locationManager;

	private CbDb db;
	
	private String mAppDir;
	
	IBinder mBinder;
	
	ReadingSender sender;
	
	// Service Interaction API Messages
	public static final int MSG_STOP = 1;
	public static final int MSG_GET_BEST_LOCATION = 2;
	public static final int MSG_BEST_LOCATION= 3;	
	public static final int MSG_GET_BEST_PRESSURE = 4;
	public static final int MSG_BEST_PRESSURE = 5;
	// In progress: 
	public static final int MSG_START_AUTOSUBMIT = 6;
	public static final int MSG_STOP_AUTOSUBMIT = 7;
	// TODO: Implement the following messages
	public static final int MSG_SET_SETTINGS = 8;
	public static final int MSG_GET_SETTINGS = 9;
	
	
	private final Handler mHandler = new Handler();
	Messenger mMessenger = new Messenger(new IncomingHandler());
	
	/**
	 * Find all the data for an observation.
	 * 
	 * Location, Measurement values, etc.
	 * 
	 * @return
	 */
	public CbObservation collectNewObservation() {
		try {
			CbObservation pressureObservation = new CbObservation();		
			log("cb collecting new observation");
			
			// Location values
			locationManager = new CbLocationManager(getApplicationContext());
			locationManager.startGettingLocations();
			
			// Measurement values
			dataCollector = new CbDataCollector(getID(), getApplicationContext());
			pressureObservation = dataCollector.getPressureObservation();
			pressureObservation.setLocation(locationManager.getCurrentBestLocation());
			
			// stop listening for locations
			locationManager.stopGettingLocations();
			return pressureObservation;
		} catch(Exception e) {
			e.printStackTrace();
			return new CbObservation();
		}
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
		dataCollector = new CbDataCollector(getID(), getApplicationContext());
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
				log("lat" + singleObservation.getLocation().getLatitude() + ", pressure " + singleObservation.getObservationValue() + singleObservation.getObservationUnit());
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
	public void stopAutoSubmit() {
		if(locationManager!=null) {
			locationManager.stopGettingLocations();
		}
		if(dataCollector  != null) {
			dataCollector.stopCollectingPressure();
		}
		mHandler.removeCallbacks(sender);
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
		try {
			CbDataSender sender = new CbDataSender(getApplicationContext());
			sender.setSettings(settings,locationManager);
			sender.execute(observation.getObservationAsParams());
			return true;
		}catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Send a new account to the server
	 * 
	 * @param group
	 * @return
	 */
	public boolean sendCbAccount(CbAccount account, CbSettingsHandler settings) {
		try {
			CbDataSender sender = new CbDataSender(getApplicationContext());
			sender.setSettings(settings,locationManager);
			sender.execute(account.getAccountAsParams());
			return true;
		}catch(Exception e) {
			return false;
		}
	}
	
	/**
	 * Start the periodic data collection.
	 */
	public void startAutoSubmit(CbSettingsHandler settings) {
		log("CbService: Starting to auto-collect and submit data.");
		
		sender = new ReadingSender(settings);
		mHandler.post(sender);
		
	}
	
	@Override
	public void onDestroy() {
		log("on destroy");
		stopAutoSubmit();
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
		
		// Check the intent for Settings initialization
	
		if (intent != null) {
			if(intent.hasExtra("serverURL")) {
				startWithIntent(intent);
			} else {
				startWithDatabase();
			}
			return START_STICKY;
		} else {
			log("INTENT NULL; checking db");
			startWithDatabase();
		}
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}
	
	public void startWithIntent(Intent intent) {
		try {
			log( "intent url " + intent.getExtras().getString("serverURL"));
			CbSettingsHandler settings = new CbSettingsHandler(getApplicationContext());
			
			settings.setServerURL(intent.getStringExtra("serverURL"));
			settings.setAppID(getID());

			// Seems like new settings. Try adding to the db.
			settings.saveSettings();
			
			// are we creating a new user?
			if (intent.hasExtra("add_account")) {
				log("adding new user");
				CbAccount account = new CbAccount();
				account.setEmail(intent.getStringExtra("email"));
				account.setTimeRegistered(intent.getLongExtra("time", 0));
				account.setUserID(intent.getStringExtra("userID"));
				sendCbAccount(account, settings);
			}
			
			// Start a new thread and return
			startAutoSubmit(settings);
		} catch(Exception e) {
			for (StackTraceElement ste : e.getStackTrace()) {
				log(ste.getMethodName() + ste.getLineNumber());
			}
		}
	}
	
	public void startWithDatabase() {
		try {
			db.open();
			// Check the database for Settings initialization
			CbSettingsHandler settings = new CbSettingsHandler(getApplicationContext());
			//db.clearDb();
			Cursor allSettings = db.fetchAllSettings();
			log("cb intent null; checking db, size " + allSettings.getCount());
			while(allSettings.moveToNext()) {
				settings.setAppID(allSettings.getString(1));
				settings.setDataCollectionFrequency(allSettings.getLong(2));
				settings.setServerURL(allSettings.getString(3));
				startAutoSubmit(settings);
				// but just once
				break;
			}
			db.close();
		} catch(Exception e)  {
			for (StackTraceElement ste : e.getStackTrace()) {
				log(ste.getMethodName() + ste.getLineNumber());
			}
		}
	}
	
	 /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_STOP:
                	log("message. bound service says stop");
                	stopAutoSubmit();
                    break;
                case MSG_GET_BEST_LOCATION:
                	log("message. bound service requesting location");
                	if(locationManager!=null) {
                		Location best = locationManager.getCurrentBestLocation();
                		try {
                			log("service sending best location");
                			msg.replyTo.send(Message.obtain(null, MSG_BEST_LOCATION, best));
                		} catch(RemoteException re) {
                			re.printStackTrace();
                		}
                	} else {
                		log("error: location null, not returning");
                	}
                	break;
                case MSG_GET_BEST_PRESSURE:
                	log("message. bound service requesting pressure");
                	if(dataCollector!=null) {
                		CbObservation pressure = dataCollector.getPressureObservation();
                		try {
                			log("service sending best pressure");
                			msg.replyTo.send(Message.obtain(null, MSG_BEST_PRESSURE, pressure));
                		} catch(RemoteException re) {
                			re.printStackTrace();
                		}
                	} else {
                		log("error: data collector null, not returning");
                	}
                	break;
                case MSG_START_AUTOSUBMIT:
                	log("start autosubmit");
                	startWithDatabase();
                	break;
                case MSG_STOP_AUTOSUBMIT:
                	log("stop autosubmit");
                	stopAutoSubmit();
                	break;
                default:
                    super.handleMessage(msg);
            }
        }
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
		log("on bind");
       return mMessenger.getBinder();
		
	}	
	@Override
	public void onRebind(Intent intent) {
		log("on rebind");
		super.onRebind(intent);
	}

	public void log(String message) {
		//logToFile(message);
		System.out.println(message);
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
