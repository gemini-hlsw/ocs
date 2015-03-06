package edu.gemini.itc.operation;

import edu.gemini.itc.shared.FormatStringWriter;

public final class USBSourceFraction implements SourceFraction {

    private final double sw_ap;
    private final double Npix;
    private final double source_fraction;

    public USBSourceFraction(final boolean isAutoAperture, final double ap_diam, final double pixel_size) {
        final double pix_per_sq_arcsec = 1 / (pixel_size * pixel_size);
        final double usbApArea;
        final double ap_diam2;
        if (isAutoAperture) {
            usbApArea = 1;
            ap_diam2 = Math.sqrt(usbApArea * 4 / Math.PI);
        } else {
            // Do nothing to ap_diam. It is correct
            usbApArea = ap_diam * ap_diam * Math.PI / 4;
            ap_diam2 = ap_diam;
        }

        final double ap_pix = usbApArea * pix_per_sq_arcsec;
        Npix = (ap_pix >= 1) ? ap_pix : 1;
        sw_ap = (ap_pix >= 1) ? ap_diam2 : 1.1 * pixel_size; //1.1 is the diameter of circle that holds 1 ap_pix
        // (Pi*D^2)/4= 1 ; D= 1.1

        source_fraction = usbApArea;

    }

    public String getTextResult(final FormatStringWriter device) {
        return getTextResult(device, true);
    }

    public String getTextResult(final FormatStringWriter device, final boolean sfPrint) {
        StringBuffer sb = new StringBuffer();
        sb.append("software aperture diameter = " +
                device.toString(sw_ap) + " arcsec\n");
        sb.append("enclosed pixels = " +
                device.toString(Npix) + "\n");
        return sb.toString();
    }

    public double getSourceFraction() {
        return source_fraction;
    }

    public double getNPix() {
        return Npix;
    }

}
