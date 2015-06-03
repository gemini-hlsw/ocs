package edu.gemini.itc.operation;

public class GaussianImageQualityCalculation implements ImageQualityCalculatable {

    private final double im_qual;

    public GaussianImageQualityCalculation(final double fwhm) {
        this.im_qual = fwhm;
    }

    public void calculate() {}

    public double getImageQuality() {
        return im_qual;
    }
}
