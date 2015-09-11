// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: DefaultSysConfig.java 37893 2011-10-06 15:25:48Z swalker $
//

package edu.gemini.spModel.data.config;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * A straight-forward implementation of the <code>ISysConfig</code>
 * interface.  A grouping of parameters destined for the same system (PSA).
 *
 * <p><b>Note that this implementation is not synchronized.</b>
 */
public final class DefaultSysConfig implements ISysConfig {
    private static final Logger LOG = Logger.getLogger(DefaultSysConfig.class.getName());

    private String _systemName;
    private Map<String, IParameter> _paramList = new TreeMap<String, IParameter>();
    private final boolean _metadata;

    public DefaultSysConfig(String systemName, boolean isMetadata) {
        if (systemName == null) {
            throw new IllegalArgumentException(
                    "parameter set 'system name' may not be null");
        }
        _systemName = systemName;
        _metadata   = isMetadata;
    }

    public DefaultSysConfig(String systemName) {
        this(systemName, false);
    }

    public DefaultSysConfig(String systemName, Set<IParameter> params, boolean isMetadata) {
        this(systemName, isMetadata);
        putParameters(params);
    }

    public DefaultSysConfig(String systemName, Set<IParameter> params) {
        this(systemName, params, false);
    }

    public boolean isMetadata() {
        return _metadata;
    }

    /**
     * Performs a deep clone (except that contained parameter values are
     * not cloned).
     */

    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    public Object clone() {
        final DefaultSysConfig res;
        try {
            res = (DefaultSysConfig) super.clone();
        } catch (CloneNotSupportedException ex) {
            // Can't happen since this object implements Cloneable.
            throw new RuntimeException();
        }

        res._paramList = new TreeMap<String, IParameter>();
        for (IParameter param : _paramList.values()) {
            res._paramList.put(param.getName(), (IParameter) param.clone());
        }
        return res;
    }

    /**
     * Overrides to provide semantic equality.
     */
    public boolean equals(Object obj) {
        // Is this the same object?
        if (this == obj) return true;

        // Is this object null?
        if (obj == null) return false;

        // Is the object at least a parameter set?
        if (obj.getClass() != getClass()) return false;

        final DefaultSysConfig other = (DefaultSysConfig) obj;

        // Compare the system names.
        if (!_systemName.equals(other._systemName)) return false;

        // Compare the parameters in the list.
        return _paramList.equals(other._paramList);

    }

    /**
     * Overrides to agree with the redefinition of <code>equals</code>.
     */
    public int hashCode() {
        int result = _systemName.hashCode();
        return result * 37 + _paramList.hashCode();
    }

    public String getSystemName() {
        return _systemName;
    }

    public int getParameterCount() {
        return _paramList.size();
    }

    public boolean containsParameter(String paramName) {
        return (getParameter(paramName) != null);
    }

    public Object getParameterValue(String paramName) {
        final IParameter param = getParameter(paramName);
        return (param == null) ? null : param.getValue();
    }

    public int getParameterValue(String paramName, int defaultValue) {
        Object o = getParameterValue(paramName);
        if (o != null) {
            try {
               return Integer.parseInt(o.toString());
            } catch(NumberFormatException e) {
                String msg = "Problem parsing parameter '" + paramName +
                        "' = '" + o.toString() + "' as an integer, returning: " +
                        defaultValue;
                LOG.log(Level.INFO, msg);
            }
        }
        return defaultValue;
    }

    public double getParameterValue(String paramName, double defaultValue) {
        Object o = getParameterValue(paramName);
        if (o != null) {
            try {
               return Double.parseDouble(o.toString());
            } catch(NumberFormatException e) {
                String msg = "Problem parsing parameter '" + paramName +
                        "' = '" + o.toString() + "' as a double, returning: " +
                        defaultValue;
                LOG.log(Level.FINE, msg);
            }
        }
        return defaultValue;
    }

    public IParameter getParameter(String paramName) {
        return _paramList.get(paramName);
    }

    public void putParameter(IParameter param) {
        if ((param == null) || (param.getName() == null)) return;

        // allan: ignore parameters with blank values. This fixes a bug where a blank
        // value in the first row of an instrument iterator overwrites the value in
        // the static instrument component.
        final Object value = param.getValue();
        if (value instanceof String && ((String)value).length() == 0) {
            return;
        }

        final String name = param.getName();
        removeParameter(name);
        _paramList.put(name, param);
    }

    public void removeParameter(String paramName) {
        _paramList.remove(paramName);
    }

    public Set<String> getParameterNames() {
        return new TreeSet<String>(_paramList.keySet());
    }

    public Collection<IParameter> getParameters() {
        return new ArrayList<IParameter>(_paramList.values());
    }

    public void putParameters(Collection<IParameter> params) {
        removeParameters();
        mergeParameters(params);
    }

    public void mergeParameters(Collection<IParameter> params) {
        for (IParameter param1 : params) putParameter(param1);
    }

    public void mergeParameters(ISysConfig sysConfig) {
        if (sysConfig instanceof DefaultSysConfig) {
            // Avoid a copy if the argument is a DefaultSysConfig
            DefaultSysConfig dsc = (DefaultSysConfig) sysConfig;
            mergeParameters(dsc._paramList.values());
        } else {
            mergeParameters(sysConfig.getParameters());
        }
    }

    public void removeParameters() {
        _paramList.clear();
    }

    public void dumpState() {
        System.out.println("System Configuration: " + getSystemName());
        for (IParameter p : _paramList.values()) {
            System.out.println("\t" + p);
        }
    }
}

