package ca.cumulonimbus.pressurenetsdk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;

/**
 * Represent developer-facing pressureNET API Background task; manage and run
 * everything Handle Intents
 * 
 * @author jacob
 * 
 */
public class CbService extends Service  {

	private CbDataCollector dataCollector;
	private CbLocationManager locationManager;
	private CbSettingsHandler settingsHandler;
	private CbDb db;
	public CbService service = this;

	private String mAppDir;

	IBinder mBinder;

	ReadingSender sender;

	Message recentMsg;

	String serverURL = "https://pressurenet.cumulonimbus.ca/";

	public static String ACTION_SEND_MEASUREMENT = "SendMeasurement";

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
	// Current Conditions API
	public static final int MSG_MAKE_CURRENT_CONDITIONS_API_CALL = 28;
	// Notifications
	public static final int MSG_CHANGE_NOTIFICATION = 31;
	// Data management
	public static final int MSG_COUNT_LOCAL_OBS = 32;
	public static final int MSG_COUNT_API_CACHE = 33;
	public static final int MSG_COUNT_LOCAL_OBS_TOTALS = 34;
	public static final int MSG_COUNT_API_CACHE_TOTALS = 35;
	// Graphing
	public static final int MSG_GET_API_RECENTS_FOR_GRAPH = 36;
	public static final int MSG_API_RECENTS_FOR_GRAPH = 37;
	// Success / Failure notification for data submission
	public static final int MSG_DATA_RESULT = 38;
	
	long lastAPICall = System.currentTimeMillis();

	private CbObservation collectedObservation;
	
	private final Handler mHandler = new Handler();

	Messenger mMessenger = new Messenger(new IncomingHandler());

	ArrayList<CbObservation> offlineBuffer = new ArrayList<CbObservation>();

	private long lastPressureChangeAlert = 0;
	
	private Messenger lastMessenger;
	
	private boolean fromUser = false;
	
	Alarm alarm = new Alarm();
	
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
			pressureObservation.setLocation(locationManager.getCurrentBestLocation());

			// stop listening for locations
			LocationStopper stop = new LocationStopper();
			mHandler.postDelayed(stop, 1000 * 10);

			log("returning pressure obs: " + pressureObservation.getObservationValue());
			
			return pressureObservation;

		} catch (Exception e) {
			log("cbservice CAUSING ALL ISSUES");
			e.printStackTrace();
			return null;
		}
	}
	
	private class LocationStopper implements Runnable  {

		@Override
		public void run() {
			try {
				locationManager.stopGettingLocations();
			} catch(Exception e) {
				
			}
		}
		
	}
	
	/**
	 * Send a single reading. 
	 * TODO: This is ugly copy+paste from the original ReadingSender. Fix that.
	 */
	
	public class SingleReadingSender implements Runnable {

		@Override
		public void run() {
			log("collecting and submitting single " + settingsHandler.getServerURL());

			dataCollector.startCollectingData(null);
			CbObservation singleObservation = new CbObservation();
			if (settingsHandler.isCollectingData()) {
				// Collect
				singleObservation = collectNewObservation();
				if (singleObservation.getObservationValue() != 0.0) {
					// Store in database
					db.open();
					long count = db.addObservation(singleObservation);

					db.close();

					try {
						if (settingsHandler.isSharingData()) {
							// Send if we're online
							if (isNetworkAvailable()) {
								log("online and sending single");
								singleObservation
										.setClientKey(getApplicationContext()
												.getPackageName());
								fromUser = true;
								sendCbObservation(singleObservation);
								fromUser = false;
								
								// also check and send the offline buffer
								if (offlineBuffer.size() > 0) {
									log("sending " + offlineBuffer.size() + " offline buffered obs");
									for (CbObservation singleOffline : offlineBuffer) {
										sendCbObservation(singleObservation);
									}
									offlineBuffer.clear();
								}
							} else {
								log("didn't send, not sharing data; i.e., offline");
								// / offline buffer variable
								// TODO: put this in the DB to survive longer
								offlineBuffer.add(singleObservation);

							}
						}
					} catch (Exception e) {
						e.printStackTrace();

					}
				}
			} 
		}
	}
	
	/**
	 * Collect and send data in a different thread. This runs itself every
	 * "settingsHandler.getDataCollectionFrequency()" milliseconds
	 */
	private class ReadingSender implements Runnable {

		public void run() {
			// retrieve updated settings
			settingsHandler = settingsHandler.getSettings();
			
			dataCollector.startCollectingData(null);
			
			log("collecting and submitting " + settingsHandler.getServerURL());
			
			boolean okayToGo = true;
			// Check if we're supposed to be charging and if we are.
			// Bail if appropriate
			if(settingsHandler.isOnlyWhenCharging()) {
				if(!isCharging()) {
					okayToGo = false;
				} 
			}
			
			if (okayToGo && settingsHandler.isCollectingData()) {
				// Collect
				CbObservation singleObservation = new CbObservation();
				singleObservation = collectNewObservation();
				if(singleObservation != null) {
					
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
									singleObservation
											.setClientKey(getApplicationContext()
													.getPackageName());
									sendCbObservation(singleObservation);
	
									// also check and send the offline buffer
									if (offlineBuffer.size() > 0) {
										log("sending " + offlineBuffer.size() + " offline buffered obs");
										for (CbObservation singleOffline : offlineBuffer) {
											sendCbObservation(singleObservation);
										}
										offlineBuffer.clear();
									}
								} else {
									log("didn't send");
									// / offline buffer variable
									// TODO: put this in the DB to survive longer
									offlineBuffer.add(singleObservation);
	
								}
							} else {
								log("cbservice not sharing data, didn't send");
							}
	
							// If notifications are enabled,
							log("is send notif " + settingsHandler.isSendNotifications());
							if (settingsHandler.isSendNotifications()) {
								// check for pressure local trend changes and notify
								// the client
	
								// ensure this only happens every once in a while
								long rightNow = System.currentTimeMillis();
								long sixHours = 1000 * 60 * 60 * 6;
								if (rightNow - lastPressureChangeAlert > (sixHours)) {
									long timeLength = 1000 * 60 * 60 * 3;
									db.open();
									Cursor localCursor = db.runLocalAPICall(-90,
											90, -180, 180,
											System.currentTimeMillis()
													- (timeLength),
											System.currentTimeMillis(), 1000);
									ArrayList<CbObservation> recents = new ArrayList<CbObservation>();
									while (localCursor.moveToNext()) {
										// just need observation value, time, and
										// location
										CbObservation obs = new CbObservation();
										obs.setObservationValue(localCursor.getDouble(8));
										obs.setTime(localCursor.getLong(10));
										Location location = new Location("network");
										location.setLatitude(localCursor
												.getDouble(1));
										location.setLongitude(localCursor
												.getDouble(2));
										
										obs.setLocation(location);
										recents.add(obs);
									}
									String tendencyChange = CbScience
											.changeInTrend(recents);
									db.close();
									
									log("cbservice tendency changes: " + tendencyChange);
									if (tendencyChange.contains(",")
											&& (!tendencyChange.toLowerCase()
													.contains("unknown"))) {
										String[] tendencies = tendencyChange
												.split(",");
										if (!tendencies[0].equals(tendencies[1])) {
											log("Trend change! " + tendencyChange);
	
											
											// TODO: send message to deliver
											// Android notification of tendency change
											try {
												if(lastMessenger!= null) {
													lastMessenger.send(Message.obtain(null,
																MSG_CHANGE_NOTIFICATION, tendencyChange));
												} else {
													log("readingsender didn't send notif, no lastMessenger");
												}
											} catch(Exception e) {
												e.printStackTrace();
											}
											lastPressureChangeAlert = rightNow;
										} else {
											log("tendency equal");
										
										}
									}
	
								} else {
									// wait
									log("tendency; hasn't been 6h, min wait time yet");
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
	
						}
					}
				} else {
					log("singleobservation is null, not sending");
				}
			} else {
				log("cbservice is not collecting data.");
			}
		}
	}

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
			sender.setSettings(settingsHandler, locationManager, dataCollector, lastMessenger, fromUser);
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
			sender.setSettings(settingsHandler, locationManager, dataCollector, null, true);
			sender.execute(account.getAccountAsParams());
			fromUser = false;
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
			fromUser = true;
			sender.setSettings(settingsHandler, locationManager, dataCollector, lastMessenger, fromUser);
			sender.execute(condition.getCurrentConditionAsParams());
			fromUser = false;
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
		
		alarm.setAlarm(getApplicationContext(), settingsHandler.getDataCollectionFrequency());
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
	 * Check charge state for preferences.
	 * TODO: In future, adjust our collection and submission frequency 
	 * based on battery level.
	 */
	public boolean isCharging() {
		// Check battery and charging status
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);

		// Are we charging / charged?
		int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
		                     status == BatteryManager.BATTERY_STATUS_FULL;

		// How are we charging?
		/*
		int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
		boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
		 */
		return isCharging;
	}
	
	/**
	 * Start running background data collection methods.
	 * 
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log("cb onstartcommand");
		
		if(intent!= null) {
			if(intent.getAction() != null ) {
				if(intent.getAction().equals(ACTION_SEND_MEASUREMENT)) {
					// send just a single measurement
					log("sending single observation, request from intent");
					sendSingleObs();
					return 0;
				}
			} else {
				if(intent.getBooleanExtra("alarm", false)) {
					log("cbservice from alarm. sending obs");
					ReadingSender reading = new ReadingSender();
					mHandler.post(reading);
					return 0;
				}
			}
		}
		
		
		// Check the intent for Settings initialization
		dataCollector = new CbDataCollector(getID(), getApplicationContext());
		log("starting service code, run count 0");
		if (intent != null) {

			startWithIntent(intent);

			return START_NOT_STICKY;
		} else {
			log("INTENT NULL; checking db");
			startWithDatabase();
		}
		super.onStartCommand(intent, flags, startId);
		return START_NOT_STICKY;
	}

	/**
	 * Convert time ago text to ms. TODO: not this. values in xml.
	 * 
	 * @param timeAgo
	 * @return
	 */
	public static long stringTimeToLongHack(String timeAgo) {
		if (timeAgo.equals("1 minute")) {
			return 1000 * 60;
		} else if (timeAgo.equals("5 minutes")) {
			return 1000 * 60 * 5;
		} else if (timeAgo.equals("10 minutes")) {
			return 1000 * 60 * 10;
		} else if (timeAgo.equals("30 minutes")) {
			return 1000 * 60 * 30;
		} else if (timeAgo.equals("1 hour")) {
			return 1000 * 60 * 60;
		}
		return 1000 * 60 * 10;
	}

	public void startWithIntent(Intent intent) {
		try {
			settingsHandler = new CbSettingsHandler(getApplicationContext());
			settingsHandler.setServerURL(serverURL);
			settingsHandler.setAppID("ca.cumulonimbus.barometernetwork");

			SharedPreferences sharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(this);
			String preferenceCollectionFrequency = sharedPreferences.getString(
					"autofrequency", "10 minutes");
			boolean preferenceShareData = sharedPreferences.getBoolean(
					"autoupdate", true);
			String preferenceShareLevel = sharedPreferences.getString(
					"sharing_preference", "Us, Researchers and Forecasters");
			boolean preferenceSendNotifications = sharedPreferences.getBoolean(
					"send_notifications", false);
			settingsHandler
					.setDataCollectionFrequency(stringTimeToLongHack(preferenceCollectionFrequency));

			settingsHandler.setSendNotifications(preferenceSendNotifications);
			
			boolean useGPS = sharedPreferences.getBoolean("use_gps", true);
			boolean onlyWhenCharging = sharedPreferences.getBoolean("only_when_charging", false);
			settingsHandler.setUseGPS(useGPS);
			settingsHandler.setOnlyWhenCharging(onlyWhenCharging);
			settingsHandler.setSharingData(preferenceShareData);
			settingsHandler.setShareLevel(preferenceShareLevel);
			
			log("cbservice startwithintent " + settingsHandler);
			
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
				settingsHandler.setServerURL(serverURL);
				settingsHandler.setShareLevel(allSettings.getString(7));
				// booleans
				int onlyWhenCharging = allSettings.getInt(4);
				int useGPS = allSettings.getInt(9);
				int sendNotifications = allSettings.getInt(8);
				int sharingData = allSettings.getInt(6);
				boolean boolCharging = (onlyWhenCharging > 0);
				boolean boolGPS = (useGPS > 0);
				boolean boolSendNotifications = (sendNotifications > 0);
				boolean boolSharingData = (sharingData > 0);
				log("only when charging processed " + boolCharging + " gps " + boolGPS);
				settingsHandler.setSendNotifications(boolSendNotifications);
				settingsHandler.setOnlyWhenCharging(boolCharging);
				settingsHandler.setUseGPS(boolGPS);
				settingsHandler.setSharingData(boolSharingData);
				settingsHandler.saveSettings();
				
				log("cbservice startwithdb, " + settingsHandler);
				
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
				//log("start autosubmit");
				//startWithDatabase();
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
				System.out.println("cbservice received new settings, " + newSettings);
				newSettings.saveSettings();
				break;
			case MSG_GET_LOCAL_RECENTS:
				log("get local recents");
				recentMsg = msg;
				CbApiCall apiCall = (CbApiCall) msg.obj;
				if (apiCall == null) {
					//log("apicall null, bailing");
					break;
				}
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
					obs.setTime(cursor.getLong(10));
					obs.setTimeZoneOffset(cursor.getInt(11));
					obs.setUser_id(cursor.getString(12));
					obs.setTrend(cursor.getString(18));

					// TODO: Add sensor information

					results.add(obs);
				}
				db.close();
				log("cbservice: " + results.size() + " local api results");
				try {
					msg.replyTo.send(Message.obtain(null, MSG_LOCAL_RECENTS,
							results));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
				break;
			case MSG_GET_API_RECENTS:
				CbApiCall apiCacheCall = (CbApiCall) msg.obj;
				log("get api recents " + apiCacheCall.toString());
				// run API call
				try {
					db.open();
	
					Cursor cacheCursor = db.runAPICacheCall(
							apiCacheCall.getMinLat(), apiCacheCall.getMaxLat(),
							apiCacheCall.getMinLon(), apiCacheCall.getMaxLon(),
							apiCacheCall.getStartTime(), apiCacheCall.getEndTime(),
							apiCacheCall.getLimit());
					ArrayList<CbObservation> cacheResults = new ArrayList<CbObservation>();
					while (cacheCursor.moveToNext()) {
						CbObservation obs = new CbObservation();
						Location location = new Location("network");
						location.setLatitude(cacheCursor.getDouble(1));
						location.setLongitude(cacheCursor.getDouble(2));
						obs.setLocation(location);
						obs.setObservationValue(cacheCursor.getDouble(3));
						obs.setTime(cacheCursor.getLong(4));
						cacheResults.add(obs);
					}
					db.close();
					try {
						msg.replyTo.send(Message.obtain(null, MSG_API_RECENTS,
								cacheResults));
					} catch (RemoteException re) {
						re.printStackTrace();
					}
				} catch(Exception e) {
					
				}

				break;
			case MSG_GET_API_RECENTS_FOR_GRAPH:
				// TODO: Put this in a method. It's a copy+paste from GET_API_RECENTS
				CbApiCall apiCacheCallGraph = (CbApiCall) msg.obj;
				log("get api recents " + apiCacheCallGraph.toString());
				// run API call
				db.open();

				Cursor cacheCursorGraph = db.runAPICacheCall(
						apiCacheCallGraph.getMinLat(), apiCacheCallGraph.getMaxLat(),
						apiCacheCallGraph.getMinLon(), apiCacheCallGraph.getMaxLon(),
						apiCacheCallGraph.getStartTime(), apiCacheCallGraph.getEndTime(),
						apiCacheCallGraph.getLimit());
				ArrayList<CbObservation> cacheResultsGraph = new ArrayList<CbObservation>();
				while (cacheCursorGraph.moveToNext()) {
					CbObservation obs = new CbObservation();
					Location location = new Location("network");
					location.setLatitude(cacheCursorGraph.getDouble(1));
					location.setLongitude(cacheCursorGraph.getDouble(2));
					obs.setLocation(location);
					obs.setObservationValue(cacheCursorGraph.getDouble(3));
					obs.setTime(cacheCursorGraph.getLong(4));
					cacheResultsGraph.add(obs);
				}
				db.close();
				try {
					msg.replyTo.send(Message.obtain(null, MSG_API_RECENTS_FOR_GRAPH,
							cacheResultsGraph));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
				break;
			case MSG_MAKE_API_CALL:
				CbApi api = new CbApi(getApplicationContext());
				CbApiCall liveApiCall = (CbApiCall) msg.obj;
				liveApiCall.setCallType("Readings");
				long timeDiff = System.currentTimeMillis() - lastAPICall;

				deleteOldData();

				lastAPICall = api.makeAPICall(liveApiCall, service,
						msg.replyTo, "Readings");

				break;
			case MSG_MAKE_CURRENT_CONDITIONS_API_CALL:

				CbApi conditionApi = new CbApi(getApplicationContext());
				CbApiCall conditionApiCall = (CbApiCall) msg.obj;
				conditionApiCall.setCallType("Conditions");
				conditionApi.makeAPICall(conditionApiCall, service,
						msg.replyTo, "Conditions");

				break;
			case MSG_CLEAR_LOCAL_CACHE:
				db.open();
				db.clearLocalCache();
				long count = db.getUserDataCount();
				db.close();
				try {
					msg.replyTo.send(Message.obtain(null,
							MSG_COUNT_LOCAL_OBS_TOTALS, (int) count, 0));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
				break;
			case MSG_REMOVE_FROM_PRESSURENET:
				// TODO: Implement
				break;
			case MSG_CLEAR_API_CACHE:
				db.open();
				db.clearAPICache();
				db.open();
				long countCache = db.getDataCacheCount();
				db.close();
				try {
					msg.replyTo.send(Message.obtain(null,
							MSG_COUNT_API_CACHE_TOTALS, (int) countCache, 0));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
				break;
			case MSG_ADD_CURRENT_CONDITION:
				CbCurrentCondition cc = (CbCurrentCondition) msg.obj;
				db.open();
				db.addCondition(cc);
				db.close();
				break;
			case MSG_GET_CURRENT_CONDITIONS:
				recentMsg = msg;
				db.open();
				CbApiCall currentConditionAPI = (CbApiCall) msg.obj;

				Cursor ccCursor = db.getCurrentConditions(
						currentConditionAPI.getMinLat(),
						currentConditionAPI.getMaxLat(),
						currentConditionAPI.getMinLon(),
						currentConditionAPI.getMaxLon(),
						currentConditionAPI.getStartTime(),
						currentConditionAPI.getEndTime(), 1000);

				ArrayList<CbCurrentCondition> conditions = new ArrayList<CbCurrentCondition>();
				while (ccCursor.moveToNext()) {
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
					msg.replyTo.send(Message.obtain(null,
							MSG_CURRENT_CONDITIONS, conditions));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
				break;
			case MSG_SEND_CURRENT_CONDITION:
				CbCurrentCondition condition = (CbCurrentCondition) msg.obj;
				if(settingsHandler == null ) {
					settingsHandler = new CbSettingsHandler(getApplicationContext()); 
					settingsHandler.setServerURL(serverURL);
					settingsHandler.setAppID("ca.cumulonimbus.barometernetwork");
				} 
				try {
					condition.setSharing_policy(settingsHandler.getShareLevel());
					sendCbCurrentCondition(condition);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case MSG_SEND_OBSERVATION:
				log("sending single observation, request from app");
				sendSingleObs();
				break;
			case MSG_COUNT_LOCAL_OBS:
				db.open();
				long countLocalObsOnly = db.getUserDataCount();
				db.close();
				try {
					msg.replyTo.send(Message.obtain(null,
							MSG_COUNT_LOCAL_OBS_TOTALS,
							(int) countLocalObsOnly, 0));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
				break;
			case MSG_COUNT_API_CACHE:
				db.open();
				long countCacheOnly = db.getDataCacheCount();
				db.close();
				try {
					msg.replyTo.send(Message
							.obtain(null, MSG_COUNT_API_CACHE_TOTALS,
									(int) countCacheOnly, 0));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
				break;
			case MSG_CHANGE_NOTIFICATION:
				if(msg.replyTo != null) {
					lastMessenger = msg.replyTo;
				} else {
					// ..
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	public void sendSingleObs() {
		if(settingsHandler != null) {
			if(settingsHandler.getServerURL()==null ) {
				settingsHandler.getSettings();
			}
		}
		SingleReadingSender singleSender = new SingleReadingSender();
		mHandler.post(singleSender);
	}
	
	/**
	 * Remove older data from cache to keep the size reasonable
	 * 
	 * @return
	 */
	public void deleteOldData() {
		log("deleting old data");
		db.open();
		db.deleteOldCacheData();
		db.close();
	}

	public boolean notifyAPIResult(Messenger reply, int count) {
		try {
			if (reply == null) {
				log("cannot notify, reply is null");
			} else {
				reply.send(Message.obtain(null, MSG_API_RESULT_COUNT, count, 0));
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
		double pressure = 0.0;
		try {
			long rowId = db.fetchObservationMaxID();
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
		} catch(Exception e) {
			obs.setObservationValue(pressure);
			return obs;
		}
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
		if(dataCollector!=null) {
			dataCollector.startCollectingData(m);
			new StreamObservation().execute(m);
		}
	}

	public void stopDataStream() {
		log("cbservice stopping stream");
		if(dataCollector!=null) {
			dataCollector.stopCollectingData();
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
		logToFile(message);
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
