package edu.gemini.itc.michelle;

import edu.gemini.itc.base.*;
import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.shared.CalculationMethod;
import edu.gemini.itc.shared.MichelleParameters;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.spModel.data.YesNoType;
import edu.gemini.spModel.gemini.michelle.MichelleParams;

/**
 * Michelle specification class
 */
public final class Michelle extends Instrument {

    public static final String WIRE_GRID = "wire_grid";
    public static final String KBR = "KBr";

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

    private static final double IMAGING_FRAME_TIME = .020;  //Seconds

    private static final double SPECTROSCOPY_PIXEL_SIZE = 0.2;

    private static final int DETECTOR_PIXELS = 320;

    private DetectorsTransmissionVisitor _dtv;

    // Keep a reference to the color filter to ask for effective wavelength
    private Filter _Filter;
    private MichelleGratingOptics _gratingOptics;
    private final double _sampling;
    private final CalculationMethod _mode;
    private final double _centralWavelength;

    private final MichelleParameters params;

    public Michelle(final MichelleParameters mp, final ObservationDetails odp) {
        super(INSTR_DIR, FILENAME);

        this.params = mp;
        _sampling = super.getSampling();
        _centralWavelength = mp.centralWavelength().toNanometers();

        _mode = odp.getMethod();

        final InstrumentWindow michelleInstrumentWindow =
                new InstrumentWindow(getDirectory() + "/" + getPrefix() +
                        KBR + Instrument.getSuffix(), KBR);
        addComponent(michelleInstrumentWindow);

        if (mp.polarimetry().equals(YesNoType.YES)) {
            final WireGrid michelleWireGrid =
                    new WireGrid(getDirectory() + "/" + getPrefix() +
                            WIRE_GRID + Instrument.getSuffix());
            addComponent(michelleWireGrid);
        }

        if (!(mp.filter().equals(MichelleParams.Filter.NONE))) {
            _Filter = Filter.fromWLFile(getPrefix(), mp.filter().name(), getDirectory() + "/");
            addFilter(_Filter);
        }


        final FixedOptics fixedOptics = new FixedOptics(getDirectory() + "/", getPrefix());
        addComponent(fixedOptics);


        //Test to see that all conditions for Spectroscopy are met
        if (_mode.isSpectroscopy()) {
            if (params.grating().equals(MichelleParams.Disperser.MIRROR))
                throw new RuntimeException("Spectroscopy mode is selected but a grating" +
                        " is not.\nPlease select a grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
            if (params.mask() == MichelleParams.Mask.MASK_IMAGING)
                throw new RuntimeException("Spectroscopy mode is selected but a focal" +
                        " plane mask is not.\nPlease select a " +
                        "grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
            if (params.polarimetry().equals(YesNoType.YES)) {
                throw new RuntimeException("Spectroscopy mode cannot be used with the " +
                        "Polarimeter in.\n Please either deselect the " +
                        "Polarimeter, or change the mode to Imaging.");
            }
        }

        if (_mode.isImaging()) {
            if (params.filter().equals(MichelleParams.Filter.NONE))
                throw new RuntimeException("Imaging mode is selected but a filter" +
                        " is not.\n  Please select a filter and resubmit the " +
                        "form to continue.");
            if (!params.grating().equals(MichelleParams.Disperser.MIRROR))
                throw new RuntimeException("Imaging mode is selected but a grating" +
                        " is also selected.\nPlease deselect the " +
                        "grating or change the mode to spectroscopy.");
            if (params.mask() != MichelleParams.Mask.MASK_IMAGING)
                throw new RuntimeException("Imaging mode is selected but a Focal" +
                        " Plane Mask is also selected.\nPlease " +
                        "deselect the Focal Plane Mask" +
                        " or change the mode to spectroscopy.");
        }


        final Detector detector = new Detector(getDirectory() + "/", getPrefix(), "det", "320x240 pixel Si:As IBC array");
        detector.setDetectorPixels(DETECTOR_PIXELS);

        _dtv = new DetectorsTransmissionVisitor(1, getDirectory() + "/" + getPrefix() + "ccdpix" + Instrument.getSuffix());

        if (!(params.grating().equals(MichelleParams.Disperser.MIRROR))) {
            _gratingOptics = new MichelleGratingOptics(getDirectory() + "/" + getPrefix(), params.grating(),
                    _centralWavelength,
                    detector.getDetectorPixels());
            addGrating(_gratingOptics);
        }


        addComponent(detector);


    }

    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted averate of
     * observed spectrum.  So this may be temporary.
     *
     * @return Effective wavelength in nm
     */
    public int getEffectiveWavelength() {
        if (params.grating().equals(MichelleParams.Disperser.MIRROR))
            return (int) _Filter.getEffectiveWavelength();
        else
            return (int) _gratingOptics.getEffectiveWavelength();
    }

    public double getGratingResolution() {
        return _gratingOptics.getGratingResolution();
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

    public double getFPMask() {
        // Can we use the slit width from the Mask objects here?
        switch (params.mask()) {
            case MASK_1:    return 0.19;
            case MASK_2:    return 0.38;
            case MASK_3:    return 0.57;
            case MASK_4:    return 0.76;
            case MASK_6:    return 1.21;
            case MASK_8:    return 1.52;
            default:        throw new Error();
        }
    }

    public boolean polarimetryIsUsed() {
        return params.polarimetry().equals(YesNoType.YES);
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

    public MichelleParams.Mask getFocalPlaneMask() {
        return params.mask();
    }

    public double getCentralWavelength() {
        return _centralWavelength;
    }
}
