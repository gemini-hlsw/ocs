package edu.gemini.spModel.gemini.visitor;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeProvider;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.obs.plannedtime.CommonStepCalculator;
import edu.gemini.spModel.obs.plannedtime.ExposureCalculator;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obscomp.InstConfigInfo;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

import java.beans.PropertyDescriptor;
import java.time.Duration;
import java.util.*;

import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_CONFIG_NAME;

public class VisitorInstrument extends SPInstObsComp
        implements PropertyProvider, GuideProbeProvider, PlannedTime.StepCalculator {
    // for serialization
    private static final long serialVersionUID = 3L;

    /**
     * This obs component's SP type.
     */
    public static final SPComponentType SP_TYPE = SPComponentType.INSTRUMENT_VISITOR;

    public static final ISPNodeInitializer<ISPObsComponent, VisitorInstrument> NI =
        new ComponentNodeInitializer<>(SP_TYPE, VisitorInstrument::new, VisitorInstrumentCB::new);

    //Properties
    public static final PropertyDescriptor NAME_PROP;
    public static final PropertyDescriptor CONFIG_PROP;
    public static final PropertyDescriptor WAVELENGTH_PROP;
    public static final PropertyDescriptor EXPOSURE_TIME_PROP;
    public static final PropertyDescriptor POS_ANGLE_PROP;

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    //Defaults
    public static final double DEFAULT_EXPOSURE_TIME  = VisitorConfig$.MODULE$.DefaultExposureTime().toMillis() / 1000.0;
    public static final double DEFAULT_POSITION_ANGLE = VisitorConfig$.MODULE$.DefaultPositionAngle().toDegrees();
    public static final String INSTRUMENT_NAME_PROP   = SP_TYPE.readableStr;
    public static final double DEF_WAVELENGTH         = VisitorConfig$.MODULE$.DefaultWavelength().toMicrons();

    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        PropertyDescriptor pd = PropertySupport.init(propName, VisitorInstrument.class, query, iter);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }

    // Instrument properties
    private String name = "";
    private double _centralWavelength = DEF_WAVELENGTH;
    private Option<VisitorConfig> visitorConfig = ImOption.empty();

    static {
        final boolean query_yes = true;
        final boolean iter_no = false;
        final boolean query_no = false;

        NAME_PROP          = initProp("name", query_yes, iter_no);
        CONFIG_PROP        = initProp("config", query_no, iter_no);
        WAVELENGTH_PROP    = initProp("wavelength", query_no, iter_no);
        EXPOSURE_TIME_PROP = initProp("exposureTime", query_no, iter_no);
        POS_ANGLE_PROP     = initProp("posAngle", query_no, iter_no);
    }

    public VisitorInstrument() {
        super(SP_TYPE);
        _exposureTime  = DEFAULT_EXPOSURE_TIME;
        _positionAngle = DEFAULT_POSITION_ANGLE;
    }

    /**
     * Implementation of the clone method.
     */
    public Object clone() {
        // No problems cloning here since private variables are immutable
        return super.clone();
    }

    public String getName() {
        return name;
    }

    public void setName(String newValue) {
        String oldValue = getName();
        if (!oldValue.equals(newValue)) {
            name = newValue;
            firePropertyChange(NAME_PROP.getName(), oldValue, newValue);
            // Update the title
            this.setTitle(String.format("%s: %s", SP_TYPE.readableStr, name));
        }
    }

    public Option<VisitorConfig> getVisitorConfig() {
        return visitorConfig;
    }

    public void setVisitorConfig(Option<VisitorConfig> newValue) {
        final Option<VisitorConfig> oldValue = getVisitorConfig();
        if (!oldValue.equals(newValue)) {
            visitorConfig = newValue;
            firePropertyChange(CONFIG_PROP.getName(), oldValue, newValue);
        }
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

    @Override
    public Duration getSetupTime(ISPObservation obs) {
        return getVisitorConfig()
                .map(VisitorConfig::setupTime)
                .getOrElse(VisitorConfig$.MODULE$.DefaultSetupTime());
    }


    @Override
    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    @Override
    public Set<Site> getSite() {
        return Site.SET_BOTH;
    }

    @Override
    public String getPhaseIResourceName() {
        return "gemVisitor";
    }

    @Override
    public PlannedTime.CategorizedTimeGroup calc(Config stepConfig, Option<Config> prevStepConfig) {
        final PlannedTime.CategorizedTime exposureTime = PlannedTime.CategorizedTime.fromSeconds(PlannedTime.Category.EXPOSURE,
                ExposureCalculator.instance.exposureTimeSec(stepConfig));

        final Duration readoutDuration =
            getVisitorConfig()
                .map(VisitorConfig::readoutTime)
                .getOrElse(VisitorConfig$.MODULE$.DefaultReadoutTime());

        final PlannedTime.CategorizedTime readoutTime =
            PlannedTime.CategorizedTime.apply(PlannedTime.Category.READOUT, readoutDuration.toMillis());

        return CommonStepCalculator
                    .instance
                    .calc(stepConfig, prevStepConfig)
                    .add(exposureTime)
                    .add(readoutTime);
    }

    /**
     * Return the configuration for this component.
     */
    public ISysConfig getSysConfig() {
        ISysConfig sc = new DefaultSysConfig(INSTRUMENT_CONFIG_NAME);

        // Fill in the current values.
        sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion()));
        sc.putParameter(DefaultParameter.getInstance(POS_ANGLE_PROP, getPosAngle()));
        sc.putParameter(DefaultParameter.getInstance(InstConstants.EXPOSURE_TIME_PROP, getExposureTime()));
        sc.putParameter(DefaultParameter.getInstance(WAVELENGTH_PROP.getName(), getWavelength()));

        sc.putParameter(DefaultParameter.getInstance(NAME_PROP, getName()));

        return sc;
    }

    /**
     * Return a list of InstConfigInfo objects describing the instrument's
     * queryable configuration parameters.
     */
    public static List<InstConfigInfo> getInstConfigInfo() {
        return new LinkedList<>();
    }

    /**
     * Return a parameter set describing the current state of this object.
     */
    @Override
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, NAME_PROP, getName());
        Pio.addParam(factory, paramSet, WAVELENGTH_PROP.getName(), Double.toString(getWavelength()));
        visitorConfig.foreach(c -> Pio.addParam(factory, paramSet, CONFIG_PROP.getName(), c.name()));

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    @Override
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v = Pio.getValue(paramSet, NAME_PROP);
        if (v != null) {
            setName(v);
        }
        v = Pio.getValue(paramSet, WAVELENGTH_PROP.getName());
        setWavelength(Double.parseDouble(v));

        setVisitorConfig(
            ImOption.apply(Pio.getValue(paramSet, CONFIG_PROP.getName()))
                    .flatMap(VisitorConfig$.MODULE$::findByNameJava)
        );
    }

    private static final Collection<GuideProbe> GUIDE_PROBES = GuideProbeUtil.instance.createCollection();

    @Override
    public Collection<GuideProbe> getGuideProbes() {
        return GUIDE_PROBES;
    }

    @Override
    public boolean hasOIWFS() {
        return false;
    }

    public String getWavelengthStr() {
        return Double.toString(_centralWavelength);
    }

    public static final Angle PWFS1_VIG = Angle.arcmins(5.7);
    public static final Angle PWFS2_VIG = Angle.arcmins(5.2);

    @Override public Angle pwfs1VignettingClearance() { return PWFS1_VIG; }
    @Override public Angle pwfs2VignettingClearance() { return PWFS2_VIG; }

}
