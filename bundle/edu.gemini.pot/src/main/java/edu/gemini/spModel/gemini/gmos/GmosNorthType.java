package edu.gemini.spModel.gemini.gmos;

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

    public enum DisperserNorth implements GmosCommonType.Disperser, ObsoletableSpType {
        MIRROR("Mirror", "mirror", 0),
        B1200_G5301("B1200_G5301", "B1200", 1200),
        R831_G5302("R831_G5302", "R831", 831),
        B600_G5303("B600_G5303", "B600", 600) {
            @Override public boolean isObsolete() { return true; }
        },
        B600_G5307("B600_G5307", "B600", 600),
        R600_G5304("R600_G5304", "R600", 600),
        R400_G5305("R400_G5305", "R400", 400),
        R150_G5306("R150_G5306", "R150", 150),
        R150_G5308("R150_G5308", "R150", 150),
        ;

        /** The default Disperser value **/
        public static DisperserNorth DEFAULT = MIRROR;

        private final String displayValue;
        private final String logValue;
        private final int    rulingDensity;

        DisperserNorth(final String displayValue, final String logValue, final int rulingDensity) {
            this.displayValue  = displayValue;
            this.logValue      = logValue;
            this.rulingDensity = rulingDensity;     // [lines/mm]
        }

        public String displayValue() {
            return displayValue;
        }

        public String logValue() {
            return logValue;
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

    public enum FilterNorth implements GmosCommonType.Filter, ObsoletableSpType {
        NONE("None", "none", "none"),
        g_G0301("g_G0301", "g", "0.475"),
        r_G0303("r_G0303", "r", "0.630"),
        i_G0302("i_G0302", "i", "0.780"),
        z_G0304("z_G0304", "z", "0.925"),
        Z_G0322("Z_G0322", "Z", "0.876"),
        Y_G0323("Y_G0323", "Y", "1.01"),
        GG455_G0305("GG455_G0305", "GG455", "0.680"),
        OG515_G0306("OG515_G0306", "OG515", "0.710"),
        RG610_G0307("RG610_G0307", "RG610", "0.750"),
        CaT_G0309("CaT_G0309", "CaT", "0.860"),
        Ha_G0310("Ha_G0310", "Ha", "0.655"),
        HaC_G0311("HaC_G0311", "HaC", "0.662"),
        DS920_G0312("DS920_G0312", "DS920", "0.920"),
        SII_G0317("SII_G0317", "SII", "0.672"),
        OIII_G0318("OIII_G0318", "OIII", "0.499"),
        OIIIC_G0319("OIIIC_G0319", "OIIIC", "0.514"),
        HeII_G0320("HeII_G0320", "HeII", "0.468"),
        HeIIC_G0321("HeIIC_G0321", "HeIIC", "0.478"),
        HartmannA_G0313_r_G0303("HartmannA_G0313 + r_G0303", "r+HartA", "0.630"),
        HartmannB_G0314_r_G0303("HartmannB_G0314 + r_G0303", "r+HartB", "0.630"),
        g_G0301_GG455_G0305("g_G0301 + GG455_G0305", "g+GG455", "0.506"),
        g_G0301_OG515_G0306("g_G0301 + OG515_G0306", "g+OG515", "0.536"),
        r_G0303_RG610_G0307("r_G0303 + RG610_G0307", "r+RG610", "0.657"),
        i_G0302_CaT_G0309("i_G0302 + CaT_G0309", "i+CaT", "0.815"),
        z_G0304_CaT_G0309("z_G0304 + CaT_G0309", "z+CaT", "0.890"),
        u_G0308("u_G0308", "u_G0308", "0.350") {
            @Override public boolean isObsolete() { return true; }
        }
        ;

        // Added 05/10/05: See OT-353 (05/29/05: removed again)
//        u_G0308("u_G0308", "u", "0.350"); // XXX removed
//        DS816_G0313("DS816_G0313", "DS816", "0.816"),
//        OIII_G0338 = new GMOSParams.UserFilter("OIII_G0338", "OIII", "0.499");
//        OIIIC_G0339 = new GMOSParams.UserFilter("OIIIC_G0339", "OIIIC", "0.514");

        public static final FilterNorth DEFAULT = NONE;

        private String _displayValue;
        private String _logValue;
        private String _wavelength;

        FilterNorth(String displayValue, String logValue, String wavelength) {
            _displayValue = displayValue;
            _logValue     = logValue;
            _wavelength   = wavelength;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String logValue() {
            return  _logValue;
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
    public enum FPUnitNorth implements GmosCommonType.FPUnit {
        FPU_NONE("None", "none"),
        LONGSLIT_1("Longslit 0.25 arcsec", 0.25, "0.25arcsec"),
        LONGSLIT_2("Longslit 0.50 arcsec", 0.50, "0.5arcsec"),
        LONGSLIT_3("Longslit 0.75 arcsec", 0.75, "0.75arcsec"),
        LONGSLIT_4("Longslit 1.00 arcsec", 1.00, "1.0arcsec"),
        LONGSLIT_5("Longslit 1.50 arcsec", 1.50, "1.5arcsec"),
        LONGSLIT_6("Longslit 2.00 arcsec", 2.00, "2.0arcsec"),
        LONGSLIT_7("Longslit 5.00 arcsec", 5.00, "5.0arcsec"),
        IFU_1("IFU 2 Slits", "IFU-2") {
            @Override public boolean isWideSlit() { return true; }
        },
        IFU_2("IFU Left Slit (blue)", "IFU-B"),
        IFU_3("IFU Right Slit (red)", "IFU-R"),
        NS_0("N and S 0.25 arcsec", 0.25, "NS0.25arcsec"),
        NS_1("N and S 0.50 arcsec", 0.50, "NS0.5arcsec"),
        NS_2("N and S 0.75 arcsec", 0.75, "NS0.75arcsec"),
        NS_3("N and S 1.00 arcsec", 1.00, "NS1.0arcsec"),
        NS_4("N and S 1.50 arcsec", 1.50, "NS1.5arcsec"),
        NS_5("N and S 2.00 arcsec", 2.00, "NS2.0arcsec"),
        CUSTOM_MASK("Custom Mask", "custom"),
        ;

        /** The default FPUnit value **/
        public static FPUnitNorth DEFAULT = FPU_NONE;

        private String _displayValue;

        // Shortened log value
        private String _logValue;

        // Slit width in arcsec, if known
        private double _width;


        // initialize with the name and slit width in arcsec
        FPUnitNorth(String displayValue, String logValue) {
            this(displayValue, -1, logValue);
        }

        // initialize with the name and slit width in arcsec
        FPUnitNorth(String displayValue, double width, String logValue) {
            _displayValue = displayValue;
            _width = width;
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
