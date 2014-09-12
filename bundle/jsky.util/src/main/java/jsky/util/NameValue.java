// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: NameValue.java 7202 2006-06-23 15:19:56Z anunez $

package jsky.util;

import java.io.Serializable;

/**
 * A simple class containing a name and an associated value.
 *
 * @version $Revision: 7202 $
 * @author Allan Brighton
 */
public final class NameValue implements Serializable {

    private String _name;
    private Object _value;

    public NameValue() {
    }

    public NameValue(String name, Object value) {
        _name = name;
        _value = value;
    }

    public void setName(String name) {
        _name = name;
    }

    public String getName() {
        return _name;
    }

    public void setValue(Object value) {
        _value = value;
    }

    public Object getValue() {
        return _value;
    }

    public String toString() {
        return _name;
    }

    public boolean equals(Object object) {

        if (!(object instanceof NameValue)) return false;
        NameValue other = (NameValue)object;
        if (_name == null) {
            if (other._name != null) return false;
        } else {
            if (!(_name.equals(other._name))) return false;
        }

        if (_value == null) {
            if (other._value != null) return false;
        } else {
            if (!(_value.equals(other._value))) return false;
        }

        return true;
    }

    public int hashCode() {
        int hash = (_value !=  null) ? _value.hashCode(): 1;
        hash = 37*hash + ((_name != null) ? _name.hashCode() : 1);
        return hash;
    }
}

