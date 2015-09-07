// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqConfigComp.java 18743 2009-03-12 22:15:39Z swalker $
//
package edu.gemini.spModel.seqcomp;

import edu.gemini.pot.sp.ISPSeqObject;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.pio.Param;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * This is the base class for all configuration-based components.
 */
public abstract class SeqConfigComp extends AbstractDataObject
        implements IConfigProvider, ISPSeqObject {

    // for serialization
    private static final long serialVersionUID = 1L;

    // The current configuration set with {@link setSysConfig}
    private ISysConfig _sysConfig;

    /**
     * Construct with a subtype.
     */
    public SeqConfigComp(SPComponentType type) {
        this(type, type.narrowType);
    }

    /**
     * Construct with a specific configuration name.
     */
    public SeqConfigComp(SPComponentType type, String configName) {
        super(type);
        // This ensures that an empty config will be returned if a get is
        // done before a set
        _sysConfig = new DefaultSysConfig(configName);
    }


    /**
     * Return the number of iterations performed by this object.
     */
    public int getStepCount() {
        int count = 0;
        if (_sysConfig != null) {
            Collection<IParameter> c = _sysConfig.getParameters();
            if (c != null) {
                for (IParameter aC : c) {
                    List l = (List) aC.getValue();
                    if (l != null) {
                        count = Math.max(count, l.size());
                    }
                }
            }
        }
        return count;
    }


    /**
     * Get the name of the item being iterated over.  Subclasses must
     * define, else the Narrow Type is used.
     */
    public String getItemName() {
        return getType().narrowType;
    }

    /**
     * Returns the current sys config for the iterator.  The ISysConfig
     * is cloned so the caller can modify it with careless abandon.
     */
    public ISysConfig getSysConfig() {
        return (ISysConfig) _sysConfig.clone();
    }

    /**
     * Set the itertor ISysConfig.  It is assumed that the contents
     * make sense for the iterator.
     * <p>
     * The input ISysConfig is cloned so the caller can no longer alter
     * the configuration after setting it.
     */
    public void setSysConfig(ISysConfig sysConfig) {
        sysConfig = (ISysConfig) sysConfig.clone();
        _sysConfig = sysConfig;
        firePropertyChange((String)null, null, null);
    }


    /**
     * Diagnostic method to dump the current configuration.
     */
    public void dumpState() {
        if (_sysConfig == null) {
            System.err.println("System config is null!");
            return;
        }

        if (_sysConfig instanceof DefaultSysConfig) {
            DefaultSysConfig dsc = (DefaultSysConfig) _sysConfig;
            dsc.dumpState();
        }
    }

    /**
     * Return a parameter set describing the current state of this object.
     * @param factory
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        ISysConfig sysConfig = getSysConfig();
        int paramCount = sysConfig.getParameterCount();

        // If there are no params, return an empty map
        if (paramCount == 0) {
            return paramSet;
        }

        Map<String, PropertyDescriptor> map = null;
        if (this instanceof PropertyProvider) {
            map = ((PropertyProvider) this).getProperties();
        }

        for (String name : sysConfig.getParameterNames()) {
            PropertyDescriptor pd = null;
            if (map != null) pd = map.get(name);

            Object paramValue = sysConfig.getParameterValue(name);

            if (paramValue instanceof List) {
                List<Object> vals    = (List<Object>) paramValue;
                List<String> strVals = PropertySupport.valueToString(vals, pd);
                Pio.addListParam(factory, paramSet, name, strVals);

            } else if (paramValue != null) {
                String strVal = PropertySupport.valueToString(paramValue, pd);
                Pio.addParam(factory, paramSet, name, strVal);
            }
        }

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        ISysConfig sysConfig = getSysConfig();
        sysConfig.removeParameters();

        Map<String, PropertyDescriptor> map = null;
        if (this instanceof PropertyProvider) {
            map = ((PropertyProvider) this).getProperties();
        }

        //noinspection unchecked
        for (Param p : ((List<Param>) paramSet.getParams())) {
            String name = p.getName();
            //noinspection unchecked
            List<String> strValues = p.getValues();

            if (map == null) {
                setNulls(strValues);
                sysConfig.putParameter(DefaultParameter.getInstance(name, strValues));
            } else {
                // If there is a property descriptor map, then the property
                // must exist in the map or we will ignore it.  Unfortunately
                // we mix properties like version and title in with the sys
                // config, but these are taken care of in the call to
                // super.setParamSet().
                PropertyDescriptor pd = map.get(name);
                if (pd != null) {
                    if (!(Option.class.isAssignableFrom(pd.getPropertyType()))) {
                        setNulls(strValues);
                    }
                    Collection<Object> objValues = PropertySupport.stringToValue(strValues, pd);
                    sysConfig.putParameter(DefaultParameter.getInstance(name, objValues));
                }
            }
        }

        setSysConfig(sysConfig);
    }

    private void setNulls(List<String> strValues) {
        if ((strValues == null) || (strValues.size() <= 1)) return;

        ListIterator<String> lit = strValues.listIterator();
        String prev = lit.next();
        while (lit.hasNext()) {
            String cur = lit.next();
            if ((cur == null) || "".equals(cur)) {
                cur = prev;
                lit.set(prev);
            }
            prev = cur;
        }
    }
}
