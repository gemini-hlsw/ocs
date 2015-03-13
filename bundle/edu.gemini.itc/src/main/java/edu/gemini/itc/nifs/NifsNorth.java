// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.nifs;

import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.parameters.ObservationDetailsParameters;
import edu.gemini.itc.shared.*;

/**
 * Nifs specification class
 */
public class NifsNorth extends Nifs {

    /**
     * Related files will start with this prefix
     */
    public static final String INSTR_PREFIX = "nifs_";

    // Instrument reads its configuration from here.
    private static final String FILENAME = "nifs" + getSuffix();

    //private static edu.gemini.itc.operation.DetectorsTransmissionVisitor _dtv;
    private edu.gemini.itc.operation.DetectorsTransmissionVisitor _dtv;

    private TransmissionElement _selectableTrans;
    private NifsParameters _gp;
    private double _wellDepth;
    private double _readNoiseValue;


    public NifsNorth(NifsParameters gp, ObservationDetailsParameters odp) {
        super(FILENAME, INSTR_PREFIX);
        // The instrument data file gives a start/end wavelength for
        // the instrument.  But with a filter in place, the filter
        // transmits wavelengths that are a subset of the original range.
        _gp = gp;

        WELL_DEPTH = 90000.0;

        double SHALLOW_WELL = 90000.0;
        double DEEP_WELL = 180000.0;

        double HIGH_READ_NOISE_VALUE = 145;  //Not used

        // REL-481: Update NIFS read noise estimates
        double MEDIUM_READ_NOISE_VALUE = 15.4;
        double LOW_READ_NOISE_VALUE = 8.1;
        double VERY_LOW_READ_NOISE_VALUE = 4.6;


        AD_SATURATION = 56636;

        HIGH_GAIN = 4.4;
        LOW_GAIN = 2.18;

        DETECTOR_PIXELS = 2048;


        _sampling = super.getSampling();

        _readNoise = gp.getReadNoise();
        _focalPlaneMask = gp.getFocalPlaneMask();
        _grating = gp.getGrating();
        _centralWavelength = gp.getInstrumentCentralWavelength();
        _filterUsed = gp.getFilter();
        _mode = odp.getMethod();

        if (_centralWavelength < 1000 || _centralWavelength > 6000) {
            throw new RuntimeException("Central wavelength must be between 1.00um and 6.0um.");
        }

        if (_focalPlaneMask.equals(gp.IFU))
            _IFUUsed = true;
        else _IFUUsed = false;

        //Set read noise and Well depth values by obsevation type
        if (_readNoise.equals(gp.HIGH_READ_NOISE)) {
            _readNoiseValue = HIGH_READ_NOISE_VALUE;
            _wellDepth = DEEP_WELL;
        } else if (_readNoise.equals(gp.MED_READ_NOISE)) {
            _readNoiseValue = MEDIUM_READ_NOISE_VALUE;
            _wellDepth = SHALLOW_WELL;
        } else if (_readNoise.equals(gp.LOW_READ_NOISE)) {
            _readNoiseValue = LOW_READ_NOISE_VALUE;
            _wellDepth = SHALLOW_WELL;
        } else if (_readNoise.equals(gp.VERY_LOW_READ_NOISE)) {
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
            if (_focalPlaneMask.equals(NifsParameters.NO_SLIT))
                throw new RuntimeException("Spectroscopy calculation method is selected but a focal" +
                        " plane mask is not.\nPlease select a " +
                        "grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
        }

        _detector = new Detector(getDirectory() + "/", getPrefix(),
                "hawaii2_HgCdTe", "2K x 2K HgCdTe HAWAII-2 CCD");
        _detector.setDetectorPixels(DETECTOR_PIXELS);

        _dtv = new DetectorsTransmissionVisitor(1,
                getDirectory() + "/" + getPrefix() + "ccdpix" + Instrument.getSuffix());

        if (_IFUUsed) {
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


        }


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

    public double getWellDepth() {
        return _wellDepth;
    }

    public double getReadNoise() {
        return _readNoiseValue;
    }

    public double getObservingStart() {
        double start = _centralWavelength - (getGratingDispersion_nmppix() * _detector.getDetectorPixels() / 2);
        return start;
    }

    public double getObservingEnd() {
        double end = _centralWavelength + (getGratingDispersion_nmppix() * _detector.getDetectorPixels() / 2);
        return end;
    }


    public void setCentralWavelength(double centralWavelength) {
        _centralWavelength = centralWavelength;
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

}
