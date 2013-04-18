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
	
	private boolean barometerReadingsActive = false;
	
    // Start getting barometer readings.
    public void setUpBarometerAndStartCollecting() {
    	try {
	    	sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	    	Sensor bar = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
	    	
	    	if(bar!=null) {
	    		barometerReadingsActive = sm.registerListener(this, bar, SensorManager.SENSOR_DELAY_NORMAL);
	    	}
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    }
    
    public void stopCollectingPressure() {
    	sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		sm.unregisterListener(this);
    }
    
	// TODO: Implement
	public CbObservation getPressureObservation() {
		CbObservation pressureObservation = new CbObservation();
		pressureObservation.setTime(System.currentTimeMillis());
		pressureObservation.setUser_id(userID);
		
		setUpBarometerAndStartCollecting();
		
		return pressureObservation;
	}
	
	public CbDataCollector(String userID, Context ctx) {
		this.userID = userID;
		this.context = ctx;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		System.out.println("sensor changed " + event.values.length + " returned values");
		
	}
}
