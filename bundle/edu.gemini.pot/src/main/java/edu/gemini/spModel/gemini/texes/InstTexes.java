package edu.gemini.spModel.gemini.texes;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.obscomp.InstConfigInfo;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.pot.sp.ISPObservation;

import java.util.*;
import java.time.Duration;
import java.beans.PropertyDescriptor;

/**
 * The Texes instrument.
 */
public final class InstTexes extends SPInstObsComp implements PropertyProvider {

    // for serialization
    private static final long serialVersionUID = 2L;


    public static final double DEF_POS_ANGLE = 0.0;  // deg
    public static final double DEF_WAVELENGTH = 10.0;  // microns

    /**
     * The name of the Texes instrument configuration
     */
    public static final String INSTRUMENT_NAME_PROP = "Texes";


    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<String, PropertyDescriptor>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    //Properties
    public static final PropertyDescriptor DISPERSER_PROP;
    public static final PropertyDescriptor WAVELENGTH_PROP;

    public static final PropertyDescriptor COADDS_PROP;
    public static final PropertyDescriptor EXPOSURE_TIME_PROP;
    public static final PropertyDescriptor POS_ANGLE_PROP;

    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        PropertyDescriptor pd;
        pd = PropertySupport.init(propName, InstTexes.class, query, iter);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }
    static {
        final boolean query_yes = true;
        final boolean iter_yes  = true;
        final boolean query_no  = false;
        final boolean iter_no   = false;
        DISPERSER_PROP     = initProp("disperser", query_yes, iter_yes);
        WAVELENGTH_PROP    = initProp("wavelength", query_no, iter_no);

        COADDS_PROP        = initProp("coadds",       query_no,  iter_yes);
        EXPOSURE_TIME_PROP = initProp("exposureTime", query_no,  iter_yes);
        POS_ANGLE_PROP     = initProp("posAngle",     query_no,  iter_no);
    }

    /**
     * This obs component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.INSTRUMENT_TEXES;

    public static final ISPNodeInitializer<ISPObsComponent, InstTexes> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new InstTexes(), c -> new InstTexesCB(c));

    private TexesParams.Disperser _disperser = TexesParams.Disperser.DEFAULT;

    private double _centralWavelength = DEF_WAVELENGTH;

    /** Constructor */
    public InstTexes() {
        super(SP_TYPE);
        // Override the default values from superclass SPInstObsComp)
        _positionAngle = DEF_POS_ANGLE;
    }

    /**
     * Implementation of the clone method.
     */

    public Object clone() {
        // No problems cloning here since private variables are immutable
        return (InstTexes) super.clone();
    }

    @Override
    public Set<Site> getSite() {
        return Site.SET_GN;
    }

    public String getPhaseIResourceName() {
        return "gemTEXES";
    }

    /**
     * Return the setup time before observing can begin
     */
    @Override
    public Duration getSetupTime(ISPObservation obs) {
        return Duration.ofMinutes(20);
    }

    /**
     * Return the science area based upon the current mask.
     */
    public double[] getScienceArea() {
        double[] size = new double[2];

        // the length is determined by the disperser in use
        size[1] = getDisperser().getSlitWidth();
        size[0] = TexesParams.Disperser.SLIT_LENGTH;

        return size;
    }

    /**
     * Set the Disperser.
     */
    public void setDisperser(TexesParams.Disperser newValue) {
        TexesParams.Disperser oldValue = getDisperser();
        if (oldValue != newValue) {
            _disperser = newValue;
            firePropertyChange(DISPERSER_PROP.getName(), oldValue, newValue);
        }
    }

    /**
         * Get the disperser.
         */
        public TexesParams.Disperser getDisperser() {
            return _disperser;
        }


    /**
     * Set the grating wavelength.
     */
    public void setWavelength(double newValue) {
        double oldValue = getWavelength();
        if (oldValue != newValue) {
            _centralWavelength = newValue;
            firePropertyChange(WAVELENGTH_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Get the wavelength.
     */
    public double getWavelength() {
        return _centralWavelength;
    }

    /**
     * Return a parameter set describing the current state of this object.
     * @param factory
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, DISPERSER_PROP.getName(), getDisperser().name());
        Pio.addParam(factory, paramSet, WAVELENGTH_PROP.getName(), Double.toString(getWavelength()));

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v;
        v = Pio.getValue(paramSet, DISPERSER_PROP.getName());
        if (v != null) {
           setDisperser(TexesParams.Disperser.valueOf(v));
        }
        v = Pio.getValue(paramSet, WAVELENGTH_PROP.getName());
        setWavelength(Double.parseDouble(v));
    }

    /**
     * Return the configuration for this component.
     */
    public ISysConfig getSysConfig() {
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);

        // Fill in the current values.
        sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion()));
        sc.putParameter(DefaultParameter.getInstance(DISPERSER_PROP.getName(), getDisperser()));
        sc.putParameter(DefaultParameter.getInstance(WAVELENGTH_PROP.getName(), getWavelength()));
        sc.putParameter(DefaultParameter.getInstance(EXPOSURE_TIME_PROP.getName(), getExposureTime()));
        sc.putParameter(DefaultParameter.getInstance(POS_ANGLE_PROP.getName(), getPosAngle()));
        sc.putParameter(DefaultParameter.getInstance(COADDS_PROP.getName(), getCoadds()));

        return sc;
    }

    /**
     * Return a list of InstConfigInfo objects describing the instrument's
     * queryable configuration parameters.
     */
    public static List<InstConfigInfo> getInstConfigInfo() {
        List<InstConfigInfo> configInfo = new LinkedList<>();
        configInfo.add(new InstConfigInfo(DISPERSER_PROP));
        return configInfo;
    }

    /**  Texes doesn't have an OIWFS */
    public boolean hasOIWFS() {
        return false;
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    private static final Angle PWFS1_VIG = Angle.arcmins(4.8);
    private static final Angle PWFS2_VIG = Angle.arcmins(4.0);

    @Override public Angle pwfs1VignettingClearance(ObsContext ctx) { return PWFS1_VIG; }
    @Override public Angle pwfs2VignettingClearance(ObsContext ctx) { return PWFS2_VIG; }

}
