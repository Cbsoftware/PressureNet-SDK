package ca.cumulonimbus.pressurenetsdk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
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
 * Make PressureNet Live API calls and manage the results locally
 * 
 * @author jacob
 * 
 */
public class CbApi {

	Context context;
	String apiServerURL 			= CbConfiguration.SERVER_URL_PRESSURENET + "list/?";
	String apiConditionsServerURL 	= CbConfiguration.SERVER_URL_CONDITIONS_QUERY; // CbConfiguration.SERVER_URL_PRESSURENET + "conditions/list/?";
	String apiStatsServerURL	 	= CbConfiguration.SERVER_URL_PRESSURENET + "stats/?";
	private CbDb db;
	private ArrayList<CbWeather> callResults = new ArrayList<CbWeather>();
	private ArrayList<CbStats> statsResults = new ArrayList<CbStats>();

	private Messenger replyResult = null;

	private CbService caller;

	Handler handler = new Handler();
	String resultText = "";
	
	private String mAppDir = "";

	/**
	 * Make an API call and store the results
	 * 
	 * @return
	 */
	public long makeAPICall(CbApiCall call, CbService caller, Messenger ms,
			String callType) {

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
	 * Make an API call and store the results
	 * 
	 * @return
	 */
	public long makeStatsAPICall(CbStatsAPICall call, CbService caller, Messenger ms) {

		this.replyResult = ms;
		this.caller = caller;
		StatsDataDownload api = new StatsDataDownload();
		api.setReplyToApp(ms);
		api.setApiCall(call);
		api.execute("");

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
		log("saving api results...");

		if (results.size() > 0) {
			db.addWeatherArrayList(results, api);
		}

		db.close();
		return false;
	}

	public CbApi(Context ctx) {
		context = ctx;
		db = new CbDb(context);
		setUpFiles();
	}
	
	/**
	 * Prepare to write a log to SD card. Not used unless logging enabled.
	 */
	private void setUpFiles() {
		try {
			File homeDirectory = context.getExternalFilesDir(null);
			if (homeDirectory != null) {
				mAppDir = homeDirectory.getAbsolutePath();

			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
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
				nvps.add(new BasicNameValuePair("min_latitude", apiCall
						.getMinLat() + "" + ""));
				nvps.add(new BasicNameValuePair("max_latitude", apiCall
						.getMaxLat() + "" + ""));
				nvps.add(new BasicNameValuePair("min_longitude", apiCall
						.getMinLon() + "" + ""));
				nvps.add(new BasicNameValuePair("max_longitude", apiCall
						.getMaxLon() + "" + ""));
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
				nvps.add(new BasicNameValuePair("sdk_version", CbConfiguration.SDK_VERSION));
				nvps.add(new BasicNameValuePair("source", "pressurenet"));

				String paramString = URLEncodedUtils.format(nvps, "utf-8");

				String serverURL = apiServerURL;

				if (params[0].equals("Readings")) {
					serverURL = apiServerURL;
				} else {
					serverURL = apiConditionsServerURL;
				}

				serverURL = serverURL + paramString;
				apiCall.setCallURL(serverURL);
				log("cbservice api sending " + serverURL);
				HttpGet get = new HttpGet(serverURL);
				// Execute the GET call and obtain the response
				HttpResponse getResponse = client.execute(get);
				HttpEntity responseEntity = getResponse.getEntity()	;
				log("response " + responseEntity.getContentLength());

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
				// System.out.println("api error");
				//e.printStackTrace();
			}
			log(responseText);
			return responseText;
		}

		protected void onPostExecute(String result) {
			resultText = result;

			// handler.postDelayed(jsonProcessor, 0);
			SaveAPIData save = new SaveAPIData();
			save.execute("");
		}

		private class SaveAPIData extends AsyncTask<String, String, String> {

			@Override
			protected String doInBackground(String... params) {
				callResults = processJSONResult(resultText, apiCall);
				saveAPIResults(callResults, apiCall);
				return null;
			}

			@Override
			protected void onPostExecute(String result) {
				// System.out.println("saved " + callResults.size() +
				// " api call results");
				caller.notifyAPIResult(replyToApp, callResults.size());

				super.onPostExecute(result);
			}
		}

	}
	
	private class StatsDataDownload extends AsyncTask<String, String, String> {

		Messenger replyToApp = null;
		private CbStatsAPICall apiCall;

		public CbStatsAPICall getApiCall() {
			return apiCall;
		}

		public void setApiCall(CbStatsAPICall apiCall) {
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
				nvps.add(new BasicNameValuePair("min_latitude", apiCall
						.getMinLatitude() + "" + ""));
				nvps.add(new BasicNameValuePair("max_latitude", apiCall
						.getMaxLatitude() + "" + ""));
				nvps.add(new BasicNameValuePair("min_longitude", apiCall
						.getMinLongitude() + "" + ""));
				nvps.add(new BasicNameValuePair("max_longitude", apiCall
						.getMaxLongitude() + "" + ""));
				nvps.add(new BasicNameValuePair("start_time", apiCall
						.getStartTime() + ""));
				nvps.add(new BasicNameValuePair("end_time", apiCall
						.getEndTime() + ""));
				nvps.add(new BasicNameValuePair("log_duration", apiCall.getLogDuration()));

				String paramString = URLEncodedUtils.format(nvps, "utf-8");

				String serverURL = apiStatsServerURL;

				serverURL = serverURL + paramString;
				
				log("cbservice api sending " + serverURL);
				HttpGet get = new HttpGet(serverURL);
				// Execute the GET call and obtain the response
				HttpResponse getResponse = client.execute(get);
				HttpEntity responseEntity = getResponse.getEntity()	;
				log("stats response " + responseEntity.getContentLength());

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
				//System.out.println("api error");
				e.printStackTrace();
			}
			return responseText;
		}

		protected void onPostExecute(String result) {
			resultText = result;
			statsResults = processJSONStats(resultText);
			log("statsResults" + statsResults.size());
			caller.notifyAPIStats(replyToApp, statsResults);
		}
	}
	
	/**
	 * Take a JSON string and return the data in a useful structure
	 * 
	 * @param resultJSON
	 */
	private ArrayList<CbStats> processJSONStats(String resultJSON) {
		ArrayList<CbStats> statistics = new ArrayList<CbStats>();
		try {
			JSONArray jsonArray = new JSONArray(resultJSON);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				CbStats singleStat = new CbStats();
				singleStat.setUsers(jsonObject.getInt("users"));
				singleStat.setMin(jsonObject.getDouble("min"));
				singleStat.setTimeStamp(jsonObject.getLong("timestamp"));
				singleStat.setMedian(jsonObject.getDouble("median"));
				singleStat.setGeohash(jsonObject.getString("geohash"));
				singleStat.setSamples(jsonObject.getInt("samples"));
				singleStat.setMax(jsonObject.getDouble("max"));
				singleStat.setStdDev(jsonObject.getDouble("std_dev"));
				singleStat.setMean(jsonObject.getDouble("mean"));
				statistics.add(singleStat);
			}

		} catch (Exception e) {
			//e.printStackTrace();
		}
		return statistics;
	}

	/**
	 * Take a JSON string and return the data in a useful structure
	 * 
	 * @param resultJSON
	 */
	private ArrayList<CbWeather> processJSONResult(String resultJSON,
			CbApiCall apiCall) {
		ArrayList<CbWeather> obsFromJSON = new ArrayList<CbWeather>();
		// System.out.println("processing json result for " +
		// apiCall.getApiName() + " call type " + apiCall.getCallType());
		// System.out.println(resultJSON);
		try {
			JSONArray jsonArray = new JSONArray(resultJSON);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				
					if (apiCall.getCallType().equals("Readings")) {
						CbObservation singleObs = new CbObservation();

						singleObs.setTime(jsonObject.getLong("daterecorded"));
						singleObs.setObservationValue(jsonObject
								.getDouble("reading"));
						Location location = new Location("network");
						location.setLatitude(jsonObject.getDouble("latitude"));
						location.setLongitude(jsonObject.getDouble("longitude"));
						if(jsonObject.has("altitude")) {
							location.setAltitude(jsonObject.getDouble("altitude"));							
						}
						singleObs.setLocation(location);
						obsFromJSON.add(singleObs);

					} else {
						//log("json condition " + jsonObject.toString());
						CbCurrentCondition current = new CbCurrentCondition();
						Location location = new Location("network");
						location.setLatitude(jsonObject.getDouble("latitude"));
						location.setLongitude(jsonObject.getDouble("longitude"));
						current.setLocation(location);
						current.setGeneral_condition(jsonObject
								.getString("general_condition"));
						current.setTime(jsonObject.getLong("daterecorded"));
						current.setTzoffset(jsonObject.getInt("tzoffset"));
						// current.setSharing_policy(jsonObject.getString("sharing"));
						// current.setUser_id(jsonObject.getString("user_id"));
						if(jsonObject.has("cloud_type")) {
							current.setCloud_type(jsonObject.getString("cloud_type"));
						}
						current.setWindy(jsonObject.getString("windy"));
						current.setFog_thickness(jsonObject
								.getString("fog_thickness"));
						current.setPrecipitation_type(jsonObject
								.getString("precipitation_type"));
						current.setPrecipitation_amount(jsonObject
								.getDouble("precipitation_amount"));
						current.setPrecipitation_unit(jsonObject
								.getString("precipitation_unit"));
						current.setThunderstorm_intensity(jsonObject
								.getString("thunderstorm_intensity"));
						current.setUser_comment(jsonObject.getString("user_comment"));
						//log("condition from API: \n" + current);
						obsFromJSON.add(current);
					}
				
			}

		} catch (Exception e) {
			//e.printStackTrace();
		}
		return obsFromJSON;
	}
	
	/**
	 * Log messages either to stdout or to a file
	 * @param message
	 */
	public void log(String message) {
		if(CbConfiguration.DEBUG_MODE) {
			System.out.println(message);
			//logToFile(message);
		}
	}
	
	/**
	 * Log to local storage
	 * @param message
	 */
	public void logToFile(String message) {
		try {
			OutputStream output = new FileOutputStream(mAppDir + "/log.txt",
					true);
			String logString = (new Date()).toString() + ": " + message + "\n";
			output.write(logString.getBytes());
			output.close();
		} catch (FileNotFoundException e) {
			//e.printStackTrace();
		} catch (IOException ioe) {
			//ioe.printStackTrace();
		}
	}

}
