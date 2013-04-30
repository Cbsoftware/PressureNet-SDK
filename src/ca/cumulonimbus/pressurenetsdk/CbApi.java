package ca.cumulonimbus.pressurenetsdk;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;

/**
 * Make pressureNET Live API calls
 * and manage the results locally
 * 
 * @author jacob
 *
 */
public class CbApi {
	
	Context context;
	String apiServerURL = "https://pressurenet.cumulonimbus.ca/live/?"; // TODO: Should be ArrayList
	private CbDb db;
	private CbApiCall apiCall;
	private ArrayList<CbObservation> callResults = new ArrayList<CbObservation>();
	
	/**
	 * Make an API call and store the results
	 * @return
	 */
	public boolean makeAPICall(CbApiCall call) {
		apiCall = call;
		APIDataDownload api = new APIDataDownload();
		api.execute("");
		return false;
	}
	
	
	/**
	 * When an API call finishes we'll have an ArrayList of results.
	 * Save them into the database
	 * @param results
	 * @return
	 */
	private boolean saveAPIResults(ArrayList<CbObservation> results) {
		db.open();
		for(CbObservation obs : results) {
			db.addAPICacheObservation(obs);
		}
		
		db.close();
		return false;
	}
	
	public CbApi (Context ctx) {
		context = ctx;
		db = new CbDb(context);
	}
	
	private class APIDataDownload extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... arg0) {
			String responseText = "";
			try {
				DefaultHttpClient client = new DefaultHttpClient();

				System.out.println("contacting api...");
				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				nvps.add(new BasicNameValuePair("min_lat", apiCall.getMinLat() + ""
						+ ""));
				nvps.add(new BasicNameValuePair("max_lat", apiCall.getMaxLat() + ""
						+ ""));
				nvps.add(new BasicNameValuePair("min_lon", apiCall.getMinLon() + ""
						+ ""));
				nvps.add(new BasicNameValuePair("max_lon", apiCall.getMaxLon() + ""
						+ ""));
				nvps.add(new BasicNameValuePair("start_time", apiCall.getStartTime() + ""));
				nvps.add(new BasicNameValuePair("end_time", apiCall.getEndTime() + ""));
				nvps.add(new BasicNameValuePair("api_key", apiCall.getApiKey()));
				nvps.add(new BasicNameValuePair("format", "json"));
				nvps.add(new BasicNameValuePair("limit", "5000")); // TODO: User preference

				String paramString = URLEncodedUtils.format(nvps, "utf-8");

				apiServerURL = apiServerURL + paramString;
				System.out.println(apiServerURL);
				HttpGet get = new HttpGet(apiServerURL);

				// Execute the GET call and obtain the response
				HttpResponse getResponse = client.execute(get);
				HttpEntity responseEntity = getResponse.getEntity();

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
				System.out.println("api error");
				e.printStackTrace();
			}
			return responseText;
		}

		protected void onPostExecute(String result) {
			processJSONResult(result);
		}
	}
	
	/**
	 * Take a JSON string and return the data in a useful structure
	 * 
	 * @param resultJSON
	 */
	void processJSONResult(String resultJSON) {
		try {
			JSONArray jsonArray = new JSONArray(resultJSON);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				CbObservation singleObs = new CbObservation();
				try {
					Location location = new Location("network");
					location.setLatitude(jsonObject.getDouble("latitude"));
					location.setLongitude(jsonObject.getDouble("longitude"));
					location.setAccuracy((float) jsonObject
							.getDouble("location_accuracy"));
					singleObs.setLocation(location);
					singleObs.setTime(jsonObject.getLong("daterecorded"));
					singleObs.setTimeZoneOffset(jsonObject
							.getDouble("tzoffset"));
					singleObs.setSharing(jsonObject.getString("sharing"));
					singleObs.setUser_id(jsonObject.getString("user_id"));
					singleObs.setObservationValue(jsonObject
							.getDouble("reading"));
					callResults.add(singleObs);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// TODO: Add dates and trends prior to graphing.
			//ArrayList<CbObservation> detailedList = CbObservation.addDatesAndTrends(apiCbObservationResults);
			//recents = CbObservation.addDatesAndTrends(apiCbObservationResults);
			
			saveAPIResults(callResults);
			System.out.println("saved " + callResults.size() + " api call results");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
}
