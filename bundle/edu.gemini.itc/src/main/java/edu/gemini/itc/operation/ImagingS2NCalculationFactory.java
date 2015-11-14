package edu.gemini.itc.operation;

import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.itc.shared.S2NMethod;

public final class ImagingS2NCalculationFactory {

    private ImagingS2NCalculationFactory() {}

    public static ImagingS2NCalculatable getCalculationInstance(final ObservationDetails obs, final Instrument instrument, final SourceFraction srcFrac, final double sed_integral, final double sky_integral) {

        if (obs.calculationMethod() instanceof S2NMethod) {
            return new ImagingS2NMethodACalculation(obs, instrument, srcFrac, sed_integral, sky_integral);
        } else {
            return new ImagingPointS2NMethodBCalculation(obs, instrument, srcFrac, sed_integral, sky_integral);
        }

    }

}

