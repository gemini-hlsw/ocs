package edu.gemini.itc.shared;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.DefaultConfig;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.guide.GuideOption;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.StandardGuideOptions;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.InstConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Using the ITC parameters creates a mini-config containing the items
 * necessary for the observation overhead calculations.
 * The config has number of steps equal to number of exposures.
 *
 * Created by osmirnov on 11/28/17.
 */

public class ConfigCreator {
    public static final ItemKey ObserveTypeKey = new ItemKey("observe:observeType");
    public static final ItemKey ObserveClassKey = new ItemKey("observe:class");
    public static final ItemKey ExposureTimeKey = new ItemKey("observe:exposureTime");
    public static final ItemKey CoaddsKey = new ItemKey("observe:coadds");
    public static final ItemKey InstInstrumentKey = new ItemKey("instrument:instrument");
    public static final ItemKey ReadModeKey = new ItemKey("instrument:readMode");
    public static final ItemKey AmpReadModeKey = new ItemKey("instrument:ampReadMode");
    public static final ItemKey SlitWidthKey = new ItemKey("instrument:slitWidth");
    public static final ItemKey DisperserKey = new ItemKey("instrument:disperser");
    public static final ItemKey BuiltinROIKey = new ItemKey("instrument:builtinROI");
    public static final ItemKey FPUKey = new ItemKey("instrument:fpu");
    public static final ItemKey CcdXBinning = new ItemKey("instrument:ccdXBinning");
    public static final ItemKey CcdYBinning = new ItemKey("instrument:ccdYBinning");
    public static final ItemKey AmpGain = new ItemKey("instrument:gainChoice");
    public static final ItemKey AmpCount = new ItemKey("instrument:ampCount");
    public static final ItemKey DetectorManufacturerKey = new ItemKey("instrument:detectorManufacturer");
    public static final ItemKey GuideWithPWFS2Key = new ItemKey("telescope:guideWithPWFS2");
    public static final ItemKey GuideWithOIWFSKey = new ItemKey("telescope:guideWithOIWFS");
    public static final ItemKey TelescopeQKey = new ItemKey("telescope:q");
    public static final ItemKey TelescopePKey = new ItemKey("telescope:p");
    public static final ItemKey GuideWithCWFS1 = new ItemKey("telescope:guideWithCWFS1");
    public static final ItemKey AOGuideStarTypeKey = new ItemKey("adaptive optics:guideStarType");
    public static final ItemKey AOSystemKey = new ItemKey("adaptive optics:aoSystem");

    private final ItcParameters itcParams;
    private final ObservationDetails obsDetailParams;

    private static String overheadTableWarning = "";
    private static String offsetString = "";

    public static final double GSAOI_SMALL_SKY_OFFSET = 120.0; // arcsec (assumed in case of sky offset <5')
    public static final double GSAOI_LARGE_SKY_OFFSET = 310.0; // arcsec (assumed in case of sky offset >5')

    public ConfigCreator(final ItcParameters p) {
        this.itcParams = p;
        this.obsDetailParams = p.observation();
    }

    // create part of config common for all instruments
    public Config[] createCommonConfig(int numberExposures) {
        final CalculationMethod calcMethod = obsDetailParams.calculationMethod();
        int numberCoadds = calcMethod.coaddsOrElse(1);
        Config[] dc = new DefaultConfig[numberExposures];
        String offset = Double.toString(calcMethod.offset());
        // for spectroscopic observations we consider ABBA offset pattern
        List<String> spectroscopyOffsets = new ArrayList<>(Arrays.asList("0", offset, offset, "0"));
        // for imaging observations we consider ABAB offset pattern
        List<String> imagingOffsets = new ArrayList<>(Arrays.asList("0", offset, "0", offset));
        List<String> offsetList = new ArrayList<>();

        for (int i = 0; i < (1 + numberExposures / 4); i++) {
            if (calcMethod instanceof Imaging) {
                offsetList.addAll(imagingOffsets);
                offsetString = "ABAB dithering pattern";
            } else if (calcMethod instanceof Spectroscopy) {
                offsetList.addAll(spectroscopyOffsets);
                offsetString = "ABBA dithering pattern";
            }
        }

        for (int i = 0; i < dc.length; i++) {
            Config step = new DefaultConfig();
            dc[i] = step;

            step.putItem(ExposureTimeKey, obsDetailParams.exposureTime());
            step.putItem(ObserveTypeKey, InstConstants.SCIENCE_OBSERVE_TYPE);
            step.putItem(ObserveClassKey, ObsClass.SCIENCE);
            step.putItem(CoaddsKey, numberCoadds);
            step.putItem(TelescopePKey, "0");
            step.putItem(TelescopeQKey, offsetList.get(i));


            if (itcParams.telescope().getWFS().equals(GuideProbe.Type.PWFS)) {
                step.putItem(GuideWithPWFS2Key, StandardGuideOptions.Value.guide);
            }
        }
        return dc;
    }

    /**
     * Instrument-specific configs
     */
    public Config[] createGnirsConfig(GnirsParameters gnirsParams, int numExp) {
        Config[] conf = createCommonConfig(numExp);

        for (Config step : conf) {
            step.putItem(ReadModeKey, (gnirsParams.readMode()));
            step.putItem(InstInstrumentKey, SPComponentType.INSTRUMENT_GNIRS.readableStr);
            step.putItem(SlitWidthKey, gnirsParams.slitWidth().logValue());

            if (gnirsParams.altair().isDefined()) {
                AltairParameters altairParameters = gnirsParams.altair().get();
                step.putItem(AOGuideStarTypeKey, altairParameters.wfsMode().displayValue());
                step.putItem(AOSystemKey, SPComponentType.AO_ALTAIR.narrowType);
            }
        }
        return conf;
    }

    public Config[] createNiriConfig(NiriParameters niriParams, int numExp) {
        Config[] conf = createCommonConfig(numExp);

        for (Config step : conf) {
            step.putItem(ReadModeKey, (niriParams.readMode()));
            step.putItem(InstInstrumentKey, SPComponentType.INSTRUMENT_NIRI.readableStr);
            step.putItem(BuiltinROIKey, (niriParams.builtinROI()));
            step.putItem(DisperserKey, (niriParams.grism().displayValue()));

            if (niriParams.altair().isDefined()) {
                AltairParameters altairParameters = niriParams.altair().get();
                step.putItem(AOGuideStarTypeKey, altairParameters.wfsMode().displayValue());
                step.putItem(AOSystemKey, SPComponentType.AO_ALTAIR.narrowType);
            }
        }
        return conf;
    }

    public Config[] createGmosConfig(GmosParameters gmosParams, int numExp) {
        Config[] conf = createCommonConfig(numExp);

        for (Config step : conf) {
            if (gmosParams.site().displayName.equals(Site.GN.displayName)) {
                step.putItem(InstInstrumentKey, SPComponentType.INSTRUMENT_GMOS.readableStr);
            } else if (gmosParams.site().displayName.equals(Site.GS.displayName)) {
                step.putItem(InstInstrumentKey, SPComponentType.INSTRUMENT_GMOSSOUTH.readableStr);
            } else {
                throw new Error("Invalid site");
            }

            step.putItem(FPUKey, (gmosParams.fpMask().displayValue()));
            step.putItem(AmpReadModeKey, (gmosParams.ampReadMode()));
            step.putItem(DetectorManufacturerKey, (gmosParams.ccdType()));
            step.putItem(BuiltinROIKey, (gmosParams.builtinROI()));
            step.putItem(DisperserKey, (gmosParams.grating().displayValue()));
            GmosCommonType.Binning xbin = GmosCommonType.Binning.getBinningByValue(gmosParams.spectralBinning());
            GmosCommonType.Binning ybin = GmosCommonType.Binning.getBinningByValue(gmosParams.spatialBinning());
            step.putItem(CcdXBinning, xbin);
            step.putItem(CcdYBinning, ybin);
            step.putItem(AmpGain, (gmosParams.ampGain()));
            if (gmosParams.ccdType().equals(GmosCommonType.DetectorManufacturer.E2V)) {
                step.putItem(AmpCount, GmosCommonType.AmpCount.SIX);
            } else if (gmosParams.ccdType().equals(GmosCommonType.DetectorManufacturer.HAMAMATSU)){
                step.putItem(AmpCount, GmosCommonType.AmpCount.TWELVE);
            } else {
                throw new Error("Invalid detector type");
            }
        }
        return conf;
    }

    public Config[] createNifsConfig(NifsParameters nifsParams, int numExp) {
        Config[] conf = createCommonConfig(numExp);

        for (Config step : conf) {
            step.putItem(ReadModeKey, (nifsParams.readMode()));
            step.putItem(InstInstrumentKey, SPComponentType.INSTRUMENT_NIFS.readableStr);

            if (nifsParams.altair().isDefined()) {
                AltairParameters altairParameters = nifsParams.altair().get();
                step.putItem(AOGuideStarTypeKey, altairParameters.wfsMode().displayValue());
                step.putItem(AOSystemKey, SPComponentType.AO_ALTAIR.narrowType);
            }
        }
        return conf;
    }

    public Config[] createGsaoiConfig(GsaoiParameters gsaoiParams, int numExp) {
        Config[] conf = createCommonConfig(numExp);
        final double sf = obsDetailParams.calculationMethod().sourceFraction();
        int stepNum = 0;
        int numLargeOffsets = gsaoiParams.largeSkyOffset();

        GuideOption g = StandardGuideOptions.Value.guide;
        GuideOption p = StandardGuideOptions.Value.park;

        List<GuideOption> guideStatusList = new ArrayList<>(conf.length);

        /**
         * GSAOI offset overheads and sequence structure is different from all other instruments. In addition
         * the unguided sky sequence for the case of sky offset is >5' is made into a separate observation in the OT,
         * whereas in the ITC it should be included in the same observation sequence. So the implementation
         * of the offset and guide status sequence is far from elegant...
         *
         * The overhead calculation is currently supported for source fraction 1 and 0.5. Number of on- and off-source steps must be equal.
         *    1. on-source fraction = 1.0, large sky offsets = 0: Regular ABAB dithering, no sky offsets, all steps guided.
         *    2. on-source fraction = 1.0, large sky offsets !=0: Warning, no overheads calculation (using large sky offsets implies some steps being taken off-source).
         *    3. on-source fraction = 0.5, large sky offsets = 0: Sequence has obj-sky-sky-obj structure, with 4-point ABAB dithering at each position
         *                   (the last block can have less steps depending on requested number of exposures). Sky is unguided. Offset to the sky and back = 120"
         *    4. on-source fraction = 0.5, large sky offsets !=0: Sequence has obj-sky-obj-sky structure, with ABAB dithering at each position. Sky is unguided. Offset to the sky and back = 310"
         *    5. on-source fraction !=1.0 && !=0.5: Warning, no overhead calculations.
         *
         */
        if (numLargeOffsets == 0) {
            if (sf == 1) {
                Collections.fill(guideStatusList, g);
                offsetString = "ABAB dithering pattern and no sky offsets";
            } else if (sf == 0.5) {
                List<GuideOption> steps = Arrays.asList(g, p);
                offsetString = "science-sky-sky-science pattern with "+GSAOI_SMALL_SKY_OFFSET+"\" sky offset and 4-point ABAB dithering at each position";
                while (guideStatusList.size() < conf.length) {
                    int rest = conf.length - guideStatusList.size();
                    int chunkLength;
                    if (rest >= 8)
                        chunkLength = 4;
                    else
                        chunkLength = rest / 2;

                    guideStatusList.addAll(Collections.nCopies(chunkLength, steps.get(0)));
                    guideStatusList.addAll(Collections.nCopies(chunkLength, steps.get(1)));

                    Collections.reverse(steps);
                }
            }
        } else {
            double sourceFraction = itcParams.observation().sourceFraction();
            int exposuresPerGroup = numExp / numLargeOffsets;
            int leftOver = numExp % numLargeOffsets;
            boolean error = false;

            overheadTableWarning = "";
            if ((sourceFraction != 1.0) && (sourceFraction != 0.5)) {
                overheadTableWarning = "Warning: Observation overheads can only be calculated for <b>fraction with source</b> values 1.0 or 0.5.";
                error = true;
            }
            if (sourceFraction == 1.0) {
                overheadTableWarning = "Warning: Observation overheads cannot be calculated: for selected <b>fraction with source<b> value 1.0 <b>number of sky offsets >5'</b> should be 0.";
                error = true;
            }

            if ((exposuresPerGroup % 2 != 0) || (leftOver % 2 != 0)) {
                overheadTableWarning = "Warning: Observation overheads cannot be calculated: uneven number of object and sky exposures. Please change <b>number of exposures</b> or <b>number of sky offsets >5'</b>.";
                error = true;
            }

            if (!error) {
                int[] ditheringPoints = new int[numLargeOffsets];
                int totalAssigned = 0;
                offsetString = "science-sky-science-sky pattern with "+GSAOI_LARGE_SKY_OFFSET+"\" sky offset and ABAB dithering at each position";

                for (int i = 0; i < (numLargeOffsets - 1); i++) {
                    ditheringPoints[i] = exposuresPerGroup;
                    totalAssigned += exposuresPerGroup;
                }
                ditheringPoints[numLargeOffsets - 1] = numExp - totalAssigned;

                for (int exps : ditheringPoints) {
                    int sub = exps / 2;

                    guideStatusList.addAll(Collections.nCopies(sub, g));
                    guideStatusList.addAll(Collections.nCopies(sub, p));
                }
            }
        }
        GuideOption currentGuideStatus;
        GuideOption previousGuideStatus = null;

        for (Config step : conf) {
            step.putItem(ReadModeKey, (gsaoiParams.readMode()));
            step.putItem(InstInstrumentKey, SPComponentType.INSTRUMENT_GSAOI.readableStr);
            if (!guideStatusList.isEmpty()) {
                currentGuideStatus = guideStatusList.get(stepNum);

                step.putItem(GuideWithCWFS1, currentGuideStatus);
                if (previousGuideStatus != null) {
                    if (!currentGuideStatus.equals(previousGuideStatus)) {
                        if (numLargeOffsets == 0) {
                            step.putItem(TelescopeQKey, Double.toString(GSAOI_SMALL_SKY_OFFSET + Double.parseDouble((String) step.getItemValue(TelescopeQKey))));
                        } else {
                            step.putItem(TelescopeQKey, Double.toString(GSAOI_LARGE_SKY_OFFSET + Double.parseDouble((String) step.getItemValue(TelescopeQKey))));
                        }
                    }
                }
                previousGuideStatus = currentGuideStatus;
            }

            stepNum = stepNum + 1;

        }
        return conf;
    }

    public Config[] createF2Config(Flamingos2Parameters f2Params, int numExp) {
        Config[] conf = createCommonConfig(numExp);

        for (Config step : conf) {
            step.putItem(ReadModeKey, (f2Params.readMode()));
            step.putItem(InstInstrumentKey, SPComponentType.INSTRUMENT_FLAMINGOS2.readableStr);
            step.putItem(DisperserKey, f2Params.grism());
            step.putItem(FPUKey, f2Params.mask());
            if (itcParams.telescope().getWFS().equals(GuideProbe.Type.PWFS)) {
                step.putItem(GuideWithPWFS2Key, StandardGuideOptions.Value.guide);
            } else if (itcParams.telescope().getWFS().equals(GuideProbe.Type.OIWFS)) {
                step.putItem(GuideWithOIWFSKey, StandardGuideOptions.Value.guide);

            }
        }
        return conf;

    }

    static public String getOverheadTableWarning() {
        return overheadTableWarning;
    }

    static public String getOffsetString() {
        return offsetString;
    }

}

