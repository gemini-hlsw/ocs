package edu.gemini.spModel.gemini.altair;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.ao.AOConstants;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.altair.AltairParams.*;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.util.SPTreeUtil;

import java.beans.PropertyDescriptor;

import java.util.*;

/**
 * The Altair instrument.
 */
public final class InstAltair extends AbstractDataObject implements PropertyProvider, GuideProbeProvider, GuideProbeConsumer, GuideProbeAvailabilityVolatileDataObject {

    public static InstAltair lookupAltair(ISPObservation obs)  {
        ISPObsComponent aoComponent = SPTreeUtil.findObsComponentByNarrowType(obs, SP_TYPE.narrowType);
        if (aoComponent == null) return null;
        return (InstAltair) aoComponent.getDataObject();
    }

    // for serialization
    private static final long serialVersionUID = 2L;


    // Properties
    public static final PropertyDescriptor WAVELENGTH_PROP;
    public static final PropertyDescriptor ADC_PROP;
    public static final PropertyDescriptor CASS_ROTATOR_PROP;
    public static final PropertyDescriptor ND_FILTER_PROP;
    public static final PropertyDescriptor MODE_PROP;


    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        PropertyDescriptor pd;
        pd = PropertySupport.init(propName, InstAltair.class, query, iter);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }

    static {
        final boolean query_yes = true;
        final boolean iter_no   = false;

        WAVELENGTH_PROP = initProp("wavelength", query_yes, iter_no);
        ADC_PROP = initProp("adc", query_yes, iter_no);
        CASS_ROTATOR_PROP  = initProp("cassRotator", query_yes, iter_no);
        ND_FILTER_PROP = initProp("ndFilter", query_yes, iter_no);
        MODE_PROP = initProp(AltairConstants.MODE_PROP, query_yes, iter_no);
    }

    private static final String VERSION = "2016B-1";
    private Wavelength _wavelength = Wavelength.DEFAULT;
    private ADC _adc = ADC.DEFAULT;
    private CassRotator _cassRotator = CassRotator.DEFAULT;
    private NdFilter _ndFilter = NdFilter.DEFAULT;
    private Mode _mode = Mode.DEFAULT;

    /**
     * This obs component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.AO_ALTAIR;

    public static final ISPNodeInitializer<ISPObsComponent, InstAltair> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new InstAltair(), c -> new InstAltairCB(c));


    /** Constructor */
    public InstAltair() {
        super(SP_TYPE);
        setVersion(VERSION);
    }

    /**
     * Implementation of the clone method.
     */
    public Object clone() {
        // No problems cloning here since private variables are immutable
        return  super.clone();
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    /**
     * Return the setup time in seconds before observing can begin
     */
    public double getSetupTime() {
        //SCT: 219 If LGS is in use, the setup is 10 minutes more than when the NGS is in use.
        //The setup overhead is instrument specific, so we just return the extra overhead for
        //using LGS here. Instrument code should call this method to take into account the cost
        //of using Altair
        if (getGuideStarType() == GuideStarType.LGS) {
            return 10.0 * 60;
        }
        return 0.0;
    }


    /**
     * Set Atmospheric Dispersion Compensator to ON/OFF.
     */
    public void setAdc(ADC newValue) {
        ADC oldValue = getAdc();
        if (oldValue != newValue) {
            _adc = newValue;
            firePropertyChange(AltairConstants.ADC_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the Atmospheric Dispersion Compensator with a String.
     */
    private void _setADC(String name) {
        ADC oldValue = getAdc();
        setAdc(ADC.getAdc(name, oldValue));
    }

    /**
     * Get the Atmospheric Dispersion Compensator.
     */
    public ADC getAdc() {
        return _adc;
    }


    /**
     * Set Cass Rotator to Fixed or Following
     */
    public void setCassRotator(CassRotator newValue) {
        CassRotator oldValue = getCassRotator();
        if (oldValue != newValue) {
            _cassRotator = newValue;
            firePropertyChange(AltairConstants.CASS_ROTATOR_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the Cass Rotator with a String.
     */
    private void _setCassRotator(String name) {
        CassRotator oldValue = getCassRotator();
        setCassRotator(CassRotator.getCassRotator(name, oldValue));
    }

    /**
     * Get the Cass Rotator.
     */
    public CassRotator getCassRotator() {
        if (_cassRotator == null) {
            _cassRotator = CassRotator.DEFAULT; // backward compat...
        }
        return _cassRotator;
    }

    /**
     * Set NdFilter to On or Off
     */
    public void setNdFilter(NdFilter newValue) {
        NdFilter oldValue = getNdFilter();
        if (oldValue != newValue) {
            _ndFilter = newValue;
            firePropertyChange(AltairConstants.ND_FILTER_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the NdFilter with a String.
     */
    private void _setNdFilter(String name) {
        NdFilter oldValue = getNdFilter();
        setNdFilter(NdFilter.getNdFilter(name, oldValue));
    }

    /**
     * Get the NdFilter.
     */
    public NdFilter getNdFilter() {
        if (_ndFilter == null) {
            _ndFilter = NdFilter.DEFAULT; // backward compat...
        }
        return _ndFilter;
    }


    /**
     * Gets the GuideStarType which is an attribute of the selected mode.
     */
    public GuideStarType getGuideStarType() {
        return _mode.guideStarType();
    }

    /**
     * Gets the FieldLens which is an attribute of the selected mode.
     */
    public FieldLens getFieldLens() {
        return _mode.fieldLens();
    }

    public void setMode(Mode newValue) {
        Mode oldValue = getMode();
        if (oldValue != newValue) {
            _mode = newValue;
            firePropertyChange(AltairConstants.MODE_PROP, oldValue, newValue);
        }
    }

    public Mode getMode() {
        return _mode;
    }

    /**
     * Sets the mode with a String.
     */
    private void _setMode(String name) {
        Mode oldValue = getMode();
        setMode(Mode.getMode(name, oldValue));
    }


    /**
     * Set the wavelength.
     */
    public void setWavelength(Wavelength newValue) {
        Wavelength oldValue = getWavelength();
        if (oldValue != newValue) {
            _wavelength = newValue;
            firePropertyChange(AltairConstants.WAVELENGTH_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the wavelength using a string.
     */
    private void _setWavelength(String name) {
        Wavelength oldValue = getWavelength();
        setWavelength(Wavelength.getWavelength(name, oldValue));
    }

    /**
     * Get the wavelength.
     */
    public Wavelength getWavelength() {
        return _wavelength;
    }

    /**
     * Return a parameter set describing the current state of this object.
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, AltairConstants.WAVELENGTH_PROP, _wavelength.name());
        Pio.addParam(factory, paramSet, AltairConstants.ADC_PROP, _adc.name());
        Pio.addParam(factory, paramSet, AltairConstants.CASS_ROTATOR_PROP, _cassRotator.name());
        Pio.addParam(factory, paramSet, AltairConstants.ND_FILTER_PROP, _ndFilter.name());
        Pio.addParam(factory, paramSet, AltairConstants.MODE_PROP, _mode.name());
        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v = Pio.getValue(paramSet, AltairConstants.WAVELENGTH_PROP);
        if (v != null) {
            _setWavelength(v);
        }

        v = Pio.getValue(paramSet, AltairConstants.ADC_PROP);
        if (v != null) {
            _setADC(v);
        }
        v = Pio.getValue(paramSet, AltairConstants.CASS_ROTATOR_PROP);
        if (v != null) {
            _setCassRotator(v);
        }
        v = Pio.getValue(paramSet, AltairConstants.ND_FILTER_PROP);
        if (v != null) {
            _setNdFilter(v);
        }
        String mode = Pio.getValue(paramSet, AltairConstants.MODE_PROP);
        if (mode != null) {
            _setMode(mode);
        }

        // UX-1423 backwards compatibilty with old versions that had field lens and guide star type as
        // separate paramters instead of the mode. This code can be removed once all data is migrated.
        if (mode == null) {
            v = Pio.getValue(paramSet, AltairConstants.GUIDESTAR_TYPE_PROP);
            GuideStarType guideStarType = GuideStarType.getGuideStarType(v, GuideStarType.DEFAULT);
            if (guideStarType.equals(GuideStarType.LGS)) {
                _setMode(Mode.LGS.name());
            } else {
                v = Pio.getValue(paramSet, AltairConstants.FIELD_LENSE_PROP);
                FieldLens fieldLens = FieldLens.getFieldLens(v, FieldLens.DEFAULT);
                if (fieldLens.equals(FieldLens.OUT)) {
                    _setMode(Mode.NGS.name());
                } else {
                    _setMode(Mode.NGS_FL.name());
                }
            }
        }
    }

    /**
     * Return the configuration for this component.
     */
    public ISysConfig getSysConfig() {
        ISysConfig sc = new DefaultSysConfig(AOConstants.AO_CONFIG_NAME);

        // Fill in the current values.
        sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion()));
        sc.putParameter(DefaultParameter.getInstance(AltairConstants.WAVELENGTH_PROP, getWavelength()));
        sc.putParameter(DefaultParameter.getInstance(AltairConstants.ADC_PROP, getAdc()));
        sc.putParameter(DefaultParameter.getInstance(AltairConstants.CASS_ROTATOR_PROP, getCassRotator()));
        sc.putParameter(DefaultParameter.getInstance(AltairConstants.ND_FILTER_PROP, getNdFilter()));
        // note: the values for field lens and guide star type are provided by the mode
        sc.putParameter(DefaultParameter.getInstance(AltairConstants.FIELD_LENSE_PROP, getFieldLens()));
        sc.putParameter(DefaultParameter.getInstance(AltairConstants.GUIDESTAR_TYPE_PROP, getGuideStarType()));
        return sc;
    }

    private static Collection<GuideProbe> GUIDE_PROBES    =
            GuideProbeUtil.instance.createCollection(AltairAowfsGuider.instance);
    private static Collection<GuideProbe> NO_GUIDE_PROBES =
            Collections.emptyList();

    public Collection<GuideProbe> getGuideProbes() {
        switch (getMode()) {
            case LGS_OI:
            case LGS_P1: return NO_GUIDE_PROBES;
            default:     return GUIDE_PROBES;
        }
    }

    private static final Collection<GuideProbe> ANTI_GUIDERS    =
            GuideProbeUtil.instance.createCollection(PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2);

    private static Set<GuideProbe> allMinus(final ImList<GuideProbe> guideProbes) {
        final Set<GuideProbe> guiders = new TreeSet<>(GuideProbe.KeyComparator.instance);
        guiders.addAll(GuideProbeMap.instance.values());
        guideProbes.foreach(guiders::remove);
        return guiders;
    }

    public Collection<GuideProbe> getConsumedGuideProbes() {
        if (getGuideStarType() == GuideStarType.LGS) {
            return allMinus(getMode().guiders());
        } else {
            return ANTI_GUIDERS;
        }
    }
}
