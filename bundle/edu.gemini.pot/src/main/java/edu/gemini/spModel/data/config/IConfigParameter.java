// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: IConfigParameter.java 38078 2011-10-18 15:15:29Z swalker $
//

package edu.gemini.spModel.data.config;

import java.io.Serializable;


/**
 * An interface that extends <code>{@link IParameter}</code> and describes a parameter that
 * contains other parameters.
 * <p>
 * This is handled by returning a value that is an instance of <code>{@link ISysConfig}</code>.
 *
 * @see IParameter
 * @see ISysConfig
 */
public interface IConfigParameter extends IParameter, Cloneable, Serializable {

    /**
     * Returns a name that should be used to represent the contents of this configuration
     * in builder tools.
     */
//    String getConfigName();

    /**
     * Allows the caller to set the name of the configuration used by builder tools.
     * <p>
     * throw IllegalArgumentException if the configName parameter is null.
     */
//    void setConfigName(String configName) throws IllegalArgumentException;

    /**
     * Returns the number of <code>IParameters</code> in the <code>IConfigParameter</code> without
     * the need to examine the value.
     */
    int getParameterCount();

}

