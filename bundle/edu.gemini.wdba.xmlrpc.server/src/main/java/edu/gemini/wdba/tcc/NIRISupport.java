package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.niri.Niri;
import edu.gemini.spModel.gemini.niri.NiriOiwfsGuideProbe;

import edu.gemini.spModel.gemini.niri.Niri.Camera;
import edu.gemini.wdba.session.Mode;
import edu.gemini.wdba.tcc.ObservationEnvironment.AoAspect;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class NIRISupport implements ITccInstrumentSupport {

    // Should be set somehow for different telescope configurations (property?)
    //private static final int NIRI_PORT = 3;

    private ObservationEnvironment _oe;

    // Private constructor
    private NIRISupport(ObservationEnvironment oe) throws NullPointerException {
        if (oe == null) throw new NullPointerException("Observation environment can not be null");
        _oe = oe;
    }

    /**
     * Factory for creating a new NIRI Instrument Support.
     *
     * @param oe The current ObservationEnvironment
     * @return A new instance
     * @throws NullPointerException returned if the ObservationEnvironment is null.
     */
    static public ITccInstrumentSupport create(ObservationEnvironment oe) throws NullPointerException {
        return new NIRISupport(oe);
    }

    /**
     * Returns the appropriate wavelength for the observation for the TCC config
     *
     * @return wavelenth in microns
     */
    public String getWavelength() {
        InstNIRI inst = (InstNIRI) _oe.getInstrument();

        Niri.Disperser disperser = inst.getDisperser();
        if (disperser == Niri.Disperser.NONE) {
            return inst.getFilter().getWavelengthAsString();
        }

        // Temp check for Wollaston - assuming WOLASTON should be same as filter
        if (disperser == Niri.Disperser.WOLLASTON) {
            return inst.getFilter().getWavelengthAsString();
        }

        // Else it's the central wavelength must convert to microns
        return disperser.getCentralWavelengthAsString();
    }

    /**
     * Return the position angle.
     *
     * @return a String value for the position angle
     */
    public String getPositionAngle() {
        InstNIRI inst = (InstNIRI) _oe.getInstrument();
        return inst.getPosAngleDegreesStr();
    }

    /**
     * Return the niri wavelength if needed name.
     */
    public void addGuideDetails(ParamSet p) {
        if (_oe.containsTargets(NiriOiwfsGuideProbe.instance)) {
            p.putParameter(TccNames.OIWFSWAVELENGTH, "NIRI OIWFS J");
        }
    }

    /**
     * Return the proper instrument config
     *
     * @return a String value for the config name
     */
    public String getTccConfigInstrument() {
        // Based upon the camera
        String DEFAULT = "NIRIF6P";

        InstNIRI inst = (InstNIRI) _oe.getInstrument();
        boolean isAltair = _oe.isAltair();

        Niri.Mask mask = inst.getMask();
        if (mask == Niri.Mask.MASK_IMAGING) {
            if (inst.getCamera() == Niri.Camera.F6) {
                return isAltair ? "AO2NIRIF6P" : "NIRIF6P";
            } else if (inst.getCamera() == Niri.Camera.F14) {
                return isAltair ? "AO2NIRIF14P" : "NIRIF14P";
            } else if (inst.getCamera() == Niri.Camera.F32) {
                return isAltair ? "AO2NIRIF32P" : "NIRIF32P";
            }
        }

        // Added Gely's feature to look at mask and return name as in http://internal.gemini.edu/science/instruments/NIRI/scifold_p3.html
        if (mask == Niri.Mask.MASK_1) {
            return "f6_2pixCen";
        } else if (mask == Niri.Mask.MASK_2) {
            return "f6_4pixCen";
        } else if (mask == Niri.Mask.MASK_3) {
            return "f6_6pixCen";
        } else if (mask == Niri.Mask.MASK_4) {
            return "f6_2pixBlu";
        } else if (mask == Niri.Mask.MASK_5) {
            return "f6_4pixBlu";
        } else if (mask == Niri.Mask.MASK_6) {
            return "f6_6pixBlu";
        }
        // Now look at altair choices
        if (!isAltair) return DEFAULT;

        if (mask == Niri.Mask.MASK_9) {
            return "f32_4/6pix";
        } else if (mask == Niri.Mask.MASK_10) {
            return "f32_7/9pix";
        } else if (mask == Niri.Mask.MASK_11) {
            return "f32_11/12pix";
        }

        // Else we just return something!
        return DEFAULT;
    }

    private static final class PointOrigKey {
        private final Camera camera;
        private final AoAspect ao;

        /**
         * If mode is null, assume all modes should match. Otherwise, must match on a specific mode.
         */
        PointOrigKey(Camera camera, AoAspect ao) {
            this.camera = camera;
            this.ao     = ao;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PointOrigKey that = (PointOrigKey) o;
            if (ao != that.ao) return false;
            if (camera != that.camera) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = camera != null ? camera.hashCode() : 0;
            result = 31 * result + (ao != null ? ao.hashCode() : 0);
            return result;
        }
    }

    private static final Map<PointOrigKey, String> POINT_ORIG_MAP;

    static {
        Map<PointOrigKey, String> m = new HashMap<PointOrigKey, String>();

        define(m, Camera.F6,  AoAspect.none, "nirif6p");
        define(m, Camera.F14, AoAspect.none, "nirif14p");
        define(m, Camera.F14, AoAspect.ngs,  "ngs2niri_f14");
        define(m, Camera.F14, AoAspect.lgs,  "lgs2niri_f14");
        define(m, Camera.F32, AoAspect.none, "nirif32p");
        define(m, Camera.F32, AoAspect.ngs,  "ngs2niri_f32");
        define(m, Camera.F32, AoAspect.lgs,  "lgs2niri_f32");

        POINT_ORIG_MAP = Collections.unmodifiableMap(m);
    }

    private static void define(Map<PointOrigKey, String> m, Camera c, AoAspect ao, String val) {
        m.put(new PointOrigKey(c, ao), val);
    }

    private static String lookupPointOrig(Camera c, AoAspect ao, AltairParams.Mode mode) {
        String val = POINT_ORIG_MAP.get(new PointOrigKey(c, ao));
        if (val != null && AltairParams.Mode.LGS_P1.equals(mode))
            val += "_p1";
        return val == null ? "unknown" : val;
    }

    /**
     * Support for instrument origins.
     *
     * @return String that is the name of a TCC config file.  See WDBA-5.
     */
    public String getTccConfigInstrumentOrigin() {
        InstNIRI inst          = (InstNIRI) _oe.getInstrument();
        Camera   c             = inst.getCamera();
        AoAspect ao            = _oe.getAoAspect();
        AltairParams.Mode mode = _oe.getAltairConfig() == null ? null : _oe.getAltairConfig().getMode();
        return lookupPointOrig(c, ao, mode);
    }

    /**
     * Return true if the instrument is using a fixed rotator position.  In this case the pos angle is used
     * in a special rotator config
     *
     * @return String value that is the name of the fixed rotator config or null if no special name is needed
     */
    public String getFixedRotatorConfigName() {
        return null;
    }

    /**
     * Returns the TCC chop parameter value.
     *
     * @return Chop value or null if there is no chop parameter for this instrument.
     */
    public String getChopState() {
        return TccNames.NOCHOP;
    }
}

