package ca.cumulonimbus.pressurenetsdk;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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
	
	private int minDistance = 0;
	private int minTime = 1000 * 60 * 5; // minimum five minute update interval
	private Context context;

    private LocationManager networkLocationManager;
    private LocationManager gpsLocationManager;
    private LocationListener locationListener;
    
    private CbSettingsHandler settings;
    
    private Location currentBestLocation;
	
	private static final int TEN_MINUTES = 1000 * 60 * 10;

	
	public CbLocationManager(Context ctx) {
		context = ctx;
	}
	
	public Location getCurrentBestLocation() {
		log("returning best location, " + currentBestLocation.getProvider());
		return currentBestLocation;
	}
	
	public boolean stopGettingLocations() {
		log("stop getting locations");
		try {
			networkLocationManager.removeUpdates(locationListener);
	        gpsLocationManager.removeUpdates(locationListener);
	        networkLocationManager = null;
	        gpsLocationManager = null;
	        return true;
		} catch(Exception e) {
			log(e.getMessage());
			return false;
		}
	}
	
	// Get the user's location from the location service
    public boolean startGettingLocations() {
    	log("start getting locations");
    	networkLocationManager = (LocationManager)  context.getSystemService(Context.LOCATION_SERVICE);
    	gpsLocationManager = (LocationManager)  context.getSystemService(Context.LOCATION_SERVICE);

    	Location lastKnown = networkLocationManager.getLastKnownLocation("network");
    	if (lastKnown != null ) {
    		log("setting best = last known");
    		currentBestLocation = lastKnown;
    	} else {
    		log("last known is null");
    	}
    	
    	// Define a listener that responds to location updates
    	locationListener = new LocationListener() {
    	    public void onLocationChanged(Location location) {
    	        // Called when a new location is found by the network location provider.
	    	    if (isBetterLocation(location)) {
	    	    	log("found a better location " + location.getProvider());
	    	    	currentBestLocation = location;
	    	    } else {
	    	    	log("new location, it's not any better");
	    	    }
    	    }

    	    public void onStatusChanged(String provider, int status, Bundle extras) {}

    	    public void onProviderEnabled(String provider) {}

    	    public void onProviderDisabled(String provider) {}
    	};

       	// Register the listener with the Location Manager to receive location updates
    	try {
    		networkLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, locationListener);
    		gpsLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, locationListener);
    	} catch(Exception e) {
    		startGettingLocations();
    		log(e.getMessage());
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
	        return true;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = location.getTime() - currentBestLocation.getTime();
	    boolean isSignificantlyNewer = timeDelta > TEN_MINUTES;
	    boolean isSignificantlyOlder = timeDelta < -TEN_MINUTES;
	    boolean isNewer = timeDelta > 0;

	    // If it's been more than two minutes since the current location, use the new location
	    // because the user has likely moved
	    if (isSignificantlyNewer) {
	        return true;
	    // If the new location is more than two minutes older, it must be worse
	    } else if (isSignificantlyOlder) {
	        return false;
	    }

	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(location.getProvider(),
	            currentBestLocation.getProvider());

	    // Determine location quality using a combination of timeliness and accuracy
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
	
    public void log(String text) {
    	System.out.println(text);
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
