package edu.gemini.itc.acqcam;

import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.base.TransmissionElement;
import edu.gemini.spModel.gemini.acqcam.AcqCamParams;

/**
 * Neutral density color wheel?
 * This class exists so that the client can specify a ND filter number
 * instead of specifying the data file name specifically.
 */
public final class NDFilterWheel extends TransmissionElement {
    private static final String FILENAME = AcquisitionCamera.getPrefix() + "ndfilt_";

    private final String _ndFilter;

    public NDFilterWheel(AcqCamParams.NDFilter ndFilter, String dir) {
        super(dir + FILENAME + ndFilter.name() + Instrument.getSuffix());
        _ndFilter = ndFilter.name();
    }

    public String toString() {
        return "Neutral Density Filter - " + _ndFilter;
    }
}
