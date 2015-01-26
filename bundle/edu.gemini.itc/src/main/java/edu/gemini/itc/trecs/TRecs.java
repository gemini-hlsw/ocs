package edu.gemini.itc.trecs;

import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.shared.*;

import java.util.Scanner;

/**
 * TRecs specification class
 */
public class TRecs extends Instrument {
    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_DIR = "trecs";

    /**
     * Related files will start with this prefix
     */
    public static final String INSTR_PREFIX = "trecs_";

    // Instrument reads its configuration from here.
    private static final String FILENAME = "trecs" + getSuffix();

    private static final String ELFN_FILENAME = INSTR_PREFIX + "elfn" + getSuffix();

    private static final double WELL_DEPTH = 30000000.0;

    private static final double AD_SATURATION = 2500000;

    private static final double HIGH_GAIN = 4.4;
    private static final double LOW_GAIN = 2.18;

    private static final double IMAGING_FRAME_TIME = .020;  //Seconds
    private static final double SPECTROSCOPY_LOW_RES_FRAME_TIME = .1; //Seconds
    private static final double SPECTROSCOPY_HI_RES_FRAME_TIME = .5; //Seconds

    private static final int DETECTOR_PIXELS = 320;

    private edu.gemini.itc.operation.DetectorsTransmissionVisitor _dtv;


    // Keep a reference to the color filter to ask for effective wavelength
    private Filter _Filter;
    private GratingOptics _gratingOptics;
    private Detector _detector;
    private double _sampling;
    private String _filterUsed;
    private String _instrumentWindow;
    private String _grating;
    private String _readNoise;
    private String _wellDepth;
    private String _focalPlaneMask;
    private String _stringSlitWidth;
    private String _mode;
    private double _centralWavelength;
    private int _spectralBinning;
    private int _spatialBinning;

    private int elfn_param;  // extra low frequency noise

    // These are the limits of observable wavelength with this configuration.
    private double _observingStart;
    private double _observingEnd;

    public TRecs(TRecsParameters tp, ObservationDetailsParameters odp) throws Exception {
        super(INSTR_DIR, FILENAME);
        // The instrument data file gives a start/end wavelength for
        // the instrument.  But with a filter in place, the filter
        // transmits wavelengths that are a subset of the original range.

        _observingStart = super.getStart();
        _observingEnd = super.getEnd();
        _sampling = super.getSampling();

        _readNoise = tp.getReadNoise();
        _wellDepth = tp.getWellDepth();
        _focalPlaneMask = tp.getFocalPlaneMask();

        _stringSlitWidth = tp.getStringSlitWidth();
        _grating = tp.getGrating();
        _filterUsed = tp.getFilter();
        _instrumentWindow = tp.getInstrumentWindow();
        _centralWavelength = tp.getInstrumentCentralWavelength();

        _mode = odp.getCalculationMode();
        _spectralBinning = tp.getSpectralBinning();
        _spatialBinning = tp.getSpatialBinning();

        //Read Extra-low freq data from file
        final String dir = ITCConstants.LIB + "/" + INSTR_DIR + "/";
        try (final Scanner in = DatFile.scan(dir + ELFN_FILENAME)) {
            elfn_param = in.nextInt();
        }

        //end of Extra-low freq data

        InstrumentWindow trecsInstrumentWindow =
                new InstrumentWindow(getDirectory() + "/" + getPrefix() +
                        _instrumentWindow + Instrument.getSuffix(), _instrumentWindow);
        addComponent(trecsInstrumentWindow);


        /// !!!!!!!!NEED to Edit all of this !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // Note for designers of other instruments:
        // Other instruments may not have filters and may just use
        // the range given in their instrument file.
        if (!(_filterUsed.equals("none"))) {


            _Filter = Filter.fromWLFile(getPrefix(), _filterUsed, getDirectory() + "/");

            if (_Filter.getStart() >= _observingStart)
                _observingStart = _Filter.getStart();
            if (_Filter.getEnd() <= _observingEnd)
                _observingEnd = _Filter.getEnd();
            addComponent(_Filter);

        }


        FixedOptics _fixedOptics = new FixedOptics(getDirectory() + "/", getPrefix());
        addComponent(_fixedOptics);


        //Test to see that all conditions for Spectroscopy are met
        if (_mode.equals(ObservationDetailsParameters.SPECTROSCOPY)) {
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

        if (_mode.equals(ObservationDetailsParameters.IMAGING)) {
            if (_filterUsed.equals("none"))
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


        _detector = new Detector(getDirectory() + "/", getPrefix(), "det",
                "320x240 pixel Si:As IBC array");
        _detector.setDetectorPixels(DETECTOR_PIXELS);

        _dtv = new edu.gemini.itc.operation.DetectorsTransmissionVisitor(_spectralBinning,
                getDirectory() + "/" + getPrefix() + "ccdpix" + Instrument.getSuffix());

        if (!(_grating.equals("none"))) {

            _gratingOptics = new GratingOptics(getDirectory() + "/", _grating,
                    _stringSlitWidth,
                    _centralWavelength,
                    _detector.getDetectorPixels(),//_spectralBinning,
                    _spectralBinning);
            _sampling = _gratingOptics.getGratingDispersion_nmppix();
            _observingStart = _gratingOptics.getStart();
            _observingEnd = _gratingOptics.getEnd();

            if (!(_grating.equals("none")) && !(_filterUsed.equals("none")))
                if ((_Filter.getStart() >= _gratingOptics.getEnd()) ||
                        (_Filter.getEnd() <= _gratingOptics.getStart()))

                {
                    throw new Exception("The " + _filterUsed + " filter" +
                            " and the " + _grating +
                            " do not overlap with the requested wavelength.\n" +
                            " Please select a different filter, grating or wavelength.");
                }

            if (getGrating().equals(TRecsParameters.LORES20_G5402) && !(_instrumentWindow.equals(TRecsParameters.KRS5))) {
                throw new Exception("The " + getGrating() + " grating must be " +
                        "used with the " + TRecsParameters.KRS5 + " window. \n" +
                        "Please change the grating or the window cover.");
            }
            addComponent(_gratingOptics);
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
        return _gratingOptics.getPixelWidth();
    }

    public double getWellDepth() {
        return WELL_DEPTH;
    }

    public double getSampling() {
        return _sampling;
    }

    public double getFrameTime() {
        if (_mode.equals(ObservationDetailsParameters.SPECTROSCOPY)) {
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

    public double getADSaturation() {
        return AD_SATURATION;
    }

    public double getHighGain() {
        return HIGH_GAIN;
    }

    public double getLowGain() {
        return LOW_GAIN;
    }

    public int getExtraLowFreqNoise() {
        if (_mode.equals(ObservationDetailsParameters.SPECTROSCOPY))
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
        if (_mode.equals(ObservationDetailsParameters.SPECTROSCOPY))
            s += "<L1> Central Wavelength: " + _centralWavelength + " nm" + "\n";
        s += "Spatial Binning: " + getSpatialBinning() + "\n";
        if (_mode.equals(ObservationDetailsParameters.SPECTROSCOPY))
            s += "Spectral Binning: " + getSpectralBinning() + "\n";
        s += "Pixel Size in Spatial Direction: " + getPixelSize() + "arcsec\n";
        if (_mode.equals(ObservationDetailsParameters.SPECTROSCOPY))
            s += "Pixel Size in Spectral Direction: " + getGratingDispersion_nmppix() + "nm\n";
        return s;
    }
}
