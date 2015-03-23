package edu.gemini.itc.operation;

import edu.gemini.itc.shared.ObservingConditions;
import edu.gemini.itc.shared.SourceDefinition;
import edu.gemini.itc.shared.TelescopeDetails;
import edu.gemini.itc.shared.Instrument;

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
                // Case B The Image Quality is defined by either of the
                // Probes in conjuction with the Atmosphric Seeing.
                // This case creates an ImageQuality Calculation
                return new ImageQualityCalculation(telescope.getWFS(),
                        observingConditions.getImageQuality(),
                        observingConditions.getAirmass(),
                        instrument.getEffectiveWavelength());
        }

    }
}

