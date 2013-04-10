package ca.cumulonimbus.pressurenetsdk;

public class CbAccount {

	String userID;
	String email;
	long timeRegistered;
	

	public String[] getAccountAsParams() {
		String[] params = {"userID," + userID, 
						   "email," + email,
						   "time," + timeRegistered,
		};
		return params;
	}
	
	public String getUserID() {
		return userID;
	}
	public void setUserID(String userID) {
		this.userID = userID;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public long getTimeRegistered() {
		return timeRegistered;
	}
	public void setTimeRegistered(long timeRegistered) {
		this.timeRegistered = timeRegistered;
	}
}
