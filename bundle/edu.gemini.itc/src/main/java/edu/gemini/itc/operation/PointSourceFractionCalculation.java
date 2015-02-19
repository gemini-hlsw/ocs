package edu.gemini.itc.operation;

import edu.gemini.itc.shared.FormatStringWriter;
import edu.gemini.itc.shared.Gaussian;

public class PointSourceFractionCalculation implements SourceFractionCalculatable {

    double im_qual = -1;
    double ap_diam, pixel_size, ap_pix, sw_ap, Npix, source_fraction;
    boolean isAutoAperture;
    boolean SFprint = true;

    public PointSourceFractionCalculation(boolean isAuto, double ap_diam, double pixel_size) {
        this.isAutoAperture = isAuto;
        this.ap_diam = ap_diam;
        this.pixel_size = pixel_size;

    }

    public void calculate() {
        if (im_qual < 0)
            throw new IllegalStateException("Programming Error, Must set image quality before calling Calculate");

        if (isAutoAperture) {
            ap_diam = 1.18 * im_qual;
        }

        ap_pix = (Math.PI / 4.) * (ap_diam / pixel_size) * (ap_diam / pixel_size);
        Npix = (ap_pix >= 9) ? ap_pix : 9;
        sw_ap = (ap_pix >= 9) ? ap_diam : 3.4 * pixel_size;

        // Calculate the fraction of source flux contained in this aperture.
        // Found by doing 2-d integral over assumed gaussian profile.
        double sigma = im_qual / 2.355;
        double ap_ratio = sw_ap / sigma;
        double ap_frac = Gaussian.get2DIntegral(ap_ratio);

        source_fraction = (ap_ratio > 5.0) ? 1.0 : ap_frac;

    }

    public String getTextResult(FormatStringWriter device) {
        StringBuffer sb = new StringBuffer();
        sb.append("software aperture diameter = " +
                device.toString(sw_ap) + " arcsec\n");
        if (SFprint) {
            sb.append("fraction of source flux in aperture = " +
                    device.toString(source_fraction) + "\n");
        }
        sb.append("enclosed pixels = " +
                device.toString(Npix) + "\n");
        return sb.toString();
    }

    // must set image quality before calculate
    public void setImageQuality(double im_qual) {
        this.im_qual = im_qual;
    }

    public void setApType(boolean ap_type) {
        this.isAutoAperture = ap_type;
    }

    public void setApDiam(double ap_diam) {
        this.ap_diam = ap_diam;
    }

    public void setSFPrint(boolean SFprint) {
        this.SFprint = SFprint;
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

}
