// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: QueryArgs.java 38711 2011-11-15 13:35:55Z swalker $

package jsky.catalog;

import java.util.Vector;

import jsky.coords.CoordinateRadius;
import jsky.util.gui.StatusLogger;


/**
 * An interface representing the values of the arguments to a catalog query.
 * The values correspond one to one with a given catalog's parameters, as
 * returned by the <code>Catalog.getParamDesc(index)</code> method.
 *
 * @see Catalog#getNumParams
 * @see Catalog#getParamDesc(String) 
 */
public abstract interface QueryArgs {

    /** Return the catalog associated with this object */
    public Catalog getCatalog();

    /** Set the value for the ith parameter */
    public void setParamValue(int i, Object value);

    /** Set the value for the parameter with the given label */
    public void setParamValue(String label, Object value);

    /** Set the min and max values for the parameter with the given label */
    public void setParamValueRange(String label, Object minValue, Object maxValue);

    /** Set the int value for the parameter with the given label */
    public void setParamValue(String label, int value);

    /** Set the double value for the parameter with the given label */
    public void setParamValue(String label, double value);

    /** Set the double value for the parameter with the given label */
    public void setParamValueRange(String label, double minValue, double maxValue);

    /** Set the array of parameter values directly. */
    public void setParamValues(Object[] values);

    /** Get the value of the ith parameter */
    public Object getParamValue(int i);

    /** Get the value of the named parameter
     *
     * @param label the parameter name or id
     * @return the value of the parameter, or null if not specified
     */
    public Object getParamValue(String label);

    /**
     * Get the value of the named parameter as an integer.
     *
     * @param label the parameter label
     * @param defaultValue the default value, if the parameter was not specified
     * @return the value of the parameter
     */
    public int getParamValueAsInt(String label, int defaultValue);

    /**
     * Get the value of the named parameter as a double.
     *
     * @param label the parameter label
     * @param defaultValue the default value, if the parameter was not specified
     * @return the value of the parameter
     */
    public double getParamValueAsDouble(String label, double defaultValue);

    /**
     * Get the value of the named parameter as a String.
     *
     * @param label the parameter label
     * @param defaultValue the default value, if the parameter was not specified
     * @return the value of the parameter
     */
    public String getParamValueAsString(String label, String defaultValue);


    /**
     * Return the object id being searched for, or null if none was defined.
     */
    public String getId();

    /**
     * Set the object id to search for.
     */
    public void setId(String id);


    /**
     * Return an object describing the query region (center position and
     * radius range), or null if none was defined.
     */
    public CoordinateRadius getRegion();

    /**
     * Set the query region (center position and radius range) for
     * the search.
     */
    public void setRegion(CoordinateRadius region);


    /**
     * Return an array of SearchCondition objects indicating the
     * values or range of values to search for.
     */
    public SearchCondition[] getConditions();

    /** Returns the max number of rows to be returned from a table query */
    public int getMaxRows();

    /** Set the max number of rows to be returned from a table query */
    public void setMaxRows(int maxRows);


    /** Returns the query type (an optional string, which may be interpreted by some catalogs) */
    public String getQueryType();

    /** Set the query type (an optional string, which may be interpreted by some catalogs) */
    public void setQueryType(String queryType);

    /**
     * Returns a copy of this object
     */
    public QueryArgs copy();

    /**
     * Optional: If not null, use this object for displaying the progress of the background query
     */
    public StatusLogger getStatusLogger();
}
