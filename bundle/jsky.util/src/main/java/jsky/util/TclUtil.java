/*
 * ESO Archive
 *
 * $Id: TclUtil.java 4414 2004-02-03 16:21:36Z brighton $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/06/04  Created
 */

package jsky.util;

import java.util.ArrayList;
import java.util.List;


/**
 * Utility class for dealing with Tcl style lists and evaluating Tcl style expressions.
 * <p>
 * Note that this class was previously based on Jacl/TclJava, but was rewritten to
 * improve performance. This version is more simple minded than the Tcl version.
 */
@Deprecated // RCN: seriously? we're still using this?
public class TclUtil {

    /**
     * Split a Tcl style list into an array of strings and
     * return the result.
     *
     * @param tclList a String in Tcl list format.
     * @return an array of Strings, one element for each Tcl list item
     */
    public static String[] splitList(String tclList) {
        if (tclList == null || tclList.length() == 0) return null;

        tclList = tclList.trim();
        final List<String> v = new ArrayList<String>();
        char[] ar = tclList.toCharArray();
        int len = ar.length;
        int depth = 0;
        int i = 0;
        int start = 0;
        boolean ignoreWhitespace = false;
        boolean inQuote = false;

        while (i < len) {
            char c = ar[i];
            if (c == '"')
                inQuote = !inQuote;
            if (c == '{' || (c == '"' && inQuote)) {
                if (depth++ == 0) {
                    ignoreWhitespace = true;
                    start = i + 1;
                }
            } else if (c == '}' || (c == '"' && !inQuote)) {
                if (--depth == 0) {
                    ignoreWhitespace = true;
                    if (start == i) {
                        v.add("");
                    } else {
                        v.add(new String(ar, start, i - start).trim());
                    }
                }
            } else if (depth == 0) {
                if (Character.isWhitespace(c)) {
                    if (!ignoreWhitespace) {
                        ignoreWhitespace = true;
                        if (start == i) {
                            v.add("");
                        } else {
                            v.add(new String(ar, start, i - start).trim());
                        }
                    }
                } else {
                    if (ignoreWhitespace)
                        start = i;
                    ignoreWhitespace = false;
                }
            }
            i++;
        }

        // check last item
        if (!ignoreWhitespace) {
            if (start == i) {
                v.add("");
            } else {
                v.add(new String(ar, start, i - start).trim());
            }
        }

        return v.toArray(new String[v.size()]);
    }

    /**
     * Convert the given array of Objects to a Tcl list formatted string
     * and return the result.
     *
     * @param ar an array of Objects, one element for each Tcl list item
     * @return a String in Tcl list format.
     */
    public static String makeList(Object[] ar) {
        if (ar == null) return "";

        final StringBuilder sb = new StringBuilder();
        if (ar.length > 0) {
            sb.append('{').append(ar[0].toString()).append('}');
        }

        for (int i=1; i<ar.length; ++i) {
            sb.append(" {").append(ar[i].toString()).append('}');
        }
        return sb.toString();
    }
}
