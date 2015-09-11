// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: IConfig.java 19014 2009-03-26 19:39:27Z swalker $
//

package edu.gemini.spModel.data.config;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;


/**
 * A grouping of {@link ISysConfig system configuration}s.  Whereas system
 * configurations control an individual system, an <code>IConfig</code>
 * instance contains information to control several systems.  It is the
 * highest level in the <code>IConfig, ISysConfig, IParameter</code>
 * hierarchy.
 *
 * <p>No two system configurations with the same system name may exist in
 * the same configuration.  For example, two "NIRI" system configurations
 * cannot live in the same configuration.
 *
 * <p>The configuration is <code>Serializable</code> provided all the
 * parameters in its system configurations are also <code>Serializable</code>.
 *
 * @see ISysConfig
 * @see IParameter
 */
public interface IConfig extends Cloneable, Serializable {
    /**
     * Clones the config (but not the values of the parameters).
     * The returned config is completely distinct except that its
     * parameters have references to the same value objects as the
     * corresponding parameters in this config.
     */
    Object clone();

    /**
     * Gets the number of parameters in all the system configurations in
     * this config.
     */
    int getParameterCount();

    /**
     * Gets the number of system configurations in this configuration.
     */
    int getSysConfigCount();

    /**
     * Returns <code>true</code> if the configuration contains the
     * given system configuration.
     *
     * @param sysName the name of the <code>{@link ISysConfig}</code>
     * whose existence in the configuration should be checked
     */
    boolean containsSysConfig(String sysName);

    /**
     * Returns <code>true</code> if the configuration contains the
     * indicated system configuration which contains the given parameter.
     * This method is actually a short-cut for the expression:
     * <pre>
     *	config.containsSysConfig(sysName) &&
     *   config.getSysConfig(sysName).containsParameter(paramName)
     * </pre>
     *
     * @param sysName the name of the <code>{@link ISysConfig}</code>
     * whose existence in the configuration should be checked
     *
     * @param paramName the name of the <code>{@link IParameter}</code>
     * whose existence in the system configuration should be checked
     */
    boolean containsParameter(String sysName, String paramName);

    /**
     * Gets the value of the indicated <code>{@link IParameter}</code>, or
     * <code>null</code> if either the system configuration or parameter are
     * not present.  This method is a short-cut for first retrieving the
     * system configuration and asking it for the parameter value.
     *
     * @param sysName the name of the <code>{@link ISysConfig}</code>
     * that contains the parameter
     *
     * @param paramName the name of the <code>{@link IParameter}</code>
     * whose value should be retrieved
     *
     * @return the corresponding parameter's value if both the system
     * configuration and parameter are present; <code>null</code> otherwise
     */
    Object getParameterValue(String sysName, String paramName);

    /**
     * Gets the value of the indicated <code>{@link IParameter}</code>, or
     * <code>null</code> if either the system configuration or parameter are
     * not present.  This method is a short-cut for first retrieving the
     * system configuration and asking it for the parameter value.
     *
     * @param sysName the name of the <code>{@link ISysConfig}</code>
     * that contains the parameter
     *
     * @param paramName the name of the <code>{@link IParameter}</code>
     * whose value should be retrieved
     *
     * @param defaultValue the value to return in case the parameter is not found or is
     * not a number
     *
     * @return the corresponding parameter's int value if it is present in the
     * config; <code>defaultValue</code> otherwise
     */
    int getParameterValue(String sysName, String paramName, int defaultValue);

    /**
     * Gets the value of the indicated <code>{@link IParameter}</code>, or
     * <code>null</code> if either the system configuration or parameter are
     * not present.  This method is a short-cut for first retrieving the
     * system configuration and asking it for the parameter value.
     *
     * @param sysName the name of the <code>{@link ISysConfig}</code>
     * that contains the parameter
     *
     * @param paramName the name of the <code>{@link IParameter}</code>
     * whose value should be retrieved
     *
     * @param defaultValue the value to return in case the parameter is not found or is
     * not a number
     *
     * @return the corresponding parameter's double value if it is present in the
     * config; <code>defaultValue</code> otherwise
     */
    double getParameterValue(String sysName, String paramName, double defaultValue);

    /**
     * Sets the value of the indicated <code>{@link IParameter}</code> if
     * it exists.  If not, a new parameter is created and added to the
     * indicated system configuration if it exists.  If not, a new system
     * configuration with the given name is first created.  This is a
     * convenience method in that the same functionality could be achieved
     * through more primitive methods.
     *
     * @param sysName the name of the <code>{@link ISysConfig}</code>
     * that should contain the parameter
     *
     * @param param the <code>{@link IParameter}</code>
     * whose value should be set
     *
     */
    //   void putParameterValue(String sysName, String paramName, Object value);
    void putParameter(String sysName, IParameter param);

    /**
     * Removes the indicated <code>{@link IParameter}</code> if it exists.
     * If either the system configuration or parameter do not exist, nothing
     * is done.
     *
     * @param sysName the name of the <code>{@link ISysConfig}</code>
     * that should contain the parameter
     *
     * @param paramName the name of the <code>{@link IParameter}</code>
     * within the system configuration that should be removed
     */
    void removeParameter(String sysName, String paramName);

    /**
     * Gets the system configuration with the given <code>sysName</code>
     * if it is present in the configuration.  The value returned is
     * <em>not</em> a copy of the one in this configuration.
     *
     * @param sysName the name of the <code>{@link ISysConfig}</code>
     * that should be retrieved
     *
     * @return the corresponding system configuration if present;
     * <code>null</code> otherwise
     */
    ISysConfig getSysConfig(String sysName);

    /**
     * Adds the system configuration to the configuration, if a sys config
     * with the same name is not already present.  Otherwise, the existing
     * sys config is <em>replaced</em> with <code>sysConfig</code>.  A copy
     * of <code>sysConfig</code> is <em>not</em> made.  After this operation
     * the configuration and the caller will have a reference to the same
     * system configuration.  The config is appended to the configuration.
     *
     * @param sysConfig the system configuration to add or to take the place
     * of the existing parameter
     */
    void appendSysConfig(ISysConfig sysConfig);

    /**
     * Adds the system configuration to the configuration, if a sys config
     * with the same name is not already present.  Otherwise, the existing
     * sys config is <em>replaced</em> with <code>sysConfig</code>.  A copy
     * of <code>sysConfig</code> is <em>not</em> made.  After this operation
     * the configuration and the caller will have a reference to the same
     * system configuration.  The config is put at the fron of the
     * configuration.
     *
     * @param sysConfig the system configuration to add or to take the place
     * of the existing parameter
     */
    void putSysConfig(ISysConfig sysConfig);

    /**
     * Removes the system configuration with the given <code>sysName</code>
     * if it exists.  Otherwise, does nothing.
     *
     * @param sysName the system configuration to remove
     */
    void removeSysConfig(String sysName);

    /**
     * Gets the names of the contained system configurations in a
     * <code>Set</code>.
     *
     * @return a <code>Set</code> containing the name of every system
     * configuration in the configuration
     */
    Set<String> getSystemNames();

    /**
     * Gets the contained system configurations in a
     * <code>java.util.Collection</code>.  The returned <code>Collection</code>
     * may be freely modified by the caller without effecting the internal
     * representation of the configuration.
     *
     * @return a <code>Collection</code> of <code>{@link ISysConfig}</code>
     * containing every system configuration in this configuration
     */
    Collection<ISysConfig> getSysConfigs();

    /**
     * Sets the collection of system configurations, forgetting any previous
     * system configurations in this configuration.  The <code>sysConfigs</code>
     * argument may be subsequently modified by the caller without effecting
     * the internal representation of an object of this class.  If more than
     * one system configuration in the given <code>Collection</code> has the
     * same system name, only one will ultimately be stored in the
     * configuration.  There are no guarantees about which will be choosen.
     *
     * <p>This operation is equivalent to calling
     * <code>{@link #removeSysConfigs}</code> and then iterating over the
     * collection calling <code>{@link #putSysConfig}</code> for each
     * system configuration.
     *
     * @param sysConfigs a <code>Collection</code> of
     * <code>{@link ISysConfig}</code> that should be added to the configuration
     */
    void putSysConfigs(Collection<ISysConfig> sysConfigs);

    /**
     * Adds (a copy of) all the system configurations in the given
     * <code>Collection</code> to this configuration, merging with any
     * existing sys configs of the same name.  If an existing system
     * configuration is merged with, parameters in the new sys config will
     * take precedence over (replace) existing parameters with the same names.
     *
     * <p>Since the sys configs themselves are cloned, a subsequent change
     * will <em>not</em> affect the caller.  This is in direct contrast to
     * the "put" methods.
     *
     * @param sysConfigs a <code>Collection</code> of
     * <code>{@link ISysConfig}</code> that should be merged with this
     * configuration
     *
     * @throws ClassCastException if any member of the <code>sysConfigs</code>
     * collection is not an <code>ISysConfig</code> instance
     */
    void mergeSysConfigs(Collection<ISysConfig> sysConfigs);

    /**
     * Adds (a copy of) all the system configurations in the given
     * <code>config</code> to this configuration, merging with any
     * existing sys configs of the same name.  If an existing system
     * configuration is merged with, parameters in the new sys config will
     * take precedence over (replace) existing parameters with the same names.
     *
     * <p>Since the sys configs themselves are cloned, a subsequent change
     * will <em>not</em> affect the caller.  This is in direct contrast to
     * the "put" methods.
     *
     * @param config a configuration whose contained system configurations
     * should be merged with this system configuration
     */
    void mergeSysConfigs(IConfig config);

    /**
     * Removes all the system configurations in this configuration, leaving
     * an empty configuration.
     */
    void removeSysConfigs();
}

