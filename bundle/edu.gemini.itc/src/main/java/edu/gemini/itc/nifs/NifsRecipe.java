package edu.gemini.itc.nifs;

import edu.gemini.itc.altair.*;
import edu.gemini.itc.operation.ImageQualityCalculatable;
import edu.gemini.itc.operation.ImageQualityCalculationFactory;
import edu.gemini.itc.operation.SpecS2NLargeSlitVisitor;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.HtmlPrinter;
import edu.gemini.itc.web.ITCRequest;
import edu.gemini.spModel.core.Site;
import scala.Option;

import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 * This class performs the calculations for Nifs
 * used for imaging.
 */
public final class NifsRecipe extends RecipeBase {
    private Calendar now = Calendar.getInstance();
    private String _header = "# NIFS ITC: " + now.getTime() + "\n";

    private String sigSpec, backSpec, singleS2N, finalS2N;
    private SpecS2NLargeSlitVisitor specS2N;

    // Parameters from the web page.
    private final SourceDefinition _sdParameters;
    private final ObservationDetails _obsDetailParameters;
    private final ObservingConditions _obsConditionParameters;
    private final NifsParameters _nifsParameters;
    private final TelescopeDetails _telescope;
    private final AltairParameters _altairParameters;
    private final PlottingDetails _plotParameters;

    /**
     * Constructs a NifsRecipe by parsing a Multipart servlet request.
     *
     * @param r   Servlet request containing form data from ITC web page.
     * @param out Results will be written to this PrintWriter.
     * @throws Exception on failure to parse parameters.
     */
    public NifsRecipe(final ITCMultiPartParser r, final PrintWriter out) {
        super(out);

        // Read parameters from the four main sections of the web page.
        _sdParameters = ITCRequest.sourceDefinitionParameters(r);
        _obsDetailParameters = ITCRequest.observationParameters(r);
        _obsConditionParameters = ITCRequest.obsConditionParameters(r);
        _nifsParameters = new NifsParameters(r);
        _telescope = ITCRequest.teleParameters(r);
        _altairParameters = ITCRequest.altairParameters(r);
        _plotParameters = ITCRequest.plotParamters(r);
    }

    /**
     * Constructs a NifsRecipe given the parameters.
     * Useful for testing.
     */
    public NifsRecipe(final SourceDefinition sdParameters,
                      final ObservationDetails obsDetailParameters,
                      final ObservingConditions obsConditionParameters,
                      final NifsParameters nifsParameters,
                      final TelescopeDetails telescope,
                      final AltairParameters altairParameters,
                      final PlottingDetails plotParameters,
                      final PrintWriter out)

    {
        super(out);

        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _nifsParameters = nifsParameters;
        _telescope = telescope;
        _altairParameters = altairParameters;
        _plotParameters = plotParameters;
    }

    /**
     * Performes recipe calculation and writes results to a cached PrintWriter
     * or to System.out.
     */
    public void writeOutput() {
        _println("");
        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2);  // Two decimal places
        device.clear();

        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED
        final Nifs instrument = new NifsNorth(_nifsParameters, _obsDetailParameters);

        if (_sdParameters.getDistributionType().equals(SourceDefinition.Distribution.ELINE)) {
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters.getELineWavelength() * 1000 * 25))) {  // *25 b/c of increased resolutuion of transmission files
                throw new RuntimeException("Please use a model line width > 0.04 nm (or " + (3E5 / (_sdParameters.getELineWavelength() * 1000 * 25)) + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }
        }

        // TODO : THIS IS PURELY FOR REGRESSION TEST ONLY, REMOVE ASAP
        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource0 = SEDFactory.calculate(instrument, Site.GN, ITCConstants.NEAR_IR, _sdParameters, _obsConditionParameters, _telescope, _plotParameters);
        final VisitableSampledSpectrum sed0 = calcSource0.sed;
        final VisitableSampledSpectrum sky0 = calcSource0.sky;
        final double sed_integral0 = sed0.getIntegral();
        final double sky_integral0 = sky0.getIntegral();
        // Update this in (or remove from) regression test baseline:
        _println("SED Int: " + sed_integral0 + " Sky Int: " + sky_integral0);
        // TODO : THIS IS PURELY FOR REGRESSION TEST ONLY, REMOVE ASAP



        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        final Option<AOSystem> altair;
        if (_altairParameters.altairIsUsed()) {
            final Altair ao = new Altair(instrument.getEffectiveWavelength(), _telescope.getTelescopeDiameter(), IQcalc.getImageQuality(), _altairParameters, 0.0);
            altair = Option.apply((AOSystem) ao);
            _println(ao.printSummary());
        } else {
            altair = Option.empty();
        }

        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, Site.GN, ITCConstants.NEAR_IR, _sdParameters, _obsConditionParameters, _telescope, _plotParameters, altair);

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

        final double pixel_size = instrument.getPixelSize();
        final List<Double> sf_list;
        final List<Double> halo_sf_list;
        final List<Double> ap_offset_list;

        //IFU morphology section
        final double im_qual = altair.isDefined() ? altair.get().getAOCorrectedFWHM() : IQcalc.getImageQuality();
        final VisitableMorphology morph, haloMorphology;
        switch (_sdParameters.getProfileType()) {
            case POINT:
                morph = new AOMorphology(im_qual);
                haloMorphology = new AOMorphology(IQcalc.getImageQuality());
                break;
            case GAUSSIAN:
                morph = new GaussianMorphology(im_qual);
                haloMorphology = new GaussianMorphology(IQcalc.getImageQuality());
                break;
            case UNIFORM:
                morph = new USBMorphology();
                haloMorphology = new USBMorphology();
                break;
            default:
                throw new IllegalArgumentException();
        }
        morph.accept(instrument.getIFU().getAperture());

        //for now just a single item from the list
        sf_list = instrument.getIFU().getFractionOfSourceInAperture();  //extract corrected source fraction list

        instrument.getIFU().clearFractionOfSourceInAperture();
        haloMorphology.accept(instrument.getIFU().getAperture());


        halo_sf_list = instrument.getIFU().getFractionOfSourceInAperture();  //extract uncorrected halo source fraction list

        ap_offset_list = instrument.getIFU().getApertureOffsetList();

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.
        double spec_source_frac = 0;
        double halo_spec_source_frac = 0;
        final int number_exposures = _obsDetailParameters.getNumExposures();
        final double frac_with_source = _obsDetailParameters.getSourceFraction();
        final double dark_current = instrument.getDarkCurrent();
        final double exposure_time = _obsDetailParameters.getExposureTime();
        final double read_noise = instrument.getReadNoise();
        // report error if this does not come out to be an integer
        checkSourceFraction(number_exposures, frac_with_source);

        //ObservationMode Imaging or spectroscopy
        if (_obsDetailParameters.getMethod().isSpectroscopy()) {

            _println("derived image halo size (FWHM) for a point source = " + device.toString(IQcalc.getImageQuality()) + "arcsec\n");
            _println("Requested total integration time = " +
                    device.toString(exposure_time * number_exposures) +
                    " secs, of which " + device.toString(exposure_time *
                    number_exposures *
                    frac_with_source) +
                    " secs is on source.");

            _print("<HR align=left SIZE=3>");

            if (instrument.IFU_IsUsed()) {
                final Iterator<Double> src_frac_it = sf_list.iterator();
                final Iterator<Double> halo_src_frac_it = halo_sf_list.iterator();
                final Iterator<Double> ifu_offset_it = ap_offset_list.iterator();

                while (src_frac_it.hasNext()) {
                    double ap_diam = 1;
                    final double ifu_offset = ifu_offset_it.next();

                    if (_nifsParameters.getIFUMethod().equals(NifsParameters.SUMMED_APERTURE_IFU)) {
                        while (src_frac_it.hasNext()) {
                            spec_source_frac = spec_source_frac + src_frac_it.next();
                            halo_spec_source_frac = halo_spec_source_frac + halo_src_frac_it.next();
                            ap_diam = (ap_offset_list.size() / 2);
                        }
                    } else {
                        spec_source_frac = src_frac_it.next();
                        halo_spec_source_frac = halo_src_frac_it.next();
                        ap_diam = 1;
                    }


                    specS2N = new SpecS2NLargeSlitVisitor(_nifsParameters.getFPMask(), pixel_size,
                                    instrument.getSpectralPixelWidth(),
                                    instrument.getObservingStart(),
                                    instrument.getObservingEnd(),
                                    instrument.getGratingDispersion_nm(),
                                    instrument.getGratingDispersion_nmppix(),
                                    instrument.getGratingResolution(),
                                    spec_source_frac, im_qual,
                                    ap_diam, number_exposures,
                                    frac_with_source, exposure_time,
                                    dark_current, read_noise,
                                    _obsDetailParameters.getSkyApertureDiameter());

                    specS2N.setDetectorTransmission(instrument.getDetectorTransmision());
                    specS2N.setSourceSpectrum(calcSource.sed);
                    specS2N.setBackgroundSpectrum(calcSource.sky);
                    specS2N.setHaloSpectrum(altair.isDefined() ? calcSource.halo.get() : (VisitableSampledSpectrum) calcSource.sed.clone());
                    specS2N.setHaloImageQuality(IQcalc.getImageQuality());
                    if (_altairParameters.altairIsUsed())
                        specS2N.setSpecHaloSourceFraction(halo_spec_source_frac);
                    else
                        specS2N.setSpecHaloSourceFraction(0.0);

                    calcSource.sed.accept(specS2N);
                    _println("<p style=\"page-break-inside: never\">");
                    device.setPrecision(3);  // NO decimal places
                    device.clear();

                    final String chart1Title =
                            _nifsParameters.getIFUMethod().equals(NifsParameters.SUMMED_APERTURE_IFU) ?
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
                            _nifsParameters.getIFUMethod().equals(NifsParameters.SUMMED_APERTURE_IFU) ?
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

        }
        _println("");
        device.setPrecision(2);  // TWO decimal places
        device.clear();

        _print("<HR align=left SIZE=3>");

        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(_sdParameters));
        _println(instrument.toString());
        if (_altairParameters.altairIsUsed())
            _println(_altairParameters.printParameterSummary());
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
