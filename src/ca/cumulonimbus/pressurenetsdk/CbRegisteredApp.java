package ca.cumulonimbus.pressurenetsdk;

/**
 * Register an application
 * 
 * @author jacob
 *
 */
public class CbRegisteredApp {
	
	private String packageName;
	private long registrationTime;
	
	public String getPackageName() {
		return packageName;
	}
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	public long getRegistrationTime() {
		return registrationTime;
	}
	public void setRegistrationTime(long registrationTime) {
		this.registrationTime = registrationTime;
	}
}
