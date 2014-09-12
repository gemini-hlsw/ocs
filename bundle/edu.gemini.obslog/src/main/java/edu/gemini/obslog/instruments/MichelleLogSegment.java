package edu.gemini.obslog.instruments;

import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.obslog.obslog.InstrumentLogSegment;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.spModel.gemini.michelle.MichelleParams;

import java.util.List;

//
// Gemini Observatory/AURA
// $Id: MichelleLogSegment.java,v 1.6 2006/05/03 21:08:02 gillies Exp $
//

public class MichelleLogSegment extends InstrumentLogSegment {

    private static final String NARROW_TYPE = "Michelle";
    public static final OlSegmentType SEG_TYPE = new OlSegmentType(NARROW_TYPE);

    private static final String SEGMENT_CAPTION = "Michelle Observing Log";

    private static final String WAVELENGTH_KEY = "wavelength";
    private static final String DISPERSER_KEY = "disperser";


    public MichelleLogSegment(List<OlLogItem> logItems, OlLogOptions obsLogOptions) {
        super(SEG_TYPE, logItems, obsLogOptions);
    }

    // Note this replaces the "disperser" value with disperser/wavelength
    private void _decorateDisperser(ConfigMap map) {
        if (map == null) return;

        String value = map.sget(DISPERSER_KEY);
        if (value == null) return;
        MichelleParams.Disperser disperser = MichelleParams.Disperser.getDisperser(value);

        String wavelengthValue = map.sget(WAVELENGTH_KEY);

        if (disperser != null && wavelengthValue != null) {
            map.put(DISPERSER_KEY, disperser.logValue() + '/' + wavelengthValue);
        }
    }

    /**
     * Given an ObservationData object, create its possibly specialized bean data.  This method is a
     * factory for the particular segments type of bean data.
     *
     * @param map <code>UniqueConfigMap</code>
     */
    public void decorateObservationData(ConfigMap map) {
        //_decorateFilter(uc);
        //_decorateSlit(uc);
        _decorateDisperser(map);
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
