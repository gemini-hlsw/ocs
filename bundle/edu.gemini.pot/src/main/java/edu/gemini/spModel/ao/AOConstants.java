package edu.gemini.spModel.ao;

import edu.gemini.pot.sp.SPComponentBroadType;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.LoggableSpType;
import edu.gemini.spModel.type.SequenceableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

public final class AOConstants {
    // The AO System Broad Type
    public static final SPComponentBroadType AO_BROAD_TYPE = SPComponentBroadType.AO;

    // The property name for specifying an AO system
    public static final String AO_SYSTEM_PROP = "aoSystem";

    // The property name for specifying the first AO natural guide star
    public static final String AO_NGS_ONE_PROP = "guideWithAOWFS1";

    /**
     * The configuration system name for an adaptive optics system.
     */
    public static final String AO_CONFIG_NAME = "adaptive optics";
    public static final String AO_GUIDE_STAR_TYPE = "guideStarType";

    public static final ItemKey AO_SYSTEM_KEY = new ItemKey(new ItemKey(AO_CONFIG_NAME), AO_SYSTEM_PROP);
    public static final ItemKey AO_GUIDE_STAR_TYPE_KEY = new ItemKey(new ItemKey(AO_CONFIG_NAME),AO_GUIDE_STAR_TYPE);


    /**
     * Types of adaptive optics
     */
    public enum AO implements DisplayableSpType, LoggableSpType, SequenceableSpType {

        NONE("None"),
        Altair_LGS("Altair + LGS"),
        Altair_NGS("Altair + NGS"),
        ;

        /** The default AO value **/
        public static AO DEFAULT = NONE;

        private String _displayValue;

        AO(String displayValue) {
            _displayValue = displayValue;
        }

        public String displayValue() {
            return _displayValue;
        }

        public String logValue() {
            return _displayValue;
        }

        public String sequenceValue() {
            return _displayValue;
        }

        /** Return a AO by name **/
        public static AO getAO(String name) {
            return getAO(name, DEFAULT);
        }

        /** Return a AO by name with a value to return upon error **/
        public static AO getAO(String name, AO nvalue) {
            return SpTypeUtil.oldValueOf(AO.class, name, nvalue);
        }
    }
}
