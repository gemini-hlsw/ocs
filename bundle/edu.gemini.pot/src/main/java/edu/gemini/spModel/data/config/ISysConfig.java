// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: ISysConfig.java 37893 2011-10-06 15:25:48Z swalker $
//

package edu.gemini.spModel.data.config;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 * A grouping of {@link IParameter parameter}s destined for the same
 * system (PSA).  No two parameters with the same name may exist in
 * the same sys config.  The sys config is <code>Serializable</code>
 * provided all of its parameters are <code>Serializable</code>.
 *
 * @see IConfig
 * @see IParameter
 */
public interface ISysConfig extends Cloneable, Serializable {
    /**
     * Clones the sys config (but not the values of the parameters).
     * The returned sys config is completely distinct except that its
     * parameters have references to the same value objects as the
     * corresponding parameters in this sys config.
     */
    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    Object clone();

    /**
     * Marks this sys config as a holder of metadata used to drive the
     * config building process.
     */
    boolean isMetadata();

    /**
     * Gets the PSA that will receive the sys config.
     */
    String getSystemName();

    /**
     * Gets the number of parameters in the sys config.
     */
    int getParameterCount();

    /**
     * Returns <code>true</code> if the sys config contains the given
     * parameter.
     *
     * @param paramName the name of the <code>{@link IParameter}</code>
     * whose existence in the config should be checked
     */
    boolean containsParameter(String paramName);

    /**
     * Gets the value of the <code>{@link IParameter}</code> with the given
     * <code>paramName</code>, if it exists.
     *
     * @param paramName the name of the <code>{@link IParameter}</code>
     * whose value should be returned
     *
     * @return the corresponding parameter's value if it is present in the
     * config; <code>null</code> otherwise
     */
    Object getParameterValue(String paramName);

    /**
     * Gets the value of the <code>{@link IParameter}</code> with the given
     * <code>paramName</code> as an int, if it exists.
     *
     * @param paramName the name of the <code>{@link IParameter}</code>
     * whose value should be returned
     *
     * @param defaultValue the value to return in case the parameter is not found or is
     * not a number
     *
     * @return the corresponding parameter's int value if it is present in the
     * config; <code>defaultValue</code> otherwise
     */
    int getParameterValue(String paramName, int defaultValue);

    /**
     * Gets the value of the <code>{@link IParameter}</code> with the given
     * <code>paramName</code> as a double, if it exists.
     *
     * @param paramName the name of the <code>{@link IParameter}</code>
     * whose value should be returned
     *
     * @param defaultValue the value to return in case the parameter is not found or is
     * not a number
     *
     * @return the corresponding parameter's double value if it is present in the
     * config; <code>defaultValue</code> otherwise
     */
    double getParameterValue(String paramName, double defaultValue);

    /**
     * Gets the parameter with the given <code>paramName</code> if it
     * is present in the sys config.  The parameter returned is <em>not</em>
     * a copy of the one in this system configuration.
     *
     * @param paramName the name of the <code>{@link IParameter}</code>
     * that should be returned
     *
     * @return the corresponding parameter if it is present in the sys config;
     * <code>null</code> otherwise
     */
    IParameter getParameter(String paramName);

    /**
     * Adds the parameter to the sys config, if a parameter with the same
     * name is not already present.  Otherwise, the existing parameter is
     * replaced with the given parameter.  A copy of the <code>param</code>
     * is <em>not</em> made.  After this operation the system configuration
     * and the caller will have a reference to the same parameter.
     *
     * @param param the parameter to add or to take the place of the
     * existing parameter
     */
    void putParameter(IParameter param);

    /**
     * Removes the parameter with the given <code>paramName</code> if it
     * exists.  Otherwise, does nothing.
     *
     * @param paramName the parameter to remove
     */
    void removeParameter(String paramName);

    /**
     * Gets the names of the parameters in the sys config in a
     * <code>Set</code>.
     *
     * @return a <code>Set</code> containing the name of every parameter
     * in the sys configuration
     */
    Set<String> getParameterNames();

    /**
     * Gets the contained parameters as a <code>java.util.Collection</code>.
     * The returned <code>Collection</code> may be freely modified by the
     * caller without effecting the internal representation of the system
     * configuration.
     *
     * @return a <code>Collection</code> of <code>{@link IParameter}</code>
     * containing every parameter in the system configuration
     */
    Collection<IParameter> getParameters();

    /**
     * Sets the collection of parameters, forgetting any previous parameters
     * in the system configuration.  The <code>params</code> argument
     * may be subsequently modified by the caller without effecting the
     * internal representation of an object of this class.  If more than
     * one parameter in the given <code>Collection</code> has the same name,
     * only one will ultimately be stored in the configuration.  There are
     * no guarantees about which will be choosen.
     *
     * <p>This operation is equivalent to calling
     * <code>{@link #removeParameters}</code> and then iterating over the
     * collection calling <code>{@link #putParameter}</code> for each
     * parameter.
     *
     * @param params a <code>Collection</code> of
     * <code>{@link IParameter}</code> that should be added to the
     * system configuration
     */
    void putParameters(Collection<IParameter> params);

    /**
     * Adds (a copy of) all the parameters in the given
     * <code>Collection</code> to this system configuration, replacing any
     * existing parameters of the same name.
     *
     * <p>Since the parameters themselves are cloned, a subsequent change
     * to a parameter in <code>sysConfig</code> will <em>not</em> affect
     * both configurations.  This is in direct contrast to the "put" methods.
     *
     * @param params a <code>Collection</code> of
     * <code>{@link IParameter}</code> that should be merged with this
     * system configuration
     *
     * @throws ClassCastException if any member of the <code>params</code>
     * collection is not an <code>IParameter</code> instance
     */
    void mergeParameters(Collection<IParameter> params)
            throws ClassCastException;

    /**
     * Adds (a copy of) all the parameters in the given
     * <code>sysConfig</code> to this system configuration, replacing any
     * existing parameters of the same name.  The <code>sysConfig</code>
     * argument is not affected.
     *
     * <p>Since the parameters themselves are cloned, a subsequent change
     * to a parameter in <code>sysConfig</code> will <em>not</em> affect
     * both configurations.  This is in direct contrast to the "put" methods.
     *
     * @param sysConfig a system configuration whose parameters should be
     * merged with this system configuration
     */
    void mergeParameters(ISysConfig sysConfig);

    /**
     * Removes all the parameters in the configuration, leaving an empty
     * system configuration.
     */
    void removeParameters();
}
