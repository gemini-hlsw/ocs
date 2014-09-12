/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: WCSKeywordProvider.java 4414 2004-02-03 16:21:36Z brighton $
 */

package jsky.coords;


/**
 * A simple interface for accessing FITS/WCS keywords.
 *
 * @version $Revision: 4414 $
 * @author Allan Brighton
 */
public abstract interface WCSKeywordProvider {

    /** Return true if the given keyword was found */
    public boolean findKey(String key);


    /** Return the value of the given keyword as a String, or null if not found. */
    public String getStringValue(String key);

    /** Return the value of the given keyword as a String, or null if not found. */
    public String getStringValue(String key, String defaultValue);


    /** Return the value of the given keyword as a double, or 0.0 if not found. */
    public double getDoubleValue(String key);

    /** Return the value of the given keyword as a double, or 0.0 if not found. */
    public double getDoubleValue(String key, double defaultValue);


    /** Return the value of the given keyword as a double, or 0.0 if not found. */
    public float getFloatValue(String key);

    /** Return the value of the given keyword as a double, or 0.0 if not found. */
    public float getFloatValue(String key, float defaultValue);


    /** Return the value of the given keyword as an int, or 0 if not found. */
    public int getIntValue(String key);

    /** Return the value of the given keyword as an int, or 0 if not found. */
    public int getIntValue(String key, int defaultValue);
}
