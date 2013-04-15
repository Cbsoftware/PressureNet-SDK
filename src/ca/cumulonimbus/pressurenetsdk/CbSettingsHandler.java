package ca.cumulonimbus.pressurenetsdk;

import android.content.Context;
import android.database.Cursor;

/**
 * Use Preferences to hold core pressureNET settings
 * Wrap and be compatible with Android Preferences
 * Collection and Submission Frequency, Privacy controls
 * 
 * @author jacob
 *
 */
public class CbSettingsHandler {

	// In use:
	// dataCollectionFrequency, serverURL, collectingData, sharingData
	
	// General Data Collection Settings
	private String appID = "";
	private long dataCollectionFrequency = 1000 * 60 * 5; // in ms. launch default: 5-10 minutes
	private boolean sendImmediately = true; // send right away. if false, check preference
	private boolean sendWiFiOnly = false; // if true, wait until wifi before sending
	private int sendBufferSize = 1; // gather this many before sending. 
	private String serverURL = ""; // may expand to array for multiple servers
	
	// Science Settings
	private boolean collectingData = true;
	private boolean sharingData = true;
	
	// Privacy Settings
	private String sharePressureLevel = "";
	private String shareHumidityLevel = "";
	private String shareTemperatureLevel = "";
	
	// Database
	private CbDb db;
	private Context context;
	
	/**
	 * Add or update application settings
	 */
	public void saveSettings() {
		System.out.println("Save settings");
		try {
			db = new CbDb(context);
			db.open();
			Cursor existing = db.fetchSettingByApp(appID);
			if (existing.getCount() < 1) {
				db.addSetting(appID, dataCollectionFrequency, serverURL);
			} else {
				db.updateSetting(appID, dataCollectionFrequency, serverURL);
			}
			db.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public CbSettingsHandler getSettings(String appID) {
		try {
			db = new CbDb(context);
			db.open();

			db.fetchSettingByApp(appID);
			
			db.close();			
		} catch(Exception e) {
			e.printStackTrace();
		}
		return this;
	}
	
	public CbSettingsHandler(Context ctx) {
		this.context = ctx;
	}

	
	public String getAppID() {
		return appID;
	}
	public void setAppID(String appID) {
		this.appID = appID;
	}
	public String getServerURL() {
		return serverURL;
	}
	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
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
