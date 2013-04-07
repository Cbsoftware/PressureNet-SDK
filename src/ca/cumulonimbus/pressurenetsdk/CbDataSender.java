package ca.cumulonimbus.pressurenetsdk;

import java.io.IOException;
import java.util.ArrayList;

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
		System.out.println("sending to " + settings.getServerURL() + " params " + params.length);
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

	@Override
	protected void onPostExecute(String result) {
		//System.out.println("post execute " + result);
		locationManager.stopGettingLocations();
		super.onPostExecute(result);
	}
}
