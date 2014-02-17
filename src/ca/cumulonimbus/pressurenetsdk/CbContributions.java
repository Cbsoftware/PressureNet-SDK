package ca.cumulonimbus.pressurenetsdk;

public class CbContributions {
	
	private int pressureLast24h;
	private int pressureLast7d;
	private int pressureAllTime;
	private int conditionsLastWeek;
	private int conditionsAllTime;
	
	public int getPressureLast24h() {
		return pressureLast24h;
	}
	public void setPressureLast24h(int pressureLast24h) {
		this.pressureLast24h = pressureLast24h;
	}
	public int getPressureLast7d() {
		return pressureLast7d;
	}
	public void setPressureLast7d(int pressureLast7d) {
		this.pressureLast7d = pressureLast7d;
	}
	public int getPressureAllTime() {
		return pressureAllTime;
	}
	public void setPressureAllTime(int pressureAllTime) {
		this.pressureAllTime = pressureAllTime;
	}
	public int getConditionsLastWeek() {
		return conditionsLastWeek;
	}
	public void setConditionsLastWeek(int conditionsLastWeek) {
		this.conditionsLastWeek = conditionsLastWeek;
	}
	public int getConditionsAllTime() {
		return conditionsAllTime;
	}
	public void setConditionsAllTime(int conditionsAllTime) {
		this.conditionsAllTime = conditionsAllTime;
	}
	
	
	
}
