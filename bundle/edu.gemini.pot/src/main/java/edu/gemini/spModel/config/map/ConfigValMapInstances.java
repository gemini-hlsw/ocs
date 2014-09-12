package edu.gemini.spModel.config.map;

import edu.gemini.shared.util.StringUtil;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.SequenceableSpType;

import java.util.Collection;

/**
 * Defines reusable instances of ConfigValMap.
 */
public class ConfigValMapInstances {
    public static final ConfigValMap IDENTITY_MAP = new ConfigValMap() {
        @Override public Object apply(Object val) { return val; }
    };

    public static abstract class ConfigValMapToString implements ConfigValMap {
        @Override public Object apply(Object val) {
            Option<String> strVal = objectToString(val);
            if (!strVal.isEmpty()) return strVal.getValue();

            if (val instanceof Collection) {
                return StringUtil.mkString((Collection<Object>) val, "", ",", "", new StringUtil.MapToString<Object>() {
                    @Override
                    public String apply(Object val) {
                        return ConfigValMapToString.this.apply(val).toString();
                    }
                });
            } else if (val instanceof Option) {
                Option o = (Option) val;
                return o.isEmpty() ? "" : ConfigValMapToString.this.apply(o.getValue());
            }
            return val.toString();
        }

        protected abstract Option<String> objectToString(Object val);
    }

    public static final ConfigValMap TO_DISPLAY_VALUE = new ConfigValMapToString() {
        @Override public Option<String> objectToString(Object val) {
            return (val instanceof DisplayableSpType) ?
                    new Some<String>(((DisplayableSpType) val).displayValue()) :
                    None.STRING;
        }
    };

    public static final ConfigValMap TO_SEQUENCE_VALUE = new ConfigValMapToString() {
        @Override public Option<String> objectToString(Object val) {
            return (val instanceof SequenceableSpType) ?
                    new Some<String>(((SequenceableSpType) val).sequenceValue()) :
                    None.STRING;
        }
    };
}
