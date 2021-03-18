package edu.gemini.spModel.gemini.gnirs;

import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.data.SuggestibleString;
import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY;
import edu.gemini.spModel.type.*;
import edu.gemini.shared.util.immutable.*;

import java.beans.PropertyEditorManager;
import java.beans.PropertyEditorSupport;
import java.util.*;
import java.util.regex.Pattern;

/**
 * This class provides data types for the GNIRS components.
 */
public class GNIRSParams {

    // Make the constructor protected.
    protected GNIRSParams() {}

    /**
     * CrossDispersed values (yes/no).
     */
    public enum CrossDispersed implements DisplayableSpType, SequenceableSpType {

        NO("No"),
        SXD("SXD"),
        LXD("LXD"),
        ;

        /**
         * The default CrossDispersed value
         */
        public static CrossDispersed DEFAULT = NO;

        private String _displayValue;

        CrossDispersed(String displayValue) {
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

        /**
         * Return a CrossDispersed by name giving a value to return upon error
         */
        static public CrossDispersed getCrossDispersed(String name, CrossDispersed nvalue, PixelScale scale) {
            if ("YES".equalsIgnoreCase(name)) {
                return convertPre2010Xd(name, scale);
            } else {
                return SpTypeUtil.oldValueOf(CrossDispersed.class, name, nvalue);
            }
        }

        public static CrossDispersed convertPre2010Xd(String name, PixelScale scale) {
            CrossDispersed res = SpTypeUtil.noExceptionValueOf(CrossDispersed.class, name);
            if (res != null) return res;

            if ("YES".equalsIgnoreCase(name)) {
                switch (scale) {
                    case PS_005: return LXD;
                    case PS_015: return SXD;
                    default: return CrossDispersed.DEFAULT;
                }
            }
            return DEFAULT;
        }
    }

    /**
     * PixelScale choices
     */
    public enum PixelScale implements DisplayableSpType, SequenceableSpType {

        PS_015("0.15\"/pix", 0.15, CrossDispersed.NO, CrossDispersed.SXD),
        PS_005("0.05\"/pix", 0.05, CrossDispersed.values()),
        ;

        public final static PixelScale DEFAULT = PS_015;

        private double _pixelScale;
        private String _displayValue;
        private Set<CrossDispersed> _xdOptions;

        PixelScale(String displayValue, double pixelScale, CrossDispersed... xd) {
            _displayValue = displayValue;
            _pixelScale   = pixelScale;
            _xdOptions    = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(xd)));
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

        public Set<CrossDispersed> getXdOptions() {
            return _xdOptions;
        }

        /**
         * Return a PixelScale by name
         */
        static public PixelScale getPixelScale(String name) {
            return getPixelScale(name, DEFAULT);
        }

        /**
         * Return a PixelScale by name, giving a value to return upon error
         */
        static public PixelScale getPixelScale(String name, PixelScale nvalue) {
            return SpTypeUtil.oldValueOf(PixelScale.class, name, nvalue);
        }

        /**
         * Return a PixelScale by index
         */
        static public PixelScale getPixelScaleByIndex(int index) {
            return SpTypeUtil.valueOf(PixelScale.class, index, DEFAULT);
        }

        public double getValue() {
            return _pixelScale;
        }
    }

    /**
     * SlitWidth choices
     */
    public enum SlitWidth implements DisplayableSpType, SequenceableSpType, LoggableSpType, ObsoletableSpType {

        SW_1("0.10 arcsec", 0.10, "0.10"),
        SW_2("0.15 arcsec", 0.15, "0.15"),
        SW_3("0.20 arcsec", 0.20, "0.20"),
        SW_4("0.30 arcsec", 0.30, "0.30"),
        SW_5("0.45 arcsec", 0.45, "0.45"),
        SW_6("0.675 arcsec", 0.675, "0.675"),
        SW_7("1.0 arcsec", 1.0, "1.0"),
        SW_8("3.0 arcsec", 3.0, "3.0") {
            @Override public boolean isObsolete() { return true; }
        },
        IFU("IFU", 3.15, "IFU") {
            @Override public boolean isObsolete() { return true; }
        },
        LR_IFU("LR-IFU", 3.15, "LR-IFU"),
        HR_IFU("HR-IFU", 1.25, "HR-IFU"),
        ACQUISITION("acquisition", "ACQ"),
        PUPIL_VIEWER("pupil viewer", "PV"),
        PINHOLE_1("pinhole 0.1", "SmPin"),
        PINHOLE_3("pinhole 0.3", "LgPin"),
        ;

        public final static SlitWidth DEFAULT = SW_4;

        private interface Defaults {
            double DEFAULT_SLIT_WIDTH = 4.33;
        }

        private final double _slitWidth;
        private final String _displayValue;
        private final String _logValue;

        SlitWidth(String displayValue, double slitWidth, String logValue) {
            _displayValue = displayValue;
            _slitWidth = slitWidth;
            _logValue = logValue;
        }

        // Note hardcoded default slit width because one can't use static in enum constructor
        SlitWidth(String displayValue, String logValue) {
            this(displayValue, Defaults.DEFAULT_SLIT_WIDTH, logValue);
        }

        /**
         * Return the special log value for this item *
         */
        public String logValue() {
            return _logValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public double getValue() {
            return _slitWidth;
        }

        public String toString() {
            return _displayValue;
        }

        /**
         * Return a SlitWidth by name
         */
        static public SlitWidth getSlitWidth(String name) {
            return getSlitWidth(name, DEFAULT);
        }

        /**
         * Return a SlitWidth by name, giving a value to return upon error
         */
        static public SlitWidth getSlitWidth(String name, SlitWidth nvalue) {
            return SpTypeUtil.oldValueOf(SlitWidth.class, name, nvalue);
        }

        /**
         * Test to see if we're doing slit spectroscopy.
         */
        public boolean isSlitSpectroscopy() {
            switch (this) {
                case SW_1:
                case SW_2:
                case SW_3:
                case SW_4:
                case SW_5:
                case SW_6:
                case SW_7:
                case SW_8:
                    return true;
                default:
                    return false;
            }
        }

        public boolean isIfu() {
            final boolean result;
            switch (this) {
                case IFU:
                case LR_IFU:
                case HR_IFU:
                    result = true;
                    break;
                default:
                    result = false;
            }
            return result;
        }
    }

    /**
     * Disperser choices
     */
    public enum Disperser implements DisplayableSpType, SequenceableSpType, LoggableSpType {
        //For the disperser, to do the conversion you will need to check pixel scale.
        //The translation is:
        //
        //for 0.05" pixel scale:
        //2000 = 10 l/mm
        //6000 = 32 l/mm
        //18000 = 110 l/mm
        //
        //for 0.15" pixel scale:
        //700 = 10
        //2000 = 32
        //6000 = 110
        //
        //-Bernadette

        D_10("10 l/mm grating", 10, 2000, 700, "10"),
        D_32("32 l/mm grating", 32, 6000, 2000, "32"),
        D_111("111 l/mm grating", 111, 18000, 6000, "111"),
        ;

        public final static Disperser DEFAULT = D_32;

        // The grating value
        private int _value;
        private String _displayValue;

        // Used in the observing log
        private String _logValue;

        // The spectral resolution when pixel scale is 0.05"/pix
        private int _sr005;

        // The spectral resolution when pixel scale is 0.15"/pix
        private int _sr015;

        // Initialize with the name, grating value and spectral resolutions at
        // 0.05 and 0.15 "/pix
        Disperser(String displayValue, int value, int sr005, int sr015, String logValue) {
            _displayValue = displayValue;
            _value = value;
            _sr005 = sr005;
            _sr015 = sr015;
            _logValue = logValue;
        }

        /**
         * Return the special log value for this item *
         */
        public String logValue() {
            return _logValue;
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

        /**
         * Return a Disperser by name
         */
        static public Disperser getDisperser(String name) {
            return getDisperser(name, DEFAULT);
        }

        /**
         * Return a Disperser by name, giving a value to return upon error
         */
        static public Disperser getDisperser(String name, Disperser nvalue) {
            return SpTypeUtil.oldValueOf(Disperser.class, name, nvalue);
        }

        /**
         * Return a Disperser by name, giving a value to return upon error.
         * This method is temporary, for backward compatibility. By passing
         * in the PixelScale and the name, we can convert the old names to the
         * new values.
         */
        static Disperser getDisperser(String name, Disperser nvalue, PixelScale ps) {
            if (ps.equals(PixelScale.PS_005)) {
                if (name.equals("700")) {
                    return D_10;
                }
                if (name.equals("2000")) {
                    return D_10;
                }
                if (name.equals("6000")) {
                    return D_32;
                }
                if (name.equals("18000")) {
                    return D_111;
                }
            } else {
                if (name.equals("700")) {
                    return D_10;
                }
                if (name.equals("2000")) {
                    return D_32;
                }
                if (name.equals("6000")) {
                    return D_111;
                }
                if (name.equals("18000")) {
                    return D_111;
                }
            }
            return getDisperser(name, nvalue);
        }

        /**
         * Return a Disperser by index
         */
        static public Disperser getDisperserByIndex(int index) {
            return SpTypeUtil.valueOf(Disperser.class, index, DEFAULT);
        }

        /**
         * Return the grating value.
         */
        public int getValue() {
            return _value;
        }

        /**
         * Return the spectral resolution for the given pixel scale.
         */
        public int getSpectralResolution(Order order, PixelScale pixelScale) {
            if (pixelScale == PixelScale.PS_005) {
                return _sr005;
            }
            return _sr015;
        }

        /** Return the delta wavelength in microns for the given order and this disperser*/
        public double getDeltaWavelength(PixelScale pixelScale, Order order) {
            //(The ratios of deltaWave in the same order for different dispersers is constant,
            // so you can use 1 set of values for the disperser=32 case and convert the others
            // by: disperser111 = disperser32 / 3.49; disperser10 = disperser32 * 3.00)
            double deltaWave = order.getDeltaWavelength();
            switch(_value) {
                case 111: deltaWave /= 3.49; break;
                case 10: deltaWave *= 3.; break;
                case 32: break;
            }

            // Again, deltaWave for pixelscale=0.05" is 1/3 of pixelscale=0.15" for all cases.
            if (pixelScale == PixelScale.PS_005) {
                return deltaWave/3.;
            }
            return deltaWave;
        }
    }


    /**
     * Central Wavelength Order
     */
    public enum Order implements DisplayableSpType, SequenceableSpType {

        // Note: If you change the order here, look at EdCompInstGNIRS._getDefaultWavelengths()
        // and the event handler, since the last two items (7,8) are not included in the menu and the
        // index in the menu is used to get back to the item in this list again.
        ONE(1, 4.85, 4.3, 6.0, 0., "M", false),
        TWO(2, 3.4, 2.7, 4.3, 0., "L", false),
        THREE(3, 2.22, 1.86, 2.7, 0.000647, "K", true),
        XD(4, 1.65, 1.42, 1.86, 0.000482, "H", true),
        FOUR(4, 1.63, 1.42, 1.86, 0.000485, "H", true),
        FIVE(5, 1.25, 1.17, 1.42, 0.000388, "J", true),
        SIX(6, 1.10, 1.03, 1.17, 0.000323, "X", true),
        SEVEN(7, 0.951, 0.88, 1.03, 0.000276, null, true),
        EIGHT(8, 0.832, 0.78, 0.88, 0.000241, null, true),
        ;

        public static final Order DEFAULT = THREE;

        /**
         * Number of different orders, not counting XD
         */
        public static final int NUM_ORDERS = 8;

        private int _order; // 1 to 8
        private double _defaultWavelength; // in um
        private double _minWavelength; // in um
        private double _maxWavelength; // in um
        private double _deltaWavelength; // in microns
        private String _band; // M, L, K, H, J, H
        private boolean _isXD; // true if available in cross-dispersed mode
        private String _displayValue;

        // Constructor
        Order(int order, double defaultWavelength,
              double minWavelength, double maxWavelength,
              double deltaWavelength, String band, boolean isXD) {

            _displayValue = String.valueOf(order);
            _order = order;
            _defaultWavelength = defaultWavelength;
            _minWavelength = minWavelength;
            _maxWavelength = maxWavelength;
            _deltaWavelength = deltaWavelength;
            _band = band;
            _isXD = isXD;
        }

        /**
         * Return an Order by name
         */
        static public Order getOrder(String name) {
            return getOrder(name, DEFAULT);
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

        /**
         * Return an Order by name giving a value to return upon error
         */
        static public Order getOrder(String name, Order nvalue) {
            return SpTypeUtil.oldValueOf(Order.class, name, nvalue);
        }

        /**
         * Return the order corresponding to the given wavelength,
         * or the default order if the wavelength is out of range.
         */
        static public Order getOrder(double wavelength, Order dvalue) {
            for (Order o : values()) {
                if (wavelength >= o.getMinWavelength() && wavelength <= o.getMaxWavelength()) {
                    return o;
                }
            }
            return dvalue;
        }


        /**
         * Return an Order by index
         */
        static public Order getOrderByIndex(int index) {
            return SpTypeUtil.valueOf(Order.class, index, DEFAULT);
        }

        /**
         * Return the order (1 to 8)
         */
        public int getOrder() {
            return _order;
        }

        /**
         * Return the default wavelength in um
         */
        public double getDefaultWavelength() {
            return _defaultWavelength;
        }

        /**
         * Return the delta wavelength in microns for disperser=32
         */
        public double getDeltaWavelength() {
            return _deltaWavelength;
        }

        /**
         * Return the minimum wavelength in um
         */
        public double getMinWavelength() {
            return _minWavelength;
        }

        /**
         * Return the maximum wavelength in um
         */
        public double getMaxWavelength() {
            return _maxWavelength;
        }

        /**
         * Return the start wavelength in um
         */
        public double getStartWavelength(double wavelength, Disperser disperser, PixelScale pixelScale) {
            double deltaWave = disperser.getDeltaWavelength(pixelScale, this);
            return wavelength - 511. * deltaWave;
        }

        /**
         * Return the end wavelength in um
         */
        public double getEndWavelength(double wavelength, Disperser disperser, PixelScale pixelScale) {
            double deltaWave = disperser.getDeltaWavelength(pixelScale, this);
            return wavelength + 511. * deltaWave;
        }


        /**
         * Return the band for this order, if known, otherwise null
         */
        public String getBand() {
            return _band;
        }

        /**
         * Return true if this order is for use in cross-dispersed mode
         */
        public boolean isXD() {
            return _isXD;
        }

    }



    /**
     * WollastonPrism values (yes/no).
     */
    public enum WollastonPrism implements DisplayableSpType, SequenceableSpType {

        NO("No"),
        YES("Yes"),
        ;

        /**
         * The default WollastonPrism value
         */
        public static WollastonPrism DEFAULT = NO;
        private String _displayValue;

        WollastonPrism(String displayValue) {
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

        /**
         * Return a WollastonPrism by index
         */
        static public WollastonPrism getWollastonPrismByIndex(int index) {
            return SpTypeUtil.valueOf(WollastonPrism.class, index, DEFAULT);
        }

        /**
         * Return a WollastonPrism by name
         */
        static public WollastonPrism getWollastonPrism(String name) {
            return getWollastonPrism(name, DEFAULT);
        }

        /**
         * Return a WollastonPrism by name giving a value to return upon error
         */
        static public WollastonPrism getWollastonPrism(String name, WollastonPrism nvalue) {
            return SpTypeUtil.oldValueOf(WollastonPrism.class, name, nvalue);
        }
    }


    /**
     * Read Mode
     */
    public enum ReadMode implements DisplayableSpType, SequenceableSpType, LoggableSpType {
        // Updated for REL-175
        BRIGHT("Bright Objects", 1, 0.6, 30, "bright"),
        FAINT("Faint Objects", 16, 9., 10, "faint"),
        VERY_FAINT("Very Faint Objects", 32, 18., 7, "veryFaint"),
        VERY_BRIGHT("Very Bright/Acq./High Bckgrd.",  1, 0.2, 155, "veryBright"),
        ;
        /**
         * The default ReadMode value
         */
        public static final ReadMode DEFAULT = BRIGHT;

        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "readMode");

        private int _lowNoiseReads;
        private double _minExp;
        private double _readNoise;
        private String _logValue;
        private String _displayValue;

        ReadMode(String displayValue, int lowNoiseReads,
                 double minExp, double readNoise, String logValue) {
            _displayValue = displayValue;
            _lowNoiseReads = lowNoiseReads;
            _minExp = minExp;
            _readNoise = readNoise;             // in e-
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

        public String toString() {
            return _displayValue;
        }

        /**
         * Return a ReadMode by index
         */
        static public ReadMode getReadModeByIndex(int index) {
            return SpTypeUtil.valueOf(ReadMode.class, index, DEFAULT);
        }

        /**
         * Return a ReadMode by name
         */
        static public ReadMode getReadMode(String name) {
            return getReadMode(name, DEFAULT);
        }

        /**
         * Return a ReadMode by name giving a value to return upon error
         */
        static public ReadMode getReadMode(String name, ReadMode nvalue) {
            // XXX Backward compatibility fix after name changes
            if (name.equals("Low Background or Bright Objects")) {
                return BRIGHT;      // name change
            } else if (name.equals("Low/Medium Background, Faint Objects")) {
                return FAINT;       // name change
            } else if (name.equals("High Background or Very Faint Objects")) {
                return VERY_BRIGHT; // bug fix
            } else if (name.equals("High Background or Very Bright Objects")) { //name change
                return VERY_BRIGHT;
            } else if (name.equals("Acquisition")) { //not in the model anymore
                return VERY_BRIGHT;
            }

            return SpTypeUtil.oldValueOf(ReadMode.class, name, nvalue);
        }


        public int getLowNoiseReads() {
            return _lowNoiseReads;
        }

        public double getMinExp() {
            return _minExp;
        }

        public String getMinExpAsString() {
            return String.valueOf(_minExp) + " secs";
        }

        public double getReadNoise() {
            return _readNoise;
        }
    }


    /**
     * Filters (for on-site sequencing only)
     */
    public enum Filter implements DisplayableSpType, SequenceableSpType, LoggableSpType {

        X_DISPERSED   ("x-dispersed",            "XD"                    ),
        ORDER_6       ("order 6 (X)",            "X",                1.10),
        ORDER_5       ("order 5 (J)",            "J",                1.25),
        ORDER_4       ("order 4 (H-MK: 1.65um)", "H", "order 4 (H)", 1.65),
        ORDER_3       ("order 3 (K)",            "K",                2.20),
        ORDER_2       ("order 2 (L)",            "L",                3.50),
        ORDER_1       ("order 1 (M)",            "M",                4.80),
        // Added for OT-349
        H2            ("H2: 2.12um",             "H2",               2.12),
        H_plus_ND100X ("H + ND100X",             "H+ND100X",         1.65),
        H2_plus_ND100X("H2 + ND100X",            "H2+ND100X",        2.12),
        PAH           ("PAH: 3.3um",             "PAH",              3.30),

        // Added for REL-444
        Y             ("Y-MK: 1.03um",           "Y", "Y: 1.03um",   1.03),
        J             ("J-MK: 1.25um",           "J", "J: 1.25um",   1.25),
        K             ("K-MK: 2.20um",           "K", "K: 2.20um",   2.20),
        ;

        /**
         * The default Filter value *
         */
        public static Filter DEFAULT = ORDER_5;
        private String _logValue;
        private String _displayValue;
        private String _sequenceValue;
        private Double _wavelength;

        Filter(String displayValue, String logValue) {
            this(displayValue, logValue, null);
        }

        Filter(String displayValue, String logValue, Double wavelength) {
            this(displayValue,logValue,displayValue, wavelength);
        }

        Filter(String displayValue, String logValue, String sequenceValue, Double wavelength) {
            _displayValue = displayValue;
            _logValue = logValue;
            _sequenceValue = sequenceValue;
            _wavelength = wavelength;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _sequenceValue;
        }

        public String logValue() {
            return _logValue;
        }

        /** Wavelength if known, otherwise null. */
        public Double wavelength() {
            return _wavelength;
        }

        public String toString() {
            return _displayValue;
        }

        /**
         * Return a Filter by name *
         */
        static public Filter getFilter(String name) {
            return getFilter(name, DEFAULT);
        }

        /**
         * Return a Filter by index *
         */
        static public Filter getFilterByIndex(int index) {
            return SpTypeUtil.valueOf(Filter.class, index, DEFAULT);
        }

        /**
         * Return a Filter by name giving a value to return upon error *
         */
        static public Filter getFilter(String name, Filter nvalue) {
            return SpTypeUtil.oldValueOf(Filter.class, name, nvalue);
        }
    }

    /**
     * Cameras (for on-site sequencing only)
     * short blue
     * long blue
     * short red
     * long red
     */
    public enum Camera implements DisplayableSpType, SequenceableSpType, LoggableSpType {

        SHORT_BLUE("short blue", "SB"),
        LONG_BLUE("long blue", "LB"),
        SHORT_RED("short red", "SR"),
        LONG_RED("long red", "LR"),
        ;

        /**
         * The default Camera value *
         */
        public static Camera DEFAULT = SHORT_BLUE;
        private String _logValue;
        private String _displayValue;

        Camera(String displayValue, String logValue) {
            _displayValue = displayValue;
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

        public String toString() {
            return _displayValue;
        }

        /**
         * Return a Camera by name *
         */
        static public Camera getCamera(String name) {
            return getCamera(name, DEFAULT);
        }

        /**
         * Return a Camera by index *
         */
        static public Camera getCameraByIndex(int index) {
            return SpTypeUtil.valueOf(Camera.class, index, DEFAULT);
        }

        /**
         * Return a Camera by name giving a value to return upon error *
         */
        static public Camera getCamera(String name, Camera nvalue) {
            return SpTypeUtil.oldValueOf(Camera.class,name, nvalue);
        }

        public static Camera getDefault(double centralWavelengthMicrons, PixelScale ps) {
            final boolean blue = centralWavelengthMicrons < 2.5;
            switch (ps) {
                case PS_005: return (blue) ? LONG_BLUE  : LONG_RED;
                case PS_015: return (blue) ? SHORT_BLUE : SHORT_RED;
            }
            throw new RuntimeException("Unexpected pixel scale: " + ps);
        }
    }


    /**
     * Deckers (for on-site sequencing only)
     */
    public enum Decker implements DisplayableSpType, SequenceableSpType, ObsoletableSpType {

        ACQUISITION("acquisition"),
        PUPIL_VIEWER("pupil viewer"),
        SHORT_CAM_LONG_SLIT("short camera long slit"),
        SHORT_CAM_X_DISP("short camera x-disp"),
        IFU("IFU") {
            @Override public boolean isObsolete() { return true; }
        },
        LONG_CAM_LONG_SLIT("long camera long slit"),
        LONG_CAM_X_DISP("long camera x-disp"),
        WOLLASTON("wollaston"),
        ;

        /**
         * The default Decker value *
         */
        public static Decker DEFAULT = ACQUISITION;
        private String _displayValue;

        Decker(String displayValue) {
            _displayValue = displayValue;
        }

        /**
         * Return a Decker by name *
         */
        static public Decker getDecker(String name) {
            return getDecker(name, DEFAULT);
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

        /**
         * Return a Decker by index *
         */
        static public Decker getDeckerByIndex(int index) {
            return SpTypeUtil.valueOf(Decker.class, index, DEFAULT);
        }

        /**
         * Return a Decker by name giving a value to return upon error *
         */
        static public Decker getDecker(String name, Decker nvalue) {
            // for backward compatiility - Remove for 2007A KG
            if (name != null && name.equals("short camera x-disp/IFU")) {
                return SHORT_CAM_X_DISP;
            }
            return SpTypeUtil.oldValueOf(Decker.class, name, nvalue);
        }
    }


    /**
     * Acquisition Mirror (for on-site sequencing only)
     */
    public enum AcquisitionMirror implements DisplayableSpType, SequenceableSpType {

        IN("in"),
        OUT("out"),
        ;

        /**
         * The default AcquisitionMirror value *
         */
        public static AcquisitionMirror DEFAULT = OUT;
        private String _displayValue;

        AcquisitionMirror(String displayValue) {
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

        /**
         * Return an AcquisitionMirror by name *
         */
        static public AcquisitionMirror getAcquisitionMirror(String name) {
            return getAcquisitionMirror(name, DEFAULT);
        }

        /**
         * Return an AcquisitionMirror by index *
         */
        static public AcquisitionMirror getAcquisitionMirrorByIndex(int index) {
            return SpTypeUtil.valueOf(AcquisitionMirror.class, index, DEFAULT);
        }

        /**
         * Return an AcquisitionMirror by name giving a value to return upon error *
         */
        static public AcquisitionMirror getAcquisitionMirror(String name, AcquisitionMirror nvalue) {
            return SpTypeUtil.oldValueOf(AcquisitionMirror.class, name, nvalue);
        }
    }

    /**
     * Hartmann Masks
     */
    public enum HartmannMask implements DisplayableSpType, SequenceableSpType {

        OUT("Out"),
        LEFT_MASK("Left"),
        RIGHT_MASK("Right"),
        ;

        public static final HartmannMask DEFAULT = OUT;
        private final String _displayValue;

        HartmannMask(String displayValue) {
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

        /**
         * Return the HartmannMask by name *
         */
        static public HartmannMask getHartmannMask(String name) {
            return getHartmannMask(name, DEFAULT);
        }

        /**
         * Return the HartmannMask by index *
         */
        static public HartmannMask getHartmannMaskByIndex(int index) {
            return SpTypeUtil.valueOf(HartmannMask.class, index, DEFAULT);
        }

        /**
         * Return the HartmannMask by name giving a value to return upon error *
         */
        static public HartmannMask getHartmannMask(String name, HartmannMask nvalue) {
            return SpTypeUtil.oldValueOf(HartmannMask.class, name, nvalue);
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

    public static final class Focus extends SuggestibleString {

        public Focus(String value) {
            super(FocusSuggestion.class);
            setStringValue(value);
        }

        public Focus() {
            this(FocusSuggestion.DEFAULT.displayValue());
        }

        public Focus copy() {
            return new Focus(getStringValue());
        }
    }

    public static final class FocusEditor extends PropertyEditorSupport {

        public Object getValue() {
            return ImOption.apply((Focus) super.getValue()).map(f -> f.copy()).getOrNull();
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
            return ImOption.apply((Focus) getValue()).map(f -> f.getStringValue()).getOrNull();
        }

        public void setAsText(String string) throws IllegalArgumentException {
            setValue(new Focus(string));
        }
    }

    /**Well Depth Values **/
    public enum WellDepth implements DisplayableSpType, SequenceableSpType {

        SHALLOW("Shallow", 300),
        DEEP("Deep", 600);

        public static final WellDepth DEFAULT = SHALLOW;

        private String _displayValue;
        private int _biasLevel; //in mV

        WellDepth(String name, int biasLevel) {
            _displayValue = name;
            _biasLevel = biasLevel;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return name();
        }

        /**
         * Get the Bias Level (in mV) for the corresponding Well Depth.
         * @return the Bias Level (in mV) for the well depth.
         */
        public int getBias() {
            return _biasLevel;
        }
    }


    /** Wavelength values **/
    public enum WavelengthSuggestion implements DisplayableSpType, SequenceableSpType {
        VAL1("4.85"),
        VAL2("3.4"),
        VAL3("2.22"),
        VAL4("1.65"),
        VAL5("1.63"),
        VAL6("1.25"),
        VAL7("1.1"),
        ;

        /** Default value **/
        public static final WavelengthSuggestion DEFAULT = VAL1;

        private String _displayValue;

        WavelengthSuggestion(String displayValue) {
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

        /**
         * Return value by index
         */
        static public WavelengthSuggestion getWavelengthSuggestionByIndex(int index) {
            return SpTypeUtil.valueOf(WavelengthSuggestion.class, index, DEFAULT);
        }

        static public WavelengthSuggestion getWavelengthSuggestion(String name) {
            return getWavelengthSuggestion(name, DEFAULT);
        }

        /**
         * Return a suggestion by name.
         */
        static public WavelengthSuggestion getWavelengthSuggestion(String name, WavelengthSuggestion nvalue) {
            return SpTypeUtil.oldValueOf(WavelengthSuggestion.class, name, nvalue);
        }
    }

    public static class Wavelength extends SuggestibleString {

        public Wavelength() {
            super(WavelengthSuggestion.class);
            setStringValue(WavelengthSuggestion.DEFAULT.displayValue());
        }

        public Wavelength(String value) {
            super(WavelengthSuggestion.class);
            setStringValue(value);
        }

        public double doubleValue() {
            return Double.parseDouble(getStringValue());
        }

        @Override public void setStringValue(String value) {
            if (isValid(value)) super.setStringValue(value);
        }

        private static final Pattern PAT = Pattern.compile("[\\d\\.]*");

        public static boolean isValid(String value) {
            // First see if it parses as a double.
            try {
                Double.parseDouble(value);
            } catch (NumberFormatException ex) {
                return false;
            }

            // Now make sure it is just digits and decimal
            return PAT.matcher(value).matches();
        }
    }

     public static class WavelengthEditor extends PropertyEditorSupport {
        public Object getValue() {
            GNIRSParams.Wavelength w = (GNIRSParams.Wavelength) super.getValue();
            if (w == null) return null;

            GNIRSParams.Wavelength res = new GNIRSParams.Wavelength();
            res.setStringValue(w.getStringValue());
            return res;
        }

        public void setValue(Object value) {
            GNIRSParams.Wavelength w = (GNIRSParams.Wavelength) value;
            GNIRSParams.Wavelength cur = (GNIRSParams.Wavelength) super.getValue();
            if (cur == null) {
                cur = new GNIRSParams.Wavelength();
                super.setValue(cur);
            }
            cur.setStringValue(w.getStringValue());
        }

        public String getAsText() {
            GNIRSParams.Wavelength val = (GNIRSParams.Wavelength) getValue();
            if (val == null) return null;
            return val.getStringValue();
        }

        public void setAsText(String string) throws IllegalArgumentException {
            if (!Wavelength.isValid(string)) throw new IllegalArgumentException("Could not parse " + string + " as a wavelength.");
            GNIRSParams.Wavelength val = (GNIRSParams.Wavelength) super.getValue();
            if (val == null) {
                val = new GNIRSParams.Wavelength();
                super.setValue(val);
            }
            val.setStringValue(string);
        }
    }

    static {
        PropertyEditorManager.registerEditor(GNIRSParams.Wavelength.class, GNIRSParams.WavelengthEditor.class);
    }

}


