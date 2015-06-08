package ca.cumulonimbus.pressurenetsdk;

import android.content.Context;
import android.database.Cursor;

/**
 * Use Preferences to hold core PressureNet settings
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
	
	@Override
	public String toString() {
		return "Settings: \n" +
				"serverURL " + serverURL + "\n" + 
			   "appID " + appID + "\n" + 
			   "dataCollectionFrequency " + dataCollectionFrequency + "\n" +
			   "apponlyWhenCharging " + onlyWhenCharging + "\n" +
			   "sendNotifications " + sendNotifications + "\n" +
			   "shareLevel " + shareLevel + "\n" +
			   "useGPS " + useGPS + "\n" + 
			   "isCollectingData " + collectingData + "\n" + 
			   "isSharingData " + sharingData + "\n";}

	/**
	 * Add or update application settings
	 */
	public void saveSettings() {
		try {
			db = new CbDb(context);
			db.open();
			Cursor existing = db.fetchAllSettings();
			if (existing.getCount() < 1) {
				db.addSetting(appID, dataCollectionFrequency, serverURL, onlyWhenCharging, collectingData, sharingData, shareLevel, sendNotifications, useGPS);
			} else {
				db.updateSetting(appID, dataCollectionFrequency, serverURL, onlyWhenCharging, collectingData, sharingData, shareLevel, sendNotifications, useGPS);
			}
			db.close();
		} catch(Exception e) {
			//e.printStackTrace();
		}
	}
	
	public CbSettingsHandler getSettings() {
		try {
			db = new CbDb(context);
			db.open();

			Cursor settings = db.fetchSettingByApp(context.getPackageName());
			
			if(settings.moveToFirst()) {
				this.appID = settings.getString(1);
				this.dataCollectionFrequency = settings.getLong(2);
				this.serverURL = CbConfiguration.SERVER_URL_PRESSURENET;
				this.sendNotifications = settings.getInt(4) > 0;
				this.useGPS = settings.getInt(5) > 0 ;
				this.onlyWhenCharging = settings.getInt(6) > 0;
				this.sharingData = (settings.getInt(7) > 0 ) ? true : false;
				this.collectingData = (settings.getInt(8) > 0 ) ? true : false;
				this.shareLevel = settings.getString(9);
			}
			db.close();
			//System.out.println("getting settings for app " + context.getPackageName() );
			//System.out.println(this);
			return this;
		} catch(Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	public CbSettingsHandler(Context ctx) {
		this.appID = ctx.getPackageName();
		this.serverURL = CbConfiguration.SERVER_URL_PRESSURENET;
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
