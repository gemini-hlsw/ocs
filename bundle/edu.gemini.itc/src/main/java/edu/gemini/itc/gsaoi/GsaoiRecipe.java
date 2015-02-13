package edu.gemini.itc.gsaoi;

import edu.gemini.itc.gems.*;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.parameters.ObservingConditionParameters;
import edu.gemini.itc.parameters.SourceDefinitionParameters;
import edu.gemini.itc.parameters.TeleParameters;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.ITCRequest;

import java.io.PrintWriter;

/**
 * This class performs the calculations for Gsaoi used for imaging.
 */
public final class GsaoiRecipe extends RecipeBase {

    private final GemsParameters _gemsParameters;
    private final GsaoiParameters _gsaoiParameters;
    private final ObservingConditionParameters _obsConditionParameters;
    private final ObservationDetailsParameters _obsDetailParameters;
    private final SourceDefinitionParameters _sdParameters;
    private final TeleParameters _teleParameters;

    /**
     * Constructs a GsaoiRecipe by parsing a Multipart servlet request.
     *
     * @param r   Servlet request containing form data from ITC web page.
     * @param out Results will be written to this PrintWriter.
     * @throws Exception on failure to parse parameters.
     */
    public GsaoiRecipe(ITCMultiPartParser r, PrintWriter out) throws Exception {
        super(out);

        _sdParameters = ITCRequest.sourceDefinitionParameters(r);
        _obsDetailParameters = new ObservationDetailsParameters(r);
        _obsConditionParameters = ITCRequest.obsConditionParameters(r);
        _gsaoiParameters = new GsaoiParameters(r);
        _teleParameters = ITCRequest.teleParameters(r);
        _gemsParameters = new GemsParameters(r);
    }

    /**
     * Constructs a GsaoiRecipe given the parameters. Useful for testing.
     */
    public GsaoiRecipe(SourceDefinitionParameters sdParameters,
                       ObservationDetailsParameters obsDetailParameters,
                       ObservingConditionParameters obsConditionParameters,
                       GsaoiParameters gsaoiParameters, TeleParameters teleParameters,
                       GemsParameters gemsParameters,
                       PrintWriter out)

    {
        super(out);
        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _gsaoiParameters = gsaoiParameters;
        _teleParameters = teleParameters;
        _gemsParameters = gemsParameters;
    }

    /**
     * Performes recipe calculation and writes results to a cached PrintWriter
     * or to System.out.
     *
     * @throws Exception A recipe calculation can fail in many ways, missing data
     *                   files, incorrectly-formatted data files, ...
     */
    public void writeOutput() throws Exception {
        // Create the Chart visitor. After a sed has been created the chart
        // visitor
        // can be used by calling the following commented out code:

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
        Gsaoi instrument = new Gsaoi(_gsaoiParameters, _obsDetailParameters);

        if (_sdParameters.getDistributionType().equals(SourceDefinitionParameters.Distribution.ELINE))
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters
                    .getELineWavelength() * 1000 * 25))) { // *25 b/c of
                // increased
                // resolution of
                // transmission
                // files
                throw new Exception(
                        "Please use a model line width > 0.04 nm (or "
                                + (3E5 / (_sdParameters.getELineWavelength() * 1000 * 25))
                                + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }

        VisitableSampledSpectrum sed;
        VisitableSampledSpectrum halo;

        // ITCChart GsaoiChart2 = new ITCChart();
        sed = SEDFactory.getSED(_sdParameters, instrument);

        // sed.applyWavelengthCorrection();
        halo = (VisitableSampledSpectrum) sed.clone(); // initialize halo

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

        if (sed.getStart() > instrument.getObservingStart()
                || sed.getEnd() < instrument.getObservingEnd()) {
            _println(" Sed start" + sed.getStart() + "> than instrument start"
                    + instrument.getObservingStart());
            _println(" Sed END" + sed.getEnd() + "< than instrument end"
                    + instrument.getObservingEnd());

            throw new Exception(
                    "Shifted spectrum lies outside of observed wavelengths");
        }

//		if (_plotParameters.getPlotLimits().equals(_plotParameters.USER_LIMITS)) {
//			if (_plotParameters.getPlotWaveL() > instrument.getObservingEnd()
//					|| _plotParameters.getPlotWaveU() < instrument
//							.getObservingStart()) {
//				_println(" The user limits defined for plotting do not overlap with the Spectrum.");
//
//				throw new Exception(
//						"User limits for plotting do not overlap with filter.");
//			}// else {
//				// GsaoiChart.setMinMaxX(_obsDetailParameters.getPlotWaveL(),
//				// _obsDetailParameters.getPlotWaveU());
//				// System.out.println(" L " +
//				// _obsDetailParameters.getPlotWaveL() + " U " +
//				// _obsDetailParameters.getPlotWaveU());
//				// }
//		}

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

        SampledSpectrumVisitor resample = new ResampleVisitor(
                instrument.getObservingStart(), instrument.getObservingEnd(),
                instrument.getSampling());
        // sed.accept(resample);

        SampledSpectrumVisitor tel = new TelescopeApertureVisitor();
        sed.accept(tel);

        // SED is now in units of photons/s/nm

        // Module 3b
        // The atmosphere and telescope modify the spectrum and
        // produce a background spectrum.
        //
        // inputs: SED, AIRMASS, sky emmision file, mirror configuration,
        // output: SED and sky background as they arrive at instruments

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
                + _obsConditionParameters.getSkyTransparencyWaterCategory() + "_"
                + _obsConditionParameters.getAirmassCategory()
                + ITCConstants.DATA_SUFFIX, instrument.getSampling());

        // Apply telescope transmission
        SampledSpectrumVisitor t = TelescopeTransmissionVisitor.create(_teleParameters);
        sed.accept(t);
        sky.accept(t);

        // Create and Add background for the telescope.
        SampledSpectrumVisitor tb = new TelescopeBackgroundVisitor(_teleParameters, ITCConstants.CERRO_PACHON, ITCConstants.NEAR_IR);
        sky.accept(tb);


        // Add instrument background to sky background for a total background.
        // At this point "sky" is not the right name.

        // Moved section where sky/sed is convolved with instrument below Gems
        // section
        // Module 5b
        // The instrument with its detectors modifies the source and
        // background spectra.
        // input: instrument, source and background SED
        // output: total flux of source and background.
        instrument.convolveComponents(sed);

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

        String ap_type = _obsDetailParameters.getApertureType();
        double pixel_size = instrument.getPixelSize();
        double ap_diam = 0;
        double ap_pix = 0;
        double sw_ap = 0;
        double Npix = 0;
        double source_fraction = 0;
        double halo_source_fraction = 0;
        double pix_per_sq_arcsec = 0;
        double peak_pixel_count = 0;

        // Calculate image quality
        double im_qual = 0.;
        double uncorrected_im_qual = 0.;

        // /!!!!!!!!!!!!!!!new Calc Object Checked out!!!!!!!!!!!!!!!!!!
        ImageQualityCalculatable IQcalc =
                ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _teleParameters, instrument);
        IQcalc.calculate();

        im_qual = IQcalc.getImageQuality();

        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

        // Gems specific section

        if (_gemsParameters.gemsIsUsed()) {

            Gems gems = new Gems(instrument.getEffectiveWavelength(),
                    _teleParameters.getTelescopeDiameter(), im_qual,
                    _gemsParameters.getAvgStrehl(), _gemsParameters.getStrehlBand(),
                    _obsConditionParameters.getImageQualityPercentile(),
                    _sdParameters);
            GemsBackgroundVisitor gemsBackgroundVisitor = new GemsBackgroundVisitor();
            GemsTransmissionVisitor gemsTransmissionVisitor = new GemsTransmissionVisitor();
            GemsFluxAttenuationVisitor gemsFluxAttenuationVisitor = new GemsFluxAttenuationVisitor(
                    gems.getFluxAttenuation());
            GemsFluxAttenuationVisitor gemsFluxAttenuationVisitorHalo = new GemsFluxAttenuationVisitor(
                    (1 - gems.getAvgStrehl()));
            sky.accept(gemsBackgroundVisitor);

            sed.accept(gemsTransmissionVisitor);
            sky.accept(gemsTransmissionVisitor);

            halo = (VisitableSampledSpectrum) sed.clone();
            halo.accept(gemsFluxAttenuationVisitorHalo);
            sed.accept(gemsFluxAttenuationVisitor);

            // derived image halo size (FWHM) for a point source
            uncorrected_im_qual = im_qual; // Save uncorrected value for the image quality for later use

            try {
                im_qual = gems.getAOCorrectedFWHM(); // FWHM of an AO-corrected core
            } catch (IllegalArgumentException ex) {
                // If the user selected the wrong IQ (any), show an error and use the value calculated by the old method
                im_qual = gems.getAOCorrectedFWHM_oldVersion();
            }

            int previousPrecision = device.getPrecision();
            device.setPrecision(3); // Two decimal places
            device.clear();
            _println(gems.printSummary(device));
            device.setPrecision(previousPrecision); // Two decimal places
            device.clear();

        }

        // Instrument background should not be affected by Gems transmission
        // (Gems is above it)
        // This is a change from original code - MD 20090722

        sky.accept(tel);
        instrument.addBackground(sky);
        // sky.accept(tel);

        // Module 4 AO module not implemented
        // The AO module affects source and background SEDs.

        // Must do this here so that Gems background is convolved with
        // instrument ?
        // Does not seem to be set up this way in currently working code on
        // phase1
        // but that code produces incorrect results on my machine. Very
        // confusing - MD 20090722
        instrument.convolveComponents(sky);

        // End of gems specific section.

        double sed_integral = sed.getIntegral();
        double sky_integral = sky.getIntegral();

        double halo_integral = 0;
        if (_gemsParameters.gemsIsUsed()) {
            halo_integral = halo.getIntegral();
        }

        SourceFractionCalculatable SFcalc =
                SourceFractionCalculationFactory.getCalculationInstance(_sdParameters, _obsDetailParameters, instrument);

        // if gems is used we need to calculate both a core and halo
        // source_fraction
        // halo first
        if (_gemsParameters.gemsIsUsed()) {
            // If gems is used turn off printing of SF calc
            SFcalc.setSFPrint(false);
            if (_obsDetailParameters.getApertureType().equals(
                    _obsDetailParameters.AUTO_APER)) {
                SFcalc.setApType(_obsDetailParameters.USER_APER);
                SFcalc.setApDiam(1.18 * im_qual);
            }
            SFcalc.setImageQuality(uncorrected_im_qual);
            SFcalc.calculate();
            halo_source_fraction = SFcalc.getSourceFraction();
            if (_obsDetailParameters.getApertureType().equals(
                    _obsDetailParameters.AUTO_APER)) {
                SFcalc.setApType(_obsDetailParameters.AUTO_APER);
            }
        }

        // this will be the core for an gems source; unchanged for non gems.
        SFcalc.setImageQuality(im_qual);
        SFcalc.calculate();
        source_fraction = SFcalc.getSourceFraction();
        Npix = SFcalc.getNPix();
        if (_obsDetailParameters.getCalculationMode().equals(
                ObservationDetailsParameters.IMAGING)) {
            _print(SFcalc.getTextResult(device));
            if (_gemsParameters.gemsIsUsed()) {
                _println("derived image halo size (FWHM) for a point source = "
                        + device.toString(uncorrected_im_qual) + " arcsec.\n");
            } else {
                _println(IQcalc.getTextResult(device));
            }
        }

        PeakPixelFluxCalc ppfc;

        if (!_sdParameters.isUniform()) {

            // calculation of image quaility was in here if the current setup
            // does not work copy it back in here from above, and uncomment
            // the section of code below for the uniform surface brightness.
            // the present way should work.

            ppfc = new PeakPixelFluxCalc(im_qual, pixel_size,
                    _obsDetailParameters.getExposureTime(), sed_integral,
                    sky_integral, instrument.getDarkCurrent());

            peak_pixel_count = ppfc.getFluxInPeakPixel();

            if (_gemsParameters.gemsIsUsed()) {
                PeakPixelFluxCalc ppfc_halo = new PeakPixelFluxCalc(
                        uncorrected_im_qual, pixel_size,
                        _obsDetailParameters.getExposureTime(), halo_integral,
                        sky_integral, instrument.getDarkCurrent());
                peak_pixel_count = peak_pixel_count + ppfc_halo.getFluxInPeakPixel();

            }

        } else  {

            ppfc = new PeakPixelFluxCalc(im_qual, pixel_size,
                    _obsDetailParameters.getExposureTime(), sed_integral,
                    sky_integral, instrument.getDarkCurrent());

            peak_pixel_count = ppfc
                    .getFluxInPeakPixelUSB(source_fraction, Npix);
        }

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.
        int number_exposures = _obsDetailParameters.getNumExposures();
        double frac_with_source = _obsDetailParameters.getSourceFraction();
        double dark_current = instrument.getDarkCurrent();
        double exposure_time = _obsDetailParameters.getExposureTime();
        double read_noise = instrument.getReadNoise();
        // report error if this does not come out to be an integer
        checkSourceFraction(number_exposures, frac_with_source);

        // ObservationMode Imaging

        ImagingS2NCalculatable IS2Ncalc =
                ImagingS2NCalculationFactory.getCalculationInstance(_sdParameters, _obsDetailParameters, instrument);
        IS2Ncalc.setSedIntegral(sed_integral);
        if (_gemsParameters.gemsIsUsed()) {
            IS2Ncalc.setSecondaryIntegral(halo_integral);
            IS2Ncalc.setSecondarySourceFraction(halo_source_fraction);
        }
        IS2Ncalc.setSkyIntegral(sky_integral);
        IS2Ncalc.setSourceFraction(source_fraction);
        IS2Ncalc.setNpix(Npix);
        IS2Ncalc.setDarkCurrent(instrument.getDarkCurrent());
        IS2Ncalc.calculate();
        _println(IS2Ncalc.getTextResult(device));
        _println(IS2Ncalc.getBackgroundLimitResult());
        device.setPrecision(0); // NO decimal places
        device.clear();

        _println("");
        _println("The peak pixel signal + background is "
                + device.toString(peak_pixel_count));

        // REL-1353
        int peak_pixel_percent = (int) (100 * peak_pixel_count / 126000);
        _println("This is "
                + peak_pixel_percent
                + "% of the full well depth of 126000 electrons");
        if (peak_pixel_percent > 65 && peak_pixel_percent <= 85) {
            _error("Warning: the peak pixel + background level exceeds 65% of the well depth and will cause deviations from linearity of more than 5%.");
        } else if (peak_pixel_percent > 85) {
            _error("Warning: the peak pixel + background level exceeds 85% of the well depth and may cause saturation.");
        }

        _println("");
        device.setPrecision(2); // TWO decimal places
        device.clear();

        _print("<HR align=left SIZE=3>");

        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(_sdParameters.printParameterSummary());
        _println(instrument.toString());
        if (_gemsParameters.gemsIsUsed()) {
            _println(printTeleParametersSummary("gems"));
            _println(_gemsParameters.printParameterSummary());
        } else {
            _println(printTeleParametersSummary());
        }
        _println(_obsConditionParameters.printParameterSummary());
        _println(_obsDetailParameters.printParameterSummary());

        sed = null;
        sky = null;
    }

    public String printTeleParametersSummary() {
        return printTeleParametersSummary(_teleParameters.getWFS().displayValue());
    }

    public String printTeleParametersSummary(String wfs) {
        StringBuffer sb = new StringBuffer();
        sb.append("Telescope configuration: \n");
        sb.append("<LI>" + _teleParameters.getMirrorCoating().displayValue() + " mirror coating.\n");
        sb.append("<LI>wavefront sensor: " + wfs + "\n");
        return sb.toString();
    }
}
