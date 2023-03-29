package edu.gemini.obslog.instruments;

import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.obslog.InstrumentLogSegment;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.spModel.type.LoggableSpType;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ObsLog support for Igrins2
 */
public class Igrins2LogSegment extends InstrumentLogSegment {
    public static final Logger LOG = Logger.getLogger(Igrins2LogSegment.class.getName());

    private static final String NARROW_TYPE = "Igrins2";
    public static final OlSegmentType SEG_TYPE = new OlSegmentType(NARROW_TYPE);

    private static final String SEGMENT_CAPTION = "Igrins2 Observing Log";

    private static final String NAME_KEY         = "name";
    private static final String EXPOSURETIME_KEY = "exposureTime";

    public Igrins2LogSegment(List<OlLogItem> logItems, OlLogOptions obsLogOptions) {
        super(SEG_TYPE, logItems, obsLogOptions);
    }

    private static abstract class SimpleDecorator {
        private final String key;

        SimpleDecorator(String key) {
            this.key = key;
        }

        void decorate(ConfigMap map) {
            if (map == null) return;

            String strval = map.sget(key);
            if (strval == null) return;

            LoggableSpType val;
            try {
                val = lookup(strval);
                map.put(key, val.logValue());
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Unknown " + key + " value: " + strval);
                map.put(key, strval);
            }
        }

        abstract LoggableSpType lookup(String value);
    }

    private static final SimpleDecorator NAME_DECORATOR = new SimpleDecorator(NAME_KEY) {
        LoggableSpType lookup(final String value) {
            return new NameLoggableSpType(value);
        }
    };

    private void _decorateExposureTime(ConfigMap map) {
        if (map == null) return;

        final String exposureTimeValue = map.sget(EXPOSURETIME_KEY);
        if (exposureTimeValue == null) {
            LOG.severe("No exposureTime property in Igrins2 Instrument items");
        } else {
            map.put(EXPOSURETIME_KEY, exposureTimeValue);
        }
    }

    /**
     * Given an ObservationData object, create its possibly specialized bean
     * data.  This method is a factory for the particular segments type of bean
     * data.
     *
     * @param map <code>UniqueConfigMap</code>
     */
    public void decorateObservationData(ConfigMap map) {
        NAME_DECORATOR.decorate(map);
        _decorateExposureTime(map);
    }

    /**
     * Return the segment caption.
     *
     * @return The caption.
     */
    public String getSegmentCaption() {
        return SEGMENT_CAPTION;
    }

    private static class NameLoggableSpType implements LoggableSpType {
        private final String value;

        public NameLoggableSpType(String value) {
            this.value = value;
        }

        @Override
        public String logValue() {
            return value;
        }

        @Override
        public String name() {
            return "Name";
        }

        @Override
        public int ordinal() {
            return 0;
        }
    }
}
