package edu.gemini.obslog.instruments;

import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.obslog.InstrumentLogSegment;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.type.LoggableSpType;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//
// Gemini Observatory/AURA
//

public class Flamingos2LogSegment extends InstrumentLogSegment {
    public static final Logger LOG = Logger.getLogger(Flamingos2LogSegment.class.getName());

    private static final String NARROW_TYPE = "Flamingos2";
    public static final OlSegmentType SEG_TYPE = new OlSegmentType(NARROW_TYPE);

    private static final String SEGMENT_CAPTION = "Flamingos 2 Observing Log";

    private static final String FILTER_KEY       = "filter";
    private static final String DISPERSER_KEY    = "disperser";
    private static final String LYOT_WHEEL_KEY   = "lyotWheel";
    private static final String FPU_KEY          = "fpu";
    private static final String READMODE_KEY     = "readMode";
    private static final String EXPOSURETIME_KEY = "exposureTime";

    public Flamingos2LogSegment(List<OlLogItem> logItems, OlLogOptions obsLogOptions) {
        super(SEG_TYPE, logItems, obsLogOptions);
    }

    private static abstract class SimpleDecorator {
        private String key;

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

    private static final SimpleDecorator DISPERSER_DECORATOR = new SimpleDecorator(DISPERSER_KEY) {
        LoggableSpType lookup(String value) {
            return Flamingos2.Disperser.byName(value).getOrNull(); // REL-1522 (value is display value, not enum name)
        }
    };

    private static final SimpleDecorator FILTER_DECORATOR = new SimpleDecorator(FILTER_KEY) {
        LoggableSpType lookup(String value) {
            return Flamingos2.Filter.byName(value).getOrNull(); // REL-1522
        }
    };

    private static final SimpleDecorator LYOT_WHEEL_DECORATOR = new SimpleDecorator(LYOT_WHEEL_KEY) {
        LoggableSpType lookup(String value) {
            return Flamingos2.LyotWheel.byName(value).getOrNull(); // REL-1522
        }
    };

    private static final SimpleDecorator FPU_DECORATOR = new SimpleDecorator(FPU_KEY) {
        LoggableSpType lookup(String value) {
            return Flamingos2.FPUnit.byName(value).getOrNull(); // REL-1522
        }
    };

    private static final SimpleDecorator READ_MODE_DECORATOR = new SimpleDecorator(READMODE_KEY) {
        LoggableSpType lookup(String value) {
            return Flamingos2.ReadMode.byName(value).getOrNull(); // REL-1522
        }
    };

    private void _decorateExposureTime(ConfigMap map) {
        if (map == null) return;

        // Note exposureTime is the entry that is reused for display
        String exposureTimeValue = map.sget(EXPOSURETIME_KEY);
        if (exposureTimeValue == null) {
            LOG.severe("No exposureTime property in Flamingos2 items");
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
        FILTER_DECORATOR.decorate(map);
        DISPERSER_DECORATOR.decorate(map);
        LYOT_WHEEL_DECORATOR.decorate(map);
        FPU_DECORATOR.decorate(map);
        READ_MODE_DECORATOR.decorate(map);
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
}