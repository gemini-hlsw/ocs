package edu.gemini.itc.nifs;

import edu.gemini.itc.shared.InstrumentDetails;

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
    private final String _Filter;
    private final String _grating; // Grating or null
    private final String _readNoise;
    private final String _instrumentCentralWavelength;
    private final String _IFUMethod;
    private final String _IFUOffset;
    private final String _IFUMinOffset;
    private final String _IFUMaxOffset;
    private final String _IFUnumX;
    private final String _IFUnumY;
    private final String _IFUcenterX;
    private final String _IFUcenterY;

    /**
     * Constructs a NifsParameters from a test file.
     */
    public NifsParameters(final String Filter,
                          final String grating,
                          final String readNoise,
                          final String instrumentCentralWavelength,
                          final String IFUMethod,
                          final String IFUOffset,
                          final String IFUMinOffset,
                          final String IFUMaxOffset,
                          final String IFUNumX,
                          final String IFUNumY,
                          final String IFUCenterX,
                          final String IFUCenterY) {

        _Filter = Filter;
        _grating = grating;
        _readNoise = readNoise;
        _instrumentCentralWavelength = instrumentCentralWavelength;
        _IFUMethod = IFUMethod;
        _IFUOffset = IFUOffset;
        _IFUMinOffset = IFUMinOffset;
        _IFUMaxOffset = IFUMaxOffset;
        _IFUnumX = IFUNumX;
        _IFUnumY = IFUNumY;
        _IFUcenterX = IFUCenterX;
        _IFUcenterY = IFUCenterY;
    }

    public String getFilter() {
        return _Filter;
    }

    public String getGrating() {
        return _grating;
    }

    public String getReadNoise() {
        return _readNoise;
    }

    public double getInstrumentCentralWavelength() {
        return new Double(_instrumentCentralWavelength) * 1000;
    }

    public double getUnXDispCentralWavelength() {
        return new Double(_instrumentCentralWavelength) * 1000;
    }


    public String getIFUMethod() {
        return _IFUMethod;
    }

    public double getIFUOffset() {
        return new Double(_IFUOffset);
    }

    public double getIFUMinOffset() {
        return new Double(_IFUMinOffset);
    }

    public double getIFUMaxOffset() {
        return new Double(_IFUMaxOffset);
    }


    public int getIFUNumX() {
        return new Integer(_IFUnumX);
    }

    public int getIFUNumY() {
        return new Integer(_IFUnumY);
    }

    public double getIFUCenterX() {
        return new Double(_IFUcenterX);
    }

    public double getIFUCenterY() {
        return new Double(_IFUcenterY);
    }

    public double getFPMask() {
        return 0.15;
    }

     /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Grating:\t" + getGrating() + "\n");
        sb.append("Instrument Central Wavelength:\t" +
                getInstrumentCentralWavelength() + "\n");
        sb.append("\n");
        return sb.toString();
    }
}
