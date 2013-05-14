package ca.cumulonimbus.pressurenetsdk;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Science methods and classes. Provide simple data processing like trend
 * discovery. This will eventually expand to forecasting and other atmospheric
 * science algorithms.
 * 
 * @author jacob
 * 
 */
public class CbScience {

	/**
	 * Take a list of recent observations and return their trend
	 * @param recents
	 * @return
	 */
	public static String findApproximateTendency(List<CbObservation> recents) {
		if (recents == null) {
			return "Unknown";
		}
		if (recents.size() < 3) {
			return "Unknown";
		}

		int decision = guessedButGoodDecision(recents);


		if (decision == 1) {
			return "Rising";
		} else if (decision == -1) {
			return "Falling";
		} else if (decision == 0) {
			return "Steady";
		} else {
			return "Unknown";
		}
	}

	private static int slopeOfBestFit(List<CbObservation> recents) {
		double time[] = new double[recents.size()];
		double pressure[] = new double[recents.size()];
		int x = 0;
		long sumTime = 0L;
		long sumPressure = 0L;
		for (CbObservation obs : recents) {
			time[x] = obs.getTime();
			sumTime += time[x];
			sumTime += time[x] * time[x];
			sumPressure += pressure[x];
			pressure[x] = obs.getObservationValue();
			x++;
		}
		double timeBar = sumTime / x;
		double pressureBar = sumPressure / x;
		double ttBar = 0.0;
		double tpBar = 0.0;
		for (int y = 0; y < x; y++) {
			ttBar += (time[y] - timeBar) * (time[y] - timeBar);
			tpBar += (time[y] - timeBar) * (pressure[y] - pressureBar);
		}
		double beta1 = tpBar / ttBar;
		if (beta1 < -0.05) {
			return -1;
		} else if (beta1 > 0.05) {
			return 1;
		} else if (beta1 >= -0.05 && beta1 <= 0.05) {
			return 0;
		} else {
			return 0;
		}
	}

	// Take a good guess about the recent meteorological trends
	// (TODO: There's too much sorting going on here. Should use min and max)
	private static int guessedButGoodDecision(List<CbObservation> recents) {
		// Sort by pressure
		Collections.sort(recents, new SensorValueComparator());
		double minPressure = recents.get(0).getObservationValue();
		double maxPressure = recents.get(recents.size() - 1)
				.getObservationValue();

		// Sort by time
		Collections.sort(recents, new TimeComparator());
		double minTime = recents.get(0).getTime();
		double maxTime = recents.get(recents.size() - 1).getTime();
		// Start time at 0
		for (CbObservation obs : recents) {
			// we'd like to compare delta pressure and delta time
			// preferably in millibars and hours.
			obs.setTime((long)(obs.getTime() - minTime) / (1000 * 3600));
			obs.setObservationValue(obs.getObservationValue() - minPressure);
		}
		int slope = slopeOfBestFit(recents);

		return slope;
	}


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