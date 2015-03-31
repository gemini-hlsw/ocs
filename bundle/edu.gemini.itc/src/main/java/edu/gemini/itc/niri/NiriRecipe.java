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

        // report error if this does not come out to be an integer
        checkSourceFraction(_obsDetailParameters.getNumExposures(), _obsDetailParameters.getSourceFraction());

    }

    public void writeOutput() {
        final Niri instrument = new Niri(_niriParameters, _obsDetailParameters);
        if (_obsDetailParameters.getMethod().isImaging()) {
            final ImagingResult result = calculateImaging(instrument);
            writeImagingOutput(instrument, result);
        } else {
            final SpectroscopyResult result = calculateSpectroscopy(instrument);
            writeSpectroscopyOutput(instrument, result);
        }
    }

    private SpectroscopyResult calculateSpectroscopy(final Niri instrument) {
        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        // Altair specific section
        final Option<AOSystem> altair;
        if (_altairParameters.altairIsUsed()) {
            final Altair ao = new Altair(instrument.getEffectiveWavelength(), _telescope.getTelescopeDiameter(), IQcalc.getImageQuality(), _altairParameters, 0.0);
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

        // if altair is used we need to calculate both a core and halo
        // source_fraction
        // halo first
        final SourceFraction SFcalc;
        if (_altairParameters.altairIsUsed()) {
            final double aoCorrImgQual = altair.get().getAOCorrectedFWHM();
            if (_obsDetailParameters.isAutoAperture()) {
                SFcalc = SourceFractionFactory.calculate(_sdParameters.isUniform(), _obsDetailParameters.isAutoAperture(), 1.18 * aoCorrImgQual, instrument.getPixelSize(), aoCorrImgQual);
            } else {
                SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, aoCorrImgQual);
            }
        } else {
            // this will be the core for an altair source; unchanged for non altair.
            SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, IQcalc.getImageQuality());
        }

        final double im_qual = altair.isDefined() ? altair.get().getAOCorrectedFWHM() : IQcalc.getImageQuality();

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        final double pixel_size = instrument.getPixelSize();

        final SlitThroughput st;
        final SlitThroughput st_halo;
        if (!_obsDetailParameters.isAutoAperture()) {
            st = new SlitThroughput(im_qual,
                    _obsDetailParameters.getApertureDiameter(), pixel_size,
                    _niriParameters.getFPMask());
            st_halo = new SlitThroughput(IQcalc.getImageQuality(),
                    _obsDetailParameters.getApertureDiameter(), pixel_size,
                    _niriParameters.getFPMask());
        } else {
            st = new SlitThroughput(im_qual, pixel_size,
                    _niriParameters.getFPMask());

            st_halo = new SlitThroughput(IQcalc.getImageQuality(), pixel_size,
                    _niriParameters.getFPMask());
        }

        double ap_diam = st.getSpatialPix();
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

        final SpecS2NVisitor specS2N = new SpecS2NVisitor(_niriParameters.getFPMask(),
                pixel_size, instrument.getSpectralPixelWidth(),
                instrument.getObservingStart(),
                instrument.getObservingEnd(),
                instrument.getGrismResolution(), spec_source_frac, im_qual,
                ap_diam,
                _obsDetailParameters.getNumExposures(),
                _obsDetailParameters.getSourceFraction(),
                _obsDetailParameters.getExposureTime(),
                instrument.getDarkCurrent(),
                instrument.getReadNoise());
        specS2N.setSourceSpectrum(calcSource.sed);
        specS2N.setBackgroundSpectrum(calcSource.sky);
        if (_altairParameters.altairIsUsed())
            specS2N.setSpecHaloSourceFraction(halo_spec_source_frac);
        else
            specS2N.setSpecHaloSourceFraction(0.0);

        calcSource.sed.accept(specS2N);

        final SpecS2N[] specS2Narr = new SpecS2N[1];
        specS2Narr[0] = specS2N;

        return SpectroscopyResult.create(SFcalc, IQcalc, specS2Narr, st);
    }

    private ImagingResult calculateImaging(final Niri instrument) {
        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();

        // Altair specific section
        final Option<AOSystem> altair;
        if (_altairParameters.altairIsUsed()) {
            final Altair ao = new Altair(instrument.getEffectiveWavelength(), _telescope.getTelescopeDiameter(), IQcalc.getImageQuality(), _altairParameters, 0.0);
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

        // if altair is used we need to calculate both a core and halo
        // source_fraction
        // halo first
        final SourceFraction SFcalc;
        if (_altairParameters.altairIsUsed()) {
            final double aoCorrImgQual = altair.get().getAOCorrectedFWHM();
            if (_obsDetailParameters.isAutoAperture()) {
                SFcalc = SourceFractionFactory.calculate(_sdParameters.isUniform(), _obsDetailParameters.isAutoAperture(), 1.18 * aoCorrImgQual, instrument.getPixelSize(), aoCorrImgQual);
            } else {
                SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, aoCorrImgQual);
            }
        } else {
            // this will be the core for an altair source; unchanged for non altair.
            SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, IQcalc.getImageQuality());
        }

        final double im_qual = altair.isDefined() ? altair.get().getAOCorrectedFWHM() : IQcalc.getImageQuality();

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        final double sed_integral = calcSource.sed.getIntegral();
        final double sky_integral = calcSource.sky.getIntegral();
        final double halo_integral = _altairParameters.altairIsUsed() ? calcSource.halo.get().getIntegral() : 0.0;

        // Calculate peak pixel flux
        final double peak_pixel_count = altair.isDefined() ?
                PeakPixelFlux.calculateWithHalo(instrument, _sdParameters, _obsDetailParameters, SFcalc, im_qual, IQcalc.getImageQuality(), halo_integral, sed_integral, sky_integral) :
                PeakPixelFlux.calculate(instrument, _sdParameters, _obsDetailParameters, SFcalc, im_qual, sed_integral, sky_integral);

        final ImagingS2NCalculatable IS2Ncalc = ImagingS2NCalculationFactory.getCalculationInstance(_obsDetailParameters, instrument, SFcalc, sed_integral, sky_integral);
        if (_altairParameters.altairIsUsed()) {
            final SourceFraction SFcalcHalo;
            final double aoCorrImgQual = altair.get().getAOCorrectedFWHM();
            if (_obsDetailParameters.isAutoAperture()) {
                SFcalcHalo = SourceFractionFactory.calculate(_sdParameters.isUniform(), false, 1.18 * aoCorrImgQual, instrument.getPixelSize(), IQcalc.getImageQuality());
            } else {
                SFcalcHalo = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, IQcalc.getImageQuality());
            }
            IS2Ncalc.setSecondaryIntegral(halo_integral);
            IS2Ncalc.setSecondarySourceFraction(SFcalcHalo.getSourceFraction());
        }
        IS2Ncalc.calculate();

        return ImagingResult.create(IQcalc, SFcalc, peak_pixel_count, IS2Ncalc);

    }


    // ===================================================================================================================
    // TODO: OUTPUT METHODS
    // TODO: These need to be simplified/cleaned/shared and then go to the web module.. and then be deleted and forgotten.
    // ===================================================================================================================

    private void writeSpectroscopyOutput(final Niri instrument, final SpectroscopyResult result) {
        _println("");

        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();

        // Altair specific section
        if (_altairParameters.altairIsUsed()) {
            final Altair ao = new Altair(instrument.getEffectiveWavelength(), _telescope.getTelescopeDiameter(), result.IQcalc.getImageQuality(), _altairParameters, 0.0);
            _println(ao.printSummary());
        }

        if (!_obsDetailParameters.isAutoAperture()) {
            _println("software aperture extent along slit = "
                    + device.toString(_obsDetailParameters
                    .getApertureDiameter()) + " arcsec");
        } else {
            switch (_sdParameters.getProfileType()) {
                case UNIFORM:
                    _println("software aperture extent along slit = " + device.toString(1 / _niriParameters.getFPMask()) + " arcsec");
                    break;
                case POINT:
                    _println("software aperture extent along slit = " + device.toString(1.4 * result.specS2N[0].getImageQuality()) + " arcsec");
                    break;
            }
        }

        if (!_sdParameters.isUniform()) {
            _println("fraction of source flux in aperture = "
                    + device.toString(result.st.getSlitThroughput()));
        }

        _println("derived image size(FWHM) for a point source = "
                + device.toString(result.specS2N[0].getImageQuality()) + " arcsec");

        _println("");
        _println("Requested total integration time = "
                + device.toString(_obsDetailParameters.getExposureTime() * _obsDetailParameters.getNumExposures())
                + " secs, of which "
                + device.toString(_obsDetailParameters.getExposureTime() * _obsDetailParameters.getNumExposures()
                * _obsDetailParameters.getSourceFraction()) + " secs is on source.");

        _print("<HR align=left SIZE=3>");

        _println("<p style=\"page-break-inside: never\">");
        final ITCChart chart1 = new ITCChart("Signal and SQRT(Background) in software aperture of " + result.specS2N[0].getSpecNpix() + " pixels", "Wavelength (nm)", "e- per exposure per spectral pixel", _plotParameters);
        final ITCChart chart2 = new ITCChart("Intermediate Single Exp and Final S/N", "Wavelength (nm)", "Signal / Noise per spectral pixel", _plotParameters);

        chart1.addArray(result.specS2N[0].getSignalSpectrum().getData(), "Signal ");
        chart1.addArray(result.specS2N[0].getBackgroundSpectrum().getData(), "SQRT(Background)  ");
        _println(chart1.getBufferedImage(), "SigAndBack");
        _println("");

        final String sigSpec = _printSpecTag("ASCII signal spectrum");
        final String backSpec = _printSpecTag("ASCII background spectrum");

        chart2.addArray(result.specS2N[0].getExpS2NSpectrum().getData(), "Single Exp S/N");
        chart2.addArray(result.specS2N[0].getFinalS2NSpectrum().getData(), "Final S/N  ");
        _println(chart2.getBufferedImage(), "Sig2N");
        _println("");

        final String singleS2N = _printSpecTag("Single Exposure S/N ASCII data");
        final String finalS2N = _printSpecTag("Final S/N ASCII data");

        printConfiguration(instrument);

        _println(HtmlPrinter.printParameterSummary(_plotParameters));
        _println(result.specS2N[0].getSignalSpectrum(), _header.toString(), sigSpec);
        _println(result.specS2N[0].getBackgroundSpectrum(), _header.toString(), backSpec);
        _println(result.specS2N[0].getExpS2NSpectrum(), _header.toString(), singleS2N);
        _println(result.specS2N[0].getFinalS2NSpectrum(), _header.toString(), finalS2N);

    }

    private void writeImagingOutput(final Niri instrument, final ImagingResult result) {
        _println("");

        // This object is used to format numerical strings.
        final FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(2); // Two decimal places
        device.clear();

        // Altair specific section
        if (_altairParameters.altairIsUsed()) {
            final Altair ao = new Altair(instrument.getEffectiveWavelength(), _telescope.getTelescopeDiameter(), result.IQcalc.getImageQuality(), _altairParameters, 0.0);
            _println(ao.printSummary());
            _print(result.SFcalc.getTextResult(device, false));
            _println("derived image halo size (FWHM) for a point source = "
                    + device.toString(result.IQcalc.getImageQuality()) + " arcsec.\n");
        } else {
            _print(result.SFcalc.getTextResult(device));
            _println(result.IQcalc.getTextResult(device));
        }

        _println(result.IS2Ncalc.getTextResult(device));
        _println(result.IS2Ncalc.getBackgroundLimitResult());
        device.setPrecision(0); // NO decimal places
        device.clear();

        _println("");
        _println("The peak pixel signal + background is "
                + device.toString(result.peak_pixel_count)
                + ". This is "
                + device.toString(result.peak_pixel_count
                / instrument.getWellDepth() * 100)
                + "% of the full well depth of "
                + device.toString(instrument.getWellDepth()) + ".");

        if (result.peak_pixel_count > (.8 * instrument.getWellDepth()))
            _println("Warning: peak pixel exceeds 80% of the well depth and may be saturated");

        _println("");

        printConfiguration(instrument);

    }

    private void printConfiguration(final Niri instrument) {
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
    }

}
