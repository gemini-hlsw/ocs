// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqConfigCalUnit.java 37893 2011-10-06 15:25:48Z swalker $
//
package edu.gemini.spModel.gemini.calunit;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Diffuser;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Filter;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Lamp;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Shutter;
import edu.gemini.spModel.seqcomp.SeqConfigObsBase;

import java.util.Arrays;

/**
 * The Gemini CalUnit configuration iterator.
 */
public class SeqConfigCalUnit extends SeqConfigObsBase { // implements PropertyProvider {

    private static final long serialVersionUID = -3007351546622049494L;

    /**
     * This iter component's SP type.
     */
    public static final SPComponentType SP_TYPE = SPComponentType.ITERATOR_CALUNIT;

    /*
    // Properties
    public static final PropertyDescriptor LAMP_PROP;

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new HashMap<String, PropertyDescriptor>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    private static class DummyInstCalUnit {
        public Lamp getLamp() {
            return null;
        }

        public void setLamp(Lamp lamp) {
        }
    }

    static {
        LAMP_PROP = PropertySupport.init("lamp", DummyInstCalUnit.class, false, true);
        PRIVATE_PROP_MAP.put("lamp", LAMP_PROP);
    }
    */

    /**
     * Default constructor.
     */
    public SeqConfigCalUnit() {
        super(SP_TYPE);
    }

    /**
     * Return a configuration of available iterable items.
     */
    public ISysConfig getAvailableSysConfig(boolean isOnSite, boolean hasEngComp) {
        // Parent has no iterable items
        DefaultSysConfig sc = new DefaultSysConfig(getType().narrowType);

        // Dumb or smart calibration?
        sc.putParameter(DefaultParameter.getInstance(CalUnitConstants.SMART,        Boolean.TRUE));

        // Add the camera
        sc.putParameter(DefaultParameter.getInstance(CalUnitConstants.LAMP_PROP,     Arrays.asList(Lamp.values())));
        sc.putParameter(DefaultParameter.getInstance(CalUnitConstants.SHUTTER_PROP,  Arrays.asList(Shutter.values())));
        sc.putParameter(DefaultParameter.getInstance(CalUnitConstants.FILTER_PROP,   Arrays.asList(Filter.values())));
        sc.putParameter(DefaultParameter.getInstance(CalUnitConstants.DIFFUSER_PROP, Arrays.asList(Diffuser.values())));

        sc.putParameter(getExposureTimeParameter());
        sc.putParameter(getCoaddsParameter());
        return sc;
    }

    /*
    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }
    */
}
