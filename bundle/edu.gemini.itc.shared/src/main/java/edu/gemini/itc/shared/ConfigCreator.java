package edu.gemini.itc.shared;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.DefaultConfig;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.guide.GuideProbe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Using the ITC parameters creates a mini-config containing the items
 * necessary for the observation overhead calculations.
 * The config has number of steps equal to number of exposures. Even though
 * just two steps would be enough to calculate overheads, I went for more
 * straightforward solution, simulating the OT sequence, which I believe
 * would make any future adds-ons easier to implement (and in case the ITC
 * may become more sophisticated in future).
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
    public static final ItemKey GuideWithCWFS1 = new ItemKey("telescope:guideWithODGW1");
    public static final ItemKey AOGuideStarTypeKey = new ItemKey("adaptive optics:guideStarType");
    public static final ItemKey AOSystemKey = new ItemKey("adaptive optics:aoSystem");
    public static final ItemKey AOFieldLensKey = new ItemKey("adaptive optics:fieldLens");

    private final ItcParameters itcParams;
    private final ObservationDetails obsDetailParams;

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
            } else if (calcMethod instanceof Spectroscopy) {
                offsetList.addAll(spectroscopyOffsets);
            }
        }

        for (int i = 0; i < dc.length; i++) {
            Config step = new DefaultConfig();
            dc[i] = step;

            step.putItem(ExposureTimeKey, obsDetailParams.exposureTime());
            step.putItem(ObserveTypeKey, "OBJECT");
            step.putItem(ObserveClassKey, "SCIENCE");
            step.putItem(CoaddsKey, numberCoadds);
            step.putItem(TelescopePKey, "0");
            step.putItem(TelescopeQKey, offsetList.get(i));

            if (itcParams.telescope().getWFS().equals(GuideProbe.Type.PWFS)) {
                step.putItem(GuideWithPWFS2Key, "guide");
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
            step.putItem(InstInstrumentKey, "GNIRS");
            step.putItem(SlitWidthKey, gnirsParams.slitWidth().logValue());

            if (gnirsParams.altair().isDefined()) {
                AltairParameters altairParameters = gnirsParams.altair().get();
                step.putItem(AOGuideStarTypeKey, altairParameters.wfsMode().displayValue());
                step.putItem(AOSystemKey, "Altair");
                step.putItem(AOFieldLensKey, altairParameters.fieldLens().displayValue());
            }
        }
        return conf;
    }

    public Config[] createNiriConfig(NiriParameters niriParams, int numExp) {
        Config[] conf = createCommonConfig(numExp);

        for (Config step : conf) {
            step.putItem(ReadModeKey, (niriParams.readMode()));
            step.putItem(InstInstrumentKey, "NIRI");
            step.putItem(BuiltinROIKey, (niriParams.builtinROI()));
            step.putItem(DisperserKey, (niriParams.grism().displayValue()));

            if (niriParams.altair().isDefined()) {
                AltairParameters altairParameters = niriParams.altair().get();
                step.putItem(AOGuideStarTypeKey, altairParameters.wfsMode().displayValue());
                step.putItem(AOSystemKey, "Altair");
                step.putItem(AOFieldLensKey, altairParameters.fieldLens().displayValue());
            }
        }
        return conf;
    }

    public Config[] createGmosConfig(GmosParameters gmosParams, int numExp) {
        Config[] conf = createCommonConfig(numExp);

        for (Config step : conf) {
            System.out.println("SIte: " + gmosParams.site().displayName);
            if (gmosParams.site().displayName.equals("Gemini North")) {
                step.putItem(InstInstrumentKey, "GMOS-N");
            } else if (gmosParams.site().displayName.equals("Gemini South")) {
                step.putItem(InstInstrumentKey, "GMOS-S");
            } else {
                throw new Error("invalid site");
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
            if (gmosParams.ccdType().displayValue().equals("E2V")) {
                step.putItem(AmpCount, GmosCommonType.AmpCount.SIX);
            } else {
                step.putItem(AmpCount, GmosCommonType.AmpCount.TWELVE);
            }
        }
        return conf;
    }

    public Config[] createNifsConfig(NifsParameters nifsParams, int numExp) {
        Config[] conf = createCommonConfig(numExp);

        for (Config step : conf) {
            step.putItem(ReadModeKey, (nifsParams.readMode()));
            step.putItem(InstInstrumentKey, "NIFS");

            if (nifsParams.altair().isDefined()) {
                AltairParameters altairParameters = nifsParams.altair().get();
                step.putItem(AOGuideStarTypeKey, altairParameters.wfsMode().displayValue());
                step.putItem(AOSystemKey, "Altair");
            }
        }
        return conf;
    }

    public Config[] createGsaoiConfig(GsaoiParameters gsaoiParams, int numExp) {
        Config[] conf = createCommonConfig(numExp);
        final double sf = obsDetailParams.calculationMethod().sourceFraction();
        int stepNum = 1;
        int numLargeOffsets = gsaoiParams.largeSkyOffset();

        String g = "guide";
        String p = "park";
        int numFullAB = numExp / 8;
        List<String> guideStatusABBA = new ArrayList<>(Arrays.asList(g,g,g,g,p,p,p,p, p,p,p,p,g,g,g,g));
        List<String> guideStatusList = new ArrayList<>(conf.length);

        // 1. on-source fraction = 1.0, large sky offsets = 0: ABAB dithering, no sky offsets, leave offsets as is. All steps guided.
        // 2. on-source fraction = 0.5, large sky offsets = 0: obj-sky-sky-obj patterns with ABAB dithering. Sky unguided. Offset to the sky = 120"
        // 3. on-source fraction = any, large sky offsets = 0:
        // 4. on-source fraction = any, large sky offsets = n: obj-sky-obj-sky pattern with ABAB dithering.
        //    Large offset number overrides the source fraction. Offset to the sky = 300"
        if (numLargeOffsets == 0) {
            if (sf == 1) {
                Collections.fill(guideStatusList, "guide");
            } else if (sf == 0.5) {
                int shortABLength = (numExp % 8) / 2;
                for (int i = 0; i < (conf.length / 16 + 1); i++) {
                    guideStatusList.addAll(guideStatusABBA);
                }
                for (int i = 0; i < shortABLength; i++) {
                    int index = (i + (conf.length / 8) * 8) - 1;
                    guideStatusList.add(index, "guide");
                    guideStatusList.add(index + shortABLength, "park");
                }
            }
        } /* else {

            if (numLargeOffsets % 2 != 0) {
                throw new IllegalArgumentException(
                    "Number of exposures must be a multiple of requested number of > 5 arcmin sky offsets");
            } else {
                int numDitheringPoints = numExp / (numLargeOffsets * 2);
            }
        }

        for (int i=1; i < conf.length; i=+4) {

        } */
        for (Config step : conf) {
            step.putItem(ReadModeKey, (gsaoiParams.readMode()));
            step.putItem(InstInstrumentKey, "GSAOI");
            step.putItem(GuideWithCWFS1, "guide");

            stepNum = stepNum + 1;
        }
        return conf;
    }

    public Config[] createF2Config(Flamingos2Parameters f2Params, int numExp) {
        Config[] conf = createCommonConfig(numExp);

        for (Config step : conf) {
            step.putItem(ReadModeKey, (f2Params.readMode()));
            step.putItem(InstInstrumentKey, "Flamingos2");
            step.putItem(DisperserKey, f2Params.grism());
            step.putItem(FPUKey, f2Params.mask());
            if (itcParams.telescope().getWFS().equals(GuideProbe.Type.PWFS)) {
                step.putItem(GuideWithPWFS2Key, "guide");
            } else if (itcParams.telescope().getWFS().equals(GuideProbe.Type.OIWFS)) {
                step.putItem(GuideWithOIWFSKey, "guide");

            }
        }
            return conf;

    }



}

