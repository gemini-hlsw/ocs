package edu.gemini.itc.operation;

import edu.gemini.itc.base.Gaussian;

/**
 * Calculate the fraction of the source flux of a point source that is contained in this aperture.
 * For point sources this is equal to the area and a gaussian distribution of the incoming flux depending
 * on the image quality.
 */
public final class PointSourceFraction implements SourceFraction {

    private double Npix;
    private double sw_ap;
    private double source_fraction;

    public PointSourceFraction(final double pixel_size, final double im_qual) {
        init(1.18 * im_qual, pixel_size, im_qual);
    }

    public PointSourceFraction(final double ap_diam, final double pixel_size, final double im_qual) {
        init(ap_diam, pixel_size, im_qual);
    }

    private void init(final double ap_diam, final double pixel_size, final double im_qual) {
        final double ap_pix = (Math.PI / 4.) * (ap_diam / pixel_size) * (ap_diam / pixel_size);
        Npix = (ap_pix >= 9) ? ap_pix : 9;
        sw_ap = (ap_pix >= 9) ? ap_diam : 3.4 * pixel_size;

        // Calculate the fraction of source flux contained in this aperture.
        // Found by doing 2-d integral over assumed gaussian profile.
        double sigma = im_qual / 2.355;
        double ap_ratio = sw_ap / sigma;
        double ap_frac = Gaussian.get2DIntegral(ap_ratio);

        source_fraction = (ap_ratio > 5.0) ? 1.0 : ap_frac;
    }

    /** {@inheritDoc} */
    public double getSourceFraction() {
        return source_fraction;
    }

    /** {@inheritDoc} */
    public double getNPix() {
        return Npix;
    }

    /** {@inheritDoc} */
    public double getSoftwareAperture() {
        return sw_ap;
    }

}
