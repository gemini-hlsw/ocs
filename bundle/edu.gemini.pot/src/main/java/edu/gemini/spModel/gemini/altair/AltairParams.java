// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
//
package edu.gemini.spModel.gemini.altair;

import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.SequenceableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

/**
 * This class provides data types for the Altair components.
 */
public final class AltairParams {

    // Make the constructor private.
    private AltairParams() {
    }

    /**
     * Wavelength
     */
    public enum Wavelength implements DisplayableSpType, SequenceableSpType {

        BS_850_2500("850 nm - 2.5 μm science",         "BS_850_2500"),
        BS_850_5000("850 nm - 5 μm science",           "BS_850_5000"),
        BS_589("589 nm notch (400 nm - 1 μm science)", "BS_589");


        /* The default Wavelength index value */
        public static final Wavelength DEFAULT = BS_850_2500;
        private final String _displayValue;
        private final String _sequenceValue;

        // Constructor
        Wavelength(String displayValue, String sequenceValue) {
            _displayValue = displayValue;
            _sequenceValue = sequenceValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _sequenceValue;
        }

        /**
         * Return a Wavelength by name giving a value to return upon error *
         */
        static public Wavelength getWavelength(String name, Wavelength value) {
            return SpTypeUtil.oldValueOf(Wavelength.class, name, value);
        }
    }

    public enum ADC implements DisplayableSpType, SequenceableSpType {

        ON("ON"),
        OFF("OFF"),;

        /**
         * The default ADC value *
         */
        public static final ADC DEFAULT = OFF;
        private final String _displayValue;

        ADC(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /**
         * Return an ADC by name *
         */
        static public ADC getAdc(String name) {
            return getAdc(name, DEFAULT);
        }

        /**
         * Return an ADC by name giving a value to return upon error *
         */
        static public ADC getAdc(String name, ADC value) {
            return SpTypeUtil.oldValueOf(ADC.class, name, value);
        }
    }

    public enum CassRotator implements DisplayableSpType, SequenceableSpType {

        FOLLOWING("Following"),
        FIXED("Fixed"),
        ;

        /**
         * The default CassRotator value *
         */
        public static final CassRotator DEFAULT = FOLLOWING;
        private final String _displayValue;

        CassRotator(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /**
         * Return an CassRotator by name *
         */
        static public CassRotator getCassRotator(String name) {
            return getCassRotator(name, DEFAULT);
        }

        /**
         * Return an CassRotator by name giving a value to return upon error *
         */
        static public CassRotator getCassRotator(String name, CassRotator value) {
            return SpTypeUtil.oldValueOf(CassRotator.class, name, value);
        }
    }


    public enum FieldLens implements DisplayableSpType, SequenceableSpType {

        IN("IN"),
        OUT("OUT"),
        ;

        /**
         * The default FieldLens value *
         */
        public static final FieldLens DEFAULT = OUT;
        private final String _displayValue;

        FieldLens(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /**
         * Return an FieldLens by name giving a value to return upon error *
         */
        static public FieldLens getFieldLens(String name, FieldLens value) {
            return SpTypeUtil.oldValueOf(FieldLens.class, name, value);
        }
    }

    public enum NdFilter implements DisplayableSpType, SequenceableSpType {

        IN("IN"),
        OUT("OUT"),
        ;

        /**
         * The default NdFilter value *
         */
        public static final NdFilter DEFAULT = OUT;
        private final String _displayValue;

        NdFilter(String displayValue) {
            _displayValue = displayValue;
        }


        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /**
         * Return an NdFilter by name giving a value to return upon error *
         */
        static public NdFilter getNdFilter(String name, NdFilter value) {
            return SpTypeUtil.oldValueOf(NdFilter.class, name, value);
        }
    }

    /**
     * Choices for Laser Guide Star or Natural Guide Star  - OT-544
     */
    public enum GuideStarType implements DisplayableSpType, SequenceableSpType {

        NGS("NGS"),
        LGS("LGS"),
        ;

        /**
         * The default GuideStarType value *
         */
        public static final GuideStarType DEFAULT = NGS;
        private final String _displayValue;

        GuideStarType(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /**
         * Return a GuideStarType by name giving a value to return upon error *
         */
        static public GuideStarType getGuideStarType(String name, GuideStarType value) {
            return SpTypeUtil.oldValueOf(GuideStarType.class, name, value);
        }
    }

    /**
     * Different Altair modes - UX-1423
     * The mode knows which guide star type, field lens position and guider belong to it.
     * Only the mode is made persistent.
     */
    public enum Mode implements DisplayableSpType, SequenceableSpType {

        NGS     ("NGS",     GuideStarType.NGS, FieldLens.OUT, DefaultImList.create(AltairAowfsGuider.instance)),
        NGS_FL  ("NGS+FL",  GuideStarType.NGS, FieldLens.IN,  DefaultImList.create(AltairAowfsGuider.instance)),
        LGS     ("LGS",     GuideStarType.LGS, FieldLens.IN,  DefaultImList.create(AltairAowfsGuider.instance, GmosOiwfsGuideProbe.instance)),
        LGS_P1  ("LGS+P1",  GuideStarType.LGS, FieldLens.IN,  DefaultImList.create(PwfsGuideProbe.pwfs1, GmosOiwfsGuideProbe.instance)),
        LGS_OI  ("LGS+OI",  GuideStarType.LGS, FieldLens.IN,  DefaultImList.create(GmosOiwfsGuideProbe.instance))
        ;

        public static final Mode DEFAULT = NGS;
        private final String _displayValue;
        private final GuideStarType _guideStarType;
        private final FieldLens _fieldLens;
        private final ImList<GuideProbe> _guiders;

        Mode(final String displayValue, final GuideStarType guideStarType, final FieldLens fieldLens, final ImList<GuideProbe> guiders) {
            _displayValue = displayValue;
            _guideStarType = guideStarType;
            _fieldLens = fieldLens;
            _guiders = guiders;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        public FieldLens fieldLens() {
            return _fieldLens;
        }

        public GuideStarType guideStarType() {
            return _guideStarType;
        }

        public ImList<GuideProbe> guiders() {
            return _guiders;
        }

        /**
         * Return a GuideStarType by name giving a value to return upon error *
         */
        static public Mode getMode(String name, Mode value) {
            return SpTypeUtil.oldValueOf(Mode.class, name, value);
        }
    }

}