package edu.gemini.obslog.instruments;

import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.obslog.InstrumentLogSegment;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.spModel.gemini.gpi.Gpi;
import edu.gemini.spModel.type.LoggableSpType;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Obslog support fol Gpi
 */
public class GpiLogSegment extends InstrumentLogSegment {
    public static final Logger LOG = Logger.getLogger(GpiLogSegment.class.getName());

    private static final String NARROW_TYPE = "GPI";
    public static final OlSegmentType SEG_TYPE = new OlSegmentType(NARROW_TYPE);

    private static final String SEGMENT_CAPTION = "Gpi Observing Log";

    private String EMPTY_STRING = "";

    private static final String OBSERVING_MODE_KEY          = "obsMode";
    private static final String DISPERSER_KEY               = "disperser";
    private static final String HALF_WAVE_PLATE_ANGLE_KEY   = "halfWavePlateAngle";
    private static final String ADC_KEY                     = "adc";
    private static final String APODIZER_KEY                = "apodizer";
    private static final String FPM_KEY                     = "fpm";
    private static final String LYOT_KEY                    = "lyot";
    private static final String FILTER_KEY                  = "filter";
    private static final String EXPOSURETIME_KEY            = "exposureTime";

    public GpiLogSegment(List<OlLogItem> logItems, OlLogOptions obsLogOptions) {
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

    private static final SimpleDecorator OBSERVING_MODE_DECORATOR = new SimpleDecorator(OBSERVING_MODE_KEY) {
        LoggableSpType lookup(String value) {
            return Gpi.ObservingMode.byName(value);
        }
    };

    private static final SimpleDecorator DISPERSER_DECORATOR = new SimpleDecorator(DISPERSER_KEY) {
        LoggableSpType lookup(String value) {
            return Gpi.Disperser.valueOf(value.toUpperCase());
        }
    };

    private static final SimpleDecorator MANUAL_HALF_WAVE_PLATE_ANGLE_DECORATOR = new SimpleDecorator(HALF_WAVE_PLATE_ANGLE_KEY) {
        LoggableSpType lookup(final String value) {
            return new LoggableSpType() {
                @Override
                public String logValue() {
                    return value + " deg";
                }

                @Override
                public String name() {
                    return HALF_WAVE_PLATE_ANGLE_KEY;
                }

                @Override
                public int ordinal() {
                    return 0;
                }
            };
        }
    };

    private static final SimpleDecorator ADC_DECORATOR = new SimpleDecorator(ADC_KEY) {
        LoggableSpType lookup(String value) {
            return Gpi.Adc.valueOf(value.toUpperCase());
        }
    };

    private static final SimpleDecorator APODIZER_DECORATOR = new SimpleDecorator(APODIZER_KEY) {
        LoggableSpType lookup(String value) {
            return Gpi.Apodizer.byName(value).getOrNull();
        }
    };

    private static final SimpleDecorator FPM_DECORATOR = new SimpleDecorator(FPM_KEY) {
        LoggableSpType lookup(String value) {
            return Gpi.FPM.byName(value).getOrNull();
        }
    };

    private static final SimpleDecorator LYOT_WHEEL_DECORATOR = new SimpleDecorator(LYOT_KEY) {
        LoggableSpType lookup(String value) {
            return Gpi.Lyot.byName(value).getOrNull();
        }
    };

    private static final SimpleDecorator FILTER_DECORATOR = new SimpleDecorator(FILTER_KEY) {
        LoggableSpType lookup(String value) {
            return Gpi.Filter.valueOf(value);
        }
    };

    private void _decorateExposureTime(ConfigMap map) {
        if (map == null) return;

        // Note exposureTime is the entry that is reused for display
        String exposureTimeValue = map.sget(EXPOSURETIME_KEY);
        if (exposureTimeValue == null) {
            LOG.severe("No exposureTime property in Gpi items");
        } else {
            map.put(EXPOSURETIME_KEY, exposureTimeValue);
        }
    }

    private void _decorateObsMode(ConfigMap map) {
        if (map == null) return;

        // Note obsMode is always available but may by null
        String obsMode = map.sget(OBSERVING_MODE_KEY);
        if (obsMode == null) {
            map.put(OBSERVING_MODE_KEY, Gpi.ObservingMode.byName("").logValue());
        } else {
            OBSERVING_MODE_DECORATOR.decorate(map);
        }
    }

    private void _decorateItem(ConfigMap map, String key, SimpleDecorator decorator) {
        if (map == null) return;

        String item = map.sget(key);
        if (item == null) {
            map.put(key, "");
        } else {
            decorator.decorate(map);
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
        DISPERSER_DECORATOR.decorate(map);
        ADC_DECORATOR.decorate(map);

        _decorateItem(map, FPM_KEY, FPM_DECORATOR);
        _decorateItem(map, APODIZER_KEY, APODIZER_DECORATOR);
        _decorateItem(map, FILTER_KEY, FILTER_DECORATOR);
        _decorateExposureTime(map);
        _decorateObsMode(map);
        MANUAL_HALF_WAVE_PLATE_ANGLE_DECORATOR.decorate(map);
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