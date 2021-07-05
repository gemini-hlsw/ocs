package edu.gemini.spModel.gemini.nifs;

import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.ao.AOConstants;
import edu.gemini.spModel.config.ConfigPostProcessor;
import edu.gemini.spModel.config.injector.ConfigInjector;
import edu.gemini.spModel.config.injector.obswavelength.ObsWavelengthCalc3;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.AltairParams.GuideStarType;
import edu.gemini.spModel.gemini.calunit.calibration.CalConfigBuilderUtil;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKeyProvider;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.CalibrationKeyImpl;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyNifs;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.gemini.nifs.NIFSParams.*;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeProvider;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.obs.plannedtime.CommonStepCalculator;
import edu.gemini.spModel.obs.plannedtime.ExposureCalculator;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTimeGroup;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.Category;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.StepCalculator;
import edu.gemini.spModel.obscomp.InstConfigInfo;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.obscomp.ItcOverheadProvider;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.seqcomp.SeqConfigNames;

import java.beans.PropertyDescriptor;
import java.time.Duration;
import java.util.*;

/**
 * The NIFS instrument.
 */
public final class InstNIFS extends SPInstObsComp implements PropertyProvider, GuideProbeProvider, StepCalculator, CalibrationKeyProvider, ConfigPostProcessor, ItcOverheadProvider {

    // for serialization
    private static final long serialVersionUID = 4L;

    public static final double ALTAIR_P_OFFSET =  2.51;
    public static final double ALTAIR_Q_OFFSET = -1.86;


    /**
     * This obs component's SP type.
     */

    public static final SPComponentType SP_TYPE =
            SPComponentType.INSTRUMENT_NIFS;

    public static final ISPNodeInitializer<ISPObsComponent, InstNIFS> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new InstNIFS(), c -> new InstNIFSCB(c));

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    //Properties

    public static final PropertyDescriptor MASK_PROP;
    public static final PropertyDescriptor DISPERSER_PROP;
    public static final PropertyDescriptor FILTER_PROP;
    public static final PropertyDescriptor READMODE_PROP;
    public static final PropertyDescriptor CENTRAL_WAVELENGTH_PROP;
    public static final PropertyDescriptor IMAGING_MIRROR_PROP;
    public static final PropertyDescriptor MASK_OFFSET_PROP;


    public static final PropertyDescriptor COADDS_PROP;
    public static final PropertyDescriptor EXPOSURE_TIME_PROP;
    public static final PropertyDescriptor POS_ANGLE_PROP;


    /**
     * The name of the NIFS instrument configuration
     */
    public static final String INSTRUMENT_NAME_PROP = "NIFS";

    /**
     * Default exposure time in seconds
     */
    public static final double DEF_EXPOSURE_TIME = 80.0;

    /**
     * Default central wavelength in microns
     */
    public static final double DEF_CENTRAL_WAVELENGTH = 2.2;

    /**
     * Constant number of seconds added to the NIFS exposure overhead time per
     * COADD per observe.
     */
    public static final double COADD_CONSTANT             = 6.0; // sec


    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
         PropertyDescriptor pd;
         pd = PropertySupport.init(propName, InstNIFS.class, query, iter);
         PRIVATE_PROP_MAP.put(pd.getName(), pd);
         return pd;
     }

    static {
        final boolean query_yes = true;
        final boolean iter_yes = true;
        final boolean query_no = false;

        MASK_PROP = initProp("mask", query_yes, iter_yes);
        DISPERSER_PROP = initProp("disperser", query_yes, iter_yes);
        FILTER_PROP = initProp("filter", query_yes, iter_yes);
        READMODE_PROP = initProp("readMode", query_yes, iter_yes);
        CENTRAL_WAVELENGTH_PROP = initProp("centralWavelength", query_no, iter_yes);
        IMAGING_MIRROR_PROP = initProp("imagingMirror", query_yes, iter_yes);
        MASK_OFFSET_PROP = initProp("maskOffset", query_no, iter_yes);


        COADDS_PROP = initProp("coadds", query_no, iter_yes);
        EXPOSURE_TIME_PROP = initProp("exposureTime", query_no, iter_yes);
        POS_ANGLE_PROP = initProp("posAngle", query_no, iter_yes);
    }


    private ImagingMirror _imagingMirror = ImagingMirror.DEFAULT;
    private Disperser _disperser = Disperser.DEFAULT;
    private Mask _mask = Mask.DEFAULT;
    private Filter _filter = Filter.DEFAULT;
    private ReadMode _readMode = ReadMode.DEFAULT;
    private double _centralWavelength = _disperser.getWavelength();
    private double _maskOffset = 0.;


    public InstNIFS() {
        super(SP_TYPE);
        // Override the default exposure time
        _exposureTime = _readMode.getMinExp();
        setVersion("2010A-1");
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
        return Site.SET_GN;
    }

    public String getPhaseIResourceName() {
        return "gemNIFS";
    }

    @Override public CategorizedTimeGroup calc(Config cur, Option<Config> prev) {
        final int coadds = ExposureCalculator.instance.coadds(cur);
        final double exp = ExposureCalculator.instance.exposureTimeSec(cur);
        final ReadMode readMode = (ReadMode) cur.getItemValue(ReadMode.KEY);

        final CategorizedTime expTime = CategorizedTime.fromSeconds(Category.EXPOSURE, coadds*exp);
        final double readoutTime      = (readMode.getMinExp() + COADD_CONSTANT) * coadds;
        final CategorizedTime readOut = CategorizedTime.fromSeconds(Category.READOUT, readoutTime);

        return CommonStepCalculator.instance.calc(cur, prev).addAll(expTime, readOut, getDhsWriteTime());
    }

    /**
     * Return the setup time in seconds before observing can begin
     */
    @Override
    public Duration getSetupTime(ISPObservation obs) {
        return Duration.ofMillis(Math.round(NifsSetupTimeService.getSetupTimeSec(obs) * 1000.0));
    }

    public static final Duration REACQUISITION_TIME = Duration.ofMinutes(6);

    @Override
    public Duration getReacquisitionTime(ISPObservation obs) {
        return REACQUISITION_TIME;
    }

    /**
     * For ITC.
     * @deprecated config is a key-object collection and is thus not type-safe. It is meant for ITC only.
     */
    @Deprecated @Override
    public Duration getSetupTime(Config conf) {
        final double secs;
        final GuideStarType guideStarType = (GuideStarType) conf.getItemValue(AOConstants.AO_GUIDE_STAR_TYPE_KEY);
        if (conf.containsItem(AOConstants.AO_SYSTEM_KEY) &&
                guideStarType.equals(AltairParams.GuideStarType.LGS)) {
                secs = NifsSetupTimeService.BASE_LGS_SETUP_TIME_SEC;
        } else {
            secs = NifsSetupTimeService.BASE_SETUP_TIME_SEC;
        }
        return Duration.ofMillis(Math.round(secs * 1000.0));
    }

    @Override
    public Duration getReacquisitionTime(Config conf) {
        return REACQUISITION_TIME;
    }

    /**
     * Return the dimensions of the science area.
     * @return an array giving the size of the detector in arcsec
     */
    public double[] getScienceArea() {
        return new double[] {Mask.SIZE, Mask.SIZE};
    }

    // ------------------------------------------------------------------------
    /**
     * Return the imaging mirror.
     */
    public ImagingMirror getImagingMirror() {
        return _imagingMirror;
    }

    /**
     * Set the imaging mirror.
     */
    public void setImagingMirror(ImagingMirror newValue) {
        ImagingMirror oldValue = getImagingMirror();
        if (oldValue != newValue) {
            _imagingMirror = newValue;
            firePropertyChange(IMAGING_MIRROR_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the imaging mirror with a String.
     */
    private void _setImagingMirror(String name) {
        ImagingMirror oldValue = getImagingMirror();
        setImagingMirror(ImagingMirror.getImagingMirror(name, oldValue));
    }

    // ------------------------------------------------------------------------

    /**
     * Get the disperser.
     */
    public Disperser getDisperser() {
        return _disperser;
    }

    /**
     * Set the disperser.
     */
    public void setDisperser(Disperser newValue) {
        Disperser oldValue = getDisperser();
        if (oldValue != newValue) {
            _disperser = newValue;
            firePropertyChange(DISPERSER_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the disperser with a String.
     */
    private void _setDisperser(String name) {
        Disperser oldValue = getDisperser();
        setDisperser(Disperser.getDisperser(name, oldValue));
    }

    // ------------------------------------------------------------------------

    /**
     * Get the detector read mode.
     */
    public ReadMode getReadMode() {
        return _readMode;
    }

    /**
     * Set the read mode.
     */
    public void setReadMode(ReadMode newValue) {
        ReadMode oldValue = getReadMode();
        if (oldValue != newValue) {
            _readMode = newValue;
            firePropertyChange(READMODE_PROP, oldValue, newValue);
        }
    }

    /**
     * Set the read mode with a String.
     */
    private void _setReadMode(String name) {
        ReadMode oldValue = getReadMode();
        setReadMode(ReadMode.getReadMode(name, oldValue));
    }

    // ------------------------------------------------------------------------

    /**
     * Get the mask.
     */
    public Mask getMask() {
        return _mask;
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

    // ------------------------------------------------------------------------

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
     * Set the filter. This method is needed as public by the NIFS
     * editor which currentl sets things via Strings.
     */
    public void setFilter(String name) {
        Filter oldValue = getFilter();
        setFilter(Filter.getFilter(name, oldValue));
    }


    // ------------------------------------------------------------------------

    /**
     * Set the central wavelength in um for the given order and update the
     * values for the other order wavelengths.
     */
    public final void setCentralWavelength(double wavelength) {
        if (_centralWavelength != wavelength) {
            double oldWavelength = _centralWavelength;
            _centralWavelength = wavelength;
            firePropertyChange(CENTRAL_WAVELENGTH_PROP, oldWavelength, wavelength);
        }
    }

    /**
     * Return the central wavelength in um.
     */
    public final double getCentralWavelength() {
        return _centralWavelength;
    }

    /**
     * Get the central wavelength as a String (in um)
     */
    public final String getCentralWavelengthAsString() {
        return Double.toString(_centralWavelength);
    }

    // ------------------------------------------------------------------------

    /**
     * Set the mask offset
     */
    public final void setMaskOffset(double newValue) {
        if (_maskOffset != newValue) {
            double oldValue = _maskOffset;
            _maskOffset = newValue;
            firePropertyChange(MASK_OFFSET_PROP, oldValue, newValue);
        }
    }

    /**
     * Return the mask offset
     */
    public final double getMaskOffset() {
        return _maskOffset;
    }

    /**
     * Get the mask offset as a String
     */
    public final String getMaskOffsetAsString() {
        return Double.toString(_maskOffset);
    }

    // ------------------------------------------------------------------------

    /**
     * Return a parameter set describing the current state of this object.
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, IMAGING_MIRROR_PROP, _imagingMirror.name());
        Pio.addParam(factory, paramSet, DISPERSER_PROP, _disperser.name());
        Pio.addParam(factory, paramSet, FILTER_PROP, _filter.name());
        Pio.addParam(factory, paramSet, READMODE_PROP, _readMode.name());
        Pio.addParam(factory, paramSet, CENTRAL_WAVELENGTH_PROP, getCentralWavelengthAsString());
        Pio.addParam(factory, paramSet, MASK_PROP, _mask.name());
        Pio.addParam(factory, paramSet, MASK_OFFSET_PROP, getMaskOffsetAsString());

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v;
        v = Pio.getValue(paramSet, IMAGING_MIRROR_PROP);
        if (v != null) {
            _setImagingMirror(v);
        }
        v = Pio.getValue(paramSet, DISPERSER_PROP);
        if (v != null) {
            _setDisperser(v);
        }
        v = Pio.getValue(paramSet, FILTER_PROP);
        if (v != null) {
            setFilter(v);
        }
        v = Pio.getValue(paramSet, READMODE_PROP);
        if (v != null) {
            _setReadMode(v);
        }
        v = Pio.getValue(paramSet, CENTRAL_WAVELENGTH_PROP);
        if (v != null) {
            setCentralWavelength(Double.parseDouble(v));
        }
        v = Pio.getValue(paramSet, MASK_PROP);
        if (v != null) {
            _setMask(v);
        }
        v = Pio.getValue(paramSet, MASK_OFFSET_PROP);
        if (v != null) {
            setMaskOffset(Double.parseDouble(v));
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
        sc.putParameter(DefaultParameter.getInstance(DISPERSER_PROP, getDisperser()));
        sc.putParameter(DefaultParameter.getInstance(READMODE_PROP, getReadMode()));
        sc.putParameter(DefaultParameter.getInstance(IMAGING_MIRROR_PROP, getImagingMirror()));
        sc.putParameter(DefaultParameter.getInstance(CENTRAL_WAVELENGTH_PROP, getCentralWavelength()));
        sc.putParameter(DefaultParameter.getInstance(EXPOSURE_TIME_PROP, getExposureTime()));
        sc.putParameter(DefaultParameter.getInstance(POS_ANGLE_PROP, getPosAngle()));
        sc.putParameter(DefaultParameter.getInstance(COADDS_PROP, getCoadds()));
        sc.putParameter(DefaultParameter.getInstance(MASK_PROP, getMask()));
        sc.putParameter(DefaultParameter.getInstance(MASK_OFFSET_PROP, getMaskOffset()));
        return sc;
    }


    /**
     * Return a list of InstConfigInfo objects describing the instrument's
     * queryable configuration parameters.
     */
    public static List<InstConfigInfo> getInstConfigInfo() {
        final List<InstConfigInfo> configInfo = new LinkedList<>();
        configInfo.add(new InstConfigInfo(IMAGING_MIRROR_PROP));
        configInfo.add(new InstConfigInfo(DISPERSER_PROP));
        configInfo.add(new InstConfigInfo(MASK_PROP));
        configInfo.add(new InstConfigInfo(FILTER_PROP));
        configInfo.add(new InstConfigInfo(READMODE_PROP));
        return configInfo;
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    private static Collection<GuideProbe> GUIDE_PROBES = GuideProbeUtil.instance.createCollection(NifsOiwfsGuideProbe.instance);

    public Collection<GuideProbe> getGuideProbes() {
        return GUIDE_PROBES;
    }

    public static final ConfigInjector<String> WAVELENGTH_INJECTOR = ConfigInjector.create(
        new ObsWavelengthCalc3<Disperser, Filter, Double>() {
            public PropertyDescriptor descriptor1() { return DISPERSER_PROP; }
            public PropertyDescriptor descriptor2() { return FILTER_PROP; }
            public PropertyDescriptor descriptor3() { return CENTRAL_WAVELENGTH_PROP; }
            public String calcWavelength(Disperser d, Filter f, Double cwl) {
                return InstNIFS.calcWavelength(d, f, cwl);
            }
        }
    );

    public static String calcWavelength(Disperser d, Filter f, Double centralWavelength) {
        if (d == Disperser.MIRROR) return f.getWavelength();
        return (centralWavelength == 0) ? null : centralWavelength.toString();
    }

    @Override public ConfigSequence postProcessSequence(ConfigSequence in) {
        Config[] configs = in.getAllSteps();

        for (Config c : configs) {
            if (CalConfigBuilderUtil.isCalStep(c)) {
                c.putItem(ReadMode.KEY, ReadMode.BRIGHT_OBJECT_SPEC);
            }
        }

        return new ConfigSequence(configs);
    }

    @Override
    public CalibrationKey extractKey(ISysConfig instrumentConfig) {
        NIFSParams.Disperser    disperser   = (NIFSParams.Disperser)    get(instrumentConfig, InstNIFS.DISPERSER_PROP);
        NIFSParams.Filter       filter      = (NIFSParams.Filter)       get(instrumentConfig, InstNIFS.FILTER_PROP);
        NIFSParams.Mask         mask        = (NIFSParams.Mask)         get(instrumentConfig, InstNIFS.MASK_PROP);
        Double wavelength = getWavelength(instrumentConfig);

        ConfigKeyNifs config = new ConfigKeyNifs(disperser, filter, mask);
        return new CalibrationKeyImpl.WithWavelength(config, wavelength);
    }

    private static final Angle PWFS1_VIG = Angle.arcmins(4.8);
    private static final Angle PWFS2_VIG = Angle.arcmins(4.0);

    @Override public Angle pwfs1VignettingClearance() { return PWFS1_VIG; }
    @Override public Angle pwfs2VignettingClearance() { return PWFS2_VIG; }
}
