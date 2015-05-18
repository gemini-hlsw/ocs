package edu.gemini.itc.base;

public class Detector extends TransmissionElement {
    private int _detectorPixels;
    private String _detectorType;
    private String _name;

    public Detector(String directory, String prefix, String filename, String detectorType) {
        this(directory, prefix, filename, detectorType, "");
    }

    public Detector(String directory, String prefix, String filename, String detectorType, String name) {
        super(directory + prefix + filename + Instrument.getSuffix());
        _detectorType = detectorType;
        _name = name;
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
}
