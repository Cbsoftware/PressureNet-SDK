package ca.cumulonimbus.pressurenetsdk;

import java.util.ArrayList;

public class CbApiCall {

	private boolean global = false;
	private boolean sinceLastCall = false;
	private double minLat = 0.0;
	private double maxLat = 0.0;
	private double minLon = 0.0;
	private double maxLon = 0.0;
	private String apiKey = "";
	private long startTime = 0;
	private long endTime = 0;
	ArrayList<CbObservation> observationResults;
	
	
	public String getApiKey() {
		return apiKey;
	}
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	public boolean isGlobal() {
		return global;
	}
	public void setGlobal(boolean global) {
		this.global = global;
	}
	public boolean isSinceLastCall() {
		return sinceLastCall;
	}
	public void setSinceLastCall(boolean sinceLastCall) {
		this.sinceLastCall = sinceLastCall;
	}
	public double getMinLat() {
		return minLat;
	}
	public void setMinLat(double minLat) {
		this.minLat = minLat;
	}
	public double getMaxLat() {
		return maxLat;
	}
	public void setMaxLat(double maxLat) {
		this.maxLat = maxLat;
	}
	public double getMinLon() {
		return minLon;
	}
	public void setMinLon(double minLon) {
		this.minLon = minLon;
	}
	public double getMaxLon() {
		return maxLon;
	}
	public void setMaxLon(double maxLon) {
		this.maxLon = maxLon;
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
	public ArrayList<CbObservation> getObservationResults() {
		return observationResults;
	}
	public void setObservationResults(ArrayList<CbObservation> observationResults) {
		this.observationResults = observationResults;
	}
}
