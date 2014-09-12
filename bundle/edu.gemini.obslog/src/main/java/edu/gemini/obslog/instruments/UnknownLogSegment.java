package edu.gemini.obslog.instruments;

import edu.gemini.obslog.obslog.InstrumentLogSegment;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.config.model.OlLogItem;

import java.util.List;

//
// Gemini Observatory/AURA
// $Id: UnknownLogSegment.java,v 1.1 2005/12/11 15:54:15 gillies Exp $
//

/**
 * A log segment for an instrument that is not known to the obslog software.
 */
public class UnknownLogSegment extends InstrumentLogSegment {

    private static final String NARROW_TYPE = "UNKNOWN";
    public static final OlSegmentType SEG_TYPE = new OlSegmentType(NARROW_TYPE);

    private static final String SEGMENT_CAPTION = "Unknown Instrument Observing Log";


    public UnknownLogSegment(List<OlLogItem> logItems, OlLogOptions obsLogOptions) {
        super(SEG_TYPE, logItems, obsLogOptions);
    }

    /**
     * Given an ObservationData object, create its possibly specialized bean data.  This method is a
     * factory for the particular segments type of bean data.
     *
     * @param map <code>UniqueConfigMap</code>
     */
    public void decorateObservationData(ConfigMap map) {
        //_decorateSteps(map.getObservationID().stringValue());
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
