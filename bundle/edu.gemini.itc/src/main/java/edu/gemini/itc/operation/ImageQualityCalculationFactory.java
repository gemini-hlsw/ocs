package edu.gemini.itc.operation;

import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.shared.ObservingConditions;
import edu.gemini.itc.shared.SourceDefinition;
import edu.gemini.itc.shared.TelescopeDetails;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.core.GaussianSource;

public final class ImageQualityCalculationFactory {

    private ImageQualityCalculationFactory() {
    }

    public static ImageQualityCalculatable getCalculationInstance(
            SourceDefinition sourceDefinition,
            ObservingConditions observingConditions,
            TelescopeDetails telescope,
            Instrument instrument) {

        if (observingConditions.javaIq().isLeft()) {
            // Case C: The exact delivered FHWM at the science wavelength is specified.
            final double fwhm = observingConditions.javaIq().toOptionLeft().getValue();
            if (fwhm <= 0.0) throw new IllegalArgumentException("Exact Image Quality must be > zero arcseconds.");
            return new GaussianImageQualityCalculation(fwhm);

        } else if (sourceDefinition.profile() instanceof GaussianSource) {
            // Case A: The Image quality is defined by the user
            // who has selected a Gaussian Extended source
            // Creates a GaussianImageQualityCalculation
            final double fwhm = ((GaussianSource) sourceDefinition.profile()).fwhm();
            return new GaussianImageQualityCalculation(fwhm);

        } else {
            // For AOWFS the image quality files of OIWFS are used (there are currently no files for AOWFS)
            final GuideProbe.Type wfs =
                    telescope.getWFS() == GuideProbe.Type.AOWFS ? GuideProbe.Type.OIWFS : telescope.getWFS();

            // Case B: The Image Quality is defined by either of the
            // Probes in conjunction with the Atmospheric Seeing.
            // This case creates an ImageQuality Calculation
            return new ImageQualityCalculation(
                    wfs,
                    observingConditions.javaIq().toOption().getValue(),
                    observingConditions.airmass(),
                    instrument.getEffectiveWavelength());
        }
    }
}
