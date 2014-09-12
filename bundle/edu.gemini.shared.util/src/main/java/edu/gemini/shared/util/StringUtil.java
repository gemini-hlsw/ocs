//
// $Id$
//

package edu.gemini.shared.util;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 */
public final class StringUtil {
    private StringUtil() {
    }

    public static String toDisplayName(String propertyName) {
        // SW: isn't there something in the JDK that does this?
        CharacterIterator it = new StringCharacterIterator(propertyName);
        StringBuilder res = new StringBuilder();

        // Append the first character of the property name in title case
        char c = it.first();
        if (c == CharacterIterator.DONE) return "";

        boolean wasUpper = Character.isUpperCase(c);
        res.append(wasUpper ? c : Character.toUpperCase(c));

        c = it.next();
        while (c != CharacterIterator.DONE) {
            boolean isUpper = Character.isUpperCase(c);
            if (!wasUpper && isUpper) {
                res.append(' ').append(Character.toUpperCase(c));
            } else {
                res.append(c);
            }
            wasUpper = isUpper;
            c = it.next();
        }
        if (res.charAt(0) == '0') return res.substring(1);
        return res.toString();
    }

    public static interface MapToString<T> {
        String apply(T t);
    }

    public static MapToString DEFAULT_MAP_TO_STRING = new MapToString() {
        @Override public String apply(Object o) { return o.toString(); }
    };

    public static <T> String mkString(Collection<T> c) {
        return mkString(c, ",");
    }

    public static <T> String mkString(Collection<T> c,String sep) {
        return mkString(c, "[", sep, "]");
    }

    public static <T> String mkString(Collection<T> c, String prefix, String sep, String suffix) {
        return mkString(c, prefix, sep, suffix, DEFAULT_MAP_TO_STRING);
    }

    public static <T> String mkString(Collection<T> c, String prefix, String sep, String suffix, MapToString<T> m) {
        StringBuilder buf = new StringBuilder();
        buf.append(prefix);

        Iterator<T> it = c.iterator();
        if (it.hasNext()) {
            buf.append(m.apply(it.next()));
            while (it.hasNext()) {
                buf.append(sep).append(m.apply(it.next()));
            }
        }

        buf.append(suffix);
        return buf.toString();
    }
}
