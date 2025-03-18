package edu.gemini.itc.operation;

import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.shared.ImagingIntegrationTime;
import edu.gemini.itc.shared.ImagingExposureCount;
import edu.gemini.itc.shared.ImagingS2N;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.itc.shared.S2NMethod;
import edu.gemini.itc.shared.SourceDefinition;
import java.util.logging.Logger;

public final class ImagingS2NCalculationFactory {
    private static final Logger Log = Logger.getLogger(ImagingS2NCalculationFactory.class.getName());
    private ImagingS2NCalculationFactory() {}

    public static ImagingS2NCalculatable getCalculationInstance(final ObservationDetails obs, final Instrument instrument, final SourceFraction srcFrac, final double sed_integral, final double sky_integral) {

        if (obs.calculationMethod() instanceof S2NMethod) {
            return new ImagingS2NMethodACalculation(obs, instrument, srcFrac, sed_integral, sky_integral);
        } else {
            return new ImagingPointS2NMethodBCalculation(obs, instrument, srcFrac, sed_integral, sky_integral);
        }
    }

    public static ImagingS2NCalculatable getCalculationInstance(
            final SourceDefinition _sdParameters,
            ObservationDetails obs,
            final Instrument instrument,
            final SourceFraction srcFrac,
            final double im_qual,
            final double sed_integral,
            final double sky_integral) {
        Log.fine("calculationMethod = " + obs.calculationMethod().toString());

        if (obs.calculationMethod() instanceof ImagingS2N) {
            return new ImagingS2NMethodACalculation(obs, instrument, srcFrac, sed_integral, sky_integral);
        } else if (obs.calculationMethod() instanceof ImagingExposureCount) {
            return new ImagingPointS2NMethodBCalculation(obs, instrument, srcFrac, sed_integral, sky_integral);
        } else if (obs.calculationMethod() instanceof ImagingIntegrationTime) {
            return new ImagingMethodExptime(obs, instrument, srcFrac, sed_integral, sky_integral, _sdParameters, im_qual);
        } else {
            throw new Error("Invalid calculation method");
        }
    }

}
