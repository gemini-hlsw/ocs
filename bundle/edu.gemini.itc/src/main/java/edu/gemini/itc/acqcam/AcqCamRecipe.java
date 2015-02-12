package edu.gemini.itc.acqcam;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.parameters.ObservingConditionParameters;
import edu.gemini.itc.parameters.SourceDefinitionParameters;
import edu.gemini.itc.parameters.TeleParameters;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.ITCRequest;

import java.io.PrintWriter;

/**
 * This class performs the calculations for the Acquisition Camera
 * used for imaging.
 */
public final class AcqCamRecipe extends RecipeBase {
    // Parameters from the web page.
    private final SourceDefinitionParameters _sdParameters;
    private final ObservationDetailsParameters _obsDetailParameters;
    private final ObservingConditionParameters _obsConditionParameters;
    private final AcquisitionCamParameters _acqCamParameters;
    private final TeleParameters _teleParameters;

    /**
     * Constructs an AcqCamRecipe by parsing a Multi part servlet request.
     *
     * @param r   Servlet request containing form data from ITC web page.
     * @param out Results will be written to this PrintWriter.
     * @throws Exception on failure to parse parameters.
     */
    public AcqCamRecipe(ITCMultiPartParser r, PrintWriter out) {
        super(out);

        // Read parameters from the four main sections of the web page.
        _sdParameters = new SourceDefinitionParameters(r);
        _obsDetailParameters = new ObservationDetailsParameters(r);
        _obsConditionParameters = ITCRequest.obsConditionParameters(r);
        _acqCamParameters = new AcquisitionCamParameters(r);
        _teleParameters = ITCRequest.teleParameters(r);
    }

    /**
     * Constructs an AcqCamRecipe given the parameters.
     * Useful for testing.
     */
    public AcqCamRecipe(SourceDefinitionParameters sdParameters,
                        ObservationDetailsParameters obsDetailParameters,
                        ObservingConditionParameters obsConditionParameters,
                        AcquisitionCamParameters acqCamParameters,
                        TeleParameters teleParameters,
                        PrintWriter out) {
        super(out);

        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _acqCamParameters = acqCamParameters;
        _teleParameters = teleParameters;
    }

    /**
     * Performes recipe calculation and writes results to a cached PrintWriter
     * or to System.out.
     *
     * @throws Exception A recipe calculation can fail in many ways,
     *                   missing data files, incorrectly-formatted data files, ...
     */
    public void writeOutput() {
        // This object is used to format numerical strings.
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2);  // Two decimal places
        device.clear();
        _println("");
        // For debugging, to be removed later
        //_print("<pre>" + _sdParameters.toString() + "</pre>");
        //_print("<pre>" + _acqCamParameters.toString() + "</pre>");
        //_print("<pre>" + _obsDetailParameters.toString() + "</pre>");
        //_print("<pre>" + _obsConditionParameters.toString() + "</pre>");

        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED
        AcquisitionCamera instrument =
                new AcquisitionCamera(_acqCamParameters.getColorFilter(),
                        _acqCamParameters.getNDFilter());


        if (_sdParameters.getSourceSpec().equals(SourceDefinitionParameters.SpectralDistribution.ELINE))
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters.getELineWavelength() * 1000))) {
                throw new IllegalArgumentException("Please use a model line width > 1 nm (or " + (3E5 / (_sdParameters.getELineWavelength() * 1000)) + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }

        //Get Source spectrum from factory
        VisitableSampledSpectrum sed =
                SEDFactory.getSED(_sdParameters,
                        instrument);

        //Apply redshift if needed 
        SampledSpectrumVisitor redshift =
                new RedshiftVisitor(_sdParameters.getRedshift());
        sed.accept(redshift);

        // Must check to see if the redshift has moved the spectrum beyond
        // useful range.  The shifted spectrum must completely overlap
        // both the normalization waveband and the observation waveband
        // (filter region).

        final WavebandDefinition band = _sdParameters.getNormBand();
        final double start = band.getStart();
        final double end = band.getEnd();
        //System.out.println("WStart:" + start + "SStart:" +sed.getStart());
        //System.out.println("WEnd:" + end + "SEnd:" +sed.getEnd());
        //System.out.println("OStart:" + instrument.getObservingStart() + "OEnd:" +instrument.getObservingEnd());


        //any sed except BBODY and ELINE have normailization regions
        switch (_sdParameters.getSourceSpec()) {
            case ELINE:
            case BBODY:
                    break;
            default:
                if (sed.getStart() > start || sed.getEnd() < end) {
                    throw new IllegalArgumentException("Shifted spectrum lies outside of specified normalisation waveband.");
                }
        }

        if (sed.getStart() > instrument.getObservingStart() ||
                sed.getEnd() < instrument.getObservingEnd()) {
            _println(" Sed start" + sed.getStart() + "> than instrument start" + instrument.getObservingStart());
            _println(" Sed END" + sed.getEnd() + "< than instrument end" + instrument.getObservingEnd());

            throw new IllegalArgumentException("Shifted spectrum lies outside of observed wavelengths");
        }


        // Module 2
        // Convert input into standard internally-used units.
        //
        // inputs: instrument,redshifted SED, waveband, normalization flux, units
        // calculates: normalized SED, resampled SED, SED adjusted for aperture
        // output: SED in common internal units
        if (!_sdParameters.getSourceSpec().equals(SourceDefinitionParameters.SpectralDistribution.ELINE)) {
            final SampledSpectrumVisitor norm =
                    new NormalizeVisitor(_sdParameters.getNormBand(),
                            _sdParameters.getSourceNormalization(),
                            _sdParameters.getUnits());
            sed.accept(norm);
        }


        // Resample the spectra for efficiency
        SampledSpectrumVisitor resample = new ResampleVisitor(
                instrument.getObservingStart(),
                instrument.getObservingEnd(),
                instrument.getSampling());
        //sed.accept(resample);

        //Create and apply Telescope aperture visitor
        SampledSpectrumVisitor tel = new TelescopeApertureVisitor();
        sed.accept(tel);

        // SED is now in units of photons/s/nm

        // Module 3b
        // The atmosphere and telescope modify the spectrum and
        // produce a background spectrum.
        //
        // inputs: SED, AIRMASS, sky emmision file, mirror configuration,
        // output: SED and sky background as they arrive at instruments

        SampledSpectrumVisitor atmos =
                new AtmosphereVisitor(_obsConditionParameters.getAirmass());
        //sed.accept(atmos);

        SampledSpectrumVisitor clouds = CloudTransmissionVisitor.create(
                _obsConditionParameters.getSkyTransparencyCloud());
        sed.accept(clouds);


        SampledSpectrumVisitor water = WaterTransmissionVisitor.create(
                _obsConditionParameters.getSkyTransparencyWater(),
                _obsConditionParameters.getAirmass(),
                "skytrans_", ITCConstants.MAUNA_KEA, ITCConstants.VISIBLE);
        sed.accept(water);


        // Background spectrum is introduced here.
        VisitableSampledSpectrum sky =
                SEDFactory.getSED(ITCConstants.SKY_BACKGROUND_LIB + "/" +
                                ITCConstants.OPTICAL_SKY_BACKGROUND_FILENAME_BASE + "_"
                                + _obsConditionParameters.getSkyBackgroundCategory() +
                                "_" + _obsConditionParameters.getAirmassCategory()
                                + ITCConstants.DATA_SUFFIX,
                        instrument.getSampling());


        //Create and Add Background for the tele

        SampledSpectrumVisitor tb = new TelescopeBackgroundVisitor(_teleParameters, ITCConstants.MAUNA_KEA, ITCConstants.VISIBLE);
        sky.accept(tb);


        // Apply telescope transmission
        SampledSpectrumVisitor t = TelescopeTransmissionVisitor.create(_teleParameters);

        sed.accept(t);
        sky.accept(t);


        sky.accept(tel);

        // Add instrument background to sky background for a total background.
        // At this point "sky" is not the right name

        instrument.addBackground(sky);

        // Module 4  AO module not implemented
        // The AO module affects source and background SEDs.

        // Module 5b
        // The instrument with its detectors modifies the source and
        // background spectra.
        // input: instrument, source and background SED
        // output: total flux of source and background.


        instrument.convolveComponents(sed);
        instrument.convolveComponents(sky);

        //ITCPlot plot2 = new ITCPlot(sky.getDataSource());
        //plot2.addDataSource(sed.getDataSource());
        //plot2.disp();


        double sed_integral = sed.getIntegral();
        double sky_integral = sky.getIntegral();

        // For debugging, print the spectrum integrals.
        //_println("SED integral: "+sed_integral+"\tSKY integral: "+sky_integral);

        // End of the Spectral energy distribution portion of the ITC.


        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio.  There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification

        String ap_type = _obsDetailParameters.getApertureType();
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


//Calculate Source fraction
        SourceFractionCalculatable SFcalc =
                SourceFractionCalculationFactory.getCalculationInstance(_sdParameters, _obsDetailParameters, instrument);
        SFcalc.setImageQuality(im_qual);
        SFcalc.calculate();
        _print(SFcalc.getTextResult(device));
        _println(IQcalc.getTextResult(device));

// Calculate the Peak Pixel Flux
        PeakPixelFluxCalc ppfc;

        if (!_sdParameters.sourceIsUniform()) {

            ppfc = new
                    PeakPixelFluxCalc(im_qual, pixel_size,
                    _obsDetailParameters.getExposureTime(),
                    sed_integral, sky_integral,
                    instrument.getDarkCurrent());

            peak_pixel_count = ppfc.getFluxInPeakPixel();

        } else {

            ppfc = new
                    PeakPixelFluxCalc(im_qual, pixel_size,
                    _obsDetailParameters.getExposureTime(),
                    sed_integral, sky_integral,
                    instrument.getDarkCurrent());
            peak_pixel_count = ppfc.getFluxInPeakPixelUSB(SFcalc.getSourceFraction(), SFcalc.getNPix());

        }

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.


        // Observation method

        int number_exposures = _obsDetailParameters.getNumExposures();
        double frac_with_source = _obsDetailParameters.getSourceFraction();

        // report error if this does not come out to be an integer
        checkSourceFraction(number_exposures, frac_with_source);

        double exposure_time = _obsDetailParameters.getExposureTime();
        double dark_current = instrument.getDarkCurrent();
        double read_noise = instrument.getReadNoise();

        //Calculate the Signal to Noise

        ImagingS2NCalculatable IS2Ncalc =
                ImagingS2NCalculationFactory.getCalculationInstance(_sdParameters, _obsDetailParameters, instrument);
        IS2Ncalc.setSedIntegral(sed_integral);
        IS2Ncalc.setSkyIntegral(sky_integral);
        IS2Ncalc.setSkyAperture(_obsDetailParameters.getSkyApertureDiameter());
        IS2Ncalc.setSourceFraction(SFcalc.getSourceFraction());
        IS2Ncalc.setNpix(SFcalc.getNPix());
        IS2Ncalc.setDarkCurrent(instrument.getDarkCurrent());
        IS2Ncalc.calculate();
        _println(IS2Ncalc.getTextResult(device));
        //_println(IS2Ncalc.getBackgroundLimitResult());
        device.setPrecision(0);  // NO decimal places
        device.clear();

        _println("");
        _println("The peak pixel signal + background is " + device.toString(peak_pixel_count) + ". This is " +
                device.toString(peak_pixel_count / instrument.getWellDepth() * 100) +
                "% of the full well depth of " + device.toString(instrument.getWellDepth()) + ".");

        if (peak_pixel_count > (.8 * instrument.getWellDepth()))
            _println("Warning: peak pixel exceeds 80% of the well depth and may be saturated");

        _println("");
        device.setPrecision(2);  // TWO decimal places
        device.clear();


        ///////////////////////////////////////////////
        //////////Print Config////////////////////////

        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(_sdParameters.printParameterSummary());
        _println(instrument.toString());
        _println(_teleParameters.printParameterSummary());
        _println(_obsConditionParameters.printParameterSummary());
        _println(_obsDetailParameters.printParameterSummary());

    }
}
