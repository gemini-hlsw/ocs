package edu.gemini.itc.operation;

import edu.gemini.itc.base.ArraySpectrum;
import edu.gemini.itc.base.DefaultArraySpectrum;
import edu.gemini.itc.base.ITCConstants;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.guide.GuideProbe;

public class ImageQualityCalculation implements ImageQualityCalculatable {

    private String im_qual_model_file;
    private double airmass, effectiveWavelength, im_qual;

    public ImageQualityCalculation(final GuideProbe.Type wfs,
                                   final SPSiteQuality.ImageQuality iq,
                                   final double airmass,
                                   final int effectiveWavelength) {

        im_qual_model_file = ITCConstants.IM_QUAL_LIB + "/" + ITCConstants.IM_QUAL_BASE + wfs.name().toLowerCase() + (iq.ordinal()+1) + ITCConstants.DATA_SUFFIX;

        this.airmass = airmass;
        this.effectiveWavelength = effectiveWavelength;
    }

    public void calculate() {
        ArraySpectrum im_qual_model = new DefaultArraySpectrum(im_qual_model_file);
        im_qual = im_qual_model.getY(effectiveWavelength) * (Math.pow(airmass, 0.6));
    }

    public double getImageQuality() {
        return im_qual;
    }
}
