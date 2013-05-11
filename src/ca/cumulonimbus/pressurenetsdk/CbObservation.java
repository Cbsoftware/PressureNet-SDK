package ca.cumulonimbus.pressurenetsdk;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.hardware.Sensor;
import android.location.Location;


/**
 * Store a single observation
 * Measurement value, measurement accuracy, location, location accuracy, time, id, privacy, client key
 * 
 * @author jacob
 *
 */
public class CbObservation {

	private String observationType = "-";
	private Location location;
	private Sensor sensor;
	private double observationValue = 0.0;
	private String observationUnit = "-";
	private String sharing = "-";
	private String user_id = "-";
	private long time = 0;
	private long timeZoneOffset = 0;
	private String clientKey = "";

	
	private Date jDate;
	private String trend = "";
	
	
	
	/**
	 * Raw data on the server does not include trends and the
	 * only date information is messy. Given an ArrayList, fix 
	 * those issues and return the new ArrayList.
	 * @return
	 */
	public static ArrayList<CbObservation> addDatesAndTrends(ArrayList<CbObservation> rawList) {
		ArrayList<CbObservation> fixedList = new ArrayList<CbObservation>();
		
		HashMap<String, ArrayList<CbObservation>> userMap = new HashMap<String, ArrayList<CbObservation>>();
		
		for(CbObservation current : rawList) {
			Date jD = new Date((long)current.getTime() + (long)current.getTimeZoneOffset());
			current.setjDate(jD);
			if(userMap.containsKey(current.getUser_id())) {
				userMap.get(current.getUser_id()).add(current);
			} else {
				ArrayList<CbObservation> newList = new ArrayList<CbObservation>();
				newList.add(current);
				userMap.put(current.getUser_id(), newList);
			}
		}
		System.out.println("there are " + userMap.size() + " users nearby who reported " + rawList.size() + " measurements");
	
		// Calculate the recent trend of this user's device readings
		// Simple, short, naive is okay for now; this estimate will be
		// looked at in aggregate with all nearby devices. 
		// for each user
		for (String id : userMap.keySet()) {
			ArrayList<CbObservation> obsList = userMap.get(id);
			String tendency = CbScience.findApproximateTendency(obsList);
			
			// TODO: improve this. dropping the large trend individually
			// into the values isn't a lasting solution
			for(CbObservation current : obsList) {
				current.setTrend(tendency);
				System.out.println("for id " + id + " setting trend " + tendency);
				fixedList.add(current);
			}
		}
		
		
		
		return fixedList;
	}
	
	
	@Override
	public String toString() {
		return "latitude," + location.getLatitude() + "\n" +
						   "longitude," + location.getLongitude() + "\n" +
						   "altitude," + location.getAltitude() + "\n" +
						   "accuracy," + location.getAccuracy() + "\n" +
						   "provider," + location.getProvider() + "\n" +
						   "observation_type," + observationType + "\n" +
						   "observation_unit," + observationUnit + "\n" +
						   "observation_value," + observationValue + "\n" +
						   "sharing," + sharing + "\n" +
						   "time," + time + "\n" +
						   "timezone," + timeZoneOffset + "\n" +
						   "user_id," + user_id  + "\n" ;
							// TODO: add sensor data
						   /*+
						   "sensor_name," + sensor.getName() + "\n" +
						   "sensor_type," + sensor.getType() + "\n" +
						   "sensor_vendor," + sensor.getVendor()  + "\n" +
						   "sensor_resolution," + sensor.getResolution() + "\n" +
						   "sensor_version," + sensor.getVersion();*/
	}
	
	public String[] getObservationAsParams() {
		String[] params = {"latitude," + location.getLatitude(), 
						   "longitude," + location.getLongitude(),
						   "altitude," + location.getAltitude(),
						   "location_accuracy," + location.getAccuracy(),
						   "provider," + location.getProvider(),
						   "observation_type," + observationType,
						   "observation_unit," + observationUnit,
						   "reading," + observationValue,
						   "sharing," + sharing,
						   "daterecorded," + time,
						   "tzoffset," + timeZoneOffset,
						   "user_id," + user_id,
						   "client_key," + clientKey, 
						   "reading_accuracy," + 0.0

						   /*
						   "sensor_name," + sensor.getName(),
						   "sensor_type," + sensor.getType() + "",
						   "sensor_vendor," + sensor.getVendor(),
						   "sensor_resolution," + sensor.getResolution() + "",
						   "sensor_version," + sensor.getVersion()  + ""
						   */
		};
		return params;
	}
	
	
	
	public String getClientKey() {
		return clientKey;
	}


	public void setClientKey(String clientKey) {
		this.clientKey = clientKey;
	}


	public Date getjDate() {
		return jDate;
	}
	public void setjDate(Date jDate) {
		this.jDate = jDate;
	}
	public String getTrend() {
		return trend;
	}
	public void setTrend(String trend) {
		this.trend = trend;
	}
	public Sensor getSensor() {
		return sensor;
	}
	public void setSensor(Sensor sensor) {
		this.sensor = sensor;
	}
	public String getObservationType() {
		return observationType;
	}
	public void setObservationType(String observationType) {
		this.observationType = observationType;
	}
	public Location getLocation() {
		return location;
	}
	public void setLocation(Location location) {
		this.location = location;
	}
	public double getObservationValue() {
		return observationValue;
	}
	public void setObservationValue(double observationValue) {
		this.observationValue = observationValue;
	}
	public String getObservationUnit() {
		return observationUnit;
	}
	public void setObservationUnit(String observationUnit) {
		this.observationUnit = observationUnit;
	}
	public String getSharing() {
		return sharing;
	}
	public void setSharing(String sharing) {
		this.sharing = sharing;
	}
	public String getUser_id() {
		return user_id;
	}
	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public long getTimeZoneOffset() {
		return timeZoneOffset;
	}
	public void setTimeZoneOffset(long timeZoneOffset) {
		this.timeZoneOffset = timeZoneOffset;
	}
	
	
}
