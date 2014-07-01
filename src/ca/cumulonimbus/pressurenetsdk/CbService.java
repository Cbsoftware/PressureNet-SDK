package ca.cumulonimbus.pressurenetsdk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;

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

	Message recentMsg;

	String serverURL = CbConfiguration.SERVER_URL;

	public static String ACTION_SEND_MEASUREMENT = "ca.cumulonimbus.pressurenetsdk.ACTION_SEND_MEASUREMENT";
	public static String ACTION_REGISTER = "ca.cumulonimbus.pressurenetsdk.ACTION_REGISTER";

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

	// Live sensor streaming
	public static final int MSG_START_STREAM = 11;
	public static final int MSG_DATA_STREAM = 12;
	public static final int MSG_STOP_STREAM = 13;
	
	// PressureNet Live API
	public static final int MSG_GET_LOCAL_RECENTS = 14;
	public static final int MSG_LOCAL_RECENTS = 15;
	public static final int MSG_GET_API_RECENTS = 16;
	public static final int MSG_API_RECENTS = 17;
	public static final int MSG_MAKE_API_CALL = 18;
	public static final int MSG_API_RESULT_COUNT = 19;
	// PressureNet API Cache
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
	// Statistics
	public static final int MSG_MAKE_STATS_CALL = 39;
	public static final int MSG_STATS = 40;
	// External Weather Services
	public static final int MSG_GET_EXTERNAL_LOCAL_EXPANDED = 41;
	public static final int MSG_EXTERNAL_LOCAL_EXPANDED = 42;
	// User contributions summary
	public static final int MSG_GET_CONTRIBUTIONS = 43;
	public static final int MSG_CONTRIBUTIONS = 44;	
	// Database info, test suites/debugging
	public static final int MSG_GET_DATABASE_INFO = 45;
	// Multitenancy Support
	public static final int MSG_GET_PRIMARY_APP = 46;
	public static final int MSG_IS_PRIMARY = 47;
	// Localized Current conditions
	public static final int MSG_GET_LOCAL_CONDITIONS = 48;
	public static final int MSG_LOCAL_CONDITIONS = 49;
	
	
	// Intents
	public static final String PRESSURE_CHANGE_ALERT = "ca.cumulonimbus.pressurenetsdk.PRESSURE_CHANGE_ALERT";
	public static final String LOCAL_CONDITIONS_ALERT = "ca.cumulonimbus.pressurenetsdk.LOCAL_CONDITIONS_ALERT";
	public static final String PRESSURE_SENT_TOAST = "ca.cumulonimbus.pressurenetsdk.PRESSURE_SENT_TOAST";
	public static final String CONDITION_SENT_TOAST = "ca.cumulonimbus.pressurenetsdk.CONDITION_SENT_TOAST";
		
	// Support for new sensor type constants
	private final int TYPE_AMBIENT_TEMPERATURE = 13;
	private final int TYPE_RELATIVE_HUMIDITY = 12;
	
	long lastAPICall = System.currentTimeMillis();
	long lastConditionNotification = System.currentTimeMillis() - (1000 * 60 * 60 * 6);
	
	private CbObservation collectedObservation;

	private final Handler mHandler = new Handler();

	Messenger mMessenger = new Messenger(new IncomingHandler());

	ArrayList<CbObservation> offlineBuffer = new ArrayList<CbObservation>();

	private long lastPressureChangeAlert = 0;

	private Messenger lastMessenger;

	private boolean fromUser = false;

	CbAlarm alarm = new CbAlarm();

	double recentPressureReading = 0.0;
	int recentPressureAccuracy = 0;
	int batchReadingCount = 0;
	
	private long lastSubmit = 0;

	private PowerManager.WakeLock wl;
	
	private boolean hasBarometer = true;

	ArrayList<CbSensorStreamer> activeStreams = new ArrayList<CbSensorStreamer>();
	
	/**
	 * Collect data from onboard sensors and store locally
	 * 
	 * @author jacob
	 * 
	 */
	public class CbDataCollector implements SensorEventListener {

		private SensorManager sm;
		Sensor pressureSensor;
		private final int TYPE_AMBIENT_TEMPERATURE = 13;
		private final int TYPE_RELATIVE_HUMIDITY = 12;

		private ArrayList<CbObservation> recentObservations = new ArrayList<CbObservation>();
		
		int stopSoonCalls = 0;
		
		public ArrayList<CbObservation> getRecentObservations() {
			return recentObservations;
		}

		/**
		 * Access the database to fetch recent, locally-recorded observations
		 * 
		 * @return
		 */
		public ArrayList<CbObservation> getRecentDatabaseObservations() {
			ArrayList<CbObservation> recentDbList = new ArrayList<CbObservation>();
			CbDb db = new CbDb(getApplicationContext());
			db.open();
			Cursor c = db.fetchAllObservations();
			while (c.moveToNext()) {
				CbObservation obs = new CbObservation();
				Location location = new Location("network");
				location.setLatitude(c.getDouble(1));
				location.setLongitude(c.getDouble(2));
				location.setAltitude(c.getDouble(3));
				location.setAccuracy(c.getInt(4));
				location.setProvider(c.getString(5));
				obs.setLocation(location);
				obs.setObservationType(c.getString(6));
				obs.setObservationUnit(c.getString(7));
				obs.setObservationValue(c.getDouble(8));
				obs.setSharing(c.getString(9));
				obs.setTime(c.getInt(10));
				obs.setTimeZoneOffset(c.getInt(11));
				obs.setUser_id(c.getString(12));
				recentDbList.add(obs);
			}

			db.close();
			return recentDbList;
		}

		public void setRecentObservations(
				ArrayList<CbObservation> recentObservations) {
			this.recentObservations = recentObservations;
		}

		/**
		 * Start collecting sensor data.
		 * 
		 * @param m
		 * @return
		 */
		public boolean startCollectingData() {
			batchReadingCount = 0;
			stopSoonCalls = 0;
			sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			pressureSensor = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
			if(wl!=null) {
				log("cbservice startcollectingdata wakelock " + wl.isHeld());
				if(!wl.isHeld()) {
					log("cbservice startcollectingdata no wakelock, bailing");
					return false;
				} 
			}
			boolean collecting = false;
			try {
				if(sm != null) {
					if (pressureSensor != null) {
						log("cbservice sensor SDK " + android.os.Build.VERSION.SDK_INT + "");
						if(android.os.Build.VERSION.SDK_INT == 19) {
							collecting = sm.registerListener(this, pressureSensor,SensorManager.SENSOR_DELAY_UI); // , 100000);
						} else {
							collecting = sm.registerListener(this, pressureSensor,SensorManager.SENSOR_DELAY_UI);
						}
					} else {
						log("cbservice pressure sensor is null");
					}
				} else {
					log("cbservice sm is null");
				}
				return collecting;
			} catch (Exception e) {
				log("cbservice sensor error " + e.getMessage());
				e.printStackTrace();
				return collecting;
			}
		}
		
		/**
		 * Stop collecting sensor data
		 */
		public void stopCollectingData() {
			log("cbservice stop collecting data");
			// sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			if(sm!=null) {
				log("cbservice sensormanager not null, unregistering");
				sm.unregisterListener(this);
				sm = null;
			} else {
				
				log("cbservice sensormanager null, walk away");
				/*
				sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
				sm.unregisterListener(this);
				sm = null;
				*/
			}
		}

		public CbDataCollector() {
			
			
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			if (sensor.getType() == Sensor.TYPE_PRESSURE) {
				recentPressureAccuracy = accuracy;
				log("cbservice accuracy changed, new barometer accuracy  " + recentPressureAccuracy);
			}
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
				if(event.values.length > 0) {
					if(event.values[0] >= 0) {
						log("cbservice sensor; new pressure reading " + event.values[0]);
						recentPressureReading = event.values[0];
					} else {
						log("cbservice sensor; pressure reading is 0 or negative" + event.values[0]);
					}
				} else {
					log("cbservice sensor; no event values");
				}
				
			} 
			
			
			if(stopSoonCalls<=1) {
				stopSoon();
			}
			/*
			batchReadingCount++;
			if(batchReadingCount>2) {
				log("batch readings " + batchReadingCount + ", stopping");
				stopCollectingData();
			} else {
				log("batch readings " + batchReadingCount + ", not stopping");
			}
			*/
			
		}
		
		
		private class SensorStopper implements Runnable {
			
			@Override
			public void run() {
				stopCollectingData();
				
			}
		}
		
		private void stopSoon() {
			stopSoonCalls++;
			SensorStopper stop = new SensorStopper();
			mHandler.postDelayed(stop, 100);
		}
		
	}
	
	/**
	 * Collect data from onboard sensors and store locally
	 * 
	 * @author jacob
	 * 
	 */
	public class CbSensorStreamer implements SensorEventListener {

		public int sensorId;
		private Messenger replyTo;
		
		private SensorManager sm;
		Sensor sensor;
				
		/**
		 * Start collecting sensor data.
		 * 
		 * @param m
		 * @return
		 */
		public void startSendingData() {
			sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			sensor = sm.getDefaultSensor(sensorId);
			try {
				if(sm != null) {
					if (sensor != null) {
						log("CbService streamer registering sensorID " + sensorId);
						sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
					} else {
						log("cbservice streaming sensor is null");
					}

				} else {
					log("cbservice sm is null");
				}
			} catch (Exception e) {
				log("cbservice sensor error " + e.getMessage());
			}
		}
		
		/**
		 * Stop collecting sensor data
		 */
		public void stopSendingData() {
			log("cbservice streaming stop collecting data");
			if(sm!=null) {
				log("cbservice streaming sensormanager not null, unregistering");
				sm.unregisterListener(this);
				sm = null;
			} else {
				log("cbservice streaming sensormanager null, walk away");
			}
		}

		public CbSensorStreamer(int id, Messenger reply) {
			this.sensorId = id;
			this.replyTo = reply;
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			if (sensor.getType() == sensorId) {

			}
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() == sensorId) {
				// new sensor reading
				CbObservation obs = new CbObservation();
				obs.setObservationValue(event.values[0]);
				try {
					replyTo.send(Message.obtain(null,
							MSG_DATA_STREAM, obs));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Check if a sensor is already being used in an active stream
	 * to avoid multiple listeners on the same sensor. 
	 * @param sensorId
	 * @return
	 */
	
	private boolean isSensorStreaming(int sensorId) {
		for(CbSensorStreamer s : activeStreams) {
			if (s.sensorId == sensorId) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Start live sensor streaming from the service
	 * to the app. MSG_START_STREAM
	 */
	public void startSensorStream(int sensorId, Messenger reply) {
		if(!isSensorStreaming(sensorId)) {
			log("CbService starting live sensor streaming " + sensorId);
			CbSensorStreamer streamer = new CbSensorStreamer(sensorId, reply);
			activeStreams.add(streamer);
			streamer.startSendingData();
		} else {
			log("CbService not starting live sensor streaming " + sensorId + ", already streaming");
		}
	}
	
	/**
	 * Stop live sensor streaming. MSG_STOP_STREAM
	 */
	public void stopSensorStream(int sensorId) {
		if(isSensorStreaming(sensorId)) {
			log("CbService stopping live sensor streaming " + sensorId);
			for(CbSensorStreamer s : activeStreams) {
				if (s.sensorId == sensorId) {
					s.stopSendingData();
					activeStreams.remove(s);
					break;
				}
			}
		} else {
			log("CbService not stopping live sensor streaming " + sensorId + " sensor not running");
		}
	}

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
			pressureObservation = buildPressureObservation();
			pressureObservation.setLocation(locationManager
					.getCurrentBestLocation());

			log("returning pressure obs: "
					+ pressureObservation.getObservationValue());

			return pressureObservation;

		} catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	private class LocationStopper implements Runnable {

		@Override
		public void run() {
			try {
				//System.out.println("locationmanager stop getting locations");
				locationManager.stopGettingLocations();
			} catch (Exception e) {
				//e.printStackTrace();
			}
		}

	}

	/**
	 * Send a single reading. TODO: Combine with ReadingSender for less code duplication.
	 * ReadingSender. Fix that.
	 */
	public class SingleReadingSender implements Runnable {

		@Override
		public void run() {
			
			checkForLocalConditionReports();
			
			if(settingsHandler == null) {
				log("single reading sender, loading settings from prefs");
				loadSetttingsFromPreferences();
			}
			log("collecting and submitting single "
					+ settingsHandler.getServerURL());
			
			
			CbObservation singleObservation = new CbObservation();
			if (hasBarometer && settingsHandler.isCollectingData()) {
				// Collect
				dataCollector = new CbDataCollector();
				dataCollector.startCollectingData();
				
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
										.setClientKey(CbConfiguration.API_KEY);
								fromUser = true;
								sendCbObservation(singleObservation);
								fromUser = false;

								// also check and send the offline buffer
								if (offlineBuffer.size() > 0) {
									log("sending " + offlineBuffer.size()
											+ " offline buffered obs");
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
						//e.printStackTrace();

					}
				}
			}
		}
	}
	
	/**
	 * Check if we have a barometer. Use info to disable menu items, choose to
	 * run the service or not, etc.
	 */
	private boolean checkBarometer() {
		PackageManager packageManager = this.getPackageManager();
		hasBarometer = packageManager
				.hasSystemFeature(PackageManager.FEATURE_SENSOR_BAROMETER);
		return hasBarometer;
	}

	
	/**
	 * Collect and send data in a different thread. This runs itself every
	 * "settingsHandler.getDataCollectionFrequency()" milliseconds
	 */
	private class ReadingSender implements Runnable {

		public void run() {
			
			checkForLocalConditionReports();
			
			long now = System.currentTimeMillis();
			if(now - lastSubmit < 2000) {
				log("cbservice readingsender too soon, bailing");
				return;
			}
	
			
			// retrieve updated settings
			settingsHandler = new CbSettingsHandler(getApplicationContext());
			settingsHandler = settingsHandler.getSettings();

			log("collecting and submitting " + settingsHandler.getServerURL());

			boolean okayToGo = true;
			// Check if we're supposed to be charging and if we are.
			// Bail if appropriate
			if (settingsHandler.isOnlyWhenCharging()) {
				if (!isCharging()) {
					okayToGo = false;
				}
			}
			
			if(!hasBarometer) {
				okayToGo = false;
			}

			if (okayToGo && settingsHandler.isCollectingData()) {
				// Collect
				// start collecting data
				dataCollector = new CbDataCollector();
				dataCollector.startCollectingData();
				
				CbObservation singleObservation = new CbObservation();
				singleObservation = collectNewObservation();
				if (singleObservation != null) {

					if (singleObservation.getObservationValue() != 0.0) {
						// Store in database
						db.open();
						long count = db.addObservation(singleObservation);

						db.close();

						try {
							if (settingsHandler.isSharingData()) {
								// Send if we're online
								if (isNetworkAvailable()) {
									lastSubmit = System.currentTimeMillis();
									log("online and sending");
									singleObservation
											.setClientKey(CbConfiguration.API_KEY);
									sendCbObservation(singleObservation);

									// also check and send the offline buffer
									if (offlineBuffer.size() > 0) {
										log("sending " + offlineBuffer.size()
												+ " offline buffered obs");
										for (CbObservation singleOffline : offlineBuffer) {
											sendCbObservation(singleObservation);
										}
										offlineBuffer.clear();
									}
								} else {
									log("didn't send");
									// / offline buffer variable
									// TODO: put this in the DB to survive
									// longer
									offlineBuffer.add(singleObservation);

								}
							} else {
								log("cbservice not sharing data, didn't send");
							}
							
							// If notifications are enabled,
							log("is send notif "
									+ settingsHandler.isSendNotifications());
							if (settingsHandler.isSendNotifications()) {
								// check for pressure local trend changes and
								// notify
								// the client

								// ensure this only happens every once in a
								// while
								long rightNow = System.currentTimeMillis();
								long sixHours = 1000 * 60 * 60 * 6;
								if (rightNow - lastPressureChangeAlert > (sixHours)) {
									long timeLength = 1000 * 60 * 60 * 3;
									db.open();
									Cursor localCursor = db.runLocalAPICall(
											-90, 90, -180, 180,
											System.currentTimeMillis()
													- (timeLength),
											System.currentTimeMillis(), 1000);
									ArrayList<CbObservation> recents = new ArrayList<CbObservation>();
									while (localCursor.moveToNext()) {
										// just need observation value, time,
										// and
										// location
										CbObservation obs = new CbObservation();
										obs.setObservationValue(localCursor
												.getDouble(8));
										obs.setTime(localCursor.getLong(10));
										Location location = new Location(
												"network");
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

									log("cbservice tendency changes: "
											+ tendencyChange);
									if (tendencyChange.contains(",")
											&& (!tendencyChange.toLowerCase()
													.contains("unknown"))) {
										String[] tendencies = tendencyChange
												.split(",");
										if (!tendencies[0]
												.equals(tendencies[1])) {
											log("Trend change! "
													+ tendencyChange);

											Intent intent = new Intent();
											intent.setAction(PRESSURE_CHANGE_ALERT);
											intent.putExtra("ca.cumulonimbus.pressurenetsdk.tendencyChange", tendencyChange);
											sendBroadcast(intent);
											
											try {
												if (lastMessenger != null) {
													lastMessenger
															.send(Message
																	.obtain(null,
																			MSG_CHANGE_NOTIFICATION,
																			tendencyChange));
												} else {
													log("readingsender didn't send notif, no lastMessenger");
												}
											} catch (Exception e) {
												//e.printStackTrace();
											}
											lastPressureChangeAlert = rightNow;
											// TODO: saveinprefs;
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
							//e.printStackTrace();

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
	

	/**
	 *  Put together all the information that defines
	 *  an observation and store it in a single object.
	 * @return
	 */
	public CbObservation buildPressureObservation() {
		CbObservation pressureObservation = new CbObservation();
		pressureObservation.setTime(System.currentTimeMillis());
		pressureObservation.setTimeZoneOffset(Calendar.getInstance()
				.getTimeZone().getRawOffset());
		pressureObservation.setUser_id(getID());
		pressureObservation.setObservationType("pressure");
		pressureObservation.setObservationValue(recentPressureReading);
		
		pressureObservation.setObservationUnit("mbar");
		// pressureObservation.setSensor(sm.getSensorList(Sensor.TYPE_PRESSURE).get(0));
		pressureObservation.setSharing(settingsHandler.getShareLevel());
		
		pressureObservation.setVersionNumber(getSDKVersion());
		
		log("cbservice buildobs, share level "
				+ settingsHandler.getShareLevel() + " " + getID());
		return pressureObservation;
	}

	/**
	 * Return the version number of the SDK sending this reading
	 * @return
	 */
	public String getSDKVersion() {
		String version = "-1.0";
		try {
			version = getPackageManager()
					.getPackageInfo("ca.cumulonimbus.pressurenetsdk", 0).versionName;
		} catch (NameNotFoundException nnfe) {
			// TODO: this is not an okay return value
			// (Don't send error messages as version numbers)
			version = nnfe.getMessage(); 
		}
		return version;
	}
	
	/**
	 * Periodically check to see if someone has
	 * reported a current condition nearby. If it's 
	 * appropriate, send a notification
	 */
	private void checkForLocalConditionReports() {
		long now = System.currentTimeMillis();
		long minWaitTime = 1000 * 60 * 60;
		if(now - minWaitTime > lastConditionNotification) {
			if(locationManager == null) {
				locationManager = new CbLocationManager(getApplicationContext());
			}
			log("cbservice checking for local conditions reports");
			// it has been long enough; make a conditions API call 
			// for the local area
			CbApi conditionApi = new CbApi(getApplicationContext());
			CbApiCall conditionApiCall = buildLocalConditionsApiCall();
			if(conditionApiCall!=null) {
				
				log("cbservice making conditions api call for local reports");
				conditionApi.makeAPICall(conditionApiCall, service,
						mMessenger, "Conditions");
		
				// TODO: store this more permanently
				lastConditionNotification = now;
			}
		} else {
			log("cbservice not checking for local conditions, too recent");
		}
	}
	
	/**
	 * Make a CbApiCall object for local conditions
	 * @return
	 */
	public CbApiCall buildLocalConditionsApiCall() {
		CbApiCall conditionApiCall = new CbApiCall();
		conditionApiCall.setCallType("Conditions");
		Location location = new Location("network");
		location.setLatitude(0);
		location.setLongitude(0);
		if(locationManager != null) {
			location = locationManager.getCurrentBestLocation();
			if(location != null) {
				conditionApiCall.setMinLat(location.getLatitude() - .1);
				conditionApiCall.setMaxLat(location.getLatitude() + .1);
				conditionApiCall.setMinLon(location.getLongitude() - .1);
				conditionApiCall.setMaxLon(location.getLongitude() + .1);
				conditionApiCall.setStartTime(System.currentTimeMillis() - (1000 * 60 * 60));
				conditionApiCall.setEndTime(System.currentTimeMillis());
				return conditionApiCall;
			} else {
				return null;
			}
		} else {
			log("cbservice not checking location condition reports, no locationmanager");
			return null;
		}
	}

	/**
	 * Check for network connection, return true
	 * if we're online.
	 * 
	 * @return
	 */
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
		log("cbservice stop autosubmit");
		// alarm.cancelAlarm(getApplicationContext());
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
			settingsHandler = settingsHandler.getSettings();
			if(settingsHandler.getServerURL().equals("")) {
				log("cbservice settings are empty; defaults");
				//loadSetttingsFromPreferences();
				// settingsHandler = settingsHandler.getSettings();
			}
			log("sendCbObservation with wakelock " + wl.isHeld() + " and settings " + settingsHandler);
			sender.setSettings(settingsHandler, locationManager,
					lastMessenger, fromUser);
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
			sender.setSettings(settingsHandler, locationManager,
					null, true);
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
			sender.setSettings(settingsHandler, locationManager,
					lastMessenger, fromUser);
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
	public void startSubmit() {
		log("CbService: Starting to auto-collect and submit data.");
		fromUser = false;
		if (!alarm.isRepeating()) {
			settingsHandler = settingsHandler.getSettings();
			log("cbservice alarm not repeating, starting alarm at " + settingsHandler.getDataCollectionFrequency());
			alarm.setAlarm(getApplicationContext(),
					settingsHandler.getDataCollectionFrequency());
		} else {
			log("cbservice startsubmit, alarm is already repeating. restarting at " + settingsHandler.getDataCollectionFrequency());
			alarm.restartAlarm(getApplicationContext(),
					settingsHandler.getDataCollectionFrequency());
		}
	}
	
	@Override
	public void onDestroy() {
		log("cbservice on destroy");
		stopAutoSubmit();
		unregisterReceiver(receiver);
		super.onDestroy();
	}

	@Override
	public void onCreate() {
		setUpFiles();
		log("cb on create");
		settingsHandler = new CbSettingsHandler(getApplicationContext());
		settingsHandler.getSettings();
		db = new CbDb(getApplicationContext());
		fromUser = false;
		
		prepForRegistration();
		
		super.onCreate();
	}
	
	/**
	 * SDK registration
	 */
	private void prepForRegistration() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(CbService.ACTION_REGISTER);
		registerReceiver(receiver, filter);
	}
	
	/**
	 * Check charge state for preferences.
	 * 
	 */
	public boolean isCharging() {
		// Check battery and charging status
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = getApplicationContext().registerReceiver(null,
				ifilter);

		// Are we charging / charged?
		int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
				|| status == BatteryManager.BATTERY_STATUS_FULL;
		return isCharging;
	}
	
	/**
	 * Each app registers with the SDK. Send the package name
	 * to 
	 */
	private void sendRegistrationInfo() {
		log("SDKTESTS: sending registration info");
		Intent intent = new Intent(ACTION_REGISTER);
		intent.putExtra("packagename", getApplicationContext().getPackageName());
		intent.putExtra("time", System.currentTimeMillis());
		sendBroadcast(intent);
	}

	private final BroadcastReceiver receiver = new BroadcastReceiver() {
	   @Override
	   public void onReceive(Context context, Intent intent) {
		   String action = intent.getAction();
		   if (action.equals(ACTION_REGISTER)) {
			   String registeredName = intent.getStringExtra("packagename");
			   Long registeredTime = intent.getLongExtra("time", 0);
			   log("SDKTESTS: registering " + registeredName + " registered at " + registeredTime);
			   
			   // addRegistration
			   db.open();
			   db.addRegistration(registeredName, registeredTime);

			   
			   // check status
			   if(db.isPrimaryApp()) {
				   log("SDKTESTS: " + getApplicationContext().getPackageName() + " is primary");
			   } else {
				   log("SDKTESTS: " + getApplicationContext().getPackageName() + " is not primary");
			   }
			   
			   db.close();			   
		   }
	   }
	};
	
	/**
	 * Start running background data collection methods.
	 * 
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log("cbservice onstartcommand");
		
		checkBarometer();
	
		// wakelock management
		if(wl!=null) {
			log("cbservice wakelock not null:");
			if(wl.isHeld()) {
				log("cbservice existing wakelock; releasing");
				wl.release();
			} else {
				log("cbservice wakelock not null but no existing lock");
			}
		}
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP , "CbService"); // PARTIAL_WAKE_LOCK
		wl.acquire(1000);
		log("cbservice acquiring wakelock " + wl.isHeld());
		
		dataCollector = new CbDataCollector();
		try {
			if (intent != null) {
				if (intent.getAction() != null) {
					if (intent.getAction().equals(ACTION_SEND_MEASUREMENT)) {
						// send just a single measurement
						fromUser = false;
						log("sending single observation, request from intent");
						sendSingleObs();
						return START_NOT_STICKY;
					} 
				} else if (intent.getBooleanExtra("alarm", false)) {
					// This runs when the service is started from the alarm.
					// Submit a data point
					log("cbservice alarm firing, sending data");
					settingsHandler = new CbSettingsHandler(getApplicationContext());
					settingsHandler = settingsHandler.getSettings();
					
					removeOldSDKApps();
					sendRegistrationInfo();
					
					
					if(settingsHandler.isSharingData()) {
						//dataCollector.startCollectingData();
						startWithIntent(intent, true);
					} else {
						log("cbservice not sharing data");
					}
					
					LocationStopper stop = new LocationStopper();
					mHandler.postDelayed(stop, 1000 * 3);
					return START_NOT_STICKY;
				} else {
				
					// Check the database
					
					log("starting service with db");
					startWithDatabase();
					return START_NOT_STICKY;
				}
			}
		} catch (Exception e) {
			log("cbservice onstartcommand exception " + e.getMessage());
		} 
		
		super.onStartCommand(intent, flags, startId);
		return START_NOT_STICKY;
	}
	
	public void removeAllUninstalledApps() {
		db.open();
		
		db.close();
	}
	
	private void removeOldSDKApps() {
		db.open();
		db.removeOldSDKApps(1);
		db.close();
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
		} else if (timeAgo.equals("3 hours")) {
			return 1000 * 60 * 60 * 3;
		} else if(timeAgo.equals("6 hours")) {
			return 1000 * 60 * 60 * 6;
		} else if(timeAgo.equals("12 hours")) {
			return 1000 * 60 * 60 * 12;
		}
		return 1000 * 60 * 10;
	}

	public void loadSetttingsFromPreferences() {
		log("cbservice loading settings from prefs");
		settingsHandler = new CbSettingsHandler(getApplicationContext());
		settingsHandler.setServerURL(serverURL);
		settingsHandler.setAppID(getApplication().getPackageName());

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
		boolean onlyWhenCharging = sharedPreferences.getBoolean(
				"only_when_charging", false);
		settingsHandler.setUseGPS(useGPS);
		settingsHandler.setOnlyWhenCharging(onlyWhenCharging);
		settingsHandler.setSharingData(preferenceShareData);
		settingsHandler.setShareLevel(preferenceShareLevel);
		
		// Seems like new settings. Try adding to the db.
		settingsHandler.saveSettings();
	}
	
	public void startWithIntent(Intent intent, boolean fromAlarm) {
		try {
			if (!fromAlarm) {
				// We arrived here from the user (i.e., not the alarm)
				// start/(update?) the alarm
				startSubmit();
			} else {
				// alarm. Go!
				ReadingSender reading = new ReadingSender();
				mHandler.post(reading);
			}
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
			Cursor allSettings = db.fetchSettingByApp(getPackageName());
			log("cb intent null; checking db, size " + allSettings.getCount());
			if (allSettings.moveToFirst()) {
				settingsHandler.setAppID(allSettings.getString(1));
				settingsHandler.setDataCollectionFrequency(allSettings
						.getLong(2));
				settingsHandler.setServerURL(serverURL);
				int sendNotifications = allSettings.getInt(4);
				int useGPS = allSettings.getInt(5);
				int onlyWhenCharging = allSettings.getInt(6);
				int sharingData = allSettings.getInt(7);
				settingsHandler.setShareLevel(allSettings.getString(9));
				boolean boolCharging = (onlyWhenCharging > 0);
				boolean boolGPS = (useGPS > 0);
				boolean boolSendNotifications = (sendNotifications > 0);
				boolean boolSharingData = (sharingData > 0);
				log("only when charging processed " + boolCharging + " gps "
						+ boolGPS);
				settingsHandler.setSendNotifications(boolSendNotifications);
				settingsHandler.setOnlyWhenCharging(boolCharging);
				settingsHandler.setUseGPS(boolGPS);
				settingsHandler.setSharingData(boolSharingData);
				settingsHandler.saveSettings();
				
			}
			
			log("cbservice startwithdb, " + settingsHandler);
			ReadingSender reading = new ReadingSender();
			mHandler.post(reading);

			startSubmit();
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
				try {
					alarm.cancelAlarm(getApplicationContext());
				} catch(Exception e) {
					
				}
				break;
			case MSG_GET_BEST_LOCATION:
				log("cbservice message. bound service requesting location");
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
				log("message. bound service requesting pressure. not responding");
				
				break;
			case MSG_START_AUTOSUBMIT:
				// log("start autosubmit");
				// startWithDatabase();
				break;
			case MSG_STOP_AUTOSUBMIT:
				log("cbservice stop autosubmit");
				stopAutoSubmit();
				break;
			case MSG_GET_SETTINGS:
				log("get settings");
				if(settingsHandler != null) {
					settingsHandler.getSettings();
				} else {
					settingsHandler = new CbSettingsHandler(getApplicationContext());
					settingsHandler.getSettings();
				}
				try {
					msg.replyTo.send(Message.obtain(null, MSG_SETTINGS,
							settingsHandler));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
				break;
			case MSG_START_STREAM:
				int sensorToStream = msg.arg1;
				startSensorStream(sensorToStream, msg.replyTo);
				break;
			case MSG_STOP_STREAM:
				int sensorToStop = msg.arg1;
				stopSensorStream(sensorToStop);
				break;
			case MSG_SET_SETTINGS:
				settingsHandler = (CbSettingsHandler) msg.obj;
				log("cbservice set settings " + settingsHandler);
				settingsHandler.saveSettings();
				startSubmit();
				break;
			case MSG_GET_LOCAL_RECENTS:
				log("get local recents");
				recentMsg = msg;
				CbApiCall apiCall = (CbApiCall) msg.obj;
				if (apiCall == null) {
					// log("apicall null, bailing");
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
							apiCacheCall.getStartTime(),
							apiCacheCall.getEndTime(), apiCacheCall.getLimit());
					ArrayList<CbObservation> cacheResults = new ArrayList<CbObservation>();
					while (cacheCursor.moveToNext()) {
						CbObservation obs = new CbObservation();
						Location location = new Location("network");
						location.setLatitude(cacheCursor.getDouble(1));
						location.setLongitude(cacheCursor.getDouble(2));
						location.setAltitude(cacheCursor.getDouble(3));
						obs.setLocation(location);
						obs.setObservationValue(cacheCursor.getDouble(4));
						obs.setTime(cacheCursor.getLong(5));
						cacheResults.add(obs);
					}
					
					// and get a few recent local results
					Cursor localCursor = db.runLocalAPICall(
							apiCacheCall.getMinLat(), apiCacheCall.getMaxLat(),
							apiCacheCall.getMinLon(), apiCacheCall.getMaxLon(),
							apiCacheCall.getStartTime(),
							apiCacheCall.getEndTime(), 5);
					ArrayList<CbObservation> localResults = new ArrayList<CbObservation>();
					while (localCursor.moveToNext()) {
						CbObservation obs = new CbObservation();
						Location location = new Location("network");
						location.setLatitude(localCursor.getDouble(1));
						location.setLongitude(localCursor.getDouble(2));
						obs.setLocation(location);
						obs.setObservationValue(localCursor.getDouble(8));
						obs.setTime(localCursor.getLong(10));
						localResults.add(obs);
					}
					db.close();
					cacheResults.addAll(localResults);
					try {
						msg.replyTo.send(Message.obtain(null, MSG_API_RECENTS,
								cacheResults));
					} catch (RemoteException re) {
						// re.printStackTrace();
					}
				} catch (Exception e) {

				}

				break;
			case MSG_GET_API_RECENTS_FOR_GRAPH:
				// TODO: Put this in a method. It's a copy+paste from
				// GET_API_RECENTS
				CbApiCall apiCacheCallGraph = (CbApiCall) msg.obj;
				log("get api recents " + apiCacheCallGraph.toString());
				// run API call
				db.open();

				Cursor cacheCursorGraph = db.runAPICacheCall(
						apiCacheCallGraph.getMinLat(),
						apiCacheCallGraph.getMaxLat(),
						apiCacheCallGraph.getMinLon(),
						apiCacheCallGraph.getMaxLon(),
						apiCacheCallGraph.getStartTime(),
						apiCacheCallGraph.getEndTime(),
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
					msg.replyTo.send(Message.obtain(null,
							MSG_API_RECENTS_FOR_GRAPH, cacheResultsGraph));
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
				// TODO: Implement a secure system to remove user data
				break;
			case MSG_CLEAR_API_CACHE:
				db.open();
				db.clearAPICache();
				long countCache = db.getDataCacheCount();
				db.close();
				try {
					msg.replyTo.send(Message.obtain(null,
							MSG_COUNT_API_CACHE_TOTALS, (int) countCache, 0));
				} catch (RemoteException re) {
					//re.printStackTrace();
				}
				break;
			case MSG_ADD_CURRENT_CONDITION:
				CbCurrentCondition cc = (CbCurrentCondition) msg.obj;
				try {
					db.open();
					db.addCondition(cc);
					db.close();
				} catch(SQLiteDatabaseLockedException dble) {
					// ...
				}
				break;
			case MSG_GET_CURRENT_CONDITIONS:
				recentMsg = msg;
				CbApiCall currentConditionAPI = (CbApiCall) msg.obj;
				ArrayList<CbCurrentCondition> conditions = getCurrentConditionsFromLocalAPI(currentConditionAPI);
				try {
					msg.replyTo.send(Message.obtain(null,
							MSG_CURRENT_CONDITIONS, conditions));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
				break;
			case MSG_SEND_CURRENT_CONDITION:
				CbCurrentCondition condition = (CbCurrentCondition) msg.obj;
				if (settingsHandler == null) {
					settingsHandler = new CbSettingsHandler(
							getApplicationContext());
					settingsHandler.setServerURL(serverURL);
					settingsHandler
							.setAppID(getApplication().getPackageName());
				}
				try {
					condition
							.setSharing_policy(settingsHandler.getShareLevel());
					sendCbCurrentCondition(condition);
				} catch (Exception e) {
					//e.printStackTrace();
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
				log("cbservice dead code, change notification");
				break;
			case MSG_API_RESULT_COUNT:
				log("cbservice msg_api_result_count");
				// Current conditions API call for (made for local conditions alerts)
				// returns here.
				
				// potentially notify about nearby conditions
				CbApiCall localConditions = buildLocalCurrentConditionsCall(1);
				ArrayList<CbCurrentCondition> localRecentConditions = getCurrentConditionsFromLocalAPI(localConditions);
				
				Intent intent = new Intent();
				intent.setAction(CbService.LOCAL_CONDITIONS_ALERT);
				if(localRecentConditions!=null) {
					if(localRecentConditions.size()> 0) {
						intent.putExtra("ca.cumulonimbus.pressurenetsdk.conditionNotification", localRecentConditions.get(0));
						sendBroadcast(intent);
					}
				}
				
				break;
			case MSG_MAKE_STATS_CALL:
				log("CbService received message to make stats API call");
				CbStatsAPICall statsCall =  (CbStatsAPICall) msg.obj;
				CbApi statsApi = new CbApi(getApplicationContext());
				statsApi.makeStatsAPICall(statsCall, service, msg.replyTo);
				break;
			case MSG_GET_CONTRIBUTIONS:
				CbContributions contrib = new CbContributions();
				db.open();
				contrib.setPressureAllTime(db.getAllTimePressureCount());
				contrib.setPressureLast24h(db.getLast24hPressureCount());
				contrib.setPressureLast7d(db.getLast7dPressureCount());
				contrib.setConditionsAllTime(db.getAllTimeConditionCount(getID()));
				contrib.setConditionsLastWeek(db.getLast7dConditionCount(getID()));
				contrib.setConditionsLastDay(db.getLastDayConditionCount(getID()));
				db.close();
				try {
					msg.replyTo.send(Message
							.obtain(null, MSG_CONTRIBUTIONS, contrib));
				} catch (RemoteException re) {
					// re.printStackTrace();
				}
				
				break;
			case MSG_GET_DATABASE_INFO:
				db.open();
				long localObsCount = db.getUserDataCount();
				db.close();
				log("SDKTESTS: CbService says localObsCount is " + localObsCount);
			    /*
				try {
					
					msg.replyTo.send(Message.obtain(null,
							MSG_COUNT_LOCAL_OBS_TOTALS,
							(int) countLocalObsOnly2, 0));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
				*/
				break;
			case MSG_GET_PRIMARY_APP:
				db.open();
				boolean primary = db.isPrimaryApp();
				int p = (primary==true) ? 1 : 0;
				db.close();
				try {
					
					msg.replyTo.send(Message.obtain(null,
							MSG_IS_PRIMARY, p, 0));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
				break;
			case MSG_GET_LOCAL_CONDITIONS:
				recentMsg = msg;
				CbApiCall localConditionsAPI = buildLocalCurrentConditionsCall(2);
				ArrayList<CbCurrentCondition> localCurrentConditions = getCurrentConditionsFromLocalAPI(localConditionsAPI);
				try {
					msg.replyTo.send(Message.obtain(null,
							MSG_LOCAL_CONDITIONS, localCurrentConditions));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	private ArrayList<CbCurrentCondition> getCurrentConditionsFromLocalAPI(CbApiCall currentConditionAPI) {
		ArrayList<CbCurrentCondition> conditions = new ArrayList<CbCurrentCondition>();
		try {
			db.open();
			Cursor ccCursor = db.getCurrentConditions(
					currentConditionAPI.getMinLat(),
					currentConditionAPI.getMaxLat(),
					currentConditionAPI.getMinLon(),
					currentConditionAPI.getMaxLon(),
					currentConditionAPI.getStartTime(),
					currentConditionAPI.getEndTime(), 1000);

			while (ccCursor.moveToNext()) {
				CbCurrentCondition cur = new CbCurrentCondition();
				Location location = new Location("network");
				double latitude = ccCursor.getDouble(1);
				double longitude = ccCursor.getDouble(2);
				location.setLatitude(latitude);
				location.setLongitude(longitude);
				cur.setLat(latitude);
				cur.setLon(longitude);
				location.setAltitude(ccCursor.getDouble(3));
				location.setAccuracy(ccCursor.getInt(4));
				location.setProvider(ccCursor.getString(5));
				cur.setLocation(location);
				cur.setSharing_policy(ccCursor.getString(6));
				cur.setTime(ccCursor.getLong(7));
				cur.setTzoffset(ccCursor.getInt(8));
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
		} catch (Exception e) {
			log("cbservice get_current_conditions failed " + e.getMessage());
		} finally {
			db.close();
		}
		return conditions;
	}
	
	
	private CbApiCall buildLocalCurrentConditionsCall(double hoursAgo) {
		log("building map conditions call for hours: "
				+ hoursAgo);
		long startTime = System.currentTimeMillis()
				- (int) ((hoursAgo * 60 * 60 * 1000));
		long endTime = System.currentTimeMillis();
		CbApiCall api = new CbApiCall();

		double minLat = 0;
		double maxLat = 0;
		double minLon = 0;
		double maxLon = 0;
		
		try {
			Location lastKnown = locationManager.getCurrentBestLocation();
			if(lastKnown.getLatitude() != 0) {
				minLat = lastKnown.getLatitude() - .1;
				maxLat = lastKnown.getLatitude() + .1;
				minLon = lastKnown.getLongitude() - .1;
				maxLon = lastKnown.getLongitude() + .1;
			} else {
				log("no location, bailing on csll");
				return null;
			}
				
			api.setMinLat(minLat);
			api.setMaxLat(maxLat);
			api.setMinLon(minLon);
			api.setMaxLon(maxLon);
			api.setStartTime(startTime);
			api.setEndTime(endTime);
			api.setLimit(500);
			api.setCallType("Conditions");
		} catch(NullPointerException npe) {
			// 
		}
		return api;
	}

	public void sendSingleObs() {
		if (settingsHandler != null) {
			if (settingsHandler.getServerURL() == null) {
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
			//npe.printStackTrace();
		}
		return false;
	}
	
	public boolean notifyAPIStats(Messenger reply, ArrayList<CbStats> statsResult) {
		try {
			if (reply == null) {
				log("cannot notify, reply is null");
			} else {
				log("cbservice notifying, " + statsResult.size());
				reply.send(Message.obtain(null, MSG_STATS, statsResult));
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
		} catch (Exception e) {
			obs.setObservationValue(pressure);
			return obs;
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
			//e.printStackTrace();
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
			//e.printStackTrace();
		} catch (IOException ioe) {
			//ioe.printStackTrace();
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
		if(CbConfiguration.DEBUG_MODE) {
			logToFile(message);
			System.out.println(message);
		}
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
