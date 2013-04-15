package ca.cumulonimbus.pressurenetsdk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;


/**
 * Securely send collected data to servers
 * 
 * @author jacob
 *
 */

public class CbDataSender  extends AsyncTask<String, Integer, String> {

	private String responseText = "";
	private static final String PREFS_NAME = "pressureNETPrefs";
	private CbLocationManager locationManager;
	
	private CbSettingsHandler settings;
	
	private Context context;
	private String mAppDir;
	
	public CbDataSender(Context ctx) {
		this.context = ctx;
		setUpFiles();
	}
	
	public CbSettingsHandler getSettings() {
		return settings;
	}
	public void setSettings(CbSettingsHandler settings, CbLocationManager locationManager) {
		this.settings = settings;
		this.locationManager = locationManager;
	}

	@Override
	protected String doInBackground(String... params) {
		// TODO: SecureHttpClient
		DefaultHttpClient client = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(settings.getServerURL());
		try {
			ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
			for(String singleParam : params) {
				String key = singleParam.split(",")[0];
				String value = singleParam.split(",")[1];
				nvps.add(new BasicNameValuePair(key, value));
			} 
			httppost.setEntity(new UrlEncodedFormEntity(nvps));
			client.execute(httppost);
			
		} catch(ClientProtocolException cpe) {
			cpe.printStackTrace();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		} catch(ArrayIndexOutOfBoundsException aioobe) {
			aioobe.printStackTrace();
		}
		return responseText;
	}
	
	public void log(String message) {
		//logToFile(message);
		//System.out.println(message);
	}

    // Used to write a log to SD card. Not used unless logging enabled.
    public void setUpFiles() {
    	try {
	    	File homeDirectory = context.getExternalFilesDir(null);
	    	if(homeDirectory!=null) {
	    		mAppDir = homeDirectory.getAbsolutePath();
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }

	@Override
	protected void onPostExecute(String result) {
		//System.out.println("post execute " + result);
		locationManager.stopGettingLocations();
		super.onPostExecute(result);
	}
	
	// Log data to SD card for debug purposes.
	// To enable logging, ensure the Manifest allows writing to SD card.
	public void logToFile(String text) {
		try {
			OutputStream output = new FileOutputStream(mAppDir + "/log.txt", true);
			String logString = (new Date()).toString() + ": " + text + "\n";
			output.write(logString.getBytes());
			output.close();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
}
