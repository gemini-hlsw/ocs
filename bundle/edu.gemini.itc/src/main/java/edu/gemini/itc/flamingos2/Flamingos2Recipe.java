package edu.gemini.itc.flamingos2;

import edu.gemini.itc.operation.*;
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

        validateInputParameters();
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

        validateInputParameters();
    }

    /**
     * Check input parameters for consistency
     */
    private void validateInputParameters() {
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
     * Performs recipe calculation and writes results to a cached PrintWriter or to System.out.
     */
    @Override
    public void writeOutput() {
        final Flamingos2 instrument = new Flamingos2(_flamingos2Parameters);
        if (_obsDetailParameters.getMethod().isSpectroscopy()) {
            final SpectroscopyResult result = calculateSpectroscopy(instrument);
            writeSpectroscopyOutput(instrument, result);
        } else {
            final ImagingResult result = calculateImaging(instrument);
            writeImagingOutput(instrument, result);
        }
    }

    public SpectroscopyResult calculateSpectroscopy() {
        final Flamingos2 instrument = new Flamingos2(_flamingos2Parameters);
        return calculateSpectroscopy(instrument);
    }

    public ImagingResult calculateImaging() {
        final Flamingos2 instrument = new Flamingos2(_flamingos2Parameters);
        return calculateImaging(instrument);
    }

    private SpectroscopyResult calculateSpectroscopy(final Flamingos2 instrument) {
        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio. There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification
        final SEDFactory.SourceResult src = SEDFactory.calculate(instrument, Site.GS, ITCConstants.NEAR_IR, _sdParameters, _obsConditionParameters, _telescope, _plotParameters);

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();
        final double im_qual = IQcalc.getImageQuality();

        // Calculate Source fraction
        final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, im_qual);

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        // report error if this does not come out to be an integer
        checkSourceFraction(_obsDetailParameters.getNumExposures(), _obsDetailParameters.getSourceFraction());

        final double pixel_size = instrument.getPixelSize();
        final SpecS2NLargeSlitVisitor specS2N;
        final SlitThroughput st;

        if (!_obsDetailParameters.isAutoAperture()) {
            st = new SlitThroughput(im_qual, _obsDetailParameters.getApertureDiameter(), pixel_size, _flamingos2Parameters.getSlitSize() * pixel_size);
        } else {
            st = new SlitThroughput(im_qual, pixel_size, _flamingos2Parameters.getSlitSize() * pixel_size);
        }

        double ap_diam = st.getSpatialPix();
        double spec_source_frac = st.getSlitThroughput();

        if (_sdParameters.isUniform()) {
            if (_obsDetailParameters.isAutoAperture()) {
                ap_diam = new Double(1 / (_flamingos2Parameters.getSlitSize() * pixel_size) + 0.5).intValue();
                spec_source_frac = 1;
            } else {
                spec_source_frac = _flamingos2Parameters.getSlitSize() * pixel_size * ap_diam * pixel_size;
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
                ap_diam,
                _obsDetailParameters.getNumExposures(),
                _obsDetailParameters.getSourceFraction(),
                _obsDetailParameters.getExposureTime(),
                instrument.getDarkCurrent(),
                instrument.getReadNoise(),
                _obsDetailParameters.getSkyApertureDiameter());

        specS2N.setDetectorTransmission(instrument.getDetectorTransmision());
        specS2N.setSourceSpectrum(src.sed);
        specS2N.setBackgroundSpectrum(src.sky);
        specS2N.setSpecHaloSourceFraction(0.0);
        src.sed.accept(specS2N);

        final SpecS2NLargeSlitVisitor[] specS2Narr = new SpecS2NLargeSlitVisitor[1];
        specS2Narr[0] = specS2N;

        return new SpectroscopyResult(SFcalc, IQcalc, specS2Narr, st);
    }

    private ImagingResult calculateImaging(final Flamingos2 instrument) {

        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio. There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification
        final SEDFactory.SourceResult src = SEDFactory.calculate(instrument, Site.GS, ITCConstants.NEAR_IR, _sdParameters, _obsConditionParameters, _telescope, _plotParameters);

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();
        final double im_qual = IQcalc.getImageQuality();

        // Calculate Source fraction
        final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, im_qual);

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        // report error if this does not come out to be an integer
        checkSourceFraction(_obsDetailParameters.getNumExposures(), _obsDetailParameters.getSourceFraction());
        // Get the summed source and sky
        final VisitableSampledSpectrum sed = src.sed;
        final VisitableSampledSpectrum sky = src.sky;
        final double sed_integral = sed.getIntegral();
        final double sky_integral = sky.getIntegral();

        // Calculate the Peak Pixel Flux
        final double peak_pixel_count = PeakPixelFlux.calculate(instrument, _sdParameters, _obsDetailParameters, SFcalc, im_qual, sed_integral, sky_integral);

        // Calculate the Signal to Noise
        final ImagingS2NCalculatable IS2Ncalc = ImagingS2NCalculationFactory.getCalculationInstance(_obsDetailParameters, instrument, SFcalc, sed_integral, sky_integral);
        IS2Ncalc.calculate();

        return new ImagingResult(IQcalc, SFcalc, peak_pixel_count, IS2Ncalc);
    }


    // ===================================================================================================================
    // TODO: OUTPUT METHODS
    // TODO: These need to be simplified/cleaned/shared and then go to the web module.. and then be deleted and forgotten.
    // ===================================================================================================================


    private void writeSpectroscopyOutput(final Flamingos2 instrument, final SpectroscopyResult result) {
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();
        _println("");

        _print(result.SFcalc.getTextResult(device));
        _println(result.IQcalc.getTextResult(device));

        if (!_obsDetailParameters.isAutoAperture()) {
            _println("software aperture extent along slit = "
                    + device.toString(_obsDetailParameters
                    .getApertureDiameter()) + " arcsec");
        } else {
            switch (_sdParameters.getProfileType()) {
                case UNIFORM:
                    _println("software aperture extent along slit = "
                            + device.toString(1 / _flamingos2Parameters
                            .getSlitSize() * instrument.getPixelSize()) + " arcsec");
                    break;
                case POINT:
                    _println("software aperture extent along slit = "
                            + device.toString(1.4 * result.IQcalc.getImageQuality()) + " arcsec");
                    break;
            }
        }

        if (!_sdParameters.isUniform()) {
            _println("fraction of source flux in aperture = " + device.toString(result.st.getSlitThroughput()));
        }

        _println("derived image size(FWHM) for a point source = " + device.toString(result.IQcalc.getImageQuality()) + " arcsec");

        _println("");
        _println("Requested total integration time = "
                + device.toString(_obsDetailParameters.getExposureTime() * _obsDetailParameters.getNumExposures())
                + " secs, of which "
                + device.toString(_obsDetailParameters.getExposureTime() * _obsDetailParameters.getNumExposures()
                * result.specS2N[0].getSpecFracWithSource()) + " secs is on source.");

        _print("<HR align=left SIZE=3>");

        final ITCChart chart1 = new ITCChart(
                "Signal and SQRT(Background) in software aperture of " + result.specS2N[0].getSpecNpix() + " pixels",
                "Wavelength (nm)", "e- per exposure per spectral pixel", _plotParameters);
        final ITCChart chart2 = new ITCChart(
                "Intermediate Single Exp and Final S/N",
                "Wavelength (nm)", "Signal / Noise per spectral pixel", _plotParameters);

        _println("<p style=\"page-break-inside: never\">");
        chart1.addArray(result.specS2N[0].getSignalSpectrum().getData(), "Signal ");
        chart1.addArray(result.specS2N[0].getBackgroundSpectrum().getData(), "SQRT(Background)  ");

        _println(chart1.getBufferedImage(), "SigAndBack");
        _println("");

        final String sigSpec = _printSpecTag("ASCII signal spectrum");
        final String backSpec = _printSpecTag("ASCII background spectrum");

        chart2.addArray(result.specS2N[0].getExpS2NSpectrum().getData(), "Single Exp S/N");
        chart2.addArray(result.specS2N[0].getFinalS2NSpectrum().getData(), "Final S/N  ");

        _println(chart2.getBufferedImage(), "Sig2N");
        _println("");

        final String singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
        final String finalS2N = _printSpecTag("Final S/N ASCII data");

        _println(result.specS2N[0].getSignalSpectrum(), _header, sigSpec);
        _println(result.specS2N[0].getBackgroundSpectrum(), _header, backSpec);
        _println(result.specS2N[0].getExpS2NSpectrum(), _header, singleS2N);
        _println(result.specS2N[0].getFinalS2NSpectrum(), _header, finalS2N);

        printConfiguration(instrument);
    }


    private void writeImagingOutput(final Flamingos2 instrument, final ImagingResult result) {
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();
        _println("");

        _print(result.SFcalc.getTextResult(device));
        _println(result.IQcalc.getTextResult(device));

        _println(result.IS2Ncalc.getTextResult(device));
        device.setPrecision(0); // NO decimal places
        device.clear();

        _println("");
        _println("The peak pixel signal + background is "
                + device.toString(result.peak_pixel_count)
                + ". This is "
                + device.toString(result.peak_pixel_count
                / instrument.getWellDepth() * 100)
                + "% of the full well depth of "
                + device.toString(instrument.getWellDepth()) + ".");

        if (result.peak_pixel_count > (.8 * instrument.getWellDepth()))
            _println("Warning: peak pixel exceeds 80% of the well depth and may be saturated");

        _println("");
        device.setPrecision(2); // TWO decimal places
        device.clear();

        printConfiguration(instrument);
    }

    private void printConfiguration(final Flamingos2 instrument) {
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
