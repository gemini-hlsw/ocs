//
// $Id: InstGmosSouth.java 45751 2012-06-04 15:28:44Z swalker $
//

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
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.ConfigKeyGmosSouth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.DisperserSouth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth;
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
public class InstGmosSouth extends
        InstGmosCommon<GmosSouthType.DisperserSouth,
                GmosSouthType.FilterSouth,
                GmosSouthType.FPUnitSouth,
                GmosSouthType.StageModeSouth> implements PropertyProvider, CalibrationKeyProvider {
    private static final Logger LOG = Logger.getLogger(InstGmosCommon.class.getName());

    public static final SPComponentType SP_TYPE =
            SPComponentType.INSTRUMENT_GMOSSOUTH;

    public static final String INSTRUMENT_NAME_PROP = "GMOS-S";

    // Properties
    public static final PropertyDescriptor DISPERSER_PROP;
    public static final PropertyDescriptor FILTER_PROP;
    public static final PropertyDescriptor FPUNIT_PROP;

    private static final Map<String, PropertyDescriptor> PRIVATE_PROP_MAP = new TreeMap<String, PropertyDescriptor>();
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP = Collections.unmodifiableMap(PRIVATE_PROP_MAP);

    private static PropertyDescriptor initProp(String propName, String getMethod, String setMethod) {
        PropertyDescriptor pd;

        try {
            pd = new PropertyDescriptor(propName, InstGmosSouth.class, getMethod, setMethod);
            PropertySupport.setIterable(pd, true);
            PropertySupport.setQueryable(pd, true);
        } catch (IntrospectionException ex) {
            throw new RuntimeException(ex);
        }
        PRIVATE_PROP_MAP.put(pd.getName(), pd);
        return pd;
    }


    static {
        // Add all the superclass properties.
        PRIVATE_PROP_MAP.putAll(PROTECTED_PROP_MAP);

        DISPERSER_PROP = initProp("disperser", "getDisperserSouth", "setDisperserSouth");
        DISPERSER_PROP.setDisplayName("Disperser");
        FILTER_PROP = initProp("filter", "getFilterSouth", "setFilterSouth");
        FILTER_PROP.setDisplayName("Filter");
        FPUNIT_PROP = initProp("fpu", "getFPUnitSouth", "setFPUnitSouth");
        FPUNIT_PROP.setDisplayName("FPU");
    }


    public InstGmosSouth() {
        super(SP_TYPE);
        setDisperser(GmosSouthType.DisperserSouth.DEFAULT);
        setFilter(GmosSouthType.FilterSouth.DEFAULT);
        setFPUnit(GmosSouthType.FPUnitSouth.DEFAULT);
        setStageMode(GmosSouthType.StageModeSouth.FOLLOW_XYZ);
        setDetectorManufacturer(GmosCommonType.DetectorManufacturer.HAMAMATSU);
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    @Override
    public Set<Site> getSite() {
        return Site.SET_GS;
    }

    public String getPhaseIResourceName() {
        return "gemGMOS-S";
    }

    // overridden solely for the creation of the property descriptor
    public GmosSouthType.DisperserSouth getDisperserSouth() {
        return super.getDisperser();
    }

    // overridden solely for the creation of the property descriptor
    public void setDisperserSouth(GmosSouthType.DisperserSouth disperser) {
        super.setDisperser(disperser);
    }

    protected GmosCommonType.DisperserBridge getDisperserBridge() {
        return GmosSouthType.DISPERSER_BRIDGE;
    }

    // overridden solely for the creation of the property descriptor
    public GmosSouthType.FilterSouth getFilterSouth() {
        return super.getFilter();
    }

    // overridden solely for the creation of the property descriptor
    public void setFilterSouth(GmosSouthType.FilterSouth filter) {
        super.setFilter(filter);
    }

    public GmosCommonType.FilterBridge getFilterBridge() {
        return GmosSouthType.FILTER_BRIDGE;
    }

    // overridden solely for the creation of the property descriptor
    public GmosSouthType.FPUnitSouth getFPUnitSouth() {
        return super.getFPUnit();
    }

    // overridden solely for the creation of the property descriptor
    public void setFPUnitSouth(GmosSouthType.FPUnitSouth fpunit) {
        super.setFPUnit(fpunit);
    }

    public GmosCommonType.FPUnitBridge getFPUnitBridge() {
        return GmosSouthType.FPUNIT_BRIDGE;
    }

    protected GmosCommonType.StageModeBridge getStageModeBridge() {
        return GmosSouthType.STAGE_MODE_BRIDGE;
    }

    public static List<InstConfigInfo> getInstConfigInfo() {
        List<InstConfigInfo> configInfo = InstGmosCommon.getCommonInstConfigInfo();
        configInfo.add(new InstConfigInfo(AMP_COUNT_PROP));
        configInfo.add(new InstConfigInfo(DISPERSER_PROP));
        configInfo.add(new InstConfigInfo(FILTER_PROP));
        configInfo.add(new InstConfigInfo(FPUNIT_PROP));
        configInfo.add(new InstConfigInfo(DETECTOR_MANUFACTURER_PROP));
        return configInfo;
    }

    // Observing Wavelength Injection
    public static final ConfigInjector WAVELENGTH_INJECTOR = ConfigInjector.create(
            new ObsWavelengthCalc3<DisperserSouth, FilterSouth, Double>() {
                public PropertyDescriptor descriptor1() {
                    return DISPERSER_PROP;
                }

                public PropertyDescriptor descriptor2() {
                    return FILTER_PROP;
                }

                public PropertyDescriptor descriptor3() {
                    return DISPERSER_LAMBDA_PROP;
                }

                public String calcWavelength(DisperserSouth d, FilterSouth f, Double cwl) {
                    return InstGmosSouth.calcWavelength(d, f, cwl);
                }
            }
    );

    @Override
    public CalibrationKey extractKey(ISysConfig instrument) {
        //-- get some common values
        GmosCommonType.Binning xBin = (GmosCommonType.Binning) get(instrument, InstGmosCommon.CCD_X_BIN_PROP);
        GmosCommonType.Binning yBin = (GmosCommonType.Binning) get(instrument, InstGmosCommon.CCD_Y_BIN_PROP);
        GmosCommonType.AmpGain ampGain = (GmosCommonType.AmpGain) get(instrument, InstGmosCommon.AMP_GAIN_CHOICE_PROP);
        GmosCommonType.FPUnitMode fpUnitMode = (GmosCommonType.FPUnitMode) get(instrument, InstGmosCommon.FPU_MODE_PROP);
        GmosSouthType.DisperserSouth disperser = (GmosSouthType.DisperserSouth) get(instrument, InstGmosCommon.DISPERSER_PROP_NAME);
        GmosSouthType.FilterSouth filter = (GmosSouthType.FilterSouth) get(instrument, InstGmosCommon.FILTER_PROP_NAME);

        //-- detect slit width
        GmosSouthType.FPUnitSouth fpUnit;
        if (fpUnitMode == GmosCommonType.FPUnitMode.BUILTIN) {
            fpUnit = (GmosSouthType.FPUnitSouth) instrument.getParameter("fpu").getValue();
        } else {
            // translate custom slit width to a known longslit fpunit with same slit width for calibration lookup
            // TODO: this could be improved by only looking at the slit width instead of looking at the actual fp unit
            GmosCommonType.CustomSlitWidth customSlitWidth = (GmosCommonType.CustomSlitWidth) get(instrument, InstGmosCommon.CUSTOM_SLIT_WIDTH);
            switch (customSlitWidth) {
                case CUSTOM_WIDTH_0_25:
                    fpUnit = GmosSouthType.FPUnitSouth.LONGSLIT_1;
                    break;
                case CUSTOM_WIDTH_0_50:
                    fpUnit = GmosSouthType.FPUnitSouth.LONGSLIT_2;
                    break;
                case CUSTOM_WIDTH_0_75:
                    fpUnit = GmosSouthType.FPUnitSouth.LONGSLIT_3;
                    break;
                case CUSTOM_WIDTH_1_00:
                    fpUnit = GmosSouthType.FPUnitSouth.LONGSLIT_4;
                    break;
                case CUSTOM_WIDTH_1_50:
                    fpUnit = GmosSouthType.FPUnitSouth.LONGSLIT_5;
                    break;
                case CUSTOM_WIDTH_2_00:
                    fpUnit = GmosSouthType.FPUnitSouth.LONGSLIT_6;
                    break;
                case CUSTOM_WIDTH_5_00:
                    fpUnit = GmosSouthType.FPUnitSouth.LONGSLIT_7;
                    break;
                default:
                    // no translation possible, return custom mask value
                    fpUnit = GmosSouthType.FPUnitSouth.CUSTOM_MASK;
            }
        }

        // check order and wavelength (values will only be present in case of spectroscopy)
        Double wavelength = getWavelength(instrument) * 1000.; // adjust scaling of wavelength from um to nm (as used in config tables)
        GmosCommonType.Order order = GmosCommonType.Order.ZERO;
        IParameter orderParameter = instrument.getParameter("disperserOrder");
        if (orderParameter != null) {
            order = (GmosCommonType.Order) orderParameter.getValue();
        }

        ConfigKeyGmosSouth config = new ConfigKeyGmosSouth(disperser, filter, fpUnit, xBin, yBin, order, ampGain);
        return new CalibrationKeyImpl.WithWavelength(config, wavelength);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getPixelSize() {
        return getDetectorManufacturer().pixelSizeSouth();
    }


    /**
     * This convenience method implements the algorithm for determining
     * the actual CCD Gain value based upon the CCD choices actually
     * selected.  The values are given in JIRA task REL-1269.
     */
    @Override
    public String getMeanGain(final GmosCommonType.AmpGain gain,
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
                    return "5.0";
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return "2.5";
                }
            } else if (readMode == GmosCommonType.AmpReadMode.SLOW) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    return "4.4";
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return "2.2";
                }
            }
        } else if (detectorManufacturer == GmosCommonType.DetectorManufacturer.HAMAMATSU) {
            if (readMode == GmosCommonType.AmpReadMode.FAST) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    return "5.1";
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return "1.4";
                }
            } else if (readMode == GmosCommonType.AmpReadMode.SLOW) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    // NOTE: This value was never specified, and indeed, it is not of much interest.
                    return "4.4";
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return "1.7";
                }
            }
        }

        return logAndThrowUnexpectedParametersException(gain, readMode, detectorManufacturer);
    }


    /**
     * This convenience method implements the algorithm for determining
     * the actual mean read noise via a lookup table.
     */
    @Override
    public String getMeanReadNoise(final GmosCommonType.AmpGain gain,
                                   final GmosCommonType.AmpReadMode readMode,
                                   final GmosCommonType.DetectorManufacturer detectorManufacturer) {

        if (detectorManufacturer == GmosCommonType.DetectorManufacturer.E2V) {
            if (readMode == GmosCommonType.AmpReadMode.FAST) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    return "7.4";
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return "4.3";
                }
            } else if (readMode == GmosCommonType.AmpReadMode.SLOW) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    return "5.0";
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return "3.7";
                }
            }
        } else if (detectorManufacturer == GmosCommonType.DetectorManufacturer.HAMAMATSU) {
            if (readMode == GmosCommonType.AmpReadMode.FAST) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    return "7.7";
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return "5.6";
                }
            } else if (readMode == GmosCommonType.AmpReadMode.SLOW) {
                if (gain == GmosCommonType.AmpGain.HIGH) {
                    // NOTE: This value was never specified, and indeed, it is not of much interest.
                    return "4.8";
                } else if (gain == GmosCommonType.AmpGain.LOW) {
                    return "4.0";
                }
            }
        }

        return logAndThrowUnexpectedParametersException(gain, readMode, detectorManufacturer);
    }

    protected GmosCommonType.AmpCount defaultAmpCountForE2V() {
        return GmosCommonType.AmpCount.THREE;
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
