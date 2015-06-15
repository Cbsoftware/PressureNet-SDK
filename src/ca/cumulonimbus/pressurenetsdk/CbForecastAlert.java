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
	
	// General Text
	private String timingText = "in 1 hour.";
	
	// Rain
	private String[] rainProbabilityText = {
			"Rain likely to splash", 
			"Droplets expected to sprinkle",
			"You might be singing in the rain",
			"Rain predicted to pour",
			"Rain slated to splash down",
			"Rain anticipated",
			"Rain expected to sprinkle",
			"Showers anticipated overhead"
	};
	private String[] rainHumanText = {
			"Heads up!", 
			"Drip drop!", 
			"Umbrella handy?"
	};

	// Hail
	private String[] hailProbabilityText = {
			"Hail might head your way",
			"Hail estimated",
			"Hard hitting hail estimated",
			"Hail likely to touch down"
	};	
	private String[] hailHumanText = {
			"Take cover!",
			"Look out below!",	
	};
	
	// Snow
	private String[] snowProbabilityText = {
			"Snow might blow your way",
			"Snowfall predicted",
			"Snow expected to glide down",
			"Snowflakes likely to visit you",
			"Snow likely to breeze through",
			"Snowfall suspected",
	};
	private String[] snowHumanText = {
			"Brrr!",
			"Let it snow!"
	};
	
	// Thunderstorm
	private String[] thunderstormProbabilityText = {
			"Thunderstorms likely to occur",
			"Thunderstorms are expected to brew",
			"Get ready for probable thunderstorms",
			"Thunderstorms expected to boom overhead",
			"Thunderstorms expected to rock your region",
			"Thunderstorms predicted to cloud the skies"
	};
	private String[] thunderstormHumanText = {
			"Boom, clap!"
	};
	
	public void composeNotificationText() {

		String finalNotificationText = "";
		String weatherEvent = condition.getGeneral_condition();
		String precipitationType = condition.getPrecipitation_type();
		
		long now = System.currentTimeMillis() / 1000;
		long timeDiff = alertTime - now;
		int minutesFuture = (int) (timeDiff / 60);
		timingText = "in " + minutesFuture + " minutes.";
		
		
		if(weatherEvent.matches("Precipitation")) {
			if(precipitationType.matches("Rain")) {
				String human = (rainHumanText[new Random().nextInt(rainHumanText.length)]);
				String prob = (rainProbabilityText[new Random().nextInt(rainProbabilityText.length)]);
				String timing = timingText;
				
				finalNotificationText = human + " " + prob + " " + timing;
			} else if (precipitationType.matches("Hail")) {
				String human = (hailHumanText[new Random().nextInt(hailHumanText.length)]);
				String prob = (hailProbabilityText[new Random().nextInt(hailProbabilityText.length)]);
				String timing = timingText;
				
				finalNotificationText = human + " " + prob + " " + timing;
			} else if (precipitationType.matches("Snow")) {
				String human = (snowHumanText[new Random().nextInt(snowHumanText.length)]);
				String prob = (snowProbabilityText[new Random().nextInt(snowProbabilityText.length)]);
				String timing = timingText;
				
				finalNotificationText = human + " " + prob + " " + timing;
			}
		} else if (weatherEvent.matches("Thunderstorm")) {
			String human = (thunderstormHumanText[new Random().nextInt(thunderstormHumanText.length)]);
			String prob = (thunderstormProbabilityText[new Random().nextInt(thunderstormProbabilityText.length)]);
			String timing = timingText;
			
			finalNotificationText = human + " " + prob + " " + timing;
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
	
}
