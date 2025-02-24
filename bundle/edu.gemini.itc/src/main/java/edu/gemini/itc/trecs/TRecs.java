package edu.gemini.itc.trecs;

import edu.gemini.itc.base.*;
import edu.gemini.itc.shared.*;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.trecs.TReCSParams;
import edu.gemini.spModel.gemini.trecs.TReCSParams.Disperser;
import edu.gemini.spModel.gemini.trecs.TReCSParams.Mask;
import edu.gemini.spModel.gemini.trecs.TReCSParams.WindowWheel;
import scala.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * TRecs specification class
 */
public final class TRecs extends Instrument implements SpectroscopyInstrument {
    private static final String INSTR_DIR = "trecs";
    private static final String INSTR_PREFIX = "trecs_";
    private static final String FILENAME = "trecs" + getSuffix();
    private static final String ELFN_FILENAME = INSTR_PREFIX + "elfn" + getSuffix();
    private static final double WELL_DEPTH = 30000000.0;
    private static final double IMAGING_FRAME_TIME = .020;  //Seconds
    private static final double SPECTROSCOPY_LOW_RES_FRAME_TIME = .1; //Seconds
    private static final double SPECTROSCOPY_HI_RES_FRAME_TIME = .5; //Seconds
    private static final int DETECTOR_PIXELS = 320;

    //Read Extra-low freq data parameter from file
    private static int elfn_param;  // extra low frequency noise
    static {
        final String dir = ITCConstants.LIB + "/" + INSTR_DIR + "/";
        try (final Scanner in = DatFile.scanFile(dir + ELFN_FILENAME)) {
            elfn_param = in.nextInt();
        }
    }

    // Keep a reference to the color filter to ask for effective wavelength
    private final Option<Filter> _filter;
    private final Option<TrecsGratingOptics> _gratingOptics;
    private final double _sampling;
    private final Disperser _grating;
    private final Mask _focalPlaneMask;
    private final CalculationMethod _mode;
    private final double _centralWavelength;

    public TRecs(final TRecsParameters tp, final ObservationDetails odp) {
        super(Site.GS, Bands.MID_IR, INSTR_DIR, FILENAME);

        _focalPlaneMask = tp.mask();
        _grating = tp.grating();
        _centralWavelength = tp.centralWavelength().toNanometers();
        _mode = odp.calculationMethod();

        final TReCSParams.WindowWheel instrumentWindow = tp.instrumentWindow();
        final String file = getDirectory() + "/" + getPrefix() + instrumentWindow.name() + Instrument.getSuffix();
        final InstrumentWindow trecsInstrumentWindow = new InstrumentWindow(file, instrumentWindow.name());
        addComponent(trecsInstrumentWindow);


        if (!(tp.filter().equals(TReCSParams.Filter.NONE))) {
            final Filter filter = Filter.fromWLFile(getPrefix(), tp.filter().name(), getDirectory() + "/");
            addFilter(filter);
            _filter = Option.apply(filter);
        } else {
            _filter = Option.empty();
        }


        final FixedOptics _fixedOptics = new FixedOptics(getDirectory() + "/", getPrefix());
        addComponent(_fixedOptics);


        //Test to see that all conditions for Spectroscopy are met
        if (_mode instanceof Spectroscopy) {
            if (_grating.equals(Disperser.MIRROR))
                throw new RuntimeException("Spectroscopy calculation method is selected but a grating" +
                        " is not.\nPlease select a grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
            if (_focalPlaneMask.equals(Mask.MASK_IMAGING) || _focalPlaneMask.equals(Mask.MASK_IMAGING_W))
                throw new RuntimeException("Spectroscopy calculation method is selected but a focal" +
                        " plane mask is not.\nPlease select a " +
                        "grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
        }

        if (_mode instanceof Imaging) {
            if (tp.filter().equals(TReCSParams.Filter.NONE))
                throw new RuntimeException("Imaging calculation method is selected but a filter" +
                        " is not.\n  Please select a filter and resubmit the " +
                        "form to continue.");
            if (!_grating.equals(Disperser.MIRROR))
                throw new RuntimeException("Imaging calculation method is selected but a grating" +
                        " is also selected.\nPlease deselect the " +
                        "grating or change the method to spectroscopy.");
            if (!_focalPlaneMask.equals(Mask.MASK_IMAGING) && !_focalPlaneMask.equals(Mask.MASK_IMAGING_W))
                throw new RuntimeException("Imaging calculation method is selected but a Focal" +
                        " Plane Mask is also selected.\nPlease " +
                        "deselect the Focal Plane Mask" +
                        " or change the method to spectroscopy.");
        }


        final Detector detector = new Detector(getDirectory() + "/", getPrefix(), "det", "320x240 pixel Si:As IBC array");
        detector.setDetectorPixels(DETECTOR_PIXELS);

        if (!(_grating.equals(Disperser.MIRROR))) {

            final TrecsGratingOptics gratingOptics = new TrecsGratingOptics(getDirectory() + "/" + TRecs.getPrefix(), _grating.name(),
                    _centralWavelength,
                    detector.getDetectorPixels());
            _sampling = gratingOptics.dispersion(-1);

            if (getGrating().equals(Disperser.LOW_RES_20) && !(instrumentWindow.equals(WindowWheel.KRS_5))) {
                throw new RuntimeException("The " + getGrating().displayValue() + " grating must be " +
                        "used with the " + WindowWheel.KRS_5.displayValue() + " window. \n" +
                        "Please change the grating or the window cover.");
            }
            addDisperser(gratingOptics);
            _gratingOptics = Option.apply(gratingOptics);
        } else {
            _gratingOptics = Option.empty();
            _sampling = super.getSampling();
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
        if (_grating.equals(Disperser.MIRROR)) return (int) _filter.get().getEffectiveWavelength();
        else return (int) _gratingOptics.get().getEffectiveWavelength();

    }

    public Disperser getGrating() {
        return _grating;
    }

    public double getGratingDispersion() {
        return _gratingOptics.get().dispersion(-1);
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
        return _gratingOptics.get().getPixelWidth();
    }

    public double getWellDepth() {
        return WELL_DEPTH;
    }

    public double getSampling() {
        return _sampling;
    }

    public double getFrameTime() {
        if (_mode instanceof Spectroscopy) {
            if (getGrating().equals(Disperser.HIGH_RES)) {
                return SPECTROSCOPY_HI_RES_FRAME_TIME;
            } else {
                return SPECTROSCOPY_LOW_RES_FRAME_TIME;
            }
        } else {
            return IMAGING_FRAME_TIME;
        }
    }

    public int getExtraLowFreqNoise() {
        if (_mode instanceof Spectroscopy)
            return elfn_param * 3;
        else
            return elfn_param;
    }

    /** {@inheritDoc} */
    public double getSlitWidth() {
        switch (_focalPlaneMask) {
            case MASK_1: return 0.21;
            case MASK_2: return 0.26;
            case MASK_3: return 0.31;
            case MASK_4: return 0.36;
            case MASK_5: return 0.66;
            case MASK_6: return 0.72;
            case MASK_7: return 1.32;
            default:     return -1.0;
        }
    }

    public Mask getFocalPlaneMask() {
        return _focalPlaneMask;
    }

    public double getCentralWavelength() {
        return _centralWavelength;
    }

    /**
     * The prefix on data file names for this instrument.
     */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }

    @Override public double wellDepth() {
        return WELL_DEPTH;
    }

    @Override public double gain() {
        return 1.0;
    }

    @Override public double maxFlux() {
        return WELL_DEPTH;
    }

    @Override public double getMinExposureTime() { throw new Error("NOT IMPLEMENTED"); }

    @Override public List<WarningRule> warnings() {
        return new ArrayList<WarningRule>() {{
            add(new SaturationLimitRule(WELL_DEPTH, 0.80));
        }};
    }

}
