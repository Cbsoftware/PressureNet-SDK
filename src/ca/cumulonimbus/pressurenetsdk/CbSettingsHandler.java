package ca.cumulonimbus.pressurenetsdk;

/**
 * Use Preferences to hold core pressureNET settings
 * Wrap and be compatible with Android Preferences
 * Collection and Submission Frequency, Privacy controls
 * 
 * @author jacob
 *
 */
public class CbSettingsHandler {

	// General Data Collection Settings
	private long dataCollectionFrequency = 1000 * 60 * 10; // in ms. default: 10 minutes
	private boolean sendImmediately = true; // send right away. if false, check preference
	private boolean sendWiFiOnly = false; // if true, wait until wifi before sending
	private int sendBufferSize = 1; // gather this many before sending. 

	// Science Settings
	private boolean collectingData = true;
	private boolean sharingData = true;
	
	// Privacy Settings
	private String sharePressureLevel = "";
	private String shareHumidityLevel = "";
	private String shareTemperatureLevel = "";
	
	public CbSettingsHandler() {
		// TODO: Implement		
	}
	
	public boolean isCollectingData() {
		return collectingData;
	}
	public void setCollectingData(boolean collectingData) {
		this.collectingData = collectingData;
	}
	public boolean isSharingData() {
		return sharingData;
	}
	public void setSharingData(boolean sharingData) {
		this.sharingData = sharingData;
	}
	public long getDataCollectionFrequency() {
		return dataCollectionFrequency;
	}
	public void setDataCollectionFrequency(long dataCollectionFrequency) {
		this.dataCollectionFrequency = dataCollectionFrequency;
	}
	public boolean isSendImmediately() {
		return sendImmediately;
	}
	public void setSendImmediately(boolean sendImmediately) {
		this.sendImmediately = sendImmediately;
	}
	public boolean isSendWiFiOnly() {
		return sendWiFiOnly;
	}
	public void setSendWiFiOnly(boolean sendWiFiOnly) {
		this.sendWiFiOnly = sendWiFiOnly;
	}
	public int getSendBufferSize() {
		return sendBufferSize;
	}
	public void setSendBufferSize(int sendBufferSize) {
		this.sendBufferSize = sendBufferSize;
	}
	public String getSharePressureLevel() {
		return sharePressureLevel;
	}
	public void setSharePressureLevel(String sharePressureLevel) {
		this.sharePressureLevel = sharePressureLevel;
	}
	public String getShareHumidityLevel() {
		return shareHumidityLevel;
	}
	public void setShareHumidityLevel(String shareHumidityLevel) {
		this.shareHumidityLevel = shareHumidityLevel;
	}
	public String getShareTemperatureLevel() {
		return shareTemperatureLevel;
	}
	public void setShareTemperatureLevel(String shareTemperatureLevel) {
		this.shareTemperatureLevel = shareTemperatureLevel;
	}

}
