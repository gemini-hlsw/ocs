package edu.gemini.spModel.gemini.bhros;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.SequenceableSpType;
import edu.gemini.spModel.type.SpTypeUtil;
import edu.gemini.spModel.type.DescribableSpType;
//$Id: BHROSParams.java 7081 2006-05-26 21:48:14Z anunez $

public final class BHROSParams {

    public static enum PostSlitFilter implements DisplayableSpType, SequenceableSpType  {

         NONE("None"),
         BG40("BG40"),
         CUSO4("CuS04"),
         GG495("GG495"),
         OG530("OG530");

        public static final PostSlitFilter DEFAULT = NONE;

        public final String _displayValue;

        private PostSlitFilter(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        static public PostSlitFilter getPostSlitFilter(String name, PostSlitFilter nvalue) {
            return SpTypeUtil.oldValueOf(PostSlitFilter.class, name, nvalue);
        }

    }

    public static enum HartmannFlap implements DisplayableSpType, SequenceableSpType  {

         NONE("None"),
         UP("Up"),
         DOWN("Down"),
         LEFT("Left"),
         RIGHT("Right");

        public static final HartmannFlap DEFAULT = NONE;

        private final String _displayValue;

        private HartmannFlap(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        static public HartmannFlap getHartmannFlap(String name, HartmannFlap nvalue) {
            return SpTypeUtil.oldValueOf(HartmannFlap.class, name, nvalue);
        }
    }

    public static enum EntranceFibre implements DisplayableSpType, SequenceableSpType, DescribableSpType  {

        OBJECT_ONLY("Object Only", "(0.9 arcsec)", 11.8, -15.98, -24.83, new double[]{0.9, 0.9}),
        OBJECT_SKY("Object-Sky", "(dual 0.7 arcsec)", 13.9, 8.95, -24.75, new double[]{0.7, 0.7});

        public static final EntranceFibre DEFAULT = OBJECT_SKY;

        private final double[] scienceArea;
        private final double goniometerOffset;
        private final double patrolFieldXOffset;
        private final double patrolFieldYOffset;
        private final String _displayValue;
        private final String _sequenceValue;
        private final String _description;

        private EntranceFibre(String name, String desc, double goniometerOffset, double patrolFieldXOffset, double patrolFieldYOffset, double[] scienceArea) {
            _displayValue = name;
            _description = desc;
            _sequenceValue = name;
            this.scienceArea = scienceArea;
            this.goniometerOffset = goniometerOffset;
            this.patrolFieldXOffset = patrolFieldXOffset;
            this.patrolFieldYOffset = patrolFieldYOffset;
        }

        public double[] getScienceArea() {
            return scienceArea; // not immutable, but let's just assume clients will behave.
        }

        static public EntranceFibre getEntranceFibre(String name, EntranceFibre nvalue) {
            return SpTypeUtil.oldValueOf(EntranceFibre.class, name, nvalue);
        }

        public double getPatrolFieldXOffset() {
            return patrolFieldXOffset;
        }

        public double getPatrolFieldYOffset() {
            return patrolFieldYOffset;
        }

        public double getGoniometerOffset() {
            return goniometerOffset;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _sequenceValue;
        }

        public String description() {
            return _description;
        }
    }

    public static enum CCDXBinning implements DisplayableSpType, SequenceableSpType {

        VAL_1(1),
        VAL_2(2),
        VAL_4(4),
        VAL_8(8);

        public static final CCDXBinning DEFAULT = VAL_1;

        private final int intValue;
        private final String _displayValue;

        public int intValue() {
            return intValue;
        }

        private CCDXBinning(int value) {
            _displayValue = Integer.toString(value);
            intValue = value;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        static public CCDXBinning getXBinning(String name, CCDXBinning nvalue) {
            return SpTypeUtil.oldValueOf(CCDXBinning.class, name, nvalue);
        }
    }


    public static enum CCDYBinning implements DisplayableSpType, SequenceableSpType  {

        VAL_1(1),
        VAL_2(2),
        VAL_4(4),
        VAL_8(8);

        public static final CCDYBinning DEFAULT = VAL_1;

        private final int intValue;
        private final String _displayValue;

        public int intValue() {
            return intValue;
        }

        private CCDYBinning(int value) {
            _displayValue = Integer.toString(value);
            intValue = value;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        static public CCDYBinning getYBinning(String name, CCDYBinning nvalue) {
            return SpTypeUtil.oldValueOf(CCDYBinning.class, name, nvalue);
        }
    }

    //These guys are known also as the CCD Amplifiers
    public static enum CCDReadoutPorts implements DisplayableSpType, SequenceableSpType  {

        ALL("All"),
        BEST("Best"),
        SECONDARY("Secondary");

        public static final CCDReadoutPorts DEFAULT = BEST;

        public final String _displayValue;

        private CCDReadoutPorts(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return displayValue();
        }

        static public CCDReadoutPorts getCCDAmplifiers(String name, CCDReadoutPorts nvalue) {
            return SpTypeUtil.oldValueOf(CCDReadoutPorts.class, name, nvalue);
        }
    }

    public static enum CCDSpeed implements DisplayableSpType, SequenceableSpType  {

        SLOW ("Slow"),
        FAST("Fast");

        public static final CCDSpeed DEFAULT = SLOW;

        private final String _displayValue;

        private CCDSpeed(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        static public CCDSpeed getCCDSpeed(String name, CCDSpeed nvalue) {
            return SpTypeUtil.oldValueOf(CCDSpeed.class, name, nvalue);
        }
    }

    public static enum CCDGain implements DisplayableSpType, SequenceableSpType  {

        // OT-427: gain should just be low/hi ... sort out the particular value in seqexec
        LOW("Low"),
        HIGH("High");

        public static final CCDGain DEFAULT = LOW;
        public final String _displayValue;

        private CCDGain(String text) {
            _displayValue = text;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        static public CCDGain getCCDGain(String name, CCDGain nvalue) {
            return SpTypeUtil.oldValueOf(CCDGain.class, name, nvalue);
        }
    }

    public static enum ROI implements DisplayableSpType, SequenceableSpType  {

        FULL_FRAME("Full Frame");

        public static final ROI DEFAULT = FULL_FRAME;

        private final String _displayValue;

        private ROI(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        static public ROI getROI(String name, ROI nvalue) {
            return SpTypeUtil.oldValueOf(ROI.class, name, nvalue);
        }
    }

    public static enum ISSPort implements DisplayableSpType, SequenceableSpType  {

        SIDE_LOOKING("Side-Looking"),
        UP_LOOKING("Up-Looking");

        public static final ISSPort DEFAULT = SIDE_LOOKING;

        private final String _displayValue;

        private ISSPort(String name) {
            _displayValue = name;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        static public ISSPort getISSPort(String name, ISSPort nvalue) {
            return SpTypeUtil.oldValueOf(ISSPort.class, name, nvalue);
        }
    }

    public static enum ExposureMeterFilter implements DisplayableSpType, SequenceableSpType  {

        U("U"),
        B("B"),
        V("V"),
        R("R"),
        I("I"),
        CLEAR("CLEAR"),
        ND055("ND0.55"),
        ND10("ND1.0"),
        ND18("ND1.8"),
        ND31("ND3.1");

        public static final ExposureMeterFilter DEFAULT = ND31;

        private final String _displayValue;

        private ExposureMeterFilter(String name) {
            _displayValue = name;
        }


        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        static public ExposureMeterFilter getExposureMeterFilter(String name, ExposureMeterFilter nvalue) {
            return SpTypeUtil.oldValueOf(ExposureMeterFilter.class, name, nvalue);
        }
    }

}
