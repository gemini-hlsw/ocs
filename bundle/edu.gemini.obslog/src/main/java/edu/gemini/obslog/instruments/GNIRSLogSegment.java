package edu.gemini.obslog.instruments;

import edu.gemini.obslog.config.model.OlLogItem;
import edu.gemini.obslog.core.OlSegmentType;
import edu.gemini.obslog.obslog.ConfigMap;
import edu.gemini.obslog.obslog.InstrumentLogSegment;
import edu.gemini.obslog.obslog.OlLogOptions;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams;

import java.util.List;
import java.util.logging.Logger;


public class GNIRSLogSegment extends InstrumentLogSegment {
    public static final Logger LOG = Logger.getLogger(GNIRSLogSegment.class.getName());

    private static final String NARROW_TYPE = "GNIRS";
    public static final OlSegmentType SEG_TYPE = new OlSegmentType(NARROW_TYPE);

    private static final String SEGMENT_CAPTION = "GNIRS Observing Log";

    private static final String WAVELENGTH_KEY = "wavelength";
    private static final String TYPE_KEY = "type";
    private static final String FILTER_KEY = "filter";
    private static final String CROSSDISPERSED_KEY = "crossDispersed";
    private static final String DISPERSER_KEY = "disperser";
    private static final String SLITWIDTH_KEY = "slitwidth";
    private static final String ACQMIRROR_KEY = "acqMirror";
    private static final String READMODE_KEY = "readMode";
    private static final String COADDS_KEY = "coadds";
    private static final String EXPOSURETIME_KEY = "exposureTime";
    private static final String PIXELSCALE_KEY = "pixelScale";
    private static final String CAMERA_KEY = "camera";

    public GNIRSLogSegment(final List<OlLogItem> logItems, final OlLogOptions obsLogOptions) {
        super(SEG_TYPE, logItems, obsLogOptions);
    }

    private void _decorateFilter(final ConfigMap map) {
        if (map == null) return;

        // Note that filterValue can return null here if there is no filter in the sequence.  Then this code needs
        // to divine it from other values based upon SeqExec details
        String filterValue = map.sget(FILTER_KEY);

        // This should work if filterValue == null - the check is added to keep
        // oldValueOf from complaining
        if (filterValue != null) {
            final GNIRSParams.Filter f = GNIRSParams.Filter.getFilter(filterValue, null);
            if (f != null) {
                final String typeValue = ImOption.apply(map.sget(TYPE_KEY)).getOrElse("");
                map.put(FILTER_KEY, !typeValue.equals("DARK") ? f.logValue() : "DARK");
                return;
            }
        }

        // If we make it here, we are divining the filter.
        final String crossValue = map.sget(CROSSDISPERSED_KEY);
        if (crossValue.equals("No")) {
            String lambdaValue = map.sget(WAVELENGTH_KEY);
            if (lambdaValue == null) return;
            try {
                double lambda = Double.parseDouble(lambdaValue);
                if (lambda < 1.17) {
                    filterValue = "X";
                } else if (lambda < 1.42) {
                    filterValue = "J";
                } else if (lambda < 1.86) {
                    filterValue = "H";
                } else if (lambda < 2.70) {
                    filterValue = "K";
                } else if (lambda < 4.30) {
                    filterValue = "L";
                } else {
                    filterValue = "M";
                }
            } catch (NumberFormatException ex) {
                filterValue = "?";
            }
        } else {
            filterValue = "XD";
        }

        // Finally, set the filter value for the not DARK and not in sequence section
        map.put(FILTER_KEY, filterValue);
    }

    // Note this replaces the "disperser" value with disperser/wavelength
    private void _decorateDisperser(final ConfigMap map) {
        if (map == null) return;

        final String value = map.sget(DISPERSER_KEY);
        if (value == null) return;
        final GNIRSParams.Disperser disperser = GNIRSParams.Disperser.getDisperser(value);

        final String wavelengthValue = map.sget(WAVELENGTH_KEY);
        if (disperser != null && wavelengthValue != null) {
            map.put(DISPERSER_KEY, disperser.logValue() + '/' + wavelengthValue);
        }
    }

    // Note this replaces the slitwidth value with the "log" value
    // Note at this time this doesn't bother with the decker the logs don't seem to either
    private void _decorateSlit(final ConfigMap map) {
        if (map == null) return;

        final String value = map.sget(SLITWIDTH_KEY);
        if (value == null) return;

        final GNIRSParams.SlitWidth s = GNIRSParams.SlitWidth.getSlitWidth(value);
        if (s != null) map.put(SLITWIDTH_KEY, s.logValue());
    }

    // Note this changes the acqmirror value from "in" to "Y"
    private void _decorateAcquisition(final ConfigMap map) {
        if (map == null) return;

        final String value = map.sget(ACQMIRROR_KEY);
        map.put(ACQMIRROR_KEY, (value != null && value.equals("in")) ? "Y" : "");
    }

    // This method looks at the coadds, readmode, and exposureTime to synthesize the new complicated exposure time
    private void _decorateExposureTime(final ConfigMap map) {
        if (map == null) return;

        final String readmodeValue = map.sget(READMODE_KEY);
        if (readmodeValue == null) {
            LOG.severe("No readMode found in GNIRS items");
        }

        final GNIRSParams.ReadMode rm = GNIRSParams.ReadMode.getReadMode(readmodeValue);
        final int lnrs = rm.getLowNoiseReads();

        final String coaddsValue = ImOption.apply(map.sget(COADDS_KEY)).getOrElse(() -> {
            LOG.severe("No coaddsValue found in GNIRS items");
            return "1";
        });

        // Note exposureTime is the entry that is reused for display
        final String exposureTimeValue = ImOption.apply(map.sget(EXPOSURETIME_KEY)).getOrElse(() -> {
            LOG.severe("No exposureTime property in GNIRS items");
            return "1";
        });

        map.put(EXPOSURETIME_KEY, exposureTimeValue + '/' + lnrs + '/' + coaddsValue);
    }

    // Note this replaces the camera item with the camera/prism value
    private void _decorateCamera(final ConfigMap map) {
        if (map == null) return;

        final String value = map.sget(WAVELENGTH_KEY);
        if (value == null) return;


        final String pscaleValue = map.sget(PIXELSCALE_KEY);
        final GNIRSParams.PixelScale ps = GNIRSParams.PixelScale.getPixelScale(pscaleValue);

        String camera;
        try {
            double lambda = Double.parseDouble(value);
            if (ps == GNIRSParams.PixelScale.PS_015) {
                    camera = (lambda > 2.5) ? "SR" : "SB";
            } else {
                    // pixel scale is PS_005
                    camera = (lambda > 2.5) ? "LR" : "LB";
            }
        } catch (NumberFormatException ex) {
            camera = "?";
        }

        // REL-1101: crossValue is either No, SXD, or LXD. Translate No into MIR.
        final String crossValue = map.sget(CROSSDISPERSED_KEY);
        final String prism = crossValue.equals("No") ? "MIR" : crossValue;
        map.put(CAMERA_KEY, camera + '/' + prism);
    }

    /**
     * Given an ObservationData object, create its possibly specialized bean data.  This method is a
     * factory for the particular segments type of bean data.
     *
     * @param map <code>UniqueConfigMap</code>
     */
    public void decorateObservationData(final ConfigMap map) {
        _decorateFilter(map);
        _decorateSlit(map);
        _decorateDisperser(map);
        _decorateExposureTime(map);
        _decorateAcquisition(map);
        _decorateCamera(map);
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
