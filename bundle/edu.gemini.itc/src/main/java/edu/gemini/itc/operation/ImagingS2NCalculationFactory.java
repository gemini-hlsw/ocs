package edu.gemini.itc.operation;

import edu.gemini.itc.service.ObservationDetails;
import edu.gemini.itc.shared.Instrument;

public final class ImagingS2NCalculationFactory {

    private ImagingS2NCalculationFactory() {}

    public static ImagingS2NCalculatable getCalculationInstance(final ObservationDetails obs, final Instrument instrument, final SourceFraction srcFrac) {

        if (obs.getMethod().isS2N()) {
            return new ImagingS2NMethodACalculation(obs, instrument, srcFrac);
        } else {
            return new ImagingPointS2NMethodBCalculation(obs, instrument, srcFrac);
        }

    }

}

