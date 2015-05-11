package edu.gemini.itc.niri;

import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.base.TransmissionElement;

/**
 * This represents the transmission of the optics native to the camera.
 */
public class F14Optics extends TransmissionElement {
    private static final String FILENAME = Niri.getPrefix() +
            "f14_optics" + Instrument.getSuffix();

    public F14Optics(String directory) {
        super(directory + FILENAME);
    }

    public String toString() {// return "F14Optics - " + FILENAME; }
        return "Camera: f14";
    }
}
