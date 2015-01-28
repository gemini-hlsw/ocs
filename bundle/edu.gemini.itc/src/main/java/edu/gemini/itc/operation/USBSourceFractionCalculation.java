// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.operation;

import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.shared.FormatStringWriter;

public class USBSourceFractionCalculation implements SourceFractionCalculatable {

    double im_qual = -1;
    double ap_diam, pixel_size, ap_pix, sw_ap, Npix, source_fraction,
            usbApArea, pix_per_sq_arcsec;
    String ap_type;

    public USBSourceFractionCalculation(String ap_type,
                                        double ap_diam, double pixel_size) {
        this.ap_type = ap_type;
        this.ap_diam = ap_diam;
        this.pixel_size = pixel_size;
    }

    public void calculate() throws Exception {
        if (im_qual < 0)
            throw new Exception("Programming Error, Must set image quality before calling Calculate");

        pix_per_sq_arcsec = 1 / (pixel_size * pixel_size);
        if (ap_type.equals(ObservationDetailsParameters.AUTO_APER)) {
            usbApArea = 1;
            ap_diam = Math.sqrt(usbApArea * 4 / Math.PI);
        } else if (ap_type.equals(
                ObservationDetailsParameters.USER_APER)) {
            // Do nothing to ap_diam. It is correct
            usbApArea = ap_diam * ap_diam * Math.PI / 4;
        } else {
            throw new Exception(
                    "Unknown aperture type: " + ap_type);
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

    public void setApType(String ap_type) {
        this.ap_type = ap_type;
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
