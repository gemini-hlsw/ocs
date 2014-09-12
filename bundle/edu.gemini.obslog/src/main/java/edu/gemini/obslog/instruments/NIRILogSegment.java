package edu.gemini.obslog.instruments;

import edu.gemini.obslog.obslog.InstrumentLogSegment;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.spModel.gemini.niri.Niri;

import java.util.List;

//
// Gemini Observatory/AURA
// $Id: NIRILogSegment.java,v 1.8 2006/05/11 17:55:34 shane Exp $
//

public class NIRILogSegment extends InstrumentLogSegment {

    private static final String NARROW_TYPE = "NIRI";
    public static final OlSegmentType SEG_TYPE = new OlSegmentType(NARROW_TYPE);

    private static final String SEGMENT_CAPTION = "NIRI Observing Log";

    private static final String READMODE_KEY = "readmode";
    private static final String SLIT_KEY = "slit";
    private static final String DISPERSER_KEY = "disperser";
    private static final String FILTER_KEY = "filter";
    private static final String ROI_KEY = "roi";

    public NIRILogSegment(List<OlLogItem> logItems, OlLogOptions obsLogOptions) {
        super(SEG_TYPE, logItems, obsLogOptions);
    }

    // Note this replaces the slit/mask value with the "log" value
    private void _decorateFilter(ConfigMap map) {
        if (map == null) return;

        String value = map.sget(FILTER_KEY);
        if (value == null) return;

        Niri.Filter s = Niri.Filter.getFilter(value);
        if (s != null) map.put(FILTER_KEY, s.logValue());
    }

    // Note this replaces the slit/mask value with the "log" value
    private void _decorateSlit(ConfigMap map) {
        if (map == null) return;

        String value = map.sget(SLIT_KEY);
        if (value == null) return;

        Niri.Mask s = Niri.Mask.getMask(value);
        if (s != null) map.put(SLIT_KEY, s.logValue());
    }

    // Note this replaces the obsMode value with the "log" value
    private void _decorateReadmode(ConfigMap map) {
        if (map == null) return;

        String value = map.sget(READMODE_KEY);
        if (value == null) return;

        Niri.ReadMode s = Niri.ReadMode.getReadMode(value);
        if (s != null) map.put(READMODE_KEY, s.logValue());
    }

    // Note this replaces the "disperser" value with disperser/wavelength
    private void _decorateDisperser(ConfigMap map) {
        if (map == null) return;

        String value = map.sget(DISPERSER_KEY);
        if (value == null) return;
        Niri.Disperser disperser = Niri.Disperser.getDisperser(value);

        map.put(DISPERSER_KEY, disperser.logValue());
    }

    // Note this replaces the roi value with the "log" value
    private void _decorateROI(ConfigMap map) {
        if (map == null) return;

        String value = map.sget(ROI_KEY);
        if (value == null) return;

        Niri.BuiltinROI s = Niri.BuiltinROI.getBuiltinROI(value);
        if (s != null) map.put(ROI_KEY, s.logValue());
    }

    /**
     * Given an ObservationData object, create its possibly specialized bean data.  This method is a
     * factory for the particular segments type of bean data.
     *
     * @param map <code>ConfigMap</code>
     */
    public void decorateObservationData(ConfigMap map) {
        _decorateFilter(map);
        _decorateReadmode(map);
        _decorateSlit(map);
        _decorateDisperser(map);
        _decorateROI(map);
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
