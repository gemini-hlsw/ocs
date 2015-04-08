package edu.gemini.itc.gnirs;

import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.shared.CalculationMethod;
import edu.gemini.itc.shared.*;


/**
 * Gnirs specification class
 */
public final class Gnirs extends Instrument {
    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_DIR = "gnirs";

    public static final String INSTR_PREFIX = "gnirs_";

    private static final String FILENAME = "gnirs" + getSuffix();

    // Instrument reads its configuration from here.
    private static final double SHALLOW_WELL = 90000.0;
    private static final double DEEP_WELL = 180000.0;

    private static final double AD_SATURATION = 56636;

    private static final double HIGH_GAIN = 4.4;
    private static final double LOW_GAIN = 2.18;
    public static final int DETECTOR_PIXELS = 1024;

    public static final double SHORT_CAMERA_PIXEL_SCALE = 0.15;
    public static final double LONG_CAMERA_PIXEL_SCALE = 0.05;

    private static final double HIGH_BACK_READ_NOISE = 155;    // Old value: 160 (changed 2/27/2014)
    private static final double MEDIUM_BACK_READ_NOISE = 30;   // Old value: 35 (changed 2/27/2014)
    private static final double LOW_BACK_READ_NOISE = 10;      // Old value: 11 (changed 2/27/2014)
    private static final double VERY_LOW_BACK_READ_NOISE = 7;  // Old value: 9 (changed 2/27/2014)


    // Keep a reference to the color filter to ask for effective wavelength
    protected Filter _Filter;
    protected GnirsGratingOptics _gratingOptics;
    protected Detector _detector;
    protected double _sampling;
    protected String _filterUsed;
    protected String _grating;
    protected String _readNoise;
    protected String _focalPlaneMask;
    protected String _stringSlitWidth;
    protected CalculationMethod _mode;
    protected double _centralWavelength;

    protected final DetectorsTransmissionVisitor _dtv;
    protected final CameraOptics _camera;
    protected final boolean _XDisp;
    protected final String _cameraLength;
    protected final double _wellDepth;
    protected final double _readNoiseValue;


    public Gnirs(GnirsParameters gp, ObservationDetails odp) {
        super(INSTR_DIR, FILENAME);
        _sampling = super.getSampling();

        ///
        // The instrument data file gives a start/end wavelength for
        // the instrument.  But with a filter in place, the filter
        // transmits wavelengths that are a subset of the original range.

        _sampling = super.getSampling();

        _readNoise = gp.getReadNoise();
        _focalPlaneMask = gp.getFocalPlaneMask();
        _stringSlitWidth = gp.getStringSlitWidth();
        _grating = gp.getGrating();
        _centralWavelength = gp.getInstrumentCentralWavelength();
        _mode = odp.getMethod();
        _XDisp = gp.isXDispUsed();

        if (_centralWavelength < 1030 || _centralWavelength > 6000) {
            throw new RuntimeException("Central wavelength must be between 1.03um and 6.0um.");
        }

        //set read noise by exporsure time
        if (odp.getExposureTime() <= 1.0) {
            _wellDepth = DEEP_WELL;
        } else if (odp.getExposureTime() <= 20.0) {
            _wellDepth = SHALLOW_WELL;
        } else if (odp.getExposureTime() <= 60.0) {
            _wellDepth = SHALLOW_WELL;
        } else {
            _wellDepth = SHALLOW_WELL;
        }
        if (_readNoise.equals(GnirsParameters.VERY_LOW_READ_NOISE)) {
            _readNoiseValue = VERY_LOW_BACK_READ_NOISE;  // Added 2/24/2014 by SLP
        } else if (_readNoise.equals(GnirsParameters.LOW_READ_NOISE)) {
            _readNoiseValue = LOW_BACK_READ_NOISE;  // Added 2/24/2014 by SLP
        } else if (_readNoise.equals(GnirsParameters.MED_READ_NOISE)) {
            _readNoiseValue = MEDIUM_BACK_READ_NOISE;  // Added 2/24/2014 by SLP
        } else {
            _readNoiseValue = HIGH_BACK_READ_NOISE;  // Added 2/24/2014 by SLP
        }

        //Select filter depending on if Cross dispersion is used.
        if (_XDisp) {
            _filterUsed = "XD";
            _Filter = Filter.fromFile(getPrefix(), _filterUsed, getDirectory() + "/");
        } else {
            //Use GnirsOrderSelecter to decide which filter to put in
            _filterUsed = "order";
            _Filter = Filter.fromFile(getPrefix(), _filterUsed + GnirsOrderSelector.getOrder(_centralWavelength), getDirectory() + "/");
        }
        addComponent(_Filter);

        //Select Transmission Element depending on if Cross dispersion is used.
        final TransmissionElement selectableTrans;
        if (_XDisp) {
            selectableTrans = new XDispersingPrism(getDirectory(), gp.getCameraLength() + "XD");
        } else {
            selectableTrans = new GnirsPickoffMirror(getDirectory(), "mirror");
        }
        addComponent(selectableTrans);

        final FixedOptics _fixedOptics = new FixedOptics(getDirectory() + "/", getPrefix());
        addComponent(_fixedOptics);

        final CameraFactory cf = new CameraFactory(gp.getCameraLength(), gp.getCameraColor(), getDirectory());
        _camera = cf.getCamera();
        addComponent((edu.gemini.itc.shared.TransmissionElement) _camera);

        _cameraLength = gp.getCameraLength();

        //Test to see that all conditions for Spectroscopy are met
        if (_mode.isSpectroscopy()) {
            if (_grating.equals("none"))
                throw new RuntimeException("Spectroscopy calculation method is selected but a grating" +
                        " is not.\nPlease select a grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
            if (_focalPlaneMask.equals(GnirsParameters.NO_SLIT))
                throw new RuntimeException("Spectroscopy calculation method is selected but a focal" +
                        " plane mask is not.\nPlease select a " +
                        "grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
        }


        _detector = new Detector(getDirectory() + "/", getPrefix(),
                "aladdin", "1K x 1K ALADDIN III InSb CCD");
        _detector.setDetectorPixels(DETECTOR_PIXELS);

        _dtv = new DetectorsTransmissionVisitor(1,
                getDirectory() + "/" + getPrefix() + "ccdpix" + Instrument.getSuffix());

        if (!(_grating.equals("none"))) {

            _gratingOptics = new GnirsGratingOptics(getDirectory() + "/" + getPrefix(), _grating,
                    _centralWavelength,
                    _detector.getDetectorPixels(),
                    1);

            if (_grating.equals(GnirsParameters.G10) && _cameraLength.equals(GnirsParameters.SHORT))
                throw new RuntimeException("The grating " + _grating + " cannot be used with the " +
                        GnirsParameters.SHORT_CAMERA + " arcsec/pix (Short) camera.\n" +
                        "  Please either change the camera or the grating.");

            if (!(_grating.equals("none")) && !(_filterUsed.equals("none")))
                if ((_Filter.getStart() >= _gratingOptics.getEnd()) ||
                        (_Filter.getEnd() <= _gratingOptics.getStart())) {
                    throw new RuntimeException("The " + _filterUsed + " filter" +
                            " and the " + _grating +
                            " do not overlap with the requested wavelength.\n" +
                            " Please select a different filter, grating or wavelength.");
                }
        }


        addComponent(_detector);


    }

    public double getFPMask() {
        //if (_FP_Mask.equals(NOSLIT)) return null;
        if (_focalPlaneMask.equals(GnirsParameters.SLIT0_1))
            return 0.1;
        else if (_focalPlaneMask.equals(GnirsParameters.SLIT0_15))
            return 0.15;
        else if (_focalPlaneMask.equals(GnirsParameters.SLIT0_2))
            return 0.2;
        else if (_focalPlaneMask.equals(GnirsParameters.SLIT0_3))
            return 0.3;
        else if (_focalPlaneMask.equals(GnirsParameters.SLIT0_45))
            return 0.45;
        else if (_focalPlaneMask.equals(GnirsParameters.SLIT0_675))
            return 0.675;
        else if (_focalPlaneMask.equals(GnirsParameters.SLIT1_0))
            return 1.0;
        else if (_focalPlaneMask.equals(GnirsParameters.SLIT3_0))
            return 3.0;
        else
            return -1.0;
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
        return _camera.getPixelScale();
    }

    public double getSpectralPixelWidth() {
        if (_cameraLength.equals(GnirsParameters.LONG)) {
            return _gratingOptics.getPixelWidth() / 3.0;
        } else {
            return _gratingOptics.getPixelWidth();
        }
    }

    public double getWellDepth() {
        return _wellDepth;
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

    /**
     * The prefix on data file names for this instrument.
     */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }

    public double getGratingResolution() {
        if (_cameraLength.equals(GnirsParameters.LONG)) {
            return _gratingOptics.getGratingResolution() * GnirsParameters.LONG_CAMERA_SCALE_FACTOR;
        } else {
            return _gratingOptics.getGratingResolution();
        }
    }

    public String getGrating() {
        return _grating;
    }

    public double getGratingBlaze() {
        return _gratingOptics.getGratingBlaze();
    }

    public double getGratingDispersion_nm() {
        try {
            if (!XDisp_IsUsed()) {
                if (_cameraLength.equals(GnirsParameters.LONG)) {
                    return _gratingOptics.getGratingDispersion_nm() / GnirsParameters.LONG_CAMERA_SCALE_FACTOR / GnirsOrderSelector.getOrder(_centralWavelength);
                } else {
                    return _gratingOptics.getGratingDispersion_nm() / GnirsOrderSelector.getOrder(_centralWavelength);
                }
            } else {
                if (_cameraLength.equals(GnirsParameters.LONG)) {
                    return _gratingOptics.getGratingDispersion_nm() / GnirsParameters.LONG_CAMERA_SCALE_FACTOR;
                } else {
                    return _gratingOptics.getGratingDispersion_nm();
                }
            }
        } catch (Exception e) {
            return _gratingOptics.getGratingDispersion_nm();
        }
    }

    public double getGratingDispersion_nmppix() {
        try {
            if (!XDisp_IsUsed()) {
                if (_cameraLength.equals(GnirsParameters.LONG)) {
                    return _gratingOptics.getGratingDispersion_nmppix() / GnirsParameters.LONG_CAMERA_SCALE_FACTOR / GnirsOrderSelector.getOrder(_centralWavelength);
                } else {
                    return _gratingOptics.getGratingDispersion_nmppix() / GnirsOrderSelector.getOrder(_centralWavelength);
                }
            } else {
                if (_cameraLength.equals(GnirsParameters.LONG)) {
                    return _gratingOptics.getGratingDispersion_nmppix() / GnirsParameters.LONG_CAMERA_SCALE_FACTOR;
                } else {
                    return _gratingOptics.getGratingDispersion_nmppix();
                }
            }

        } catch (Exception e) {
            return _gratingOptics.getGratingDispersion_nmppix();
        }
    }

    public double getReadNoise() {
        return _readNoiseValue;
    }

    public double getObservingStart() {
        return _centralWavelength - (getGratingDispersion_nmppix() * _detector.getDetectorPixels() / 2);
    }

    public double getObservingEnd() {
        return _centralWavelength + (getGratingDispersion_nmppix() * _detector.getDetectorPixels() / 2);
    }

    public DetectorsTransmissionVisitor getDetectorTransmision() {
        return _dtv;
    }

    public boolean XDisp_IsUsed() {
        return _XDisp;
    }

    public int getOrder() {
        try {
            return GnirsOrderSelector.getOrder(_centralWavelength);
        } catch (Exception e) {
            System.out.println("Cannot find Order setting to 1.");
            return 1;
        }
    }

    public String getFocalPlaneMask() {
        return _focalPlaneMask;
    }

    public double getCentralWavelength() {
        return _centralWavelength;
    }

    public TransmissionElement getGratingOrderNTransmission(int order) {
        return GnirsGratingsTransmission.getOrderNTransmission(_grating, order);
    }

}
