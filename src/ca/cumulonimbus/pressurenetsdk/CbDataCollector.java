package ca.cumulonimbus.pressurenetsdk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;


/**
 * Collect data from onboard sensors and store locally
 * 
 * @author jacob
 *
 */
public class CbDataCollector implements SensorEventListener{

	private String userID = "";
	private SensorManager sm;
	private Context context;
	
	private String mAppDir = "";
	
	// TODO: Keep a list of recent readings rather than single values
	private double recentPressureReading = 0.0;
	private int recentPressureAccuracy = 0;
	private long lastPressureTime = 0;
	
	private double recentHumidityReading = 0.0;
	private int recentHumidityAccuracy = 0;
	private long lastHumidityTime = 0;
	
	private double recentTemperatureReading = 0.0;
	private int recentTemperatureAccuracy = 0;
	private long lastTemperatureTime = 0;
	
	private boolean pressureReadingsActive = false;
	private boolean humidityReadingsActive = false;
	private boolean temperatureReadingsActive = false;
	
	private final int TYPE_AMBIENT_TEMPERATURE = 13;
	private final int TYPE_RELATIVE_HUMIDITY = 12;
	
	private ArrayList<CbObservation> recentObservations = new ArrayList<CbObservation>();
	
	private boolean streaming = false;
	
	private Messenger msgr;
	
	private String sharingLevel = "";
	
	private CbSettingsHandler settings;
	
    public ArrayList<CbObservation> getRecentObservations() {
		return recentObservations;
	}

	/**
	 * Access the database to fetch recent, locally-recorded observations 
	 * @return
	 */
    public ArrayList<CbObservation> getRecentDatabaseObservations() {
    	ArrayList<CbObservation> recentDbList = new ArrayList<CbObservation>();
    	CbDb db = new CbDb(context);
		db.open();
    	Cursor c = db.fetchAllObservations();
		while(c.moveToNext()) {
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

			// TODO: Add sensor information
			
			recentDbList.add(obs);
		}
    	
		db.close();
		return recentDbList;
	}
    
	public void setRecentObservations(ArrayList<CbObservation> recentObservations) {
		this.recentObservations = recentObservations;
	}

	/**
	 * Start collecting sensor data. 
	 * 
	 * @param m
	 * @return
	 */
	public int startCollectingData(Messenger m) {
		this.msgr = m;
    	streaming = true;
    	try {
	    	sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	    	Sensor pressureSensor = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
	    	Sensor temperatureSensor = sm.getDefaultSensor(TYPE_AMBIENT_TEMPERATURE);
	    	Sensor humiditySensor = sm.getDefaultSensor(TYPE_RELATIVE_HUMIDITY); 
	    	
	    	if(pressureSensor!=null) {
	    		pressureReadingsActive = sm.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_UI);
	    	}
	    	if(temperatureSensor!=null) {
	    		temperatureReadingsActive = sm.registerListener(this, temperatureSensor, SensorManager.SENSOR_DELAY_UI);
	    	}
	    	if(humiditySensor!=null) {
	    		humidityReadingsActive = sm.registerListener(this, humiditySensor, SensorManager.SENSOR_DELAY_UI);
	    	}
	    	return 1;
    	} catch(Exception e) {
    		e.printStackTrace();
    		return -1;
    	}
    }
    
	/**
	 * Stop collecting sensor data
	 */
    public void stopCollectingData() {
    	sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		sm.unregisterListener(this);
		streaming = false;
    }
    
    
	public CbObservation getPressureObservation() {
		CbObservation pressureObservation = new CbObservation();
		pressureObservation.setTime(System.currentTimeMillis());
		pressureObservation.setUser_id(userID);
		pressureObservation.setObservationType("pressure");
		pressureObservation.setObservationValue(recentPressureReading);
		pressureObservation.setObservationUnit("mbar");
		pressureObservation.setSensor(sm.getSensorList(Sensor.TYPE_PRESSURE).get(0));
		pressureObservation.setSharing(settings.getShareLevel());
		log("share level " + settings.getShareLevel() + " " + userID);
		return pressureObservation;
	}
	
	public CbDataCollector(String userID, Context ctx) {
		this.userID = userID;
		this.context = ctx;
		settings = new CbSettingsHandler(ctx);
		settings = settings.getSettings();
		setUpFiles();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		if(sensor.getType() == Sensor.TYPE_PRESSURE) {
			recentPressureAccuracy = accuracy;
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_PRESSURE) {
			log("new pressure reading " + event.values[0]);
			recentPressureReading = event.values[0];
			lastPressureTime = System.currentTimeMillis();
		} else if(event.sensor.getType() == TYPE_RELATIVE_HUMIDITY) {
			recentHumidityReading = event.values[0];
			lastHumidityTime = System.currentTimeMillis();
		} else if(event.sensor.getType() == TYPE_AMBIENT_TEMPERATURE) {
			recentTemperatureReading = event.values[0];
			lastTemperatureTime = System.currentTimeMillis();
		}
		stopCollectingData();
		if(streaming) {
			CbObservation observation = getPressureObservation();
			CbLocationManager locationManager = new CbLocationManager(context);
			observation.setLocation(locationManager
					.getCurrentBestLocation());
			recentObservations.add(observation);
			CbDb db = new CbDb(context);
			db.open();
			long result = db.addObservation(observation );
			log("streaming db add, result count " + result);
			db.close();
			
			if(msgr!=null) {
				try {
					msgr.send(Message.obtain(null,
							CbService.MSG_DATA_STREAM, observation));
				} catch (RemoteException re) {
					re.printStackTrace();
				}
			}
			
			
		} else {
			stopCollectingData();
		}
	}
	
	/** 
	 * Log data to SD card for debug purposes.
	 * To enable logging, ensure the Manifest allows writing to SD card.
	 * 
	 * @param text
	 */
	private void logToFile(String text) {
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
	
	/**
	 * Prepare to write a log to SD card. Not used unless logging enabled.
	 */
	private void setUpFiles() {
		try {
			File homeDirectory = context.getExternalFilesDir(null);
			if (homeDirectory != null) {
				mAppDir = homeDirectory.getAbsolutePath();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Log
	 */
	public void log(String message) {
		System.out.println(message);
		logToFile(message);
	}
}
