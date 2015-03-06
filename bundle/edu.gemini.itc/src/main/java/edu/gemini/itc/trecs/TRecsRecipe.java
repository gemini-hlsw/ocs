package edu.gemini.itc.trecs;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.parameters.*;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.ITCRequest;
import edu.gemini.spModel.core.Site;

import java.io.PrintWriter;
import java.util.Calendar;

/**
 * This class performs the calculations for T-Recs used for imaging.
 */
public final class TRecsRecipe extends RecipeBase {
    // Parameters from the web page.
    private SourceDefinitionParameters _sdParameters;
    private ObservationDetailsParameters _obsDetailParameters;
    private ObservingConditionParameters _obsConditionParameters;
    private TRecsParameters _trecsParameters;
    private TeleParameters _teleParameters;
    private PlottingDetailsParameters _plotParameters;

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
    public TRecsRecipe(ITCMultiPartParser r, PrintWriter out) throws Exception {
        _out = out;

        // Read parameters from the four main sections of the web page.
        _trecsParameters = new TRecsParameters(r);
        _sdParameters = ITCRequest.sourceDefinitionParameters(r);
        _obsDetailParameters = correctedObsDetails(_trecsParameters, ITCRequest.observationParameters(r));
        _obsConditionParameters = ITCRequest.obsConditionParameters(r);
        _teleParameters = ITCRequest.teleParameters(r);
        _plotParameters = ITCRequest.plotParamters(r);
    }

    /**
     * Constructs a TRecsRecipe given the parameters. Useful for testing.
     */
    public TRecsRecipe(SourceDefinitionParameters sdParameters,
                       ObservationDetailsParameters obsDetailParameters,
                       ObservingConditionParameters obsConditionParameters,
                       TRecsParameters trecsParameters, TeleParameters teleParameters,
                       PlottingDetailsParameters plotParameters,
                       PrintWriter out) throws Exception

    {
        super(out);
        _sdParameters = sdParameters;
        _obsDetailParameters = correctedObsDetails(trecsParameters, obsDetailParameters);
        _obsConditionParameters = obsConditionParameters;
        _trecsParameters = trecsParameters;
        _teleParameters = teleParameters;
        _plotParameters = plotParameters;
    }

    private ObservationDetailsParameters correctedObsDetails(TRecsParameters tp, ObservationDetailsParameters odp) throws Exception {
        // TODO : These corrections were previously done in random places throughout the recipe. I moved them here
        // TODO : so the ObservationDetailsParameters object can become immutable. Basically this calculates
        // TODO : some missing parameters and/or turns the total exposure time into a single exposure time.
        // TODO : This is a temporary hack. There needs to be a better solution for this.
        // NOTE : odp.getExposureTime() carries the TOTAL exposure time (as opposed to exp time for a single frame)
        final TRecs instrument = new TRecs(tp, odp); // TODO: Avoid creating an instrument instance twice.
        final double correctedExposureTime = instrument.getFrameTime();
        final int correctedNumExposures = new Double(odp.getExposureTime() / instrument.getFrameTime() + 0.5).intValue();
        if (odp.getMethod() instanceof ImagingInt) {
            return new ObservationDetailsParameters(
                    new ImagingInt(odp.getSNRatio(), correctedExposureTime, odp.getSourceFraction()),
                    odp.getAnalysis()
            );
        } else if (odp.getMethod() instanceof ImagingSN) {
            return new ObservationDetailsParameters(
                    new ImagingSN(correctedNumExposures, correctedExposureTime, odp.getSourceFraction()),
                    odp.getAnalysis()
            );
        } else if (odp.getMethod() instanceof SpectroscopySN) {
            return new ObservationDetailsParameters(
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
    public void writeOutput() throws Exception {
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

        if (_sdParameters.getDistributionType().equals(SourceDefinitionParameters.Distribution.ELINE))
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters
                    .getELineWavelength() * 1000 / 4))) { // /4 b/c of increased
                // resolution of
                // transmission
                // files
                throw new Exception(
                        "Please use a model line width > 4 nm (or "
                                + (3E5 / (_sdParameters.getELineWavelength() * 1000 / 4))
                                + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }

        VisitableSampledSpectrum sed;

        sed = SEDFactory.getSED(_sdParameters, instrument);
        // sed.applyWavelengthCorrection();

        // ITCChart Chart2 = new ITCChart();

        SampledSpectrumVisitor redshift = new RedshiftVisitor(
                _sdParameters.getRedshift());
        sed.accept(redshift);

        // Must check to see if the redshift has moved the spectrum beyond
        // useful range. The shifted spectrum must completely overlap
        // both the normalization waveband and the observation waveband
        // (filter region).

        final WavebandDefinition band = _sdParameters.getNormBand();
        final double start = band.getStart();
        final double end = band.getEnd();

        // any sed except BBODY and ELINE have normailization regions
        switch (_sdParameters.getDistributionType()) {
            case ELINE:
            case BBODY:
                break;
            default:
                if (sed.getStart() > start || sed.getEnd() < end) {
                    throw new Exception(
                            "Shifted spectrum lies outside of specified normalisation waveband.");
                }
        }

        // if (sed.getStart() > instrument.getObservingStart() ||
        // sed.getEnd() < instrument.getObservingEnd()) {
        // _println(" Sed start" + sed.getStart() + "> than instrument start"+
        // instrument.getObservingStart());
        // _println(" Sed END" + sed.getEnd() + "< than instrument end"+
        // instrument.getObservingEnd());

        // throw new
        // Exception("Shifted spectrum lies outside of observed wavelengths");
        // }

        if (_plotParameters.getPlotLimits().equals(PlottingDetailsParameters.PlotLimits.USER)) {
            if (_plotParameters.getPlotWaveL() > instrument.getObservingEnd()
                    || _plotParameters.getPlotWaveU() < instrument
                    .getObservingStart()) {
                _println(" The user limits defined for plotting do not overlap with the Spectrum.");

                throw new Exception(
                        "User limits for plotting do not overlap with filter.");
            }
        }

        // Module 2
        // Convert input into standard internally-used units.
        //
        // inputs: instrument,redshifted SED, waveband, normalization flux,
        // units
        // calculates: normalized SED, resampled SED, SED adjusted for aperture
        // output: SED in common internal units
        if (!_sdParameters.getDistributionType().equals(SourceDefinitionParameters.Distribution.ELINE)) {
            final SampledSpectrumVisitor norm = new NormalizeVisitor(
                    _sdParameters.getNormBand(),
                    _sdParameters.getSourceNormalization(),
                    _sdParameters.getUnits());
            sed.accept(norm);
        }

        // Resample the spectra for efficiency
        SampledSpectrumVisitor resample = new ResampleWithPaddingVisitor(
                instrument.getObservingStart(), instrument.getObservingEnd(),
                instrument.getSampling(), 0);
        // sed.accept(resample);

        // _println("Sed Start: " + sed.getStart());
        // _println("Sed End:   " + sed.getEnd());
        // _println("Sampling:  " + sed.getSampling());
        // _println("Length:    " + sed.getLength());
        // _println("");

        SampledSpectrumVisitor tel = new TelescopeApertureVisitor();
        sed.accept(tel);

        // SED is now in units of photons/s/nm

        // Module 3b
        // The atmosphere and telescope modify the spectrum and
        // produce a background spectrum.
        //
        // inputs: SED, AIRMASS, sky emmision file, mirror configuration,
        // output: SED and sky background as they arrive at instruments

        SampledSpectrumVisitor clouds = CloudTransmissionVisitor.create(
                _obsConditionParameters.getSkyTransparencyCloud());
        sed.accept(clouds);

        // For mid-IR observation the watervapor percentile and sky background
        // percentile must be the same
        if (!_obsConditionParameters.getSkyTransparencyWaterCategory().equals(_obsConditionParameters.getSkyBackgroundCategory())) {
            _println("");
            _println("Sky background percentile must be equal to sky transparency(water vapor): \n "
                    + "    Please modify the Observing condition constraints section of the HTML form \n"
                    + "    and recalculate.");

            throw new Exception("");
        }

        SampledSpectrumVisitor water = WaterTransmissionVisitor.create(
                _obsConditionParameters.getSkyTransparencyWater(),
                _obsConditionParameters.getAirmass(), "midIR_trans_",
                Site.GS, ITCConstants.MID_IR);
        sed.accept(water);

        // Background spectrum is introduced here.
        VisitableSampledSpectrum sky = SEDFactory.getSED("/"
                + ITCConstants.HI_RES + "/cp"
                + ITCConstants.MID_IR + ITCConstants.SKY_BACKGROUND_LIB + "/"
                + ITCConstants.MID_IR_SKY_BACKGROUND_FILENAME_BASE + "_"
                + _obsConditionParameters.getSkyBackgroundCategory() + "_"
                + _obsConditionParameters.getAirmassCategory()
                + ITCConstants.DATA_SUFFIX, instrument.getSampling());

        // Chart2.addArray(sky.getData(),"Sky");
        // Chart2.addTitle("Original Sky Spectrum");
        // _println(Chart2.getBufferedImage(), "OrigSky");
        // _println("");
        // Chart2.flush();

        // resample sky_background to instrument parameters
        // sky.accept(resample);

        // Apply telescope transmission to both sed and sky
        SampledSpectrumVisitor t = TelescopeTransmissionVisitor.create(_teleParameters);
        sed.accept(t);
        sky.accept(t);

        // _println("Telescope Back ave: " + sky.getAverage());
        // Create and Add background for the telescope.
        SampledSpectrumVisitor tb = new TelescopeBackgroundVisitor(_teleParameters, Site.GS, ITCConstants.MID_IR);
        sky.accept(tb);
        // _println("Telescope Back ave: " + sky.getAverage());

        sky.accept(tel);

        // Add instrument background to sky background for a total background.
        // At this point "sky" is not the right name.
        instrument.addBackground(sky);

        // _println("Telescope Back ave: " + sky.getAverage());

        // Module 4 AO module not implemented
        // The AO module affects source and background SEDs.

        // Module 5b
        // The instrument with its detectors modifies the source and
        // background spectra.
        // input: instrument, source and background SED
        // output: total flux of source and background.
        double before = sky.getAverage();
        instrument.convolveComponents(sed);
        instrument.convolveComponents(sky);

        // _println("Telescope Back ave chage: " + sky.getAverage()/before);

        // Get the summed source and sky
        double sed_integral = sed.getIntegral();
        double sky_integral = sky.getIntegral();

        // For debugging, print the spectrum integrals.
        // _println("SED integral: "+sed_integral+"\tSKY integral: "+sky_integral);
        // _println(sky.printSpecAsString());

        // End of the Spectral energy distribution portion of the ITC.

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
        double ap_pix = 0;
        double sw_ap = 0;
        double Npix = 0;
        double source_fraction = 0;
        double pix_per_sq_arcsec = 0;
        double peak_pixel_count = 0;

        // Calculate image quality
        double im_qual = 0.;
        ImageQualityCalculatable IQcalc =
                ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _teleParameters, instrument);
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
        PeakPixelFluxCalc ppfc;

        if (!_sdParameters.isUniform()) {

            ppfc = new PeakPixelFluxCalc(im_qual, pixel_size,
                    exp_time, sed_integral, sky_integral,
                    instrument.getDarkCurrent());

            peak_pixel_count = ppfc.getFluxInPeakPixel();

        } else {

            ppfc = new PeakPixelFluxCalc(im_qual, pixel_size,
                    exp_time, sed_integral, sky_integral,
                    instrument.getDarkCurrent());

            peak_pixel_count = ppfc.getFluxInPeakPixelUSB(SFcalc.getSourceFraction(), SFcalc.getNPix());
        }

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.
        int binFactor;
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

            // DetectorsTransmissionVisitor dtv =
            // new
            // DetectorsTransmissionVisitor(instrument.getSpectralBinning());

            // sed.accept(dtv);
            // sky.accept(dtv);

            // ChartVisitor TRecsChart = new ChartVisitor();
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
                    exp_time, dark_current
                    * instrument.getSpatialBinning()
                    * instrument.getSpectralBinning(), read_noise,
                    _obsDetailParameters.getSkyApertureDiameter(),
                    instrument.getSpectralBinning());
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

            binFactor = instrument.getSpatialBinning()
                    * instrument.getSpectralBinning();

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

            ImagingS2NCalculatable IS2Ncalc =
                    ImagingS2NCalculationFactory.getCalculationInstance(_sdParameters, _obsDetailParameters, instrument);
            IS2Ncalc.setSedIntegral(sed_integral);
            IS2Ncalc.setSkyIntegral(sky_integral);
            IS2Ncalc.setSkyAperture(_obsDetailParameters
                    .getSkyApertureDiameter());
            IS2Ncalc.setSourceFraction(SFcalc.getSourceFraction());
            IS2Ncalc.setNpix(SFcalc.getNPix());
            IS2Ncalc.setDarkCurrent(instrument.getDarkCurrent()
                    * instrument.getSpatialBinning()
                    * instrument.getSpatialBinning());

            IS2Ncalc.setExtraLowFreqNoise(instrument.getExtraLowFreqNoise());
            IS2Ncalc.calculate();
            _println(IS2Ncalc.getTextResult(device));
            // _println(IS2Ncalc.getBackgroundLimitResult());
            device.setPrecision(0); // NO decimal places
            device.clear();
            binFactor = instrument.getSpatialBinning()
                    * instrument.getSpatialBinning();

            _println("");
            _println("The peak pixel signal + background is "
                    + device.toString(peak_pixel_count) + ". ");// This is " +
            // device.toString(peak_pixel_count/instrument.getWellDepth()*100) +
            // "% of the full well depth of "+device.toString(instrument.getWellDepth())+".");

            // if (peak_pixel_count > (.95*instrument.getWellDepth()*binFactor))
            // _println("Warning: peak pixel may be saturating the (binned) CCD full well of "+
            // .95*instrument.getWellDepth()*binFactor);

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
        _println(_sdParameters.printParameterSummary());
        _println(instrument.toString());
        _println(_teleParameters.printParameterSummary());
        _println(_obsConditionParameters.printParameterSummary());
        _println(_obsDetailParameters.printParameterSummary());
        if (_obsDetailParameters.getMethod().isSpectroscopy()) {
            _println(_plotParameters.printParameterSummary());
            _println(specS2N.getSignalSpectrum(), _header, sigSpec);
            _println(specS2N.getBackgroundSpectrum(), _header, backSpec);
            _println(specS2N.getExpS2NSpectrum(), _header, singleS2N);
            _println(specS2N.getFinalS2NSpectrum(), _header, finalS2N);
        }
    }
}