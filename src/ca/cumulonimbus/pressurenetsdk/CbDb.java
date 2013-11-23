package ca.cumulonimbus.pressurenetsdk;

import java.util.ArrayList;
import java.util.Random;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

/**
 * Keep track of app settings, as this SDK may be used by more than one app on a
 * device. This allows an empty Intent to start the Service generically, and we
 * can then read saved settings for each registered app to act accordingly.
 * 
 * @author jacob
 * 
 */

public class CbDb {

	// Tables
	public static final String SETTINGS_TABLE = "cb_settings";
	public static final String OBSERVATIONS_TABLE = "cb_observations";
	public static final String CURRENT_CONDITIONS_TABLE = "cb_current_conditions";
	public static final String API_LIST_TABLE = "cb_api_list";
	
	// Settings Fields
	public static final String KEY_ROW_ID = "_id";
	public static final String KEY_APP_ID = "app_id";
	public static final String KEY_DATA_COLLECTION_FREQUENCY = "data_frequency";
	public static final String KEY_SERVER_URL = "server_url";
	public static final String KEY_ONLY_WHEN_CHARGING = "only_when_charging";
	public static final String KEY_COLLECTING_DATA = "collecting_data";
	public static final String KEY_SHARING_DATA = "sharing_data";
	public static final String KEY_SHARE_LEVEL = "share_level";
	public static final String KEY_SEND_NOTIFICATIONS = "send_notifications";

	// Observation Fields
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_ALTITUDE = "altitude";
	public static final String KEY_ACCURACY = "accuracy";
	public static final String KEY_PROVIDER = "provider";
	public static final String KEY_OBSERVATION_TYPE = "observation_type";
	public static final String KEY_OBSERVATION_UNIT = "observation_unit";
	public static final String KEY_OBSERVATION_VALUE = "observation_value";
	public static final String KEY_SHARING = "sharing";
	public static final String KEY_TIME = "time";
	public static final String KEY_TIMEZONE = "timezone";
	public static final String KEY_USERID = "user_id";
	public static final String KEY_SENSOR_NAME = "sensor_name";
	public static final String KEY_SENSOR_TYPE = "sensor_type";
	public static final String KEY_SENSOR_VENDOR = "sensor_vendor";
	public static final String KEY_SENSOR_RESOLUTION = "sensor_resolution";
	public static final String KEY_SENSOR_VERSION = "sensor_version";
	public static final String KEY_OBSERVATION_TREND = "observation_trend";

	// Current Conditions Fields
	// + KEY_LATITUDE, KEY_LONGITUDE, KEY_ALTITUDE, KEY_ACCURACY, KEY_PROVIDER,
	// KEY_TIME, KEY_TIMEZONE, KEY_SHARING, KEY_USERID,
	public static final String KEY_GENERAL_CONDITION = "general_condition";
	public static final String KEY_WINDY = "windy";
	public static final String KEY_FOGGY = "foggy";
	public static final String KEY_CLOUD_TYPE = "cloud_type";
	public static final String KEY_PRECIPITATION_TYPE = "precipitation_type";
	public static final String KEY_PRECIPITATION_AMOUNT = "precipitation_amount";
	public static final String KEY_PRECIPITATION_UNIT = "precipitation_unit";
	public static final String KEY_THUNDERSTORM_INTENSITY = "thunderstorm_intensity";
	public static final String KEY_USER_COMMENT = "user_comment";

	// Observation /list/
	public static final String KEY_MIN_LAT = "min_lat";
	public static final String KEY_MAX_LAT = "max_lat";
	public static final String KEY_MIN_LON = "min_lon";
	public static final String KEY_MAX_LON = "max_lon";
	
	public static final String KEY_USE_GPS = "use_gps";
	
	private Context mContext;

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDB;
	private static final String SETTINGS_TABLE_CREATE = "create table "
			+ SETTINGS_TABLE + " (_id integer primary key autoincrement, "
			+ KEY_APP_ID + " text not null, " + KEY_DATA_COLLECTION_FREQUENCY + " real not null, " 
			+ KEY_SERVER_URL + " text not null, " 
			+ KEY_ONLY_WHEN_CHARGING + " text, "
			+ KEY_COLLECTING_DATA + " text, "
			+ KEY_SHARING_DATA + " text," 
			+ KEY_SHARE_LEVEL + " text," 
			+ KEY_SEND_NOTIFICATIONS + " text,"
			+ KEY_USE_GPS + " text)";

	private static final String OBSERVATIONS_TABLE_CREATE = "create table "
			+ OBSERVATIONS_TABLE + " (_id integer primary key autoincrement, "
			+ KEY_LATITUDE + " real not null, " + KEY_LONGITUDE
			+ " real not null, " + KEY_ALTITUDE + " real not null, "
			+ KEY_ACCURACY + " real not null, " + KEY_PROVIDER
			+ " text not null, " + KEY_OBSERVATION_TYPE + " text not null, "
			+ KEY_OBSERVATION_UNIT + " text not null, " + KEY_OBSERVATION_VALUE
			+ " real not null, " + KEY_SHARING + " text not null, " + KEY_TIME
			+ " real not null, " + KEY_TIMEZONE + " real not null, "
			+ KEY_USERID + " text not null, " + KEY_SENSOR_NAME
			+ " text , " + KEY_SENSOR_TYPE + " real , "
			+ KEY_SENSOR_VENDOR + " text , " + KEY_SENSOR_RESOLUTION
			+ " real , " + KEY_SENSOR_VERSION + " real ,"
			+ KEY_OBSERVATION_TREND + " text," + "UNIQUE (" + KEY_LATITUDE
			+ ", " + KEY_LONGITUDE + "," + KEY_TIME + ", " + KEY_USERID + ","
			+ KEY_OBSERVATION_VALUE + ") ON CONFLICT REPLACE)";
	
	
	private static final String API_LIST_TABLE_CREATE = "create table "
			+ API_LIST_TABLE + " (_id integer primary key autoincrement, "
			+ KEY_LATITUDE + " real not null, " 
			+ KEY_LONGITUDE + " real not null, " + KEY_TIME
			+ " real not null, " + KEY_OBSERVATION_VALUE
			+ " real not null, UNIQUE (" + KEY_OBSERVATION_VALUE +", " + KEY_TIME + ") ON CONFLICT REPLACE)";

	private static final String CURRENT_CONDITIONS_TABLE_CREATE = "create table "
			+ CURRENT_CONDITIONS_TABLE
			+ " (_id integer primary key autoincrement, "
			+ KEY_LATITUDE
			+ " real not null, "
			+ KEY_LONGITUDE
			+ " real not null, "
			+ KEY_ALTITUDE
			+ " real not null, "
			+ KEY_ACCURACY
			+ " real not null, "
			+ KEY_PROVIDER
			+ " text not null, "
			+ KEY_SHARING
			+ " text not null, "
			+ KEY_TIME
			+ " real not null, "
			+ KEY_TIMEZONE
			+ " real not null, "
			+ KEY_USERID
			+ " text not null, "
			+ KEY_GENERAL_CONDITION
			+ " text not null, "
			+ KEY_WINDY
			+ " text not null, "
			+ KEY_FOGGY
			+ " text not null, "
			+ KEY_CLOUD_TYPE
			+ " text not null, "
			+ KEY_PRECIPITATION_TYPE
			+ " text not null, "
			+ KEY_PRECIPITATION_AMOUNT
			+ " real not null, "
			+ KEY_PRECIPITATION_UNIT
			+ " text not null, "
			+ KEY_THUNDERSTORM_INTENSITY
			+ " real not null, "
			+ KEY_USER_COMMENT + " text not null, " + "UNIQUE (" + KEY_LATITUDE
			+ ", " + KEY_LONGITUDE + "," + KEY_TIME + ","
			+ KEY_GENERAL_CONDITION + ") ON CONFLICT REPLACE)";

	private static final String DATABASE_NAME = "CbDb";
	private static final int DATABASE_VERSION = 40;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SETTINGS_TABLE_CREATE);
			db.execSQL(OBSERVATIONS_TABLE_CREATE);
			db.execSQL(CURRENT_CONDITIONS_TABLE_CREATE);
			db.execSQL(API_LIST_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Build upgrade mechanism
			db.execSQL("DROP TABLE IF EXISTS " + SETTINGS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + OBSERVATIONS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + CURRENT_CONDITIONS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + API_LIST_TABLE);			
			onCreate(db);
		}
	}
	
	
	/**
	 * Return the total number of recorded and stored measurements
	 * that came directly from this device
	 * @return
	 */
	public long getUserDataCount() {
		return DatabaseUtils.queryNumEntries(mDB,  OBSERVATIONS_TABLE,
                null,null);
	}
	
	/**
	 * This service caches data from the pressureNET API to improve app performance.
	 * Return the number of cached measurements. 
	 * @return
	 */
	public long getDataCacheCount() {
		return DatabaseUtils.queryNumEntries(mDB, API_LIST_TABLE,
                        null,null) + DatabaseUtils.queryNumEntries(mDB,  CURRENT_CONDITIONS_TABLE,
                                null,null);
	}

	/**
	 * Keep the database cache clean
	 */
	public void deleteOldCacheData() {
		long hoursAgo = 24*3;
		long timeAgo = System.currentTimeMillis() - (1000 * 60 * 60 * hoursAgo);
		mDB.execSQL("delete from " + API_LIST_TABLE + " WHERE " + KEY_TIME + " < " + timeAgo);
		mDB.execSQL("delete from " + CURRENT_CONDITIONS_TABLE + " WHERE " + KEY_TIME + " < " + timeAgo);
	}
	
	/**
	 * Get local current conditions
	 * 
	 * @return
	 */
	public Cursor getCurrentConditions(double min_lat, double max_lat,
			double min_lon, double max_lon, long start_time, long end_time,
			double limit) {
		Cursor cursor = mDB.query(false, CURRENT_CONDITIONS_TABLE,
				new String[] { KEY_ROW_ID, KEY_LATITUDE, KEY_LONGITUDE,
						KEY_ALTITUDE, KEY_ACCURACY, KEY_PROVIDER, KEY_SHARING,
						KEY_TIME, KEY_TIMEZONE, KEY_USERID,
						KEY_GENERAL_CONDITION, KEY_WINDY, KEY_FOGGY,
						KEY_CLOUD_TYPE, KEY_PRECIPITATION_TYPE,
						KEY_PRECIPITATION_AMOUNT, KEY_PRECIPITATION_UNIT,
						KEY_THUNDERSTORM_INTENSITY, KEY_USER_COMMENT },
				KEY_LATITUDE + " > ? and " + KEY_LATITUDE + " < ? and "
						+ KEY_LONGITUDE + " > ? and " + KEY_LONGITUDE
						+ " < ? and " + KEY_TIME + " > ? and " + KEY_TIME
						+ " < ? ", new String[] { min_lat + "", max_lat + "",
						min_lon + "", max_lon + "", start_time + "",
						end_time + "" }, null, null, null, null);
		return cursor;
	}

	/**
	 * Run an API call against the API cache
	 * 
	 * @return
	 */
	public Cursor runAPICacheCall(double min_lat, double max_lat,
			double min_lon, double max_lon, long start_time, long end_time,
			double limit) {
		Cursor cursor = mDB.query(false, API_LIST_TABLE, new String[] {
				KEY_ROW_ID, KEY_LATITUDE, KEY_LONGITUDE,
				KEY_OBSERVATION_VALUE, 
				KEY_TIME }, KEY_LATITUDE
				+ " > ? and " + KEY_LATITUDE + " < ? and " + KEY_LONGITUDE
				+ " > ? and " + KEY_LONGITUDE + " < ? and " + KEY_TIME
				+ " > ? and " + KEY_TIME + " < ? ", new String[] {
				min_lat + "", max_lat + "", min_lon + "", max_lon + "",
				start_time + "", end_time + "" }, null, null, null, null);
		return cursor;
	}

	
	/**
	 * Run an API call against the API /list/ cache
	 * 
	 * @return
	 */
	public Cursor runListCacheCall(double min_lat, double max_lat,
			double min_lon, double max_lon, long start_time, long end_time,
			double limit) {
		Cursor cursor = mDB.query(false, API_LIST_TABLE, new String[] {
				KEY_ROW_ID, KEY_OBSERVATION_VALUE, KEY_TIME}, KEY_LATITUDE
				+ " > ? and " + KEY_LATITUDE + " < ? and " + KEY_LONGITUDE
				+ " > ? and " + KEY_LONGITUDE + " < ? and " + KEY_TIME
				+ " > ? and " + KEY_TIME + " < ? ", new String[] {
				min_lat + "", max_lat + "", min_lon + "", max_lon + "",
				start_time + "", end_time + "" }, null, null, null, null);
		return cursor;
	}

	/**
	 * Run an "API call" against the local database
	 * 
	 * @return
	 */
	public Cursor runLocalAPICall(double min_lat, double max_lat,
			double min_lon, double max_lon, long start_time, long end_time,
			double limit) {
		Cursor cursor = mDB.query(false, OBSERVATIONS_TABLE, new String[] {
				KEY_ROW_ID, KEY_LATITUDE, KEY_LONGITUDE, KEY_ALTITUDE,
				KEY_ACCURACY, KEY_PROVIDER, KEY_OBSERVATION_TYPE,
				KEY_OBSERVATION_UNIT, KEY_OBSERVATION_VALUE, KEY_SHARING,
				KEY_TIME, KEY_TIMEZONE, KEY_USERID, KEY_SENSOR_NAME,
				KEY_SENSOR_TYPE, KEY_SENSOR_VENDOR, KEY_SENSOR_RESOLUTION,
				KEY_SENSOR_VERSION, KEY_OBSERVATION_TREND },

		KEY_LATITUDE + " > ? and " + KEY_LATITUDE + " < ? and " + KEY_LONGITUDE
				+ " > ? and " + KEY_LONGITUDE + " < ? and " + KEY_TIME
				+ " > ? and " + KEY_TIME + " < ? ", new String[] {
				min_lat + "", max_lat + "", min_lon + "", max_lon + "",
				start_time + "", end_time + "" }, null, null, null, null);

		return cursor;
	}

	/**
	 * Get a single observation
	 * 
	 * @param rowId
	 * @return
	 * @throws SQLException
	 * 
	 */
	public Cursor fetchObservation(long rowId) throws SQLException {
		Cursor mCursor =

		mDB.query(true, OBSERVATIONS_TABLE, new String[] { KEY_ROW_ID,
				KEY_LATITUDE, KEY_LONGITUDE, KEY_ALTITUDE, KEY_ACCURACY,
				KEY_PROVIDER, KEY_OBSERVATION_TYPE, KEY_OBSERVATION_UNIT,
				KEY_OBSERVATION_VALUE, KEY_SHARING, KEY_TIME, KEY_TIMEZONE,
				KEY_USERID, KEY_SENSOR_NAME, KEY_SENSOR_TYPE,
				KEY_SENSOR_VENDOR, KEY_SENSOR_RESOLUTION, KEY_SENSOR_VERSION,
				KEY_OBSERVATION_TREND }, KEY_ROW_ID + "=" + rowId, null, null,
				null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	/**
	 * How many observations are there?
	 * 
	 * @param rowId
	 * @return
	 * @throws SQLException
	 * 
	 */
	public long fetchObservationMaxID() throws SQLException {
		open();
		Cursor mCount = mDB.rawQuery("SELECT COUNT(*) FROM "
				+ OBSERVATIONS_TABLE, null);
		mCount.moveToFirst();
		long rowId = mCount.getInt(0);
		mCount.close();
		return rowId;
	}

	/**
	 * Get a single application's settings by row id
	 * 
	 * @param rowId
	 * @return
	 * @throws SQLException
	 */
	public Cursor fetchSetting(long rowId) throws SQLException {
		Cursor mCursor =

		mDB.query(true, SETTINGS_TABLE, new String[] { KEY_ROW_ID, KEY_APP_ID,
				KEY_DATA_COLLECTION_FREQUENCY, KEY_SERVER_URL,
				KEY_ONLY_WHEN_CHARGING, KEY_COLLECTING_DATA, KEY_SHARING_DATA,
				KEY_SHARE_LEVEL, KEY_SEND_NOTIFICATIONS, KEY_USE_GPS }, KEY_ROW_ID
				+ "=" + rowId, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	
	
	/**
	 * Clear the local measurements from the device
	 * 
	 * @return
	 */
	public void clearLocalCache() {
		mDB.execSQL("delete from " + OBSERVATIONS_TABLE);
	}

	/**
	 * Clear the API cache from the device
	 * 
	 * @return
	 */
	public void clearAPICache() {
		mDB.execSQL("delete from " + API_LIST_TABLE);
		mDB.execSQL("delete from " + CURRENT_CONDITIONS_TABLE);
	}

	/**
	 * Fetch every application setting.
	 * 
	 * @return
	 */
	public Cursor fetchAllSettings() {
		return mDB.query(SETTINGS_TABLE, new String[] { KEY_ROW_ID, 
				KEY_APP_ID,
				KEY_DATA_COLLECTION_FREQUENCY, 
				KEY_SERVER_URL,
				KEY_ONLY_WHEN_CHARGING,
				KEY_COLLECTING_DATA,
				KEY_SHARING_DATA,
				KEY_SHARE_LEVEL,
				KEY_SEND_NOTIFICATIONS,
				KEY_USE_GPS}, null, null,
				null, null, null);
	}

	/**
	 * Fetch every stored current condition
	 * 
	 * @return
	 */
	public Cursor fetchAllConditions() {
		return mDB.query(CURRENT_CONDITIONS_TABLE, new String[] { KEY_ROW_ID,
				KEY_LATITUDE, KEY_LONGITUDE, KEY_ALTITUDE, KEY_ACCURACY,
				KEY_PROVIDER, KEY_SHARING, KEY_TIME, KEY_TIMEZONE, KEY_USERID,
				KEY_GENERAL_CONDITION, KEY_WINDY, KEY_FOGGY, KEY_CLOUD_TYPE,
				KEY_PRECIPITATION_TYPE, KEY_PRECIPITATION_AMOUNT,
				KEY_PRECIPITATION_UNIT, KEY_THUNDERSTORM_INTENSITY,
				KEY_USER_COMMENT }, null, null, null, null, null);
	}

	/**
	 * Fetch every stored local observation
	 * 
	 * @return
	 */
	public Cursor fetchAllObservations() {
		return mDB.query(OBSERVATIONS_TABLE, new String[] { KEY_ROW_ID,
				KEY_LATITUDE, KEY_LONGITUDE, KEY_ALTITUDE, KEY_ACCURACY,
				KEY_PROVIDER, KEY_OBSERVATION_TYPE, KEY_OBSERVATION_UNIT,
				KEY_OBSERVATION_VALUE, KEY_SHARING, KEY_TIME, KEY_TIMEZONE,
				KEY_USERID, KEY_SENSOR_NAME, KEY_SENSOR_TYPE,
				KEY_SENSOR_VENDOR, KEY_SENSOR_RESOLUTION, KEY_SENSOR_VERSION,
				KEY_OBSERVATION_TREND }, null, null, null, null, null);
	}

	/**
	 * Fetch every stored API observation
	 * 
	 * @return
	 */
	public Cursor fetchAllAPICacheObservations() {
		return mDB.query(API_LIST_TABLE, new String[] { KEY_ROW_ID,
				KEY_LATITUDE, KEY_LONGITUDE, KEY_ALTITUDE, KEY_ACCURACY,
				KEY_PROVIDER, KEY_OBSERVATION_TYPE, KEY_OBSERVATION_UNIT,
				KEY_OBSERVATION_VALUE, KEY_SHARING, KEY_TIME, KEY_TIMEZONE,
				KEY_USERID, KEY_SENSOR_NAME, KEY_SENSOR_TYPE,
				KEY_SENSOR_VENDOR, KEY_SENSOR_RESOLUTION, KEY_SENSOR_VERSION,
				KEY_OBSERVATION_TREND }, null, null, null, null, null);
	}

	/**
	 * Get a single application's settings by app id
	 * 
	 * @param rowId
	 * @return
	 * @throws SQLException
	 */
	public Cursor fetchSettingByApp(String appID) throws SQLException {
		Cursor mCursor =

		mDB.query(true, SETTINGS_TABLE, new String[] { KEY_ROW_ID, KEY_APP_ID,
				KEY_DATA_COLLECTION_FREQUENCY, KEY_SERVER_URL, KEY_SEND_NOTIFICATIONS, KEY_USE_GPS, KEY_ONLY_WHEN_CHARGING, KEY_SHARING_DATA, KEY_COLLECTING_DATA, KEY_SHARE_LEVEL}, KEY_APP_ID
				+ "='" + appID + "'", null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	/**
	 * Update existing settings for an application
	 * 
	 * @param appID
	 * @param dataCollectionFrequency
	 * @return
	 */
	public long updateSetting(String appID, long dataCollectionFrequency,
			String serverURL, boolean onlyWhenCharging, boolean collectingData,
			boolean sharingData, String shareLevel, boolean sendNotifications, boolean useGPS) {

		ContentValues newValues = new ContentValues();
		newValues.put(KEY_APP_ID, appID);
		newValues.put(KEY_DATA_COLLECTION_FREQUENCY, dataCollectionFrequency);
		newValues.put(KEY_SERVER_URL, serverURL);
		newValues.put(KEY_ONLY_WHEN_CHARGING, onlyWhenCharging);
		newValues.put(KEY_COLLECTING_DATA, collectingData);
		newValues.put(KEY_SHARING_DATA, sharingData);
		newValues.put(KEY_SHARE_LEVEL, shareLevel);
		newValues.put(KEY_SEND_NOTIFICATIONS, sendNotifications);
		newValues.put(KEY_USE_GPS, useGPS);
		return mDB.update(SETTINGS_TABLE, newValues, KEY_APP_ID + "='" + appID
				+ "'", null);
	}

	public ArrayList<CbWeather> fudgeGPSData(ArrayList<CbWeather> readings) {
		ArrayList<CbWeather> fudgedReadings = new ArrayList<CbWeather>();
		for(CbWeather obWeather : readings) {
			CbObservation ob = (CbObservation) obWeather;
			double longitude = ob.getLocation().getLongitude();
			double latitude = ob.getLocation().getLatitude();
			double range = .01;
			Random lat = new Random(System.currentTimeMillis());
			Random lon = new Random(System.currentTimeMillis());
			latitude = (latitude - range) + (int)(lat.nextDouble()  * ((2 * range) + 1));
			longitude = (longitude - range) + (int)(lon.nextDouble() * ((2 * range) + 1));
			ob.getLocation().setLatitude(latitude);
			ob.getLocation().setLongitude(longitude);
			fudgedReadings.add(ob);
		}

		return fudgedReadings;
	}

	
	public boolean addWeatherArrayList(ArrayList<CbWeather> results, CbApiCall api) {
		if (results.get(0).getClass() == (CbObservation.class)) {
			results = fudgeGPSData(results);
			addObservationArrayList(results, api);
		} else {
			addCurrentConditionArrayList(results, api);
		}

		return true;
	}

	public boolean addCurrentConditionArrayList(ArrayList<CbWeather> weather, CbApiCall api) {

		mDB.beginTransaction();

		String insertSQL = "INSERT INTO "
				+ CURRENT_CONDITIONS_TABLE
				+ " ("
				+ KEY_LATITUDE
				+ ", "
				+ KEY_LONGITUDE
				+ ", "
				+ KEY_ALTITUDE
				+ ", "
				+ KEY_ACCURACY
				+ ", "
				+ KEY_PROVIDER
				+ ", "
				+ KEY_SHARING
				+ ", "
				+ KEY_TIME
				+ ", "
				+ KEY_TIMEZONE
				+ ", "
				+ KEY_USERID
				+ ", "
				+ KEY_GENERAL_CONDITION
				+ ", "
				+ KEY_WINDY
				+ ", "
				+ KEY_FOGGY
				+ ", "
				+ KEY_CLOUD_TYPE
				+ ", "
				+ KEY_PRECIPITATION_TYPE
				+ ", "
				+ KEY_PRECIPITATION_AMOUNT
				+ ", "
				+ KEY_PRECIPITATION_UNIT
				+ ", "
				+ KEY_THUNDERSTORM_INTENSITY
				+ ", "
				+ KEY_USER_COMMENT
				+ ") values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		try {
			SQLiteStatement insert = mDB.compileStatement(insertSQL);
			for (CbWeather weatherItem : weather) {
				CbCurrentCondition condition = (CbCurrentCondition) weatherItem;
				insert.bindDouble(1, condition.getLocation().getLatitude());
				insert.bindDouble(2, condition.getLocation().getLongitude());
				insert.bindDouble(3, condition.getLocation().getAltitude());
				insert.bindDouble(4, condition.getLocation().getAccuracy());
				insert.bindString(5, condition.getLocation().getProvider());
				insert.bindString(6, condition.getSharing_policy());
				insert.bindLong(7, condition.getTime());
				insert.bindLong(8, condition.getTzoffset());
				insert.bindString(9, condition.getUser_id());
				insert.bindString(10, condition.getGeneral_condition());
				insert.bindString(11, condition.getWindy());
				insert.bindString(12, condition.getFog_thickness());
				insert.bindString(13, condition.getCloud_type());
				insert.bindString(14, condition.getPrecipitation_type());
				insert.bindDouble(15, condition.getPrecipitation_amount());
				insert.bindString(16, condition.getPrecipitation_unit());
				insert.bindString(17, condition.getThunderstorm_intensity());
				insert.bindString(18, condition.getUser_comment());
				insert.executeInsert();
			}

			mDB.setTransactionSuccessful();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} finally {
			mDB.endTransaction();
		}

		return true;
	}

	/**
	 * Add a new Observations in an ArrayList
	 * 
	 * @return
	 */
	public boolean addObservationArrayList(ArrayList<CbWeather> weather, CbApiCall api) {
		mDB.beginTransaction();

		String insertSQL = "INSERT INTO "
				+ API_LIST_TABLE
				+ " ("
				+ KEY_LATITUDE
				+ ", "
				+ KEY_LONGITUDE
				+ ", "		
				+ KEY_TIME
				+ ", " 
				+ KEY_OBSERVATION_VALUE
				+ " "
				+ ") values (?, ?, ?, ?)";
		try {
			SQLiteStatement insert = mDB.compileStatement(insertSQL);
			
			for (CbWeather weatherItem : weather) {
				
				CbObservation ob = (CbObservation) weatherItem;
				double latitude = ob.getLocation().getLatitude(); 
				double longitude = ob.getLocation().getLongitude();
				insert.bindDouble(1, latitude);
				insert.bindDouble(2, longitude); 
				insert.bindLong(3, ob.getTime());
				insert.bindDouble(4, ob.getObservationValue());
				insert.executeInsert();
			}

			mDB.setTransactionSuccessful();
			
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} finally {
			mDB.endTransaction();
		}
		return true;
	}

	
	/**
	 * Add a new current condition
	 * 
	 * @return
	 */
	public long addCondition(CbCurrentCondition cc) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_LATITUDE, cc.getLocation().getLatitude());
		initialValues.put(KEY_LONGITUDE, cc.getLocation().getLongitude());
		initialValues.put(KEY_ALTITUDE, cc.getLocation().getAltitude());
		initialValues.put(KEY_ACCURACY, cc.getLocation().getAccuracy());
		initialValues.put(KEY_PROVIDER, cc.getLocation().getProvider());
		initialValues.put(KEY_SHARING, "default");
		initialValues.put(KEY_TIME, cc.getTime());
		initialValues.put(KEY_TIMEZONE, cc.getTzoffset());
		initialValues.put(KEY_USERID, cc.getUser_id());
		initialValues.put(KEY_GENERAL_CONDITION, cc.getGeneral_condition());
		initialValues.put(KEY_WINDY, cc.getWindy());
		initialValues.put(KEY_FOGGY, cc.getFog_thickness());
		initialValues.put(KEY_CLOUD_TYPE, cc.getCloud_type());
		initialValues.put(KEY_PRECIPITATION_TYPE, cc.getPrecipitation_type());
		initialValues.put(KEY_PRECIPITATION_AMOUNT,
				cc.getPrecipitation_amount());
		initialValues.put(KEY_PRECIPITATION_UNIT, cc.getPrecipitation_unit());
		initialValues.put(KEY_THUNDERSTORM_INTENSITY,
				cc.getThunderstorm_intensity());
		initialValues.put(KEY_USER_COMMENT, cc.getUser_comment());
		return mDB.insert(CURRENT_CONDITIONS_TABLE, null, initialValues);
	}

	/**
	 * Add a new observation
	 * 
	 * @return
	 */
	public long addObservation(CbObservation observation) {
		ContentValues initialValues = new ContentValues();
		ContentValues listValues = new ContentValues();
		try {
			initialValues
					.put(KEY_LATITUDE, observation.getLocation().getLatitude());
			initialValues.put(KEY_LONGITUDE, observation.getLocation()
					.getLongitude());
			initialValues
					.put(KEY_ALTITUDE, observation.getLocation().getAltitude());
			initialValues
					.put(KEY_ACCURACY, observation.getLocation().getAccuracy());
			initialValues
					.put(KEY_PROVIDER, observation.getLocation().getProvider());
			initialValues.put(KEY_OBSERVATION_TYPE,
					observation.getObservationType());
			initialValues.put(KEY_OBSERVATION_UNIT,
					observation.getObservationUnit());
			initialValues.put(KEY_OBSERVATION_VALUE,
					observation.getObservationValue());
			initialValues.put(KEY_SHARING, observation.getSharing());
			initialValues.put(KEY_TIME, observation.getTime());
			initialValues.put(KEY_TIMEZONE, observation.getTimeZoneOffset());
			initialValues.put(KEY_USERID, observation.getUser_id());
			initialValues.put(KEY_OBSERVATION_TREND, observation.getTrend());
			
			listValues.put(KEY_LATITUDE, observation.getLocation().getLatitude());
			listValues.put(KEY_LONGITUDE, observation.getLocation().getLongitude());
			listValues.put(KEY_TIME, observation.getTime());
			listValues.put(KEY_OBSERVATION_VALUE, observation.getTime());
			
			
			mDB.insert(API_LIST_TABLE, null, listValues);
			return mDB.insert(OBSERVATIONS_TABLE, null, initialValues);
		} catch(NullPointerException npe) {
			//npe.printStackTrace();
		}
		return -1;
	}

	/**
	 * Add new settings for an application
	 * 
	 * @param appID
	 * @param dataCollectionFrequency
	 * @return
	 */

	public long addSetting(String appID, long dataCollectionFrequency,
			String serverURL, boolean onlyWhenCharging, boolean collectingData,
			boolean sharingData, String shareLevel, boolean sendNotifications, boolean useGPS) {

		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_APP_ID, appID);
		initialValues.put(KEY_DATA_COLLECTION_FREQUENCY,
				dataCollectionFrequency);
		initialValues.put(KEY_SERVER_URL, serverURL);
		initialValues.put(KEY_ONLY_WHEN_CHARGING, onlyWhenCharging);
		initialValues.put(KEY_COLLECTING_DATA, collectingData);
		initialValues.put(KEY_SHARING_DATA, sharingData);
		initialValues.put(KEY_SHARE_LEVEL, shareLevel);
		initialValues.put(KEY_SEND_NOTIFICATIONS, sendNotifications);
		initialValues.put(KEY_USE_GPS, useGPS);
		return mDB.insert(SETTINGS_TABLE, null, initialValues);
	}

	/**
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public CbDb open() throws SQLException {
		mDbHelper = new DatabaseHelper(mContext);
		mDB = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public CbDb(Context ctx) {
		this.mContext = ctx;
	}
}
