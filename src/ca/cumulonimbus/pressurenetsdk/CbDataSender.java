package ca.cumulonimbus.pressurenetsdk;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.os.AsyncTask;


/**
 * Securely send collected data to servers
 * 
 * @author jacob
 *
 */

public class CbDataSender  extends AsyncTask<String, Integer, String> {

	Context appContext = null;
	private String responseText = "";
	private static final String PREFS_NAME = "pressureNETPrefs";
	
	private CbSettingsHandler settings = new CbSettingsHandler();
	
	public CbDataSender(Context context) {
		appContext = context;
	}
	
	@Override
	protected String doInBackground(String... params) {
		// TODO: SecureHttpClient
		DefaultHttpClient client = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(settings.getServerURL());
		try {

			/*
			 * Check Sharing preferences before sending.
			SharedPreferences settings = appContext.getSharedPreferences(PREFS_NAME, 0);
			String share = settings.getString("sharing_preference", "Us, Researchers and Forecasters");
			
			// No sharing? get out!
			if(share.equals("Nobody")) {
				return null;
			}
			*/
			
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
		// TODO: Implement
		super.onPostExecute(result);
	}
}
