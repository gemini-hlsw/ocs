package edu.gemini.itc.base;

/**
 * This represents the transmission of the optics native to the camera.
 */
public class FixedOptics extends TransmissionElement {
    private static final String FILENAME =
            "fixed_optics" + Instrument.getSuffix();

    public FixedOptics(String directory, String prefix) {
        super(directory + prefix + FILENAME);
    }

    public FixedOptics(String directory, String prefix, String suffix) {
        super(directory + prefix + "fixed_optics" + suffix + Instrument.getSuffix());
    }

    public String toString() {
        return "Fixed Optics";
    }

}
