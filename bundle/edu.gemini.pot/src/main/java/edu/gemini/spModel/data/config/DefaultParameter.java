// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: DefaultParameter.java 7056 2006-05-25 14:26:35Z anunez $
//

package edu.gemini.spModel.data.config;

import java.util.ArrayList;
import java.util.LinkedList;
import java.beans.PropertyDescriptor;


/**
 * A straight-forward implementation of the <code>IParameter</code>
 * interface.
 *
 * <p><b>Note that this implementation is not synchronized.</b>
 */
public class DefaultParameter implements IParameter {
    private String _name;
    private Object _value;

    /**
     * The default value.
     */
    public static final Object DEFAULT_VALUE = null;

    /**
     * The Factory method for creating a DefaultParameter from a name only.
     */
    static public DefaultParameter getInstance(String name) {
        if (name == null) {
            throw new IllegalArgumentException("parameter 'name' may not be null");
        }

        return new DefaultParameter(name, DEFAULT_VALUE);
    }

    /**
     * The Factory method for creating a DefaultParameter from a name and value.
     */
    static public DefaultParameter getInstance(String name, Object value) {
        return new DefaultParameter(name, value);
    }

    /**
     * The Factory method for creating a DefaultParameter from a PropertyDescriptor and value.
     */
    static public DefaultParameter getInstance(PropertyDescriptor pd, Object value) {
        return new DefaultParameter(pd.getName(), value);
    }

    private DefaultParameter(String name, Object value) {
        _name = name;
        _value = value;
    }

    /**
     * Performs a clone of the parameter.
     * The parameter value is also cloned if it is of type ArrayList.
     */
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    public Object clone() {
        DefaultParameter res;
        try {
            res = (DefaultParameter) super.clone();
        } catch (CloneNotSupportedException ex) {
            // can't happen since this object implements Cloneable
            throw new RuntimeException();
        }

        // allan: This is needed for undo/redo/apply, to keep the data object being
        // edited separate from the original. It would make more sense to call
        // Object.clone(), but that is protected...
        if (_value instanceof ArrayList) {
            res._value = ((ArrayList) _value).clone();
        } else if (_value instanceof LinkedList) {
            res._value = ((LinkedList) _value).clone();
        }
        return res;
    }

    /**
     * Overrides to provide semantic equality.
     */
    public boolean equals(Object obj) {
        // Is this the same object?
        if (this == obj) return true;

        // Is the object null?
        if (obj == null) return false;

        // Does the given object have the same class as this?
        if (obj.getClass() != getClass()) return false;

        DefaultParameter other = (DefaultParameter) obj;

        // Check the names.
        if (!_name.equals(other._name)) return false;

        // Check the values (either may be null).
        if (_value == null) {
            if (other._value != null) return false;
        } else {
            //if (other.getValue() == null) return false;
            if (!_value.equals(other._value)) return false;
        }

        return true;
    }

    /**
     * Overrides to agree with the redefinition of <code>equals</code>.
     */
    public int hashCode() {
        int result = _name.hashCode();
        return result * 37 + ((_value == null) ? 0 : _value.hashCode());
    }

    public String getName() {
        return _name;
    }

    public Object getValue() {
        return _value;
    }

    public void setValue(Object newValue) {
        _value = newValue;
    }

    public String toString() {
        return getClass().getName() + " [name=" + _name + ", value=" + _value + "]";
    }

    public String getAsString() {
        return _name + " = " + _value;
    }


}
