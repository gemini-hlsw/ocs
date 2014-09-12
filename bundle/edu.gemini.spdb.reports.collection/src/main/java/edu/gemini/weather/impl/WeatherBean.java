package edu.gemini.weather.impl;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.gemini.epics.IEpicsClient;
import edu.gemini.weather.IWeatherBean;

/**
 * JavaBean for monitoring the weather on Cerro Pachon.
 */
public class WeatherBean implements IEpicsClient, IWeatherBean {

	private static final Logger LOGGER = Logger.getLogger(WeatherBean.class.getName());
	
	// Map the channel names to property names.
	private static final Map<String, String> CHANNEL_PROP = new HashMap<String, String>();
	static {
        CHANNEL_PROP.put("ws:seeFwhm", PROP_DIMM);
        CHANNEL_PROP.put("ws:pwv", PROP_WATER_VAPOR);
        CHANNEL_PROP.put("ws:wsFilter.VALP", PROP_HUMIDITY);
		CHANNEL_PROP.put("ws:wsFilter.VALO", PROP_PRESSURE);
		CHANNEL_PROP.put("ws:wsFilter.VALL", PROP_TEMPERATURE);
		CHANNEL_PROP.put("ws:wsFilter.VALN", PROP_WIND_DIRECTION);
		CHANNEL_PROP.put("ws:wsFilter.VALM", PROP_WIND_SPEED);
	}

	// And provide the list of channels we care about.
	public static final String[] CHANNELS = CHANNEL_PROP.keySet().toArray(new String[0]);
	
	// Members to store current conditions.
    private Double dimm;
    private Double waterVapor;
	private Double humidity;
	private Double temperature;
	private Double windDirection;
	private Double windSpeed;
	private Double pressure;

	private boolean connected;

	// And support for property change listeners.
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	
	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		pcs.addPropertyChangeListener(pcl);
	}

	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		pcs.removePropertyChangeListener(pcl);
	}

	// This is a trivial case because the channels all map one for one and they are all Doubles. 
	public void channelChanged(String channel, Object value) {
		try {
			String prop = CHANNEL_PROP.get(channel);
			Field field = getClass().getDeclaredField(prop);
			Object prev = field.get(this);
			field.set(this, value);
			pcs.firePropertyChange(prop, prev, value);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Trouble setting value.", e);
		}
	}

	public void connected() {
		LOGGER.info("Connected to EPICS.");
		connected = true;
		pcs.firePropertyChange(PROP_CONNECTED, false, true);
	}

	public void disconnected() {
		LOGGER.warning("Disconnected!");
		connected = false;
		pcs.firePropertyChange(PROP_CONNECTED, true, false);
		for (Iterator it = CHANNEL_PROP.keySet().iterator(); it.hasNext(); ) {
			channelChanged((String) it.next(), null);
		}
	}

	public boolean isConnected() {
		return connected;
	}

    public Double getDimm() {
        return dimm;
    }

    public Double getWaterVapor() {
        return waterVapor;
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

}

