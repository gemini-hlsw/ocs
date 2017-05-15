package edu.gemini.spModel.gemini.gmos;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.config2.ItemKey;
import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY;

import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.type.*;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.*;

/**
 * This class provides data types for the GMOS components.
 */
public class GmosCommonType {
    private GmosCommonType() {
        // defeat construction
    }

    public interface Disperser extends DisplayableSpType, LoggableSpType, SequenceableSpType {
        boolean isMirror();
        // The dispersers "ruling density" in lines/mm.
        int rulingDensity();
    }

    public interface DisperserBridge<D extends Enum<D> & Disperser> {
        Class<D> getPropertyType();
        D getDefaultValue();
        D parse(String name, D defaultValue);
    }

    public interface Filter extends DisplayableSpType, LoggableSpType, SequenceableSpType {
        boolean isNone();
        String getWavelength();
    }

    public interface FilterBridge<F extends Enum<F> & Filter> {
        Class<F> getPropertyType();
        F getDefaultValue();
        F parse(String name, F defaultValue);
    }

    public interface FPUnit extends DisplayableSpType, LoggableSpType, SequenceableSpType {
        // IFU visualisation in TPE
        // The offset from the base position in arcsec
        double IFU_FOV_OFFSET = 30.;

        // The offsets (from the base pos) and dimensions of the IFU FOV (in arcsec)
        Rectangle2D.Double[] IFU_FOV = new Rectangle2D.Double[]{
            new Rectangle2D.Double(-30. - IFU_FOV_OFFSET, 0., 3.5, 5.),
            new Rectangle2D.Double(30. - IFU_FOV_OFFSET, 0., 7., 5.)
        };

        // Indexes for above array
        int IFU_FOV_SMALLER_RECT_INDEX = 0;
        int IFU_FOV_LARGER_RECT_INDEX = 1;


        double getWidth();
        boolean isImaging();
        boolean isSpectroscopic();
        boolean isIFU();
        boolean isNS();
        boolean isNSslit();

        /**
         * True if the slit is wider than it is tall.  See REL-661 comment on
         * adding 90 degrees to the average parallactic angle.
         */
        boolean isWideSlit();

        /**
         * Calculate the IFU offset
         *
         * @return the offset in the X axis
         */
        double getWFSOffset();
    }

    public interface FPUnitBridge<P extends Enum<P> & FPUnit> {
        Class<P> getPropertyType();
        P getDefaultValue();
        P parse(String name, P defaultValue);

        P getCustomMask();
        P getNone();
    }

    /**
     * AmpCount indicates the number of amps that should be used
     * when reading out the detectors.
     */
    public enum AmpCount implements DisplayableSpType, LoggableSpType, SequenceableSpType, ObsoletableSpType {
        THREE("Three", DetectorManufacturer.E2V),
        SIX("Six", DetectorManufacturer.E2V, DetectorManufacturer.HAMAMATSU),
        TWELVE("Twelve", DetectorManufacturer.HAMAMATSU),
        ;

        // Hamamatsu detectors use twelve amps, so make this the default.
        public final static AmpCount DEFAULT = AmpCount.TWELVE;
        public final static ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "ampCount");

        private final String _displayValue;
        private final Set<DetectorManufacturer> supportedBy;

        AmpCount(String displayValue, DetectorManufacturer... supportedBy) {
            _displayValue = displayValue;
            Set<DetectorManufacturer> tmp = new HashSet<DetectorManufacturer>(Arrays.asList(supportedBy));
            this.supportedBy = Collections.unmodifiableSet(tmp);
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String logValue() {
            return _displayValue;
        }

        // This is backwards, but DetectorManufacturer is a common type
        // and the counts are defined in the north and south extensions.
        public Set<GmosCommonType.DetectorManufacturer> getSupportedBy() {
            return supportedBy;
        }

        /** Return an AmpCount by name **/
        public static AmpCount getAmpCount(String name) {
            return AmpCount.getAmpCount(name, AmpCount.DEFAULT);
        }

        /** Return an AmpCount by name giving a value to return upon error **/
        public static AmpCount getAmpCount(String name, AmpCount nvalue) {
            return SpTypeUtil.oldValueOf(AmpCount.class, name, nvalue);
        }
    }


    public interface StageMode extends DisplayableSpType, LoggableSpType, SequenceableSpType, ObsoletableSpType {

    }

    public interface StageModeBridge<SM extends Enum<SM> & StageMode> {
        SM parse(String name, SM defaultValue);
        SM getDefaultValue();
    }

    /**
     * ADC
     */
    public enum ADC implements DisplayableSpType, LoggableSpType, SequenceableSpType {

        NONE("No Correction"),
        BEST_STATIC("Best Static Correction"),
        FOLLOW("Follow During Exposure"),
        ;

        /** The default ADC value **/
        public static ADC DEFAULT = ADC.NONE;

        private String _displayValue;

        ADC(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String logValue() {
            return _displayValue;
        }

        /** Return an ADC by index **/
        public static ADC getADCByIndex(int index) {
            return SpTypeUtil.valueOf(ADC.class, index, DEFAULT);
        }

        /** Return an ADC by name **/
        public static ADC getADC(String name) {
            return ADC.getADC(name, ADC.DEFAULT);
        }

        /** Return an ADC by name giving a value to return upon error **/
        public static ADC getADC(String name, ADC nvalue) {
            return SpTypeUtil.oldValueOf(ADC.class, name, nvalue);
        }
    }

    /*
    public static enum AmpGainReadCombo implements DisplayableSpType, SequenceableSpType {
        SLOW_HIGH("Slow Read/High Gain", GmosCommonType.AmpReadMode.SLOW, GmosCommonType.AmpGain.HIGH),
        SLOW_LOW("Slow Read/Low Gain", GmosCommonType.AmpReadMode.SLOW, GmosCommonType.AmpGain.LOW),
        FAST_LOW("Fast Read/Low Gain", GmosCommonType.AmpReadMode.FAST, GmosCommonType.AmpGain.LOW),
        FAST_HIGH("Fast Read/High Gain", GmosCommonType.AmpReadMode.FAST, GmosCommonType.AmpGain.HIGH);

        private String _displayValue;
        private GmosCommonType.AmpReadMode _ampReadMode;
        private GmosCommonType.AmpGain _ampGain;

        private AmpGainReadCombo(String displayValue,
              GmosCommonType.AmpReadMode ampReadMode, GmosCommonType.AmpGain ampGain) {
            this._displayValue = displayValue;
            this._ampGain = ampGain;
            this._ampReadMode = ampReadMode;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public GmosCommonType.AmpReadMode getAmpReadMode() {
            return _ampReadMode;
        }

        public GmosCommonType.AmpGain getAmpGain() {
            return _ampGain;
        }

        public static AmpGainReadCombo getAmpGainReadCombo(String name) {
            return SpTypeUtil.oldValueOf(AmpGainReadCombo.class, name, SLOW_LOW);
        }

        public static AmpGainReadCombo lookup(AmpGain gain, AmpReadMode mode) {
            for (AmpGainReadCombo combo : values()) {
                if ((mode == combo.getAmpReadMode()) &&
                    (gain == combo.getAmpGain())) {
                    return combo;
                }
            }
            // all combinations have to be accounted for ...
            throw new RuntimeException("Could not find combo for gain '" + gain +
                        "' and mode '" + mode + "'");
        }
    }
    */

    /**
     * CCD Gain indicates which gain mode to use
     */
    public enum AmpGain implements DisplayableSpType, LoggableSpType, SequenceableSpType {
        LOW("Low"),
        HIGH("High"),
        ;

        public static final AmpGain DEFAULT = AmpGain.LOW;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "gainChoice");

        private String _displayValue;

        AmpGain(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String logValue() {
            return _displayValue;
        }

        /** Return an AmpName by name **/
        public static AmpGain getAmpGain(String name) {
            return AmpGain.getAmpGain(name, AmpGain.DEFAULT);
        }

        /** Return an AmpGain by name giving a value to return upon error **/
        public static AmpGain getAmpGain(String name, AmpGain nvalue) {
            return SpTypeUtil.oldValueOf(AmpGain.class, name, nvalue);
        }
    }

    /**
     * CCD ReadoutSpead indicates speed of CCD readout.
     */
    public enum AmpReadMode implements DisplayableSpType, LoggableSpType, SequenceableSpType {
        SLOW("Slow", "slow"),
        FAST("Fast", "fast"),
        ;

        private String _displayValue;
        private String _logValue;

        public static final AmpReadMode DEFAULT = SLOW;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "ampReadMode");

        AmpReadMode(String displayValue, String logValue) {
            _displayValue = displayValue;
            _logValue     = logValue;
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


        /** Return an AmpSpeed by name **/
        public static AmpReadMode getAmpReadMode(String name) {
            return AmpReadMode.getAmpReadMode(name, DEFAULT);
        }

        /** Return an AmpSpeed by name giving a value to return upon error **/
        public static AmpReadMode getAmpReadMode(String name, AmpReadMode nvalue) {
            return SpTypeUtil.oldValueOf(AmpReadMode.class, name, nvalue);
        }
    }

    /**
     * CCD Bin factor.
     */
    public enum Binning implements DisplayableSpType, LoggableSpType, SequenceableSpType {
        ONE(1),
        TWO(2),
        FOUR(4),
        ;

        public static final Binning DEFAULT = Binning.ONE;

        private int _value;

        Binning(int value) {
            _value = value;
        }

        public String displayValue() {
            return String.valueOf(_value);
        }

        public String sequenceValue() {
            return displayValue();
        }

        public String logValue() {
            return displayValue();
        }

        /** Return the integer binning value **/
        public int getValue() {
            return _value;
        }

        /** Return a Binning by name **/
        public static Binning getBinning(String name) {
            return Binning.getBinning(name, Binning.DEFAULT);
        }

        /** Return a Binning by name giving a value to return upon error **/
        public static Binning getBinning(String name, Binning nvalue) {
            return SpTypeUtil.oldValueOf(Binning.class, name, nvalue);
        }

        /** Return a Binning value by index **/
        public static Binning getBinningByIndex(int index) {
            return SpTypeUtil.valueOf(Binning.class, index, DEFAULT);
        }
    }

    /**
     * Disperser Order
     */
    public enum Order implements DisplayableSpType, LoggableSpType, SequenceableSpType {
        ZERO("0"),
        ONE("1"),
        TWO("2"),
        ;

        public static final Order DEFAULT = Order.ONE;

        private String _displayValue;

        Order(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String logValue() {
            return _displayValue;
        }

        /** Return an Order by name **/
        public static Order getOrder(String name) {
            return Order.getOrder(name, DEFAULT);
        }

        /** Return an Order by name giving a value to return upon error **/
        public static Order getOrder(String name, Order nvalue) {
            return SpTypeUtil.oldValueOf(Order.class, name, nvalue);
        }

        /** Return an Order by index **/
        public static Order getOrderByIndex(int index) {
            return SpTypeUtil.valueOf(Order.class, index, DEFAULT);
        }
    }


    /**
     * UseNS is True if using nod & shuffle, otherwise False
     * (XXX Using true/false instead of yes/no for backward compatibility)
     */
    public enum UseNS implements DisplayableSpType, LoggableSpType, SequenceableSpType {
        TRUE("Yes"),
        FALSE("No"),
        ;

        public final static UseNS DEFAULT = FALSE;

        private String _displayValue;

        UseNS(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String logValue() {
            return _displayValue;
        }

        /** Return a UseNS by name **/
        public static UseNS getUseNS(String name) {
            return getUseNS(name, DEFAULT);
        }

        /** Return a UseNS by name giving a value to return upon error **/
        public static UseNS getUseNS(String name, UseNS nvalue) {
            if ("true".equals(name)) return TRUE;
            if ("false".equals(name)) return FALSE;
            return SpTypeUtil.oldValueOf(UseNS.class, name, nvalue);
        }
    }

    /**
     * FP unit mode indicates a custom mask is in use or a built in
     * is in use.
     */
    public enum FPUnitMode implements DisplayableSpType, LoggableSpType, SequenceableSpType {
        BUILTIN("Builtin"),
        CUSTOM_MASK("Custom Mask"),
        ;

        public static final FPUnitMode DEFAULT = FPUnitMode.BUILTIN;

        private String _displayValue;

        FPUnitMode(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String logValue() {
            return _displayValue;
        }

        /** Return a FPUnitMode by name **/
        public static FPUnitMode getFPUnitMode(String name) {
            return getFPUnitMode(name, DEFAULT);
        }

        /** Return a FPUnitMode by name giving a value to return upon error **/
        public static FPUnitMode getFPUnitMode(String name, FPUnitMode nvalue) {
            return SpTypeUtil.oldValueOf(FPUnitMode.class, name, nvalue);
        }
    }

    public static final ROIDescription DEFAULT_BUILTIN_ROID = new ROIDescription(1, 1, 6144, 4608);

    /**
     * BuiltInROI is a class to select from a small set of selected Regions of Interest.
     */
    public enum BuiltinROI implements DisplayableSpType, LoggableSpType, SequenceableSpType, ObsoletableSpType, PartiallyEngineeringSpType {

        FULL_FRAME("Full Frame Readout", new Some<>(DEFAULT_BUILTIN_ROID), "full"),
        CCD2("CCD 2", new Some<>(new ROIDescription(2049, 1, 2048, 4608)), "ccd2"),
        CENTRAL_SPECTRUM("Central Spectrum", new Some<>(new ROIDescription(1, 1792, 6144, 1024)), "cspec"),
        CENTRAL_STAMP("Central Stamp", new Some<>(new ROIDescription(2922, 2154, 300, 300)), "stamp"),
        TOP_SPECTRUM("Top Spectrum", new Some<>(new ROIDescription(1, 3328, 6144, 1024)), "tspec") {
            @Override public boolean isObsolete() { return true; }
        },
        BOTTOM_SPECTRUM("Bottom Spectrum", new Some<>(new ROIDescription(1, 256, 6144, 1024)), "bspec") {
            @Override public boolean isObsolete() { return true; }
        },
        CUSTOM("Custom ROI", None.instance(), "custom") {
            public boolean isEngineering() { return true; }
        },
        ;



        public static final BuiltinROI DEFAULT = FULL_FRAME;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "builtinROI");

        private String _displayValue;
        private Option<ROIDescription> _roid;
        private String _logValue;

        BuiltinROI(String displayValue, Option<ROIDescription> roid, String logValue) {
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

        public Option<ROIDescription> getROIDescription() {
            return _roid;
        }

        /** Return a BuiltinROI by name **/
        public static BuiltinROI getBuiltinROI(String name) {
            return getBuiltinROI(name, DEFAULT);
        }

        /** Return a BuiltinROI by name giving a value to return upon error **/
        public static BuiltinROI getBuiltinROI(String name, BuiltinROI nvalue) {
            return SpTypeUtil.oldValueOf(BuiltinROI.class, name, nvalue);
        }
    }

    /**
     * This class provides a description and storage for a description of a
     * Region of Interest.
     */
    public static final class ROIDescription implements Serializable {
        private final int _xStart;
        private final int _yStart;
        private final int _xSize;
        private final int _ySize;
        private static String[] paramNames = new String[]{"Xmin", "Ymin", "Xrange", "Yrange"};

        /**
         * Constructor for an ROIDescription takes an x and y start along with
         * an x and y size in unbinned pixels.
         */
        public ROIDescription(int xStart, int yStart, int xSize, int ySize) {
                _xStart = xStart;
                _yStart = yStart;
                _xSize = xSize;
                _ySize = ySize;
        }

        public ROIDescription(ParamSet p) {
            this(Integer.parseInt(p.getParam(paramNames[0]).getValue()),
                    Integer.parseInt(p.getParam(paramNames[1]).getValue()),
                    Integer.parseInt(p.getParam(paramNames[2]).getValue()),
                    Integer.parseInt(p.getParam(paramNames[3]).getValue()));
        }

        public ParamSet getParamSet(PioFactory factory, int number) {
            ParamSet p = factory.createParamSet("ROI" + number);
            Pio.addParam(factory, p, paramNames[0], String.valueOf(getXStart()));
            Pio.addParam(factory, p, paramNames[1], String.valueOf(getYStart()));
            Pio.addParam(factory, p, paramNames[2], String.valueOf(getXSize()));
            Pio.addParam(factory, p, paramNames[3], String.valueOf(getYSize()));
            return p;
        }

        public List<IParameter> getSysConfig(int i) {
            List<IParameter> params = new ArrayList<>();
            params.add(DefaultParameter.getInstance("customROI" + i + paramNames[0], getXStart()));
            params.add(DefaultParameter.getInstance("customROI" + i + paramNames[1], getYStart()));
            params.add(DefaultParameter.getInstance("customROI" + i + paramNames[2], getXSize()));
            params.add(DefaultParameter.getInstance("customROI" + i + paramNames[3], getYSize()));
            return params;
        }

        private boolean _isBetween(int val, int low, int high) {
            return val >= low && val <= high;
        }

        /**
         * Return the x start pixel.
         */
        public int getXStart() {
            return _xStart;
        }

        @Override
        public String toString() {
            return (new StringBuilder()).append(paramNames[0]).append(": ").append(_xStart).append(" ").
                    append(paramNames[1]).append(": ").append(_yStart).append(" ").
                    append(paramNames[2]).append(": ").append(_xSize).append(" ").
                    append(paramNames[3]).append(": ").append(_ySize).toString();
        }

        /**
         * Return the y start pixel.
         */
        public int getYStart() {
            return _yStart;
        }

        /**
         * Return the x size in unbinned pixels.
         */
        public int getXSize() {
            return _xSize;
        }

        // Private routine to isolate the factor used to get size
        private int _getFactor(Binning bvalue) {
            int factor = 1;
            if (bvalue == Binning.TWO) {
                factor = 2;
            } else if (bvalue == Binning.FOUR) {
                factor = 4;
            }
            return factor;
        }

        /**
         * Return the x size in pixels given a specific binning value..
         */
        public int getXSize(Binning bvalue) {
            return getXSize() / _getFactor(bvalue);
        }

        /**
         * Return the y size in unbinned pixels.
         */
        public int getYSize() {
            return _ySize;
        }

        /**
         * Return the y size in pixels given a specific binning value..
         */
        public int getYSize(Binning bvalue) {
            return getYSize() / _getFactor(bvalue);
        }

        /**
         * Checks if this overlaps with that
         *
         * @param that
         * @return true if this overlaps with that
         */
        public boolean pixelOverlap(ROIDescription that) {
            //check that columns overlap
            if ((this.getXStart() <= that.getXStart() && (this.getXStart() + this.getXSize() - 1) >= that.getXStart()) ||
                    (that.getXStart() <= this.getXStart() && (that.getXStart() + that.getXSize() - 1) >= this.getXStart())) {
                //check that rows overlap
                if ((this.getYStart() <= that.getYStart() && (this.getYStart() + this.getYSize() - 1) >= that.getYStart()) ||
                        (that.getYStart() <= this.getYStart() && (that.getYStart() + that.getYSize() - 1) >= this.getYStart())) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Checks if this shares rows with that
         *
         * @param that
         * @return true if this shares rows with that
         */
        public boolean rowOverlap(ROIDescription that) {
            if ((this.getYStart() <= that.getYStart() && (this.getYStart() + this.getYSize() - 1) >= that.getYStart()) ||
                    (that.getYStart() <= this.getYStart() && (that.getYStart() + that.getYSize() - 1) >= this.getYStart())) {
                return true;
            }
            return false;
        }

        /**
         * Validate this ROI against a detector size
         *
         * @param detXSize detector width
         * @param detYSize detector  height
         *
         * @return true if ROI is valid, false otherwise
         */
        public boolean validate(int detXSize, int detYSize) {
            if (_xStart < 1 || _xStart > detXSize ||
                    _yStart < 1 || _yStart > detYSize ||
                    _xSize < 1 || _xSize > (detXSize - _xStart + 1) ||
                    _ySize < 1 || _ySize > (detYSize - _yStart + 1)) {
                return false;
            } else {
                return true;
            }
        }

    }

    /**
     * DTAX Offset Values - restricted to +/- 6, zero default
     */
    public enum DTAX implements DisplayableSpType, LoggableSpType, SequenceableSpType {

        MSIX("-6", -6),
        MFIVE("-5", -5),
        MFOUR("-4", -4),
        MTHREE("-3", -3),
        MTWO("-2", -2),
        MONE("-1", -1),
        ZERO("0", 0),
        ONE("1", 1),
        TWO("2", 2),
        THREE("3", 3),
        FOUR("4", 4),
        FIVE("5", 5),
        SIX("6", 6),
        ;

        public static final DTAX DEFAULT = ZERO;

        private String _displayValue;
        private int _dtax;

        DTAX(String displayValue, int value) {
            _displayValue = displayValue;
            _dtax = value;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String logValue() {
            return _displayValue;
        }

        /**
         * The value of the offset as an integer
         * @return int value for this allowed value
         */
        public int intValue() {
            return _dtax;
        }

        /**
         * The minimum x offset
         * @return the minimum DTAX value.
         */
        public static DTAX getMinimumXOffset() {
            return MSIX;
        }

        /**
         * The maximum x offset
         * @return the maximum DTAX value.
         */
        public static DTAX getMaximumXOffset() {
            return SIX;
        }

        /** Return a Port by name **/
        public static DTAX getDTAX(String name) {
            return getDTAX(name, DEFAULT);
        }

        /**
         * Lookup a DTAX by its integer value
         * @param offset the integer value of interest
         * @return DTAX for that integer or the DEFAULT if it's not a good value.
         */
        public static DTAX valueOf(int offset) {
            int index = offset - getMinimumXOffset().intValue();
            DTAX[] allDtax = values();
            if ((index >= 0) && (index < allDtax.length)) {
                return values()[index];
            }
            return DEFAULT;
        }

        /** Return a Port by name giving a value to return upon error **/
        public static DTAX getDTAX(String name, DTAX nvalue) {
            return SpTypeUtil.oldValueOf(DTAX.class, name, nvalue);
        }
    }


    public static final double E2V_NORTH_PIXEL_SIZE = 0.0727;
    public static final double E2V_SOUTH_PIXEL_SIZE = 0.073;
    public static final int E2V_SHUFFLE_OFFSET = 1536; // pixel

    public static final double HAMAMATSU_PIXEL_SIZE = 0.0809;
    public static final int HAMAMATSU_SHUFFLE_OFFSET = 1392; // pixel

    public enum DetectorManufacturer implements DisplayableSpType {
        E2V("E2V", E2V_NORTH_PIXEL_SIZE, E2V_SOUTH_PIXEL_SIZE, E2V_SHUFFLE_OFFSET, 6144, 4608, 4),
        HAMAMATSU("HAMAMATSU", HAMAMATSU_PIXEL_SIZE, HAMAMATSU_PIXEL_SIZE, HAMAMATSU_SHUFFLE_OFFSET, 6144, 4224, 5);

        public static final DetectorManufacturer DEFAULT = HAMAMATSU;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "detectorManufacturer");

        private final String _displayValue;
        private final double _pixelSizeNorth;
        private final double _pixelSizeSouth;
        private final int _defaultShuffleOffsetPixel;
        private final int xSize;
        private final int ySize;
        private final int maxROIs;

        DetectorManufacturer(final String displayValue, final double pixelSizeNorth, final double pixelSizeSouth,
                                     final int offset, final int xSize, final int ySize, final int maxROIs) {
            this._displayValue = displayValue;
            this._pixelSizeNorth = pixelSizeNorth;
            this._pixelSizeSouth = pixelSizeSouth;
            this._defaultShuffleOffsetPixel = offset;
            this.xSize = xSize;
            this.ySize = ySize;
            this.maxROIs = maxROIs;
        }

        public String displayValue() {
            return _displayValue;
        }

        /**
         * arcsec/pixel
         */
        public double pixelSizeNorth() {
            return _pixelSizeNorth;
        }

        /**
         * arcsec/pixel
         */
        public double pixelSizeSouth() {
            return _pixelSizeSouth;
        }

        /**
         * pixels
         */
        public int shuffleOffsetPixels() {
            return _defaultShuffleOffsetPixel;
        }

        public int getXsize() {
            return xSize;
        }

        public int getYsize() {
            return ySize;
        }

        /** Maximum number of "regions of interest". */
        public int getMaxROIs() {
            return maxROIs;
        }
    }


    /**
     * GMOS custom mask slit widths
     */
    public enum CustomSlitWidth implements DisplayableSpType {
        OTHER("Other", 0),
        CUSTOM_WIDTH_0_25("0.25 arcsec", 0.25),
        CUSTOM_WIDTH_0_50("0.50 arcsec", 0.50),
        CUSTOM_WIDTH_0_75("0.75 arcsec", 0.75),
        CUSTOM_WIDTH_1_00("1.00 arcsec", 1.00),
        CUSTOM_WIDTH_1_50("1.50 arcsec", 1.50),
        CUSTOM_WIDTH_2_00("2.00 arcsec", 2.00),
        CUSTOM_WIDTH_5_00("5.00 arcsec", 5.00);

        private String _displayValue;
        private double _width;

        CustomSlitWidth(String displayValue, double width) {
            this._displayValue = displayValue;
            this._width = width;
        }

        /** Returns a value representing this item as it should be displayed to a user. */
        @Override
        public String displayValue() {
            return _displayValue;
        }

        public double getWidth() {
            return _width;
        }

        /** Return a custom slit width value by index **/
        public static CustomSlitWidth getByIndex(int index) {
            return SpTypeUtil.valueOf(CustomSlitWidth.class, index, OTHER);
        }

    }


    /**
     * Immutable Custom ROI List
     */
    public static class CustomROIList implements Serializable {
        private final List<ROIDescription> rois;

        public static CustomROIList create() {
            return new CustomROIList();
        }

        public static CustomROIList create(ParamSet paramSet) {
            final ArrayList<ROIDescription> newList = new ArrayList<>();
            paramSet.getParamSets().stream().map(ROIDescription::new).forEach(newList::add);
            return new CustomROIList(newList);
        }

        private CustomROIList() {
            rois = new ArrayList<>();
        }

        private CustomROIList(ArrayList<ROIDescription> rois) {
            this.rois = rois;
        }

        public List<ROIDescription> get() {
            return Collections.unmodifiableList(rois);
        }

        public CustomROIList add(ROIDescription roi) {
            final ArrayList<ROIDescription> newList = new ArrayList<>(rois);
            newList.add(roi);
            return new CustomROIList(newList);
        }

        public CustomROIList remove(int i) {
            final ArrayList<ROIDescription> newList= new ArrayList<>(rois);
            newList.remove(i);
            return new CustomROIList(newList);
        }

        public CustomROIList remove(ROIDescription roi) {
            return remove(rois.indexOf(roi));
        }

        public CustomROIList update(int i, ROIDescription roi) {
            final ArrayList<ROIDescription> newList= new ArrayList<>(rois);
            newList.remove(i);
            newList.add(i, roi);
            return new CustomROIList(newList);
        }

        public CustomROIList update(ROIDescription oldRoi, ROIDescription newRoi) {
            return update(rois.indexOf(oldRoi), newRoi);
        }

        public ParamSet getParamSet(PioFactory factory, String name) {
            ParamSet p = factory.createParamSet(name);
            for (int i = 0; i < rois.size(); i++) {
                p.addParamSet(rois.get(i).getParamSet(factory,i));
            }
            return p;
        }

        public ROIDescription get(int i){
           return rois.get(i);
        }

        public int size() {
            return rois.size();
        }

        public boolean isEmpty() {
            return rois.isEmpty();
        }

        public List<IParameter> getSysConfig() {
            List<IParameter> params = new ArrayList<>();
            for (int i = 0; i < rois.size(); i++) {
                params.addAll(rois.get(i).getSysConfig(i+1));
            }
            return params;
        }

        public boolean pixelOverlap(){
            for(int i =0;i<rois.size()-1;i++){
                for(int j=i+1;j<rois.size();j++){
                  if(rois.get(i).pixelOverlap(rois.get(j))){
                      return true;
                  }
                }
            }
            return false;
        }

        public boolean rowOverlap(){
            for(int i =0;i<rois.size()-1;i++){
                for(int j=i+1;j<rois.size();j++){
                    if(rois.get(i).rowOverlap(rois.get(j))){
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * (0/:rois) { _ + _.getYSize }
         */
        public int totalUnbinnedRows() {
            return rois.stream().reduce(0, (rows, roi) -> rows + roi.getYSize(), (a,b) -> a+b);
        }
    }
}
