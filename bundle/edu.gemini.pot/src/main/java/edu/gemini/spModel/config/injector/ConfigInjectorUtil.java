package edu.gemini.spModel.config.injector;

import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;

import java.beans.PropertyDescriptor;

/**
 * Support code for working directly with configuration sequences and property
 * descriptors.
 */
public enum ConfigInjectorUtil {
    instance;

    /**
     * Lookup the value of a property in the given sys config object.
     *
     * @param pd property descriptor for the property of interest (may not
     * be null)
     * @param sys configuration in which to search for the property; if null
     * then the lookup returns null
     *
     * @return the value associated with the given property as found in the
     * given config; <code>null</code> if the config is null, if the property
     * is not found in the config, or if the value associated with the property
     * is not defined
     */
    public Object lookup(PropertyDescriptor pd, ISysConfig sys) {
        return lookup(pd.getName(), sys);
    }

    /**
     * Finds the value of the observing wavelength property in the given
     * configuration, if any.
     *
     * @param propName name of the property of interest (may not be null)
     * @param sys configuration in which to search (may be null)
     *
     * @return the value of the wavelength property, if any; returns
     * <code>null</code> if not present or the configuration is null
     */
    public Object lookup(String propName, ISysConfig sys) {
        if (sys == null) return null;
        IParameter param = sys.getParameter(propName);
        if (param == null) return null;
        return param.getValue();
    }

    /**
     * Determines whether the value of the given property differs in the two
     * configuration instances.  The "cur" configuration contains the step
     * under construction, which only contains the changes from the previous
     * step.  The "fullPrev" config contains the value in the previous step
     * for all properties that have been set to the current point.
     *
     * <p>If not defined in the "cur" configuration, then the previous value
     * is assumed to be up-to-date.  If not defined in "prevFull" but defined
     * in the current step, then the two values are considered to differ since
     * we're introducing a value for the property for the first time.
     *
     * <p>Otherwise, if the previous and current values are both defined then
     * we check whether they are different.
     *
     * @param pd descriptor of the property in question
     * @param cur current configuration step
     * @param fullPrev previous configuration step containing the last value
     * for all properties that have been defined in all previous steps
     *
     * @return <code>true</code> if the two versions can be considered
     * different; <code>false</code> otherwise
     */
    public boolean differs(PropertyDescriptor pd, ISysConfig cur, ISysConfig fullPrev) {
        Object curVal = lookup(pd, cur);
        Object oldVal = lookup(pd, fullPrev);
        if (curVal == null) return false;
        if (oldVal == null) return true;
        return !curVal.equals(oldVal);
    }

    /**
     * Gets the current value of the property.  It first checks the "cur"
     * configuration that is under construction.  If defined there, that is
     * the value.  Otherwise, it uses the last value defined from previous
     * steps.
     *
     * @param pd descriptor of the property in question
     * @param cur current configuration step
     * @param prev previous configuration step conaining all the last values
     * for all properties that have been defined in all previous steps
     *
     * @return the value that should be considered as current for this step,
     * if any; <code>null</code> if it cannot be determined
     */
    public Object getValue(PropertyDescriptor pd, ISysConfig cur, ISysConfig prev) {
        Object val = lookup(pd, cur);
        val = val == null ? lookup(pd, prev) : val;

        // A bit of a hack here...  Provide a default for uninitialized
        // double properties -- which are always central wavelength / disperser
        // lamda values.  They are unset when using not using a disperser, but
        // that shouldn't mean that the filter's wavelength cannot be determined.
        if (val == null) {
            Class<?> pt = pd.getPropertyType();
            if (double.class.equals(pt) || Double.class.equals(pt)) {
                val = 0.0;
            }
        }

        return val;
    }
}
