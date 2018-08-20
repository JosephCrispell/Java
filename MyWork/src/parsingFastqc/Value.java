package parsingFastqc;

public class Value {

	public String type; // String, int, double, int[]
	public int intValue;
	public double doubleValue;
	public String stringValue;
	public int[] intValues;
	
	public Value(String type) {
		this.type = type;
	}

	// Getting methods
	public String getType() {
		return type;
	}
	public int getIntValue() {
		return intValue;
	}
	public double getDoubleValue() {
		return doubleValue;
	}
	public String getStringValue() {
		return stringValue;
	}
	public int[] getIntValues() {
		return intValues;
	}
	
	// Setting methods
	public void setIntValues(int[] intValues) {
		this.intValues = intValues;
	}
	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}
	public void setDoubleValue(double doubleValue) {
		this.doubleValue = doubleValue;
	}
	public void setIntValue(int intValue) {
		this.intValue = intValue;
	}
	public void setType(String type) {
		this.type = type;
	}	
}
