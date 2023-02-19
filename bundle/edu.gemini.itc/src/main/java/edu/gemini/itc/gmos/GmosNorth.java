package edu.gemini.itc.gmos;

import edu.gemini.itc.shared.GmosParameters;
import edu.gemini.itc.shared.ImagingExp;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.gmos.GmosNorthType;

import java.util.logging.Logger;

/**
 * Gmos specification class
 */
public final class GmosNorth extends Gmos {

    private static final Logger Log = Logger.getLogger(GmosNorth.class.getName());

    // value taken from instrument's web documentation
    private static final double WellDepth = 105000;

    // Average full well depth of 12 amplifiers for GMOS-N Hamamatsu CCD
    private static final double HAMAMATSU_WELL_DEPTH = 125000;

    private static final int HAMAMATSU_DETECTOR_PIXELS = 6278;
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

    @Override
    public boolean isIfu2() {
        return getFpMask() == GmosNorthType.FPUnitNorth.IFU_1;
    }

    @Override
    protected Gmos[] createCcdArray() {
        if (odp.calculationMethod() instanceof ImagingExp) {  // Only use the central CCD to speed up calculations
            Log.fine("Returning CCD1 only");                  // This should return CCD2 only.
            return new Gmos[]{this};                          // This line is not correct,
            //return new Gmos[]{new GmosNorth(gp, odp, 1)};   // but this line does not work.
        } else {
            Log.fine("Returning array of all 3 CCDs");
            return new Gmos[]{this, new GmosNorth(gp, odp, 1), new GmosNorth(gp, odp, 2)};
        }
    }

    @Override
    protected String getPrefix() {
        return INSTR_PREFIX;
    }

    @Override
    protected String[] getCcdFiles() {
        return DETECTOR_CCD_FILES;
    }

    @Override
    protected String[] getCcdNames() {
        return DETECTOR_CCD_NAMES;
    }

    @Override
    public double wellDepth() {
        switch (gp.ccdType()) {
            case E2V:
                return WellDepth;
            case HAMAMATSU:
                return HAMAMATSU_WELL_DEPTH;
            default:
                throw new Error("invalid ccd type");
        }
    }

    public int detectorPixels() {
        switch (gp.ccdType()) {
            case E2V:
                return E2V_DETECTOR_PIXELS;
            case HAMAMATSU:
                return HAMAMATSU_DETECTOR_PIXELS;
            default:
                throw new Error("invalid ccd type");
        }
    }

    @Override public double gain() {
        return InstGmosNorth.getMeanGain(gp.ampGain(), gp.ampReadMode(), gp.ccdType());
    }

}
