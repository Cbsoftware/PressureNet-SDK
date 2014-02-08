package ca.cumulonimbus.pressurenetsdk;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.os.AsyncTask;

/**
 * Gather weather data for external sources. Use for MSLP correction,
 * weather alerts, etc.
 * 
 * @author jacob
 *
 */
public class CbExternalWeatherData {
	
	public String apiString = "";
	
	ArrayList<CbExpandedCondition> externalResults = new ArrayList<CbExpandedCondition>();
	
	public void getCurrentTemperatureForLocation (double latitude, double longitde) {
		apiString = CbConfiguration.EXTERNAL_URL + 
						   CbConfiguration.EXTERNAL_KEY + "/" +
						   latitude + "," + longitde;
		ExternalDataDownload download = new ExternalDataDownload();
		download.execute("");
		
	}
	
	private class ExternalDataDownload extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {
			String responseText = "";
			try {
				DefaultHttpClient client = new DefaultHttpClient();
				log("cbexternal api sending " + apiString);
				HttpGet get = new HttpGet(apiString);
				// Execute the GET call and obtain the response
				HttpResponse getResponse = client.execute(get);
				HttpEntity responseEntity = getResponse.getEntity()	;
				log("cbexternal api response " + responseEntity.getContentLength());

				BufferedReader r = new BufferedReader(new InputStreamReader(
						responseEntity.getContent()));

				StringBuilder total = new StringBuilder();
				String line;
				if (r != null) {
					while ((line = r.readLine()) != null) {
						total.append(line);
					}
					responseText = total.toString();
				}
			} catch (Exception e) {
				System.out.println("cbexternal api error");
				e.printStackTrace();
			}
			return responseText;
		}

		protected void onPostExecute(String result) {
			log(result);
			//externalResults = processJSONConditions(result);
			//log("externalResults " + externalResults.size());
		}
	}
	
	/**
	 * Take a JSON string and return the data in a useful structure
	 * 
	 * @param resultJSON
	 */
	private ArrayList<CbExpandedCondition> processJSONConditions(String resultJSON) {
		ArrayList<CbExpandedCondition> conditions = new ArrayList<CbExpandedCondition>();
		try {
			JSONArray jsonArray = new JSONArray(resultJSON);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				CbExpandedCondition condition = new CbExpandedCondition();
				//condition.set(jsonObject.getInt("users"));
				//condition.setMin(jsonObject.getDouble("min"));
				conditions.add(condition);
			}

		} catch (Exception e) {
			log("error in parsing external api");
		}
		return conditions;
	}
	
	public CbExternalWeatherData() {
		
	}
	
	public void log(String message) {
		if (CbConfiguration.DEBUG_MODE) {
			System.out.println(message);
		}
	}
	
}
