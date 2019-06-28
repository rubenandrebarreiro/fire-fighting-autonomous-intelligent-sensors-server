package pt.unl.fct.di.firefighting.util;


/**
 * 
 * Class definition for a sensor node.
 * 
 * @author Bernardo
 *
 */
public class SensorData {
	
	public String sensorId;
	public String temperature;
	public String humidity;
	public String latlon;
	
	public SensorData() {
		
	}
	
	public SensorData(String id) {
		sensorId = id;
	}
	
	public SensorData(String id, String latlon) {
		sensorId = id;
		this.latlon = latlon;
	}
	
	public SensorData(String id, String temp, String humid) {
		sensorId = id;
		temperature = temp;
		humidity = humid;
	}
}
