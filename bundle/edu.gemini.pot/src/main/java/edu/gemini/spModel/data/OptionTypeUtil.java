package edu.gemini.spModel.data;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.type.DisplayableSpType;

/**
 * Utility for working with {@link Option} types in the OT.
 */
public final class OptionTypeUtil {
    private OptionTypeUtil() { }

    /**
     * Converts an {@link Option} value to a string.  {@link None} instances
     * yield the string "*Unspecified" while the <code>toString</code>
     * method of the wrapped object of
     * {@link edu.gemini.shared.util.immutable.Some} instances are returned
     * otherwise.
     */
    public static <E> String toDisplayString(Option<E> opt) {
        if (None.instance().equals(opt)) return "*Unspecified";
        E obj = opt.getValue();
        
        return (obj instanceof DisplayableSpType) ?
                ((DisplayableSpType) obj).displayValue() :
                opt.getValue().toString();
    }
}
