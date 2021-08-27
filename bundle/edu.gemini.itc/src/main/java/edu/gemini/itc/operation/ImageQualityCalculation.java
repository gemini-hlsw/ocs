package edu.gemini.itc.operation;

import edu.gemini.itc.base.ArraySpectrum;
import edu.gemini.itc.base.DefaultArraySpectrum;
import edu.gemini.itc.base.ITCConstants;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.guide.GuideProbe;
import java.util.logging.Logger;

public class ImageQualityCalculation implements ImageQualityCalculatable {

    private final String im_qual_model_file;
    private final double airmass;
    private final double effectiveWavelength;
    private double im_qual;
    private static final Logger Log = Logger.getLogger( ImageQualityCalculation.class.getName() );

    public ImageQualityCalculation(final GuideProbe.Type wfs,
                                   final SPSiteQuality.ImageQuality iq,
                                   final double airmass,
                                   final int effectiveWavelength) {

        im_qual_model_file = ITCConstants.IM_QUAL_LIB + "/" + ITCConstants.IM_QUAL_BASE + wfs.name().toLowerCase() + "_" + iq.sequenceValue() + ITCConstants.DATA_SUFFIX;

        this.airmass = airmass;
        this.effectiveWavelength = effectiveWavelength;
    }

    public void calculate() {
        ArraySpectrum im_qual_model = new DefaultArraySpectrum(im_qual_model_file);
        im_qual = im_qual_model.getY(effectiveWavelength) * (Math.pow(airmass, 0.6));
        Log.fine(String.format("Image quality = %.5f arcseconds at airmass = %.2f", im_qual, airmass));
    }

    public double getImageQuality() {
        return im_qual;
    }
}
