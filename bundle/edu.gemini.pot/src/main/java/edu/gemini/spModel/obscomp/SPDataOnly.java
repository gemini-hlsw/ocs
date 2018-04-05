package edu.gemini.spModel.obscomp;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.gemini.init.SimpleNodeInitializer;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;

import java.util.*;

/**
 * The SPDataOnly is defined to be a "hidden" container of generic
 * information, which is not visualized in the OT view.
 */
public final class SPDataOnly extends AbstractDataObject {
    /**
     * This obs component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.DATA_DATAONLY;

    public static final ISPNodeInitializer<ISPObsComponent, SPDataOnly> NI =
        new SimpleNodeInitializer<>(SP_TYPE, () -> new SPDataOnly());

    // for serialization
    private static final long serialVersionUID = 1L;

    // Temp HashMap for data storage
    private Map<String, List<String>> _data;

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
        result._data = new HashMap<>();

        // Make a copy of the values.
        for (String key : _data.keySet()) {
            List<String> src = _data.get(key);
            // src can be null
            if (src != null) {
                // Okay since values are Strings
                List<String> dest = new ArrayList<>(src);
                result._data.put(key, dest);
            }
        }

        return result;
    }

    /**
     * Provide access to the internal table.
     */
    public Map<String, List<String>> getTable() {
        // Just return it for now -- this is only for testing.
        if (_data == null) _data = new HashMap<>();
        return _data;
    }

    /**
     * A generic method for adding a property.
     */
    public void setProperty(String prop, List<String> values) {
        getTable().put(prop, values);
    }

    /**
     * Return the value for a property.
     */
    public List<String> getProperty(String key) {
        if ((key == null) || (size() == 0)) return null;

        return  getTable().get(key);
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
     */
    public ParamSet getParamSet(PioFactory factory) {
        return super.getParamSet(factory);
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);
    }
}
