package edu.gemini.weather;

import java.beans.PropertyChangeListener;

public interface IWeatherBean {

	// Property names for listeners to use.
    public static final String PROP_DIMM = "dimm";

    public static final String PROP_WATER_VAPOR = "waterVapor";

    public static final String PROP_HUMIDITY = "humidity";

	public static final String PROP_TEMPERATURE = "temperature";

	public static final String PROP_WIND_DIRECTION = "windDirection";

	public static final String PROP_WIND_SPEED = "windSpeed";

	public static final String PROP_PRESSURE = "pressure";

	public static final String PROP_CONNECTED = "connected";

	public abstract void addPropertyChangeListener(PropertyChangeListener pcl);

	public abstract void removePropertyChangeListener(PropertyChangeListener pcl);

	public abstract boolean isConnected();

    public abstract Double getDimm();

    public abstract Double getWaterVapor();

	public abstract Double getHumidity();

	public abstract Double getPressure();

	public abstract Double getTemperature();

	public abstract Double getWindDirection();

	public abstract Double getWindSpeed();

}
