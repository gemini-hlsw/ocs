package edu.gemini.itc.gmos;

import edu.gemini.itc.parameters.ObservationDetailsParameters;

/**
 * Gmos specification class
 */
public class GmosSouth extends Gmos {

    /**
     * /** Related files will start with this prefix
     */
    public static final String INSTR_PREFIX = "gmos_s_";

    // Instrument reads its configuration from here.
    private static final String FILENAME = "gmos_s" + getSuffix();

    // Detector data files (see REL-478)
    private static final String[] DETECTOR_CCD_FILES = {"ccd_hamamatsu_bb", "ccd_hamamatsu_hsc", "ccd_hamamatsu_sc"};

    // Detector display names corresponding to the detectorCcdIndex
    private static final String[] DETECTOR_CCD_NAMES = {"BB", "HSC", "SC"};


    public GmosSouth(GmosParameters gp, ObservationDetailsParameters odp, int detectorCcdIndex) throws Exception {
        super(gp, odp, FILENAME, detectorCcdIndex);
    }

    protected Gmos[] createCcdArray() throws Exception {
        return new Gmos[]{this, new GmosSouth(gp, odp, 1), new GmosSouth(gp, odp, 2)};
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
