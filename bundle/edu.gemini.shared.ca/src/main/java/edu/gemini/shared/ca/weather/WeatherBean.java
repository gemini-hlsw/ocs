package edu.gemini.shared.ca.weather;

import edu.gemini.shared.ca.ChannelBindingSupport;
import gov.aps.jca.CAException;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.security.AccessControlException;

/**
 * JavaBean for monitoring the weather on Cerro Pachon.
 */
public class WeatherBean {

	public static final String PROP_HUMIDITY = "humidity";
	public static final String PROP_TEMPERATURE = "temperature";
	public static final String PROP_WIND_DIRECTION = "windDirection";
	public static final String PROP_WIND_SPEED = "windSpeed";
	public static final String PROP_PRESSURE = "pressure";

	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	
	private Double humidity;
	private Double temperature;
	private Double windDirection;
	private Double windSpeed;
	private Double pressure;
	
	/*
	 * Note that CAException is the only exception that clients can be expected
	 * to handle (they would only be caused by coding problems), so all the others 
	 * are re-thrown unchecked. 
	 */
	public WeatherBean() throws CAException {
		try {
			ChannelBindingSupport cbs = new ChannelBindingSupport(this, pcs);
			cbs.bindChannel(PROP_HUMIDITY, "ws:wsFilter.VALP");
			cbs.bindChannel(PROP_PRESSURE, "ws:wsFilter.VALO");
			cbs.bindChannel(PROP_TEMPERATURE, "ws:wsFilter.VALL");
			cbs.bindChannel(PROP_WIND_DIRECTION, "ws:wsFilter.VALN");
			cbs.bindChannel(PROP_WIND_SPEED, "ws:wsFilter.VALM");
		} catch (NoSuchFieldException e) {
			throw new Error("Coding Error", e);
		} catch (IllegalStateException iae) {
			throw new Error("Logic Error", iae);
		} catch (SecurityException e) {
			throw new AccessControlException(e.toString());
		}
	}
	
	public Double getHumidity() {
		return humidity;
	}
	
	public Double getPressure() {
		return pressure;
	}
	
	public Double getTemperature() {
		return temperature;
	}
	
	public Double getWindDirection() {
		return windDirection;
	}
	
	public Double getWindSpeed() {
		return windSpeed;
	}

	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		pcs.addPropertyChangeListener(pcl);
	}

	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		pcs.removePropertyChangeListener(pcl);
	}

}

