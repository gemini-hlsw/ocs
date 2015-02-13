package edu.gemini.itc.nici;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.parameters.ObservingConditionParameters;
import edu.gemini.itc.parameters.SourceDefinitionParameters;
import edu.gemini.itc.parameters.TeleParameters;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.ITCRequest;

import java.io.PrintWriter;

/**
 * This class performs the calculations for the Acquisition Camera used for
 * imaging.
 */
public final class NiciRecipe extends RecipeBase {

    // Parameters from the web page.
    private final SourceDefinitionParameters _sdParameters;
    private final ObservationDetailsParameters _obsDetailParameters;
    private final ObservingConditionParameters _obsConditionParameters;
    private final NiciParameters _niciParameters;
    private final TeleParameters _teleParameters;

    /**
     * Constructs an NiciRecipe by parsing a Multi part servlet request.
     *
     * @param r   Servlet request containing form data from ITC web page.
     * @param out Results will be written to this PrintWriter.
     * @throws Exception on failure to parse parameters.
     */
    public NiciRecipe(ITCMultiPartParser r, PrintWriter out) throws Exception {
        super(out);

        // Read parameters from the four main sections of the web page.
        _sdParameters = ITCRequest.sourceDefinitionParameters(r);
        _obsDetailParameters = new ObservationDetailsParameters(r);
        _obsConditionParameters = ITCRequest.obsConditionParameters(r);
        _niciParameters = new NiciParameters(r);
        _teleParameters = ITCRequest.teleParameters(r);
    }

    /**
     * Constructs an NiciRecipe given the parameters. Useful for testing.
     */
    public NiciRecipe(SourceDefinitionParameters sdParameters,
                      ObservationDetailsParameters obsDetailParameters,
                      ObservingConditionParameters obsConditionParameters,
                      NiciParameters niciParameters, TeleParameters teleParameters,
                      PrintWriter out) {
        super(out);
        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _niciParameters = niciParameters;
        _teleParameters = teleParameters;
    }

    /**
     * Performes recipe calculation and writes results to a cached PrintWriter
     * or to System.out.
     *
     * @throws Exception A recipe calculation can fail in many ways, missing data
     *                   files, incorrectly-formatted data files, ...
     */
    public void writeOutput() throws Exception {
        // This object is used to format numerical strings.
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();
        _println("");
        // For debugging, to be removed later
        _print("<pre>" + _sdParameters.toString() + "</pre>");
        _print("<pre>" + _niciParameters.toString() + "</pre>");
        _print("<pre>" + _obsDetailParameters.toString() + "</pre>");
        _print("<pre>" + _obsConditionParameters.toString() + "</pre>");

        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED
        // Nici instrument =
        // new Nici(_niciParameters.getColorFilter(),
        // _niciParameters.getNDFilter());

        // Create two NICI's, one for each channel (may not both be used
        // depending on instrument mode).
        // Really these are NICI channels. It would make the most sense
        // to be able to deal with dual-channel imaging at the instrument
        // level rather than here, but the current ITC was not designed with
        // multiple light paths and detectors in mind.
        Nici instrumentChannel1 = new Nici(_niciParameters.getChannel1Filter(),
                "1", _niciParameters.getPupilMask(),
                _niciParameters.getDichroicPosition());

        Nici instrumentChannel2 = new Nici(_niciParameters.getChannel2Filter(),
                "2", _niciParameters.getPupilMask(),
                _niciParameters.getDichroicPosition());

        _print("<pre> Nici channels created </pre>");
        if (_sdParameters.getDistributionType().equals(SourceDefinitionParameters.Distribution.ELINE))
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters
                    .getELineWavelength() * 1000))) {
                throw new Exception(
                        "Please use a model line width > 1 nm (or "
                                + (3E5 / (_sdParameters.getELineWavelength() * 1000))
                                + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }

        // Determine the full wavelength range for NICI (covers filters in both
        // channels)

        double fullRangeStart;
        double fullRangeEnd;

        if (instrumentChannel1.getObservingStart() < instrumentChannel2
                .getObservingStart())
            fullRangeStart = instrumentChannel1.getObservingStart();
        else
            fullRangeStart = instrumentChannel2.getObservingStart();

        if (instrumentChannel1.getObservingEnd() > instrumentChannel2
                .getObservingEnd())
            fullRangeEnd = instrumentChannel1.getObservingEnd();
        else
            fullRangeEnd = instrumentChannel2.getObservingEnd();

        // Get Source spectrum from factory
        // This SED needs to span both filters
        VisitableSampledSpectrum sed = SEDFactory.getSED(_sdParameters,
                instrumentChannel1.getSampling(), fullRangeStart, fullRangeEnd);

        // Apply redshift if needed
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
            case BBODY:
            case ELINE:
                break;
            default:
                if (sed.getStart() > start || sed.getEnd() < end) {
                    throw new Exception(
                            "Shifted spectrum lies outside of specified normalisation waveband.");
                }
        }

        if (sed.getStart() > fullRangeStart || sed.getEnd() < fullRangeEnd) {
            _println(" Sed start" + sed.getStart() + "> than instrument start"
                    + fullRangeStart);
            _println(" Sed END" + sed.getEnd() + "< than instrument end"
                    + fullRangeEnd);

            throw new Exception(
                    "Shifted spectrum lies outside of observed wavelengths");
        }

        // System.out.println("NON-Normalized SED before telescope aperature:");
        // for (int i=900; i < 4000; i ++)
        // System.out.println("Value at "+i+": "+ sed.getY(i));

        // Module 2
        // Convert input into standard internally-used units.
        //
        // inputs: instrument,redshifted SED, waveband, normalization flux,
        // units
        // calculates: normalized SED, resampled SED, SED adjusted for aperture
        // output: SED in common internal units
        _print("Module 2");
        if (!_sdParameters.getDistributionType().equals(SourceDefinitionParameters.Distribution.ELINE)) {
            final SampledSpectrumVisitor norm = new NormalizeVisitor(
                    _sdParameters.getNormBand(),
                    _sdParameters.getSourceNormalization(),
                    _sdParameters.getUnits());
            sed.accept(norm);
        }

        // Resample the spectra for efficiency
        // SampledSpectrumVisitor resample = new ResampleVisitor(
        // fullRangeStart,
        // fullRangeEnd,
        // instrumentChannel1.getSampling());
        // sed.accept(resample);

        // Trim SED to only include values inside the wavelength range of the
        // filters.
        // The above ResampleVisitor should do that, but is returning SED's with
        // incorrect
        // Y values. A simple trim() should be good enough. Since NICI will be
        // dealing with
        // up to 8 SED's, we need to keep them as small as possible to conserve
        // memory
        sed.trim(fullRangeStart, fullRangeEnd);

        // System.out.println("Full range start/end: "+fullRangeStart+" "+fullRangeEnd);
        // System.out.println("Normalized Resampled SED before telescope aperature:");
        // for (int i=0; i < sed.getLength(); i++){
        // double xVal = sed.getX(i);
        // System.out.println("Value at "+xVal+": "+ sed.getY(xVal));
        // }

        // Create and apply Telescope aperture visitor
        SampledSpectrumVisitor tel = new TelescopeApertureVisitor();
        sed.accept(tel);

        // SED is now in units of photons/s/nm

        // Module 3b
        // The atmosphere and telescope modify the spectrum and
        // produce a background spectrum.
        //
        // inputs: SED, AIRMASS, sky emmision file, mirror configuration,
        // output: SED and sky background as they arrive at instruments
        _print("Module 3b");

        SampledSpectrumVisitor atmos = new AtmosphereVisitor(
                _obsConditionParameters.getAirmass());
        // sed.accept(atmos);

        SampledSpectrumVisitor clouds = CloudTransmissionVisitor.create(
                _obsConditionParameters.getSkyTransparencyCloud());
        sed.accept(clouds);

        SampledSpectrumVisitor water = WaterTransmissionVisitor.create(
                _obsConditionParameters.getSkyTransparencyWater(),
                _obsConditionParameters.getAirmass(), "nearIR_trans_",
                ITCConstants.CERRO_PACHON, ITCConstants.NEAR_IR);
        sed.accept(water);

        // Background spectrum is introduced here.
        VisitableSampledSpectrum sky = SEDFactory.getSED("/"
                + ITCConstants.HI_RES + "/" + ITCConstants.CERRO_PACHON
                + ITCConstants.NEAR_IR + ITCConstants.SKY_BACKGROUND_LIB + "/"
                + ITCConstants.NEAR_IR_SKY_BACKGROUND_FILENAME_BASE + "_"
                + _obsConditionParameters.getSkyBackgroundCategory() + "_"
                + _obsConditionParameters.getAirmassCategory()
                + ITCConstants.DATA_SUFFIX, instrumentChannel1.getSampling());

        // Create and Add Background for the tele

        SampledSpectrumVisitor tb = new TelescopeBackgroundVisitor(_teleParameters, ITCConstants.CERRO_PACHON, ITCConstants.VISIBLE);
        sky.accept(tb);

        // Apply telescope transmission
        SampledSpectrumVisitor t = TelescopeTransmissionVisitor.create(_teleParameters);

        sed.accept(t);
        sky.accept(t);

        // _println("Before Instrument input. SED: "+sed.getIntegral()+" Sky: "+sky.getIntegral());

        sky.accept(tel);

        // Add instrument background to sky background for a total background.
        // At this point "sky" is not the right name
        // For now, background on both NICI channels is equal. Only add
        // background once. Which channel is unimportant.

        instrumentChannel1.addBackground(sky);
        // instrumentChannel2.addBackground(sky);

        // Module 4 AO module not implemented
        // The AO module affects source and background SEDs.

        // Module 5b
        // The instrument with its detectors modifies the source and
        // background spectra.
        // input: instrument, source and background SED
        // output: total flux of source and background.
        _print("Module 4");

        // If in dual-channel mode, reduce flux by 50% due to beamsplitter.
        // Should be done inside NICI's convolve components
        // method, but would require new Dichroic class.
        // Use this method to fudge it for now.
        // if (_niciParameters.isDualChannel())
        // sed.rescaleY(0.468);
        //
        // The above is now down correctly via a Dichroic object added to
        // the list of Nici components.

        // SED's now ready to be modified by instrument optics
        // Split into two, one for each channel
        VisitableSampledSpectrum sedChannel1 = (VisitableSampledSpectrum) sed
                .clone();
        VisitableSampledSpectrum sedChannel2 = sed;

        VisitableSampledSpectrum skyChannel1 = (VisitableSampledSpectrum) sky
                .clone();
        VisitableSampledSpectrum skyChannel2 = sky;

        instrumentChannel1.convolveComponents(sedChannel1);
        instrumentChannel1.convolveComponents(skyChannel1);

        instrumentChannel2.convolveComponents(sedChannel2);
        instrumentChannel2.convolveComponents(skyChannel2);

        // ITCPlot plot2 = new ITCPlot(sky.getDataSource());
        // plot2.addDataSource(sed.getDataSource());
        // plot2.disp();
        String dichroicPosition = _niciParameters.getDichroicPosition();

        // Determine which channel(s) the light from this observation
        // is reaching and do the appropriate calculations for S2N
        if (dichroicPosition.equals("open") || dichroicPosition.equals("h5050")) {
            _println("<B>Channel 1:</B>");
            _println("-------------");
            CalcNiciChannel(sedChannel1, skyChannel1, instrumentChannel1,
                    device);
        }

        if (dichroicPosition.equals("mirror")
                || dichroicPosition.equals("h5050")) {
            _println("<B>Channel 2:</B>");
            _println("----------");
            CalcNiciChannel(sedChannel2, skyChannel2, instrumentChannel2,
                    device);
        }

        // /////////////////////////////////////////////
        // ////////Print Config////////////////////////

        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrumentChannel1.getName() + "\n");
        _println(_sdParameters.printParameterSummary());
        _println(instrumentChannel1.toString());
        _println(_teleParameters.printParameterSummary());
        _println(_obsConditionParameters.printParameterSummary());
        _println(_obsDetailParameters.printParameterSummary());

    }

    // Maybe not the best use of a function, but keeps the process of
    // doing identical calculations on two separate NICI channels managable.
    // Also allows us to keep only 6 SED's in memory at once rather than 8.
    public void CalcNiciChannel(VisitableSampledSpectrum sed,
                                VisitableSampledSpectrum sky, Nici instrument,
                                FormatStringWriter device) throws Exception {

        // double sed_integral = sed.getIntegral();
        // double sky_integral = sky.getIntegral();

        // For debugging, print the spectrum integrals.
        // _println("SED (Channel 1) integral: "+sed_integral+"\tSKY integral: "+sky_integral);

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
        // _print("Module 1a");

        String ap_type = _obsDetailParameters.getApertureType();
        double pixel_size = instrument.getPixelSize();
        double ap_diam = 0;
        double ap_pix = 0;
        double sw_ap = 0;
        double Npix = 0;
        double source_fraction = 0;
        double pix_per_sq_arcsec = 0;
        double peak_pixel_count = 0;
        // _print("Mod1a variables set");

        // Create clone of input SED, which will become the residual halo of the
        // AO system.
        VisitableSampledSpectrum halo = (VisitableSampledSpectrum) sed.clone();

        // Calculate FWHM of the AO Corrected core
        double im_qual = instrument.getAOCorrectedFWHM();

        // Calculate image quality without AO correction
        // double im_qual = 0.;

        ImageQualityCalculatable IQcalc =
                ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _teleParameters, instrument);
        IQcalc.calculate();

        double uncorrected_im_qual = IQcalc.getImageQuality();

        // Get Strehl ratio
        double strehl = instrument.getStrehl();

        // Scale SED's by the strehl ratio to put the appropriate
        // amount of flux in each
        sed.rescaleY(strehl);
        halo.rescaleY(1 - strehl);

        // Get integrals of core, halo, and background SED's
        double sed_integral = sed.getIntegral();
        double sky_integral = sky.getIntegral();
        double halo_integral = halo.getIntegral();

        // Calculate Source fractions of halo and core
        SourceFractionCalculatable SFcalc =
                SourceFractionCalculationFactory.getCalculationInstance(_sdParameters, _obsDetailParameters, instrument);

        // SFcalc.setSFPrint();

        if (_obsDetailParameters.getApertureType().equals(
                _obsDetailParameters.AUTO_APER)) {
            // If the user has selected auto aperature, we need this section to
            // force the
            // halo source fraction be to calculated with an aperature based on
            // the AO
            // corrected FHWM rather than the natural seeing FWHM.
            SFcalc.setApType(_obsDetailParameters.USER_APER);
            SFcalc.setApDiam(1.18 * im_qual);
        }
        SFcalc.setImageQuality(uncorrected_im_qual);
        SFcalc.calculate();
        double halo_source_fraction = SFcalc.getSourceFraction();

        if (_obsDetailParameters.getApertureType().equals(
                _obsDetailParameters.AUTO_APER)) {
            SFcalc.setApType(_obsDetailParameters.AUTO_APER);
        }

        // Reset SFcalc's image quality to AO corrected and calculate source
        // fraction for the core
        SFcalc.setImageQuality(im_qual);
        SFcalc.calculate();

        // _print(SFcalc.getTextResult(device));
        // _println(IQcalc.getTextResult(device));
        // _print("Source fraction calculated");

        // if (_altairParameters.altairIsUsed()) {
        _println("Sky integral per exposure: " + sky_integral
                * _obsDetailParameters.getExposureTime());
        _println("Core integral per exposure: " + sed_integral
                * _obsDetailParameters.getExposureTime());
        _println("Core source_fraction: " + source_fraction);
        _println("Halo integral per exposure: " + halo_integral
                * _obsDetailParameters.getExposureTime());
        _println("Halo source_fraction: " + halo_source_fraction);
        _println("Width of AO corrected core: " + im_qual);
        _println("");
        // }

        // Calculate the Peak Pixel Flux
        // Equal to the peak pixel of core and halo combined
        //
        // Possible problem: Does this method of just adding peak pixel
        // in core and halo add dark current twice?
        PeakPixelFluxCalc ppfc, ppfc_halo;
        double exp_time;

        if (!_sdParameters.isUniform()) {

            ppfc = new PeakPixelFluxCalc(im_qual, pixel_size,
                    _obsDetailParameters.getExposureTime(), sed_integral,
                    sky_integral, instrument.getDarkCurrent());

            ppfc_halo = new PeakPixelFluxCalc(uncorrected_im_qual, pixel_size,
                    _obsDetailParameters.getExposureTime(), halo_integral,
                    sky_integral, instrument.getDarkCurrent());

            peak_pixel_count = ppfc.getFluxInPeakPixel()
                    + ppfc_halo.getFluxInPeakPixel();

        } else {

            ppfc = new PeakPixelFluxCalc(im_qual, pixel_size,
                    _obsDetailParameters.getExposureTime(), sed_integral,
                    sky_integral, instrument.getDarkCurrent());
            peak_pixel_count = ppfc.getFluxInPeakPixelUSB(
                    SFcalc.getSourceFraction(), SFcalc.getNPix());
        }

        // _print("Peak Pixel Flux calculated");

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
        double read_noise = instrument.getReadNoise(_obsDetailParameters
                .getExposureTime());

        // _print("Observation method completed");

        // Calculate the Signal to Noise
        // Includes halo SED via setting secondary integral value in
        // ImagingS2NCalculatable object

        ImagingS2NCalculatable IS2Ncalc =
                ImagingS2NCalculationFactory.getCalculationInstance(_sdParameters, _obsDetailParameters, instrument);
        IS2Ncalc.setSedIntegral(sed_integral);
        IS2Ncalc.setSecondaryIntegral(halo_integral);
        IS2Ncalc.setSecondarySourceFraction(halo_source_fraction);
        IS2Ncalc.setSkyIntegral(sky_integral);
        IS2Ncalc.setSkyAperture(_obsDetailParameters.getSkyApertureDiameter());
        IS2Ncalc.setSourceFraction(SFcalc.getSourceFraction());
        IS2Ncalc.setNpix(SFcalc.getNPix());
        IS2Ncalc.setDarkCurrent(instrument.getDarkCurrent());
        IS2Ncalc.calculate();
        _println(IS2Ncalc.getTextResult(device));
        // _println(IS2Ncalc.getBackgroundLimitResult());
        device.setPrecision(0); // NO decimal places
        device.clear();

        _println("");
        _println("The peak pixel signal + background is "
                + device.toString(peak_pixel_count)
                + ". This is "
                + device.toString(peak_pixel_count / instrument.getWellDepth()
                * 100) + "% of the full well depth of "
                + device.toString(instrument.getWellDepth()) + ".");

        if (peak_pixel_count > (.8 * instrument.getWellDepth()))
            _println("Warning: peak pixel exceeds 80% of the well depth and may be saturated");

        _println("");
        device.setPrecision(2); // TWO decimal places
        device.clear();
    }

}
