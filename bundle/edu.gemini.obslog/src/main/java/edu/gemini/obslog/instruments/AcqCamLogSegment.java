package edu.gemini.obslog.instruments;

import edu.gemini.obslog.obslog.InstrumentLogSegment;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.config.model.OlLogItem;

import java.util.List;

//
// Gemini Observatory/AURA
// $Id: AcqCamLogSegment.java,v 1.3 2005/12/11 15:54:15 gillies Exp $
//

public class AcqCamLogSegment extends InstrumentLogSegment {

    private static final String NARROW_TYPE = "AcqCam";
    public static final OlSegmentType SEG_TYPE = new OlSegmentType(NARROW_TYPE);

    private static final String SEGMENT_CAPTION = "Acquisition Camera Observing Log";


    public AcqCamLogSegment(List<OlLogItem> logItems, OlLogOptions obsLogOptions) {
        super(SEG_TYPE, logItems, obsLogOptions);
    }

    /**
     * Given an ObservationData object, create its possibly specialized bean data.  This method is a
     * factory for the particular segments type of bean data.
     *
     * @param td <code>TransferData</code>
     */
    public void decorateObservationData(ConfigMap td) {
        // No decorations needed at this time
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
