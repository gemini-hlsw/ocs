package edu.gemini.spModel.gemini.flamingos2;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.config.injector.ConfigInjector;
import edu.gemini.spModel.config.injector.obswavelength.ObsWavelengthCalc2;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.PreImagingType;
import edu.gemini.spModel.data.YesNoType;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKeyProvider;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.CalibrationKeyImpl;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyFlamingos2;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.gemini.parallacticangle.ParallacticAngleSupportInst;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeProvider;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.ictd.*;
import edu.gemini.spModel.inst.ElectronicOffsetProvider;
import edu.gemini.spModel.inst.ScienceAreaGeometry;
import edu.gemini.spModel.inst.VignettableScienceAreaInstrument;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obs.plannedtime.CommonStepCalculator;
import edu.gemini.spModel.obs.plannedtime.ExposureCalculator;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTimeGroup;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.Category;
import edu.gemini.spModel.obscomp.InstConfigInfo;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.obscomp.ItcOverheadProvider;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.obsComp.TargetObsCompConstants;
import edu.gemini.spModel.telescope.IssPort;
import edu.gemini.spModel.telescope.IssPortProvider;
import edu.gemini.spModel.telescope.PosAngleConstraint;
import edu.gemini.spModel.telescope.PosAngleConstraintAware;
import edu.gemini.spModel.type.*;
import edu.gemini.spModel.util.SPTreeUtil;

import java.beans.PropertyDescriptor;
import java.time.Duration;
import java.util.*;

import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_CONFIG_NAME;
import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY;
import static edu.gemini.spModel.seqcomp.SeqConfigNames.TELESCOPE_KEY;

public final class Flamingos2 extends ParallacticAngleSupportInst
        implements PropertyProvider, GuideProbeProvider, IssPortProvider, ElectronicOffsetProvider,
        PlannedTime.StepCalculator, PosAngleConstraintAware, CalibrationKeyProvider, VignettableScienceAreaInstrument,
        ItcOverheadProvider {

    // for serialization
    private static final long serialVersionUID = 4L;

    /**
      * Flamingos2 Dispersers.
      */
    public enum Disperser implements DisplayableSpType, SequenceableSpType, LoggableSpType {

        NONE("None", "none", None.DOUBLE),
        R1200JH("R=1200 (J + H) grism", "R1200JH", new Some<>(1.3385)),
        R1200HK("R=1200 (H + K) grism", "R1200HK", new Some<>(1.9)),
        R3000("R=3000 (J or H or K) grism", "R3000", new Some<>(1.65)),
        ;

        /** The default Disperser value **/
        public static final Disperser DEFAULT = NONE;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "disperser");

        private final String _displayName;
        private final String _logValue;
        private final Option<Double> _wavelength;  // in um

        Disperser(String name, String logValue, Option<Double> wavelength) {
            _displayName = name;
            _logValue    = logValue;
            _wavelength  = wavelength;
        }

        public String displayValue() {
            return _displayName;
        }

        public Option<Double> getWavelength() {
            return _wavelength;
        }

        public String sequenceValue() {
            return name();
        }

        public String logValue() {
            return _logValue;
        }

        public String toString() {
            return displayValue();
        }

        // REL-1522
        public static Option<Disperser> byName(String name) {
            for (Disperser m: values()) {
                if (m.displayValue().equals(name)) {
                    return new Some<>(m);
                }
            }
            return None.instance();
        }
    }

    /**
     * Filters
     */
    public enum Filter implements DisplayableSpType, SequenceableSpType, LoggableSpType, ObsoletableSpType, IctdType {

        OPEN("Open", "Open", new Some<>(1.6),                     Ictd.unavailable()) {
            @Override public boolean isObsolete() {
                return true;
            }
        },
        Y("Y (1.02 um)", "Y", new Some<>(1.02),                   Ictd.track("Y")),
        F1056("F1056 (1.056 um)", "F1056", new Some<>(1.056),     Ictd.track("F1056")),
        F1063("F1063 (1.063 um)", "F1063", new Some<>(1.063),     Ictd.track("F1063")),
        J_LOW("J-low (1.15 um)", "J-low", new Some<>(1.15),       Ictd.track("J-low")),
        J("J (1.25 um)", "J", new Some<>(1.25),                   Ictd.track("J")),
        H("H (1.65 um)", "H", new Some<>(1.65),                   Ictd.track("H")),
        K_LONG("K-long (2.20 um)", "K-long", new Some<>(2.20),    Ictd.track("K-long")),
        K_SHORT("K-short (2.15 um)", "K-short", new Some<>(2.15), Ictd.track("K-short")),
        K_BLUE("K-blue (2.06 um)", "K-blue", new Some<>(2.06),    Ictd.track("K-blue")),
        K_RED("K-red (2.31 um)", "K-red", new Some<>(2.31),       Ictd.track("K-red")),
        JH("JH (spectroscopic)", "JH", new Some<>(1.3385),        Ictd.track("JH")),
        HK("HK (spectroscopic)", "HK", new Some<>(1.9),           Ictd.track("HK")),
        DARK("Dark", "Dark", None.DOUBLE,                         Ictd.track("Dark")) {
            @Override public boolean isObsolete() {
                return true;
            }
        };

        /** The default Filter value **/
        public static final Filter DEFAULT = H;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "filter");

        private final String _displayName;
        private final String _logValue;
        private final Option<Double> _wavelength;  // in um
        private final IctdTracking _ictd;

        Filter(String name, String logValue, Option<Double> wavelength, IctdTracking ictd) {
            _displayName = name;
            _logValue    = logValue;
            _wavelength  = wavelength;
            _ictd        = ictd;
        }

        /** Return the filter's effective wavelength, if known, otherwise null **/
        public Option<Double> getWavelength() {
            return _wavelength;
        }

        public String displayValue() {
            return _displayName;
        }

        public String sequenceValue() {
            return name();
        }

        public String logValue() {
            return _logValue;
        }

        @Override
        public IctdTracking ictdTracking() {
            return _ictd;
        }

        public String toString() {
            return displayValue();
        }

        // REL-1522
        public static Option<Filter> byName(String name) {
            for (Filter m: values()) {
                if (m.displayValue().equals(name)) {
                    return new Some<>(m);
                }
            }
            return None.instance();
        }
    }

    /**
     * Lyot Wheel Values
     */
    public enum LyotWheel implements DisplayableSpType, SequenceableSpType, LoggableSpType, ObsoletableSpType  {


        // f/16:  plate scale = 1.61 arcsec/mm;  pixel scale=0.18 arcsec/pixel
        // f/32:  plate scale = 0.805 arcsec/mm; pixel scale =0.09 arcsec/pixel
        // If the Lyot wheel is set to HartmannA or HartmannB, the
        // FOV should just be a point at the base position (this is not a
        // scientifically useful option, but is used for focusing)

        OPEN("f/16 (open)", "f/16", 1.61, 0.18),
        HIGH("f/32 MCAO high background", "f/32 high", 0.805, 0.09) {
            @Override public boolean isObsolete() { return true; }
        },
        LOW("f/32 MCAO low background", "f/32 low", 0.805, 0.09) {
            @Override public boolean isObsolete() { return true; }
        },
        GEMS("f/33 (Gems)", "f/33 Gems", 0.784, 0.09) {
            @Override public boolean isObsolete() { return true; }
        },
        GEMS_UNDER("f/33 (GeMS under-sized)", "GeMS under", 0.784, 0.09),
        GEMS_OVER("f/33 (GeMS over-sized)", "GeMS over", 0.784, 0.09),
        H1("Hartmann A (H1)") {
            @Override public boolean isObsolete() { return true; }
        },
        H2("Hartmann B (H2)") {
            @Override public boolean isObsolete() { return true; }
        };

        /** The default LyotWheel value **/
        public static LyotWheel DEFAULT = OPEN;

        // The pixel scale to use with this setting in arcsec/pixel
        private final double _pixelScale;

        // The plate scale to use with this setting in arcsec/mm
        private final double _plateScale;

        private final String _displayName;
        private final String _logValue;

        LyotWheel(String name, String logValue, double plateScale, double pixelScale) {
            _displayName = name;
            _logValue    = logValue;
            _plateScale  = plateScale;
            _pixelScale  = pixelScale;
        }

        LyotWheel(String name) {
            this(name, name, 0., 0.);
        }

        /**
          * Returns the plate scale to use with this setting in arcsec/mm
          */
         public double getPlateScale() {
             return _plateScale;
         }

        /**
          * Returns the pixel scale to use with this setting in arcsec/pixel
          */
         public double getPixelScale() {
             return _pixelScale;
         }

        public String displayValue() {
            return _displayName;
        }

        public String logValue() {
            return _logValue;
        }

        public String sequenceValue() {
            return name();
        }

        public String toString() {
            return displayValue();
        }

        // REL-1522
        public static Option<LyotWheel> byName(String name) {
            for (LyotWheel m: values()) {
                if (m.displayValue().equals(name)) {
                    return new Some<>(m);
                }
            }
            return None.instance();
        }
    }

    /**
     * Focal Plan Unit support.
     */
    public enum FPUnit implements DisplayableSpType, SequenceableSpType, LoggableSpType, IctdType {

        FPU_NONE("Imaging (none)", "none",                     0, Decker.IMAGING,   Ictd.installed()),
        LONGSLIT_1("1-pix longslit", "longslit_1",             1, Decker.LONG_SLIT, Ictd.track("1 pix longslit")),
        LONGSLIT_2("2-pix longslit", "longslit_2",             2, Decker.LONG_SLIT, Ictd.track("2 pix longslit")),
        LONGSLIT_3("3-pix longslit", "longslit_3",             3, Decker.LONG_SLIT, Ictd.track("3 pix longslit")),
        LONGSLIT_4("4-pix longslit", "longslit_4",             4, Decker.LONG_SLIT, Ictd.track("4 pix longslit")),
        LONGSLIT_6("6-pix longslit", "longslit_6",             6, Decker.LONG_SLIT, Ictd.track("6 pix longslit")),
        LONGSLIT_8("8-pix longslit", "longslit_8",             8, Decker.LONG_SLIT, Ictd.track("8 pix longslit")),
        PINHOLE("2-pix pinhole grid", "pinhole",               0, Decker.IMAGING,   Ictd.track("2 pix pinhole grid")),
        SUBPIX_PINHOLE("subpix pinhole grid", "subpixPinhole", 0, Decker.IMAGING,   Ictd.track("Subpix pinhole grid")),
        CUSTOM_MASK("Custom Mask", "custom",                   0, Decker.MOS,       Ictd.installed());

        /** The default FPUnit value **/
        public static final FPUnit DEFAULT = FPU_NONE;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "fpu");

        // Shortened log value
        private final String _logValue;

        private final String _displayValue;

        private final int _slitWidth;
        private final Decker _decker;
        private final IctdTracking _ictd;

        // initialize with the name
        FPUnit(String name, String logValue, int slitWidth, Decker decker, IctdTracking ictd) {
            _displayValue = name;
            _logValue     = logValue;
            _slitWidth    = slitWidth;
            _decker       = decker;
            _ictd         = ictd;
        }

        /**
         * Returns true if this is a longslit
         */
        public boolean isLongslit() {
            return _decker == Decker.LONG_SLIT;
        }

        /**
         * Returns the slit width, or 0 if this is not a slit
         */
        public int getSlitWidth() {
            return _slitWidth;
        }

        /**
         * Test to see if  imaging.  This checks for the
         *  no FPU inserted.
         */
        public static boolean isImaging(FPUnit fpu) {
            return fpu == FPUnit.FPU_NONE;
        }

        public Decker getDecker() {
            return _decker;
        }

        @Override
        public IctdTracking ictdTracking() {
            return _ictd;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return name();
        }

        public String logValue() {
            return _logValue;
        }


        public String toString() {
            return displayValue();
        }

        // REL-1522
        public static Option<FPUnit> byName(String name) {
            for (FPUnit m: values()) {
                if (m.displayValue().equals(name)) {
                    return new Some<>(m);
                }
            }
            return None.instance();
        }
    }

    /**
     * Custom slit widths
     * The current smartgcal design did not anticipate custom masks with arbitrary slit widths and
     * the lookup mechanism in use today only works with the F2 FPUnits implemented as enums.
     * In order to be able to use smartgcals for custom masks, we currently only support a limited set
     * of custom slits which each correspond to one of the F2 FPUnits. The same is true for GMOS btw.
     * Andy S. is aware of this and we plan to revise the smartgcal design at some point in the future.
     */
    public enum CustomSlitWidth implements DisplayableSpType, SequenceableSpType {
        OTHER("Other", FPUnit.CUSTOM_MASK),
        CUSTOM_WIDTH_1_PIX("1 Pixel", FPUnit.LONGSLIT_1),
        CUSTOM_WIDTH_2_PIX("2 Pixel", FPUnit.LONGSLIT_2),
        CUSTOM_WIDTH_3_PIX("3 Pixel", FPUnit.LONGSLIT_3),
        CUSTOM_WIDTH_4_PIX("4 Pixel", FPUnit.LONGSLIT_4),
        CUSTOM_WIDTH_6_PIX("6 Pixel", FPUnit.LONGSLIT_6),
        CUSTOM_WIDTH_8_PIX("8 Pixel", FPUnit.LONGSLIT_8);

        public static final CustomSlitWidth DEFAULT = OTHER;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "customSlitWidth");

        private final String displayValue;
        private final FPUnit fpUnit;

        CustomSlitWidth(final String name, final FPUnit smartgcalUnit) {
            this.displayValue = name;
            this.fpUnit       = smartgcalUnit;
        }
        public String displayValue() {
            return displayValue;
        }
        public Option<Integer> width() {
            switch (fpUnit) {
                case CUSTOM_MASK:   return None.instance();
                default:            return new Some<>(fpUnit.getSlitWidth());
            }
        }
        public FPUnit smartgcalFPUnit() {
            return fpUnit;
        }
        public String sequenceValue() {
            return name();
        }
        public String toString() {
            return displayValue();
        }

    }

    /**
     * Read Mode
     */
    public enum ReadMode implements StandardSpType {
        BRIGHT_OBJECT_SPEC() {
            public String displayValue() { return "Bright Object";}
            public String description()  { return "Strong Source";}
            public String logValue()     { return "bright";       }

            public double minimumExpTimeSec()    { return  1.5; }
            public double recomendedExpTimeSec() { return  5.0; }
            public double readoutTimeSec()       { return  8.0; }
            public int readCount()               { return  1;   }
            public double readNoise()            { return 11.7; }
        },

        MEDIUM_OBJECT_SPEC() {
            public String displayValue() { return "Medium Object";}
            public String description()  { return "Medium Source";}
            public String logValue()     { return "medium";       }

            public double minimumExpTimeSec()    { return  6.0; }
            public double recomendedExpTimeSec() { return 21.0; }
            public double readoutTimeSec()       { return 14.0; }
            public int readCount()               { return  4;   }
            public double readNoise()            { return  6.0; }

        },

        FAINT_OBJECT_SPEC() {
            public String displayValue() { return "Faint Object";}
            public String description()  { return "Weak Source"; }
            public String logValue()     { return "faint";       }

            public double minimumExpTimeSec()    { return 12.0; }
            public double recomendedExpTimeSec() { return 85.0; }
            public double readoutTimeSec()       { return 20.0; }
            public int readCount()               { return  8;   }
            public double readNoise()            { return  5.0; }

            public String formatReadNoise() {
                return "<" + super.formatReadNoise();
            }
        },
        ;


        /** The default ReadMode value */
        public static final ReadMode DEFAULT = FAINT_OBJECT_SPEC;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "readMode");

        public abstract String displayValue();
        public abstract String description();

        public abstract double minimumExpTimeSec();
        public abstract double recomendedExpTimeSec();
        public abstract double readoutTimeSec();

        public abstract int readCount();
        public abstract double readNoise();

        public String formatReadNoise() {
            return String.format("%3.1f e- @ 77K", readNoise());
        }

        public String sequenceValue() {
            return name();
        }

        public abstract String logValue();

        public String toString() {
            return displayValue();
        }

        // REL-1522
        public static Option<ReadMode> byName(String name) {
            for (ReadMode m: values()) {
                if (m.displayValue().equals(name)) {
                    return new Some<>(m);
                }
            }
            return None.instance();
        }
    }

    public enum WindowCover implements StandardSpType {
        OPEN("Open"),
        CLOSE("Close"),
        ;

        private final String displayValue;

        WindowCover(String displayValue) { this.displayValue = displayValue; }
        public String description() { return displayValue; }
        public String displayValue() { return displayValue; }
        public String logValue() { return displayValue; }
        public String sequenceValue() { return name(); }
        public String toString() { return displayValue; }

        public static Option<WindowCover> valueOf(String tag, Option<WindowCover> def) {
            return SpTypeUtil.optionValueOf(WindowCover.class, tag).orElse(def);
        }
    }

    public enum Decker implements StandardSpType {
        IMAGING("Imaging"),
        LONG_SLIT("Long Slit"),
        MOS("MOS"),
        ;

        private final String displayValue;

        Decker(String displayValue) { this.displayValue = displayValue; }
        public String description() { return displayValue; }
        public String displayValue() { return displayValue; }
        public String logValue() { return displayValue; }
        public String sequenceValue() { return name(); }
        public String toString() { return displayValue; }

        public static Decker valueOf(String name, Decker nvalue) {
            return SpTypeUtil.oldValueOf(Decker.class, name, nvalue);
        }
    }

    public enum ReadoutMode implements StandardSpType {
        SCIENCE("Science"),
        ENGINEERING("Engineering"),
        ;

        private final String displayValue;

        ReadoutMode(String displayValue) { this.displayValue = displayValue; }
        public String description() { return displayValue; }
        public String displayValue() { return displayValue; }
        public String logValue() { return displayValue; }
        public String sequenceValue() { return name(); }
        public String toString() { return displayValue; }

        public static Option<ReadoutMode> valueOf(String tag, Option<ReadoutMode> def) {
            return SpTypeUtil.optionValueOf(ReadoutMode.class, tag).orElse(def);
        }
    }

    public enum Reads implements StandardSpType {
        READS_1( 1),
        READS_3( 3),
        READS_4( 4),
        READS_5( 5),
        READS_6( 6),
        READS_7( 7),
        READS_8( 8),
        READS_9( 9),
        READS_10(10),
        READS_11(11),
        READS_12(12),
        READS_13(13),
        READS_14(14),
        READS_15(15),
        READS_16(16),
        ;

        private final int reads;

        Reads(int reads) { this.reads = reads; }
        public String description() { return displayValue(); }
        public String displayValue() { return String.valueOf(reads); }
        public String logValue() { return displayValue(); }
        public String sequenceValue() { return name(); }
        public String toString() { return displayValue(); }
        public int getCount() { return reads; }

        public static Option<Reads> valueOf(String tag, Option<Reads> def) {
            return SpTypeUtil.optionValueOf(Reads.class, tag).orElse(def);
        }
    }

    /**
     * This obs component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.INSTRUMENT_FLAMINGOS2;

    public static final ISPNodeInitializer<ISPObsComponent, Flamingos2> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new Flamingos2(), c -> new Flamingos2CB(c));

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<>();
    public  static final Map<String, PropertyDescriptor> PROPERTY_MAP     = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    //Properties
    public static final PropertyDescriptor DISPERSER_PROP;
    public static final PropertyDescriptor FILTER_PROP;
    public static final PropertyDescriptor READMODE_PROP;
    public static final PropertyDescriptor LYOT_WHEEL_PROP;
    public static final PropertyDescriptor FPU_PROP;
    public static final PropertyDescriptor FPU_MASK_PROP;
    public static final PropertyDescriptor CUSTOM_SLIT_WIDTH_PROP;
    public static final PropertyDescriptor PORT_PROP;
    public static final PropertyDescriptor EXPOSURE_TIME_PROP;
    public static final PropertyDescriptor POS_ANGLE_PROP;
    public static final PropertyDescriptor POS_ANGLE_CONSTRAINT_PROP;
    public static final PropertyDescriptor MOS_PREIMAGING_PROP;
    public static final PropertyDescriptor USE_ELECTRONIC_OFFSETTING_PROP;
    public static final PropertyDescriptor WINDOW_COVER_PROP;
    public static final PropertyDescriptor DECKER_PROP;
    public static final PropertyDescriptor READOUT_MODE_PROP;
    public static final PropertyDescriptor READS_PROP;

    /**
    * The name of the Flamingos2 instrument configuration
    */
    public static final String INSTRUMENT_NAME_PROP = "Flamingos2";

    // The number of seconds below which fractional exposure times are permitted
    public static final int FRACTIONAL_EXP_TIME_MAX = 65;

    // Science and OI FOV rotation and flip from config file
    private static final Map<IssPort,Angle[]> FOV_ROTATION;
    private static final Map<IssPort,boolean[]> FLIP;

    /**
     * Set the configuration for rotation and flip
     *
     * @param port the port to which this config applies
     * @param gems whether this config applies with(true) or without(false) gems
     * @param rotation rotation Angle
     * @param flip whether to flip X axis or not
     */
    public static synchronized void setFlipRotationConfig(IssPort port, boolean gems, Angle rotation, boolean flip){
        FOV_ROTATION.get(port)[gems?1:0]=rotation;
        FLIP.get(port)[gems?1:0]=flip;
    }

    /**
     * Get whether to flip or not
     *
     * @param gems get config with or without gems
     * @return true, flip, false, dont flip
     */
    public synchronized boolean getFlipConfig(boolean gems){
        return FLIP.get(getIssPort())[gems?1:0];
    }

    /**
     * Get the angle by which to rotate the FOVs
     *
     * @param gems get config with or without gems
     * @return  the rotation angle
     */
    public synchronized Angle getRotationConfig(boolean gems){
        return FOV_ROTATION.get(getIssPort())[gems?1:0];
    }
    // Pre-imaging flag
    public static final boolean DEFAULT_IS_MOS_PREIMAGING = false;

    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        PropertyDescriptor pd;
        pd = PropertySupport.init(propName, Flamingos2.class, query, iter);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }


    static {
        FOV_ROTATION=new HashMap<>();
        FOV_ROTATION.put(IssPort.SIDE_LOOKING,new Angle[]{Angle.ANGLE_0DEGREES,Angle.ANGLE_0DEGREES});
        FOV_ROTATION.put(IssPort.UP_LOOKING,new Angle[]{Angle.ANGLE_0DEGREES,Angle.ANGLE_0DEGREES});
        FLIP=new HashMap<>();
        FLIP.put(IssPort.SIDE_LOOKING,new boolean[]{false,true});
        FLIP.put(IssPort.UP_LOOKING,new boolean[]{true,false});

        final boolean query_yes = true;
        final boolean iter_yes = true;
        final boolean query_no = false;
        final boolean iter_no = false;

        DISPERSER_PROP = initProp(Disperser.KEY.getName(), query_yes, iter_yes);
        FILTER_PROP = initProp(Filter.KEY.getName(), query_yes, iter_yes);
        READMODE_PROP = initProp(ReadMode.KEY.getName(), query_yes, iter_yes);
        LYOT_WHEEL_PROP = initProp("lyotWheel", query_yes, iter_yes);
        FPU_PROP = initProp(FPUnit.KEY.getName(), query_yes, iter_yes);
        FPU_PROP.setDisplayName("Focal Plane Unit");
        FPU_MASK_PROP = initProp("fpuCustomMask", query_no, iter_yes);
        FPU_MASK_PROP.setDisplayName("Custom MDF");
        CUSTOM_SLIT_WIDTH_PROP = initProp(CustomSlitWidth.KEY.getName(), query_no, iter_no);
        CUSTOM_SLIT_WIDTH_PROP.setDisplayName("Slit Width");
        PORT_PROP = initProp(IssPortProvider.PORT_PROPERTY_NAME, query_no, iter_no);
        MOS_PREIMAGING_PROP = initProp("mosPreimaging", query_yes, iter_no);

        EXPOSURE_TIME_PROP = initProp("exposureTime", query_no, iter_yes);
        POS_ANGLE_PROP     = initProp("posAngle", query_no, iter_no);
        POS_ANGLE_CONSTRAINT_PROP = initProp("posAngleConstraint", query_no, iter_no);

        USE_ELECTRONIC_OFFSETTING_PROP = initProp("useElectronicOffsetting", query_no, iter_no);

        WINDOW_COVER_PROP = initProp("windowCover", query_no, iter_yes);
        WINDOW_COVER_PROP.setExpert(true);
        PropertySupport.setWrappedType(WINDOW_COVER_PROP, WindowCover.class);

        // The decker is "volatile" in that it is automatically updated when
        // the FPU is changed.
        DECKER_PROP = initProp("decker", query_no, iter_yes);
        DECKER_PROP.setExpert(true);
        PropertySupport.setVolatile(DECKER_PROP, true);
        PropertySupport.setWrappedType(DECKER_PROP, Decker.class);

        READOUT_MODE_PROP = initProp("readoutMode", query_no, iter_yes);
        READOUT_MODE_PROP.setExpert(true);
        PropertySupport.setWrappedType(READOUT_MODE_PROP, ReadoutMode.class);

        READS_PROP = initProp("reads", query_no, iter_yes);
        READS_PROP.setExpert(true);
        PropertySupport.setWrappedType(READS_PROP, Reads.class);
    }


    private PosAngleConstraint _posAngleConstraint = PosAngleConstraint.FIXED;

    private Disperser _disperser = Disperser.DEFAULT;
    private Filter _filter = Filter.DEFAULT;
    private ReadMode _readMode = ReadMode.DEFAULT;
    private LyotWheel _lyotWheel = LyotWheel.DEFAULT;
    private IssPort _port = IssPort.DEFAULT;

    private FPUnit _fpu = FPUnit.DEFAULT;
    private String _fpuMaskLabel = EMPTY_STRING;
    private CustomSlitWidth customSlitWidth = CustomSlitWidth.DEFAULT;

    private Option<WindowCover> windowCover = None.instance();
    private Decker decker = _fpu.getDecker();
    private Option<ReadoutMode> readoutMode = None.instance();
    private Option<Reads> reads = None.instance();

    //for the MOS pre-imaging flag
    private boolean _isMosPreimaging = DEFAULT_IS_MOS_PREIMAGING;

    private boolean eOffsetting = false;

    public Flamingos2() {
        super(SP_TYPE);
        // Override the default exposure time
        _exposureTime = _readMode.recomendedExpTimeSec();
    }

    /**
     * Implementation of the clone method.
     */
    public Object clone() {
        // No problems cloning here since private variables are immutable
        return super.clone();
    }

    @Override
    public Set<Site> getSite() {
        return Site.SET_GS;
    }

    public String getPhaseIResourceName() {
        return "gemFlamingos2";
    }

    /**
     * Get the science area in arcsec x arcsec.  For now, this must
     * be a rectangular region.
     */
    public double[] getScienceArea() {
        return F2ScienceAreaGeometry.javaScienceAreaDimensions(this);
    }

    public double getRecommendedExposureTimeSecs() {
        return getReadMode().recomendedExpTimeSec();
    }

    public double getMinimumExposureTimeSecs() {
        return getReadMode().minimumExpTimeSec();
    }

    public static double getFilterChangeOverheadSec() {
        return 50.0;
    }

    public static double getDisperserChangeOverheadSec() {
        return 40.0;
    }

    public static double getFpuChangeOverheadSec() {
        return 95.0;
    }

    @Override public CategorizedTimeGroup calc(final Config cur, final Option<Config> prev) {
        final Collection<CategorizedTime> times = new ArrayList<>();

        if (PlannedTime.isUpdated(cur, prev, FPUnit.KEY)) {
            times.add(CategorizedTime.fromSeconds(Category.CONFIG_CHANGE, getFpuChangeOverheadSec(), "FPU"));
        }
        if (PlannedTime.isUpdated(cur, prev, Filter.KEY)) {
            times.add(CategorizedTime.fromSeconds(Category.CONFIG_CHANGE, getFilterChangeOverheadSec(), "Filter"));
        }
        if (PlannedTime.isUpdated(cur, prev, Disperser.KEY)) {
            times.add(CategorizedTime.fromSeconds(Category.CONFIG_CHANGE, getDisperserChangeOverheadSec(), "Disperser"));
        }

        final ReadMode mode = (ReadMode) cur.getItemValue(ReadMode.KEY);
        times.add(CategorizedTime.fromSeconds(Category.READOUT, mode.readoutTimeSec()));
        times.add(CategorizedTime.fromSeconds(Category.EXPOSURE, ExposureCalculator.instance.exposureTimeSec(cur)));

        times.add(getWriteTime()); // REL-1678

        return CommonStepCalculator.instance.calc(cur, prev).addAll(times);
    }

    private static final Duration IMAGING_SETUP_TIME_OIWFS = Duration.ofMinutes(8);

    private static final Duration IMAGING_SETUP_TIME_PWFS2 = Duration.ofMinutes(6);

    private static final ItemKey GUIDE_WITH_OIWFS_KEY = new ItemKey(TELESCOPE_KEY, TargetObsCompConstants.GUIDE_WITH_OIWFS_PROP);

    public static Duration getImagingSetup(ISPObservation obs) {
        return usesF2Oiwfs(obs) ? IMAGING_SETUP_TIME_OIWFS : IMAGING_SETUP_TIME_PWFS2;
    }

    /**
     * For ITC.
     * @deprecated config is a key-object collection and is thus not type-safe. It is meant for ITC only.
     */
    @Deprecated
    public static Duration getImagingSetup(Config conf) {
            return ImOption.apply(conf.getItemValue(GUIDE_WITH_OIWFS_KEY))
                    .exists(g -> ((GuideOption) g).isActive()) ? IMAGING_SETUP_TIME_OIWFS : IMAGING_SETUP_TIME_PWFS2;
        }


    // Imaging setup differs based on the guide probe in use, OI vs. anything
    // else (PWFS2 presumably).  See REL-1678.
    private static boolean usesF2Oiwfs(ISPObservation obs) {
        final ISPObsComponent obsComp = SPTreeUtil.findTargetEnvNode(obs);
        if (obsComp == null) return false;

        final TargetObsComp dataObj   = (TargetObsComp) obsComp.getDataObject();
        final TargetEnvironment tenv  = dataObj.getTargetEnvironment();

        final GuideGroup gg = tenv.getPrimaryGuideGroup();

        final Option<GuideProbeTargets> gptOpt = gg.get(Flamingos2OiwfsGuideProbe.instance);
        return gptOpt.exists(gpt -> gpt.getPrimary().isDefined());
    }

    public static Duration getSpectroscopySetup() {
        return Duration.ofMinutes(20);
	}

	public static Duration getCustomMaskSetup() {
        return Duration.ofMinutes(30);
    }

    private static final Duration REACQUISITION_TIME = Duration.ofMinutes(6);

    @Override
    public Duration getReacquisitionTime(ISPObservation obs) { return REACQUISITION_TIME; }

    /**
     * Return the setup time in seconds before observing can begin
     */
    @Override
    public Duration getSetupTime(ISPObservation obs) {
        if (isImaging()) return getImagingSetup(obs);
        else if (getFpu() == FPUnit.CUSTOM_MASK) return getCustomMaskSetup();
        return getSpectroscopySetup();
    }

    /**
     * for ITC overheads
     * @deprecated config is a key-object collection and is thus not type-safe. It is meant for ITC only.
     */
    @Deprecated @Override
    public Duration getSetupTime(Config conf) {
        if (isImagingConfig(conf)) return getImagingSetup(conf);
        return getSpectroscopySetup();
    }

    @Override
    public Duration getReacquisitionTime(Config conf) { return REACQUISITION_TIME; }


    /**
     * Is the instrument in imaging mode.
     */
    private static boolean isImagingConfig(FPUnit fpu, Disperser disperser) {
        return (fpu == FPUnit.FPU_NONE) && (disperser == Disperser.NONE);
    }

    /**
     * For ITC.
     * @deprecated config is a key-object collection and is thus not type-safe. It is meant for ITC only.
     */
    @Deprecated
    private static boolean isImagingConfig(Config conf) {
        return isImagingConfig(
                (FPUnit) conf.getItemValue(FPUnit.KEY),
                (Disperser) conf.getItemValue(Disperser.KEY));
    }

    private boolean isImaging() {
        return isImagingConfig(_fpu, _disperser);
    }


    /**
     * Get the disperser.
     */
    public Disperser getDisperser() {
        return _disperser;
    }


    /**
     * Set the disperser.
     */
    public void setDisperser(Disperser newValue) {
        Disperser oldValue = getDisperser();
        if (oldValue != newValue) {
            _disperser = newValue;
            firePropertyChange(DISPERSER_PROP, oldValue, newValue);
            if (_disperser != Disperser.NONE) _setMosPreimaging(false);
        }
    }

    /**
     * Get the filter.
     */
    public Filter getFilter() {
        return _filter;
    }

    /**
     * Set the filter.
     */
    public void setFilter(Filter newValue) {
        Filter oldValue = getFilter();
        if (oldValue != newValue) {
            _filter = newValue;
            firePropertyChange(FILTER_PROP, oldValue, newValue);
        }
    }

    /**
     * Determines the observing wavelength for the disperser and filter
     * combination in this data object.
     */
    public Option<Double> getObservingWavelength() {
        return getObservingWavelength(_disperser, _filter);
    }

    /**
     * Determines the observing wavelength to use for the given combination of
     * disperser and filter.  Implements the algorithm specified in SCI-0206.
     */
    public static Option<Double> getObservingWavelength(Disperser d, Filter f) {
        // When in spectroscopy mode, if the filter is OPEN, use the grism
        // wavelength.
        if ((d != Disperser.NONE) && (f == Filter.OPEN)) return d.getWavelength();

        // Otherwise, use the filter wavelength.
        return f.getWavelength();
    }

    public static final ConfigInjector<String> WAVELENGTH_INJECTOR = ConfigInjector.create(
        new ObsWavelengthCalc2<Disperser, Filter>() {
            @Override public PropertyDescriptor descriptor1() { return DISPERSER_PROP; }
            @Override public PropertyDescriptor descriptor2() { return FILTER_PROP; }
            @Override public String calcWavelength(Disperser d, Filter f) {
                return getObservingWavelength(d, f).map(String::valueOf).getOrNull();
            }
        }
    );

    /**
     * Get the detector read mode.
     */
    public ReadMode getReadMode() {
        return _readMode;
    }

    /**
     * Set the read mode.
     */
    public void setReadMode(ReadMode newValue) {
        ReadMode oldValue = getReadMode();
        if (oldValue != newValue) {
            _readMode = newValue;
            firePropertyChange(READMODE_PROP, oldValue, newValue);
        }
    }

    /**
     * Get the Lyot Wheel.
     */
    public LyotWheel getLyotWheel() {
        return _lyotWheel;
    }

    /**
     * Set the Lyot Wheel.
     */
    public void setLyotWheel(LyotWheel newValue) {
        LyotWheel oldValue = getLyotWheel();
        if (oldValue != newValue) {
            _lyotWheel = newValue;
            firePropertyChange(LYOT_WHEEL_PROP, oldValue, newValue);
        }
    }

    /**
      * Get the ISS Port
      */
     @Override public IssPort getIssPort() {
         return _port;
     }

     /**
      * Set the Port.
      */
     @Override public void setIssPort(IssPort newValue) {
         IssPort oldValue = getIssPort();
         if (oldValue != newValue) {
             _port = newValue;
             firePropertyChange(PORT_PROP, oldValue, newValue);
         }
     }

    public PosAngleConstraint getPosAngleConstraint() {
        return (_posAngleConstraint == null) ? PosAngleConstraint.FIXED : _posAngleConstraint;
    }

    public void setPosAngleConstraint(PosAngleConstraint newValue) {
        PosAngleConstraint oldValue = getPosAngleConstraint();
        if (oldValue != newValue) {
            _posAngleConstraint = newValue;
            firePropertyChange(POS_ANGLE_CONSTRAINT_PROP.getName(), oldValue, newValue);
        }
    }

    private void _setPosAngleConstraint(final String name) {
        final PosAngleConstraint oldValue = getPosAngleConstraint();
        try {
            _posAngleConstraint = PosAngleConstraint.valueOf(name);
        } catch (Exception ex) {
            _posAngleConstraint = oldValue;
        }
    }

    private void _setPosAngleConstraint(final PosAngleConstraint pac) {
        _posAngleConstraint = pac;
    }


    /**
     * Get the FPUnit.
     */
    public FPUnit getFpu() {
        return _fpu;
    }

    /**
     * Set the FPUnit.
     */
    public void setFpu(FPUnit newValue) {
        FPUnit oldValue = getFpu();
        if (oldValue != newValue) {
            _fpu = newValue;
            firePropertyChange(FPU_PROP, oldValue, newValue);
            if (_fpu != FPUnit.FPU_NONE) _setMosPreimaging(false);
            setDecker(_fpu.getDecker());
        }
    }

    /**
     * Set an FPU Custom Mask label.
     **/
    public void setFpuCustomMask(String newValue) {
        if (newValue != null) {
            newValue = newValue.trim(); // remove white space
        }
        String oldValue = getFpuCustomMask();
        if (!oldValue.equals(newValue)) {
            _fpuMaskLabel = newValue;
            firePropertyChange(FPU_MASK_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the FPUMask label.  Note that this is meaningful only
     * if the user has placed the <@link FPUMode> to indicate the
     * <code><@link CUSTOM_MASK FPUnitMode.CUSTOM_MASK></code>
     * mode.
     */
    public String getFpuCustomMask() {
        return _fpuMaskLabel;
    }

    public CustomSlitWidth getCustomSlitWidth() {
        return customSlitWidth;
    }

    public void setCustomSlitWidth(final CustomSlitWidth newValue) {
        final CustomSlitWidth oldValue = getCustomSlitWidth();
        if (oldValue != newValue) {
            customSlitWidth = newValue;
            firePropertyChange(CUSTOM_SLIT_WIDTH_PROP.getName(), oldValue, newValue);
        }
    }

    /** Return yes if this is a MOS Pre-imaging observation */
    public YesNoType getMosPreimaging() {
      return _isMosPreimaging ? YesNoType.YES : YesNoType.NO;
    }

    /** Translate the preimaging value into a dedicated enum (needed for QPT and QV) */
    public PreImagingType getPreImaging() {
        return _isMosPreimaging ? PreImagingType.TRUE : PreImagingType.FALSE;
    }

    /** Set to Yes to mark this imaging observation as MOS Pre-imaging */
    public void setMosPreimaging(YesNoType newValue) {
      //this is another case of a stupid enum made for the browser to work
      _setMosPreimaging(newValue.toBoolean());
    }

    private void _setMosPreimaging(boolean newValue) {
      boolean oldValue = _isMosPreimaging;
      if (newValue != oldValue) {
          _isMosPreimaging = newValue;
          firePropertyChange(MOS_PREIMAGING_PROP.getName(),
                  oldValue, newValue);

          if (_isMosPreimaging) {
              setFpu(FPUnit.FPU_NONE);
              setDisperser(Disperser.NONE);
          }
      }
    }

    public Option<WindowCover> getWindowCover() {
        return windowCover;
    }

    public void setWindowCover(Option<WindowCover> newValue) {
        Option<WindowCover> oldValue = getWindowCover();
        if (!oldValue.equals(newValue)) {
            windowCover = newValue;
            firePropertyChange(WINDOW_COVER_PROP.getName(), oldValue, newValue);
        }
    }

    public Decker getDecker() {
        return decker;
    }

    public void setDecker(Decker newValue) {
        Decker oldValue = getDecker();
        if (!oldValue.equals(newValue)) {
            decker = newValue;
            firePropertyChange(DECKER_PROP.getName(), oldValue, newValue);
        }
    }

    public Option<ReadoutMode> getReadoutMode() {
        return readoutMode;
    }

    public void setReadoutMode(Option<ReadoutMode> newValue) {
        Option<ReadoutMode> oldValue = getReadoutMode();
        if (!oldValue.equals(newValue)) {
            readoutMode = newValue;
            firePropertyChange(READOUT_MODE_PROP.getName(), oldValue, newValue);
        }
    }

    public Option<Reads> getReads() {
        return reads;
    }

    public void setReads(Option<Reads> newValue) {
        Option<Reads> oldValue = getReads();
        if (!oldValue.equals(newValue)) {
            reads = newValue;
            firePropertyChange(READS_PROP.getName(), oldValue, newValue);
        }
    }

    public boolean getUseElectronicOffsetting() {
        return eOffsetting;
    }

    public void setUseElectronicOffsetting(boolean newValue) {
        boolean oldValue = getUseElectronicOffsetting();
        if (oldValue != newValue) {
            this.eOffsetting = newValue;
            firePropertyChange(USE_ELECTRONIC_OFFSETTING_PROP.getName(), oldValue, newValue);
        }
    }

    public GuideProbe getElectronicOffsetGuider() {
        return Flamingos2OiwfsGuideProbe.instance;
    }

    /**
     * Return a parameter set describing the current state of this object.
     */
    public ParamSet getParamSet(final PioFactory factory) {
        final ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, DISPERSER_PROP, _disperser.name());
        Pio.addParam(factory, paramSet, FILTER_PROP, _filter.name());
        Pio.addParam(factory, paramSet, READMODE_PROP, _readMode.name());
        Pio.addParam(factory, paramSet, LYOT_WHEEL_PROP, _lyotWheel.name());
        Pio.addParam(factory, paramSet, PORT_PROP, _port.name());

        Pio.addParam(factory, paramSet, POS_ANGLE_CONSTRAINT_PROP.getName(), getPosAngleConstraint().name());

        Pio.addParam(factory, paramSet, FPU_PROP, _fpu.name());
        if (_fpu == FPUnit.CUSTOM_MASK) {
            Pio.addParam(factory, paramSet, FPU_MASK_PROP, _fpuMaskLabel);
            Pio.addParam(factory, paramSet, CUSTOM_SLIT_WIDTH_PROP.getName(), customSlitWidth.name());
        }

        Pio.addParam(factory, paramSet, MOS_PREIMAGING_PROP.getName(), getMosPreimaging().name());

        Pio.addBooleanParam(factory, paramSet, USE_ELECTRONIC_OFFSETTING_PROP.getName(), eOffsetting);

        if (!windowCover.isEmpty()) {
            Pio.addParam(factory, paramSet, WINDOW_COVER_PROP.getName(), windowCover.getValue().name());
        }
        Pio.addParam(factory, paramSet, DECKER_PROP.getName(), decker.name());
        if (!readoutMode.isEmpty()) {
            Pio.addParam(factory, paramSet, READOUT_MODE_PROP.getName(), readoutMode.getValue().name());
        }
        if (!reads.isEmpty()) {
            Pio.addParam(factory, paramSet, READS_PROP.getName(), reads.getValue().name());
        }

        return paramSet;
    }


    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(final ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v;
        v = Pio.getValue(paramSet, DISPERSER_PROP);
        if (v != null) setDisperser(Disperser.valueOf(v));
        v = Pio.getValue(paramSet, FILTER_PROP);
        if (v != null) setFilter(Filter.valueOf(v));
        v = Pio.getValue(paramSet, READMODE_PROP);
        if (v != null) setReadMode(ReadMode.valueOf(v));

        // REL-2090: Special workaround for elimination of former PositionAngleMode, since functionality has been
        // merged with PosAngleConstraint but we still need legacy code.
        v = Pio.getValue(paramSet, POS_ANGLE_CONSTRAINT_PROP.getName());
        final String pam = Pio.getValue(paramSet, "positionAngleMode");
        if ("MEAN_PARALLACTIC_ANGLE".equals(pam))
            _setPosAngleConstraint(PosAngleConstraint.PARALLACTIC_ANGLE);
        else if (v != null)
            _setPosAngleConstraint(v);

        v = Pio.getValue(paramSet, LYOT_WHEEL_PROP);
        if (v != null) setLyotWheel(LyotWheel.valueOf(v));

        v = Pio.getValue(paramSet, PORT_PROP);
        if (v == null) {
            // UX-810: When replacing the F2 port enum implementation with the IssPort enum the property name
            // changed from "port" to "issPort". For backwards compatibility we have to support the old name.
            v = Pio.getValue(paramSet, "port");
        }
        if (v != null) setIssPort(IssPort.valueOf(v));

        v = Pio.getValue(paramSet, FPU_PROP);
        if (v != null) setFpu(FPUnit.valueOf(v));

        v = Pio.getValue(paramSet, FPU_MASK_PROP);
        if (v != null) setFpuCustomMask(v);

        v = Pio.getValue(paramSet, CUSTOM_SLIT_WIDTH_PROP.getName());
        if (v != null) setCustomSlitWidth(CustomSlitWidth.valueOf(v));

        setUseElectronicOffsetting(Pio.getBooleanValue(paramSet, USE_ELECTRONIC_OFFSETTING_PROP.getName(), false));

        v = Pio.getValue(paramSet, MOS_PREIMAGING_PROP.getName());
        if (v != null) setMosPreimaging(YesNoType.getYesNoType(v, YesNoType.NO));

        v = Pio.getValue(paramSet, WINDOW_COVER_PROP.getName());
        if (v != null) setWindowCover(WindowCover.valueOf(v, getWindowCover()));

        v = Pio.getValue(paramSet, DECKER_PROP.getName());
        if (v != null) setDecker(Decker.valueOf(v, getDecker()));

        v = Pio.getValue(paramSet, READOUT_MODE_PROP.getName());
        if (v != null) setReadoutMode(ReadoutMode.valueOf(v, getReadoutMode()));

        v = Pio.getValue(paramSet, READS_PROP.getName());
        if (v != null) setReads(Reads.valueOf(v, getReads()));
    }

    private static final Angle PWFS1_VIG = Angle.arcmins(5.8);
    private static final Angle PWFS2_VIG = Angle.arcmins(5.3);

    @Override public Angle pwfs1VignettingClearance(ObsContext ctx) { return PWFS1_VIG; }
    @Override public Angle pwfs2VignettingClearance(ObsContext ctx) { return PWFS2_VIG; }

    /**
     * Return the configuration for this component.
     */
    public ISysConfig getSysConfig() {
        final ISysConfig sc = new DefaultSysConfig(INSTRUMENT_CONFIG_NAME);

        // Fill in the current values.
        sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion()));
        sc.putParameter(DefaultParameter.getInstance(POS_ANGLE_PROP, getPosAngle()));
        sc.putParameter(DefaultParameter.getInstance(InstConstants.EXPOSURE_TIME_PROP, getExposureTime()));

        sc.putParameter(DefaultParameter.getInstance(DISPERSER_PROP, getDisperser()));
        sc.putParameter(DefaultParameter.getInstance(FILTER_PROP, getFilter()));
        sc.putParameter(DefaultParameter.getInstance(READMODE_PROP, getReadMode()));
        sc.putParameter(DefaultParameter.getInstance(LYOT_WHEEL_PROP, getLyotWheel()));
        sc.putParameter(DefaultParameter.getInstance(PORT_PROP, getIssPort()));

        sc.putParameter(DefaultParameter.getInstance(FPU_PROP, getFpu()));
        sc.putParameter(DefaultParameter.getInstance(CUSTOM_SLIT_WIDTH_PROP.getName(), getCustomSlitWidth()));
        if (getFpu() == FPUnit.CUSTOM_MASK) {
            sc.putParameter(DefaultParameter.getInstance(FPU_MASK_PROP, getFpuCustomMask()));
        }

        sc.putParameter(DefaultParameter.getInstance(MOS_PREIMAGING_PROP.getName(), getMosPreimaging()));

        sc.putParameter(DefaultParameter.getInstance(USE_ELECTRONIC_OFFSETTING_PROP.getName(), getUseElectronicOffsetting()));

        if (!getWindowCover().isEmpty()) {
            sc.putParameter(DefaultParameter.getInstance(WINDOW_COVER_PROP.getName(), getWindowCover().getValue()));
        }
        sc.putParameter(DefaultParameter.getInstance(DECKER_PROP.getName(), getDecker()));
        if (!getReadoutMode().isEmpty()) {
            sc.putParameter(DefaultParameter.getInstance(READOUT_MODE_PROP.getName(), getReadoutMode().getValue()));
        }
        if (!getReads().isEmpty()) {
            sc.putParameter(DefaultParameter.getInstance(READS_PROP.getName(), getReads().getValue()));
        }

        return sc;
    }


    /**
     * Return a list of InstConfigInfo objects describing the instrument's
     * queryable configuration parameters.
     */
    public static List<InstConfigInfo> getInstConfigInfo() {
        List<InstConfigInfo> configInfo = new LinkedList<>();

        configInfo.add(new InstConfigInfo(DISPERSER_PROP));
        configInfo.add(new InstConfigInfo(FILTER_PROP));
        configInfo.add(new InstConfigInfo(READMODE_PROP));
        configInfo.add(new InstConfigInfo(LYOT_WHEEL_PROP));
        configInfo.add(new InstConfigInfo(FPU_PROP));

        return configInfo;
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    private static final Collection<GuideProbe> GUIDE_PROBES = GuideProbeUtil.instance.createCollection(Flamingos2OiwfsGuideProbe.instance);

    public Collection<GuideProbe> getGuideProbes() {
        return GUIDE_PROBES;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CalibrationKey extractKey(final ISysConfig instrument) {
        // -- get all values needed for smartgcal lookup
        final Disperser disperser = (Disperser) get(instrument, DISPERSER_PROP);
        final Filter filter = (Filter) get(instrument, FILTER_PROP);
        // -- for custom masks we need to translate the slit width to a known FPU enum value with the same slit width
        final FPUnit fpUnit = (FPUnit) get(instrument, FPU_PROP);
        final FPUnit gcalLookupUnit;
        if (fpUnit == FPUnit.CUSTOM_MASK) {
            gcalLookupUnit = ((CustomSlitWidth) get(instrument, CUSTOM_SLIT_WIDTH_PROP.getName())).smartgcalFPUnit();
        } else {
            gcalLookupUnit = fpUnit;
        }
        // create and return lookup key
        ConfigKeyFlamingos2 config = new ConfigKeyFlamingos2(disperser, filter, gcalLookupUnit);
        return new CalibrationKeyImpl(config);
    }

    @Override
    public boolean isCompatibleWithMeanParallacticAngleMode() {
        return !(_fpu == FPUnit.FPU_NONE || _fpu == FPUnit.CUSTOM_MASK);
    }

    @Override
    public ImList<PosAngleConstraint> getSupportedPosAngleConstraints() {
        return DefaultImList.create(PosAngleConstraint.FIXED,
                                    PosAngleConstraint.FIXED_180,
                                    PosAngleConstraint.UNBOUNDED,
                                    PosAngleConstraint.PARALLACTIC_ANGLE,
                                    PosAngleConstraint.PARALLACTIC_OVERRIDE);
    }

    @Override
    public boolean allowUnboundedPositionAngle() {
        // Note that we disable unbounded position angle as an option for MOS preimaging and FPU Custom Mask.
        boolean isMos        = getMosPreimaging() == YesNoType.YES;
        boolean isCustomMask = getFpu() == FPUnit.CUSTOM_MASK;
        return !isMos && !isCustomMask;
    }

    // REL-814 Preserve the FPU Custom Mask Name
    @Override
    public void restoreScienceDetails(final SPInstObsComp oldData) {
        super.restoreScienceDetails(oldData);
        if (oldData instanceof Flamingos2) {
            final Flamingos2 oldF2 = (Flamingos2)oldData;
            setFpuCustomMask(oldF2.getFpuCustomMask());
        }
    }

    @Override
    public ScienceAreaGeometry getVignettableScienceArea() {
        return F2ScienceAreaGeometry$.MODULE$;
    }
}
