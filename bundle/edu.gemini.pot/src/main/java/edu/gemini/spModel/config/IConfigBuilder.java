// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: IConfigBuilder.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.config;

import edu.gemini.pot.sp.ISPCloneable;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;

import java.io.Serializable;

import java.util.Map;


/**
 * The configuration builder provides an abstract interface to the
 * iterative process of extracting configuration information from an
 * observation.  The use of a configuration builder instance follows
 * the pattern:
 *
 * <pre>
 * configBuilder.reset();
 * while (configBuilder.hasNext()) {
 *    IConfiguration nextConfig = new DefaultConfiguration();
 *    configBuilder.applyNext(nextConfig);
 *
 *    // use the nextConfig, breaking it into system configurations
 *    // and sending each to the appropriate system
 * }
 * </pre>
 *
 * <p>Configuration builders are associated with nodes in
 * a science program (as "user" objects).  A configuration builder might
 * make use of a nested node's builder to complete its task.  For example,
 * an observation's configuration builder will use its observation
 * component's builders to set the static configuration, and its
 * sequence component's builder for each step of the iteration.
 *
 * @see IConfig
 * @see ISysConfig
 * @see IParameter
 */
public interface IConfigBuilder extends ISPCloneable, Serializable {

    /**
     * The key used to retrieve a node's configuration builder from its
     * set of "user" objects.  See
     * <code>{@link edu.gemini.pot.sp.ISPNode#getClientData}</code>.
     */
    String USER_OBJ_KEY = "configBuilder";

    /**
     * Resets the configuration builder based upon the current state
     * of its node.  This method must be called before using the builder
     * and may be called subsequently to iterate over the available
     * configurations from the beginning.
     */
    void reset(Map<String, Object> options);

    /**
     * Returns <code>true</code> if more configuration information is
     * available (compare to <code>java.util.Iterator</code>).  A configuration
     * builder that has no further configurations should return
     * <code>false</code>.
     *
     * <p>The <code>{@link #reset}</code> must be called before the first
     * call to <code>hasNext</code>.
     *
     * @return <code>true</code> if there are any more steps left in the
     * iteration, <code>false</code> otherwise
     */
    boolean hasNext();

    /**
     * Applies this node's "next" configuration.  This method should only
     * be called after a successful call to <code>{@link #hasNext}</code>.
     *
     * @param current configuration that should be updated with the
     * parameters in "next" step
     *
     * @param prevFull configuration containing the state of the system in
     * the previous step which is often required to determine whether
     * a dependent value has changed in the current step
     */
    void applyNext(IConfig current, IConfig prevFull);
}
