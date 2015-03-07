package edu.gemini.itc.gmos;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.parameters.*;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.ITCRequest;

import java.awt.*;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * This class performs the calculations for Gmos used for imaging.
 */
public final class GmosRecipe extends RecipeBase {

    private final Calendar now = Calendar.getInstance();
    private final String _header = "# GMOS ITC: " + now.getTime() + "\n";

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
    public GmosRecipe(final ITCMultiPartParser r, final PrintWriter out) {
        super(out);

        System.out.println("ServerName: " + ServerInfo.getServerURL());

        // Read parameters from the four main sections of the web page.
        _sdParameters = ITCRequest.sourceDefinitionParameters(r);
        _obsDetailParameters = ITCRequest.observationParameters(r);
        _obsConditionParameters = ITCRequest.obsConditionParameters(r);
        _gmosParameters = ITCRequest.gmosParameters(r);
        _teleParameters = ITCRequest.teleParameters(r);
        _plotParameters = ITCRequest.plotParamters(r);
    }

    /**
     * Constructs a GmosRecipe given the parameters. Useful for testing.
     */
    public GmosRecipe(final SourceDefinitionParameters sdParameters,
                      final ObservationDetailsParameters obsDetailParameters,
                      final ObservingConditionParameters obsConditionParameters,
                      final GmosParameters gmosParameters, TeleParameters teleParameters,
                      final PlottingDetailsParameters plotParameters,
                      final PrintWriter out)

    {
        super(out);
        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _gmosParameters = gmosParameters;
        _teleParameters = teleParameters;
        _plotParameters = plotParameters;


        // TODO: this validation should be done centrally somewhere (several instruments do similar stuff here)
        if (_sdParameters.getDistributionType().equals(SourceDefinitionParameters.Distribution.ELINE)) {
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters.getELineWavelength() * 1000))) {
                throw new RuntimeException(
                        "Please use a model line width > 1 nm (or "
                                + (3E5 / (_sdParameters.getELineWavelength() * 1000))
                                + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }
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
        final Gmos mainInstrument = createGmos();
        final GmosResult[] results = calculate(mainInstrument);
        if (_obsDetailParameters.getMethod().isSpectroscopy()) {
            writeSpectroscopyOutput(mainInstrument, results);
        } else {
            writeImagingOutput(mainInstrument, results);
        }
    }

    public GmosResult[] calculate() throws Exception {
        final Gmos mainInstrument = createGmos();
        final Gmos[] ccdArray = mainInstrument.getDetectorCcdInstruments();
        final GmosResult[] results = new GmosResult[ccdArray.length];
        if (_obsDetailParameters.getMethod().isSpectroscopy()) {
            for (int i = 0; i < ccdArray.length; i++) {
                final Gmos instrument = ccdArray[i];
                final GmosSourceResult calcSrcGmos = invoke(instrument);
                results[i] = invokeSpectroscopy(calcSrcGmos, mainInstrument, ccdArray.length);
            }
        } else {
            for (int i = 0; i < ccdArray.length; i++) {
                final Gmos instrument = ccdArray[i];
                final GmosSourceResult calcSrcGmos = invoke(instrument);
                results[i] = invokeImaging(calcSrcGmos);
            }
        }

        return results;
    }

    private GmosResult[] calculate(final Gmos mainInstrument) throws Exception {

        final Gmos[] ccdArray = mainInstrument.getDetectorCcdInstruments();
        final GmosResult[] results = new GmosResult[ccdArray.length];
        if (_obsDetailParameters.getMethod().isSpectroscopy()) {
            for (int i = 0; i < ccdArray.length; i++) {
                final Gmos instrument = ccdArray[i];
                final GmosSourceResult calcSrcGmos = invoke(instrument);
                results[i] = invokeSpectroscopy(calcSrcGmos, mainInstrument, ccdArray.length);
            }
        } else {
            for (int i = 0; i < ccdArray.length; i++) {
                final Gmos instrument = ccdArray[i];
                final GmosSourceResult calcSrcGmos = invoke(instrument);
                results[i] = invokeImaging(calcSrcGmos);
            }
        }

        return results;

    }

    private Gmos createGmos() {
        switch (_gmosParameters.getSite()) {
            case GN: return new GmosNorth(_gmosParameters, _obsDetailParameters, 0);
            case GS: return new GmosSouth(_gmosParameters, _obsDetailParameters, 0);
            default: throw new Error("invalid site");
        }
    }

    private void writeSpectroscopyOutput(final Gmos mainInstrument, final GmosResult[] results) throws Exception {
        _println("");

        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();

        // Create one chart to use for all 3 CCDS (one for Signal and Background and one for Intermediate Single Exp and Final S/N)
        final ITCChart gmosChart1;
        final ITCChart gmosChart2;
        final boolean ifuAndNotUniform = mainInstrument.isIfuUsed() && !(_sdParameters.isUniform());
        final double ifu_offset = ifuAndNotUniform ? mainInstrument.getIFU().getApertureOffsetList().iterator().next() : 0.0;
        final String chart1Title = ifuAndNotUniform ? "Signal and Background (IFU element offset: " + device.toString(ifu_offset) + " arcsec)" : "Signal and Background ";
        final String chart2Title = ifuAndNotUniform ? "Intermediate Single Exp and Final S/N (IFU element offset: " + device.toString(ifu_offset) + " arcsec)" : "Intermediate Single Exp and Final S/N";
        gmosChart1 = new ITCChart(chart1Title, "Wavelength (nm)", "e- per exposure per spectral pixel", _plotParameters);
        gmosChart2 = new ITCChart(chart2Title, "Wavelength (nm)", "Signal / Noise per spectral pixel", _plotParameters);

        String sigSpec = null, backSpec = null, singleS2N = null, finalS2N = null;
        final Gmos[] ccdArray = mainInstrument.getDetectorCcdInstruments();
        final DetectorsTransmissionVisitor tv = mainInstrument.getDetectorTransmision();
        final int detectorCount = ccdArray.length;

        for (final Gmos instrument : ccdArray) {

            final int ccdIndex = instrument.getDetectorCcdIndex();
            final String ccdName = instrument.getDetectorCcdName();
            final Color ccdColor = instrument.getDetectorCcdColor();
            final Color ccdColorDarker = ccdColor == null ? null : ccdColor.darker().darker();
            final int firstCcdIndex = tv.getDetectorCcdStartIndex(ccdIndex);
            final int lastCcdIndex = tv.getDetectorCcdEndIndex(ccdIndex, detectorCount);
            // REL-478: include the gaps in the text data output
            final int lastCcdIndexWithGap = (ccdIndex < 2 && detectorCount > 1)
                    ? tv.getDetectorCcdStartIndex(ccdIndex + 1)
                    : lastCcdIndex;

            final GmosSpectroscopyResult calcGmos = (GmosSpectroscopyResult) results[ccdIndex];

            final int number_exposures = _obsDetailParameters.getNumExposures();
            final double frac_with_source = _obsDetailParameters.getSourceFraction();
            final double exposure_time = _obsDetailParameters.getExposureTime();

            if (ccdIndex == 0) {
                _println("Read noise: " + instrument.getReadNoise());
                if (!instrument.isIfuUsed()) {
                    if (!_obsDetailParameters.isAutoAperture()) {
                        _println("software aperture extent along slit = " + device.toString(_obsDetailParameters.getApertureDiameter()) + " arcsec");
                    } else {
                        switch (_sdParameters.getProfileType()) {
                            case UNIFORM:
                                _println("software aperture extent along slit = " + device.toString(1 / _gmosParameters.getFPMask()) + " arcsec");
                                break;
                            case POINT:
                                _println("software aperture extent along slit = " + device.toString(1.4 * calcGmos.IQcalc.getImageQuality()) + " arcsec");
                                break;
                        }
                    }

                    if (!_sdParameters.isUniform()) {
                        _println("fraction of source flux in aperture = " + device.toString(calcGmos.st.getSlitThroughput()));
                    }
                }
                _println("derived image size(FWHM) for a point source = " + device.toString(calcGmos.IQcalc.getImageQuality()) + "arcsec\n");
                _println("Sky subtraction aperture = " + _obsDetailParameters.getSkyApertureDiameter() + " times the software aperture.");
                _println("");
                _println("Requested total integration time = " + device.toString(exposure_time * number_exposures) + " secs, of which " + device.toString(exposure_time * number_exposures * frac_with_source) + " secs is on source.");
                _print("<HR align=left SIZE=3>");
            }

            // For IFUs we can have more than one S2N result.
            for (int i = 0; i < calcGmos.specS2N.length; i++) {

                gmosChart1.addArray(calcGmos.specS2N[i].getSignalSpectrum().getData(firstCcdIndex, lastCcdIndex), "Signal " + ccdName, ccdColor);
                gmosChart1.addArray(calcGmos.specS2N[i].getBackgroundSpectrum().getData(firstCcdIndex, lastCcdIndex), "SQRT(Background) " + ccdName, ccdColorDarker);

                gmosChart2.addArray(calcGmos.specS2N[i].getExpS2NSpectrum().getData(firstCcdIndex, lastCcdIndex), "Single Exp S/N " + ccdName, ccdColor);
                gmosChart2.addArray(calcGmos.specS2N[i].getFinalS2NSpectrum().getData(firstCcdIndex, lastCcdIndex), "Final S/N " + ccdName, ccdColorDarker);

                if (ccdIndex == 0) {
                    _println("<p style=\"page-break-inside: never\">");
                    sigSpec = _printSpecTag("ASCII signal spectrum");
                    backSpec = _printSpecTag("ASCII background spectrum");
                    singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
                    finalS2N = _printSpecTag("Final S/N ASCII data");
                }
                _println("");
            }

            _println(calcGmos.specS2N[calcGmos.specS2N.length-1].getSignalSpectrum(), _header, sigSpec, firstCcdIndex, lastCcdIndexWithGap);
            _println(calcGmos.specS2N[calcGmos.specS2N.length-1].getBackgroundSpectrum(), _header, backSpec, firstCcdIndex, lastCcdIndexWithGap);
            _println(calcGmos.specS2N[calcGmos.specS2N.length-1].getExpS2NSpectrum(), _header, singleS2N, firstCcdIndex, lastCcdIndexWithGap);
            _println(calcGmos.specS2N[calcGmos.specS2N.length-1].getFinalS2NSpectrum(), _header, finalS2N, firstCcdIndex, lastCcdIndexWithGap);

        }

        _println(gmosChart1.getBufferedImage(), "SigAndBack");
        _println("");
        _println(gmosChart2.getBufferedImage(), "Sig2N");
        _println("");

        printConfiguration(mainInstrument);
    }


    private void writeImagingOutput(final Gmos mainInstrument, final GmosResult[] results) {
        _println("");

        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();


        final Gmos[] ccdArray = mainInstrument.getDetectorCcdInstruments();
        for (final Gmos instrument : ccdArray) {
            final int ccdIndex = instrument.getDetectorCcdIndex();
            final String ccdName = instrument.getDetectorCcdName();
            final String forCcdName = ccdName.length() == 0 ? "" : " for " + ccdName;

            final GmosImagingResult calcGmos = (GmosImagingResult) results[ccdIndex];

            if (ccdIndex == 0) {
                _print(calcGmos.SFcalc.getTextResult(device));
                _println(calcGmos.IQcalc.getTextResult(device));
                _println("Sky subtraction aperture = "
                        + _obsDetailParameters.getSkyApertureDiameter()
                        + " times the software aperture.\n");
                _println("Read noise: " + instrument.getReadNoise());
            }
            _println("");
            _println("<b>S/N" + forCcdName + ":</b>");
            _println("");
            _println(calcGmos.IS2Ncalc.getTextResult(device));

            device.setPrecision(0); // NO decimal places
            device.clear();
            final int binFactor = instrument.getSpatialBinning() * instrument.getSpatialBinning();

            _println("");
            _println("The peak pixel signal + background is " + device.toString(calcGmos.peak_pixel_count) + ". ");

            if (calcGmos.peak_pixel_count > (.95 * instrument.getWellDepth() * binFactor))
                _println("Warning: peak pixel may be saturating the (binned) CCD full well of "
                        + .95 * instrument.getWellDepth() * binFactor);

            if (calcGmos.peak_pixel_count > (.95 * instrument.getADSaturation() * instrument.getLowGain()))
                _println("Warning: peak pixel may be saturating the low gain setting of "
                        + .95
                        * instrument.getADSaturation()
                        * instrument.getLowGain());

            if (calcGmos.peak_pixel_count > (.95 * instrument.getADSaturation() * instrument.getHighGain()))
                _println("Warning: peak pixel may be saturating the high gain setting "
                        + .95
                        * instrument.getADSaturation()
                        * instrument.getHighGain());

        }

        printConfiguration(mainInstrument);
    }

    private void printConfiguration(final Gmos mainInstrument) {
        _println("");

        final FormatStringWriter device = new FormatStringWriter();
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

    // Calculation results
    // TEMPORARY, these will probably turn into Scala case classes
    private final class GmosSourceResult {
        public final Gmos instrument;
        public final VisitableSampledSpectrum sed;
        public final VisitableSampledSpectrum sky;
        public GmosSourceResult(final Gmos instrument, final VisitableSampledSpectrum sed, final VisitableSampledSpectrum sky) {
            this.instrument         = instrument;
            this.sed                = sed;
            this.sky                = sky;
        }

    }
    interface GmosResult {}
    private final class GmosImagingResult implements GmosResult {
        public final SourceFraction SFcalc;
        public final double peak_pixel_count;
        public final ImagingS2NCalculatable IS2Ncalc;
        public final ImageQualityCalculatable IQcalc;
        public GmosImagingResult(final ImageQualityCalculatable IQcalc, final SourceFraction SFcalc, final double peak_pixel_count, final ImagingS2NCalculatable IS2Ncalc) {
            this.IQcalc             = IQcalc;
            this.SFcalc             = SFcalc;
            this.peak_pixel_count   = peak_pixel_count;
            this.IS2Ncalc           = IS2Ncalc;
        }

    }
    private final class GmosSpectroscopyResult implements GmosResult {
        public final SpecS2NLargeSlitVisitor[] specS2N;
        public final SlitThroughput st;
        public final ImageQualityCalculatable IQcalc;
        public GmosSpectroscopyResult(final ImageQualityCalculatable IQcalc, final SpecS2NLargeSlitVisitor[] specS2N, final SlitThroughput st) {
            this.IQcalc             = IQcalc;
            this.specS2N            = specS2N;
            this.st                 = st;
        }
    }


    // ===========  SPECTROSCOPY

    private GmosSpectroscopyResult invokeSpectroscopy(final GmosSourceResult src, final Gmos mainInstrument, final int detectorCount) throws Exception {

        final SpecS2NLargeSlitVisitor[] specS2N;
        final SlitThroughput st;

        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED

        final int ccdIndex = src.instrument.getDetectorCcdIndex();
        final DetectorsTransmissionVisitor tv = mainInstrument.getDetectorTransmision();
        final int firstCcdIndex = tv.getDetectorCcdStartIndex(ccdIndex);
        final int lastCcdIndex = tv.getDetectorCcdEndIndex(ccdIndex, detectorCount);

        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio. There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification

        final double pixel_size = src.instrument.getPixelSize();
        double ap_diam;
        double source_fraction;
        List<Double> sf_list = new ArrayList<>();

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _teleParameters, src.instrument);
        IQcalc.calculate();
        double im_qual = IQcalc.getImageQuality();

        if (!src.instrument.isIfuUsed()) {
            // Calculate the Fraction of source in the aperture
            final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, src.instrument, im_qual);
            source_fraction = SFcalc.getSourceFraction();
        } else {
            final VisitableMorphology morph;
            if (!_sdParameters.isUniform()) {
                morph = new GaussianMorphology(im_qual);
            } else {
                morph = new USBMorphology();
            }
            morph.accept(src.instrument.getIFU().getAperture());
            // for now just a single item from the list
            sf_list = src.instrument.getIFU().getFractionOfSourceInAperture();
            source_fraction = sf_list.get(0);
        }


        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.
        double spec_source_frac;
        final int number_exposures = _obsDetailParameters.getNumExposures();
        final double frac_with_source = _obsDetailParameters.getSourceFraction();
        final double dark_current = src.instrument.getDarkCurrent();
        final double exposure_time = _obsDetailParameters.getExposureTime();
        final double read_noise = src.instrument.getReadNoise();

        // report error if this does not come out to be an integer
        checkSourceFraction(number_exposures, frac_with_source);

        // ObservationMode Imaging or spectroscopy
        if (!src.instrument.isIfuUsed()) {
            if (!_obsDetailParameters.isAutoAperture()) {
                st = new SlitThroughput(im_qual, _obsDetailParameters.getApertureDiameter(), pixel_size, _gmosParameters.getFPMask());
            } else {
                st = new SlitThroughput(im_qual, pixel_size, _gmosParameters.getFPMask());
            }
            ap_diam = st.getSpatialPix();
            spec_source_frac = st.getSlitThroughput();
        } else {
            st = null; // TODO: how to deal with no ST in case of IFU?
            spec_source_frac = source_fraction;
            ap_diam = 5 / src.instrument.getSpatialBinning();
        }

        // For the usb case we want the resolution to be determined by the
        // slit width and not the image quality for a point source.
        if (_sdParameters.isUniform()) {
            im_qual = 10000;

            if (!src.instrument.isIfuUsed()) {

                if (!_obsDetailParameters.isAutoAperture()) {
                    spec_source_frac = _gmosParameters.getFPMask() * ap_diam * pixel_size;
                } else {
                    ap_diam = new Double(1 / (_gmosParameters.getFPMask() * pixel_size) + 0.5).intValue();
                    spec_source_frac = 1;
                }
            }
        }

        if (src.instrument.isIfuUsed() && !_sdParameters.isUniform()) {
            specS2N = new SpecS2NLargeSlitVisitor[sf_list.size()];
            for (int i = 0; i < sf_list.size(); i++) {
                final double spsf = sf_list.get(i);
                specS2N[i] = new SpecS2NLargeSlitVisitor(
                        _gmosParameters.getFPMask(),
                        pixel_size,
                        src.instrument.getSpectralPixelWidth(),
                        src.instrument.getObservingStart(),
                        src.instrument.getObservingEnd(),
                        src.instrument.getGratingDispersion_nm(),
                        src.instrument.getGratingDispersion_nmppix(),
                        src.instrument.getGratingResolution(),
                        spsf,
                        im_qual,
                        ap_diam,
                        number_exposures,
                        frac_with_source,
                        exposure_time,
                        dark_current * src.instrument.getSpatialBinning() * src.instrument.getSpectralBinning(),
                        read_noise,
                        _obsDetailParameters.getSkyApertureDiameter(),
                        src.instrument.getSpectralBinning());

                specS2N[i].setDetectorTransmission(mainInstrument.getDetectorTransmision());
                specS2N[i].setCcdPixelRange(firstCcdIndex, lastCcdIndex);
                specS2N[i].setSourceSpectrum(src.sed);
                specS2N[i].setBackgroundSpectrum(src.sky);
                src.sed.accept(specS2N[i]);

            }
        } else {
            specS2N = new SpecS2NLargeSlitVisitor[1];
            specS2N[0] = new SpecS2NLargeSlitVisitor(
                    _gmosParameters.getFPMask(),
                    pixel_size,
                    src.instrument.getSpectralPixelWidth(),
                    src.instrument.getObservingStart(),
                    src.instrument.getObservingEnd(),
                    src.instrument.getGratingDispersion_nm(),
                    src.instrument.getGratingDispersion_nmppix(),
                    src.instrument.getGratingResolution(),
                    spec_source_frac,
                    im_qual,
                    ap_diam,
                    number_exposures,
                    frac_with_source,
                    exposure_time,
                    dark_current * src.instrument.getSpatialBinning() * src.instrument.getSpectralBinning(),
                    read_noise,
                    _obsDetailParameters.getSkyApertureDiameter(),
                    src.instrument.getSpectralBinning());

            specS2N[0].setDetectorTransmission(mainInstrument.getDetectorTransmision());
            specS2N[0].setCcdPixelRange(firstCcdIndex, lastCcdIndex);
            specS2N[0].setSourceSpectrum(src.sed);
            specS2N[0].setBackgroundSpectrum(src.sky);
            src.sed.accept(specS2N[0]);

        }

        return new GmosSpectroscopyResult(IQcalc, specS2N, st);

    }


    // ===========  IMAGING

    private GmosImagingResult invokeImaging(final GmosSourceResult src) {

        // Start of morphology section of ITC

        // Module 1a
        // The purpose of this section is to calculate the fraction of the
        // source flux which is contained within an aperture which we adopt
        // to derive the signal to noise ratio. There are several cases
        // depending on the source morphology.
        // Define the source morphology
        //
        // inputs: source morphology specification

        final double pixel_size = src.instrument.getPixelSize();
        final double sed_integral = src.sed.getIntegral();
        final double sky_integral = src.sky.getIntegral();

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _teleParameters, src.instrument);
        IQcalc.calculate();
        final double im_qual = IQcalc.getImageQuality();

        // Calculate the Fraction of source in the aperture
        final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, src.instrument, im_qual);
        final double source_fraction = SFcalc.getSourceFraction();
        final double Npix = SFcalc.getNPix();

        // Calculate the Peak Pixel Flux
        final PeakPixelFluxCalc ppfc  = new PeakPixelFluxCalc(im_qual, pixel_size, _obsDetailParameters.getExposureTime(), sed_integral, sky_integral, src.instrument.getDarkCurrent());
        final double peak_pixel_count;
        if (!_sdParameters.isUniform()) {
            peak_pixel_count = ppfc.getFluxInPeakPixel();
        } else {
            peak_pixel_count = ppfc.getFluxInPeakPixelUSB(source_fraction, Npix);
        }

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.
        final int number_exposures = _obsDetailParameters.getNumExposures();
        final double frac_with_source = _obsDetailParameters.getSourceFraction();
        // report error if this does not come out to be an integer
        checkSourceFraction(number_exposures, frac_with_source);

        final ImagingS2NCalculatable IS2Ncalc = ImagingS2NCalculationFactory.getCalculationInstance(_sdParameters, _obsDetailParameters, src.instrument);
        IS2Ncalc.setSedIntegral(sed_integral);
        IS2Ncalc.setSkyIntegral(sky_integral);
        IS2Ncalc.setSkyAperture(_obsDetailParameters.getSkyApertureDiameter());
        IS2Ncalc.setSourceFraction(source_fraction);
        IS2Ncalc.setNpix(Npix);
        IS2Ncalc.setDarkCurrent(src.instrument.getDarkCurrent() * src.instrument.getSpatialBinning() * src.instrument.getSpatialBinning());
        IS2Ncalc.calculate();

        return new GmosImagingResult(IQcalc, SFcalc, peak_pixel_count, IS2Ncalc);

    }

    // ===========  GENERAL
    private GmosSourceResult invoke(final Gmos instrument) {

        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED

        final VisitableSampledSpectrum sed = SEDFactory.getSED(_sdParameters, instrument);
        final SampledSpectrumVisitor redshift = new RedshiftVisitor(_sdParameters.getRedshift());
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
                    throw new RuntimeException("Shifted spectrum lies outside of specified normalisation waveband.");
                }
        }

        if (_plotParameters.getPlotLimits().equals(PlottingDetailsParameters.PlotLimits.USER)) {
            if (_plotParameters.getPlotWaveL() > instrument.getObservingEnd()
                    || _plotParameters.getPlotWaveU() < instrument
                    .getObservingStart()) {
                //_println(" The user limits defined for plotting do not overlap with the Spectrum.");

                throw new RuntimeException("User limits for plotting do not overlap with filter.");
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

        final SampledSpectrumVisitor tel = new TelescopeApertureVisitor();
        sed.accept(tel);

        // SED is now in units of photons/s/nm

        // Module 3b
        // The atmosphere and telescope modify the spectrum and
        // produce a background spectrum.
        //
        // inputs: SED, AIRMASS, sky emmision file, mirror configuration,
        // output: SED and sky background as they arrive at instruments

        final SampledSpectrumVisitor clouds = CloudTransmissionVisitor.create(_obsConditionParameters.getSkyTransparencyCloud());
        sed.accept(clouds);

        final SampledSpectrumVisitor water = WaterTransmissionVisitor.create(
                _obsConditionParameters.getSkyTransparencyWater(),
                _obsConditionParameters.getAirmass(), "skytrans_",
                _gmosParameters.getSite(), ITCConstants.VISIBLE);
        sed.accept(water);

        // Background spectrum is introduced here.
        final VisitableSampledSpectrum sky = SEDFactory.getSED(
                ITCConstants.SKY_BACKGROUND_LIB + "/"
                        + ITCConstants.OPTICAL_SKY_BACKGROUND_FILENAME_BASE
                        + "_"
                        + _obsConditionParameters.getSkyBackgroundCategory()
                        + "_" + _obsConditionParameters.getAirmassCategory()
                        + ITCConstants.DATA_SUFFIX, instrument.getSampling());

        // Apply telescope transmission to both sed and sky
        final SampledSpectrumVisitor t = TelescopeTransmissionVisitor.create(_teleParameters);
        sed.accept(t);
        sky.accept(t);

        // Create and Add background for the telescope.
        final SampledSpectrumVisitor tb = new TelescopeBackgroundVisitor(_teleParameters, _gmosParameters.getSite(), ITCConstants.VISIBLE);
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

        // For debugging, print the spectrum integrals.
        // _println("SED integral: "+sed_integral+"\tSKY integral: "+sky_integral);

        // End of the Spectral energy distribution portion of the ITC.
        return new GmosSourceResult(instrument, sed, sky);
    }

}
