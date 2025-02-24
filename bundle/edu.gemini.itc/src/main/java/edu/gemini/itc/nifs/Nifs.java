package edu.gemini.itc.nifs;

import edu.gemini.itc.base.*;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.nifs.NIFSParams;

import java.util.ArrayList;
import java.util.List;

/**
 * Nifs specification class
 */
public final class Nifs extends Instrument implements SpectroscopyInstrument {

    // values are taken from instrument's web documentation
    private static final double WellDepth      = 134400;
    private static final double LinearityLimit =  98000; // electrons

    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_DIR = "nifs";

    /**
     * Related files will start with this prefix
     */
    public static final String INSTR_PREFIX = "nifs_";

    // Instrument reads its configuration from here.
    private static final String FILENAME = "nifs" + getSuffix();

    public static final int DETECTOR_PIXELS = 2048;

    // Keep a reference to the color filter to ask for effective wavelength
    protected Filter _Filter;
    protected NifsGratingOptics _gratingOptics;
    protected Detector _detector;
    protected double _sampling;
    protected CalculationMethod _mode;
    protected double _centralWavelength;

    protected IfuMethod _IFUMethod;
    protected double _IFUOffset;
    protected double _IFUMinOffset;
    protected double _IFUMaxOffset;
    protected int _IFUNumX;
    protected int _IFUNumY;
    protected double _IFUCenterX;
    protected double _IFUCenterY;
    protected IFUComponent _IFU;

    protected double _readNoiseValue;
    protected double _minimumExpousureTime;


    public Nifs(final NifsParameters gp, final ObservationDetails odp) {
        super(Site.GN, Bands.NEAR_IR, INSTR_DIR, FILENAME);

        // The instrument data file gives a start/end wavelength for
        // the instrument.  But with a filter in place, the filter
        // transmits wavelengths that are a subset of the original range.

        _sampling = super.getSampling();

        _centralWavelength = gp.centralWavelength().toNanometers();
        _mode = odp.calculationMethod();

        if (_centralWavelength < 1000 || _centralWavelength > 6000) {
            throw new RuntimeException("Central wavelength must be between 1.00um and 6.0um.");
        }

        if (gp.altair().isDefined()) {
            if (gp.altair().get().guideStarSeparation() < 0 || gp.altair().get().guideStarSeparation() > 25)
                throw new RuntimeException("Altair Guide star distance must be between 0 and 25 arcsecs for NIFS.\n");
        }

        //Set read noise and Well depth values by obsevation type
        _readNoiseValue = gp.readMode().getReadNoise();
        _minimumExpousureTime = gp.readMode().getMinExp();

        // decide which filter we are going to use in case "Same as Disperser" is selected
        final NIFSParams.Filter filter;
        switch (gp.filter()) {
            case SAME_AS_DISPERSER: filter = gp.grating().defaultFilter(); break;
            default:                filter = gp.filter(); break;
        }

        _Filter = Filter.fromFile(getPrefix(), filter.name(), getDirectory() + "/");
        addFilter(_Filter);

        FixedOptics _fixedOptics = new FixedOptics(getDirectory() + "/", getPrefix());
        addComponent(_fixedOptics);

        _detector = new Detector(getDirectory() + "/", getPrefix(), "hawaii2_HgCdTe", "2K x 2K HgCdTe HAWAII-2 CCD");
        _detector.setDetectorPixels(DETECTOR_PIXELS);

        _IFUMethod = (IfuMethod) odp.analysisMethod();
        if (odp.analysisMethod() instanceof IfuSingle) {
            _IFUOffset      = ((IfuSingle) odp.analysisMethod()).offset();
            _IFU            = new IFUComponent(_IFUOffset);
        }
        else if (odp.analysisMethod() instanceof IfuRadial) {
            _IFUMinOffset   = ((IfuRadial) odp.analysisMethod()).minOffset();
            _IFUMaxOffset   = ((IfuRadial) odp.analysisMethod()).maxOffset();
            _IFU            = new IFUComponent(_IFUMinOffset, _IFUMaxOffset);
        }
        else if (odp.analysisMethod() instanceof IfuSummed) {
            _IFUNumX        = ((IfuSummed) odp.analysisMethod()).numX();
            _IFUNumY        = ((IfuSummed) odp.analysisMethod()).numY();
            _IFUCenterX     = ((IfuSummed) odp.analysisMethod()).centerX();
            _IFUCenterY     = ((IfuSummed) odp.analysisMethod()).centerY();
            _IFU            = new IFUComponent(_IFUNumX, _IFUNumY, _IFUCenterX, _IFUCenterY);
        }
        else {
            throw new IllegalArgumentException("Unknown IFU method");
        }
        addComponent(_IFU);


        _gratingOptics = new NifsGratingOptics(getDirectory() + "/" + getPrefix(), gp.grating().name(),
                _centralWavelength,
                _detector.getDetectorPixels(),
                1);
        _sampling = _gratingOptics.dispersion(-1);
        addDisperser(_gratingOptics);

        addComponent(_detector);

    }

    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted average of
     * observed spectrum.  So this may be temporary.
     *
     * @return Effective wavelength in nm
     */
    public int getEffectiveWavelength() {
        return (int) _gratingOptics.getEffectiveWavelength();
    }


    /**
     * Returns the subdirectory where this instrument's data files are.
     */
    public String getDirectory() {
        return ITCConstants.LIB + "/" + INSTR_DIR;
    }

    public double getPixelSize() {
        return super.getPixelSize();
    }

    public double getSpectralPixelWidth() {
        return _gratingOptics.getPixelWidth();
    }

    public double getSampling() {
        return _sampling;
    }

    public IFUComponent getIFU() {
        return _IFU;
    }

    /** {@inheritDoc} */
    public double getSlitWidth() {
        // fp mask is fixed as 0.15
        return 0.15;
    }

    /**
     * The prefix on data file names for this instrument.
     */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }

    public double getGratingDispersion() {
        return _gratingOptics.dispersion(-1);
    }

    public double getObservingStart() {
        return _centralWavelength - (getGratingDispersion() * _detector.getDetectorPixels() / 2);
    }

    public double getObservingEnd() {
        return _centralWavelength + (getGratingDispersion() * _detector.getDetectorPixels() / 2);
    }

    public double getIFUOffset() {
        return _IFUOffset;
    }

    public double getIFUMinOffset() {
        return _IFUMinOffset;
    }

    public double getIFUMaxOffset() {
        return _IFUMaxOffset;
    }

    public int getIFUNumX() {
        return _IFUNumX;
    }

    public int getIFUNumY() {
        return _IFUNumY;
    }

    public IfuMethod getIFUMethod() {
        return _IFUMethod;
    }

    public double getCentralWavelength() {
        return _centralWavelength;
    }

    public double getReadNoise() {
        return _readNoiseValue;
    }

    @Override public double wellDepth() {
        return WellDepth;
    }

    @Override public double gain() {
        return 2.8; // electrons / ADU
    }

    @Override public double getMinExposureTime() { return _minimumExpousureTime; }

    public double maxFlux() {
        return LinearityLimit;
    }

    @Override public List<WarningRule> warnings() {
        return new ArrayList<WarningRule>() {{
            add(new LinearityLimitRule(LinearityLimit, 0.80));
            add(new SaturationLimitRule(WellDepth, 0.80));
        }};
    }


}
