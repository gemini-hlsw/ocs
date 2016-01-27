package edu.gemini.itc.operation;

/**
 * Helper to calculate the fraction of the incoming source flux that goes through the are of an aperture.
 * For uniform sources this is equal to the area; i.e. the area covered by the aperture in arcsec² multiplied
 * with the flux/arcsec² will give us the total flux that arrives on the CCD.
 */
public final class USBSourceFraction implements SourceFraction {

    private final double sw_ap;
    private final double Npix;
    private final double source_fraction;

    /**
     * Creates source fraction for an "optimal" (automatic) aperture.
     * Auto aperture is defined as 1 arcsec².
     */
    public USBSourceFraction(final double pixel_size) {

        final double pix_per_sq_arcsec = 1 / (pixel_size * pixel_size);
        final double ap_diam           = Math.sqrt(4 / Math.PI);

        Npix  = (pix_per_sq_arcsec >= 1) ? pix_per_sq_arcsec : 1;
        sw_ap = (pix_per_sq_arcsec >= 1) ? ap_diam : 1.1 * pixel_size; //1.1 is the diameter of circle that holds 1 ap_pix (Pi*D^2)/4= 1 ; D= 1.1

        source_fraction = 1.0;

    }

    /**
     * Creates source fraction for a user defined aperture.
     */
    public USBSourceFraction(final double ap_diam, final double pixel_size) {

        final double pix_per_sq_arcsec = 1 / (pixel_size * pixel_size);
        final double usbApArea         = ap_diam * ap_diam * Math.PI / 4;
        final double ap_pix            = usbApArea * pix_per_sq_arcsec;

        Npix  = (ap_pix >= 1) ? ap_pix  : 1;
        sw_ap = (ap_pix >= 1) ? ap_diam : 1.1 * pixel_size; //1.1 is the diameter of circle that holds 1 ap_pix (Pi*D^2)/4= 1 ; D= 1.1

        source_fraction = usbApArea;

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
