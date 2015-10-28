package edu.gemini.itc.gmos;

import edu.gemini.itc.base.GainLimit;
import edu.gemini.itc.base.SaturationLimit;
import edu.gemini.itc.base.LimitWarning;
import edu.gemini.itc.shared.GmosParameters;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;

import java.util.ArrayList;
import java.util.List;

/**
 * Gmos specification class
 */
public final class GmosNorth extends Gmos {

    /**
     * Related files will start with this prefix
     */
    public static final String INSTR_PREFIX = "gmos_n_";

    // Instrument reads its configuration from here.
    private static final String FILENAME = "gmos_n" + getSuffix();

    // Detector data files (see REL-478)
    // The GMOS team has decided that the GMOS-N focal plane will be composed of:
    // CCDr -> BB
    // CCDg -> HSC
    // CCDb -> BB
    private static final String[] DETECTOR_CCD_FILES = {"ccd_hamamatsu_bb", "ccd_hamamatsu_hsc", "ccd_hamamatsu_bb"};

    // Detector display names corresponding to the detectorCcdIndex
    private static final String[] DETECTOR_CCD_NAMES = {"BB(B)", "HSC", "BB(R)"};

    public GmosNorth(final GmosParameters gp, final ObservationDetails odp, final int detectorCcdIndex) {
        super(gp, odp, FILENAME, detectorCcdIndex);
    }

    protected Gmos[] createCcdArray() {
        return new Gmos[]{this, new GmosNorth(gp, odp, 1), new GmosNorth(gp, odp, 2)};
    }

    protected String getPrefix() {
        return INSTR_PREFIX;
    }

    protected String[] getCcdFiles() {
        return DETECTOR_CCD_FILES;
    }

    protected String[] getCcdNames() {
        return DETECTOR_CCD_NAMES;
    }

    @Override public List<LimitWarning> warnings() {
        // value taken from instrument's web documentation
        final double WellDepth = 105000;

        return new ArrayList<LimitWarning>() {{
            add(new SaturationLimit(WellDepth * getSpatialBinning() * getSpectralBinning(), 0.95));
            add(new GainLimit(getADSaturation() * InstGmosNorth.getMeanGain(gp.ampGain(), gp.ampReadMode(), gp.ccdType()), 0.95));
        }};
    }

}
