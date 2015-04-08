package edu.gemini.itc.michelle;

import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.itc.shared.CalculationMethod;
import edu.gemini.itc.shared.*;

/**
 * Michelle specification class
 */
public class Michelle extends Instrument {
    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_DIR = "michelle";

    /**
     * Related files will start with this prefix
     */
    public static final String INSTR_PREFIX = "michelle_";

    // Instrument reads its configuration from here.
    private static final String FILENAME = "michelle" + getSuffix();

    private static final double WELL_DEPTH = 30000000.0;

    private static final double AD_SATURATION = 2500000;

    private static final double HIGH_GAIN = 4.4;
    private static final double LOW_GAIN = 2.18;

    private static final double IMAGING_FRAME_TIME = .020;  //Seconds

    private static final double SPECTROSCOPY_PIXEL_SIZE = 0.2;

    private static final int DETECTOR_PIXELS = 320;

    private edu.gemini.itc.operation.DetectorsTransmissionVisitor _dtv;

    // Keep a reference to the color filter to ask for effective wavelength
    private Filter _Filter;
    private MichelleGratingOptics _gratingOptics;
    private Detector _detector;
    private final double _sampling;
    private final String _filterUsed;
    private final String _grating;
    private String _focalPlaneMask;
    private CalculationMethod _mode;
    private double _centralWavelength;
    private final boolean _usesPolarimetry;

    public Michelle(MichelleParameters mp, ObservationDetails odp) {
        super(INSTR_DIR, FILENAME);
        _sampling = super.getSampling();
        _focalPlaneMask = mp.getFocalPlaneMask();
        _grating = mp.getGrating();
        _filterUsed = mp.getFilter();
        _centralWavelength = mp.getInstrumentCentralWavelength();
        _usesPolarimetry = mp.polarimetryIsUsed();

        _mode = odp.getMethod();

        InstrumentWindow michelleInstrumentWindow =
                new InstrumentWindow(getDirectory() + "/" + getPrefix() +
                        mp.KBR + Instrument.getSuffix(), mp.KBR);
        addComponent(michelleInstrumentWindow);

        if (mp.polarimetryIsUsed()) {
            WireGrid michelleWireGrid =
                    new WireGrid(getDirectory() + "/" + getPrefix() +
                            mp.WIRE_GRID + Instrument.getSuffix());
            addComponent(michelleWireGrid);
        }

        if (!(_filterUsed.equals("none"))) {
            _Filter = Filter.fromWLFile(getPrefix(), _filterUsed, getDirectory() + "/");
            addFilter(_Filter);
        }


        FixedOptics _fixedOptics = new FixedOptics(getDirectory() + "/", getPrefix());
        addComponent(_fixedOptics);


        //Test to see that all conditions for Spectroscopy are met
        if (_mode.isSpectroscopy()) {
            if (_grating.equals("none"))
                throw new RuntimeException("Spectroscopy mode is selected but a grating" +
                        " is not.\nPlease select a grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
            if (_focalPlaneMask.equals(MichelleParameters.NO_SLIT))
                throw new RuntimeException("Spectroscopy mode is selected but a focal" +
                        " plane mask is not.\nPlease select a " +
                        "grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
            if (mp.polarimetryIsUsed()) {
                throw new RuntimeException("Spectroscopy mode cannot be used with the " +
                        "Polarimeter in.\n Please either deselect the " +
                        "Polarimeter, or change the mode to Imaging.");
            }
        }

        if (_mode.isImaging()) {
            if (_filterUsed.equals("none"))
                throw new RuntimeException("Imaging mode is selected but a filter" +
                        " is not.\n  Please select a filter and resubmit the " +
                        "form to continue.");
            if (!_grating.equals("none"))
                throw new RuntimeException("Imaging mode is selected but a grating" +
                        " is also selected.\nPlease deselect the " +
                        "grating or change the mode to spectroscopy.");
            if (!_focalPlaneMask.equals("none"))
                throw new RuntimeException("Imaging mode is selected but a Focal" +
                        " Plane Mask is also selected.\nPlease " +
                        "deselect the Focal Plane Mask" +
                        " or change the mode to spectroscopy.");
        }


        _detector = new Detector(getDirectory() + "/", getPrefix(), "det",
                "320x240 pixel Si:As IBC array");
        _detector.setDetectorPixels(DETECTOR_PIXELS);

        _dtv = new DetectorsTransmissionVisitor(1, getDirectory() + "/" + getPrefix() + "ccdpix" + Instrument.getSuffix());

        if (!(_grating.equals("none"))) {
            _gratingOptics = new MichelleGratingOptics(getDirectory() + "/" + getPrefix(), _grating,
                    _centralWavelength,
                    _detector.getDetectorPixels());
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
        if (_grating.equals("none"))
            return (int) _Filter.getEffectiveWavelength();
        else
            return (int) _gratingOptics.getEffectiveWavelength();
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


    /**
     * Returns the subdirectory where this instrument's data files are.
     */
    public String getDirectory() {
        return ITCConstants.LIB + "/" + INSTR_DIR;
    }

    public double getPixelSize() {
        if (_mode.isSpectroscopy()) {
            return SPECTROSCOPY_PIXEL_SIZE;
        } else
            return super.getPixelSize();
    }

    public double getSpectralPixelWidth() {
        return _gratingOptics.getPixelWidth();
    }

    public double getWellDepth() {
        return WELL_DEPTH;
    }

    public double getSampling() {
        return _sampling;
    }

    public double getFrameTime() {
        if (_mode.isSpectroscopy()) {
            return _gratingOptics.getFrameTime();
        } else {
            return IMAGING_FRAME_TIME;
        }
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

    public double getFPMask() {
        //if (_FP_Mask.equals(NOSLIT)) return null;
        if (_focalPlaneMask.equals(MichelleParameters.SLIT0_19))
            return 0.19;
        else if (_focalPlaneMask.equals(MichelleParameters.SLIT0_38))
            return 0.38;
        else if (_focalPlaneMask.equals(MichelleParameters.SLIT0_57))
            return 0.57;
        else if (_focalPlaneMask.equals(MichelleParameters.SLIT0_76))
            return 0.76;
        else if (_focalPlaneMask.equals(MichelleParameters.SLIT1_52))
            return 1.52;
        else
            return -1.0;
    }

    public boolean polarimetryIsUsed() {
        return _usesPolarimetry;
    }


    /**
     * The prefix on data file names for this instrument.
     */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }

    public DetectorsTransmissionVisitor getDetectorTransmision() {
        return _dtv;
    }

    public String getFocalPlaneMask() {
        return _focalPlaneMask;
    }

    public double getCentralWavelength() {
        return _centralWavelength;
    }
}
