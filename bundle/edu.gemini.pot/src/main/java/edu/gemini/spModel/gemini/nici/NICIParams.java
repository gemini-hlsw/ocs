package edu.gemini.spModel.gemini.nici;

import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.type.*;
import edu.gemini.skycalc.Angle;
import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY;


import java.util.HashMap;


public final class NICIParams {

    public enum FocalPlaneMask implements DisplayableSpType, SequenceableSpType, ObsoletableSpType {

        OPEN("Open") {
            @Override public boolean isObsolete() { return true; }
        },
        CLEAR("Clear"),
        MASK_1("0.90 arcsec"),
        MASK_2("0.65 arcsec"),
        MASK_3("0.46 arcsec"),
        MASK_4("0.32 arcsec"),
        MASK_5("0.22 arcsec"),
        GRID("Grid"),
        USER("User");

        public static final FocalPlaneMask DEFAULT = CLEAR;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "focalPlaneMask");

        private String _displayValue;

        FocalPlaneMask(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return name();
        }
    }

    public enum PupilMask implements DisplayableSpType, SequenceableSpType {

        BLOCK("Block"),
        OPEN("Open"),
        HUNDRED_PERCENT("100%"),
        NINETY_FIVE_PERCENT("95%"),
        NINETY_PERCENT("90%"),
        EIGHTY_FIVE_PERCENT("85%"),
        EIGHTY_PERCENT("80%"),
        APODIZED("Apodized"),
        NINETY_EIGHT_PERCENT("98%"),
        NINETY_SIX_PERCENT("96%"),
        NINETY_FOUR_PERCENT("94%");

        public static final PupilMask DEFAULT = NINETY_FIVE_PERCENT;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "pupilMask");

        private final String _displayName;

        PupilMask(String name){
            _displayName = name;
        }

        public String displayValue() {
            return _displayName;
        }

        public String sequenceValue() {
            return name();
        }
    }

    private static final Angle DEF_FIXED_ANGLE = new Angle(180, Angle.Unit.DEGREES);

    public enum CassRotator implements DisplayableSpType, SequenceableSpType {

        FIXED("Fixed") {
            public Angle defaultAngle() { return DEF_FIXED_ANGLE; }
            public CassRotator opposite() { return FOLLOW; }
        },
        FOLLOW("Follow") {
            public Angle defaultAngle() { return Angle.ANGLE_0DEGREES; }
            public CassRotator opposite() { return FIXED; }
        };

        public static final CassRotator DEFAULT = FIXED;

        private String _displayName;

        CassRotator(String name) {
            _displayName = name;
        }

        public String sequenceValue() {
            return name();
        }

        public String displayValue() {
            return _displayName;
        }

        public abstract Angle defaultAngle();
        public abstract CassRotator opposite();
    }


    public enum ImagingMode implements DisplayableSpType, SequenceableSpType, ObsoletableSpType {

        MANUAL("Manual"),
        H1SLA("H 1% S,L A"),
        H1SLB("H 1% S,L B"),
        H1SPLA("H 1% Sp,L A"),
        H1SPLB("H 1% Sp,L B"),
        H4SLA("H 4% S,L A"),
        H4SLB("H 4% S,L B"),
        PUPIL_IMAGING("Pupil Imaging") {
            @Override public boolean isObsolete() { return true; }
        }

        ;

        public static final ImagingMode DEFAULT = MANUAL;

        private String _displayValue;

        ImagingMode(String name) {
            _displayValue = name;
        }

        public String sequenceValue() {
            return name();
        }

        public String displayValue() {
            return _displayValue;
        }
    }


    public enum DichroicWheel implements DisplayableSpType, SequenceableSpType {

        H5050_BEAMSPLITTER("H 50-50 Beamsplitter"),
        CH4_H_DICHROIC("CH4 H Dichroic"),
        H_K_DICHROIC("H/K Dichroic"),
        MIRROR("Mirror"),
        BLOCK("Block"),
        OPEN("Open");

        public static final DichroicWheel DEFAULT = H5050_BEAMSPLITTER;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "dichroicWheel");

        private String _displayValue;

        DichroicWheel(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return name();
        }
    }

    /**
     * Red Filter wheel See
     * <a href="http://www.gemini.edu/sciops/instruments/nici/imaging/filters">
     * NICI Filters
     * </a>
     */
    public enum Channel1FW implements DisplayableSpType, SequenceableSpType, ObsoletableSpType {

        BLOCK("Block", Double.NaN),
        OPEN("Open", Double.NaN) {
            @Override public boolean isObsolete() { return true; }
        },
        KS("Ks", 2.15),
        K("K", 2.20),
        K_PRIMMA("Kprime", 2.12),
        L_PRIMMA("Lprime", 3.78),
        M_PRIMMA("Mprime", 4.68),
        K_CONT("Kcont", 2.2718),
        BR_GAMMA("Br-gamma", 2.1686) {
            //moved to blue filter wheel (SCT-231)
            @Override public boolean isObsolete() { return true; }
        },
        CH4H1S("CH4 H 1% S", 1.587),
        CH4H1SP("CH4 H 1% Sp", 1.603),
        CH4H1L("CH4 H 1% L", 1.628),
        CH4H4S("CH4 H 4% S", 1.578),
        CH4H4L("CH4 H 4% L", 1.652),
        CH4H65L("CH4 H 6.5% L", 1.701),
        K_CH4("CH4 K 5% L",2.187),
        H20("H20 Ice L",3.1),
        ;

        public static final Channel1FW DEFAULT = BLOCK;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "channel1Fw");

        private final String _displayValue;
        private final double _centralWavelength;

        Channel1FW(String name, double wavelength) {
            _displayValue      = name;
            _centralWavelength = wavelength;
        }

        public String sequenceValue() {
            return name();
        }

        public String displayValue() {
            return _displayValue;
        }

        public double centralWavelength() {
            return _centralWavelength;
        }
    }

    /**
     * Blue filter wheel.  See
     * <a href="http://www.gemini.edu/sciops/instruments/nici/imaging/filters">
     * NICI Filters
     * </a>
     */
    public enum Channel2FW implements DisplayableSpType, SequenceableSpType, ObsoletableSpType {

        BLOCK("Block", Double.NaN),
        OPEN("Open", Double.NaN) {
            @Override public boolean isObsolete() { return true; }
        },
        J("J", 1.25),
        H("H", 1.65),
        FE_II("[Fe II]", 1.644),
        H210S1("H2 1-0 S1", 2.1239),
        BR_GAMMA("Br-gamma", 2.1686),
        CH4H1S("CH4 H 1% S", 1.587),
        CH4H1SP("CH4 H 1% Sp", 1.603),
        CH4H1L("CH4 H 1% L", 1.628),
        CH4H4S("CH4 H 4% S", 1.578),
        CH4H4L("CH4 H 4% L", 1.652),
        CH4H65S("CH4 H 6.5% S", 1.596),
        K_CH4("CH4 K 5% S",2.028),
        H20("H20 Ice",3.1) {
            @Override public boolean isObsolete() { return true; }
        },
        ;

        public static final Channel2FW DEFAULT = BLOCK;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "channel2Fw");

        private String _displayValue;
        private final double _centralWavelength;

        Channel2FW(String name, double wavelength) {
            _displayValue      = name;
            _centralWavelength = wavelength;
        }


        public String sequenceValue() {
            return name();
        }

        public String displayValue() {
            return _displayValue;
        }

        public double centralWavelength() {
            return _centralWavelength;
        }
    }


    public enum WellDepth implements DisplayableSpType, SequenceableSpType {
        SHALLOW("Shallow (200 mV)"),
        NORMAL("Normal (300 mV)"),
        DEEP("Deep (400 mV)"),
        ;

        public static final WellDepth DEFAULT = NORMAL;

        private String _displayValue;

        WellDepth(String text) {
            _displayValue = text;
        }

        public String sequenceValue() {
            return name();
        }

        public String displayValue() {
            return _displayValue;
        }
    }


    // Obsolete -- replaced by WellDepth
    public enum BiasVoltage implements DisplayableSpType, SequenceableSpType, ObsoletableSpType {

        LOW("Low 200 mV", WellDepth.SHALLOW),
        MEDIUM("Medium 400 mV", WellDepth.NORMAL),
        HIGH("High 600 mV", WellDepth.DEEP);

        private String _displayValue;
        private WellDepth _wellDepth;

        BiasVoltage(String text, WellDepth wellDepth) {
            _displayValue = text;
            _wellDepth = wellDepth;
        }

        public String sequenceValue() {
            return name();
        }

        public String displayValue() {
            return _displayValue;
        }

        public WellDepth wellDepth() {
            return _wellDepth;
        }

        @Override public boolean isObsolete() {
            return true;
        }
    }

    public enum DHSMode implements DisplayableSpType, SequenceableSpType {

        SAVE("Save"),
        DISCARD("Discard");

        public static final DHSMode DEFAULT = SAVE;

        private String _displayName;

        DHSMode(String name) {
            _displayName = name;
        }



        public String displayValue() {
            return _displayName;
        }

        public String sequenceValue() {
            return name();
        }
    }

    /**
     * Following are the types requested for engineering use
     */

    public enum Focs implements DisplayableSpType, SequenceableSpType {

        IN_OFF("In/Off"),
        IN_ON("In/On"),
        OUT("Out"),
        GRID("Grid"),
        CORNER("Corner");

        public static final Focs DEFAULT = OUT;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "focs");

        private String _displayValue;

        Focs(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return name();
        }
    }

    public enum NDFW implements DisplayableSpType, SequenceableSpType, ObsoletableSpType {

        AUTO("Auto"),
        BLOCK("Block"),
        ND5("ND5"),
        ND4("ND4") {
            @Override public boolean isObsolete() { return true; }
        },
        ND3("ND3"),
        ND2("ND2"),
        RED("Red"),
        OPEN("Open");

        public static final NDFW DEFAULT = NDFW.AUTO;

        private String _displayValue;

        NDFW(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return name();
        }
    }

    public enum PupilImager implements DisplayableSpType, SequenceableSpType {

        OPEN("Open"),
        PUPIL_IMAGING("Pupil Imaging");

        public static final PupilImager DEFAULT = PupilImager.OPEN;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "pupilImager");

        private String _displayValue;

        PupilImager(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return name();
        }
    }

    public enum SpiderMask implements DisplayableSpType, SequenceableSpType {

        UPDATE("Update between Int."),
        FOLLOW("Cont. Follow"),
        FIXED("Fixed");

        public static final SpiderMask DEFAULT = SpiderMask.UPDATE;

        private String _displayValue;

        SpiderMask(String name) {
            _displayValue = name;
        }


        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return name();
        }
    }

    // This class defines metaconfigurations for the Imaging Mode
    public static class ImagingModeMetaconfig {

        public DichroicWheel getDw() {
            return dw;
        }

        public Channel1FW getChannel1Fw() {
            return channel1Fw;
        }

        public Channel2FW getChannel2Fw() {
            return channel2Fw;
        }

        public PupilImager getPupilImager() {
            return pupilImager;
        }

        private ImagingModeMetaconfig(DichroicWheel dw, Channel1FW ch1, Channel2FW ch2, PupilImager pi) {
            this.dw = dw;
            channel1Fw = ch1;
            channel2Fw = ch2;
            pupilImager = pi;
        }

        private DichroicWheel dw;
        private Channel1FW channel1Fw;
        private Channel2FW channel2Fw;
        private PupilImager pupilImager;

        private static HashMap<ImagingMode, ImagingModeMetaconfig> metaConfigs = new HashMap<>();

        static {
            metaConfigs.put(ImagingMode.H1SLA,
                    new ImagingModeMetaconfig(DichroicWheel.H5050_BEAMSPLITTER,
                            Channel1FW.CH4H1S,
                            Channel2FW.CH4H1L,
                            PupilImager.OPEN));

            metaConfigs.put(ImagingMode.H1SLB,
                    new ImagingModeMetaconfig(DichroicWheel.H5050_BEAMSPLITTER,
                            Channel1FW.CH4H1L,
                            Channel2FW.CH4H1S,
                            PupilImager.OPEN));

            metaConfigs.put(ImagingMode.H1SPLA,
                    new ImagingModeMetaconfig(DichroicWheel.H5050_BEAMSPLITTER,
                            Channel1FW.CH4H1SP,
                            Channel2FW.CH4H1L,
                            PupilImager.OPEN));

            metaConfigs.put(ImagingMode.H1SPLB,
                    new ImagingModeMetaconfig(DichroicWheel.H5050_BEAMSPLITTER,
                            Channel1FW.CH4H1L,
                            Channel2FW.CH4H1SP,
                            PupilImager.OPEN));

            metaConfigs.put(ImagingMode.H4SLA,
                    new ImagingModeMetaconfig(DichroicWheel.H5050_BEAMSPLITTER,
                            Channel1FW.CH4H4S,
                            Channel2FW.CH4H4L,
                            PupilImager.OPEN));

            metaConfigs.put(ImagingMode.H4SLB,
                    new ImagingModeMetaconfig(DichroicWheel.H5050_BEAMSPLITTER,
                            Channel1FW.CH4H4L,
                            Channel2FW.CH4H4S,
                            PupilImager.OPEN));


            metaConfigs.put(ImagingMode.PUPIL_IMAGING,
                    new ImagingModeMetaconfig(DichroicWheel.H5050_BEAMSPLITTER,
                            Channel1FW.CH4H1SP,
                            Channel2FW.CH4H1SP,
                            PupilImager.PUPIL_IMAGING));
        }

        public static ImagingModeMetaconfig getMetaConfig(ImagingMode mode) {
            return metaConfigs.get(mode);
        }
    }
}
