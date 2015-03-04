package edu.gemini.itc.gmos;

import edu.gemini.itc.parameters.ObservationDetailsParameters;

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

    public GmosNorth(final GmosParameters gp, final ObservationDetailsParameters odp, final int detectorCcdIndex) {
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

}
