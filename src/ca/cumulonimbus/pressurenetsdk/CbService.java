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
	public CbService service = this;

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

	// pressureNET Live API
	public static final int MSG_GET_LOCAL_RECENTS = 14;
	public static final int MSG_LOCAL_RECENTS = 15;
	public static final int MSG_GET_API_RECENTS = 16;
	public static final int MSG_API_RECENTS = 17;
	public static final int MSG_MAKE_API_CALL = 18;
	public static final int MSG_API_RESULT_COUNT = 19;
	
	// pressureNET API Cache
	public static final int MSG_CLEAR_LOCAL_CACHE = 20;
	public static final int MSG_REMOVE_FROM_PRESSURENET = 21;
	public static final int MSG_CLEAR_API_CACHE = 22;
	
	// Current Conditions
	public static final int MSG_ADD_CURRENT_CONDITION = 23;	
	public static final int MSG_GET_CURRENT_CONDITIONS = 24;
	public static final int MSG_CURRENT_CONDITIONS = 25;

	// Sending Data
	public static final int MSG_SEND_OBSERVATION = 26;
	public static final int MSG_SEND_CURRENT_CONDITION = 27;

	
	
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
	 * Collect and send data in a different thread. This runs itself every
	 * "settingsHandler.getDataCollectionFrequency()" milliseconds
	 */
	private class ReadingSender implements Runnable {

		public void run() {
			log("collecting and submitting " + settingsHandler.getServerURL());
			long base = SystemClock.uptimeMillis();

			dataCollector.startCollectingData(null);

			CbObservation singleObservation = new CbObservation();

			if (settingsHandler.isCollectingData()) {
				// Collect
				singleObservation = collectNewObservation();
				log("collected");
				
				if (singleObservation.getObservationValue() != 0.0) {
					// Store in database
					db.open();
					long count = db.addObservation(singleObservation);
					db.close();

					try {
						if (settingsHandler.isSharingData()) {
							// Send if we're online
							if (isNetworkAvailable()) {
								log("online and sending");
								singleObservation.setClientKey(getApplicationContext().getPackageName());
								sendCbObservation(singleObservation);
							} else {
								log("didn't send");
								// TODO: and store for later if not
							}
						}
					} catch (Exception e) {
						e.printStackTrace();

					}
				}
			}
			mHandler.postAtTime(this,
					base + (settingsHandler.getDataCollectionFrequency()));
		}
	};

	public boolean isNetworkAvailable() {
		log("is net available?");
		ConnectivityManager cm = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		// test for connection
		if (cm.getActiveNetworkInfo() != null
				&& cm.getActiveNetworkInfo().isAvailable()
				&& cm.getActiveNetworkInfo().isConnected()) {
			log("yes");
			return true;
		} else {
			log("no");
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
	 * Send the observation to the server
	 * 
	 * @param observation
	 * @return
	 */
	public boolean sendCbObservation(CbObservation observation) {
		try {
			CbDataSender sender = new CbDataSender(getApplicationContext());
			sender.setSettings(settingsHandler, locationManager, dataCollector );
			sender.execute(observation.getObservationAsParams());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	
	/**
	 * Send a new account to the server
	 * 
	 * @param account
	 * @return
	 */
	public boolean sendCbAccount(CbAccount account) {
		
		try {
			CbDataSender sender = new CbDataSender(getApplicationContext());
			sender.setSettings(settingsHandler, locationManager, dataCollector);
			sender.execute(account.getAccountAsParams());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Send the current condition to the server
	 * 
	 * @param observation
	 * @return
	 */
	public boolean sendCbCurrentCondition(CbCurrentCondition condition) {
		log("sending cbcurrent condition");
		try {
			CbDataSender sender = new CbDataSender(getApplicationContext());
			sender.setSettings(settingsHandler, locationManager, dataCollector );
			sender.execute(condition.getCurrentConditionAsParams());
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
			case MSG_GET_LOCAL_RECENTS:
				log("get recents");
				CbApiCall apiCall = (CbApiCall) msg.obj;

				// run API call
				db.open();
				Cursor cursor = db.runLocalAPICall(apiCall.getMinLat(),
						apiCall.getMaxLat(), apiCall.getMinLon(),
						apiCall.getMaxLon(), apiCall.getStartTime(),
						apiCall.getEndTime(), 2000);
				ArrayList<CbObservation> results = new ArrayList<CbObservation>();
				while (cursor.moveToNext()) {
					// TODO: This is duplicated in CbDataCollector. Fix that
					CbObservation obs = new CbObservation();
					Location location = new Location("network");
					location.setLatitude(cursor.getDouble(1));
					location.setLongitude(cursor.getDouble(2));
					location.setAltitude(cursor.getDouble(3));
					location.setAccuracy(cursor.getInt(4));
					location.setProvider(cursor.getString(5));
					obs.setLocation(location);
					obs.setObservationType(cursor.getString(6));
					obs.setObservationUnit(cursor.getString(7));
					obs.setObservationValue(cursor.getDouble(8));
					obs.setSharing(cursor.getString(9));
					obs.setTime(cursor.getInt(10));
					obs.setTimeZoneOffset(cursor.getInt(11));
					obs.setUser_id(cursor.getString(12));

					// TODO: Add sensor information

					results.add(obs);
				}

				log("cbservice: " + results.size() + " local api results");
				try {
					msg.replyTo.send(Message.obtain(null, MSG_LOCAL_RECENTS,
							results));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
				break;
			case MSG_GET_API_RECENTS:
				log("get api recents");
				CbApiCall apiCacheCall = (CbApiCall) msg.obj;
				log(apiCacheCall.toString());
				// run API call
				db.open();
				
				Cursor cacheCursor = db.runAPICacheCall(
						apiCacheCall.getMinLat(), apiCacheCall.getMaxLat(),
						apiCacheCall.getMinLon(), apiCacheCall.getMaxLon(),
						apiCacheCall.getStartTime(), apiCacheCall.getEndTime(),
						2000);
				ArrayList<CbObservation> cacheResults = new ArrayList<CbObservation>();
				System.out.println("cache cursor count " + cacheCursor.getCount());
				while (cacheCursor.moveToNext()) {
					// TODO: This is duplicated in CbDataCollector. Fix that
					CbObservation obs = new CbObservation();
					Location location = new Location("network");
					location.setLatitude(cacheCursor.getDouble(1));
					location.setLongitude(cacheCursor.getDouble(2));
					location.setAltitude(cacheCursor.getDouble(3));
					location.setAccuracy(cacheCursor.getInt(4));
					location.setProvider(cacheCursor.getString(5));
					obs.setLocation(location);
					obs.setObservationType(cacheCursor.getString(6));
					obs.setObservationUnit(cacheCursor.getString(7));
					obs.setObservationValue(cacheCursor.getDouble(8));
					obs.setSharing(cacheCursor.getString(9));
					obs.setTime(cacheCursor.getLong(10));
					obs.setTimeZoneOffset(cacheCursor.getLong(11));
					obs.setUser_id(cacheCursor.getString(12));

					// TODO: Add sensor information

					cacheResults.add(obs);
				}

				try {
					msg.replyTo.send(Message.obtain(null, MSG_API_RECENTS,
							cacheResults));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
				break;
			case MSG_MAKE_API_CALL:
				CbApi api = new CbApi(getApplicationContext());
				CbApiCall liveApiCall = (CbApiCall) msg.obj;
				api.makeAPICall(liveApiCall, service, msg.replyTo);

				break;
			case MSG_CLEAR_LOCAL_CACHE:
				db.open();
				db.clearLocalCache();
				db.close();
				break;
			case MSG_REMOVE_FROM_PRESSURENET:
				// TODO: Implement
				break;
			case MSG_CLEAR_API_CACHE:
				db.open();
				db.clearAPICache();
				db.close();
				break;
			case MSG_ADD_CURRENT_CONDITION:
				CbCurrentCondition cc = (CbCurrentCondition) msg.obj;
				db.open();
				db.addCondition(cc);
				db.close();
				break;
			case MSG_GET_CURRENT_CONDITIONS:
				db.open();
				CbApiCall currentConditionAPI = (CbApiCall) msg.obj;

				Cursor ccCursor = db.getCurrentConditions(
						currentConditionAPI.getMinLat(), 
						currentConditionAPI.getMaxLat(), 
						currentConditionAPI.getMinLon(),
						currentConditionAPI.getMaxLon(),
						currentConditionAPI.getStartTime(),
						currentConditionAPI.getEndTime(),
						1000);
				
				ArrayList<CbCurrentCondition> conditions = new ArrayList<CbCurrentCondition>();
				while(ccCursor.moveToNext()) {
					CbCurrentCondition cur = new CbCurrentCondition();
					Location location = new Location("network");
					location.setLatitude(ccCursor.getDouble(1));
					location.setLongitude(ccCursor.getDouble(2));
					location.setAltitude(ccCursor.getDouble(3));
					location.setAccuracy(ccCursor.getInt(4));
					location.setProvider(ccCursor.getString(5));
					cur.setLocation(location);
					cur.setTime(ccCursor.getLong(6));
					cur.setTime(ccCursor.getLong(7));
					cur.setUser_id(ccCursor.getString(9));
					cur.setGeneral_condition(ccCursor.getString(10));
					cur.setWindy(ccCursor.getString(11));
					cur.setFog_thickness(ccCursor.getString(12));
					cur.setCloud_type(ccCursor.getString(13));
					cur.setPrecipitation_type(ccCursor.getString(14));
					cur.setPrecipitation_amount(ccCursor.getDouble(15));
					cur.setPrecipitation_unit(ccCursor.getString(16));
					cur.setThunderstorm_intensity(ccCursor.getString(17));
					cur.setUser_comment(ccCursor.getString(18));
					conditions.add(cur);
				}
				db.close();
				
				try {
					msg.replyTo.send(Message.obtain(null, MSG_CURRENT_CONDITIONS,
							conditions));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
				break;
			case MSG_SEND_CURRENT_CONDITION:
				CbCurrentCondition condition = (CbCurrentCondition) msg.obj;
				sendCbCurrentCondition(condition);
				break;
			case MSG_SEND_OBSERVATION:
				// TODO: Implement
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	public boolean notifyAPIResult(Messenger reply, int count) {
		try {
			if (reply == null) {
				System.out.println("cannot notify, reply is null");
			} else {
				reply.send(Message.obtain(null, MSG_API_RESULT_COUNT,
						count, 0));
			}

		} catch (RemoteException re) {
			re.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}
		return false;
	}

	public CbObservation recentPressureFromDatabase() {
		CbObservation obs = new CbObservation();
		long rowId = db.fetchObservationMaxID();
		double pressure = 0.0;

		Cursor c = db.fetchObservation(rowId);

		while (c.moveToNext()) {
			pressure = c.getDouble(8);
		}
		log(pressure + " pressure from db");
		if (pressure == 0.0) {
			log("returning null");
			return null;
		}
		obs.setObservationValue(pressure);
		return obs;
	}

	private class StreamObservation extends AsyncTask<Messenger, Void, String> {

		@Override
		protected String doInBackground(Messenger... m) {
			try {
				for (Messenger msgr : m) {
					if (msgr != null) {
						msgr.send(Message.obtain(null, MSG_DATA_STREAM,
								recentPressureFromDatabase()));
					} else {
						log("messenger is null");
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
		log("cbService starting stream " + (m == null));
		dataCollector.startCollectingData(m);
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
