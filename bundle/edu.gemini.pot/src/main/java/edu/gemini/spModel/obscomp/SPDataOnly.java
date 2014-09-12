// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SPDataOnly.java 6000 2005-04-29 19:30:48Z brighton $
//
package edu.gemini.spModel.obscomp;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;

import java.util.*;

/**
 * The SPDataOnly is defined to be a "hidden" container of generic
 * information, which is not visualized in the OT view.
 */
public class SPDataOnly extends AbstractDataObject {
    /**
     * This obs component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.DATA_DATAONLY;

    // for serialization
    private static final long serialVersionUID = 1L;

    // Temp HashMap for data storage
    private Map _data;

    /**
     * Default constructor.
     */
    public SPDataOnly() {
        super(SP_TYPE);
    }

    /**
     * Implementation of the clone method.
     */
    public Object clone() {
        SPDataOnly result;
        result = (SPDataOnly) super.clone();

        if (_data == null) return result;
        result._data = new HashMap();

        // Make a copy of the values.
        Iterator keys = _data.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            List src = (List) _data.get(key);
            // src can be null
            if (src != null) {
                // Okay since values are Strings
                List dest = new ArrayList(src);
                result._data.put(key, dest);
            }
        }

        return result;
    }

    /**
     * Provide access to the internal table.
     */
    public Map getTable() {
        // Just return it for now -- this is only for testing.
        if (_data == null) _data = new HashMap();
        return _data;
    }

    /**
     * A generic method for adding a property.
     */
    public void setProperty(String prop, List values) {
        getTable().put(prop, values);
    }

    /**
     * Return the value for a property.
     */
    public List getProperty(String key) {
        if ((key == null) || (size() == 0)) return null;

        return (List) getTable().get(key);
    }

    /**
     * Returns the number of properties in the component.
     */
    public int size() {
        if (_data == null) return 0;
        return getTable().size();
    }

    /**
     * Return a parameter set describing the current state of this object.
     * @param factory
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        // TODO

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        // TODO
    }
}
