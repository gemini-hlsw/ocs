/**
 * $Id: InstEngNifs.java 7063 2006-05-25 16:17:10Z anunez $
 */

package edu.gemini.spModel.gemini.nifs;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.gemini.nifs.NIFSParams.EngReadMode;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.seqcomp.SeqConfigNames;

import java.beans.PropertyDescriptor;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;

/**
 * The Nifs Engineering Component.
 */
public final class InstEngNifs extends AbstractDataObject implements PropertyProvider {

    // for serialization
    private static final long serialVersionUID = 4L;

    /**
     * This engineering obs component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.ENG_ENGNIFS;

    public static final ISPNodeInitializer<ISPObsComponent, InstEngNifs> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new InstEngNifs(), c -> new InstEngNifsCB(c));

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<String, PropertyDescriptor>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    //properties
    public static final PropertyDescriptor ENGINEERING_READMODE_PROP;
    public static final PropertyDescriptor NUMBER_OF_SAMPLES_PROP;
    public static final PropertyDescriptor PERIOD_PROP;
    public static final PropertyDescriptor NUMBER_OF_PERIODS_PROP;
    public static final PropertyDescriptor NUMBER_OF_RESETS_PROP;

    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
            PropertyDescriptor pd;
            pd = PropertySupport.init(propName, InstEngNifs.class, query, iter);
            pd.setExpert(true);
            PropertySupport.setEngineering(pd, true);
            PRIVATE_PROP_MAP.put(pd.getName(), pd);
            return pd;
        }
    static {
        boolean iter_yes = true;
        boolean query_no = false;

        ENGINEERING_READMODE_PROP = initProp("engineeringReadMode", query_no, iter_yes);
        NUMBER_OF_SAMPLES_PROP = initProp("numberOfSamples", query_no, iter_yes);
        PERIOD_PROP = initProp("period", query_no, iter_yes);
        NUMBER_OF_PERIODS_PROP = initProp("numberOfPeriods", query_no, iter_yes);
        NUMBER_OF_RESETS_PROP = initProp("numberOfResets", query_no, iter_yes);

    }
    //Default values
    public static final int DEF_NUMBER_OF_SAMPLES = 1;
    public static final int DEF_NUMBER_OF_RESETS = 1;
    public static final int DEF_NUMBER_OF_PERIODS = 1;
    public static final int DEF_PERIOD = 5;

    // Engineering settings
    private EngReadMode _engReadMode = EngReadMode.DEFAULT;
    private int _numberOfSamples = DEF_NUMBER_OF_SAMPLES;
    private int _period = DEF_PERIOD; // seconds
    private int _numberOfPeriods = DEF_NUMBER_OF_PERIODS;
    private int _numberOfResets = DEF_NUMBER_OF_RESETS;
    private String _VERSION = "2006B-1";

    /**
     * Constructor
     */
    public InstEngNifs() {
        super(SP_TYPE);
        setVersion(_VERSION);
    }

    /**
     * Implementation of the clone method.
     */

    public Object clone() {
        // No problems cloning here since private variables are immutable
        return (InstEngNifs)super.clone();
    }

    /**
     * Get the Engineering Read Mode.
     */
    public EngReadMode getEngineeringReadMode() {
        return _engReadMode;
    }

    /**
     * Set the Engineering Read Mode.
     */
    public void setEngineeringReadMode(EngReadMode newValue) {
        EngReadMode oldValue = getEngineeringReadMode();
        if (oldValue != newValue) {
            _engReadMode = newValue;
            firePropertyChange(ENGINEERING_READMODE_PROP, oldValue, newValue);
        }
    }


    public int getNumberOfSamples() {
        return _numberOfSamples;
    }

    public void setNumberOfSamples(int numberOfSamples) {
        if (_numberOfSamples != numberOfSamples) {
            int oldVal = _numberOfSamples;
            _numberOfSamples = numberOfSamples;
            firePropertyChange(NUMBER_OF_SAMPLES_PROP, oldVal, numberOfSamples);
        }
    }

    public int getPeriod() {
        return _period;
    }

    public void setPeriod(int period) {
        if (_period != period) {
            int oldVal = _period;
            _period = period;
            firePropertyChange(PERIOD_PROP, oldVal, period);
        }
    }

    public int getNumberOfPeriods() {
        return _numberOfPeriods;
    }

    public void setNumberOfPeriods(int numberOfPeriods) {
        if (_numberOfPeriods != numberOfPeriods) {
            int oldVal = _numberOfPeriods;
            _numberOfPeriods = numberOfPeriods;
            firePropertyChange(NUMBER_OF_PERIODS_PROP, oldVal, numberOfPeriods);
        }
    }

    public int getNumberOfResets() {
        return _numberOfResets;
    }

    public void setNumberOfResets(int numberOfResets) {
        if (_numberOfResets != numberOfResets) {
            int oldVal = _numberOfResets;
            _numberOfResets = numberOfResets;
            firePropertyChange(NUMBER_OF_RESETS_PROP, oldVal, numberOfResets);
        }
    }

    /**
     * Return a parameter set describing the current state of this object.
     *
     * @param factory
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, ENGINEERING_READMODE_PROP, _engReadMode.name());
        Pio.addParam(factory, paramSet, NUMBER_OF_SAMPLES_PROP, Integer.toString(_numberOfSamples));
        Pio.addParam(factory, paramSet, PERIOD_PROP, Integer.toString(_period));
        Pio.addParam(factory, paramSet, NUMBER_OF_PERIODS_PROP, Integer.toString(_numberOfPeriods));
        Pio.addParam(factory, paramSet, NUMBER_OF_RESETS_PROP, Integer.toString(_numberOfResets));

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);
        String v;

        v = Pio.getValue(paramSet, ENGINEERING_READMODE_PROP);
        if (v != null) {
            setEngineeringReadMode(EngReadMode.getReadMode(v));
        }
        v = Pio.getValue(paramSet, NUMBER_OF_SAMPLES_PROP);
        if (v != null) {
            setNumberOfSamples(Integer.parseInt(v));
        }
        v = Pio.getValue(paramSet, PERIOD_PROP);
        if (v != null) {
            setPeriod(Integer.parseInt(v));
        }
        v = Pio.getValue(paramSet, NUMBER_OF_PERIODS_PROP);
        if (v != null) {
            setNumberOfPeriods(Integer.parseInt(v));
        }
        v = Pio.getValue(paramSet, NUMBER_OF_RESETS_PROP);
        if (v != null) {
            setNumberOfResets(Integer.parseInt(v));
        }
    }

    /**
     * Return the configuration for this component.
     */
    public ISysConfig getSysConfig() {
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);

        // Fill in the current values.
        sc.putParameter(DefaultParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion()));
        sc.putParameter(DefaultParameter.getInstance(ENGINEERING_READMODE_PROP, getEngineeringReadMode()));
        sc.putParameter(DefaultParameter.getInstance(NUMBER_OF_SAMPLES_PROP, _numberOfSamples));
        sc.putParameter(DefaultParameter.getInstance(PERIOD_PROP, _period));
        sc.putParameter(DefaultParameter.getInstance(NUMBER_OF_PERIODS_PROP, _numberOfPeriods));
        sc.putParameter(DefaultParameter.getInstance(NUMBER_OF_RESETS_PROP, _numberOfResets));

        return sc;
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }
}
