package edu.gemini.spModel.data.config;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A straight-forward implementation of the <code>IConfig</code>
 * interface.  A grouping of {@link ISysConfig parameter sets}.
 *
 * <p><b>Note that this implementation is not synchronized.</b>
 */
public class DefaultConfig implements IConfig {
    private LinkedList<ISysConfig> _sysConfigList = new LinkedList<>();

    public DefaultConfig() {
    }

    public DefaultConfig(Set<ISysConfig> sysConfigs) {
        putSysConfigs(sysConfigs);
    }

    /**
     * Performs a deep clone (except that contained parameter values are not
     * cloned).
     */
    public Object clone() {
        DefaultConfig res;
        try {
            res = (DefaultConfig) super.clone();
        } catch (CloneNotSupportedException ex) {
            // can't happen since this class implements Cloneable
            return null;
        }

        res._sysConfigList = new LinkedList<>();

        res._sysConfigList.addAll(_sysConfigList.stream().map(sysConfig -> (ISysConfig) sysConfig.clone()).collect(Collectors.toList()));

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

        // Is the object at least a configuration?
        if (obj.getClass() != getClass()) return false;

        DefaultConfig conf = (DefaultConfig) obj;

        // Compare the parameter sets.
        return _sysConfigList.equals(conf._sysConfigList);

    }

    /**
     * Overrides to agree with the redefinition of <code>equals</code>.
     */
    public int hashCode() {
        return _sysConfigList.hashCode();
    }

    public int getParameterCount() {
        int count = 0;

        for (ISysConfig sysConfig : _sysConfigList) {
            count += sysConfig.getParameterCount();
        }

        return count;
    }

    public int getSysConfigCount() {
        return _sysConfigList.size();
    }

    public ISysConfig getSysConfig(String systemName) {
        for (ISysConfig sysConfig : _sysConfigList) {
            if (sysConfig.getSystemName().equals(systemName))
                return sysConfig;
        }
        return null;
    }

    public boolean containsParameter(String systemName, String paramName) {
        ISysConfig sc = getSysConfig(systemName);
        return sc != null && sc.containsParameter(paramName);
    }

    public boolean containsSysConfig(String systemName) {
        return getSysConfig(systemName) != null;
    }

    public Object getParameterValue(String systemName, String paramName) {
        ISysConfig sc = getSysConfig(systemName);
        if (sc == null) return null;
        return sc.getParameterValue(paramName);
    }

    public int getParameterValue(String systemName, String paramName, int defaultValue) {
        ISysConfig sc = getSysConfig(systemName);
        if (sc == null) return defaultValue;
        return sc.getParameterValue(paramName, defaultValue);
    }

    public double getParameterValue(String systemName, String paramName, double defaultValue) {
        ISysConfig sc = getSysConfig(systemName);
        if (sc == null) return defaultValue;
        return sc.getParameterValue(paramName, defaultValue);
    }

    public void putParameter(String systemName, IParameter ip) {
        ISysConfig sc = getSysConfig(systemName);
        if (sc == null) {
            sc = new DefaultSysConfig(systemName);
            appendSysConfig(sc);
        }
        sc.putParameter(ip);
    }

    public void removeParameter(String systemName, String paramName) {
        ISysConfig sc = getSysConfig(systemName);
        if (sc != null) {
            sc.removeParameter(paramName);

            if (sc.getParameterCount() == 0) {
                removeSysConfig(systemName);
            }
        }
    }

    public void appendSysConfig(ISysConfig sysConfig) {
        _sysConfigList.add(sysConfig);
    }

    public void putSysConfig(ISysConfig sysConfig) {
        _sysConfigList.addFirst(sysConfig);
    }

    public void removeSysConfig(String systemName) {
        ISysConfig sc = getSysConfig(systemName);
        if (sc != null)
            _sysConfigList.remove(sc);
    }

    public Set<String> getSystemNames() {
        Set<String> set = new HashSet<>(_sysConfigList.size());
        set.addAll(_sysConfigList.stream().map(ISysConfig::getSystemName).collect(Collectors.toList()));
        return set;
    }

    public Collection<ISysConfig> getSysConfigs() {
        return _sysConfigList;
    }

    public void putSysConfigs(Collection<ISysConfig> sysConfigs) {
        removeSysConfigs();
        _sysConfigList.addAll(sysConfigs.stream().collect(Collectors.toList()));
    }

    public void mergeSysConfigs(Collection<ISysConfig> sysConfigs) {
        for (ISysConfig sc0 : sysConfigs) {
            String name = sc0.getSystemName();
            ISysConfig sc1 = getSysConfig(name);
            if (sc1 == null) {
                _sysConfigList.add((ISysConfig) sc0.clone());
            } else {
                sc1.mergeParameters(sc0);
            }
        }
    }

    public void mergeSysConfigs(IConfig config) {
        if (config instanceof DefaultConfig) {
            mergeSysConfigs(((DefaultConfig) config)._sysConfigList);
        } else {
            mergeSysConfigs(config.getSysConfigs());
        }
    }

    public void removeSysConfigs() {
        _sysConfigList.clear();
    }

}
