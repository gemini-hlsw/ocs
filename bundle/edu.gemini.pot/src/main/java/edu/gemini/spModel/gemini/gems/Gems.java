package edu.gemini.spModel.gemini.gems;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.ao.AOConstants;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeConsumer;
import edu.gemini.spModel.guide.GuideProbeProvider;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.SequenceableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * The data object for the Gems observation component.
 */
public final class Gems extends AbstractDataObject implements PropertyProvider, GuideProbeProvider, GuideProbeConsumer {
    private static final Logger LOG = Logger.getLogger(Gems.class.getName());

    public static enum Adc implements DisplayableSpType, SequenceableSpType {
        ON("On"),
        OFF("Off"),
        ;

        public static Adc DEFAULT = OFF;

        private String displayValue;

        private Adc(String displayValue) {
            this.displayValue = displayValue;
        }

        public String displayValue() {
            return this.displayValue;
        }

        public String sequenceValue() {
            return name();
        }

        public static Adc valueOf(String name, Adc value) {
            Adc res = SpTypeUtil.noExceptionValueOf(Adc.class, name);
            return res == null ? value : res;
        }
    }

    public static enum AstrometricMode implements DisplayableSpType, SequenceableSpType {
        OFF("Off"),
        REGULAR("Regular"),
        GOOD("Good"),
        BEST("Best"),
        ;

        public static AstrometricMode DEFAULT = REGULAR;

        private String displayValue;

        private AstrometricMode(String displayValue) {
            this.displayValue  = displayValue;
        }

        public String displayValue() {
            return displayValue;
        }

        public String sequenceValue() {
            return name();
        }

        public static AstrometricMode valueOf(String name, AstrometricMode value) {
            AstrometricMode res;
            res = SpTypeUtil.noExceptionValueOf(AstrometricMode.class, name);
            return res == null ? value : res;
        }
    }

    public static enum DichroicBeamsplitter implements DisplayableSpType, SequenceableSpType {
        WAVELENGTH_850NM("0.85 um"),
        WAVELENGTH_1UM("1 um"),
        ;

        public static DichroicBeamsplitter DEFAULT = WAVELENGTH_850NM;

        private String displayValue;

        private DichroicBeamsplitter(String displayValue) {
            this.displayValue  = displayValue;
        }

        public String displayValue() {
            return displayValue;
        }

        public String sequenceValue() {
            return name();
        }

        public static DichroicBeamsplitter valueOf(String name, DichroicBeamsplitter value) {
            DichroicBeamsplitter res;
            res = SpTypeUtil.noExceptionValueOf(DichroicBeamsplitter.class, name);
            return res == null ? value : res;
        }
    }

    private static final long serialVersionUID = 1L;

    public static final String SYSTEM_NAME = "GeMS";
    private static final String VERSION = "2009A-1";

    public static final SPComponentType SP_TYPE = SPComponentType.AO_GEMS;

    public static final PropertyDescriptor ADC_PROP;
    public static final PropertyDescriptor DICHROIC_BEAMSPLITTER_PROP;
    public static final PropertyDescriptor ASTROMETRIC_MODE_PROP;

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<String, PropertyDescriptor>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    private static PropertyDescriptor initProp(String propName) {
        PropertyDescriptor pd;
        pd = PropertySupport.init(propName, Gems.class, true, false);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }

    static {
        ADC_PROP                   = initProp("adc");
        DICHROIC_BEAMSPLITTER_PROP = initProp("dichroicBeamsplitter");
        ASTROMETRIC_MODE_PROP      = initProp("astrometricMode");
    }

    private Adc adc = Adc.DEFAULT;
    private DichroicBeamsplitter dichroicBeamsplitter = DichroicBeamsplitter.DEFAULT;
    private AstrometricMode astrometricMode = AstrometricMode.DEFAULT;

    public Gems() {
        super(SP_TYPE);
        setVersion(VERSION);
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    public Adc getAdc() {
        return adc;
    }

    public void setAdc(Adc adc) {
        Adc oldValue = getAdc();
        if (oldValue != adc) {
            this.adc = adc;
            firePropertyChange(ADC_PROP, oldValue, adc);
        }
    }

    public DichroicBeamsplitter getDichroicBeamsplitter() {
        return dichroicBeamsplitter;
    }

    public void setDichroicBeamsplitter(DichroicBeamsplitter beamsplitter) {
        DichroicBeamsplitter oldValue = getDichroicBeamsplitter();
        if (oldValue != beamsplitter) {
            this.dichroicBeamsplitter = beamsplitter;
            firePropertyChange(DICHROIC_BEAMSPLITTER_PROP, oldValue, beamsplitter);
        }
    }

    public AstrometricMode getAstrometricMode() {
        return astrometricMode;
    }

    public void setAstrometricMode(AstrometricMode mode) {
        AstrometricMode oldValue = getAstrometricMode();
        if (oldValue != mode) {
            this.astrometricMode = mode;
            firePropertyChange(ASTROMETRIC_MODE_PROP, oldValue, mode);
        }
    }

    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, ADC_PROP.getName(), adc.name());
        Pio.addParam(factory, paramSet, DICHROIC_BEAMSPLITTER_PROP.getName(), dichroicBeamsplitter.name());
        Pio.addParam(factory, paramSet, ASTROMETRIC_MODE_PROP.getName(), astrometricMode.name());

        return paramSet;
    }

    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v = Pio.getValue(paramSet, ADC_PROP.getName());
        if (v != null) {
            setAdc(Adc.valueOf(v, getAdc()));
        }

        v = Pio.getValue(paramSet, DICHROIC_BEAMSPLITTER_PROP.getName());
        if (v != null) {
            setDichroicBeamsplitter(DichroicBeamsplitter.valueOf(v, getDichroicBeamsplitter()));
        }

        v = Pio.getValue(paramSet, ASTROMETRIC_MODE_PROP.getName());
        if (v != null) {
            setAstrometricMode(AstrometricMode.valueOf(v, getAstrometricMode()));
        }
    }

    public ISysConfig getSysConfig() {
        ISysConfig sc = new DefaultSysConfig(AOConstants.AO_CONFIG_NAME);

        sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion()));
        sc.putParameter(DefaultParameter.getInstance(ADC_PROP, getAdc()));
        sc.putParameter(DefaultParameter.getInstance(DICHROIC_BEAMSPLITTER_PROP, getDichroicBeamsplitter()));
        sc.putParameter(DefaultParameter.getInstance(ASTROMETRIC_MODE_PROP, getAstrometricMode()));

        return sc;
    }

    private static final Collection<GuideProbe> GUIDERS = GuideProbeUtil.instance.createCollection(CanopusWfs.values());

    public Collection<GuideProbe> getGuideProbes() {
        return GUIDERS;
    }

    private static final Collection<GuideProbe> ANTI_GUIDERS = GuideProbeUtil.instance.createCollection(PwfsGuideProbe.pwfs2);

    public Collection<GuideProbe> getConsumedGuideProbes() {
        return ANTI_GUIDERS;
    }

}
