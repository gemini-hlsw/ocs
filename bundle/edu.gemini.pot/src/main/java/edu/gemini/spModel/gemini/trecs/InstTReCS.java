// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: InstTReCS.java 45259 2012-05-14 23:58:29Z fnussber $
//

package edu.gemini.spModel.gemini.trecs;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.config.injector.ConfigInjector;
import edu.gemini.spModel.config.injector.obswavelength.ObsWavelengthCalc3;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.gemini.trecs.TReCSParams.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obs.plannedtime.CommonStepCalculator;
import edu.gemini.spModel.obs.plannedtime.ExposureCalculator;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTimeGroup;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.Category;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.StepCalculator;
import edu.gemini.spModel.obscomp.InstConfigInfo;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.util.Angle;

import java.beans.PropertyDescriptor;
import java.time.Duration;
import java.util.*;

import static edu.gemini.spModel.obscomp.InstConstants.EXPOSURE_TIME_KEY;
import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY;

/**
 * The TReCS instrument.
 */
public final class InstTReCS extends SPInstObsComp implements PropertyProvider, StepCalculator {

    // for serialization
    private static final long serialVersionUID = 6L;

    private static final double MAX_CHOP_THROW = 15.;

    /**
     * This obs component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.INSTRUMENT_TRECS;

    public static final ISPNodeInitializer<ISPObsComponent, InstTReCS> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new InstTReCS(), c -> new InstTReCSCB(c));

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    public static final PropertyDescriptor DISPERSER_PROP;
    public static final PropertyDescriptor DISPERSER_LAMBDA_PROP;
    public static final PropertyDescriptor FILTER_PROP;
    public static final PropertyDescriptor MASK_PROP;
    public static final PropertyDescriptor TOTAL_ON_SOURCE_TIME_PROP;
    public static final ItemKey TOTAL_ON_SOURCE_TIME_KEY = new ItemKey(INSTRUMENT_KEY, "timeOnSource");

    public static final PropertyDescriptor TIME_PER_SAVESET_PROP;
    public static final PropertyDescriptor NOD_DWELL_PROP;
    public static final PropertyDescriptor NOD_SETTLE_PROP;
    public static final PropertyDescriptor NOD_ORIENTATION_PROP;
    public static final PropertyDescriptor CHOP_ANGLE_PROP;
    public static final PropertyDescriptor CHOP_THROW_PROP;
    public static final PropertyDescriptor DATA_MODE_PROP;
    public static final PropertyDescriptor OBS_MODE_PROP;
    public static final PropertyDescriptor WINDOW_WHEEL_PROP;
    public static final PropertyDescriptor READOUT_MODE_PROP;

    public static final PropertyDescriptor COADDS_PROP;
    public static final PropertyDescriptor POS_ANGLE_PROP;
    /**
     * The name of the TReCS instrument configuration
     */
    public static final String INSTRUMENT_NAME_PROP = "TReCS";

    // default values
    public static final int DEF_COADDS = 1;
    public static final double DEF_EXPOSURE_TIME = 0.02; // sec
    public static final double DEFAULT_TOTAL_ON_SOURCE_TIME = 900.; // sec
    public static final double DEFAULT_TIME_PER_SAVESET = 10.; // sec
    public static final double DEFAULT_CHOP_ANGLE = 0.; // deg
    public static final double DEFAULT_CHOP_THROW = 15.; // arcsec

    public static final double DEFAULT_CHOP_DUTY_CYCLE = 0.71; // percent (SCT-107)
    public static final double SPECTROSCOPY_CHOP_DUTY_CYCLE = 0.6; // percent (SCT-107)
//    public static final double DEFAULT_NOD_DELAY = 10.; // sec

    public static final double DEFAULT_NOD_DWELL = 45.; // sec
    public static final double DEFAULT_NOD_SETTLE = 8.; // sec
    public static final double DEFAULT_DISPERSER_LAMBDA = 10.5; // nanometers

    public static final double DEFAULT_LAMBDA = DEFAULT_DISPERSER_LAMBDA;


    // Instrument settings
    private double _lambda = DEFAULT_LAMBDA;
    private Disperser disperser = Disperser.DEFAULT;

    private Mask _mask = Mask.DEFAULT;
    private Filter _filter = Filter.DEFAULT;
    private NodOrientation _nodOrientation = NodOrientation.DEFAULT;

    private double _totalOnSourceTime = DEFAULT_TOTAL_ON_SOURCE_TIME; // sec
    private double _timePerSaveset = DEFAULT_TIME_PER_SAVESET; // sec

    private double _chopAngle = DEFAULT_CHOP_ANGLE; // deg
    private double _chopThrow = DEFAULT_CHOP_THROW; // arcsec

    private DataMode _dataMode = DataMode.DEFAULT;
    private ObsMode _obsMode = ObsMode.DEFAULT;
    private WindowWheel _windowWheel = WindowWheel.DEFAULT;
    private ReadoutMode _readoutMode = ReadoutMode.DEFAULT;

    // (These two parameters will disappear when the busy/idle and recognition
    // of telescope nod state are correctly implemented in T-ReCS instrument
    // software).
    private double _nodDwell = DEFAULT_NOD_DWELL; // sec
    private double _nodSettle = DEFAULT_NOD_SETTLE; // sec

    private static final String _VERSION = "2007A-1";

   private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        PropertyDescriptor pd;
        pd = PropertySupport.init(propName, InstTReCS.class, query, iter);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }

    static {
        final boolean query_yes = true;
        final boolean iter_yes = true;
        final boolean query_no = false;
        final boolean iter_no = false;
        DISPERSER_PROP          = initProp(Disperser.KEY.getName(), query_yes, iter_yes);
        DISPERSER_LAMBDA_PROP   = initProp("disperserLambda", query_no, iter_yes);
        FILTER_PROP             = initProp("filter", query_yes, iter_yes);
        MASK_PROP               = initProp("mask", query_yes, iter_yes);
        TOTAL_ON_SOURCE_TIME_PROP = initProp(TOTAL_ON_SOURCE_TIME_KEY.getName(), query_no, iter_yes);
        TIME_PER_SAVESET_PROP   = initProp("timePerSaveset", query_no, iter_yes);
        NOD_DWELL_PROP          = initProp("nodDwell", query_no, iter_yes);
        NOD_SETTLE_PROP         = initProp("nodSettle", query_no, iter_yes);
        NOD_ORIENTATION_PROP    = initProp("nodOrientation", query_no, iter_no);
        CHOP_ANGLE_PROP         = initProp("chopAngle", query_no, iter_yes);
        CHOP_THROW_PROP         = initProp("chopThrow", query_no, iter_yes);
        DATA_MODE_PROP          = initProp("dataMode", query_no, iter_yes);
        OBS_MODE_PROP           = initProp("obsMode", query_no, iter_yes);
        WINDOW_WHEEL_PROP       = initProp("windowWheel", query_no, iter_yes);
        READOUT_MODE_PROP       = initProp("readoutMode", query_no, iter_yes);

        COADDS_PROP                 = initProp("coadds", query_no, iter_no);
        POS_ANGLE_PROP              = initProp("posAngle", query_no, iter_no);
    }

    /** Constructor */
    public InstTReCS() {
        super(SP_TYPE);
        // Override the default exposure time
        _exposureTime = DEF_EXPOSURE_TIME;
        _coadds = DEF_COADDS;
        setVersion(_VERSION);
    }

    public Object clone() {
        // No problems cloning here since private variables are immutable
        return super.clone();
    }

    @Override
    public Set<Site> getSite() {
        return Site.SET_GS;
    }

    public String getPhaseIResourceName() {
        return "gemT-ReCS";
    }

    /**
     * Returns true if the instrument is in chopping mode.
     */
    @Override public boolean isChopping() {
       return (getObsMode() == ObsMode.CHOP || getObsMode() == ObsMode.CHOP_NOD);
    }

    /**
     * Return the setup time in seconds before observing can begin
     * TReCS returns 15 minutes if imaging mode and 30 minutes if spectroscopy.
     * (Update: see OT-243: 10 and 20 min).
     * (Update: SCT-275: 6 minutes imaging, 20 minutes otherwise)
     */
    @Override
    public Duration getSetupTime(ISPObservation obs) {
        final Disperser d  = getDisperser();
        if (d == Disperser.MIRROR || d == Disperser.HIGH_RES_REF_MIRROR || d == Disperser.LOW_RES_REF_MIRROR) {
            return Duration.ofMinutes(6);
        }
        return Duration.ofMinutes(20);
    }

    /**
     * Set the exposure time from the given String.
     * (For backward compatibility: This method does nothing, since the
     * Exposure (Frame) Time is now in the engineering component.)
     */
    public void setExposureTimeAsString(String newValue) {
    }

    // The TReCS engineering component will add an exposure time property
    // as a String "auto".
    private double extractExposureTime(Config cur) {
        Object obj = cur.getItemValue(EXPOSURE_TIME_KEY);
        if (obj == null) return _exposureTime; // default value

        if (obj instanceof Double) return (Double) obj;

        if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException ex) {
            }
        }
        return _exposureTime;
    }

    // Calculate the elapsed time based on the current settings.
    //
    // Description from Phil:
    //
    // The total elapsed time depends on the chopping duty cycle and nod
    // overhead; these are not user-definable parameters. As placeholders,
    // assume chop duty cycle = 80% and nod delay = 10s.
    //
    // As an approximation:
    //
    // Elapsed time ~ (2 * Time on source / chop duty cycle) / (1 - nod delay / nod interval)
    //
    // Obviously the value of nod interval must be greater than the nod delay.
    //
    // The elapsed time is an approximation. The displayed value should be
    // rounded up to 2N times the nod interval (where N is an integer).
    @Override public CategorizedTimeGroup calc(Config cur, Option<Config> prev) {
        int coadds  = ExposureCalculator.instance.coadds(cur);
        double exp  = extractExposureTime(cur);

        double onSource    = (Double) cur.getItemValue(TOTAL_ON_SOURCE_TIME_KEY);
        double nodDelay    = _nodSettle;    // SCT-107. Nod Delay should be the sam as Nod Settle
        double nodInterval = _nodSettle + _nodDwell; //Nod Settle changes, so we change the nod interval TODO: right?

        //SCT-107: The Chop Duty Cycle changes if we are in imaging or spectroscopic mode.
        Disperser disperser  = (Disperser) cur.getItemValue(Disperser.KEY);
        double chopDutyCicle = ((disperser == Disperser.MIRROR) || (disperser == Disperser.LOW_RES_REF_MIRROR) || (disperser == Disperser.HIGH_RES_REF_MIRROR)) ? DEFAULT_CHOP_DUTY_CYCLE : SPECTROSCOPY_CHOP_DUTY_CYCLE;

        double secs = exp;
        if (nodInterval > nodDelay) {
            secs = (2. * onSource / chopDutyCicle) / (1. - nodDelay / nodInterval);

            // round up to 2N times the nod interval
            double dn = 2. * nodInterval;
            secs += dn - (secs % dn);
        }
        secs *= coadds;

        CategorizedTime expTime = CategorizedTime.fromSeconds(Category.EXPOSURE, secs);
        return CommonStepCalculator.instance.calc(cur, prev).add(expTime);
    }


    /**
     * Return the science area based upon the current camera.
     * @return an array giving the size of the detector in arcsec
     */
    public double[] getScienceArea() {
        Mask mask = getMask();
        return new double[]{mask.getWidth(), mask.getHeight()};
    }


    /**
     * Set the disperser type object using the same wavelength.
     */
    public void setDisperser(Disperser newDisperser) {
        Disperser oldDisperser = getDisperser();
        if (oldDisperser != newDisperser) {
            disperser = newDisperser;
            firePropertyChange(DISPERSER_PROP.getName(), oldDisperser, newDisperser);
        }
    }

    public Disperser getDisperser() {
        return disperser;
    }

    /**
     * Set the disperser name with a String.
     */
    public void setDisperserName(String name) {
        // Slightly inefficient, but okay for small objects.
        if (name == null) return;
        setDisperser(Disperser.getDisperser(name, getDisperser()));
    }

    public void setDisperserLambda(double lambda) {
        double oldValue = getDisperserLambda();
        if (oldValue != lambda) {
            _lambda = lambda;
            firePropertyChange(DISPERSER_LAMBDA_PROP.getName(), oldValue, lambda);
        }
    }

    public double getDisperserLambda() {
        return _lambda;
    }

    /**
     * Get the Mask.
     */
    public Mask getMask() {
        if (_mask == null) {
            _mask = Mask.DEFAULT;
        }
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
     * Get the DataMode.
     */
    public DataMode getDataMode() {
        if (_dataMode == null) {
            _dataMode = DataMode.DEFAULT;
        }
        return _dataMode;
    }

    /**
     * Set the DataMode.
     */
    public void setDataMode(DataMode newValue) {
        DataMode oldValue = getDataMode();
        if (oldValue != newValue) {
            _dataMode = newValue;
            firePropertyChange(DATA_MODE_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the DataMode with a String.
     */
    private void _setDataMode(String name) {
        DataMode oldValue = getDataMode();
        setDataMode(DataMode.getDataMode(name, oldValue));
    }


    /**
     * Get the ObsMode.
     */
    public ObsMode getObsMode() {
        if (_obsMode == null) {
            _obsMode = ObsMode.DEFAULT;
        }
        return _obsMode;
    }

    /**
     * Set the ObsMode.
     */
    public void setObsMode(ObsMode newValue) {
        ObsMode oldValue = getObsMode();
        if (oldValue != newValue) {
            _obsMode = newValue;
            firePropertyChange(OBS_MODE_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the ObsMode with a String.
     */
    private void _setObsMode(String name) {
        ObsMode oldValue = getObsMode();
        setObsMode(ObsMode.getObsMode(name, oldValue));
    }


    /**
     * Get the Window Wheel.
     */
    public WindowWheel getWindowWheel() {
        if (_windowWheel == null) {
            _windowWheel = WindowWheel.DEFAULT;
        }
        return _windowWheel;
    }

    /**
     * Set the Window Wheel.
     */
    public void setWindowWheel(WindowWheel newValue) {
        WindowWheel oldValue = getWindowWheel();
        if (oldValue != newValue) {
            _windowWheel = newValue;
            firePropertyChange(WINDOW_WHEEL_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the Window Wheel with a String.
     */
    private void _setWindowWheel(String name) {
        WindowWheel oldValue = getWindowWheel();
        setWindowWheel(WindowWheel.getWindowWheel(name, oldValue));
    }

    /**
     * Get the Readout Mode.
     */
    public ReadoutMode getReadoutMode() {
        if (_readoutMode == null) {
            _readoutMode = ReadoutMode.DEFAULT;
        }
        return _readoutMode;
    }

    /**
     * Set the Readout Mode.
     */
    public void setReadoutMode(ReadoutMode newValue) {
        ReadoutMode oldValue = getReadoutMode();
        if (oldValue != newValue) {
            _readoutMode = newValue;
            firePropertyChange(READOUT_MODE_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the Readout Mode with a String.
     */
    private void _setReadoutMode(String name) {
        ReadoutMode oldValue = getReadoutMode();
        setReadoutMode(ReadoutMode.getReadoutMode(name, oldValue));
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
     * Set the time per saveset.
     */
    public final void setTimePerSaveset(double newValue) {
        double oldValue = getTimePerSaveset();
        if (oldValue != newValue) {
            _timePerSaveset = newValue;
            firePropertyChange(TIME_PER_SAVESET_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Get the time per saveset.
     */
    public final double getTimePerSaveset() {
        return _timePerSaveset;
    }

    /**
     * Get the time per saveset as a string.
     */
    public final String getTimePerSavesetAsString() {
        return Double.toString(_timePerSaveset);
    }

    /**
     * Set the nod dwell time in secs
     */
    public final void setNodDwell(double newValue) {
        double oldValue = getNodDwell();
        if (oldValue != newValue) {
            _nodDwell = newValue;
            firePropertyChange(NOD_DWELL_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Get the nod dwell time in secs.
     */
    public final double getNodDwell() {
        if (_nodDwell == 0.) {
            _nodDwell = DEFAULT_NOD_DWELL;
        }
        return _nodDwell;
    }

    /**
     * Get the nod dwell as a string.
     */
    public final String getNodDwellAsString() {
        return Double.toString(getNodDwell());
    }


    /**
     * Set the nod settle time in secs
     */
    public final void setNodSettle(double newValue) {
        double oldValue = getNodSettle();
        if (oldValue != newValue) {
            _nodSettle = newValue;
            firePropertyChange(NOD_SETTLE_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Get the nod settle time in secs.
     */
    public final double getNodSettle() {
        if (_nodSettle == 0.) {
            _nodSettle = DEFAULT_NOD_SETTLE;
        }
        return _nodSettle;
    }

    /**
     * Get the nod settle as a string.
     */
    public final String getNodSettleAsString() {
        return Double.toString(getNodSettle());
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
        if (newValue > MAX_CHOP_THROW) {
            newValue = MAX_CHOP_THROW;
        }
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

    /**
     * Return a parameter set describing the current state of this object.
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        // If using mirror, only write mirror
        Disperser d = getDisperser();
        Pio.addParam(factory, paramSet, DISPERSER_PROP.getName(), d.name());
        if (d != Disperser.MIRROR) {
            Pio.addParam(factory, paramSet, DISPERSER_LAMBDA_PROP.getName(), Double.toString(getDisperserLambda()));
        }
        //TODO: What the heck does this line down here..doesn't make sense if the comment upthere is correct
        Pio.addParam(factory, paramSet, DISPERSER_LAMBDA_PROP.getName(), Double.toString(getDisperserLambda()));
        Pio.addParam(factory, paramSet, MASK_PROP.getName(), getMask().name());
        Pio.addParam(factory, paramSet, FILTER_PROP.getName(), getFilter().name());
        Pio.addParam(factory, paramSet, TOTAL_ON_SOURCE_TIME_PROP.getName(), getTotalOnSourceTimeAsString());
        Pio.addParam(factory, paramSet, TIME_PER_SAVESET_PROP.getName(), getTimePerSavesetAsString());
        Pio.addParam(factory, paramSet, CHOP_ANGLE_PROP.getName(), getChopAngleAsString());
        Pio.addParam(factory, paramSet, CHOP_THROW_PROP.getName(), getChopThrowAsString());
        Pio.addParam(factory, paramSet, DATA_MODE_PROP.getName(), getDataMode().name());
        Pio.addParam(factory, paramSet, OBS_MODE_PROP.getName(), getObsMode().name());
        Pio.addParam(factory, paramSet, WINDOW_WHEEL_PROP.getName(), getWindowWheel().name());
        Pio.addParam(factory, paramSet, READOUT_MODE_PROP.getName(), getReadoutMode().name());
        Pio.addParam(factory, paramSet, NOD_DWELL_PROP.getName(), getNodDwellAsString());
        Pio.addParam(factory, paramSet, NOD_SETTLE_PROP.getName(), getNodSettleAsString());
        Pio.addParam(factory, paramSet, NOD_ORIENTATION_PROP.getName(), getNodOrientation().name());

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v = Pio.getValue(paramSet, DISPERSER_PROP.getName(), getDisperser().name());
        if (v != null) {
            setDisperserName(v);
            if (!v.equals(Disperser.MIRROR.name())) {
                String wavelength = Pio.getValue(paramSet, DISPERSER_LAMBDA_PROP.getName(), Double.toString(getDisperserLambda()));
                try {
                    setDisperserLambda(Double.parseDouble(wavelength));
                } catch (NumberFormatException ex) {
                    setDisperserLambda(DEFAULT_LAMBDA);
                }
            }
        }

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
        v = Pio.getValue(paramSet, TIME_PER_SAVESET_PROP.getName());
        if (v != null) {
            setTimePerSaveset(Double.parseDouble(v));
        }
        v = Pio.getValue(paramSet, CHOP_ANGLE_PROP.getName());
        if (v != null) {
            setChopAngle(Double.parseDouble(v));
        }
        v = Pio.getValue(paramSet, CHOP_THROW_PROP.getName());
        if (v != null) {
            setChopThrow(Double.parseDouble(v));
        }
        v = Pio.getValue(paramSet, DATA_MODE_PROP.getName());
        if (v != null) {
            _setDataMode(v);
        }
        v = Pio.getValue(paramSet, OBS_MODE_PROP.getName());
        if (v != null) {
            _setObsMode(v);
        }
        v = Pio.getValue(paramSet, WINDOW_WHEEL_PROP.getName());
        if (v != null) {
            _setWindowWheel(v);
        }
        v = Pio.getValue(paramSet, READOUT_MODE_PROP.getName());
        if (v != null) {
            _setReadoutMode(v);
        }
        v = Pio.getValue(paramSet, NOD_DWELL_PROP.getName());
        if (v != null) {
            setNodDwell(Double.parseDouble(v));
        }
        v = Pio.getValue(paramSet, NOD_SETTLE_PROP.getName());
        if (v != null) {
            setNodSettle(Double.parseDouble(v));
        }
        v = Pio.getValue(paramSet, NOD_ORIENTATION_PROP.getName());
        if (v != null) {
            _setNodOrientation(v);
        }
    }

    /**
     * Return the configuration for this component.
     */
    public ISysConfig getSysConfig() {
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);

        // Fill in the current values.
        sc.putParameter(DefaultParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion()));

        Disperser d = getDisperser();
        sc.putParameter(DefaultParameter.getInstance(DISPERSER_PROP.getName(), getDisperser()));

        if (d != Disperser.MIRROR) {
            sc.putParameter(DefaultParameter.getInstance(DISPERSER_LAMBDA_PROP.getName(), getDisperserLambda()));
        }

        sc.putParameter(DefaultParameter.getInstance(MASK_PROP.getName(), getMask()));
        sc.putParameter(DefaultParameter.getInstance(FILTER_PROP.getName(), getFilter()));

        sc.putParameter(DefaultParameter.getInstance(TOTAL_ON_SOURCE_TIME_PROP.getName(), getTimeOnSource()));
        sc.putParameter(DefaultParameter.getInstance(TIME_PER_SAVESET_PROP.getName(), getTimePerSaveset()));
        sc.putParameter(DefaultParameter.getInstance(CHOP_ANGLE_PROP.getName(), getChopAngle()));
        sc.putParameter(DefaultParameter.getInstance(CHOP_THROW_PROP.getName(), getChopThrow()));

        sc.putParameter(DefaultParameter.getInstance(POS_ANGLE_PROP.getName(), getPosAngleDegrees()));

        sc.putParameter(DefaultParameter.getInstance(DATA_MODE_PROP.getName(), getDataMode()));
        sc.putParameter(DefaultParameter.getInstance(OBS_MODE_PROP.getName(), getObsMode()));
        sc.putParameter(DefaultParameter.getInstance(WINDOW_WHEEL_PROP.getName(), getWindowWheel()));
        sc.putParameter(DefaultParameter.getInstance(READOUT_MODE_PROP.getName(), getReadoutMode()));
        sc.putParameter(DefaultParameter.getInstance(NOD_DWELL_PROP.getName(), getNodDwellAsString()));
        sc.putParameter(DefaultParameter.getInstance(NOD_SETTLE_PROP.getName(), getNodSettleAsString()));

        return sc;
    }


    /**
     * Return a list of InstConfigInfo objects describing the instrument's queryable configuration parameters.
     */
    public static List<InstConfigInfo> getInstConfigInfo() {
        final List<InstConfigInfo> configInfo = new LinkedList<>();
        configInfo.add(new InstConfigInfo(DISPERSER_PROP));
        configInfo.add(new InstConfigInfo(MASK_PROP));
        configInfo.add(new InstConfigInfo(FILTER_PROP));
        return configInfo;
    }

    /** TReCS doesn't have an OIWFS */
    public boolean hasOIWFS() {
        return false;
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    public static final ConfigInjector<String> WAVELENGTH_INJECTOR = ConfigInjector.create(
        new ObsWavelengthCalc3<Disperser, Filter, Double>() {
            public PropertyDescriptor descriptor1() { return DISPERSER_PROP; }
            public PropertyDescriptor descriptor2() { return FILTER_PROP; }
            public PropertyDescriptor descriptor3() { return DISPERSER_LAMBDA_PROP; }
            public String calcWavelength(Disperser d, Filter f, Double cwl) {
                return InstTReCS.calcWavelength(d, f, cwl);
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
