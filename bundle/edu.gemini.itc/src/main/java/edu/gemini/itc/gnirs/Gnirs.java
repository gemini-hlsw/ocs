package edu.gemini.itc.gnirs;

import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.shared.CalculationMethod;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.Disperser;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.SlitWidth;


/**
 * Gnirs specification class
 */
public final class Gnirs extends Instrument {

    private static final double LONG_CAMERA_SCALE_FACTOR = 3.0;

    private static final double XDISP_CENTRAL_WAVELENGTH = 1616.85;

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

    // Keep a reference to the color filter to ask for effective wavelength
    private final GnirsParameters params;
    protected Filter _Filter;
    protected GnirsGratingOptics _gratingOptics;
    protected Detector _detector;
    protected double _sampling;
    protected String _filterUsed;
    protected Disperser _grating;
    protected CalculationMethod _mode;
    protected double _centralWavelength;

    protected final DetectorsTransmissionVisitor _dtv;
    protected final TransmissionElement _camera;
    protected final boolean _XDisp;
    protected final double _wellDepth;

    public Gnirs(GnirsParameters gp, ObservationDetails odp) {
        super(INSTR_DIR, FILENAME);
        ///
        // The instrument data file gives a start/end wavelength for
        // the instrument.  But with a filter in place, the filter
        // transmits wavelengths that are a subset of the original range.

        _sampling = super.getSampling();

        params = gp;
        _grating = gp.getGrating();
        _centralWavelength = correctedCentralWavelength(); // correct central wavelength if cross dispersion is used
        _mode = odp.getMethod();
        _XDisp = isXDispUsed();

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
            selectableTrans = new XDispersingPrism(getDirectory(), isLongCamera() ? "LXD" : "SXD");
        } else {
            selectableTrans = new GnirsPickoffMirror(getDirectory(), "mirror");
        }
        addComponent(selectableTrans);

        final FixedOptics _fixedOptics = new FixedOptics(getDirectory() + "/", getPrefix());
        addComponent(_fixedOptics);

        _camera = CameraFactory.camera(params.getPixelScale(), _centralWavelength, getDirectory());
        addComponent(_camera);

        // GNIRS is spectroscopy only
        if (_mode.isImaging()) {
            throw new RuntimeException("GNIRS does not support imaging.");
        }


        _detector = new Detector(getDirectory() + "/", getPrefix(), "aladdin", "1K x 1K ALADDIN III InSb CCD");
        _detector.setDetectorPixels(DETECTOR_PIXELS);

        _dtv = new DetectorsTransmissionVisitor(1, getDirectory() + "/" + getPrefix() + "ccdpix" + Instrument.getSuffix());

        _gratingOptics = new GnirsGratingOptics(getDirectory() + "/" + getPrefix(), _grating,
                _centralWavelength,
                _detector.getDetectorPixels(),
                1);

        if (_grating.equals(Disperser.D_10) && !isLongCamera())
            throw new RuntimeException("The grating " + _grating + " cannot be used with the " +
                    "0.15\" arcsec/pix (Short) camera.\n" +
                    "  Please either change the camera or the grating.");

        if (!(_filterUsed.equals("none")))
            if ((_Filter.getStart() >= _gratingOptics.getEnd()) ||
                    (_Filter.getEnd() <= _gratingOptics.getStart())) {
                throw new RuntimeException("The " + _filterUsed + " filter" +
                        " and the " + _grating +
                        " do not overlap with the requested wavelength.\n" +
                        " Please select a different filter, grating or wavelength.");
            }


        addComponent(_detector);


    }

    public double getFPMask() {
        return params.getSlitWidth().getValue();
    }

    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted averate of
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
        return params.getPixelScale().getValue();
    }

    public double getSpectralPixelWidth() {
        if (isLongCamera()) {
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
        if (isLongCamera()) {
            return _gratingOptics.getGratingResolution() * LONG_CAMERA_SCALE_FACTOR;
        } else {
            return _gratingOptics.getGratingResolution();
        }
    }

    public Disperser getGrating() {
        return _grating;
    }

    public double getGratingBlaze() {
        return _gratingOptics.getGratingBlaze();
    }

    public double getGratingDispersion_nm() {
        try {
            if (!XDisp_IsUsed()) {
                if (isLongCamera()) {
                    return _gratingOptics.getGratingDispersion_nm() / LONG_CAMERA_SCALE_FACTOR / GnirsOrderSelector.getOrder(_centralWavelength);
                } else {
                    return _gratingOptics.getGratingDispersion_nm() / GnirsOrderSelector.getOrder(_centralWavelength);
                }
            } else {
                if (isLongCamera()) {
                    return _gratingOptics.getGratingDispersion_nm() / LONG_CAMERA_SCALE_FACTOR;
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
                if (isLongCamera()) {
                    return _gratingOptics.getGratingDispersion_nmppix() / LONG_CAMERA_SCALE_FACTOR / GnirsOrderSelector.getOrder(_centralWavelength);
                } else {
                    return _gratingOptics.getGratingDispersion_nmppix() / GnirsOrderSelector.getOrder(_centralWavelength);
                }
            } else {
                if (isLongCamera()) {
                    return _gratingOptics.getGratingDispersion_nmppix() / LONG_CAMERA_SCALE_FACTOR;
                } else {
                    return _gratingOptics.getGratingDispersion_nmppix();
                }
            }

        } catch (Exception e) {
            return _gratingOptics.getGratingDispersion_nmppix();
        }
    }

    public double getReadNoise() {
        return params.getReadMode().getReadNoise();
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

    public SlitWidth getFocalPlaneMask() {
        return params.getSlitWidth();
    }

    public double getCentralWavelength() {
        return _centralWavelength;
    }

    public TransmissionElement getGratingOrderNTransmission(int order) {
        return GnirsGratingsTransmission.getOrderNTransmission(_grating, order);
    }

    private double correctedCentralWavelength() {
        if (!isXDispUsed()) {
            return params.getCentralWavelength().toNanometers();
        } else {
            return XDISP_CENTRAL_WAVELENGTH;
        }
    }

    private boolean isLongCamera() {
        return params.getPixelScale().equals(GNIRSParams.PixelScale.PS_005);
    }

    private boolean isXDispUsed() {
        return !params.getCrossDispersed().equals(GNIRSParams.CrossDispersed.NO);
    }


}
