package edu.gemini.obslog.instruments;

import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.obslog.InstrumentLogSegment;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.GmosNorthType;
import edu.gemini.spModel.gemini.gmos.GmosSouthType;

import java.util.List;
import java.util.logging.Logger;

//
// Gemini Observatory/AURA
// $Id: GMOSLogSegment.java,v 1.10 2006/12/05 14:56:16 gillies Exp $
//

public class GMOSLogSegment extends InstrumentLogSegment {
    private static final Logger LOG = Logger.getLogger(GMOSLogSegment.class.getName());

    private static final String NARROW_TYPE = "GMOS";
    public static final OlSegmentType SEG_TYPE = new OlSegmentType(NARROW_TYPE);

    private static final String SEGMENT_CAPTION = "GMOS Observing Log";

    private static final String FILTER_KEY = "filter";
    private static final String DISPERSER_KEY = "disperser";
    private static final String WAVELENGTH_KEY = "wavelength";
    private static final String ROI_KEY = "roi";
    private static final String FPUMODE_KEY = "fpuMode";
    private static final String MASK_KEY = "mask";
    private static final String FPUCUSTOMMASK_KEY = "fpuCustomMask";

    public GMOSLogSegment(List<OlLogItem> logItems, OlLogOptions obsLogOptions) {
        super(SEG_TYPE, logItems, obsLogOptions);
    }

    private GmosCommonType.Filter _getFilter(boolean north, String filter) {
        return (north) ? GmosNorthType.FilterNorth.getFilterNorth(filter) : GmosSouthType.FilterSouth.getFilterSouth(filter);
    }

    // Note that this decorator replaces the filter value with a log value
    private void _decorateFilter(boolean north, ConfigMap map) {
        if (map == null) return;

        String value = map.sget(FILTER_KEY);
        if (value == null) return;

        GmosCommonType.Filter f = _getFilter(north, value);
        if (f != null) map.put(FILTER_KEY, f.logValue());
    }

    private GmosCommonType.Disperser _getDisperser(boolean north, String disperser) {
        return (north) ? GmosNorthType.DisperserNorth.getDisperser(disperser) : GmosSouthType.DisperserSouth.getDisperser(disperser);
    }

    // Note that this decorator replaces the value with a log value or a combined log value and wavelength
    private void _decorateDisperser(boolean north, ConfigMap map) {
        if (map == null) return;

        String disperserValue = map.sget(DISPERSER_KEY);
        GmosCommonType.Disperser d = _getDisperser(north, disperserValue);
        if (d != null) {
            if (d.logValue().equals("mirror")) {
                map.put(DISPERSER_KEY, d.logValue());
            } else {
                String lambda = map.sget(WAVELENGTH_KEY);
                map.put(DISPERSER_KEY, d.logValue() + '/' + lambda);
            }
        }
    }

    // Note that this decorator replaces the ROI with a shortened log value
    private void _decorateROI(ConfigMap map) {
        if (map == null) return;

        String value = map.sget(ROI_KEY);
        if (value == null) return;

        GmosCommonType.BuiltinROI r = GmosCommonType.BuiltinROI.getBuiltinROI(value);
        if (r != null) map.put(ROI_KEY, r.logValue());
    }

    private void _decorateMask(boolean north, ConfigMap map) {
        if (map == null) return;

        String maskValue = "-";
        String fpuModeValue = map.sget(FPUMODE_KEY);
        GmosCommonType.FPUnitMode mode = GmosCommonType.FPUnitMode.getFPUnitMode(fpuModeValue);
        if (mode == GmosCommonType.FPUnitMode.BUILTIN) {
            String fpuValue = map.sget(MASK_KEY);
            GmosCommonType.FPUnit fpu;
            if (north) {
                fpu = GmosNorthType.FPUnitNorth.getFPUnit(fpuValue);
            } else {
                fpu = GmosSouthType.FPUnitSouth.getFPUnit(fpuValue);
            }
            maskValue = fpu.logValue();
        } else if (mode == GmosCommonType.FPUnitMode.CUSTOM_MASK) {
            maskValue = map.sget(FPUCUSTOMMASK_KEY);
        }
        map.put(MASK_KEY, (maskValue == null) ? "-" : maskValue);
    }

    /**
     * Given an ObservationData object, create its possibly specialized bean data.  This method is a
     * factory for the particular segments type of bean data.
     *
     * @param map UniqueConfigMap
     */
    public void decorateObservationData(ConfigMap map) {
        // Get the options to set the north/south parameters
        OlLogOptions options = getLogOptions();
        boolean isNorth = options.isNorth();
        LOG.fine("isNorth is: " + isNorth);
        _decorateFilter(isNorth, map);
        _decorateDisperser(isNorth, map);
        _decorateROI(map);
        _decorateMask(isNorth, map);
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
