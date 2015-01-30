package edu.gemini.itc.operation;

import edu.gemini.itc.parameters.TeleParameters;
import edu.gemini.itc.shared.ArraySpectrum;
import edu.gemini.itc.shared.DefaultArraySpectrum;
import edu.gemini.itc.shared.FormatStringWriter;
import edu.gemini.itc.shared.ITCConstants;

public class ImageQualityCalculation implements ImageQualityCalculatable {

    String im_qual_model_file;
    double airmass, effectiveWavelength, im_qual;

    public ImageQualityCalculation(TeleParameters.Wfs wfs,
                                   int imageQuality,
                                   double airmass,
                                   int effectiveWavelength) {

        im_qual_model_file = ITCConstants.IM_QUAL_LIB + "/" + ITCConstants.IM_QUAL_BASE + wfs.displayValue() + imageQuality + ITCConstants.DATA_SUFFIX;

        this.airmass = airmass;
        this.effectiveWavelength = effectiveWavelength;
    }

    public void calculate() throws Exception {
        ArraySpectrum im_qual_model = new DefaultArraySpectrum(im_qual_model_file);
        im_qual = im_qual_model.getY(effectiveWavelength) * (Math.pow(airmass, 0.6));
    }

    public String getTextResult(FormatStringWriter device) {
        return "derived image size (FWHM) for a point source = " + device.toString(im_qual) + " arcsec.\n";
    }

    public double getImageQuality() {
        return im_qual;
    }
}
