package ca.cumulonimbus.pressurenetsdk;

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
	private double observationValue = 0.0;
	private String observationUnit = "-";
	private String sharing = "-";
	private String user_id = "-";
	private double time = 0;
	private double timeZoneOffset = 0;
	
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
						   "user_id," + user_id;
	}
	
	public String[] getObservationAsParams() {
		String[] params = {"latitude," + location.getLatitude(), 
						   "longitude," + location.getLongitude(),
						   "altitude," + location.getAltitude(),
						   "accuracy," + location.getAccuracy(),
						   "provider," + location.getProvider(),
						   "observation_type," + observationType,
						   "observation_unit," + observationUnit,
						   "observation_value," + observationValue,
						   "sharing," + sharing,
						   "time," + time,
						   "timezone," + timeZoneOffset,
						   "user_id," + user_id,
		};
		return params;
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
	public double getTime() {
		return time;
	}
	public void setTime(double time) {
		this.time = time;
	}
	public double getTimeZoneOffset() {
		return timeZoneOffset;
	}
	public void setTimeZoneOffset(double timeZoneOffset) {
		this.timeZoneOffset = timeZoneOffset;
	}
	
	
}
