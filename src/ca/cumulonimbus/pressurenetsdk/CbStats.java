package ca.cumulonimbus.pressurenetsdk;

public class CbStats {

	/**
	 * {"users": 23, 
	 * "min": 984.65997314499998, 
	 * "timestamp": 1389762000000, 
	 * "median": 992.58996581999997, 
	 * "geohash": "10minute-dpz83", 
	 * "samples": 24, 
	 * "max": 996.97204589800003, 
	 * "std_dev": 3.0161819909599998, 
	 * "mean": 992.45627848300001}
	 */

	private double min;
	private long timeStamp;
	private double median;
	private String geohash;
	private int samples;
	private double max;
	private double stdDev;
	private double mean;
	
	public CbStats(double min, long timeStamp, double median, String geohash, int samples, double max, double stdDev, double mean) {
		this.min = min;
		this.timeStamp = timeStamp;
		this.median = median;
		this.geohash = geohash;
		this.samples = samples;
		this.max = max;
		this.stdDev = stdDev;
		this.mean = mean;
	}
	
	public double getMin() {
		return min;
	}
	public void setMin(double min) {
		this.min = min;
	}
	public long getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	public double getMedian() {
		return median;
	}
	public void setMedian(double median) {
		this.median = median;
	}
	public String getGeohash() {
		return geohash;
	}
	public void setGeohash(String geohash) {
		this.geohash = geohash;
	}
	public int getSamples() {
		return samples;
	}
	public void setSamples(int samples) {
		this.samples = samples;
	}
	public double getMax() {
		return max;
	}
	public void setMax(double max) {
		this.max = max;
	}
	public double getStdDev() {
		return stdDev;
	}
	public void setStdDev(double stdDev) {
		this.stdDev = stdDev;
	}
	public double getMean() {
		return mean;
	}
	public void setMean(double mean) {
		this.mean = mean;
	}
	
	
	
}
