//
// $Id$
//

package edu.gemini.obslog.instruments;

import edu.gemini.obslog.obslog.InstrumentLogSegment;
import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.config.model.OlLogItem;

import java.util.List;

/**
 *
 */
public class NICILogSegment extends InstrumentLogSegment {
    private static final String NARROW_TYPE = "NICI";
    public static final OlSegmentType SEG_TYPE = new OlSegmentType(NARROW_TYPE);

    private static final String SEGMENT_CAPTION = "NICI Observing Log";

    // Note these must agree with the contents of ObsLogConfig.xml
    /*
    private static final String FOCAL_PLANE_MASK_KEY = "focalPlaneMask";
    private static final String PUPIL_MASK_KEY       = "pupilMask";
    private static final String RED_FILTER_KEY       = "channel1Fw";
    private static final String BLUE_FILTER_KEY      = "channel2Fw";
    private static final String PUPIL_IMAGER_KEY     = "pupilImager";
    private static final String NDR_KEY              = "nonDestructiveReads";

    private static final String WAVELENGTH_KEY = "wavelength";
    */

    public NICILogSegment(List<OlLogItem> logItems, OlLogOptions obsLogOptions) {
        super(SEG_TYPE, logItems, obsLogOptions);
    }

    public String getSegmentCaption() {
        return SEGMENT_CAPTION;
    }

    public void decorateObservationData(ConfigMap map) {
//        if (map == null) return;

//        String val = map.sget(FOCAL_PLANE_MASK_KEY);
//        if (val != null) {
//
//        }

    }
}
