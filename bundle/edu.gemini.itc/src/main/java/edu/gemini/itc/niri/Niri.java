package edu.gemini.itc.niri;

import edu.gemini.itc.service.ObservationDetails;
import edu.gemini.itc.service.CalculationMethod;
import edu.gemini.itc.shared.*;

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
    private CalculationMethod _mode;

    /**
     * construct an Niri with specified Broadband filter or Narrowband filter.
     * grism, and camera type.
     */
    //public Niri(String FilterUsed, String grism, String camera,
    //       String readNoise, String wellDepth, String mode,
    //       String focalPlaneMask, String focalPlaneMaskOffset,
    //       String stringSlitWidth)
    //   throws Exception
    public Niri(NiriParameters np, ObservationDetails odp) {
        super(INSTR_DIR, FILENAME);

        _readNoise = np.getReadNoise();
        _wellDepth = np.getWellDepth();
        _focalPlaneMask = np.getFocalPlaneMask();
        _focalPlaneMaskOffset = np.getFPMaskOffset();
        _stringSlitWidth = np.getStringSlitWidth();
        _filterUsed = np.getFilter();
        _grism = np.getGrism();
        _camera = np.getCamera();
        _mode = odp.getMethod();


        if (!(_filterUsed.equals("none"))) {
            _Filter = Filter.fromFile(getPrefix(), _filterUsed, getDirectory() + "/");
            addFilter(_Filter);

            //The PK50 filter is used with many NIRI narrow-band filters but has
            //not been included until now (20100105).  Most of NIRI's filter curves don't
            //extend far enough for it to matter.
            //To do this right we should have full transmission curves for all filters
            //that are used with the PK50 and include them all.
            if (_filterUsed.equals("Y-G0241")) {
                _Filter2 = Filter.fromFile(getPrefix(), "PK50-fake", getDirectory() + "/");

                addComponent(_Filter2); // TODO: SHOULD THIS ONE LIMIT WAVELENGTHS, TOO?????
            }

        }

        FixedOptics test = new FixedOptics(getDirectory() + "/", getPrefix());
        addComponent(test);

        //Test to see that all conditions for Spectroscopy are met
        if (_mode.isSpectroscopy()) {
            if (_grism.equals("none"))
                throw new RuntimeException("Spectroscopy calculation method is selected but a grism" +
                        " is not.\nPlease select a grism and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
            if (_focalPlaneMask.equals(NiriParameters.NO_SLIT))
                throw new RuntimeException("Spectroscopy calculation method is selected but a focal" +
                        " plane mask is not.\nPlease select a " +
                        "grism and a " +
                        "focal plane mask in the Instrument " +
                        "configuration section.");
        }

        if (_mode.isImaging()) {
            if (!_grism.equals("none"))
                throw new RuntimeException("Imaging calculation method is selected but a grism" +
                        " is also selected.\nPlease deselect the " +
                        "grism or change the method to spectroscopy.");
            if (!_focalPlaneMask.equals("none"))
                throw new RuntimeException("Imaging calculation method is selected but a Focal" +
                        " Plane Mask is also selected.\nPlease " +
                        "deselect the Focal Plane Mask" +
                        " or change the method to spectroscopy.");
        }


        if (!(_grism.equals("none"))) {
            if (_camera.equals("F14") || _camera.equals("F32"))
                throw new RuntimeException("The " + _camera + " camera cannot be used in Spectroscopy" +
                        " mode.  \nPlease select the F6 camera and resubmit.");


            if (_camera.equals("F32") && (_grism.equals(NiriParameters.MGRISM) || _grism.equals(NiriParameters.LGRISM)))//||_camera.equals("F32"))
                throw new RuntimeException("The " + _camera + " camera cannot be used with L or M Band Spectroscopy" +
                        " mode.  \nPlease select a different band and resubmit.");


            if (_camera.equals("F32") && !_focalPlaneMaskOffset.equals("center"))
                throw new RuntimeException("The " + _camera + " camera must be used with the center slit.\n " +
                        "Please select a center slit and resubmit.");

            if (_camera.equals("F32") && !(_focalPlaneMask.equals(NiriParameters.F32_SLIT_4_PIX_CENTER) || _focalPlaneMask.equals(NiriParameters.F32_SLIT_7_PIX_CENTER) || _focalPlaneMask.equals(NiriParameters.F32_SLIT_10_PIX_CENTER)))
                throw new RuntimeException("The " + _focalPlaneMask + " slit cannot be used with the f/32 camera.\n " +
                        "Please select a f/32 compatable slit and resubmit.");

            if (!_camera.equals("F32") && (_focalPlaneMask.equals(NiriParameters.F32_SLIT_4_PIX_CENTER) || _focalPlaneMask.equals(NiriParameters.F32_SLIT_7_PIX_CENTER) || _focalPlaneMask.equals(NiriParameters.F32_SLIT_10_PIX_CENTER)))
                throw new RuntimeException("The " + _focalPlaneMask + " slit must be used with the f/32 camera.\n " +
                        "Please select the f/32 camera and resubmit.");


            _grismOptics = new GrismOptics(getDirectory() + "/", _grism, _camera,
                    _focalPlaneMaskOffset,
                    _stringSlitWidth);

            resetBackGround(INSTR_DIR, "spec_");  //Niri has spectroscopic scattering from grisms and needs
            addGrism(_grismOptics);
        }

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
    public String getDirectory() {
        return ITCConstants.LIB + "/" + INSTR_DIR;
    }

    // TODO: This mimics the result of the original convoluted code. (And is probably wrong?)
    // TODO: Verify with science and use unified getObservingStart() method from the base class.
    // TODO: Left as is for now to keep regression tests working.
    public double getObservingStart() {
        if (!(_grism.equals("none")) && !(_filterUsed.equals("none"))) {
            return Math.max(_Filter.getStart(), _grismOptics.getStart());
        } else if (!(_grism.equals("none"))) {
            return Math.max(getStart(), _grismOptics.getStart());
        } else if (!(_filterUsed.equals("none"))) {
            return _Filter.getStart();
        } else {
            return getStart();
        }
    }

    // TODO: This mimics the result of the original convoluted code. (And is probably wrong?)
    // TODO: Verify with science and use unified getObservingStart() method from the base class.
    // TODO: Left as is for now to keep regression tests working.
    public double getObservingEnd() {
        if (!(_grism.equals("none")) && !(_filterUsed.equals("none"))) {
            return Math.min(_Filter.getEnd(), _grismOptics.getEnd());
        } else if (!(_grism.equals("none"))) {
            return Math.max(getEnd(), _grismOptics.getEnd());
        } else if (!(_filterUsed.equals("none"))) {
            return _Filter.getEnd();
        } else {
            return getEnd();
        }
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
        for (Iterator itr = getComponents().iterator(); itr.hasNext(); ) {
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
