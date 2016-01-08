package jsky.catalog;

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
public interface QueryArgs {

    /** Return the catalog associated with this object */
    Catalog getCatalog();

    /** Set the value for the ith parameter */
    void setParamValue(int i, Object value);

    /** Set the value for the parameter with the given label */
    void setParamValue(String label, Object value);

    /** Set the double value for the parameter with the given label */
    void setParamValue(String label, double value);

    /** Set the array of parameter values directly. */
    void setParamValues(Object[] values);

    /** Get the value of the ith parameter */
    Object getParamValue(int i);

    /** Get the value of the named parameter
     *
     * @param label the parameter name or id
     * @return the value of the parameter, or null if not specified
     */
    Object getParamValue(String label);

    /**
     * Get the value of the named parameter as a String.
     *
     * @param label the parameter label
     * @param defaultValue the default value, if the parameter was not specified
     * @return the value of the parameter
     */
    String getParamValueAsString(String label, String defaultValue);


    /**
     * Return the object id being searched for, or null if none was defined.
     */
    String getId();

    /**
     * Set the object id to search for.
     */
    void setId(String id);


    /**
     * Return an object describing the query region (center position and
     * radius range), or null if none was defined.
     */
    CoordinateRadius getRegion();

    /**
     * Set the query region (center position and radius range) for
     * the search.
     */
    void setRegion(CoordinateRadius region);


    /**
     * Return an array of SearchCondition objects indicating the
     * values or range of values to search for.
     */
    SearchCondition[] getConditions();

    /** Returns the max number of rows to be returned from a table query */
    int getMaxRows();

    /** Set the max number of rows to be returned from a table query */
    void setMaxRows(int maxRows);

    /**
     * Returns a copy of this object
     */
    QueryArgs copy();

    /**
     * Optional: If not null, use this object for displaying the progress of the background query
     */
    default StatusLogger getStatusLogger() { return null; }
}
