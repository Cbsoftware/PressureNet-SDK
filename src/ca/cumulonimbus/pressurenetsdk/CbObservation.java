package ca.cumulonimbus.pressurenetsdk;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.hardware.Sensor;
import android.location.Location;

/**
 * Store a single observation Measurement value, measurement accuracy, location,
 * location accuracy, time, id, privacy, client key
 * 
 * @author jacob
 * 
 */
public class CbObservation extends CbWeather {

	private String observationType = "-";
	private Location location;
	private Sensor sensor;
	private double observationValue = 0.0;
	private String observationUnit = "-";
	private String sharing = "-";
	private String user_id = "-";
	private long time = 0;
	private long timeZoneOffset = 0;
	private String clientKey = "-";

	private String versionNumber = "-";
	private String packageName = "-";
	
	private String modelType = "-";
	private String isCharging = "-";
	
	@Override
	public String toString() {
		return "latitude," + location.getLatitude() + "\n" + "longitude,"
				+ location.getLongitude() + "\n" + "altitude,"
				+ location.getAltitude() + "\n" + "accuracy,"
				+ location.getAccuracy() + "\n" + "provider,"
				+ location.getProvider() + "\n" + "observation_type,"
				+ observationType + "\n" + "observation_unit,"
				+ observationUnit + "\n" + "observation_value,"
				+ observationValue + "\n" + "sharing," + sharing + "\n"
				+ "time," + time + "\n" 
				+ "timezone," + timeZoneOffset + "\n"
				+ "user_id," + user_id + "\n" 
				+ "version_number," + versionNumber + "\n"
				+ "package_name," + packageName + "\n"
				+ "model_type," + modelType + "\n"
				+ "is_charging," + isCharging + "\n";
	}

	public String[] getObservationAsParams() {
		String[] params = { "latitude," + location.getLatitude(),
				"longitude," + location.getLongitude(),
				"altitude," + location.getAltitude(),
				"location_accuracy," + location.getAccuracy(),
				"provider," + location.getProvider(),
				"observation_type," + observationType,
				"observation_unit," + observationUnit,
				"reading," + observationValue, "sharing," + sharing,
				"daterecorded," + time, "tzoffset," + timeZoneOffset,
				"user_id," + user_id, "client_key," + clientKey,
				"reading_accuracy," + 0.0,
				"version_number," + versionNumber,
				"package_name," + packageName,
				"model_type," + versionNumber,
				"is_charging," + isCharging
		};
		return params;
	}
	
	
	
	public String getModelType() {
		return modelType;
	}

	public void setModelType(String modelType) {
		this.modelType = modelType;
	}

	public String getIsCharging() {
		return isCharging;
	}

	public void setIsCharging(String isCharging) {
		this.isCharging = isCharging;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getVersionNumber() {
		return versionNumber;
	}

	public void setVersionNumber(String versionNumber) {
		this.versionNumber = versionNumber;
	}

	public String getClientKey() {
		return clientKey;
	}

	public void setClientKey(String clientKey) {
		this.clientKey = clientKey;
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
