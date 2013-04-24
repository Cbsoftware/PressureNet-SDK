package ca.cumulonimbus.pressurenetsdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.view.MotionEvent;

import com.google.android.maps.MapView;

public class CbMapView extends MapView {

	int oldZoomLevel = -1;
	public static final String CB_REFRESH_MAP_INTENT = "ca.cumulonimbus.pressurenetsdk.REFRESHMAP";
	
	public class OutgoingReceiver extends BroadcastReceiver {

	    @Override
	    public void onReceive(Context context, Intent intent) {
	        Intent i = new Intent();
	        i.setAction(CB_REFRESH_MAP_INTENT);
	        context.sendBroadcast(i);
	    }

	}
	
	public void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		try {
			if (getZoomLevel() != oldZoomLevel) {
		        Intent i = new Intent();
		        i.setAction(CB_REFRESH_MAP_INTENT);
		        getContext().sendBroadcast(i);
				oldZoomLevel = getZoomLevel();
			}
		} catch(Exception e) {
			// TODO: See onTouchEvent.

		}
	}

	public CbMapView (android.content.Context context,
			android.util.AttributeSet attrs) {
		super(context, attrs);
	}

	public boolean onTouchEvent(MotionEvent ev) {
		try {
			if (ev.getAction() == MotionEvent.ACTION_UP) {
		        Intent i = new Intent();
		        i.setAction(CB_REFRESH_MAP_INTENT);
		        getContext().sendBroadcast(i);
			}
		} catch(Exception e) {
			// TODO: Investigate. Stack trace on Android Developer console. Report Oct 19, 2011.
			// Autorefresh failed. User can still press Reload to get new data. 
		}
		return super.onTouchEvent(ev);
	}

	public CbMapView (android.content.Context context,
			android.util.AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public CbMapView (android.content.Context context,
			java.lang.String apiKey) {
		super(context, apiKey);
	}
}