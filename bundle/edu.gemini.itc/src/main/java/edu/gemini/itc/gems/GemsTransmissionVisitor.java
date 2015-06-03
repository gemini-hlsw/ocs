package edu.gemini.itc.gems;

import edu.gemini.itc.base.ITCConstants;
import edu.gemini.itc.base.TransmissionElement;

/**
 * The GemsTransmissionVisitor is designed to adjust the SED for the
 * Tranmsission of the Gems optics.
 */
public final class GemsTransmissionVisitor extends TransmissionElement {

    /**
     * The GemsTrans constructor
     */
    public GemsTransmissionVisitor() {

        super(Gems.GEMS_LIB + "/" +
                Gems.GEMS_PREFIX +
                Gems.GEMS_TRANSMISSION_FILENAME +
                ITCConstants.DATA_SUFFIX);
    }

    public String toString() {
        return ("GemsTransmissionVisitor");
    }
}
