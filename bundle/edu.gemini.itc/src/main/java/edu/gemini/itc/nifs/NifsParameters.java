package edu.gemini.itc.nifs;

import edu.gemini.itc.shared.AltairParameters;
import edu.gemini.itc.shared.InstrumentDetails;
import edu.gemini.spModel.core.Wavelength;
import edu.gemini.spModel.gemini.nifs.NIFSParams;
import scala.Option;

/**
 * This class holds the information from the Nifs section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class NifsParameters implements InstrumentDetails {

    public static final String SINGLE_IFU = "singleIFU";
    public static final String RADIAL_IFU = "radialIFU";
    public static final String SUMMED_APERTURE_IFU = "summedApertureIFU";

    public static final String LOW_READ_NOISE = "lowNoise";
    public static final String VERY_LOW_READ_NOISE = "verylowNoise";
    public static final String HIGH_READ_NOISE = "highNoise";
    public static final String MED_READ_NOISE = "medNoise";
    public static final String NO_SLIT = "none";
    public static final String NIFS = "nifs";

    // Data members
    private final NIFSParams.Filter filter;
    private final NIFSParams.Disperser grating;
    private final String readNoise;
    private final Wavelength cenralWavelength;
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
    public NifsParameters(final NIFSParams.Filter filter,
                          final NIFSParams.Disperser grating,
                          final String readNoise,
                          final Wavelength centralWavelength,
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
        this.cenralWavelength   = centralWavelength;
    }

    public NIFSParams.Filter getFilter() {
        return filter;
    }

    public NIFSParams.Disperser getGrating() {
        return grating;
    }

    public String getReadNoise() {
        return readNoise;
    }

    public double getInstrumentCentralWavelength() {
        return cenralWavelength.toNanometers();
    }

    public double getUnXDispCentralWavelength() {
        return cenralWavelength.toNanometers();
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
