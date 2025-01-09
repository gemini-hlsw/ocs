package edu.gemini.spModel.gemini.gmos;

import edu.gemini.spModel.ictd.*;
import edu.gemini.spModel.type.*;

public class GmosNorthType {

    private GmosNorthType() {
        // defeat construction
    }

    /**
     * Translation Stage options.
     */
    public enum StageModeNorth implements GmosCommonType.StageMode {
        NO_FOLLOW("Do Not Follow"),
        FOLLOW_XYZ("Follow in XYZ(focus)") {
            @Override public boolean isObsolete() { return true; }
        },
        FOLLOW_XY("Follow in XY"),
        FOLLOW_Z_ONLY("Follow in Z Only") {
            @Override public boolean isObsolete() { return true; }
        }
        ;

        public static final StageModeNorth DEFAULT = StageModeNorth.FOLLOW_XY;

        private String _displayValue;

        StageModeNorth(String displayValue) {
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
        public static StageModeNorth getStageMode(String name) {
            return getStageMode(name, DEFAULT);
        }

        /** Return a StageMode by name giving a value to return upon error **/
        public static StageModeNorth getStageMode(String name, StageModeNorth nvalue) {
            return SpTypeUtil.oldValueOf(StageModeNorth.class, name, nvalue);
        }
    }

    public static GmosCommonType.StageModeBridge<StageModeNorth> STAGE_MODE_BRIDGE = new GmosCommonType.StageModeBridge<StageModeNorth>() {
        public Class<StageModeNorth> getPropertyType() {
            return StageModeNorth.class;
        }

        public StageModeNorth getDefaultValue() {
            return StageModeNorth.DEFAULT;
        }

        public StageModeNorth parse(String name, StageModeNorth defaultValue) {
            return StageModeNorth.getStageMode(name, defaultValue);
        }
    };

    public enum DisperserNorth implements GmosCommonType.Disperser, ObsoletableSpType, IctdType {
        // Mirror isn't tracked but is always installed.
        MIRROR(     "Mirror",      "mirror",  0, Ictd.installed()),
        B1200_G5301("B1200",  "B1200",  "G5301", 1200),
        R831_G5302( "R831",   "R831",   "G5302",  831),
        B600_G5303( "B600_G5303",  "B600",  600, Ictd.unavailable()) {
            @Override public boolean isObsolete() { return true; }
        },
        B600_G5307( "B600",   "B600",   "G5307",  600),
        R600_G5304( "R600",   "R600",   "G5304",  600),
        B480_G5309( "B480",   "B480",   "G5309",  480),
        R400_G5305( "R400",   "R400",   "G5305",  400),
        R400_G5310( "R400",   "R400",   "G5310",  400),
        R150_G5306( "R150_G5306",  "R150",  150, Ictd.unavailable()) {
            @Override public boolean isObsolete() { return true; }
        },
        R150_G5308( "R150",   "R150",   "G5308",  150),
        ;

        /** The default Disperser value **/
        public static DisperserNorth DEFAULT = MIRROR;

        private final String       displayValue;
        private final String       logValue;
        private final int          rulingDensity;
        private final IctdTracking ictd;

        DisperserNorth(final String displayValue, final String logValue, final int rulingDensity, final IctdTracking ictd) {
            this.displayValue  = displayValue;
            this.logValue      = logValue;
            this.rulingDensity = rulingDensity;     // [lines/mm]
            this.ictd          = ictd;
        }

        DisperserNorth(final String name, final String logValue, final String geminiID, final int rulingDensity) {
            this.displayValue  = name + "_" + geminiID;
            this.logValue      = logValue;
            this.rulingDensity = rulingDensity;     // [lines/mm]
            this.ictd          = Ictd.trackWithID(name, geminiID);
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

        public boolean isMirror() {
            return (this == MIRROR);
        }

        public int rulingDensity() {
            return rulingDensity;
        }

        public String toString() {
            return displayValue();
        }

        /** Return a Disperser by index **/
        public static DisperserNorth getDisperserByIndex(int index) {
            return SpTypeUtil.valueOf(DisperserNorth.class, index, DEFAULT);
        }

        /** Return a Disperser by name **/
        public static DisperserNorth getDisperser(String name) {
            return getDisperser(name, DEFAULT);
        }

        /** Return a Disperser by name giving a value to return upon error **/
        public static DisperserNorth getDisperser(String name, DisperserNorth nvalue) {
            return SpTypeUtil.oldValueOf(DisperserNorth.class, name, nvalue);
        }
    }

    public static GmosCommonType.DisperserBridge<DisperserNorth> DISPERSER_BRIDGE = new GmosCommonType.DisperserBridge<DisperserNorth>() {
        public Class<DisperserNorth> getPropertyType() {
            return DisperserNorth.class;
        }

        public DisperserNorth getDefaultValue() {
            return DisperserNorth.DEFAULT;
        }

        public DisperserNorth parse(String name, DisperserNorth defaultValue) {
            return DisperserNorth.getDisperser(name, defaultValue);
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
    private static final IctdTracking Track_CaT   = Ictd.track("CaT");

    public enum FilterNorth implements GmosCommonType.Filter, ObsoletableSpType, IctdType {
        NONE("None", "none", "none",                 Ictd.installed()),
        g_G0301("g_G0301", "g", "0.475",             Track_g),
        r_G0303("r_G0303", "r", "0.630",             Track_r),
        i_G0302("i_G0302", "i", "0.780",             Track_i),
        z_G0304("z_G0304", "z", "0.925",             Track_z),
        Z_G0322("Z_G0322", "Z", "0.876",             Ictd.track("Z")), // missing ?
        Y_G0323("Y_G0323", "Y", "1.01",              Ictd.track("Y")), // missing ?
        ri_G0349("ri_G0349", "ri", "0.700",          Ictd.track("ri")),
        GG455_G0305("GG455_G0305", "GG455", "0.680", Track_GG455),
        OG515_G0306("OG515_G0306", "OG515", "0.710", Track_OG515),
        RG610_G0307("RG610_G0307", "RG610", "0.750", Track_RG610),
        CaT_G0309("CaT_G0309", "CaT", "0.860",       Track_CaT),
        Ha_G0310("Ha_G0310", "Ha", "0.655",          Ictd.track("Ha")),
        HaC_G0311("HaC_G0311", "HaC", "0.662",       Ictd.track("HaC")),
        DS920_G0312("DS920_G0312", "DS920", "0.920", Ictd.track("DS920")),
        SII_G0317("SII_G0317", "SII", "0.672",       Ictd.track("SII")),
        OIII_G0318("OIII_G0318", "OIII", "0.499",    Ictd.track("OIII")),
        OIIIC_G0319("OIIIC_G0319", "OIIIC", "0.514", Ictd.track("OIIIC")),
        HeII_G0320("HeII_G0320", "HeII", "0.468",    Ictd.track("HeII")),
        HeIIC_G0321("HeIIC_G0321", "HeIIC", "0.478", Ictd.track("HeIIC")),
        OVI_G0345("OVI_G0345", "OVI", "0.6835",      Ictd.track("OVI")),
        OVIC_G0346("OVIC_G0346", "OVIC", "0.678",    Ictd.track("OVIC")),
        HartmannA_G0313_r_G0303("HartmannA_G0313 + r_G0303", "r+HartA", "0.630", Track_r.plus(Track_HartA)),
        HartmannB_G0314_r_G0303("HartmannB_G0314 + r_G0303", "r+HartB", "0.630", Track_r.plus(Track_HartB)),
        g_G0301_GG455_G0305("g_G0301 + GG455_G0305", "g+GG455", "0.506",         Track_g.plus(Track_GG455)),
        g_G0301_OG515_G0306("g_G0301 + OG515_G0306", "g+OG515", "0.536",         Track_g.plus(Track_OG515)),
        r_G0303_RG610_G0307("r_G0303 + RG610_G0307", "r+RG610", "0.657",         Track_r.plus(Track_RG610)),
        i_G0302_CaT_G0309("i_G0302 + CaT_G0309", "i+CaT", "0.815",               Track_i.plus(Track_CaT)),
        z_G0304_CaT_G0309("z_G0304 + CaT_G0309", "z+CaT", "0.890",               Track_z.plus(Track_CaT)),
        u_G0308("u_G0308", "u_G0308", "0.350",       Ictd.track("u")) {
            @Override public boolean isObsolete() { return true; }
        }
        ;

        public static final FilterNorth DEFAULT = NONE;

        private final String       _displayValue;
        private final String       _logValue;
        private final String       _wavelength;
        private final IctdTracking _ictd;

        FilterNorth(String displayValue, String logValue, String wavelength, IctdTracking ictd) {
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
        public static FilterNorth getFilterByIndex(int index) {
            return SpTypeUtil.valueOf(FilterNorth.class, index, DEFAULT);
        }

        /** Return a Filter by name **/
        static public FilterNorth getFilterNorth(String name) {
            return getFilterNorth(name, DEFAULT);
        }

        /** Return a Filter by name giving a value to return upon error **/
        static public FilterNorth getFilterNorth(String name, FilterNorth nvalue) {
            return SpTypeUtil.oldValueOf(FilterNorth.class, name, nvalue);
        }
    }

    public static GmosCommonType.FilterBridge<FilterNorth> FILTER_BRIDGE = new GmosCommonType.FilterBridge<FilterNorth>() {
        public Class<FilterNorth> getPropertyType() {
            return FilterNorth.class;
        }

        public FilterNorth getDefaultValue() {
            return FilterNorth.DEFAULT;
        }

        public FilterNorth parse(String name, FilterNorth defaultValue) {
            return FilterNorth.getFilterNorth(name, defaultValue);
        }
    };

    /**
     * Focal Plan Unit support.
     */
    public enum FPUnitNorth implements GmosCommonType.FPUnit, IctdType {
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
        NS_0("N and S 0.25 arcsec", 0.25, "NS0.25arcsec",      Ictd.track("NS0.25arcsec")),
        NS_1("N and S 0.50 arcsec", 0.50, "NS0.5arcsec",       Ictd.track("NS0.5arcsec")),
        NS_2("N and S 0.75 arcsec", 0.75, "NS0.75arcsec",      Ictd.track("NS0.75arcsec")),
        NS_3("N and S 1.00 arcsec", 1.00, "NS1.0arcsec",       Ictd.track("NS1.0arcsec")),
        NS_4("N and S 1.50 arcsec", 1.50, "NS1.5arcsec",       Ictd.track("NS1.5arcsec")),
        NS_5("N and S 2.00 arcsec", 2.00, "NS2.0arcsec",       Ictd.track("NS2.0arcsec")),
        CUSTOM_MASK("Custom Mask", "custom",                   Ictd.installed()) {
            @Override public boolean isCustom() { return true; }
        },
        ;

        /** The default FPUnit value **/
        public static FPUnitNorth DEFAULT = FPU_NONE;

        private String _displayValue;

        // Shortened log value
        private String _logValue;

        // Slit width in arcsec, if known
        private double _width;

        private final IctdTracking _ictd;

        // initialize with the name and slit width in arcsec
        FPUnitNorth(String displayValue, String logValue, IctdTracking ictd) {
            this(displayValue, -1, logValue, ictd);
        }

        // initialize with the name and slit width in arcsec
        FPUnitNorth(String displayValue, double width, String logValue, IctdTracking ictd) {
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
        public static FPUnitNorth getFPUnitByIndex(int index) {
            return SpTypeUtil.valueOf(FPUnitNorth.class, index, DEFAULT);
        }

        /** Return a FPUnit by name **/
        public static FPUnitNorth getFPUnit(String name) {
            return getFPUnit(name, DEFAULT);
        }

        /** Return a FPUnit by name giving a value to return upon error **/
        public static FPUnitNorth getFPUnit(String name, FPUnitNorth nvalue) {
            return SpTypeUtil.oldValueOf(FPUnitNorth.class, name, nvalue);
        }

        /**
         * Test to see if GMOS is imaging.  This checks for the
         *  no FPU inserted.
         */
        public boolean isImaging() {
            return this == FPU_NONE;
        }

        @Override public boolean isCustom() { return false; }

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
            return (this == IFU_1 || this == IFU_2 || this == IFU_3);
        }

        /**
         * Test to see if FPU is in nod & shuffle mode.
         */
        public boolean isNS() {
            switch (this) {
                case NS_0:
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

        /**
         * Is a NS-Slit. See SCT-203, Gmos-rules for this definition
         */
        public boolean isNSslit() {
            return isNS();
        }

        @Override public boolean isWideSlit() {
            return false;
        }

        @Override
        public double getWFSOffset() {

            if (isIFU()) {

                // w is used to center larger slit on base pos
                double w = IFU_FOV[IFU_FOV_LARGER_RECT_INDEX].width / 2.0;
                double offset = -IFU_FOV_OFFSET - w;

                if (this == GmosNorthType.FPUnitNorth.IFU_2) {
                    // left slit: shift to center on base pos
                    offset += w / 2.0;
                } else if (this == GmosNorthType.FPUnitNorth.IFU_3) {
                    // right slit: shift to center on base pos
                    offset -= w / 2.0;
                }

                return offset;
            } else {
                return 0.0;
            }
        }

    }

    public static GmosCommonType.FPUnitBridge<FPUnitNorth> FPUNIT_BRIDGE = new GmosCommonType.FPUnitBridge<FPUnitNorth>() {

        public Class<FPUnitNorth> getPropertyType() {
            return FPUnitNorth.class;
        }

        public FPUnitNorth getDefaultValue() {
            return FPUnitNorth.DEFAULT;
        }

        public FPUnitNorth parse(String name, FPUnitNorth defaultValue) {
            return FPUnitNorth.getFPUnit(name, defaultValue);
        }

        public FPUnitNorth getCustomMask() {
            return FPUnitNorth.CUSTOM_MASK;
        }

        public FPUnitNorth getNone() {
            return FPUnitNorth.FPU_NONE;
        }
    };
}
