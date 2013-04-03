package ca.cumulonimbus.pressurenetsdk;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * 
 * @author jacob
 * 
 */
public class CbService extends Service {
	
	public void startCollectingData() {
		log("CbService: Starting to collect data.");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		startCollectingData();
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public void log(String message) {
		System.out.println(message);
	}
	
}
