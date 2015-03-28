package edu.gemini.itc.gnirs;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.HtmlPrinter;
import edu.gemini.itc.web.ITCRequest;
import edu.gemini.spModel.core.Site;
import org.jfree.chart.ChartColor;

import java.awt.*;
import java.io.PrintWriter;
import java.util.Calendar;

/**
 * This class performs the calculations for Gnirs used for imaging.
 */
public final class GnirsRecipe extends RecipeBase {

    private static final int ORDERS = 6;
    private static final Color[] ORDER_COLORS =
            new Color[] {ChartColor.DARK_RED,           ChartColor.DARK_BLUE,       ChartColor.DARK_GREEN,
                         ChartColor.DARK_MAGENTA,       ChartColor.black,           ChartColor.DARK_CYAN};
    private static final Color[] ORDER_BG_COLORS =
            new Color[] {ChartColor.VERY_LIGHT_RED,     ChartColor.VERY_LIGHT_BLUE, ChartColor.VERY_LIGHT_GREEN,
                         ChartColor.VERY_LIGHT_MAGENTA, ChartColor.lightGray,       ChartColor.VERY_LIGHT_CYAN};

    private final Calendar now = Calendar.getInstance();
    private final String _header = "# GNIRS ITC: " + now.getTime() + "\n";

    // Parameters from the web page.
    private final SourceDefinition _sdParameters;
    private final ObservationDetails _obsDetailParameters;
    private final ObservingConditions _obsConditionParameters;
    private final GnirsParameters _gnirsParameters;
    private final TelescopeDetails _telescope;
    private final PlottingDetails _plotParameters;

    // TODO: these values are needed for the web output, add them to SpectroscopyResult?
    private VisitableSampledSpectrum[] signalOrder;
    private VisitableSampledSpectrum[] backGroundOrder;
    private VisitableSampledSpectrum[] finalS2NOrder;

    /**
     * Constructs a GnirsRecipe by parsing a Multipart servlet request.
     *
     * @param r   Servlet request containing form data from ITC web page.
     * @param out Results will be written to this PrintWriter.
     * @throws Exception on failure to parse parameters.
     */
    public GnirsRecipe(final ITCMultiPartParser r, final PrintWriter out) {
        super(out);
        // Read parameters from the four main sections of the web page.
        _sdParameters = ITCRequest.sourceDefinitionParameters(r);
        _obsDetailParameters = ITCRequest.observationParameters(r);
        _obsConditionParameters = ITCRequest.obsConditionParameters(r);
        _gnirsParameters = new GnirsParameters(r);
        _telescope = ITCRequest.teleParameters(r);
        _plotParameters = ITCRequest.plotParamters(r);

        signalOrder = new VisitableSampledSpectrum[ORDERS];
        backGroundOrder = new VisitableSampledSpectrum[ORDERS];
        finalS2NOrder = new VisitableSampledSpectrum[ORDERS];

        validateInputParameters();
    }

    /**
     * Constructs a GnirsRecipe given the parameters. Useful for testing.
     */
    public GnirsRecipe(final SourceDefinition sdParameters,
                       final ObservationDetails obsDetailParameters,
                       final ObservingConditions obsConditionParameters,
                       final GnirsParameters gnirsParameters, TelescopeDetails telescope,
                       final PlottingDetails plotParameters,
                       final PrintWriter out)

    {
        super(out);
        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _gnirsParameters = gnirsParameters;
        _telescope = telescope;
        _plotParameters = plotParameters;

        signalOrder = new VisitableSampledSpectrum[ORDERS];
        backGroundOrder = new VisitableSampledSpectrum[ORDERS];
        finalS2NOrder = new VisitableSampledSpectrum[ORDERS];

        validateInputParameters();
    }

    private void validateInputParameters() {
        if (_sdParameters.getDistributionType().equals(SourceDefinition.Distribution.ELINE))
            // *25 b/c of increased resolutuion of transmission files
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters.getELineWavelength() * 1000 * 25))) {
                throw new RuntimeException(
                        "Please use a model line width > 0.04 nm (or "
                                + (3E5 / (_sdParameters.getELineWavelength() * 1000 * 25))
                                + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }

        // report error if this does not come out to be an integer
        checkSourceFraction(_obsDetailParameters.getNumExposures(), _obsDetailParameters.getSourceFraction());
    }

    /**
     * Performes recipe calculation and writes results to a cached PrintWriter
     * or to System.out.
     *
     * @throws Exception A recipe calculation can fail in many ways, missing data
     *                   files, incorrectly-formatted data files, ...
     */
    public void writeOutput() {
        final Gnirs instrument = new GnirsNorth(_gnirsParameters, _obsDetailParameters);
        final SpectroscopyResult result = calculateSpectroscopy(instrument);
        writeSpectroscopyOutput(instrument, result);
    }

    public SpectroscopyResult calculateSpectroscopy() {
        final Gnirs instrument = new GnirsNorth(_gnirsParameters, _obsDetailParameters);
        return calculateSpectroscopy(instrument);
    }

    private SpectroscopyResult calculateSpectroscopy(final Gnirs instrument) {
        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED

        final double pixel_size = instrument.getPixelSize();
        double ap_diam = 0;

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();


        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, Site.GN, ITCConstants.NEAR_IR, _sdParameters, _obsConditionParameters, _telescope, _plotParameters);
        final VisitableSampledSpectrum sed = calcSource.sed;
        final VisitableSampledSpectrum sky = calcSource.sky;

        // Calculate the Fraction of source in the aperture
        final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, IQcalc.getImageQuality());

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        final SlitThroughput st;
        if (!_obsDetailParameters.isAutoAperture()) {
            st = new SlitThroughput(IQcalc.getImageQuality(),
                    _obsDetailParameters.getApertureDiameter(),
                    pixel_size, _gnirsParameters.getFPMask());
        } else {
            st = new SlitThroughput(IQcalc.getImageQuality(), pixel_size, _gnirsParameters.getFPMask());
        }

        ap_diam = st.getSpatialPix(); // ap_diam really Spec_Npix on

        double spec_source_frac = st.getSlitThroughput();

        // For the usb case we want the resolution to be determined by the
        // slit width and not the image quality for a point source.
        final double im_qual;
        if (_sdParameters.isUniform()) {
            im_qual = 10000;
            if (_obsDetailParameters.isAutoAperture()) {
                ap_diam = new Double(1 / (_gnirsParameters.getFPMask() * pixel_size) + 0.5).intValue();
                spec_source_frac = 1;
            } else {
                spec_source_frac = _gnirsParameters.getFPMask() * ap_diam * pixel_size;
            }
        } else {
            im_qual = IQcalc.getImageQuality();
        }

        final SpecS2NLargeSlitVisitor specS2N = new SpecS2NLargeSlitVisitor(
                _gnirsParameters.getFPMask(), pixel_size,
                instrument.getSpectralPixelWidth() / instrument.getOrder(),
                instrument.getObservingStart(),
                instrument.getObservingEnd(),
                instrument.getGratingDispersion_nm(),
                instrument.getGratingDispersion_nmppix(),
                instrument.getGratingResolution(),
                spec_source_frac,
                im_qual, ap_diam,
                _obsDetailParameters.getNumExposures(),
                _obsDetailParameters.getSourceFraction(),
                _obsDetailParameters.getExposureTime(),
                instrument.getDarkCurrent(),
                instrument.getReadNoise(),
                _obsDetailParameters.getSkyApertureDiameter());

        specS2N.setDetectorTransmission(instrument.getDetectorTransmision());

        if (instrument.XDisp_IsUsed()) {
            final VisitableSampledSpectrum[] sedOrder = new VisitableSampledSpectrum[6];
            for (int i = 0; i < ORDERS; i++) {
                sedOrder[i] = (VisitableSampledSpectrum) sed.clone();
            }

            final VisitableSampledSpectrum[] skyOrder = new VisitableSampledSpectrum[6];
            for (int i = 0; i < ORDERS; i++) {
                skyOrder[i] = (VisitableSampledSpectrum) sky.clone();
            }

            final double trimCenter;
            if (instrument.getGrating().equals(GnirsParameters.G110)) {
                trimCenter = _gnirsParameters.getUnXDispCentralWavelength();
            } else {
                trimCenter = 2200.0;
            }

            for (int i = 0; i < ORDERS; i++) {
                final int order = i + 3;
                final double d         = instrument.getGratingDispersion_nmppix() / order * Gnirs.DETECTOR_PIXELS / 2;
                final double trimStart = trimCenter * 3 / order - d;
                final double trimEnd   = trimCenter * 3 / order + d;

                sedOrder[i].accept(instrument.getGratingOrderNTransmission(order));
                sedOrder[i].trim(trimStart, trimEnd);

                skyOrder[i].accept(instrument.getGratingOrderNTransmission(order));
                skyOrder[i].trim(trimStart, trimEnd);
            }

            for (int i = 0; i < ORDERS; i++) {
                final int order = i + 3;
                specS2N.setSourceSpectrum(sedOrder[i]);
                specS2N.setBackgroundSpectrum(skyOrder[i]);

                specS2N.setGratingDispersion_nmppix(instrument.getGratingDispersion_nmppix() / order);
                specS2N.setGratingDispersion_nm(instrument.getGratingDispersion_nm() / order);
                specS2N.setSpectralPixelWidth(instrument.getSpectralPixelWidth() / order);

                specS2N.setStartWavelength(sedOrder[i].getStart());
                specS2N.setEndWavelength(sedOrder[i].getEnd());

                sed.accept(specS2N);

                signalOrder[i] = (VisitableSampledSpectrum) specS2N.getSignalSpectrum().clone();
                backGroundOrder[i] = (VisitableSampledSpectrum) specS2N.getBackgroundSpectrum().clone();
            }

            for (int i = 0; i < ORDERS; i++) {
                final int order = i + 3;
                specS2N.setSourceSpectrum(sedOrder[i]);
                specS2N.setBackgroundSpectrum(skyOrder[i]);

                specS2N.setGratingDispersion_nmppix(instrument.getGratingDispersion_nmppix() / order);
                specS2N.setGratingDispersion_nm(instrument.getGratingDispersion_nm() / order);
                specS2N.setSpectralPixelWidth(instrument.getSpectralPixelWidth() / order);

                specS2N.setStartWavelength(sedOrder[i].getStart());
                specS2N.setEndWavelength(sedOrder[i].getEnd());

                sed.accept(specS2N);

                finalS2NOrder[i] = (VisitableSampledSpectrum) specS2N.getFinalS2NSpectrum().clone();
            }

        } else {

            sed.accept(instrument.getGratingOrderNTransmission(instrument.getOrder()));

            specS2N.setSourceSpectrum(sed);
            specS2N.setBackgroundSpectrum(sky);
            specS2N.setHaloImageQuality(0.0);
            specS2N.setSpecHaloSourceFraction(0.0);

            sed.accept(specS2N);

        }

        final SpecS2N[] specS2Narr = new SpecS2N[] {specS2N};
        return new SpectroscopyResult(SFcalc, IQcalc, specS2Narr, st);

    }


    // ===================================================================================================================
    // TODO: OUTPUT METHODS
    // TODO: These need to be simplified/cleaned/shared and then go to the web module.. and then be deleted and forgotten.
    // ===================================================================================================================


    private void writeSpectroscopyOutput(final Gnirs instrument, final SpectroscopyResult result) {
        _println("");

        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();

        if (!_obsDetailParameters.isAutoAperture()) {
            _println("software aperture extent along slit = "
                    + device.toString(_obsDetailParameters
                    .getApertureDiameter()) + " arcsec");
        } else {
            switch (_sdParameters.getProfileType()) {
                case UNIFORM:
                    _println("software aperture extent along slit = "
                            + device.toString(1 / _gnirsParameters
                            .getFPMask()) + " arcsec");
                    break;
                case POINT:
                    _println("software aperture extent along slit = "
                            + device.toString(1.4 * result.IQcalc.getImageQuality()) + " arcsec");
                    break;
            }
        }

        if (!_sdParameters.isUniform()) {
            _println("fraction of source flux in aperture = "
                    + device.toString(result.st.getSlitThroughput()));
        }

        _println("derived image size(FWHM) for a point source = "
                + device.toString(result.IQcalc.getImageQuality()) + "arcsec\n");

        _println("Sky subtraction aperture = "
                + _obsDetailParameters.getSkyApertureDiameter()
                + " times the software aperture.");

        _println("");
        _println("Requested total integration time = "
                + device.toString(_obsDetailParameters.getExposureTime() * _obsDetailParameters.getNumExposures())
                + " secs, of which "
                + device.toString(_obsDetailParameters.getExposureTime() * _obsDetailParameters.getNumExposures()
                * _obsDetailParameters.getSourceFraction()) + " secs is on source.");

        _print("<HR align=left SIZE=3>");

        _println("<p style=\"page-break-inside: never\">");

        final String sigSpec, singleS2N, backSpec, finalS2N;
        if (instrument.XDisp_IsUsed()) {

            final ITCChart chart1 = new ITCChart("Signal and Background in software aperture of " + result.specS2N[0].getSpecNpix() + " pixels", "Wavelength (nm)", "e- per exposure per spectral pixel", _plotParameters);
            for (int i = 0; i < ORDERS; i++) {
                chart1.addArray(signalOrder[i].getData(), "Signal Order "+(i+3), ORDER_COLORS[i]);
                chart1.addArray(backGroundOrder[i].getData(), "SQRT(Background) Order "+(i+3), ORDER_BG_COLORS[i]);
            }
            _println(chart1.getBufferedImage(), "SigAndBack");
            _println("");

            sigSpec = _printSpecTag("ASCII signal spectrum");
            backSpec = _printSpecTag("ASCII background spectrum");

            final ITCChart chart2 = new ITCChart("Final S/N", "Wavelength (nm)", "Signal / Noise per spectral pixel", _plotParameters);
            for (int i = 0; i < ORDERS; i++) {
                chart2.addArray(finalS2NOrder[i].getData(), "Final S/N Order "+(i+3), ORDER_COLORS[i]);
            }
            _println(chart2.getBufferedImage(), "Sig2N");
            _println("");

            singleS2N = "";
            finalS2N = _printSpecTag("Final S/N ASCII data");

        } else {

            final ITCChart chart1 = new ITCChart("Signal and Background in software aperture of " + result.specS2N[0].getSpecNpix() + " pixels", "Wavelength (nm)", "e- per exposure per spectral pixel", _plotParameters);
            chart1.addArray(result.specS2N[0].getSignalSpectrum().getData(), "Signal ");
            chart1.addArray(result.specS2N[0].getBackgroundSpectrum().getData(), "SQRT(Background)  ");
            _println(chart1.getBufferedImage(), "SigAndBack");
            _println("");

            sigSpec = _printSpecTag("ASCII signal spectrum");
            backSpec = _printSpecTag("ASCII background spectrum");

            final ITCChart chart2 = new ITCChart("Intermediate Single Exp and Final S/N", "Wavelength (nm)", "Signal / Noise per spectral pixel", _plotParameters);
            chart2.addArray(result.specS2N[0].getExpS2NSpectrum().getData(), "Single Exp S/N");
            chart2.addArray(result.specS2N[0].getFinalS2NSpectrum().getData(), "Final S/N  ");
            _println(chart2.getBufferedImage(), "Sig2N");
            _println("");

            singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
            finalS2N = _printSpecTag("Final S/N ASCII data");
        }

        _println("");
        device.setPrecision(2); // TWO decimal places
        device.clear();

        _print("<HR align=left SIZE=3>");

        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(_sdParameters));
        _println(instrument.toString());
        _println(HtmlPrinter.printParameterSummary(_telescope));
        _println(HtmlPrinter.printParameterSummary(_obsConditionParameters));
        _println(HtmlPrinter.printParameterSummary(_obsDetailParameters));
        _println(HtmlPrinter.printParameterSummary(_plotParameters));

        if (instrument.XDisp_IsUsed()) {
            for (int i = 0; i < ORDERS; i++) {
                _println(signalOrder[i], _header, sigSpec);
            }
            for (int i = 0; i < ORDERS; i++) {
                _println(backGroundOrder[i], _header, backSpec);
            }
            for (int i = 0; i < ORDERS; i++) {
                _println(finalS2NOrder[i], _header, finalS2N);
            }
        } else {
            _println(result.specS2N[0].getSignalSpectrum(), _header, sigSpec);
            _println(result.specS2N[0].getBackgroundSpectrum(), _header, backSpec);
            _println(result.specS2N[0].getExpS2NSpectrum(), _header, singleS2N);
            _println(result.specS2N[0].getFinalS2NSpectrum(), _header, finalS2N);
        }
    }
}
