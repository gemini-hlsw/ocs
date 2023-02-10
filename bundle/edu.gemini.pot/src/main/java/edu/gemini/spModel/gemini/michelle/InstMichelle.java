package edu.gemini.spModel.gemini.michelle;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.config.injector.ConfigInjector;
import edu.gemini.spModel.config.injector.obswavelength.ObsWavelengthCalc3;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.YesNoType;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.gemini.michelle.MichelleParams.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obs.plannedtime.CommonStepCalculator;
import edu.gemini.spModel.obs.plannedtime.ExposureCalculator;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTimeGroup;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.Category;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.StepCalculator;
import edu.gemini.spModel.obscomp.InstConfigInfo;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.util.Angle;

import java.beans.PropertyDescriptor;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;

import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY;

/**
 * The Michelle instrument.
 */
public final class InstMichelle extends SPInstObsComp implements PropertyProvider, StepCalculator {
    private static final Logger LOG = Logger.getLogger(InstMichelle.class.getName());

    // for serialization
    private static final long serialVersionUID = 8L;
    private static final String VERSION = "2009B-2";

    /**
     * This obs component's SP type.
     */
    public static final SPComponentType SP_TYPE = SPComponentType.INSTRUMENT_MICHELLE;

    public static final ISPNodeInitializer<ISPObsComponent, InstMichelle> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new InstMichelle(), c -> new InstMichelleCB(c));

    public static final String INSTRUMENT_NAME_PROP = "Michelle";
    // default values

    private static final int DEF_COADDS = 1;
    private static final double DEF_EXPOSURE_TIME        =  0.02;   // sec
    private static final double DEF_TOTAL_ON_SOURCE_TIME = 60.0;    // sec
    private static final double DEF_NOD_INTERVAL         = 30.0;    // sec
    private static final double DEF_CHOP_ANGLE           = 30.0;    // deg
    private static final double DEF_CHOP_THROW           = 15.0;    // arcsec
    private static final Option<Double> DEF_FIELD_ROTATOR_ANGLE  = None.instance(); // new Some<Double>(-0.455);  // degrees
    private static final Option<Double> DEF_SLIT_ANGLE           = None.instance();    // degrees
    private static final int    DEF_NEXP                 = 50;

    private static final double DEFAULT_NOD_DELAY        = 10.0;    // sec

    /**
     * Maximum chop throw in arcsecs.
     */
    public static final double MAX_CHOP_THROW           = 15.0;    // arcsec

    // Instrument settings
    private MichelleParams.Disperser _disperser = MichelleParams.Disperser.DEFAULT;
    private double _disperserLambda = _disperser.getLamda();

    private MichelleParams.Mask _mask = MichelleParams.Mask.DEFAULT;
    private MichelleParams.Filter _filter = MichelleParams.Filter.DEFAULT;
    private MichelleParams.NodOrientation _nodOrientation = MichelleParams.NodOrientation.DEFAULT;

    private double _totalOnSourceTime = DEF_TOTAL_ON_SOURCE_TIME; // sec
    private double _nodInterval = DEF_NOD_INTERVAL; // sec
    private double _chopAngle = DEF_CHOP_ANGLE; // deg
    private double _chopThrow = DEF_CHOP_THROW; // arcsec

    // Engineering parameters.
    private Option<DisperserOrder> _disperserOrder = None.instance();
    private Option<FilterWheelA> _filterA = None.instance();
    private Option<FilterWheelB> _filterB = None.instance();
    private Option<Position> _injectorPosition  = None.instance();
    private Option<Position> _extractorPosition = None.instance();
    private Option<Double> _fieldRotatorAngle = DEF_FIELD_ROTATOR_ANGLE;
    private Option<Double> _slitAngle         = DEF_SLIT_ANGLE;
    private Option<EngMask>            _engMask = None.instance();
    private Option<ChopMode>          _chopMode = None.instance();
    private Option<ChopWaveform>  _chopWaveform = None.instance();
    private int _nexp = DEF_NEXP;

    private MichelleParams.AutoConfigure _autoConfigure = MichelleParams.AutoConfigure.DEFAULT;
    private YesNoType _polarimetry = YesNoType.NO;

    public static final PropertyDescriptor AUTO_CONFIGURE_PROP;
    public static final PropertyDescriptor CHOP_ANGLE_PROP;
    public static final PropertyDescriptor CHOP_THROW_PROP;
    public static final PropertyDescriptor DISPERSER_PROP;
    public static final PropertyDescriptor DISPERSER_LAMBDA_PROP;
    public static final PropertyDescriptor EXPOSURE_TIME_PROP;
    public static final PropertyDescriptor FILTER_PROP;
    public static final PropertyDescriptor MASK_PROP;
    public static final PropertyDescriptor NOD_INTERVAL_PROP;
    public static final PropertyDescriptor NOD_ORIENTATION_PROP;
    public static final PropertyDescriptor POLARIMETRY_PROP;
    public static final ItemKey POLARIMETRY_KEY = new ItemKey(INSTRUMENT_KEY, "polarimetry");
    public static final PropertyDescriptor POS_ANGLE_PROP;
    public static final PropertyDescriptor TOTAL_ON_SOURCE_TIME_PROP;
    public static final ItemKey TOTAL_ON_SOURCE_TIME_KEY = new ItemKey(INSTRUMENT_KEY, "timeOnSource");

    public static final PropertyDescriptor DISPERSER_ORDER_PROP;
    public static final PropertyDescriptor FILTER_A_PROP;
    public static final PropertyDescriptor FILTER_B_PROP;
    public static final PropertyDescriptor INJECTOR_POSITION_PROP;
    public static final PropertyDescriptor EXTRACTOR_POSITION_PROP;
    public static final PropertyDescriptor FIELD_ROTATOR_ANGLE_PROP;
    public static final PropertyDescriptor SLIT_ANGLE_PROP;
    public static final PropertyDescriptor ENG_MASK_PROP;
    public static final PropertyDescriptor CHOP_MODE_PROP;
    public static final PropertyDescriptor CHOP_WAVEFORM_PROP;
    public static final PropertyDescriptor NEXP_PROP;


    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    static {
        final boolean query_yes = true;
        final boolean iter_yes = true;
        final boolean query_no = false;
        final boolean iter_no = false;

        AUTO_CONFIGURE_PROP = initProp("autoConfigure", query_no, iter_yes);
        CHOP_ANGLE_PROP = initProp("chopAngle", query_no, iter_yes);
        CHOP_THROW_PROP = initProp("chopThrow", query_no, iter_yes);
        DISPERSER_PROP        = initProp(Disperser.KEY.getName(), query_yes, iter_yes);
        DISPERSER_LAMBDA_PROP = initProp("disperserLambda", query_yes, iter_yes);
        EXPOSURE_TIME_PROP = initProp("exposureTime", query_no, iter_yes);
        FILTER_PROP = initProp(Filter.KEY.getName(), query_yes, iter_yes);
        MASK_PROP = initProp("mask", query_yes, iter_yes);
        NOD_INTERVAL_PROP = initProp("nodInterval", query_no, iter_no);
        NOD_ORIENTATION_PROP = initProp("nodOrientation", query_no, iter_no);
        POLARIMETRY_PROP = initProp(POLARIMETRY_KEY.getName(), query_no, iter_no);
        POS_ANGLE_PROP = initProp(InstConstants.POS_ANGLE_PROP, query_no, iter_no);
        TOTAL_ON_SOURCE_TIME_PROP = initProp(TOTAL_ON_SOURCE_TIME_KEY.getName(), query_no, iter_yes);


        // Engineering parameters

        DISPERSER_ORDER_PROP  = initProp("disperserOrder", query_yes, iter_no);
        DISPERSER_ORDER_PROP.setExpert(true);
        PropertySupport.setWrappedType(DISPERSER_ORDER_PROP, DisperserOrder.class);

        FILTER_A_PROP = initProp("filterWheelA", query_no, iter_no);
        FILTER_A_PROP.setExpert(true);
        PropertySupport.setWrappedType(FILTER_A_PROP, FilterWheelA.class);

        FILTER_B_PROP = initProp("filterWheelB", query_no, iter_no);
        FILTER_B_PROP.setExpert(true);
        PropertySupport.setWrappedType(FILTER_B_PROP, FilterWheelB.class);

        INJECTOR_POSITION_PROP  = initProp("injectorPosition", query_no, iter_no);
        INJECTOR_POSITION_PROP.setExpert(true);
        PropertySupport.setWrappedType(INJECTOR_POSITION_PROP, Position.class);

        EXTRACTOR_POSITION_PROP = initProp("extractorPosition", query_no, iter_no);
        EXTRACTOR_POSITION_PROP.setExpert(true);
        PropertySupport.setWrappedType(EXTRACTOR_POSITION_PROP, Position.class);

        FIELD_ROTATOR_ANGLE_PROP = initProp("fieldRotatorAngle", query_no, iter_no);
        FIELD_ROTATOR_ANGLE_PROP.setExpert(true);
        PropertySupport.setWrappedType(FIELD_ROTATOR_ANGLE_PROP, Double.class);

        SLIT_ANGLE_PROP = initProp("slitAngle", query_no, iter_no);
        SLIT_ANGLE_PROP.setExpert(true);
        PropertySupport.setWrappedType(SLIT_ANGLE_PROP, Double.class);

        ENG_MASK_PROP = initProp("engineeringMask", query_yes, iter_no);
        ENG_MASK_PROP.setExpert(true);
        PropertySupport.setWrappedType(ENG_MASK_PROP, EngMask.class);

        CHOP_MODE_PROP = initProp("chopMode", query_no, iter_no);
        CHOP_MODE_PROP.setDisplayName("Exposure Mode");
        CHOP_MODE_PROP.setExpert(true);
        PropertySupport.setWrappedType(CHOP_MODE_PROP, ChopMode.class);

        CHOP_WAVEFORM_PROP = initProp("chopWaveform", query_no, iter_no);
        CHOP_WAVEFORM_PROP.setDisplayName("Waveform");
        CHOP_WAVEFORM_PROP.setExpert(true);
        PropertySupport.setWrappedType(CHOP_WAVEFORM_PROP, ChopWaveform.class);

        NEXP_PROP = initProp("nexp", query_no, iter_no);
        NEXP_PROP.setExpert(true);
    }

    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        PropertyDescriptor pd;
        pd = PropertySupport.init(propName, InstMichelle.class, query, iter);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }

    /**
     * Constructor
     */
    public InstMichelle() {
        super(SP_TYPE);
        // Override the default exposure time
        _exposureTime = DEF_EXPOSURE_TIME;
        _coadds = DEF_COADDS;
        setVersion(VERSION);

        _disperserLambda = _disperser.getLamda();
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

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    public String getPhaseIResourceName() {
        return "gemMichelle";
    }

    /**
     * Returns true if the instrument is in chopping mode.
     */
    @Override public boolean isChopping() {
        Option<ChopMode> maybeChopMode = getChopMode();

        if (maybeChopMode.isEmpty()) { // exposure mode = "unspecified"
            switch (getDisperser()) {
                case MIRROR:    return true;
                case LOW_RES_10:return true;
                case LOW_RES_20:return true;
                default: return false;
            }
        }

        return maybeChopMode.getValue().equals(ChopMode.CHOP);
    }

    /**
     * Return the setup time before observing can begin
     * Michelle returns 10 minutes if imaging mode and 20 minutes if spectroscopy.
     * (Update: see OT-243: 10 and 20 min).
     * (Update: SCT-275, 6 minutes imaging, 15 minutes "longslit")
     */
    @Override
    public Duration getSetupTime(ISPObservation obs) {
        if (getDisperser() == Disperser.MIRROR) {
            return Duration.ofMinutes(6);
        }
        return Duration.ofMinutes(15);
    }

    @Override public CategorizedTimeGroup calc(Config cur, Option<Config> prev) {
        int      coadds = ExposureCalculator.instance.coadds(cur);
        Double onSource = (Double) cur.getItemValue(TOTAL_ON_SOURCE_TIME_KEY);

        Disperser disperser   = (Disperser) cur.getItemValue(Disperser.KEY);
        Filter filter         = (Filter) cur.getItemValue(Filter.KEY);
        YesNoType polarimetry = (YesNoType) cur.getItemValue(POLARIMETRY_KEY);

        double exp;
        if (polarimetry == YesNoType.YES) {
            // Magic numbers are from OT-548 - Kevin Volk.
            // * Update: magic numbers now from SCT-267 (Rachel Mason)
            //   all filters should be 0.075
            exp = onSource / 0.075;
        } else {
            // All those non polarimetry cases
            //adjust efficiency for QA filter and Imaging mode (SCT-128, OT-537)
            //magic number 0.15 come from K. Volk
            //OT-638: Qa filter efficiency changed from 0.15 to 0.21 (requested by K.Volk)
            double efficiency = (Disperser.MIRROR == disperser && Filter.QA == filter) ? 0.21 : disperser.getEfficiency();
            exp = (onSource / efficiency) * coadds;
        }

        CategorizedTime expTime = CategorizedTime.fromSeconds(Category.EXPOSURE, exp);
        return CommonStepCalculator.instance.calc(cur, prev).add(expTime);
    }

    /**
     * Set the auto-configure property.
     */
    public final void setAutoConfigure(AutoConfigure newValue) {
        AutoConfigure oldValue = getAutoConfigure();
        if (oldValue != newValue) {
            _autoConfigure = newValue;
            firePropertyChange(AUTO_CONFIGURE_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Get the auto-configure property.
     */
    public final AutoConfigure getAutoConfigure() {
        return _autoConfigure;
    }

    /**
     * Set the polarimetry property.
     */
    public final void setPolarimetry(YesNoType newValue) {
        YesNoType oldValue = getPolarimetry();
        if (oldValue != newValue) {
            _polarimetry = newValue;
            firePropertyChange(POLARIMETRY_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Get the polarimetry property.
     */
    public final YesNoType getPolarimetry() {
        return _polarimetry;
    }

    /**
     * Return the science area based upon the current camera.
     *
     * @return an array giving the size of the detector in arcsec
     */
    public double[] getScienceArea() {
        double w, h;
        if (!getEngineeringMask().isEmpty()) {
            EngMask engMask = getEngineeringMask().getValue();
            w = engMask.getWidth();
            h = engMask.getHeight();
        } else {
            Mask mask = getMask();
            w = mask.getWidth();
            h = mask.getHeight();
        }
        return new double[] {w, h};
    }

    /**
     * Gets the disperser order.
     */
    public Option<DisperserOrder> getDisperserOrder() {
        return _disperserOrder;
    }

    public void setDisperserOrder(Option<DisperserOrder> newValue) {
        Option<DisperserOrder> oldValue = getDisperserOrder();
        if (oldValue != newValue) {
            _disperserOrder = newValue;
            firePropertyChange(DISPERSER_ORDER_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Get the disperser.
     */
    public Disperser getDisperser() {
        return _disperser;
    }

    /**
     * Set the disperser type object using the same wavelength.
     */
    public void setDisperser(Disperser newDisperser) {
        Disperser oldDisperser = getDisperser();
        if (oldDisperser != newDisperser) {
            _disperser = newDisperser;
            firePropertyChange(DISPERSER_PROP.getName(), oldDisperser, newDisperser);
            setDisperserLambda(_disperser.getLamda());

            // can't limit it here -- won't work with the synchronization of the
            // first step of an instrument iterator
//            if ((newDisperser.getMode() == DisperserMode.CHOP) && (_chopThrow > MAX_CHOP_THROW)) {
//                setChopThrow(MAX_CHOP_THROW);
//            }
        }
    }

    public double getDisperserLambda() {
        return _disperserLambda;
    }

    public void setDisperserLambda(double newValue) {
        double oldValue = _disperserLambda;
        if (oldValue != newValue) {
            _disperserLambda = newValue;
            firePropertyChange(DISPERSER_LAMBDA_PROP.getName(), oldValue, newValue);
        }
    }

    public String getDisperserLambdaAsString() {
        return Double.toString(_disperserLambda);
    }

    /**
     * Set the disperser
     */
    private void _setDisperser(String name) {
        Disperser oldValue = getDisperser();
        setDisperser(Disperser.getDisperser(name, oldValue));
    }

    /**
     * Get the Mask.
     */
    public Mask getMask() {
        return _mask;
    }

    /**
     * Set the Mask.
     */
    public void setMask(Mask newValue) {
        Mask oldValue = getMask();
        if (oldValue != newValue) {
            _mask = newValue;
            firePropertyChange(MASK_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the Mask with a String.
     */
    private void _setMask(String name) {
        Mask oldValue = getMask();
        setMask(Mask.getMask(name, oldValue));
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
            firePropertyChange(FILTER_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the filter.
     */
    private void _setFilter(String name) {
        Filter oldValue = getFilter();
        setFilter(Filter.getFilter(name, oldValue));
    }

    /**
     * Get the nodOrientation.
     */
    public NodOrientation getNodOrientation() {
        return _nodOrientation;
    }

    /**
     * Set the nodOrientation.
     */
    public void setNodOrientation(NodOrientation newValue) {
        NodOrientation oldValue = getNodOrientation();
        if (oldValue != newValue) {
            _nodOrientation = newValue;
            firePropertyChange(NOD_ORIENTATION_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the nodOrientation.
     */
    private void _setNodOrientation(String name) {
        NodOrientation oldValue = getNodOrientation();
        setNodOrientation(NodOrientation.getNodOrientation(name, oldValue));
    }

    /**
     * Set the total on source time.
     */
    public final void setTimeOnSource(double newValue) {
        double oldValue = getTimeOnSource();
        if (oldValue != newValue) {
            _totalOnSourceTime = newValue;
            firePropertyChange(TOTAL_ON_SOURCE_TIME_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Get the total on source time.
     */
    public final double getTimeOnSource() {
        return _totalOnSourceTime;
    }

    /**
     * Get the total on source time as a string.
     */
    public final String getTotalOnSourceTimeAsString() {
        return Double.toString(_totalOnSourceTime);
    }


    /**
     * Set the nod interval (must be greater than the nod delay).
     */
    public final void setNodInterval(double newValue) {
        double oldValue = getNodInterval();
        if (oldValue != newValue && newValue > DEFAULT_NOD_DELAY) {
            _nodInterval = newValue;
            firePropertyChange(NOD_INTERVAL_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Get the nod interval.
     */
    public final double getNodInterval() {
        return _nodInterval;
    }

    /**
     * Get the nod interval as a string.
     */
    public final String getNodIntervalAsString() {
        return Double.toString(_nodInterval);
    }


    /**
     * Set the chop angle (in deg E of N).
     */
    public final void setChopAngle(double newValue) {
        double oldValue = getChopAngle();
        if (oldValue != newValue) {
            newValue = Angle.normalizeDegrees(newValue);
            _chopAngle = newValue;
            firePropertyChange(CHOP_ANGLE_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the chop angle in radians from due north (rounded to the nearest degree).
     */
    public final void setChopAngleRadians(double chopAngle) {
        setChopAngle(Math.round(Angle.radiansToDegrees(chopAngle)));
    }


    /**
     * Get the chop angle (in deg E of N).
     */
    public final double getChopAngle() {
        return _chopAngle;
    }

    /**
     * Get the chop angle as a string (in deg E of N).
     */
    public final String getChopAngleAsString() {
        return Double.toString(_chopAngle);
    }


    /**
     * Get the chop angle in radians.
     */
    public final double getChopAngleRadians() {
        return Angle.degreesToRadians(_chopAngle);
    }


    /**
     * Add the given angle to the current chop angle.
     */
    public final void addChopAngleRadians(double addAngle) {
        double angle = getChopAngleRadians();
        angle += addAngle;
        setChopAngleRadians(angle);
    }


    /**
     * Set the chop throw in arcsec.
     */
    public final void setChopThrow(double newValue) {
        // doesn't work when combined with the michelle inst iterator
//        newValue = Math.abs(newValue);
//        if ((_disperser.getMode() == DisperserMode.CHOP) && (newValue > MAX_CHOP_THROW)) {
//            newValue = MAX_CHOP_THROW;
//        }
        double oldValue = getChopThrow();
        if (oldValue != newValue) {
            _chopThrow = newValue;
            firePropertyChange(CHOP_THROW_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Return the chop throw in arcsec.
     */
    public final double getChopThrow() {
        return _chopThrow;
    }

    /**
     * Get the chop throw as a string (arcsec).
     */
    public final String getChopThrowAsString() {
        return Double.toString(_chopThrow);
    }

    public Option<FilterWheelA> getFilterWheelA() {
        return _filterA;
    }

    public void setFilterWheelA(Option<FilterWheelA> newValue) {
        Option<FilterWheelA> oldValue = getFilterWheelA();
        if (!oldValue.equals(newValue)) {
            _filterA = newValue;
            firePropertyChange(FILTER_A_PROP.getName(), oldValue, newValue);
        }
    }

    public Option<FilterWheelB> getFilterWheelB() {
        return _filterB;
    }

    public void setFilterWheelB(Option<FilterWheelB> newValue) {
        Option<FilterWheelB> oldValue = getFilterWheelB();
        if (!oldValue.equals(newValue)) {
            _filterB = newValue;
            firePropertyChange(FILTER_B_PROP.getName(), oldValue, newValue);
        }
    }

    public Option<Position> getInjectorPosition() {
        return _injectorPosition;
    }

    public void setInjectorPosition(Option<Position> newValue) {
        Option<Position> oldValue = getInjectorPosition();
        if (!oldValue.equals(newValue)) {
            _injectorPosition = newValue;
            firePropertyChange(INJECTOR_POSITION_PROP.getName(), oldValue, newValue);
        }
    }

    public Option<Position> getExtractorPosition() {
        return _extractorPosition;
    }

    public void setExtractorPosition(Option<Position> newValue) {
        Option<Position> oldValue = getExtractorPosition();
        if (!oldValue.equals(newValue)) {
            _extractorPosition = newValue;
            firePropertyChange(EXTRACTOR_POSITION_PROP.getName(), oldValue, newValue);
        }
    }

    public Option<Double> getFieldRotatorAngle() {
        return _fieldRotatorAngle;
    }

    public void setFieldRotatorAngle(Option<Double> newValue) {
        Option<Double> oldValue = getFieldRotatorAngle();
        if (oldValue != newValue) {
            _fieldRotatorAngle = newValue;
            firePropertyChange(FIELD_ROTATOR_ANGLE_PROP.getName(), oldValue, newValue);
        }
    }

    public Option<Double> getSlitAngle() {
        return _slitAngle;
    }

    public void setSlitAngle(Option<Double> newValue) {
        Option<Double> oldValue = getSlitAngle();
        if (oldValue != newValue) {
            _slitAngle = newValue;
            firePropertyChange(SLIT_ANGLE_PROP.getName(), oldValue, newValue);
        }
    }

    public Option<EngMask> getEngineeringMask() {
        return _engMask;
    }

    public void setEngineeringMask(Option<EngMask> newValue) {
        Option<EngMask> oldValue = getEngineeringMask();
        if (!oldValue.equals(newValue)) {
            _engMask = newValue;
            firePropertyChange(ENG_MASK_PROP.getName(), oldValue, newValue);
        }
    }

    public Option<ChopMode> getChopMode() {
        return _chopMode;
    }

    public void setChopMode(Option<ChopMode> newValue) {
        Option<ChopMode> oldValue = getChopMode();
        if (!oldValue.equals(newValue)) {
            _chopMode = newValue;
            firePropertyChange(CHOP_MODE_PROP.getName(), oldValue, newValue);
        }
    }

    public Option<ChopWaveform> getChopWaveform() {
        return _chopWaveform;
    }

    public void setChopWaveform(Option<ChopWaveform> newValue) {
        Option<ChopWaveform> oldValue = getChopWaveform();
        if (!oldValue.equals(newValue)) {
            _chopWaveform = newValue;
            firePropertyChange(CHOP_WAVEFORM_PROP.getName(), oldValue, newValue);
        }
    }

    public int getNexp() {
        return _nexp;
    }

    public void setNexp(int newValue) {
        int oldValue = getNexp();
        if (oldValue != newValue) {
            _nexp = newValue;
            firePropertyChange(NEXP_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Return a parameter set describing the current state of this object.
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        if (!_disperserOrder.isEmpty()) {
            Pio.addParam(factory, paramSet, DISPERSER_ORDER_PROP.getName(), getDisperserOrder().getValue().name());
        }

        // If using mirror, only write mirror
        Disperser d = _disperser;
        Pio.addParam(factory, paramSet, DISPERSER_PROP.getName(), d.name());
        if (d != Disperser.MIRROR) {
            Pio.addParam(factory, paramSet, DISPERSER_LAMBDA_PROP.getName(), getDisperserLambdaAsString());
        }

        Pio.addParam(factory, paramSet, MASK_PROP.getName(), getMask().name());
        Pio.addParam(factory, paramSet, FILTER_PROP.getName(), getFilter().name());
        Pio.addParam(factory, paramSet, TOTAL_ON_SOURCE_TIME_PROP.getName(), getTotalOnSourceTimeAsString());
        Pio.addParam(factory, paramSet, NOD_INTERVAL_PROP.getName(), getNodIntervalAsString());
        Pio.addParam(factory, paramSet, NOD_ORIENTATION_PROP.getName(), getNodOrientation().name());
        Pio.addParam(factory, paramSet, CHOP_ANGLE_PROP.getName(), getChopAngleAsString());
        Pio.addParam(factory, paramSet, CHOP_THROW_PROP.getName(), getChopThrowAsString());
        Pio.addParam(factory, paramSet, AUTO_CONFIGURE_PROP.getName(), getAutoConfigure().name());
        Pio.addParam(factory, paramSet, POLARIMETRY_PROP.getName(), getPolarimetry().name());

        if (!_filterA.isEmpty()) {
            Pio.addParam(factory, paramSet, FILTER_A_PROP.getName(), getFilterWheelA().getValue().name());
        }
        if (!_filterB.isEmpty()) {
            Pio.addParam(factory, paramSet, FILTER_B_PROP.getName(), getFilterWheelB().getValue().name());
        }
        if (!_injectorPosition.isEmpty()) {
            Pio.addParam(factory, paramSet, INJECTOR_POSITION_PROP.getName(), getInjectorPosition().getValue().name());
        }
        if (!_extractorPosition.isEmpty()) {
            Pio.addParam(factory, paramSet, EXTRACTOR_POSITION_PROP.getName(), getExtractorPosition().getValue().name());
        }
        if (!_fieldRotatorAngle.isEmpty()) {
            Pio.addParam(factory, paramSet, FIELD_ROTATOR_ANGLE_PROP.getName(), String.format("%f", getFieldRotatorAngle().getValue()));
        }
        if (!_slitAngle.isEmpty()) {
            Pio.addParam(factory, paramSet, SLIT_ANGLE_PROP.getName(), String.format("%f", getSlitAngle().getValue()));
        }
        if (!_engMask.isEmpty()) {
            Pio.addParam(factory, paramSet, ENG_MASK_PROP.getName(), getEngineeringMask().getValue().name());
        }

        if (!_chopMode.isEmpty()) {
            Pio.addParam(factory, paramSet, CHOP_MODE_PROP.getName(), getChopMode().getValue().name());
        }
        if (!_chopWaveform.isEmpty()) {
            Pio.addParam(factory, paramSet, CHOP_WAVEFORM_PROP.getName(), getChopWaveform().getValue().name());
        }
        Pio.addIntParam(factory, paramSet, NEXP_PROP.getName(), getNexp());

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String disperser = Pio.getValue(paramSet, DISPERSER_PROP.getName(), _disperser.name());
        if (disperser.equals(Disperser.MIRROR.name())) {
            _setDisperser(disperser);
        } else {
            String wavelength = Pio.getValue(paramSet, DISPERSER_LAMBDA_PROP.getName());
            _setDisperser(disperser);
            if (wavelength != null) setDisperserLambda(Double.parseDouble(wavelength));
        }

        String v = Pio.getValue(paramSet, DISPERSER_ORDER_PROP.getName());
        if (v != null) setDisperserOrder(DisperserOrder.valueOf(v, getDisperserOrder()));

        v = Pio.getValue(paramSet, MASK_PROP.getName());
        if (v != null) {
            _setMask(v);
        }
        v = Pio.getValue(paramSet, FILTER_PROP.getName());
        if (v != null) {
            _setFilter(v);
        }
        v = Pio.getValue(paramSet, TOTAL_ON_SOURCE_TIME_PROP.getName());
        if (v != null) {
            setTimeOnSource(Double.parseDouble(v));
        }
        v = Pio.getValue(paramSet, NOD_INTERVAL_PROP.getName());
        if (v != null) {
            setNodInterval(Double.parseDouble(v));
        }
        v = Pio.getValue(paramSet, NOD_ORIENTATION_PROP.getName());
        if (v != null) {
            _setNodOrientation(v);
        }
        v = Pio.getValue(paramSet, CHOP_ANGLE_PROP.getName());
        if (v != null) {
            setChopAngle(Double.parseDouble(v));
        }
        v = Pio.getValue(paramSet, CHOP_THROW_PROP.getName());
        if (v != null) {
            setChopThrow(Double.parseDouble(v));
        }
        v = Pio.getValue(paramSet, AUTO_CONFIGURE_PROP.getName());
        if (v != null) {
            setAutoConfigure(AutoConfigure.getAutoConfigure(v));
        }
        v = Pio.getValue(paramSet, POLARIMETRY_PROP.getName());
        if (v != null) {
            setPolarimetry(YesNoType.getYesNoType(v, YesNoType.NO));
        }

        v = Pio.getValue(paramSet, FILTER_A_PROP.getName());
        if (v != null) setFilterWheelA(FilterWheelA.valueOf(v, getFilterWheelA()));

        v = Pio.getValue(paramSet, FILTER_B_PROP.getName());
        if (v != null) setFilterWheelB(FilterWheelB.valueOf(v, getFilterWheelB()));

        v = Pio.getValue(paramSet, INJECTOR_POSITION_PROP.getName());
        if (v != null) setInjectorPosition(Position.valueOf(v, getInjectorPosition()));

        v = Pio.getValue(paramSet, EXTRACTOR_POSITION_PROP.getName());
        if (v != null) setExtractorPosition(Position.valueOf(v, getExtractorPosition()));

        v = Pio.getValue(paramSet, FIELD_ROTATOR_ANGLE_PROP.getName());
        if (v != null) {
            try {
                setFieldRotatorAngle(new Some<>(Double.parseDouble(v)));
            } catch (Exception ex) {
                LOG.warning("Could not parse field rotator angle " + v);
            }
        }
        v = Pio.getValue(paramSet, SLIT_ANGLE_PROP.getName());
        if (v != null) {
            try {
                setSlitAngle(new Some<>(Double.parseDouble(v)));
            } catch (Exception ex) {
                LOG.warning("Could not parse slit angle " + v);
            }
        }

        v = Pio.getValue(paramSet, ENG_MASK_PROP.getName());
        if (v != null) setEngineeringMask(EngMask.valueOf(v, getEngineeringMask()));

        v = Pio.getValue(paramSet, CHOP_MODE_PROP.getName());
        if (v != null) setChopMode(ChopMode.valueOf(v, getChopMode()));

        v = Pio.getValue(paramSet, CHOP_WAVEFORM_PROP.getName());
        if (v != null) setChopWaveform(ChopWaveform.valueOf(v, getChopWaveform()));

        setNexp(Pio.getIntValue(paramSet, NEXP_PROP.getName(), getNexp()));
    }

    /**
     * Return the configuration for this component.
     */
    public ISysConfig getSysConfig() {
        final ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);

        // Fill in the current values.
        sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion()));

        final Option<DisperserOrder> dorder = getDisperserOrder();
        if (!dorder.isEmpty()) {
            sc.putParameter(DefaultParameter.getInstance(DISPERSER_ORDER_PROP.getName(), dorder.getValue()));
        }

        final Disperser d = getDisperser();
        sc.putParameter(DefaultParameter.getInstance(DISPERSER_PROP.getName(), d));
        if (d != Disperser.MIRROR) {
            sc.putParameter(DefaultParameter.getInstance(DISPERSER_LAMBDA_PROP.getName(), getDisperserLambda()));
        }

        sc.putParameter(DefaultParameter.getInstance(MASK_PROP.getName(), getMask()));
        sc.putParameter(DefaultParameter.getInstance(FILTER_PROP.getName(), getFilter()));

        sc.putParameter(DefaultParameter.getInstance(TOTAL_ON_SOURCE_TIME_PROP.getName(), getTimeOnSource()));
        sc.putParameter(DefaultParameter.getInstance(NOD_INTERVAL_PROP.getName(), getNodInterval()));
        sc.putParameter(DefaultParameter.getInstance(CHOP_ANGLE_PROP.getName(), getChopAngle()));
        sc.putParameter(DefaultParameter.getInstance(CHOP_THROW_PROP.getName(), getChopThrow()));

        if (getAutoConfigure() == AutoConfigure.NO) {
            sc.putParameter(DefaultParameter.getInstance(EXPOSURE_TIME_PROP.getName(), getExposureTime()));
        }
        sc.putParameter(DefaultParameter.getInstance(AUTO_CONFIGURE_PROP.getName(), getAutoConfigure()));
        sc.putParameter(DefaultParameter.getInstance(POLARIMETRY_PROP.getName(), getPolarimetry()));
        sc.putParameter(DefaultParameter.getInstance(POS_ANGLE_PROP.getName(), getPosAngleDegrees()));

        final Option<FilterWheelA> filtA = getFilterWheelA();
        if (!filtA.isEmpty()) {
            sc.putParameter(DefaultParameter.getInstance(FILTER_A_PROP.getName(), filtA.getValue()));
        }
        final Option<FilterWheelB> filtB = getFilterWheelB();
        if (!filtB.isEmpty()) {
            sc.putParameter(DefaultParameter.getInstance(FILTER_B_PROP.getName(), filtB.getValue()));
        }

        Option<Position> pos = getInjectorPosition();
        if (!pos.isEmpty()) {
            sc.putParameter(DefaultParameter.getInstance(INJECTOR_POSITION_PROP.getName(), pos.getValue()));
        }
        pos = getExtractorPosition();
        if (!pos.isEmpty()) {
            sc.putParameter(DefaultParameter.getInstance(EXTRACTOR_POSITION_PROP.getName(), pos.getValue()));
        }
        final Option<Double> rotAngle = getFieldRotatorAngle();
        if (!rotAngle.isEmpty()) {
            sc.putParameter(DefaultParameter.getInstance(FIELD_ROTATOR_ANGLE_PROP.getName(), rotAngle.getValue()));
        }
        final Option<Double> slitAngle = getSlitAngle();
        if (!slitAngle.isEmpty()) {
            sc.putParameter(DefaultParameter.getInstance(SLIT_ANGLE_PROP.getName(), getSlitAngle().getValue()));
        }
        final Option<EngMask> engMask = getEngineeringMask();
        if (!engMask.isEmpty()) {
            sc.putParameter(DefaultParameter.getInstance(ENG_MASK_PROP.getName(), engMask.getValue()));
        }
        final Option<ChopMode> chopMode = getChopMode();
        if (!chopMode.isEmpty()) {
            sc.putParameter(DefaultParameter.getInstance(CHOP_MODE_PROP.getName(), chopMode.getValue()));
            sc.putParameter(DefaultParameter.getInstance(NEXP_PROP.getName(), getNexp()));
        }
        final Option<ChopWaveform> chopWaveform = getChopWaveform();
        if (!chopWaveform.isEmpty()) {
            sc.putParameter(DefaultParameter.getInstance(CHOP_WAVEFORM_PROP.getName(), chopWaveform.getValue()));
        }
        return sc;
    }


    /**
     * Return a list of InstConfigInfo objects describing the instrument's
     * queryable configuration parameters.
     */
    public static List<InstConfigInfo> getInstConfigInfo() {
        final List<InstConfigInfo> configInfo = new LinkedList<>();

        configInfo.add(new InstConfigInfo(DISPERSER_PROP));
        configInfo.add(new InstConfigInfo(MASK_PROP));
        configInfo.add(new InstConfigInfo(ENG_MASK_PROP));
        configInfo.add(new InstConfigInfo(FILTER_PROP));
        configInfo.add(new InstConfigInfo(DISPERSER_ORDER_PROP));

        return configInfo;
    }

    /**
     * Michelle doesn't have an OIWFS
     */
    public boolean hasOIWFS() {
        return false;
    }

    public static final ConfigInjector<String> WAVELENGTH_INJECTOR = ConfigInjector.create(
        new ObsWavelengthCalc3<Disperser, Filter, Double>() {
            public PropertyDescriptor descriptor1() { return DISPERSER_PROP; }
            public PropertyDescriptor descriptor2() { return FILTER_PROP; }
            public PropertyDescriptor descriptor3() { return DISPERSER_LAMBDA_PROP; }
            public String calcWavelength(Disperser d, Filter f, Double cwl) {
                return InstMichelle.calcWavelength(d, f, cwl);
            }
        }
    );

    public static String calcWavelength(Disperser d, Filter f, Double centralWavelength) {
        if (d == Disperser.MIRROR) return f.getWavelength();
        return (centralWavelength == 0.0) ? null : centralWavelength.toString();
    }

    private static final edu.gemini.skycalc.Angle PWFS1_VIG = edu.gemini.skycalc.Angle.arcmins(5.3);
    private static final edu.gemini.skycalc.Angle PWFS2_VIG = edu.gemini.skycalc.Angle.arcmins(4.8);

    @Override public edu.gemini.skycalc.Angle pwfs1VignettingClearance(ObsContext ctx) { return PWFS1_VIG; }
    @Override public edu.gemini.skycalc.Angle pwfs2VignettingClearance(ObsContext ctx) { return PWFS2_VIG; }

}
