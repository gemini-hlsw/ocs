// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: IConfigProvider.java 4726 2004-05-14 16:50:12Z brighton $
//

package edu.gemini.spModel.data.config;

import java.io.Serializable;


/**
 * An interface that describes a possible interface between
 * an observation component or sequence component and a
 * configuration builder matched to that component.
 * <p>
 * The interface describes one method which allows the
 * configuration builder to request a <code>{@link ISysConfig}</code>
 * from the component.
 * <p>
 * The client can also set the ISysConfig to be used for an upcoming
 * configuration build.
 *
 * @see ISysConfig
 */
public interface IConfigProvider extends Serializable {
    /**
     * Gets the ISysConfig configuration of the component.
     */
    ISysConfig getSysConfig();

    /**
     * Allows an outside source to set the ISysConfig.
     */
    void setSysConfig(ISysConfig config);
}

