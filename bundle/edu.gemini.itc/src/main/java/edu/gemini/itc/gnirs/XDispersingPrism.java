package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.base.TransmissionElement;

/**
 * This represents the transmission of the optics native to the X-Disp Prism.
 */
public class XDispersingPrism extends TransmissionElement {

    public XDispersingPrism(String directory, String xDispTransmissionFile) {
        super(directory + "/" + Gnirs.getPrefix() + xDispTransmissionFile + Instrument.getSuffix());
    }

    public String toString() {
        return "Cross-Dispersing Prism";
    }
}

