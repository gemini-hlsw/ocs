package edu.gemini.spModel.gemini.niri;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.ao.AOConstants;
import edu.gemini.spModel.config.ConfigPostProcessor;
import edu.gemini.spModel.config.injector.ConfigInjector;
import edu.gemini.spModel.config.injector.obswavelength.ObsWavelengthCalc2;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.calunit.calibration.CalConfigBuilderUtil;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKeyProvider;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.CalibrationKeyImpl;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyNiri;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.gemini.niri.Niri.*;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeProvider;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.obs.plannedtime.CommonStepCalculator;
import edu.gemini.spModel.obs.plannedtime.ExposureCalculator;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTimeGroup;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.Category;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.ItcOverheadProvider;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.StepCalculator;
import edu.gemini.spModel.obscomp.InstConfigInfo;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.seqcomp.SeqConfigNames;

import java.beans.PropertyDescriptor;

import java.util.*;

import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY;

/**
 * The NIRI instrument.
 */
public final class InstNIRI extends SPInstObsComp implements PropertyProvider, GuideProbeProvider, StepCalculator, CalibrationKeyProvider, ConfigPostProcessor, ItcOverheadProvider {

    // for serialization
    private static final long serialVersionUID = 2L;

    /**
     * Updated version for changes in 2008B *
     */
    private static final String VERSION = "2007B-1";

    /**
     * This obs component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.INSTRUMENT_NIRI;

    public static final ISPNodeInitializer<ISPObsComponent, InstNIRI> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new InstNIRI(), c -> new InstNIRICB(c));

    public static final double DEF_EXPOSURE_TIME = 60.0;
    public static int DEF_FASTMODE_EXPOSURES = 1;
    public static final String INSTRUMENT_NAME_PROP = "NIRI";

    // Properties
    public static final PropertyDescriptor BEAM_SPLITTER_PROP;
    public static final PropertyDescriptor CAMERA_PROP;
    public static final PropertyDescriptor COADDS_PROP;
    public static final PropertyDescriptor DISPERSER_PROP;
    public static final PropertyDescriptor EXPOSURE_TIME_PROP;
    public static final PropertyDescriptor FILTER_PROP;
    public static final PropertyDescriptor FOCUS_PROP;
    public static final PropertyDescriptor MASK_PROP;
    public static final PropertyDescriptor POS_ANGLE_PROP;
    public static final ItemKey DISPERSER_KEY = new ItemKey(INSTRUMENT_KEY, "disperser");
    public static final PropertyDescriptor READ_MODE_PROP;
    public static final PropertyDescriptor BUILTIN_ROI_PROP;
    public static final PropertyDescriptor WELL_DEPTH_PROP;
    public static final PropertyDescriptor FAST_MODE_PROP;
    public static final ItemKey FAST_MODE_KEY = new ItemKey(INSTRUMENT_KEY, "fastModeExposures");

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        PropertyDescriptor pd;
        pd = PropertySupport.init(propName, InstNIRI.class, query, iter);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }

    static {
        final boolean query_yes = true;
        final boolean iter_yes = true;
        final boolean query_no = false;
        final boolean iter_no = false;

        BEAM_SPLITTER_PROP = initProp("beamSplitter", query_yes, iter_yes);
        BUILTIN_ROI_PROP = initProp(BuiltinROI.KEY.getName(), query_no, iter_yes);
        CAMERA_PROP = initProp("camera", query_yes, iter_yes);
        COADDS_PROP = initProp("coadds", query_no, iter_yes);
        DISPERSER_PROP = initProp("disperser", query_yes, iter_yes);
        EXPOSURE_TIME_PROP = initProp("exposureTime", query_no, iter_yes);
        FILTER_PROP = initProp(Filter.KEY.getName(), query_yes, iter_yes);
        FOCUS_PROP = initProp("focus", query_no, iter_yes);
        MASK_PROP = initProp("mask", query_yes, iter_yes);
        POS_ANGLE_PROP = initProp("posAngle", query_no, iter_no);
        READ_MODE_PROP = initProp(ReadMode.KEY.getName(), query_yes, iter_yes);
        WELL_DEPTH_PROP = initProp("wellDepth", query_no, iter_no);
        FAST_MODE_PROP = initProp(FAST_MODE_KEY.getName(), query_no, iter_yes);
    }


    private Camera _camera = Camera.DEFAULT;
    private Disperser _disperser = Disperser.DEFAULT;
    private Mask _mask = Mask.DEFAULT;
    private Filter _filter = Filter.DEFAULT;
    private BeamSplitter _splitter = BeamSplitter.DEFAULT;
    private ReadMode _readMode = ReadMode.DEFAULT;
    private WellDepth _wellDepth = WellDepth.DEFAULT;
    private BuiltinROI _builtinROI = BuiltinROI.DEFAULT;
    // The number of fast mode reads
    private int _fastModeExposures = DEF_FASTMODE_EXPOSURES;

    public InstNIRI() {
        super(SP_TYPE);
        // Override the default exposure time
        _exposureTime = DEF_EXPOSURE_TIME;
        setVersion(VERSION);
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
        return "gemNIRI";
    }

    public enum Mode {
        imaging,
        spectroscopy,;

        private static Mode getMode(InstNIRI niri) {
            return niri._disperser == Disperser.NONE ? imaging : spectroscopy;
        }
    }

    public Mode getMode() {
        return Mode.getMode(this);
    }

    private static class SetupTimeKey {
        private final Mode _mode;
        private final AltairParams.Mode _altairMode;

        SetupTimeKey(Mode mode) {
            if (mode == null) throw new IllegalArgumentException("mode must not be null");
            _mode = mode;
            _altairMode = null;
        }

        SetupTimeKey(Mode mode, AltairParams.Mode altairMode) {
            if (mode == null) throw new IllegalArgumentException("mode must not be null");
            if (altairMode == null) throw new IllegalArgumentException("altair mode must not be null");
            _mode = mode;
            _altairMode = altairMode;
        }

        public boolean equals(Object other) {
            if (!(other instanceof SetupTimeKey)) return false;
            SetupTimeKey that = (SetupTimeKey) other;
            return _mode == that._mode && (_altairMode == that._altairMode);
        }

        public int hashCode() {
            if (_altairMode != null) {
                return 37 * _mode.hashCode() + _altairMode.hashCode();
            } else {
                return _mode.hashCode();
            }
        }
    }

    public static double getFilterChangeOverheadSec() {
        return 30.0;
    }

    public double getReacquisitionTime () {
        return 0.0;
    } // considering it zero, as currently NIRI is imaging-only

    private static final Map<SetupTimeKey, Double> SETUP_TIME = new HashMap<>();

    static {
        SETUP_TIME.put(new SetupTimeKey(Mode.imaging), 6 * 60.);
        SETUP_TIME.put(new SetupTimeKey(Mode.imaging, AltairParams.Mode.NGS), 10 * 60.);
        SETUP_TIME.put(new SetupTimeKey(Mode.imaging, AltairParams.Mode.NGS_FL), 10 * 60.);
        SETUP_TIME.put(new SetupTimeKey(Mode.imaging, AltairParams.Mode.LGS), 15 * 60.);
        SETUP_TIME.put(new SetupTimeKey(Mode.imaging, AltairParams.Mode.LGS_P1), 15 * 60.);
        SETUP_TIME.put(new SetupTimeKey(Mode.imaging, AltairParams.Mode.LGS_OI), 15 * 60.); // not really supported
        SETUP_TIME.put(new SetupTimeKey(Mode.spectroscopy), 13 * 60.);
        SETUP_TIME.put(new SetupTimeKey(Mode.spectroscopy, AltairParams.Mode.NGS), 20 * 60.);
        SETUP_TIME.put(new SetupTimeKey(Mode.spectroscopy, AltairParams.Mode.NGS_FL), 20 * 60.);
        SETUP_TIME.put(new SetupTimeKey(Mode.spectroscopy, AltairParams.Mode.LGS), 25 * 60.);
        SETUP_TIME.put(new SetupTimeKey(Mode.spectroscopy, AltairParams.Mode.LGS_P1), 25 * 60.);
        SETUP_TIME.put(new SetupTimeKey(Mode.spectroscopy, AltairParams.Mode.LGS_OI), 25 * 60.); // not really supported
    }

    public static double getSetupTime(Mode mode) {
        return SETUP_TIME.get(new SetupTimeKey(mode));
    }

    public static double getSetupTime(Mode mode, AltairParams.Mode altairMode) {
        return SETUP_TIME.get(new SetupTimeKey(mode, altairMode));
    }

    /**
     * Return the setup time in seconds before observing can begin.
     */
    public double getSetupTime(ISPObservation obs) {
        // OT-243: 10 and 20 min
        // OT-470:
        //     NIRI + Altair imaging setup time should be 15 min
        //     NIRI + Altair spectroscopy setup should be 30 min
        //     NIRI spectroscopy setup time should add an extra 10 min for every
        //        additional hour of integration (for recentering in the slit)

        //SCT-219/OT-637: Add additional overhead for acquisitions with LGS

        // Update for SCT-275.  New values for the 6 different combinations of
        // imaging/spectroscopy vs. no AO/natural GS/laser GS.  Since this
        // crap gets confusing, I made a SETUP_TIME table to just lookup the
        // value.

        final Mode mode = getMode();
        final InstAltair altair = InstAltair.lookupAltair(obs);
        return (altair == null) ? getSetupTime(mode) : getSetupTime(mode, altair.getMode());
    }

    public double getSetupTime(Config[] conf) {
        String aoSystem = (String) conf[0].getItemValue(AOConstants.AO_SYSTEM_KEY);
        String guideStarType = (String) conf[0].getItemValue(AOConstants.AO_GUIDE_STAR_TYPE_KEY);
        String disperser = (String) conf[0].getItemValue(DISPERSER_KEY);

        Mode mode;
        AltairParams.Mode altairMode = null;
        if (disperser.equals("none"))  {
            mode = Mode.imaging;
        } else {
            mode = Mode.spectroscopy;
        }

        if (conf[0].containsItem(AOConstants.AO_SYSTEM_KEY) && aoSystem.equals("Altair")) {
            if (guideStarType.equals("LGS")) {
                altairMode = AltairParams.Mode.LGS;
            } else if (guideStarType.equals("NGS")) {
                altairMode = AltairParams.Mode.NGS;
            }
        }

        if (!conf[0].containsItem(AOConstants.AO_SYSTEM_KEY))  {
            return getSetupTime(mode);
        } else {
            return getSetupTime(mode, altairMode);
        }
    }

    @Override
    public CategorizedTimeGroup calc(final Config cur, final Option<Config> prev) {
        final Collection<CategorizedTime> times = new ArrayList<>();

        // Add filter change overhead if necessary
        if (cur.containsItem(Filter.KEY)) {
            if (PlannedTime.isUpdated(cur, prev, Filter.KEY)) {
                times.add(CategorizedTime.fromSeconds(Category.CONFIG_CHANGE, getFilterChangeOverheadSec(), "Filter"));
            }
        }

        // Add readout time.
        final int coadds = ExposureCalculator.instance.coadds(cur);
        final ReadMode readMode = (ReadMode) cur.getItemValue(ReadMode.KEY);
        final BuiltinROI roi = (BuiltinROI) cur.getItemValue(BuiltinROI.KEY);

        final Option<NiriReadoutTime> nrt = NiriReadoutTime.lookup(roi, readMode);
        nrt.foreach(niriReadoutTime -> {
            times.add(CategorizedTime.fromSeconds(Category.DHS_WRITE, niriReadoutTime.getDhsWriteTime()));
            times.add(CategorizedTime.fromSeconds(Category.READOUT, niriReadoutTime.getReadout(coadds)));
        });

        // Add exposure time
        final double exp = ExposureCalculator.instance.exposureTimeSec(cur);
        final double secs;

        if (cur.containsItem(FAST_MODE_KEY)) {
            final int fast = (Integer) cur.getItemValue(FAST_MODE_KEY);
            secs = fast * exp * coadds;
        } else {
            secs = exp * coadds;
        }
        times.add(CategorizedTime.fromSeconds(Category.EXPOSURE, secs));

        return CommonStepCalculator.instance.calc(cur, prev).addAll(times);
    }

    /**
     * Return the science area based upon the current camera.
     *
     * @return an array giving the size of the detector in arcsec
     */
    public double[] getScienceArea() {
        // If a mask has been applied, shrink the width.
        // The size depends upon the selected camera.
        // Index 0 is width, 1 is length/height

        final ROIDescription roi = _builtinROI.getROIDescription();
        final Mask m = getMask();
        final double height = _getCorrectHeight(m, roi);
        double width = height;
        if (m != Mask.MASK_IMAGING && m != Mask.PINHOLE_MASK) {
            // must be a slit
            width = m.getWidth();
        } else if (_builtinROI == BuiltinROI.SPEC_1024_512) {
            width *= 2.; // XXX This would seem to be a special case...
        }
        return new double[]{width, height};
    }

    /**
     * A private method to return the minimum size for the height based upon
     * the BeamSplitter or Camera
     * <p/>
     * The algorithm is to get the height of the camera and beamsplitter and
     * use the smaller of the two.  The only trick is that beamsplitter can be
     * set to SAME_AS_CAMERA, which means that the beamsplitter can't always
     * know its height and needs to know the camera too.
     */
    private double _getCorrectHeight(Mask m, ROIDescription roi) {
        Camera c = getCamera();
        double cheight = c.getScienceAreaHeight(roi);
        // Must pass Camera to BeamSplitter to handle the SAME_AS_CAMERA option.
        double bheight = getBeamSplitter().getScienceAreaHeight(c, roi);
        double height = (cheight < bheight) ? cheight : bheight;

        // Else it's spectroscopic mode so use the height of the mask too
        if (m != Mask.MASK_IMAGING && m != Mask.PINHOLE_MASK) {
            double mheight = m.getHeight(roi);
            height = (mheight < height) ? mheight : height;
        }
        return height;
    }

    /**
     * Return the Camera as a <code>{@link Niri.Camera Camera }</code>
     * object.
     */
    public Camera getCamera() {
        return _camera;
    }

    /**
     * Set the camera.
     */
    public void setCamera(Camera newValue) {
        Camera oldValue = getCamera();
        if (oldValue != newValue) {
            _camera = newValue;
            firePropertyChange(CAMERA_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the camera with a String.
     */
    private void _setCamera(String name) {
        Camera oldValue = getCamera();
        setCamera(Camera.getCamera(name, oldValue));
    }

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
            firePropertyChange(DISPERSER_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the disperser with a String.
     */
    private void _setDisperser(String name) {
        Disperser oldValue = getDisperser();
        setDisperser(Disperser.getDisperser(name, oldValue));
    }

    /**
     * Get the beam splitter.
     */
    public BeamSplitter getBeamSplitter() {
        return _splitter;
    }

    /**
     * Set the beam splitter.
     */
    public void setBeamSplitter(BeamSplitter newValue) {
        BeamSplitter oldValue = getBeamSplitter();
        if (oldValue != newValue) {
            _splitter = newValue;
            firePropertyChange(BEAM_SPLITTER_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the beam splitter with a String.
     */
    private void _setBeamSplitter(String name) {
        BeamSplitter oldValue = getBeamSplitter();
        setBeamSplitter(BeamSplitter.getBeamSplitter(name, oldValue));
    }

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
            firePropertyChange(READ_MODE_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the read mode with a String.
     */
    private void _setReadMode(String name) {
        ReadMode oldValue = getReadMode();
        setReadMode(ReadMode.getReadMode(name, oldValue));
    }

    /**
     * Get the detector well depth.
     */
    public WellDepth getWellDepth() {
        return _wellDepth;
    }

    /**
     * Set the well depth.
     */
    public void setWellDepth(WellDepth newValue) {
        WellDepth oldValue = getWellDepth();
        if (oldValue != newValue) {
            _wellDepth = newValue;
            firePropertyChange(WELL_DEPTH_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the well depth with a String.
     */
    private void _setWellDepth(String name) {
        WellDepth oldValue = getWellDepth();
        setWellDepth(WellDepth.getWellDepth(name, oldValue));
    }

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
            firePropertyChange(MASK_PROP.getName(), oldValue, newValue);
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
     * Get the filter index, going through both filter lists to match
     * the code.
     */
//    public int getFilterIndex() {
//        Filter f = getFilter();
//        return f.ordinal();
//    }

    /**
     * Set the filter. This method is needed as public by the NIRI
     * editor which currentl sets things via Strings.
     */
    private void _setFilter(String name) {
        Filter oldValue = getFilter();
        setFilter(Filter.getFilter(name, oldValue));
    }

    /**
     * Set the mode for the builtin ROI.  The default is full size of the
     * detector.
     */
    public void setBuiltinROI(BuiltinROI newValue) {
        BuiltinROI oldValue = getBuiltinROI();
        if (newValue != oldValue) {
            _builtinROI = newValue;
            firePropertyChange(BUILTIN_ROI_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the builtin ROI with a String.
     */
    private void _setBuiltinROI(String name) {
        BuiltinROI oldValue = getBuiltinROI();
        setBuiltinROI(BuiltinROI.getBuiltinROI(name, oldValue));
    }

    /**
     * Return the current value for the stage mode.
     */
    public BuiltinROI getBuiltinROI() {
        return _builtinROI;
    }

    /**
     * Return the (read-only, static) value for focus.
     */
    public Focus getFocus() {
        return new Focus();
    }

    public void setFocus(Focus focus) {
        // Required for reflection
    }

    /**
     * Returns the number of fast mode exposures.
     *
     * @return fast mode exposures as an int
     */
    public int getFastModeExposures() {
        return _fastModeExposures;
    }

    /**
     * Set the value for fast mode exposures
     *
     * @param newValue the new value for exposures
     */
    public void setFastModeExposures(int newValue) {
        if (newValue < 1) newValue = 1;
        int oldValue = _fastModeExposures;
        if (newValue != oldValue) {
            _fastModeExposures = newValue;
            firePropertyChange(FAST_MODE_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Get the number of fast mode exposures as a String.
     */
    public String getFastModeExposuresAsString() {
        return Integer.toString(_fastModeExposures);
    }

    /**
     * Set the fast mode exposures with a String.
     */
    private void _setFastModeExposures(String newValue) {
        setFastModeExposures(Integer.parseInt(newValue));
    }

    /**
     * Return a parameter set describing the current state of this object.
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, CAMERA_PROP.getName(), _camera.name());
        Pio.addParam(factory, paramSet, DISPERSER_PROP.getName(), _disperser.name());
        Pio.addParam(factory, paramSet, MASK_PROP.getName(), _mask.name());
        Pio.addParam(factory, paramSet, FILTER_PROP.getName(), _filter.name());
        Pio.addParam(factory, paramSet, BEAM_SPLITTER_PROP.getName(), _splitter.name());
        Pio.addParam(factory, paramSet, READ_MODE_PROP.getName(), _readMode.name());
        Pio.addParam(factory, paramSet, WELL_DEPTH_PROP.getName(), _wellDepth.name());
        Pio.addParam(factory, paramSet, BUILTIN_ROI_PROP.getName(), getBuiltinROI().name());
        Pio.addParam(factory, paramSet, FAST_MODE_PROP.getName(), getFastModeExposuresAsString());

        return paramSet;
    }

    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v;
        v = Pio.getValue(paramSet, CAMERA_PROP.getName());
        if (v != null) {
            _setCamera(v);
        }
        v = Pio.getValue(paramSet, DISPERSER_PROP.getName());
        if (v != null) {
            _setDisperser(v);
        }
        v = Pio.getValue(paramSet, MASK_PROP.getName());
        if (v != null) {
            _setMask(v);
        }
        v = Pio.getValue(paramSet, FILTER_PROP.getName());
        if (v != null) {
            _setFilter(v);
        }
        v = Pio.getValue(paramSet, BEAM_SPLITTER_PROP.getName());
        if (v != null) {
            _setBeamSplitter(v);
        }
        v = Pio.getValue(paramSet, READ_MODE_PROP.getName());
        if (v != null) {
            _setReadMode(v);
        }
        v = Pio.getValue(paramSet, WELL_DEPTH_PROP.getName());
        if (v != null) {
            _setWellDepth(v);
        }
        v = Pio.getValue(paramSet, BUILTIN_ROI_PROP.getName());
        if (v != null) {
            _setBuiltinROI(v);
        }
        v = Pio.getValue(paramSet, FAST_MODE_PROP.getName());
        if (v != null) {
            _setFastModeExposures(v);
        }
    }

    /**
     * Return the configuration for this component.
     */
    public ISysConfig getSysConfig() {
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);

        // Fill in the current values.
        sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion()));
        sc.putParameter(DefaultParameter.getInstance(FILTER_PROP.getName(), getFilter()));
        sc.putParameter(DefaultParameter.getInstance(MASK_PROP.getName(), getMask()));
        sc.putParameter(DefaultParameter.getInstance(DISPERSER_PROP.getName(), getDisperser()));
        sc.putParameter(DefaultParameter.getInstance(BEAM_SPLITTER_PROP.getName(), getBeamSplitter()));
        sc.putParameter(DefaultParameter.getInstance(READ_MODE_PROP.getName(), getReadMode()));
        sc.putParameter(DefaultParameter.getInstance(WELL_DEPTH_PROP.getName(), getWellDepth()));
        sc.putParameter(DefaultParameter.getInstance(CAMERA_PROP.getName(), getCamera()));
        sc.putParameter(DefaultParameter.getInstance(BUILTIN_ROI_PROP.getName(), getBuiltinROI()));
        sc.putParameter(DefaultParameter.getInstance(FOCUS_PROP.getName(), getFocus()));
        sc.putParameter(DefaultParameter.getInstance(EXPOSURE_TIME_PROP.getName(), getExposureTime()));
        sc.putParameter(DefaultParameter.getInstance(POS_ANGLE_PROP.getName(), getPosAngleDegrees()));
        sc.putParameter(DefaultParameter.getInstance(COADDS_PROP.getName(), getCoadds()));
        sc.putParameter(DefaultParameter.getInstance(FAST_MODE_PROP.getName(), getFastModeExposures()));

        return sc;
    }


    /**
     * Return a list of InstConfigInfo objects describing the instrument's
     * queryable configuration parameters.
     */
    public static List<InstConfigInfo> getInstConfigInfo() {
        List<InstConfigInfo> configInfo = new LinkedList<>();
        configInfo.add(new InstConfigInfo(CAMERA_PROP));
        configInfo.add(new InstConfigInfo(DISPERSER_PROP));
        configInfo.add(new InstConfigInfo(MASK_PROP));
        configInfo.add(new InstConfigInfo(FILTER_PROP));
        configInfo.add(new InstConfigInfo(BEAM_SPLITTER_PROP));
        configInfo.add(new InstConfigInfo(READ_MODE_PROP));
        return configInfo;
    }

    private static Collection<GuideProbe> GUIDE_PROBES = GuideProbeUtil.instance.createCollection(NiriOiwfsGuideProbe.instance);

    public Collection<GuideProbe> getGuideProbes() {
        return GUIDE_PROBES;
    }

    public static final ConfigInjector<String> WAVELENGTH_INJECTOR = ConfigInjector.create(
            new ObsWavelengthCalc2<Disperser, Filter>() {
                public PropertyDescriptor descriptor1() {
                    return DISPERSER_PROP;
                }

                public PropertyDescriptor descriptor2() {
                    return FILTER_PROP;
                }

                public String calcWavelength(Disperser d, Filter f) {
                    return InstNIRI.calcWavelength(d, f);
                }
            }
    );

    public static String calcWavelength(Disperser d, Filter f) {
        return (d == Disperser.NONE) ? f.getWavelengthAsString() : d.getCentralWavelengthAsString();
    }

    @Override public ConfigSequence postProcessSequence(ConfigSequence in) {
        Config[] configs = in.getAllSteps();

        for (Config c : configs) {
            if (CalConfigBuilderUtil.isCalStep(c)) {
                c.putItem(ReadMode.KEY, ReadMode.IMAG_1TO25);
            }
        }

        return new ConfigSequence(configs);
    }


    @Override
    public CalibrationKey extractKey(ISysConfig instrumentConfig) {
        Niri.Disperser disperser = (Niri.Disperser) get(instrumentConfig, InstNIRI.DISPERSER_PROP);
        Niri.Filter filter = (Niri.Filter) get(instrumentConfig, InstNIRI.FILTER_PROP);
        Niri.Mask mask = (Niri.Mask) get(instrumentConfig, InstNIRI.MASK_PROP);
        Niri.Camera camera = (Niri.Camera) get(instrumentConfig, InstNIRI.CAMERA_PROP);
        Niri.BeamSplitter beamSplitter = (Niri.BeamSplitter) get(instrumentConfig, InstNIRI.BEAM_SPLITTER_PROP);

        ConfigKeyNiri config = new ConfigKeyNiri(disperser, filter, mask, camera, beamSplitter);
        return new CalibrationKeyImpl(config);
    }

    @Override public Angle pwfs1VignettingClearance() {
        switch (getCamera()) {
            case F6:     return Angle.arcmins(5.7);
            case F14:    return Angle.arcmins(5.3);
            case F32 :   return Angle.arcmins(4.8);
            case F32_PV: return Angle.arcmins(4.8);
            default:     return super.pwfs1VignettingClearance();
        }
    }

    @Override public Angle pwfs2VignettingClearance() {
        switch (getCamera()) {
            case F6:     return Angle.arcmins(5.2);
            case F14:    return Angle.arcmins(4.8);
            case F32 :   return Angle.arcmins(4.3);
            case F32_PV: return Angle.arcmins(4.3);
            default:     return super.pwfs2VignettingClearance();
        }
    }

}
