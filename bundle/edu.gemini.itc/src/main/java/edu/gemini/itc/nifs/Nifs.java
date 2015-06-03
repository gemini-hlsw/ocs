package edu.gemini.itc.nifs;

import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.shared.CalculationMethod;
import edu.gemini.itc.shared.ObservationDetails;

/**
 * Nifs specification class
 */
public final class Nifs extends Instrument {
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

    private static final double AD_SATURATION = 56636;

    private static final double HIGH_GAIN = 4.4;
    private static final double LOW_GAIN = 2.18;
    public static final int DETECTOR_PIXELS = 2048;

    private static final double SHALLOW_WELL = 90000.0;
    private static final double DEEP_WELL = 180000.0;

    private static final double HIGH_READ_NOISE_VALUE = 145;  //Not used

    // REL-481: Update NIFS read noise estimates
    private static final double MEDIUM_READ_NOISE_VALUE = 15.4;
    private static final double LOW_READ_NOISE_VALUE = 8.1;
    private static final double VERY_LOW_READ_NOISE_VALUE = 4.6;



    // Keep a reference to the color filter to ask for effective wavelength
    protected Filter _Filter;
    protected NifsGratingOptics _gratingOptics;
    protected Detector _detector;
    protected double _sampling;
    protected String _filterUsed;
    protected String _grating;
    protected String _readNoise;
    protected CalculationMethod _mode;
    protected double _centralWavelength;

    protected String _IFUMethod;
    protected double _IFUOffset;
    protected double _IFUMinOffset;
    protected double _IFUMaxOffset;
    protected int _IFUNumX;
    protected int _IFUNumY;
    protected double _IFUCenterX;
    protected double _IFUCenterY;
    protected IFUComponent _IFU;
    protected boolean _IFU_IsSingle = false;
    protected boolean _IFU_IsSummed = false;

    protected DetectorsTransmissionVisitor _dtv;
    protected double _wellDepth;
    protected double _readNoiseValue;



    public Nifs(final NifsParameters gp, final ObservationDetails odp) {
        super(INSTR_DIR, FILENAME);

        // The instrument data file gives a start/end wavelength for
        // the instrument.  But with a filter in place, the filter
        // transmits wavelengths that are a subset of the original range.

        _sampling = super.getSampling();

        _readNoise = gp.getReadNoise();
        _grating = gp.getGrating();
        _centralWavelength = gp.getInstrumentCentralWavelength();
        _filterUsed = gp.getFilter();
        _mode = odp.getMethod();

        if (_centralWavelength < 1000 || _centralWavelength > 6000) {
            throw new RuntimeException("Central wavelength must be between 1.00um and 6.0um.");
        }

        //Set read noise and Well depth values by obsevation type
        if (_readNoise.equals(NifsParameters.HIGH_READ_NOISE)) {
            _readNoiseValue = HIGH_READ_NOISE_VALUE;
            _wellDepth = DEEP_WELL;
        } else if (_readNoise.equals(NifsParameters.MED_READ_NOISE)) {
            _readNoiseValue = MEDIUM_READ_NOISE_VALUE;
            _wellDepth = SHALLOW_WELL;
        } else if (_readNoise.equals(NifsParameters.LOW_READ_NOISE)) {
            _readNoiseValue = LOW_READ_NOISE_VALUE;
            _wellDepth = SHALLOW_WELL;
        } else if (_readNoise.equals(NifsParameters.VERY_LOW_READ_NOISE)) {
            _readNoiseValue = VERY_LOW_READ_NOISE_VALUE;
            _wellDepth = SHALLOW_WELL;
        }

        if (!(_filterUsed.equals("none"))) {
            _Filter = Filter.fromFile(getPrefix(), _filterUsed, getDirectory() + "/");
            addFilter(_Filter);
        }

        //Might use this for creating a ITC for imaging mode of NIFS
        //_selectableTrans = new NifsPickoffMirror(getDirectory(), "mirror");
        //addComponent(_selectableTrans);

        FixedOptics _fixedOptics = new FixedOptics(getDirectory() + "/", getPrefix());
        addComponent(_fixedOptics);

        //Test to see that all conditions for Spectroscopy are met
        if (_mode.isSpectroscopy()) {
            if (_grating.equals("none"))
                throw new RuntimeException("Spectroscopy calculation method is selected but a grating" +
                        " is not.\nPlease select a grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
        }

        _detector = new Detector(getDirectory() + "/", getPrefix(),
                "hawaii2_HgCdTe", "2K x 2K HgCdTe HAWAII-2 CCD");
        _detector.setDetectorPixels(DETECTOR_PIXELS);

        _dtv = new DetectorsTransmissionVisitor(1,
                getDirectory() + "/" + getPrefix() + "ccdpix" + Instrument.getSuffix());

        _IFUMethod = gp.getIFUMethod();
        if (_IFUMethod.equals(gp.SINGLE_IFU)) {
            _IFU_IsSingle = true;
            _IFUOffset = gp.getIFUOffset();
            _IFU = new IFUComponent(_IFUOffset, getPixelSize());
        }
        if (_IFUMethod.equals(gp.RADIAL_IFU)) {
            _IFUMinOffset = gp.getIFUMinOffset();
            _IFUMaxOffset = gp.getIFUMaxOffset();

            _IFU = new IFUComponent(_IFUMinOffset, _IFUMaxOffset, getPixelSize());
        }
        if (_IFUMethod.equals(gp.SUMMED_APERTURE_IFU)) {
            _IFU_IsSummed = true;
            _IFUNumX = gp.getIFUNumX();
            _IFUNumY = gp.getIFUNumY();
            _IFUCenterX = gp.getIFUCenterX();
            _IFUCenterY = gp.getIFUCenterY();

            _IFU = new IFUComponent(_IFUNumX, _IFUNumY, _IFUCenterX, _IFUCenterY, getPixelSize());
        }
        addComponent(_IFU);



        if (!(_grating.equals("none"))) {
            _gratingOptics = new NifsGratingOptics(getDirectory() + "/" + getPrefix(), _grating,
                    _centralWavelength,
                    _detector.getDetectorPixels(),
                    1);
            _sampling = _gratingOptics.getGratingDispersion_nmppix();
            addGrating(_gratingOptics);
        }


        addComponent(_detector);



    }

    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted averate of
     * observed spectrum.  So this may be temporary.
     *
     * @return Effective wavelength in nm
     */
    public int getEffectiveWavelength() {
        if (_grating.equals("none")) return (int) _Filter.getEffectiveWavelength();
        else return (int) _gratingOptics.getEffectiveWavelength();

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

    public double getADSaturation() {
        return AD_SATURATION;
    }

    public double getHighGain() {
        return HIGH_GAIN;
    }

    public double getLowGain() {
        return LOW_GAIN;
    }

    public IFUComponent getIFU() {
        return _IFU;
    }

    /**
     * The prefix on data file names for this instrument.
     */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }

    public double getGratingResolution() {
        return _gratingOptics.getGratingResolution();
    }

    public String getGrating() {
        return _grating;
    }

    public double getGratingBlaze() {
        return _gratingOptics.getGratingBlaze();
    }

    public double getGratingDispersion_nm() {
        return _gratingOptics.getGratingDispersion_nm();
    }

    public double getGratingDispersion_nmppix() {
        return _gratingOptics.getGratingDispersion_nmppix();
    }

    public DetectorsTransmissionVisitor getDetectorTransmision() {
        return _dtv;
    }

    public double getObservingStart() {
        double start = _centralWavelength - (getGratingDispersion_nmppix() * _detector.getDetectorPixels() / 2);
        return start;
    }

    public double getObservingEnd() {
        double end = _centralWavelength + (getGratingDispersion_nmppix() * _detector.getDetectorPixels() / 2);
        return end;
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

    public String getIFUMethod() {
        return _IFUMethod;
    }


    public double getCentralWavelength() {
        return _centralWavelength;
    }

    public double getWellDepth() {
        return _wellDepth;
    }

    public double getReadNoise() {
        return _readNoiseValue;
    }

}
