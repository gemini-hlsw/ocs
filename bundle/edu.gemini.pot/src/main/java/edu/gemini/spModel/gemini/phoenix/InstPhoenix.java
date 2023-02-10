package edu.gemini.spModel.gemini.phoenix;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.gemini.phoenix.PhoenixParams.Filter;
import edu.gemini.spModel.gemini.phoenix.PhoenixParams.Mask;
import edu.gemini.spModel.gemini.visitor.VisitorInstrument;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obs.plannedtime.DefaultStepCalculator;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.*;
import edu.gemini.spModel.obscomp.InstConfigInfo;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.seqcomp.SeqConfigNames;

import java.beans.PropertyDescriptor;
import java.time.Duration;
import java.util.*;

/**
 * The Phoenix instrument.
 */
public final class InstPhoenix extends SPInstObsComp implements PropertyProvider, StepCalculator {

    // for serialization
    private static final long serialVersionUID = 3L;

    /**
     * This obs component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.INSTRUMENT_PHOENIX;

    public static final ISPNodeInitializer<ISPObsComponent, InstPhoenix> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new InstPhoenix(), c -> new InstPhoenixCB(c));

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    //Properties
    public static final PropertyDescriptor FILTER_PROP;
    public static final PropertyDescriptor MASK_PROP;
    public static final PropertyDescriptor GRATING_WAVELENGTH_PROP;
    public static final PropertyDescriptor GRATING_WAVENUMBER_PROP;

    public static final PropertyDescriptor EXPOSURE_TIME_PROP;
    public static final PropertyDescriptor POS_ANGLE_PROP;
    public static final PropertyDescriptor COADDS_PROP;

    /**
     * The name of the Phoenix instrument configuration
     */
    public static final String INSTRUMENT_NAME_PROP = "Phoenix";

    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        PropertyDescriptor pd;
        pd = PropertySupport.init(propName, InstPhoenix.class, query, iter);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }


    static {
        boolean query_yes = true;
        boolean query_no = false;
        boolean iter_no = false;
        FILTER_PROP = initProp("filter", query_yes, iter_no);
        MASK_PROP = initProp("mask", query_yes, iter_no);
        GRATING_WAVELENGTH_PROP = initProp("gratingWavelength", query_no, iter_no);
        GRATING_WAVENUMBER_PROP = initProp("gratingWavenumber", query_no, iter_no);

        EXPOSURE_TIME_PROP          = initProp("exposureTime", query_no, iter_no);
        POS_ANGLE_PROP              = initProp("posAngle", query_no, iter_no);
        COADDS_PROP                 = initProp("coadds", query_no, iter_no);
    }


    //Default values
    public static final double DEF_EXPOSURE_TIME = 900.0; // seconds
    public static final double DEF_POS_ANGLE = 90.0;  // deg
    public static final double DEF_GRATING_WAVELENGTH = 2.3;
    public static final double DEF_GRATING_WAVENUMBER = 4346.65;
    public static final int DEF_COADDS = 1;

    public static final double CONVERSION_CONSTANT = 9997.3;

    /**
     * Time in seconds required for readout.
     */
    public static final double READOUT_OVERHEAD = 18; // secs

    private Filter _filter = Filter.DEFAULT;
    private Mask _mask = Mask.DEFAULT;
    private double _gratingWavelength = DEF_GRATING_WAVELENGTH;
    private double _gratingWavenumber = DEF_GRATING_WAVENUMBER;

    private final String _VERSION =  "2006B-1";

    /** Constructor */
    public InstPhoenix() {
        super(SP_TYPE);
        // Override the default exposure time (see superclass SPInstObsComp)
        _exposureTime = DEF_EXPOSURE_TIME;
        _coadds = DEF_COADDS;
        _positionAngle = DEF_POS_ANGLE;
        setVersion(_VERSION);
    }

    /**
     * Implementation of the clone method.
     */

    public Object clone() {
        // No problems cloning here since private variables are immutable
        return super.clone();
    }

    @Override
    public Set<Site> getSite() {
        return Site.SET_GS;
    }

    public String getPhaseIResourceName() {
        return "gemPhoenix";
    }

    /**
     * Return the setup time in seconds before observing can begin
     */
    @Override
    public Duration getSetupTime(ISPObservation obs) {
        //SCI-0107: change to 20 minutes
        return Duration.ofMinutes(20);
    }

    @Override public CategorizedTimeGroup calc(Config cur, Option<Config> prev) {
        CategorizedTime readout = CategorizedTime.fromSeconds(Category.READOUT, READOUT_OVERHEAD);
        return DefaultStepCalculator.instance.calc(cur, prev).add(readout);
    }

    /**
     * Return the science area based upon the current mask.
     */
    public double[] getScienceArea() {
        double[] size = new double[2];

        // the width is determined by the mask in use
        size[0] = getMask().getSlitWidth();
        size[1] = Mask.SLIT_LENGTH;

        return size;
    }

    /**
     * Get the filter.
     */
    public Filter getFilter() {
        return _filter;
    }

    /**
     * Set the filter.
     */
    public void setFilter(Filter newValue) {
        Filter oldValue = getFilter();
        if (oldValue != newValue) {
            _filter = newValue;
            firePropertyChange(FILTER_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the filter. This method is needed as public by the Phoenix
     * editor which currently sets things via Strings.
     */
    public void setFilter(String name) {
        Filter oldValue = getFilter();
        setFilter(Filter.getFilter(name, oldValue));
    }

    /**
     * Set the mask.
     */
    public void setMask(Mask newValue) {
        Mask oldValue = getMask();
        if (oldValue != newValue) {
            _mask = newValue;
            firePropertyChange(MASK_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the mask.
     */
    private void _setMask(String name) {
        Mask oldValue = getMask();
        setMask(Mask.getMask(name, oldValue));
    }

    /**
     * Get the mask.
     */
    public Mask getMask() {
        return _mask;
    }

    /**
     * Set the grating wavelength.
     */
    public void setGratingWavelength(double newValue) {
        double oldValue = getGratingWavelength();
        if (oldValue != newValue) {
            _gratingWavelength = newValue;
            firePropertyChange(GRATING_WAVELENGTH_PROP, oldValue, newValue);
        }
    }

    /**
     * Get the grating wavelength.
     */
    public double getGratingWavelength() {
        return _gratingWavelength;
    }

    /**
     * Get the grating wavelength as a string.
     */
    public String getGratingWavelengthAsString() {
        return Double.toString(getGratingWavelength());
    }

    /**
     * Set the grating wavenumber.
     */
    public void setGratingWavenumber(double newValue) {
        double oldValue = getGratingWavenumber();
        if (oldValue != newValue) {
            _gratingWavenumber = newValue;
            firePropertyChange(GRATING_WAVENUMBER_PROP, oldValue, newValue);
        }
    }

    /**
     * Get the grating wavenumber.
     */
    public double getGratingWavenumber() {
        return _gratingWavenumber;
    }

    /**
     * Return a parameter set describing the current state of this object.
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, FILTER_PROP, _filter.name());
        Pio.addParam(factory, paramSet, MASK_PROP, _mask.name());
        Pio.addParam(factory, paramSet, GRATING_WAVELENGTH_PROP, Double.toString(getGratingWavelength()));
        Pio.addParam(factory, paramSet, GRATING_WAVENUMBER_PROP, Double.toString(getGratingWavenumber()));

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v;
        v = Pio.getValue(paramSet, MASK_PROP);
        if (v != null) {
            _setMask(v);
        }
        v = Pio.getValue(paramSet, FILTER_PROP);
        if (v != null) {
            setFilter(v);
        }
        v = Pio.getValue(paramSet, GRATING_WAVELENGTH_PROP);
        if (v != null) {
            setGratingWavelength(Double.parseDouble(v));
        }
        v = Pio.getValue(paramSet, GRATING_WAVENUMBER_PROP);
        if (v != null) {
            setGratingWavenumber(Double.parseDouble(v));
        }
    }

    /**
     * Return the configuration for this component.
     */
    public ISysConfig getSysConfig() {
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);

        // Fill in the current values.
        sc.putParameter(DefaultParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion()));
        sc.putParameter(DefaultParameter.getInstance(FILTER_PROP, getFilter()));
        sc.putParameter(DefaultParameter.getInstance(MASK_PROP, getMask()));
        sc.putParameter(DefaultParameter.getInstance(GRATING_WAVELENGTH_PROP, getGratingWavelength()));
        sc.putParameter(DefaultParameter.getInstance(GRATING_WAVENUMBER_PROP, getGratingWavenumber()));
        sc.putParameter(DefaultParameter.getInstance(EXPOSURE_TIME_PROP, getExposureTime()));
        sc.putParameter(DefaultParameter.getInstance(POS_ANGLE_PROP, getPosAngle()));
        sc.putParameter(DefaultParameter.getInstance(COADDS_PROP, getCoadds()));

        return sc;
    }

    /**
     * Return a list of InstConfigInfo objects describing the instrument's
     * queryable configuration parameters.
     */
    public static List<InstConfigInfo> getInstConfigInfo() {
        List<InstConfigInfo> configInfo = new LinkedList<>();
        configInfo.add(new InstConfigInfo(MASK_PROP));
        configInfo.add(new InstConfigInfo(FILTER_PROP));
        return configInfo;
    }

    /**  Phoenix doesn't have an OIWFS */
    public boolean hasOIWFS() {
        return false;
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    // REL-2346 Use the same Vignetting clearance as Visitors
    @Override public Angle pwfs1VignettingClearance(ObsContext ctx) { return VisitorInstrument.PWFS1_VIG; }
    @Override public Angle pwfs2VignettingClearance(ObsContext ctx) { return VisitorInstrument.PWFS2_VIG; }

}
