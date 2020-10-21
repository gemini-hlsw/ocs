package edu.gemini.spModel.gemini.gmos;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentBroadType;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.config.injector.ConfigInjector;
import edu.gemini.spModel.config.injector.ConfigInjectorCalc3;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.core.Angle$;
import edu.gemini.spModel.data.IOffsetPosListProvider;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.PreImagingType;
import edu.gemini.spModel.data.YesNoType;
import edu.gemini.spModel.data.config.*;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.calunit.calibration.CalDictionary;
import edu.gemini.spModel.gemini.parallacticangle.ParallacticAngleSupportInst;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeProvider;
import edu.gemini.spModel.inst.ScienceAreaGeometry;
import edu.gemini.spModel.inst.VignettableScienceAreaInstrument;
import edu.gemini.spModel.obs.plannedtime.CommonStepCalculator;
import edu.gemini.spModel.obs.plannedtime.ExposureCalculator;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.CategorizedTimeGroup;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.Category;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.StepCalculator;
import edu.gemini.spModel.obscomp.InstConfigInfo;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.obscomp.ItcOverheadProvider;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.target.offset.OffsetPos;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.target.offset.OffsetPosListChangePropagator;
import edu.gemini.spModel.telescope.IssPort;
import edu.gemini.spModel.telescope.IssPortProvider;
import edu.gemini.spModel.telescope.PosAngleConstraint;
import edu.gemini.spModel.telescope.PosAngleConstraintAware;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.String;
import java.util.Optional;

/**
 * The GMOS instrument.
 */
public abstract class InstGmosCommon<
        D extends Enum<D> & GmosCommonType.Disperser,
        F extends Enum<F> & GmosCommonType.Filter,
        P extends Enum<P> & GmosCommonType.FPUnit,
        SM extends Enum<SM> & GmosCommonType.StageMode>
        extends ParallacticAngleSupportInst implements IOffsetPosListProvider<OffsetPos>, GuideProbeProvider,
            IssPortProvider, PosAngleConstraintAware, StepCalculator, VignettableScienceAreaInstrument, ItcOverheadProvider {

    private static final Logger LOG = Logger.getLogger(InstGmosCommon.class.getName());

    // for serialization
    private static final long serialVersionUID = 1L;

    /**
     * This obs component's SP type.
     */
    public static final SPComponentBroadType BROAD_TYPE = SPComponentBroadType.INSTRUMENT;

    public static final double DEFAULT_DISPERSER_LAMBDA = 550.0; //nanometers
    public static final String DISPERSER_PROP_NAME = "disperser";
    public static final String FILTER_PROP_NAME = "filter";
    public static final String FPU_PROP_NAME = "fpu";
    public static final String AMP_COUNT_PROP_NAME = GmosCommonType.AmpCount.KEY.getName();
    public static final String DETECTOR_MANUFACTURER_PROP_NAME = GmosCommonType.DetectorManufacturer.KEY.getName();
    public static final ItemKey DISPERSER_KEY = new ItemKey(SeqConfigNames.INSTRUMENT_KEY, DISPERSER_PROP_NAME);
    public static final ItemKey FPU_KEY = new ItemKey(SeqConfigNames.INSTRUMENT_KEY, FPU_PROP_NAME);

    // Nod & Shuffle values
    public static final boolean DEFAULT_USE_NS = false;
    public static final boolean DEFAULT_USE_ELECTRONIC_OFFSETTING = false;
    public static final int DEFAULT_NUM_NS_CYCLES = 1;
    public static final int DEFAULT_DETECTOR_ROWS = 0;

    public static final double DEF_EXPOSURE_TIME = 300.0; // sec
    public static final int DEF_COADDS = 1;
    public static final int DEFAULT_DISPERSER_ORDER = 1;

    // Pre-imaging flag
    public static final boolean DEFAULT_IS_MOS_PREIMAGING = false;

    // The size of the detector in arc secs
    public static final double DETECTOR_WIDTH = 330.34;
    public static final double DETECTOR_HEIGHT = 330.34;

    // Properties
    public static final PropertyDescriptor AMP_COUNT_PROP;
    public static final PropertyDescriptor AMP_READ_MODE_PROP;
    public static final PropertyDescriptor AMP_GAIN_CHOICE_PROP;

    public static final PropertyDescriptor ADC_PROP;
    public static final PropertyDescriptor AMP_GAIN_SETTING_PROP;

    public static final PropertyDescriptor BUILTIN_ROI_PROP;
    public static final PropertyDescriptor CUSTOM_ROI_PROP;
    public static final PropertyDescriptor CCD_X_BIN_PROP;
    public static final PropertyDescriptor CCD_Y_BIN_PROP;
    public static final ItemKey X_BIN_KEY = new ItemKey(SeqConfigNames.INSTRUMENT_KEY, "ccdXBinning");
    public static final ItemKey Y_BIN_KEY = new ItemKey(SeqConfigNames.INSTRUMENT_KEY, "ccdYBinning");

    public static final PropertyDescriptor DETECTOR_MANUFACTURER_PROP;

    public static final PropertyDescriptor DISPERSER_ORDER_PROP;
    public static final PropertyDescriptor DISPERSER_LAMBDA_PROP;
    public static final PropertyDescriptor FPU_MODE_PROP;
    public static final PropertyDescriptor FPU_MASK_PROP;
    public static final PropertyDescriptor PORT_PROP;
    public static final PropertyDescriptor STAGE_MODE_PROP;

    public static final String OFFSET_POS_LIST_PROP = "PosList";

    public static final PropertyDescriptor USE_NS_PROP;
    public static final PropertyDescriptor USE_ELECTRONIC_OFFSETTING_PROP;
    public static final PropertyDescriptor SHUFFLE_OFFSET_PROP;
    public static final PropertyDescriptor NUM_NS_CYCLES_PROP;
    public static final PropertyDescriptor DETECTOR_ROWS_PROP;

    // This is a derived property whose value is equal to the number of n&s
    // offset positions.
    public static final String             NS_STEP_COUNT_PROP_NAME = "nsStepCount";

    public static final PropertyDescriptor IS_MOS_PREIMAGING_PROP;

    public static final PropertyDescriptor DTAX_OFFSET_PROP;

    public static final PropertyDescriptor EXPOSURE_TIME_PROP;
    public static final PropertyDescriptor POS_ANGLE_PROP;

    public static final PropertyDescriptor CUSTOM_SLIT_WIDTH;
    public static final PropertyDescriptor POS_ANGLE_CONSTRAINT_PROP;

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<>();
    protected static final Map<String, PropertyDescriptor> PROTECTED_PROP_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        PropertyDescriptor pd;
        pd = PropertySupport.init(propName, InstGmosCommon.class, query, iter);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }

    // Initialize the properties.
    static {
        final boolean query_yes = true;
        final boolean iter_yes = true;
        final boolean query_no = false;
        final boolean iter_no = false;

        AMP_COUNT_PROP = initProp(AMP_COUNT_PROP_NAME, true, true);
        AMP_COUNT_PROP.setDisplayName("Amp Count");
        PropertySupport.setEngineering(AMP_COUNT_PROP, true);

        ADC_PROP = initProp("adc", query_yes, iter_no);

        try {
            AMP_GAIN_SETTING_PROP = new PropertyDescriptor("gainSetting", InstGmosCommon.class, "getActualGain", null);
            AMP_GAIN_SETTING_PROP.setDisplayName("Gain Setting");
            PropertySupport.setQueryable(AMP_GAIN_SETTING_PROP, false);
            PropertySupport.setIterable(AMP_GAIN_SETTING_PROP, false);
            PRIVATE_PROP_MAP.put(AMP_GAIN_SETTING_PROP.getName(), AMP_GAIN_SETTING_PROP);
        } catch (IntrospectionException ex) {
            throw new RuntimeException(ex);
        }

        AMP_GAIN_CHOICE_PROP = initProp("gainChoice", false, true);
        AMP_GAIN_CHOICE_PROP.setDisplayName("Gain Choice");

        AMP_READ_MODE_PROP = initProp(GmosCommonType.AmpReadMode.KEY.getName(), false, true);
        AMP_READ_MODE_PROP.setDisplayName("Amp Read Mode");

        BUILTIN_ROI_PROP = initProp(GmosCommonType.BuiltinROI.KEY.getName(), query_yes, iter_yes);
        BUILTIN_ROI_PROP.setDisplayName("Builtin ROI");
        try {
            CUSTOM_ROI_PROP = new PropertyDescriptor("customROIs", InstGmosCommon.class, "getCustomROIs", "setCustomROIs");
            CUSTOM_ROI_PROP.setDisplayName("Custom ROI");
            PropertySupport.setQueryable(CUSTOM_ROI_PROP, false);
            PropertySupport.setIterable(CUSTOM_ROI_PROP, false);
            PRIVATE_PROP_MAP.put(CUSTOM_ROI_PROP.getName(), CUSTOM_ROI_PROP);
        } catch (IntrospectionException ex) {
            throw new RuntimeException(ex);
        }

        CCD_X_BIN_PROP = initProp(X_BIN_KEY.getName(), query_yes, iter_yes);
        CCD_X_BIN_PROP.setDisplayName("X Bin");
        CCD_X_BIN_PROP.setShortDescription("X Binning Factor");
        CCD_Y_BIN_PROP = initProp(Y_BIN_KEY.getName(), query_yes, iter_yes);
        CCD_Y_BIN_PROP.setDisplayName("Y Bin");
        CCD_Y_BIN_PROP.setShortDescription("Y Binning Factor");

        DETECTOR_MANUFACTURER_PROP = initProp(InstGmosCommon.DETECTOR_MANUFACTURER_PROP_NAME, false, false);
        DETECTOR_MANUFACTURER_PROP.setDisplayName("Detector Manufacturer");

        DISPERSER_ORDER_PROP = initProp("disperserOrder", query_no, iter_yes);
        DISPERSER_LAMBDA_PROP = initProp("disperserLambda", query_yes, iter_yes);
        DISPERSER_LAMBDA_PROP.setDisplayName("Grating Ctrl Wvl");
        DISPERSER_LAMBDA_PROP.setShortDescription("Grating Central Wavelength");

        try {
            FPU_MODE_PROP = new PropertyDescriptor("fpuMode", InstGmosCommon.class, "getFPUnitMode", "setFPUnitMode");
            FPU_MODE_PROP.setDisplayName("FPU Mode");
            PropertySupport.setQueryable(FPU_MODE_PROP, false);
            PropertySupport.setIterable(FPU_MODE_PROP, false);
            PRIVATE_PROP_MAP.put(FPU_MODE_PROP.getName(), FPU_MODE_PROP);
        } catch (IntrospectionException ex) {
            throw new RuntimeException(ex);
        }

        try {
            FPU_MASK_PROP = new PropertyDescriptor("fpuCustomMask", InstGmosCommon.class, "getFPUnitCustomMask", "setFPUnitCustomMask");
            FPU_MASK_PROP.setDisplayName("Custom Mask MDF");
            PropertySupport.setQueryable(FPU_MASK_PROP, false);
            PropertySupport.setIterable(FPU_MASK_PROP, true);
            PRIVATE_PROP_MAP.put(FPU_MASK_PROP.getName(), FPU_MASK_PROP);
        } catch (IntrospectionException ex) {
            throw new RuntimeException(ex);
        }

        PORT_PROP = initProp(IssPortProvider.PORT_PROPERTY_NAME, query_no, iter_no);
        STAGE_MODE_PROP = initProp("stageMode", query_no, iter_no);
        USE_NS_PROP = initProp("useNS", query_yes, iter_no);
        USE_NS_PROP.setDisplayName("Nod & Shuffle");
        USE_ELECTRONIC_OFFSETTING_PROP = initProp("useElectronicOffsetting", query_no, iter_no);

        IS_MOS_PREIMAGING_PROP = initProp("mosPreimaging", query_yes, iter_no);
        IS_MOS_PREIMAGING_PROP.setDisplayName("MOS Pre-Imaging");

        try {
            SHUFFLE_OFFSET_PROP = new PropertyDescriptor("nsShuffleOffset", InstGmosCommon.class, "getShuffleOffset", "setShuffleOffset");
            SHUFFLE_OFFSET_PROP.setDisplayName("Shuffle Offset");
            PropertySupport.setQueryable(SHUFFLE_OFFSET_PROP, false);
            PropertySupport.setIterable(SHUFFLE_OFFSET_PROP, false);
            PRIVATE_PROP_MAP.put(SHUFFLE_OFFSET_PROP.getName(), SHUFFLE_OFFSET_PROP);
        } catch (IntrospectionException ex) {
            throw new RuntimeException(ex);
        }

        NUM_NS_CYCLES_PROP = initProp("nsNumCycles", query_no, iter_no);
        NUM_NS_CYCLES_PROP.setDisplayName("Number of NS Cycles");

        try {
            DETECTOR_ROWS_PROP = new PropertyDescriptor("nsDetectorRows", InstGmosCommon.class);
            DETECTOR_ROWS_PROP.setDisplayName("Detector Rows");
            PropertySupport.setQueryable(DETECTOR_ROWS_PROP, false);
            PropertySupport.setIterable(DETECTOR_ROWS_PROP, false);
            PRIVATE_PROP_MAP.put(DETECTOR_ROWS_PROP.getName(), DETECTOR_ROWS_PROP);
        } catch (IntrospectionException ex) {
            throw new RuntimeException(ex);
        }


        DTAX_OFFSET_PROP = initProp("dtaXOffset", query_yes, iter_yes);
        DTAX_OFFSET_PROP.setDisplayName("DTA X Offset");

        EXPOSURE_TIME_PROP = initProp("exposureTime", query_no, iter_yes);
        POS_ANGLE_PROP = initProp("posAngle", query_no, iter_no);
        CUSTOM_SLIT_WIDTH = initProp("customSlitWidth", query_yes, iter_no);
        POS_ANGLE_CONSTRAINT_PROP = initProp("posAngleConstraint", query_no, iter_no);
    }


    private GmosCommonType.ADC _adc = GmosCommonType.ADC.DEFAULT;
    protected GmosCommonType.AmpReadMode _ampReadMode = GmosCommonType.AmpReadMode.DEFAULT;
    private GmosCommonType.AmpCount _ampCount;
    protected GmosCommonType.AmpGain _gainChoice = GmosCommonType.AmpGain.DEFAULT;
    //    private GmosCommonType.AmpGainReadCombo _gainReadCombo = GmosCommonType.AmpGainReadCombo.SLOW_LOW;
    private GmosCommonType.DetectorManufacturer _detectorManufacturer = GmosCommonType.DetectorManufacturer.DEFAULT;
    //    private DET _detectorManufacturer;
    //    private GMOSParams.DisperserData _disperser = new GMOSParams.DisperserData();
    private D _disperser;
    private GmosCommonType.Order _disperserOrder = GmosCommonType.Order.DEFAULT;
    private double _disperserLambda = DEFAULT_DISPERSER_LAMBDA;

    private GmosCommonType.FPUnitMode _fpuMode = GmosCommonType.FPUnitMode.DEFAULT;
    private GmosCommonType.CustomSlitWidth _customSlitWidth = GmosCommonType.CustomSlitWidth.OTHER;
    //    private GMOSParams.FPUnit _fpu = GMOSParams.FPUnit.DEFAULT;
    private P _fpu;
    private String _fpuMaskLabel = EMPTY_STRING;
    //    private GMOSParams.Filter _filter = GMOSParams.Filter.DEFAULT;
    private F _filter;
    private GmosCommonType.Binning _xBin = GmosCommonType.Binning.DEFAULT;
    private GmosCommonType.Binning _yBin = GmosCommonType.Binning.DEFAULT;
    private PosAngleConstraint _posAngleConstraint = PosAngleConstraint.FIXED;

    //    private GmosCommonType.StageMode _stageMode = GmosCommonType.StageMode.DEFAULT;
    private SM _stageMode;
    private GmosCommonType.BuiltinROI _builtinROI = GmosCommonType.BuiltinROI.DEFAULT;
    private GmosCommonType.CustomROIList _customROIs = GmosCommonType.CustomROIList.create();

    private IssPort _port = IssPort.SIDE_LOOKING;
    private GmosCommonType.DTAX _dtaOffset = GmosCommonType.DTAX.DEFAULT; // Detector translation assembly (DTA-X) offset

    // fields for nod & shuffle support
    private boolean _useNS = DEFAULT_USE_NS;
    private boolean _useElectronicOffsetting = DEFAULT_USE_ELECTRONIC_OFFSETTING;
    private OffsetPosList<OffsetPos> _posList;  // list of nod & shuffle offset positions

    private final class PceNotifier implements OffsetPosListChangePropagator.Notifier {
        public void apply() {
            firePropertyChange(OFFSET_POS_LIST_PROP, null, _posList);
        }
    }

    //will be calculated from _detectorRows;
    //private double _shuffleOffset = _detectorManufacturer.shuffleOffsetArcsec();

    private int _numNSCycles = DEFAULT_NUM_NS_CYCLES;
    private int _detectorRows;
//    private double _pixelSize = GMOS_E2V_PIXEL_SIZE;

    //for the MOS pre-imaging flag
    private boolean _isMosPreimaging = DEFAULT_IS_MOS_PREIMAGING;

    // Used to format the shuffle offset as a string
    private static NumberFormat nf = NumberFormat.getInstance(Locale.US);

    static {
        InstGmosCommon.nf.setMaximumFractionDigits(2);
    }

    // Gain setting.
    public static final ConfigInjector<String> GAIN_SETTING_INJECTOR = ConfigInjector.create(
            new ConfigInjectorCalc3<GmosCommonType.AmpGain, GmosCommonType.AmpReadMode, GmosCommonType.DetectorManufacturer, String>() {

                public PropertyDescriptor descriptor1() {
                    return AMP_GAIN_CHOICE_PROP;
                }

                public PropertyDescriptor descriptor2() {
                    return AMP_READ_MODE_PROP;
                }

                public PropertyDescriptor descriptor3() {
                    return DETECTOR_MANUFACTURER_PROP;
                }

                public String resultPropertyName() {
                    return AMP_GAIN_SETTING_PROP.getName();
                }

                @Override
                public String apply(GmosCommonType.AmpGain gain, GmosCommonType.AmpReadMode mode, GmosCommonType.DetectorManufacturer man) {
                    return String.format("%d", getActualGain(gain, mode, man));
                }
            }
    );

    /**
     * Constructor
     */
    protected InstGmosCommon(SPComponentType type) {
        super(type);

        // Override the default exposure time
        _exposureTime = DEF_EXPOSURE_TIME;
        _coadds = DEF_COADDS;

        // calculate these values from the defaults
        //  _updateDetectorRows();
        _detectorRows =  _validateDetectorRows(GmosCommonType.DetectorManufacturer.DEFAULT.shuffleOffsetPixels());

        setDetectorManufacturer(GmosCommonType.DetectorManufacturer.DEFAULT);
    }

    /**
     * Implementation of the clone method.
     */
    public Object clone() {
        InstGmosCommon<?, ?, ?, ?> result = (InstGmosCommon<?, ?, ?, ?>) super.clone();

        if (result._posList != null) {
            //noinspection unchecked
            result._posList = (OffsetPosList<OffsetPos>) result._posList.clone();
            result._posList.addWatcher(new OffsetPosListChangePropagator<>(result.new PceNotifier(), result._posList));
        }
        return result;
    }

    public abstract String getPhaseIResourceName();

    /**
     * Provides a specialized serialization read method to make sure that
     * _fpuMode and _fpu are in sync. This was needed for backward compatibility
     * after implementing a requested change in the OT browser, so that Custom Mask
     * appears as one of the FP Unit choices, instead of as a separate FP Unit Mode choice.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        if (_fpuMode == GmosCommonType.FPUnitMode.CUSTOM_MASK) {
            GmosCommonType.FPUnitBridge<P> bridge = getFPUnitBridge();
            _fpu = bridge.getCustomMask();
        }

        if (_posList != null) {
            _posList.addWatcher(new OffsetPosListChangePropagator<>(new PceNotifier(), _posList));
        }
    }

    private static final Duration SETUP_TIME_LS_SPECTROSCOPY = Duration.ofMinutes(16);
    private static final Duration SETUP_TIME_IMAGING         = Duration.ofMinutes(6);
    private static final Duration SETUP_TIME_IFU_MOS         = Duration.ofMinutes(18);

    /**
     * Return the setup time in seconds before observing can begin
     * GMOS returns 15 minutes if imaging mode and 30 minutes if spectroscopy.
     * (Update1: see OT-243: 10 and 20 min).
     * (Update2: see OT-469: 15 and 25 min).
     * (Update3: Inger requested putting back to 10 and 20 min).
     * (Update4: SCT-275, 6 minutes imaging, 20 minutes spectroscopy)
     * (Update5: SCI-0107)
     */
    @Override
    public Duration getSetupTime(ISPObservation obs) {
        if (_disperser.isMirror()) return SETUP_TIME_IMAGING;
        if (_fpu.isSpectroscopic() || _fpu.isNSslit()) return SETUP_TIME_LS_SPECTROSCOPY;
        return SETUP_TIME_IFU_MOS;
    }

    /**
     * For ITC.
     * @deprecated config is a key-object collection and is thus not type-safe. It is meant for ITC only.
     */
    @Deprecated @Override
    public Duration getSetupTime(Config conf) {
        return Optional.ofNullable(conf.getItemValue(FPU_KEY))
                .map(c -> (GmosCommonType.FPUnit) c)
                .filter(f -> !f.isImaging())
                .map(f -> f.isIFU() ? SETUP_TIME_IFU_MOS : SETUP_TIME_LS_SPECTROSCOPY)
                .orElse(SETUP_TIME_IMAGING);
    }

    /**
     * Return the science area based upon the current camera.
     */
    public double[] getScienceArea() {
        return GmosScienceAreaGeometry.javaScienceAreaDimensions(this.getFPUnit());
    }

    /**
     * Get the ADC.
     */
    public GmosCommonType.ADC getAdc() {
        return _adc;
    }

    /**
     * Set the ADC.
     */
    public void setAdc(GmosCommonType.ADC newValue) {
        GmosCommonType.ADC oldValue = getAdc();
        if (oldValue != newValue) {
            _adc = newValue;
            firePropertyChange(ADC_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the ADC with a String.
     */
    private void _setADC(String name) {
        GmosCommonType.ADC oldValue = getAdc();
        setAdc(GmosCommonType.ADC.getADC(name, oldValue));
    }

    /**
     * Return the current ccd amp readout speed.
     */
    public GmosCommonType.AmpReadMode getAmpReadMode() {
        return _ampReadMode;
    }


    /**
     * This convenience method implements the algorithm for determining
     * the actual CCD Gain value based upon the CCD choices actually
     * selected.
     * <p/>
     * The E2V CCD's are being replaced in GMOS-B by Hamamatsu CCDs
     */
    public double getActualGain() {
        final GmosCommonType.AmpGain gain = getGainChoice();
        final GmosCommonType.AmpReadMode readMode = getAmpReadMode();
        final GmosCommonType.DetectorManufacturer detectorManufacturer = getDetectorManufacturer();

        return getActualGain(gain, readMode, detectorManufacturer);
    }


    /**
     * This convenience method implements the algorithm for determining
     * the actual CCD Gain value based upon the CCD choices actually
     * selected.
     */
    public static int getActualGain(final GmosCommonType.AmpGain gain,
                                       final GmosCommonType.AmpReadMode readMode,
                                       final GmosCommonType.DetectorManufacturer detectorManufacturer) {
        // Complicated switch nesting like this cries out for building a type hierarchy.  The parallel
        // type classes (GmosNorthType, et al) look promising, but I'm not willing to embed this information
        // there yet.  I'm changing this to an if-then just for brevity and lack of fall through, but
        // really it looks like we need the ability to comfortably group properties into something
        // that can hold this information about their combined properties.

        if (detectorManufacturer == GmosCommonType.DetectorManufacturer.E2V) {
            if (readMode == GmosCommonType.AmpReadMode.FAST) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    return 5;
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return 6;
                }
            } else if (readMode == GmosCommonType.AmpReadMode.SLOW) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    return 1;
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return 2;
                }
            }
        } else if (detectorManufacturer == GmosCommonType.DetectorManufacturer.HAMAMATSU) {
            if (readMode == GmosCommonType.AmpReadMode.FAST) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    return 5;
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return 6;
                }
            } else if (readMode == GmosCommonType.AmpReadMode.SLOW) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    return 1;
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return 2;
                }
            }
        }

        throw new IllegalArgumentException("unsupported configuration");
    }

    /**
     * Calculates the mean gain.
     */
    public abstract double getMeanGain();


    /**
     * Calculates the mean read noise.
     */
    public abstract double getMeanReadNoise();

    /**
     * Set the CCD readout speed.
     */
    public void setAmpReadMode(GmosCommonType.AmpReadMode newValue) {
        GmosCommonType.AmpReadMode oldValue = getAmpReadMode();
        if (newValue != oldValue) {
            _ampReadMode = newValue;
            firePropertyChange(AMP_READ_MODE_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the AmpReadMode with a String.
     */
    protected void _setAmpReadMode(String name) {
        GmosCommonType.AmpReadMode oldValue = getAmpReadMode();
        setAmpReadMode(GmosCommonType.AmpReadMode.getAmpReadMode(name, oldValue));
    }

    /**
     * Return the current CCD amp count choice.
     */
    public GmosCommonType.AmpCount getAmpCount() {
        return _ampCount;
    }

    /**
     * Set the CCD amp count choice.
     */
    public void setAmpCount(GmosCommonType.AmpCount newValue) {
        GmosCommonType.AmpCount oldValue = getAmpCount();
        if (newValue != oldValue) {
            _ampCount = newValue;
            firePropertyChange(AMP_COUNT_PROP_NAME, oldValue, newValue);
        }
    }

    public void setAmpCount(String name) {
        GmosCommonType.AmpCount oldValue = getAmpCount();
        GmosCommonType.AmpCount newValue = GmosCommonType.AmpCount.getAmpCount(name, oldValue);
        setAmpCount(newValue);
    }

    /**
     * Return the current CCD amp gain range.
     */
    public GmosCommonType.AmpGain getGainChoice() {
        return _gainChoice;
    }

    /**
     * Set the CCD amp gain choice.
     */
    public void setGainChoice(GmosCommonType.AmpGain newValue) {
        GmosCommonType.AmpGain oldValue = getGainChoice();
        if (newValue != oldValue) {
            _gainChoice = newValue;
            firePropertyChange(AMP_GAIN_CHOICE_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the AmpGain with a String.
     */
    protected void _setGainChoice(String name) {
        GmosCommonType.AmpGain oldValue = getGainChoice();
        setGainChoice(GmosCommonType.AmpGain.getAmpGain(name, oldValue));
    }

    /**
     * Get the disperser.
     */
    public D getDisperser() {
        return _disperser;
    }

    /**
     * Set the disperser type object using the same order and wavelength.
     */
    public void setDisperser(D newDisperser) {
        GmosCommonType.Disperser oldDisperser = getDisperser();
        if (oldDisperser != newDisperser) {
            _disperser = newDisperser;
            firePropertyChange(DISPERSER_PROP_NAME, oldDisperser, newDisperser);
        }
    }

    //    protected abstract D getDisperserFromString(String name, D defaultValue);
    protected abstract GmosCommonType.DisperserBridge<D> getDisperserBridge();

    private void _setDisperser(String name) {
        D oldValue = getDisperser();
        GmosCommonType.DisperserBridge<D> bridge = getDisperserBridge();
        //noinspection unchecked
        D newValue = bridge.parse(name, oldValue);
        setDisperser(newValue);
    }

    public GmosCommonType.Order getDisperserOrder() {
        return _disperserOrder;
    }

    public void setDisperserOrder(GmosCommonType.Order newValue) {
        GmosCommonType.Order oldValue = _disperserOrder;
        if (oldValue != newValue) {
            _disperserOrder = newValue;
            firePropertyChange(DISPERSER_ORDER_PROP.getName(), oldValue, newValue);
        }
    }

    private void _setDisperserOrder(String order) {
        setDisperserOrder(GmosCommonType.Order.getOrder(order));
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

    protected abstract GmosCommonType.FPUnitBridge<P> getFPUnitBridge();

    /**
     * Get the FPUnit.
     */
    public P getFPUnit() {
        if (_fpu == null) {
            GmosCommonType.FPUnitBridge<P> bridge = getFPUnitBridge();
            //noinspection unchecked
            _fpu = bridge.getDefaultValue();
        }
        return _fpu;
    }

    /**
     * Set the FPUnit.
     */
    public void setFPUnit(P newValue) {
        P oldValue = getFPUnit();
        if (oldValue != newValue) {
            _fpu = newValue;
            firePropertyChange(FPU_PROP_NAME, oldValue, newValue);
        }

        GmosCommonType.FPUnitBridge<P> bridge = getFPUnitBridge();
        //noinspection unchecked
        P customMaskFPU = bridge.getCustomMask();
        GmosCommonType.FPUnitMode customMaskMode = GmosCommonType.FPUnitMode.CUSTOM_MASK;
        if (_fpu == customMaskFPU && _fpuMode != customMaskMode) {
            setFPUnitMode(customMaskMode);
        } else if (_fpu != customMaskFPU && _fpuMode == customMaskMode) {
            setFPUnitMode(GmosCommonType.FPUnitMode.BUILTIN);
        }
    }

    protected void _setFPUnit(String name) {
        P oldValue = getFPUnit();

        GmosCommonType.FPUnitBridge<P> bridge = getFPUnitBridge();
        //noinspection unchecked
        P newValue = bridge.parse(name, oldValue);

        setFPUnit(newValue);
    }

    /**
     * Return the current FPUnit mode.
     */
    public GmosCommonType.FPUnitMode getFPUnitMode() {
        return _fpuMode;
    }

    /**
     * Set the FPUnit mode.  This can be either BUILTIN or CUSTOM_Mask.
     */
    public void setFPUnitMode(GmosCommonType.FPUnitMode newValue) {
        GmosCommonType.FPUnitMode oldValue = _fpuMode;
        if (oldValue != newValue) {
            _fpuMode = newValue;
            firePropertyChange(FPU_MODE_PROP.getName(), oldValue, newValue);
        }


        GmosCommonType.FPUnitBridge<P> bridge = getFPUnitBridge();
        //noinspection unchecked
        P customMaskFPU = bridge.getCustomMask();
        GmosCommonType.FPUnitMode customMaskMode = GmosCommonType.FPUnitMode.CUSTOM_MASK;
        if (_fpuMode == customMaskMode && _fpu != customMaskFPU) {
            setFPUnit(customMaskFPU);
        } else if (_fpuMode == GmosCommonType.FPUnitMode.BUILTIN && _fpu == customMaskFPU) {
            //noinspection unchecked
            setFPUnit(bridge.getNone());
        }
    }

    private void _setFPUnitMode(String name) {
        GmosCommonType.FPUnitMode oldValue = getFPUnitMode();
        setFPUnitMode(GmosCommonType.FPUnitMode.getFPUnitMode(name, oldValue));
    }


    /**
     * Set an FPU Custom Mask label.
     */
    public void setFPUnitCustomMask(String newValue) {
        if (newValue != null) {
            newValue = newValue.trim(); // remove white space
        }
        String oldValue = getFPUnitCustomMask();
        if (!oldValue.equals(newValue)) {
            _fpuMaskLabel = newValue;
            firePropertyChange(FPU_MASK_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Get the FPUMask label.  Note that this is meaningful only
     * if the user has placed the <@link FPUMode> to indicate the
     * <code><@link CUSTOM_MASK FPUnitMode.CUSTOM_MASK></code>
     * mode.
     */
    public String getFPUnitCustomMask() {
        if (_fpuMaskLabel != null) {
            // remove white space (for existing programs that may have it)
            _fpuMaskLabel = _fpuMaskLabel.trim();
        }
        return _fpuMaskLabel;
    }

    /**
     * The average parallactic angle for GMOS is rotated 90 degrees for wide
     * slits.
     */
    @Override
    public Option<edu.gemini.spModel.core.Angle> calculateParallacticAngle(ISPObservation obs) {
        return super.calculateParallacticAngle(obs).map(angle -> _fpu.isWideSlit() ?
                angle.$plus(Angle$.MODULE$.fromDegrees(90)) :
                angle);
    }

    /**
     * Test to see if FPU is in nod & shuffle mode.
     */
    public boolean isNS() {
        return getFPUnit().isNS();
    }


    /**
     * Is GMOS in imaging mode.
     */
    public boolean isImaging() {
        return getFPUnit().isImaging();
    }

    /**
     * Is GMOS in spectroscopic mode.
     */
    public boolean isSpectroscopic() {
        return getFPUnit().isSpectroscopic();
    }

    /**
     * Is GMOS in IFU mode.
     */
    public boolean isIFU() {
        return getFPUnit().isIFU();
    }

    /**
     * Is GMOS using a custom mask?
     */
    public boolean isCustomMaskInUse() {
        return getFPUnitMode() == GmosCommonType.FPUnitMode.CUSTOM_MASK;
    }

    /**
     * Get the filter.
     */
    public F getFilter() {
        return _filter;
    }

    /**
     * Set the filter.
     */
    public void setFilter(F newValue) {
        F oldValue = getFilter();
        if (oldValue != newValue) {
            _filter = newValue;
            firePropertyChange(FILTER_PROP_NAME, oldValue, newValue);
        }
    }

    protected abstract GmosCommonType.FilterBridge<F> getFilterBridge();

    /**
     * Set the filter.
     */
    protected void _setFilter(String name) {
        F oldValue = getFilter();
        GmosCommonType.FilterBridge<F> bridge = getFilterBridge();
        //noinspection unchecked
        F newValue = bridge.parse(name, oldValue);
        setFilter(newValue);
    }

    /**
     * Set the X CCD binning.
     */
    public void setCcdXBinning(GmosCommonType.Binning newValue) {
        GmosCommonType.Binning oldValue = getCcdXBinning();
        if (newValue != oldValue) {
            _xBin = newValue;
            firePropertyChange(CCD_X_BIN_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Return the current X CCD binning value.
     */
    public GmosCommonType.Binning getCcdXBinning() {
        return _xBin;
    }

    /**
     * Set the Y CCD binning.
     */
    public void setCcdYBinning(GmosCommonType.Binning newValue) {
        GmosCommonType.Binning oldValue = getCcdYBinning();
        if (newValue != oldValue) {
            _yBin = newValue;
            firePropertyChange(CCD_Y_BIN_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Return the current Y CCD binning value.
     */
    public GmosCommonType.Binning getCcdYBinning() {
        return _yBin;
    }

    // Private routine to set x binning with String
    private void _setXBinning(String newXValue) {
        GmosCommonType.Binning oldXValue = getCcdXBinning();
        setCcdXBinning(GmosCommonType.Binning.getBinning(newXValue, oldXValue));
    }

    // Private routine to set y binning with String
    private void _setYBinning(String newYValue) {
        GmosCommonType.Binning oldYValue = getCcdYBinning();
        setCcdYBinning(GmosCommonType.Binning.getBinning(newYValue, oldYValue));
    }

    /**
     * Get the GMOS Port
     */
    public IssPort getIssPort() {
        // for compatibility with previous versions
        if (_port == null) _port = IssPort.DEFAULT;
        return _port;
    }

    /**
     * Set the Port.
     */
    public void setIssPort(IssPort newValue) {
        IssPort oldValue = getIssPort();
        if (oldValue != newValue) {
            _port = newValue;
            firePropertyChange(PORT_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the Port with a String.
     */
    private void _setPort(String name) {
        IssPort oldValue = getIssPort();
        setIssPort(IssPort.getPort(name, oldValue));
    }

    protected abstract GmosCommonType.StageModeBridge<SM> getStageModeBridge();

    /**
     * Set the mode for the translation stage use.
     */
    public void setStageMode(SM newValue) {
        GmosCommonType.StageMode oldValue = getStageMode();
        if (newValue != oldValue) {
            _stageMode = newValue;
            firePropertyChange(STAGE_MODE_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Used by derived class to set stage mode
     */
    protected void _setStageMode(SM newValue) {
        _stageMode = newValue;
    }

    /**
     * Set the StageMode Mode with a String.
     * UGLY, FIXME Duplicating the private method and exposing it.  Having
     * trouble reasoning my waythrough the type straightjacket and need to make progress.
     */
    public void setStageMode(String name) {
        _setStageMode(name);
    }

    /**
     * Set the StageMode Mode with a String.
     */
    private void _setStageMode(String name) {
        SM oldValue = getStageMode();
        GmosCommonType.StageModeBridge<SM> bridge = getStageModeBridge();
        //noinspection unchecked
        SM newValue = bridge.parse(name, oldValue);
        setStageMode(newValue);
    }

    /**
     * Return the current value for the stage mode.
     */
    public SM getStageMode() {
        return _stageMode;
    }

    /**
     * Set the mode for the builtin ROI.  The default is full size of the
     * detector.
     */
    public void setBuiltinROI(GmosCommonType.BuiltinROI newValue) {
        GmosCommonType.BuiltinROI oldValue = getBuiltinROI();
        if (newValue != oldValue) {
            _builtinROI = newValue;
            firePropertyChange(BUILTIN_ROI_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the builtin ROI with a String.
     */
    private void _setBuiltinROI(String name) {
        GmosCommonType.BuiltinROI oldValue = getBuiltinROI();
        setBuiltinROI(GmosCommonType.BuiltinROI.getBuiltinROI(name, oldValue));
    }

    /**
     * Return the current value for the stage mode.
     */
    public GmosCommonType.BuiltinROI getBuiltinROI() {
        return _builtinROI;
    }

    public GmosCommonType.CustomROIList getCustomROIs() {
        return _customROIs;
    }

    public boolean validateCustomROIs() {
        for (GmosCommonType.ROIDescription roi : getCustomROIs().get()) {
            if (!roi.validate(getDetectorManufacturer().getXsize(), getDetectorManufacturer().getYsize())) {
                return false;
            }
        }
        return true;
    }

    public void setCustomROIs(final GmosCommonType.CustomROIList customROIs) {
        final GmosCommonType.CustomROIList oldValue = _customROIs;
        if (!customROIs.equals(oldValue)) {
            _customROIs = customROIs;
            firePropertyChange(CUSTOM_ROI_PROP.getName(), oldValue, _customROIs);
        }
    }

    /**
     * Return true if nod & shuffle is enabled
     */
    public boolean useNS() {
        return _useNS;
    }

    public GmosCommonType.UseNS getUseNS() {
        return useNS() ? GmosCommonType.UseNS.TRUE : GmosCommonType.UseNS.FALSE;
    }

    /**
     * Set to true to enable nod & shuffle
     */
    public void setUseNS(boolean newValue) {
        boolean oldValue = _useNS;
        if (newValue != oldValue) {
            _useNS = newValue;
            firePropertyChange(USE_NS_PROP.getName(), oldValue, newValue);
        }
    }

    public void setUseNS(GmosCommonType.UseNS useNS) {
        // this enum is stupid, but required to get the browser to work ...
        switch (useNS) {
            case TRUE:
                setUseNS(true);
                break;
            case FALSE:
                setUseNS(false);
                break;
        }

    }

    private void _setUseNS(String name) {
        GmosCommonType.UseNS oldValue = getUseNS();
        setUseNS(GmosCommonType.UseNS.getUseNS(name, oldValue));
    }

    /**
     * Return true if electronic offsetting is enabled
     */
    public boolean isUseElectronicOffsetting() {
        return _useElectronicOffsetting;
    }

    /**
     * Set to true to enable nod & shuffle
     */
    public void setUseElectronicOffsetting(boolean newValue) {
        boolean oldValue = _useElectronicOffsetting;
        if (newValue != oldValue) {
            _useElectronicOffsetting = newValue;
            firePropertyChange(USE_ELECTRONIC_OFFSETTING_PROP.getName(),
                    oldValue, newValue);
        }
    }

    /**
     * Return yes if this is a MOS Pre-imaging observation
     */
    public YesNoType isMosPreimaging() {
        return _isMosPreimaging ? YesNoType.YES : YesNoType.NO;
    }

    /**
     * Translate the preimaging value into a dedicated enum (needed for QPT and QV)
     */
    public PreImagingType getPreImaging() {
        return _isMosPreimaging ? PreImagingType.TRUE : PreImagingType.FALSE;
    }


    /**
     * Set to Yes to mark this imaging observation as MOS Pre-imaging
     */
    public void setMosPreimaging(YesNoType newValue) {
        //this is another case of an stupid enum made for the browser to work

        switch (newValue) {
            case NO:
                _setMosPreimaging(false);
                break;
            case YES:
                _setMosPreimaging(true);
                break;
        }
    }

    private void _setMosPreimaging(boolean newValue) {
        boolean oldValue = _isMosPreimaging;
        if (newValue != oldValue) {
            _isMosPreimaging = newValue;
            firePropertyChange(IS_MOS_PREIMAGING_PROP.getName(),
                    oldValue, newValue);
        }
    }


    /**
     * Return true if it is allowed to use an offset iterator with this instrument in the current mode.
     */
    public boolean offsetsAllowed(SPComponentType type, Set<SPComponentType> set) {
        return !_useNS;  //  only if not using nod & shuffle
    }

    /**
     * Return true if it is allowed to use electronic offsets for nod&shuffle
     */
    public boolean electronicOffsetsAllowed() {
        return _useNS && _posList != null && (checkUseElectronicOffsetting(this, _posList).allow);
    }

    /**
     * Return true if the offset position list provider feature is enabled.
     */
    public boolean isOffsetPosListProviderEnabled() {
        return _useNS;
    }


    /**
     * Get the position list data structure for the offset positions in
     * the item's list.  This method creates a new copy of the list.
     */
    public OffsetPosList<OffsetPos> getPosList() {
        if (_posList == null) {
            _posList = new OffsetPosList<>(OffsetPos.FACTORY);
            _posList.setAdvancedGuiding(Collections.singleton(getGuideProbe()));
            _posList.addWatcher(new OffsetPosListChangePropagator<>(new PceNotifier()));
        }
        return _posList;
    }


    /**
     * Set the shuffle offset in arcsec.
     */
    public final void setShuffleOffset(double newValue) {
        double oldValue = getShuffleOffset();
        if (oldValue != newValue) {
//            _shuffleOffset = newValue;
//            _updateDetectorRows();
            _detectorRows = calculateDetectorRows(newValue, getPixelSize(), getCcdYBinning().getValue());
            firePropertyChange(SHUFFLE_OFFSET_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Return the shuffle offset in arcsec.
     */
    public final double getShuffleOffset() {
        return calculateShuffleOffset(_detectorRows, getPixelSize(), getCcdYBinning().getValue());
    }

    /**
     * Get the shuffle offset as a string (arcsec).
     */
    public final String getShuffleOffsetAsString() {
        return nf.format(getShuffleOffset());
    }

    /**
     * Set the number detector rows
     */
    public final void setNsDetectorRows(int newValue) {
        int oldValue = getNsDetectorRows();
        if (oldValue != newValue) {
            _detectorRows = _validateDetectorRows(newValue);
            firePropertyChange(DETECTOR_ROWS_PROP.getName(), oldValue, _detectorRows);
        }
    }

    /**
     * Return the number detector rows
     */
    public final int getNsDetectorRows() {
        return _detectorRows;
    }


    public static int calculateDetectorRows(double shuffleOffsetArcsec, double pixelSize, int yBin) {
        // Round to nearest int.
        int r = (int) (shuffleOffsetArcsec / pixelSize + 0.5);

        // Round up to a multiple of yBin.
        int mod = r % yBin;
        return (mod == 0) ? r : r + (yBin - mod);
    }

    public static double calculateShuffleOffset(int detectorRows, double pixelSize, int yBin) {
        return pixelSize * detectorRows;
    }

    private int _validateDetectorRows(int rawDetectorRows) {
        int yBin = getCcdYBinning().getValue();
        int mod = rawDetectorRows % yBin;
        return (mod == 0) ? rawDetectorRows : rawDetectorRows + (yBin - mod);
    }

    /**
     * Set the number of nod & shuffle cycles
     */
    public final void setNsNumCycles(int newValue) {
        int oldValue = getNsNumCycles();
        if (oldValue != newValue) {
            _numNSCycles = newValue;
            firePropertyChange(NUM_NS_CYCLES_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Return the number of nod & shuffle cycles
     */
    public final int getNsNumCycles() {
        return _numNSCycles;
    }

    /**
     * Return the total integration time in seconds
     */
    public final double getTotalIntegrationTime() {
        int numSteps = getPosList().size();
        return _exposureTime * numSteps * _numNSCycles;
    }

    private static final Set<String> NOD_AND_SHUFFLE_OBS_TYPES = Collections.unmodifiableSet(new HashSet<String>() {{
        add(InstConstants.DARK_OBSERVE_TYPE);
        add(InstConstants.SCIENCE_OBSERVE_TYPE);
    }});

    public static boolean isNodAndShuffleableObsType(String obsType) {
        return obsType != null && NOD_AND_SHUFFLE_OBS_TYPES.contains(obsType);
    }

    public static boolean isNodAndShuffleableObsType(Config c) {
        Object obj = c.getItemValue(CalDictionary.OBS_TYPE_ITEM.key);
        return obj != null && isNodAndShuffleableObsType(obj.toString());
    }

    // REL-1678 (REL-1385)
    private static final CategorizedTime DHS_WRITE = CategorizedTime.fromSeconds(Category.DHS_WRITE, 10.0);

    public CategorizedTimeGroup calc(Config cur, Option<Config> prev) {
        final Collection<CategorizedTime> times = new ArrayList<>();

        double exposureTime = ExposureCalculator.instance.exposureTimeSec(cur);

        if (_useNS && isNodAndShuffleableObsType(cur)) {
            // OT-469: Nod & Shuffle overheads: 11 sec per nod cycle electronic offsets
            //         25 sec per nod cycle regular offsets
            // OT-477: setup-time + numN&Scycles*2*exposure time + numN&Scycles*Noverhead + readout
            //       = setup-time + "total observe time" + numN&Scycles*Noverhead + readout
            int nOverhead = (_useElectronicOffsetting ? 11 : 25);
            int numSteps = getPosList().size();
            exposureTime = (exposureTime * numSteps * _numNSCycles) + (_numNSCycles * nOverhead);
        }
        times.add(CategorizedTime.fromSeconds(Category.EXPOSURE, exposureTime));

        if (PlannedTime.isUpdated(cur, prev, GmosCommonType.FPUnit.KEY)) {
            times.add(CategorizedTime.fromSeconds(
                    Category.CONFIG_CHANGE, GmosCommonType.FPUnit.CHANGE_OVERHEAD, "FPU")
            );
        }
        if (PlannedTime.isUpdated(cur, prev, GmosCommonType.Filter.KEY)) {
            times.add(CategorizedTime.fromSeconds(
                    Category.CONFIG_CHANGE, GmosCommonType.Filter.CHANGE_OVERHEAD, "Filter")
            );
        }
        if (PlannedTime.isUpdated(cur, prev, GmosCommonType.Disperser.KEY)) {
            times.add(CategorizedTime.fromSeconds(
                    Category.CONFIG_CHANGE, GmosCommonType.Disperser.CHANGE_OVERHEAD, "Disperser")
            );
        }

        double readoutTime = GmosReadoutTime.getReadoutOverhead(cur, getCustomROIs());
        times.add(CategorizedTime.fromSeconds(Category.READOUT, readoutTime));

        times.add(DHS_WRITE);

        return CommonStepCalculator.instance.calc(cur, prev).addAll(times);
    }

    public void updateConfig(IConfig config) {
        // Okay, these are needed by GmosReadoutTime.  So make sure they are
        // cached.  This is a shameful hack for SCT-328.  These calls are all
        // made for their side-effect of caching crap.
        String s = SeqConfigNames.INSTRUMENT_CONFIG_NAME;
        config.getParameterValue(s, InstGmosCommon.AMP_COUNT_PROP_NAME);
        config.getParameterValue(s, InstGmosCommon.BUILTIN_ROI_PROP.getName());
        config.getParameterValue(s, InstGmosCommon.CCD_X_BIN_PROP.getName());
        config.getParameterValue(s, InstGmosCommon.CCD_Y_BIN_PROP.getName());
        config.getParameterValue(s, InstGmosCommon.AMP_READ_MODE_PROP.getName());
        config.getParameterValue(s, InstGmosCommon.AMP_GAIN_CHOICE_PROP.getName());
    }

    /**
     * Return the Detector translation assembly (DTA-X) offset.
     */
    public GmosCommonType.DTAX getDtaXOffset() {
        return _dtaOffset;
    }

    /**
     * Set the Detector translation assembly (DTA-X) offset and fire an event.
     */
    public void setDtaXOffset(GmosCommonType.DTAX newValue) {
        GmosCommonType.DTAX oldValue = _dtaOffset;
        if (newValue != oldValue) {
            _dtaOffset = newValue;
            firePropertyChange(DTAX_OFFSET_PROP.getName(), oldValue, newValue);
        }
    }

    /**
     * Set the Detector translation assembly (DTA-X) offset and fire an event.
     */
    protected void _setDtaXOffset(GmosCommonType.DTAX newValue) {
        _dtaOffset = newValue;
    }

    /**
     * Set the Port with a String.
     */
    private void _setDtaXOffset(String name) {
        GmosCommonType.DTAX oldValue = getDtaXOffset();
        setDtaXOffset(GmosCommonType.DTAX.getDTAX(name, oldValue));
    }

    /**
     * Gets the custom slit width
     */
    public GmosCommonType.CustomSlitWidth getCustomSlitWidth() {
        return _customSlitWidth;
    }

    /**
     * Sets the custom slit width
     */
    public void setCustomSlitWidth(GmosCommonType.CustomSlitWidth newValue) {
        GmosCommonType.CustomSlitWidth oldValue = getCustomSlitWidth();
        if (oldValue != newValue) {
            _customSlitWidth = newValue;
            firePropertyChange(CUSTOM_SLIT_WIDTH, oldValue, newValue);
        }
    }

    /**
     * Return a parameter set describing the current state of this object.
     *
     * @param factory
     */
    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, ADC_PROP.getName(), getAdc().name());
        Pio.addParam(factory, paramSet, AMP_COUNT_PROP_NAME, getAmpCount().name());
        Pio.addParam(factory, paramSet, AMP_GAIN_CHOICE_PROP.getName(), getGainChoice().name());
        Pio.addParam(factory, paramSet, AMP_READ_MODE_PROP.getName(), getAmpReadMode().name());
//        Pio.addParam(factory, paramSet, AMP_GAIN_READ_COMBO_PROP.getName(), getGainReadCombo().name());
//        Pio.addParam(factory, paramSet, ENGINEERING_AMP_GAIN_READ_COMBO_PROP.getName(), getEngineeringGainReadCombo().name());
        Pio.addParam(factory, paramSet, DETECTOR_MANUFACTURER_PROP_NAME, getDetectorManufacturer().name());

        Pio.addParam(factory, paramSet, DISPERSER_PROP_NAME, _disperser.name());
        Pio.addParam(factory, paramSet, DISPERSER_LAMBDA_PROP.getName(), String.valueOf(getDisperserLambda()));
        Pio.addParam(factory, paramSet, DISPERSER_ORDER_PROP.getName(), getDisperserOrder().name());

        GmosCommonType.FPUnitMode fpuMode = getFPUnitMode();
        Pio.addParam(factory, paramSet, FPU_MODE_PROP.getName(), fpuMode.name());
        Pio.addParam(factory, paramSet, FPU_PROP_NAME, getFPUnit().name());

        if (fpuMode == GmosCommonType.FPUnitMode.CUSTOM_MASK) {
            // It's a custom mask
            Pio.addParam(factory, paramSet, FPU_MASK_PROP.getName(), getFPUnitCustomMask());
            Pio.addParam(factory, paramSet, CUSTOM_SLIT_WIDTH.getName(), getCustomSlitWidth().name());
        }

        Pio.addParam(factory, paramSet, FILTER_PROP_NAME, getFilter().name());

        Pio.addParam(factory, paramSet, CCD_X_BIN_PROP.getName(), getCcdXBinning().name());
        Pio.addParam(factory, paramSet, CCD_Y_BIN_PROP.getName(), getCcdYBinning().name());

        Pio.addParam(factory, paramSet, PORT_PROP.getName(), getIssPort().name());

        PosAngleConstraint pac = getPosAngleConstraint();
        Pio.addParam(factory, paramSet, POS_ANGLE_CONSTRAINT_PROP.getName(), pac.name());

        Pio.addParam(factory, paramSet, STAGE_MODE_PROP.getName(), getStageMode().name());
        Pio.addParam(factory, paramSet, DTAX_OFFSET_PROP.getName(), getDtaXOffset().name());
        Pio.addParam(factory, paramSet, BUILTIN_ROI_PROP.getName(), getBuiltinROI().name());
        if (_customROIs != null && !_customROIs.isEmpty()) {
            paramSet.addParamSet(_customROIs.getParamSet(factory, CUSTOM_ROI_PROP.getName()));
        }

        Pio.addParam(factory, paramSet, USE_NS_PROP.getName(), getUseNS().name());

        // Only put nod/shuffle stuff in if it's a nod/shuffle observation
        if (useNS()) {
            Pio.addParam(factory, paramSet, NUM_NS_CYCLES_PROP.getName(), String.valueOf(getNsNumCycles()));
            Pio.addParam(factory, paramSet, DETECTOR_ROWS_PROP.getName(), String.valueOf(getNsDetectorRows()));
            Pio.addParam(factory, paramSet, USE_ELECTRONIC_OFFSETTING_PROP.getName(),
                    isUseElectronicOffsetting() ? "true" : "false");

            if (_posList != null && _posList.size() != 0) {
                ParamSet p = _posList.getParamSet(factory, SHUFFLE_OFFSET_PROP.getName());
                if (p != null) {
                    paramSet.addParamSet(p);
                }
            }
        }

        Pio.addParam(factory, paramSet, IS_MOS_PREIMAGING_PROP.getName(), isMosPreimaging().name());

        return paramSet;
    }


    /**
     * Set the state of this object from the given parameter set.
     */
    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v = Pio.getValue(paramSet, ADC_PROP.getName());
        if (v != null) _setADC(v);

        // REL-1194: setDetectorManufacturer has the side effect of changing the amp mode, so do this first
        v = Pio.getValue(paramSet, DETECTOR_MANUFACTURER_PROP_NAME);
        if (v != null) setDetectorManufacturer(v);

        v = Pio.getValue(paramSet, AMP_COUNT_PROP_NAME);
        if (v != null) setAmpCount(v);

        v = Pio.getValue(paramSet, AMP_READ_MODE_PROP.getName());
        if (v != null) _setAmpReadMode(v);

        v = Pio.getValue(paramSet, AMP_GAIN_CHOICE_PROP.getName());
        if (v != null) _setGainChoice(v);

        v = Pio.getValue(paramSet, DISPERSER_PROP_NAME);
        if (v != null) _setDisperser(v);

        if (getDisperser().isMirror()) {
            setDisperserOrder(GmosCommonType.Order.DEFAULT);
            setDisperserLambda(DEFAULT_DISPERSER_LAMBDA);
        } else {
            v = Pio.getValue(paramSet, DISPERSER_LAMBDA_PROP.getName());
            if (v != null) {
                double lambda = DEFAULT_DISPERSER_LAMBDA;
                try {
                    lambda = Double.parseDouble(v);
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Could not parse disperser lambda: " + v, ex);
                }
                setDisperserLambda(lambda);
            }

            v = Pio.getValue(paramSet, DISPERSER_ORDER_PROP.getName());
            if (v != null) _setDisperserOrder(v);
        }

        v = Pio.getValue(paramSet, FPU_MODE_PROP.getName());
        if (v != null) _setFPUnitMode(v);

        // Check the mode to see what else should be read
        if (getFPUnitMode() == GmosCommonType.FPUnitMode.BUILTIN) {
            v = Pio.getValue(paramSet, FPU_PROP_NAME);
            if (v != null) _setFPUnit(v);
        } else if (getFPUnitMode() == GmosCommonType.FPUnitMode.CUSTOM_MASK) {
            v = Pio.getValue(paramSet, FPU_MASK_PROP.getName());
            if (v != null) {
                setFPUnitCustomMask(v);
            }
            v = Pio.getValue(paramSet, CUSTOM_SLIT_WIDTH.getName());
            if (v != null) {
                setCustomSlitWidth(GmosCommonType.CustomSlitWidth.valueOf(v));
            }
        }
        v = Pio.getValue(paramSet, FILTER_PROP_NAME);
        if (v != null) _setFilter(v);

        v = Pio.getValue(paramSet, CCD_X_BIN_PROP.getName());
        if (v != null) _setXBinning(v);

        v = Pio.getValue(paramSet, CCD_Y_BIN_PROP.getName());
        if (v != null) _setYBinning(v);

        v = Pio.getValue(paramSet, PORT_PROP.getName());
        if (v == null) {
            // 2010B ISS Port property name.
            v = Pio.getValue(paramSet, "port");
        }
        if (v != null) _setPort(v);

        // REL-2090: Special workaround for elimination of former PositionAngleMode, since functionality has been
        // merged with PosAngleConstraint but we still need legacy code.
        v = Pio.getValue(paramSet, POS_ANGLE_CONSTRAINT_PROP.getName());
        final String pam = Pio.getValue(paramSet, "positionAngleMode");
        if ("MEAN_PARALLACTIC_ANGLE".equals(pam))
            _setPosAngleConstraint(PosAngleConstraint.PARALLACTIC_ANGLE);
        else if (v != null)
            _setPosAngleConstraint(v);

        v = Pio.getValue(paramSet, STAGE_MODE_PROP.getName());
        if (v != null) _setStageMode(v);

        v = Pio.getValue(paramSet, DTAX_OFFSET_PROP.getName());
        if (v != null) _setDtaXOffset(v);

        v = Pio.getValue(paramSet, BUILTIN_ROI_PROP.getName());
        if (v != null) _setBuiltinROI(v);

        ParamSet p = paramSet.getParamSet(CUSTOM_ROI_PROP.getName());
        if (p != null) {
            try {
                setCustomROIs(GmosCommonType.CustomROIList.create(p));
            } catch (IllegalArgumentException ex) {
                LOG.log(Level.WARNING, "Could not parse custom ROIs: " + p, ex);
            }
        }

        // Add the nod/shuffle state
        v = Pio.getValue(paramSet, USE_NS_PROP.getName());
        if (v != null) _setUseNS(v);

        if (useNS()) {
            v = Pio.getValue(paramSet, NUM_NS_CYCLES_PROP.getName());
            if (v != null) setNsNumCycles(Integer.parseInt(v));

            v = Pio.getValue(paramSet, USE_ELECTRONIC_OFFSETTING_PROP.getName());
            if (v != null) setUseElectronicOffsetting(Boolean.valueOf(v));

            v = Pio.getValue(paramSet, DETECTOR_ROWS_PROP.getName());
            if (v != null) setNsDetectorRows(Integer.parseInt(v));

            p = paramSet.getParamSet(SHUFFLE_OFFSET_PROP.getName());
            if (p != null) {
                getPosList().setParamSet(p);
                getPosList().setAdvancedGuiding(Collections.singleton(getGuideProbe()));
            }
        }

        v = Pio.getValue(paramSet, IS_MOS_PREIMAGING_PROP.getName());
        if (v != null)
            setMosPreimaging(YesNoType.getYesNoType(v, YesNoType.NO));
    }

    /**
     * Return the configuration for this component.
     */
    public ISysConfig getSysConfig() {
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);

        // Fill in the current values.
        sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion()));
        sc.putParameter(DefaultParameter.getInstance(ADC_PROP.getName(), getAdc()));
        sc.putParameter(DefaultParameter.getInstance(AMP_COUNT_PROP_NAME, getAmpCount()));

        sc.putParameter(DefaultParameter.getInstance(AMP_GAIN_SETTING_PROP.getName(), getActualGain()));
        sc.putParameter(DefaultParameter.getInstance(AMP_GAIN_CHOICE_PROP.getName(), getGainChoice()));
        sc.putParameter(DefaultParameter.getInstance(AMP_READ_MODE_PROP.getName(), getAmpReadMode()));

        sc.putParameter(DefaultParameter.getInstance(DISPERSER_PROP_NAME, _disperser));
        sc.putParameter(DefaultParameter.getInstance(DISPERSER_ORDER_PROP.getName(), getDisperserOrder()));
        sc.putParameter(DefaultParameter.getInstance(DISPERSER_LAMBDA_PROP.getName(), getDisperserLambda()));

        GmosCommonType.FPUnitMode fpuMode = getFPUnitMode();
        sc.putParameter(DefaultParameter.getInstance(FPU_MODE_PROP.getName(), fpuMode));
        if (fpuMode == GmosCommonType.FPUnitMode.BUILTIN) {
            sc.putParameter(DefaultParameter.getInstance(FPU_PROP_NAME, getFPUnit()));
        } else {
            sc.putParameter(StringParameter.getInstance(FPU_MASK_PROP.getName(), getFPUnitCustomMask()));
            sc.putParameter(DefaultParameter.getInstance(CUSTOM_SLIT_WIDTH.getName(), getCustomSlitWidth()));
        }

        sc.putParameter(DefaultParameter.getInstance(FILTER_PROP_NAME, getFilter()));

        sc.putParameter(DefaultParameter.getInstance(CCD_X_BIN_PROP.getName(), getCcdXBinning()));
        sc.putParameter(DefaultParameter.getInstance(CCD_Y_BIN_PROP.getName(), getCcdYBinning()));

        // Unclear if this needed.  We are maintaining consistency with the
        // 2010B (and earlier) OT.
        sc.putParameter(DefaultParameter.getInstance("port", getIssPort().displayValue().toLowerCase()));

        sc.putParameter(DefaultParameter.getInstance(POS_ANGLE_PROP.getName(),
                getPosAngleDegrees()));
        sc.putParameter(DefaultParameter.getInstance(POS_ANGLE_PROP.getName(),
                getPosAngleDegrees()));

        sc.putParameter(DefaultParameter.getInstance(EXPOSURE_TIME_PROP.getName(), getExposureTime()));


        sc.putParameter(DefaultParameter.getInstance(STAGE_MODE_PROP.getName(), getStageMode()));

        sc.putParameter(DefaultParameter.getInstance(BUILTIN_ROI_PROP.getName(), getBuiltinROI()));
        if (!getCustomROIs().isEmpty()) {
            sc.mergeParameters(getCustomROIs().getSysConfig());
        }

        sc.putParameter(DefaultParameter.getInstance(DTAX_OFFSET_PROP.getName(), getDtaXOffset()));

        sc.putParameter(DefaultParameter.getInstance(USE_NS_PROP, useNS()));

        if (useNS()) {
            sc.putParameter(DefaultParameter.getInstance(USE_ELECTRONIC_OFFSETTING_PROP.getName(),
                    isUseElectronicOffsetting()));

            sc.putParameter(DefaultParameter.getInstance(NS_STEP_COUNT_PROP_NAME,
                    getPosList().size()));

            sc.putParameter(DefaultParameter.getInstance(NUM_NS_CYCLES_PROP.getName(),
                    getNsNumCycles()));

            sc.putParameter(DefaultParameter.getInstance(DETECTOR_ROWS_PROP.getName(),
                    getNsDetectorRows()));
        }

        sc.putParameter(DefaultParameter.getInstance(IS_MOS_PREIMAGING_PROP.getName(),
                isMosPreimaging()));

        sc.putParameter(DefaultParameter.getInstance(DETECTOR_MANUFACTURER_PROP_NAME,
                getDetectorManufacturer()));

        return sc;
    }


    /**
     * Return a list of InstConfigInfo objects describing the instrument's
     * queryable configuration parameters.
     */
    protected static List<InstConfigInfo> getCommonInstConfigInfo() {
        List<InstConfigInfo> configInfo = new LinkedList<>();

        configInfo.add(new InstConfigInfo(DISPERSER_LAMBDA_PROP, false));
        configInfo.add(new InstConfigInfo(CCD_X_BIN_PROP));
        configInfo.add(new InstConfigInfo(CCD_Y_BIN_PROP));
        configInfo.add(new InstConfigInfo(BUILTIN_ROI_PROP));
        configInfo.add(new InstConfigInfo(USE_NS_PROP));
        configInfo.add(new InstConfigInfo(DTAX_OFFSET_PROP));
        configInfo.add(new InstConfigInfo(FPU_MASK_PROP, false));
        configInfo.add(new InstConfigInfo(IS_MOS_PREIMAGING_PROP));

        return configInfo;
    }

    public void setDetectorManufacturer(final String newValue) {
        setDetectorManufacturer(GmosCommonType.DetectorManufacturer.valueOf(newValue));
    }

    /**
     * Return the current CCD manufacturer.
     */
    public GmosCommonType.DetectorManufacturer getDetectorManufacturer() {
        return _detectorManufacturer;
    }

    /**
     * Set the CCD manufacturer.
     */
    public void setDetectorManufacturer(GmosCommonType.DetectorManufacturer newValue) {
        final GmosCommonType.DetectorManufacturer oldValue = getDetectorManufacturer();
        if (newValue != oldValue) {
            double offsetArcsec = getShuffleOffset(); // old value in arcsec
            _detectorManufacturer = newValue;
            firePropertyChange(DETECTOR_MANUFACTURER_PROP_NAME, oldValue, newValue);
            // REL-1444
            if (getNsDetectorRows() == oldValue.shuffleOffsetPixels()) {
                // if the old value was the default for the old detector,
                // set the new value to the default for the new detector
                setNsDetectorRows(newValue.shuffleOffsetPixels());
            } else {
                // Keep the old value in arcsec (approx.)
                setNsDetectorRows((int)Math.rint(offsetArcsec/getPixelSize()));
            }
        }

        initializeDetectorManufacturerValues();
    }

    public void initializeDetectorManufacturerValues() {
        final GmosCommonType.DetectorManufacturer dm = getDetectorManufacturer();

        if (dm == GmosCommonType.DetectorManufacturer.E2V) {
            setAmpCount(defaultAmpCountForE2V());
        } else if (dm == GmosCommonType.DetectorManufacturer.HAMAMATSU) {
            setAmpCount(GmosCommonType.AmpCount.TWELVE);
        } else {
            throw new IllegalArgumentException("Detector " + dm.name() + " not valid for GMOS");
        }
    }

    protected abstract GmosCommonType.AmpCount defaultAmpCountForE2V();

    /**
     * Gets the pixel size in arcsec for the currently selected detector
     * manufacturer. This is a convenience method that yields the same result
     * as <code>getDetectorManufacturer().pixelSize()</code>.
     *
     * @return pixel size in arcsec for the currently selected detector
     */
    abstract public double getPixelSize();

//    public GmosCommonType.AmpGainReadCombo getGainReadCombo() {
//        return _gainReadCombo;
//    }
//
//    private void _setGainReadCombo(String name) {
//        final GmosCommonType.AmpGainReadCombo readCombo = GmosCommonType.AmpGainReadCombo.getAmpGainReadCombo(name);
//        setGainReadCombo(readCombo);
//    }
//
//    public void setGainReadCombo(GmosCommonType.AmpGainReadCombo newValue) {
//        GmosCommonType.AmpGainReadCombo oldValue = getGainReadCombo();
//        if (newValue != oldValue) {
//            _gainReadCombo = newValue;
//            firePropertyChange(AMP_GAIN_READ_COMBO_PROP.getName(), oldValue, newValue);
//            setAmpReadMode(newValue.getAmpReadMode());
//            setGainChoice(newValue.getAmpGain());
//        }
//    }

    public static String calcWavelength(GmosCommonType.Disperser d, GmosCommonType.Filter f, Double centralWavelength) {
        if (d.isMirror()) return f.getWavelength();
        return (centralWavelength == 0.0) ? null :
                String.format("%.3f", centralWavelength / 1000.0);
    }


    public static final class UseElectronicOffsettingRuling {
        public final boolean allow;
        public final String message;

        public UseElectronicOffsettingRuling(boolean use, String message) {
            this.allow = use;
            this.message = message;
        }

        public static UseElectronicOffsettingRuling allow() {
            return new UseElectronicOffsettingRuling(true, "");
        }

        public static UseElectronicOffsettingRuling deny(String message) {
            return new UseElectronicOffsettingRuling(false, message);
        }
    }

    /**
     * Check if electronic offsets are allowed for the given instrument and the
     * current contents of the offset position list, and if not return a string
     * indicating why, otherwise null.
     * <p/>
     * Note that this should not be allowed and a warning message given if any step is
     * more than 2 arcsec from any other. If doing a square dither pattern, for example,
     * the diagonal of the square should be <=2 arcsec.
     * The default should be for no electronic offsets.
     * @return a string explaining why electronic offsets are not allowed, or null if they are allowed
     */
    public static UseElectronicOffsettingRuling checkUseElectronicOffsetting(InstGmosCommon<?, ?, ?, ?> inst, OffsetPosList<OffsetPos> p) {
        int size = p.size();
        if (size == 0) {
            return UseElectronicOffsettingRuling.deny("There are no offset positions defined.");
        }

        // Check that all offsets are within 2 arcsec from each other
        double x0 = 0, y0 = 0, x1 = 0, y1 = 0;
        for (int i = 0; i < size; ++i) {
            OffsetPos op = p.getPositionAt(i);
            double x = op.getXaxis(), y = op.getYaxis();
            if (i == 0) {
                x0 = x1 = x;
                y0 = y1 = y;
            } else {
                if (x < x0) x0 = x;
                if (x > x1) x1 = x;
                if (y < y0) y0 = y;
                if (y > y1) y1 = y;
            }
        }
        double xd = x1 - x0, yd = y1 - y0;
        double dist = Math.sqrt(xd * xd + yd * yd);
        if (dist > 2) {
            return UseElectronicOffsettingRuling.deny("Max electronic offset distance of 2 arcsec exceeded");
        }

        // must be okay then
        return UseElectronicOffsettingRuling.allow();
    }

    // The reacquisition time.
    private static final Duration REACQUISITION_TIME = Duration.ofMinutes(5);

    @Override
    public Duration getReacquisitionTime(ISPObservation obs) {
        return REACQUISITION_TIME;
    }

    @Override
    public Duration getReacquisitionTime(Config conf) {
        return REACQUISITION_TIME;
    }


    abstract protected GuideProbe getGuideProbe();

    private static final Angle PWFS1_VIG = Angle.arcmins(5.8);
    private static final Angle PWFS2_VIG = Angle.arcmins(5.3);

    @Override public Angle pwfs1VignettingClearance() { return PWFS1_VIG; }
    @Override public Angle pwfs2VignettingClearance() { return PWFS2_VIG; }

    @Override
    public boolean isCompatibleWithMeanParallacticAngleMode() {
        return !(_fpu.isImaging() || _fpuMode == GmosCommonType.FPUnitMode.CUSTOM_MASK);
    }

    /**
     * This needs to be overridden to support the PosAngleConstraint.
     */
    public PosAngleConstraint getPosAngleConstraint() {
        return (_posAngleConstraint == null) ? PosAngleConstraint.FIXED : _posAngleConstraint;
    }

    public void setPosAngleConstraint(PosAngleConstraint newValue) {
        PosAngleConstraint oldValue = getPosAngleConstraint();
        if (!oldValue.equals(newValue)) {
            _posAngleConstraint = newValue;
            firePropertyChange(POS_ANGLE_CONSTRAINT_PROP.getName(), oldValue, newValue);
        }
    }

    private void _setPosAngleConstraint(final String name) {
        final PosAngleConstraint oldValue = getPosAngleConstraint();
        try {
            _posAngleConstraint = PosAngleConstraint.valueOf(name);
        } catch (Exception ex) {
            _posAngleConstraint = oldValue;
        }
    }

    private void _setPosAngleConstraint(final PosAngleConstraint pac) {
        _posAngleConstraint = pac;
    }

    @Override
    public ImList<PosAngleConstraint> getSupportedPosAngleConstraints() {
        return DefaultImList.create(PosAngleConstraint.FIXED,
                                    PosAngleConstraint.FIXED_180,
                                    PosAngleConstraint.UNBOUNDED,
                                    PosAngleConstraint.PARALLACTIC_ANGLE,
                                    PosAngleConstraint.PARALLACTIC_OVERRIDE);
    }

    @Override
    public boolean allowUnboundedPositionAngle() {
        // Note that we disable unbounded position angle as an option for MOS preimaging and FPU Custom Mask.
        boolean isMos    = isMosPreimaging().equals(YesNoType.YES);
        boolean isCustom = getFPUnitMode() == GmosCommonType.FPUnitMode.CUSTOM_MASK;
        boolean isNone   = getFPUnit() == getFPUnitBridge().getNone();
        return !isMos && !isCustom && !isNone;
    }

    // REL-814 Preserve the FPU Custom Mask Name
    @Override
    public void restoreScienceDetails(final SPInstObsComp oldData) {
        super.restoreScienceDetails(oldData);
        if (oldData instanceof InstGmosCommon) {
            final InstGmosCommon<?, ?, ?, ?> oldGmos = (InstGmosCommon<?, ?, ?, ?>)oldData;
            setFPUnitCustomMask(oldGmos.getFPUnitCustomMask());
        }
    }

    @Override
    public ScienceAreaGeometry getVignettableScienceArea() {
        return GmosScienceAreaGeometry$.MODULE$;
    }

}
