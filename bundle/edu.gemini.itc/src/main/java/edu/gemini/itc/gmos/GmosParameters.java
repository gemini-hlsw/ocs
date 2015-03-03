package edu.gemini.itc.gmos;

import edu.gemini.itc.shared.ITCParameters;
import edu.gemini.itc.shared.IfuMethod;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import scala.Option;

/**
 * This class holds the information from the Gmos section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class GmosParameters extends ITCParameters {

    public static final String GMOS_NORTH = "gmosNorth";
    public static final String GMOS_SOUTH = "gmosSouth";

    // Data members
    private final GmosCommonType.Filter filter;
    private final GmosCommonType.Disperser grating;
    private final double centralWavelength;
    private final GmosCommonType.FPUnit fpMask;
    private final int spatBinning;
    private final int specBinning;
    private final Option<IfuMethod> ifuMethod;
    private final String location;
    private final String ccdType;

    /**
     * Constructs a GmosParameters from a test file.
     */
    public GmosParameters(final GmosCommonType.Filter filter,
                          final GmosCommonType.Disperser grating,
                          final double centralWavelength,
                          final GmosCommonType.FPUnit fpMask,
                          final int spatBinning,
                          final int specBinning,
                          final Option<IfuMethod> ifuMethod,
                          final String ccdType,
                          final String location) {
        this.filter             = filter;
        this.grating            = grating;
        this.centralWavelength  = centralWavelength;
        this.fpMask             = fpMask;
        this.spatBinning        = spatBinning;
        this.specBinning        = specBinning;
        this.ifuMethod          = ifuMethod;
        this.ccdType            = ccdType;
        this.location           = location;

    }

    public GmosCommonType.Filter getFilter() {
        return filter;
    }

    public GmosCommonType.Disperser getGrating() {
        return grating;
    }

    public GmosCommonType.FPUnit getFocalPlaneMask() {
        return fpMask;
    }

    public double getCentralWavelength() {
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
        if (fpMask.isIFU()) {
            return 0.3;
        } else {
            return fpMask.getWidth();
        }
    }

    public Option<IfuMethod> getIFUMethod() {
        return ifuMethod;
    }

    public String getInstrumentLocation() {
        return location;
    }

//    /**
//     * Return a human-readable string for debugging
//     */
//    public String toString() {
//        StringBuffer sb = new StringBuffer();
//        sb.append("Filter:\t" + getFilter() + "\n");
//        sb.append("Grating:\t" + getGrating().name() + "\n");
//        sb.append("Instrument Central Wavelength:\t" + getCentralWavelength() + "\n");
//        sb.append("Focal Plane Mask: \t " + getFPMask() + " arcsec slit \n");
//        sb.append("\n");
//        return sb.toString();
//    }
}
