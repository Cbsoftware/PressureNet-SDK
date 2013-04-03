package ca.cumulonimbus.pressurenetsdk;

import java.util.ArrayList;

/**
 * Multiple observations will be picked up at the same time. Store them together to keep organized. 
 * They may have the same latitude, longitude, time, privacy, id, client key values.
 * 
 * @author jacob
 *
 */

public class CbObservationGroup {

	private ArrayList<CbObservation> group;

	public ArrayList<CbObservation> getGroup() {
		return group;
	}

	public void setGroup(ArrayList<CbObservation> group) {
		this.group = group;
	}
}
