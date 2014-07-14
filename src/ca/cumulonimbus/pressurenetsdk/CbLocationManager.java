package ca.cumulonimbus.pressurenetsdk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
/**
 * 
 * Implement location strategy for GPS, Network location
 * Find the most accurate location within a time limit
 * Minimize battery use
 * 
 * @author jacob
 *
 */

public class CbLocationManager {
	private static final int TEN_MINUTES = 1000 * 60 * 10;
	private static final int TWO_MINUTES = 1000 * 60 * 2;
	
	private int minDistance = 0;
	private int minTime = TEN_MINUTES;
	private int minTimeGPS = 1000;
	private Context context;

    private LocationManager networkLocationManager;
    private LocationManager gpsLocationManager;
    private LocationListener locationListener;
    
    private CbSettingsHandler settings;
    
    private String mAppDir;
    
    private Location currentBestLocation;
    
    private Handler mHandler = new Handler();
	
    /**
     * Construct our location manager using network and gps managers, preferences
     * @param ctx
     */
	public CbLocationManager(Context ctx) {
		context = ctx;
		setUpFiles();
		try {
	    	networkLocationManager = (LocationManager)  context.getSystemService(Context.LOCATION_SERVICE);
	    	gpsLocationManager = (LocationManager)  context.getSystemService(Context.LOCATION_SERVICE);
	
    		settings = new CbSettingsHandler(context);
    		settings = settings.getSettings();
    		
    		Location lastKnownNetwork = networkLocationManager.getLastKnownLocation("network");
    
    		if(settings.isUseGPS()) {
    			Location lastKnownGPS = gpsLocationManager.getLastKnownLocation("gps");
    			if(lastKnownGPS != null ) {
    				currentBestLocation = lastKnownGPS;
    			} else {
    				currentBestLocation = lastKnownNetwork;
    			}
    		} else {
				currentBestLocation = lastKnownNetwork;
	    	} 
    	} catch(Exception e) {
    		//e.printStackTrace();
    	}
	}
	
	/**
	 * Give the current best location 
	 * @return
	 */
	public Location getCurrentBestLocation() {
		return currentBestLocation;
	}
	
	/**
	 * Stop all location listeners
	 * @return
	 */
	public boolean stopGettingLocations() {
		try {
			if(gpsLocationManager!=null) {
				log("stopping gps locations");
				gpsLocationManager.removeUpdates(locationListener);
			}
		} catch(Exception e ) {
			//e.printStackTrace();
		}
		try {
			if(locationListener!=null) {
				if(networkLocationManager!=null) {
					log("stopping network location");
					networkLocationManager.removeUpdates(locationListener);
				}
			}
		} catch(Exception e) {
			//e.printStackTrace();
		}
		return true;
	}
	
	private class LocationStopper implements Runnable  {

		@Override
		public void run() {
			stopGettingLocations();
		}
		
	}
	
	/**
	 * Get the user's location from the location service
	 * @return
	 */
    public boolean startGettingLocations() {
    	
    	// Define a listener that responds to location updates
    	locationListener = new LocationListener() {
    	    public void onLocationChanged(Location location) {
    	        // Called when a new location is found by the network location provider.
	    	    if (isBetterLocation(location)) {
	    	    	log("found a better location " + location.getProvider() + " " + location.getAltitude());
	    	    	currentBestLocation = location;
	    	    } else {
	    	    	log("new location, it's not any better " + location.getProvider() + ", best altitude is " + currentBestLocation.getAltitude());
	    	    }
    	    	LocationStopper stopLater = new LocationStopper();
    	    	mHandler.postDelayed(stopLater, 1000 * 10);
    	    }

    	    public void onStatusChanged(String provider, int status, Bundle extras) {}

    	    public void onProviderEnabled(String provider) {}

    	    public void onProviderDisabled(String provider) {}
    	};

       	// Register the listener with the Location Manager to receive location updates
    	try {
    		networkLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, locationListener);
    		if(settings.isUseGPS()) {
    			log("cblocationmanager starting gps location updates");
    			gpsLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeGPS, minDistance, locationListener);
    		} else {
    			log("cblocationmanager NOT starting gps location updates");
    		}
    	} catch(Exception e) {
    		//e.printStackTrace();
    		return false;
    	}
    	return true;
    	
    }
	
	/** Determines whether one Location reading is better than the current Location fix
	  * @param location  The new Location that you want to evaluate
	  * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	  */
	protected boolean isBetterLocation(Location location) {
	    if (currentBestLocation == null) {
	        // A new location is always better than no location
	    	log("new location is always better than no location");
	        return true;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = location.getTime() - currentBestLocation.getTime();
	    boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
	    boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
	    boolean isNewer = timeDelta > 0;
	    
	
	    /*
	    // If it's been more than two minutes since the current location, use the new location
	    // because the user has likely moved
	    if (isSignificantlyNewer) {
	    	log("new location is significantly newer");
	        return true;
	    // If the new location is more than two minutes older, it must be worse
	    } else if (isSignificantlyOlder) {
	    	log("new location is significantly older");
	        return false;
	    }
	    */
	    
	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(location.getProvider(),
	            currentBestLocation.getProvider());
	    
	   
	    
	    // Determine location quality using a combination of timeliness, accuracy, and completeness (altitude)
	    if (isMoreAccurate) {
	    	return true;
	    } else if (isNewer && !isLessAccurate) {
	        return true;
	    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	        return true;
	    }
	    return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}
	

    /**
     *  Prepare to write a log to SD card. Not used unless logging enabled.
     */
    public void setUpFiles() {
    	try {
	    	File homeDirectory = context.getExternalFilesDir(null);
	    	if(homeDirectory!=null) {
	    		mAppDir = homeDirectory.getAbsolutePath();
	    	}
    	} catch (Exception e) {
    		//e.printStackTrace();
    	}
    }
   
	/**
	 * Log data to SD card for debug purposes.
	 * To enable logging, ensure the Manifest allows writing to SD card.
	 * @param text
	 */
	public void logToFile(String text) {
		try {
			OutputStream output = new FileOutputStream(mAppDir + "/log.txt", true);
			String logString = (new Date()).toString() + ": " + text + "\n";
			output.write(logString.getBytes());
			output.close();
		} catch(FileNotFoundException e) {
			//e.printStackTrace();
		} catch(IOException ioe) {
			//ioe.printStackTrace();
		}
	}
	
    public void log(String message) {
    	if(CbConfiguration.DEBUG_MODE) {
	    	//logToFile(message);
	    	System.out.println(message);
    	}
    }
    
	public int getMinDistance() {
		return minDistance;
	}
	public void setMinDistance(int minDistance) {
		this.minDistance = minDistance;
	}
	public int getMinTime() {
		return minTime;
	}
	public void setMinTime(int minTime) {
		this.minTime = minTime;
	}
}
