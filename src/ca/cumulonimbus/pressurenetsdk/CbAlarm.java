package ca.cumulonimbus.pressurenetsdk;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;

public class CbAlarm extends BroadcastReceiver {
	
	private boolean repeating = false;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK, "");
		wl.acquire();

		Intent serviceIntent = new Intent(context, CbService.class);
		serviceIntent.putExtra("alarm", true);
		context.startService(serviceIntent);
		
		wl.release();
	}

	public void restartAlarm(Context context, long time) {
		cancelAlarm(context);
		setAlarm(context, time);
	}
	
	public void setAlarm(Context context, long time) {
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent("ca.cumulonimbus.pressurenetsdk.START_ALARM");
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(),
				time, pi); // Millisec * Second * Minute
		repeating = true;
	}
	
	public void cancelAlarm(Context context) {
		System.out.println("cbservice cancelling alarm");
		Intent intent = new Intent("ca.cumulonimbus.pressurenetsdk.START_ALARM");
		PendingIntent sender = PendingIntent
				.getBroadcast(context, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
		repeating = false;
	}

	public boolean isRepeating() {
		return repeating;
	}

	public void setRepeating(boolean repeating) {
		this.repeating = repeating;
	}
	
	
}