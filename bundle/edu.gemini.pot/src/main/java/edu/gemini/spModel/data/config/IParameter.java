// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: IParameter.java 4726 2004-05-14 16:50:12Z brighton $
//

package edu.gemini.spModel.data.config;

import java.io.Serializable;


/**
 * An interface that describes a single configurable parameter in a
 * system.  The parameter is serializable provided its value is
 * serializable.
 *
 * @see IConfig
 * @see ISysConfig
 */
public interface IParameter extends Cloneable, Serializable {
    /**
     * Clones the parameter (but not the value).  In other words,
     * performs a shallow clone.
     */
    Object clone();

    /**
     * Gets the name of the parameter.
     */
    String getName();

    /**
     * Gets the value of the parameter.  A reference is simply returned so
     * a change to the value object will effect this parameter.
     */
    Object getValue();

    /**
     * Sets the value of the parameter.  Should simply store a reference
     * to this value.  Changes made to the value external to the parameter
     * will be visible.
     */
    void setValue(Object value);

    /**
     * Returns the value as a String that can be used to display the contents of
     * the object.
     */
    String getAsString();
}

