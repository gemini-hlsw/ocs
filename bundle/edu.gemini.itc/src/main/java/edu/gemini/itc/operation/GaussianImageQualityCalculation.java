package edu.gemini.itc.operation;

import java.util.logging.Logger;

public class GaussianImageQualityCalculation implements ImageQualityCalculatable {
    private static final Logger Log = Logger.getLogger( GaussianImageQualityCalculation.class.getName() );

    private final double im_qual;

    public GaussianImageQualityCalculation(final double fwhm) {
        this.im_qual = fwhm;
        Log.fine(String.format("Image quality = %.5f arcsec", im_qual));
    }

    public void calculate() {}

    public double getImageQuality() {
        return im_qual;
    }
}
