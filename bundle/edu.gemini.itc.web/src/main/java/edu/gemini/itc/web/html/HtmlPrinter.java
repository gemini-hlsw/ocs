package edu.gemini.itc.web.html;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.base.TransmissionElement;
import edu.gemini.itc.gems.Gems;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.target.Library;
import edu.gemini.spModel.telescope.IssPort;

/**
 * This is a temporary class that helps to collect output methods (print as html) up to the point where the code
 * is separated enough that this stuff can be moved to the web module.
 * TODO: Move this code to edu.gemini.itc.web once html output code has been removed from all recipes.
 */
public final class HtmlPrinter {

    private HtmlPrinter() {}

    public static String printParameterSummary(final SourceDefinition sdp) {
        final StringBuilder sb = new StringBuilder();

        sb.append("Source spatial profile, brightness, and spectral distribution: \n");
        sb.append("  The z = ");
        sb.append(sdp.getRedshift());
        sb.append(" ");
        sb.append(sdp.getSourceGeometryStr());
        sb.append(" is a");
        switch (sdp.getDistributionType()) {
            case ELINE:
                sb.append(String.format("n emission line, at a wavelength of %.4f microns, ", sdp.getELineWavelength().toMicrons()));
                sb.append(String.format(
                        "and with a width of %.2f km/s.\n  It's total flux is %.3e watts_flux on a flat continuum of flux density %.3e watts_fd_wavelength.",
                        sdp.getELineWidth(), sdp.getELineFlux().toWatts(), sdp.getELineContinuumFlux().toWatts()));
                break;
            case BBODY:
                sb.append(" " + sdp.getBBTemp() + "K Blackbody, at " + sdp.getSourceNormalization() +
                        " " + sdp.units.displayValue() + " in the " + sdp.getNormBand().name + " band.");
                break;
            case LIBRARY_STAR:
                sb.append(" " + sdp.getSourceNormalization() + " " + sdp.units.displayValue() + " " + ((Library) sdp.distribution).sedSpectrum() +
                        " star in the " + sdp.getNormBand().name + " band.");
                break;
            case LIBRARY_NON_STAR:
                sb.append(" " + sdp.getSourceNormalization() + " " + sdp.units.displayValue() + " " + ((Library) sdp.distribution).sedSpectrum() +
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

    public static String printParameterSummary(final ObservingConditions ocp) {
        final StringBuilder sb = new StringBuilder();

        sb.append("Observing Conditions:");
        sb.append(String.format("<LI> Image Quality: %.2f%%", ocp.getImageQualityPercentile() * 100));
        sb.append(String.format("<LI> Sky Transparency (cloud cover): %.2f%%", ocp.getSkyTransparencyCloudPercentile() * 100));
        sb.append(String.format("<LI> Sky transparency (water vapour): %.2f%%", ocp.getSkyTransparencyWaterPercentile() * 100));
        sb.append(String.format("<LI> Sky background: %.2f%%", ocp.getSkyBackgroundPercentile() * 100));
        sb.append(String.format("<LI> Airmass: %.2f", ocp.getAirmass()));
        sb.append("<BR>");

        sb.append(String.format("Frequency of occurrence of these conditions: %.2f%%<BR>",
                ocp.getImageQualityPercentile() *
                ocp.getSkyTransparencyCloudPercentile() *
                ocp.getSkyTransparencyWaterPercentile() *
                ocp.getSkyBackgroundPercentile() * 100));

        return sb.toString();
    }

    public static String printParameterSummary(final ObservationDetails odp) {
        final StringBuilder sb = new StringBuilder();

        sb.append("Calculation and analysis methods:\n");
        sb.append("<LI>mode: ");
        sb.append((odp.getMethod().isImaging() ? "imaging" : "spectroscopy"));
        sb.append("\n");
        sb.append("<LI>Calculation of ");
        if (odp.getMethod().isS2N()) {
            sb.append(String.format("S/N ratio with " + odp.getNumExposures() + " exposures of %.2f secs,", odp.getExposureTime()));
            sb.append(String.format(" and %.2f %% of them were on source.\n", odp.getSourceFraction() * 100));
        } else {
            sb.append(String.format("integration time from a S/N ratio of %.2f for exposures of", odp.getSNRatio()));
            sb.append(String.format(" %.2f with %.2f %% of them were on source.\n", odp.getExposureTime(), odp.getSourceFraction() * 100));
        }
        sb.append("<LI>Analysis performed for aperture ");
        if (odp.isAutoAperture()) {
            sb.append("that gives 'optimum' S/N ");
        } else {
            sb.append(String.format("of diameter %.2f ", odp.getApertureDiameter()));
        }
        sb.append(String.format("and a sky aperture that is %.2f times the target aperture.\n", odp.getSkyApertureDiameter()));

        return sb.toString();
    }

    public static String printParameterSummary(final PlottingDetails pdp) {
        final StringBuilder sb = new StringBuilder();

        sb.append("Output:\n<LI>Spectra ");
        switch (pdp.getPlotLimits()) {
            case AUTO: sb.append("autoscaled."); break;
            default:   sb.append("plotted over range " + pdp.getPlotWaveL()*1000 + " - " + pdp.getPlotWaveU()*1000);
        }
        sb.append("\n");
        return sb.toString();
    }

    public static String printParameterSummary(final TelescopeDetails pdp) {
        return printParameterSummary(pdp, pdp.getWFS().name().toLowerCase());
    }

    public static String printParameterSummary(final TelescopeDetails pdp, final String wfs) {
        return "Telescope configuration: \n" +
            "<LI>" + pdp.getMirrorCoating().displayValue() + " mirror coating.\n" +
            "<LI>" + portToString(pdp.getInstrumentPort()) + " looking port.\n" +
            "<LI>wavefront sensor: " + wfs + "\n";
    }

    public static String printSummary(final Altair altair) {
        return String.format(
            "r0(%.0fnm) = %.3f m\n" +
            "Strehl = %.3f\n"+
            "FWHM of an AO-corrected core = %.3f arcsec\n",
            altair.getWavelength(), altair.getr0(), altair.getStrehl(), altair.getAOCorrectedFWHM());
    }

    public static String printParameterSummary(final Altair altair) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Altair Guide Star properties:");

        if (altair.getWFSMode().equals(AltairParams.GuideStarType.LGS)) {
            sb.append("<LI>Laser Guide Star Mode");

        } else {
            sb.append("<LI>Natural Guide Star Mode");
            sb.append("<LI>Guide Star Seperation ");
            sb.append(altair.getGuideStarSeparation());
            sb.append("<LI>Guide Star Magnitude ");
            sb.append(altair.getGuideStarMagnitude());
        }

        sb.append("<BR>");
        return sb.toString();
    }

    public static String printSummary(final Gems gems) {
        String s = String.format("r0(" + gems.getWavelength() + "nm) = %.3f m\n", gems.getr0());
        s += String.format("Average Strehl = %.3f%%\n", gems.getAvgStrehl() * 100);
        s += "FWHM of an AO-corrected core = ";
        try {
            s += String.format("%.3f arcsec\n", gems.getAOCorrectedFWHM(true));
        } catch (IllegalArgumentException ex) {
            s += "<span style=\"color:red; font-style:italic;\">Error: " + ex.getMessage() + "</span>\n";
        }

        return s;
    }

    public static String printParameterSummary(final Gems gems) {
        return
            "Average Strehl:\t" + gems.getAvgStrehl() + "\n" +
            "Strehl Band:\t" + gems.getStrehlBand() + "\n\n";
    }

    public static String opticalComponentsToString(final Instrument instrument) {
        String s = "Optical Components: <BR>";
        for (final TransmissionElement te : instrument.getComponents()) {
            s += "<LI>" + te.toString() + "<BR>";
        }
        return s;
    }

    // TODO: compatibility for regression testing, can go away after regression tests have passed
    private static String portToString(final IssPort port) {
        switch (port) {
            case SIDE_LOOKING:  return "side";
            case UP_LOOKING:    return "up";
            default:            throw new IllegalArgumentException("unknown port");
        }
    }

}
