package edu.gemini.spModel.gemini.gmos;

import edu.gemini.spModel.ictd.*;
import edu.gemini.spModel.type.SpTypeUtil;

/**
 *
 */
public class GmosSouthType {
    private GmosSouthType() {
        // defeat construction
    }

            /**
     * Translation Stage options.
     */
    public enum StageModeSouth implements GmosCommonType.StageMode {
        NO_FOLLOW("Do Not Follow"),
        FOLLOW_XYZ("Follow in XYZ(focus)"),
        FOLLOW_XY("Follow in XY") {
            @Override public boolean isObsolete() { return true; }
        },
        FOLLOW_Z_ONLY("Follow in Z Only")
        ;

        public static final StageModeSouth DEFAULT = StageModeSouth.FOLLOW_XYZ;

        private String _displayValue;

        StageModeSouth(String displayValue) {
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

        /** Return a StageMode by name **/
        public static StageModeSouth getStageMode(String name) {
            return getStageMode(name, DEFAULT);
        }

        /** Return a StageMode by name giving a value to return upon error **/
        public static StageModeSouth getStageMode(String name, StageModeSouth nvalue) {
            return SpTypeUtil.oldValueOf(StageModeSouth.class, name, nvalue);
        }
    }

    public static GmosCommonType.StageModeBridge<StageModeSouth> STAGE_MODE_BRIDGE = new GmosCommonType.StageModeBridge<StageModeSouth>() {
        public Class<StageModeSouth> getPropertyType() {
            return StageModeSouth.class;
        }

        public StageModeSouth getDefaultValue() {
            return StageModeSouth.DEFAULT;
        }

        public StageModeSouth parse(String name, StageModeSouth defaultValue) {
            return StageModeSouth.getStageMode(name, defaultValue);
        }
    };


    public enum DisperserSouth implements GmosCommonType.Disperser, IctdType {
        // Mirror isn't tracked but is always installed.
        MIRROR(     "Mirror",     "mirror",    0, Ictd.installed()),
        B1200_G5321("B1200_G5321", "B1200", 1200, Ictd.track("B1200")),
        R831_G5322(  "R831_G5322",  "R831",  831, Ictd.track( "R831")),
        B600_G5323(  "B600_G5323",  "B600",  600, Ictd.track( "B600")),
        R600_G5324(  "R600_G5324",  "R600",  600, Ictd.track( "R600")),
        B480_G5327(  "B480_G5327",  "B480",  480, Ictd.track( "B480")),
        R400_G5325(  "R400_G5325",  "R400",  400, Ictd.track( "R400")),
        R150_G5326(  "R150_G5326",  "R150",  150, Ictd.track( "R150")),
        ;

        /** The default Disperser value **/
        public static DisperserSouth DEFAULT = MIRROR;

        private final String displayValue;
        private final String logValue;
        private final int    rulingDensity;        // [lines/mm]
        private final IctdTracking ictd;

        DisperserSouth(final String displayValue, final String logValue, final int rulingDensity, final IctdTracking ictd) {
            this.displayValue  = displayValue;
            this.logValue      = logValue;
            this.rulingDensity = rulingDensity;
            this.ictd          = ictd;
        }

        public String displayValue() {
            return displayValue;
        }

        public String logValue() {
            return logValue;
        }

        @Override
        public IctdTracking ictdTracking() {
            return ictd;
        }

        public String sequenceValue() {
            return displayValue;
        }

        public String toString() {
            return displayValue();
        }

        public boolean isMirror() {
            return (this == MIRROR);
        }

        public int rulingDensity() {
            return rulingDensity;
        }

        /** Return a Disperser by index **/
        public static DisperserSouth getDisperserByIndex(int index) {
            return SpTypeUtil.valueOf(DisperserSouth.class, index, DEFAULT);
        }

        /** Return a Disperser by name **/
        public static DisperserSouth getDisperser(String name) {
            return getDisperser(name, DEFAULT);
        }

        /** Return a Disperser by name giving a value to return upon error **/
        public static DisperserSouth getDisperser(String name, DisperserSouth nvalue) {
            return SpTypeUtil.oldValueOf(DisperserSouth.class, name, nvalue);
        }
    }


    public static GmosCommonType.DisperserBridge<DisperserSouth> DISPERSER_BRIDGE = new GmosCommonType.DisperserBridge<DisperserSouth>() {
        public Class<DisperserSouth> getPropertyType() {
            return DisperserSouth.class;
        }

        public DisperserSouth getDefaultValue() {
            return DisperserSouth.DEFAULT;
        }

        public DisperserSouth parse(String name, DisperserSouth defaultValue) {
            return DisperserSouth.getDisperser(name, defaultValue);
        }
    };

    // Individual filters used in multiple enum definitions.
    private static final IctdTracking Track_HartA = Ictd.track("Hartmann A");
    private static final IctdTracking Track_HartB = Ictd.track("Hartmann B");
    private static final IctdTracking Track_g     = Ictd.track("g");
    private static final IctdTracking Track_r     = Ictd.track("r");
    private static final IctdTracking Track_i     = Ictd.track("i");
    private static final IctdTracking Track_z     = Ictd.track("z");
    private static final IctdTracking Track_GG455 = Ictd.track("GG455");
    private static final IctdTracking Track_OG515 = Ictd.track("OG515");
    private static final IctdTracking Track_RG610 = Ictd.track("RG610");
    private static final IctdTracking Track_RG780 = Ictd.track("RG780");
    private static final IctdTracking Track_CaT   = Ictd.track("CaT");

    public enum FilterSouth implements GmosCommonType.Filter, IctdType {
        NONE("None", "none", "none",                 Ictd.installed()),
        u_G0332("u_G0332", "u", "0.350",             Ictd.track("u")),
        g_G0325("g_G0325", "g", "0.475",             Track_g),
        r_G0326("r_G0326", "r", "0.630",             Track_r),
        i_G0327("i_G0327", "i", "0.780",             Track_i),
        z_G0328("z_G0328", "z", "0.925",             Track_z),
        Z_G0343("Z_G0343", "Z", "0.876",             Ictd.track("Z")),
        Y_G0344("Y_G0344", "Y", "1.01",              Ictd.track("Y")),
        GG455_G0329("GG455_G0329", "GG455", "0.680", Track_GG455),
        OG515_G0330("OG515_G0330", "OG515", "0.710", Track_OG515),
        RG610_G0331("RG610_G0331", "RG610", "0.750", Track_RG610),
        RG780_G0334("RG780_G0334", "RG780", "0.850", Track_RG780),
        CaT_G0333("CaT_G0333", "CaT", "0.860",       Track_CaT),
        HartmannA_G0337_r_G0326("HartmannA_G0337 + r_G0326", "r+HartA", "0.630", Track_r.plus(Track_HartA)),
        HartmannB_G0338_r_G0326("HartmannB_G0338 + r_G0326", "r+HartB", "0.630", Track_r.plus(Track_HartB)),
        g_G0325_GG455_G0329("g_G0325 + GG455_G0329", "g+GG455", "0.506",         Track_g.plus(Track_GG455)),
        g_G0325_OG515_G0330("g_G0325 + OG515_G0330", "g+OG515", "0.536",         Track_g.plus(Track_OG515)),
        r_G0326_RG610_G0331("r_G0326 + RG610_G0331", "r+RG610", "0.657",         Track_r.plus(Track_RG610)),
        i_G0327_RG780_G0334("i_G0327 + RG780_G0334", "i+RG780", "0.819",         Track_i.plus(Track_RG780)),
        i_G0327_CaT_G0333("i_G0327 + CaT_G0333", "i+CaT", "0.815",               Track_i.plus(Track_CaT)),
        z_G0328_CaT_G0333("z_G0328 + CaT_G0333", "z+CaT", "0.890",               Track_z.plus(Track_CaT)),
        Ha_G0336("Ha_G0336", "Ha", "0.656",              Ictd.track("Ha")),
        SII_G0335("SII_G0335", "SII", "0.672",           Ictd.track("SII")),
        HaC_G0337("HaC_G0337", "HaC", "0.662",           Ictd.track("HaC")),
        OIII_G0338("OIII_G0338", "OIII", "0.499",        Ictd.track("OIII")),
        OIIIC_G0339("OIIIC_G0339", "OIIIC", "0.514",     Ictd.track("OIIIC")),
        HeII_G0340("HeII_G0340", "HeII", "0.468",        Ictd.track("HeII")),
        HeIIC_G0341("HeIIC_G0341", "HeIIC", "0.478",     Ictd.track("HeIIC")),
        Lya395_G0342("Lya395_G0342", "Lya395", "0.3955", Ictd.track("Lya395")),
        OVI_G0347("OVI_G0347", "OVI", "0.6835",          Ictd.track("OVI")),
        OVIC_G0348("OVIC_G0348", "OVIC", "0.678",        Ictd.track("OVIC")),
        JWL34_G0350("JWL34_G0350", "JWL34", "0.3361",    Ictd.track("JWL34")),
        JWL38_G0351("JWL38_G0351", "JWL38", "0.3920",    Ictd.track("JWL38")),
        F396N_G0352("F396N_G0352", "F396N", "0.3960",    Ictd.track("F396N")),
        JWL43_G0353("JWL43_G0353", "JWL43", "0.4303",    Ictd.track("JWL43")),
        STRY_G0354("Stry_G0354", "Stry", "0.4671",       Ictd.track("Stry")),
        STRB_G0355("Strb_G0355", "Strb", "0.5460",       Ictd.track("Strb")),
        ;

        public static final FilterSouth DEFAULT = NONE;

        private final String       _displayValue;
        private final String       _logValue;
        private final String       _wavelength;
        private final IctdTracking _ictd;

        FilterSouth(String displayValue, String logValue, String wavelength, IctdTracking ictd) {
            _displayValue = displayValue;
            _logValue     = logValue;
            _wavelength   = wavelength;
            _ictd         = ictd;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String logValue() {
            return  _logValue;
        }

        @Override
        public IctdTracking ictdTracking() {
            return _ictd;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public String getWavelength() {
            return _wavelength;
        }

        public boolean isNone() {
            return this == NONE;
        }

        public String toString() {
            return displayValue();
        }

        /** Return a User filter by index **/
        public static FilterSouth getFilterByIndex(int index) {
            return SpTypeUtil.valueOf(FilterSouth.class, index, DEFAULT);
        }

        /** Return a Filter by name **/
        static public FilterSouth getFilterSouth(String name) {
            return getFilterSouth(name, DEFAULT);
        }

        /** Return a Filter by name giving a value to return upon error **/
        static public FilterSouth getFilterSouth(String name, FilterSouth nvalue) {
            if ("Ha_G0310".equals(name)) return Ha_G0336;
            return SpTypeUtil.oldValueOf(FilterSouth.class, name, nvalue);
        }
    }

    public static GmosCommonType.FilterBridge<FilterSouth> FILTER_BRIDGE = new GmosCommonType.FilterBridge<FilterSouth>() {
        public Class<FilterSouth> getPropertyType() {
            return FilterSouth.class;
        }

        public FilterSouth getDefaultValue() {
            return FilterSouth.DEFAULT;
        }

        public FilterSouth parse(String name, FilterSouth defaultValue) {
            return FilterSouth.getFilterSouth(name, defaultValue);
        }
    };

    public enum FPUnitSouth implements GmosCommonType.FPUnit, IctdType {
        FPU_NONE("None", "none",                               Ictd.installed()),
        LONGSLIT_1("Longslit 0.25 arcsec", 0.25, "0.25arcsec", Ictd.track("0.25arcsec")),
        LONGSLIT_2("Longslit 0.50 arcsec", 0.50, "0.5arcsec",  Ictd.track("0.5arcsec")),
        LONGSLIT_3("Longslit 0.75 arcsec", 0.75, "0.75arcsec", Ictd.track("0.75arcsec")),
        LONGSLIT_4("Longslit 1.00 arcsec", 1.00, "1.0arcsec",  Ictd.track("1.0arcsec")),
        LONGSLIT_5("Longslit 1.50 arcsec", 1.50, "1.5arcsec",  Ictd.track("1.5arcsec")),
        LONGSLIT_6("Longslit 2.00 arcsec", 2.00, "2.0arcsec",  Ictd.track("2.0arcsec")),
        LONGSLIT_7("Longslit 5.00 arcsec", 5.00, "5.0arcsec",  Ictd.track("5.0arcsec")),
        IFU_1("IFU 2 Slits", "IFU-2",                          Ictd.track("IFU-2")) {
            @Override public boolean isWideSlit() { return true; }
        },
        IFU_2("IFU Left Slit (blue)", "IFU-B",                 Ictd.track("IFU-B")),
        IFU_3("IFU Right Slit (red)", "IFU-R",                 Ictd.track("IFU-R")),
        BHROS("bHROS", "bHROS",                                Ictd.track("bHROS")),

        IFU_N("IFU N and S 2 Slits", "IFU-NS-2",               Ictd.track("IFU-NS-2")),
        IFU_N_B("IFU N and S Left Slit (blue)", "IFU-NS-B",    Ictd.track("IFU-NS-B")),
        IFU_N_R("IFU N and S Right Slit (red)", "IFU-NS-R",    Ictd.track("IFU-NS-R")),

        NS_1("N and S 0.50 arcsec", 0.50, "NS0.5arcsec",       Ictd.track("NS0.5arcsec")),
        NS_2("N and S 0.75 arcsec", 0.75, "NS0.75arcsec",      Ictd.track("NS0.75arcsec")),
        NS_3("N and S 1.00 arcsec", 1.00, "NS1.0arcsec",       Ictd.track("NS1.0arcsec")),
        NS_4("N and S 1.50 arcsec", 1.50, "NS1.5arcsec",       Ictd.track("NS1.5arcsec")),
        NS_5("N and S 2.00 arcsec", 2.00, "NS2.0arcsec",       Ictd.track("NS2.0arcsec")),
        CUSTOM_MASK("Custom Mask", "custom",                   Ictd.installed()){
            @Override public boolean isCustom() { return true; }
        },
        ;

        /** The default FPUnit value **/
        public static FPUnitSouth DEFAULT = FPU_NONE;

        private String _displayValue;

        // Shortened log value
        private String _logValue;

        // Slit width in arcsec, if known
        private double _width;

        private final IctdTracking _ictd;


        // initialize with the name and slit width in arcsec
        FPUnitSouth(String displayValue, String logValue, IctdTracking ictd) {
            this(displayValue, -1, logValue, ictd);
        }

        // initialize with the name and slit width in arcsec
        FPUnitSouth(String displayValue, double width, String logValue, IctdTracking ictd) {
            _displayValue = displayValue;
            _width        = width;
            _logValue     = logValue;
            _ictd         = ictd;
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

        @Override
        public IctdTracking ictdTracking() {
            return _ictd;
        }

        /** Return the slit width in arcsec, or -1 if not applicable */
        public double getWidth() {
            return _width;
        }

        /** Return a FPUnit by index **/
        public static FPUnitSouth getFPUnitByIndex(int index) {
            return SpTypeUtil.valueOf(FPUnitSouth.class, index, DEFAULT);
        }

        /** Return a FPUnit by name **/
        public static FPUnitSouth getFPUnit(String name) {
            return getFPUnit(name, DEFAULT);
        }

        /** Return a FPUnit by name giving a value to return upon error **/
        public static FPUnitSouth getFPUnit(String name, FPUnitSouth nvalue) {
            return SpTypeUtil.oldValueOf(FPUnitSouth.class, name, nvalue);
        }

        @Override public boolean isCustom() { return false; }

        /**
         * Test to see if GMOS is imaging.  This checks for the
         *  no FPU inserted.
         */
        public boolean isImaging() {
            return this == FPU_NONE;
        }

        /**
         * Test to see if FPU is in spectroscopic mode.
         */
        public boolean isSpectroscopic() {
            switch (this) {
                case LONGSLIT_1:
                case LONGSLIT_2:
                case LONGSLIT_3:
                case LONGSLIT_4:
                case LONGSLIT_5:
                case LONGSLIT_6:
                case LONGSLIT_7:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Is an IFU selected.
         */
        public boolean isIFU() {
            switch(this) {
                case IFU_1:
                case IFU_2:
                case IFU_3:
                case IFU_N:
                case IFU_N_B:
                case IFU_N_R:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Test to see if FPU is in nod & shuffle mode.
         */
        public boolean isNS() {
            switch (this) {
                case NS_1:
                case NS_2:
                case NS_3:
                case NS_4:
                case NS_5:
                case IFU_N:
                case IFU_N_B:
                case IFU_N_R:
                    return true;
                default:
                    return false;
            }
        }
        /**
         * Is a NS-Slit. See SCT-203, Gmos-rules for this definition
         */
        public boolean isNSslit() {
            switch (this) {
                case NS_1:
                case NS_2:
                case NS_3:
                case NS_4:
                case NS_5:
                    return true;
                default:
                    return false;
            }
        }

        @Override public boolean isWideSlit() {
            return false;
        }

        @Override
        public double getWFSOffset() {

            if (isIFU()) {
                // w is used to center smaller slit on base pos
                double w = IFU_FOV[IFU_FOV_SMALLER_RECT_INDEX].width / 2.0;
                double offset = IFU_FOV_OFFSET + w;

                if (this == GmosSouthType.FPUnitSouth.IFU_2 || this == GmosSouthType.FPUnitSouth.IFU_N_B) {
                    // left slit: shift to center on base pos
                    offset -= w / 2.0;
                } else if (this == GmosSouthType.FPUnitSouth.IFU_3 || this == GmosSouthType.FPUnitSouth.IFU_N_R) {
                    // right slit: shift to center on base pos
                    offset += w / 2.0;
                }

                return offset;
            } else {
                return 0.0;
            }
        }
    }

    public static GmosCommonType.FPUnitBridge<FPUnitSouth> FPUNIT_BRIDGE = new GmosCommonType.FPUnitBridge<FPUnitSouth>() {

        public Class<FPUnitSouth> getPropertyType() {
            return FPUnitSouth.class;
        }

        public FPUnitSouth getDefaultValue() {
            return FPUnitSouth.DEFAULT;
        }

        public FPUnitSouth parse(String name, FPUnitSouth defaultValue) {
            return FPUnitSouth.getFPUnit(name, defaultValue);
        }

        public FPUnitSouth getCustomMask() {
            return FPUnitSouth.CUSTOM_MASK;
        }

        public FPUnitSouth getNone() {
            return FPUnitSouth.FPU_NONE;
        }
    };
}
