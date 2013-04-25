package ca.cumulonimbus.pressurenetsdk;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


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
	
    // Get a set of measurements
    public void getSomeMeasurements() {
    	try {
	    	sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	    	Sensor pressureSensor = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
	    	Sensor temperatureSensor = sm.getDefaultSensor(TYPE_AMBIENT_TEMPERATURE);
	    	Sensor humiditySensor = sm.getDefaultSensor(TYPE_RELATIVE_HUMIDITY); 
	    	
	    	if(pressureSensor!=null) {
	    		pressureReadingsActive = sm.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
	    	}
	    	if(temperatureSensor!=null) {
	    		temperatureReadingsActive = sm.registerListener(this, temperatureSensor, SensorManager.SENSOR_DELAY_NORMAL);
	    	}
	    	if(humiditySensor!=null) {
	    		humidityReadingsActive = sm.registerListener(this, humiditySensor, SensorManager.SENSOR_DELAY_NORMAL);
	    	}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    }
    
    public void stopCollectingData() {
    	sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		sm.unregisterListener(this);
    }
    
    
	/**
	 * Collect a full group of observations. The principle way data
	 * should be gathered.
	 * @return
	 */
    public CbObservationGroup getObservationGroup() {
        // TODO: Implement
    	return null;
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
		System.out.println("sensor changed: " + event.sensor.getName());
		if(event.sensor.getType() == Sensor.TYPE_PRESSURE) {
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
	}
}
