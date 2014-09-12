package edu.gemini.spModel.gemini.plan;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.Pio;

import java.io.Serializable;

/**
 * Weather information for the nightly plan.
 */
public class WeatherInfo implements Serializable {
    private static final String TIME = "time";
    private static final String TEMPERATUR = "temperature";
    private static final String WIND_SPEED = "windSpeed";
    private static final String WIND_DIRECTION = "windDirection";
    private static final String BAROMETRIC_PRESURE = "barometricPressure";
    private static final String RELATIVE_HUMIDITY = "relativeHumidity";
    private static final String WATER_VAPOR = "waterVapor";
    private static final String DIMM = "dimm";
    private static final String COMMENT = "comment";

    private static final String DEFAULT_VALUE="";

    private long _time = 0;
    private String _temperature = DEFAULT_VALUE;
    private String _windSpeed = DEFAULT_VALUE;
    private String _windDirection = DEFAULT_VALUE;
    private String _barometricPressure = DEFAULT_VALUE;
    private String _relativeHumidity = DEFAULT_VALUE;
    private String _waterVapor = DEFAULT_VALUE;
    private String _dimm = DEFAULT_VALUE;
    private String _comment = DEFAULT_VALUE;

    public WeatherInfo() {
    }

    public WeatherInfo(long time, String temperature, String windSpeed,
                       String windDirection, String barometricPressure,
                       String relativeHumidity, String dimm, String waterVapor, String comment) {
        _time = time;
        _temperature = temperature;
        _windSpeed = windSpeed;
        _windDirection = windDirection;
        _barometricPressure = barometricPressure;
        _relativeHumidity = relativeHumidity;
        _dimm = dimm;
        _waterVapor = waterVapor;
        _comment = comment;
    }

    public long getTime() {
        return _time;
    }

    public void setTime(long time) {
        _time = time;
    }

    public String getTemperature() {
        return _temperature;
    }

    public void setTemperature(String temperature) {
        _temperature = temperature;
    }

    public String getWindSpeed() {
        return _windSpeed;
    }

    public void setWindSpeed(String windSpeed){
        _windSpeed = windSpeed;
    }

    public String getWindDirection() {
        return _windDirection;
    }

    public void setWindDirection(String windDirection) {
        _windDirection = windDirection;
    }

    public String getBarometricPressure() {
        return _barometricPressure;
    }

    public void setBarometricPressure(String barometricPressure) {
        _barometricPressure = barometricPressure;
    }

    public String getRelativeHumidity() {
        return _relativeHumidity;
    }

    public void setRelativeHumidity(String relativeHumidity) {
        _relativeHumidity = relativeHumidity;
    }

    public String getWaterVapor() {
        return _waterVapor;
    }

    public void setWaterVapor(String waterVapor) {
        _waterVapor = waterVapor;
    }

    public String getDimm() {
        return _dimm;
    }

    public void setDimm(String dimm) {
        _dimm = dimm;
    }

    public String getComment() {
        return _comment;
    }

    public void setComment(String comment) {
        _comment = comment;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WeatherInfo)) return false;

        final WeatherInfo weatherInfo = (WeatherInfo) o;

        if (_time != weatherInfo._time) return false;

        if (_barometricPressure != null ? !_barometricPressure.equals(weatherInfo._barometricPressure) : weatherInfo._barometricPressure != null) return false;
        if (_comment != null ? !_comment.equals(weatherInfo._comment) : weatherInfo._comment != null) return false;
        if (_dimm != null ? !_dimm.equals(weatherInfo._dimm) : weatherInfo._dimm != null) return false;
        if (_relativeHumidity != null ? !_relativeHumidity.equals(weatherInfo._relativeHumidity) : weatherInfo._relativeHumidity != null) return false;
        if (_temperature != null ? !_temperature.equals(weatherInfo._temperature) : weatherInfo._temperature != null) return false;
        if (_windDirection != null ? !_windDirection.equals(weatherInfo._windDirection) : weatherInfo._windDirection != null) return false;
        if (_waterVapor != null ? !_waterVapor.equals(weatherInfo._waterVapor) : weatherInfo._waterVapor != null) return false;
        return !(_windSpeed != null ? !_windSpeed.equals(weatherInfo._windSpeed) : weatherInfo._windSpeed != null);
    }

    public int hashCode() {
        int result;
        result = (int) (_time ^ (_time >>> 32));
        result = 29 * result + (_temperature != null ? _temperature.hashCode() : 0);
        result = 29 * result + (_windSpeed != null ? _windSpeed.hashCode() : 0);
        result = 29 * result + (_windDirection != null ? _windDirection.hashCode() : 0);
        result = 29 * result + (_barometricPressure != null ? _barometricPressure.hashCode() : 0);
        result = 29 * result + (_relativeHumidity != null ? _relativeHumidity.hashCode() : 0);
        result = 29 * result + (_waterVapor != null ? _waterVapor.hashCode() : 0);
        result = 29 * result + (_dimm != null ? _dimm.hashCode() : 0);
        result = 29 * result + (_comment != null ? _comment.hashCode() : 0);
        return result;
    }

    /**
     * Return a paramset describing this object
     */
    public ParamSet getParamSet(PioFactory factory, String name) {
        ParamSet paramSet = factory.createParamSet(name);
        Pio.addParam(factory, paramSet, TIME, String.valueOf(_time));
        if (_temperature != null) {
            Pio.addParam(factory, paramSet, TEMPERATUR, _temperature);
        }
        if (_windSpeed != null) {
            Pio.addParam(factory, paramSet, WIND_SPEED, _windSpeed);
        }
        if (_windDirection != null) {
            Pio.addParam(factory, paramSet, WIND_DIRECTION, _windDirection);
        }
        if (_barometricPressure != null) {
            Pio.addParam(factory, paramSet, BAROMETRIC_PRESURE, _barometricPressure);
        }
        if (_relativeHumidity != null) {
            Pio.addParam(factory, paramSet, RELATIVE_HUMIDITY, _relativeHumidity);
        }
        if (_waterVapor != null) {
            Pio.addParam(factory, paramSet, WATER_VAPOR, _waterVapor);
        }
        if (_dimm != null) {
            Pio.addParam(factory, paramSet, DIMM, _dimm);
        }
        if (_comment != null) {
            Pio.addParam(factory, paramSet, COMMENT, _comment);
        }
        return paramSet;
    }

    // Initialize this object from the given paramset
    public void setParamSet(ParamSet paramSet) {
        if (paramSet == null) {
            return;
        }

        String timeStr = Pio.getValue(paramSet, TIME);
        if ((timeStr != null) && !"".equals(timeStr)) {
            try {
                _time = Long.parseLong(timeStr);
            } catch (NumberFormatException ex) {
            }
        }

        _temperature = Pio.getValue(paramSet, TEMPERATUR);
        _windSpeed = Pio.getValue(paramSet, WIND_SPEED);
        _windDirection = Pio.getValue(paramSet, WIND_DIRECTION);
        _barometricPressure = Pio.getValue(paramSet, BAROMETRIC_PRESURE);
        _relativeHumidity = Pio.getValue(paramSet, RELATIVE_HUMIDITY);
        _waterVapor = Pio.getValue(paramSet, WATER_VAPOR);
        _dimm = Pio.getValue(paramSet, DIMM);
        _comment = Pio.getValue(paramSet, COMMENT);
    }
}
