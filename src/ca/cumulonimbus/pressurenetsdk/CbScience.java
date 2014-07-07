package ca.cumulonimbus.pressurenetsdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.location.Location;

/**
 * Science methods and classes. Provide simple data processing like trend
 * discovery. This will eventually expand to forecasting and other atmospheric
 * science algorithms.
 * 
 * @author jacob
 * 
 */
public class CbScience {

	// Mean Sea-Level Pressure calculation constants and estimates
	private static final double STANDARD_PRESSURE = 1013.25; // mb
	private static final double e = 2.71828;
	private static final double GRAVITY = -9.80665;
	private static final double M = 0.0289644; // Molar mass of Earth's air, km/mol
	private static final double R_STAR = 8.31432; // universal gas constant for air; N*m /  (mol*)
    public static final double EARTH_RADIUS = 6372.8; // In kilometers
	
	/**
	 * Estimate MSLP
	 * 
	 * @param altitude (m)
	 * @param temperature (K)
	 * 
	 * MSLP ~= pressure - (.12mbar / 1m)
	 * 
	 * @return
	 */
	public static double estimateMSLP(double pressure, double altitude, double temperature) {
		// System.out.println("estimating mslp with pressure " + pressure + " and altitude " + altitude);
		if(altitude!=0) {
			return (pressure + (.12*altitude));
		} else {
			return pressure;
		}
	}
	
    /**
     * Calculate distance in km between two points on the surface of the Earth
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @return
     */
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
 
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double kmReturn = EARTH_RADIUS * c;
        double mReturn = kmReturn * 1000;
        return mReturn;
    }
    
    public static double angle(double lat1, double long1, double lat2,
            double long2) {

        double dLon = Math.toRadians(long2 - long1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = (Math.cos(lat1) * Math.sin(lat2)) - (Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon));

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;
        brng = 360 - brng;

        return brng;
    }
    
    private static void log(String message) {
    	if(CbConfiguration.DEBUG_MODE) {
    		System.out.println(message);
    	}
    }
    
    /**
     * 
     * @param angle
     * @return
     */
    public static String englishDirection(double angle ) {
    	log("english direction input " + angle);
		if ( (angle > 0) && (angle <= 22.5) ) {
			return "North";
		} else if ((angle > 22.5) && (angle <= 67.5) ){
			return "Northeast";
		} else if ((angle > 67.5) && (angle <= 112.5) ){
			return "East";
		} else if ((angle > 112.5) && (angle <= 157.5) ){
			return "Southeast";
		} else if ((angle > 157.5) && (angle <= 202.5) ){
			return "South";
		} else if ((angle > 202.5) && (angle <= 247.5) ){
			return "Southwest";
		} else if ((angle > 247.5) && (angle <= 292.5) ){
			return "West";
		} else if ((angle > 292.5) && (angle <= 337.5) ){
			return "Northwest";
		} else if ((angle > 337.5) && (angle <= 360) ){
			return "North";
		} else {
			return "";
		}
	}
    
	/**
	 * Look for a recent change in trend
	 */
	public static String changeInTrend(List<CbObservation> recents) {
		// Reject the request if there's not enough data
		if(recents == null) {
			return "";
		} else if (recents.size() < 3 ) {
			return "";
		}
		//System.out.println("change in trend recents size " + recents.size());
		
		// split up the lists.
		Collections.sort(recents, new TimeComparator());
		List<CbObservation> firstHalf = recents.subList(0, recents.size() / 2);
		List<CbObservation> secondHalf = recents.subList(recents.size() / 2, recents.size() - 1);
		String firstTendency = CbScience.findApproximateTendency(firstHalf);
		String secondTendency = CbScience.findApproximateTendency(secondHalf);
		return firstTendency + "," + secondTendency;
	}
	
	/**
	 * Take a list of recent observations and return their trend
	 * @param recents
	 * @return
	 */
	public static String findApproximateTendency(List<CbObservation> recents) {
		// Reject the request if there's not enough data
		if (recents == null) {
			return "Unknown null";
		}
		if (recents.size() < 3) {
			return "Unknown too small";
		}
		
		// Reject the request if the location coordinates vary too much
		// TODO: future revisions should account for this change and perform
		// the altitude correction in order to use the data rather than bailing
		if (! locationsAreClose(recents)) {
			return "Unknown distance";
		}

		double decision = guessedButGoodDecision(recents);

		//System.out.println("decision  " + decision);
		if (decision > .01) {
			return "Rising";
		} else if (decision <= -.01) {
			return "Falling";
		} else if ((decision >=-.01 ) && (decision <=.01)) {
			return "Steady";
		} else {
			return "Unknown decision " + decision;
		}
	}
	
	/**
	 * Determine if a list of locations are all close by
	 * @param recents
	 * @return
	 */
	private static boolean locationsAreClose(List<CbObservation> recents) {
		double minLat = 90;
		double maxLat = -90;
		double minLon = 180;
		double maxLon = -180;
		for (CbObservation obs : recents ) {
			Location location = obs.getLocation();
			double latitude = location.getLatitude();
			double longitude = location.getLongitude();
			if(latitude > maxLat) {
				maxLat = latitude;
			} 
			if(latitude < minLat) {
				minLat = latitude;
			}
			if(longitude > maxLon) {
				maxLon = longitude;
			}
			if(longitude < minLon) {
				minLon = longitude;
			}
		}
		
		float[] results = new float[2];
		Location.distanceBetween(minLat, minLon, maxLat, maxLon, results);
		float distanceMeters = results[0];
		
		//System.out.println(distanceMeters + "; Locations' proximity for change notification: " + minLat + " to " + maxLat + ", " + minLon + " to " + minLon);

		if(distanceMeters < 2000) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Calculate the slope of a line of best fit
	 * @param recents
	 * @return
	 */
	public static double slopeOfBestFit(List<CbObservation> recents) {
		long time[] = new long[recents.size()];
		double pressure[] = new double[recents.size()];
		int x = 0;
		long sumTime = 0L;
		double sumPressure = 0L;
		for (CbObservation obs : recents) {
			time[x] = obs.getTime() / (1000 * 3600);
			sumTime = sumTime + time[x];
			//sumTime += time[x] * time[x];
			pressure[x] = obs.getObservationValue();
			sumPressure = sumPressure + pressure[x];
			x++;
		}
		long timeBar = sumTime / x;
		double pressureBar = sumPressure / x;
		double ttBar = 0.0; 
		double tpBar = 0.0;
		for (int y = 0; y < x; y++) {
			ttBar += (time[y] - timeBar) * (time[y] - timeBar);
			tpBar += (time[y] - timeBar) * (pressure[y] - pressureBar);
		}
		double beta1 = tpBar / ttBar;
		return beta1;
	}

	/**
	 *  Take a good guess about the recent trends
	 *  to see if they appear meteorological  
	 *   
	 * @param recents
	 * @return
	 */
	public static double guessedButGoodDecision(List<CbObservation> recents) {
		// (TODO: There's too much sorting going on here. Should use min and max)
		// Sort by pressure
		Collections.sort(recents, new SensorValueComparator());
		double minPressure = recents.get(0).getObservationValue();
		double maxPressure = recents.get(recents.size() - 1)
				.getObservationValue();
		// Sort by time
		Collections.sort(recents, new TimeComparator());
		
		return slopeOfBestFit(recents);
	}


	/**
	 * Compare two current conditions' time vales
	 * @author jacob
	 *
	 */
	public static class ConditionTimeComparator implements Comparator<CbCurrentCondition> {
		@Override
		public int compare(CbCurrentCondition o1, CbCurrentCondition o2) {
			if (o1.getTime() < o2.getTime()) {
				return -1;
			} else {
				return 1;
			}
		}
	}
	
	/**
	 * Compare to observation's time values
	 * @author jacob
	 *
	 */
	public static class TimeComparator implements Comparator<CbObservation> {
		@Override
		public int compare(CbObservation o1, CbObservation o2) {
			if (o1.getTime() < o2.getTime()) {
				return -1;
			} else {
				return 1;
			}
		}
	}

	/**
	 * Compare to observation's sensor values
	 * @author jacob
	 *
	 */
	public static class SensorValueComparator implements
			Comparator<CbObservation> {
		@Override
		public int compare(CbObservation o1, CbObservation o2) {
			if (o1.getObservationValue() < o2.getObservationValue()) {
				return -1;
			} else {
				return 1;
			}
		}
	}

}