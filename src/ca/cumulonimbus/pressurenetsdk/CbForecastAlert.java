package ca.cumulonimbus.pressurenetsdk;

import java.io.Serializable;
import java.util.Random;


/**
 * Basic weather alert information
 * @author jacob
 *
 */
public class CbForecastAlert implements Serializable {

	private static final long serialVersionUID = -5150523041192341789L;
	private long alertTime;
	private CbCurrentCondition condition;
	private double temperature;
	private String temperatureUnit;
	
	private String tagLine;

	private String notificationTitle;
	private String notificationContent;
	
	// General Text
	private String timingText = "in 1 hour.";

	
	// Rain
	private String[] rainProbabilityText = {
			"Rain likely to splash", 
			"You might be singing in the rain",
			"Rain slated to splash down",
			"Showers anticipated overhead"
			
	};
	
	private String[] lightRainProbabilityText = {
			"Droplets should sprinkle", 
			"Rain expected to sprinkle"
	};
	
	private String[] heavyRainProbabilityText = {
			"Rain predicted to pour", 
			"Rain anticipated to pour"
	};
	
	
	private String[] rainHumanText = {
			"Heads up!", 
			"Drip drop!", 
			"Umbrella handy?"
	};

	// Hail
	private String[] hailProbabilityText = {
			"Hail might head your way",
			"You might get pelted by hail",
			"Hail likely to fall",
			"Hail estimated",
			"Hard hitting hail estimated",
			"Hail likely to touch down"
	};	
	
	
	// Snow
	private String[] snowProbabilityText = {
			"Snow might blow your way",
			"Snowfall predicted",
			"Snow expected to glide down",
			"Snowflakes likely to visit you",
			"Snow could breeze through",
			"Snowfall suspected",
	};
	
	
	// Thunderstorm
	private String[] thunderstormProbabilityText = {
			"Thunderstorms likely",
			"Thunderstorms expected",
			"Probable thunderstorms",
			"Thunderstorms anticipated",
			"Thunderstorms predicted",
	};
	private String[] thunderstormHumanText = {
			"Boom, clap!"
	};
	
	public void composeNotificationText() {

		String finalNotificationText = "";
		String weatherEvent = condition.getGeneral_condition();
		String precipitationType = condition.getPrecipitation_type();
		String preTime = "";
		
		long now = System.currentTimeMillis();
		long timeDiff = alertTime - now;
		int minutesFuture = (int) (timeDiff / (60*1000));
		
		if( minutesFuture < 50) {
			timingText = "in " + minutesFuture + " minutes.";
			preTime = " in " + minutesFuture + " minutes. ";	
		} else if ((minutesFuture >=50) && (minutesFuture < 100) ) {
			timingText = "in about an hour.";
			preTime = " in about two hours.";
		} else {
			timingText = "soon.";
			preTime = " soon.";
		}
		
		
		String tapForForecast = " Tap for forecast.";
		
		
		
		if(weatherEvent.matches("Precipitation")) {
			if(precipitationType.matches("Rain")) {
				String human = "";
				String prob = "";
				String timing = timingText;

				if(condition.getPrecipitation_amount()==0) {
					prob = (lightRainProbabilityText[new Random().nextInt(lightRainProbabilityText.length)]);
				} else if(condition.getPrecipitation_amount()==1) {
					prob = (rainProbabilityText[new Random().nextInt(rainProbabilityText.length)]);
				} else if(condition.getPrecipitation_amount()==2) {
					prob = (heavyRainProbabilityText[new Random().nextInt(heavyRainProbabilityText.length)]);
				} else {
					prob = (rainProbabilityText[new Random().nextInt(rainProbabilityText.length)]);
				}
				
				notificationTitle = prob;
				notificationContent = timingText + tapForForecast;
				
				
				finalNotificationText = prob + " " + timing;
			} else if (precipitationType.matches("Hail")) {
				
				String prob = (hailProbabilityText[new Random().nextInt(hailProbabilityText.length)]);
				String timing = timingText;
				
				notificationTitle = prob;
				notificationContent = timingText + tapForForecast;
				
				finalNotificationText = prob + " " + timing;
			} else if (precipitationType.matches("Snow")) {
				
				String prob = (snowProbabilityText[new Random().nextInt(snowProbabilityText.length)]);
				String timing = timingText;
				
				notificationTitle = prob;
				notificationContent = timingText + tapForForecast;
				
				finalNotificationText = prob + " " + timing;
			}
		} else if (weatherEvent.matches("Thunderstorm")) {
			String human = (thunderstormHumanText[new Random().nextInt(thunderstormHumanText.length)]);
			String prob = (thunderstormProbabilityText[new Random().nextInt(thunderstormProbabilityText.length)]);
			String timing = timingText;
			
			notificationTitle = prob;
			notificationContent = timingText + tapForForecast;
			
			finalNotificationText = prob + " " + timing;
		} else {
			finalNotificationText = "";
		}
		
		
		
		tagLine = finalNotificationText;
	}


	public String getTimingText() {
		return timingText;
	}
	public void setTimingText(String timingText) {
		this.timingText = timingText;
	}
	public long getAlertTime() {
		return alertTime;
	}
	public void setAlertTime(long alertTime) {
		this.alertTime = alertTime;
	}
	public CbCurrentCondition getCondition() {
		return condition;
	}
	public void setCondition(CbCurrentCondition condition) {
		this.condition = condition;
	}
	public double getTemperature() {
		return temperature;
	}
	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}
	public String getTagLine() {
		return tagLine;
	}
	public void setTagLine(String tagLine) {
		this.tagLine = tagLine;
	}
	public String getTemperatureUnit() {
		return temperatureUnit;
	}
	public void setTemperatureUnit(String temperatureUnit) {
		this.temperatureUnit = temperatureUnit;
	}
	public String getNotificationTitle() {
		return notificationTitle;
	}
	public void setNotificationTitle(String notificationTitle) {
		this.notificationTitle = notificationTitle;
	}
	public String getNotificationContent() {
		return notificationContent;
	}
	public void setNotificationContent(String notificationContent) {
		this.notificationContent = notificationContent;
	}
	
}
