package edu.gemini.itc.trecs;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.HtmlPrinter;
import edu.gemini.itc.web.ITCRequest;
import edu.gemini.spModel.core.Site;

import java.io.PrintWriter;
import java.util.Calendar;

/**
 * This class performs the calculations for T-Recs used for imaging.
 */
public final class TRecsRecipe extends RecipeBase {
    // Parameters from the web page.
    private SourceDefinition _sdParameters;
    private ObservationDetails _obsDetailParameters;
    private ObservingConditions _obsConditionParameters;
    private TRecsParameters _trecsParameters;
    private TelescopeDetails _telescope;
    private PlottingDetails _plotParameters;

    private final Calendar now = Calendar.getInstance();
    private final String _header = "# T-ReCS ITC: " + now.getTime() + "\n";

    /**
     * Constructs a TRecsRecipe by parsing a Multipart servlet request.
     *
     * @param r   Servlet request containing form data from ITC web page.
     * @param out Results will be written to this PrintWriter.
     * @throws Exception on failure to parse parameters.
     */
    public TRecsRecipe(final ITCMultiPartParser r, final PrintWriter out) {
        _out = out;

        // Read parameters from the four main sections of the web page.
        _trecsParameters = new TRecsParameters(r);
        _sdParameters = ITCRequest.sourceDefinitionParameters(r);
        _obsDetailParameters = correctedObsDetails(_trecsParameters, ITCRequest.observationParameters(r));
        _obsConditionParameters = ITCRequest.obsConditionParameters(r);
        _telescope = ITCRequest.teleParameters(r);
        _plotParameters = ITCRequest.plotParameters(r);

        validateInputParameters();
    }

    /**
     * Constructs a TRecsRecipe given the parameters. Useful for testing.
     */
    public TRecsRecipe(final SourceDefinition sdParameters,
                       final ObservationDetails obsDetailParameters,
                       final ObservingConditions obsConditionParameters,
                       final TRecsParameters trecsParameters, TelescopeDetails telescope,
                       final PlottingDetails plotParameters,
                       final PrintWriter out) {
        super(out);
        _sdParameters = sdParameters;
        _obsDetailParameters = correctedObsDetails(trecsParameters, obsDetailParameters);
        _obsConditionParameters = obsConditionParameters;
        _trecsParameters = trecsParameters;
        _telescope = telescope;
        _plotParameters = plotParameters;

        validateInputParameters();
    }

    private void validateInputParameters() {
        if (_sdParameters.getDistributionType().equals(SourceDefinition.Distribution.ELINE))
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters
                    .getELineWavelength() * 1000 / 4))) { // /4 b/c of increased
                // resolution of
                // transmission
                // files
                throw new RuntimeException(
                        "Please use a model line width > 4 nm (or "
                                + (3E5 / (_sdParameters.getELineWavelength() * 1000 / 4))
                                + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }

        // For mid-IR observation the watervapor percentile and sky background
        // percentile must be the same
        if (!_obsConditionParameters.getSkyTransparencyWaterCategory().equals(_obsConditionParameters.getSkyBackgroundCategory())) {
            _println("");
            _println("Sky background percentile must be equal to sky transparency(water vapor): \n "
                    + "    Please modify the Observing condition constraints section of the HTML form \n"
                    + "    and recalculate.");

            throw new RuntimeException("");
        }

        // report error if this does not come out to be an integer
        Validation.checkSourceFraction(_obsDetailParameters.getNumExposures(), _obsDetailParameters.getSourceFraction());
    }

    private ObservationDetails correctedObsDetails(final TRecsParameters tp, final ObservationDetails odp) {
        // TODO : These corrections were previously done in random places throughout the recipe. I moved them here
        // TODO : so the ObservationDetailsParameters object can become immutable. Basically this calculates
        // TODO : some missing parameters and/or turns the total exposure time into a single exposure time.
        // TODO : This is a temporary hack. There needs to be a better solution for this.
        // NOTE : odp.getExposureTime() carries the TOTAL exposure time (as opposed to exp time for a single frame)
        final TRecs instrument = new TRecs(tp, odp); // TODO: Avoid creating an instrument instance twice.
        final double correctedExposureTime = instrument.getFrameTime();
        final int correctedNumExposures = new Double(odp.getExposureTime() / instrument.getFrameTime() + 0.5).intValue();
        if (odp.getMethod() instanceof ImagingInt) {
            return new ObservationDetails(
                    new ImagingInt(odp.getSNRatio(), correctedExposureTime, odp.getSourceFraction()),
                    odp.getAnalysis()
            );
        } else if (odp.getMethod() instanceof ImagingSN) {
            return new ObservationDetails(
                    new ImagingSN(correctedNumExposures, correctedExposureTime, odp.getSourceFraction()),
                    odp.getAnalysis()
            );
        } else if (odp.getMethod() instanceof SpectroscopySN) {
            return new ObservationDetails(
                    new SpectroscopySN(correctedNumExposures, correctedExposureTime, odp.getSourceFraction()),
                    odp.getAnalysis()
            );
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Performes recipe calculation and writes results to a cached PrintWriter
     * or to System.out.
     *
     * @throws Exception A recipe calculation can fail in many ways, missing data
     *                   files, incorrectly-formatted data files, ...
     */
    public void writeOutput() {
        final TRecs instrument = new TRecs(_trecsParameters, _obsDetailParameters);
        if (_obsDetailParameters.getMethod().isSpectroscopy()) {
            final SpectroscopyResult result = calculateSpectroscopy(instrument);
            writeSpectroscopyOutput(instrument, result);
        } else {
            final ImagingResult result = calculateImaging(instrument);
            writeImagingOutput(instrument, result);
        }
    }

    private SpectroscopyResult calculateSpectroscopy(final TRecs instrument) {

        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, Site.GS, ITCConstants.MID_IR, _sdParameters, _obsConditionParameters, _telescope);
        final VisitableSampledSpectrum sed = calcSource.sed;
        final VisitableSampledSpectrum sky = calcSource.sky;

        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio. There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification

        double pixel_size = instrument.getPixelSize();
        double ap_diam = 0;

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.
        final double exp_time = _obsDetailParameters.getExposureTime();
        final int number_exposures = _obsDetailParameters.getNumExposures();
        final double frac_with_source = _obsDetailParameters.getSourceFraction();
        double spec_source_frac = 0;

        final SlitThroughput st;
        if (!_obsDetailParameters.isAutoAperture()) {
            st = new SlitThroughput(IQcalc.getImageQuality(),
                    _obsDetailParameters.getApertureDiameter(), pixel_size,
                    _trecsParameters.getFPMask());
        } else {
            st = new SlitThroughput(IQcalc.getImageQuality(), pixel_size, _trecsParameters.getFPMask());
        }


        ap_diam = st.getSpatialPix();
        spec_source_frac = st.getSlitThroughput();

        // For the usb case we want the resolution to be determined by the
        // slit width and not the image quality for a point source.
        final double im_qual;
        if (_sdParameters.isUniform()) {
            im_qual = 10000;
            if (_obsDetailParameters.isAutoAperture()) {
                ap_diam = new Double(1 / (_trecsParameters.getFPMask() * pixel_size) + 0.5).intValue();
                spec_source_frac = 1;
            } else {
                spec_source_frac = _trecsParameters.getFPMask() * ap_diam * pixel_size; // ap_diam = Spec_NPix
            }
        } else {
            im_qual = IQcalc.getImageQuality();
        }

        final SpecS2NLargeSlitVisitor specS2N = new SpecS2NLargeSlitVisitor(_trecsParameters.getFPMask(),
                pixel_size, instrument.getSpectralPixelWidth(),
                instrument.getObservingStart(),
                instrument.getObservingEnd(),
                instrument.getGratingDispersion_nm(),
                instrument.getGratingDispersion_nmppix(),
                instrument.getGratingResolution(), spec_source_frac,
                im_qual, ap_diam, number_exposures, frac_with_source,
                exp_time,
                instrument.getDarkCurrent(),
                instrument.getReadNoise(),
                _obsDetailParameters.getSkyApertureDiameter());
        specS2N.setDetectorTransmission(instrument.getDetectorTransmision());
        specS2N.setSourceSpectrum(sed);
        specS2N.setBackgroundSpectrum(sky);
        sed.accept(specS2N);

        final Parameters p = new Parameters(_sdParameters, _obsDetailParameters, _obsConditionParameters, _telescope);
        final SpecS2NLargeSlitVisitor[] specS2Narr = new SpecS2NLargeSlitVisitor[1];
        specS2Narr[0] = specS2N;
        return SpectroscopyResult.apply(p, instrument, null, IQcalc, specS2Narr, st); // TODO SFCalc not needed!
    }

    private ImagingResult calculateImaging(final TRecs instrument) {
        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED

        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, Site.GS, ITCConstants.MID_IR, _sdParameters, _obsConditionParameters, _telescope);
        final VisitableSampledSpectrum sed = calcSource.sed;
        final VisitableSampledSpectrum sky = calcSource.sky;
        double sed_integral = sed.getIntegral();
        double sky_integral = sky.getIntegral();


        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio. There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        // Calculate the Fraction of source in the aperture
        final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, IQcalc.getImageQuality());

        // Calculate the Peak Pixel Flux
        final double peak_pixel_count = PeakPixelFlux.calculate(instrument, _sdParameters, _obsDetailParameters, SFcalc, IQcalc.getImageQuality(), sed_integral, sky_integral);

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        final ImagingS2NCalculatable IS2Ncalc = ImagingS2NCalculationFactory.getCalculationInstance(_obsDetailParameters, instrument, SFcalc, sed_integral, sky_integral);
        IS2Ncalc.calculate();

        final Parameters p = new Parameters(_sdParameters, _obsDetailParameters, _obsConditionParameters, _telescope);
        return ImagingResult.apply(p, instrument, IQcalc, SFcalc, peak_pixel_count, IS2Ncalc);

    }



    // ===================================================================================================================
    // TODO: OUTPUT METHODS
    // TODO: These need to be simplified/cleaned/shared and then go to the web module.. and then be deleted and forgotten.
    // ===================================================================================================================

    private void writeSpectroscopyOutput(final TRecs instrument, final SpectroscopyResult result) {
        _println("");

        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();

        if (!_obsDetailParameters.isAutoAperture()) {
            _println("software aperture extent along slit = " + device.toString(_obsDetailParameters.getApertureDiameter()) + " arcsec");
        } else {
            switch (_sdParameters.getProfileType()) {
                case UNIFORM:
                    _println("software aperture extent along slit = " + device.toString(1 / _trecsParameters.getFPMask()) + " arcsec");
                    break;
                case POINT:
                    _println("software aperture extent along slit = " + device.toString(1.4 * result.iqCalc().getImageQuality()) + " arcsec");
                    break;
            }
        }

        if (!_sdParameters.isUniform()) {
            _println("fraction of source flux in aperture = " + device.toString(result.st().getSlitThroughput()));
        }

        _println("derived image size(FWHM) for a point source = " + device.toString(result.iqCalc().getImageQuality()) + "arcsec\n");

        _println("Sky subtraction aperture = "
                + _obsDetailParameters.getSkyApertureDiameter()
                + " times the software aperture.");

        _println("");

        final double exp_time = _obsDetailParameters.getExposureTime();
        final int number_exposures = _obsDetailParameters.getNumExposures();
        final double frac_with_source = _obsDetailParameters.getSourceFraction();

        _println("Requested total integration time = "
                + device.toString(exp_time * number_exposures)
                + " secs, of which "
                + device.toString(exp_time * number_exposures
                * frac_with_source) + " secs is on source.");

        _print("<HR align=left SIZE=3>");

        _println("<p style=\"page-break-inside: never\">");


        final ITCChart chart1 = new ITCChart("Signal and Background", "Wavelength (nm)", "e- per exposure per spectral pixel", _plotParameters);
        final ITCChart chart2 = new ITCChart("Intermediate Single Exp and Final S/N", "Wavelength (nm)", "Signal / Noise per spectral pixel", _plotParameters);

        chart1.addArray(result.specS2N()[0].getSignalSpectrum().getData(), "Signal ");
        chart1.addArray(result.specS2N()[0].getBackgroundSpectrum().getData(), "SQRT(Background)  ");
        _println(chart1.getBufferedImage(), "SigAndBack");
        _println("");

        final String sigSpec = _printSpecTag("ASCII signal spectrum");
        final String backSpec = _printSpecTag("ASCII background spectrum");

        chart2.addArray(result.specS2N()[0].getExpS2NSpectrum().getData(), "Single Exp S/N");
        chart2.addArray(result.specS2N()[0].getFinalS2NSpectrum().getData(), "Final S/N  ");
        _println(chart2.getBufferedImage(), "Sig2N");
        _println("");

        final String singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
        final String finalS2N = _printSpecTag("Final S/N ASCII data");

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
        _println(result.specS2N()[0].getSignalSpectrum(), _header, sigSpec);
        _println(result.specS2N()[0].getBackgroundSpectrum(), _header, backSpec);
        _println(result.specS2N()[0].getExpS2NSpectrum(), _header, singleS2N);
        _println(result.specS2N()[0].getFinalS2NSpectrum(), _header, finalS2N);
    }

    private void writeImagingOutput(final TRecs instrument, final ImagingResult result) {
        _println("");

        // This object is used to format numerical strings.
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();


        _print(result.sfCalc().getTextResult(device));
        _println(result.iqCalc().getTextResult(device));
        _println("Sky subtraction aperture = "
                + _obsDetailParameters.getSkyApertureDiameter()
                + " times the software aperture.\n");

        _println(result.is2nCalc().getTextResult(device));
        device.setPrecision(0); // NO decimal places
        device.clear();

        _println("");
        _println("The peak pixel signal + background is "
                + device.toString(result.peakPixelCount()) + ". ");

        if (result.peakPixelCount() > (instrument.getWellDepth()))
            _println("Warning: peak pixel may be saturating the imaging deep well setting of "
                    + instrument.getWellDepth());

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
    }


}