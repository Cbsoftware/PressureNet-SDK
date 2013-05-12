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
	private String format = "json";
	private String callType = "Readings";
	private ArrayList<CbObservation> observationResults;
	private ArrayList<CbCurrentCondition> conditionResults;
	
	@Override
	public String toString() {
		return callType + "," + global + ", " + sinceLastCall + ", " + minLat + ", " + maxLat + "," + minLon + "," + maxLon + "," + startTime + "," + endTime;
	}
	
	public static CbApiCall buildAPICall(boolean global, boolean sinceLastCall, int hoursAgo, double minLat, double maxLat, double minLon, double maxLon, String format, String apiKey) {
		long startTime = System.currentTimeMillis() - (hoursAgo * 60 * 60 * 1000);
		long endTime = System.currentTimeMillis();
		CbApiCall api = new CbApiCall();
		if(global) {
			api.setGlobal(true);
		} else {
			api.setMinLat(minLat);
			api.setMaxLat(maxLat);
			api.setMinLon(minLon);
			api.setMaxLon(maxLon);
		}
		if(sinceLastCall) {
			api.setSinceLastCall(true);
		} else {
			api.setStartTime(startTime);
			api.setEndTime(endTime);
		}
		api.setFormat(format);
		api.setApiKey(apiKey);
		return api;
	}
	
	
	
	public ArrayList<CbCurrentCondition> getConditionResults() {
		return conditionResults;
	}

	public void setConditionResults(ArrayList<CbCurrentCondition> conditionResults) {
		this.conditionResults = conditionResults;
	}

	public String getCallType() {
		return callType;
	}

	public void setCallType(String callType) {
		this.callType = callType;
	}

	public String getFormat() {
		return format;
	}
	public void setFormat(String format) {
		this.format = format;
	}
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
