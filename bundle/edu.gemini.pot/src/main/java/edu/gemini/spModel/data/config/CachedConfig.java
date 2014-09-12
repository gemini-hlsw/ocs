/**
 * $Id: CachedConfig.java 6743 2005-11-17 19:27:55Z brighton $
 */

package edu.gemini.spModel.data.config;


/**
 * An IConfig that lets you specify an extra cache ISysConfig object.
 * The extra object can be used to keep track of parameter values in a sequence.
 */
public class CachedConfig extends DefaultConfig {

    private ISysConfig _cache;

    public CachedConfig(ISysConfig cache) {
        _cache = cache;
    }

    public ISysConfig getCache() {
        return _cache;
    }

    /**
     * Returns the parameter value (and caches it), if found. Otherwise, returns the
     * cached value, or null, if not in the cache.
     */
    public Object getParameterValue(String systemName, String paramName) {
        ISysConfig sc = getSysConfig(systemName);
        IParameter p = null;
        if (sc != null) {
            p = sc.getParameter(paramName);
        }

        if (p == null) {
            p = _cache.getParameter(paramName);
            if (p != null) {
                return p.getValue();
            }
            return null;
        }

        _cache.putParameter(p);
        return p.getValue();
    }

    public int getParameterValue(String systemName, String paramName, int defaultValue) {
        Object o = getParameterValue(systemName, paramName);
        if (o != null) {
            try {
                return Integer.parseInt(o.toString());
            } catch (NumberFormatException e) {
            }
        }
        return defaultValue;
    }

    public double getParameterValue(String systemName, String paramName, double defaultValue) {
        Object o = getParameterValue(systemName, paramName);
        if (o != null) {
            try {
                return Double.parseDouble(o.toString());
            } catch (NumberFormatException e) {
            }
        }
        return defaultValue;
    }
}
