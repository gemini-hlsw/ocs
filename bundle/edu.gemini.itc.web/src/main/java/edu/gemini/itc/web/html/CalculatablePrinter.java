package edu.gemini.itc.web.html;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.itc.shared.S2NMethod;
import edu.gemini.shared.util.immutable.*;
/*
 * A helper class that collects some printing methods that used to be defined on the calculatable objects directly
 * but had to be moved somewhere else in order to be able to separate the calculation from the presentation logic.
 */
public final class CalculatablePrinter {

    // === Image Quality

    public static String getTextResult(final ImageQualityCalculatable iq) {
        if (iq instanceof GaussianImageQualityCalculation)  return getTextResult((GaussianImageQualityCalculation)iq);
        if (iq instanceof ImageQualityCalculation)          return getTextResult((ImageQualityCalculation)iq);
        throw new Error();
    }

    private static String getTextResult(final GaussianImageQualityCalculation iq) {
        return String.format("derived image size for Gaussian source = %.2f\n", iq.getImageQuality());
    }

    private static String getTextResult(final ImageQualityCalculation iq) {
        return String.format("derived image size (FWHM) for a point source = %.2f arcsec.\n", iq.getImageQuality());
    }


    // === Source Fraction

    public static String getTextResult(final SourceFraction sf) {
        return getTextResult(sf, true);
    }

    public static String getTextResult(final SourceFraction sf, final boolean sfPrint) {
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("software aperture diameter = %.2f arcsec\n", sf.getSoftwareAperture()));
        if (sf instanceof PointSourceFraction && sfPrint) {
            sb.append(String.format("fraction of source flux in aperture = %.2f\n", sf.getSourceFraction()));
        }
        sb.append(String.format("enclosed pixels = %.2f\n", sf.getNPix()));
        return sb.toString();
    }

    // === Imaging S2N

    public static String getTextResult(final ImagingS2NCalculatable s2n, final ObservationDetails obs) {
        if      (s2n instanceof ImagingS2NMethodACalculation)      return getTextResult(obs, (ImagingS2NMethodACalculation) s2n);
        else if (s2n instanceof ImagingPointS2NMethodBCalculation) return getTextResult(obs, (ImagingPointS2NMethodBCalculation) s2n);
        else throw new Error();
    }

    private static String getTextResult(final ObservationDetails obs, final ImagingPointS2NMethodBCalculation s2n) {
        final StringBuilder sb = new StringBuilder(CalculatablePrinter.getTextResult(s2n));

        sb.append(String.format("Derived number of exposures = %.0f", s2n.reqNumberExposures()));

        if (obs.calculationMethod().coaddsOrElse(1) > 1) {
            sb.append(String.format(" each having %d coadds", obs.calculationMethod().coaddsOrElse(1)));
        }

        sb.append(String.format(", of which %.0f", s2n.reqNumberExposures() * obs.sourceFraction()));
        if (s2n.reqNumberExposures() == 1)
            sb.append(" is on source.\n");
        else
            sb.append(" are on source.\n");

        sb.append(String.format("Taking %.0f", Math.ceil(s2n.reqNumberExposures())));
        if (Math.ceil(s2n.reqNumberExposures()) == 1)
            sb.append(" exposure");
        else
            sb.append(" exposures");
        sb.append(", the effective S/N for the whole observation is ");

        sb.append(String.format("%.2f (including sky subtraction)\n\n", s2n.totalSNRatio()));

        final double totReqTime = s2n.reqNumberExposures() * obs.exposureTime() * obs.calculationMethod().coaddsOrElse(1);
        final double srcReqTime = totReqTime * obs.sourceFraction();
        sb.append(String.format("Required total integration time is %.2f secs, of which %.2f secs is on source.\n", totReqTime, srcReqTime));

        return sb.toString();
    }

    private static String getTextResult(final ObservationDetails obs, final ImagingS2NMethodACalculation s2n) {
        if (obs.calculationMethod() instanceof S2NMethod) {
            final double totExpTime = obs.exposureTime() * ((S2NMethod) obs.calculationMethod()).exposures() * obs.calculationMethod().coaddsOrElse(1);
            final double srcFrcTime = totExpTime * obs.sourceFraction();
            return
                    CalculatablePrinter.getTextResult(s2n) +
                            String.format("Intermediate S/N for one exposure = %.2f\n\n", s2n.singleSNRatio()) +
                            String.format("S/N for the whole observation = %.2f (including sky subtraction)\n\n", s2n.totalSNRatio()) +
                            String.format("Requested total integration time = %.2f secs, of which %.2f secs is on source.\n", totExpTime, srcFrcTime);
        } else {
            throw new Error("Unsupported calculation method");
        }
    }


    private static String getTextResult(final ImagingS2NCalculation s2n) {
        return
            "Contributions to total noise (e-) in aperture (per exposure):\n" +
            String.format("Source noise = %.2f\n", Math.sqrt(s2n.getVarSource())) +
            String.format("Background noise = %.2f\n", Math.sqrt(s2n.getVarBackground())) +
            String.format("Dark current noise = %.2f\n", Math.sqrt(s2n.getVarDark())) +
            String.format("Readout noise = %.2f\n\n", Math.sqrt(s2n.getVarReadout())) +
            String.format("Total noise per exposure = %.2f\n", s2n.getNoise()) +
            String.format("Total signal per exposure = %.2f\n\n", s2n.getSignal());
    }

    public static String getBackgroundLimitResult(final ImagingS2NCalculatable s2n) {
        if (Math.sqrt(s2n.getVarSource() + s2n.getVarDark() + s2n.getVarReadout()) > Math.sqrt(s2n.getVarBackground()))
            return "Warning: observation is NOT background noise limited";
        else return "Observation is background noise limited.";
    }

    public static Option<String> ReadNoiseLimitedWarning(final ImagingS2NCalculatable s2n) {
    return ( s2n.getVarReadout() > s2n.getVarSource() + s2n.getVarBackground() + s2n.getVarDark() ) ?
        ImOption.apply("Warning: observation is read noise limited; consider using a longer exposure time or a lower noise read mode.\n") :
        ImOption.<String>empty();
    }

}
