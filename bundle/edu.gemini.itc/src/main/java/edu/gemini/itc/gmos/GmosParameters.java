package edu.gemini.itc.gmos;

import edu.gemini.itc.shared.ITCParameters;
import edu.gemini.itc.service.IfuMethod;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.gmos.GmosCommonType.*;
import scala.Option;

/**
 * This class holds the information for GMOS ITC requests.
 */
public final class GmosParameters extends ITCParameters {

    private final Site site;
    private final DetectorManufacturer ccdType;
    private final Filter filter;
    private final Disperser grating;
    private final FPUnit fpMask;
    private final Option<IfuMethod> ifuMethod;      // only if fpu is IFU
    private final double centralWavelength;
    private final int spatBinning;
    private final int specBinning;

    // TODO: allow definition of IFU method only if fpu=ifu (?)
    // TODO: allow GS parameters only for site=GS and same for GN, two parameter types: GmosNorthParameters, GmosSouthParameters?

    /**
     * Constructs a GmosParameters from a test file.
     */
    public GmosParameters(final Filter filter,
                          final Disperser grating,
                          final double centralWavelength,
                          final FPUnit fpMask,
                          final int spatBinning,
                          final int specBinning,
                          final Option<IfuMethod> ifuMethod,
                          final DetectorManufacturer ccdType,
                          final Site site) {
        this.filter             = filter;
        this.grating            = grating;
        this.centralWavelength  = centralWavelength;
        this.fpMask             = fpMask;
        this.spatBinning        = spatBinning;
        this.specBinning        = specBinning;
        this.ifuMethod          = ifuMethod;
        this.ccdType            = ccdType;
        this.site               = site;

    }

    public Filter getFilter() {
        return filter;
    }

    public Disperser getGrating() {
        return grating;
    }

    public FPUnit getFocalPlaneMask() {
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

    public DetectorManufacturer getCCDtype() {
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

    public Site getSite() {
        return site;
    }

    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        final StringBuilder sb = new StringBuilder("Filter:\t");
        sb.append(getFilter());
        sb.append("\nGrating:\t");
        sb.append(getGrating().name());
        sb.append("\nInstrument Central Wavelength:\t");
        sb.append(getCentralWavelength());
        sb.append("\nFocal Plane Mask: \t ");
        sb.append(getFPMask());
        sb.append(" arcsec slit \n\n");
        return sb.toString();
    }
}
