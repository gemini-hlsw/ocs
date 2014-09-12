package edu.gemini.obslog.instruments;

import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.obslog.ConfigMapUtil;
import edu.gemini.obslog.obslog.OlBasicSegment;
import edu.gemini.obslog.util.SummaryUtils;
import edu.gemini.spModel.gemini.plan.WeatherInfo;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: WeatherSegment.java,v 1.11 2006/12/05 14:56:16 gillies Exp $
//

public class WeatherSegment extends OlBasicSegment {
    private static final Logger LOG = Logger.getLogger(WeatherSegment.class.getName());

    private static final String NARROW_TYPE = "WEATHER";
    public static final OlSegmentType SEG_TYPE = new OlSegmentType(NARROW_TYPE);

    private static final String SEGMENT_CAPTION = "Weather Summary";

    private List<WeatherInfo> _weatherInfo;

    public static WeatherSegment EMPTY_WEATHER_SEGMENT;

    static {
        EMPTY_WEATHER_SEGMENT = new WeatherSegment(Collections.<OlLogItem>emptyList(), null);
    }

    /**
     * An adapter class to help DisplayTag show the weather.  Note that the property names must be the same
     * as the property values in the config file. There's not a way to show which values go with which properties -- at least
     * that I know of right now.
     */
    public static class WeatherAdapter extends ConfigMap {

        public WeatherAdapter(WeatherInfo winfo, int id) {
            put("ID", Integer.toString(id));
            put("time", SummaryUtils.formatUTCDateTime(winfo.getTime()));
            put("temperature", winfo.getTemperature());
            put("windSpeed", winfo.getWindSpeed());
            put("windDirection", winfo.getWindDirection());
            put("barometricPressure", winfo.getBarometricPressure());
            put("relativeHumidity", winfo.getRelativeHumidity());
            put("dimm", winfo.getDimm());
            put("waterVapor", winfo.getWaterVapor());
            put(ConfigMapUtil.OBSLOG_COMMENT_ITEM_NAME, winfo.getComment());
            _decorateComment();
        }

        /**
         * This method checks for a comment and adds a property that is the number of rows in the comment.
         * Used by display code to dynamically set size of text area
         *
         */
        protected void _decorateComment() {
            final int MAX_COMMENT_ROWS = 5;

            String comment = sget(ConfigMapUtil.OBSLOG_COMMENT_ITEM_NAME);
            if (comment == null || comment.length() == 0) return;

            int rows = 0;
            BufferedReader bread = null;
            try {
                bread = new BufferedReader(new StringReader(comment));
                while (bread.readLine() != null) rows++;
                if (rows > MAX_COMMENT_ROWS) rows = MAX_COMMENT_ROWS;
            } catch (Exception ex) {
                LOG.severe("Exception while counting comment lines: " + ex);
                return;
            } finally {
                try {
                    if (bread != null) bread.close();
                } catch (Exception ex) {
                    LOG.severe("Exception while closing BufferedReader");
                }
            }
            put(ConfigMapUtil.OBSLOG_COMMENT_ROWS_ITEM_NAME, String.valueOf(rows));
        }
    }

    public WeatherSegment(List<OlLogItem> logItems, List<WeatherInfo> weatherInfo) {
        super(SEG_TYPE, logItems);
        _weatherInfo = weatherInfo;
    }

    /**
     * Given an ObservationData object, create its possibly specialized bean data.  This method is a
     * factory for the particular segments type of bean data.
     *
     * @param map <code>UniqueConfigMap</code>
     */
    public void decorateObservationData(ConfigMap map) {
    }

    /**
     * Return the segment caption.
     *
     * @return The caption.
     */
    public String getSegmentCaption() {
        return SEGMENT_CAPTION;
    }

    public List<ConfigMap> getRows() {
        if (_weatherInfo == null) {
            _weatherInfo = new ArrayList<WeatherInfo>();
        }
        // Create new objects if list is not empty
        List<ConfigMap> newWeather = new ArrayList<ConfigMap>();
        if (_weatherInfo.size() > 0) {
            for (int i = 0, size = _weatherInfo.size(); i < size; i++) {
                newWeather.add(new WeatherAdapter(_weatherInfo.get(i), i));
            }
        }
        return newWeather;
    }

    /**
     * Return the number of weather entries.
     *
     * @return the number of weather entries
     */
    public int getSize() {
        // Note this is done this way to keep from creating weather adapters.
        if (_weatherInfo == null) return 0;
        return _weatherInfo.size();
    }

    private void _dumpOneWeatherInfo(WeatherInfo info) {
        LOG.info("Local Time: " + SummaryUtils.formatUTCDateTime(info.getTime()));
        LOG.info("Temperature:" + info.getTemperature());
        LOG.info("Wind Speed: " + info.getWindSpeed());
        LOG.info("Wind Direction: " + info.getWindDirection());
        LOG.info("Barometric Pressure: " + info.getBarometricPressure());
        LOG.info("Relative Humidity: " + info.getRelativeHumidity());
        LOG.info("Dimm: " + info.getDimm());
        LOG.info("Water Vapor: " + info.getWaterVapor());
        LOG.info("Comment: " + info.getComment());
    }

    public void dump() {
        for (WeatherInfo info : _weatherInfo) {
            _dumpOneWeatherInfo(info);
        }
    }

}
