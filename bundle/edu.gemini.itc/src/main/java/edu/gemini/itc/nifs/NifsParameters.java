package edu.gemini.itc.nifs;

import edu.gemini.itc.shared.AltairParameters;
import edu.gemini.itc.shared.IfuMethod;
import edu.gemini.itc.shared.InstrumentDetails;
import edu.gemini.spModel.core.Wavelength;
import edu.gemini.spModel.gemini.nifs.NIFSParams;
import scala.Option;

/**
 * This class holds the information from the Nifs section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class NifsParameters implements InstrumentDetails {

    public static final String NIFS = "nifs";

    // Data members
    private final NIFSParams.Filter filter;
    private final NIFSParams.Disperser grating;
    private final NIFSParams.ReadMode readMode;
    private final Wavelength cenralWavelength;
    private final IfuMethod ifuMethod;
    private final Option<AltairParameters> altair;

    /**
     * Constructs a NifsParameters from a test file.
     */
    public NifsParameters(final NIFSParams.Filter filter,
                          final NIFSParams.Disperser grating,
                          final NIFSParams.ReadMode readMode,
                          final Wavelength centralWavelength,
                          final IfuMethod ifuMethod,
                          final Option<AltairParameters> altair) {

        this.filter             = filter;
        this.grating            = grating;
        this.readMode           = readMode;
        this.ifuMethod          = ifuMethod;
        this.altair             = altair;
        this.cenralWavelength   = centralWavelength;
    }

    public NIFSParams.Filter getFilter() {
        return filter;
    }

    public NIFSParams.Disperser getGrating() {
        return grating;
    }

    public NIFSParams.ReadMode getReadMode() {
        return readMode;
    }

    public double getInstrumentCentralWavelength() {
        return cenralWavelength.toNanometers();
    }

    public double getUnXDispCentralWavelength() {
        return cenralWavelength.toNanometers();
    }

    public IfuMethod getIFUMethod() {
        return ifuMethod;
    }

    public double getFPMask() {
        return 0.15;
    }

    public Option<AltairParameters> getAltair() {
        return altair;
    }

}
