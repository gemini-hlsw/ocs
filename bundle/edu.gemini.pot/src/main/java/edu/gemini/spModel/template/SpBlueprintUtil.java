package edu.gemini.spModel.template;

import edu.gemini.spModel.type.DisplayableSpType;

import java.util.Collection;
import java.util.Iterator;

public final class SpBlueprintUtil {
    private SpBlueprintUtil() {}

    /**
     * Formats a collection of SpDisplayableSpType into a comma separated
     * String list of values.
     */
    public static <T extends DisplayableSpType> String mkString(Collection<T> displayables) {
        return mkString(displayables, "", ", ", "");
    }

    public static <T extends DisplayableSpType> String mkString(Collection<T> displayables, String sep) {
        return mkString(displayables, "", sep, "");
    }

    public static <T extends DisplayableSpType> String mkString(Collection<T> displayables, String prefix, String sep, String suffix) {
        StringBuilder buf = new StringBuilder(prefix);
        Iterator<T> it = displayables.iterator();
        if (it.hasNext()) buf.append(it.next().displayValue());
        while (it.hasNext()) buf.append(sep).append(it.next().displayValue());
        buf.append(suffix);
        return buf.toString();
    }
}
