package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.base.TransmissionElement;

/**
 * This represents the transmission of the optics for the GNIRS Pickoff mirror when XD is not used.
 */
public class GnirsPickoffMirror extends TransmissionElement {

    public GnirsPickoffMirror(String directory, String mirrorTransmissionFile) {
        super(directory + "/" + Gnirs.getPrefix() + mirrorTransmissionFile + Instrument.getSuffix());
    }

    public String toString() {
        return "(mirror) no prism";
    }
}

