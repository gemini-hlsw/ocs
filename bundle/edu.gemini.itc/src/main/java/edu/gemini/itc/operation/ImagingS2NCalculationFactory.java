package edu.gemini.itc.operation;

import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.parameters.SourceDefinitionParameters;
import edu.gemini.itc.shared.Instrument;

public final class ImagingS2NCalculationFactory {

    private ImagingS2NCalculationFactory() {
    }

    public static ImagingS2NCalculatable getCalculationInstance(
            final SourceDefinitionParameters sourceDefinitionParameters,
            final ObservationDetailsParameters observationDetailsParameters,
            final Instrument instrument) {


        final String calcMethod = observationDetailsParameters.getCalculationMethod();
        switch (calcMethod) {

            // --- Signal to noise
            case ObservationDetailsParameters.S2N:
                return new ImagingS2NMethodACalculation(
                        observationDetailsParameters.getNumExposures(),
                        observationDetailsParameters.getSourceFraction(),
                        observationDetailsParameters.getExposureTime(),
                        instrument.getReadNoise(),
                        instrument.getPixelSize());

            // --- Integration time
            case ObservationDetailsParameters.INTTIME:
                if (sourceDefinitionParameters.getSourceGeometry().equals(SourceDefinitionParameters.EXTENDED_SOURCE)) {
                    if (sourceDefinitionParameters.getExtendedSourceType().equals(SourceDefinitionParameters.UNIFORM)) {
                        return new ImagingUSBS2NMethodBCalculation(
                                observationDetailsParameters.getNumExposures(),
                                observationDetailsParameters.getSourceFraction(),
                                observationDetailsParameters.getExposureTime(),
                                instrument.getReadNoise(),
                                observationDetailsParameters.getSNRatio(),
                                instrument.getPixelSize());
                    } else {
                        return new ImagingPointS2NMethodCCalculation(
                                observationDetailsParameters.getNumExposures(),
                                observationDetailsParameters.getSourceFraction(),
                                observationDetailsParameters.getExposureTime(),
                                instrument.getReadNoise(),
                                observationDetailsParameters.getSNRatio(),
                                instrument.getPixelSize());

                    }
                } else {
                    return new ImagingPointS2NMethodBCalculation(
                            observationDetailsParameters.getNumExposures(),
                            observationDetailsParameters.getSourceFraction(),
                            observationDetailsParameters.getExposureTime(),
                            instrument.getReadNoise(),
                            observationDetailsParameters.getSNRatio(),
                            instrument.getPixelSize());
                }

            default:
                throw new IllegalArgumentException("unknown calculation method " + calcMethod);
        }

    }

}

