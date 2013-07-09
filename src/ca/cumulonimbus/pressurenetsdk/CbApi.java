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
import android.os.Handler;
import android.os.Messenger;

/**
 * Make pressureNET Live API calls and manage the results locally
 * 
 * @author jacob
 * 
 */
public class CbApi {

	Context context;
	String apiServerURL = "https://pressurenet.cumulonimbus.ca/list/?";
	String liveApiServerURL = "https://pressurenet.cumulonimbus.ca/live/?";
	String apiConditionsServerURL = "https://pressurenet.cumulonimbus.ca/conditions/list/?";
	private CbDb db;
	private ArrayList<CbWeather> callResults = new ArrayList<CbWeather>();

	private Messenger replyResult = null;

	private CbService caller;

	Handler handler = new Handler();
	String resultText = "";
	
	/**
	 * Make an API call and store the results
	 * 
	 * @return
	 */
	public long makeAPICall(CbApiCall call, CbService caller, Messenger ms, String callType) {

		this.replyResult = ms;
		this.caller = caller;
		APIDataDownload api = new APIDataDownload();
		api.setReplyToApp(ms);
		call.setCallType(callType);
		api.setApiCall(call);
		
		api.execute(callType);
		
		return System.currentTimeMillis();
	}

	/**
	 * When an API call finishes we'll have an ArrayList of results. Save them
	 * into the database
	 * 
	 * @param results
	 * @return
	 */
	private boolean saveAPIResults(ArrayList<CbWeather> results, CbApiCall api) {
		db.open();
		System.out.println("saving api results...");

		if(results.size()> 0) {
			db.addWeatherArrayList(results, api);
		}
		
		db.close();
		return false;
	}

	public CbApi(Context ctx) {
		context = ctx;
		db = new CbDb(context);
	}

	private class APIDataDownload extends AsyncTask<String, String, String> {

		Messenger replyToApp = null;
		private CbApiCall apiCall;
		
		
		
		public CbApiCall getApiCall() {
			return apiCall;
		}

		public void setApiCall(CbApiCall apiCall) {
			this.apiCall = apiCall;
		}

		public Messenger getReplyToApp() {
			return replyToApp;
		}

		public void setReplyToApp(Messenger replyToApp) {
			this.replyToApp = replyToApp;
		}

		@Override
		protected String doInBackground(String... params) {
			String responseText = "";
			try {
				DefaultHttpClient client = new DefaultHttpClient();
				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				nvps.add(new BasicNameValuePair("min_lat", apiCall.getMinLat()
						+ "" + ""));
				nvps.add(new BasicNameValuePair("max_lat", apiCall.getMaxLat()
						+ "" + ""));
				nvps.add(new BasicNameValuePair("min_lon", apiCall.getMinLon()
						+ "" + ""));
				nvps.add(new BasicNameValuePair("max_lon", apiCall.getMaxLon()
						+ "" + ""));
				nvps.add(new BasicNameValuePair("start_time", apiCall
						.getStartTime() + ""));
				nvps.add(new BasicNameValuePair("end_time", apiCall
						.getEndTime() + ""));
				nvps.add(new BasicNameValuePair("api_key", apiCall.getApiKey()));
				nvps.add(new BasicNameValuePair("format", apiCall.getFormat()));
				nvps.add(new BasicNameValuePair("limit", apiCall.getLimit()
						+ ""));
				nvps.add(new BasicNameValuePair("global", apiCall.isGlobal()
						+ ""));
				nvps.add(new BasicNameValuePair("since_last_call", apiCall
						.isSinceLastCall() + ""));

				String paramString = URLEncodedUtils.format(nvps, "utf-8");

				String serverURL = apiServerURL;
				
				if (params[0].equals("Readings")) {
					if(apiCall.getApiName().equals("live")) {
						serverURL = liveApiServerURL;
					} else if (apiCall.getApiName().equals("list")) {
						serverURL = apiServerURL;
					} else {
						serverURL = apiServerURL;
					}
				} else {
					serverURL = apiConditionsServerURL;
				}

				serverURL = serverURL + paramString;
				apiCall.setCallURL(serverURL);
				System.out.println("cbservice api sending " + serverURL);
				HttpGet get = new HttpGet(serverURL);
				// Execute the GET call and obtain the response
				HttpResponse getResponse = client.execute(get);
				HttpEntity responseEntity = getResponse.getEntity();
				System.out.println("response "
						+ responseEntity.getContentLength());

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
			resultText = result;
			
			handler.postDelayed(jsonProcessor, 0);
			

		}
		
		final Runnable jsonProcessor = new Runnable()
		{
			public void run() 
		    {
		    	callResults = processJSONResult(resultText, apiCall);
		    	saveAPIResults(callResults, apiCall);
				System.out.println("saved " + callResults.size()
						+ " api call results");
				caller.notifyAPIResult(replyToApp, callResults.size());	       
		    }
		};
	}

	
	
	
	/**
	 * Take a JSON string and return the data in a useful structure
	 * 
	 * @param resultJSON
	 */
	private ArrayList<CbWeather> processJSONResult(String resultJSON, CbApiCall apiCall) {
		ArrayList<CbWeather> obsFromJSON = new ArrayList<CbWeather>();
		System.out.println("processing json result for " + apiCall.getApiName() + " call type " + apiCall.getCallType());
		try {
			JSONArray jsonArray = new JSONArray(resultJSON);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				try {
					if(apiCall.getCallType().equals("Readings")) {
						CbObservation singleObs = new CbObservation();
						if(apiCall.getCallURL().contains("/live/")) {
							Location location = new Location("network");
							location.setLatitude(jsonObject.getDouble("latitude"));
							location.setLongitude(jsonObject.getDouble("longitude"));
							location.setAccuracy((float) jsonObject
							.getDouble("location_accuracy"));
							singleObs.setLocation(location);
							singleObs.setTime(jsonObject.getLong("daterecorded"));
							singleObs.setTimeZoneOffset(jsonObject.getLong("tzoffset"));
							singleObs.setSharing(jsonObject.getString("sharing"));
							singleObs.setUser_id(jsonObject.getString("user_id"));
							singleObs.setObservationValue(jsonObject
									.getDouble("reading"));
						} else if (apiCall.getCallURL().contains("/list/")) {

							singleObs.setTime(jsonObject.getLong("daterecorded"));
							singleObs.setObservationValue(jsonObject
									.getDouble("reading"));
						}
					
						obsFromJSON.add(singleObs);
					
					} else {
						CbCurrentCondition current = new CbCurrentCondition();
						Location location = new Location("network");
						location.setLatitude(jsonObject.getDouble("latitude"));
						location.setLongitude(jsonObject.getDouble("longitude"));
						current.setLocation(location);
						current.setGeneral_condition(jsonObject.getString("general_condition"));
						current.setTime(jsonObject.getLong("daterecorded"));
						//current.setTzoffset(jsonObject.getInt("tzoffset"));
						//current.setSharing_policy(jsonObject.getString("sharing"));
						//current.setUser_id(jsonObject.getString("user_id"));
						current.setWindy(jsonObject.getString("windy"));
						current.setFog_thickness(jsonObject.getString("fog_thickness"));
						current.setWindy(jsonObject.getString("precipitation_type"));
						current.setWindy(jsonObject.getString("precipitation_amount"));
						current.setWindy(jsonObject.getString("precipitation_unit"));
						current.setWindy(jsonObject.getString("thunderstorm_intensity"));
						current.setWindy(jsonObject.getString("user_comment"));						
						obsFromJSON.add(current);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}


		} catch (Exception e) {
			e.printStackTrace();
		}
		return obsFromJSON;
	}

}
