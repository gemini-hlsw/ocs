package edu.gemini.spModel.gemini.nifs;

import edu.gemini.spModel.config2.ItemKey;
import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY;
import edu.gemini.spModel.type.*;
import scala.Option;

/**
 * This class provides data types for the NIFS components.
 */
public final class NIFSParams {

    private NIFSParams() {
    }

    /**
     * Dispersers
     * All dispersers have a default filter they correspond to in case the filter is set to "Same as Disperser".
     */
    public enum Disperser implements DisplayableSpType, SequenceableSpType {

        Z(      "Z Grating",        1.050,  Filter.ZJ_FILTER),
        J(      "J Grating",        1.250,  Filter.ZJ_FILTER),
        H(      "H Grating",        1.650,  Filter.JH_FILTER),
        K(      "K Grating",        2.200,  Filter.HK_FILTER),
        K_SHORT("K_short Grating",  2.100,  Filter.HK_FILTER),
        K_LONG( "K_long Grating",   2.300,  Filter.HK_FILTER),
        MIRROR( "Mirror",           0.0,    Filter.HK_FILTER),
        ;

        /**
         * The default Disperser value
         */
        public static final Disperser DEFAULT = K;

        // The default central wavelength in um
        private double _wavelength;
        private String _displayValue;
        // The default filter for this grating if filter is "Same as Disperser"
        private Filter _defaultFilter;

        Disperser(String displayValue, double wavelength, Filter defaultFilter) {
            _displayValue   = displayValue;
            _wavelength     = wavelength;
            _defaultFilter  = defaultFilter;
        }

        /**
         * Returns the default central wavelength for this disperser setting in um.
         */
        public double getWavelength() {
            return _wavelength;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public Filter defaultFilter() {
            return _defaultFilter;
        }

        /**
         * Return a Disperser by index
         */
        static public Disperser getDisperserByIndex(int index) {
            return SpTypeUtil.valueOf(Disperser.class, index, DEFAULT);
        }

        /**
         * Return a Disperser by name
         */
        static public Disperser getDisperser(String name) {
            return getDisperser(name, DEFAULT);
        }

        /**
         * Return a Disperser by name giving a value to return upon error
         */
        static public Disperser getDisperser(String name, Disperser nvalue) {
            return SpTypeUtil.oldValueOf(Disperser.class, name, nvalue);
        }
    }

    /**
     * Read Mode (REL-481)
     */
    public enum ReadMode implements StandardSpType {

        // Faint object read = Fowler 16-16, readnoise = 6e- (overhead = 85sec per image)
        FAINT_OBJECT_SPEC(
                "1-2.5 um: Faint Object Spectroscopy",
                "Weak Source", 4.6, 85.0, "> 85 sec", 16, "faint"),

        // Added for OT-507
        // Medium read = Fowler 4-4, readnoise=9e- (overhead = 21sec per image)
        MEDIUM_OBJECT_SPEC(
                "1-2.5 um: Medium Object Spectroscopy",
                "Medium Source", 8.1, 21.0, "> 21 sec", 4, "medium"),

        // Bright object read = Fowler 1-1, readnoise = 18 e- (overhead = 5sec per image)
        BRIGHT_OBJECT_SPEC(
                "1-2.5 um: Bright Object Spectroscopy",
                "Strong Source", 15.4, 5.3, "> 5 sec", 1, "bright"),;

        /**
         * The default ReadMode value
         */
        public static final ReadMode DEFAULT = FAINT_OBJECT_SPEC;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "readMode");

        private double _minExp;
        private String _displayValue;
        private String _description;
        private String _logValue;
        private double _readNoise;
        private String _recommendedExp;
        private int _fowlerSampling;

        ReadMode(String displayValue, String description, double readNoise,
                         double minExp, String recommendedExp, int fowlerSampling,
                         String logValue) {
            _displayValue = displayValue;
            _description = description;
            _minExp = minExp;
            _logValue = logValue;
            _readNoise = readNoise;
            _recommendedExp = recommendedExp;
            _fowlerSampling = fowlerSampling;
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
            return SpTypeUtil.oldValueOf(ReadMode.class, name, nvalue);
        }

        /**
         * Returns the minimum exposure time in seconds
         */
        public double getMinExp() {
            return _minExp;
        }

        /**
         * Returns the minimum exposure time as a string
         */
        public String getMinExpAsString() {
            return String.valueOf(_minExp) + " sec";
        }

        /**
         * Returns the fowler sampling value for this read mode
         */
        public int getFowlerSampling() {
            return _fowlerSampling;
        }

        public double getReadNoise() {
            return _readNoise;
        }

        /**
         * Return the recommended minimum exposure time.
         */
        public String getRecommendedMinExp() {
            return _recommendedExp;
        }
    }


    /**
     * Engineering Read Mode
     */
    public enum EngReadMode implements DisplayableSpType, LoggableSpType, SequenceableSpType {

        FOWLER_SAMPLING_READOUT("Fowler Sampling Readout", "fowler"),
        LINEAR_READ("Linear Read", "linear"),
        ;

        /**
         * The default EngReadMode value
         */
        public static final EngReadMode DEFAULT = FOWLER_SAMPLING_READOUT;

        private String _displayValue;
        private String _logValue;

        EngReadMode(String displayValue, String logValue) {
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

        /**
         * Return an EngReadMode by index
         */
        static public EngReadMode getReadModeByIndex(int index) {
            return SpTypeUtil.valueOf(EngReadMode.class, index, DEFAULT);
        }

        /**
         * Return an EngReadMode by name
         */
        static public EngReadMode getReadMode(String name) {
            return getEngReadMode(name, DEFAULT);
        }

        /**
         * Return an EngReadMode by name giving a value to return upon error
         */
        static public EngReadMode getEngReadMode(String name, EngReadMode nvalue) {
            return SpTypeUtil.oldValueOf(EngReadMode.class, name, nvalue);
        }
    }

    /**
     * Masks
     */
    public enum Mask implements DisplayableSpType, SequenceableSpType, LoggableSpType {

        CLEAR("Clear", "clear", false),
        PINHOLE("0.1 arcsec Pinhole", "pinhole", false),
        PINHOLE_ARRAY("0.2 arcsec Pinhole Array", "pinhole_array", false),
        SLIT("0.2 arcsec Slit", "slit", false),
        RONCHE("Ronche Cal Mask", "slit", false),
        OD_1("0.1 arcsec Occulting Disk", "od_1", true),
        OD_2("0.2 arcsec Occulting Disk", "od_2", true),
        OD_5("0.5 arcsec Occulting Disk", "od_5", true),
        KG3_ND_FILTER("KG3 ND Filter", "kg3NdFilter", false),
        KG5_ND_FILTER("KG5 ND Filter", "kg5NdFilter", false),
        BLOCKED("Blocked", "blocked", false),
        ;

        /**
         * The default Mask value
         */
        public static final Mask DEFAULT = CLEAR;

        // The internal values of height and width for this instance of Mask
        // science area size im arcsec
        public static final double SIZE = 3;
        private double _width = SIZE;
        private double _height = SIZE;

        private String _displayValue;
        private boolean _isOccultingDisk = false;
        private String _logValue;

        /**
         * Constructor.
         *
         * @param displayValue the value displayed in the GUI
         * @param logValue  the name for logging
         * @param isOccultingDisk true if the mask is an occulting disk
         */
        Mask(String displayValue, String logValue, boolean isOccultingDisk) {
            _displayValue = displayValue;
            _isOccultingDisk = isOccultingDisk;
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
         * Return a Mask by index
         */
        static public Mask getMaskByIndex(int index) {
            return SpTypeUtil.valueOf(Mask.class, index, DEFAULT);
        }

        /**
         * Returns true if this mask is an occulting disk
         */
        public boolean isOccultingDisk() {
            return _isOccultingDisk;
        }

        /**
         * Return the width for this mask
         */
        public double getWidth() {
            return _width;
        }

        /**
         * Return the height for this mask
         */
        public double getHeight() {
            return _height;
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
            return SpTypeUtil.oldValueOf(Mask.class, name, nvalue);
        }
    }

    /**
     * Filters
     */
    public enum Filter implements DisplayableSpType, SequenceableSpType {

        // OT-500:
        // IF (grating = "Mirror" or Imaging Mirror = "In") and filter =
        // HK, central wavelength = 2.20
        // JH, central wavelength = 1.65
        // ZJ, central wavelength = 1.25
        SAME_AS_DISPERSER("Same as Disperser"),
        ZJ_FILTER("ZJ Filter", "1.25"),
        JH_FILTER("JH Filter", "1.65"),
        HK_FILTER("HK Filter", "2.20"),
        WIRE_GRID("K Filter + Wire Grid"),
        BLOCKED("Blocked"),
        ;

        /**
         * The default Filter value *
         */
        public static Filter DEFAULT = SAME_AS_DISPERSER;

        private String _wavelength;  // in µm
        private String _displayValue;

        Filter(String displayValue) {
            _displayValue = displayValue;
        }

        Filter(String displayValue, String wavelength) {
            this(displayValue);
            _wavelength = wavelength;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
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

        /**
         * Return the filter's effective wavelength, if known, otherwise null *
         */
        public String getWavelength() {
            return _wavelength;
        }
    }

    /**
     * ImagingMirror values (out/in).
     */
    public enum ImagingMirror implements DisplayableSpType, SequenceableSpType {

        OUT("Out"),
        IN("In"),
        ;

        /**
         * The default ImagingMirror value
         */
        public static ImagingMirror DEFAULT = OUT;

        private String _displayValue;

        ImagingMirror(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /**
         * Return a ImagingMirror by index
         */
        static public ImagingMirror getImagingMirrorByIndex(int index) {
            return SpTypeUtil.valueOf(ImagingMirror.class, index, DEFAULT);
        }

        /**
         * Return a ImagingMirror by name
         */
        static public ImagingMirror getImagingMirror(String name) {
            return getImagingMirror(name, DEFAULT);
        }

        /**
         * Return a ImagingMirror by name giving a value to return upon error
         */
        static public ImagingMirror getImagingMirror(String name, ImagingMirror nvalue) {
            return SpTypeUtil.oldValueOf(ImagingMirror.class, name, nvalue);
        }
    }
}
