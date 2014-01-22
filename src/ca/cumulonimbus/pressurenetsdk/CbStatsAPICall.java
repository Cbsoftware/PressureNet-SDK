package ca.cumulonimbus.pressurenetsdk;

import java.util.ArrayList;

public class CbStatsAPICall {
	private double minLatitude;
	private double maxLatitude;
	private double minLongitude;
	private double maxLongitude;
	private long startTime;
	private long endTime;
	private String logDuration;

	@Override
	public String toString() {
		return minLatitude + ", " + maxLatitude + "," + minLongitude + "," + maxLongitude + "," + startTime + "," + endTime + "," + logDuration;
	}
	
	public CbStatsAPICall (double minLat, double maxLat, double minLon, double maxLon, long startTime, long endTime, String duration) {
		this.minLatitude = minLat;
		this.maxLatitude = maxLat;
		this.minLongitude = minLon;
		this.maxLongitude = maxLon;
		this.startTime = startTime;
		this.endTime = endTime;
		this.logDuration = duration;
	}
	
	public CbStatsAPICall() {
		
	}

	public double getMinLatitude() {
		return minLatitude;
	}

	public void setMinLatitude(double minLatitude) {
		this.minLatitude = minLatitude;
	}

	public double getMaxLatitude() {
		return maxLatitude;
	}

	public void setMaxLatitude(double maxLatitude) {
		this.maxLatitude = maxLatitude;
	}

	public double getMinLongitude() {
		return minLongitude;
	}

	public void setMinLongitude(double minLongitude) {
		this.minLongitude = minLongitude;
	}

	public double getMaxLongitude() {
		return maxLongitude;
	}

	public void setMaxLongitude(double maxLongitude) {
		this.maxLongitude = maxLongitude;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public String getLogDuration() {
		return logDuration;
	}

	public void setLogDuration(String logDuration) {
		this.logDuration = logDuration;
	}
	
	
	
}
