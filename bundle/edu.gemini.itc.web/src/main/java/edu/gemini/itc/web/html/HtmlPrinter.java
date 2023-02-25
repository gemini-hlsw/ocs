package edu.gemini.itc.web.html;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.base.Instrument;
import edu.gemini.itc.base.TransmissionElement;
import edu.gemini.itc.gems.Gems;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.telescope.IssPort;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

/**
 * This is a temporary class that helps to collect output methods (print as html) up to the point where the code
 * is separated enough that this stuff can be moved to the web module.
 */
public final class HtmlPrinter {

    private static final Logger Log = Logger.getLogger(HtmlPrinter.class.getName());

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

    public static String printParameterSummary(final ObservingConditions ocp, final double wavelength, final double iqAtSource) {

        final double airmass = ocp.airmass();
        final double iqAtZenith = iqAtSource / Math.pow(airmass, 0.6);

        final Byte iq = ocp.javaIq().<Byte>biFold(e ->
                        iq2pct(wavelength, iqAtZenith),
                        SPSiteQuality.ImageQuality::getPercentage);

        final Byte cc = ocp.javaCc().<Byte>biFold(e ->
                        cc2pct(ocp.javaCc().toOptionLeft().getValue().toExtinction()),
                        SPSiteQuality.CloudCover::getPercentage);

        final StringBuilder sb = new StringBuilder();
        final String EXACT = "<span style=\"color:red;\">exact condition specified</span>";
        sb.append("Observing Conditions:");
        sb.append(String.format("<LI> Airmass: %.2f", airmass));

        ocp.javaIq().biForEach(
            exactIq -> sb.append(String.format("<LI> Image Quality: %d%% &nbsp; (&leq; %.2f\" at zenith, &leq; %.2f\" on-source, %s)", iq, iqAtZenith, iqAtSource, EXACT)),
            enumIq  -> sb.append(String.format("<LI> Image Quality: %d%% &nbsp; (&leq; %.2f\" at zenith, &leq; %.2f\" on-source)", iq, iqAtZenith, iqAtSource))
        );

        ocp.javaCc().biForEach(
            exactCc -> sb.append(String.format("<LI> Cloud Cover: %d%% &nbsp; (&leq; %.2f magnitudes, %s)", cc, exactCc.toExtinction(), EXACT)),
            enumCc  -> sb.append(String.format("<LI> Cloud cover: %d%%", cc))
        );

        sb.append(String.format("<LI> Water Vapor: %d%%", ocp.wv().getPercentage()));
        sb.append(String.format("<LI> Sky Background: %d%%", ocp.sb().getPercentage()));
        sb.append("<BR>");

        sb.append(String.format("<b>Likelihood of execution: %.0f%%</b><BR>",
            (iq / 100.0) *
            (cc / 100.0) *
            (ocp.wv().getPercentage() / 100.0) *
            (ocp.sb().getPercentage() / 100.0) * 100));

        return sb.toString();
    }

    // Convert wavelength (nanometers) and zenith IQ (arcseconds) to the corresponding legacy IQ bin percentile
    public static Byte iq2pct (final double wavelength, final double iqAtZenith) {
        Log.fine(String.format("Wavelength = %.3f nm", wavelength));
        Log.fine(String.format("Zenith IQ = %.3f arcsec", iqAtZenith));

        List<Integer> waveBands = Arrays.asList(350, 475, 630, 780, 900, 1020, 1200, 1650, 2200, 3400, 4800, 11700);

        byte[] percentile = new byte[] {20, 70, 85, 100};
        double[][] bins = new double[][] { // Zenith IQ bins
                {0.60, 0.90, 1.20, 2.0},   // u (0.350 µm)
                {0.60, 0.85, 1.10, 1.90},  // g (0.475 µm)
                {0.50, 0.75, 1.05, 1.80},  // r (0.630 µm)
                {0.50, 0.75, 1.05, 1.70},  // i (0.780 µm)
                {0.50, 0.70, 0.95, 1.70},  // Z (0.900 µm)
                {0.40, 0.70, 0.95, 1.65},  // Y (1.02 µm)
                {0.40, 0.60, 0.85, 1.55},  // J (1.2 µm)
                {0.40, 0.60, 0.85, 1.50},  // H (1.65µm)
                {0.35, 0.55, 0.80, 1.40},  // K (2.2 µm)
                {0.35, 0.50, 0.75, 1.25},  // L (3.4 µm)
                {0.35, 0.50, 0.70, 1.15},  // M (4.8 µm)
                {0.31, 0.37, 0.45, 0.75}}; // N' (11.7 µm)

        int closestWaveBand = waveBands.stream()
            .min(Comparator.comparingDouble(w -> Math.abs(w - wavelength)))
            .orElseThrow(() -> new NoSuchElementException("No wavelength available"));

        Log.fine(String.format("Closest waveband = %d nm", closestWaveBand));
        int idx = waveBands.indexOf(closestWaveBand);

        byte iqpct = 100;  // percentile when IQ > Any
        for (int i = 0; i < bins[idx].length; i++) {
            if (iqAtZenith <= bins[idx][i]) {
                iqpct = percentile[i];
                break;
            }
        }
        Log.fine("IQ Percentile = " + iqpct);
        return iqpct;
    }

    // Convert extinction in magnitudes to the corresponding legacy CC bin percentile
    public static Byte cc2pct (final double extinction) {
        Log.fine(String.format("Extinction = %.3f mag", extinction));
        byte[] percentile = new byte[] {50, 70, 80, 100};
        double[] bins_edge = new double[] {0.0, 0.3, 1.0, 3.0};
        byte ccpct = 100;  // when CC is > Any
        for (int i = 0; i < bins_edge.length; i++) {
            if (extinction <= bins_edge[i]) {
                ccpct = percentile[i];
                break;
            }
        }
        Log.fine("CC Percentile = " + ccpct);
        return ccpct;
    }

    public static String printParameterSummary(final ObservationDetails odp) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Calculation and analysis methods:\n");
        sb.append("<LI>Mode: ");
        sb.append((odp.calculationMethod() instanceof Imaging ? "imaging" : "spectroscopy"));
        sb.append("\n");
        sb.append("<LI>Calculation of ");
        if (odp.calculationMethod() instanceof S2NMethod) {
            sb.append(String.format("S/N ratio with %d", ((S2NMethod) odp.calculationMethod()).exposures()));
        } else if (odp.calculationMethod() instanceof IntMethod) {
            sb.append(String.format("integration time from a S/N ratio of %.2f for", ((ImagingInt) odp.calculationMethod()).sigma()));
        } else if (odp.calculationMethod() instanceof ExpMethod) {
            sb.append(String.format("exposure time for a S/N ratio of %.2f.", ((ImagingExp) odp.calculationMethod()).sigma()));
        } else if (odp.calculationMethod() instanceof SpectroscopyInt) {
            sb.append(String.format("exposure time and number of exposures for a S/N ratio of %.1f at wavelength %.2f nm.",
                    ((SpectroscopyInt) odp.calculationMethod()).sigma(), ((SpectroscopyInt) odp.calculationMethod()).wavelength()));
        } else {
            throw new Error("Unsupported calculation method");
        }
        if ( (odp.calculationMethod() instanceof S2NMethod) || (odp.calculationMethod() instanceof IntMethod)) {
            sb.append(String.format(" exposures of %.2f secs", odp.exposureTime()));
            if (odp.calculationMethod().coaddsOrElse(1) > 1) {
                sb.append(String.format(" and %d coadds", odp.calculationMethod().coaddsOrElse(1)));
            }
            sb.append(String.format(", and %.2f%% of them on source.\n", odp.sourceFraction() * 100));
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

        if (altair.getWFSMode().equals(AltairParams.GuideStarType.LGS)) sb.append("<LI>Laser Guide Star Mode selected");

        sb.append("<LI>Natural Guide Star Mode selected");
        sb.append("<LI>Guide Star Separation ");
        sb.append(altair.getGuideStarSeparation());
        sb.append("<LI>Guide Star Magnitude ");
        sb.append(altair.getGuideStarMagnitude());

        sb.append("<LI>Altair Field Lens position: ");
        sb.append(altair.getfieldlens());
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
