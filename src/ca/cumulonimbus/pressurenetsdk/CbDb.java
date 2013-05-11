package ca.cumulonimbus.pressurenetsdk;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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
	public static final String API_CACHE_TABLE = "cb_api_cache";
	public static final String CURRENT_CONDITIONS_TABLE = "cb_current_conditions";

	// Settings Fields
	public static final String KEY_ROW_ID = "_id";
	public static final String KEY_APP_ID = "app_id";
	public static final String KEY_DATA_COLLECTION_FREQUENCY = "data_frequency";
	public static final String KEY_SERVER_URL = "server_url";

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

	private Context mContext;

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDB;

	private static final String SETTINGS_TABLE_CREATE = "create table "
			+ SETTINGS_TABLE + " (_id integer primary key autoincrement, "
			+ KEY_APP_ID + " text not null, " + KEY_DATA_COLLECTION_FREQUENCY
			+ " real not null, " + KEY_SERVER_URL + " text not null)";

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
			+ " text not null, " + KEY_SENSOR_TYPE + " real not null, "
			+ KEY_SENSOR_VENDOR + " text not null, " + KEY_SENSOR_RESOLUTION
			+ " real not null, " + KEY_SENSOR_VERSION + " real not null,"
			+ "UNIQUE (" + KEY_LATITUDE + ", " + KEY_LONGITUDE + "," + KEY_TIME
			+ ", " + KEY_USERID + "," + KEY_OBSERVATION_VALUE
			+ ") ON CONFLICT REPLACE)";

	private static final String API_CACHE_TABLE_CREATE = "create table "
			+ API_CACHE_TABLE + " (_id integer primary key autoincrement, "
			+ KEY_LATITUDE + " real not null, " + KEY_LONGITUDE
			+ " real not null, " + KEY_ALTITUDE + " real not null, "
			+ KEY_ACCURACY + " real not null, " + KEY_PROVIDER
			+ " text not null, " + KEY_OBSERVATION_TYPE + " text not null, "
			+ KEY_OBSERVATION_UNIT + " text not null, " + KEY_OBSERVATION_VALUE
			+ " real not null, " + KEY_SHARING + " text not null, " + KEY_TIME
			+ " real not null, " + KEY_TIMEZONE + " real not null, "
			+ KEY_USERID + " text not null, " + KEY_SENSOR_NAME
			+ " text not null, " + KEY_SENSOR_TYPE + " real not null, "
			+ KEY_SENSOR_VENDOR + " text not null, " + KEY_SENSOR_RESOLUTION
			+ " real not null, " + KEY_SENSOR_VERSION + " real not null,"
			+ "UNIQUE (" + KEY_LATITUDE + ", " + KEY_LONGITUDE + "," + KEY_TIME
			+ "," + KEY_USERID + "," + KEY_OBSERVATION_VALUE
			+ ") ON CONFLICT REPLACE)";

	private static final String CURRENT_CONDITIONS_TABLS_CREATE = "create table "
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
			+ KEY_USER_COMMENT + " text not null)";

	private static final String DATABASE_NAME = "CbDb";
	private static final int DATABASE_VERSION = 14;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SETTINGS_TABLE_CREATE);
			db.execSQL(OBSERVATIONS_TABLE_CREATE);
			db.execSQL(API_CACHE_TABLE_CREATE);
			db.execSQL(CURRENT_CONDITIONS_TABLS_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Build upgrade mechanism
			db.execSQL("DROP TABLE IF EXISTS " + SETTINGS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + OBSERVATIONS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + API_CACHE_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + CURRENT_CONDITIONS_TABLE);
			onCreate(db);
		}
	}
	
	/**
	 * Get local current conditions
	 * 
	 * @return
	 */
	public Cursor getCurrentConditions(double min_lat, double max_lat,
			double min_lon, double max_lon, long start_time, long end_time,
			double limit) {
		Cursor cursor = mDB.query(false, CURRENT_CONDITIONS_TABLE, new String[] {
				KEY_ROW_ID, KEY_LATITUDE, KEY_LONGITUDE, KEY_ALTITUDE,
				KEY_ACCURACY, KEY_PROVIDER, KEY_SHARING,
				KEY_TIME, KEY_TIMEZONE, KEY_USERID, KEY_GENERAL_CONDITION,
				KEY_WINDY, KEY_FOGGY, KEY_CLOUD_TYPE, KEY_PRECIPITATION_TYPE,
				KEY_PRECIPITATION_AMOUNT, KEY_PRECIPITATION_UNIT,
				KEY_THUNDERSTORM_INTENSITY,KEY_USER_COMMENT }, KEY_LATITUDE + " > ? and " + KEY_LATITUDE
				+ " < ? and " + KEY_LONGITUDE + " > ? and " + KEY_LONGITUDE
				+ " < ? and " + KEY_TIME + " > ? and " + KEY_TIME + " < ? ",
				new String[] { min_lat + "", max_lat + "", min_lon + "",
						max_lon + "", start_time + "", end_time + "" }, null,
				null, null, null);
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
		Cursor cursor = mDB.query(false, API_CACHE_TABLE, new String[] {
				KEY_ROW_ID, KEY_LATITUDE, KEY_LONGITUDE, KEY_ALTITUDE,
				KEY_ACCURACY, KEY_PROVIDER, KEY_OBSERVATION_TYPE,
				KEY_OBSERVATION_UNIT, KEY_OBSERVATION_VALUE, KEY_SHARING,
				KEY_TIME, KEY_TIMEZONE, KEY_USERID, KEY_SENSOR_NAME,
				KEY_SENSOR_TYPE, KEY_SENSOR_VENDOR, KEY_SENSOR_RESOLUTION,
				KEY_SENSOR_VERSION }, KEY_LATITUDE + " > ? and " + KEY_LATITUDE
				+ " < ? and " + KEY_LONGITUDE + " > ? and " + KEY_LONGITUDE
				+ " < ? and " + KEY_TIME + " > ? and " + KEY_TIME + " < ? ",
				new String[] { min_lat + "", max_lat + "", min_lon + "",
						max_lon + "", start_time + "", end_time + "" }, null,
				null, null, null);
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
				KEY_SENSOR_VERSION },

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
				KEY_SENSOR_VENDOR, KEY_SENSOR_RESOLUTION, KEY_SENSOR_VERSION },
				KEY_ROW_ID + "=" + rowId, null, null, null, null, null);
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
				KEY_DATA_COLLECTION_FREQUENCY, KEY_SERVER_URL }, KEY_ROW_ID
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
		mDB.execSQL("delete from " + API_CACHE_TABLE);
	}

	/**
	 * Fetch every application setting.
	 * 
	 * @return
	 */
	public Cursor fetchAllSettings() {
		return mDB.query(SETTINGS_TABLE, new String[] { KEY_ROW_ID, KEY_APP_ID,
				KEY_DATA_COLLECTION_FREQUENCY, KEY_SERVER_URL }, null, null,
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
				KEY_SENSOR_VENDOR, KEY_SENSOR_RESOLUTION, KEY_SENSOR_VERSION },
				null, null, null, null, null);
	}

	/**
	 * Fetch every stored API observation
	 * 
	 * @return
	 */
	public Cursor fetchAllAPICacheObservations() {
		return mDB.query(API_CACHE_TABLE, new String[] { KEY_ROW_ID,
				KEY_LATITUDE, KEY_LONGITUDE, KEY_ALTITUDE, KEY_ACCURACY,
				KEY_PROVIDER, KEY_OBSERVATION_TYPE, KEY_OBSERVATION_UNIT,
				KEY_OBSERVATION_VALUE, KEY_SHARING, KEY_TIME, KEY_TIMEZONE,
				KEY_USERID, KEY_SENSOR_NAME, KEY_SENSOR_TYPE,
				KEY_SENSOR_VENDOR, KEY_SENSOR_RESOLUTION, KEY_SENSOR_VERSION },
				null, null, null, null, null);
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
				KEY_DATA_COLLECTION_FREQUENCY, KEY_SERVER_URL }, KEY_APP_ID
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
			String serverURL) {

		ContentValues newValues = new ContentValues();
		newValues.put(KEY_APP_ID, appID);
		newValues.put(KEY_DATA_COLLECTION_FREQUENCY, dataCollectionFrequency);
		newValues.put(KEY_SERVER_URL, serverURL);
		return mDB.update(SETTINGS_TABLE, newValues, KEY_APP_ID + "='" + appID
				+ "'", null);
	}

	/**
	 * Add a new Observations in an ArrayList
	 * 
	 * @return
	 */
	public boolean addObservationArrayList(ArrayList<CbObservation> observations) {
		mDB.beginTransaction();

		String insertSQL = "INSERT INTO "
				+ API_CACHE_TABLE
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
				+ KEY_OBSERVATION_TYPE
				+ ", "
				+ KEY_OBSERVATION_UNIT
				+ ", "
				+ KEY_OBSERVATION_VALUE
				+ ", "
				+ KEY_SHARING
				+ ", "
				+ KEY_TIME
				+ ", "
				+ KEY_TIMEZONE
				+ ", "
				+ KEY_USERID
				+ ", "
				+ KEY_SENSOR_NAME
				+ ", "
				+ KEY_SENSOR_TYPE
				+ ", "
				+ KEY_SENSOR_VENDOR
				+ ", "
				+ KEY_SENSOR_RESOLUTION
				+ ", "
				+ KEY_SENSOR_VERSION
				+ ") values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		try {
			SQLiteStatement insert = mDB.compileStatement(insertSQL);
			for (CbObservation ob : observations) {
				insert.bindDouble(1, ob.getLocation().getLatitude());
				insert.bindDouble(2, ob.getLocation().getLongitude());
				insert.bindDouble(3, ob.getLocation().getAltitude());
				insert.bindDouble(4, ob.getLocation().getAccuracy());
				insert.bindString(5, ob.getLocation().getProvider());
				insert.bindString(6, ob.getObservationType());
				insert.bindString(7, ob.getObservationUnit());
				insert.bindDouble(8, ob.getObservationValue());
				insert.bindString(9, ob.getSharing());
				insert.bindLong(10, ob.getTime());
				insert.bindLong(11, ob.getTimeZoneOffset());
				insert.bindString(12, ob.getUser_id());
				if (ob.getSensor() == null) {
					insert.bindString(13, "");
					insert.bindDouble(14, 0.0);
					insert.bindString(15, "");
					insert.bindDouble(16, 0.0);
					insert.bindDouble(17, 0.0);
				} else {
					insert.bindString(13, ob.getSensor().getName());
					insert.bindDouble(14, ob.getSensor().getType());
					insert.bindString(15, ob.getSensor().getVendor());
					insert.bindDouble(16, ob.getSensor().getResolution());
					insert.bindDouble(17, ob.getSensor().getVersion());
				}

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
		initialValues.put(KEY_SENSOR_NAME, observation.getSensor().getName());
		initialValues.put(KEY_SENSOR_TYPE, observation.getSensor().getType());
		initialValues.put(KEY_SENSOR_VENDOR, observation.getSensor()
				.getVendor());
		initialValues.put(KEY_SENSOR_RESOLUTION, observation.getSensor()
				.getResolution());
		initialValues.put(KEY_SENSOR_VERSION, observation.getSensor()
				.getVersion());
		return mDB.insert(OBSERVATIONS_TABLE, null, initialValues);
	}

	/**
	 * Add API Cache Observation
	 * 
	 * @return
	 */
	public long addAPICacheObservation(CbObservation observation) {
		ContentValues initialValues = new ContentValues();
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
		// TODO: implement the sensor data
		if (observation.getSensor() == null) {
			initialValues.put(KEY_SENSOR_NAME, "");
			initialValues.put(KEY_SENSOR_TYPE, 0);
			initialValues.put(KEY_SENSOR_VENDOR, "");
			initialValues.put(KEY_SENSOR_RESOLUTION, 0);
			initialValues.put(KEY_SENSOR_VERSION, 0);

		} else {
			initialValues.put(KEY_SENSOR_NAME, observation.getSensor()
					.getName());
			initialValues.put(KEY_SENSOR_TYPE, observation.getSensor()
					.getType());
			initialValues.put(KEY_SENSOR_VENDOR, observation.getSensor()
					.getVendor());
			initialValues.put(KEY_SENSOR_RESOLUTION, observation.getSensor()
					.getResolution());
			initialValues.put(KEY_SENSOR_VERSION, observation.getSensor()
					.getVersion());
		}
		return mDB.insert(API_CACHE_TABLE, null, initialValues);
	}

	/**
	 * Add new settings for an application
	 * 
	 * @param appID
	 * @param dataCollectionFrequency
	 * @return
	 */
	public long addSetting(String appID, long dataCollectionFrequency,
			String serverURL) {

		ContentValues initialValues = new ContentValues();
		// System.out.println("adding " + appID + " freq " +
		// dataCollectionFrequency);
		initialValues.put(KEY_APP_ID, appID);
		initialValues.put(KEY_DATA_COLLECTION_FREQUENCY,
				dataCollectionFrequency);
		initialValues.put(KEY_SERVER_URL, serverURL);
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
