package edu.gemini.itc.shared;

/**
 * This represents the transmission of the optics native to the camera.
 */
public class FixedOptics extends TransmissionElement {
    private static final String FILENAME =
            "fixed_optics" + Instrument.getSuffix();

    public FixedOptics(String directory, String prefix) {
        super(directory + prefix + FILENAME);

    }

    public String toString() {
        return "Fixed Optics";
    }

}
