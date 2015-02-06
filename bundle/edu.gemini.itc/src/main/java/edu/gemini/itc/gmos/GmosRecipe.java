package edu.gemini.itc.gmos;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.parameters.*;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.ITCRequest;

import java.awt.*;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 * This class performs the calculations for Gmos used for imaging.
 */
public final class GmosRecipe extends RecipeBase {
    // Images will be saved to this session object
    // private HttpSession _sessionObject = null; // set from servlet request

    private final Calendar now = Calendar.getInstance();
    private final String _header = new StringBuffer("# GMOS ITC: " + now.getTime() + "\n").toString();

    // Parameters from the web page.
    private final SourceDefinitionParameters _sdParameters;
    private final ObservationDetailsParameters _obsDetailParameters;
    private final ObservingConditionParameters _obsConditionParameters;
    private final GmosParameters _gmosParameters;
    private final TeleParameters _teleParameters;
    private final PlottingDetailsParameters _plotParameters;

    /**
     * Constructs a GmosRecipe by parsing a Multipart servlet request.
     *
     * @param r   Servlet request containing form data from ITC web page.
     * @param out Results will be written to this PrintWriter.
     * @throws Exception on failure to parse parameters.
     */
    public GmosRecipe(ITCMultiPartParser r, PrintWriter out) throws Exception {
        super(out);
        // Set the Http Session object
        // _sessionObject = r.getSession(true);

        // System.out.println(" Session is over after"
        // +_sessionObject.getCreationTime());

        System.out.println("ServerName: " + ServerInfo.getServerURL());

        // Read parameters from the four main sections of the web page.
        _sdParameters = new SourceDefinitionParameters(r);
        _obsDetailParameters = new ObservationDetailsParameters(r);
        _obsConditionParameters = ITCRequest.obsConditionParameters(r);
        _gmosParameters = new GmosParameters(r);
        _teleParameters = ITCRequest.teleParameters(r);
        _plotParameters = ITCRequest.plotParamters(r);
    }

    /**
     * Constructs a GmosRecipe given the parameters. Useful for testing.
     */
    public GmosRecipe(SourceDefinitionParameters sdParameters,
                      ObservationDetailsParameters obsDetailParameters,
                      ObservingConditionParameters obsConditionParameters,
                      GmosParameters gmosParameters, TeleParameters teleParameters,
                      PlottingDetailsParameters plotParameters,
                      PrintWriter out)

    {
        super(out);
        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _gmosParameters = gmosParameters;
        _teleParameters = teleParameters;
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
        Gmos mainInstrument;
        String site;
        if (_gmosParameters.getInstrumentLocation().equals(_gmosParameters.GMOS_NORTH)) {
            mainInstrument = new GmosNorth(_gmosParameters, _obsDetailParameters, 0);
            site = ITCConstants.MAUNA_KEA;
        } else {
            mainInstrument = new GmosSouth(_gmosParameters, _obsDetailParameters, 0);
            site = ITCConstants.CERRO_PACHON;
        }

        // Create one chart to use for all 3 CCDS (one for Signal and Background and one for Intermediate Single Exp and Final S/N)
        final ITCChart gmosChart1;
        final ITCChart gmosChart2;
        if (_obsDetailParameters.getCalculationMode().equals(ObservationDetailsParameters.SPECTROSCOPY)) {
            final boolean ifuAndUniform =
                    mainInstrument.IFU_IsUsed() &&
                            !(_sdParameters.getSourceGeometry().equals(SourceDefinitionParameters.EXTENDED_SOURCE) &&
                            _sdParameters.getExtendedSourceType().equals(SourceDefinitionParameters.UNIFORM));
            final double ifu_offset = ifuAndUniform ? (Double) mainInstrument.getIFU().getApertureOffsetList().iterator().next() : 0.0;
            final String chart1Title = ifuAndUniform ? "Signal and Background (IFU element offset: " + device.toString(ifu_offset) + " arcsec)" : "Signal and Background ";
            final String chart2Title = ifuAndUniform ? "Intermediate Single Exp and Final S/N (IFU element offset: " + device.toString(ifu_offset) + " arcsec)" : "Intermediate Single Exp and Final S/N";
            gmosChart1 = new ITCChart(chart1Title, "Wavelength (nm)", "e- per exposure per spectral pixel", _plotParameters);
            gmosChart2 = new ITCChart(chart2Title, "Wavelength (nm)", "Signal / Noise per spectral pixel", _plotParameters);

        } else {
            gmosChart1 = null;
            gmosChart2 = null;
        }

        String sigSpec = null, backSpec = null, singleS2N = null, finalS2N = null;
        Gmos[] ccdArray = mainInstrument.getDetectorCcdInstruments();
        edu.gemini.itc.operation.DetectorsTransmissionVisitor tv = mainInstrument.getDetectorTransmision();
        int detectorCount = ccdArray.length;
        for (Gmos instrument : ccdArray) {
            int ccdIndex = instrument.getDetectorCcdIndex();
            String ccdName = instrument.getDetectorCcdName();
            String forCcdName = ccdName.length() == 0 ? "" : " for " + ccdName;
            Color ccdColor = instrument.getDetectorCcdColor();
            Color ccdColorDarker = ccdColor == null ? null : ccdColor.darker().darker();
            int firstCcdIndex = tv.getDetectorCcdStartIndex(ccdIndex);
            int lastCcdIndex = tv.getDetectorCcdEndIndex(ccdIndex, detectorCount);
            // REL-478: include the gaps in the text data output
            int lastCcdIndexWithGap = (ccdIndex < 2 && detectorCount > 1)
                    ? tv.getDetectorCcdStartIndex(ccdIndex + 1)
                    : lastCcdIndex;

            SpecS2NLargeSlitVisitor specS2N = null;
            SlitThroughput st = null;

            if (_sdParameters.getSourceSpec().equals(_sdParameters.ELINE))
                if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters
                        .getELineWavelength() * 1000))) {
                    throw new Exception(
                            "Please use a model line width > 1 nm (or "
                                    + (3E5 / (_sdParameters.getELineWavelength() * 1000))
                                    + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
                }

            VisitableSampledSpectrum sed;

            sed = SEDFactory.getSED(_sdParameters, instrument);
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

            SampledSpectrumVisitor clouds = CloudTransmissionVisitor.create(
                    _obsConditionParameters.getSkyTransparencyCloud());
            sed.accept(clouds);

            SampledSpectrumVisitor water = WaterTransmissionVisitor.create(
                    _obsConditionParameters.getSkyTransparencyWater(),
                    _obsConditionParameters.getAirmass(), "skytrans_",
                    site, ITCConstants.VISIBLE);
            sed.accept(water);

            // Background spectrum is introduced here.
            VisitableSampledSpectrum sky = SEDFactory.getSED(
                    ITCConstants.SKY_BACKGROUND_LIB + "/"
                            + ITCConstants.OPTICAL_SKY_BACKGROUND_FILENAME_BASE
                            + "_"
                            + _obsConditionParameters.getSkyBackgroundCategory()
                            + "_" + _obsConditionParameters.getAirmassCategory()
                            + ITCConstants.DATA_SUFFIX, instrument.getSampling());

            // resample sky_background to instrument parameters
            // sky.accept(resample);

            // Apply telescope transmission to both sed and sky
            SampledSpectrumVisitor t = TelescopeTransmissionVisitor.create(_teleParameters);
            sed.accept(t);
            sky.accept(t);

            // Create and Add background for the telescope.
            SampledSpectrumVisitor tb = new TelescopeBackgroundVisitor(_teleParameters, site, ITCConstants.VISIBLE);
            sky.accept(tb);

            sky.accept(tel);

            // Add instrument background to sky background for a total background.
            // At this point "sky" is not the right name.
            instrument.addBackground(sky);

            // Module 4 AO module not implemented
            // The AO module affects source and background SEDs.

            // Module 5b
            // The instrument with its detectors modifies the source and
            // background spectra.
            // input: instrument, source and background SED
            // output: total flux of source and background.
            instrument.convolveComponents(sed);
            instrument.convolveComponents(sky);

            // Get the summed source and sky
            double sed_integral = sed.getIntegral();
            double sky_integral = sky.getIntegral();

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
            double pix_per_sq_arcsec = 0;
            double peak_pixel_count = 0;
            List sf_list = new ArrayList();
            List ap_offset_list = new ArrayList();

            // Calculate image quality
            double im_qual = 0.;
            ImageQualityCalculatable IQcalc =
                    ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _teleParameters, instrument);
            IQcalc.calculate();

            im_qual = IQcalc.getImageQuality();

            if (!instrument.IFU_IsUsed()) {
                // Calculate the Fraction of source in the aperture
                SourceFractionCalculatable SFcalc =
                        SourceFractionCalculationFactory.getCalculationInstance(_sdParameters, _obsDetailParameters, instrument);
                SFcalc.setImageQuality(im_qual);
                SFcalc.calculate();
                source_fraction = SFcalc.getSourceFraction();
                Npix = SFcalc.getNPix();

                if (_obsDetailParameters.getCalculationMode().equals(ObservationDetailsParameters.IMAGING)
                        && ccdIndex == 0) {
                    _print(SFcalc.getTextResult(device));
                    _println(IQcalc.getTextResult(device));
                    _println("Sky subtraction aperture = "
                            + _obsDetailParameters.getSkyApertureDiameter()
                            + " times the software aperture.\n");
                }
            } else {
                VisitableMorphology morph;
                if (_sdParameters.getSourceGeometry().equals(
                        SourceDefinitionParameters.POINT_SOURCE)
                        || _sdParameters.getExtendedSourceType().equals(
                        SourceDefinitionParameters.GAUSSIAN)) {
                    morph = new GaussianMorphology(im_qual);
                } else {
                    morph = new USBMorphology();
                }
                morph.accept(instrument.getIFU().getAperture());
                ap_diam = instrument.getIFU().IFU_DIAMETER;
                // for now just a single item from the list
                sf_list = instrument.getIFU().getFractionOfSourceInAperture();
                ap_offset_list = instrument.getIFU().getApertureOffsetList();

                source_fraction = ((Double) sf_list.get(0)).doubleValue();

                Npix = (Math.PI / 4.) * (ap_diam / pixel_size)
                        * (ap_diam / pixel_size);
                if (Npix < 9)
                    Npix = 9;
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
            double spec_source_frac = 0;
            int number_exposures = _obsDetailParameters.getNumExposures();
            double frac_with_source = _obsDetailParameters.getSourceFraction();
            double dark_current = instrument.getDarkCurrent();
            double exposure_time = _obsDetailParameters.getExposureTime();
            double read_noise = instrument.getReadNoise();
            if (ccdIndex == 0) {
                _println("Read noise: " + read_noise);
            }
            // report error if this does not come out to be an integer
            checkSourceFraction(number_exposures, frac_with_source);

            // ObservationMode Imaging or spectroscopy
            if (_obsDetailParameters.getCalculationMode().equals(ObservationDetailsParameters.SPECTROSCOPY)) {
                if (!instrument.IFU_IsUsed()) {
                    if (ap_type.equals(ObservationDetailsParameters.USER_APER)) {
                        st = new SlitThroughput(im_qual,
                                _obsDetailParameters.getApertureDiameter(),
                                pixel_size, _gmosParameters.getFPMask());
                        if (ccdIndex == 0) {
                            _println("software aperture extent along slit = "
                                    + device.toString(_obsDetailParameters
                                    .getApertureDiameter()) + " arcsec");
                        }
                    } else {
                        st = new SlitThroughput(im_qual, pixel_size,
                                _gmosParameters.getFPMask());
                        if (_sdParameters.getSourceGeometry().equals(
                                SourceDefinitionParameters.EXTENDED_SOURCE)) {
                            if (_sdParameters.getExtendedSourceType().equals(
                                    SourceDefinitionParameters.UNIFORM)) {
                                if (ccdIndex == 0) {
                                    _println("software aperture extent along slit = "
                                            + device.toString(1 / _gmosParameters
                                            .getFPMask()) + " arcsec");
                                }
                            }
                        } else {
                            if (ccdIndex == 0) {
                                _println("software aperture extent along slit = "
                                        + device.toString(1.4 * im_qual) + " arcsec");
                            }
                        }
                    }

                    if (_sdParameters.getSourceGeometry().equals(
                            SourceDefinitionParameters.POINT_SOURCE)
                            || _sdParameters.getExtendedSourceType().equals(
                            SourceDefinitionParameters.GAUSSIAN)) {
                        if (ccdIndex == 0) {
                            _println("fraction of source flux in aperture = "
                                    + device.toString(st.getSlitThroughput()));
                        }
                    }
                }

                if (ccdIndex == 0) {
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
                }

                if (!instrument.IFU_IsUsed()) {
                    ap_diam = st.getSpatialPix(); // ap_diam really Spec_Npix on
                    // Phil's Mathcad change later
                    spec_source_frac = st.getSlitThroughput();
                } else {
                    spec_source_frac = source_fraction;
                    // _println("spec: " + source_fraction);
                    ap_diam = 5 / instrument.getSpatialBinning();
                }

                // For the usb case we want the resolution to be determined by the
                // slit width and not the image quality for a point source.
                if (_sdParameters.getSourceGeometry().equals(
                        SourceDefinitionParameters.EXTENDED_SOURCE)) {
                    if (_sdParameters.getExtendedSourceType().equals(
                            SourceDefinitionParameters.UNIFORM)) {
                        im_qual = 10000;

                        if (!instrument.IFU_IsUsed()) {

                            if (ap_type
                                    .equals(ObservationDetailsParameters.USER_APER)) {
                                spec_source_frac = _gmosParameters.getFPMask()
                                        * ap_diam * pixel_size; // ap_diam =
                                // Spec_NPix
                            } else if (ap_type
                                    .equals(ObservationDetailsParameters.AUTO_APER)) {
                                ap_diam = new Double(
                                        1 / (_gmosParameters.getFPMask() * pixel_size) + 0.5)
                                        .intValue();
                                spec_source_frac = 1;
                            }
                        }
                    }
                }

                if (instrument.IFU_IsUsed() && !(_sdParameters.getSourceGeometry().equals(
                        SourceDefinitionParameters.EXTENDED_SOURCE) && _sdParameters
                        .getExtendedSourceType().equals(
                                SourceDefinitionParameters.UNIFORM))) {
                    Iterator src_frac_it = sf_list.iterator();
                    Iterator ifu_offset_it = ap_offset_list.iterator();

                    while (src_frac_it.hasNext()) {
                        spec_source_frac = (Double) src_frac_it.next();
                        specS2N = new SpecS2NLargeSlitVisitor(
                                _gmosParameters.getFPMask(), pixel_size,
                                instrument.getSpectralPixelWidth(),
                                instrument.getObservingStart(),
                                instrument.getObservingEnd(),
                                instrument.getGratingDispersion_nm(),
                                instrument.getGratingDispersion_nmppix(),
                                instrument.getGratingResolution(),
                                spec_source_frac, im_qual, ap_diam,
                                number_exposures, frac_with_source, exposure_time,
                                dark_current * instrument.getSpatialBinning()
                                        * instrument.getSpectralBinning(),
                                read_noise,
                                _obsDetailParameters.getSkyApertureDiameter(),
                                instrument.getSpectralBinning());

                        specS2N.setDetectorTransmission(mainInstrument.getDetectorTransmision());
                        specS2N.setCcdPixelRange(firstCcdIndex, lastCcdIndex);
                        specS2N.setSourceSpectrum(sed);
                        specS2N.setBackgroundSpectrum(sky);
                        sed.accept(specS2N);
                        if (ccdIndex == 0) {
                            _println("<p style=\"page-break-inside: never\">");
                        }
                        device.setPrecision(3); // NO decimal places
                        device.clear();
                        gmosChart1.addArray(specS2N.getSignalSpectrum().getData(firstCcdIndex, lastCcdIndex),
                                "Signal " + ccdName, ccdColor);
                        gmosChart1.addArray(specS2N.getBackgroundSpectrum().getData(firstCcdIndex, lastCcdIndex),
                                "SQRT(Background) " + ccdName, ccdColorDarker);

                        if (ccdIndex == 0) {
                            sigSpec = _printSpecTag("ASCII signal spectrum");
                            backSpec = _printSpecTag("ASCII background spectrum");
                        }

                        gmosChart2.addArray(specS2N.getExpS2NSpectrum().getData(firstCcdIndex, lastCcdIndex),
                                "Single Exp S/N " + ccdName, ccdColor);
                        gmosChart2.addArray(specS2N.getFinalS2NSpectrum().getData(firstCcdIndex, lastCcdIndex),
                                "Final S/N " + ccdName, ccdColorDarker);

                        if (ccdIndex == 0) {
                            singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
                            finalS2N = _printSpecTag("Final S/N ASCII data");
                        }
                        _println("");
                    }
                } else {
                    specS2N = new SpecS2NLargeSlitVisitor(
                            _gmosParameters.getFPMask(), pixel_size,
                            instrument.getSpectralPixelWidth(),
                            instrument.getObservingStart(),
                            instrument.getObservingEnd(),
                            instrument.getGratingDispersion_nm(),
                            instrument.getGratingDispersion_nmppix(),
                            instrument.getGratingResolution(), spec_source_frac,
                            im_qual, ap_diam, number_exposures, frac_with_source,
                            exposure_time,
                            dark_current * instrument.getSpatialBinning() * instrument.getSpectralBinning(),
                            read_noise,
                            _obsDetailParameters.getSkyApertureDiameter(),
                            instrument.getSpectralBinning());
                    specS2N.setSourceSpectrum(sed);
                    specS2N.setBackgroundSpectrum(sky);
                    specS2N.setDetectorTransmission(mainInstrument.getDetectorTransmision());
                    specS2N.setCcdPixelRange(firstCcdIndex, lastCcdIndex);

                    sed.accept(specS2N);
                    if (ccdIndex == 0) {
                        _println("<p style=\"page-break-inside: never\">");
                    }

                    gmosChart1.addArray(specS2N.getSignalSpectrum().getData(firstCcdIndex, lastCcdIndex),
                            "Signal " + ccdName, ccdColor);
                    gmosChart1.addArray(specS2N.getBackgroundSpectrum().getData(firstCcdIndex, lastCcdIndex),
                            "SQRT(Background) " + ccdName, ccdColorDarker);

                    if (ccdIndex == 0) {
                        sigSpec = _printSpecTag("ASCII signal spectrum");
                        backSpec = _printSpecTag("ASCII background spectrum");
                    }

                    gmosChart2.addArray(specS2N.getExpS2NSpectrum().getData(firstCcdIndex, lastCcdIndex),
                            "Single Exp S/N " + ccdName, ccdColor);
                    gmosChart2.addArray(specS2N.getFinalS2NSpectrum().getData(firstCcdIndex, lastCcdIndex),
                            "Final S/N " + ccdName, ccdColorDarker);

                    if (ccdIndex == 0) {
                        singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
                        finalS2N = _printSpecTag("Final S/N ASCII data");
                    }
                    _println("");
                }
            } else {
                ImagingS2NCalculatable IS2Ncalc =
                        ImagingS2NCalculationFactory.getCalculationInstance(_sdParameters, _obsDetailParameters, instrument);
                IS2Ncalc.setSedIntegral(sed_integral);
                IS2Ncalc.setSkyIntegral(sky_integral);
                IS2Ncalc.setSkyAperture(_obsDetailParameters
                        .getSkyApertureDiameter());
                IS2Ncalc.setSourceFraction(source_fraction);
                IS2Ncalc.setNpix(Npix);
                IS2Ncalc.setDarkCurrent(instrument.getDarkCurrent()
                        * instrument.getSpatialBinning()
                        * instrument.getSpatialBinning());
                IS2Ncalc.calculate();
                _println("");
                _println("<b>S/N" + forCcdName + ":</b>");
                _println("");
                _println(IS2Ncalc.getTextResult(device));

                // _println(IS2Ncalc.getBackgroundLimitResult());
                device.setPrecision(0); // NO decimal places
                device.clear();
                binFactor = instrument.getSpatialBinning()
                        * instrument.getSpatialBinning();

                _println("");
                _println("The peak pixel signal + background is " + device.toString(peak_pixel_count) + ". ");
                // This is " +
                // device.toString(peak_pixel_count/instrument.getWellDepth()*100) +
                // "% of the full well depth of "+device.toString(instrument.getWellDepth())+".");

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

            if (_obsDetailParameters.getCalculationMode().equals(ObservationDetailsParameters.SPECTROSCOPY)) {
                _println(specS2N.getSignalSpectrum(), _header, sigSpec, firstCcdIndex, lastCcdIndexWithGap);
                _println(specS2N.getBackgroundSpectrum(), _header, backSpec, firstCcdIndex, lastCcdIndexWithGap);
                _println(specS2N.getExpS2NSpectrum(), _header, singleS2N, firstCcdIndex, lastCcdIndexWithGap);
                _println(specS2N.getFinalS2NSpectrum(), _header, finalS2N, firstCcdIndex, lastCcdIndexWithGap);
            }
        }

        if (gmosChart1 != null && gmosChart2 != null) {
            _println(gmosChart1.getBufferedImage(), "SigAndBack");
            _println("");
            _println(gmosChart2.getBufferedImage(), "Sig2N");
            _println("");
        }

        _println("");
        device.setPrecision(2); // TWO decimal places
        device.clear();

        _print("<HR align=left SIZE=3>");

        _println(_plotParameters.printParameterSummary());

        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + mainInstrument.getName() + "\n");
        _println(_sdParameters.printParameterSummary());
        _println(mainInstrument.toString());
        _println(_teleParameters.printParameterSummary());
        _println(_obsConditionParameters.printParameterSummary());
        _println(_obsDetailParameters.printParameterSummary());
    }
}
