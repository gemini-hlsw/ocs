// Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: TypeBase.java 18053 2009-02-20 20:16:23Z swalker $
//
package edu.gemini.spModel.target.system;

import java.io.Serializable;

/**
 * Base class for science program types (which are immutable).
 */
public abstract class TypeBase implements Serializable {
    private int _typeCode;
    private String _name;

    protected TypeBase(int typeCode, String name) {
        _typeCode = typeCode;
        _name = name;
    }

    public int getTypeCode() {
        return _typeCode;
    }

    public String getName() {
        return _name;
    }

    public String toString() {
        return getClass().getName() + " [typeCode=" + _typeCode +
                ", name=" + getName() + "]";
    }

}

