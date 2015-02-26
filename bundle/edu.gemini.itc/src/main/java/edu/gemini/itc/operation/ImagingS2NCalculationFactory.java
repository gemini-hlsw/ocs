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


        if (odp.getMethod().isS2N()) {

            // --- Signal to noise
            return new ImagingS2NMethodACalculation(
                    odp.getNumExposures(),
                    odp.getSourceFraction(),
                    odp.getExposureTime(),
                    instrument.getReadNoise(),
                    instrument.getPixelSize());

        } else {

            // --- Integration time
            switch (sdp.getProfileType()) {
                case POINT:
                    return new ImagingPointS2NMethodBCalculation(
                            odp.getNumExposures(),
                            odp.getSourceFraction(),
                            odp.getExposureTime(),
                            instrument.getReadNoise(),
                            odp.getSNRatio(),
                            instrument.getPixelSize());
                default:
                    return new ImagingUSBS2NMethodBCalculation(
                            odp.getNumExposures(),
                            odp.getSourceFraction(),
                            odp.getExposureTime(),
                            instrument.getReadNoise(),
                            odp.getSNRatio(),
                            instrument.getPixelSize());
            }
        }

    }

}

