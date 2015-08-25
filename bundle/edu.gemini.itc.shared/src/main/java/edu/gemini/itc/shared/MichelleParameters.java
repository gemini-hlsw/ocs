package edu.gemini.itc.shared;

import edu.gemini.itc.shared.InstrumentDetails;
import edu.gemini.spModel.core.Wavelength;
import edu.gemini.spModel.gemini.michelle.MichelleParams.*;

/**
 * This class holds the information from the Michelle section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class MichelleParameters implements InstrumentDetails {

    public static final String KBR = "KBr";
    public static final String KRS5 = "KRS5";

    public static final String LOW_N = "lowN";
    public static final String LOW_Q = "lowQ";
    public static final String MED_N1 = "medN1";
    public static final String MED_N2 = "medN2";
    public static final String ECHELLE_N = "EchelleN";
    public static final String ECHELLE_Q = "EchelleQ";

    public static final String WIRE_GRID = "wire_grid";

    public static final String ENABLED = "enabled";
    public static final String DISABLED = "disabled";

    public static final String NO_DISPERSER = "none";

    private final Mask mask;
    private final String filter;
    private final String grating;
    private final Wavelength centralWavelength;
    private final String polarimetry;

    /**
     * Constructs a MichelleParameters from a servlet request
     */
    public MichelleParameters(final String filter,
                              final String grating,
                              final Wavelength instrumentCentralWavelength,
                              final Mask mask,
                              final String polarimetry) {
        this.filter = filter;
        this.grating = grating;
        this.centralWavelength = instrumentCentralWavelength;
        this.mask = mask;
        this.polarimetry = polarimetry;

    }

    public String getFilter() {
        return filter;
    }

    public String getGrating() {
        return grating;
    }

    public Mask getFocalPlaneMask() {
        return mask;
    }

    public double getInstrumentCentralWavelength() {
        return centralWavelength.toNanometers();
    }

    public boolean polarimetryIsUsed() {
        return polarimetry.equals(ENABLED);
    }

}
