package edu.gemini.obslog.instruments;

import edu.gemini.obslog.obslog.InstrumentLogSegment;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.spModel.gemini.nifs.NIFSParams;
import edu.gemini.spModel.gemini.nifs.InstEngNifs;

import java.util.List;

//
// Gemini Observatory/AURA
// $Id: NIFSLogSegment.java,v 1.4 2006/05/25 16:17:10 anunez Exp $
//

public class NIFSLogSegment extends InstrumentLogSegment {

    private static final String NARROW_TYPE = "NIFS";
    public static final OlSegmentType SEG_TYPE = new OlSegmentType(NARROW_TYPE);

    private static final String SEGMENT_CAPTION = "NIFS Observing Log";
    // Note these must agree with the contents of ObsLogConfig.xml
    private static final String DISPERSER_KEY = "disperser";
    private static final String WAVELENGTH_KEY = "wavelength";
    private static final String IMAGINGMIRROR_KEY = "imagingMirror";
    private static final String ENGREADMODE_KEY = "engreadmode";
    private static final String ENGNUMSAMPLES_KEY = "numsamples";
    private static final String ENGPERIOD_KEY = "period";
    private static final String ENGNUMPERIODS_KEY = "numperiods";
    private static final String ENGNUMRESETS_KEY = "numresets";
    private static final String READMODE_KEY  = "readmode";

    public NIFSLogSegment(List<OlLogItem> logItems, OlLogOptions obsLogOptions) {
        super(SEG_TYPE, logItems, obsLogOptions);
    }

    /**
     * Given an ObservationData object, create its possibly specialized bean data.  This method is a
     * factory for the particular segments type of bean data.
     *
     * @param map <code>UniqueConfigMap</code>
     */
    public void decorateObservationData(ConfigMap map) {
        _decorateDisperser(map);
        _decorateImagingMirror(map);
        _decorateReadMode(map);
    }

    // Note that this decorator replaces the value a combined value and wavelength if it's not mirror
    private void _decorateDisperser(ConfigMap map) {
        if (map == null) return;

        String disperserValue = map.sget(DISPERSER_KEY);
        if (disperserValue == null) return;

        NIFSParams.Disperser d = NIFSParams.Disperser.getDisperser(disperserValue);
        if (d != null) {
            if (d != NIFSParams.Disperser.MIRROR) {
                String lambda = map.sget(WAVELENGTH_KEY);
                map.put(DISPERSER_KEY, disperserValue + '/' + lambda);
            }
        }
    }

    // Note this changes the imageing mirror value from "in" to "Y"
    private void _decorateImagingMirror(ConfigMap map) {
        if (map == null) return;

        String value = map.sget(IMAGINGMIRROR_KEY);
        map.put(IMAGINGMIRROR_KEY, (value != null && value.equals("In")) ? "Y" : "");
    }

    // Check for engineering detector params
    private void _decorateReadMode(ConfigMap map) {
        if (map == null) return;

        String engReadMode = map.sget(ENGREADMODE_KEY);
        if (engReadMode == null) return;

        // Okay this is an engineering readmode, assemble the read mode from eng pieces
        StringBuffer sb = new StringBuffer();
        NIFSParams.EngReadMode emode = NIFSParams.EngReadMode.getReadMode(engReadMode);
        if (NIFSParams.EngReadMode.FOWLER_SAMPLING_READOUT == emode) {
            sb.append("Fowler");
        } else if (NIFSParams.EngReadMode.LINEAR_READ == emode) {
            sb.append("Linear");
        }
        String value = map.sget(ENGNUMSAMPLES_KEY);
        sb.append('/');
        sb.append(value != null ? value : String.valueOf(InstEngNifs.DEF_NUMBER_OF_SAMPLES));

        value = map.sget(ENGPERIOD_KEY);
        sb.append('/');
        sb.append(value != null ? value : String.valueOf(InstEngNifs.DEF_PERIOD));

        value = map.sget(ENGNUMPERIODS_KEY);
        sb.append('/');
        sb.append(value != null ? value : String.valueOf(InstEngNifs.DEF_NUMBER_OF_PERIODS));

        value = map.sget(ENGNUMRESETS_KEY);
        sb.append('/');
        sb.append(value != null ? value : String.valueOf(InstEngNifs.DEF_NUMBER_OF_RESETS));

        map.put(READMODE_KEY, sb.toString());
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
