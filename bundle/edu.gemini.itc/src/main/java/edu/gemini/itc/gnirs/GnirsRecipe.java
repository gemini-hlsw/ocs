package edu.gemini.itc.gnirs;

import edu.gemini.itc.operation.*;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.core.Site;

/**
 * This class performs the calculations for Gnirs used for imaging.
 */
public final class GnirsRecipe {

    public static final int ORDERS = 6;

    // Parameters from the web page.
    private final SourceDefinition _sdParameters;
    private final ObservationDetails _obsDetailParameters;
    private final ObservingConditions _obsConditionParameters;
    private final GnirsParameters _gnirsParameters;
    private final TelescopeDetails _telescope;

    private final VisitableSampledSpectrum[] signalOrder;
    private final VisitableSampledSpectrum[] backGroundOrder;
    private final VisitableSampledSpectrum[] finalS2NOrder;

    /**
     * Constructs a GnirsRecipe given the parameters. Useful for testing.
     */
    public GnirsRecipe(final SourceDefinition sdParameters,
                       final ObservationDetails obsDetailParameters,
                       final ObservingConditions obsConditionParameters,
                       final GnirsParameters gnirsParameters,
                       final TelescopeDetails telescope)

    {
        _sdParameters = sdParameters;
        _obsDetailParameters = obsDetailParameters;
        _obsConditionParameters = obsConditionParameters;
        _gnirsParameters = gnirsParameters;
        _telescope = telescope;

        signalOrder = new VisitableSampledSpectrum[ORDERS];
        backGroundOrder = new VisitableSampledSpectrum[ORDERS];
        finalS2NOrder = new VisitableSampledSpectrum[ORDERS];

        validateInputParameters();
    }

    private void validateInputParameters() {
        if (_sdParameters.getDistributionType().equals(SourceDefinition.Distribution.ELINE))
            // *25 b/c of increased resolutuion of transmission files
            if (_sdParameters.getELineWidth() < (3E5 / (_sdParameters.getELineWavelength() * 1000 * 25))) {
                throw new RuntimeException(
                        "Please use a model line width > 0.04 nm (or "
                                + (3E5 / (_sdParameters.getELineWavelength() * 1000 * 25))
                                + " km/s) to avoid undersampling of the line profile when convolved with the transmission response");
            }

        // report error if this does not come out to be an integer
        Validation.checkSourceFraction(_obsDetailParameters.getNumExposures(), _obsDetailParameters.getSourceFraction());
    }

   public GnirsSpectroscopyResult calculateSpectroscopy() {
        final Gnirs instrument = new Gnirs(_gnirsParameters, _obsDetailParameters);
        return calculateSpectroscopy(instrument);
    }

    private GnirsSpectroscopyResult calculateSpectroscopy(final Gnirs instrument) {
        // Module 1b
        // Define the source energy (as function of wavelength).
        //
        // inputs: instrument, SED
        // calculates: redshifted SED
        // output: redshifteed SED

        final double pixel_size = instrument.getPixelSize();
        double ap_diam = 0;

        // Calculate image quality
        final ImageQualityCalculatable IQcalc = ImageQualityCalculationFactory.getCalculationInstance(_sdParameters, _obsConditionParameters, _telescope, instrument);
        IQcalc.calculate();


        // Get the summed source and sky
        final SEDFactory.SourceResult calcSource = SEDFactory.calculate(instrument, Site.GN, ITCConstants.NEAR_IR, _sdParameters, _obsConditionParameters, _telescope);
        final VisitableSampledSpectrum sed = calcSource.sed;
        final VisitableSampledSpectrum sky = calcSource.sky;

        // Calculate the Fraction of source in the aperture
        final SourceFraction SFcalc = SourceFractionFactory.calculate(_sdParameters, _obsDetailParameters, instrument, IQcalc.getImageQuality());

        // In this version we are bypassing morphology modules 3a-5a.
        // i.e. the output morphology is same as the input morphology.
        // Might implement these modules at a later time.

        final SlitThroughput st;
        if (!_obsDetailParameters.isAutoAperture()) {
            st = new SlitThroughput(IQcalc.getImageQuality(),
                    _obsDetailParameters.getApertureDiameter(),
                    pixel_size, instrument.getFPMask());
        } else {
            st = new SlitThroughput(IQcalc.getImageQuality(), pixel_size, instrument.getFPMask());
        }

        ap_diam = st.getSpatialPix(); // ap_diam really Spec_Npix on

        double spec_source_frac = st.getSlitThroughput();

        // For the usb case we want the resolution to be determined by the
        // slit width and not the image quality for a point source.
        final double im_qual;
        if (_sdParameters.isUniform()) {
            im_qual = 10000;
            if (_obsDetailParameters.isAutoAperture()) {
                ap_diam = new Double(1 / (instrument.getFPMask() * pixel_size) + 0.5).intValue();
                spec_source_frac = 1;
            } else {
                spec_source_frac = instrument.getFPMask() * ap_diam * pixel_size;
            }
        } else {
            im_qual = IQcalc.getImageQuality();
        }

        final SpecS2NLargeSlitVisitor specS2N = new SpecS2NLargeSlitVisitor(
                instrument.getFPMask(), pixel_size,
                instrument.getSpectralPixelWidth() / instrument.getOrder(),
                instrument.getObservingStart(),
                instrument.getObservingEnd(),
                instrument.getGratingDispersion_nm(),
                instrument.getGratingDispersion_nmppix(),
                instrument.getGratingResolution(),
                spec_source_frac,
                im_qual, ap_diam,
                _obsDetailParameters.getNumExposures(),
                _obsDetailParameters.getSourceFraction(),
                _obsDetailParameters.getExposureTime(),
                instrument.getDarkCurrent(),
                instrument.getReadNoise(),
                _obsDetailParameters.getSkyApertureDiameter());

        specS2N.setDetectorTransmission(instrument.getDetectorTransmision());

        if (instrument.XDisp_IsUsed()) {
            final VisitableSampledSpectrum[] sedOrder = new VisitableSampledSpectrum[6];
            for (int i = 0; i < ORDERS; i++) {
                sedOrder[i] = (VisitableSampledSpectrum) sed.clone();
            }

            final VisitableSampledSpectrum[] skyOrder = new VisitableSampledSpectrum[6];
            for (int i = 0; i < ORDERS; i++) {
                skyOrder[i] = (VisitableSampledSpectrum) sky.clone();
            }

            final double trimCenter;
            if (instrument.getGrating().equals(GnirsParameters.G110)) {
                trimCenter = _gnirsParameters.getUnXDispCentralWavelength();
            } else {
                trimCenter = 2200.0;
            }

            for (int i = 0; i < ORDERS; i++) {
                final int order = i + 3;
                final double d         = instrument.getGratingDispersion_nmppix() / order * Gnirs.DETECTOR_PIXELS / 2;
                final double trimStart = trimCenter * 3 / order - d;
                final double trimEnd   = trimCenter * 3 / order + d;

                sedOrder[i].accept(instrument.getGratingOrderNTransmission(order));
                sedOrder[i].trim(trimStart, trimEnd);

                skyOrder[i].accept(instrument.getGratingOrderNTransmission(order));
                skyOrder[i].trim(trimStart, trimEnd);
            }

            for (int i = 0; i < ORDERS; i++) {
                final int order = i + 3;
                specS2N.setSourceSpectrum(sedOrder[i]);
                specS2N.setBackgroundSpectrum(skyOrder[i]);

                specS2N.setGratingDispersion_nmppix(instrument.getGratingDispersion_nmppix() / order);
                specS2N.setGratingDispersion_nm(instrument.getGratingDispersion_nm() / order);
                specS2N.setSpectralPixelWidth(instrument.getSpectralPixelWidth() / order);

                specS2N.setStartWavelength(sedOrder[i].getStart());
                specS2N.setEndWavelength(sedOrder[i].getEnd());

                sed.accept(specS2N);

                signalOrder[i] = (VisitableSampledSpectrum) specS2N.getSignalSpectrum().clone();
                backGroundOrder[i] = (VisitableSampledSpectrum) specS2N.getBackgroundSpectrum().clone();
            }

            for (int i = 0; i < ORDERS; i++) {
                final int order = i + 3;
                specS2N.setSourceSpectrum(sedOrder[i]);
                specS2N.setBackgroundSpectrum(skyOrder[i]);

                specS2N.setGratingDispersion_nmppix(instrument.getGratingDispersion_nmppix() / order);
                specS2N.setGratingDispersion_nm(instrument.getGratingDispersion_nm() / order);
                specS2N.setSpectralPixelWidth(instrument.getSpectralPixelWidth() / order);

                specS2N.setStartWavelength(sedOrder[i].getStart());
                specS2N.setEndWavelength(sedOrder[i].getEnd());

                sed.accept(specS2N);

                finalS2NOrder[i] = (VisitableSampledSpectrum) specS2N.getFinalS2NSpectrum().clone();
            }

        } else {

            sed.accept(instrument.getGratingOrderNTransmission(instrument.getOrder()));

            specS2N.setSourceSpectrum(sed);
            specS2N.setBackgroundSpectrum(sky);
            specS2N.setHaloImageQuality(0.0);
            specS2N.setSpecHaloSourceFraction(0.0);

            sed.accept(specS2N);

        }

        final Parameters p = new Parameters(_sdParameters, _obsDetailParameters, _obsConditionParameters, _telescope);
        final SpecS2N[] specS2Narr = new SpecS2N[] {specS2N};
        return new GnirsSpectroscopyResult(p, instrument, SFcalc, IQcalc, specS2Narr, st, signalOrder, backGroundOrder, finalS2NOrder);

    }

}
