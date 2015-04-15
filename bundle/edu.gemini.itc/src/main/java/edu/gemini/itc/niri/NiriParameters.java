package edu.gemini.itc.niri;

import edu.gemini.itc.altair.AltairParameters;
import edu.gemini.itc.shared.InstrumentDetails;
import edu.gemini.spModel.gemini.niri.Niri.*;
import scala.Option;

/**
 * NIRI parameters relevant for ITC.
 */
public final class NiriParameters implements InstrumentDetails {

    private final Filter filter;
    private final Disperser grism;
    private final Camera camera;
    private final ReadMode readMode;
    private final WellDepth wellDepth;
    private final Mask mask;
    private final Option<AltairParameters> altair;

    /**
     * Constructs a AcquisitionCamParameters from a servlet request
     */
    public NiriParameters(final Filter filter,
                          final Disperser grism,
                          final Camera camera,
                          final ReadMode readMode,
                          final WellDepth wellDepth,
                          final Mask mask,
                          final Option<AltairParameters> altair) {
        this.filter = filter;
        this.grism = grism;
        this.camera = camera;
        this.readMode = readMode;
        this.wellDepth = wellDepth;
        this.mask = mask;
        this.altair = altair;
    }

    public Filter getFilter() {
        return filter;
    }

    public Disperser getGrism() {
        return grism;
    }

    public Camera getCamera() {
        return camera;
    }

    public ReadMode getReadMode() {
        return readMode;
    }

    public WellDepth getWellDepth() {
        return wellDepth;
    }

    public Mask getFocalPlaneMask() {
        return mask;
    }

    public Option<AltairParameters> getAltair() {
        return altair;
    }

}
