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
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings.Secure;
import android.widget.TextView;

/**
 * Represent developer-facing pressureNET API Background task; manage and run
 * everything Handle Intents
 * 
 * @author jacob
 * 
 */
public class CbService extends Service {

	private CbDataCollector dataCollector;
	private CbLocationManager locationManager;
	private CbSettingsHandler settingsHandler;
	private CbDb db;

	ArrayList<CbObservation> recentObservations = new ArrayList<CbObservation>();

	private String mAppDir;

	IBinder mBinder;

	ReadingSender sender;

	// Service Interaction API Messages
	public static final int MSG_OKAY = 0;
	public static final int MSG_STOP = 1;
	public static final int MSG_GET_BEST_LOCATION = 2;
	public static final int MSG_BEST_LOCATION = 3;
	public static final int MSG_GET_BEST_PRESSURE = 4;
	public static final int MSG_BEST_PRESSURE = 5;
	public static final int MSG_START_AUTOSUBMIT = 6;
	public static final int MSG_STOP_AUTOSUBMIT = 7;
	public static final int MSG_SET_SETTINGS = 8;
	public static final int MSG_GET_SETTINGS = 9;
	public static final int MSG_SETTINGS = 10;

	public static final int MSG_START_DATA_STREAM = 11;
	public static final int MSG_DATA_STREAM = 12;	
	public static final int MSG_STOP_DATA_STREAM = 13;

	public static final int MSG_GET_RECENTS = 14;
	public static final int MSG_RECENTS = 15;

	private final Handler mHandler = new Handler();
	Messenger mMessenger = new Messenger(new IncomingHandler());

	private CbService thisService = this;

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
			pressureObservation = dataCollector.getPressureObservation();
			pressureObservation.setLocation(locationManager
					.getCurrentBestLocation());

			// stop listening for locations
			locationManager.stopGettingLocations();
			return pressureObservation;
		} catch (Exception e) {
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
	/*
	 * public CbObservationGroup collectNewObservationGroup() {
	 * ArrayList<CbObservation> observations = new ArrayList<CbObservation>();
	 * CbObservation pressureObservation = new CbObservation();
	 * 
	 * // Location values locationManager = new CbLocationManager(this);
	 * locationManager.startGettingLocations();
	 * 
	 * // Measurement values dataCollector = new CbDataCollector(getID(),
	 * getApplicationContext()); pressureObservation =
	 * dataCollector.getPressureObservation();
	 * 
	 * // Put everything together observations.add(pressureObservation);
	 * CbObservationGroup newGroup = new CbObservationGroup();
	 * newGroup.setGroup(observations); return newGroup; }
	 */

	/**
	 * Collect and send data in a different thread. This runs itself every
	 * "settingsHandler.getDataCollectionFrequency()" milliseconds
	 */
	private class ReadingSender implements Runnable {

		public void run() {
			log("collecting and submitting " + settingsHandler.getServerURL());
			long base = SystemClock.uptimeMillis();

			dataCollector.startCollectingData(thisService);

			CbObservation singleObservation = new CbObservation();

			if (settingsHandler.isCollectingData()) {
				// Collect
				singleObservation = collectNewObservation();
				// Store in memory buffer
				// TODO: Careful of double adding with data collector
				// recentObservations.add(singleObservation);
				
				// Store in database
				db.open();
				db.addObservation(singleObservation);
				db.close();
				log("lat" + singleObservation.getLocation().getLatitude()
						+ ", pressure "
						+ singleObservation.getObservationValue()
						+ singleObservation.getObservationUnit());
				if (settingsHandler.isSharingData()) {
					// Send if we're online
					if (isNetworkAvailable()) {
						sendCbObservation(singleObservation);
					} else {
						// TODO: and store for later if not
					}
				}
			}
			mHandler.postAtTime(this,
					base + (settingsHandler.getDataCollectionFrequency()));
		}
	};

	public boolean isNetworkAvailable() {
		ConnectivityManager cm = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		// test for connection
		if (cm.getActiveNetworkInfo() != null
				&& cm.getActiveNetworkInfo().isAvailable()
				&& cm.getActiveNetworkInfo().isConnected()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Stop all listeners, active sensors, etc, and shut down.
	 * 
	 */
	public void stopAutoSubmit() {
		if (locationManager != null) {
			locationManager.stopGettingLocations();
		}
		if (dataCollector != null) {
			dataCollector.stopCollectingData();
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
	public boolean sendCbObservation(CbObservation observation) {
		try {
			CbDataSender sender = new CbDataSender(getApplicationContext());
			sender.setSettings(settingsHandler, locationManager);
			sender.execute(observation.getObservationAsParams());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Send a new account to the server
	 * 
	 * @param group
	 * @return
	 */
	public boolean sendCbAccount(CbAccount account) {
		try {
			CbDataSender sender = new CbDataSender(getApplicationContext());
			sender.setSettings(settingsHandler, locationManager);
			sender.execute(account.getAccountAsParams());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Start the periodic data collection.
	 */
	public void startAutoSubmit() {
		log("CbService: Starting to auto-collect and submit data.");

		sender = new ReadingSender();
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
		dataCollector = new CbDataCollector(getID(), getApplicationContext());

		if (intent != null) {
			if (intent.hasExtra("serverURL")) {
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
			log("intent url " + intent.getExtras().getString("serverURL"));
			settingsHandler = new CbSettingsHandler(getApplicationContext());

			settingsHandler.setServerURL(intent.getStringExtra("serverURL"));
			settingsHandler.setAppID(getID());

			// Seems like new settings. Try adding to the db.
			settingsHandler.saveSettings();

			// are we creating a new user?
			if (intent.hasExtra("add_account")) {
				log("adding new user");
				CbAccount account = new CbAccount();
				account.setEmail(intent.getStringExtra("email"));
				account.setTimeRegistered(intent.getLongExtra("time", 0));
				account.setUserID(intent.getStringExtra("userID"));
				sendCbAccount(account);
			}

			// Start a new thread and return
			startAutoSubmit();
		} catch (Exception e) {
			for (StackTraceElement ste : e.getStackTrace()) {
				log(ste.getMethodName() + ste.getLineNumber());
			}
		}
	}

	public void startWithDatabase() {
		try {
			db.open();
			// Check the database for Settings initialization
			settingsHandler = new CbSettingsHandler(getApplicationContext());
			// db.clearDb();
			Cursor allSettings = db.fetchAllSettings();
			log("cb intent null; checking db, size " + allSettings.getCount());
			while (allSettings.moveToNext()) {
				settingsHandler.setAppID(allSettings.getString(1));
				settingsHandler.setDataCollectionFrequency(allSettings
						.getLong(2));
				settingsHandler.setServerURL(allSettings.getString(3));
				startAutoSubmit();
				// but just once
				break;
			}
			db.close();
		} catch (Exception e) {
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
				if (locationManager != null) {
					Location best = locationManager.getCurrentBestLocation();
					try {
						log("service sending best location");
						msg.replyTo.send(Message.obtain(null,
								MSG_BEST_LOCATION, best));
					} catch (RemoteException re) {
						re.printStackTrace();
					}
				} else {
					log("error: location null, not returning");
				}
				break;
			case MSG_GET_BEST_PRESSURE:
				log("message. bound service requesting pressure");
				if (dataCollector != null) {
					CbObservation pressure = dataCollector
							.getPressureObservation();
					try {
						log("service sending best pressure");
						msg.replyTo.send(Message.obtain(null,
								MSG_BEST_PRESSURE, pressure));
					} catch (RemoteException re) {
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
			case MSG_GET_SETTINGS:
				log("get settings");
				try {
					msg.replyTo.send(Message.obtain(null, MSG_SETTINGS,
							settingsHandler));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
				break;
			case MSG_START_DATA_STREAM:
				startDataStream(msg.replyTo);
				break;
			case MSG_STOP_DATA_STREAM:
				stopDataStream();
				break;
			case MSG_SET_SETTINGS:
				log("set settings");
				CbSettingsHandler newSettings = (CbSettingsHandler) msg.obj;
				newSettings.saveSettings();
				break;
			case MSG_GET_RECENTS:
				log("get recents");
				try {
					msg.replyTo.send(Message.obtain(null, MSG_RECENTS,
							recentObservations));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
			default:
				super.handleMessage(msg);
			}
		}
	}

	private class StreamObservation extends AsyncTask<Messenger, Void, String> {

		@Override
		protected String doInBackground(Messenger... m) {
				try {
					for(Messenger msgr : m) {
						if(msgr!=null) {
							msgr.send(Message.obtain(null, MSG_DATA_STREAM,recentObservations.get(recentObservations.size()-1)
								));
						}
					}
				} catch (RemoteException re) {
					re.printStackTrace();
				}
			
			return "--";
		}

		@Override
		protected void onPostExecute(String result) {
			
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected void onProgressUpdate(Void... values) {
		}
	}

	public void startDataStream(Messenger m) {
		log("cbService starting stream");
		dataCollector.startCollectingData(this);
		new StreamObservation().execute(m);
	}

	public void stopDataStream() {
		log("cbservice stopping stream");
		dataCollector.stopCollectingData();
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
			if (homeDirectory != null) {
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
			OutputStream output = new FileOutputStream(mAppDir + "/log.txt",
					true);
			String logString = (new Date()).toString() + ": " + text + "\n";
			output.write(logString.getBytes());
			output.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException ioe) {
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
		// logToFile(message);
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
