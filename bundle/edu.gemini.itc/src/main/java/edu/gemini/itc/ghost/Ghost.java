package edu.gemini.itc.ghost;

import edu.gemini.itc.base.*;
import edu.gemini.itc.shared.GhostCameraParameters;
import edu.gemini.itc.shared.GhostParameters;
import edu.gemini.itc.shared.IfuMethod;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.ghost.Detector;
import edu.gemini.spModel.gemini.ghost.GhostReadNoiseGain;
import static edu.gemini.spModel.gemini.ghost.GhostReadNoiseGain.*;
import scala.Option;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Objects;

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
    private final GhostParameters gp;
    private final GhostCameraParameters camera;
    private final ObservationDetails odp;
    private final GhostGratingOptics _gratingOptics;
    private final edu.gemini.itc.base.Detector _detector;
    private final double _sampling;
    private static final String FILENAME = "ghost" + getSuffix();
    private final Detector _ccdColor;

    private static final double SIZE_ONE_FIBER_SR_PIXELS = 2.7;  // Size of one fiber in pixels.
    private static final double SIZE_ONE_FIBER_HR_PIXELS = 1.62;  // Size of one fiber in pixels.

    public Ghost(final GhostParameters gp, final ObservationDetails odp, final Detector ccdColor) {
        super(Site.GS, Bands.VISIBLE, INSTR_DIR, FILENAME);

        Log.fine("Resolution: " + gp.resolution());
        Log.fine("ccd_color: " + ccdColor.displayValue());
        this.odp    = odp;
        this.gp     = gp;

        _ccdColor = ccdColor;
         camera = getCamera();
        _detector = new edu.gemini.itc.base.Detector(getDirectory() + "/",
                                                     getPrefix(),
                                                     _ccdColor.getModel(),
                                                     _ccdColor.getModel(),
                                                     _ccdColor.name());

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
        _fixedOptics.setDescription("Fixed Optics Throughtputs defined in "+Ghost.INSTR_PREFIX + "fixedOptics" + getSuffix()+" file.");
        addComponent(_fixedOptics);

        _gratingOptics = new GhostGratingOptics(
                getDirectory() + "/" + Ghost.INSTR_PREFIX,
                resolutionFileEnding(gp),
                "gratings",                                                // ghost_gratings.dat
                gp.centralWavelength().toNanometers(),
                _detector.getDetectorPixels(),
                camera.binning().getSpectralBinning());
        _gratingOptics.setDescription(gp.resolution().displayValue());
        addDisperser(_gratingOptics);
        _sampling = super.getSampling();
        _ifu = new IFUComponent(gp.resolution());
        addComponent(_ifu);

        _ghostResolution = new TransmissionElement(getDirectory()+"/" + Ghost.INSTR_PREFIX + resolutionFileEnding(gp)+ "_perResolution" +getSuffix());
    }

    public GhostCameraParameters getCamera() {
        switch (_ccdColor) {
                case RED: return gp.redCamera();
                case BLUE: return gp.blueCamera();
                default: throw new RuntimeException("Invalid CCD field color");
        }
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

    public double getPixelSize() {
        // The E2V_PIXEL_SIZE should be in arcsecs/pixel.
        return Detector.PIXEL_SIZE * camera.binning().getSpatialBinning();  // Both detectors have the same pixel size.
    }

    public double getSpectralPixelWidth() {
        Log.fine("getSpectralPixelWidth, getSampling: " + getSampling());
        return _gratingOptics.getPixelWidth();
    }

    public double getSampling() {
        return _sampling;
    }

    public int getSpectralBinning() {
        return camera.binning().getSpectralBinning();
    }

    public int getSpatialBinning() {
        return camera.binning().getSpatialBinning();
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

     @Override
    public double getSlitWidth() {
        switch (gp.resolution()) {
            case GhostStandard:
                return  0.32;  // arcsecs
            case GhostPRV:
            case GhostHigh:
                return 0.19;
            default:
                Log.warning("Incorrect option defined in the GhostParameter resolution, please check this issue. It is used the DEFAULT value (0.32)");
                return 0.32;
        }
    }

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

    private static final class ReadModePair {
        final GhostReadNoiseGain blue, red;
        ReadModePair(GhostReadNoiseGain blue, GhostReadNoiseGain red) {
            this.blue = blue;
            this.red  = red;
        }

        @Override public boolean equals(Object o) {
            if (!(o instanceof ReadModePair)) return false;
            ReadModePair other = (ReadModePair) o;
            return blue == other.blue && red == other.red;
        }
        @Override public int hashCode() { return Objects.hash(blue, red); }
    }

    private static final Map<ReadModePair, double[]> READMODE_TABLE = new HashMap<>();
    // double[] = { blueGain, redGain, blueReadNoise, redReadNoise }               ___Gain___     _Read_Noise_
    static {  //                                                                   Blue   Red     Blue    Red
        READMODE_TABLE.put(new ReadModePair(SLOW_LOW,   SLOW_LOW),   new double[]{ 0.40,  0.30,   1.48,   1.30 });
        READMODE_TABLE.put(new ReadModePair(SLOW_LOW,   MEDIUM_LOW), new double[]{ 0.56,  0.52,   2.23,   2.31 });
        READMODE_TABLE.put(new ReadModePair(MEDIUM_LOW, SLOW_LOW),   new double[]{ 0.60,  0.49,   2.56,   1.96 });
        READMODE_TABLE.put(new ReadModePair(MEDIUM_LOW, MEDIUM_LOW), new double[]{ 0.60,  0.52,   2.55,   2.26 });
        READMODE_TABLE.put(new ReadModePair(MEDIUM_LOW, FAST_LOW),   new double[]{ 0.59,  0.69,   2.52,   4.70 });
        READMODE_TABLE.put(new ReadModePair(FAST_LOW,   FAST_LOW),   new double[]{ 0.75,  0.68,   4.79,   4.66 });
    }

    private double[] lookupDetectorValues() {
        GhostReadNoiseGain blueMode = gp.blueCamera().readMode();
        GhostReadNoiseGain redMode  = gp.redCamera().readMode();
        double[] values = READMODE_TABLE.get(new ReadModePair(blueMode, redMode));
        if (values == null) {
            throw new IllegalArgumentException(
                String.format("Unsupported combination of '%s' + '%s'", blueMode, redMode));
        }
        return values;
    }

    @Override
    public double gain() {
        double[] v = lookupDetectorValues();
        return (_ccdColor == Detector.BLUE) ? v[0] : v[1];
    }

    public double getReadNoise() {
        double[] v = lookupDetectorValues();
        return (_ccdColor == Detector.BLUE) ? v[2] : v[3];
    }

    public IFUComponent getIFU() {
        return _ifu;
    }

    public void transPerResolutionElement(VisitableSampledSpectrum finalS2NSpectrum) {
        _ghostResolution.visit(finalS2NSpectrum);
    }

    public int getSaturationLimit() {
        return _ccdColor.getSaturationLimit();
    }

    public String getDetectorName() {
        return _ccdColor.displayValue();
    }

    public double maxFlux() {
        return _ccdColor.getSaturationLimit() * gain();
    }

    @Override public double getMinExposureTime() { throw new Error("NOT IMPLEMENTED"); }
}
