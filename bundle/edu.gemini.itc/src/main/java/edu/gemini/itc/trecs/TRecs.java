package edu.gemini.itc.trecs;

import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.shared.*;
import scala.Option;

import java.util.Scanner;

/**
 * TRecs specification class
 */
public final class TRecs extends Instrument {
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
    private final String _grating;
    private final String _focalPlaneMask;
    private final CalculationMethod _mode;
    private final double _centralWavelength;
    private final int _spectralBinning;
    private final int _spatialBinning;
    private final DetectorsTransmissionVisitor _dtv;

    // These are the limits of observable wavelength with this configuration.
    private double _observingStart;
    private double _observingEnd;

    public TRecs(final TRecsParameters tp, final ObservationDetailsParameters odp) throws Exception {
        super(INSTR_DIR, FILENAME);
        // The instrument data file gives a start/end wavelength for
        // the instrument.  But with a filter in place, the filter
        // transmits wavelengths that are a subset of the original range.

        _observingStart = super.getStart();
        _observingEnd = super.getEnd();
        _focalPlaneMask = tp.getFocalPlaneMask();
        _grating = tp.getGrating();
        _centralWavelength = tp.getInstrumentCentralWavelength();
        _mode = odp.getMethod();
        _spectralBinning = tp.getSpectralBinning();
        _spatialBinning = tp.getSpatialBinning();

        final String instrumentWindow = tp.getInstrumentWindow();
        final String file = getDirectory() + "/" + getPrefix() + instrumentWindow + Instrument.getSuffix();
        final InstrumentWindow trecsInstrumentWindow = new InstrumentWindow(file, instrumentWindow);
        addComponent(trecsInstrumentWindow);


        /// !!!!!!!!NEED to Edit all of this !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // Note for designers of other instruments:
        // Other instruments may not have filters and may just use
        // the range given in their instrument file.
        if (!(tp.getFilter().equals("none"))) {
            final Filter filter = Filter.fromWLFile(getPrefix(), tp.getFilter(), getDirectory() + "/");
            if (filter.getStart() >= _observingStart) {
                _observingStart = filter.getStart();
            }
            if (filter.getEnd() <= _observingEnd) {
                _observingEnd = filter.getEnd();
            }
            addComponent(filter);
            _filter = Option.apply(filter);
        } else {
            _filter = Option.empty();
        }


        final FixedOptics _fixedOptics = new FixedOptics(getDirectory() + "/", getPrefix());
        addComponent(_fixedOptics);


        //Test to see that all conditions for Spectroscopy are met
        if (_mode.isSpectroscopy()) {
            if (_grating.equals("none"))
                throw new Exception("Spectroscopy calculation method is selected but a grating" +
                        " is not.\nPlease select a grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
            if (_focalPlaneMask.equals(TRecsParameters.NO_SLIT))
                throw new Exception("Spectroscopy calculation method is selected but a focal" +
                        " plane mask is not.\nPlease select a " +
                        "grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
        }

        if (_mode.isImaging()) {
            if (tp.getFilter().equals("none"))
                throw new Exception("Imaging calculation method is selected but a filter" +
                        " is not.\n  Please select a filter and resubmit the " +
                        "form to continue.");
            if (!_grating.equals("none"))
                throw new Exception("Imaging calculation method is selected but a grating" +
                        " is also selected.\nPlease deselect the " +
                        "grating or change the method to spectroscopy.");
            if (!_focalPlaneMask.equals("none"))
                throw new Exception("Imaging calculation method is selected but a Focal" +
                        " Plane Mask is also selected.\nPlease " +
                        "deselect the Focal Plane Mask" +
                        " or change the method to spectroscopy.");
        }


        final Detector detector = new Detector(getDirectory() + "/", getPrefix(), "det", "320x240 pixel Si:As IBC array");
        detector.setDetectorPixels(DETECTOR_PIXELS);

        _dtv = new edu.gemini.itc.operation.DetectorsTransmissionVisitor(_spectralBinning,
                getDirectory() + "/" + getPrefix() + "ccdpix" + Instrument.getSuffix());

        if (!(_grating.equals("none"))) {

            final TrecsGratingOptics gratingOptics = new TrecsGratingOptics(getDirectory() + "/" + TRecs.getPrefix(), _grating,
                    _centralWavelength,
                    detector.getDetectorPixels(),//_spectralBinning,
                    _spectralBinning);
            _sampling = gratingOptics.getGratingDispersion_nmppix();
            _observingStart = gratingOptics.getStart();
            _observingEnd = gratingOptics.getEnd();

            if (!(_grating.equals("none")) && _filter.isDefined())
                if ((_filter.get().getStart() >= gratingOptics.getEnd()) ||
                        (_filter.get().getEnd() <= gratingOptics.getStart()))

                {
                    throw new Exception("The " + tp.getFilter() + " filter" +
                            " and the " + _grating +
                            " do not overlap with the requested wavelength.\n" +
                            " Please select a different filter, grating or wavelength.");
                }

            if (getGrating().equals(TRecsParameters.LORES20_G5402) && !(instrumentWindow.equals(TRecsParameters.KRS5))) {
                throw new Exception("The " + getGrating() + " grating must be " +
                        "used with the " + TRecsParameters.KRS5 + " window. \n" +
                        "Please change the grating or the window cover.");
            }
            addComponent(gratingOptics);
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
        if (_grating.equals("none")) return (int) _filter.get().getEffectiveWavelength();
        else return (int) _gratingOptics.get().getEffectiveWavelength();

    }

    public double getGratingResolution() {
        return _gratingOptics.get().getGratingResolution();
    }


    public String getGrating() {
        return _grating;
    }

    public double getGratingDispersion_nm() {
        return _gratingOptics.get().getGratingDispersion_nm();
    }

    public double getGratingDispersion_nmppix() {
        return _gratingOptics.get().getGratingDispersion_nmppix();
    }


    /**
     * Returns the subdirectory where this instrument's data files are.
     */
    //Changed Oct 19.  If any problem reading in lib files change back...
    //public String getDirectory() { return ITCConstants.INST_LIB + "/" +
    //			      INSTR_DIR+"/lib"; }
    public String getDirectory() {
        return ITCConstants.LIB + "/" +
                INSTR_DIR;
    }

    public double getObservingStart() {
        return _observingStart;
    }

    public double getObservingEnd() {
        return _observingEnd;
    }

    public double getPixelSize() {
        return super.getPixelSize() * _spatialBinning;
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
        if (_mode.isSpectroscopy()) {
            if (getGrating().equals(TRecsParameters.HIRES10_G5403)) {
                return SPECTROSCOPY_HI_RES_FRAME_TIME;
            } else {
                return SPECTROSCOPY_LOW_RES_FRAME_TIME;
            }
        } else {
            return IMAGING_FRAME_TIME;
        }
    }

    public int getSpectralBinning() {
        return _spectralBinning;
    }

    public int getSpatialBinning() {
        return _spatialBinning;
    }

    public int getExtraLowFreqNoise() {
        if (_mode.isSpectroscopy())
            return elfn_param * 3;
        else
            return elfn_param;
    }

    /**
     * The prefix on data file names for this instrument.
     */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }

    public edu.gemini.itc.operation.DetectorsTransmissionVisitor getDetectorTransmision() {
        return _dtv;
    }

    public String toString() {

        String s = "Instrument configuration: \n";
        s += super.opticalComponentsToString();

        if (!_focalPlaneMask.equals(TRecsParameters.NO_SLIT))
            s += "<LI> Focal Plane Mask: " + _focalPlaneMask + "\n";
        s += "\n";
        if (_mode.isSpectroscopy())
            s += "<L1> Central Wavelength: " + _centralWavelength + " nm" + "\n";
        s += "Spatial Binning: " + getSpatialBinning() + "\n";
        if (_mode.isSpectroscopy())
            s += "Spectral Binning: " + getSpectralBinning() + "\n";
        s += "Pixel Size in Spatial Direction: " + getPixelSize() + "arcsec\n";
        if (_mode.isSpectroscopy())
            s += "Pixel Size in Spectral Direction: " + getGratingDispersion_nmppix() + "nm\n";
        return s;
    }
}
