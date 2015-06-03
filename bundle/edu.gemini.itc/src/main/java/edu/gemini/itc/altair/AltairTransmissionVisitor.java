package edu.gemini.itc.altair;

import edu.gemini.itc.base.ITCConstants;
import edu.gemini.itc.base.TransmissionElement;

/**
 * The AltairTransmissionVisitor is designed to adjust the SED for the
 * Tranmsission of the Altair optics.
 */
public final class AltairTransmissionVisitor extends TransmissionElement {

    /**
     * The AltairTrans constructor
     */
    public AltairTransmissionVisitor() {

        super(Altair.ALTAIR_LIB + "/" +
                Altair.ALTAIR_PREFIX +
                Altair.ALTAIR_TRANSMISSION_FILENAME +
                ITCConstants.DATA_SUFFIX);
    }

    public String toString() {
        return ("AltairTransmissionVisitor");
    }
}
