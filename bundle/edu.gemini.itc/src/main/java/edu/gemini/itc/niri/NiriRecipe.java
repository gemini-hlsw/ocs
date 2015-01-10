// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: NiriRecipe.java,v 1.17 2004/02/16 18:49:01 bwalls Exp $
//
package edu.gemini.itc.niri;

import java.io.PrintWriter;

import java.util.Enumeration;

import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import edu.gemini.itc.shared.ArraySpectrum;
import edu.gemini.itc.shared.BlackBodySpectrum;
import edu.gemini.itc.shared.DefaultArraySpectrum;
import edu.gemini.itc.shared.EmissionLineSpectrum;
import edu.gemini.itc.shared.FormatStringWriter;
import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.ITCConstants;
import edu.gemini.itc.shared.Gaussian;
import edu.gemini.itc.shared.Recipe;
import edu.gemini.itc.shared.RecipeBase;
import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.shared.SEDFactory;
import edu.gemini.itc.shared.VisitableSampledSpectrum;
import edu.gemini.itc.shared.WavebandDefinition;
import edu.gemini.itc.shared.ITCImageFileIO;
import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ServerInfo;
import edu.gemini.itc.shared.ITCChart;
import edu.gemini.itc.shared.SEDCombination;
import edu.gemini.itc.shared.StopWatch;

import edu.gemini.itc.altair.Altair;
import edu.gemini.itc.altair.AltairBackgroundVisitor;
import edu.gemini.itc.altair.AltairFluxAttenuationVisitor;
import edu.gemini.itc.altair.AltairParameters;
import edu.gemini.itc.altair.AltairTransmissionVisitor;

//import edu.gemini.itc.operation.ChartDataSource;
//import edu.gemini.itc.operation.ChartCreatePNG;
//import edu.gemini.itc.operation.ChartCreate;

import edu.gemini.itc.parameters.ObservingConditionParameters;
import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.parameters.SourceDefinitionParameters;
import edu.gemini.itc.parameters.TeleParameters;
import edu.gemini.itc.parameters.PlottingDetailsParameters;

import edu.gemini.itc.operation.ResampleVisitor;
import edu.gemini.itc.operation.RedshiftVisitor;
import edu.gemini.itc.operation.AtmosphereVisitor;
import edu.gemini.itc.operation.TelescopeApertureVisitor;
import edu.gemini.itc.operation.TelescopeTransmissionVisitor;
import edu.gemini.itc.operation.TelescopeBackgroundVisitor;
import edu.gemini.itc.operation.NormalizeVisitor;
import edu.gemini.itc.operation.CloudTransmissionVisitor;
import edu.gemini.itc.operation.WaterTransmissionVisitor;
//import edu.gemini.itc.operation.ChartVisitor;
import edu.gemini.itc.operation.PeakPixelFluxCalc;
import edu.gemini.itc.operation.SpecS2NVisitor;
import edu.gemini.itc.operation.SlitThroughput;
import edu.gemini.itc.operation.ImageQualityCalculatable;
import edu.gemini.itc.operation.ImageQualityCalculation;
import edu.gemini.itc.operation.ImageQualityCalculationFactory;
import edu.gemini.itc.operation.SourceFractionCalculationFactory;
import edu.gemini.itc.operation.SourceFractionCalculatable;
import edu.gemini.itc.operation.ImagingS2NCalculationFactory;
import edu.gemini.itc.operation.ImagingS2NCalculatable;
import edu.gemini.itc.operation.Calculatable;

import java.util.Calendar;

/**
 * This class performs the calculations for Niri used for imaging.
 */
public final class NiriRecipe extends RecipeBase {
    // Images will be saved to this session object
    // private HttpSession _sessionObject = null; // set from servlet request

    private AltairParameters _altairParameters;
    private StringBuffer _header = new StringBuffer("# NIRI ITC: "
            + Calendar.getInstance().getTime() + "\n");

    private NiriParameters _niriParameters;
    private ObservingConditionParameters _obsConditionParameters;
    private ObservationDetailsParameters _obsDetailParameters;
    private PlottingDetailsParameters _plotParameters;
    // Parameters from the web page.
    private SourceDefinitionParameters _sdParameters;
    private TeleParameters _teleParameters;

    private String sigSpec, backSpec, singleS2N, finalS2N;
    private SpecS2NVisitor specS2N;

    /**
     * Constructs a NiriRecipe by parsing servlet request.
     *
     * @param r   Servlet request containing form data from ITC web page.
     * @param out Results will be written to this PrintWriter.
     * @throws Exception on failure to parse parameters.
     */
    public NiriRecipe(HttpServletRequest r, PrintWriter out) throws Exception {
        super(out);
        // Set the Http Session object
        // _sessionObject = r.getSession(true);

        // Read parameters from the four main sections of the web page.
        _sdParameters = new SourceDefinitionParameters(r);
        _obsDetailParameters = new ObservationDetailsParameters(r);
        _obsConditionParameters = new ObservingConditionParameters(r);
        _niriParameters = new NiriParameters(r);
        _teleParameters = new TeleParameters(r);
        _altairParameters = new AltairParameters(r);
        _plotParameters = new PlottingDetailsParameters(r);
    }

    /**
     * Constructs a NiriRecipe by parsing a Multipart servlet request.
     *
     * @param r   Servlet request containing form data from ITC web page.
     * @param out Results will be written to this PrintWriter.
     * @throws Exception on failure to parse parameters.
     */
    public NiriRecipe(ITCMultiPartParser r, PrintWriter out) throws Exception {
        super(out);
        // Set the Http Session object
        // _sessionObject = r.getSession(true);

        // Read parameters from the four main sections of the web page.
        _sdParameters = new SourceDefinitionParameters(r);
        _obsDetailParameters = new ObservationDetailsParameters(r);
        _obsConditionParameters = new ObservingConditionParameters(r);
        _niriParameters = new NiriParameters(r);
        _teleParameters = new TeleParameters(r);
        _altairParameters = new AltairParameters(r);
        _plotParameters = new PlottingDetailsParameters(r);
    }

    /**
     * Constructs a NiriRecipe given the parameters. Useful for testing.
     */
    public NiriRecipe(SourceDefinitionParameters sdParameters,
                      ObservationDetailsParameters obsDetailParameters,
                      ObservingConditionParameters obsConditionParameters,
                      NiriParameters niriParameters, TeleParameters teleParameters,
                      AltairParameters altairParameters,
                      PlottingDetailsParameters plotParameters,
                      PrintWriter out)

    {
        super(out);
        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _niriParameters = niriParameters;
        _teleParameters = teleParameters;
        _altairParameters = altairParameters;
        _plotParameters = plotParameters;
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

        // NiriChart.setName("Original SED");
        // sed.accept(NiriChart);
        // _println(NiriChart.getTag());
        _println("");
        // ChartVisitor NiriChart = new ChartVisitor();

        // This object is used to format numerical strings.
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();

        // For debugging, to be removed later
        // _print("<pre>" + _sdParameters.toString() + "</pre>");
        // _print("<pre>" + _niriParameters.toString() + "</pre>");
        // _print("<pre>" + _obsDetailParameters.toString() + "</pre>");
        // _print("<pre>" + _obsConditionParameters.toString() + "</pre>");

        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED
        Niri instrument = new Niri(_niriParameters, _obsDetailParameters);

        if (_sdParameters.getSourceSpec().equals(_sdParameters.ELINE))
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

        // ITCChart NiriChart2 = new ITCChart();
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

        String band = _sdParameters.getNormBand();
        double start = WavebandDefinition.getStart(band);
        double end = WavebandDefinition.getEnd(band);

        // any sed except BBODY and ELINE have normailization regions
        if (!(_sdParameters.getSpectrumResource().equals(_sdParameters.ELINE) || _sdParameters
                .getSpectrumResource().equals(_sdParameters.BBODY))) {
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

        if (_plotParameters.getPlotLimits().equals(_plotParameters.USER_LIMITS)) {
            if (_plotParameters.getPlotWaveL() > instrument.getObservingEnd()
                    || _plotParameters.getPlotWaveU() < instrument
                    .getObservingStart()) {
                _println(" The user limits defined for plotting do not overlap with the Spectrum.");

                throw new Exception(
                        "User limits for plotting do not overlap with filter.");
            }// else {
            // NiriChart.setMinMaxX(_obsDetailParameters.getPlotWaveL(),
            // _obsDetailParameters.getPlotWaveU());
            // System.out.println(" L " +
            // _obsDetailParameters.getPlotWaveL() + " U " +
            // _obsDetailParameters.getPlotWaveU());
            // }
        }

        // Module 2
        // Convert input into standard internally-used units.
        //
        // inputs: instrument,redshifted SED, waveband, normalization flux,
        // units
        // calculates: normalized SED, resampled SED, SED adjusted for aperture
        // output: SED in common internal units
        SampledSpectrumVisitor norm = new NormalizeVisitor(
                _sdParameters.getNormBand(),
                _sdParameters.getSourceNormalization(),
                _sdParameters.getUnits());
        if (!_sdParameters.getSpectrumResource().equals(_sdParameters.ELINE)) {// ||
            // !_sdParameters.getSpectrumResource().equals(_sdParameters.BBODY)){
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

        SampledSpectrumVisitor clouds = new CloudTransmissionVisitor(
                _obsConditionParameters.getSkyTransparencyCloud());
        sed.accept(clouds);

        SampledSpectrumVisitor water = new WaterTransmissionVisitor(
                _obsConditionParameters.getSkyTransparencyWater(),
                _obsConditionParameters.getAirmass(), "nearIR_trans_",
                ITCConstants.MAUNA_KEA, ITCConstants.NEAR_IR);
        sed.accept(water);

        // Background spectrum is introduced here.
        VisitableSampledSpectrum sky = SEDFactory.getSED("/"
                + ITCConstants.HI_RES + "/" + ITCConstants.MAUNA_KEA
                + ITCConstants.NEAR_IR + ITCConstants.SKY_BACKGROUND_LIB + "/"
                + ITCConstants.NEAR_IR_SKY_BACKGROUND_FILENAME_BASE + "_"
                + _obsConditionParameters.getSkyTransparencyWaterCategory() + "_"
                + _obsConditionParameters.getAirmassCategory()
                + ITCConstants.DATA_SUFFIX, instrument.getSampling());

        // /NiriChart2.addArray(sky.getData(),"Sky");
        // NiriChart2.addTitle("Original Sky Spectrum");
        // _println(NiriChart2.getBufferedImage(), "OrigSky");
        // _println("");
        // NiriChart2.flush();

        // resample sky_background to instrument parameters
        // sky.accept(resample);

        // System.out.println("Average: " + sky.getAverage());

        // System.out.println("Average: " + sky.getAverage());
        // Apply telescope transmission
        SampledSpectrumVisitor t = new TelescopeTransmissionVisitor(
                _teleParameters.getMirrorCoating(),
                _teleParameters.getInstrumentPort());
        sed.accept(t);
        sky.accept(t);

        // Create and Add background for the telescope.
        SampledSpectrumVisitor tb = new TelescopeBackgroundVisitor(
                _teleParameters.getMirrorCoating(),
                _teleParameters.getInstrumentPort(), ITCConstants.MAUNA_KEA,
                ITCConstants.NEAR_IR);
        sky.accept(tb);

        // DEBUGGING GRAPHS
        // ITCChart DebugChart = new ITCChart();

        // DebugChart.setDomainMinMax(3750, 5750);
        // DebugChart.setRangeMinMax(0, 1000000);
        // DebugChart.addArray(sed.getData(), "Full SED");
        // GnirsChart.addArray(specS2N.getBackgroundSpectrum().getData(),
        // "SQRT(Background)  ");

        // DebugChart.addTitle("DEBUG: SED after atmos and telescope");
        // DebugChart.addxAxisLabel("Wavelength (nm)");
        // DebugChart.addyAxisLabel("e- per exposure per spectral pixel");

        // _println(DebugChart.getBufferedImage(), "DEBUG");
        // _println("");

        // Add instrument background to sky background for a total background.
        // At this point "sky" is not the right name.

        // Moved section where sky/sed is convolved with instrument below Altair
        // section
        // Module 5b
        // The instrument with its detectors modifies the source and
        // background spectra.
        // input: instrument, source and background SED
        // output: total flux of source and background.
        instrument.convolveComponents(sed);

        // For debugging, print the spectrum integrals.
        // _println("SED integral: "+sed_integral+"\tSKY integral: "+sky_integral);

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
        ImageQualityCalculationFactory IQcalcFactory = new ImageQualityCalculationFactory();
        ImageQualityCalculatable IQcalc = (ImageQualityCalculatable) IQcalcFactory
                .getCalculationInstance(_sdParameters, _obsDetailParameters,
                        _obsConditionParameters, _teleParameters, instrument);
        IQcalc.calculate();

        im_qual = IQcalc.getImageQuality();

        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

        // Altair specific section

        if (_altairParameters.altairIsUsed()) {

            if (_obsDetailParameters.getCalculationMode().equals(ObservationDetailsParameters.SPECTROSCOPY)) {
                throw new Exception(
                        "Altair cannot currently be used with Spectroscopy mode in the ITC.  Please deselect either altair or spectroscopy and resubmit the form.");
            }
            Altair altair = new Altair(instrument.getEffectiveWavelength(),
                    _teleParameters.getTelescopeDiameter(), im_qual,
                    _altairParameters.getGuideStarSeperation(),
                    _altairParameters.getGuideStarMagnitude(),
                    _altairParameters.getWFSMode(),
                    _altairParameters.fieldLensIsUsed(),
                    0.0);
            AltairBackgroundVisitor altairBackgroundVisitor = new AltairBackgroundVisitor();
            AltairTransmissionVisitor altairTransmissionVisitor = new AltairTransmissionVisitor();
            AltairFluxAttenuationVisitor altairFluxAttenuationVisitor = new AltairFluxAttenuationVisitor(
                    altair.getFluxAttenuation());
            AltairFluxAttenuationVisitor altairFluxAttenuationVisitorHalo = new AltairFluxAttenuationVisitor(
                    (1 - altair.getStrehl()));
            sky.accept(altairBackgroundVisitor);

            sed.accept(altairTransmissionVisitor);
            sky.accept(altairTransmissionVisitor);

            // Moved Background visitor here so Altair background isn't affected
            // by Altair's own transmission. Correct? - MD 20090723
            // Moved back for now. The instrument background is done the other
            // way (background is affected by instrument transmission)

            // sky.accept(altairBackgroundVisitor);

            halo = (VisitableSampledSpectrum) sed.clone();
            halo.accept(altairFluxAttenuationVisitorHalo);
            sed.accept(altairFluxAttenuationVisitor);

            uncorrected_im_qual = im_qual; // Save uncorrected value for the
            // image quality for later use

            im_qual = altair.getAOCorrectedFWHMc();

            int previousPrecision = device.getPrecision();
            device.setPrecision(3); // Two decimal places
            device.clear();
            _println(altair.printSummary(device));
            // _println(altair.toString());
            device.setPrecision(previousPrecision); // Two decimal places
            device.clear();

        }

        // Instrument background should not be affected by Altair transmission
        // (Altair is above it)
        // This is a change from original code - MD 20090722

        sky.accept(tel);
        instrument.addBackground(sky);
        // sky.accept(tel);

        // Module 4 AO module not implemented
        // The AO module affects source and background SEDs.

        // Must do this here so that Altair background is convolved with
        // instrument ?
        // Does not seem to be set up this way in currently working code on
        // phase1
        // but that code produces incorrect results on my machine. Very
        // confusing - MD 20090722
        instrument.convolveComponents(sky);

        // End of altair specific section.

        double sed_integral = sed.getIntegral();
        double sky_integral = sky.getIntegral();

        double halo_integral = 0;
        if (_altairParameters.altairIsUsed()) {
            halo_integral = halo.getIntegral();
        }

        SourceFractionCalculationFactory SFcalcFactory = new SourceFractionCalculationFactory();
        SourceFractionCalculatable SFcalc = (SourceFractionCalculatable) SFcalcFactory
                .getCalculationInstance(_sdParameters, _obsDetailParameters,
                        _obsConditionParameters, _teleParameters, instrument);

        // if altair is used we need to calculate both a core and halo
        // source_fraction
        // halo first
        if (_altairParameters.altairIsUsed()) {
            // If altair is used turn off printing of SF calc
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
        // this will be the core for an altair source; unchanged for non altair.
        SFcalc.setImageQuality(im_qual);
        SFcalc.calculate();
        source_fraction = SFcalc.getSourceFraction();
        Npix = SFcalc.getNPix();
        if (_obsDetailParameters.getCalculationMode().equals(
                ObservationDetailsParameters.IMAGING)) {
            _print(SFcalc.getTextResult(device));
            if (_altairParameters.altairIsUsed()) {
                _println("derived image halo size (FWHM) for a point source = "
                        + device.toString(uncorrected_im_qual) + " arcsec.\n");
            } else {
                _println(IQcalc.getTextResult(device));
            }
        }

        // if (_altairParameters.altairIsUsed()) {
        // _println("Sky integral per exposure: " +
        // sky_integral*_obsDetailParameters.getExposureTime());
        // _println("Core integral per exposure: " +
        // sed_integral*_obsDetailParameters.getExposureTime());
        // _println("Core source_fraction: " + source_fraction);
        // _println("Halo integral per exposure: " +
        // halo_integral*_obsDetailParameters.getExposureTime());
        // _println("Halo source_fraction: " + halo_source_fraction);
        // _println("");
        // }
        PeakPixelFluxCalc ppfc;

        if (_sdParameters.getSourceGeometry().equals(
                SourceDefinitionParameters.POINT_SOURCE)
                || _sdParameters.getExtendedSourceType().equals(
                SourceDefinitionParameters.GAUSSIAN)) {

            // calculation of image quaility was in here if the current setup
            // does not work copy it back in here from above, and uncomment
            // the section of code below for the uniform surface brightness.
            // the present way should work.

            ppfc = new PeakPixelFluxCalc(im_qual, pixel_size,
                    _obsDetailParameters.getExposureTime(), sed_integral,
                    sky_integral, instrument.getDarkCurrent());

            peak_pixel_count = ppfc.getFluxInPeakPixel();

            if (_altairParameters.altairIsUsed()) {
                PeakPixelFluxCalc ppfc_halo = new PeakPixelFluxCalc(
                        uncorrected_im_qual, pixel_size,
                        _obsDetailParameters.getExposureTime(), halo_integral,
                        sky_integral, instrument.getDarkCurrent());
                // _println("Peak pixel in halo: " +
                // ppfc_halo.getFluxInPeakPixel());
                // _println("Peak pixel in core: " + peak_pixel_count + "\n");
                peak_pixel_count = peak_pixel_count
                        + ppfc_halo.getFluxInPeakPixel();
                // _println("Total peak pixel count: " + peak_pixel_count +
                // " \n");

            }

        } else if (_sdParameters.getExtendedSourceType().equals(
                SourceDefinitionParameters.UNIFORM)) {
            double usbApArea = 0;

            ppfc = new PeakPixelFluxCalc(im_qual, pixel_size,
                    _obsDetailParameters.getExposureTime(), sed_integral,
                    sky_integral, instrument.getDarkCurrent());

            peak_pixel_count = ppfc
                    .getFluxInPeakPixelUSB(source_fraction, Npix);
        } else {
            throw new Exception(
                    "Source geometry not supported for image quality calculation: "
                            + _sdParameters.getSourceGeometry());
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

        // ObservationMode Imaging or spectroscopy

        if (_obsDetailParameters.getCalculationMode().equals(
                ObservationDetailsParameters.SPECTROSCOPY)) {

            SlitThroughput st;// = new SlitThroughput(im_qual,pixel_size,
            // _niriParameters.getFPMask());
            SlitThroughput st_halo;

            // ChartVisitor NiriChart = new ChartVisitor();
            ITCChart NiriChart = new ITCChart();

            if (ap_type.equals(ObservationDetailsParameters.USER_APER)) {
                st = new SlitThroughput(im_qual,
                        _obsDetailParameters.getApertureDiameter(), pixel_size,
                        _niriParameters.getFPMask());
                st_halo = new SlitThroughput(uncorrected_im_qual,
                        _obsDetailParameters.getApertureDiameter(), pixel_size,
                        _niriParameters.getFPMask());

                _println("software aperture extent along slit = "
                        + device.toString(_obsDetailParameters
                        .getApertureDiameter()) + " arcsec");
            } else {
                st = new SlitThroughput(im_qual, pixel_size,
                        _niriParameters.getFPMask());

                st_halo = new SlitThroughput(uncorrected_im_qual, pixel_size,
                        _niriParameters.getFPMask());

                if (_sdParameters.getSourceGeometry().equals(
                        SourceDefinitionParameters.EXTENDED_SOURCE)) {
                    if (_sdParameters.getExtendedSourceType().equals(
                            SourceDefinitionParameters.UNIFORM)) {
                        _println("software aperture extent along slit = "
                                + device.toString(1 / _niriParameters
                                .getFPMask()) + " arcsec");
                    }
                } else {
                    _println("software aperture extent along slit = "
                            + device.toString(1.4 * im_qual) + " arcsec");
                }
            }

            if (_sdParameters.getSourceGeometry().equals(
                    SourceDefinitionParameters.POINT_SOURCE)
                    || _sdParameters.getExtendedSourceType().equals(
                    SourceDefinitionParameters.GAUSSIAN)) {
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
            // System.out.println(" im_qual: " + im_qual + " " + pixel_size);
            ap_diam = st.getSpatialPix();
            double spec_source_frac = st.getSlitThroughput();
            double halo_spec_source_frac = st_halo.getSlitThroughput();

            if (_plotParameters.getPlotLimits().equals(
                    _plotParameters.USER_LIMITS)) {
                NiriChart.setDomainMinMax(_plotParameters.getPlotWaveL(),
                        _plotParameters.getPlotWaveU());
            } else {
                NiriChart.autoscale();
            }

            if (_sdParameters.getSourceGeometry().equals(
                    SourceDefinitionParameters.EXTENDED_SOURCE)) {
                if (_sdParameters.getExtendedSourceType().equals(
                        SourceDefinitionParameters.UNIFORM)) {
                    // im_qual=10000;

                    if (ap_type.equals(ObservationDetailsParameters.USER_APER)) {
                        spec_source_frac = _niriParameters.getFPMask()
                                * ap_diam * pixel_size; // ap_diam = Spec_NPix
                    } else if (ap_type
                            .equals(ObservationDetailsParameters.AUTO_APER)) {
                        ap_diam = new Double(
                                1 / (_niriParameters.getFPMask() * pixel_size) + 0.5)
                                .intValue();
                        spec_source_frac = 1;
                    }
                }
            }

            specS2N = new SpecS2NVisitor(_niriParameters.getFPMask(),
                    pixel_size, instrument.getSpectralPixelWidth(),
                    instrument.getObservingStart(),
                    instrument.getObservingEnd(),
                    instrument.getGrismResolution(), spec_source_frac, im_qual,
                    ap_diam, number_exposures, frac_with_source, exposure_time,
                    dark_current, read_noise);
            specS2N.setSourceSpectrum(sed);
            specS2N.setBackgroundSpectrum(sky);
            specS2N.setHaloImageQuality(uncorrected_im_qual);
            if (_altairParameters.altairIsUsed())
                specS2N.setSpecHaloSourceFraction(halo_spec_source_frac);
            else
                specS2N.setSpecHaloSourceFraction(0.0);

            sed.accept(specS2N);
            _println("<p style=\"page-break-inside: never\">");
            /*
			 * NiriChart.setSeriesName("Signal  ");
			 * NiriChart.setName("Signal and Background ");
			 * //NiriChart.setName("");
			 * NiriChart.setYaxisTitle("e- per exposure per spectral pixel");
			 * NiriChart.setSpectrum(specS2N.getSignalSpectrum());
			 * 
			 * NiriChart.setSeriesName("SQRT(Background)  ");
			 * NiriChart.addSpectrum(specS2N.getBackgroundSpectrum());
			 * specS2N.getBackgroundSpectrum().accept(NiriChart);
			 * //_println(NiriChart.getTag()); _println(NiriChart.getImage(),
			 * "SigAndBack"); _println("");
			 * 
			 * sigSpec = _printSpecTag("ASCII signal spectrum"); backSpec =
			 * _printSpecTag("ASCII background spectrum");
			 * 
			 * 
			 * 
			 * NiriChart.setSeriesName("Single Exp S/N  ");
			 * NiriChart.setName("Intermediate Single Exp and Final S/N");
			 * NiriChart.setYaxisTitle("Signal / Noise per spectral pixel");
			 * 
			 * NiriChart.setSpectrum(specS2N.getExpS2NSpectrum());
			 * 
			 * NiriChart.setSeriesName("Final S/N  ");
			 * NiriChart.addSpectrum(specS2N.getFinalS2NSpectrum());
			 * specS2N.getFinalS2NSpectrum().accept(NiriChart);
			 * //_println(NiriChart.getTag()); _println(NiriChart.getImage(),
			 * "SigAndBack"); _println("");
			 * 
			 * singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
			 * finalS2N = _printSpecTag("Final S/N ASCII data");
			 */

			/*
			 * SATURATION LEVEL
			 * 
			 * NiriChart.addArray(SEDCombination.combine(specS2N.getSignalSpectrum
			 * (
			 * ),specS2N.getBackgroundSpectrum_wo_sqrt(),SEDCombination.ADD).getData
			 * (), "Signal + Background");
			 * NiriChart.addTitle("Total Signal + background");
			 * 
			 * NiriChart.addxAxisLabel("Wavelength (nm)");
			 * NiriChart.addyAxisLabel("e- per exposure per spectral pixel");
			 * 
			 * NiriChart.addHorizontalLine(instrument.getWellDepth()*.8,
			 * java.awt.Color.BLACK, "80% full well");
			 * NiriChart.addHorizontalLine(instrument.getWellDepth(),
			 * java.awt.Color.BLACK, "Saturation Level");
			 * 
			 * _println(NiriChart.getBufferedImage(), "SigPlusBack");
			 * _println("");
			 * 
			 * //sigSpec =
			 * _printSpecTag("ASCII signal plus Background spectrum");
			 * 
			 * NiriChart.flush();
			 */
            NiriChart
                    .addArray(specS2N.getSignalSpectrum().getData(), "Signal ");
            NiriChart.addArray(specS2N.getBackgroundSpectrum().getData(),
                    "SQRT(Background)  ");

            NiriChart
                    .addTitle("Signal and SQRT(Background) in software aperture of "
                            + ap_diam + " pixels");
            NiriChart.addxAxisLabel("Wavelength (nm)");
            NiriChart.addyAxisLabel("e- per exposure per spectral pixel");

            _println(NiriChart.getBufferedImage(), "SigAndBack");
            _println("");

            sigSpec = _printSpecTag("ASCII signal spectrum");
            backSpec = _printSpecTag("ASCII background spectrum");

            NiriChart.flush();

            NiriChart.addArray(specS2N.getExpS2NSpectrum().getData(),
                    "Single Exp S/N");
            NiriChart.addArray(specS2N.getFinalS2NSpectrum().getData(),
                    "Final S/N  ");

            NiriChart.addTitle("Intermediate Single Exp and Final S/N");
            NiriChart.addxAxisLabel("Wavelength (nm)");
            NiriChart.addyAxisLabel("Signal / Noise per spectral pixel");

            _println(NiriChart.getBufferedImage(), "Sig2N");
            _println("");

            singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
            finalS2N = _printSpecTag("Final S/N ASCII data");
            NiriChart.flush();

        } else {

			/*
			 * // Observation method //Get all vars that Both will use
			 * 
			 * 
			 * 
			 * //*****************************************!!!!!! //This is where
			 * we will use the if statement to decide wether to calculate // S/N
			 * given a time or time given a S/N.
			 * //*****************************************!!!!!!
			 * 
			 * // if (_obsDetailParameters.getCalculationMethod().equals( //
			 * ObservationDetailsParameters.S2N)) // {
			 * 
			 * 
			 * // Calculate contributions to noise from different components per
			 * exposure. // The parameters calculated are actually the square of
			 * the noise. // Add contributions in quadrature.
			 * 
			 * // Shot noise on source flux in aperture double var_source =
			 * sed_integral * source_fraction * exposure_time;
			 * 
			 * // Shot noise on background flux in aperture double
			 * var_background = sky_integral * exposure_time * pixel_size *
			 * pixel_size * Npix;
			 * 
			 * // Shot noise on dark current flux in aperture
			 * 
			 * double var_dark = dark_current * Npix * exposure_time;
			 * 
			 * // Readout noise in aperture
			 * 
			 * double var_readout = read_noise * read_noise * Npix;
			 * 
			 * _println(""); _println(
			 * "Contributions to total noise (e-) in aperture (per exposure):");
			 * _println("Source noise = " +
			 * device.toString(Math.sqrt(var_source)));
			 * _println("Background noise = " +
			 * device.toString(Math.sqrt(var_background)));
			 * _println("Dark current noise = " +
			 * device.toString(Math.sqrt(var_dark)));
			 * _println("Readout noise = " +
			 * device.toString(Math.sqrt(var_readout))); _println(""); if
			 * (Math.sqrt(var_source + var_dark +var_readout)
			 * >Math.sqrt(var_background))
			 * _println("Warning: observation is NOT background noise limited");
			 * else _println("Observation is background noise limited.");
			 * 
			 * 
			 * 
			 * device.setPrecision(0); // NO decimal places device.clear();
			 * 
			 * _println("");
			 * //_println("The Well depth, "+device.toString(instrument
			 * .getWellDepth())+", is " + //
			 * device.toString(peak_pixel_count/instrument.getWellDepth()*100) +
			 * //
			 * "% full in the peak pixel, "+device.toString(peak_pixel_count)+
			 * ".");
			 * _println("The peak pixel signal + background is "+device.toString
			 * (peak_pixel_count)+". This is " +
			 * device.toString(peak_pixel_count/instrument.getWellDepth()*100) +
			 * "% of the full well depth of "
			 * +device.toString(instrument.getWellDepth())+".");
			 * 
			 * 
			 * if (peak_pixel_count > (.8*instrument.getWellDepth())) _println(
			 * "Warning: peak pixel exceeds 80% of the well depth and may be saturated"
			 * );
			 * 
			 * device.setPrecision(2); // TWO decimal places device.clear();
			 * 
			 * 
			 * double noise = Math.sqrt(var_source + var_background + var_dark +
			 * var_readout);
			 * 
			 * double sourceless_noise = Math.sqrt(var_background + var_dark +
			 * var_readout);
			 * 
			 * // total source flux in aperture is double signal = sed_integral
			 * * source_fraction * exposure_time;
			 * 
			 * _println(""); _println("Total noise per exposure = " +
			 * device.toString(noise)); _println("Total signal per exposure = "
			 * + device.toString(signal));
			 * 
			 * // S/N ratio is double exp_s2n = signal / noise;
			 * 
			 * 
			 * double final_s2n = Math.sqrt(number_source_exposures) * signal /
			 * Math.sqrt(signal + 2 * sourceless_noise * sourceless_noise);
			 * 
			 * // Calculate the final S/N ratio for Uniform Surface Brightness.
			 * double usb_final_s2n=0; //if
			 * (_sdParameters.getSourceGeometry().equals( //
			 * SourceDefinitionParameters.EXTENDED_SOURCE)) //{ // if
			 * (_sdParameters.getExtendedSourceType(). //
			 * equals(SourceDefinitionParameters.UNIFORM)) // { // final_s2n =
			 * final_s2n*Math.sqrt(pix_per_sq_arcsec); // exp_s2n =
			 * exp_s2n*Math.sqrt(pix_per_sq_arcsec); // } //}
			 * 
			 * if (_obsDetailParameters.getCalculationMethod().equals(
			 * ObservationDetailsParameters.S2N)) {
			 * 
			 * _println(""); _println("S/N per exposure = " +
			 * device.toString(exp_s2n)); _println("");
			 * _println("S/N for the whole observation = " +
			 * device.toString(final_s2n)+ " (including sky subtraction)");
			 * 
			 * _println(""); _println("Requested total integration time = " +
			 * device.toString(exposure_time* number_exposures) +
			 * " secs, of which " + device.toString(exposure_time*
			 * number_exposures* frac_with_source) + " secs is on source." );
			 * 
			 * 
			 * }
			 * 
			 * if (_obsDetailParameters.getCalculationMethod().equals(
			 * ObservationDetailsParameters.INTTIME)) {
			 * 
			 * double req_s2n = _obsDetailParameters.getSNRatio(); int
			 * int_req_source_exposures; double req_number_exposures,
			 * effective_s2n,req_source_exposures;
			 * 
			 * if (_sdParameters.getSourceGeometry().equals(
			 * SourceDefinitionParameters.EXTENDED_SOURCE)) { if
			 * (_sdParameters.getExtendedSourceType().
			 * equals(SourceDefinitionParameters.UNIFORM)) { req_s2n =
			 * req_s2n/Math.sqrt(pix_per_sq_arcsec); } }
			 * 
			 * req_source_exposures = (req_s2n/signal)*(req_s2n/signal)* (signal
			 * + 2*sourceless_noise*sourceless_noise); //
			 * _println("req_source_exposures: " +
			 * device.toString(req_source_exposures));
			 * 
			 * int_req_source_exposures = new
			 * Double(Math.ceil(req_source_exposures)).intValue();
			 * 
			 * req_number_exposures = int_req_source_exposures/frac_with_source;
			 * 
			 * effective_s2n = (Math.sqrt(int_req_source_exposures)*signal)/
			 * Math.sqrt(signal +2*sourceless_noise*sourceless_noise);
			 * 
			 * if (_sdParameters.getSourceGeometry().equals(
			 * SourceDefinitionParameters.EXTENDED_SOURCE)) { if
			 * (_sdParameters.getExtendedSourceType().
			 * equals(SourceDefinitionParameters.UNIFORM)) {
			 * 
			 * effective_s2n = ((Math.sqrt(int_req_source_exposures)* signal)/
			 * Math.sqrt(signal+2*sourceless_noise*sourceless_noise))
			 * Math.sqrt(pix_per_sq_arcsec); } } _println("");
			 * device.setPrecision(0); // NO decimal places device.clear();
			 * 
			 * _print("Derived number of exposures = " +
			 * device.toString(req_number_exposures) + " , of which " +
			 * device.toString(req_number_exposuresfrac_with_source) ); if
			 * (req_number_exposures == 1) _println (" is on source."); else
			 * _println(" are on source.");
			 * 
			 * 
			 * _print("Taking " +
			 * device.toString(Math.ceil(req_number_exposures)));
			 * if(Math.ceil(req_number_exposures)==1) _print(" exposure"); else
			 * _print(" exposures"); _print(", the effective S/N for the whole"
			 * + " observation is " ); device.setPrecision(2); // TWO decimal
			 * places device.clear();
			 * 
			 * _println(device.toString(effective_s2n)+
			 * " (including sky subtraction)");
			 * 
			 * 
			 * _println(""); _println("Required total integration time is " +
			 * device.toString(req_number_exposures* exposure_time) +
			 * " secs, of which " + device.toString(req_number_exposures*
			 * exposure_time* frac_with_source) + " secs is on source.");
			 * 
			 * 
			 * } } //****************THIS IS NOW METHOD C.. MAY BE USED IN THE
			 * FUTURE****** //else perform calculation of inttime given S/N //
			 * }else { // // define variables that will be used. the rest were
			 * defined above. // double
			 * req_s2n=_obsDetailParameters.getSNRatio(); // double
			 * partial_equation; // double derived_exposure_time; // double
			 * derived_int_time; // double read_noise =
			 * instrument.getReadNoise(); // double dark_current =
			 * instrument.getDarkCurrent(); // double
			 * summed_source=sed_integral; // double
			 * summed_background=sky_integral;
			 * 
			 * // if (_sdParameters.getSourceGeometry().equals( //
			 * SourceDefinitionParameters.EXTENDED_SOURCE)) // { // if
			 * (_sdParameters.getExtendedSourceType(). //
			 * equals(SourceDefinitionParameters.UNIFORM)) // { // req_s2n =
			 * req_s2n/Math.sqrt(pix_per_sq_arcsec); // } // }
			 * 
			 * // //equation directly from math cad. // partial_equation
			 * =(req_s2n*source_fraction*summed_source+ //
			 * (2*req_s2n*summed_background*Math.pow(pixel_size,2)* //
			 * Npix+2*req_s2n*dark_current*Npix)+Math.sqrt(Math.pow(req_s2n,2)*
			 * // Math.pow(source_fraction,2)*Math.pow(summed_source,2)+ //
			 * 4*Math.pow(req_s2n,2)*source_fraction*summed_source* //
			 * summed_background*Math.pow(pixel_size,2)*Npix+ //
			 * 4*Math.pow(req_s2n,2)*source_fraction*summed_source* //
			 * dark_current*Npix+4*Math.pow(req_s2n,2)* //
			 * Math.pow(summed_background,2)*Math.pow(pixel_size,4)* //
			 * Math.pow(Npix,2)+8*Math.pow(req_s2n,2)*summed_background* //
			 * Math.pow(pixel_size,2)*Math.pow(Npix,2)*dark_current+4* //
			 * Math.pow(req_s2n,2)*Math.pow(dark_current,2)* //
			 * Math.pow(Npix,2)+8*Math.pow(source_fraction,2)* //
			 * Math.pow(summed_source,2)*number_source_exposures* //
			 * Math.pow(read_noise,2)*Npix));
			 * 
			 * // derived_exposure_time=req_s2n*partial_equation/(2* //
			 * Math.pow(source_fraction,2)* // Math.pow(summed_source,2)* //
			 * number_source_exposures);
			 * 
			 * // derived_int_time = number_exposures*derived_exposure_time;
			 * 
			 * // _println(""); // _println("Derived Exposure time = " + //
			 * device.toString(derived_exposure_time)); // _println(""); //
			 * _println("Derived Integration time = " + //
			 * device.toString(derived_int_time));
			 * 
			 * 
			 * // }
			 */
            // for testing
			/*
			 * if (_sdParameters.getSourceGeometry().
			 * equals(SourceDefinitionParameters.UNIFORM)) {
			 * _println("Final Uniform Surface Brightness S2N: " +
			 * usb_final_s2n); }
			 */

            ImagingS2NCalculationFactory IS2NcalcFactory = new ImagingS2NCalculationFactory();
            ImagingS2NCalculatable IS2Ncalc = (ImagingS2NCalculatable) IS2NcalcFactory
                    .getCalculationInstance(_sdParameters,
                            _obsDetailParameters, _obsConditionParameters,
                            _teleParameters, instrument);
            IS2Ncalc.setSedIntegral(sed_integral);
            if (_altairParameters.altairIsUsed()) {
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

        // _println("");
        _print("<HR align=left SIZE=3>");
        // _println("");

        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(_sdParameters.printParameterSummary());
        _println(instrument.toString());
        if (_altairParameters.altairIsUsed()) {
            _println(_teleParameters.printParameterSummary("altair"));
            _println(_altairParameters.printParameterSummary());
        } else {
            _println(_teleParameters.printParameterSummary());
        }

        _println(_obsConditionParameters.printParameterSummary());
        _println(_obsDetailParameters.printParameterSummary());
        if (_obsDetailParameters.getCalculationMode().equals(
                ObservationDetailsParameters.SPECTROSCOPY)) {
            _println(_plotParameters.printParameterSummary());
        }

        if (_obsDetailParameters.getCalculationMode().equals(
                ObservationDetailsParameters.SPECTROSCOPY)) {
            _println(specS2N.getSignalSpectrum(), _header.toString(), sigSpec);
            _println(specS2N.getBackgroundSpectrum(), _header.toString(),
                    backSpec);
            _println(specS2N.getExpS2NSpectrum(), _header.toString(), singleS2N);
            _println(specS2N.getFinalS2NSpectrum(), _header.toString(),
                    finalS2N);
        }

        sed = null;
        sky = null;
    }

}
