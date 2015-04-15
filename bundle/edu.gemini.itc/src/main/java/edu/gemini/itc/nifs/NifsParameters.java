package edu.gemini.itc.nifs;

import edu.gemini.itc.altair.AltairParameters;
import edu.gemini.itc.shared.InstrumentDetails;
import scala.Option;

/**
 * This class holds the information from the Nifs section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class NifsParameters implements InstrumentDetails {

    public static final String SINGLE_IFU = "singleIFU";
    public static final String RADIAL_IFU = "radialIFU";
    public static final String SUMMED_APERTURE_IFU = "summedApertureIFU";


    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.

    public static final String NO_DISPERSER = "none";
    public static final String Z_G5602 = "z_G5602";
    public static final String J_G5603 = "j_G5603";
    public static final String H_G5604 = "h_G5604";
    public static final String K_G5605 = "k_G5605";
    public static final String KS_G5606 = "ks_G5606";
    public static final String KL_G5607 = "kl_G5607";

    //filters
    public static final String ZJ_G0601 = "ZJ_G0601";
    public static final String HJ_G0602 = "HJ_G0602";
    public static final String HK_G0603 = "HK_G0603";

    public static final String LOW_READ_NOISE = "lowNoise";
    public static final String VERY_LOW_READ_NOISE = "verylowNoise";
    public static final String HIGH_READ_NOISE = "highNoise";
    public static final String MED_READ_NOISE = "medNoise";
    public static final String NO_SLIT = "none";
    public static final String NIFS = "nifs";

    // Data members
    private final String filter;
    private final String grating;
    private final String readNoise;
    private final double cenralWavelength;
    private final String ifuMethod;
    private final String ifuOffset;
    private final String ifuMinOffset;
    private final String ifuMaxOffset;
    private final String ifuNumX;
    private final String ifuNumY;
    private final String ifuCenterX;
    private final String ifuCenterY;
    private final Option<AltairParameters> altair;

    /**
     * Constructs a NifsParameters from a test file.
     */
    public NifsParameters(final String filter,
                          final String grating,
                          final String readNoise,
                          final double centralWavelength,
                          final String ifuMethod,
                          final String ifuOffset,
                          final String ifuMinOffset,
                          final String ifuMaxOffset,
                          final String ifuNumX,
                          final String ifuNumY,
                          final String ifuCenterX,
                          final String ifuCenterY,
                          final Option<AltairParameters> altair) {

        this.filter             = filter;
        this.grating            = grating;
        this.readNoise          = readNoise;
        this.ifuMethod          = ifuMethod;
        this.ifuOffset          = ifuOffset;
        this.ifuMinOffset       = ifuMinOffset;
        this.ifuMaxOffset       = ifuMaxOffset;
        this.ifuNumX            = ifuNumX;
        this.ifuNumY            = ifuNumY;
        this.ifuCenterX         = ifuCenterX;
        this.ifuCenterY         = ifuCenterY;
        this.altair             = altair;
        this.cenralWavelength   = centralWavelength * 1000; // convert to [nm]
    }

    public String getFilter() {
        return filter;
    }

    public String getGrating() {
        return grating;
    }

    public String getReadNoise() {
        return readNoise;
    }

    public double getInstrumentCentralWavelength() {
        return cenralWavelength;
    }

    public double getUnXDispCentralWavelength() {
        return cenralWavelength;
    }


    public String getIFUMethod() {
        return ifuMethod;
    }

    public double getIFUOffset() {
        return new Double(ifuOffset);
    }

    public double getIFUMinOffset() {
        return new Double(ifuMinOffset);
    }

    public double getIFUMaxOffset() {
        return new Double(ifuMaxOffset);
    }


    public int getIFUNumX() {
        return new Integer(ifuNumX);
    }

    public int getIFUNumY() {
        return new Integer(ifuNumY);
    }

    public double getIFUCenterX() {
        return new Double(ifuCenterX);
    }

    public double getIFUCenterY() {
        return new Double(ifuCenterY);
    }

    public double getFPMask() {
        return 0.15;
    }

    public Option<AltairParameters> getAltair() {
        return altair;
    }

}
