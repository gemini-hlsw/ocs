/*
 * ESO Archive
 *
 * $Id: StringUtil.java 4540 2004-02-23 20:01:10Z brighton $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/07/02  Created
 */

package jsky.util;

/**
 * Contains static String utility methods.
 */
public class StringUtil {

    /**
     * Return true if the two strings are equal (like String.equals(), but
     * allowing null values for both operands).
     */
    public static boolean equals(String s1, String s2) {
        if (s1 == null && s2 == null)
            return true;
        if (s1 == null || s2 == null)
            return false;
        return s1.equals(s2);
    }

    /**
     * Checks whether a string matches a given wildcard pattern.
     * Only does ? and * (or '%'), and multiple patterns separated by |.
     * (Taken from http://www.acme.com/java/software/Acme.Utils.html).
     */
    public static boolean match(String pattern, String string) {
        int sLen = string.length();
        int pLen = pattern.length();
        for (int p = 0; ; p++) {
            for (int s = 0; ; p++, s++) {
                boolean sEnd = (s >= sLen);
                boolean pEnd = (p >= pLen || pattern.charAt(p) == '|');

                // Make sure '*' or '%' also match the empty string
                if (sEnd && !pEnd && pattern.charAt(p) == '*' && (p == pLen - 1 || (p < pLen - 1 && pattern.charAt(p + 1) == '|')))
                    return true;

                if (sEnd && pEnd)
                    return true;

                if (sEnd || pEnd)
                    break;

                if (pattern.charAt(p) == '?')
                    continue;

                if (pattern.charAt(p) == '*' || pattern.charAt(p) == '%') {
                    p++;

                    for (int i = sLen; i >= s; --i)
                        if (match(pattern.substring(p), string.substring(i)))  /* not quite right */
                            return true;
                    break;
                }

                if (Character.toUpperCase(pattern.charAt(p)) != Character.toUpperCase(string.charAt(s)))
                    break;

            }

            p = pattern.indexOf('|', p);
            if (p == -1)
                return false;
        }
    }


    /**
     * Pad the given string to the given length with blanks on the left or right, depending
     * on the value of the leftJustify argument.
     *
     * @param s the source string
     * @param length fill the string with blanks if it is less than this length
     * @param leftJustify if true, add blanks after the s, otherwise before
     */
    public static String pad(String s, int length, boolean leftJustify) {
        StringBuilder sb = new StringBuilder(length);
        if (leftJustify) {
            sb.append(s);
            int n = s.length();
            while (n++ < length)
                sb.append(' ');
        } else {
            int n = s.length();
            while (n++ < length)
                sb.append(' ');
            sb.append(s);
        }
        return sb.toString();
    }


}


