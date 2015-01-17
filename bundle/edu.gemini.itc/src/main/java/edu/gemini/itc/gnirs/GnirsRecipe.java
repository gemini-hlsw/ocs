// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
//
package edu.gemini.itc.gnirs;

import java.io.PrintWriter;

import java.util.Enumeration;

import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

//import edu.gemini.itc.altair.Altair;
//import edu.gemini.itc.altair.AltairBackgroundVisitor;
//import edu.gemini.itc.altair.AltairFluxAttenuationVisitor;
//import edu.gemini.itc.altair.AltairParameters;
//import edu.gemini.itc.altair.AltairTransmissionVisitor;
import edu.gemini.itc.shared.FormatStringWriter;
import edu.gemini.itc.shared.GaussianMorphology;
import edu.gemini.itc.shared.HexagonalAperture;
import edu.gemini.itc.shared.ITCConstants;
import edu.gemini.itc.shared.RecipeBase;
import edu.gemini.itc.shared.SampledSpectrumVisitor;
import edu.gemini.itc.shared.SEDFactory;
import edu.gemini.itc.shared.USBMorphology;
import edu.gemini.itc.shared.VisitableMorphology;
import edu.gemini.itc.shared.VisitableSampledSpectrum;
import edu.gemini.itc.shared.WavebandDefinition;
import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.ITCChart;

import edu.gemini.itc.parameters.ObservingConditionParameters;
import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.parameters.SourceDefinitionParameters;
import edu.gemini.itc.parameters.TeleParameters;
import edu.gemini.itc.parameters.PlottingDetailsParameters;

import edu.gemini.itc.operation.ResampleWithPaddingVisitor;
import edu.gemini.itc.operation.RedshiftVisitor;
import edu.gemini.itc.operation.TelescopeApertureVisitor;
import edu.gemini.itc.operation.TelescopeTransmissionVisitor;
import edu.gemini.itc.operation.TelescopeBackgroundVisitor;
import edu.gemini.itc.operation.NormalizeVisitor;
import edu.gemini.itc.operation.CloudTransmissionVisitor;
import edu.gemini.itc.operation.WaterTransmissionVisitor;
import edu.gemini.itc.operation.PeakPixelFluxCalc;
import edu.gemini.itc.operation.SpecS2NLargeSlitVisitor;
import edu.gemini.itc.operation.SlitThroughput;
import edu.gemini.itc.operation.ImageQualityCalculatable;
import edu.gemini.itc.operation.ImageQualityCalculationFactory;
import edu.gemini.itc.operation.SourceFractionCalculationFactory;
import edu.gemini.itc.operation.SourceFractionCalculatable;
import edu.gemini.itc.operation.ImagingS2NCalculationFactory;
import edu.gemini.itc.operation.ImagingS2NCalculatable;

import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;

/**
 * This class performs the calculations for Gnirs used for imaging.
 */
public final class GnirsRecipe extends RecipeBase {
    // Images will be saved to this session object
    // private HttpSession _sessionObject = null; // set from servlet request

    //    private AltairParameters _altairParameters; // REL-472: Commenting out Altair option for now
    private Calendar now = Calendar.getInstance();
    private String _header = new StringBuffer("# GNIRS ITC: "
            + now.getTime() + "\n").toString();

    private String sigSpec, backSpec, singleS2N, finalS2N;
    private SpecS2NLargeSlitVisitor specS2N;

    // Parameters from the web page.
    private SourceDefinitionParameters _sdParameters;
    private ObservationDetailsParameters _obsDetailParameters;
    private ObservingConditionParameters _obsConditionParameters;
    private GnirsParameters _gnirsParameters;
    private TeleParameters _teleParameters;
    private PlottingDetailsParameters _plotParameters;

    private VisitableSampledSpectrum signalOrder3, signalOrder4, signalOrder5,
            signalOrder6, signalOrder7, signalOrder8;
    private VisitableSampledSpectrum backGroundOrder3, backGroundOrder4,
            backGroundOrder5, backGroundOrder6, backGroundOrder7,
            backGroundOrder8;
    private VisitableSampledSpectrum finalS2NOrder3, finalS2NOrder4,
            finalS2NOrder5, finalS2NOrder6, finalS2NOrder7, finalS2NOrder8;

    /**
     * Constructs a GnirsRecipe by parsing servlet request.
     *
     * @param r   Servlet request containing form data from ITC web page.
     * @param out Results will be written to this PrintWriter.
     * @throws Exception on failure to parse parameters.
     */
    public GnirsRecipe(HttpServletRequest r, PrintWriter out) throws Exception {
        super(out);
        // Read parameters from the four main sections of the web page.
        _sdParameters = new SourceDefinitionParameters(r);
        _obsDetailParameters = new ObservationDetailsParameters(r);
        _obsConditionParameters = new ObservingConditionParameters(r);
        _gnirsParameters = new GnirsParameters(r);
        _teleParameters = new TeleParameters(r);
//        _altairParameters = new AltairParameters(r); // REL-472: Commenting out Altair option for now
        _plotParameters = new PlottingDetailsParameters(r);
    }

    /**
     * Constructs a GnirsRecipe by parsing a Multipart servlet request.
     *
     * @param r   Servlet request containing form data from ITC web page.
     * @param out Results will be written to this PrintWriter.
     * @throws Exception on failure to parse parameters.
     */
    public GnirsRecipe(ITCMultiPartParser r, PrintWriter out) throws Exception {
        super(out);
        // Read parameters from the four main sections of the web page.
        _sdParameters = new SourceDefinitionParameters(r);
        _obsDetailParameters = new ObservationDetailsParameters(r);
        _obsConditionParameters = new ObservingConditionParameters(r);
        _gnirsParameters = new GnirsParameters(r);
        _teleParameters = new TeleParameters(r);
//        _altairParameters = new AltairParameters(r); // REL-472: Commenting out Altair option for now
        _plotParameters = new PlottingDetailsParameters(r);
    }

    /**
     * Constructs a GnirsRecipe given the parameters. Useful for testing.
     */
    public GnirsRecipe(SourceDefinitionParameters sdParameters,
                       ObservationDetailsParameters obsDetailParameters,
                       ObservingConditionParameters obsConditionParameters,
                       GnirsParameters gnirsParameters, TeleParameters teleParameters,
//                       AltairParameters altairParameters, // REL-472: Commenting out Altair option for now
                       PlottingDetailsParameters plotParameters,
                       PrintWriter out)

    {
        super(out);
        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _gnirsParameters = gnirsParameters;
        _teleParameters = teleParameters;
//        _altairParameters = altairParameters;// REL-472: Commenting out Altair option for now
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
        Gnirs instrument;
        ITCChart GnirsChart2 = new ITCChart();
        GnirsChart2.setDomainMinMax(1150, 1270);

        //instrument = new GnirsSouth(_gnirsParameters, _obsDetailParameters);
        instrument = new GnirsNorth(_gnirsParameters, _obsDetailParameters);   // Added on 2/27/2014 (see REL-480)

        if (_sdParameters.getSourceSpec().equals(_sdParameters.ELINE))
            // *25 b/c of increased resolutuion of transmission files
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters.getELineWavelength() * 1000 * 25))) {
                throw new Exception(
                        "Please use a model line width > 0.04 nm (or "
                                + (3E5 / (_sdParameters.getELineWavelength() * 1000 * 25))
                                + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }

        VisitableSampledSpectrum sed;
        VisitableSampledSpectrum halo;

        sed = SEDFactory.getSED(_sdParameters, instrument);
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

        if (_plotParameters.getPlotLimits().equals(_plotParameters.USER_LIMITS)) {
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
        SampledSpectrumVisitor norm = new NormalizeVisitor(
                _sdParameters.getNormBand(),
                _sdParameters.getSourceNormalization(),
                _sdParameters.getUnits());
        if (!_sdParameters.getSpectrumResource().equals(_sdParameters.ELINE)) {
            sed.accept(norm);
        }

        // Resample the spectra for efficiency
        SampledSpectrumVisitor resample = new ResampleWithPaddingVisitor(
                instrument.getObservingStart(), instrument.getObservingEnd(),
                instrument.getSampling(), 0);

        SampledSpectrumVisitor tel = new TelescopeApertureVisitor();
        sed.accept(tel);

        // SED is now in units of photons/s/nm

        // Module 3b
        // The atmosphere and telescope modify the spectrum and
        // produce a background spectrum.
        //
        // inputs: SED, AIRMASS, sky emmision file, mirror configuration,
        // output: SED and sky background as they arrive at instruments

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

        // resample sky_background to instrument parameters
        // sky.accept(resample);

        // Apply telescope transmission to both sed and sky
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

        // DebugChart.setDomainMinMax(3000, 4000);
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
        List sf_list = new ArrayList();
        List ap_offset_list = new ArrayList();

        // Calculate image quality
        double im_qual = 0.;
        double uncorrected_im_qual = 0.;

        ImageQualityCalculatable IQcalc =
                ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _teleParameters, instrument);
        IQcalc.calculate();

        im_qual = IQcalc.getImageQuality();


        // REL-472: Commenting out Altair option for now
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

        // Altair specific section

//        if (_altairParameters.altairIsUsed()) {
//
//            Altair altair = new Altair(instrument.getEffectiveWavelength(),
//                    _teleParameters.getTelescopeDiameter(), im_qual,
//                    _altairParameters.getGuideStarSeperation(),
//                    _altairParameters.getGuideStarMagnitude(),
//                    _altairParameters.getWFSMode(),
//                    _altairParameters.fieldLensIsUsed(),
//                    0.0); // See REL-472 (was 0.1)
//            AltairBackgroundVisitor altairBackgroundVisitor = new AltairBackgroundVisitor();
//            AltairTransmissionVisitor altairTransmissionVisitor = new AltairTransmissionVisitor();
//            AltairFluxAttenuationVisitor altairFluxAttenuationVisitor = new AltairFluxAttenuationVisitor(
//                    altair.getFluxAttenuation());
//            AltairFluxAttenuationVisitor altairFluxAttenuationVisitorHalo = new AltairFluxAttenuationVisitor(
//                    (1 - altair.getStrehl()));
//            sky.accept(altairBackgroundVisitor);
//
//            sed.accept(altairTransmissionVisitor);
//            sky.accept(altairTransmissionVisitor);
//
//            // Moved Background visitor here so Altair background isn't affected
//            // by Altair's own transmission. Correct? - MD 20090723
//            // Moved back for now. The instrument background is done the other
//            // way (background is affected by instrument transmission)
//
//            // sky.accept(altairBackgroundVisitor);
//
//            halo = (VisitableSampledSpectrum) sed.clone();
//            halo.accept(altairFluxAttenuationVisitorHalo);
//            sed.accept(altairFluxAttenuationVisitor);
//
//            uncorrected_im_qual = im_qual; // Save uncorrected value for the
//            // image quality for later use
//
//            im_qual = altair.getAOCorrectedFWHMc();
//
//
////            // XXX REL-472: Not sure about this
////            VisitableMorphology morph = new GaussianMorphology(im_qual);
////         	morph.accept(new HexagonalAperture(0, 0, 0.10));
//
//
//            int previousPrecision = device.getPrecision();
//            device.setPrecision(3); // Two decimal places
//            device.clear();
//            _println(altair.printSummary(device));
//            // _println(altair.toString());
//            device.setPrecision(previousPrecision); // Two decimal places
//            device.clear();
//
//        }


        // Instrument background should not be affected by Altair transmission
        // (Altair is above it)
        // This is a change from original code - MD 20090722

        sky.accept(tel);
        instrument.addBackground(sky);

        // Module 4 AO module not implemented
        // The AO module affects source and background SEDs.

        // Module 5b
        // The instrument with its detectors modifies the source and
        // background spectra.
        // input: instrument, source and background SED
        // output: total flux of source and background.

//        instrument.convolveComponents(sed);
        instrument.convolveComponents(sky);

        // _println("Total photons/s between 1205 - 1215: "+
        // sed.getIntegral(1205.0,1215.0));
        // sed.accept(resample);
        // sky.accept(resample);

        // Get the summed source and sky
        double sed_integral = sed.getIntegral();
        double sky_integral = sky.getIntegral();

        double halo_integral = 0;
        // REL-472: Commenting out Altair option for now
//        if (_altairParameters.altairIsUsed()) {
//            halo_integral = halo.getIntegral();
//        }

        // Calculate the Fraction of source in the aperture
        SourceFractionCalculatable SFcalc =
                SourceFractionCalculationFactory.getCalculationInstance(_sdParameters, _obsDetailParameters, instrument);

        // REL-472: Commenting out Altair option for now
//        // if altair is used we need to calculate both a core and halo
//        // source_fraction
//        // halo first
//        if (_altairParameters.altairIsUsed()) {
//            // If altair is used turn off printing of SF calc
//            SFcalc.setSFPrint(false);
//            if (_obsDetailParameters.getApertureType().equals(
//                    _obsDetailParameters.AUTO_APER)) {
//                SFcalc.setApType(_obsDetailParameters.USER_APER);
//                SFcalc.setApDiam(1.18 * im_qual);
//            }
//            SFcalc.setImageQuality(uncorrected_im_qual);
//            SFcalc.calculate();
//            halo_source_fraction = SFcalc.getSourceFraction();
//            if (_obsDetailParameters.getApertureType().equals(
//                    _obsDetailParameters.AUTO_APER)) {
//                SFcalc.setApType(_obsDetailParameters.AUTO_APER);
//            }
//        }

        // this will be the core for an altair source; unchanged for non altair.
        SFcalc.setImageQuality(im_qual);
        SFcalc.calculate();
        source_fraction = SFcalc.getSourceFraction();
        Npix = SFcalc.getNPix();
        if (_obsDetailParameters.getCalculationMode().equals(
                ObservationDetailsParameters.IMAGING)) {
            _print(SFcalc.getTextResult(device));
            _println(IQcalc.getTextResult(device));
            _println("Sky subtraction aperture = "
                    + _obsDetailParameters.getSkyApertureDiameter()
                    + " times the software aperture.\n");

//            // REL-472: Commenting out Altair option for now
//            if (_altairParameters.altairIsUsed()) {
//                _println("derived image halo size (FWHM) for a point source = "
//                        + device.toString(uncorrected_im_qual) + " arcsec.\n");
//            } else {
            _println(IQcalc.getTextResult(device));
//            }
        }

        // Calculate the Peak Pixel Flux
        PeakPixelFluxCalc ppfc;

        if (_sdParameters.getSourceGeometry().equals(
                SourceDefinitionParameters.POINT_SOURCE)
                || _sdParameters.getExtendedSourceType().equals(
                SourceDefinitionParameters.GAUSSIAN)) {

            ppfc = new PeakPixelFluxCalc(im_qual, pixel_size,
                    _obsDetailParameters.getExposureTime(), sed_integral,
                    sky_integral, instrument.getDarkCurrent());

            peak_pixel_count = ppfc.getFluxInPeakPixel();

//            // REL-472: Commenting out Altair option for now
//            if (_altairParameters.altairIsUsed()) {
//                PeakPixelFluxCalc ppfc_halo = new PeakPixelFluxCalc(
//                        uncorrected_im_qual, pixel_size,
//                        _obsDetailParameters.getExposureTime(), halo_integral,
//                        sky_integral, instrument.getDarkCurrent());
//                // _println("Peak pixel in halo: " +
//                // ppfc_halo.getFluxInPeakPixel());
//                // _println("Peak pixel in core: " + peak_pixel_count + "\n");
//                peak_pixel_count = peak_pixel_count
//                        + ppfc_halo.getFluxInPeakPixel();
//                // _println("Total peak pixel count: " + peak_pixel_count +
//                // " \n");
//
//            }
        } else if (_sdParameters.getExtendedSourceType().equals(
                SourceDefinitionParameters.UNIFORM)) {
            double usbApArea = 0;

            ppfc = new PeakPixelFluxCalc(im_qual, pixel_size,
                    _obsDetailParameters.getExposureTime(), sed_integral,
                    sky_integral, instrument.getDarkCurrent());

            peak_pixel_count = ppfc
                    .getFluxInPeakPixelUSB(source_fraction, Npix);
        } else {
            throw new Exception("Peak Pixel could not be calculated ");
        }

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.
        int binFactor;
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

            SlitThroughput st;
            SlitThroughput st_halo = null;

            // ChartVisitor GnirsChart = new ChartVisitor();
            ITCChart GnirsChart = new ITCChart();

            if (ap_type.equals(ObservationDetailsParameters.USER_APER)) {
                st = new SlitThroughput(im_qual,
                        _obsDetailParameters.getApertureDiameter(),
                        pixel_size, _gnirsParameters.getFPMask());
                st_halo = new SlitThroughput(uncorrected_im_qual,
                        _obsDetailParameters.getApertureDiameter(), pixel_size,
                        _gnirsParameters.getFPMask());
                _println("software aperture extent along slit = "
                        + device.toString(_obsDetailParameters
                        .getApertureDiameter()) + " arcsec");
            } else {
                st = new SlitThroughput(im_qual, pixel_size, _gnirsParameters.getFPMask());
                st_halo = new SlitThroughput(uncorrected_im_qual, pixel_size, _gnirsParameters.getFPMask());

                if (_sdParameters.getSourceGeometry().equals(
                        SourceDefinitionParameters.EXTENDED_SOURCE)) {
                    if (_sdParameters.getExtendedSourceType().equals(
                            SourceDefinitionParameters.UNIFORM)) {
                        _println("software aperture extent along slit = "
                                + device.toString(1 / _gnirsParameters
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
                    + device.toString(im_qual) + "arcsec\n");

            _println("Sky subtraction aperture = "
                    + _obsDetailParameters.getSkyApertureDiameter()
                    + " times the software aperture.");

            _println("");
            _println("Requested total integration time = "
                    + device.toString(exposure_time * number_exposures)
                    + " secs, of which "
                    + device.toString(exposure_time * number_exposures
                    * frac_with_source) + " secs is on source.");

            _print("<HR align=left SIZE=3>");

            ap_diam = st.getSpatialPix(); // ap_diam really Spec_Npix on

            // REL-472: Since GNIRS does not have perfect optics, the PSF delivered by Altair should then be
            // convolved with a ~0.10" Gaussian to reproduce the ~0.12" images which are measured under optimal conditions.
            // XXX TODO: Not sure how to do this. See comments in REL-472


            double spec_source_frac = st.getSlitThroughput();
            double halo_spec_source_frac = 0;
            if (st_halo != null) halo_spec_source_frac = st_halo.getSlitThroughput();

            if (_plotParameters.getPlotLimits().equals(
                    _plotParameters.USER_LIMITS)) {
                GnirsChart.setDomainMinMax(_plotParameters.getPlotWaveL(),
                        _plotParameters.getPlotWaveU());
            } else {
                GnirsChart.autoscale();
            }

            // For the usb case we want the resolution to be determined by the
            // slit width and not the image quality for a point source.
            if (_sdParameters.getSourceGeometry().equals(
                    SourceDefinitionParameters.EXTENDED_SOURCE)) {
                if (_sdParameters.getExtendedSourceType().equals(
                        SourceDefinitionParameters.UNIFORM)) {
                    im_qual = 10000;

                    if (ap_type
                            .equals(ObservationDetailsParameters.USER_APER)) {
                        spec_source_frac = _gnirsParameters.getFPMask()
                                * ap_diam * pixel_size; // Spec_NPix
                    } else if (ap_type
                            .equals(ObservationDetailsParameters.AUTO_APER)) {
                        ap_diam = new Double(
                                1 / (_gnirsParameters.getFPMask() * pixel_size) + 0.5)
                                .intValue();
                        spec_source_frac = 1;
                    }
                }
            }

            specS2N = new SpecS2NLargeSlitVisitor(
                    _gnirsParameters.getFPMask(), pixel_size,
                    instrument.getSpectralPixelWidth() / instrument.getOrder(),
                    instrument.getObservingStart(),
                    instrument.getObservingEnd(),
                    instrument.getGratingDispersion_nm(),
                    instrument.getGratingDispersion_nmppix(),
                    instrument.getGratingResolution(),
                    spec_source_frac,
                    im_qual, ap_diam,
                    number_exposures,
                    frac_with_source,
                    exposure_time,
                    dark_current * instrument.getSpatialBinning() * instrument.getSpectralBinning(),
                    read_noise,
                    _obsDetailParameters.getSkyApertureDiameter(),
                    instrument.getSpectralBinning());

            // DEBUG
            // _println("RESOLUTION DEBUGGING");
            // _println("gratingDispersion_nm: " +
            // instrument.getGratingDispersion_nm());
            // _println("slit_width: " + _gnirsParameters.getFPMask());
            // _println("source sampling: " + sed.getSampling());

            _println("<p style=\"page-break-inside: never\">");

            specS2N.setDetectorTransmission(instrument
                    .getDetectorTransmision());

            if (instrument.XDisp_IsUsed()) {
                // sed.trim(700, 2533);
                VisitableSampledSpectrum sedOrder3 = (VisitableSampledSpectrum) sed.clone();
                VisitableSampledSpectrum sedOrder4 = (VisitableSampledSpectrum) sed.clone();
                VisitableSampledSpectrum sedOrder5 = (VisitableSampledSpectrum) sed.clone();
                VisitableSampledSpectrum sedOrder6 = (VisitableSampledSpectrum) sed.clone();
                VisitableSampledSpectrum sedOrder7 = (VisitableSampledSpectrum) sed.clone();
                VisitableSampledSpectrum sedOrder8 = (VisitableSampledSpectrum) sed.clone();
                // sky.trim(700, 2533);
                VisitableSampledSpectrum skyOrder3 = (VisitableSampledSpectrum) sky.clone();
                VisitableSampledSpectrum skyOrder4 = (VisitableSampledSpectrum) sky.clone();
                VisitableSampledSpectrum skyOrder5 = (VisitableSampledSpectrum) sky.clone();
                VisitableSampledSpectrum skyOrder6 = (VisitableSampledSpectrum) sky.clone();
                VisitableSampledSpectrum skyOrder7 = (VisitableSampledSpectrum) sky.clone();
                VisitableSampledSpectrum skyOrder8 = (VisitableSampledSpectrum) sky.clone();

                double trimCenter;
                if (instrument.getGrating().equals(_gnirsParameters.G110))
                    trimCenter = _gnirsParameters.getUnXDispCentralWavelength();
                else
                    trimCenter = 2200.0;

                sedOrder3.accept(instrument.getGratingOrderNTransmission(3));
                sedOrder3.trim(
                        trimCenter
                                * 3
                                / 3
                                - (instrument.getGratingDispersion_nmppix()
                                / 3 * instrument.DETECTOR_PIXELS / 2),
                        trimCenter
                                * 3
                                / 3
                                + (instrument
                                .getGratingDispersion_nmppix()
                                / 3
                                * instrument.DETECTOR_PIXELS / 2));
                skyOrder3
                        .accept(instrument.getGratingOrderNTransmission(3));
                skyOrder3
                        .trim(trimCenter
                                * 3
                                / 3
                                - (instrument.getGratingDispersion_nmppix()
                                / 3 * instrument.DETECTOR_PIXELS / 2),
                                trimCenter
                                        * 3
                                        / 3
                                        + (instrument
                                        .getGratingDispersion_nmppix()
                                        / 3
                                        * instrument.DETECTOR_PIXELS / 2));
                sedOrder4
                        .accept(instrument.getGratingOrderNTransmission(4));
                sedOrder4
                        .trim(trimCenter
                                * 3
                                / 4
                                - (instrument.getGratingDispersion_nmppix()
                                / 4 * instrument.DETECTOR_PIXELS / 2),
                                trimCenter
                                        * 3
                                        / 4
                                        + (instrument
                                        .getGratingDispersion_nmppix()
                                        / 4
                                        * instrument.DETECTOR_PIXELS / 2));
                skyOrder4
                        .accept(instrument.getGratingOrderNTransmission(4));
                skyOrder4
                        .trim(trimCenter
                                * 3
                                / 4
                                - (instrument.getGratingDispersion_nmppix()
                                / 4 * instrument.DETECTOR_PIXELS / 2),
                                trimCenter
                                        * 3
                                        / 4
                                        + (instrument
                                        .getGratingDispersion_nmppix()
                                        / 4
                                        * instrument.DETECTOR_PIXELS / 2));
                sedOrder5
                        .accept(instrument.getGratingOrderNTransmission(5));
                sedOrder5
                        .trim(trimCenter
                                * 3
                                / 5
                                - (instrument.getGratingDispersion_nmppix()
                                / 5 * instrument.DETECTOR_PIXELS / 2),
                                trimCenter
                                        * 3
                                        / 5
                                        + (instrument
                                        .getGratingDispersion_nmppix()
                                        / 5
                                        * instrument.DETECTOR_PIXELS / 2));
                skyOrder5
                        .accept(instrument.getGratingOrderNTransmission(5));
                skyOrder5
                        .trim(trimCenter
                                * 3
                                / 5
                                - (instrument.getGratingDispersion_nmppix()
                                / 5 * instrument.DETECTOR_PIXELS / 2),
                                trimCenter
                                        * 3
                                        / 5
                                        + (instrument
                                        .getGratingDispersion_nmppix()
                                        / 5
                                        * instrument.DETECTOR_PIXELS / 2));
                sedOrder6
                        .accept(instrument.getGratingOrderNTransmission(6));
                sedOrder6
                        .trim(trimCenter
                                * 3
                                / 6
                                - (instrument.getGratingDispersion_nmppix()
                                / 6 * instrument.DETECTOR_PIXELS / 2),
                                trimCenter
                                        * 3
                                        / 6
                                        + (instrument
                                        .getGratingDispersion_nmppix()
                                        / 6
                                        * instrument.DETECTOR_PIXELS / 2));
                skyOrder6
                        .accept(instrument.getGratingOrderNTransmission(6));
                skyOrder6
                        .trim(trimCenter
                                * 3
                                / 6
                                - (instrument.getGratingDispersion_nmppix()
                                / 6 * instrument.DETECTOR_PIXELS / 2),
                                trimCenter
                                        * 3
                                        / 6
                                        + (instrument
                                        .getGratingDispersion_nmppix()
                                        / 6
                                        * instrument.DETECTOR_PIXELS / 2));
                sedOrder7
                        .accept(instrument.getGratingOrderNTransmission(7));
                sedOrder7
                        .trim(trimCenter
                                * 3
                                / 7
                                - (instrument.getGratingDispersion_nmppix()
                                / 7 * instrument.DETECTOR_PIXELS / 2),
                                trimCenter
                                        * 3
                                        / 7
                                        + (instrument
                                        .getGratingDispersion_nmppix()
                                        / 7
                                        * instrument.DETECTOR_PIXELS / 2));
                skyOrder7
                        .accept(instrument.getGratingOrderNTransmission(7));
                skyOrder7
                        .trim(trimCenter
                                * 3
                                / 7
                                - (instrument.getGratingDispersion_nmppix()
                                / 7 * instrument.DETECTOR_PIXELS / 2),
                                trimCenter
                                        * 3
                                        / 7
                                        + (instrument
                                        .getGratingDispersion_nmppix()
                                        / 7
                                        * instrument.DETECTOR_PIXELS / 2));
                sedOrder8
                        .accept(instrument.getGratingOrderNTransmission(8));
                sedOrder8
                        .trim(trimCenter
                                * 3
                                / 8
                                - (instrument.getGratingDispersion_nmppix()
                                / 8 * instrument.DETECTOR_PIXELS / 2),
                                trimCenter
                                        * 3
                                        / 8
                                        + (instrument
                                        .getGratingDispersion_nmppix()
                                        / 8
                                        * instrument.DETECTOR_PIXELS / 2));
                skyOrder8
                        .accept(instrument.getGratingOrderNTransmission(8));
                skyOrder8
                        .trim(trimCenter
                                * 3
                                / 8
                                - (instrument.getGratingDispersion_nmppix()
                                / 8 * instrument.DETECTOR_PIXELS / 2),
                                trimCenter
                                        * 3
                                        / 8
                                        + (instrument
                                        .getGratingDispersion_nmppix()
                                        / 8
                                        * instrument.DETECTOR_PIXELS / 2));

                GnirsChart
                        .addTitle("Signal and Background in software aperture of "
                                + ap_diam + " pixels");
                GnirsChart.addxAxisLabel("Wavelength (nm)");
                GnirsChart
                        .addyAxisLabel("e- per exposure per spectral pixel");

                specS2N.setSourceSpectrum(sedOrder3);
                specS2N.setBackgroundSpectrum(skyOrder3);

                specS2N.setGratingDispersion_nmppix(instrument
                        .getGratingDispersion_nmppix() / 3);
                specS2N.setGratingDispersion_nm(instrument
                        .getGratingDispersion_nm() / 3);
                specS2N.setSpectralPixelWidth(instrument
                        .getSpectralPixelWidth() / 3);

                specS2N.setStartWavelength(sedOrder3.getStart());
                specS2N.setEndWavelength(sedOrder3.getEnd());

                sed.accept(specS2N);
                GnirsChart.addArray(specS2N.getSignalSpectrum().getData(),
                        "Signal Order 3",
                        org.jfree.chart.ChartColor.DARK_RED);
                GnirsChart.addArray(specS2N.getBackgroundSpectrum()
                        .getData(), "SQRT(Background) Order 3 ",
                        org.jfree.chart.ChartColor.VERY_LIGHT_RED);

                signalOrder3 = (VisitableSampledSpectrum) specS2N
                        .getSignalSpectrum().clone();
                backGroundOrder3 = (VisitableSampledSpectrum) specS2N
                        .getBackgroundSpectrum().clone();

                specS2N.setSourceSpectrum(sedOrder4);
                specS2N.setBackgroundSpectrum(skyOrder4);

                specS2N.setGratingDispersion_nmppix(instrument
                        .getGratingDispersion_nmppix() / 4);
                specS2N.setGratingDispersion_nm(instrument
                        .getGratingDispersion_nm() / 4);
                specS2N.setSpectralPixelWidth(instrument
                        .getSpectralPixelWidth() / 4);

                specS2N.setStartWavelength(sedOrder4.getStart());
                specS2N.setEndWavelength(sedOrder4.getEnd());

                sed.accept(specS2N);

                GnirsChart.addArray(specS2N.getSignalSpectrum().getData(),
                        "Signal Order 4",
                        org.jfree.chart.ChartColor.DARK_BLUE);
                GnirsChart.addArray(specS2N.getBackgroundSpectrum()
                        .getData(), "SQRT(Background)  Order 4",
                        org.jfree.chart.ChartColor.VERY_LIGHT_BLUE);

                signalOrder4 = (VisitableSampledSpectrum) specS2N
                        .getSignalSpectrum().clone();
                backGroundOrder4 = (VisitableSampledSpectrum) specS2N
                        .getBackgroundSpectrum().clone();

                specS2N.setSourceSpectrum(sedOrder5);
                specS2N.setBackgroundSpectrum(skyOrder5);

                specS2N.setGratingDispersion_nmppix(instrument
                        .getGratingDispersion_nmppix() / 5);
                specS2N.setGratingDispersion_nm(instrument
                        .getGratingDispersion_nm() / 5);
                specS2N.setSpectralPixelWidth(instrument
                        .getSpectralPixelWidth() / 5);

                specS2N.setStartWavelength(sedOrder5.getStart());
                specS2N.setEndWavelength(sedOrder5.getEnd());

                sed.accept(specS2N);

                GnirsChart.addArray(specS2N.getSignalSpectrum().getData(),
                        "Signal Order 5",
                        org.jfree.chart.ChartColor.DARK_GREEN);
                GnirsChart.addArray(specS2N.getBackgroundSpectrum()
                        .getData(), "SQRT(Background)  Order 5",
                        org.jfree.chart.ChartColor.VERY_LIGHT_GREEN);

                signalOrder5 = (VisitableSampledSpectrum) specS2N
                        .getSignalSpectrum().clone();
                backGroundOrder5 = (VisitableSampledSpectrum) specS2N
                        .getBackgroundSpectrum().clone();

                specS2N.setSourceSpectrum(sedOrder6);
                specS2N.setBackgroundSpectrum(skyOrder6);

                specS2N.setGratingDispersion_nmppix(instrument
                        .getGratingDispersion_nmppix() / 6);
                specS2N.setGratingDispersion_nm(instrument
                        .getGratingDispersion_nm() / 6);
                specS2N.setSpectralPixelWidth(instrument
                        .getSpectralPixelWidth() / 6);

                specS2N.setStartWavelength(sedOrder6.getStart());
                specS2N.setEndWavelength(sedOrder6.getEnd());

                sed.accept(specS2N);

                GnirsChart.addArray(specS2N.getSignalSpectrum().getData(),
                        "Signal Order 6",
                        org.jfree.chart.ChartColor.DARK_MAGENTA);
                GnirsChart.addArray(specS2N.getBackgroundSpectrum()
                        .getData(), "SQRT(Background) Order 6",
                        org.jfree.chart.ChartColor.VERY_LIGHT_MAGENTA);

                signalOrder6 = (VisitableSampledSpectrum) specS2N
                        .getSignalSpectrum().clone();
                backGroundOrder6 = (VisitableSampledSpectrum) specS2N
                        .getBackgroundSpectrum().clone();

                specS2N.setSourceSpectrum(sedOrder7);
                specS2N.setBackgroundSpectrum(skyOrder7);

                specS2N.setGratingDispersion_nmppix(instrument
                        .getGratingDispersion_nmppix() / 7);
                specS2N.setGratingDispersion_nm(instrument
                        .getGratingDispersion_nm() / 7);
                specS2N.setSpectralPixelWidth(instrument
                        .getSpectralPixelWidth() / 7);

                specS2N.setStartWavelength(sedOrder7.getStart());
                specS2N.setEndWavelength(sedOrder7.getEnd());

                sed.accept(specS2N);

                GnirsChart.addArray(specS2N.getSignalSpectrum().getData(),
                        "Signal Order 7", org.jfree.chart.ChartColor.black);
                GnirsChart.addArray(specS2N.getBackgroundSpectrum()
                        .getData(), "SQRT(Background) Order 7",
                        org.jfree.chart.ChartColor.lightGray);

                signalOrder7 = (VisitableSampledSpectrum) specS2N
                        .getSignalSpectrum().clone();
                backGroundOrder7 = (VisitableSampledSpectrum) specS2N
                        .getBackgroundSpectrum().clone();

                specS2N.setSourceSpectrum(sedOrder8);
                specS2N.setBackgroundSpectrum(skyOrder8);

                specS2N.setGratingDispersion_nmppix(instrument
                        .getGratingDispersion_nmppix() / 8);
                specS2N.setGratingDispersion_nm(instrument
                        .getGratingDispersion_nm() / 8);
                specS2N.setSpectralPixelWidth(instrument
                        .getSpectralPixelWidth() / 8);

                specS2N.setStartWavelength(sedOrder8.getStart());
                specS2N.setEndWavelength(sedOrder8.getEnd());

                sed.accept(specS2N);

                GnirsChart.addArray(specS2N.getSignalSpectrum().getData(),
                        "Signal Order 8",
                        org.jfree.chart.ChartColor.DARK_CYAN);
                GnirsChart.addArray(specS2N.getBackgroundSpectrum()
                        .getData(), "SQRT(Background) Order 8",
                        org.jfree.chart.ChartColor.VERY_LIGHT_CYAN);

                signalOrder8 = (VisitableSampledSpectrum) specS2N
                        .getSignalSpectrum().clone();
                backGroundOrder8 = (VisitableSampledSpectrum) specS2N
                        .getBackgroundSpectrum().clone();

                _println(GnirsChart.getBufferedImage(), "SigAndBack");
                _println("");

                sigSpec = _printSpecTag("ASCII signal spectrum");
                backSpec = _printSpecTag("ASCII background spectrum");

                GnirsChart.flush();

                // GnirsChart.addTitle("Intermediate Single Exp and Final S/N");
                GnirsChart.addTitle("Final S/N");
                GnirsChart.addxAxisLabel("Wavelength (nm)");
                GnirsChart
                        .addyAxisLabel("Signal / Noise per spectral pixel");

                specS2N.setSourceSpectrum(sedOrder3);
                specS2N.setBackgroundSpectrum(skyOrder3);

                specS2N.setGratingDispersion_nmppix(instrument
                        .getGratingDispersion_nmppix() / 3);
                specS2N.setGratingDispersion_nm(instrument
                        .getGratingDispersion_nm() / 3);
                specS2N.setSpectralPixelWidth(instrument
                        .getSpectralPixelWidth() / 3);

                specS2N.setStartWavelength(sedOrder3.getStart());
                specS2N.setEndWavelength(sedOrder3.getEnd());

                sed.accept(specS2N);

                // GnirsChart.addArray(specS2N.getExpS2NSpectrum().getData(),
                // "Single Exp S/N Order 3");
                GnirsChart.addArray(
                        specS2N.getFinalS2NSpectrum().getData(),
                        "Final S/N Order 3",
                        org.jfree.chart.ChartColor.DARK_RED);

                finalS2NOrder3 = (VisitableSampledSpectrum) specS2N
                        .getFinalS2NSpectrum().clone();

                specS2N.setSourceSpectrum(sedOrder4);
                specS2N.setBackgroundSpectrum(skyOrder4);

                specS2N.setGratingDispersion_nmppix(instrument
                        .getGratingDispersion_nmppix() / 4);
                specS2N.setGratingDispersion_nm(instrument
                        .getGratingDispersion_nm() / 4);
                specS2N.setSpectralPixelWidth(instrument
                        .getSpectralPixelWidth() / 4);

                specS2N.setStartWavelength(sedOrder4.getStart());
                specS2N.setEndWavelength(sedOrder4.getEnd());

                sed.accept(specS2N);

                // GnirsChart.addArray(specS2N.getExpS2NSpectrum().getData(),
                // "Single Exp S/N Order 4");
                GnirsChart.addArray(
                        specS2N.getFinalS2NSpectrum().getData(),
                        "Final S/N Order 4",
                        org.jfree.chart.ChartColor.DARK_BLUE);

                finalS2NOrder4 = (VisitableSampledSpectrum) specS2N
                        .getFinalS2NSpectrum().clone();

                specS2N.setSourceSpectrum(sedOrder5);
                specS2N.setBackgroundSpectrum(skyOrder5);

                specS2N.setGratingDispersion_nmppix(instrument
                        .getGratingDispersion_nmppix() / 5);
                specS2N.setGratingDispersion_nm(instrument
                        .getGratingDispersion_nm() / 5);
                specS2N.setSpectralPixelWidth(instrument
                        .getSpectralPixelWidth() / 5);

                specS2N.setStartWavelength(sedOrder5.getStart());
                specS2N.setEndWavelength(sedOrder5.getEnd());

                sed.accept(specS2N);

                // GnirsChart.addArray(specS2N.getExpS2NSpectrum().getData(),
                // "Single Exp S/N Order 5");
                GnirsChart.addArray(
                        specS2N.getFinalS2NSpectrum().getData(),
                        "Final S/N Order 5",
                        org.jfree.chart.ChartColor.DARK_GREEN);

                finalS2NOrder5 = (VisitableSampledSpectrum) specS2N
                        .getFinalS2NSpectrum().clone();

                specS2N.setSourceSpectrum(sedOrder6);
                specS2N.setBackgroundSpectrum(skyOrder6);

                specS2N.setGratingDispersion_nmppix(instrument
                        .getGratingDispersion_nmppix() / 6);
                specS2N.setGratingDispersion_nm(instrument
                        .getGratingDispersion_nm() / 6);
                specS2N.setSpectralPixelWidth(instrument
                        .getSpectralPixelWidth() / 6);

                specS2N.setStartWavelength(sedOrder6.getStart());
                specS2N.setEndWavelength(sedOrder6.getEnd());

                sed.accept(specS2N);

                // GnirsChart.addArray(specS2N.getExpS2NSpectrum().getData(),
                // "Single Exp S/N Order 6");
                GnirsChart.addArray(
                        specS2N.getFinalS2NSpectrum().getData(),
                        "Final S/N Order 6",
                        org.jfree.chart.ChartColor.DARK_MAGENTA);

                finalS2NOrder6 = (VisitableSampledSpectrum) specS2N
                        .getFinalS2NSpectrum().clone();

                specS2N.setSourceSpectrum(sedOrder7);
                specS2N.setBackgroundSpectrum(skyOrder7);

                specS2N.setGratingDispersion_nmppix(instrument
                        .getGratingDispersion_nmppix() / 7);
                specS2N.setGratingDispersion_nm(instrument
                        .getGratingDispersion_nm() / 7);
                specS2N.setSpectralPixelWidth(instrument
                        .getSpectralPixelWidth() / 7);

                specS2N.setStartWavelength(sedOrder7.getStart());
                specS2N.setEndWavelength(sedOrder7.getEnd());

                sed.accept(specS2N);

                // GnirsChart.addArray(specS2N.getExpS2NSpectrum().getData(),
                // "Single Exp S/N Order 7");
                GnirsChart.addArray(
                        specS2N.getFinalS2NSpectrum().getData(),
                        "Final S/N Order 7",
                        org.jfree.chart.ChartColor.black);

                finalS2NOrder7 = (VisitableSampledSpectrum) specS2N
                        .getFinalS2NSpectrum().clone();

                specS2N.setSourceSpectrum(sedOrder8);
                specS2N.setBackgroundSpectrum(skyOrder8);

                specS2N.setGratingDispersion_nmppix(instrument
                        .getGratingDispersion_nmppix() / 8);
                specS2N.setGratingDispersion_nm(instrument
                        .getGratingDispersion_nm() / 8);
                specS2N.setSpectralPixelWidth(instrument
                        .getSpectralPixelWidth() / 8);

                specS2N.setStartWavelength(sedOrder8.getStart());
                specS2N.setEndWavelength(sedOrder8.getEnd());

                sed.accept(specS2N);

                // GnirsChart.addArray(specS2N.getExpS2NSpectrum().getData(),
                // "Single Exp S/N Order 8");
                GnirsChart.addArray(
                        specS2N.getFinalS2NSpectrum().getData(),
                        "Final S/N Order 8",
                        org.jfree.chart.ChartColor.DARK_CYAN);

                finalS2NOrder8 = (VisitableSampledSpectrum) specS2N
                        .getFinalS2NSpectrum().clone();

                _println(GnirsChart.getBufferedImage(), "Sig2N");
                _println("");

                // singleS2N =
                // _printSpecTag("Single Exposure S/N ASCII data");
                finalS2N = _printSpecTag("Final S/N ASCII data");

                GnirsChart.flush();

            } else {

                // Added 20100924. As far as I can tell the grating
                // transmission was not applied
                // unless you were in cross-dispersed mode. I'm adding it
                // here. MD
                sed.accept(instrument
                        .getGratingOrderNTransmission(instrument.getOrder()));

                specS2N.setSourceSpectrum(sed);
                specS2N.setBackgroundSpectrum(sky);
                specS2N.setHaloImageQuality(uncorrected_im_qual);

//                // REL-472: Commenting out Altair option for now
//                if (_altairParameters.altairIsUsed()) {
//                    specS2N.setSpecHaloSourceFraction(halo_spec_source_frac);
//                }
//                else {
                specS2N.setSpecHaloSourceFraction(0.0);
//                }

                sed.accept(specS2N);

                GnirsChart.addArray(specS2N.getSignalSpectrum().getData(),
                        "Signal ");
                GnirsChart.addArray(specS2N.getBackgroundSpectrum()
                        .getData(), "SQRT(Background)  ");

                GnirsChart.addTitle("Signal and Background in software aperture of "
                        + ap_diam + " pixels");

                GnirsChart.addxAxisLabel("Wavelength (nm)");
                GnirsChart
                        .addyAxisLabel("e- per exposure per spectral pixel");

                _println(GnirsChart.getBufferedImage(), "SigAndBack");
                _println("");

                sigSpec = _printSpecTag("ASCII signal spectrum");
                backSpec = _printSpecTag("ASCII background spectrum");

                GnirsChart.flush();

                GnirsChart.addArray(specS2N.getExpS2NSpectrum().getData(),
                        "Single Exp S/N");
                GnirsChart.addArray(
                        specS2N.getFinalS2NSpectrum().getData(),
                        "Final S/N  ");

                GnirsChart
                        .addTitle("Intermediate Single Exp and Final S/N");
                GnirsChart.addxAxisLabel("Wavelength (nm)");
                GnirsChart
                        .addyAxisLabel("Signal / Noise per spectral pixel");

                _println(GnirsChart.getBufferedImage(), "Sig2N");
                _println("");

                singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
                finalS2N = _printSpecTag("Final S/N ASCII data");
                GnirsChart.flush();
            }

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

//            // REL-472: Commenting out Altair option for now
//            if (_altairParameters.altairIsUsed()) {
//                IS2Ncalc.setSecondaryIntegral(halo_integral);
//                IS2Ncalc.setSecondarySourceFraction(halo_source_fraction);
//            }

            IS2Ncalc.setSkyIntegral(sky_integral);
            IS2Ncalc.setSkyAperture(_obsDetailParameters
                    .getSkyApertureDiameter());
            IS2Ncalc.setSourceFraction(source_fraction);
            IS2Ncalc.setNpix(Npix);
            IS2Ncalc.setDarkCurrent(instrument.getDarkCurrent()
                    * instrument.getSpatialBinning()
                    * instrument.getSpatialBinning());
            IS2Ncalc.calculate();
            _println(IS2Ncalc.getTextResult(device));
            // _println(IS2Ncalc.getBackgroundLimitResult());
            device.setPrecision(0); // NO decimal places
            device.clear();
            binFactor = instrument.getSpatialBinning()
                    * instrument.getSpatialBinning();

            _println("");
            _println("The peak pixel signal + background is "
                    + device.toString(peak_pixel_count) + ". ");

            if (peak_pixel_count > (.95 * instrument.getWellDepth() * binFactor))
                _println("Warning: peak pixel may be saturating the (binned) CCD full well of "
                        + .95 * instrument.getWellDepth() * binFactor);

            if (peak_pixel_count > (.95 * instrument.getADSaturation() * instrument
                    .getLowGain()))
                _println("Warning: peak pixel may be saturating the low gain setting of "
                        + .95
                        * instrument.getADSaturation()
                        * instrument.getLowGain());

            if (peak_pixel_count > (.95 * instrument.getADSaturation() * instrument
                    .getHighGain()))
                _println("Warning: peak pixel may be saturating the high gain setting "
                        + .95
                        * instrument.getADSaturation()
                        * instrument.getHighGain());

        }

        _println("");
        device.setPrecision(2); // TWO decimal places
        device.clear();

        // _println("");
        _print("<HR align=left SIZE=3>");

        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(_sdParameters.printParameterSummary());
        _println(instrument.toString());

//        // REL-472: Commenting out Altair option for now
//        if (_altairParameters.altairIsUsed()) {
//            _teleParameters.setWFS("altair");
//        }

        _println(_teleParameters.printParameterSummary());

//        // REL-472: Commenting out Altair option for now
//        if (_altairParameters.altairIsUsed()) {
//            _println(_altairParameters.printParameterSummary());
//        }

        _println(_obsConditionParameters.printParameterSummary());
        _println(_obsDetailParameters.printParameterSummary());
        if (_obsDetailParameters.getCalculationMode().equals(
                ObservationDetailsParameters.SPECTROSCOPY)) {
            _println(_plotParameters.printParameterSummary());
        }

        if (_obsDetailParameters.getCalculationMode().equals(
                ObservationDetailsParameters.SPECTROSCOPY)) { // 49 ms
            if (instrument.XDisp_IsUsed()) {
                _println(signalOrder3, _header, sigSpec);
                _println(signalOrder4, _header, sigSpec);
                _println(signalOrder5, _header, sigSpec);
                _println(signalOrder6, _header, sigSpec);
                _println(signalOrder7, _header, sigSpec);
                _println(signalOrder8, _header, sigSpec);

                _println(backGroundOrder3, _header, backSpec);
                _println(backGroundOrder4, _header, backSpec);
                _println(backGroundOrder5, _header, backSpec);
                _println(backGroundOrder6, _header, backSpec);
                _println(backGroundOrder7, _header, backSpec);
                _println(backGroundOrder8, _header, backSpec);

                _println(finalS2NOrder3, _header, finalS2N);
                _println(finalS2NOrder4, _header, finalS2N);
                _println(finalS2NOrder5, _header, finalS2N);
                _println(finalS2NOrder6, _header, finalS2N);
                _println(finalS2NOrder7, _header, finalS2N);
                _println(finalS2NOrder8, _header, finalS2N);
            } else {
                _println(specS2N.getSignalSpectrum(), _header, sigSpec);
                _println(specS2N.getBackgroundSpectrum(), _header, backSpec);
                _println(specS2N.getExpS2NSpectrum(), _header, singleS2N);
                _println(specS2N.getFinalS2NSpectrum(), _header, finalS2N);
            }
        }
    }
}
