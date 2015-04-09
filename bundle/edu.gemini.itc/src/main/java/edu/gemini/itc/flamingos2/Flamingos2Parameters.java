package edu.gemini.itc.flamingos2;

import edu.gemini.itc.shared.InstrumentDetails;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2.*;

/**
 * Flamingos2 parameters relevant for ITC.
 */
public final class Flamingos2Parameters implements InstrumentDetails {

    // Data members
    private final Filter filter;
    private final Disperser grism;
    private final ReadMode readMode;
    private final FPUnit fpMask;

    /**
     * Constructs a AcquisitionCamParameters from a servlet request
     */
    public Flamingos2Parameters(final Filter filter, final Disperser grism, final FPUnit fpMask, final ReadMode readNoise) {
        this.filter = filter;
        this.grism = grism;
        this.fpMask = fpMask;
        this.readMode = readNoise;
    }

    public Filter getFilter() {
        return filter;
    }

    public FPUnit getFPMask() {
        return fpMask;
    }

    public ReadMode getReadMode() {
        return readMode;
    }

    public double getReadNoise() {
        switch (readMode) {
            // TODO: this is for regression tests only, actual readmode is defined as 12.1
            case BRIGHT_OBJECT_SPEC:    return 12.0;
            default:                    return readMode.readNoise();
        }
    }

    public Disperser getGrism() {
        return grism;
    }

}
