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
	
	// Fields
	public static final String KEY_ROW_ID = "_id";
	public static final String KEY_APP_ID = "app_id";
	public static final String KEY_DATA_COLLECTION_FREQUENCY = "data_frequency";
	public static final String KEY_SERVER_URL = "server_url";

	private Context mContext;
	
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDB;
	
	private static final String DATABASE_CREATE = "create table " 
			+ SETTINGS_TABLE + " (_id integer primary key autoincrement, "
			+ KEY_APP_ID + " text not null, "
			+ KEY_DATA_COLLECTION_FREQUENCY + " real not null, "
			+ KEY_SERVER_URL + " text not null)"; 
	private static final String DATABASE_NAME = "CbDb";
	private static final int DATABASE_VERSION = 3;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
	
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// Build upgrade mechanism
			db.execSQL("DROP TABLE IF EXISTS " + SETTINGS_TABLE);
			onCreate(db);
		}
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
