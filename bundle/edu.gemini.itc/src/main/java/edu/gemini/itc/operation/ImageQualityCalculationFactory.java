package edu.gemini.itc.operation;

import edu.gemini.itc.parameters.ObservingConditionParameters;
import edu.gemini.itc.parameters.SourceDefinitionParameters;
import edu.gemini.itc.parameters.TeleParameters;
import edu.gemini.itc.shared.Instrument;

public final class ImageQualityCalculationFactory {

    private ImageQualityCalculationFactory() {}

    public static ImageQualityCalculatable getCalculationInstance(
            SourceDefinitionParameters sourceDefinitionParameters,
            ObservingConditionParameters observingConditionParameters,
            TeleParameters teleParameters,
            Instrument instrument) {

        // Case A The Image quality is defined by the user
        // who has selected a Gaussian Extended source
        // Creates a GaussianImageQualityCalculation

        if (sourceDefinitionParameters.getSourceGeometry().equals(
                SourceDefinitionParameters.EXTENDED_SOURCE)) {

            if (sourceDefinitionParameters.getExtendedSourceType().equals(
                    SourceDefinitionParameters.GAUSSIAN)) {
                return new GaussianImageQualityCalculation(
                        sourceDefinitionParameters.getFWHM());
            }
        }

        // Case B The Image Quality is defined by either of the
        // Probes in conjuction with the Atmosphric Seeing.
        // This case creates an ImageQuality Calculation
        return new ImageQualityCalculation(teleParameters.getWFS(),
                                           observingConditionParameters.getImageQuality(),
                                           observingConditionParameters.getAirmass(),
                                           instrument.getEffectiveWavelength());

    }
}

