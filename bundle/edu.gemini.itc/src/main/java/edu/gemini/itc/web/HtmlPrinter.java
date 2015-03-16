package edu.gemini.itc.web;

import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.parameters.ObservingConditionParameters;
import edu.gemini.itc.parameters.PlottingDetailsParameters;
import edu.gemini.itc.parameters.TeleParameters;
import edu.gemini.itc.parameters.SourceDefinitionParameters;
import edu.gemini.itc.shared.FormatStringWriter;
import edu.gemini.spModel.telescope.IssPort;

/**
 * This is a temporary class that helps to collect output methods (print as html) up to the point where the code
 * is separated enough that this stuff can be moved to the web module.
 * TODO: Move this code to edu.gemini.itc.web once html output code has been removed from all recipes.
 */
public final class HtmlPrinter {

    private HtmlPrinter() {}

    public static String printParameterSummary(final SourceDefinitionParameters sdp) {
        StringBuffer sb = new StringBuffer();

        // This object is used to format numerical strings.
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(4);  // four decimal places
        device.clear();

        sb.append("Source spatial profile, brightness, and spectral distribution: \n");
        sb.append("  The z = ");
        sb.append(sdp.getRedshift());
        sb.append(" ");
        sb.append(sdp.getSourceGeometryStr());
        sb.append(" is a");
        switch (sdp.getDistributionType()) {
            case ELINE:
                sb.append("n emission line, at a wavelength of " + device.toString(sdp.getELineWavelength()));
                device.setPrecision(2);
                device.clear();
                sb.append(" microns, and with a width of " + device.toString(sdp.getELineWidth()) + " km/s.\n  It's total flux is " +
                        device.toString(sdp.getELineFlux()) + " " + sdp.getELineFluxUnits() + " on a flat continuum of flux density " +
                        device.toString(sdp.getELineContinuumFlux()) + " " + sdp.getELineContinuumFluxUnits() + ".");
                break;
            case BBODY:
                sb.append(" " + sdp.getBBTemp() + "K Blackbody, at " + sdp.getSourceNormalization() +
                        " " + sdp.profile.units().displayValue() + " in the " + sdp.getNormBand().name + " band.");
                break;
            case LIBRARY_STAR:
                sb.append(" " + sdp.getSourceNormalization() + " " + sdp.profile.units().displayValue() + " " + sdp.getSpecType() +
                        " star in the " + sdp.getNormBand().name + " band.");
                break;
            case LIBRARY_NON_STAR:
                sb.append(" " + sdp.getSourceNormalization() + " " + sdp.profile.units().displayValue() + " " + sdp.getSpecType() +
                        " in the " + sdp.getNormBand().name + " band.");
                break;
            case USER_DEFINED:
                sb.append(" a user defined spectrum with the name: " + sdp.getUserDefinedSpectrum());
                break;
            case PLAW:
                sb.append(" Power Law Spectrum, with an index of " + sdp.getPowerLawIndex()
                        + " and " + sdp.getSourceNormalization() + " mag in the " + sdp.getNormBand().name + " band.");
                break;
        }
        sb.append("\n");
        return sb.toString();

    }

    public static String printParameterSummary(final ObservingConditionParameters ocp) {
        StringBuffer sb = new StringBuffer();

        // This object is used to format numerical strings.
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2);  // Two decimal places
        device.clear();


        sb.append("Observing Conditions:");
        sb.append("<LI> Image Quality: " + device.toString(ocp.getImageQualityPercentile() * 100) + "%");
        sb.append("<LI> Sky Transparency (cloud cover): " + device.toString(ocp.getSkyTransparencyCloudPercentile() * 100) + "%");
        sb.append("<LI> Sky transparency (water vapour): " + device.toString(ocp.getSkyTransparencyWaterPercentile() * 100) + "%");
        sb.append("<LI> Sky background: " + device.toString(ocp.getSkyBackgroundPercentile() * 100) + "%");
        sb.append("<LI> Airmass: " + device.toString(ocp.getAirmass()));
        sb.append("<BR>");

        sb.append("Frequency of occurrence of these conditions: " +
                        device.toString(ocp.getImageQualityPercentile() *
                                ocp.getSkyTransparencyCloudPercentile() *
                                ocp.getSkyTransparencyWaterPercentile() *
                                ocp.getSkyBackgroundPercentile() * 100)
                        + "%<BR>"
        );

        return sb.toString();
    }

    public static String printParameterSummary(final ObservationDetailsParameters odp) {
        StringBuffer sb = new StringBuffer();

        // This object is used to format numerical strings.
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2);  // Two decimal places
        device.clear();

        sb.append("Calculation and analysis methods:\n");
        sb.append("<LI>mode: " + (odp.getMethod().isImaging() ? "imaging" : "spectroscopy") + "\n");
        sb.append("<LI>Calculation of ");
        if (odp.getMethod().isS2N()) {
            sb.append("S/N ratio with " + odp.getNumExposures() + " exposures of " + device.toString(odp.getExposureTime()) + " secs,");
            sb.append(" and " + device.toString(odp.getSourceFraction() * 100) + " % of them were on source.\n");
        } else {
            sb.append("integration time from a S/N ratio of " + device.toString(odp.getSNRatio()) + " for exposures of");
            sb.append(" " + device.toString(odp.getExposureTime()) + " with " + device.toString(odp.getSourceFraction() * 100) + " % of them were on source.\n");
        }
        sb.append("<LI>Analysis performed for aperture ");
        if (odp.isAutoAperture()) {
            sb.append("that gives 'optimum' S/N ");
        } else {
            sb.append("of diameter " + device.toString(odp.getApertureDiameter()) + " ");
        }
        sb.append("and a sky aperture that is " + device.toString(odp.getSkyApertureDiameter()) + " times the target aperture.\n");

        return sb.toString();
    }

    public static String printParameterSummary(final PlottingDetailsParameters pdp) {
        StringBuffer sb = new StringBuffer();

        // This object is used to format numerical strings.
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2);  // Two decimal places
        device.clear();

        sb.append("Output:\n<LI>Spectra ");
        switch (pdp.getPlotLimits()) {
            case AUTO: sb.append("autoscaled."); break;
            default:   sb.append("plotted over range " + pdp.getPlotWaveL() + " - " + pdp.getPlotWaveU());
        }
        sb.append("\n");
        return sb.toString();
    }

    public static String printParameterSummary(final TeleParameters pdp) {
        return printParameterSummary(pdp, pdp.getWFS().displayValue());
    }

    public static String printParameterSummary(final TeleParameters pdp, final String wfs) {
        StringBuffer sb = new StringBuffer();
        sb.append("Telescope configuration: \n");
        sb.append("<LI>" + pdp.getMirrorCoating().displayValue() + " mirror coating.\n");
        sb.append("<LI>" + portToString(pdp.getInstrumentPort()) + " looking port.\n");
        sb.append("<LI>wavefront sensor: " + wfs + "\n");
        return sb.toString();
    }

    // compatibility for regression testing, can go away after regression tests have passed
    private static String portToString(final IssPort port) {
        switch (port) {
            case SIDE_LOOKING:  return "side";
            case UP_LOOKING:    return "up";
            default:            throw new IllegalArgumentException("unknown port");
        }
    }




}
