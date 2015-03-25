package edu.gemini.itc.niri;

import edu.gemini.itc.altair.*;
import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import edu.gemini.itc.web.HtmlPrinter;
import edu.gemini.itc.web.ITCRequest;
import edu.gemini.spModel.core.Site;
import scala.Option;

import java.io.PrintWriter;
import java.util.Calendar;

/**
 * This class performs the calculations for Niri used for imaging.
 */
public final class NiriRecipe extends RecipeBase {

    private final AltairParameters _altairParameters;
    private final StringBuffer _header = new StringBuffer("# NIRI ITC: " + Calendar.getInstance().getTime() + "\n");

    private final NiriParameters _niriParameters;
    private final ObservingConditions _obsConditionParameters;
    private final ObservationDetails _obsDetailParameters;
    private final PlottingDetails _plotParameters;
    private final SourceDefinition _sdParameters;
    private final TelescopeDetails _telescope;

    private String sigSpec, backSpec, singleS2N, finalS2N;
    private SpecS2NVisitor specS2N;

    /**
     * Constructs a NiriRecipe by parsing a Multipart servlet request.
     *
     * @param r   Servlet request containing form data from ITC web page.
     * @param out Results will be written to this PrintWriter.
     * @throws Exception on failure to parse parameters.
     */
    public NiriRecipe(ITCMultiPartParser r, PrintWriter out) {
        super(out);
        _sdParameters = ITCRequest.sourceDefinitionParameters(r);
        _obsDetailParameters = ITCRequest.observationParameters(r);
        _obsConditionParameters = ITCRequest.obsConditionParameters(r);
        _niriParameters = new NiriParameters(r);
        _telescope = ITCRequest.teleParameters(r);
        _altairParameters = ITCRequest.altairParameters(r);
        _plotParameters = ITCRequest.plotParamters(r);

        validateInputParameters();
    }

    /**
     * Constructs a NiriRecipe given the parameters. Useful for testing.
     */
    public NiriRecipe(SourceDefinition sdParameters,
                      ObservationDetails obsDetailParameters,
                      ObservingConditions obsConditionParameters,
                      NiriParameters niriParameters, TelescopeDetails telescope,
                      AltairParameters altairParameters,
                      PlottingDetails plotParameters,
                      PrintWriter out)

    {
        super(out);
        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _niriParameters = niriParameters;
        _telescope = telescope;
        _altairParameters = altairParameters;
        _plotParameters = plotParameters;

        validateInputParameters();
    }

    private void validateInputParameters() {
        if (_altairParameters.altairIsUsed()) {
            if (_obsDetailParameters.getMethod().isSpectroscopy()) {
                throw new IllegalArgumentException(
                        "Altair cannot currently be used with Spectroscopy mode in the ITC.  Please deselect either altair or spectroscopy and resubmit the form.");
            }
        }

        if (_sdParameters.getDistributionType().equals(SourceDefinition.Distribution.ELINE))
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters
                    .getELineWavelength() * 1000 * 25))) { // *25 b/c of
                // increased
                // resolution of
                // transmission
                // files
                throw new RuntimeException(
                        "Please use a model line width > 0.04 nm (or "
                                + (3E5 / (_sdParameters.getELineWavelength() * 1000 * 25))
                                + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }
    }

    /**
     * Performes recipe calculation and writes results to a cached PrintWriter
     * or to System.out.
     *
     * @throws Exception A recipe calculation can fail in many ways, missing data
     *                   files, incorrectly-formatted data files, ...
     */
    public void writeOutput() {
        // Create the Chart visitor. After a sed has been created the chart
        // visitor
        // can be used by calling the following commented out code:

        // NiriChart.setName("Original SED");
        // sed.accept(NiriChart);
        // _println(NiriChart.getTag());
        _println("");
        // ChartVisitor NiriChart = new ChartVisitor();

        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
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
        final Niri instrument = new Niri(_niriParameters, _obsDetailParameters);

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        // Altair specific section
        final Option<AOSystem> altair;
        if (_altairParameters.altairIsUsed()) {
            final Altair ao = new Altair(instrument.getEffectiveWavelength(), _telescope.getTelescopeDiameter(), IQcalc.getImageQuality(), _altairParameters, 0.0);
            _println(ao.printSummary());
            altair = Option.apply((AOSystem) ao);
        } else {
            altair = Option.empty();
        }

        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, Site.GN, ITCConstants.NEAR_IR, _sdParameters, _obsConditionParameters, _telescope, _plotParameters, altair);

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

        final double pixel_size = instrument.getPixelSize();
        double ap_diam = 0;
        double halo_source_fraction = 0;

        final double sed_integral = calcSource.sed.getIntegral();
        final double sky_integral = calcSource.sky.getIntegral();

        double halo_integral = 0;
        if (_altairParameters.altairIsUsed()) {
            halo_integral = calcSource.halo.get().getIntegral();
        }


        // if altair is used we need to calculate both a core and halo
        // source_fraction
        // halo first
        final SourceFraction SFcalc;
        if (_altairParameters.altairIsUsed()) {
            // If altair is used turn off printing of SF calc
            final SourceFraction SFcalcHalo;
            final double im_qual = altair.get().getAOCorrectedFWHM();
            if (_obsDetailParameters.isAutoAperture()) {
                SFcalcHalo = SourceFractionFactory.calculate(_sdParameters.isUniform(), false, 1.18 * im_qual, instrument.getPixelSize(), IQcalc.getImageQuality());
                SFcalc = SourceFractionFactory.calculate(_sdParameters.isUniform(), _obsDetailParameters.isAutoAperture(), 1.18 * im_qual, instrument.getPixelSize(), im_qual);
            } else {
                SFcalcHalo = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, IQcalc.getImageQuality());
                SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, im_qual);
            }
            halo_source_fraction = SFcalcHalo.getSourceFraction();
        } else {
            // this will be the core for an altair source; unchanged for non altair.
            SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, IQcalc.getImageQuality());
        }
        if (_obsDetailParameters.getMethod().isImaging()) {
            if (_altairParameters.altairIsUsed()) {
                _print(SFcalc.getTextResult(device, false));
                _println("derived image halo size (FWHM) for a point source = "
                        + device.toString(IQcalc.getImageQuality()) + " arcsec.\n");
            } else {
                _print(SFcalc.getTextResult(device));
                _println(IQcalc.getTextResult(device));
            }
        }

        // Calculate peak pixel flux
        final double im_qual = altair.isDefined() ? altair.get().getAOCorrectedFWHM() : IQcalc.getImageQuality();
        final double peak_pixel_count = altair.isDefined() ?
                PeakPixelFlux.calculateWithHalo(instrument, _sdParameters, _obsDetailParameters, SFcalc, im_qual, IQcalc.getImageQuality(), halo_integral, sed_integral, sky_integral) :
                PeakPixelFlux.calculate(instrument, _sdParameters, _obsDetailParameters, SFcalc, im_qual, sed_integral, sky_integral);

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.
        final int number_exposures = _obsDetailParameters.getNumExposures();
        final double frac_with_source = _obsDetailParameters.getSourceFraction();
        final double dark_current = instrument.getDarkCurrent();
        final double exposure_time = _obsDetailParameters.getExposureTime();
        final double read_noise = instrument.getReadNoise();

        // report error if this does not come out to be an integer
        checkSourceFraction(number_exposures, frac_with_source);

        // ObservationMode Imaging or spectroscopy

        if (_obsDetailParameters.getMethod().isSpectroscopy()) {

            final SlitThroughput st;
            final SlitThroughput st_halo;

            if (!_obsDetailParameters.isAutoAperture()) {
                st = new SlitThroughput(im_qual,
                        _obsDetailParameters.getApertureDiameter(), pixel_size,
                        _niriParameters.getFPMask());
                st_halo = new SlitThroughput(IQcalc.getImageQuality(),
                        _obsDetailParameters.getApertureDiameter(), pixel_size,
                        _niriParameters.getFPMask());

                _println("software aperture extent along slit = "
                        + device.toString(_obsDetailParameters
                        .getApertureDiameter()) + " arcsec");
            } else {
                st = new SlitThroughput(im_qual, pixel_size,
                        _niriParameters.getFPMask());

                st_halo = new SlitThroughput(IQcalc.getImageQuality(), pixel_size,
                        _niriParameters.getFPMask());

                switch (_sdParameters.getProfileType()) {
                    case UNIFORM:
                        _println("software aperture extent along slit = " + device.toString(1 / _niriParameters.getFPMask()) + " arcsec");
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
                    + device.toString(im_qual) + " arcsec");

            _println("");
            _println("Requested total integration time = "
                    + device.toString(exposure_time * number_exposures)
                    + " secs, of which "
                    + device.toString(exposure_time * number_exposures
                    * frac_with_source) + " secs is on source.");

            _print("<HR align=left SIZE=3>");

            ap_diam = st.getSpatialPix();
            double spec_source_frac = st.getSlitThroughput();
            double halo_spec_source_frac = st_halo.getSlitThroughput();

            if (_sdParameters.isUniform()) {

                if (_obsDetailParameters.isAutoAperture()) {
                    ap_diam = new Double(1 / (_niriParameters.getFPMask() * pixel_size) + 0.5).intValue();
                    spec_source_frac = 1;
                } else {
                    spec_source_frac = _niriParameters.getFPMask() * ap_diam * pixel_size;
                }
            }

            specS2N = new SpecS2NVisitor(_niriParameters.getFPMask(),
                    pixel_size, instrument.getSpectralPixelWidth(),
                    instrument.getObservingStart(),
                    instrument.getObservingEnd(),
                    instrument.getGrismResolution(), spec_source_frac, im_qual,
                    ap_diam, number_exposures, frac_with_source, exposure_time,
                    dark_current, read_noise);
            specS2N.setSourceSpectrum(calcSource.sed);
            specS2N.setBackgroundSpectrum(calcSource.sky);
            specS2N.setHaloImageQuality(IQcalc.getImageQuality());
            if (_altairParameters.altairIsUsed())
                specS2N.setSpecHaloSourceFraction(halo_spec_source_frac);
            else
                specS2N.setSpecHaloSourceFraction(0.0);

            calcSource.sed.accept(specS2N);
            _println("<p style=\"page-break-inside: never\">");
            final ITCChart chart1 = new ITCChart("Signal and SQRT(Background) in software aperture of " + ap_diam + " pixels", "Wavelength (nm)", "e- per exposure per spectral pixel", _plotParameters);
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

        } else {
            final ImagingS2NCalculatable IS2Ncalc = ImagingS2NCalculationFactory.getCalculationInstance(_obsDetailParameters, instrument, SFcalc, sed_integral, sky_integral);
            if (_altairParameters.altairIsUsed()) {
                IS2Ncalc.setSecondaryIntegral(halo_integral);
                IS2Ncalc.setSecondarySourceFraction(halo_source_fraction);
            }
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

        _print("<HR align=left SIZE=3>");
        _println("<b>Input Parameters:</b>");
        _println("Instrument: " + instrument.getName() + "\n");
        _println(HtmlPrinter.printParameterSummary(_sdParameters));
        _println(instrument.toString());
        if (_altairParameters.altairIsUsed()) {
            _println(HtmlPrinter.printParameterSummary(_telescope, "altair"));
            _println(_altairParameters.printParameterSummary());
        } else {
            _println(HtmlPrinter.printParameterSummary(_telescope));
        }

        _println(HtmlPrinter.printParameterSummary(_obsConditionParameters));
        _println(HtmlPrinter.printParameterSummary(_obsDetailParameters));
        if (_obsDetailParameters.getMethod().isSpectroscopy()) {
            _println(HtmlPrinter.printParameterSummary(_plotParameters));
            _println(specS2N.getSignalSpectrum(), _header.toString(), sigSpec);
            _println(specS2N.getBackgroundSpectrum(), _header.toString(), backSpec);
            _println(specS2N.getExpS2NSpectrum(), _header.toString(), singleS2N);
            _println(specS2N.getFinalS2NSpectrum(), _header.toString(), finalS2N);
        }

    }

}
