package edu.gemini.itc.operation;

import edu.gemini.itc.shared.FormatStringWriter;

public class USBSourceFractionCalculation implements SourceFractionCalculatable {

    double im_qual = -1;
    double ap_diam, pixel_size, ap_pix, sw_ap, Npix, source_fraction, usbApArea, pix_per_sq_arcsec;
    boolean isAutoAperture;

    public USBSourceFractionCalculation(boolean isAutoAperture, double ap_diam, double pixel_size) {
        this.isAutoAperture = isAutoAperture;
        this.ap_diam = ap_diam;
        this.pixel_size = pixel_size;
    }

    public void calculate() {
        if (im_qual < 0)
            throw new IllegalStateException("Programming Error, Must set image quality before calling Calculate");

        pix_per_sq_arcsec = 1 / (pixel_size * pixel_size);
        if (isAutoAperture) {
            usbApArea = 1;
            ap_diam = Math.sqrt(usbApArea * 4 / Math.PI);
        } else {
            // Do nothing to ap_diam. It is correct
            usbApArea = ap_diam * ap_diam * Math.PI / 4;
        }

        ap_pix = usbApArea * pix_per_sq_arcsec;
        Npix = (ap_pix >= 1) ? ap_pix : 1;
        sw_ap = (ap_pix >= 1) ? ap_diam : 1.1 * pixel_size; //1.1 is the diameter of circle that holds 1 ap_pix
        // (Pi*D^2)/4= 1 ; D= 1.1

        source_fraction = usbApArea;

    }

    public String getTextResult(FormatStringWriter device) {
        StringBuffer sb = new StringBuffer();
        sb.append("software aperture diameter = " +
                device.toString(sw_ap) + " arcsec\n");
        sb.append("enclosed pixels = " +
                device.toString(Npix) + "\n");
        return sb.toString();
    }

    // must set image quality before calculate
    public void setImageQuality(double im_qual) {
        this.im_qual = im_qual;
    }

    public void setApType(boolean isAuto) {
        this.isAutoAperture = isAuto;
    }

    public void setApDiam(double ap_diam) {
        this.ap_diam = ap_diam;
    }

    public double getSourceFraction() {
        return source_fraction;
    }

    public double getNPix() {
        return Npix;
    }

    public double getApDiam() {
        return ap_diam;
    }

    public double getApPix() {
        return ap_pix;
    }

    public double getSwAp() {
        return sw_ap;
    }

    public void setSFPrint(boolean SFprint) {
    }

}
