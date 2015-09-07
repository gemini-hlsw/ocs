package edu.gemini.obslog.instruments;

import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.obslog.InstrumentLogSegment;
import edu.gemini.obslog.obslog.OlLogOptions;

import java.util.List;

//
// Gemini Observatory/AURA
// $Id: BHROSLogSegment.java,v 1.3 2006/06/12 05:06:59 gillies Exp $
//

public class BHROSLogSegment extends InstrumentLogSegment {

    private static final String NARROW_TYPE = "BHROS";
    public static final OlSegmentType SEG_TYPE = new OlSegmentType(NARROW_TYPE);

    private static final String SEGMENT_CAPTION = "bHROS Observing Log";

    private static final String CCDGAIN_KEY = "gain";
    private static final String CCDSPEED_KEY = "speed";
    private static final String WAVELENGTH_KEY = "wavelength";

    public BHROSLogSegment(List<OlLogItem> logItems, OlLogOptions obsLogOptions) {
        super(SEG_TYPE, logItems, obsLogOptions);
    }

    // Note this replaces the "ccd" values with gain/speed
    private void _decorateCCD(ConfigMap map) {
        if (map == null) return;

        String ccdGain = map.sget(CCDGAIN_KEY);
        String ccdSpeed = map.sget(CCDSPEED_KEY);

        if (ccdGain != null && ccdSpeed != null) {
            map.put(CCDGAIN_KEY, ccdGain + '/' + ccdSpeed);
        }
    }

    private void _fixWavelength(ConfigMap map) {
        if (map == null) return;

        String wavelengthString = map.sget(WAVELENGTH_KEY);
        if (wavelengthString != null) {
            double wavelength = Double.parseDouble(wavelengthString);
            // Check to see if it's been fixed (6/20/2006) if it's greater than 1 it's in nanometers already
            // Poor but should work until it's fixed in bHROS
            if (wavelength > 1.0) return;
            map.put(WAVELENGTH_KEY, String.format("%.3f",wavelength*1000.0));
        }

    }

    /**
     * Given an ObservationData object, create its possibly specialized bean data.  This method is a
     * factory for the particular segments type of bean data.
     *
     * @param map <code>UniqueConfigMap</code>
     */
    public void decorateObservationData(ConfigMap map) {
        _decorateCCD(map);
        _fixWavelength(map);
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
