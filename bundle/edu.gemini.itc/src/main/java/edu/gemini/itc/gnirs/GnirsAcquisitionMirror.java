package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.base.TransmissionElement;

/**
 * This represents the transmission of the optics for the GNIRS acquisition mirror when doing imaging.
 */
public class GnirsAcquisitionMirror extends TransmissionElement {

    public GnirsAcquisitionMirror(String directory, String acqMirrorTransmissionFile) {
        super(directory + "/" + Gnirs.getPrefix() + acqMirrorTransmissionFile + Instrument.getSuffix());
    }

    public String toString() {
        return "acquisition mirror";
    }
}