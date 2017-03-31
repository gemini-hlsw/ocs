package edu.gemini.spModel.data;

import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.LoggableSpType;
import edu.gemini.spModel.type.SequenceableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

/**
 * An enumerated type for yes/no values.
 */
public enum YesNoType implements DisplayableSpType, LoggableSpType, SequenceableSpType {
    NO("No") {
        public boolean toBoolean() {
            return false;
        }
    },
    YES("Yes") {
        public boolean toBoolean() {
            return true;
        }
    },
    ;

    public static YesNoType DEFAULT = YES; // be positive!

    private String _displayValue;

    YesNoType(String name) {
        _displayValue = name;
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

    public abstract boolean toBoolean();

    public static YesNoType fromBoolean(boolean value) {
        return value ? YES : NO;
    }
    public static YesNoType getYesNoTypeByIndex(int index) {
        return SpTypeUtil.valueOf(YesNoType.class, index, DEFAULT);
    }

    public static YesNoType getYesNoType(String name) {
        return getYesNoType(name, DEFAULT);
    }

    public static YesNoType getYesNoType(String name, YesNoType nvalue) {
        return SpTypeUtil.oldValueOf(YesNoType.class, name, nvalue);
    }
}
