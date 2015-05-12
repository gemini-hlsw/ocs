package edu.gemini.itc.base;

import java.awt.*;

public class Detector extends TransmissionElement {
    //private static final String FILENAME = "ccd_red" + Instrument.getSuffix();
    private int _detectorPixels;
    private String _detectorType;
    private String _name;
    private Color _color; // Optional color for plot

    public Detector(String directory, String prefix, String filename, String detectorType) {
        super(directory + prefix + filename + Instrument.getSuffix());
        _detectorType = detectorType;
        _name = "";
        _color = null;
    }

    public Detector(String directory, String prefix, String filename, String detectorType, String name, Color color) {
        super(directory + prefix + filename + Instrument.getSuffix());
        _detectorType = detectorType;
        _name = name;
        _color = color;
    }

    public int getDetectorPixels() {
        return _detectorPixels;
    }

    public void setDetectorPixels(int detectorPixels) {
        _detectorPixels = detectorPixels;
    }

    public String toString() {
        return "Detector - " + _detectorType;
    }

    public String getName() {
        return _name;
    }

    public Color getColor() {
        return _color;
    }
}
