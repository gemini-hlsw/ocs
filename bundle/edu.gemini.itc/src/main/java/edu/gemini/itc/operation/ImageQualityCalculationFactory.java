package edu.gemini.itc.operation;

import edu.gemini.itc.shared.ObservingConditions;
import edu.gemini.itc.shared.SourceDefinition;
import edu.gemini.itc.shared.TelescopeDetails;
import edu.gemini.itc.shared.Instrument;
import edu.gemini.spModel.guide.GuideProbe;

public final class ImageQualityCalculationFactory {

    private ImageQualityCalculationFactory() {
    }

    public static ImageQualityCalculatable getCalculationInstance(
            SourceDefinition sourceDefinition,
            ObservingConditions observingConditions,
            TelescopeDetails telescope,
            Instrument instrument) {

        switch (sourceDefinition.getProfileType()) {
            case GAUSSIAN:
                // Case A The Image quality is defined by the user
                // who has selected a Gaussian Extended source
                // Creates a GaussianImageQualityCalculation
                return new GaussianImageQualityCalculation(sourceDefinition.getFWHM());

            default:
                // For AOWFS the image quality files of OIWFS are used (there are currently no files for AOWFS)
                final GuideProbe.Type wfs =
                        telescope.getWFS() == GuideProbe.Type.AOWFS ? GuideProbe.Type.OIWFS : telescope.getWFS();

                // Case B The Image Quality is defined by either of the
                // Probes in conjuction with the Atmosphric Seeing.
                // This case creates an ImageQuality Calculation
                return new ImageQualityCalculation(
                        wfs,
                        observingConditions.getImageQuality(),
                        observingConditions.getAirmass(),
                        instrument.getEffectiveWavelength());
        }

    }
}

