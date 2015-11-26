package jsky.catalog;

import java.io.Serializable;

/**
 * An abstract base class for SearchConditions. The derived classes determine
 * the value type.
 */
public abstract class AbstractSearchCondition implements SearchCondition, Serializable {

    /** Describes the column or parameter whose values are given here */
    private FieldDesc _fieldDesc;

    /**
     * Create a new AbstractSearchCondition for the given column or parameter description.
     */
    public AbstractSearchCondition(FieldDesc fieldDesc) {
        _fieldDesc = fieldDesc;
    }

    /** Return the column or parameter name. */
    public String getName() {
        return _fieldDesc.getName();
    }

    /** Return the column or parameter id. */
    public String getId() {
        String s = _fieldDesc.getId();
        if (s == null)
            s = _fieldDesc.getName();
        return s;
    }

    /** Returns the value or values as a String in the format "value" or "val1,val2 */
    public String getValueAsString() {
        return getValueAsString(",");
    }

    /**
     * Return a string representation of this class in the form "name=val"
     */
    public String toString() {
        return getId() + "=" + getValueAsString(",");
    }

    /**
     * Return a string representation of this class in the form "name=val"
     */
    public String toString(String sep) {
        return getId() + "=" + getValueAsString(sep);
    }
}

