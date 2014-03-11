package ca.cumulonimbus.pressurenetsdk;

public class CbContributions {
	
	private long pressureLast24h;
	private long pressureLast7d;
	private long pressureAllTime;
	private long conditionsLastWeek;
	private long conditionsAllTime;
	private long conditionsLastDay;
	
	public long getConditionsLastDay() {
		return conditionsLastDay;
	}
	public void setConditionsLastDay(long conditionsLastDay) {
		this.conditionsLastDay = conditionsLastDay;
	}
	public long getPressureLast24h() {
		return pressureLast24h;
	}
	public void setPressureLast24h(long pressureLast24h) {
		this.pressureLast24h = pressureLast24h;
	}
	public long getPressureLast7d() {
		return pressureLast7d;
	}
	public void setPressureLast7d(long pressureLast7d) {
		this.pressureLast7d = pressureLast7d;
	}
	public long getPressureAllTime() {
		return pressureAllTime;
	}
	public void setPressureAllTime(long pressureAllTime) {
		this.pressureAllTime = pressureAllTime;
	}
	public long getConditionsLastWeek() {
		return conditionsLastWeek;
	}
	public void setConditionsLastWeek(long conditionsLastWeek) {
		this.conditionsLastWeek = conditionsLastWeek;
	}
	public long getConditionsAllTime() {
		return conditionsAllTime;
	}
	public void setConditionsAllTime(long conditionsAllTime) {
		this.conditionsAllTime = conditionsAllTime;
	}
}
