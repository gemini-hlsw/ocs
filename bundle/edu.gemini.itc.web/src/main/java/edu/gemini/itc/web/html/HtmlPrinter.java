package edu.gemini.itc.web.html;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.base.TransmissionElement;
import edu.gemini.itc.gems.Gems;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.telescope.IssPort;

/**
 * This is a temporary class that helps to collect output methods (print as html) up to the point where the code
 * is separated enough that this stuff can be moved to the web module.
 */
public final class HtmlPrinter {

    private HtmlPrinter() {}

    public static String printParameterSummary(final SourceDefinition sdp) {
        final StringBuilder sb = new StringBuilder();

        sb.append("Source spatial profile, brightness, and spectral distribution: \n");
        sb.append("  The z = ");
        sb.append(String.format("%.5f", sdp.redshift().z()));
        sb.append(" ");
        sb.append(HtmlUtil.sourceProfileString(sdp.profile()));
        sb.append(" is a");
        sb.append(HtmlUtil.sourceDistributionString(sdp));
        sb.append("\n");
        return sb.toString();

    }

    public static String printParameterSummary(final ObservingConditions ocp) {
        final StringBuilder sb = new StringBuilder();

        sb.append("Observing Conditions:");
        sb.append(String.format("<LI> Image Quality: %d.00%%", ocp.iq().getPercentage()));
        sb.append(String.format("<LI> Sky Transparency (cloud cover): %d.00%%", ocp.cc().getPercentage()));
        sb.append(String.format("<LI> Sky transparency (water vapour): %d.00%%", ocp.wv().getPercentage()));
        sb.append(String.format("<LI> Sky background: %d.00%%", ocp.sb().getPercentage()));
        sb.append(String.format("<LI> Airmass: %.2f", ocp.airmass()));
        sb.append("<BR>");

        sb.append(String.format("<b>Likelihood of execution:</b> %.2f%%<BR>",
                (ocp.iq().getPercentage()/100.0) *
                (ocp.cc().getPercentage()/100.0) *
                (ocp.wv().getPercentage()/100.0) *
                (ocp.sb().getPercentage()/100.0) * 100));

        return sb.toString();
    }

    public static String printParameterSummary(final ObservationDetails odp) {
        final StringBuilder sb = new StringBuilder();

        sb.append("Calculation and analysis methods:\n");
        sb.append("<LI>mode: ");
        sb.append((odp.calculationMethod() instanceof Imaging ? "imaging" : "spectroscopy"));
        sb.append("\n");
        sb.append("<LI>Calculation of ");
        if (odp.calculationMethod() instanceof S2NMethod) {
            sb.append(String.format("S/N ratio with " + ((S2NMethod) odp.calculationMethod()).exposures() + " exposures of %.2f secs,", odp.exposureTime()));
            sb.append(String.format(" and %.2f %% of them were on source.\n", odp.sourceFraction() * 100));
        } else {
            sb.append(String.format("integration time from a S/N ratio of %.2f for exposures of", ((ImagingInt) odp.calculationMethod()).sigma()));
            sb.append(String.format(" %.2f with %.2f %% of them were on source.\n", odp.exposureTime(), odp.sourceFraction() * 100));
        }
        sb.append("<LI>Analysis performed for aperture ");
        if (odp.analysisMethod() instanceof AutoAperture) {
            sb.append("that gives 'optimum' S/N ");
            sb.append(String.format("and a sky aperture that is %.2f times the target aperture.\n", ((AutoAperture) odp.analysisMethod()).skyAperture()));
        } else if (odp.analysisMethod() instanceof UserAperture) {
            sb.append(String.format("of diameter %.2f ", ((UserAperture) odp.analysisMethod()).diameter()));
            sb.append(String.format("and a sky aperture that is %.2f times the target aperture.\n", ((UserAperture) odp.analysisMethod()).skyAperture()));
        } else if (odp.analysisMethod() instanceof IfuMethod) {
            sb.append("that gives 'optimum' S/N ");
            sb.append(String.format("and %d fibres on sky.\n", ((IfuMethod) odp.analysisMethod()).skyFibres()));
        } else {
            throw new Error("Unsupported analysis method");
        }

        return sb.toString();
    }

    public static String printParameterSummary(final PlottingDetails pdp) {
        final StringBuilder sb = new StringBuilder();

        sb.append("Output:\n<LI>Spectra ");
        switch (pdp.getPlotLimits()) {
            case AUTO: sb.append("autoscaled."); break;
            case USER: sb.append("plotted over range " + pdp.getPlotWaveL()*1000 + " - " + pdp.getPlotWaveU()*1000); break;
            default:   throw new Error();
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

    private static String portToString(final IssPort port) {
        switch (port) {
            case SIDE_LOOKING:  return "side";
            case UP_LOOKING:    return "up";
            default:            throw new IllegalArgumentException("unknown port");
        }
    }

}
