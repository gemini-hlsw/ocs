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
 */

public final class ConfigCreator {
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

    public static final double GSAOI_SMALL_SKY_OFFSET = 120.0; // arcsec (assumed in case of sky offset <5')
    public static final double GSAOI_LARGE_SKY_OFFSET = 310.0; // arcsec (assumed in case of sky offset >5')

    public class ConfigCreatorResult {
        private final List<String> warnings;
        private String offsetMessage;
        private final Config[] config;

        public ConfigCreatorResult(final Config[] config) {
            warnings = new ArrayList<>();
            offsetMessage = "";
            this.config = config;
        }

        public final List<String> getWarnings() {
            return warnings;
        }

        public final boolean hasWarnings() {
            return warnings.size() != 0;
        }

        public final void addWarning(final String warning) {
            warnings.add(warning);
        }

        public final String getOffsetMessage() {
            return offsetMessage;
        }

        public final void setOffsetMessage(final String offsetMessage) {
            this.offsetMessage = offsetMessage;
        }

        public final Config[] getConfig() {
            return config;
        }
    }

    public ConfigCreator(final ItcParameters p) {
        this.itcParams = p;
        this.obsDetailParams = p.observation();
    }

    // create part of config common for all instruments
    private final ConfigCreatorResult createCommonConfig(final int numberExposures) {
        final CalculationMethod calcMethod = obsDetailParams.calculationMethod();
        final ConfigCreatorResult result = new ConfigCreatorResult(new DefaultConfig[numberExposures]);
        if (numberExposures < 1) {
            result.addWarning("Warning: Observation overheads cannot be calculated for the number of exposures = 0.");
        }
        final int numberCoadds = calcMethod.coaddsOrElse(1);
        final double offset = calcMethod.offset();
        // for spectroscopic observations we consider ABBA offset pattern
        final List<Double> spectroscopyOffsets = new ArrayList<>(Arrays.asList(0.0, offset, offset, 0.0));
        // for imaging observations we consider ABAB offset pattern
        final List<Double> imagingOffsets = new ArrayList<>(Arrays.asList(0.0, offset, 0.0, offset));
        final List<Double> offsetList = new ArrayList<>();

        for (int i = 0; i < (1 + numberExposures / 4); i++) {
            if (calcMethod instanceof Imaging) {
                offsetList.addAll(imagingOffsets);
                result.setOffsetMessage("ABAB dithering pattern");
            } else if (calcMethod instanceof Spectroscopy) {
                offsetList.addAll(spectroscopyOffsets);
                result.setOffsetMessage("ABBA dithering pattern");
            }
        }

        for (int i = 0; i < numberExposures; i++) {
            final Config step = new DefaultConfig();

            step.putItem(ExposureTimeKey, obsDetailParams.exposureTime());
            step.putItem(ObserveTypeKey, InstConstants.SCIENCE_OBSERVE_TYPE);
            step.putItem(ObserveClassKey, ObsClass.SCIENCE);
            step.putItem(CoaddsKey, numberCoadds);
            step.putItem(TelescopePKey, 0.0);
            step.putItem(TelescopeQKey, offsetList.get(i));


            if (itcParams.telescope().getWFS().equals(GuideProbe.Type.PWFS)) {
                step.putItem(GuideWithPWFS2Key, StandardGuideOptions.Value.guide);
            }

            result.getConfig()[i] = step;
        }

        return result;
    }

    /**
     * Instrument-specific configs
     */
    public final ConfigCreatorResult createGnirsConfig(final GnirsParameters gnirsParams, final int numExp) {
        final ConfigCreatorResult result = createCommonConfig(numExp);

        for (final Config step : result.getConfig()) {
            step.putItem(ReadModeKey, (gnirsParams.readMode()));
            step.putItem(InstInstrumentKey, SPComponentType.INSTRUMENT_GNIRS);
            step.putItem(SlitWidthKey, gnirsParams.slitWidth());

            if (gnirsParams.altair().isDefined()) {
                final AltairParameters altairParameters = gnirsParams.altair().get();
                step.putItem(AOGuideStarTypeKey, altairParameters.wfsMode());
                step.putItem(AOSystemKey, SPComponentType.AO_ALTAIR);
            }
        }
        return result;
    }

    public final ConfigCreatorResult createNiriConfig(final NiriParameters niriParams, final int numExp) {
        final ConfigCreatorResult result = createCommonConfig(numExp);

        for (final Config step : result.getConfig()) {
            step.putItem(ReadModeKey, (niriParams.readMode()));
            step.putItem(InstInstrumentKey, SPComponentType.INSTRUMENT_NIRI);
            step.putItem(BuiltinROIKey, (niriParams.builtinROI()));
            step.putItem(DisperserKey, (niriParams.grism()));

            if (niriParams.altair().isDefined()) {
                final AltairParameters altairParameters = niriParams.altair().get();
                step.putItem(AOGuideStarTypeKey, altairParameters.wfsMode());
                step.putItem(AOSystemKey, SPComponentType.AO_ALTAIR);
            }
        }
        return result;
    }

    public final ConfigCreatorResult createGmosConfig(final GmosParameters gmosParams, final int numExp) {
        final ConfigCreatorResult result = createCommonConfig(numExp);

        for (final Config step : result.getConfig()) {
            if (gmosParams.site().equals(Site.GN)) {
                step.putItem(InstInstrumentKey, SPComponentType.INSTRUMENT_GMOS);
            } else if (gmosParams.site().equals(Site.GS)) {
                step.putItem(InstInstrumentKey, SPComponentType.INSTRUMENT_GMOSSOUTH);
            } else {
                throw new Error("Invalid site");
            }

            step.putItem(FPUKey, (gmosParams.fpMask()));
            step.putItem(AmpReadModeKey, (gmosParams.ampReadMode()));
            step.putItem(DetectorManufacturerKey, (gmosParams.ccdType()));
            step.putItem(BuiltinROIKey, (gmosParams.builtinROI()));
            step.putItem(DisperserKey, (gmosParams.grating()));

            final GmosCommonType.Binning xbin = GmosCommonType.Binning.getBinningByValue(gmosParams.spectralBinning());
            final GmosCommonType.Binning ybin = GmosCommonType.Binning.getBinningByValue(gmosParams.spatialBinning());
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
        return result;
    }

    public final ConfigCreatorResult createNifsConfig(final NifsParameters nifsParams, final int numExp) {
        final ConfigCreatorResult result = createCommonConfig(numExp);

        for (final Config step : result.getConfig()) {
            step.putItem(ReadModeKey, (nifsParams.readMode()));
            step.putItem(InstInstrumentKey, SPComponentType.INSTRUMENT_NIFS);

            if (nifsParams.altair().isDefined()) {
                final AltairParameters altairParameters = nifsParams.altair().get();
                step.putItem(AOGuideStarTypeKey, altairParameters.wfsMode());
                step.putItem(AOSystemKey, SPComponentType.AO_ALTAIR);
            }
        }
        return result;
    }

    public final ConfigCreatorResult createGsaoiConfig(final GsaoiParameters gsaoiParams, final int numExp) {
        final ConfigCreatorResult result = createCommonConfig(numExp);
        final double sourceFraction = itcParams.observation().sourceFraction();
        int stepNum = 0;
        final int numLargeOffsets = gsaoiParams.largeSkyOffset();
        boolean error = false;

        final GuideOption g = StandardGuideOptions.Value.guide;
        final GuideOption p = StandardGuideOptions.Value.park;

        final List<GuideOption> guideStatusList = new ArrayList<>(numExp);

        /* GSAOI offset overheads and sequence structure is different from all other instruments. In addition
         * the unguided sky sequence for the case of sky offset is >5' is made into a separate observation in the OT,
         * whereas in the ITC it should be included in the same observation sequence. So the implementation
         * of the offset and guide status sequence is far from elegant...
         *
         * The overhead calculation is currently supported for source fraction 1 and 0.5. Number of on- and off-source steps must be equal.
         *    1. on-source fraction = 1.0, large sky offsets = 0: Regular ABAB dithering, no sky offsets, all steps guided.
         *    2. on-source fraction = 1.0, large sky offsets !=0: Warning, no overheads calculation (using large sky offsets implies some steps being taken off-source).
         *    3. on-source fraction = 0.5, large sky offsets = 0: Sequence has obj-sky-sky-obj structure, with 4-point ABAB dithering at each position
         *                   (the last block can have less steps depending on requested number of exposures). Sky is unguided. Offset to the sky and back = 120"
         *    4. on-source fraction = 0.5, large sky offsets !=0: Sequence has obj-sky-obj-sky structure, with ABAB dithering at each position. Sky is unguided. Offset to the sky and back = 310". When changing from unguided to guided, the LGS reacquisition overhead is added.
         *    5. on-source fraction !=1.0 && !=0.5: Warning, no overhead calculations.
         */

        if ((sourceFraction != 1.0) && (sourceFraction != 0.5)) {
            result.addWarning("Warning: Observation overheads can only be calculated for the fraction of exposures on source 1.0 or 0.5.");
            error = true;
        }

        if (!error) {
            if (numLargeOffsets == 0) {
                if (sourceFraction == 1.0) {
                    Collections.fill(guideStatusList, g);
                    result.setOffsetMessage("ABAB dithering pattern and no sky offsets");
                } else if (sourceFraction == 0.5) {
                    final List<GuideOption> steps = Arrays.asList(g, p);
                    result.setOffsetMessage("science-sky-sky-science pattern with " + GSAOI_SMALL_SKY_OFFSET + "\" sky offset and 4-point dithering");
                    while (guideStatusList.size() < numExp) {
                        final int rest = numExp - guideStatusList.size();
                        final int chunkLength;
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
                final int exposuresPerGroup = numExp / numLargeOffsets;
                final int leftOver = numExp % numLargeOffsets;

                if (numLargeOffsets > numExp / 2) {
                    result.addWarning("Warning: Observation overheads cannot be calculated: number of sky offsets >5' is greater than half of the total science exposures.");
                    error = true;
                }

                if (sourceFraction == 1.0) {
                    result.addWarning("Warning: Observation overheads cannot be calculated: the fraction of exposures on source is incompatible with the number of sky offsets >5'.");
                    error = true;
                }

                if ((exposuresPerGroup % 2 != 0) || (leftOver != 0)) {
                    result.addWarning("Warning: Observation overheads cannot be calculated: uneven numbers of object and sky exposures. Please change the number of exposures, or the number of sky offsets >5'.");
                    error = true;
                }


                if (!error) {
                    result.setOffsetMessage("science-sky-science-sky pattern with " + GSAOI_LARGE_SKY_OFFSET + "\" sky offset and ABAB dithering at each position");
                    final int sub = exposuresPerGroup / 2;

                    for (int i = 0; i < numLargeOffsets; i++) {
                        guideStatusList.addAll(Collections.nCopies(sub, g));
                        guideStatusList.addAll(Collections.nCopies(sub, p));
                    }
                }
            }
        }

        for (Config step : result.getConfig()) {
            step.putItem(ReadModeKey, (gsaoiParams.readMode()));
            step.putItem(InstInstrumentKey, SPComponentType.INSTRUMENT_GSAOI);
            if (!guideStatusList.isEmpty()) {
                final GuideOption currentGuideStatus = guideStatusList.get(stepNum);

                step.putItem(GuideWithCWFS1, currentGuideStatus);
                if (currentGuideStatus.equals(StandardGuideOptions.Value.park)  ) {
                    if (numLargeOffsets == 0) {
                        step.putItem(TelescopeQKey, GSAOI_SMALL_SKY_OFFSET + (double)step.getItemValue(TelescopeQKey));
                    } else {
                        step.putItem(TelescopeQKey, GSAOI_LARGE_SKY_OFFSET + (double)step.getItemValue(TelescopeQKey));
                    }
                }
            }
            ++stepNum;
        }
        return result;
    }

    public final ConfigCreatorResult createF2Config(final Flamingos2Parameters f2Params, final int numExp) {
        final ConfigCreatorResult result = createCommonConfig(numExp);

        for (final Config step : result.getConfig()) {
            step.putItem(ReadModeKey, (f2Params.readMode()));
            step.putItem(InstInstrumentKey, SPComponentType.INSTRUMENT_FLAMINGOS2);
            step.putItem(DisperserKey, f2Params.grism());
            step.putItem(FPUKey, f2Params.mask());
            if (itcParams.telescope().getWFS().equals(GuideProbe.Type.PWFS)) {
                step.putItem(GuideWithPWFS2Key, StandardGuideOptions.Value.guide);
            } else if (itcParams.telescope().getWFS().equals(GuideProbe.Type.OIWFS)) {
                step.putItem(GuideWithOIWFSKey, StandardGuideOptions.Value.guide);

            }
        }
        return result;

    }
}

