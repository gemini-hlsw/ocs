package edu.gemini.itc.shared;


/**
 * This is just a basic transmission element for polarimetry wire grid elements
 * on instruments.
 */

public class WireGrid extends TransmissionElement {


    public WireGrid(String resource) {
        super(resource);
    }

    public String toString() {
        return "Wire Grid Transmission ";
    }

}
