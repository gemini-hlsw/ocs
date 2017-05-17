package edu.gemini.spModel.gemini.gmos;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.config.injector.ConfigInjector;
import edu.gemini.spModel.config.injector.obswavelength.ObsWavelengthCalc3;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationKeyProvider;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.CalibrationKeyImpl;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyGmosNorth;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.DisperserNorth;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FilterNorth;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.obscomp.InstConfigInfo;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 */
public class InstGmosNorth extends
        InstGmosCommon<GmosNorthType.DisperserNorth,
                GmosNorthType.FilterNorth,
                GmosNorthType.FPUnitNorth,
                GmosNorthType.StageModeNorth>
        implements PropertyProvider, CalibrationKeyProvider {
    private static final Logger LOG = Logger.getLogger(InstGmosCommon.class.getName());

    public static final SPComponentType SP_TYPE = SPComponentType.INSTRUMENT_GMOS;

    // The name of the GMOS instrument configuration
    public static final String INSTRUMENT_NAME_PROP = "GMOS-N";

    // Properties
    public static final PropertyDescriptor DISPERSER_PROP;
    public static final PropertyDescriptor FILTER_PROP;
    public static final PropertyDescriptor FPUNIT_PROP;

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    private static PropertyDescriptor initProp(final String propName, final String getMethod, String setMethod) {
        PropertyDescriptor propertyDescriptor;

        try {
            propertyDescriptor = new PropertyDescriptor(propName, InstGmosNorth.class, getMethod, setMethod);
            PropertySupport.setIterable(propertyDescriptor, true);
            PropertySupport.setQueryable(propertyDescriptor, true);
        } catch (IntrospectionException ex) {
            throw new RuntimeException(ex);
        }
        PRIVATE_PROP_MAP.put(propertyDescriptor.getName(), propertyDescriptor);
        return propertyDescriptor;
    }

    static {
        // Add all the superclass properties.
        PRIVATE_PROP_MAP.putAll(PROTECTED_PROP_MAP);

        DISPERSER_PROP = initProp("disperser", "getDisperserNorth", "setDisperserNorth");
        DISPERSER_PROP.setDisplayName("Disperser");
        FILTER_PROP = initProp("filter", "getFilterNorth", "setFilterNorth");
        FILTER_PROP.setDisplayName("Filter");
        FPUNIT_PROP = initProp("fpu", "getFPUnitNorth", "setFPUnitNorth");
        FPUNIT_PROP.setDisplayName("FPU");
    }


    public InstGmosNorth() {
        super(SP_TYPE);
        setDisperser(GmosNorthType.DisperserNorth.DEFAULT);
        setFilter(GmosNorthType.FilterNorth.DEFAULT);
        setFPUnit(GmosNorthType.FPUnitNorth.DEFAULT);
        setStageMode(GmosNorthType.StageModeNorth.DEFAULT);
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    @Override
    public Set<Site> getSite() {
        return Site.SET_GN;
    }

    public String getPhaseIResourceName() {
        return "gemGMOS-N";
    }

    // overridden solely for the creation of the property descriptor
    public GmosNorthType.DisperserNorth getDisperserNorth() {
        return super.getDisperser();
    }

    // overridden solely for the creation of the property descriptor
    public void setDisperserNorth(final GmosNorthType.DisperserNorth disperser) {
        super.setDisperser(disperser);
    }

    protected GmosCommonType.DisperserBridge<DisperserNorth> getDisperserBridge() {
        return GmosNorthType.DISPERSER_BRIDGE;
    }

    // overridden solely for the creation of the property descriptor
    public GmosNorthType.FilterNorth getFilterNorth() {
        return super.getFilter();
    }

    // overridden solely for the creation of the property descriptor
    public void setFilterNorth(final GmosNorthType.FilterNorth filter) {
        super.setFilter(filter);
    }

    public GmosCommonType.FilterBridge<FilterNorth> getFilterBridge() {
        return GmosNorthType.FILTER_BRIDGE;
    }

    // overridden solely for the creation of the property descriptor
    public GmosNorthType.FPUnitNorth getFPUnitNorth() {
        return super.getFPUnit();
    }

    // overridden solely for the creation of the property descriptor
    public void setFPUnitNorth(final GmosNorthType.FPUnitNorth fpunit) {
        super.setFPUnit(fpunit);
    }

    public GmosCommonType.FPUnitBridge<GmosNorthType.FPUnitNorth> getFPUnitBridge() {
        return GmosNorthType.FPUNIT_BRIDGE;
    }

    protected GmosCommonType.StageModeBridge<GmosNorthType.StageModeNorth> getStageModeBridge() {
        return GmosNorthType.STAGE_MODE_BRIDGE;
    }

    public static List<InstConfigInfo> getInstConfigInfo() {
        final List<InstConfigInfo> configInfo = InstGmosCommon.getCommonInstConfigInfo();
        configInfo.add(new InstConfigInfo(AMP_COUNT_PROP));
        configInfo.add(new InstConfigInfo(DISPERSER_PROP));
        configInfo.add(new InstConfigInfo(FILTER_PROP));
        configInfo.add(new InstConfigInfo(FPUNIT_PROP));
        configInfo.add(new InstConfigInfo(DETECTOR_MANUFACTURER_PROP));
        return configInfo;
    }

    // Observing Wavelength Injection
    public static final ConfigInjector<String> WAVELENGTH_INJECTOR = ConfigInjector.create(
            new ObsWavelengthCalc3<DisperserNorth, FilterNorth, Double>() {
                public PropertyDescriptor descriptor1() {
                    return DISPERSER_PROP;
                }

                public PropertyDescriptor descriptor2() {
                    return FILTER_PROP;
                }

                public PropertyDescriptor descriptor3() {
                    return DISPERSER_LAMBDA_PROP;
                }

                public String calcWavelength(DisperserNorth d, FilterNorth f, Double cwl) {
                    return InstGmosNorth.calcWavelength(d, f, cwl);
                }
            }
    );

    @Override
    public CalibrationKey extractKey(ISysConfig instrument) {
        //-- get some common values
        GmosCommonType.Binning xBin = (GmosCommonType.Binning) get(instrument, InstGmosCommon.CCD_X_BIN_PROP);
        GmosCommonType.Binning yBin = (GmosCommonType.Binning) get(instrument, InstGmosCommon.CCD_Y_BIN_PROP);
        GmosCommonType.AmpGain ampGain = (GmosCommonType.AmpGain) get(instrument, InstGmosNorth.AMP_GAIN_CHOICE_PROP);
        GmosCommonType.FPUnitMode fpUnitMode = (GmosCommonType.FPUnitMode) get(instrument, InstGmosCommon.FPU_MODE_PROP);
        GmosNorthType.DisperserNorth disperser = (GmosNorthType.DisperserNorth) get(instrument, InstGmosCommon.DISPERSER_PROP_NAME);
        GmosNorthType.FilterNorth filter = (GmosNorthType.FilterNorth) get(instrument, InstGmosCommon.FILTER_PROP_NAME);

        //-- detect slit width
        GmosNorthType.FPUnitNorth fpUnit;
        if (fpUnitMode == GmosCommonType.FPUnitMode.BUILTIN) {
            fpUnit = (GmosNorthType.FPUnitNorth) instrument.getParameter("fpu").getValue();
        } else {
            // translate custom slit width to a known longslit fpunit with same slit width for calibration lookup
            // TODO: this could be improved by only looking at the slit width instead of looking at the actual fp unit
            GmosCommonType.CustomSlitWidth customSlitWidth = (GmosCommonType.CustomSlitWidth) get(instrument, InstGmosCommon.CUSTOM_SLIT_WIDTH);
            switch (customSlitWidth) {
                case CUSTOM_WIDTH_0_25:
                    fpUnit = GmosNorthType.FPUnitNorth.LONGSLIT_1;
                    break;
                case CUSTOM_WIDTH_0_50:
                    fpUnit = GmosNorthType.FPUnitNorth.LONGSLIT_2;
                    break;
                case CUSTOM_WIDTH_0_75:
                    fpUnit = GmosNorthType.FPUnitNorth.LONGSLIT_3;
                    break;
                case CUSTOM_WIDTH_1_00:
                    fpUnit = GmosNorthType.FPUnitNorth.LONGSLIT_4;
                    break;
                case CUSTOM_WIDTH_1_50:
                    fpUnit = GmosNorthType.FPUnitNorth.LONGSLIT_5;
                    break;
                case CUSTOM_WIDTH_2_00:
                    fpUnit = GmosNorthType.FPUnitNorth.LONGSLIT_6;
                    break;
                case CUSTOM_WIDTH_5_00:
                    fpUnit = GmosNorthType.FPUnitNorth.LONGSLIT_7;
                    break;
                default:
                    // no translation possible, return custom mask value
                    fpUnit = GmosNorthType.FPUnitNorth.CUSTOM_MASK;
            }
        }

        // check order and wavelength (values will only be present in case of spectroscopy)
        Double wavelength = getWavelength(instrument) * 1000.; // adjust scaling of wavelength from um to nm (as used in config tables)
        GmosCommonType.Order order = GmosCommonType.Order.DEFAULT;
        IParameter orderParameter = instrument.getParameter("disperserOrder");
        if (orderParameter != null) {
            order = (GmosCommonType.Order) orderParameter.getValue();
        }

        ConfigKeyGmosNorth config = new ConfigKeyGmosNorth(disperser, filter, fpUnit, xBin, yBin, order, ampGain);
        return new CalibrationKeyImpl.WithWavelength(config, wavelength);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getPixelSize() {
        return getDetectorManufacturer().pixelSizeNorth();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMeanGain() {
        return getMeanGain(getGainChoice(), getAmpReadMode(), getDetectorManufacturer());
    }


    /**
     * Calculates the mean gain for the given parameters for GMOS North.
     */
    public static double getMeanGain(final GmosCommonType.AmpGain gain,
                              final GmosCommonType.AmpReadMode readMode,
                              final GmosCommonType.DetectorManufacturer detectorManufacturer) {

        if (detectorManufacturer == GmosCommonType.DetectorManufacturer.E2V) {
            if (readMode == GmosCommonType.AmpReadMode.FAST) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    return 5.0;
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return 2.5;
                }
            } else if (readMode == GmosCommonType.AmpReadMode.SLOW) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    return 4.4;
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return 2.2;
                }
            }
        } else if (detectorManufacturer == GmosCommonType.DetectorManufacturer.HAMAMATSU) {
            if (readMode == GmosCommonType.AmpReadMode.FAST) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    return 5.11;
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return 1.96;
                }
            } else if (readMode == GmosCommonType.AmpReadMode.SLOW) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    return 4.4;
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return 1.63;
                }
            }
        }

        throw new IllegalArgumentException("unsupported configuration");

    }

    /**
     * Calculates the mean read noise for this instrument.
     */
    public double getMeanReadNoise() {
        final GmosCommonType.AmpGain gain = getGainChoice();
        final GmosCommonType.AmpReadMode readMode = getAmpReadMode();
        final GmosCommonType.DetectorManufacturer detectorManufacturer = getDetectorManufacturer();
        return getMeanReadNoise(gain, readMode, detectorManufacturer);
    }

    /**
     * Calculates the mean read noise for the given parameters for GMOS North.
     */
    public static double getMeanReadNoise(final GmosCommonType.AmpGain gain,
                                          final GmosCommonType.AmpReadMode readMode,
                                          final GmosCommonType.DetectorManufacturer detectorManufacturer) {

        if (detectorManufacturer == GmosCommonType.DetectorManufacturer.E2V) {
            if (readMode == GmosCommonType.AmpReadMode.FAST) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    return 7.4;
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return 4.9;
                }
            } else if (readMode == GmosCommonType.AmpReadMode.SLOW) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    return 4.8;
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return 3.4;
                }
            }
        } else if (detectorManufacturer == GmosCommonType.DetectorManufacturer.HAMAMATSU) {
            if (readMode == GmosCommonType.AmpReadMode.FAST) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    return 8.69;
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return 6.27;
                }
            } else if (readMode == GmosCommonType.AmpReadMode.SLOW) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    return 4.8;
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return 4.14;
                }
            }
        }

        throw new IllegalArgumentException("unsupported configuration");

    }

    protected GmosCommonType.AmpCount defaultAmpCountForE2V() {
        return GmosCommonType.AmpCount.SIX;
    }

    // Method to return the guide probe associated with this instrument, so that we can move general functionality
    // independent of guide probe to InstGmosCommon.
    @Override
    protected GuideProbe getGuideProbe() {
        return GmosOiwfsGuideProbe.instance;
    }

    private static Collection<GuideProbe> GUIDE_PROBES = GuideProbeUtil.instance.createCollection(GmosOiwfsGuideProbe.instance);

    @Override
    public Collection<GuideProbe> getGuideProbes() {
        return GUIDE_PROBES;
    }
}
