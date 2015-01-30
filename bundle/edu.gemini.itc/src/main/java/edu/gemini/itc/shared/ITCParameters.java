// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

// $Id: ITCParameters.java,v 1.2 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.shared;

import edu.gemini.spModel.type.SpTypeUtil;

import javax.servlet.http.HttpServletRequest;

/**
 * This class supplies static methods used by the parameter parsing classes.
 */
public abstract class ITCParameters {

    public static void notFoundException(String s) throws Exception {
        throw new Exception("Can't find " + s);
    }

    /** Tries to get the given parameter form an http request. */
    public static String getParameter(final HttpServletRequest r, final String parameter) {
        final String value = r.getParameter(parameter);
        if (value == null) {
            throw new NoSuchParameterException(parameter);
        }
        return value;
    }

    /** Tries to get the given parameter form an http request and translates it into an enum. */
    public static <T extends Enum<T>> T getParameter(final Class<T> c, final HttpServletRequest r) {
        final String value = r.getParameter(c.getSimpleName());
        return getParameter(c, value);
    }

    /** Tries to get the given parameter form an http request and translates it into an enum. */
    public static <T extends Enum<T>> T getParameter(final Class<T> c, final ITCMultiPartParser r) {
        final String value = r.getParameter(c.getSimpleName());
        return getParameter(c, value);
    }

    private static <T extends Enum<T>> T getParameter(final Class<T> c, final String value) {
        final T e = SpTypeUtil.oldValueOf(c, value, null);
        if (e == null) {
            throw new IllegalArgumentException("option " + value + " is invalid for " + c.getSimpleName());
        }
        return e;
    }

    /**
     * Try to parse given string, throw exception on failure
     * Exception will mention the description
     */
    public static int parseInt(String string,
                               String description) throws Exception {
        if (string == null) {
            throw new Exception(description + " - null input");
        }
        string = string.trim();
        if (string.equals("")) {
            throw new Exception(description + " - empty input");
        }
        int i;
        try {
            i = parseInt(string);
        } catch (Exception e) {
            throw new Exception(description +
                    " - can't parse into integer: " + string);
        }
        return i;
    }

    /**
     * Try to parse given string, throw informative exception on failure
     */
    public static int parseInt(String string) throws Exception {
        if (string == null) {
            throw new Exception("null input");
        }
        string = string.trim();
        if (string.equals("")) {
            throw new Exception("empty input");
        }
        int i;
        try {
            i = Integer.parseInt(string);
        } catch (NumberFormatException e) {
            throw new Exception("Can't parse into integer: " + string);
        }
        return i;
    }

    /**
     * Try to parse given string, throw exception on failure
     * Exception will mention the description
     */
    public static double parseDouble(String string,
                                     String description) throws Exception {
        if (string == null) {
            throw new Exception(description + " - null input");
        }
        string = string.trim();
        if (string.equals("")) {
            throw new Exception(description + " - empty input");
        }
        double d;
        try {
            d = parseDouble(string);
        } catch (Exception e) {
            throw new Exception(description +
                    " - can't parse into floating point number: " + string);
        }
        return d;
    }

    /**
     * Try to parse given string, throw informative exception on failure
     */
    public static double parseDouble(String string) throws Exception {
        if (string == null) {
            throw new Exception("null input");
        }
        string = string.trim();
        if (string.equals("")) {
            throw new Exception("empty input");
        }
        double d;
        try {
            d = Double.parseDouble(string);
        } catch (NumberFormatException e) {
            throw new Exception("Can't parse into floating point number: " +
                    string);
        }
        return d;
    }
}
