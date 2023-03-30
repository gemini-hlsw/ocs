package edu.gemini.itc.ghost;

import edu.gemini.itc.base.*;
import edu.gemini.itc.shared.GhostParameters;
import edu.gemini.itc.shared.IfuMethod;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.ghost.Detector;
import edu.gemini.spModel.target.env.ResolutionMode;
import scala.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Ghost specification class
 */
public final class Ghost extends Instrument implements BinningProvider, SpectroscopyInstrument {

    private static final Logger Log = Logger.getLogger(Ghost.class.getName());

    public static final String INSTR_DIR = "ghost";

    public static final double PLATE_SCALE = 1.64;  // arcsec/mm

    private static final double WellDepth = 350000;  // VENU has to confirm the correct value.
    private final IFUComponent _ifu;
    private final TransmissionElement _ghostResolution;

    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_PREFIX = "ghost_";

    // Instrument reads its configuration from here.
    private static final double AD_SATURATION = 65535;

    private final GhostParameters gp;
    private final ObservationDetails odp;

    private final GhostGratingOptics _gratingOptics;
    private final edu.gemini.itc.base.Detector _detector;
    private final double _sampling;

    private static final String[] DETECTOR_CCD_NAMES = {"BLUE", "RED"};

    private static final String FILENAME = "ghost" + getSuffix();

    // These are the limits of observable wavelength with this configuration.

    private final Detector _ccdColor;

    private final GhostSaturLimitRule _ghostSaturLimitWarning;  // GHOST-specific saturation limit warning

    public Ghost(final GhostParameters gp, final ObservationDetails odp, final Detector ccdColor) {
        super(Site.GS, Bands.VISIBLE, INSTR_DIR, FILENAME);

        Log.fine("Resolution: " + gp.resolution());
        Log.fine("ccd_color: " + ccdColor.displayValue());
        this.odp    = odp;
        this.gp     = gp;

        _ccdColor = ccdColor;
        _detector = new edu.gemini.itc.base.Detector(getDirectory() + "/", getPrefix(),  _ccdColor.getModel(), _ccdColor.getModel());

        _detector.setDetectorPixels(_ccdColor.getXsize());
        addComponent(_detector);
        /*
      Provide value(s) of throughput for GHOST Cass unit and science cable, based on
      results from Ross Zhelem 21 May 2019. The throughput includes Cass unit
      (telecentricity lens and ADC) and fibre cable + microlens arrays at both ends,
      but not any aperture losses
     */
        TransmissionElement _cableThrougthput = new TransmissionElement(getDirectory() + "/" + Ghost.INSTR_PREFIX + "cable" + getSuffix());
        _cableThrougthput.setDescription("GHOST Cass unit and science cable");
        addComponent(_cableThrougthput);
        TransmissionElement _fixedOptics = new TransmissionElement(getDirectory() + "/" + Ghost.INSTR_PREFIX + "fixedOptics" + getSuffix());
        _fixedOptics.setDescription("Fixed Optics");
        addComponent(_fixedOptics);

        _gratingOptics = new GhostGratingOptics(
                getDirectory() + "/" + Ghost.INSTR_PREFIX,
                resolutionFileEnding(gp),
                "gratings",                                                // ghost_gratings.dat
                gp.centralWavelength().toNanometers(),
                _detector.getDetectorPixels(),
                gp.binning().getSpectralBinning());
        _gratingOptics.setDescription("Grating Resolution");
        addDisperser(_gratingOptics);
        _sampling = super.getSampling();
        _ifu = new IFUComponent(gp.resolution());
        addComponent(_ifu);

        _ghostSaturLimitWarning = new GhostSaturLimitRule(AD_SATURATION, WellDepth, getSpatialBinning(), getSpectralBinning(), gain() , 0.90);

        _ghostResolution = new TransmissionElement(getDirectory()+"/" + Ghost.INSTR_PREFIX + resolutionFileEnding(gp)+ "_perResolution" +getSuffix());
    }

    private String resolutionFileEnding(GhostParameters gp) {
        switch (gp.resolution()) {
            case GhostStandard: return "SR";
            case GhostPRV:
            case GhostHigh:
                return "HR";
            default: throw new RuntimeException("Cannot find an extension for resolution type");
        }
    }

    /**
     * Returns the name of the detector CCD
     */
    public String getDetectorCcdName() {
        return _detector.getName();
    }

    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted averate of
     * observed spectrum.  So this may be temporary.
     *
     * @return Effective wavelength in nm
     */
    public int getEffectiveWavelength() {
        Log.fine("ghost getEffectiveWavelength: "+ _gratingOptics.getEffectiveWavelength());
        return (int) _gratingOptics.getEffectiveWavelength();

    }

    public double getGratingDispersion() {
        Log.fine("dispersion: " + _gratingOptics.dispersion(-1));
        return _gratingOptics.dispersion(-1);
    }

    /**
     * Returns the subdirectory where this instrument's data files are.
     */
    public String getDirectory() {
        return ITCConstants.LIB + "/" + INSTR_DIR;
    }

    @Override
    public double wellDepth() {
        Log.info("TODO. VENU has to give this information. Not implemented yet Ghost.java wellDepth ");
        return WellDepth;
    }


    @Override
    public double gain() {
        Log.info("TODO. Venu has to confirm the gain for each read Mode");
        switch (gp.readMode()) {
            case SLOW_LOW:
                return (_ccdColor == Detector.BLUE) ? 0.75 : 0.7;
            case MEDIUM_LOW:
                return (_ccdColor == Detector.BLUE) ? 0.63 : 0.57;
            case FAST_LOW:
                return (_ccdColor == Detector.BLUE) ? 0.58 : 0.53;
            default:
                Log.warning("Bad definition of the readMode");
                return 0;
        }
    }

    public double getPixelSize() {
        // The E2V_PIXEL_SIZE should be in arcsecs/pixel.
        return Detector.PIXEL_SIZE * gp.binning().getSpectralBinning();  // Both detectors has the same pixel size.
    }

    public double getSpectralPixelWidth() {
        Log.fine("getSpectralPixelWidth, getSampling: " + getSampling());
        return _gratingOptics.getPixelWidth();
    }

    public double getSampling() {
        return _sampling;
    }

    public int getSpectralBinning() {
        return gp.binning().getSpectralBinning();
    }

    public int getSpatialBinning() {
        return gp.binning().getSpatialBinning();
    }

    public double getADSaturation() {
        return AD_SATURATION;
    }

    public Option<IfuMethod> getIfuMethod() {
        return (odp.analysisMethod() instanceof IfuMethod) ? Option.apply((IfuMethod) odp.analysisMethod()): Option.empty();
    }

    public double getCentralWavelength() {
        return gp.centralWavelength().toNanometers();
    }


    protected String getPrefix() {
        return INSTR_PREFIX;
    }
    protected String[] getCcdNames() {
        return DETECTOR_CCD_NAMES;
    }

    private void validate() {
        Log.warning("Not implemented because Ghost form doesn't allow a bad configuration");
    }

    @Override public List<WarningRule> warnings() {
        return new ArrayList<WarningRule>() {{
            add(_ghostSaturLimitWarning);
        }};
    }


    public GhostSaturLimitRule getGhostSaturLimitWarning() {
        return _ghostSaturLimitWarning;
    }

    @Override
    public double getSlitWidth() {
        switch (gp.resolution()) {
            case GhostStandard:
                return  0.32;
            case GhostPRV:
            case GhostHigh:
                return 0.19;
            default:
                Log.warning("Incorrect option defined in the GhostParameter resolution, please check this issue. It is used the DEFAULT value (0.32)");
                return 0.32;
        }
    }

    private static final double SIZE_ONE_FIBER_SR_PIXELS = 2.7;  // Size of one fiber in pixels.
    private static final double SIZE_ONE_FIBER_HR_PIXELS = 1.62;  // Size of one fiber in pixels.

    public double getSlitLength() {
        double slitLength=0.0;

        switch (gp.resolution()) {
            case GhostStandard:
                slitLength = SIZE_ONE_FIBER_SR_PIXELS * 7;  // SIZE_ONE_FIBER_SR_PIXELS = 2.7
                break;
            case GhostPRV:
            case GhostHigh:
                slitLength = SIZE_ONE_FIBER_HR_PIXELS * 19;  // SIZE_ONE_FIBER_HR_PIXELS = 1.62
                break;
            default:
                Log.warning("Incorrect option defined in the GhostParameter resolution, please check this issue. It is used the DEFAULT value ("+slitLength+")");
        }
        Log.info("slitLength: " + slitLength );
        return slitLength;
    }


    public double getDarkCurrent() {
        return _ccdColor.getDarkCurrent().doubleValue();
    }

    /*

        READ MODES- Standard science mode (slow read, low gain) Default: Standard science mode
	    Fast read (fast read, low gain)
	    Bright targets (fast read, fast gain)
     */
    public double getReadNoise() {
        switch (_ccdColor) {
            case RED: {
                switch (gp.readMode()) {
                    case SLOW_LOW:
                        return 4.5;  // electrons
                    case MEDIUM_LOW:
                        return 4.5;
                    case FAST_LOW:
                        return 2.3;
                    default:
                        Log.warning("Bad option provided by GhostParamenter read Mode, return 0 for read noise in the Detector Red");
                        return 0;
                }
            }
            case BLUE:
                switch (gp.readMode()) {
                    case SLOW_LOW:
                        return 4.5;  // electrons
                    case MEDIUM_LOW:
                        return 4.5;
                    case FAST_LOW:
                        return 2.3;
                    default:
                        Log.warning("Bad option provided by GhostParamenter read Mode, return 0 for read noise in the Detector Blue");
                        return 0;
                }
            default:
                Log.warning("Bad CCD field built. Please check the class definition. ");
                return 0;
        }
    }

    public void applySkyCoeff(VisitableSampledSpectrum sky) {
        double skyCoeff = 1 + 7.0/ gp.nSkyMicrolens();
        if (gp.resolution() == ResolutionMode.GhostHigh)
            skyCoeff = 3.714;  // i.e 1 + 19/7
        for (int i = 0; i < sky.getLength(); i++) {
            sky.setY(i, sky.getY(i) * skyCoeff);
        }
    }

    public IFUComponent getIFU() {
        return _ifu;
    }

    public void transPerResolutionElement(VisitableSampledSpectrum finalS2NSpectrum) {
        _ghostResolution.visit(finalS2NSpectrum);
    }

    public String getCCDType() {
        return _ccdColor.displayValue();
    }

    public double maxFlux() {
        double wellSatLimit = wellDepth() * getSpatialBinning() * getSpectralBinning();
        double adcSatLimit = AD_SATURATION * gain();
        return Math.min(wellSatLimit, adcSatLimit);
    }
}
