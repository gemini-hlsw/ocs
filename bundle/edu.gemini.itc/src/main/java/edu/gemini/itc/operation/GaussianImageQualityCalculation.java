// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
package edu.gemini.itc.operation;

import edu.gemini.itc.shared.FormatStringWriter;

public class GaussianImageQualityCalculation implements ImageQualityCalculatable {

    double im_qual, fwhm;

    public GaussianImageQualityCalculation(
            double fwhm) {
        this.fwhm = fwhm;
    }

    public void calculate() {
        im_qual = fwhm;
    }

    public String getTextResult(FormatStringWriter device) {
        return "derived image size for source = "
                + device.toString(im_qual) + "\n";
    }

    public double getImageQuality() {
        return im_qual;
    }
}
