/*
 * Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: EditableParameter.java 4726 2004-05-14 16:50:12Z brighton $
 */
package edu.gemini.spModel.data.config;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * A parameter that indicates the type of editor widget to use for the value.
 * Some parameters only allow numeric values, some have a limited number of choices,
 * and some have choices, but can also be edited. This class assumes the last case:
 * That the value is not limited to any choices specified. In addition,
 * you can specify if the values should be forced to be numeric.
 */
public class EditableParameter implements IParameter {

    private String _name;
    private Object _value;
    private boolean _isNumeric;

    /**
     * The default value.
     */
    public static final Object DEFAULT_VALUE = null;

    /**
     * The Factory method for creating an EditableParameter from a name only.
     */
    static public EditableParameter getInstance(String name, boolean isNumeric) {
        if (name == null) {
            throw new IllegalArgumentException("parameter 'name' may not be null");
        }

        return new EditableParameter(name, DEFAULT_VALUE, isNumeric);
    }

    /**
     * Calls getInstance(name, true).
     */
    static public EditableParameter getInstance(String name) {
        return getInstance(name, true);
    }

    /**
     * The Factory method for creating a EditableParameter from a name and value.
     */
    static public EditableParameter getInstance(String name, Object value, boolean isNumeric) {
        return new EditableParameter(name, value, isNumeric);
    }

    /**
     * Calls getInstance(name, value, true).
     */
    static public EditableParameter getInstance(String name, Object value) {
        return getInstance(name, value, true);
    }

    private EditableParameter(String name, Object value, boolean isNumeric) {
        _name = name;
        _value = value;
        _isNumeric = isNumeric;
    }

    /** Return true if the user edited values must be numeric */
    public boolean isNumeric() {return _isNumeric;}

    /**
     * Performs a clone of the parameter.
     * The parameter value is also cloned if it is of type ArrayList.
     */
    public Object clone() {
        EditableParameter res;
        try {
            res = (EditableParameter) super.clone();
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

        EditableParameter other = (EditableParameter) obj;

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
