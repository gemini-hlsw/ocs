package edu.gemini.spModel.gemini.nici;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.config.injector.ConfigInjector;
import edu.gemini.spModel.config.injector.obswavelength.ObsWavelengthCalc3;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeProvider;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.obs.plannedtime.CommonStepCalculator;
import edu.gemini.spModel.obs.plannedtime.ExposureCalculator;
import edu.gemini.spModel.obs.plannedtime.OffsetOverheadCalculator;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTimeGroup;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.Category;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.StepCalculator;
import edu.gemini.spModel.obscomp.InstConfigInfo;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.PioNodeParent;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.telescope.FixedPositionAngleInstrument;
import edu.gemini.spModel.telescope.IssPort;
import edu.gemini.spModel.telescope.IssPortProvider;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.gemini.spModel.gemini.nici.NICIParams.*;

//
// SW: Note on the hacked Cass Rotator "Fixed" vs. "Follow" angle.  When in
// FIXED mode, the corresponding angle is a "cass rotator" angle but when in
// FOLLOW mode, the angle is a position angle.
//
// All instrument models are "SPInstObsComp" subclasses and inherit a
// pos angle property and value.  The OT and WDBA are pretty much set on using
// this base class value.  The position editor, for example, displays and edits
// this property when the user drags the science area around the base position.
// NICI uses the pos angle property to represent both the cass rotator angle
// and the position angle.
//
// For 2010B, somehow we need to track the two angles though and allow the user
// to get back to the CR angle when switching to FIXED and then to the true
// position angle when switching to FOLLOW.  We hack this here by storing a
// second not active angle.  There are properties for the fixed (CR) angle and
// the follow (pos angle).  The base class "pos angle" will hold the value
// associated with the current CR mode.  InstNICI will hold the value the
// value associated with the not active CR mode.  When the CR mode is switched,
// we also swap the not-active angle and the base class "pos angle".  Grim.
//
public final class InstNICI extends FixedPositionAngleInstrument implements PropertyProvider, GuideProbeProvider, IssPortProvider, StepCalculator {
    private static final Logger LOG = Logger.getLogger(InstNICI.class.getName());

    private static final long serialVersionUID = 2L;

    public static final SPComponentType SP_TYPE = SPComponentType.INSTRUMENT_NICI;

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    //Properties
    public static final PropertyDescriptor FOCAL_PLANE_MASK_PROP;
    public static final PropertyDescriptor PUPIL_MASK_PROP;
    public static final PropertyDescriptor CASS_ROTATOR_PROP;
    public static final PropertyDescriptor CASS_ROTATOR_FIXED_ANGLE_PROP;
    public static final PropertyDescriptor CASS_ROTATOR_FOLLOW_ANGLE_PROP;
    public static final PropertyDescriptor IMAGING_MODE_PROP;
    public static final PropertyDescriptor DICHROIC_WHEEL_PROP;
    public static final PropertyDescriptor CHANNEL1_FW_PROP;
    public static final PropertyDescriptor CHANNEL2_FW_PROP;
    public static final PropertyDescriptor EXPOSURES_PROP;
    public static final PropertyDescriptor WELL_DEPTH_PROP;
    public static final PropertyDescriptor DHS_MODE_PROP;
    public static final PropertyDescriptor CENTRAL_WAVELENGTH_PROP;

    public static final PropertyDescriptor PORT_PROP;

    //Engineering fields
    public static final PropertyDescriptor FOCS_PROP;
    public static final PropertyDescriptor PUPIL_IMAGER_PROP;
    public static final PropertyDescriptor SPIDER_MASK_PROP;
    public static final PropertyDescriptor SMR_ANGLE_PROP;

    public static final PropertyDescriptor COADDS_PROP;
    public static final PropertyDescriptor EXPOSURE_TIME_PROP;

    public static final String INSTRUMENT_NAME_PROP = "NICI";

    public static final double DEF_CENTRAL_WAVELENGTH = 1.6; // FR-7972
    public static final double DEF_SMR_TOLERANCE = 0.1;
    public static final double DEF_SMR_ANGLE = 0.0;

    /**
     * Constant number of seconds added to the NICI exposure overhead time per
     * observe.
     */
    public static final double EXPOSURE_OVERHEAD_CONSTANT = 8.0; // sec

    /**
     * Constant number of seconds added to the NICI exposure overhead time per
     * COADD per observe.
     */
    public static final double COADD_CONSTANT             = 0.38; // sec

    /**
     * Constant number of seconds added to overhead time whenever there is
     */
    public static final double CONFIG_CHANGE_COST         = 10.0; // sec

    /**
     * Offset distance that counts as "small" enough to incur a reduce offset
     * overhead.  Anything bigger than this will get the standard offset
     * overhead.
     */
    public static final Angle SMALL_OFFSET_DISTANCE      = new Angle(0.3, Angle.Unit.ARCSECS);

    /**
     * Time spent to configure the telescope for a "small" offset with NICI,
     * where small is defined as a less than the {@link #SMALL_OFFSET_DISTANCE}.
     */
    public static final double SMALL_OFFSET_OVERHEAD     = 1.0; // sec
    public static final CategorizedTime SMALL_OFFSET_OVERHEAD_CATEGORIZED_TIME =
            CategorizedTime.fromSeconds(Category.CONFIG_CHANGE, SMALL_OFFSET_OVERHEAD, OffsetOverheadCalculator.DETAIL);

    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter, boolean expert) {
        PropertyDescriptor pd;
        pd = PropertySupport.init(propName, InstNICI.class, query, iter);
        pd.setExpert(expert);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }

    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        return initProp(propName, query, iter, false);
    }

    static {
        final boolean query_yes = true;
        final boolean iter_yes = true;
        final boolean query_no = false;
        final boolean iter_no = false;
        FOCAL_PLANE_MASK_PROP       = initProp(FocalPlaneMask.KEY.getName(), query_yes, iter_yes);
        PUPIL_MASK_PROP             = initProp(PupilMask.KEY.getName(), query_yes, iter_yes);
        CASS_ROTATOR_PROP           = initProp("cassRotator", query_yes, iter_no);
        CASS_ROTATOR_FIXED_ANGLE_PROP  = initProp("cassRotatorFixedAngle", query_no, iter_no);
        CASS_ROTATOR_FOLLOW_ANGLE_PROP = initProp("cassRotatorFollowAngle", query_no, iter_no);
        IMAGING_MODE_PROP           = initProp("imagingMode", query_yes, iter_yes);
        DICHROIC_WHEEL_PROP         = initProp(DichroicWheel.KEY.getName(), query_yes, iter_yes);
        CHANNEL1_FW_PROP            = initProp(Channel1FW.KEY.getName(), query_yes, iter_yes);
        //SCT-231. change "Channel 1" and "Channel 2" filters to "Red" and "Blue" filters in
        //the NICI sequencer and in the OT browser (to match the static component)
        CHANNEL1_FW_PROP.setDisplayName("Filter Red Channel");
        CHANNEL2_FW_PROP            = initProp(Channel2FW.KEY.getName(), query_yes, iter_yes);
        CHANNEL2_FW_PROP.setDisplayName("Filter Blue Channel");
        EXPOSURES_PROP              = initProp("exposures", query_no, iter_no);
        WELL_DEPTH_PROP             = initProp("wellDepth", query_yes, iter_yes);
        CENTRAL_WAVELENGTH_PROP     = initProp("centralWavelength", query_no, iter_no);
        COADDS_PROP                 = initProp("coadds", query_no, iter_yes);
        EXPOSURE_TIME_PROP          = initProp("exposureTime", query_no, iter_yes);
        PORT_PROP                   = initProp("issPort", query_no, iter_no);

        //Engineering fields
        FOCS_PROP = initProp(Focs.KEY.getName(), query_yes, iter_yes, true);
        FOCS_PROP.setDisplayName("FOCS");
        PUPIL_IMAGER_PROP = initProp(PupilImager.KEY.getName(), query_yes, iter_yes, true);
        SPIDER_MASK_PROP = initProp("spiderMask", query_yes, iter_yes, true);
        SMR_ANGLE_PROP = initProp("SMRAngle", query_no, iter_yes, true);
        DHS_MODE_PROP               = initProp("dhsMode", query_yes, iter_yes, true);
        DHS_MODE_PROP.setDisplayName("DHS Mode");
    }


//    private double centralWavelength = DEF_CENTRAL_WAVELENGTH;

    private int exposureNumber = 1;
    private double smrAngle = DEF_SMR_ANGLE;

    private NICIParams.FocalPlaneMask focalPlaneMask = NICIParams.FocalPlaneMask.DEFAULT;
    private NICIParams.PupilMask pupilMask = NICIParams.PupilMask.DEFAULT;
    private NICIParams.CassRotator cassRotator = NICIParams.CassRotator.DEFAULT;
    private Angle alternateCrAngle = cassRotator.opposite().defaultAngle();
    private NICIParams.ImagingMode imagingMode = NICIParams.ImagingMode.DEFAULT;
    private NICIParams.DichroicWheel dichroicWheel = NICIParams.DichroicWheel.DEFAULT;
    private NICIParams.Channel1FW channel1Fw = NICIParams.Channel1FW.DEFAULT;
    private NICIParams.Channel2FW channel2Fw = NICIParams.Channel2FW.DEFAULT;
    private NICIParams.WellDepth wellDepth = NICIParams.WellDepth.DEFAULT;
    private NICIParams.DHSMode dhsMode = NICIParams.DHSMode.DEFAULT;

    private IssPort port = IssPort.UP_LOOKING; // default for 2009B

    //engineering fields
    private NICIParams.Focs focs = NICIParams.Focs.DEFAULT;
    private NICIParams.PupilImager pupilImager = NICIParams.PupilImager.DEFAULT;
    private NICIParams.SpiderMask spiderMask = NICIParams.SpiderMask.DEFAULT;

    private static final String _VERSION =  "2009B-1";

    public InstNICI() {
        super(InstNICI.SP_TYPE);
        setVersion(_VERSION);

        // Need to initialize the position angle to the default value for the
        // chosen cass rotator option.
        //
        // Bad form to call an overridable method from a constructor.
        // InstNICI is final though and we don't override setPosAngleDegrees.
        setPosAngleDegrees(cassRotator.defaultAngle().toDegrees().getMagnitude());
    }

    public Object clone() {
        return super.clone();
    }

    @Override
    public Set<Site> getSite() {
        return Site.SET_GS;
    }

    public String getPhaseIResourceName() {
        return "gemNICI";
    }

    public double getSetupTime(ISPObservation obs) {
        // OT-726. Nici overhead is 30 minutes
        //return 30 * 60;
        // SCI-0107: set NICI setup time to 10 minutes
        return 10 * 60;
    }

    // Predicate that leaves all CategorizedTime except for the offset overhead.
    private static final PredicateOp<CategorizedTime> RM_OFFSET_OVERHEAD = ct -> !((ct.category == Category.CONFIG_CHANGE) &&
             OffsetOverheadCalculator.DETAIL.equals(ct.detail));

    // Get correct offset overhead in the common group.  If a small offset,
    // we remove the offset overhead put there by the common step calculator
    // and we add the small offset overhead.
    private CategorizedTimeGroup commonGroup(Config cur, Option<Config> prev) {
        CategorizedTimeGroup ctg = CommonStepCalculator.instance.calc(cur, prev);
        return (offsetAspect(cur, prev) == OffsetAspect.SMALL) ?
            ctg.filter(RM_OFFSET_OVERHEAD).add(SMALL_OFFSET_OVERHEAD_CATEGORIZED_TIME) :
            ctg;
    }

    @Override public CategorizedTimeGroup calc(Config cur, Option<Config> prev) {
        Collection<CategorizedTime> times = new ArrayList<>();

        // Add time for any configuration change.
        if (PlannedTime.isUpdated(cur, prev,
                DichroicWheel.KEY, Focs.KEY, FocalPlaneMask.KEY,
                PupilImager.KEY, PupilMask.KEY, Channel1FW.KEY, Channel2FW.KEY)) {
            times.add(CategorizedTime.fromSeconds(Category.CONFIG_CHANGE, CONFIG_CHANGE_COST, "Instrument"));
        }

        // Calculate the exposure time using the formula in SCI-0128
        double exposureTime = ExposureCalculator.instance.exposureTimeSec(cur);
        int    coadds       = ExposureCalculator.instance.coadds(cur);
        double coaddTime    = exposureTime + COADD_CONSTANT;
        double totalExpTime = EXPOSURE_OVERHEAD_CONSTANT + coadds*coaddTime;
        times.add(ExposureCalculator.instance.categorize(totalExpTime));

        // Add the common items (with offset overhead correction)
        return commonGroup(cur, prev).addAll(times);
    }

    public enum OffsetAspect {
        NONE,
        SMALL,
        BIG,
    }

    public static OffsetAspect offsetAspect(Config cur, Option<Config> prev) {
        Offset curOff  = OffsetOverheadCalculator.instance.extract(cur);
        Offset prevOff = OffsetOverheadCalculator.instance.extract(prev);
        if (curOff.equals(prevOff)) return OffsetAspect.NONE;

        Angle d = curOff.distance(prevOff);
        return d.toArcsecs().getMagnitude() <= SMALL_OFFSET_DISTANCE.toArcsecs().getMagnitude() ?
               OffsetAspect.SMALL : OffsetAspect.BIG;
    }

    public double[] getScienceArea() {
        return new double[] {18, 18};
    }

    public ParamSet getParamSet(final PioFactory factory) {
        final ParamSet paramSet = super.getParamSet(factory);
        Pio.addParam(factory, paramSet, FOCAL_PLANE_MASK_PROP.getName(), focalPlaneMask.name());
        Pio.addParam(factory, paramSet, PUPIL_MASK_PROP.getName(), pupilMask.name());
        Pio.addParam(factory, paramSet, CASS_ROTATOR_PROP.getName(), cassRotator.name());

        Pio.addDoubleParam(factory, paramSet, CASS_ROTATOR_FIXED_ANGLE_PROP.getName(), getCassRotatorFixedAngle().toDegrees().getMagnitude());
        Pio.addDoubleParam(factory, paramSet, CASS_ROTATOR_FOLLOW_ANGLE_PROP.getName(), getCassRotatorFollowAngle().toDegrees().getMagnitude());

        Pio.addParam(factory, paramSet, IMAGING_MODE_PROP.getName(), imagingMode.name());
        Pio.addParam(factory, paramSet, DICHROIC_WHEEL_PROP.getName(), dichroicWheel.name());
        Pio.addParam(factory, paramSet, CHANNEL1_FW_PROP.getName(), channel1Fw.name());
        Pio.addParam(factory, paramSet, CHANNEL2_FW_PROP.getName(), channel2Fw.name());
        Pio.addParam(factory, paramSet, EXPOSURES_PROP.getName(), Integer.toString(exposureNumber));
        Pio.addParam(factory, paramSet, WELL_DEPTH_PROP.getName(), wellDepth.name());
        Pio.addParam(factory, paramSet, DHS_MODE_PROP.getName(), dhsMode.name());

        Pio.addParam(factory, paramSet, PORT_PROP, port.name());

        //Engineering fields
        Pio.addParam(factory, paramSet, FOCS_PROP.getName(), focs.name());
        Pio.addParam(factory, paramSet, PUPIL_IMAGER_PROP.getName(), pupilImager.name());
        Pio.addParam(factory, paramSet, SPIDER_MASK_PROP.getName(), spiderMask.name());
        Pio.addParam(factory, paramSet, SMR_ANGLE_PROP.getName(), Double.toString(smrAngle));

//        Pio.addParam(factory, paramSet, CENTRAL_WAVELENGTH_PROP.getName(), Double.toString(centralWavelength));
        return paramSet;
    }

    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v;
        v = Pio.getValue(paramSet, FOCAL_PLANE_MASK_PROP.getName());
        if (v != null) {
            // SCT-295
            if (NICIParams.FocalPlaneMask.OPEN.name().equals(v)) {
                setFocalPlaneMask(NICIParams.FocalPlaneMask.CLEAR);
            } else {
                setFocalPlaneMask(NICIParams.FocalPlaneMask.valueOf(v));
            }
        }

        v = Pio.getValue(paramSet, PUPIL_MASK_PROP.getName());
        if (v != null) {
            setPupilMask(NICIParams.PupilMask.valueOf(v));
        }

        v = Pio.getValue(paramSet, CASS_ROTATOR_PROP.getName());
        if (v != null) {
            setCassRotator(NICIParams.CassRotator.valueOf(v));
        }

        Angle defAngle = CassRotator.FIXED.defaultAngle();
        double a = Pio.getDoubleValue(paramSet, CASS_ROTATOR_FIXED_ANGLE_PROP.getName(), defAngle.getMagnitude());
        if (a == defAngle.getMagnitude()) {
            // I know we're comparing double values but this is only an
            // optimization here anyway.
            setCassRotatorFixedAngle(defAngle);
        } else {
            setCassRotatorFixedAngle(new Angle(a, Angle.Unit.DEGREES));
        }

        defAngle = CassRotator.FOLLOW.defaultAngle();
        a = Pio.getDoubleValue(paramSet, CASS_ROTATOR_FOLLOW_ANGLE_PROP.getName(), defAngle.getMagnitude());
        if (a == defAngle.getMagnitude()) {
            // I know we're comparing double values but this is only an
            // optimization here anyway.
            setCassRotatorFollowAngle(defAngle);
        } else {
            setCassRotatorFollowAngle(new Angle(a, Angle.Unit.DEGREES));
        }

        v = Pio.getValue(paramSet, IMAGING_MODE_PROP.getName());
        if (v != null) {
            setImagingMode(NICIParams.ImagingMode.valueOf(v));
        }

        v = Pio.getValue(paramSet, DICHROIC_WHEEL_PROP.getName());
        if (v != null) {
            setDichroicWheel(NICIParams.DichroicWheel.valueOf(v));
        }

        v = Pio.getValue(paramSet, CHANNEL1_FW_PROP.getName());
        if (v != null) {
            setChannel1Fw(NICIParams.Channel1FW.valueOf(v));
        }

        v = Pio.getValue(paramSet, CHANNEL2_FW_PROP.getName());
        if (v != null) {
            setChannel2Fw(NICIParams.Channel2FW.valueOf(v));
        }

        setExposures(getIntValue(paramSet, EXPOSURES_PROP.getName(), exposureNumber));

        v = Pio.getValue(paramSet, WELL_DEPTH_PROP.getName());
        if (v == null) {
            // older NICI programs used "BiasVoltage".
            v = Pio.getValue(paramSet, "biasVoltage");
            if (v != null) {
                NICIParams.BiasVoltage biasVoltage = NICIParams.BiasVoltage.valueOf(v);
                setWellDepth(biasVoltage.wellDepth());
            }
        } else {
            setWellDepth(NICIParams.WellDepth.valueOf(v));
        }

        v = Pio.getValue(paramSet, DHS_MODE_PROP.getName());
        if (v != null) {
            setDhsMode(NICIParams.DHSMode.valueOf(v));
        }

        v = Pio.getValue(paramSet, PORT_PROP);
        if (v == null) {
            // Old NICI programs w/o an ISS port option were using NICI on the
            // side-looking port.
            setIssPort(IssPort.SIDE_LOOKING);
        } else {
            setIssPort(IssPort.valueOf(v));
        }


        //Now, the engineering parameters
        v = Pio.getValue(paramSet, FOCS_PROP.getName());
        if (v != null) {
            setFocs(NICIParams.Focs.valueOf(v));
        }

        v = Pio.getValue(paramSet, PUPIL_IMAGER_PROP.getName());
        if (v != null) {
            setPupilImager(NICIParams.PupilImager.valueOf(v));
        }

        v = Pio.getValue(paramSet, SPIDER_MASK_PROP.getName());
        if (v != null) {
            setSpiderMask(NICIParams.SpiderMask.valueOf(v));
        }

        setSMRAngle(getDoubleValue(paramSet, SMR_ANGLE_PROP.getName(), smrAngle));
//        setCentralWavelength(getDoubleValue(paramSet, CENTRAL_WAVELENGTH_PROP.getName(), centralWavelength));
    }

    public ISysConfig getSysConfig() {
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
        sc.putParameter(DefaultParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion()));
        sc.putParameter(DefaultParameter.getInstance(InstConstants.EXPOSURE_TIME_PROP, getExposureTime()));
        sc.putParameter(DefaultParameter.getInstance(InstConstants.COADDS_PROP, getCoadds()));

        sc.putParameter(DefaultParameter.getInstance(FOCAL_PLANE_MASK_PROP.getName(), focalPlaneMask));
        sc.putParameter(DefaultParameter.getInstance(PUPIL_MASK_PROP.getName(), pupilMask));
        sc.putParameter(DefaultParameter.getInstance(CASS_ROTATOR_PROP.getName(), cassRotator));
        sc.putParameter(DefaultParameter.getInstance(IMAGING_MODE_PROP.getName(), imagingMode));
        sc.putParameter(DefaultParameter.getInstance(DICHROIC_WHEEL_PROP.getName(), dichroicWheel));
        sc.putParameter(DefaultParameter.getInstance(CHANNEL1_FW_PROP.getName(), channel1Fw));
        sc.putParameter(DefaultParameter.getInstance(CHANNEL2_FW_PROP.getName(), channel2Fw));
        sc.putParameter(DefaultParameter.getInstance(EXPOSURES_PROP.getName(), exposureNumber));
        sc.putParameter(DefaultParameter.getInstance(WELL_DEPTH_PROP.getName(), wellDepth));
        sc.putParameter(DefaultParameter.getInstance(DHS_MODE_PROP.getName(), dhsMode));

        sc.putParameter(DefaultParameter.getInstance(PORT_PROP, getIssPort()));

        sc.putParameter(DefaultParameter.getInstance(CENTRAL_WAVELENGTH_PROP.getName(), getCentralWavelength()));

        //Engineering fields
        //Only place the non-default parameters
        // Update: according to SCT-295, they want all the parameters, all the
        // time...
//        if (focs != NICIParams.Focs.DEFAULT) {
            sc.putParameter(DefaultParameter.getInstance(FOCS_PROP.getName(), focs));
//        }
//        if (pupilImager != NICIParams.PupilImager.DEFAULT) {
            sc.putParameter(DefaultParameter.getInstance(PUPIL_IMAGER_PROP.getName(), pupilImager));
//        }
//        if (spiderMask != NICIParams.SpiderMask.DEFAULT) {
            sc.putParameter(DefaultParameter.getInstance(SPIDER_MASK_PROP.getName(), spiderMask));
//        }
//        if (!equalsDouble(smrAngle, DEF_SMR_ANGLE)) {
            sc.putParameter(DefaultParameter.getInstance(SMR_ANGLE_PROP.getName(), smrAngle));
//        }
        return sc;
    }

    /**
     * This method is called by the OT Browser to determine how to query the instrument
     */
    public static List<InstConfigInfo> getInstConfigInfo() {
        final List<InstConfigInfo> configInfo = new LinkedList<>();
        configInfo.add(new InstConfigInfo(FOCAL_PLANE_MASK_PROP));
        configInfo.add(new InstConfigInfo(PUPIL_MASK_PROP));
        configInfo.add(new InstConfigInfo(CASS_ROTATOR_PROP));
        configInfo.add(new InstConfigInfo(IMAGING_MODE_PROP));
        configInfo.add(new InstConfigInfo(DICHROIC_WHEEL_PROP));
        configInfo.add(new InstConfigInfo(CHANNEL1_FW_PROP));
        configInfo.add(new InstConfigInfo(CHANNEL2_FW_PROP));
        configInfo.add(new InstConfigInfo(WELL_DEPTH_PROP));
        configInfo.add(new InstConfigInfo(DHS_MODE_PROP));
        return configInfo;
    }

    public NICIParams.FocalPlaneMask getFocalPlaneMask() {
        return focalPlaneMask;
    }

    public void setFocalPlaneMask(NICIParams.FocalPlaneMask fpm) {
        Object prev = this.focalPlaneMask;
        this.focalPlaneMask = fpm;
        firePropertyChange(FOCAL_PLANE_MASK_PROP.getName(), prev, this.focalPlaneMask);
    }

    public double getCentralWavelength() {
        ImagingMode mode = getImagingMode();
        Channel1FW  red  = getChannel1Fw();
        Channel2FW  blue = getChannel2Fw();

        return ((mode == null) || (red == null) || (blue == null)) ?
            DEF_CENTRAL_WAVELENGTH :
            calcWavelength(mode, red, blue);
    }

    public static double calcWavelength(ImagingMode mode, Channel1FW red, Channel2FW blue) {
        // Using the imaging mode, we can get the "metaconfig" associated with
        // it, which contains the red and blue filter wheel settings.
        // Assuming we get that information, we will ignore any independently
        // set explicit red and or blue filter wheel values.
        ImagingModeMetaconfig meta = ImagingModeMetaconfig.getMetaConfig(mode);

        // Unfortunately not all imaging modes have a meta config -- in
        // particular "manual" mode.  In that case, we need the explicit red
        // and blue filters.
        Channel1FW selectedRed;
        Channel2FW selectedBlue;
        if (meta == null) {
            selectedRed  = red;
            selectedBlue = blue;
        } else {
            selectedRed  = meta.getChannel1Fw();
            selectedBlue = meta.getChannel2Fw();
        }

        double res = selectedRed.centralWavelength();
        if (Double.isNaN(res)) {
            res = selectedBlue.centralWavelength();
            if (Double.isNaN(res)) {
                res = DEF_CENTRAL_WAVELENGTH;
            }
        }

        return res;
    }

    public static ConfigInjector<String> WAVELENGTH_INJECTOR = ConfigInjector.create(
        new ObsWavelengthCalc3<ImagingMode, Channel1FW, Channel2FW>() {

            public PropertyDescriptor descriptor1() { return IMAGING_MODE_PROP; }
            public PropertyDescriptor descriptor2() { return CHANNEL1_FW_PROP; }
            public PropertyDescriptor descriptor3() { return CHANNEL2_FW_PROP; }

            public String calcWavelength(ImagingMode mode, Channel1FW red, Channel2FW blue) {
                return String.valueOf(InstNICI.calcWavelength(mode, red, blue));
            }
        }
    );

    /**
     * Sets the central wavelength and updates the echelle settings.
     *
     * @param lambdaNm wavelength in nanometers
     */
    public void setCentralWavelength(double lambdaNm) {
// needed by the java beans interface for properties
//        double prev = this.centralWavelength;
//        this.centralWavelength = lambdaNm;
//        try {
//            firePropertyChange(CENTRAL_WAVELENGTH_PROP.getName(), prev, this.centralWavelength);
//        } catch (IllegalArgumentException iae) {
//            this.centralWavelength = prev;
//            throw iae;
//        }
    }

    public NICIParams.PupilMask getPupilMask() {
        return pupilMask;
    }

    public void setPupilMask(NICIParams.PupilMask pupilMask) {
        Object prev = this.pupilMask;
        this.pupilMask = pupilMask;
        firePropertyChange(PUPIL_MASK_PROP.getName(), prev, this.pupilMask);
    }

    public NICIParams.CassRotator getCassRotator() {
        return cassRotator;
    }

    public void setCassRotator(NICIParams.CassRotator cr) {
        if (cr == this.cassRotator) return;  // no change made

        Object prev = this.cassRotator;
        this.cassRotator = cr;

        // Swap the alternate and current pos angles.
        Angle alternate = new Angle(getPosAngleDegrees(), Angle.Unit.DEGREES);
        setPosAngleDegrees(alternateCrAngle.toDegrees().getMagnitude());
        alternateCrAngle = alternate;

        firePropertyChange(CASS_ROTATOR_PROP.getName(), prev, this.cassRotator);
    }

    private Angle getCurCrAngle() {
        // see note at top of file
        return new Angle(getPosAngleDegrees(), Angle.Unit.DEGREES);
    }

    public Angle getCassRotatorFixedAngle() {
        // see note at top of file
        return (cassRotator == CassRotator.FIXED) ? getCurCrAngle() : alternateCrAngle;
    }

    public Angle getCassRotatorFollowAngle() {
        // see note at top of file
        return (cassRotator == CassRotator.FOLLOW) ? getCurCrAngle() : alternateCrAngle;
    }

    private void updateCrAngle(Angle oldVal, Angle newVal, CassRotator cr, PropertyDescriptor prop) {
        if (oldVal.compareToAngle(newVal) == 0) return;

        if (cassRotator == cr) {
            setPosAngle(newVal.toDegrees().getMagnitude());
        } else {
            alternateCrAngle = newVal;
        }
        firePropertyChange(prop.getName(), oldVal, newVal);
    }

    public void setCassRotatorFixedAngle(Angle angle) {
        // see note at top of file
        Angle prevVal = getCassRotatorFixedAngle();
        updateCrAngle(prevVal, angle, CassRotator.FIXED, CASS_ROTATOR_FIXED_ANGLE_PROP);
    }

    public void setCassRotatorFollowAngle(Angle angle) {
        // see note at top of file
        Angle prevVal = getCassRotatorFollowAngle();
        updateCrAngle(prevVal, angle, CassRotator.FOLLOW, CASS_ROTATOR_FOLLOW_ANGLE_PROP);
    }

    public NICIParams.ImagingMode getImagingMode() {
        return imagingMode;
    }

    public void setImagingMode(NICIParams.ImagingMode imagingMode) {
        if (imagingMode == null) {
            LOG.log(Level.WARNING, "Tried to assign a null imaging mode", new RuntimeException());
            return;
        }

        Object prev = this.imagingMode;
        this.imagingMode = imagingMode;
        if (this.imagingMode != NICIParams.ImagingMode.MANUAL) {
            setImageModeMetaconfig(this.imagingMode);
        }
        firePropertyChange(IMAGING_MODE_PROP.getName(), prev, this.imagingMode);
    }


    public NICIParams.DichroicWheel getDichroicWheel() {
        return dichroicWheel;
    }

    public void setDichroicWheel(NICIParams.DichroicWheel dw) {
        Object prev = this.dichroicWheel;
        this.dichroicWheel = dw;
        firePropertyChange(DICHROIC_WHEEL_PROP.getName(), prev, this.dichroicWheel);
    }

    public NICIParams.Channel1FW getChannel1Fw() {
        return channel1Fw;
    }

    public void setChannel1Fw(NICIParams.Channel1FW channel1Fw) {
        if (channel1Fw == null) {
            LOG.log(Level.WARNING, "Tried to assign a null channel1Fw", new RuntimeException());
            return;
        }
        Object prev = this.channel1Fw;
        this.channel1Fw = channel1Fw;
        firePropertyChange(CHANNEL1_FW_PROP.getName(), prev, this.channel1Fw);
    }


    public NICIParams.Channel2FW getChannel2Fw() {
        return channel2Fw;
    }

    public void setChannel2Fw(NICIParams.Channel2FW channel2Fw) {
        if (channel2Fw == null) {
            LOG.log(Level.WARNING, "Tried to assign a null channel2Fw", new RuntimeException());
            return;
        }
        Object prev = this.channel2Fw;
        this.channel2Fw = channel2Fw;
        firePropertyChange(CHANNEL2_FW_PROP.getName(), prev, this.channel2Fw);
    }

    public int getExposures() {
        return exposureNumber;
    }

    public void setExposures(int number) {
        int prev = this.exposureNumber;
        this.exposureNumber = number;
        try {
            firePropertyChange(EXPOSURES_PROP.getName(), prev, this.exposureNumber);
        } catch (IllegalArgumentException iae) {
            this.exposureNumber = prev;
            throw iae;
        }
    }

    public NICIParams.WellDepth getWellDepth() {
        return wellDepth;
    }

    public void setWellDepth(NICIParams.WellDepth wellDepth) {
        Object prev = this.wellDepth;
        this.wellDepth = wellDepth;
        firePropertyChange(WELL_DEPTH_PROP.getName(), prev, this.wellDepth);
    }

    public NICIParams.DHSMode getDhsMode() {
        return dhsMode;
    }

    public void setDhsMode(NICIParams.DHSMode mode) {
        Object prev = dhsMode;
        dhsMode = mode;
        firePropertyChange(DHS_MODE_PROP.getName(), prev, dhsMode);
    }

    /**
      * Get the ISS Port
      */
     public IssPort getIssPort() {
         if (port == null) return IssPort.DEFAULT;
         return port;
     }

     /**
      * Set the Port.
      */
     public void setIssPort(IssPort newValue) {
         IssPort oldValue = getIssPort();
         if (oldValue != newValue) {
             port = newValue;
             firePropertyChange(PORT_PROP, oldValue, newValue);
         }
     }

    //Engineering getter and setters....

    public NICIParams.Focs getFocs() {
        return focs;
    }

    public void setFocs(NICIParams.Focs f) {
        Object prev = focs;
        focs = f;
        firePropertyChange(FOCS_PROP.getName(), prev, focs);
    }

    public NICIParams.PupilImager getPupilImager() {
        return pupilImager;
    }

    public void setPupilImager(NICIParams.PupilImager value) {
        Object prev = pupilImager;
        pupilImager = value;
        firePropertyChange(PUPIL_IMAGER_PROP.getName(), prev, pupilImager);
    }


    public NICIParams.SpiderMask getSpiderMask() {
        return spiderMask;
    }

    public void setSpiderMask(NICIParams.SpiderMask value) {
        Object prev = spiderMask;
        spiderMask = value;
        firePropertyChange(SPIDER_MASK_PROP.getName(), prev, spiderMask);
    }

    public double getSMRAngle() {
        return smrAngle;
    }

    public void setSMRAngle(double angle) {
        double prev = this.smrAngle;
        this.smrAngle = angle;
        try {
            firePropertyChange(SMR_ANGLE_PROP.getName(), prev, this.smrAngle);
        } catch (IllegalArgumentException iae) {
            this.smrAngle = prev;
            throw iae;
        }
    }

    private double getDoubleValue(PioNodeParent context, String path, double nValue) {
        String val = Pio.getValue(context, path);
        return val != null ? Double.parseDouble(val) : nValue;
    }


    private int getIntValue(PioNodeParent context, String path, int nValue) {
        String val = Pio.getValue(context, path);
        return val != null ? Integer.parseInt(val) : nValue;
    }


    // Ultimately this is just an ADT and needs to have equality defined in terms of
    // the equality of all fields. At least for now. This is important for Undo and
    // possibly for other things.
    /*
    public boolean equals(Object obj) {
        if ((obj instanceof InstNICI) && super.equals(obj)) {
            InstNICI other = (InstNICI) obj;
            return other.focalPlaneMask == focalPlaneMask &&
                    other.pupilMask == pupilMask &&
                    other.cassRotator == cassRotator &&
                    other.imagingMode == imagingMode &&
                    other.dichroicWheel == dichroicWheel &&
                    other.channel1Fw == channel1Fw &&
                    other.channel2Fw == channel2Fw &&
                    other.exposureNumber == exposureNumber &&
                    other.wellDepth == wellDepth &&
                    other.dhsMode == dhsMode &&
                    other.focs == focs &&
                    other.pupilImager == pupilImager &&
                    other.spiderMask == spiderMask &&
                    other.smrAngle == smrAngle;
        }
        return false;
    }


    public int hashCode() {
        int hash = focalPlaneMask.hashCode();
        hash = 37*hash + pupilMask.hashCode();
        hash = 37*hash + cassRotator.hashCode();
        hash = 37*hash + imagingMode.hashCode();
        hash = 37*hash + dichroicWheel.hashCode();
        hash = 37*hash + channel1Fw.hashCode();
        hash = 37*hash + channel2Fw.hashCode();
        hash = 37*hash + exposureNumber;
        hash = 37*hash + wellDepth.hashCode();
        hash = 37*hash + dhsMode.hashCode();
        hash = 37*hash + focs.hashCode();
        hash = 37*hash + pupilImager.hashCode();
        hash = 37*hash + spiderMask.hashCode();
        long f = Double.doubleToLongBits(smrAngle);
        hash = 37*hash + (int)(f ^ (f >>> 32));
        return hash;
    }
    */

    //Updates the properties associated when the Image Mode changes.
    private void setImageModeMetaconfig(NICIParams.ImagingMode imageMode) {
        NICIParams.ImagingModeMetaconfig metaconfig = NICIParams.ImagingModeMetaconfig.getMetaConfig(imageMode);
        setDichroicWheel(metaconfig.getDw());
        setChannel1Fw(metaconfig.getChannel1Fw());
        setChannel2Fw(metaconfig.getChannel2Fw());
        setPupilImager(metaconfig.getPupilImager());
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    public boolean offsetsAllowed(SPComponentType type, Set<SPComponentType> set) {
        return false; /// NICI has a special offset iterator
    }

    private static Collection<GuideProbe> GUIDE_PROBES = GuideProbeUtil.instance.createCollection(NiciOiwfsGuideProbe.instance);

    public Collection<GuideProbe> getGuideProbes() {
        return GUIDE_PROBES;
    }

    // Handle pre-2010B data objects which don't have a cass rotator angle
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (alternateCrAngle == null) alternateCrAngle = cassRotator.opposite().defaultAngle();
    }

    private static final Angle PWFS1_VIG = Angle.arcmins(4.8);
    private static final Angle PWFS2_VIG = Angle.arcmins(4.3);

    @Override public Angle pwfs1VignettingClearance() { return PWFS1_VIG; }
    @Override public Angle pwfs2VignettingClearance() { return PWFS2_VIG; }
}
