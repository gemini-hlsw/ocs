// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
//
package edu.gemini.itc.michelle;

import edu.gemini.itc.parameters.ObservationDetailsParameters;
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
    private static final double SPECTROSCOPY_FRAME_TIME = .1; //Seconds
    private static final double SPECTROSCOPY_LOWRES_N_FRAME_TIME = .25; //Seconds
    private static final double SPECTROSCOPY_MED_N1_FRAME_TIME = 1.25; //Seconds
    private static final double SPECTROSCOPY_MED_N2_FRAME_TIME = 3.0; //Seconds
    private static final double SPECTROSCOPY_ECHELLE_FRAME_TIME = 30; //Seconds

    private static final double SPECTROSCOPY_PIXEL_SIZE = 0.2;

    private static final int DETECTOR_PIXELS = 320;

    private edu.gemini.itc.operation.DetectorsTransmissionVisitor _dtv;

    // Keep a reference to the color filter to ask for effective wavelength
    private Filter _Filter;
    private MichelleGratingOptics _gratingOptics;
    private Detector _detector;
    private double _sampling;
    private String _filterUsed;
    private String _grating;
    private String _focalPlaneMask;
    private String _mode;
    private double _centralWavelength;
    private int _spectralBinning;
    private int _spatialBinning;

    // These are the limits of observable wavelength with this configuration.
    private double _observingStart;
    private double _observingEnd;

    public Michelle(MichelleParameters mp, ObservationDetailsParameters odp) throws Exception {
        super(INSTR_DIR, FILENAME);
        // The instrument data file gives a start/end wavelength for
        // the instrument.  But with a filter in place, the filter
        // transmits wavelengths that are a subset of the original range.

        _observingStart = super.getStart();
        _observingEnd = super.getEnd();
        _sampling = super.getSampling();
        _focalPlaneMask = mp.getFocalPlaneMask();
        _grating = mp.getGrating();
        _filterUsed = mp.getFilter();
        _centralWavelength = mp.getInstrumentCentralWavelength();

        _mode = odp.getCalculationMode();
        _spectralBinning = mp.getSpectralBinning();
        _spatialBinning = mp.getSpatialBinning();


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

        /// !!!!!!!!NEED to Edit all of this !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // Note for designers of other instruments:
        // Other instruments may not have filters and may just use
        // the range given in their instrument file.
        if (!(_filterUsed.equals("none"))) {

            //if(!(_grating.equals("none"))){
            //	throw new Exception("Please select Grism Order Sorting from the filter list."); }

            _Filter = Filter.fromWLFile(getPrefix(), _filterUsed, getDirectory() + "/");

            if (_Filter.getStart() >= _observingStart)
                _observingStart = _Filter.getStart();
            if (_Filter.getEnd() <= _observingEnd)
                _observingEnd = _Filter.getEnd();
            addComponent(_Filter);

        }


        FixedOptics _fixedOptics = new FixedOptics(getDirectory() + "/", getPrefix());
        //addComponent(new FixedOptics(getDirectory()+"/"));
        addComponent(_fixedOptics);


        //Test to see that all conditions for Spectroscopy are met
        if (_mode.equals(ObservationDetailsParameters.SPECTROSCOPY)) {
            if (_grating.equals("none"))
                throw new Exception("Spectroscopy mode is selected but a grating" +
                        " is not.\nPlease select a grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
            if (_focalPlaneMask.equals(MichelleParameters.NO_SLIT))
                throw new Exception("Spectroscopy mode is selected but a focal" +
                        " plane mask is not.\nPlease select a " +
                        "grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
            if (mp.polarimetryIsUsed()) {
                throw new Exception("Spectroscopy mode cannot be used with the " +
                        "Polarimeter in.\n Please either deselect the " +
                        "Polarimeter, or change the mode to Imaging.");
            }
        }

        if (_mode.equals(ObservationDetailsParameters.IMAGING)) {
            if (_filterUsed.equals("none"))
                throw new Exception("Imaging mode is selected but a filter" +
                        " is not.\n  Please select a filter and resubmit the " +
                        "form to continue.");
            if (!_grating.equals("none"))
                throw new Exception("Imaging mode is selected but a grating" +
                        " is also selected.\nPlease deselect the " +
                        "grating or change the mode to spectroscopy.");
            if (!_focalPlaneMask.equals("none"))
                throw new Exception("Imaging mode is selected but a Focal" +
                        " Plane Mask is also selected.\nPlease " +
                        "deselect the Focal Plane Mask" +
                        " or change the mode to spectroscopy.");
        }


        _detector = new Detector(getDirectory() + "/", getPrefix(), "det",
                "320x240 pixel Si:As IBC array");
        _detector.setDetectorPixels(DETECTOR_PIXELS);

        _dtv = new edu.gemini.itc.operation.DetectorsTransmissionVisitor(_spectralBinning,
                getDirectory() + "/" + getPrefix() + "ccdpix" + Instrument.getSuffix());

        if (!(_grating.equals("none"))) {

            _gratingOptics = new MichelleGratingOptics(getDirectory() + "/" + getPrefix(), _grating,
                    _centralWavelength,
                    _detector.getDetectorPixels(), //_spectralBinning,
                    _spectralBinning);
            //_sampling = _gratingOptics.getGratingDispersion_nmppix();
            //if (super.getStart()< _gratingOptics.getStart())
            _observingStart = _gratingOptics.getStart();
            //   else _observingStart = super.getStart();
            //if (super.getEnd() > _gratingOptics.getEnd())
            _observingEnd = _gratingOptics.getEnd();
            //   else _observingEnd = super.getEnd();

            if (!(_grating.equals("none")) && !(_filterUsed.equals("none")))
//	    if ((_observingStart >= _gratingOptics.getEnd())||
//		(_observingEnd <= _gratingOptics.getStart()))
                if ((_Filter.getStart() >= _gratingOptics.getEnd()) ||
                        (_Filter.getEnd() <= _gratingOptics.getStart())) {
                    throw new Exception("The " + _filterUsed + " filter" +
                            " and the " + _grating +
                            " do not overlap with the requested wavelength.\n" +
                            " Please select a different filter, grating or wavelength.");
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
        if (_mode.equals(ObservationDetailsParameters.SPECTROSCOPY)) {
            return SPECTROSCOPY_PIXEL_SIZE * _spatialBinning;
        } else
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
            String tempGrating = getGrating();
            double frameTime = 0.1;

            switch (_gratingOptics.getGratingNumber()) {
                case MichelleParameters.LOWN:
                    frameTime = SPECTROSCOPY_LOWRES_N_FRAME_TIME;
                    break;
                case MichelleParameters.MEDN1:
                    frameTime = SPECTROSCOPY_MED_N1_FRAME_TIME;
                    break;
                case MichelleParameters.MEDN2:
                    frameTime = SPECTROSCOPY_MED_N2_FRAME_TIME;
                    break;
                case MichelleParameters.ECHELLEN:
                case MichelleParameters.ECHELLEQ:
                    frameTime = SPECTROSCOPY_ECHELLE_FRAME_TIME;
                    break;
                default:
                    frameTime = SPECTROSCOPY_FRAME_TIME;
                    break;
            }

            return frameTime;
            /*
            if (tempGrating.equals(MichelleParameters.LOW_N))
                return SPECTROSCOPY_LOWRES_N_FRAME_TIME;
            else if (tempGrating.equals(MichelleParameters.MED_N1))
                return SPECTROSCOPY_MED_N1_FRAME_TIME;
            else if (tempGrating.equals(MichelleParameters.MED_N2))
                return SPECTROSCOPY_MED_N2_FRAME_TIME;
            else if (tempGrating.equals(MichelleParameters.ECHELLE_N)||
                    tempGrating.equals(MichelleParameters.ECHELLE_Q))
                return SPECTROSCOPY_ECHELLE_FRAME_TIME;
            else
                return SPECTROSCOPY_FRAME_TIME;
             */
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

        if (!_focalPlaneMask.equals(MichelleParameters.NO_SLIT))
            s += "<LI> Focal Plane Mask: " + _focalPlaneMask;
        s += "\n";
        s += "\n";
        if (_mode.equals(ObservationDetailsParameters.SPECTROSCOPY))
            s += "<L1> Central Wavelength: " + _centralWavelength + " nm" + "\n";
        //s += "Instrument: " +super.getName() + "\n";
        s += "Spatial Binning: " + getSpatialBinning() + "\n";
        if (_mode.equals(ObservationDetailsParameters.SPECTROSCOPY))
            s += "Spectral Binning: " + getSpectralBinning() + "\n";
        s += "Pixel Size in Spatial Direction: " + getPixelSize() + "arcsec\n";
        if (_mode.equals(ObservationDetailsParameters.SPECTROSCOPY))
            s += "Pixel Size in Spectral Direction: " + getGratingDispersion_nmppix() + "nm\n";
        return s;
    }
}
