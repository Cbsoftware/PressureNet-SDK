package ca.cumulonimbus.pressurenetsdk;

import java.io.Serializable;

import android.location.Location;

/**
 * A description of the current weather 
 * at a specific location.
 * 
 * @author jacob
 *
 */
public class CbCurrentCondition extends CbWeather implements Serializable {

	private static final long serialVersionUID = 1344011178344527607L;
	private long time = 0;
	private int tzoffset = 0;
	private transient Location location;
	private String general_condition = "-";
	private String windy = "-";
	private String fog_thickness = "-";
	private String cloud_type = "-";
	private String precipitation_type = "-";
	private double precipitation_amount = 0;
	private String precipitation_unit = "-";
	private String thunderstorm_intensity = "-";
	private String user_id = "-";
	private String sharing_policy = "-";
	private String user_comment = "-";
	private double lat = 0;
	private double lon = 0;
	
	
	// We double-up on some location parameters because keeping them
	// inside Location prevents Serialization, which is required for Notifications.
	public double getLat() {
		return lat;
	}
	
	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLon() {
		return lon;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public String[] getCurrentConditionAsParams() {
		String[] params = {"daterecorded," + time, 
						   "tzoffset," + tzoffset,
						   "latitude," + location.getLatitude(),
						   "longitude," + location.getLongitude(),
						   "altitude," + location.getAltitude(),
						   "accuracy," + location.getAccuracy(),
						   "provider," + location.getProvider(),
						   "general_condition," + general_condition,
						   "windy," + windy,
						   "fog_thickness," + fog_thickness,
						   "cloud_type," + cloud_type,
						   "precipitation_type," + precipitation_type,
						   "precipitation_amount," + precipitation_amount,
						   "precipitation_unit," + precipitation_unit,
						   "thunderstorm_intensity," + thunderstorm_intensity,
						   "user_id," + user_id,
						   "sharing," + sharing_policy,
						   "user_comment," + user_comment,
						   "client_key," + "TODO",
		};
		return params;
	}
	
	
	
	@Override
	public String toString() {
		return user_id + ", " + time + ", " + tzoffset + ", " + location.getLatitude() + ", " + location.getLongitude() + ", " + 
				general_condition + "," + windy + ", " +  cloud_type + ", " + 
				precipitation_type + ", " + precipitation_amount + ", " + thunderstorm_intensity;
	}
	
	
	public Location getLocation() {
		return location;
	}



	public void setLocation(Location location) {
		this.location = location;
	}



	public String getUser_comment() {
		return user_comment;
	}
	public void setUser_comment(String user_comment) {
		this.user_comment = user_comment;
	}
	public String getUser_id() {
		return user_id;
	}
	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}
	public String getSharing_policy() {
		return sharing_policy;
	}
	public void setSharing_policy(String sharing_policy) {
		this.sharing_policy = sharing_policy;
	}
	public long getTime() {
		return time;
	}
	public int getTzoffset() {
		return tzoffset;
	}
	public void setTzoffset(int tzoffset) {
		this.tzoffset = tzoffset;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public String getGeneral_condition() {
		return general_condition;
	}
	public void setGeneral_condition(String general_condition) {
		this.general_condition = general_condition;
	}
	public String getWindy() {
		return windy;
	}
	public void setWindy(String windy) {
		this.windy = windy;
	}
	public String getFog_thickness() {
		return fog_thickness;
	}
	public void setFog_thickness(String fog_thickness) {
		this.fog_thickness = fog_thickness;
	}
	public String getCloud_type() {
		return cloud_type;
	}
	public void setCloud_type(String cloud_type) {
		this.cloud_type = cloud_type;
	}
	public String getPrecipitation_type() {
		return precipitation_type;
	}
	public void setPrecipitation_type(String precipitation_type) {
		this.precipitation_type = precipitation_type;
	}
	public double getPrecipitation_amount() {
		return precipitation_amount;
	}
	public void setPrecipitation_amount(double precipitation_amount) {
		this.precipitation_amount = precipitation_amount;
	}
	public String getPrecipitation_unit() {
		return precipitation_unit;
	}
	public void setPrecipitation_unit(String precipitation_unit) {
		this.precipitation_unit = precipitation_unit;
	}
	public String getThunderstorm_intensity() {
		return thunderstorm_intensity;
	}
	public void setThunderstorm_intensity(String thunderstorm_intensity) {
		this.thunderstorm_intensity = thunderstorm_intensity;
	}

}