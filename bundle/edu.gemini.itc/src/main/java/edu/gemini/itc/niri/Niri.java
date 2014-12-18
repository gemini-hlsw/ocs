// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id Niri.java,v 1.2 1999/10/14 16:37:00 cvs-tuc Exp $
//
package edu.gemini.itc.niri;

import edu.gemini.itc.shared.Instrument;
import edu.gemini.itc.shared.ITCConstants;
import edu.gemini.itc.shared.WavebandDefinition;
import edu.gemini.itc.shared.Filter;
import edu.gemini.itc.shared.Detector;
import edu.gemini.itc.shared.FixedOptics;
import edu.gemini.itc.parameters.ObservationDetailsParameters;

import java.util.Iterator;

/**
 * Niri specification class
 */
public class Niri extends Instrument {
    /**
     * Related files will be in this subdir of lib
     */
    public static final String INSTR_DIR = "niri";

    /**
     * Related files will start with this prefix
     */
    public static final String INSTR_PREFIX = "";

    // Instrument reads its configuration from here.
    private static final String FILENAME = "niri" + getSuffix();

    private static final double LOW_BACK_WELL_DEPTH = 200000.0;
    private static final double HIGH_BACK_WELL_DEPTH = 280000.0; //updated Feb 13. 2001

    private static final double LOW_BACK_READ_NOISE = 12.0;
    private static final double MED_BACK_READ_NOISE = 35.0;
    private static final double HIGH_BACK_READ_NOISE = 70.0;

    // _Filter2 is used only in the case that the PK50 filter is required
    private Filter _Filter, _Filter2;
    private GrismOptics _grismOptics;
    private String _filterUsed;
    private String _camera;
    private String _grism;
    private String _readNoise;
    private String _wellDepth;
    private String _focalPlaneMask;
    private String _focalPlaneMaskOffset;
    private String _stringSlitWidth;
    private String _mode;

    // These are the limits of observable wavelength with this configuration.
    private double _observingStart;
    private double _observingEnd;

    /**
     * construct an Niri with specified Broadband filter or Narrowband filter.
     * grism, and camera type.
     */
    //public Niri(String FilterUsed, String grism, String camera,
    //       String readNoise, String wellDepth, String mode,
    //       String focalPlaneMask, String focalPlaneMaskOffset,
    //       String stringSlitWidth)
    //   throws Exception
    public Niri(NiriParameters np, ObservationDetailsParameters odp) throws Exception {
        super(INSTR_DIR, FILENAME);
        // The instrument data file gives a start/end wavelength for
        // the instrument.  But with a filter in place, the filter
        // transmits wavelengths that are a subset of the original range.

        _observingStart = super.getStart();
        _observingEnd = super.getEnd();

        _readNoise = np.getReadNoise();
        _wellDepth = np.getWellDepth();
        _focalPlaneMask = np.getFocalPlaneMask();
        _focalPlaneMaskOffset = np.getFPMaskOffset();
        _stringSlitWidth = np.getStringSlitWidth();
        _filterUsed = np.getFilter();
        _grism = np.getGrism();
        _camera = np.getCamera();
        _mode = odp.getCalculationMode();
        ////_readNoise = readNoise;
        ////_wellDepth = wellDepth;
        ////_focalPlaneMask = focalPlaneMask;
        ////_focalPlaneMaskOffset = focalPlaneMaskOffset;
        ////_stringSlitWidth = stringSlitWidth;


        // Note for designers of other instruments:
        // Other instruments may not have filters and may just use
        // the range given in their instrument file.
        if (!(_filterUsed.equals("none"))) {

            //System.out.println("gris: " +grism);
            //if(!(_grism.equals("none"))){
            //	throw new Exception("Please select Grism Order Sorting from the filter list."); }
            _Filter = new Filter(getPrefix(), _filterUsed, getDirectory() + "/", Filter.CALC_EFFECTIVE_WAVELEN);

            _observingStart = _Filter.getStart();
            _observingEnd = _Filter.getEnd();
            addComponent(_Filter);

            //The PK50 filter is used with many NIRI narrow-band filters but has
            //not been included until now (20100105).  Most of NIRI's filter curves don't
            //extend far enough for it to matter.
            //To do this right we should have full transmission curves for all filters
            //that are used with the PK50 and include them all.
            if (_filterUsed.equals("Y-G0241")) {
                _Filter2 = new Filter(getPrefix(), "PK50-fake", getDirectory() + "/", Filter.CALC_EFFECTIVE_WAVELEN);

                addComponent(_Filter2);
            }

        }

        FixedOptics test = new FixedOptics(getDirectory() + "/", getPrefix());
        //addComponent(new FixedOptics(getDirectory()+"/"));
        addComponent(test);

        ////_grism=grism;
        ////_camera=camera;

        //Test to see that all conditions for Spectroscopy are met
        if (_mode.equals(ObservationDetailsParameters.SPECTROSCOPY)) {
            if (_grism.equals("none"))
                throw new Exception("Spectroscopy calculation method is selected but a grism" +
                        " is not.\nPlease select a grism and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
            if (_focalPlaneMask.equals(NiriParameters.NO_SLIT))
                throw new Exception("Spectroscopy calculation method is selected but a focal" +
                        " plane mask is not.\nPlease select a " +
                        "grism and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
            //if (_camera.equals(NiriParameters.F32))
            //    throw new Exception("Spectroscopy is not allowed with the F32 " +
            //			 "camera. Please select the F6 camera\n "+
            //			 "and resubmit the form. ");
        }

        if (_mode.equals(ObservationDetailsParameters.IMAGING)) {
            if (!_grism.equals("none"))
                throw new Exception("Imaging calculation method is selected but a grism" +
                        " is also selected.\nPlease deselect the " +
                        "grism or change the method to spectroscopy.");
            if (!_focalPlaneMask.equals("none"))
                throw new Exception("Imaging calculation method is selected but a Focal" +
                        " Plane Mask is also selected.\nPlease " +
                        "deselect the Focal Plane Mask" +
                        " or change the method to spectroscopy.");
        }


        if (!(_grism.equals("none"))) {
            if (_camera.equals("F14") || _camera.equals("F32"))
                throw new Exception("The " + _camera + " camera cannot be used in Spectroscopy" +
                        " mode.  \nPlease select the F6 camera and resubmit.");


            if (_camera.equals("F32") && (_grism.equals(NiriParameters.MGRISM) || _grism.equals(NiriParameters.LGRISM)))//||_camera.equals("F32"))
                throw new Exception("The " + _camera + " camera cannot be used with L or M Band Spectroscopy" +
                        " mode.  \nPlease select a different band and resubmit.");


            if (_camera.equals("F32") && !_focalPlaneMaskOffset.equals("center"))
                throw new Exception("The " + _camera + " camera must be used with the center slit.\n " +
                        "Please select a center slit and resubmit.");

            if (_camera.equals("F32") && !(_focalPlaneMask.equals(NiriParameters.F32_SLIT_4_PIX_CENTER) || _focalPlaneMask.equals(NiriParameters.F32_SLIT_7_PIX_CENTER) || _focalPlaneMask.equals(NiriParameters.F32_SLIT_10_PIX_CENTER)))
                throw new Exception("The " + _focalPlaneMask + " slit cannot be used with the f/32 camera.\n " +
                        "Please select a f/32 compatable slit and resubmit.");

            if (!_camera.equals("F32") && (_focalPlaneMask.equals(NiriParameters.F32_SLIT_4_PIX_CENTER) || _focalPlaneMask.equals(NiriParameters.F32_SLIT_7_PIX_CENTER) || _focalPlaneMask.equals(NiriParameters.F32_SLIT_10_PIX_CENTER)))
                throw new Exception("The " + _focalPlaneMask + " slit must be used with the f/32 camera.\n " +
                        "Please select the f/32 camera and resubmit.");


            _grismOptics = new GrismOptics(getDirectory() + "/", _grism, _camera,
                    _focalPlaneMaskOffset,
                    _stringSlitWidth);
            if (_observingStart < _grismOptics.getStart())
                _observingStart = _grismOptics.getStart();
            if (_observingEnd > _grismOptics.getEnd())
                _observingEnd = _grismOptics.getEnd();

            if (!(_grism.equals("none")) && !(_filterUsed.equals("none")))
                if ((_observingStart >= _grismOptics.getEnd()) ||
                        (_observingEnd <= _grismOptics.getStart())) {
                    throw new Exception("The " + _filterUsed + " filter" +
                            " and the " + _grism +
                            " do not overlap.\nTo continue with " +
                            "Spectroscopy mode " +
                            "either deselect the filter or choose " +
                            "one that overlaps with the grism.");
                }

            resetBackGround(INSTR_DIR, "spec_");  //Niri has spectroscopic scattering from grisms and needs
            // a new background file.
            addComponent(_grismOptics);
        }//else { throw new Exception("Please select Grism Order Sorting from the filter list."); }


        if (_camera.equals("F6"))
            addComponent(new F6Optics(getDirectory() + "/"));
        else if (_camera.equals("F14"))
            addComponent(new F14Optics(getDirectory() + "/"));
        else if (_camera.equals("F32"))
            addComponent(new F32Optics(getDirectory() + "/"));

        addComponent(new Detector(getDirectory() + "/", getPrefix(), "detector",
                "1024x1024-pixel ALADDIN InSb array"));

    }

    /**
     * Returns the effective observing wavelength.
     * This is properly calculated as a flux-weighted averate of
     * observed spectrum.  So this may be temporary.
     *
     * @return Effective wavelength in nm
     */
    public int getEffectiveWavelength() {
        if (_grism.equals("none")) return (int) _Filter.getEffectiveWavelength();
        else return (int) _grismOptics.getEffectiveWavelength();

    }

    public double getGrismResolution() {
        return _grismOptics.getGrismResolution();
    }

    public double getReadNoise() {
        if (_readNoise.equals(NiriParameters.LOW_READ_NOISE))
            return LOW_BACK_READ_NOISE;
        else if (_readNoise.equals(NiriParameters.MED_READ_NOISE))
            return MED_BACK_READ_NOISE;
        else return HIGH_BACK_READ_NOISE;
    }

    public String getGrism() {
        return _grism;
    }

    public String getCamera() {
        return _camera;
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
        double F6pixelsize = super.getPixelSize();
        //double bin= super.getXBinning();
        if (_camera.equals("F6")) return F6pixelsize;
        else if (_camera.equals("F14")) return 0.05; //*bin
        else if (_camera.equals("F32")) return 0.022; //*bin
        else return F6pixelsize;
    }

    public double getSpectralPixelWidth() {
        return _grismOptics.getPixelWidth();
    }

    public double getWellDepth() {
        if (_wellDepth.equals(NiriParameters.LOW_WELL_DEPTH))
            return LOW_BACK_WELL_DEPTH;
        else return HIGH_BACK_WELL_DEPTH;
    }


    /**
     * The prefix on data file names for this instrument.
     */
    public static String getPrefix() {
        return INSTR_PREFIX;
    }

    public String toString() {
        String s = "Instrument configuration: \n";
        s += "Optical Components: <BR>";
        for (Iterator itr = getComponents().iterator(); itr.hasNext();) {
            s += "<LI>" + itr.next().toString() + "<BR>";
        }
        if (!_focalPlaneMask.equals(NiriParameters.NO_SLIT))
            s += "<LI>Focal Plane Mask: " + _focalPlaneMask + "\n";
        s += "<LI>Read Mode: " + _readNoise + "\n";
        s += "<LI>Detector Bias: " + _wellDepth + "\n";

        s += "<BR>Pixel Size: " + getPixelSize() + "<BR>";

        return s;
    }
}
