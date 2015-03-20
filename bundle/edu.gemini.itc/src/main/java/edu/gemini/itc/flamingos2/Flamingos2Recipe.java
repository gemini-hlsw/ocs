package edu.gemini.itc.flamingos2;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.service.*;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.HtmlPrinter;
import edu.gemini.itc.web.ITCRequest;
import edu.gemini.spModel.core.Site;

import java.io.PrintWriter;
import java.util.Calendar;

/**
 * This class performs the calculations for Flamingos 2 used for imaging.
 */
public final class Flamingos2Recipe extends RecipeBase {

    private final Flamingos2Parameters _flamingos2Parameters;
    private final String _header = "# Flamingos-2 ITC: " + Calendar.getInstance().getTime() + "\n";
    private final ObservingConditions _obsConditionParameters;
    private final ObservationDetails _obsDetailParameters;
    private final PlottingDetails _plotParameters;
    private final SourceDefinition _sdParameters;
    private final TelescopeDetails _telescope;

    /**
     * Constructs an Flamingos 2 object by parsing a Multi part servlet request.
     *
     * @param r   Servlet request containing form data from ITC web page.
     * @param out Results will be written to this PrintWriter.
     */
    public Flamingos2Recipe(final ITCMultiPartParser r, final PrintWriter out) {
        super(out);

        // Read parameters from the four main sections of the web page.
        _sdParameters = ITCRequest.sourceDefinitionParameters(r);
        _obsDetailParameters = ITCRequest.observationParameters(r);
        _obsConditionParameters = ITCRequest.obsConditionParameters(r);
        _flamingos2Parameters = new Flamingos2Parameters(r);
        _telescope = ITCRequest.teleParameters(r);
        _plotParameters = ITCRequest.plotParamters(r);
    }

    /**
     * Constructs an Flamingos 2 object given the parameters. Useful for
     * testing.
     */
    public Flamingos2Recipe(final SourceDefinition sdParameters,
                            final ObservationDetails obsDetailParameters,
                            final ObservingConditions obsConditionParameters,
                            final Flamingos2Parameters flamingos2Parameters,
                            final TelescopeDetails telescope,
                            final PlottingDetails plotParameters,
                            final PrintWriter out) {
        super(out);

        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _flamingos2Parameters = flamingos2Parameters;
        _telescope = telescope;
        _plotParameters = plotParameters;
    }

    /**
     * Check input parameters for consistency
     */
    public void checkInputParameters() {
        if (_obsDetailParameters.getMethod().isSpectroscopy()) {
            if (_flamingos2Parameters.getGrism().equalsIgnoreCase("none")) {
                throw new IllegalArgumentException("In spectroscopy mode, a grism must be selected");
            }
            if (_flamingos2Parameters.getFPMask().equalsIgnoreCase("none")) {
                throw new IllegalArgumentException("In spectroscopy mode, a FP must must be selected");
            }
        }
    }

    /**
     * Performes recipe calculation and writes results to a cached PrintWriter
     * or to System.out.
     *
     * @throws Exception A recipe calculation can fail in many ways, missing data
     *                   files, incorrectly-formatted data files, ...
     */
    @Override
    public void writeOutput() {
        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();
        _println("");

        // Module 1b
        // Define the source energy (as function of wavelength).
        //

        checkInputParameters();

        final Flamingos2 instrument = new Flamingos2(_flamingos2Parameters);

        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, Site.GS, ITCConstants.NEAR_IR, _sdParameters, _obsConditionParameters, _telescope, _plotParameters);
        final VisitableSampledSpectrum sed = calcSource.sed;
        final VisitableSampledSpectrum sky = calcSource.sky;
        final double sed_integral = sed.getIntegral();
        final double sky_integral = sky.getIntegral();

        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio. There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification

        final double pixel_size = instrument.getPixelSize();
        double ap_diam;

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();
        final double im_qual = IQcalc.getImageQuality();

        // Calculate Source fraction
        final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, im_qual);
        _print(SFcalc.getTextResult(device));
        _println(IQcalc.getTextResult(device));

        // Calculate the Peak Pixel Flux
        final double peak_pixel_count = PeakPixelFlux.calculate(instrument, _sdParameters, _obsDetailParameters, SFcalc, im_qual, sed_integral, sky_integral);

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        final int number_exposures = _obsDetailParameters.getNumExposures();
        final double frac_with_source = _obsDetailParameters.getSourceFraction();

        // report error if this does not come out to be an integer
        checkSourceFraction(number_exposures, frac_with_source);

        final double exposure_time = _obsDetailParameters.getExposureTime();
        final double dark_current = instrument.getDarkCurrent();
        final double read_noise = instrument.getReadNoise();

        if (_obsDetailParameters.getMethod().isSpectroscopy()) {

            final String sigSpec, backSpec, singleS2N, finalS2N;
            final SpecS2NLargeSlitVisitor specS2N;
            final SlitThroughput st;

            if (!_obsDetailParameters.isAutoAperture()) {
                st = new SlitThroughput(im_qual,
                        _obsDetailParameters.getApertureDiameter(), pixel_size,
                        _flamingos2Parameters.getSlitSize() * pixel_size);

                _println("software aperture extent along slit = "
                        + device.toString(_obsDetailParameters
                        .getApertureDiameter()) + " arcsec");
            } else {
                st = new SlitThroughput(im_qual, pixel_size,
                        _flamingos2Parameters.getSlitSize() * pixel_size);

                switch (_sdParameters.getProfileType()) {
                    case UNIFORM:
                        _println("software aperture extent along slit = "
                                + device.toString(1 / _flamingos2Parameters
                                .getSlitSize() * pixel_size) + " arcsec");
                        break;
                    case POINT:
                    _println("software aperture extent along slit = "
                            + device.toString(1.4 * im_qual) + " arcsec");
                        break;
                }
            }

            if (!_sdParameters.isUniform()) {
                _println("fraction of source flux in aperture = "
                        + device.toString(st.getSlitThroughput()));
            }

            _println("derived image size(FWHM) for a point source = "
                    + device.toString(im_qual) + " arcsec");

            _println("");
            _println("Requested total integration time = "
                    + device.toString(exposure_time * number_exposures)
                    + " secs, of which "
                    + device.toString(exposure_time * number_exposures
                    * frac_with_source) + " secs is on source.");

            _print("<HR align=left SIZE=3>");
            ap_diam = st.getSpatialPix();
            double spec_source_frac = st.getSlitThroughput();

            if (_sdParameters.isUniform()) {
               if (_obsDetailParameters.isAutoAperture()) {
                    ap_diam = new Double(1 / (_flamingos2Parameters.getSlitSize() * pixel_size) + 0.5).intValue();
                    spec_source_frac = 1;
                } else {
                    spec_source_frac = _flamingos2Parameters.getSlitSize() * pixel_size * ap_diam * pixel_size; // ap_diam = Spec_NPix
                }
            }

            final double gratDispersion_nmppix = instrument.getSpectralPixelWidth();
            final double gratDispersion_nm = 0.5 / pixel_size * gratDispersion_nmppix;

            specS2N = new SpecS2NLargeSlitVisitor(_flamingos2Parameters.getSlitSize() * pixel_size,
                    pixel_size, instrument.getSpectralPixelWidth(),
                    instrument.getObservingStart(),
                    instrument.getObservingEnd(),
                    gratDispersion_nm,
                    gratDispersion_nmppix,
                    instrument.getGrismResolution(), spec_source_frac, im_qual,
                    ap_diam, number_exposures, frac_with_source, exposure_time,
                    dark_current, read_noise,
                    _obsDetailParameters.getSkyApertureDiameter());

            specS2N.setDetectorTransmission(instrument.getDetectorTransmision());
            specS2N.setSourceSpectrum(sed);
            specS2N.setBackgroundSpectrum(sky);
            specS2N.setSpecHaloSourceFraction(0.0);

            final ITCChart chart1 = new ITCChart(
                    "Signal and SQRT(Background) in software aperture of " + ap_diam + " pixels",
                    "Wavelength (nm)", "e- per exposure per spectral pixel", _plotParameters);
            final ITCChart chart2 = new ITCChart(
                    "Intermediate Single Exp and Final S/N",
                    "Wavelength (nm)", "Signal / Noise per spectral pixel", _plotParameters);

            sed.accept(specS2N);
            _println("<p style=\"page-break-inside: never\">");
            chart1.addArray(specS2N.getSignalSpectrum().getData(), "Signal ");
            chart1.addArray(specS2N.getBackgroundSpectrum().getData(), "SQRT(Background)  ");

            _println(chart1.getBufferedImage(), "SigAndBack");
            _println("");

            sigSpec = _printSpecTag("ASCII signal spectrum");
            backSpec = _printSpecTag("ASCII background spectrum");

            chart2.addArray(specS2N.getExpS2NSpectrum().getData(), "Single Exp S/N");
            chart2.addArray(specS2N.getFinalS2NSpectrum().getData(), "Final S/N  ");

            _println(chart2.getBufferedImage(), "Sig2N");
            _println("");

            singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
            finalS2N = _printSpecTag("Final S/N ASCII data");

            _println(specS2N.getSignalSpectrum(), _header, sigSpec);
            _println(specS2N.getBackgroundSpectrum(), _header, backSpec);
            _println(specS2N.getExpS2NSpectrum(), _header, singleS2N);
            _println(specS2N.getFinalS2NSpectrum(), _header, finalS2N);

        } else {
            // Observing mode: Imaging

            // Calculate the Signal to Noise

            final ImagingS2NCalculatable IS2Ncalc = ImagingS2NCalculationFactory.getCalculationInstance(_obsDetailParameters, instrument, SFcalc, sed_integral, sky_integral);
            IS2Ncalc.calculate();
            _println(IS2Ncalc.getTextResult(device));
            device.setPrecision(0); // NO decimal places
            device.clear();

            _println("");
            _println("The peak pixel signal + background is "
                    + device.toString(peak_pixel_count)
                    + ". This is "
                    + device.toString(peak_pixel_count
                    / instrument.getWellDepth() * 100)
                    + "% of the full well depth of "
                    + device.toString(instrument.getWellDepth()) + ".");

            if (peak_pixel_count > (.8 * instrument.getWellDepth()))
                _println("Warning: peak pixel exceeds 80% of the well depth and may be saturated");

            _println("");
            device.setPrecision(2); // TWO decimal places
            device.clear();
        }

		/*
         * Here end
		 */

        // /////////////////////////////////////////////
        // ////////Print Config////////////////////////

        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: Flamingos 2\n"); // TODO: move names of instrument to instrument classes?
        _println(HtmlPrinter.printParameterSummary(_sdParameters));
        _println(instrument.toString());

        _println(HtmlPrinter.printParameterSummary(_telescope));
        _println(HtmlPrinter.printParameterSummary(_obsConditionParameters));
        _println(HtmlPrinter.printParameterSummary(_obsDetailParameters));

        if (_obsDetailParameters.getMethod().isSpectroscopy()) {
            _println(HtmlPrinter.printParameterSummary(_plotParameters));
        }
    }
}
