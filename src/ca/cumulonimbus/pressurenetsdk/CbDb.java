package ca.cumulonimbus.pressurenetsdk;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Keep track of app settings, as this SDK may be used by more
 * than one app on a device. This allows an empty Intent
 * to start the Service generically, and we can then read 
 * saved settings for each registered app to act accordingly.
 *
 * @author jacob
 *
 */

public class CbDb {

	// Tables
	public static final String SETTINGS_TABLE = "cb_settings";
	public static final String OBSERVATIONS_TABLE = "cb_observations";
	
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
	public static final String KEY_SENSOR_VENDOR ="sensor_vendor";
	public static final String KEY_SENSOR_RESOLUTION = "sensor_resolution";
	public static final String KEY_SENSOR_VERSION = "sensor_version";
	
	private Context mContext;
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDB;
	
	private static final String SETTINGS_TABLE_CREATE = "create table " 
			+ SETTINGS_TABLE + " (_id integer primary key autoincrement, "
			+ KEY_APP_ID + " text not null, "
			+ KEY_DATA_COLLECTION_FREQUENCY + " real not null, "
			+ KEY_SERVER_URL + " text not null)"; 
	
	private static final String OBSERVATIONS_TABLE_CREATE = "create table " 
			+ OBSERVATIONS_TABLE + " (_id integer primary key autoincrement, "
			+ KEY_LATITUDE + " real not null, "
			+ KEY_LONGITUDE + " real not null, "
			+ KEY_ALTITUDE + " real not null, "
			+ KEY_ACCURACY + " real not null, "
			+ KEY_PROVIDER + " text not null, "
			+ KEY_OBSERVATION_TYPE + " text not null, "
			+ KEY_OBSERVATION_UNIT + " text not null, "
			+ KEY_OBSERVATION_VALUE + " real not null, "
			+ KEY_SHARING + " text not null, "
			+ KEY_TIME + " real not null, "
			+ KEY_TIMEZONE + " real not null, "
			+ KEY_USERID + " text not null, "
			+ KEY_SENSOR_NAME + " text not null, "
			+ KEY_SENSOR_TYPE + " real not null, "			
			+ KEY_SENSOR_VENDOR + " text not null, "
			+ KEY_SENSOR_RESOLUTION + " real not null, "
			+ KEY_SENSOR_VERSION + " real not null)";
	
	private static final String DATABASE_NAME = "CbDb";
	private static final int DATABASE_VERSION = 8;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
	
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SETTINGS_TABLE_CREATE);
			db.execSQL(OBSERVATIONS_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Build upgrade mechanism
			db.execSQL("DROP TABLE IF EXISTS " + SETTINGS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + OBSERVATIONS_TABLE);
			onCreate(db);
		}
	}

	/**
	 * Get a single observation
	 * @param rowId
	 * @return
	 * @throws SQLException
	 * 
	 */
    public Cursor fetchObservation(long rowId) throws SQLException {
        Cursor mCursor =

            mDB.query(true, OBSERVATIONS_TABLE, new String[] {KEY_ROW_ID,
                    KEY_LATITUDE, KEY_LONGITUDE, KEY_ALTITUDE, KEY_ACCURACY, KEY_PROVIDER,
                    KEY_OBSERVATION_TYPE, KEY_OBSERVATION_UNIT, KEY_OBSERVATION_VALUE, 
                    KEY_SHARING, KEY_TIME, KEY_TIMEZONE, KEY_USERID, KEY_SENSOR_NAME,
                    KEY_SENSOR_TYPE, KEY_SENSOR_VENDOR, KEY_SENSOR_RESOLUTION, KEY_SENSOR_VERSION}, KEY_ROW_ID + "=" + rowId, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }
    

	/**
	 * How many observations are there?
	 * @param rowId
	 * @return
	 * @throws SQLException
	 * 
	 */
    public long fetchObservationMaxID() throws SQLException {
    	open();
    	Cursor mCount=mDB.rawQuery("SELECT COUNT(*) FROM " + OBSERVATIONS_TABLE, null);
    	mCount.moveToFirst();
    	long rowId = mCount.getInt(0);
    	mCount.close();
    	return rowId;
    }
	
	/**
	 * Get a single application's settings by row id
	 * @param rowId
	 * @return
	 * @throws SQLException
	 */
    public Cursor fetchSetting(long rowId) throws SQLException {
        Cursor mCursor =

            mDB.query(true, SETTINGS_TABLE, new String[] {KEY_ROW_ID,
                    KEY_APP_ID, KEY_DATA_COLLECTION_FREQUENCY, KEY_SERVER_URL}, KEY_ROW_ID + "=" + rowId, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }
    
    /**
     * Empty the database
     * @return
     */
    public void clearDb() {
    	mDB.execSQL("delete from " + SETTINGS_TABLE);
    }
	
    /**
     * Fetch every application setting.
     * 
     * @return
     */
    public Cursor fetchAllSettings() {
        return mDB.query(SETTINGS_TABLE, new String[] {KEY_ROW_ID, KEY_APP_ID, KEY_DATA_COLLECTION_FREQUENCY, KEY_SERVER_URL}, null, null, null, null, null);
    }
    
    /**
     * Fetch every stored local observation
     * 
     * @return
     */
    public Cursor fetchAllObservations() {
        return mDB.query(OBSERVATIONS_TABLE, new String[] {KEY_ROW_ID,
                KEY_LATITUDE, KEY_LONGITUDE, KEY_ALTITUDE, KEY_ACCURACY, KEY_PROVIDER,
                KEY_OBSERVATION_TYPE, KEY_OBSERVATION_UNIT, KEY_OBSERVATION_VALUE, 
                KEY_SHARING, KEY_TIME, KEY_TIMEZONE, KEY_USERID, KEY_SENSOR_NAME,
                KEY_SENSOR_TYPE, KEY_SENSOR_VENDOR, KEY_SENSOR_RESOLUTION, KEY_SENSOR_VERSION}, null, null, null, null, null);
    }

	/**
	 * Get a single application's settings by app id
	 * @param rowId
	 * @return
	 * @throws SQLException
	 */
    public Cursor fetchSettingByApp(String appID) throws SQLException {
        Cursor mCursor =

            mDB.query(true, SETTINGS_TABLE, new String[] {KEY_ROW_ID,
                    KEY_APP_ID, KEY_DATA_COLLECTION_FREQUENCY, KEY_SERVER_URL}, KEY_APP_ID + "='" + appID + "'", null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    
    /**
     * Update existing settings for an application
     * @param appID
     * @param dataCollectionFrequency
     * @return
     */
    public long updateSetting(String appID, long dataCollectionFrequency, String serverURL) {
    	
        ContentValues newValues = new ContentValues();
        newValues.put(KEY_APP_ID, appID);
        newValues.put(KEY_DATA_COLLECTION_FREQUENCY, dataCollectionFrequency);
        newValues.put(KEY_SERVER_URL, serverURL);
        return mDB.update(SETTINGS_TABLE, newValues, KEY_APP_ID + "='" + appID + "'", null);
    }
    

    /**
     * Add a new observation
     * 			+ OBSERVATIONS_TABLE + " (_id integer primary key autoincrement, "
			+ KEY_LATITUDE + " real not null, "
			+ KEY_LONGITUDE + " real not null, "
			+ KEY_ALTITUDE + " real not null, "
			+ KEY_ACCURACY + " real not null, "
			+ KEY_PROVIDER + " text not null, "
			+ KEY_OBSERVATION_TYPE + " text not null, "
			+ KEY_OBSERVATION_UNIT + " text not null, "
			+ KEY_OBSERVATION_VALUE + " real not null, "
			+ KEY_SHARING + " text not null, "
			+ KEY_TIME + " real not null, "
			+ KEY_TIMEZONE + " real not null, "
			+ KEY_USERID + " text not null, "
			+ KEY_SENSOR_NAME + " text not null, "
			+ KEY_SENSOR_TYPE + " real not null, "			
			+ KEY_SENSOR_VENDOR + " text not null, "
			+ KEY_SENSOR_RESOLUTION + " real not null, "
			+ KEY_SENSOR_VERSION + " real not null)";
     * @return
     */
    public long addObservation(CbObservation observation) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_LATITUDE, observation.getLocation().getLatitude());
        initialValues.put(KEY_LONGITUDE, observation.getLocation().getLongitude());
        initialValues.put(KEY_ALTITUDE, observation.getLocation().getAltitude());
        initialValues.put(KEY_ACCURACY, observation.getLocation().getAccuracy());
        initialValues.put(KEY_PROVIDER, observation.getLocation().getProvider());
        initialValues.put(KEY_OBSERVATION_TYPE, observation.getObservationType());
        initialValues.put(KEY_OBSERVATION_UNIT, observation.getObservationUnit());
        initialValues.put(KEY_OBSERVATION_VALUE, observation.getObservationValue());
        initialValues.put(KEY_SHARING, observation.getSharing());
        initialValues.put(KEY_TIME, observation.getTime());
        initialValues.put(KEY_TIMEZONE, observation.getTimeZoneOffset());
        initialValues.put(KEY_USERID, observation.getUser_id());
        initialValues.put(KEY_SENSOR_NAME, observation.getSensor().getName());
        initialValues.put(KEY_SENSOR_TYPE, observation.getSensor().getType());
        initialValues.put(KEY_SENSOR_VENDOR, observation.getSensor().getVendor());
        initialValues.put(KEY_SENSOR_RESOLUTION, observation.getSensor().getResolution());
        initialValues.put(KEY_SENSOR_VERSION, observation.getSensor().getVersion());
        return mDB.insert(OBSERVATIONS_TABLE, null, initialValues);
    }
    
    /**
     * Add new settings for an application
     * @param appID
     * @param dataCollectionFrequency
     * @return
     */
    public long addSetting(String appID, long dataCollectionFrequency, String serverURL) {
    	
        ContentValues initialValues = new ContentValues();
        //System.out.println("adding " + appID + " freq " + dataCollectionFrequency);
        initialValues.put(KEY_APP_ID, appID);
        initialValues.put(KEY_DATA_COLLECTION_FREQUENCY, dataCollectionFrequency);
        initialValues.put(KEY_SERVER_URL, serverURL);
        return mDB.insert(SETTINGS_TABLE, null, initialValues);
    }

    /**
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
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
     * @param ctx the Context within which to work
     */
    public CbDb(Context ctx) {
        this.mContext = ctx;
    }
}
