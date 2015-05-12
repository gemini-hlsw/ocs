package edu.gemini.itc.operation;

import edu.gemini.itc.base.Gaussian;

public final class PointSourceFraction implements SourceFraction {

    private final double Npix;
    private final double sw_ap;
    private final double source_fraction;

    public PointSourceFraction(final boolean isAuto, final double ap_diam, final double pixel_size, final double im_qual) {

        final double ap_diam2;
        if (isAuto) {
            ap_diam2 = 1.18 * im_qual;
        } else {
            ap_diam2 = ap_diam;
        }

        final double ap_pix = (Math.PI / 4.) * (ap_diam2 / pixel_size) * (ap_diam2 / pixel_size);
        Npix = (ap_pix >= 9) ? ap_pix : 9;
        sw_ap = (ap_pix >= 9) ? ap_diam2 : 3.4 * pixel_size;

        // Calculate the fraction of source flux contained in this aperture.
        // Found by doing 2-d integral over assumed gaussian profile.
        double sigma = im_qual / 2.355;
        double ap_ratio = sw_ap / sigma;
        double ap_frac = Gaussian.get2DIntegral(ap_ratio);

        source_fraction = (ap_ratio > 5.0) ? 1.0 : ap_frac;

    }

    public double getSourceFraction() {
        return source_fraction;
    }

    public double getNPix() {
        return Npix;
    }

    public double getSoftwareAperture() {
        return sw_ap;
    }

}
