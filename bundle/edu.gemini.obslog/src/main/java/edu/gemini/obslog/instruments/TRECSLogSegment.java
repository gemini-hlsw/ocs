package edu.gemini.obslog.instruments;

import edu.gemini.obslog.obslog.InstrumentLogSegment;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.spModel.gemini.trecs.TReCSParams;

import java.util.List;

//
// Gemini Observatory/AURA
// $Id: TRECSLogSegment.java,v 1.6 2006/06/12 05:06:59 gillies Exp $
//

public class TRECSLogSegment extends InstrumentLogSegment {

    private static final String NARROW_TYPE = "TReCS";
    public static final OlSegmentType SEG_TYPE = new OlSegmentType(NARROW_TYPE);

    private static final String SEGMENT_CAPTION = "TReCS Observing Log";

    private static final String OBSMODE_KEY = "obsMode";
    private static final String SLIT_KEY = "slit";
    private static final String WAVELENGTH_KEY = "wavelength";
    private static final String DISPERSER_KEY = "disperser";


    public TRECSLogSegment(List<OlLogItem> logItems, OlLogOptions obsLogOptions) {
        super(SEG_TYPE, logItems, obsLogOptions);
    }

    // Note this replaces the slit/mask value with the "log" value
    private void _decorateSlit(ConfigMap map) {
        if (map == null) return;

        String value = map.sget(SLIT_KEY);
        if (value == null) return;

        TReCSParams.Mask s = TReCSParams.Mask.getMask(value);
        if (s != null) map.put(SLIT_KEY, s.logValue());
    }

    // Note this replaces the obsMode value with the "log" value
    private void _decorateObsMode(ConfigMap map) {
        if (map == null) return;

        String value = map.sget(OBSMODE_KEY);
        if (value == null) return;

        TReCSParams.ObsMode s = TReCSParams.ObsMode.getObsMode(value);
        if (s != null) map.put(OBSMODE_KEY, s.logValue());
    }

    // Note this replaces the "disperser" value with disperser/wavelength
    private void _decorateDisperser(ConfigMap map) {
        if (map == null) return;

        String value = map.sget(DISPERSER_KEY);
        if (value == null) return;
        TReCSParams.Disperser disperser = TReCSParams.Disperser.getDisperser(value);

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
        _decorateSlit(map);
        _decorateDisperser(map);
        _decorateObsMode(map);
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
