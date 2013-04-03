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

	private String observationType;
	private Location location;
	private double observationValue;
	private String observationUnit;
	private String sharing;
	private String user_id;
	private double time;
	private double timeZoneOffset;
	
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
