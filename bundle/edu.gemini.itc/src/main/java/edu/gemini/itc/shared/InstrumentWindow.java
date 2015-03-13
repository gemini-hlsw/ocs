package edu.gemini.itc.shared;

/**
 * This is just a basic transmission element for user selectable windows
 * on instruments.
 */

public class InstrumentWindow extends TransmissionElement {
    String windowName;

    public InstrumentWindow(String resource, String windowName) {
        super(resource);
        this.windowName = windowName;

    }

    public String toString() {
        return "User Selectable Window: " + windowName;
    }

}
