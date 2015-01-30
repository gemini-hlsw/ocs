package edu.gemini.itc.shared;

import edu.gemini.spModel.type.SpTypeUtil;

import javax.servlet.http.HttpServletRequest;

/**
 * This class supplies static methods used by the parameter parsing classes.
 */
public abstract class ITCParameters {

    public static void notFoundException(String s) {
        throw new IllegalArgumentException("Can't find " + s);
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
                               String description) {
        if (string == null) {
            throw new IllegalArgumentException(description + " - null input");
        }
        string = string.trim();
        if (string.equals("")) {
            throw new IllegalArgumentException(description + " - empty input");
        }
        int i;
        try {
            i = parseInt(string);
        } catch (Exception e) {
            throw new IllegalArgumentException(description +
                    " - can't parse into integer: " + string);
        }
        return i;
    }

    /**
     * Try to parse given string, throw informative exception on failure
     */
    public static int parseInt(String string) {
        if (string == null) {
            throw new IllegalArgumentException("null input");
        }
        string = string.trim();
        if (string.equals("")) {
            throw new IllegalArgumentException("empty input");
        }
        int i;
        try {
            i = Integer.parseInt(string);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Can't parse into integer: " + string);
        }
        return i;
    }

    /**
     * Try to parse given string, throw exception on failure
     * Exception will mention the description
     */
    public static double parseDouble(String string,
                                     String description) {
        if (string == null) {
            throw new IllegalArgumentException(description + " - null input");
        }
        string = string.trim();
        if (string.equals("")) {
            throw new IllegalArgumentException(description + " - empty input");
        }
        double d;
        try {
            d = parseDouble(string);
        } catch (Exception e) {
            throw new IllegalArgumentException(description +
                    " - can't parse into floating point number: " + string);
        }
        return d;
    }

    /**
     * Try to parse given string, throw informative exception on failure
     */
    public static double parseDouble(String string) {
        if (string == null) {
            throw new IllegalArgumentException("null input");
        }
        string = string.trim();
        if (string.equals("")) {
            throw new IllegalArgumentException("empty input");
        }
        double d;
        try {
            d = Double.parseDouble(string);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Can't parse into floating point number: " +
                    string);
        }
        return d;
    }
}
