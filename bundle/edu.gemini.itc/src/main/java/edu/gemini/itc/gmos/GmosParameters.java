package edu.gemini.itc.gmos;

import edu.gemini.itc.shared.ITCParameters;

/**
 * This class holds the information from the Gmos section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class GmosParameters extends ITCParameters {
    public static final String SINGLE_IFU = "singleIFU";
    public static final String RADIAL_IFU = "radialIFU";

    public static final String R600_G5304 = "R600_G5304";
    public static final String B1200_G5301 = "B1200_G5301";
    public static final String R150_G5306 = "R150_G5306";
    public static final String B600_G5303 = "B600_G5303";
    public static final String R400_G5305 = "R400_G5305";
    public static final String R831_G5302 = "R831_G5302";

    public static final int R600 = 3;
    public static final int B1200 = 0;
    public static final int R150 = 5;
    public static final int B600 = 2;
    public static final int R400 = 4;
    public static final int R831 = 1;

    public static final String NO_DISPERSER = "none";
    public static final String G_G0301 = "g_G0301";
    public static final String R_G0303 = "r_G0303";
    public static final String I_G0302 = "i_G0302";
    public static final String Z_G0304 = "z_G0304";
    public static final String GG455 = "gg455";
    public static final String OG515 = "og515";
    public static final String RG610 = "rg610";
    public static final String SLIT0_25 = "slit0.25";
    public static final String SLIT0_5 = "slit0.5";
    public static final String SLIT0_75 = "slit0.75";
    public static final String SLIT1_0 = "slit1.0";
    public static final String SLIT1_5 = "slit1.5";
    public static final String SLIT2_0 = "slit2.0";
    public static final String SLIT5_0 = "slit5.0";
    public static final String IFU = "ifu";
    public static final String NO_SLIT = "none";
    public static final String GMOS_NORTH = "gmosNorth";
    public static final String GMOS_SOUTH = "gmosSouth";

    // Data members
    private final String filter;
    private final String grating;
    private final double centralWavelength;
    private final String fpMask;
    private final int spatBinning;
    private final int specBinning;
    private final String ifuMethod;
    private final double ifuOffset;
    private final double ifuMinOffset;
    private final double ifuMaxOffset;
    private final String location;
    private final String ccdType;

    /**
     * Constructs a GmosParameters from a test file.
     */
    public GmosParameters(final String filter,
                          final String grating,
                          final double centralWavelength,
                          final String fpMask,
                          final int spatBinning,
                          final int specBinning,
                          final String ifuMethod,
                          final double ifuOffset,
                          final double ifuMinOffset,
                          final double ifuMaxOffset,
                          final String ccdType,
                          final String location) {
        this.filter             = filter;
        this.grating            = grating;
        this.centralWavelength  = centralWavelength;
        this.fpMask             = fpMask;
        this.spatBinning        = spatBinning;
        this.specBinning        = specBinning;
        this.ifuMethod          = ifuMethod;
        this.ifuOffset          = ifuOffset;
        this.ifuMinOffset       = ifuMinOffset;
        this.ifuMaxOffset       = ifuMaxOffset;
        this.ccdType            = ccdType;
        this.location           = location;

    }

    public String getFilter() {
        return filter;
    }

    public String  getGrating() {
        return grating;
    }

    public String getFocalPlaneMask() {
        return fpMask;
    }

    public double getInstrumentCentralWavelength() {
        return centralWavelength;
    }

    public int getSpectralBinning() {
        return specBinning;
    }

    public int getSpatialBinning() {
        return spatBinning;
    }

    public String getCCDtype() {
        return ccdType;
    }

    public double getFPMask() {
        if (fpMask.equals(SLIT0_25))
            return 0.25;
        else if (fpMask.equals(SLIT0_5))
            return 0.5;
        else if (fpMask.equals(SLIT0_75))
            return 0.75;
        else if (fpMask.equals(SLIT1_0))
            return 1.0;
        else if (fpMask.equals(SLIT1_5))
            return 1.5;
        else if (fpMask.equals(SLIT2_0))
            return 2.0;
        else if (fpMask.equals(SLIT5_0))
            return 5.0;
        else if (fpMask.equals(IFU))
            return 0.3;
        else
            return -1.0;
    }

    public String getIFUMethod() {
        return ifuMethod;
    }

    public double getIFUOffset() {
        return ifuOffset;
    }

    public double getIFUMinOffset() {
        return ifuMinOffset;
    }

    public double getIFUMaxOffset() {
        return ifuMaxOffset;
    }

    public String getInstrumentLocation() {
        return location;
    }

    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Filter:\t" + getFilter() + "\n");
        sb.append("Grating:\t" + getGrating() + "\n");
        sb.append("Instrument Central Wavelength:\t" + getInstrumentCentralWavelength() + "\n");
        sb.append("Focal Plane Mask: \t " + getFPMask() + " arcsec slit \n");
        sb.append("\n");
        return sb.toString();
    }
}
