package edu.gemini.spModel.gemini.niri;

import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.data.SuggestibleString;
import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY;
import edu.gemini.spModel.type.*;

import java.beans.PropertyEditorManager;
import java.beans.PropertyEditorSupport;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides data types for the NIRI components.
 */
public final class Niri {
    private Niri() {
        // defeat construction
    }

    /**
     * Cameras
     */
    public enum Camera implements StandardSpType {

        F6("f/6", "(0.12 arcsec/pix)", "f6", Size.F6),
        F14("f/14", "(0.05 arcsec/pix)", "f14", Size.F14),
        F32("f/32", "(0.02 arcsec/pix)", "f32", Size.F32),
        F32_PV("f/32 + pupil viewer", "", "f32_pv", Size.F32),;

        public interface Size {
            double F6 = 119.91;
            double F14 = 51.10;
            double F32 = 22.43;
        }

        // OT-368: Take ROI into account:
        //Subarray      f/6         f/14        f/32
        //--------      ---         ----        ----
        //1024 (full)   119.91"     51.10"      22.43"
        //768           89.93"      38.32"      16.82"
        //512           59.96"      25.55"      11.21"
        //256           29.98"      12.77"      5.61"
        //
        // The other subarray is the "Spectroscopy 1024x512", which as the name suggests,
        // is never used for imaging, and therefore the size should not be important (the
        // slit defines the TPE size).


        /**
         * The default Camera value *
         */
        public static final Camera DEFAULT = F6;

        private final String _displayValue;
        private final String _description;
        private final String _logValue;

        // The default science area size for the camera.  Values is "height".
        private final double _height;

        Camera(final String displayVal, final String desc, final String logValue, final double height) {
            _displayValue = displayVal;
            _description = desc;
            _logValue = logValue;

            // No new copy needed since constructor is private
            _height = height;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String description() {
            return _description;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String logValue() {
            return _logValue;
        }

        /**
         * Return the science area height using the given ROI
         */
        public double getScienceAreaHeight(ROIDescription roi) {
            return getScienceAreaHeight(_height, roi);
        }

        /**
         * Return the science area height based on the given default height and ROI
         */
        public static double getScienceAreaHeight(double height, ROIDescription roi) {
            return height * roi.getYSize() / BuiltinROI.DefaultRoi.SIZE;
        }

        /**
         * Return a Camera by index
         */
        public static Camera getCameraByIndex(int index) {
            return SpTypeUtil.valueOf(Camera.class, index, DEFAULT);
        }

        /**
         * Return a Camera by name
         */
        static public Camera getCamera(String name) {
            return getCamera(name, DEFAULT);
        }

        /**
         * Return a Camera by name giving a value to return upon error
         */
        static public Camera getCamera(String name, Camera nvalue) {
            return SpTypeUtil.oldValueOf(Camera.class, name, nvalue);
        }
    }


    /**
     * Beam Splitter
     */
    public enum BeamSplitter implements DisplayableSpType, SequenceableSpType {

        same_as_camera("same as camera", 0.),
        f6("f/6", Camera.Size.F6),
        f14("f/14", Camera.Size.F14),
        f32("f/32", Camera.Size.F32),;

        public static final BeamSplitter DEFAULT = same_as_camera;

        private final String _displayValue;
        private final double _height;

        BeamSplitter(final String displayVal, final double height) {
            _displayValue = displayVal;
            _height = height;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /**
         * Return a BeamSplitter by index
         */
        public static BeamSplitter getBeamSplitterByIndex(int index) {
            return SpTypeUtil.valueOf(BeamSplitter.class, index, DEFAULT);
        }

        /**
         * Return a BeamSplitter by name giving a value to return upon error
         */
        public static BeamSplitter getBeamSplitter(String name, BeamSplitter nvalue) {
            return SpTypeUtil.oldValueOf(BeamSplitter.class, name, nvalue);
        }

        /**
         * Return the science area height
         */
        public double getScienceAreaHeight(Camera c, ROIDescription roi) {
            if (this == same_as_camera) {
                return c.getScienceAreaHeight(roi);
            }
            return Camera.getScienceAreaHeight(_height, roi);
        }

        /**
         * Return a Camera object for the given BeamSplitter object.
         * The camera argument is returned if the beam splitter is
         * set to SAME_AS_CAMERA.
         */
        static public Camera getCamera(BeamSplitter bs, Camera camera) {
            if (bs == f6) {
                return Camera.F6;
            }
            if (bs == f14) {
                return Camera.F14;
            }
            if (bs == f32) {
                return Camera.F32;
            }
            return camera;
        }
    }

    /**
     * Dispersers
     */
    public enum Disperser implements DisplayableSpType, SequenceableSpType, LoggableSpType {

        NONE("none", 0.0, "none"),
        J("J-grism f/6", 1.20, "Jgrism"),
        H("H-grism f/6", 1.65, "Hgrism"),
        K("K-grism f/6", 2.20, "Kgrism"),
        L("L-grism f/6", 3.50, "Lgrism"),
        M("M-grism f/6", 4.80, "Mgrism"),
        WOLLASTON("Wollaston", -1.0, "Woll"),
        J_F32("J-grism f/32", J.getCentralWavelength(), "Jgrism"),
        H_F32("H-grism f/32", H.getCentralWavelength(), "Hgrism"),
        K_F32("K-grism f/32", K.getCentralWavelength(), "Kgrism"),;

        private static final Map<String, Disperser> CONVERTER = new HashMap<>();

        static {
            CONVERTER.put("H-grism", H);
            CONVERTER.put("J-grism", J);
            CONVERTER.put("K-grism", K);
            CONVERTER.put("L-grism", L);
            CONVERTER.put("M-grism", M);
        }

        /**
         * The default Disperser value *
         */
        public static final Disperser DEFAULT = NONE;

        // String value for central wavelength, used by TCC
        private final double _wavelength;

        // The log value for this disperser
        private final String _displayValue;
        private final String _logValue;

        Disperser(final String displayValue, final double wavelength, final String logValue) {
            _displayValue = displayValue;
            _wavelength = wavelength;
            _logValue = logValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String logValue() {
            return _logValue;
        }

        /**
         * Return the value for the central wavelength
         */
        public double getCentralWavelength() {
            return _wavelength;
        }

        /**
         * Return a string representation of the wavelength
         */
        public String getCentralWavelengthAsString() {
            String wavelength;
            try {
                if (_wavelength < 0) {
                    wavelength = "?";
                } else if (_wavelength == 0.0) {
                    wavelength = "";
                } else {
                    wavelength = String.valueOf(_wavelength);
                }
            } catch (NumberFormatException e) {
                wavelength = "";
            }
            return wavelength;
        }

        /**
         * Return a Disperser by index *
         */
        static public Disperser getDisperserByIndex(int index) {
            return SpTypeUtil.valueOf(Disperser.class, index, DEFAULT);
        }

        /**
         * Return a Disperser by name *
         */
        static public Disperser getDisperser(String name) {
            return getDisperser(name, DEFAULT);
        }

        /**
         * Return a Disperser by name giving a value to return upon error *
         */
        static public Disperser getDisperser(String name, Disperser nvalue) {
            // XXX for backward compatibility
            Disperser d = CONVERTER.get(name);
            if (d != null) return d;
            return SpTypeUtil.oldValueOf(Disperser.class, name, nvalue);
        }
    }

    /**
     * Read Mode
     */
    public enum ReadMode implements StandardSpType {

//In NIRI sequence component change readmode choices to be the same as in the NIRI
// component (with some abbreviation). They should be
//        - 1-2.5um: faint obj narrowband im/sp
//        - 1-2.5um: JHK & bright obj narrowband im/sp
//        - 3-5um: imaging/spectroscopy (no change)
//07/14/04

// Detailed description : I don't know how we missed this for so long, but the read noises listed for
//   the 1-2.5um JHK/bright narrow band and for 3-5um should be 35 e- and 70 e-,
//   not the 50 e- and 200 e- currently listed. I just got an email from an outside
//   observer pointing out the discrepancy between these values and the ones on the
//   web pages.
//05/02/05

// OT-365: Correct the minimum exposure times (in seconds) for subarrays: (XXX?)
//Array     LowRN   MedRN   HighRN
//1024      8.762   0.548   0.179
//768       4.980   0.313   0.106
//512       2.276   0.144   0.052
//256       0.654   0.043   0.02

        IMAG_SPEC_NB(
                "1-2.5um: Faint Object Narrow-band Imaging/Spectroscopy",
                "low background", 12.0,
                new double[]{0.654, 2.276, 4.980, 8.762},
                ">44 sec", "LowRN"),

        IMAG_1TO25(
                "1-2.5um: JHK and Bright Object Narrow-band Imaging/Spectroscopy",
                "high background", 35.0,
                new double[]{0.043, 0.144, 0.313, 0.548},
                ">2.7 sec", "MedRN"),

        IMAG_SPEC_3TO5(
                "3-5um: Imaging/Spectroscopy",
                "highest flux/thermal", 70.0,
                new double[]{0.02, 0.052, 0.106, 0.179},
                ">0.9 sec", "HiRN"),;

        /**
         * The default ReadMode value *
         */
        public static final ReadMode DEFAULT = IMAG_1TO25;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "readMode");

        private final String _displayValue;
        private final String _description;
        private final double _readNoise;
        private final double[] _minExp;
        private final String _recommendedExp;
        private final String _logValue;

        // Create a ReadMode with the given name and description.
        // The array of min exposure times (minExp) corresponds to the ROI array
        // sizes 256,512,768,1024. There should be one value in the array for
        // each of these at indexes 0,1,2,3.
        // The recommened exposure time is just for display (XXX should it be
        // an array also?)
        ReadMode(final String displayValue, final String description, final double readNoise,
                         final double[] minExp, final String recommendedExp, final String logValue) {
            _displayValue = displayValue;
            _description = description;
            _readNoise = readNoise;
            _minExp = minExp;
            _recommendedExp = recommendedExp;
            _logValue = logValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String description() {
            return _description;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String logValue() {
            return _logValue;
        }

        /**
         * Return a ReadMode by name *
         */
        static public ReadMode getReadMode(String name) {
            return getReadMode(name, DEFAULT);
        }

        /**
         * Return a ReadMode by name giving a value to return upon error *
         */
        static public ReadMode getReadMode(String name, ReadMode nvalue) {

            // XXX for backward compatibility
            switch (name) {
                case "narrow-band imaging/spectroscopy":
                    return IMAG_SPEC_NB;
                case "1-2.5 um imaging":
                    return IMAG_1TO25;
                case "3-5um imaging/spectroscopy":
                    return IMAG_SPEC_3TO5;
            }

            return SpTypeUtil.oldValueOf(ReadMode.class, name, nvalue);
        }

        public double getReadNoise() {
            return _readNoise;
        }

        /**
         * Return the min exposure time for this read mode and the given ROI
         */
        public double getMinExp(ROIDescription roi) {
            int index = roi.getXSize() / 256 - 1;
            return _minExp[index];
        }

        /**
         * Return the min exposure time for this read mode and the given ROI as a string
         */
        public String getMinExpAsString(ROIDescription roi) {
            return String.valueOf(getMinExp(roi)) + " sec";
        }


        /**
         * Return the recommended minimum exposure time.
         */
        public String getRecommendedMinExp() {
            return _recommendedExp;
        }
    }

    /**
     * Masks
     */
    public enum Mask implements StandardSpType {

        // Note the f/6 value is given for the MASK_NONE, which is sometimes wrong.
        MASK_IMAGING("imaging",
                Size.IMAGING_SIZE, Size.IMAGING_SIZE, "imaging"),
        MASK_1("f/6 2-pix slit center",
                Size.F6_2PIX_CENTERED_WIDTH, Size.MASK_1_HEIGHT, "F6-2pix-cen"),
        MASK_4("f/6 2-pix slit blue",
                Size.F6_2PIX_BLUE_WIDTH, Size.MASK_4_HEIGHT, "2bl"),
        MASK_2("f/6 4-pix slit center",
                Size.F6_4PIX_CENTERED_WIDTH, Size.MASK_2_HEIGHT, "F6-4pix-cen"),
        MASK_5("f/6 4-pix slit blue",
                Size.F6_4PIX_BLUE_WIDTH, Size.MASK_5_HEIGHT, "4bl"),
        MASK_3("f/6 6-pix slit center",
                Size.F6_6PIX_CENTERED_WIDTH, Size.MASK_3_HEIGHT, "F6-6pix-cen"),
        MASK_6("f/6 6-pix slit blue",
                Size.F6_6PIX_BLUE_WIDTH, Size.MASK_6_HEIGHT, "6bl"),
        MASK_7("Polarimetry 1-2.5 um",
                Size.MASK_7_HEIGHT, Size.MASK_7_HEIGHT, "p1-2.5"),
        MASK_8("Polarimetry 3-5 um",
                Size.MASK_8_HEIGHT, Size.MASK_8_HEIGHT, "p3-5"),
        MASK_9("f/32 4-pix slit center",
                Size.MASK_F32_4PIX_CENTERED_WIDTH, Size.MASK_9_HEIGHT, "f32-4cen"),
        MASK_10("f/32 7-pix slit center",
                Size.MASK_F32_7PIX_CENTERED_WIDTH, Size.MASK_10_HEIGHT, "f32-7cen"),
        MASK_11("f/32 10-pix slit center",
                Size.MASK_F32_10PIX_CENTERED_WIDTH, Size.MASK_11_HEIGHT, "f32-10cen"),
        PINHOLE_MASK("Pinhole",
                Size.IMAGING_SIZE, Size.IMAGING_SIZE, "pinhole"),;

        public interface Size {
            double F6_2PIX_CENTERED_WIDTH = 0.226;
            double F6_4PIX_CENTERED_WIDTH = 0.470;
            double F6_6PIX_CENTERED_WIDTH = 0.750;

            double F6_2PIX_BLUE_WIDTH = 0.226;
            double F6_4PIX_BLUE_WIDTH = 0.409;
            double F6_6PIX_BLUE_WIDTH = 0.696;

            double MASK_F32_4PIX_CENTERED_WIDTH = 0.10;
            double MASK_F32_7PIX_CENTERED_WIDTH = 0.144;
            double MASK_F32_10PIX_CENTERED_WIDTH = 0.22;

            // The following is a special case for two masks that Joe added in
            // Summer 2003
            double F6_SPECIAL_SIZE = 110.0;

            double IMAGING_SIZE = Camera.Size.F6;

            double MASK_1_HEIGHT = Camera.Size.F14;
            double MASK_2_HEIGHT = F6_SPECIAL_SIZE;
            double MASK_3_HEIGHT = F6_SPECIAL_SIZE;

            double MASK_4_HEIGHT = Camera.Size.F14;
            double MASK_5_HEIGHT = Camera.Size.F14;
            double MASK_6_HEIGHT = Camera.Size.F14;

            double MASK_7_HEIGHT = Camera.Size.F6;
            double MASK_8_HEIGHT = Camera.Size.F14;

            double MASK_9_HEIGHT = Camera.Size.F32;
            double MASK_10_HEIGHT = Camera.Size.F32;
            double MASK_11_HEIGHT = Camera.Size.F32;
        }

        private static final Map<String, Mask> CONVERTER = new HashMap<>();

        static {
            CONVERTER.put("f/32 6-pix slit center", MASK_9);
            CONVERTER.put("f/32 9-pix slit center", MASK_10);
            CONVERTER.put("6-pix slit center", MASK_3);
            CONVERTER.put("2-pix slit blue", MASK_4);
            CONVERTER.put("2-pix slit center", MASK_1);
            CONVERTER.put("4-pix slit blue", MASK_5);
            CONVERTER.put("4-pix slit center", MASK_2);
            CONVERTER.put("6-pix slit blue", MASK_6);

        }


        /**
         * The default Mask value *
         */
        public static final Mask DEFAULT = MASK_IMAGING;

        // The internal values of height and width for this instance of Mask
        private final double _width;
        private final double _height;
        private final String _displayValue;
        private final String _logValue;

        Mask(final String displayValue, final double width, final double height, final String logValue) {
            _displayValue = displayValue;
            _width = width;
            _height = height;
            _logValue = logValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String description() {
            return "";
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String logValue() {
            return _logValue;
        }

        /**
         * Return a Mask by index
         */
        static public Mask getMaskByIndex(int index) {
            return SpTypeUtil.valueOf(Mask.class, index, DEFAULT);
        }

        /**
         * Return the width for this mask
         */
        public double getWidth() {
            return _width;
        }

        /**
         * Return the height for this mask based on the given ROI
         */
        public double getHeight(ROIDescription roi) {
            return Camera.getScienceAreaHeight(_height, roi);
        }

        /**
         * Return a Mask by name
         */
        static public Mask getMask(String name) {
            return getMask(name, DEFAULT);
        }

        /**
         * Return a Mask by name giving a value to return upon error
         */
        static public Mask getMask(String name, Mask nvalue) {
            // XXX for backward compatibility
            Mask m = CONVERTER.get(name);
            if (m != null) return m;
            return SpTypeUtil.oldValueOf(Mask.class, name, nvalue);
        }
    }

    /**
     * Class for Filters.
     */
    public enum Filter implements StandardSpType, ObsoletableSpType {

        // broadband
        BBF_Y("Y", "Y (1.02 um)", "Y", 1.02, Type.broadband),
        BBF_J("J", "J (1.25 um)", "J", 1.25, Type.broadband),
        BBF_H("H", "H (1.65 um)", "H", 1.65, Type.broadband),
        BBF_KPRIME("K(prime)", "K(prime) (2.12 um)", "Kprime", 2.12, Type.broadband),
        BBF_KSHORT("K(short)", "K(short) (2.15 um)", "Kshort", 2.15, Type.broadband),
        BBF_K("K", "K (2.20 um)", "K", 2.20, Type.broadband),
        BBF_LPRIME("L(prime)", "L(prime) (3.78 um)", "Lprime", 3.78, Type.broadband),
        BBF_MPRIME("M(prime)", "M(prime) (4.68 um)", "Mprime", 4.68, Type.broadband),
        BBF_J_ORDER_SORT("Order sorting J", "Order sorting J (1.30 um)", "J/OS", 1.30, Type.broadband),
        BBF_H_ORDER_SORT("Order sorting H", "Order sorting H (1.69 um)", "H/OS", 1.69, Type.broadband),
        BBF_K_ORDER_SORT("Order sorting K", "Order sorting K (2.20 um)", "K/OS", 2.20, Type.broadband),
        BBF_L_ORDER_SORT("Order sorting L", "Order sorting L (3.50 um)", "L/OS", 3.50, Type.broadband),
        BBF_M_ORDER_SORT("Order sorting M", "Order sorting M (5.00 um)", "M/OS", 5.00, Type.broadband),

        // narrowband
        J_CONTINUUM_106("J-continuum (1.065 um)", "J-continuum (1.065 um)", "J-cont(1.065)", 1.065, Type.narrowband) {
            @Override public boolean isObsolete() { return true; }
        },
        NBF_HEI("HeI", "HeI (1.083 um)", "HeI", 1.083, Type.narrowband),
        NBF_PAGAMMA("Pa(gamma)", "Pa(gamma) (1.094 um)", "Pa-gam", 1.094, Type.narrowband),
        J_CONTINUUM_122("J-continuum (1.122 um)", "J-continuum (1.122 um)", "J-cont(1.122)", 1.122, Type.narrowband) {
            @Override public boolean isObsolete() { return true; }
        },
        NBF_H("J-continuum(1.207)", "J-continuum (1.207 um)", "J-cont", 1.207, Type.narrowband),
        NBF_PABETA("Pa(beta)", "Pa(beta) (1.282 um)", "Pa-beta", 1.282, Type.narrowband),
        NBF_HCONT("H-continuum(1.57)", "H-continuum (1.570 um)", "H-cont", 1.570, Type.narrowband),
        NBF_CH4SHORT("CH4(short)", "CH4(short) (1.56 um)", "CH4short", 1.56, Type.narrowband),
        NBF_CH4LONG("CH4(long)", "CH4(long) (1.70 um)", "CH4long", 1.70, Type.narrowband),
        NBF_FEII("[FeII]", "[FeII] (1.644 um)", "[FeII]", 1.644, Type.narrowband),
        NBF_H2O_2045("H2O ice (2.045)", "H2O ice (2.045 um)", "H2O-2045", 2.045, Type.narrowband),
        NBF_HE12P2S("HeI (2p2s)", "HeI (2p2s) (2.059 um)", "HeI2p2s", 2.059, Type.narrowband),
        NBF_KCONT1("K-continuum(2.09)", "K-continuum (2.09 um)", "K-cont", 2.09, Type.narrowband),
        NBF_H210("H2 1-0 S(1)", "H2 1-0 S(1) (2.122 um)", "H2 1-0", 2.122, Type.narrowband),
        NBF_BRGAMMA("Br(gamma)", "Br(gamma) (2.166 um)", "Brgamma", 2.166, Type.narrowband),
        NBF_H221("H2 2-1 S(1)", "H2 2-1 S(1) (2.248 um)", "H2 2-1", 2.248, Type.narrowband),
        NBF_KCONT2("K-continuum(2.27)", "K-continuum (2.27 um)", "Kcont227", 2.27, Type.narrowband),
        NBF_CH4ICE("CH4 ice (2.275)", "CH4 ice (2.275 um)", "CH4ice", 2.275, Type.narrowband),
        NBF_CO20("CO 2-0 (bh)", "CO 2-0 (bh) (2.294 um)", "CO 2-0", 2.294, Type.narrowband),
        NBF_CO31("CO 3-1 (bh)", "CO 3-1 (bh) (2.323 um)", "CO 3-1", 2.323, Type.narrowband),
        NBF_H2O("H2O ice (3.050)", "H2O ice (3.050 um)", "H2O", 3.050, Type.narrowband) {
            public String sequenceValue() {
                return "H2O ice";
            }
        },
        NBF_HC("hydrocarbon", "hydrocarbon (3.295 um)", "hy-car", 3.295, Type.narrowband),
        NBF_BRACONT("Br(alpha) cont", "Br(alpha) cont (3.990 um)", "BrA-399", 3.990, Type.narrowband),
        NBF_BRA("Br(alpha)", "Br(alpha) (4.052 um)", "BrA-405", 4.052, Type.narrowband),;

        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "filter");

        public enum Type {
            broadband,
            narrowband,;
        }

        private final String _displayValue;
        private final String _description;
        private final String _logValue;
        private final double _wavelength;
        private final Type _type;

        Filter(String displayValue, String desc, String logValue,
                       double wavelength, Type type) {
            _displayValue = displayValue;
            _description = desc;
            _logValue = logValue;
            _wavelength = wavelength;
            _type = type;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String description() {
            return _description;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String logValue() {
            return _logValue;
        }

        public Type type() {
            return _type;
        }

        /**
         * The default Filter value
         */
        public static Filter DEFAULT = BBF_H;

        /**
         * Return the Filter by searching through the known types.
         */
        public static Filter getFilter(String name) {
            return getFilter(name, DEFAULT);
        }


        /**
         * Return the effective wavelength
         */
        public double getWavelength() {
            return _wavelength;
        }

        /**
         * Compute a String representation of the wavelength
         */
        public String getWavelengthAsString() {
            String wavelength;
            try {
                wavelength = String.valueOf(_wavelength);
            } catch (NumberFormatException e) {
                wavelength = "";
            }
            return wavelength;
        }

        /**
         * Return the Filter by searching through the known types.
         * The nvalue is return upon failure.
         */
        public static Filter getFilter(String name, Filter nvalue) {
            // backward compat after name change
            if ("J_CONTINUUM_112".equals(name)) return J_CONTINUUM_122;
            if (name.equals("J-continuum (1.12 um)")) {
                return J_CONTINUUM_122;
            }
            if (name.equals("J-continuum (1.06 um)")) {
                return J_CONTINUUM_106;
            }

            return SpTypeUtil.oldValueOf(Filter.class, name, nvalue);
        }

        public static Filter getFilterByIndex(int index) {
            return SpTypeUtil.valueOf(Filter.class, index, DEFAULT);
        }
    }


    /**
     * This class provides a description and storage for a description of a
     * Region of Interest.  Currently it is like the NIRI class but lacks
     * binning.  This could be refactored and shared I suppose.
     */
    public static final class ROIDescription implements Serializable {

        private final int _xSize;
        private final int _ySize;

        /**
         * Constructor for an ROIDescription takes
         * an x and y size in unbinned pixels.
         */
        public ROIDescription(final int xSize, final int ySize) {
            _xSize = xSize;
            _ySize = ySize;
        }

        /**
         * A copy constructor for creating copies of an already
         * existing ROIDescription.
         */
        public ROIDescription(final ROIDescription roid) {
            this(roid._xSize, roid._ySize);
        }

        /**
         * Return the x size in unbinned pixels.
         */
        public int getXSize() {
            return _xSize;
        }

        /**
         * Return the y size in unbinned pixels.
         */
        public int getYSize() {
            return _ySize;
        }
    }

    /**
     * BuiltInROI is a class to select from a small set of
     * selected Regions of Interest.  Currently, NIRI supports only
     * the full detector.
     */
    public enum BuiltinROI implements DisplayableSpType, LoggableSpType, SequenceableSpType {

        FULL_FRAME("full frame readout", DefaultRoi.DESCRIPTION, "full"),
        CENTRAL_768("central 768x768", new ROIDescription(768, 768), "c768"),
        CENTRAL_512("central 512x512", new ROIDescription(512, 512), "c512"),
        CENTRAL_256("central 256x256", new ROIDescription(256, 256), "c256"),
        SPEC_1024_512("spectroscopy 1024x512", new ROIDescription(1024, 512), "spec"),;

        // The default ROI size (width, height)
        interface DefaultRoi {
            int SIZE = 1024;
            ROIDescription DESCRIPTION = new ROIDescription(SIZE, SIZE);
        }

        public static final BuiltinROI DEFAULT = FULL_FRAME;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "builtinROI");

        private final ROIDescription _roid;
        private final String _displayValue;
        private final String _logValue;

        BuiltinROI(final String displayValue, final ROIDescription roid, final String logValue) {
            _displayValue = displayValue;
            _roid = roid;
            _logValue = logValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String logValue() {
            return _logValue;
        }

        /**
         * Return a BuiltinROI by name giving a value to return upon error *
         */
        public static BuiltinROI getBuiltinROI(String name, BuiltinROI nvalue) {
            return SpTypeUtil.oldValueOf(BuiltinROI.class, name, nvalue);
        }

        /**
         * Return a BuiltinROI and return the default value if not found *
         */
        public static BuiltinROI getBuiltinROI(String name) {
            return getBuiltinROI(name, DEFAULT);
        }

        /**
         * Return this BuiltinROIs ROIDescription.
         */
        public ROIDescription getROIDescription() {
            return new ROIDescription(_roid);
        }
    }

    /**
     * Well Depth
     */
    public enum WellDepth implements DisplayableSpType, DescribableSpType, SequenceableSpType {

        SHALLOW("shallow well", "1-2.5 um only", 200000.0, 123000.0),
        DEEP   ("deep well",    "3-5 um",        280000.0, 184500.0);

        /**
         * The default WellDepth value *
         */
        public static final WellDepth DEFAULT = SHALLOW;

        private final String displayValue;
        private final String description;
        private final double wellDepth;
        private final double linearityLimit;

        WellDepth(final String displayValue, final String description, final double wellDepth, final double linearityLimit) {
            this.displayValue   = displayValue;
            this.description    = description;
            this.wellDepth      = wellDepth;
            this.linearityLimit = linearityLimit;
        }

        public String displayValue() {
            return displayValue;
        }

        public String description() {
            return description;
        }

        public double depth() {
            return wellDepth;
        }

        public double linearityLimit() {
            return linearityLimit;
        }

        public String sequenceValue() {
            return displayValue;
        }

        /**
         * Return a WellDepth by name *
         */
        static public WellDepth getWellDepth(String name) {
            return getWellDepth(name, DEFAULT);
        }

        /**
         * Return a WellDepth by name giving a value to return upon error *
         */
        static public WellDepth getWellDepth(String name, WellDepth nvalue) {
            return SpTypeUtil.oldValueOf(WellDepth.class, name, nvalue);
        }
    }


    /**
     * Focus
     */
    public enum FocusSuggestion implements DisplayableSpType, SequenceableSpType {

        BEST_FOCUS("best focus"),;

        /**
         * The default Focus value *
         */
        public static final FocusSuggestion DEFAULT = BEST_FOCUS;

        private final String _displayValue;

        FocusSuggestion(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String toString() {
            return _displayValue;
        }

    }

    public static class Focus extends SuggestibleString {
        public Focus() {
            super(FocusSuggestion.class);
            setStringValue(FocusSuggestion.DEFAULT.displayValue());
        }
    }

    public static class FocusEditor extends PropertyEditorSupport {
        public Object getValue() {
            Focus f = (Focus) super.getValue();
            if (f == null) return null;

            Focus res = new Focus();
            res.setStringValue(f.getStringValue());
            return res;
        }

        public void setValue(Object value) {
            Focus f = (Focus) value;
            Focus cur = (Focus) super.getValue();
            if (cur == null) {
                cur = new Focus();
                super.setValue(cur);
            }
            cur.setStringValue(f.getStringValue());
        }

        public String getAsText() {
            Focus val = (Focus) getValue();
            if (val == null) return null;
            return val.getStringValue();
        }

        public void setAsText(String string) throws IllegalArgumentException {
            Focus val = (Focus) super.getValue();
            if (val == null) {
                val = new Focus();
                super.setValue(val);
            }
            val.setStringValue(string);
        }
    }

    static {
        PropertyEditorManager.registerEditor(Focus.class, FocusEditor.class);
    }

}


