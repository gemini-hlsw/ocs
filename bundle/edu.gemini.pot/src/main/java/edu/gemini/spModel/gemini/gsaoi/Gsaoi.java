package edu.gemini.spModel.gemini.gsaoi;

import edu.gemini.pot.sp.Instrument;
import edu.gemini.pot.sp.ISPFactory;
import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Offset;
import edu.gemini.spModel.config.injector.ConfigInjector;
import edu.gemini.spModel.config.injector.obswavelength.ObsWavelengthCalc1;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.core.BandsList;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.core.SingleBand;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.gems.CanopusWfs;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.gemini.init.ObservationNI;
import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeProvider;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.obs.plannedtime.CommonStepCalculator;
import edu.gemini.spModel.obs.plannedtime.ExposureCalculator;
import edu.gemini.spModel.obs.plannedtime.OffsetOverheadCalculator;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
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
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.telescope.IssPort;
import edu.gemini.spModel.telescope.IssPortProvider;
import edu.gemini.spModel.telescope.PosAngleConstraint;
import edu.gemini.spModel.telescope.PosAngleConstraintAware;
import edu.gemini.spModel.type.DisplayableSpType;
import edu.gemini.spModel.type.LoggableSpType;
import edu.gemini.spModel.type.SequenceableSpType;
import edu.gemini.spModel.type.SpTypeUtil;

import java.beans.PropertyDescriptor;
import java.time.Duration;
import java.util.*;

import static edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY;

/**
 * This class defines the GS AOI instrument.
 */
public final class Gsaoi extends SPInstObsComp
   implements PropertyProvider, GuideProbeProvider, IssPortProvider, StepCalculator, PosAngleConstraintAware, ItcOverheadProvider {
//    From REL-439:
//    ----
//    OT changes:
//    The text in the GSAOI component describing each read mode, the exposure time warning/error limits,
//    and the readout overheads in the planned time calculations must be updated with the information below.
//    The default exposure time for the GSAOI component must be 60.0 seconds.
//
//    Bright Objects:
//    Low Noise Reads: 2 (1-1 Fowler Sample)
//    Read Noise : 28e-
//    Exposure Time : > 5.3 sec (recommended) 5.3 sec (minimum)
//    Readout overhead: 10 sec
//
//    Faint Objects / Broad Band Imaging
//    Low Noise Reads: 8 (4-4 Fowler Sample)
//    Read Noise : 13e-
//    Exposure Time : > 21.5 sec (recommended) 21.5 sec (min)
//    Readout overhead: 26 sec
//
//    Very Faint Objects / Narrow-band Imaging
//    Low Noise Reads: 16 (8-8 Fowler Sample)
//    Read Noise : 10e-
//    Exposure Time : > 42.5 sec (recommended) 42.5 sec (min)
//    Readout overhead: 48 sec

    public enum ReadMode implements DisplayableSpType, SequenceableSpType, LoggableSpType {
        // Updated for REL-439
        BRIGHT("Bright Objects", "Bright", 2, 28, 5.3, 10),
        FAINT("Faint Objects / Broad-band Imaging", "Faint", 8, 13, 21.5, 26),
        VERY_FAINT("Very Faint Objects / Narrow-band Imaging", "V. Faint", 16, 10, 42.5, 48),;

        public static final ReadMode DEFAULT = ReadMode.BRIGHT;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "readMode");

        private final String displayValue;
        private final String logValue;
        private final int ndr;
        private final int readNoise;
        private final double minExposureTime; // seconds
        private final int overhead; // seconds

        ReadMode(String displayValue, String logValue, int ndr, int readNoise, double minExposureTime,
                         int overhead) {
            this.displayValue = displayValue;
            this.logValue = logValue;
            this.ndr = ndr;
            this.readNoise = readNoise;
            this.minExposureTime = minExposureTime;
            this.overhead = overhead;
        }

        public String displayValue() {
            return displayValue;
        }

        public String logValue() {
            return logValue;
        }

        public String sequenceValue() {
            return name();
        }

        public int ndr() {
            return ndr;
        }

        public int readNoise() {
            return readNoise;
        }

        public double minExposureTimeSecs() {
            return minExposureTime;
        }

        public int overhead() {
            return overhead;
        }

        public String toString() {
            return displayValue;
        }

        /**
         * Returns the read mode matching the given name by searching through
         * the known types.  If not found, nvalue is returned.
         */
        public static ReadMode valueOf(String name, ReadMode nvalue) {
            return SpTypeUtil.oldValueOf(ReadMode.class, name, nvalue);
        }

    }

    // REL-445: Updated using the new 50/50 times below
    public enum Filter implements DisplayableSpType, SequenceableSpType, LoggableSpType {
        Z("Z (1.015 um)", "Z",
                1.015, ReadMode.FAINT, 26.0, 4619, new SingleBand((MagnitudeBand.J$.MODULE$))),
        HEI("HeI (1.083 um)", "HeI",
                1.083, ReadMode.VERY_FAINT, 72.6, 21792, new SingleBand((MagnitudeBand.J$.MODULE$))),
        PA_GAMMA("Pa(gamma) (1.094 um)", "Pagma",
                1.094, ReadMode.VERY_FAINT, 122.0, 36585, new SingleBand((MagnitudeBand.J$.MODULE$))),
        J_CONTINUUM("J-continuum (1.207 um)", "Jcont",
                1.207, ReadMode.VERY_FAINT, 32.6, 9793, new SingleBand((MagnitudeBand.J$.MODULE$))),
        J("J (1.250 um)", "J",
                1.250, ReadMode.FAINT, 5.7, 1004, new SingleBand((MagnitudeBand.J$.MODULE$))),
        H("H (1.635 um)", "H",
                1.635, ReadMode.BRIGHT, 12.0, 460, new SingleBand((MagnitudeBand.H$.MODULE$))),
        PA_BETA("Pa(beta) (1.282 um)", "Pabeta",
                1.282, ReadMode.FAINT, 21.8, 3879, new SingleBand((MagnitudeBand.J$.MODULE$))),
        H_CONTINUUM("H-continuum (1.570 um)", "Hcont",
                1.570, ReadMode.FAINT, 31.2, 5545, new SingleBand((MagnitudeBand.H$.MODULE$))),
        CH4_SHORT("CH4(short) (1.580 um)", "CH4short",
                1.580, ReadMode.FAINT, 6.6, 1174, new SingleBand((MagnitudeBand.H$.MODULE$))),
        FE_II("[Fe II] (1.644 um)", "FeII1644",
                1.644, ReadMode.FAINT, 24.9, 4416, new SingleBand((MagnitudeBand.H$.MODULE$))),
        CH4_LONG("CH4(long) (1.690 um)", "CH4long",
                1.690, ReadMode.FAINT, 6.8, 1202, new SingleBand((MagnitudeBand.H$.MODULE$))),
        H20_ICE("H20 ice (2.000 um)", "H20ice",
                2.000, ReadMode.FAINT, 19.1, 3395, new SingleBand((MagnitudeBand.K$.MODULE$))),
        HEI_2P2S("HeI (2p2s) (2.058 um)", "HeI2p2s",
                2.058, ReadMode.FAINT, 28.3, 5032, new SingleBand((MagnitudeBand.K$.MODULE$))),
        K_CONTINUUM1("Ks-continuum (2.093 um)", "Kcontshrt",
                2.093, ReadMode.FAINT, 7.8, 6069, new SingleBand((MagnitudeBand.K$.MODULE$))),
        BR_GAMMA("Br(gamma) (2.166 um)", "Brgma",
                2.166, ReadMode.FAINT, 31.0, 5496, new SingleBand((MagnitudeBand.K$.MODULE$))),
        K_CONTINUUM2("Kl-continuum (2.270 um)", "Kcontlong",
                2.270, ReadMode.FAINT, 33.3, 5911, new SingleBand((MagnitudeBand.K$.MODULE$))),
        K_PRIME("K(prime) (2.120 um)", "Kprime",
                2.120, ReadMode.BRIGHT, 14.8, 566, new SingleBand((MagnitudeBand.K$.MODULE$))),
        H2_1_0_S_1("H2 1-0 S(1) (2.122 um)", "H2(1-0)",
                2.122, ReadMode.FAINT, 27.5, 5400, new SingleBand((MagnitudeBand.K$.MODULE$))),
        K_SHORT("K(short) (2.150 um)", "Kshort",
                2.150, ReadMode.BRIGHT, 14.4, 551, new SingleBand((MagnitudeBand.K$.MODULE$))),
        K("K (2.200 um)", "K",
                2.200, ReadMode.BRIGHT, 12.3, 470, new SingleBand((MagnitudeBand.K$.MODULE$))),
        H2_2_1_S_1("H2 2-1 S(1) (2.248 um)", "H2(2-1)",
                2.248, ReadMode.FAINT, 32.6, 5784, new SingleBand((MagnitudeBand.K$.MODULE$))),
        CO("CO (2.360 um)", "CO2360",
                2.360, ReadMode.FAINT, 7.7, 1370, new SingleBand((MagnitudeBand.K$.MODULE$))),
        DIFFUSER1("Diffuser1", "Diffuser1",
                0.0, ReadMode.BRIGHT, 0.0, 0),
        DIFFUSER2("Diffuser2", "Diffuser2",
                0.0, ReadMode.BRIGHT, 0.0, 0),
        BLOCKED("Blocked", "Blocked",
                0.0, ReadMode.BRIGHT, 0.0, 0),;

        public static Filter DEFAULT = Z;
        public static final ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "filter");

        private final String displayValue;
        private final String logValue;
        private final double wavelength;
        private final ReadMode readMode;
        private final double expTime5050;
        private final double expTimeHalfWell;
        private final Option<BandsList> catalogBand;

        private Filter(String displayValue, String logValue, double wavelength, ReadMode readMode, double expTime5050,
               double expTimeHalfWell, Option<BandsList> catalogBand) {
            this.displayValue = displayValue;
            this.logValue = logValue;
            this.wavelength = wavelength;
            this.readMode = readMode;
            this.expTime5050 = expTime5050;
            this.expTimeHalfWell = expTimeHalfWell;
            this.catalogBand = catalogBand;
        }

        Filter(String displayValue, String logValue, double wavelength, ReadMode readMode, double expTime5050,
                       double expTimeHalfWell, BandsList catalogBand) {
            this(displayValue, logValue, wavelength, readMode, expTime5050, expTimeHalfWell, new Some<>(catalogBand));
        }

        Filter(String displayValue, String logValue, double wavelength, ReadMode readMode, double expTime5050,
                       double expTimeHalfWell) {
            this(displayValue, logValue, wavelength, readMode, expTime5050, expTimeHalfWell, None.instance());
        }

        public String displayValue() {
            return displayValue;
        }

        public String logValue() {
            return logValue;
        }

        public double wavelength() {
            return wavelength;
        }

        public String formattedWavelength() {
            return String.format("%.3f", wavelength);
        }

        public ReadMode readMode() {
            return readMode;
        }

        public double exposureTime5050Secs() {
            return expTime5050;
        }

        public double exposureTimeHalfWellSecs() {
            return expTimeHalfWell;
        }

        public String sequenceValue() {
            return name();
        }

        public String toString() {
            return displayValue;
        }

        /**
         * Returns the filter matching the given name by searching through the
         * known types.  If not found, nvalue is returned.
         */
        public static Filter valueOf(String name, Filter nvalue) {
            return SpTypeUtil.oldValueOf(Filter.class, name, nvalue);
        }

        /**
         * Returns the filter matching the given magnitude band by searching through the
         * known types.  If not found, nvalue is returned.
         */
        public static Filter getFilter(MagnitudeBand band, Filter nvalue) {
            for(Filter filter : values()) {
                if (!filter.catalogBand.isEmpty() && filter.catalogBand.getValue().bandSupported(band)) {
                    return filter;
                }
            }
            return nvalue;
        }

        public Option<BandsList> getCatalogBand() {
            return catalogBand;
        }
    }

    public enum UtilityWheel implements DisplayableSpType, SequenceableSpType, LoggableSpType {
        EXTRAFOCAL_LENS_1("Extra-focal lens 1", "xf 1"),
        EXTRAFOCAL_LENS_2("Extra-focal lens 2", "xf 2"),
        PUPIL_IMAGER("Pupil Imager", "pupil"),
        CLEAR("Clear", "clear"),;

        public static UtilityWheel DEFAULT = CLEAR;
        public static ItemKey KEY = new ItemKey(INSTRUMENT_KEY, "utilityWheel");

        private final String displayValue;
        private final String logValue;

        UtilityWheel(String displayValue, String logValue) {
            this.displayValue = displayValue;
            this.logValue = logValue;
        }

        public String displayValue() {
            return displayValue;
        }

        public String logValue() {
            return logValue;
        }

        public String sequenceValue() {
            return name();
        }

        public String toString() {
            return displayValue;
        }

        public static UtilityWheel valueOf(String name, UtilityWheel nvalue) {
            return SpTypeUtil.oldValueOf(UtilityWheel.class, name, nvalue);
        }
    }

    public enum Roi implements DisplayableSpType, SequenceableSpType, LoggableSpType {
        FULL_ARRAY("Full Array", "Full Array"),
        ARRAY_64("Array 64", "Array64x64"),
        ARRAY_128("Array 128", "Array128x128"),
        ARRAY_256("Array 256", "Array256x256"),
        ARRAY_512("Array 512", "Array512x512"),
        ARRAY_1K("Array 1K", "Array1kx1k"),
        CENTRAL_64("Central 64", "Det64x64"),
        CENTRAL_128("Central 128", "Det128x128"),
        CENTRAL_256("Central 256", "Det256x256"),
        CENTRAL_512("Central 512", "Det512x512"),
        CENTRAL_1K("Central 1K", "Det1kx1k"),
        CENTRAL_2K("Central 2K", "Det2kx2k");

        public static Roi DEFAULT = FULL_ARRAY;

        private final String displayValue;
        private final String logValue;

        Roi(String displayValue, String logValue) {
            this.displayValue = displayValue;
            this.logValue = logValue;
        }

        public String displayValue() {
            return displayValue;
        }

        public String logValue() {
            return logValue;
        }

        public String sequenceValue() {
            return logValue;
        }

        public String toString() {
            return displayValue;
        }

        public static Roi valueOf(String name, Roi nvalue) {
            return SpTypeUtil.oldValueOf(Roi.class, name, nvalue);
        }
    }

    public enum OdgwSize implements DisplayableSpType, SequenceableSpType, LoggableSpType {
        SIZE_4(4),
        SIZE_6(6),
        SIZE_8(8),
        SIZE_16(16),
        SIZE_32(32),
        SIZE_64(64);

        public static OdgwSize DEFAULT = SIZE_64;

        private final int size;

        OdgwSize(int size) {
            this.size = size;
        }

        public int getSize() {
            return size;
        }

        public String displayValue() {
            return String.valueOf(size);
        }

        public String sequenceValue() {
            return displayValue();
        }

        public String logValue() {
            return displayValue();
        }

        public String toString() {
            return displayValue();
        }

        public static OdgwSize valueOf(String name, OdgwSize nvalue) {
            return SpTypeUtil.oldValueOf(OdgwSize.class, name, nvalue);
        }

    }

    private static final String VERSION = "2009A-1";

    public static final SPComponentType SP_TYPE =
            SPComponentType.INSTRUMENT_GSAOI;

    public static final ISPNodeInitializer<ISPObsComponent, Gsaoi> NI =
        new ComponentNodeInitializer<>(SP_TYPE, Gsaoi::new, GsaoiCB::new);

    public static final ISPNodeInitializer<ISPObservation, SPObservation> OBSERVATION_NI =
        new ObservationNI(Instrument.Gsaoi.some()) {
            @Override
            public void addSubnodes(ISPFactory factory, ISPObservation obsNode) {
                super.addSubnodes(factory, obsNode);

                // Add GeMS
                addObsComponent(factory, obsNode, SPComponentType.AO_GEMS);
            }
        };

    public static final String INSTRUMENT_NAME_PROP = "GSAOI";

    // REL-2645 offset overhead is 15 secs
    static final double GUIDED_OFFSET_OVERHEAD = 15.0; // sec
    private static final Duration MCAO_SETUP_TIME          = Duration.ofMinutes(30);
    private static final Duration GSAOI_REACQUISITION_TIME = Duration.ofMinutes(10);

    private static final CategorizedTime GUIDED_OFFSET_OVERHEAD_CATEGORIZED_TIME =
            CategorizedTime.fromSeconds(Category.CONFIG_CHANGE, GUIDED_OFFSET_OVERHEAD, OffsetOverheadCalculator.DETAIL);

    public static final CategorizedTime LGS_REACQUISITION_OVERHEAD_CATEGORIZED_TIME =
            CategorizedTime.fromSeconds(Category.CONFIG_CHANGE, GSAOI_REACQUISITION_TIME.getSeconds(), "LGS Reacquisition");

    public static final PropertyDescriptor FILTER_PROP;
    public static final PropertyDescriptor READ_MODE_PROP;
    public static final PropertyDescriptor PORT_PROP;
    public static final PropertyDescriptor EXPOSURE_TIME_PROP;
    public static final PropertyDescriptor COADDS_PROP;
    public static final PropertyDescriptor POS_ANGLE_PROP;
    public static final PropertyDescriptor POS_ANGLE_CONSTRAINT_PROP;
    public static final PropertyDescriptor UTILITY_WHEEL_PROP;
    public static final PropertyDescriptor ROI_PROP;
    public static final PropertyDescriptor ODGW_SIZE_PROP;

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    private static PropertyDescriptor initProp(String propName, boolean query, boolean iter) {
        PropertyDescriptor pd;
        pd = PropertySupport.init(propName, Gsaoi.class, query, iter);
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }

    static {
        boolean query_yes = true;
        boolean iter_yes = true;
        boolean query_no = false;
        boolean iter_no = false;

        FILTER_PROP = initProp(Filter.KEY.getName(), query_yes, iter_yes);
        READ_MODE_PROP = initProp(ReadMode.KEY.getName(), query_yes, iter_yes);
        PORT_PROP = initProp("issPort", query_no, iter_no);
        EXPOSURE_TIME_PROP = initProp("exposureTime", query_no, iter_yes);
        COADDS_PROP = initProp("coadds", query_no, iter_yes);
        POS_ANGLE_PROP = initProp("posAngle", query_no, iter_no);
        POS_ANGLE_CONSTRAINT_PROP = initProp("posAngleConstraint", query_no, iter_no);

        UTILITY_WHEEL_PROP = initProp(UtilityWheel.KEY.getName(), query_no, iter_yes);
        UTILITY_WHEEL_PROP.setExpert(true);
        PropertySupport.setWrappedType(UTILITY_WHEEL_PROP, UtilityWheel.class);

        ODGW_SIZE_PROP = initProp("odgwSize", query_no, iter_yes);
        ODGW_SIZE_PROP.setExpert(true);
        ODGW_SIZE_PROP.setDisplayName("ODGW Size");
        PropertySupport.setWrappedType(ODGW_SIZE_PROP, OdgwSize.class);

        ROI_PROP = initProp("roi", query_no, iter_yes);
        ROI_PROP.setExpert(true);
        ROI_PROP.setDisplayName("Region of Interest");
        PropertySupport.setWrappedType(ROI_PROP, Roi.class);
    }

    private PosAngleConstraint _posAngleConstraint = PosAngleConstraint.FIXED;

    private Filter filter = Filter.DEFAULT;
    private ReadMode readMode;
    private IssPort port = IssPort.UP_LOOKING;

    private UtilityWheel utilityWheel = UtilityWheel.DEFAULT;
    private OdgwSize odgwSize = OdgwSize.DEFAULT;
    private Roi roi = Roi.DEFAULT;

    public Gsaoi() {
        super(SP_TYPE);
        setVersion(VERSION);
        readMode = filter.readMode();
        setExposureTime(60); // REL-445
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    @Override
    public Set<Site> getSite() {
        return Site.SET_GS;
    }

    @Override
    public double[] getScienceArea() {
        return new double[]{85.0, 85.0};
    }

    public String getPhaseIResourceName() {
        return "gemGSAOI";
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter newValue) {
        Filter oldValue = getFilter();
        if (oldValue != newValue) {
            filter = newValue;
            firePropertyChange(FILTER_PROP.getName(), oldValue, newValue);
        }
    }

    public UtilityWheel getUtilityWheel() {
        return utilityWheel;
    }

    public void setUtilityWheel(UtilityWheel newValue) {
        UtilityWheel oldValue = getUtilityWheel();
        if (!oldValue.equals(newValue)) {
            utilityWheel = newValue;
            firePropertyChange(UTILITY_WHEEL_PROP.getName(), oldValue, newValue);
        }
    }

    public Roi getRoi() {
        return roi;
    }

    public void setRoi(Roi newValue) {
        Roi oldValue = getRoi();
        if (!oldValue.equals(newValue)) {
            roi = newValue;
            firePropertyChange(ROI_PROP.getName(), oldValue, newValue);
        }
    }

    public OdgwSize getOdgwSize() {
        return odgwSize;
    }

    public void setOdgwSize(OdgwSize newValue) {
        OdgwSize oldValue = getOdgwSize();
        if (!oldValue.equals(newValue)) {
            odgwSize = newValue;
            firePropertyChange(ODGW_SIZE_PROP.getName(), oldValue, newValue);
        }
    }


    public ReadMode getReadMode() {
        return readMode;
    }

    public void setReadMode(ReadMode newValue) {
        ReadMode oldValue = getReadMode();
        if (oldValue != newValue) {
            readMode = newValue;
            firePropertyChange(READ_MODE_PROP.getName(), oldValue, newValue);
        }
    }

    public IssPort getIssPort() {
        return port;
    }

    public void setIssPort(IssPort newValue) {
        IssPort oldValue = getIssPort();
        if (oldValue != newValue) {
            port = newValue;
            firePropertyChange(PORT_PROP.getName(), oldValue, newValue);
        }
    }

    public double getRecommendedExposureTimeSecs() {
        return getRecommendedExposureTimeSecs(getFilter(), getReadMode());
    }

    public static double getRecommendedExposureTimeSecs(Filter filter, ReadMode readMode) {
        double min = getMinimumExposureTimeSecs(readMode);
        if (filter == null) return min;
        double res = 3 * filter.exposureTime5050Secs();
        return (res < min) ? min : res;
    }

    public double getMinimumExposureTimeSecs() {
        return getMinimumExposureTimeSecs(getReadMode());
    }

    public static double getMinimumExposureTimeSecs(ReadMode readMode) {
        if (readMode == null) return 0;
        return readMode.minExposureTimeSecs();
    }

    public int getNonDestructiveReads() {
        ReadMode readMode = getReadMode();
        if (readMode == null) return 0;
        return readMode.ndr();
    }

    public int getReadNoise() {
        ReadMode readMode = getReadMode();
        if (readMode == null) return 0;
        return readMode.readNoise();
    }

    /**
     * Time needed to setup the instrument before the Observation
     *
     * @param obs the observation for which the setup time is wanted
     * @return time in seconds
     */
    @Override
    public Duration getSetupTime(ISPObservation obs) {
        return MCAO_SETUP_TIME;
    }

    /**
     * For ITC.
     * @deprecated config is a key-object collection and is thus not type-safe. It is meant for ITC only.
     */
    @Deprecated @Override
    public Duration getSetupTime(Config conf) {
        return MCAO_SETUP_TIME;
    }

    /**
     * Time needed to re-setup the instrument before the Observation following a previous full setup.
     *
     * @param obs the observation for which the setup time is wanted
     * @return time in seconds
     */
    @Override
    public Duration getReacquisitionTime(ISPObservation obs) {
        return GSAOI_REACQUISITION_TIME; // 10 mins as defined in REL-1346
    }

    @Override
    public Duration getReacquisitionTime(Config conf) {
        return GSAOI_REACQUISITION_TIME; // 10 mins as defined in REL-1346
    }

    public static CategorizedTime getWheelMoveOverhead() {
        // REL-1103 - 15 seconds for wheel move overhead
        return CategorizedTime.apply(Category.CONFIG_CHANGE, 15000, "Instrument");
    }

        // Predicate that leaves all CategorizedTime except for the offset overhead.
    private static final PredicateOp<CategorizedTime> RM_OFFSET_OVERHEAD = ct -> !((ct.category == Category.CONFIG_CHANGE) &&
             (ct.detail.equals(OffsetOverheadCalculator.DETAIL)));

    private static double getOffsetArcsec(Config c, ItemKey k) {
        final String d;
        try {
            d = (String) c.getItemValue(k); // yes a string :/

        } catch (ClassCastException cce) {
            return (double) c.getItemValue(k);
        }
        return (d == null) ? 0.0 : Double.parseDouble(d);
    }

    private static boolean isOffset(Config c, Option<Config> prev) {
        final double p1 = getOffsetArcsec(c, OffsetPosBase.TEL_P_KEY);
        final double q1 = getOffsetArcsec(c, OffsetPosBase.TEL_Q_KEY);

        final double p0, q0;
        if (prev.isEmpty()) {
            p0 = 0.0;
            q0 = 0.0;
        } else {
            p0 = getOffsetArcsec(prev.getValue(), OffsetPosBase.TEL_P_KEY);
            q0 = getOffsetArcsec(prev.getValue(), OffsetPosBase.TEL_Q_KEY);
        }
        return (p0 != p1) || (q0 != q1);
    }

    private static boolean isActive(Config c, String prop) {
        final ItemKey k = new ItemKey(SeqConfigNames.TELESCOPE_KEY, prop);
        final GuideOption go = (GuideOption) c.getItemValue(k);
        return (go != null) && go.isActive();
    }

    public static boolean isGuided(Config c) {
        for (final GsaoiOdgw odgw : GsaoiOdgw.values()) {
            if (isActive(c, odgw.getSequenceProp())) return true;
        }
        for (final CanopusWfs wfs : CanopusWfs.values()) {
            if (isActive(c, wfs.getSequenceProp())) return true;
        }
        return false;
    }

    private static boolean isGuided(Option<Config> c) {
        return !c.isEmpty() && isGuided(c.getValue());
    }

    private static boolean isExpensiveOffset(Config cur, Option<Config> prev) {
        if (!isOffset(cur, prev)) return false;

        final boolean curGuided = isGuided(cur);
        return curGuided || isGuided(prev);

    }

    /**
     * This is for use in the ITC overheads calculations only.
     * LGS reacquisition is required when coming back from sky offset >5'
     * @deprecated config is a key-object collection and is thus not type-safe. It is meant for ITC only.
     */
    @Deprecated
    private static boolean lgsReacquisitionRequired(Config cur, Option<Config> prev) {
        if (!isOffset(cur, prev)) {
            return false;
        }

        if (prev.isDefined()) {
            Offset curOff  = OffsetOverheadCalculator.instance.extract(cur);
            Offset prevOff = OffsetOverheadCalculator.instance.extract(prev);
            double distance = curOff.distance(prevOff).toArcsecs().getMagnitude();

            if (isGuided(cur) && !isGuided(prev)) {

                if (distance > 300.0) {
                    return true;
                }
            }
        }

        return false;
    }

        // REL-1103
    // Get correct offset overhead in the common group.  If a guided offset
    // or a switch from guided to non-guided, it is expensive.  If going from
    // a sky position to another sky position, it counts as a normal offset.
    private CategorizedTimeGroup commonGroup(Config cur, Option<Config> prev) {
        CategorizedTimeGroup ctg = CommonStepCalculator.instance.calc(cur, prev);

        // This is used only for the ITC overhead calculations, since in the OT the sky
        // observations with large offsets are made into separate observations
        if (lgsReacquisitionRequired(cur,prev)) {
            ctg = ctg.add(LGS_REACQUISITION_OVERHEAD_CATEGORIZED_TIME);
        }
        if (isExpensiveOffset(cur, prev)) {
            ctg = ctg.filter(RM_OFFSET_OVERHEAD).add(GUIDED_OFFSET_OVERHEAD_CATEGORIZED_TIME);
        }
        return ctg;
    }


    public double readout(int coadds, int lowNoiseReads) {
        return 21 + 2.8 * lowNoiseReads * coadds + 6.5 * (coadds - 1);
    }  // REL-445

    @Override public CategorizedTimeGroup calc(Config cur, Option<Config> prev) {
        // Add wheel move overhead
        Collection<CategorizedTime> times = new ArrayList<>();
        if (cur.containsItem(Filter.KEY)) {
            if (PlannedTime.isUpdated(cur, prev, Filter.KEY, UtilityWheel.KEY)) {
                times.add(getWheelMoveOverhead());
            }
        }

        // Add exposure time
        double exposureTime = ExposureCalculator.instance.exposureTimeSec(cur);
        int coadds = ExposureCalculator.instance.coadds(cur);
        times.add(CategorizedTime.fromSeconds(Category.EXPOSURE, exposureTime * coadds));

        // Add readout overhead
        int lowNoiseReads = getNonDestructiveReads();

        times.add(CategorizedTime.fromSeconds(Category.READOUT, readout(coadds, lowNoiseReads)).add(- Category.DHS_OVERHEAD.time)); // REL-1678
        times.add(Category.DHS_OVERHEAD); // REL-1678

        return commonGroup(cur, prev).addAll(times);
    }

    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = super.getParamSet(factory);

        Pio.addParam(factory, paramSet, FILTER_PROP.getName(), filter.name());
        Pio.addParam(factory, paramSet, READ_MODE_PROP.getName(), readMode.name());
        Pio.addParam(factory, paramSet, PORT_PROP.getName(), port.name());
        Pio.addParam(factory, paramSet, POS_ANGLE_CONSTRAINT_PROP.getName(), getPosAngleConstraint().name());
        Pio.addParam(factory, paramSet, UTILITY_WHEEL_PROP.getName(), utilityWheel.name());
        Pio.addParam(factory, paramSet, ODGW_SIZE_PROP.getName(), odgwSize.name());
        Pio.addParam(factory, paramSet, ROI_PROP.getName(), roi.name());

        return paramSet;
    }

    @Override
    public boolean hasOIWFS() {
        // No OIWFS -- there is a on-detector guide window
        return false;
    }

    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);

        String v;
        v = Pio.getValue(paramSet, FILTER_PROP.getName());
        if (v != null) setFilter(Filter.valueOf(v, getFilter()));

        v = Pio.getValue(paramSet, READ_MODE_PROP.getName());
        if (v != null) setReadMode(ReadMode.valueOf(v, getReadMode()));

        v = Pio.getValue(paramSet, PORT_PROP.getName());
        if (v != null) setIssPort(IssPort.valueOf(v));

        v = Pio.getValue(paramSet, POS_ANGLE_CONSTRAINT_PROP.getName());
        if (v != null) setPosAngleConstraint(PosAngleConstraint.valueOf(v));

        v = Pio.getValue(paramSet, UTILITY_WHEEL_PROP.getName());
        if (v != null) setUtilityWheel(UtilityWheel.valueOf(v, getUtilityWheel()));

        v = Pio.getValue(paramSet, ODGW_SIZE_PROP.getName());
        if (v != null) setOdgwSize(OdgwSize.valueOf(v, getOdgwSize()));

        v = Pio.getValue(paramSet, ROI_PROP.getName());
        if (v != null) setRoi(Roi.valueOf(v, getRoi()));

    }

    public ISysConfig getSysConfig() {
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);

        sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion()));
        sc.putParameter(DefaultParameter.getInstance(FILTER_PROP.getName(), getFilter()));
        sc.putParameter(DefaultParameter.getInstance(READ_MODE_PROP.getName(), getReadMode()));
        sc.putParameter(DefaultParameter.getInstance(PORT_PROP, getIssPort()));
        sc.putParameter(DefaultParameter.getInstance(UTILITY_WHEEL_PROP.getName(), getUtilityWheel()));
        sc.putParameter(DefaultParameter.getInstance(ODGW_SIZE_PROP.getName(), getOdgwSize()));
        sc.putParameter(DefaultParameter.getInstance(ROI_PROP.getName(), getRoi()));
        sc.putParameter(DefaultParameter.getInstance(EXPOSURE_TIME_PROP.getName(), getExposureTime()));
        sc.putParameter(DefaultParameter.getInstance(POS_ANGLE_PROP.getName(), getPosAngleDegrees()));
        sc.putParameter(DefaultParameter.getInstance(POS_ANGLE_CONSTRAINT_PROP.getName(), getPosAngleConstraint()));
        sc.putParameter(DefaultParameter.getInstance(COADDS_PROP.getName(), getCoadds()));

        return sc;
    }

    public static List<InstConfigInfo> getInstConfigInfo() {
        List<InstConfigInfo> configInfo = new LinkedList<>();
        configInfo.add(new InstConfigInfo(FILTER_PROP));
        configInfo.add(new InstConfigInfo(READ_MODE_PROP));
        return configInfo;
    }

    private static final Collection<GuideProbe> GUIDERS = GuideProbeUtil.instance.createCollection(GsaoiOdgw.values());

    public Collection<GuideProbe> getGuideProbes() {
        return GUIDERS;
    }

    public static final ConfigInjector<String> WAVELENGTH_INJECTOR = ConfigInjector.create(
            new ObsWavelengthCalc1<Filter>() {
                @Override
                public PropertyDescriptor descriptor1() {
                    return FILTER_PROP;
                }

                @Override
                public String calcWavelength(Filter f) {
                    return f.formattedWavelength();
                }
            }
    );

    private static final Angle PWFS1_VIG = Angle.arcmins(5.8);
    @Override public Angle pwfs1VignettingClearance() { return PWFS1_VIG; }

    @Override
    public PosAngleConstraint getPosAngleConstraint() {
        return (_posAngleConstraint == null) ? PosAngleConstraint.UNBOUNDED : _posAngleConstraint;
    }

    @Override
    public void setPosAngleConstraint(PosAngleConstraint newValue) {
        if (getSupportedPosAngleConstraints().contains(newValue)) { // Ignore unknown values
            PosAngleConstraint oldValue = getPosAngleConstraint();
            if (oldValue != newValue) {
                _posAngleConstraint = newValue;
                firePropertyChange(POS_ANGLE_CONSTRAINT_PROP.getName(), oldValue, newValue);
            }
        }
    }

    @Override
    public ImList<PosAngleConstraint> getSupportedPosAngleConstraints() {
        return DefaultImList.create(PosAngleConstraint.FIXED,
                                    PosAngleConstraint.UNBOUNDED);
    }

    @Override
    public boolean allowUnboundedPositionAngle() {
        return true;
    }

}
