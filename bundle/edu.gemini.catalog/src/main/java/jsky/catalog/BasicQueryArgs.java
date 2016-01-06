package jsky.catalog;

import jsky.coords.CoordinateRadius;
import jsky.util.gui.StatusLogger;

import java.util.Vector;

/**
 * Represents the values of the arguments to a catalog query.
 */
public class BasicQueryArgs implements QueryArgs {

    /** Catalog we are accessing */
    private final Catalog _catalog;

    /** Array of parameter values corresponding to the catalog parameters */
    private Object[] _values;

    /** Optional limit on the number of rows returned from a query */
    private int _maxRows;

    /** Optional object id, if searching by object id */
    private String _id;

    /** Optional query region (center position and radius range) for query */
    private CoordinateRadius _region;

    /** Optional query type, which may be used by the catalog to determine the type of query */
    private String _queryType;

    /**
     * Create a BasicQueryArgs object for the given catalog, with no parameter
     * values (or only default values) set.
     */
    public BasicQueryArgs(Catalog catalog) {
        _catalog = catalog;
        int n = _catalog.getNumParams();
        if (n != 0) {
            _values = new Object[n];
            for (int i = 0; i < n; i++) {
                FieldDesc param = _catalog.getParamDesc(i);
                if (param != null)
                    _values[i] = param.getDefaultValue();
                else
                    _values[i] = null;
            }
        }
    }

    @Override
    public QueryArgs copy() {
        BasicQueryArgs result = new BasicQueryArgs(_catalog);
        int n = _catalog.getNumParams();
        System.arraycopy(_values, 0, result._values, 0, n);
        result._maxRows = _maxRows;
        result._id = _id;
        result._region = _region;
        result._queryType = _queryType;
        return result;
    }


    /** Set the value for the ith parameter */
    @Override
    public void setParamValue(int i, Object value) {
        _values[i] = value;
    }

    /** Set the value for the parameter with the given label */
    @Override
    public void setParamValue(String label, Object value) {
        int n = _catalog.getNumParams();
        for (int i = 0; i < n; i++) {
            FieldDesc param = _catalog.getParamDesc(i);
            if (param != null) {
                String name = param.getName();
                String id = param.getId();
                if ((id != null && id.equalsIgnoreCase(label)) || (name != null && name.equalsIgnoreCase(label))) {
                    setParamValue(i, value);
                    return;
                }
            }
        }
    }

    /** Set the double value for the parameter with the given label */
    @Override
    public void setParamValue(String label, double value) {
        setParamValue(label, new Double(value));
    }

    /** Set the array of parameter values directly. */
    @Override
    public void setParamValues(Object[] values) {
        _values = values;
    }

    /** Get the value of the ith parameter */
    @Override
    public Object getParamValue(int i) {
        return _values[i];
    }

    /** Get the value of the named parameter
     *
     * @param label the parameter name or id
     * @return the value of the parameter, or null if not specified
     */
    @Override
    public Object getParamValue(String label) {
        int n = _catalog.getNumParams();
        for (int i = 0; i < n; i++) {
            FieldDesc param = _catalog.getParamDesc(i);
            if (param != null) {
                String name = param.getName();
                String id = param.getId();
                if ((id != null && id.equalsIgnoreCase(label)) || (name != null && name.equalsIgnoreCase(label)))
                    return getParamValue(i);
            }
        }
        return null;
    }

    /**
     * Get the value of the named parameter as a String.
     *
     * @param label the parameter label
     * @param defaultValue the default value, if the parameter was not specified
     * @return the value of the parameter
     */
    @Override
    public String getParamValueAsString(String label, String defaultValue) {
        Object o = getParamValue(label);
        if (o == null)
            return defaultValue;
        if (o instanceof String)
            return (String) o;
        return o.toString();
    }

    /**
     * Return the object id being searched for, or null if none was defined.
     */
    @Override
    public String getId() {
        return _id;
    }

    /**
     * Set the object id to search for.
     */
    @Override
    public void setId(String id) {
        _id = id;
    }

    /**
     * Return an object describing the query region (center position and
     * radius range), or null if none was defined.
     */
    @Override
    public CoordinateRadius getRegion() {
        return _region;
    }

    /**
     * Set the query region (center position and radius range) for
     * the search.
     */
    @Override
    public void setRegion(CoordinateRadius region) {
        _region = region;
    }


    /** Return the catalog we are accessing. */
    @Override
    public Catalog getCatalog() {
        return _catalog;
    }


    /**
     * Return an array of SearchCondition objects indicating the
     * values or range of values to search for.
     */
    @Override
    public SearchCondition[] getConditions() {
        if (_values == null)
            return null;

        int n = _catalog.getNumParams();
        Vector<SearchCondition> v = new Vector<>(n);
        for (int i = 0; i < n; i++) {
            if (_values[i] != null) {
                FieldDesc p = _catalog.getParamDesc(i);
                FieldDesc nextP = null;
                if (i+1 < n) {
                    nextP = _catalog.getParamDesc(i+1);
                }
                if (p != null) {
                    if (_values[i] instanceof ValueRange) {
                        ValueRange r = (ValueRange) _values[i];
                        v.add(new RangeSearchCondition(p, r.getMinValue(), r.isMinInclusive(),
                                                       r.getMaxValue(), r.isMaxInclusive()));
                    } else if (_values[i] instanceof Comparable) {
                        // The min/max param handling assumes that the max param follows the min param
                        if (p.isMin()) {
                            // parameter has two values: min and max: save the min value here
                            if (nextP != null && nextP.isMax() && nextP.getId().equals(p.getId())) {
                                v.add(new RangeSearchCondition(p, (Comparable)_values[i], (Comparable)_values[i+1]));
                                i++; // skip the next param, which is the max value for this min value param
                            } else {
                                // Only a min value was specified
                                v.add(new RangeSearchCondition(p, (Comparable)_values[i], null));
                            }
                        } else if (p.isMax()) {
                            // If this wasn't handled above, there must be no min value
                            v.add(new RangeSearchCondition(p, null, (Comparable)_values[i]));
                        } else {
                            v.add(new ValueSearchCondition(p, (Comparable) _values[i]));
                        }
                    } else if (_values[i] instanceof Object[]) {
                        v.add(new ArraySearchCondition(p, (Object[]) _values[i]));
                    }
                }
            }
        }

        // convert result vector to array for return
        n = v.size();
        if (n == 0)
            return null;

        SearchCondition[] sc = new SearchCondition[n];
        v.toArray(sc);
        return sc;
    }

    /** Returns the max number of rows to be returned from a table query */
    @Override
    public int getMaxRows() {
        return _maxRows;
    }

    /** Set the max number of rows to be returned from a table query */
    @Override
    public void setMaxRows(int maxRows) {
        _maxRows = maxRows;
    }

    /** Return a string of the form: arg=value&arg=value, ...*/
    @Override
    public String toString() {
        SearchCondition[] sc = getConditions();
        if (sc == null) return "";

        StringBuilder sb = new StringBuilder();
        if (sc.length > 0) sb.append(sc[0].toString());
        for (int i=1; i<sc.length; i++) {
            sb.append("&").append(sc[i].toString());
        }
        return sb.toString();
    }
}
