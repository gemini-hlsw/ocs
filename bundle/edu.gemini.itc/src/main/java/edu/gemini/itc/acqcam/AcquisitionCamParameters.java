package edu.gemini.itc.acqcam;

import edu.gemini.itc.shared.InstrumentDetails;
import edu.gemini.spModel.gemini.acqcam.AcqCamParams.*;

/**
 * This class holds the information from the Acquisition Camera section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class AcquisitionCamParameters implements InstrumentDetails {

    // Data members
    private final ColorFilter _colorFilter;  // U, V, B, ...
    private final NDFilter _ndFilter;  // NDa, NDb, ...  or null for clear

    /**
     * Constructs a AcquisitionCamParameters from a servlet request
     */
    public AcquisitionCamParameters(final ColorFilter colorFilter, final NDFilter ndFilter) {
        _colorFilter = colorFilter;
        _ndFilter = ndFilter;
    }

    public ColorFilter getColorFilter() {
        return _colorFilter;
    }

    public NDFilter getNDFilter() {
        return _ndFilter;
    }

}
