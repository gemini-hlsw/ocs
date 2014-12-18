// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
//
package edu.gemini.itc.gmos;

import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.ITCConstants;
import edu.gemini.itc.shared.WavebandDefinition;
import edu.gemini.itc.shared.Filter;
import edu.gemini.itc.shared.Detector;
import edu.gemini.itc.shared.FixedOptics;
import edu.gemini.itc.operation.DetectorsTransmissionVisitor;
import edu.gemini.itc.parameters.ObservationDetailsParameters;

import java.awt.*;

/**
 * Gmos specification class
 */
public class GmosSouth extends Gmos {

    //Plate scales for original and Hamamatsu CCD's (temporary)
    public static final double ORIG_PLATE_SCALE = 0.0727;
    public static final double HAM_PLATE_SCALE = 0.080778;

    protected String _CCDtype;
    /**
     * /** Related files will start with this prefix
     */
    public static final String INSTR_PREFIX = "gmos_s_";

    // Instrument reads its configuration from here.
    private static final String FILENAME = "gmos_s" + getSuffix();

    // Detector data files (see REL-478)
    private static final String[] DETECTOR_CCD_FILES = {"ccd_hamamatsu_bb", "ccd_hamamatsu_hsc", "ccd_hamamatsu_sc"};
//    private static final String[] DETECTOR_CCD_FILES = {"ccd_hamamatsu_bb", "ccd_hamamatsu_blue", "ccd_hamamatsu"};

    // Detector display names corresponding to the detectorCcdIndex
    private static final String[] DETECTOR_CCD_NAMES = {"BB", "HSC", "SC"};

    // Colors to use for charts corresponding to the detectorCcdIndex
    private static final Color[] DETECTOR_CCD_COLORS = {Color.blue, Color.green, Color.red};

    private DetectorsTransmissionVisitor _dtv;

    public GmosSouth(GmosParameters gp, ObservationDetailsParameters odp, int detectorCcdIndex) throws Exception {
        super(FILENAME, INSTR_PREFIX, detectorCcdIndex);

        // The instrument data file gives a start/end wavelength for
        // the instrument.  But with a filter in place, the filter
        // transmits wavelengths that are a subset of the original range.

        WELL_DEPTH = 125000.0;

        AD_SATURATION = 56636;

        HIGH_GAIN = 4.4;
        LOW_GAIN = 2.18;

        DETECTOR_PIXELS = 6218;

        _observingStart = super.getStart();
        _observingEnd = super.getEnd();
        _sampling = super.getSampling();

        _readNoise = gp.getReadNoise();
        _wellDepth = gp.getWellDepth();
        _focalPlaneMask = gp.getFocalPlaneMask();
        //_focalPlaneMaskOffset = gp.getFPMaskOffset();
        _stringSlitWidth = gp.getStringSlitWidth();
        _grating = gp.getGrating();
        _filterUsed = gp.getFilter();
        _centralWavelength = gp.getInstrumentCentralWavelength();
        //_camera = gp.getCamera();
        _mode = odp.getCalculationMode();
        _spectralBinning = gp.getSpectralBinning();
        _spatialBinning = gp.getSpatialBinning();

        //_IFUUsed = gp.usingIFU();
        if (_focalPlaneMask.equals(gp.IFU))
            _IFUUsed = true;
        else _IFUUsed = false;

        /// !!!!!!!!NEED to Edit all of this !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // Note for designers of other instruments:
        // Other instruments may not have filters and may just use
        // the range given in their instrument file.
        if (!(_filterUsed.equals("none"))) {

            //if(!(_grating.equals("none"))){
            //	throw new Exception("Please select Grism Order Sorting from the filter list."); }
            _Filter = new Filter(getPrefix(), _filterUsed, getDirectory() + "/", Filter.GET_EFFECTIVE_WAVELEN_FROM_FILE);

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
                throw new Exception("Spectroscopy calculation method is selected but a grating" +
                        " is not.\nPlease select a grating and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
            if (_focalPlaneMask.equals(GmosParameters.NO_SLIT))
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
            if (_IFUUsed)
                throw new Exception("Imaging calculation method is selected but an IFU" +
                        " is also selected.\nPlease deselect the IFU or" +
                        " change the method to spectroscopy.");
        }


        //Original vs Hamamatsu CCD's
        _CCDtype = gp.getCCDtype();

        // See REL-478
        if (_CCDtype.equals("1")) {
            _detector = new Detector(getDirectory() + "/", getPrefix(), "ccd_red", "EEV legacy array");
            _detector.setDetectorPixels(DETECTOR_PIXELS);
            if (detectorCcdIndex == 0) _instruments = new Gmos[]{this};
        } else if (_CCDtype.equals("2")) {
            String fileName = DETECTOR_CCD_FILES[detectorCcdIndex];
            String name = DETECTOR_CCD_NAMES[detectorCcdIndex];
            Color color = DETECTOR_CCD_COLORS[detectorCcdIndex];
            _detector = new Detector(getDirectory() + "/", getPrefix(), fileName, "Hamamatsu array", name, color);
            _detector.setDetectorPixels(DETECTOR_PIXELS);
            if (detectorCcdIndex == 0)
                _instruments = new Gmos[]{this, new GmosSouth(gp, odp, 1), new GmosSouth(gp, odp, 2)};
        }

        if (detectorCcdIndex == 0) {
            _dtv = new DetectorsTransmissionVisitor(_spectralBinning,
                    getDirectory() + "/" + getPrefix() + "ccdpix_red" + Instrument.getSuffix());
        }

        if (_IFUUsed) {
            _IFUMethod = gp.getIFUMethod();

            if (_IFUMethod.equals(gp.SINGLE_IFU)) {
                _IFU_IsSingle = true;
                _IFUOffset = gp.getIFUOffset();
                _IFU = new IFUComponent(_IFUOffset);
            }
            if (_IFUMethod.equals(gp.RADIAL_IFU)) {
                _IFUMinOffset = gp.getIFUMinOffset();
                _IFUMaxOffset = gp.getIFUMaxOffset();

                _IFU = new IFUComponent(_IFUMinOffset, _IFUMaxOffset);
            }
            addComponent(_IFU);


        }


        if (!(_grating.equals("none"))) {

            _gratingOptics = new GratingOptics(getDirectory() + "/", getPrefix(), _grating, _detector,
                    // _focalPlaneMaskOffset,
                    _stringSlitWidth,
                    _centralWavelength,
                    _detector.getDetectorPixels(),//_spectralBinning,
                    _spectralBinning);
            _sampling = _gratingOptics.getGratingDispersion_nmppix();
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
                        (_Filter.getEnd() <= _gratingOptics.getStart()))

                {
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
     * The prefix on data file names for this instrument.
     */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }

    public edu.gemini.itc.operation.DetectorsTransmissionVisitor getDetectorTransmision() {
        return _dtv;
    }


    public double getPixelSize() {

        //Temp method of returning correct GMOS-N pixel size.
        //Cannot change the value directly since pixel_size is a private
        //member variable of the superclass Instrument.

        // REL-477: XXX FIXME
        if (_CCDtype.equals("1")) {
            return ORIG_PLATE_SCALE * _spatialBinning;
        } else {
            return HAM_PLATE_SCALE * _spatialBinning;
        }
    }

}
