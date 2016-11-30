package edu.gemini.itc.base;

public class Detector extends TransmissionElement {
    private int _detectorPixels;
    private final String _detectorType;
    private final String _name;

    public Detector(final String directory, final String prefix, final String filename, final String detectorType) {
        this(directory, prefix, filename, detectorType, "");
    }

    public Detector(final String directory, final String prefix, final String filename, final String detectorType, final String name) {
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

    @Override public String toString() {
        return "Detector - " + _detectorType;
    }

    public String getName() {
        return _name;
    }
}
