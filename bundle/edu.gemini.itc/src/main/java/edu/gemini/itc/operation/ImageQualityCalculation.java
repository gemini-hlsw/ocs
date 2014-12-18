// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.operation;

import edu.gemini.itc.shared.FormatStringWriter;

import edu.gemini.itc.shared.ArraySpectrum;
import edu.gemini.itc.shared.DefaultArraySpectrum;
import edu.gemini.itc.shared.ITCConstants;

public class ImageQualityCalculation implements ImageQualityCalculatable {

    String wfs,imageQuality, im_qual_model_file;
    double airmass,effectiveWavelength, im_qual;

    public ImageQualityCalculation(String wfs,
                                   int imageQuality,
                                   double airmass,
                                   int effectiveWavelength) {

        im_qual_model_file = ITCConstants.IM_QUAL_LIB + "/" +
                ITCConstants.IM_QUAL_BASE + wfs + imageQuality +
                ITCConstants.DATA_SUFFIX;

        this.airmass = airmass;
        this.effectiveWavelength = effectiveWavelength;
    }

    public void calculate() throws Exception {
        ArraySpectrum im_qual_model = new
                DefaultArraySpectrum(im_qual_model_file);
        im_qual = im_qual_model.getY((double) effectiveWavelength) *
                (Math.pow(airmass, 0.6));
        //((airmass-1)*.9+1);  //Old second line
    }

    public String getTextResult(FormatStringWriter device) {
        return "derived image size (FWHM) for a point source = " +
                device.toString(im_qual) + " arcsec.\n";
    }

    public double getImageQuality() {
        return im_qual;
    }
}
