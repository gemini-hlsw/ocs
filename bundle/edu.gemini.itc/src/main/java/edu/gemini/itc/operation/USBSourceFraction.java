package edu.gemini.itc.operation;

public final class USBSourceFraction implements SourceFraction {

    private final double sw_ap;
    private final double Npix;
    private final double source_fraction;

    public USBSourceFraction(final double pixel_size) {

        final double pix_per_sq_arcsec = 1 / (pixel_size * pixel_size);
        final double ap_diam           = Math.sqrt(4 / Math.PI);

        Npix  = (pix_per_sq_arcsec >= 1) ? pix_per_sq_arcsec : 1;
        sw_ap = (pix_per_sq_arcsec >= 1) ? ap_diam : 1.1 * pixel_size; //1.1 is the diameter of circle that holds 1 ap_pix (Pi*D^2)/4= 1 ; D= 1.1

        source_fraction = 1.0;

    }

    public USBSourceFraction(final double ap_diam, final double pixel_size) {

        final double pix_per_sq_arcsec = 1 / (pixel_size * pixel_size);
        final double usbApArea         = ap_diam * ap_diam * Math.PI / 4;
        final double ap_pix            = usbApArea * pix_per_sq_arcsec;

        Npix  = (ap_pix >= 1) ? ap_pix  : 1;
        sw_ap = (ap_pix >= 1) ? ap_diam : 1.1 * pixel_size; //1.1 is the diameter of circle that holds 1 ap_pix (Pi*D^2)/4= 1 ; D= 1.1

        source_fraction = usbApArea;

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
