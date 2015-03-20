package edu.gemini.itc.trecs;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.service.*;
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

    private String sigSpec, backSpec, singleS2N, finalS2N;
    private SpecS2NLargeSlitVisitor specS2N;

    private Calendar now = Calendar.getInstance();
    private String _header = new StringBuffer("# T-ReCS ITC: " + now.getTime() + "\n").toString();

    /**
     * Constructs a TRecsRecipe by parsing a Multipart servlet request.
     *
     * @param r   Servlet request containing form data from ITC web page.
     * @param out Results will be written to this PrintWriter.
     * @throws Exception on failure to parse parameters.
     */
    public TRecsRecipe(ITCMultiPartParser r, PrintWriter out) {
        _out = out;

        // Read parameters from the four main sections of the web page.
        _trecsParameters = new TRecsParameters(r);
        _sdParameters = ITCRequest.sourceDefinitionParameters(r);
        _obsDetailParameters = correctedObsDetails(_trecsParameters, ITCRequest.observationParameters(r));
        _obsConditionParameters = ITCRequest.obsConditionParameters(r);
        _telescope = ITCRequest.teleParameters(r);
        _plotParameters = ITCRequest.plotParamters(r);
    }

    /**
     * Constructs a TRecsRecipe given the parameters. Useful for testing.
     */
    public TRecsRecipe(SourceDefinition sdParameters,
                       ObservationDetails obsDetailParameters,
                       ObservingConditions obsConditionParameters,
                       TRecsParameters trecsParameters, TelescopeDetails telescope,
                       PlottingDetails plotParameters,
                       PrintWriter out) {
        super(out);
        _sdParameters = sdParameters;
        _obsDetailParameters = correctedObsDetails(trecsParameters, obsDetailParameters);
        _obsConditionParameters = obsConditionParameters;
        _trecsParameters = trecsParameters;
        _telescope = telescope;
        _plotParameters = plotParameters;
    }

    private ObservationDetails correctedObsDetails(TRecsParameters tp, ObservationDetails odp) {
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
        _println("");

        // This object is used to format numerical strings.
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();

        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED

        TRecs instrument = new TRecs(_trecsParameters, _obsDetailParameters);

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


        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, Site.GS, ITCConstants.MID_IR, _sdParameters, _obsConditionParameters, _telescope, _plotParameters);
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

        double pixel_size = instrument.getPixelSize();
        double ap_diam = 0;

        // Calculate image quality
        double im_qual = 0.;
        ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        im_qual = IQcalc.getImageQuality();
        final double exp_time = _obsDetailParameters.getExposureTime();

        // Calculate the Fraction of source in the aperture
        final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, im_qual);
        if (_obsDetailParameters.getMethod().isImaging()) {
            _print(SFcalc.getTextResult(device));
            _println(IQcalc.getTextResult(device));
            _println("Sky subtraction aperture = "
                    + _obsDetailParameters.getSkyApertureDiameter()
                    + " times the software aperture.\n");
        }

        // Calculate the Peak Pixel Flux
        final double peak_pixel_count = PeakPixelFlux.calculate(instrument, _sdParameters, _obsDetailParameters, SFcalc, im_qual, sed_integral, sky_integral);

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.
        final int number_exposures = _obsDetailParameters.getNumExposures();
        double spec_source_frac = 0;
        double frac_with_source = _obsDetailParameters.getSourceFraction();
        double dark_current = instrument.getDarkCurrent();
        double read_noise = instrument.getReadNoise();
        // report error if this does not come out to be an integer
        checkSourceFraction(number_exposures, frac_with_source);

        // ObservationMode Imaging or spectroscopy

        if (_obsDetailParameters.getMethod().isSpectroscopy()) {

            SlitThroughput st;
            if (!_obsDetailParameters.isAutoAperture()) {
                st = new SlitThroughput(im_qual,
                        _obsDetailParameters.getApertureDiameter(), pixel_size,
                        _trecsParameters.getFPMask());
                _println("software aperture extent along slit = "
                        + device.toString(_obsDetailParameters
                        .getApertureDiameter()) + " arcsec");
            } else {
                st = new SlitThroughput(im_qual, pixel_size, _trecsParameters.getFPMask());
                switch (_sdParameters.getProfileType()) {
                    case UNIFORM:
                        _println("software aperture extent along slit = " + device.toString(1 / _trecsParameters.getFPMask()) + " arcsec");
                        break;
                    case POINT:
                        _println("software aperture extent along slit = " + device.toString(1.4 * im_qual) + " arcsec");
                        break;
                }
            }

            if (!_sdParameters.isUniform()) {
                _println("fraction of source flux in aperture = "
                        + device.toString(st.getSlitThroughput()));
            }

            _println("derived image size(FWHM) for a point source = "
                    + device.toString(im_qual) + "arcsec\n");

            _println("Sky subtraction aperture = "
                    + _obsDetailParameters.getSkyApertureDiameter()
                    + " times the software aperture.");

            _println("");
            _println("Requested total integration time = "
                    + device.toString(exp_time * number_exposures)
                    + " secs, of which "
                    + device.toString(exp_time * number_exposures
                    * frac_with_source) + " secs is on source.");

            _print("<HR align=left SIZE=3>");

            ap_diam = st.getSpatialPix(); // ap_diam really Spec_Npix on Phil's
            // Mathcad change later
            spec_source_frac = st.getSlitThroughput();

            // _println("Spec_source_frac: " + st.getSlitThroughput()+
            // "  Spec_npix: "+ ap_diam);

            // For the usb case we want the resolution to be determined by the
            // slit width and not the image quality for a point source.
            if (_sdParameters.isUniform()) {
                im_qual = 10000;
                if (_obsDetailParameters.isAutoAperture()) {
                    ap_diam = new Double(1 / (_trecsParameters.getFPMask() * pixel_size) + 0.5).intValue();
                    spec_source_frac = 1;
                } else {
                    spec_source_frac = _trecsParameters.getFPMask() * ap_diam * pixel_size; // ap_diam = Spec_NPix
                }
            }
            // _println("Spec_source_frac: " + spec_source_frac+
            // "  Spec_npix: "+ ap_diam);
            // sed.trim(instrument.g);
            // sky.trim();
            specS2N = new SpecS2NLargeSlitVisitor(_trecsParameters.getFPMask(),
                    pixel_size, instrument.getSpectralPixelWidth(),
                    instrument.getObservingStart(),
                    instrument.getObservingEnd(),
                    instrument.getGratingDispersion_nm(),
                    instrument.getGratingDispersion_nmppix(),
                    instrument.getGratingResolution(), spec_source_frac,
                    im_qual, ap_diam, number_exposures, frac_with_source,
                    exp_time, dark_current, read_noise,
                    _obsDetailParameters.getSkyApertureDiameter());
            specS2N.setDetectorTransmission(instrument.getDetectorTransmision());
            specS2N.setSourceSpectrum(sed);
            specS2N.setBackgroundSpectrum(sky);
            sed.accept(specS2N);
            _println("<p style=\"page-break-inside: never\">");


            final ITCChart chart1 = new ITCChart("Signal and Background", "Wavelength (nm)", "e- per exposure per spectral pixel", _plotParameters);
            final ITCChart chart2 = new ITCChart("Intermediate Single Exp and Final S/N", "Wavelength (nm)", "Signal / Noise per spectral pixel", _plotParameters);

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

            // THis was used for TED to output the data might be useful later.
            /**
             * double [][] temp = specS2N.getSignalSpectrum().getData(); for
             * (int i=0; i< specS2N.getSignalSpectrum().getLength()-2; i++) {
             * System.out.print(" " +temp[0][i]+ "  ");
             * System.out.println(temp[1][i]); } System.out.println("END");
             * double [][] temp2 = specS2N.getFinalS2NSpectrum().getData(); for
             * (int i=0; i< specS2N.getFinalS2NSpectrum().getLength()-2; i++) {
             * System.out.print(" " +temp2[0][i]+ "  ");
             * System.out.println(temp2[1][i]); } System.out.println("END");
             *
             **/

        } else {

            final ImagingS2NCalculatable IS2Ncalc = ImagingS2NCalculationFactory.getCalculationInstance(_obsDetailParameters, instrument, SFcalc, sed_integral, sky_integral);
            IS2Ncalc.calculate();
            _println(IS2Ncalc.getTextResult(device));
            device.setPrecision(0); // NO decimal places
            device.clear();

            _println("");
            _println("The peak pixel signal + background is "
                    + device.toString(peak_pixel_count) + ". ");// This is " +

            if (peak_pixel_count > (instrument.getWellDepth()))
                _println("Warning: peak pixel may be saturating the imaging deep well setting of "
                        + instrument.getWellDepth());

        }

        _println("");
        device.setPrecision(2); // TWO decimal places
        device.clear();

        // _println("");
        _print("<HR align=left SIZE=3>");
        // _println("");

        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(_sdParameters));
        _println(instrument.toString());
        _println(HtmlPrinter.printParameterSummary(_telescope));
        _println(HtmlPrinter.printParameterSummary(_obsConditionParameters));
        _println(HtmlPrinter.printParameterSummary(_obsDetailParameters));
        if (_obsDetailParameters.getMethod().isSpectroscopy()) {
            _println(HtmlPrinter.printParameterSummary(_plotParameters));
            _println(specS2N.getSignalSpectrum(), _header, sigSpec);
            _println(specS2N.getBackgroundSpectrum(), _header, backSpec);
            _println(specS2N.getExpS2NSpectrum(), _header, singleS2N);
            _println(specS2N.getFinalS2NSpectrum(), _header, finalS2N);
        }
    }
}