// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: DefaultConfigParameter.java 38078 2011-10-18 15:15:29Z swalker $
//

package edu.gemini.spModel.data.config;

import java.util.Iterator;


/**
 * A simple implementation of the <code>IParameter</code>
 * interface where the value is an instance of <code>{@link ISysConfig}</code>.
 * <p>
 * Then intended use is that some complex parameters have their own related set of
 * parameters.
 * <p><b>Note that this implementation is not synchronized.</b>
 */
public class DefaultConfigParameter implements IConfigParameter {
    private ISysConfig _config;

    /**
     * The Factory method for creating a <code>DefaultConfigParameter</code> from a name.
     * <p>The sysName is the name given to the system in the <code>ISysConfig<code>
     * <p>The value is an instance of <code>{@link DefaultSysConfig}</code>.
     */
    static public DefaultConfigParameter getInstance(String sysName) {
        if (sysName == null) {
            throw new IllegalArgumentException("parameter 'name' may not be null");
        }

        return new DefaultConfigParameter(sysName);
    }

    // The private constructor
    private DefaultConfigParameter(String name) {
        _config = new DefaultSysConfig(name);
    }

    /**
     * Performs a clone of the parameter.
     * The parameter value is also cloned if it is of type ArrayList.
     */
    public Object clone() {
        DefaultConfigParameter res;
        try {
            res = (DefaultConfigParameter) super.clone();
        } catch (CloneNotSupportedException ex) {
            // can't happen since this object implements Cloneable
            throw new RuntimeException();
        }

        res._config = (ISysConfig) _config.clone();

        return res;
    }

    /**
     * Overrides to provide semantic equality.
     */
    public boolean equals(Object obj) {
        // Is this the same object?
        if (this == obj) return true;

        // Is the object null?
        if (obj == null) return false;

        // Does the given object have the same class as this?
        if (obj.getClass() != getClass()) return false;

        DefaultConfigParameter other = (DefaultConfigParameter) obj;

        // Check the names.
        return _config.equals(other._config);
    }

    /**
     * Overrides to agree with the redefinition of <code>equals</code>.
     */
    public int hashCode() {
        return _config.hashCode();
    }

    public String getName() {
        return _config.getSystemName();
    }

    /**
     * Returns the number of parameters in the config.
     */
    public int getParameterCount() {
        return _config.getParameterCount();
    }

    /**
     * Required for the interface.
     */
    public void setValue(Object obj) {
        if (!(obj instanceof ISysConfig)) throw new IllegalArgumentException("Value must be an ISysConfig");

        ISysConfig src = (ISysConfig) obj;
        _config = (ISysConfig) src.clone();
    }

    /**
     * Diagnostic method to dump the contents of a <code>DefaultConfigParameter</code>
     */
    public void dump() {
        Iterator i = _config.getParameters().iterator();
        while (i.hasNext()) {
            ((IParameter) i.next()).getAsString();
        }
    }

    /**
     * Required for the interface.
     */
    public Object getValue() {
        return _config;
    }

    public String toString() {
        return getClass().getName() + " [name=" + getName() + ", value=" + _config + "]";
    }

    public String getAsString() {
        return getName();
    }


}
