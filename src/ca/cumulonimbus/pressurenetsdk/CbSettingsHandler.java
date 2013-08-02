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
	
	// General Data Collection Settings
	private String appID = "";
	private long dataCollectionFrequency = 1000 * 60 * 10; // in ms. launch default: 5-10 minutes
	private String serverURL = ""; // may expand to array for multiple servers
	private boolean onlyWhenCharging = false; // only run when charging
	private boolean sendNotifications = false;
	
	private boolean useGPS = false;
	
	// Science Settings
	private boolean collectingData = true;
	private boolean sharingData = true;
	
	// Privacy Settings
	private String shareLevel = "Us, Researchers and Forecasters";	

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
				db.addSetting(appID, dataCollectionFrequency, serverURL, onlyWhenCharging, collectingData, sharingData, shareLevel, sendNotifications, useGPS);
			} else {
				db.updateSetting(appID, dataCollectionFrequency, serverURL, onlyWhenCharging, collectingData, sharingData, shareLevel, sendNotifications, useGPS);
			}
			db.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public CbSettingsHandler getSettings() {
		String appID = "ca.cumulonimbus.barometernetwork";
		try {
			db = new CbDb(context);
			db.open();

			Cursor settings = db.fetchAllSettings();
			while(settings.moveToNext()) {
				// TODO: fix and fill out all fields
				this.onlyWhenCharging = (settings.getInt(4) == 1) ? true: false;
				this.useGPS = (settings.getInt(9) == 1) ? true : false;
				/*
				KEY_ROW_ID, KEY_APP_ID,
				KEY_DATA_COLLECTION_FREQUENCY, KEY_SERVER_URL, KEY_SEND_NOTIFICATIONS, KEY_USE_GPS, KEY_ONLY_WHEN_CHARGING}, KEY_APP_ID
				+ "='" + appID + "'", null, null, null, null
			*/
			}
			System.out.println("get settings gps " + this.useGPS);
			db.close();			
		} catch(Exception e) {
			e.printStackTrace();
		}
		return this;
	}
	
	public CbSettingsHandler(Context ctx) {
		this.context = ctx;
	}
	
	public boolean isUseGPS() {
		return useGPS;
	}

	public void setUseGPS(boolean useGPS) {
		this.useGPS = useGPS;
	}

	public boolean isSendNotifications() {
		return sendNotifications;
	}

	public void setSendNotifications(boolean sendNotifications) {
		this.sendNotifications = sendNotifications;
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
	public String getShareLevel() {
		return shareLevel;
	}
	public void setShareLevel(String shareLevel) {
		this.shareLevel = shareLevel;
	}
	public boolean isOnlyWhenCharging() {
		return onlyWhenCharging;
	}
	public void setOnlyWhenCharging(boolean onlyWhenCharging) {
		this.onlyWhenCharging = onlyWhenCharging;
	}
}
