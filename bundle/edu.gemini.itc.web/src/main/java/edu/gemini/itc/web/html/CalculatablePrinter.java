package edu.gemini.itc.web.html;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.ObservationDetails;

/*
 * A helper class that collects some printing methods that used to be defined on the calculatable objects directly
 * but had to be moved somewhere else in order to be able to separate the calculation from the presentation logic.
 */
public final class CalculatablePrinter {

    // === Image Quality

    public static String getTextResult(final ImageQualityCalculatable iq, final FormatStringWriter device) {
        if (iq instanceof GaussianImageQualityCalculation)  return getTextResult((GaussianImageQualityCalculation)iq, device);
        if (iq instanceof ImageQualityCalculation)          return getTextResult((ImageQualityCalculation)iq, device);
        throw new Error();
    }

    private static String getTextResult(final GaussianImageQualityCalculation iq, final FormatStringWriter device) {
        return "derived image size for source = " + device.toString(iq.getImageQuality()) + "\n";
    }

    private static String getTextResult(final ImageQualityCalculation iq, final FormatStringWriter device) {
        return "derived image size (FWHM) for a point source = " + device.toString(iq.getImageQuality()) + " arcsec.\n";
    }


    // === Source Fraction

    public static String getTextResult(final SourceFraction sf, final FormatStringWriter device) {
        return getTextResult(sf, device, true);
    }

    public static String getTextResult(final SourceFraction sf, final FormatStringWriter device, final boolean sfPrint) {
        StringBuffer sb = new StringBuffer();
        sb.append("software aperture diameter = " + device.toString(sf.getSoftwareAperture()) + " arcsec\n");
        if (sf instanceof PointSourceFraction && sfPrint) {
            sb.append("fraction of source flux in aperture = " + device.toString(sf.getSourceFraction()) + "\n");
        }
        sb.append("enclosed pixels = " + device.toString(sf.getNPix()) + "\n");
        return sb.toString();
    }

    // === Imaging S2N

    public static String getTextResult(final ImagingS2NCalculatable s2n, final ObservationDetails obs, final FormatStringWriter device) {
        if (s2n instanceof ImagingS2NMethodACalculation) return getTextResult(obs, (ImagingS2NMethodACalculation) s2n, device);
        else if (s2n instanceof ImagingPointS2NMethodBCalculation) return getTextResult(obs, (ImagingPointS2NMethodBCalculation) s2n, device);
        else throw new Error();
    }

    private static String getTextResult(final ObservationDetails obs, final ImagingPointS2NMethodBCalculation s2n, final FormatStringWriter device) {
        StringBuffer sb = new StringBuffer(CalculatablePrinter.getTextResult(s2n, device));
        device.setPrecision(0);
        device.clear();

        sb.append("Derived number of exposures = " +
                device.toString(s2n.reqNumberExposures()) +
                " , of which " + device.toString(s2n.reqNumberExposures()
                * obs.getSourceFraction()));
        if (s2n.reqNumberExposures() == 1)
            sb.append(" is on source.\n");
        else
            sb.append(" are on source.\n");

        sb.append("Taking " +
                device.toString(Math.ceil(s2n.reqNumberExposures())));
        if (Math.ceil(s2n.reqNumberExposures()) == 1)
            sb.append(" exposure");
        else
            sb.append(" exposures");
        sb.append(", the effective S/N for the whole" +
                " observation is ");

        device.setPrecision(2);
        device.clear();

        sb.append(device.toString(s2n.effectiveS2N()) +
                " (including sky subtraction)\n\n");


        sb.append("Required total integration time is " +
                device.toString(s2n.reqNumberExposures() *
                        obs.getExposureTime()) +
                " secs, of which " +
                device.toString(s2n.reqNumberExposures() *
                        obs.getExposureTime() *
                        obs.getSourceFraction())
                + " secs is on source.\n");

        return sb.toString();
    }

    private static String getTextResult(final ObservationDetails obs, final ImagingS2NMethodACalculation s2n, final FormatStringWriter device) {
        StringBuffer sb = new StringBuffer(CalculatablePrinter.getTextResult(s2n, device));
        sb.append("Intermediate S/N for one exposure = " + device.toString(s2n.singleSNRatio()) + "\n\n");
        sb.append("S/N for the whole observation = " + device.toString(s2n.totalSNRatio()) + " (including sky subtraction)\n\n");
        sb.append("Requested total integration time = " +
                device.toString(obs.getExposureTime() * obs.getNumExposures()) +
                " secs, of which " + device.toString(obs.getExposureTime() *
                obs.getNumExposures() *
                obs.getSourceFraction()) +
                " secs is on source.\n");
        return sb.toString();
    }


    public static String getTextResult(final ImagingS2NCalculation s2n, final FormatStringWriter device) {
        StringBuffer sb = new StringBuffer();
        sb.append("Contributions to total noise (e-) in aperture (per exposure):\n");
        sb.append("Source noise = " + device.toString(Math.sqrt(s2n.getVarSource())) + "\n");
        sb.append("Background noise = " + device.toString(Math.sqrt(s2n.getVarBackground())) + "\n");
        sb.append("Dark current noise = " + device.toString(Math.sqrt(s2n.getVarDark())) + "\n");
        sb.append("Readout noise = " + device.toString(Math.sqrt(s2n.getVarReadout())) + "\n\n");
        sb.append("Total noise per exposure = " + device.toString(s2n.getNoise()) + "\n");
        sb.append("Total signal per exposure = " + device.toString(s2n.getSignal()) + "\n\n");
        return sb.toString();
    }

    public static String getBackgroundLimitResult(final ImagingS2NCalculatable s2n) {
        if (Math.sqrt(s2n.getVarSource() + s2n.getVarDark() + s2n.getVarReadout()) > Math.sqrt(s2n.getVarBackground()))
            return "Warning: observation is NOT background noise limited";
        else return "Observation is background noise limited.";
    }

}
