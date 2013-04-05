package ca.cumulonimbus.pressurenetsdk;


/**
 * Collect data from onboard sensors and store locally
 * 
 * @author jacob
 *
 */
public class CbDataCollector {

	private String userID = "";
	
	// TODO: Implement
	public CbObservation getPressureObservation() {
		CbObservation pressureObservation = new CbObservation();
		pressureObservation.setTime(System.currentTimeMillis());
		pressureObservation.setUser_id(userID);
		
		return pressureObservation;
	}
	
	public CbDataCollector(String userID) {
		this.userID = userID;
	}
}
