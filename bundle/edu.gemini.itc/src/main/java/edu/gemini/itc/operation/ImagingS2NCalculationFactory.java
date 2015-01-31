package edu.gemini.itc.operation;

import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.parameters.SourceDefinitionParameters;
import edu.gemini.itc.shared.Instrument;

public final class ImagingS2NCalculationFactory {

    private ImagingS2NCalculationFactory() {}

    public static ImagingS2NCalculatable getCalculationInstance(
            final SourceDefinitionParameters sdp,
            final ObservationDetailsParameters odp,
            final Instrument instrument) {


        final String calcMethod = odp.getCalculationMethod();
        switch (calcMethod) {

            // --- Signal to noise
            case ObservationDetailsParameters.S2N:
                return new ImagingS2NMethodACalculation(
                        odp.getNumExposures(),
                        odp.getSourceFraction(),
                        odp.getExposureTime(),
                        instrument.getReadNoise(),
                        instrument.getPixelSize());

            // --- Integration time
            case ObservationDetailsParameters.INTTIME:
                final boolean extendedSource = sdp.getSourceGeometry().equals(SourceDefinitionParameters.EXTENDED_SOURCE);
                final boolean uniformSource  = sdp.getExtendedSourceType().equals(SourceDefinitionParameters.UNIFORM);
                if (extendedSource && uniformSource) {
                    return new ImagingUSBS2NMethodBCalculation(
                            odp.getNumExposures(),
                            odp.getSourceFraction(),
                            odp.getExposureTime(),
                            instrument.getReadNoise(),
                            odp.getSNRatio(),
                            instrument.getPixelSize());
                } else {
                    return new ImagingPointS2NMethodBCalculation(
                            odp.getNumExposures(),
                            odp.getSourceFraction(),
                            odp.getExposureTime(),
                            instrument.getReadNoise(),
                            odp.getSNRatio(),
                            instrument.getPixelSize());
                }

            default:
                throw new IllegalArgumentException("unknown calculation method " + calcMethod);
        }

    }

}

