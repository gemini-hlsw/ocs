package edu.gemini.itc.nifs;

import edu.gemini.itc.altair.*;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.parameters.*;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.ITCRequest;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 * This class performs the calculations for Nifs
 * used for imaging.
 */
public final class NifsRecipe extends RecipeBase {
    private Calendar now = Calendar.getInstance();
    private String _header = new StringBuffer("# NIFS ITC: " + now.getTime() + "\n").toString();

    private String sigSpec, backSpec, singleS2N, finalS2N;
    private SpecS2NLargeSlitVisitor specS2N;

    // Parameters from the web page.
    private final SourceDefinitionParameters _sdParameters;
    private final ObservationDetailsParameters _obsDetailParameters;
    private final ObservingConditionParameters _obsConditionParameters;
    private final NifsParameters _nifsParameters;
    private final TeleParameters _teleParameters;
    private final AltairParameters _altairParameters;
    private final PlottingDetailsParameters _plotParameters;

    /**
     * Constructs a NifsRecipe by parsing a Multipart servlet request.
     *
     * @param r   Servlet request containing form data from ITC web page.
     * @param out Results will be written to this PrintWriter.
     * @throws Exception on failure to parse parameters.
     */
    public NifsRecipe(ITCMultiPartParser r, PrintWriter out) throws Exception {
        super(out);

        // Read parameters from the four main sections of the web page.
        _sdParameters = new SourceDefinitionParameters(r);
        _obsDetailParameters = new ObservationDetailsParameters(r);
        _obsConditionParameters = ITCRequest.obsConditionParameters(r);
        _nifsParameters = new NifsParameters(r);
        _teleParameters = ITCRequest.teleParameters(r);
        _altairParameters = new AltairParameters(r);
        _plotParameters = new PlottingDetailsParameters(r);
    }

    /**
     * Constructs a NifsRecipe given the parameters.
     * Useful for testing.
     */
    public NifsRecipe(SourceDefinitionParameters sdParameters, ObservationDetailsParameters obsDetailParameters, ObservingConditionParameters obsConditionParameters, NifsParameters nifsParameters, TeleParameters teleParameters, AltairParameters altairParameters, PlottingDetailsParameters plotParameters, PrintWriter out)

    {
        super(out);
        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _nifsParameters = nifsParameters;
        _teleParameters = teleParameters;
        _altairParameters = altairParameters;
        _plotParameters = plotParameters;
    }

    /**
     * Performes recipe calculation and writes results to a cached PrintWriter
     * or to System.out.
     *
     * @throws Exception A recipe calculation can fail in many ways,
     *                   missing data files, incorrectly-formatted data files, ...
     */
    public void writeOutput() throws Exception {
        _println("");
        // This object is used to format numerical strings.
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2);  // Two decimal places
        device.clear();

        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED
        Nifs instrument;
        instrument = new NifsNorth(_nifsParameters, _obsDetailParameters);

        if (_sdParameters.getSourceSpec().equals(_sdParameters.ELINE))
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters.getELineWavelength() * 1000 * 25))) {  // *25 b/c of increased resolutuion of transmission files
                throw new Exception("Please use a model line width > 0.04 nm (or " + (3E5 / (_sdParameters.getELineWavelength() * 1000 * 25)) + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }

        VisitableSampledSpectrum sed, halo;

        sed = SEDFactory.getSED(_sdParameters, instrument);

        halo = (VisitableSampledSpectrum) sed.clone();  //initialize halo

        //_println("Initial Photons.. ");
        //_println("Total photons/s between 999 - 1001: "+ sed.getIntegral(999.0,1001.0));
        //_println("Total photons/s between 1649 - 1651: "+ sed.getIntegral(1649.0,1651.0));
        //_println("Total photons/s between 2199 - 2201: "+ sed.getIntegral(2199.0,2201.0));
        SampledSpectrumVisitor redshift =
                new RedshiftVisitor(_sdParameters.getRedshift());
        sed.accept(redshift);
        // Must check to see if the redshift has moved the spectrum beyond
        // useful range.  The shifted spectrum must completely overlap
        // both the normalization waveband and the observation waveband
        // (filter region).

        String band = _sdParameters.getNormBand();
        double start = WavebandDefinition.getStart(band);
        double end = WavebandDefinition.getEnd(band);

        //any sed except BBODY and ELINE have normailization regions
        if (!(_sdParameters.getSpectrumResource().equals(_sdParameters.ELINE) ||
                _sdParameters.getSpectrumResource().equals(_sdParameters.BBODY))) {
            if (sed.getStart() > start || sed.getEnd() < end) {
                throw new Exception("Shifted spectrum lies outside of specified normalisation waveband.");
            }
        }

        // Check to see if user has defined plot limits; if so check to make sure they are not outside of the
        // actual data
        if (_plotParameters.getPlotLimits().equals(_plotParameters.USER_LIMITS)) {
            if (_plotParameters.getPlotWaveL() > instrument.getObservingEnd() ||
                    _plotParameters.getPlotWaveU() < instrument.getObservingStart()) {
                _println(" The user limits defined for plotting do not overlap with the Spectrum.");

                throw new Exception("User limits for plotting do not overlap with filter.");
            }
        }
        // Module 2
        // Convert input into standard internally-used units.
        //
        // inputs: instrument,redshifted SED, waveband, normalization flux, units
        // calculates: normalized SED, resampled SED, SED adjusted for aperture
        // output: SED in common internal units
        SampledSpectrumVisitor norm =
                new NormalizeVisitor(_sdParameters.getNormBand(),
                        _sdParameters.getSourceNormalization(),
                        _sdParameters.getUnits());
        if (!_sdParameters.getSpectrumResource().equals(_sdParameters.ELINE)) {
            sed.accept(norm);
        }

        SampledSpectrumVisitor tel = new TelescopeApertureVisitor();
        sed.accept(tel);

        //_println("Initial Photons..(scaled to photons/s/nm)");
        //_println("Total photons/s between 999 - 1001: "+ sed.getIntegral(999.0,1001.0));
        //_println("Total photons/s between 1649 - 1651: "+ sed.getIntegral(1649.0,1651.0));
        //_println("Total photons/s between 2199 - 2201: "+ sed.getIntegral(2199.0,2201.0));


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


        SampledSpectrumVisitor water = WaterTransmissionVisitor.create(
                _obsConditionParameters.getSkyTransparencyWater(),
                _obsConditionParameters.getAirmass(),
                "nearIR_trans_", ITCConstants.MAUNA_KEA, ITCConstants.NEAR_IR);
        sed.accept(water);

        // Background spectrum is introduced here.
        VisitableSampledSpectrum sky =
                SEDFactory.getSED("/" + ITCConstants.HI_RES + "/" + ITCConstants.MAUNA_KEA + ITCConstants.NEAR_IR +
                                ITCConstants.SKY_BACKGROUND_LIB + "/" +
                                ITCConstants.NEAR_IR_SKY_BACKGROUND_FILENAME_BASE + "_"
                                + _obsConditionParameters.getSkyTransparencyWaterCategory() +
                                "_" + _obsConditionParameters.getAirmassCategory() +
                                ITCConstants.DATA_SUFFIX,
                        instrument.getSampling());

        //_println("Total Photons..(After Sky)");
        //_println("Total photons/s between 999 - 1001: "+ sed.getIntegral(999.0,1001.0));
        //_println("Total photons/s between 1649 - 1651: "+ sed.getIntegral(1649.0,1651.0));
        //_println("Total photons/s between 2199 - 2201: "+ sed.getIntegral(2199.0,2201.0));

        // Apply telescope transmission to both sed and sky
        SampledSpectrumVisitor t = TelescopeTransmissionVisitor.create(_teleParameters);
        sed.accept(t);
        sky.accept(t);

        //_println("Total Photons..(After Telescope Transmission)");
        //_println("Total photons/s between 999 - 1001: "+ sed.getIntegral(999.0,1001.0));
        //_println("Total photons/s between 1649 - 1651: "+ sed.getIntegral(1649.0,1651.0));
        //_println("Total photons/s between 2199 - 2201: "+ sed.getIntegral(2199.0,2201.0));

        //Create and Add background for the telescope.
        SampledSpectrumVisitor tb =
                new TelescopeBackgroundVisitor(_teleParameters,ITCConstants.MAUNA_KEA, ITCConstants.NEAR_IR);
        sky.accept(tb);
        sky.accept(tel);

        // Add instrument background to sky background for a total background.
        // At this point "sky" is not the right name.
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

        //_println("Total Photons..(After Instrument Transmission)");
        //_println("Total photons/s between 999 - 1001: "+ sed.getIntegral(999.0,1001.0));
        // _println("Total photons/s between 1649 - 1651: "+ sed.getIntegral(1649.0,1651.0));
        // _println("Total photons/s between 2199 - 2201: "+ sed.getIntegral(2199.0,2201.0));


        // Get the summed source and sky  Uncomment if needed for NIFS imaging ITC
        double sed_integral = sed.getIntegral();
        double sky_integral = sky.getIntegral();

        //Debugging
        _println("SED Int: " + sed_integral + " Sky Int: " + sky_integral);

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
        List sf_list = new ArrayList();
        List halo_sf_list = new ArrayList();
        List ap_offset_list = new ArrayList();

        // Calculate image quality
        double im_qual = 0.;
        double uncorrected_im_qual = 0.;

        ImageQualityCalculatable IQcalc =
                ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _teleParameters, instrument);
        IQcalc.calculate();

        im_qual = IQcalc.getImageQuality();

        //Altair Section
        Altair altair = new Altair(instrument.getEffectiveWavelength(), _teleParameters.getTelescopeDiameter(),
                im_qual, _altairParameters.getGuideStarSeperation(), _altairParameters.getGuideStarMagnitude(),
                _altairParameters.getWFSMode(), _altairParameters.fieldLensIsUsed(), 0);
        AltairBackgroundVisitor altairBackgroundVisitor = new AltairBackgroundVisitor();
        AltairTransmissionVisitor altairTransmissionVisitor = new AltairTransmissionVisitor();
        AltairFluxAttenuationVisitor altairFluxAttenuationVisitor = new AltairFluxAttenuationVisitor(altair.getFluxAttenuation());
        AltairFluxAttenuationVisitor altairFluxAttenuationVisitorHalo = new AltairFluxAttenuationVisitor((1 - altair.getStrehl()));

        if (_altairParameters.altairIsUsed()) {
            sky.accept(altairBackgroundVisitor);

            sed.accept(altairTransmissionVisitor);
            sky.accept(altairTransmissionVisitor);
        }

        halo = (VisitableSampledSpectrum) sed.clone();

        if (_altairParameters.altairIsUsed()) {
            halo.accept(altairFluxAttenuationVisitorHalo);
            sed.accept(altairFluxAttenuationVisitor);
        }

        uncorrected_im_qual = im_qual;  //Save uncorrected value for the image quality for later use

        if (_altairParameters.altairIsUsed())
            im_qual = altair.getAOCorrectedFWHMc();

        int previousPrecision = device.getPrecision();
        device.setPrecision(3);  // Two decimal places
        device.clear();
        if (_altairParameters.altairIsUsed())
            _println(altair.printSummary(device));
        //_println(altair.toString());
        device.setPrecision(previousPrecision);  // Two decimal places
        device.clear();
        //End of Altair Section


        //IFU morphology section
        VisitableMorphology morph, haloMorphology;
        if (_sdParameters.getSourceGeometry().equals(SourceDefinitionParameters.POINT_SOURCE)) {
            morph = new AOMorphology(im_qual);
            haloMorphology = new AOMorphology(uncorrected_im_qual);
        } else if (_sdParameters.getExtendedSourceType().equals(SourceDefinitionParameters.GAUSSIAN)) {
            morph = new GaussianMorphology(im_qual);
            haloMorphology = new GaussianMorphology(uncorrected_im_qual);
        } else {
            morph = new USBMorphology();
            haloMorphology = new USBMorphology();
        }
        morph.accept(instrument.getIFU().getAperture());

        ap_diam = instrument.getIFU().IFU_DIAMETER;

        //for now just a single item from the list
        sf_list = instrument.getIFU().getFractionOfSourceInAperture();  //extract corrected source fraction list

        instrument.getIFU().clearFractionOfSourceInAperture();
        haloMorphology.accept(instrument.getIFU().getAperture());


        halo_sf_list = instrument.getIFU().getFractionOfSourceInAperture();  //extract uncorrected halo source fraction list
        //_println("halo:" + halo_sf_list.size() + " corrected: "+sf_list.size());

        ap_offset_list = instrument.getIFU().getApertureOffsetList();

        //source_fraction = ((Double)sf_list.get(0)).doubleValue();

        Npix = (Math.PI / 4.) * (ap_diam / pixel_size) * (ap_diam / pixel_size);
        if (Npix < 9) Npix = 9;
        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.
        int binFactor;
        double spec_source_frac = 0;
        double halo_spec_source_frac = 0;
        int number_exposures = _obsDetailParameters.getNumExposures();
        double frac_with_source = _obsDetailParameters.getSourceFraction();
        double dark_current = instrument.getDarkCurrent();
        double exposure_time = _obsDetailParameters.getExposureTime();
        double read_noise = instrument.getReadNoise();
        // report error if this does not come out to be an integer
        checkSourceFraction(number_exposures, frac_with_source);

        //ObservationMode Imaging or spectroscopy


        if (_obsDetailParameters.getCalculationMode().equals(ObservationDetailsParameters.SPECTROSCOPY)) {

            _println("derived image halo size (FWHM) for a point source = " + device.toString(uncorrected_im_qual) + "arcsec\n");

            //_println("Sky subtraction aperture = " +
            //        _obsDetailParameters.getSkyApertureDiameter()
            //        +" times the software aperture.");

            //_println("");
            _println("Requested total integration time = " +
                    device.toString(exposure_time * number_exposures) +
                    " secs, of which " + device.toString(exposure_time *
                    number_exposures *
                    frac_with_source) +
                    " secs is on source.");

            _print("<HR align=left SIZE=3>");

            if (instrument.IFU_IsUsed()) {//&&
                //!(_sdParameters.getSourceGeometry().equals(SourceDefinitionParameters.EXTENDED_SOURCE)
                //&&_sdParameters.getExtendedSourceType().equals(SourceDefinitionParameters.UNIFORM))) {
                ap_diam = 1 / instrument.getSpatialBinning();
                Iterator src_frac_it = sf_list.iterator();
                Iterator halo_src_frac_it = halo_sf_list.iterator();
                Iterator ifu_offset_it = ap_offset_list.iterator();

                while (src_frac_it.hasNext()) {
                    double ifu_offset = ((Double) ifu_offset_it.next()).doubleValue();
                    double ifu_offset_Y = 0.0;
                    if (_nifsParameters.getIFUMethod().equals(_nifsParameters.SUMMED_APERTURE_IFU))
                        ifu_offset_Y = ((Double) ifu_offset_it.next()).doubleValue();

                    if (_nifsParameters.getIFUMethod().equals(_nifsParameters.SUMMED_APERTURE_IFU)) {
                        while (src_frac_it.hasNext()) {
                            spec_source_frac = spec_source_frac + ((Double) src_frac_it.next()).doubleValue();
                            halo_spec_source_frac = halo_spec_source_frac + ((Double) halo_src_frac_it.next()).doubleValue();
                            ap_diam = (ap_offset_list.size() / 2) / instrument.getSpatialBinning();
                        }
                    } else {
                        spec_source_frac = ((Double) src_frac_it.next()).doubleValue();
                        halo_spec_source_frac = ((Double) halo_src_frac_it.next()).doubleValue();
                        ap_diam = 1 / instrument.getSpatialBinning();
                    }


                    specS2N =
                            new SpecS2NLargeSlitVisitor(_nifsParameters.getFPMask(), pixel_size,
                                    instrument.getSpectralPixelWidth(),
                                    instrument.getObservingStart(),
                                    instrument.getObservingEnd(),
                                    instrument.getGratingDispersion_nm(),
                                    instrument.getGratingDispersion_nmppix(),
                                    instrument.getGratingResolution(),
                                    spec_source_frac, im_qual,
                                    ap_diam, number_exposures,
                                    frac_with_source, exposure_time,
                                    dark_current * instrument.getSpatialBinning() *
                                            instrument.getSpectralBinning(), read_noise,
                                    _obsDetailParameters.getSkyApertureDiameter(),
                                    instrument.getSpectralBinning());

                    specS2N.setDetectorTransmission(instrument.getDetectorTransmision());
                    specS2N.setSourceSpectrum(sed);
                    specS2N.setBackgroundSpectrum(sky);
                    specS2N.setHaloSpectrum(halo);
                    specS2N.setHaloImageQuality(uncorrected_im_qual);
                    if (_altairParameters.altairIsUsed())
                        specS2N.setSpecHaloSourceFraction(halo_spec_source_frac);
                    else
                        specS2N.setSpecHaloSourceFraction(0.0);

                    sed.accept(specS2N);
                    _println("<p style=\"page-break-inside: never\">");
                    device.setPrecision(3);  // NO decimal places
                    device.clear();

                    final String chart1Title =
                            _nifsParameters.getIFUMethod().equals(_nifsParameters.SUMMED_APERTURE_IFU) ?
                                    "Signal and Background (IFU summed apertures: " +
                                            device.toString(_nifsParameters.getIFUNumX()) + "x" + device.toString(_nifsParameters.getIFUNumY()) +
                                            ", " + device.toString(_nifsParameters.getIFUNumX() * instrument.getIFU().IFU_LEN_X) + "\"x" +
                                            device.toString(_nifsParameters.getIFUNumY() * instrument.getIFU().IFU_LEN_Y) + "\")" :
                                    "Signal and Background (IFU element offset: " + device.toString(ifu_offset) + " arcsec)";

                    final ITCChart chart1 = new ITCChart(chart1Title, "Wavelength (nm)", "e- per exposure per spectral pixel", _plotParameters);
                    chart1.addArray(specS2N.getSignalSpectrum().getData(), "Signal ");
                    chart1.addArray(specS2N.getBackgroundSpectrum().getData(), "SQRT(Background)  ");
                    _println(chart1.getBufferedImage(), "SigAndBack");
                    _println("");


                    sigSpec = _printSpecTag("ASCII signal spectrum");
                    backSpec = _printSpecTag("ASCII background spectrum");

                    final String chart2Title =
                            _nifsParameters.getIFUMethod().equals(_nifsParameters.SUMMED_APERTURE_IFU) ?
                                    "Intermediate Single Exp and Final S/N \n(IFU apertures:" +
                                            device.toString(_nifsParameters.getIFUNumX()) + "x" + device.toString(_nifsParameters.getIFUNumY()) +
                                            ", " + device.toString(_nifsParameters.getIFUNumX() * instrument.getIFU().IFU_LEN_X) + "\"x" +
                                            device.toString(_nifsParameters.getIFUNumY() * instrument.getIFU().IFU_LEN_Y) + "\")" :
                                    "Intermediate Single Exp and Final S/N (IFU element offset: " + device.toString(ifu_offset) + " arcsec)";

                    final ITCChart chart2 = new ITCChart(chart2Title, "Wavelength (nm)", "Signal / Noise per spectral pixel", _plotParameters);
                    chart2.addArray(specS2N.getExpS2NSpectrum().getData(), "Single Exp S/N");
                    chart2.addArray(specS2N.getFinalS2NSpectrum().getData(), "Final S/N  ");
                    _println(chart2.getBufferedImage(), "Sig2N");
                    _println("");

                    singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
                    finalS2N = _printSpecTag("Final S/N ASCII data");
                }
            }

            binFactor = instrument.getSpatialBinning() *
                    instrument.getSpectralBinning();


        }
        _println("");
        device.setPrecision(2);  // TWO decimal places
        device.clear();

        _print("<HR align=left SIZE=3>");

        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(_sdParameters.printParameterSummary());
        _println(instrument.toString());
        if (_altairParameters.altairIsUsed())
            _println(_altairParameters.printParameterSummary());
        _println(_teleParameters.printParameterSummary());
        _println(_obsConditionParameters.printParameterSummary());
        _println(_obsDetailParameters.printParameterSummary());
        if (_obsDetailParameters.getCalculationMode().equals(ObservationDetailsParameters.SPECTROSCOPY)) {
            _println(_plotParameters.printParameterSummary());
        }


        if (_obsDetailParameters.getCalculationMode().equals(ObservationDetailsParameters.SPECTROSCOPY)) {  //49 ms
            _println(specS2N.getSignalSpectrum(), _header, sigSpec);
            _println(specS2N.getBackgroundSpectrum(), _header, backSpec);
            _println(specS2N.getExpS2NSpectrum(), _header, singleS2N);
            _println(specS2N.getFinalS2NSpectrum(), _header, finalS2N);
        }
    }
}
