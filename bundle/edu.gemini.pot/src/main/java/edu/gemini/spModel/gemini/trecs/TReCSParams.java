package edu.gemini.spModel.gemini.trecs;

import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY;

import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.type.*;


/**
 * This class provides data types for the TReCS components.
 */
public final class TReCSParams {

    // Make the constructor private.
    private TReCSParams() {
    }

    /**
     * TReCS Dispersers.
     */
    public enum Disperser implements DisplayableSpType, LoggableSpType, SequenceableSpType {

        MIRROR("Mirror", "Mirror"),
        LOW_RES_10("Low Res 10um Grating", "LR10"),
        LOW_RES_20("Low Res 20um Grating", "LR20"),
        HIGH_RES("High Res Grating", "HR10"),
        LOW_RES_REF_MIRROR("Low Res Ref Mirror", "LRRef"),
        HIGH_RES_REF_MIRROR("High Res Ref Mirror", "HRRef"),;

        /**
         * The default Disperser value *
         */
        public static Disperser DEFAULT = MIRROR;
        public static ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "disperser");

        private String _displayValue;
        /**
         * The log value *
         */
        private String _logValue;

        Disperser(String displayValue, String logValue) {
            _displayValue = displayValue;
            _logValue = logValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String logValue() {
            return _logValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String toString() {
            return _displayValue;
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
            return SpTypeUtil.oldValueOf(Disperser.class, name, nvalue);
        }

    }


    /**
     * Masks
     */
    public enum Mask implements DisplayableSpType, LoggableSpType, SequenceableSpType {

        MASK_IMAGING("Imaging", 28.5, 21.5, "open"), // according to Kevin
        MASK_IMAGING_W("Imaging w/o Flexure Mask", 28.8, 21.6, "open"),
        MASK_1("Slit 0.21 arcsec", 0.21, 20.0, "0.21"),
        MASK_2("Slit 0.26 arcsec", 0.26, 20.0, "0.26"),
        MASK_3("Slit 0.31 arcsec", 0.31, 20.0, "0.31"),
        MASK_4("Slit 0.35 arcsec", 0.35, 20.0, "0.35"),
        MASK_5("Slit 0.65 arcsec", 0.65, 20.0, "0.65"),
        MASK_6("Slit 0.70 arcsec", 0.70, 20.0, "0.70"),
        MASK_7("Slit 1.30 arcsec", 1.30, 20.0, "1.30"),
        OCCULTING_BAR("Occulting Bar", 28.5, 21.5, "occulting bar"); //Moved here from the eng. comp. (SCT-139)

        /**
         * The default Mask value *
         */
        public static final Mask DEFAULT = MASK_IMAGING;

        /**
         * The log value *
         */
        private String _logValue;
        private String _displayValue;

        // The width of the mask in arcsec
        private double _width;

        // The height (length) of the mask in arcsec
        private double _height;

        Mask(String displayValue, double width, double height, String logValue) {
            _displayValue = displayValue;
            _width = width;
            _height = height;
            _logValue = logValue;
        }

        // Return the width of the mask in arcsec.
        public double getWidth() {
            return _width;
        }

        // Return the height (length) of the mask in arcsec.
        public double getHeight() {
            return _height;
        }

        /**
         * Return the log value for this slit/mask *
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
         * Return a Mask by index *
         */
        static public Mask getMaskByIndex(int index) {
            return SpTypeUtil.valueOf(Mask.class, index, DEFAULT);
        }

        /**
         * Return a Mask by name *
         */
        static public Mask getMask(String name) {
            return getMask(name, DEFAULT);
        }

        /**
         * Return a Mask by name giving a value to return upon error *
         */
        static public Mask getMask(String name, Mask nvalue) {
            return SpTypeUtil.oldValueOf(Mask.class, name, nvalue);
        }

    }

    /**
     * User Filters
     */
    public enum Filter implements DisplayableSpType, LoggableSpType, SequenceableSpType, ObsoletableSpType {

        NONE("None", "None", "none"),
        N("N (broad 10um)", "N", "10"),
        N_PRIME("N' 11.2um (semi-broad)", "Nprime", "11.2"),
        SI_1("Si-1 7.73um", "Si1", "7.73"),
        SI_2("Si-2 8.74um", "Si2", "8.74"),
        SI_3("Si-3 9.69um", "Si3", "9.69"),
        SI_4("Si-4 10.38um", "Si4", "10.38"),
        SI_5("Si-5 11.66um", "Si5", "11.66"),
        SI_6("Si-6 12.33um", "Si6", "12.33"),
        AR_III("[Ar III] 8.99um", "ArIII", "8.99"),
        S_IV("[S IV] 10.52um", "SIV", "10.52"),
        NE_II("[Ne II] 12.81um", "NeII_ref2", "12.81"),
        NE_II_CONT("[Ne II] cont 13.10um", "NeII13.1", "13.10"),
        PAH_8_6("PAH 8.6um", "PAH1", "8.6"),
        PAH_11_3("PAH 11.3um", "PAH2", "11.3"),
        Q_SHORT("Q short 17.65um", "Qone", "17.65") {
            @Override public boolean isObsolete() { return true; }
        },
        QA("Qa 18.30um", "Qa", "18.30"),
        QB("Qb 24.56um", "Qb", "24.56"),
        Q("Q (broad 20.8um)", "Qw", "20.8"),
        K("K", "K", "2.19"),
        L("L", "L", "3.85"),
        M("M", "M", "4.68"),
        Block("Block", "Block", "Block");

        public static Filter DEFAULT = N;

        private String _displayValue;
        private String _logValue;
        private String _wavelength;  // in µm

        Filter(String displayValue, String logValue, String wavelength) {
            _displayValue = displayValue;
            _logValue = logValue;
            _wavelength = wavelength;
        }

        /**
         * Return the log value for this filter *
         */
        public String logValue() {
            return _logValue;
        }

        /**
         * Return the filter's effective wavelength *
         */
        public String getWavelength() {
            return _wavelength;
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
         * Return a Filter by name *
         */
        static public Filter getFilter(String name) {
            return getFilter(name, DEFAULT);
        }

        /**
         * Return a User filter by index *
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
     * DataMode values.
     */
    public enum DataMode implements SequenceableSpType, DisplayableSpType {

        SAVE("Save"),
        DISCARD("Discard"),
        DISCARD_ALL("Discard All"),
        DISCARD_DHS("Discard DHS"),
        ;

        /**
         * The default DataMode value *
         */
        public static DataMode DEFAULT = SAVE;

        private String _displayValue;

        DataMode(String displayValue) {
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
         * Return a DataMode by index *
         */
        static public DataMode getDataModeByIndex(int index) {
            return SpTypeUtil.valueOf(DataMode.class, index, DEFAULT);
        }

        /**
         * Return a DataMode by name *
         */
        static public DataMode getDataMode(String name) {
            return getDataMode(name, DEFAULT);
        }

        /**
         * Return a DataMode by name giving a value to return upon error *
         */
        static public DataMode getDataMode(String name, DataMode nvalue) {
            return SpTypeUtil.oldValueOf(DataMode.class, name, nvalue);
        }

    }


    /**
     * ObsMode values.
     */
    public enum ObsMode implements SequenceableSpType, DisplayableSpType, LoggableSpType {

        CHOP_NOD("Chop-Nod", "C-N"),
        STARE("Stare", "S"),
        CHOP("Chop", "C"),
        NOD("Nod", "N"),
        ;

        /**
         * The default ObsMode value
         */
        public static ObsMode DEFAULT = CHOP_NOD;

        private String _logValue;
        private String _displayValue;

        ObsMode(String displayValue, String logValue) {
            _displayValue = displayValue;
            _logValue = logValue;
        }

        /**
         * Return the log value for this ObsMode *
         */
        public String getLogValue() {
            return _logValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String logValue() {
            return _logValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String toString() {
            return _displayValue;
        }

        /**
         * Return an ObsMode by index *
         */
        static public ObsMode getObsModeByIndex(int index) {
            return SpTypeUtil.valueOf(ObsMode.class, index, DEFAULT);
        }

        /**
         * Return an ObsMode by name *
         */
        static public ObsMode getObsMode(String name) {
            return getObsMode(name, DEFAULT);
        }

        /**
         * Return an ObsMode by name giving a value to return upon error *
         */
        static public ObsMode getObsMode(String name, ObsMode nvalue) {
            return SpTypeUtil.oldValueOf(ObsMode.class, name, nvalue);
        }
    }


    /**
     * Window Wheel values.
     */
    public enum WindowWheel implements SequenceableSpType,  DisplayableSpType {

        AUTO("auto"),
        BLOCK("Block"),
        KRS_5("KRS-5"),
        ZNSE("ZnSe"),
        KBR("KBr"),
        KBRC("KBrC"),
        DATUM("Datum"),
        ;

        /**
         * The default WindowWheel value *
         */
        public static WindowWheel DEFAULT = AUTO;
        private String _displayValue;

        WindowWheel(String displayValue) {
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
         * Return a WindowWheel by index *
         */
        static public WindowWheel getWindowWheelByIndex(int index) {
            return SpTypeUtil.valueOf(WindowWheel.class, index, DEFAULT);
        }

        /**
         * Return a WindowWheel by name *
         */
        static public WindowWheel getWindowWheel(String name) {
            return getWindowWheel(name, DEFAULT);
        }

        /**
         * Return a WindowWheel by name giving a value to return upon error *
         */
        static public WindowWheel getWindowWheel(String name, WindowWheel nvalue) {
            return SpTypeUtil.oldValueOf(WindowWheel.class, name, nvalue);
        }

    }

    /** For Engineering -- */

    /**
     * Sector Wheel values.
     */
    public enum SectorWheel implements SequenceableSpType, DisplayableSpType {

        OPEN("Open"),
        POLY_115("Poly_115"),
        BLACK_PLATE("Black_Plate"),
        POLY_105("Poly_105"),
        DATUM("Datum"),
        ;

        /**
         * The default SectorWheel value *
         */
        public static SectorWheel DEFAULT = OPEN;

        private String _displayValue;

        SectorWheel(String displayValue) {
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
         * Return a SectorWheel by index *
         */
        static public SectorWheel getSectorWheelByIndex(int index) {
            return SpTypeUtil.valueOf(SectorWheel.class, index, DEFAULT);
        }

        /**
         * Return a SectorWheel by name *
         */
        static public SectorWheel getSectorWheel(String name) {
            return getSectorWheel(name, DEFAULT);
        }

        /**
         * Return a SectorWheel by name giving a value to return upon error *
         */
        static public SectorWheel getSectorWheel(String name, SectorWheel nvalue) {
            return SpTypeUtil.oldValueOf(SectorWheel.class, name, nvalue);
        }
    }

    /**
     * Lyot Wheel Values
     */
    public enum LyotWheel implements SequenceableSpType, DisplayableSpType {

        GRID_MASK("Grid_Mask"),
        SPOT_MASK("Spot_Mask"),
        CIARDI("Ciardi"),
        OPEN("Open"),
        QUAKHAM_MASK("Quakham_Mask"),
        POLYSTYRENE("Polystyrene"),
        CIRC_MINUS_TWO("Circ-2"),
        CIRC_MINUS_FOUR("Circ-4"),
        CIRC_PLUS_TWO("Circ+2"),
        CIRC_PLUS_FOUR("Circ+4"),
        CIRC_PLUS_SIX("Circ+6"),
        CIRC_PLUS_EIGHT("Circ+8"),
        BLOCK("Block"),
        DATUM("Datum"),
        ;

        /**
         * The default LyotWheel value *
         */
        public static LyotWheel DEFAULT = CIRC_MINUS_TWO;

        private String _displayValue;

        LyotWheel(String displayValue) {
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
         * Return a LyotWheel by index *
         */
        static public LyotWheel getLyotWheelByIndex(int index) {
            return SpTypeUtil.valueOf(LyotWheel.class, index, DEFAULT);
        }

        /**
         * Return a LyotWheel by name *
         */
        static public LyotWheel getLyotWheel(String name) {
            return getLyotWheel(name, DEFAULT);
        }

        /**
         * Return a LyotWheel by name giving a value to return upon error *
         */
        static public LyotWheel getLyotWheel(String name, LyotWheel nvalue) {
            return SpTypeUtil.oldValueOf(LyotWheel.class, name, nvalue);
        }
    }

    /**
     * Pupil Imaging Wheel values.
     */
    public enum PupilImagingWheel implements DisplayableSpType, SequenceableSpType {

        OPEN_1("Open-1"),
        PUPIL_IMAGER("Pupil_Imager"),
        OPEN_2("Open-2"),
        OPEN_3("Open-3"),
        DATUM("Datum"),
        ;

        /**
         * The default PupilImagingWheel value *
         */
        public static PupilImagingWheel DEFAULT = OPEN_1;
        private String _displayValue;

        PupilImagingWheel(String displayValue) {
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
         * Return a PupilImagingWheel by index *
         */
        static public PupilImagingWheel getPupilImagingWheelByIndex(int index) {
            return SpTypeUtil.valueOf(PupilImagingWheel.class, index, DEFAULT);
        }

        /**
         * Return a PupilImagingWheel by name *
         */
        static public PupilImagingWheel getPupilImagingWheel(String name) {
            return getPupilImagingWheel(name, DEFAULT);
        }

        /**
         * Return a PupilImagingWheel by name giving a value to return upon error *
         */
        static public PupilImagingWheel getPupilImagingWheel(String name, PupilImagingWheel nvalue) {
            return SpTypeUtil.oldValueOf(PupilImagingWheel.class, name, nvalue);
        }
    }

    /**
     * Aperture Wheel values.
     */
    public enum ApertureWheel implements DisplayableSpType, SequenceableSpType, ObsoletableSpType {

        GRID_MASK("Grid_Mask"),
        MATCHED("Matched"),
        OCCULTING_BAR("Occulting_Bar") {
            @Override public boolean isObsolete() { return true; }
        },
        WINDOW_IMAGER("Window_Imager"),
        SPOT_MASK("Spot_Mask"),
        DATUM("Datum"),
        ;

        /**
         * The default ApertureWheel value *
         */
        public static ApertureWheel DEFAULT = MATCHED;
        private String _displayValue;

        ApertureWheel(String displayValue) {
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
         * Return a ApertureWheel by index *
         */
        static public ApertureWheel getApertureWheelByIndex(int index) {
            return SpTypeUtil.valueOf(ApertureWheel.class, index, DEFAULT);
        }

        /**
         * Return a ApertureWheel by name *
         */
        static public ApertureWheel getApertureWheel(String name) {
            return getApertureWheel(name, DEFAULT);
        }

        /**
         * Return a ApertureWheel by name giving a value to return upon error *
         */
        static public ApertureWheel getApertureWheel(String name, ApertureWheel nvalue) {
            if (name.equals("Oversized")) return OCCULTING_BAR;
            return SpTypeUtil.oldValueOf(ApertureWheel.class, name, nvalue);
        }
    }

    /**
     * Nod orientation values.
     */
    public enum NodOrientation implements DisplayableSpType, SequenceableSpType {

        PARALLEL("Parallel to Chop"),
        ORTHOGONAL("Orthogonal to Chop"),
        ;

        /**
         * The default NodOrientation value *
         */
        public static NodOrientation DEFAULT = PARALLEL;
        private String _displayValue;

        NodOrientation(String displayValue) {
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
         * Return a NodOrientation by name *
         */
        static public NodOrientation getNodOrientation(String name) {
            return getNodOrientation(name, DEFAULT);
        }

        /**
         * Return a NodOrientation by index *
         */
        static public NodOrientation getNodOrientationByIndex(int index) {
            return SpTypeUtil.valueOf(NodOrientation.class, index, DEFAULT);
        }

        /**
         * Return a NodOrientation by name giving a value to return upon error *
         */
        static public NodOrientation getNodOrientation(String name, NodOrientation nvalue) {
            return SpTypeUtil.oldValueOf(NodOrientation.class, name, nvalue);
        }
    }

    /**
     * ReadoutMode values.
     */
    public enum ReadoutMode implements DisplayableSpType, SequenceableSpType {

        NORMAL_IMAGING("Normal Imaging and Spectroscopy"),
        FAINT_SOURCE("Faint-source Spectroscopy"),
        ;

        /**
         * The default ReadoutMode value *
         */
        public static ReadoutMode DEFAULT = NORMAL_IMAGING;
        private String _displayValue;

        ReadoutMode(String displayValue) {
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
         * Return a ReadoutMode by name *
         */
        static public ReadoutMode getReadoutMode(String name) {
            return getReadoutMode(name, DEFAULT);
        }

        /**
         * Return a ReadoutMode by index *
         */
        static public ReadoutMode getReadoutModeByIndex(int index) {
            return SpTypeUtil.valueOf(ReadoutMode.class, index, DEFAULT);
        }

        /**
         * Return a ReadoutMode by name giving a value to return upon error *
         */
        static public ReadoutMode getReadoutMode(String name, ReadoutMode nvalue) {
            // for backward compatibility
            if ("Normal Imaging & Spectroscopy".equals(name)) {
                return NORMAL_IMAGING;
            }
            return SpTypeUtil.oldValueOf(ReadoutMode.class, name, nvalue);
        }
    }

    /**
     * Well depth - SCT-109/OT-
     */
    public enum WellDepth implements DisplayableSpType, SequenceableSpType {

        AUTO("auto"),
        SHALLOW("Shallow"),
        DEEP("Deep"),
        ;

        /**
         * The default Well depth value
         */
        public static WellDepth DEFAULT = AUTO;
        private String _displayValue;

        WellDepth(String displayValue) {
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
         * Return a Well Depth by name
         */
        static public WellDepth getWellDepth(String name) {
            return getWellDepth(name, DEFAULT);
        }

        /**
         * Return a WellDepth by index
         */
        static public WellDepth getWellDepthByIndex(int index) {
            return SpTypeUtil.valueOf(WellDepth.class, index, DEFAULT);
        }

        /**
         * Return a WellDepth by name giving a value to return upon error
         */
        static public WellDepth getWellDepth(String name, WellDepth nvalue) {
            return SpTypeUtil.oldValueOf(WellDepth.class, name, nvalue);
        }
    }
}


