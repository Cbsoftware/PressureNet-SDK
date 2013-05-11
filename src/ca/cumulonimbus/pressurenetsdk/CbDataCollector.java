package ca.cumulonimbus.pressurenetsdk;

import java.util.ArrayList;

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
	
    public ArrayList<CbObservation> getRecentObservations() {
		return recentObservations;
	}

	
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

	public void startCollectingData(Messenger m) {
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
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    }
    
    public void stopCollectingData() {
    	sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		sm.unregisterListener(this);
		streaming = false;
    }
    
    
	public CbObservation getPressureObservation() {
		CbObservation pressureObservation = new CbObservation();
		pressureObservation.setTime(System.currentTimeMillis());
		pressureObservation.setUser_id(userID);
		pressureObservation.setObservationType(Sensor.TYPE_PRESSURE + ""); // TODO: Fix hack
		pressureObservation.setObservationValue(recentPressureReading);
		pressureObservation.setObservationUnit("mbar");
		pressureObservation.setSensor(sm.getSensorList(Sensor.TYPE_PRESSURE).get(0));
		return pressureObservation;
	}
	
	public CbDataCollector(String userID, Context ctx) {
		this.userID = userID;
		this.context = ctx;
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
			System.out.println("new pressure reading " + event.values[0]);
			recentPressureReading = event.values[0];
			lastPressureTime = System.currentTimeMillis();
		} else if(event.sensor.getType() == TYPE_RELATIVE_HUMIDITY) {
			recentHumidityReading = event.values[0];
			lastHumidityTime = System.currentTimeMillis();
		} else if(event.sensor.getType() == TYPE_AMBIENT_TEMPERATURE) {
			recentTemperatureReading = event.values[0];
			lastTemperatureTime = System.currentTimeMillis();
		}
		
		if(streaming) {
			CbObservation observation = getPressureObservation();
			CbLocationManager locationManager = new CbLocationManager(context);
			observation.setLocation(locationManager
					.getCurrentBestLocation());
			recentObservations.add(observation);
			CbDb db = new CbDb(context);
			db.open();
			long result = db.addObservation(observation );
			//System.out.println("streaming db add, result count " + result);
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
}
