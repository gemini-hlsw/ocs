package edu.gemini.p2checker.rules.gmos;

import edu.gemini.p2checker.api.*;
import edu.gemini.p2checker.rules.altair.AltairRule;
import edu.gemini.p2checker.rules.general.GeneralRule;
import edu.gemini.p2checker.util.AbstractConfigRule;
import edu.gemini.p2checker.util.MdfConfigRule;
import edu.gemini.p2checker.util.NoPOffsetWithSlitRule;
import edu.gemini.p2checker.util.SequenceRule;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPProgramNode;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.skycalc.Offset;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.core.*;
import edu.gemini.spModel.data.YesNoType;
import edu.gemini.spModel.gemini.gmos.GmosCommonType.*;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.DisperserNorth;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FPUnitNorth;
import edu.gemini.spModel.gemini.gmos.GmosNorthType.FilterNorth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.DisperserSouth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FPUnitSouth;
import edu.gemini.spModel.gemini.gmos.GmosSouthType.FilterSouth;
import edu.gemini.spModel.gemini.gmos.InstGmosCommon;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.ImageQuality;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.SkyBackground;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.offset.OffsetPos;
import scala.Option;
import scala.runtime.AbstractFunction2;

import java.beans.PropertyDescriptor;
import java.util.*;

import static edu.gemini.spModel.gemini.gmos.GmosCommonType.DetectorManufacturer.HAMAMATSU;

/**
 * GMOS Rule set
 */
public final class GmosRule implements IRule {
    private static final String PREFIX = "GmosRule_";
    private static Collection<IConfigRule> GMOS_RULES = new ArrayList<>();

    //keys to access sequence elements
    private static final ItemKey FPU_KEY = new ItemKey("instrument:fpu");
    private static final ItemKey FPU_MODE_KEY = new ItemKey("instrument:fpuMode");
    private static final ItemKey DISPERSER_KEY = new ItemKey("instrument:disperser");
    private static final ItemKey FILTER_KEY = new ItemKey("instrument:filter");
    private static final ItemKey DETECTOR_KEY = new ItemKey("instrument:detectorManufacturer");
    private static final ItemKey GAIN_KEY = new ItemKey("instrument:gainChoice");
    private static final ItemKey CCD_X_BINNING_KEY = new ItemKey("instrument:ccdXBinning");
    private static final ItemKey CCD_Y_BINNING_KEY = new ItemKey("instrument:ccdYBinning");

    /**
     * either a filter or a grating should be defined whatever the fpu
     * WARN if (disperser == 'Mirror' && filter == 'none'), \
     */
    private static IConfigRule DISPERSER_AND_MIRROR_RULE = new AbstractConfigRule() {
        private static final String MESSAGE = "Mirror must be used with a filter, or select a grating " +
                "and a filter is optional";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            final Disperser disperser = getDisperser(config);
            final Filter filter = getFilter(config);
            if (disperser == null || filter == null) return null;

            if (disperser.isMirror() && filter.isNone()) {
                return new Problem(WARNING, PREFIX + "DISPERSER_AND_MIRROR_RULE",
                        MESSAGE, SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }
            return null;
        }
    };


    /**
     * Acquisition
     * Shouldn't contain grating
     * WARN if (class == 'Acquisition) and (disperser != 'Mirror'),
     */
    private static IConfigRule ACQUISITION_RULE = new AbstractConfigRule() {
        private static final String MESSAGE = "Acquisition observation should not contain grating";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            final ObsClass obsClass = SequenceRule.getObsClass(config);
            if (obsClass != ObsClass.ACQ && obsClass != ObsClass.ACQ_CAL) {
                return null;
            }
            final Disperser disperser = getDisperser(config);
            if (!disperser.isMirror()) {
                return new Problem(WARNING, PREFIX + "ACQUISITION_RULE", MESSAGE,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }
            return null;
        }
    };


    private static IConfigMatcher SCIENCE_DAYCAL_MATCHER = (config, step, elems) -> {
        ObsClass obsClass = SequenceRule.getObsClass(config);
        return obsClass == ObsClass.SCIENCE || obsClass == ObsClass.DAY_CAL;
    };

    private static IConfigMatcher IMAGING_MATCHER = (config, step, elems) -> {
        if (!SequenceRule.SCIENCE_MATCHER.matches(config, step, elems))
            return false;
        return getDisperser(config).isMirror() && getFPU(config, elems).isImaging();
    };

    private static IConfigMatcher SPECTROSCOPY_MATCHER = (config, step, elems) -> {
        if (!SequenceRule.SCIENCE_MATCHER.matches(config, step, elems))
            return false;
        if (!isSpecFPUnselected(config, elems)) return false;
        final Disperser disperser = getDisperser(config);
        return !disperser.isMirror();
    };

    private static IConfigMatcher N_S_SPECTROSCOPY_MATCHER = (config, step, elems) -> {
        if (!SPECTROSCOPY_MATCHER.matches(config, step, elems))
            return false;
        final InstGmosCommon<?,?,?,?> inst = (InstGmosCommon) elems.getInstrument();
        return inst != null && inst.getUseNS() == UseNS.TRUE;
    };

    private static IConfigMatcher N_S_SPECTROSCOPY_SCIENCE_DAYCAL__MATCHER = (config, step, elems) -> {
        if (!SCIENCE_DAYCAL_MATCHER.matches(config, step, elems)) {
            return false;
        }
        if (!isSpecFPUnselected(config, elems)) {
            return false;
        }
        final Disperser disperser = getDisperser(config);
        if (disperser.isMirror()) {
            return false;
        }
        final InstGmosCommon<?,?,?,?> inst = (InstGmosCommon) elems.getInstrument();
        return inst != null && inst.getUseNS() == UseNS.TRUE;
    };


    private static class ScienceRule implements IConfigRule {

        interface IScienceChecker {
            boolean check(Config config, ObservationElements elems);

            // The error or warning message
            String getMessage();

            // A unique id used to allow the user to ignore selected messages
            String getId();
        }

        private IConfigMatcher _matcher;
        private IScienceChecker _checker;
        private Problem.Type _type;

        public ScienceRule(IScienceChecker checker, IConfigMatcher validator) {
            this(checker, validator, WARNING);
        }

        public ScienceRule(IScienceChecker checker, IConfigMatcher validator, Problem.Type type) {
            _checker = checker;
            _matcher = validator;
            _type = type;
        }

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            if (_checker.check(config, elems)) {
                return new Problem(_type, _checker.getId(), _checker.getMessage(),
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }
            return null;
        }

        public IConfigMatcher getMatcher() {
            return _matcher;
        }
    }


    /**
     * REL-298: IF (GMOS-N && detector == E2V && amps != 6) then ERROR "The E2V detectors must use 6 amp mode."
     * This is for GMOS-N only.
     * REL-1194: Also for GMOS-S now, but with different default and rule.
     */
    private static IConfigRule CHECK_3_AMP_MODE = new AbstractConfigRule() {
        private static final String GMOS_NORTH_MESSAGE = "The E2V detectors must use 6 amp mode";
        private static final String GMOS_SOUTH_MESSAGE = "The E2V detectors must use 3 amp mode";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            SPInstObsComp inst = elems.getInstrument();
            if (inst instanceof InstGmosNorth) {
                // Get the detector manufacturer.
                Object tmp = SequenceRule.getInstrumentItem(config, InstGmosNorth.DETECTOR_MANUFACTURER_PROP);
                if (tmp == null) return null;
                final DetectorManufacturer man = (DetectorManufacturer) tmp;

                // Get the amp count.
                tmp = SequenceRule.getInstrumentItem(config, InstGmosNorth.AMP_COUNT_PROP);
                if (tmp == null) return null;
                final AmpCount cnt = (AmpCount) tmp;

                if (man == DetectorManufacturer.E2V && cnt != AmpCount.SIX) {
                    return new Problem(Problem.Type.ERROR, PREFIX + "CHECK_3_AMP_MODE", GMOS_NORTH_MESSAGE,
                            SequenceRule.getInstrumentOrSequenceNode(step, elems));
                }
            } else if (inst instanceof InstGmosSouth) {
                // Get the detector manufacturer.
                Object tmp = SequenceRule.getInstrumentItem(config, InstGmosSouth.DETECTOR_MANUFACTURER_PROP);
                if (tmp == null) return null;
                final DetectorManufacturer man = (DetectorManufacturer) tmp;

                // Get the amp count.
                tmp = SequenceRule.getInstrumentItem(config, InstGmosSouth.AMP_COUNT_PROP);
                if (tmp == null) return null;
                final AmpCount cnt = (AmpCount) tmp;

                if (man == DetectorManufacturer.E2V && cnt != AmpCount.THREE) {
                    return new Problem(Problem.Type.ERROR, PREFIX + "CHECK_6_AMP_MODE", GMOS_SOUTH_MESSAGE,
                            SequenceRule.getInstrumentOrSequenceNode(step, elems));
                }
            }

            return null;
        }
    };


    /**
     * Error if we try to use an amp count not supported by the CCD.
     */
    private static IConfigRule BAD_AMP_COUNT_RULE = new AbstractConfigRule() {
        private static final String MESSAGE = "Amp count %s is not compatible with the %s CCD";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            // Get the detector manufacturer.
            Object tmp = SequenceRule.getInstrumentItem(config, InstGmosNorth.DETECTOR_MANUFACTURER_PROP);
            if (tmp == null) {
                tmp = SequenceRule.getInstrumentItem(config, InstGmosSouth.DETECTOR_MANUFACTURER_PROP);
            }
            if (tmp == null) return null;
            final DetectorManufacturer man = (DetectorManufacturer) tmp;

            // Get the amp count.
            tmp = SequenceRule.getInstrumentItem(config, InstGmosSouth.AMP_COUNT_PROP);
            if (tmp == null) {
                tmp = SequenceRule.getInstrumentItem(config, InstGmosNorth.AMP_COUNT_PROP);
            }
            if (tmp == null) return null;
            final AmpCount cnt = (AmpCount) tmp;

            // Verify that the count is supported by the CCD.  Would rather
            // turn this around and ask the manufacturer what counts it
            // supports.  The issue is that the amp counts are defined in
            // subclasses.
            if (!cnt.getSupportedBy().contains(man)) {
                String message = String.format(MESSAGE, cnt.displayValue(), man.displayValue());
                return new Problem(Problem.Type.ERROR, PREFIX + "BAD_AMP_COUNT_RULE", message, elems.getSeqComponentNode());
            }

            return null;
        }
    };


    /**
     * WARN if (gain != 'low' && !HAMAMATSU)
     */
    private static ScienceRule GAIN_SCIENCE_RULE = new ScienceRule(
            new ScienceRule.IScienceChecker() {
                private static final String MESSAGE = "Low gain is recommended for science observations";

                public boolean check(Config config, ObservationElements elems) {

                    DetectorManufacturer det =
                            (DetectorManufacturer) SequenceRule.getInstrumentItem(config, InstGmosSouth.DETECTOR_MANUFACTURER_PROP);
                    if (det == null) {
                        det = (DetectorManufacturer) SequenceRule.getInstrumentItem(config, InstGmosNorth.DETECTOR_MANUFACTURER_PROP);
                    }
                    if (det == DetectorManufacturer.HAMAMATSU) {
                        return false;
                    }

                    AmpGain gain =
                            (AmpGain) SequenceRule.getInstrumentItem(config, InstGmosSouth.AMP_GAIN_CHOICE_PROP);
                    if (gain == null)
                        gain = (AmpGain) SequenceRule.getInstrumentItem(config, InstGmosNorth.AMP_GAIN_CHOICE_PROP);
                    return gain != AmpGain.LOW;

                }

                public String getMessage() {
                    return MESSAGE;
                }

                public String getId() {
                    return PREFIX + "GAIN_SCIENCE_RULE";
                }
            }
            , SequenceRule.SCIENCE_MATCHER
    );


    /**
     * WARN if (read == 'fast'  && !HAMAMATSU)
     */
    private static ScienceRule READMODE_SCIENCE_RULE = new ScienceRule(
            new ScienceRule.IScienceChecker() {
                private static final String MESSAGE = "Slow read-out is recommended for science observations";

                public boolean check(Config config, ObservationElements elems) {

                    DetectorManufacturer det =
                            (DetectorManufacturer) SequenceRule.getInstrumentItem(config, InstGmosSouth.DETECTOR_MANUFACTURER_PROP);
                    if (det == null) {
                        det = (DetectorManufacturer) SequenceRule.getInstrumentItem(config, InstGmosNorth.DETECTOR_MANUFACTURER_PROP);
                    }
                    if (det == DetectorManufacturer.HAMAMATSU) {
                        return false;
                    }

                    AmpReadMode readMode =
                            (AmpReadMode) SequenceRule.getInstrumentItem(config, InstGmosSouth.AMP_READ_MODE_PROP);
                    if (readMode == null)
                        readMode = (AmpReadMode) SequenceRule.getInstrumentItem(config, InstGmosNorth.AMP_READ_MODE_PROP);

                    return readMode == AmpReadMode.FAST;
                }

                public String getMessage() {
                    return MESSAGE;
                }

                public String getId() {
                    return PREFIX + "READMODE_SCIENCE_RULE";
                }
            }
            , SequenceRule.SCIENCE_MATCHER
    );
    /**
     * WARNING  if configured with slow-read and high-gain.
     */
    private static IConfigRule GAIN_READMODE_RULE = new AbstractConfigRule() {
        private static final String MESSAGE = "Slow readout and high gain is not recommended.";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            AmpReadMode readMode =
                    (AmpReadMode) SequenceRule.getInstrumentItem(config, InstGmosSouth.AMP_READ_MODE_PROP);
            if (readMode == null) {
                readMode = (AmpReadMode) SequenceRule.getInstrumentItem(config, InstGmosNorth.AMP_READ_MODE_PROP);
            }

            AmpGain gain =
                    (AmpGain) SequenceRule.getInstrumentItem(config, InstGmosSouth.AMP_GAIN_CHOICE_PROP);
            if (gain == null) {
                gain = (AmpGain) SequenceRule.getInstrumentItem(config, InstGmosNorth.AMP_GAIN_CHOICE_PROP);
            }
            if ((readMode == AmpReadMode.SLOW) && (gain == AmpGain.HIGH)) {
                return new Problem(WARNING, PREFIX + "GAIN_READMODE_RULE", MESSAGE,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }
            return null;
        }
    };

    /**
     * For imaging observations, it is required to use 1x1, 2x2, or 4x4 binning.
     * For MOS pre-imaging, only 1x1 or 2x2 are allowe.
     */
    private static IConfigRule BINNING_RULE = new IConfigRule() {

        private static final String IMAGING_MSG = "For imaging, binning is limited to 1x1, 2x2 or 4x4.";
        private static final String PREIMAGING_MSG = "For MOS pre-imaging, binning is limited to 1x1 or 2x2.";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {

            // Collect the x and y binning values.
            PropertyDescriptor xProp = InstGmosCommon.CCD_X_BIN_PROP;
            Binning x = (Binning) SequenceRule.getInstrumentItem(config, xProp);
            PropertyDescriptor yProp = InstGmosCommon.CCD_Y_BIN_PROP;
            Binning y = (Binning) SequenceRule.getInstrumentItem(config, yProp);

            // Determine whether this is preimaging.
            PropertyDescriptor preProp = InstGmosCommon.IS_MOS_PREIMAGING_PROP;
            YesNoType pre;
            pre = (YesNoType) SequenceRule.getInstrumentItem(config, preProp);
            boolean isPreimaging = pre.toBoolean();

            // If they don't match, we must warn.
            if (x != y) {
                String msg = isPreimaging ? PREIMAGING_MSG : IMAGING_MSG;
                String id = isPreimaging ? PREFIX + "PREIMAGING_MSG" : PREFIX + "IMAGING_MSG";
                ISPProgramNode node;
                node = SequenceRule.getInstrumentOrSequenceNode(step, elems, config);
                return new Problem(ERROR, id, msg, node);
            }

            // Even if they match, make sure that we aren't preimaging with
            // binning 4x4.
            if ((x == Binning.FOUR) && isPreimaging) {
                ISPProgramNode node;
                node = SequenceRule.getInstrumentOrSequenceNode(step, elems, config);
                return new Problem(ERROR, PREFIX + "PREIMAGING_MSG", PREIMAGING_MSG, node);
            }

            return null;
        }

        // We only want to do this check for imaging observations.
        public IConfigMatcher getMatcher() {
            return IMAGING_MATCHER;
        }
    };

    public static final String GMOS_S_DTA_X_RULE_Y1_ID = PREFIX + "Y1";
    public static final String GMOS_S_DTA_X_RULE_Y2_4_ID = PREFIX + "Y2_4";

    private static IConfigRule GMOS_S_DTA_X_RULE = new IConfigRule() {

        private static final String Y1   = "The GMOS-S Hamamatsu allowed DTA X range is -4 to +6 for Ybin=1";
        private static final String Y2_4 = "The GMOS-S Hamamatsu allowed DTA X range is -2 to +6 for Ybin=2 or 4";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            final Binning y = (Binning) SequenceRule.getInstrumentItem(config, InstGmosCommon.CCD_Y_BIN_PROP);
            final DTAX dtaX = (DTAX) SequenceRule.getInstrumentItem(config, InstGmosCommon.DTAX_OFFSET_PROP);

            final ISPProgramNode node = SequenceRule.getInstrumentOrSequenceNode(step, elems, config);
            if ((y == Binning.ONE) && (dtaX.intValue() < -4)) {
                return new Problem(ERROR, GMOS_S_DTA_X_RULE_Y1_ID, Y1, node);
            } else if ((y != Binning.ONE) && (dtaX.intValue() < -2)) {
                return new Problem(ERROR, GMOS_S_DTA_X_RULE_Y2_4_ID, Y2_4, node);
            } else {
                return null;
            }
        }

        public IConfigMatcher getMatcher() {
            return (config, step, elems) -> {
                final InstGmosCommon<?,?,?,?> gmos = (InstGmosCommon) elems.getInstrument();
                return gmos.getSite().equals(Site.SET_GS) && (gmos.getDetectorManufacturer() == HAMAMATSU);
            };
        }
    };

    /**
     * State used to check for the SPATIAL_DITHER_IMAGING_RULE.  If a sequence
     * contains two or more observes at distinct positions, then there should
     * not be a warning.  Otherwise, a warning is generated.  This only applies
     * for science imaging.
     */
    private static final class GmosSpatialDitherState {
        private static final String MESSAGE = "Imaging observations usually benefit from spatial dithers, consider including an offset iterator";

        double p;
        double q;
        boolean foundTwoDifferentPositions;
        boolean ruleInEffect; // if false, the imaging rule was never applied

        // REL-389
        boolean foundMultipleExposurePerPerFilterChange;
        boolean foundExpTimeGreaterThan300s;
        Filter filter;
        int exposureCount;

        // Adds a warning to the problem list if necessary -- if doing science
        // imaging and there are not two or more observes in different
        // positions
        void addWarning(IP2Problems probs, ObservationElements elems) {
            if (!ruleInEffect) return;
            if (foundTwoDifferentPositions) return;
            // REL-389: add conditions: && ( >1 exposure per filter || exptime > 300s )
            if (foundMultipleExposurePerPerFilterChange || foundExpTimeGreaterThan300s) {
                probs.addWarning(PREFIX + "GmosSpatialDitherState", MESSAGE, elems.getSeqComponentNode());
            }
        }
    }

    /**
     * WARN if (no spatial dithers).  This rule will never directly generate
     * a warning because the entire sequence must be examined to determine
     * whether a warning is necessary.  It updates the GmosSpatialDitherState
     * so that the GmosRule can check whether to add a warning after the
     * entire sequence has been iterated.
     */
    private static IConfigRule SPATIAL_DITHER_IMAGING_RULE = new IConfigRule() {

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            GmosSpatialDitherState gsds = (GmosSpatialDitherState) state;

            // REL-389
            if (!gsds.foundExpTimeGreaterThan300s) {
                Double expTime = SequenceRule.getExposureTime(config);
                if (expTime != null && expTime > 300) {
                    gsds.foundExpTimeGreaterThan300s = true;
                }
            }
            if (!gsds.foundMultipleExposurePerPerFilterChange) {
                final Filter filter = getFilter(config);
                if (filter != gsds.filter) {
                    gsds.filter = filter;
                    gsds.exposureCount = 0;
                }
                final String obsType = SequenceRule.getObserveType(config);
                if (InstConstants.SCIENCE_OBSERVE_TYPE.equals(obsType)) {
                    Integer repeatCount = SequenceRule.getStepCount(config);
                    if (repeatCount != null && repeatCount > 1) {
                        gsds.exposureCount += repeatCount;
                    } else {
                        gsds.exposureCount++;
                    }
                }
                if (gsds.exposureCount > 1) {
                    gsds.foundMultipleExposurePerPerFilterChange = true;
                }
            }

            if (gsds.foundTwoDifferentPositions) {
                return null; // already know the
            }
            gsds.ruleInEffect = true;

            final Option<Double> pOpt = SequenceRule.getPOffset(config);
            final Option<Double> qOpt = SequenceRule.getQOffset(config);
            final double p = pOpt.isDefined() ? pOpt.get() : 0.0;
            final double q = qOpt.isDefined() ? qOpt.get() : 0.0;

            if (step == 0) {
                gsds.p = p;
                gsds.q = q;
                return null;
            }

            if (!Offset.areEqual(p, gsds.p) || !Offset.areEqual(q, gsds.q)) {
                gsds.foundTwoDifferentPositions = true;
            }
            return null;
        }

        public IConfigMatcher getMatcher() {
            return IMAGING_MATCHER;
        }
    };

    /**
     * WARN if (ccd bin = 1,1) && (IQ != 20) && !Altair
     */
    private static ScienceRule CCD_BIN_AND_IQ_IMAGING_RULE = new ScienceRule(
            new ScienceRule.IScienceChecker() {
                private static final String MESSAGE = "1x1 binning is usually only necessary in IQ=20";

                public boolean check(Config config, ObservationElements elems) {
                    for (SPSiteQuality sq : elems.getSiteQuality()) {
                        boolean hasAOComp = elems.hasAltair();

                        final Binning binningX =
                                (Binning) SequenceRule.getInstrumentItem(config, InstGmosCommon.CCD_X_BIN_PROP);
                        final Binning binningY =
                                (Binning) SequenceRule.getInstrumentItem(config, InstGmosCommon.CCD_Y_BIN_PROP);

                        return binningX == Binning.ONE &&
                                binningY == Binning.ONE &&
                                sq.getImageQuality() != ImageQuality.PERCENT_20 &&
                                !hasAOComp;
                    }
                    return false;
                }

                public String getMessage() {
                    return MESSAGE;
                }

                public String getId() {
                    return PREFIX + "CCD_BIN_AND_IQ_IMAGING_RULE";
                }
            }
            , IMAGING_MATCHER
    );
    /**
     * WARN if (ccd bin != 1,1) && Altair
     */
    private static ScienceRule CCD_BIN_AND_ALTAIR_IMAGING_RULE = new ScienceRule(
            new ScienceRule.IScienceChecker() {
                private static final String MESSAGE = "Altair observations should use 1x1 binning.";

                public boolean check(Config config, ObservationElements elems) {

                    boolean hasAOComp = elems.hasAltair();

                    final Binning binningX =
                            (Binning) SequenceRule.getInstrumentItem(config, InstGmosCommon.CCD_X_BIN_PROP);
                    final Binning binningY =
                            (Binning) SequenceRule.getInstrumentItem(config, InstGmosCommon.CCD_Y_BIN_PROP);

                    return (binningX != Binning.ONE ||
                            binningY != Binning.ONE) &&
                            hasAOComp;
                }

                public String getMessage() {
                    return MESSAGE;
                }

                public String getId() {
                    return PREFIX + "CCD_BIN_AND_ALTAIR_IMAGING_RULE";
                }
            }
            , IMAGING_MATCHER
    );


    /**
     * WARN if (disperser != 'Mirror) && (Built In fpu == selected) && (fpu == 'None')
     */
    private static ScienceRule GRATING_NO_SLIT_SCIENCE_RULE = new ScienceRule(
            new ScienceRule.IScienceChecker() {
                private static final String MESSAGE = "A grating is defined but not a slit, mask or IFU";

                public boolean check(Config config, ObservationElements elems) {
                    return !getDisperser(config).isMirror() && getFPU(config, elems).isImaging();
                }

                public String getMessage() {
                    return MESSAGE;
                }

                public String getId() {
                    return PREFIX + "GRATING_NO_SLIT_SCIENCE_RULE";
                }
            }
            , SequenceRule.SCIENCE_MATCHER
    );

    /**
     * REL-388: OT Phase-2 check for old GMOS-N B600 grating
     */
    private static ScienceRule B600_G5303_RULE = new ScienceRule(
            new ScienceRule.IScienceChecker() {
                private static final String MESSAGE = "The B600_G5303 grating has been superseded by the B600_G5307.";

                public boolean check(Config config, ObservationElements elems) {
                    final Disperser disperser = getDisperser(config);
                    return disperser == DisperserNorth.B600_G5303;
                }

                public String getMessage() {
                    return MESSAGE;
                }

                public String getId() {
                    return PREFIX + "B600_G5303_RULE";
                }
            }
            , SequenceRule.SCIENCE_MATCHER
    );

    /**
     * Spectroscopic element in fpu without grating
     */
    private static ScienceRule SPECTROSCOPIC_ELEMENT_IN_FPU_SCIENCE_RULE = new ScienceRule(
            new ScienceRule.IScienceChecker() {
                private static final String MESSAGE = "A slit, mask or IFU is defined, but no grating is selected";

                public boolean check(Config config, ObservationElements elems) {
                    final Disperser disperser = getDisperser(config);
                    return disperser != null && isSpecFPUnselected(config, elems) && disperser.isMirror();
                }

                public String getMessage() {
                    return MESSAGE;
                }

                public String getId() {
                    return PREFIX + "SPECTROSCOPIC_ELEMENT_IN_FPU_SCIENCE_RULE";
                }
            }
            , SequenceRule.SCIENCE_MATCHER
    );


    /**
     * nod and shuffle without slit or grating
     */
    private static ScienceRule N_S_NO_SLIT_OR_GRATING_SCIENCE_RULE = new ScienceRule(
            new ScienceRule.IScienceChecker() {
                private static final String MESSAGE = "Nod and Shuffle science observations require a " +
                        "grating and a spectroscopic element in the fpu";

                public boolean check(Config config, ObservationElements elems) {
                    final Disperser disperser = getDisperser(config);
                    if (disperser == null) return false; //can't check
                    final InstGmosCommon<?,?,?,?> inst = (InstGmosCommon) elems.getInstrument();
                    if (inst == null) return false; //can't check
                    final UseNS useNs = inst.getUseNS();
                    return useNs == UseNS.TRUE && disperser.isMirror() && !isSpecFPUnselected(config, elems);
                }

                public String getMessage() {
                    return MESSAGE;
                }

                public String getId() {
                    return PREFIX + "N_S_NO_SLIT_OR_GRATING_SCIENCE_RULE";
                }
            }
            , SequenceRule.SCIENCE_MATCHER
    );


    /**
     * WARN  if (exposure > 3600) under spectroscopy mode
     * <p/>
     * This is implemented differently since it needs to get the exposure time and depending on where
     * this value is defined the result is reported either in the static component or in the sequence node
     */

    private static IConfigRule EXP_SPECTROSCOPIC_RULE = new IConfigRule() {
        private static final String MESSAGE = "It is usually best to keep spectroscopic exposure " +
                "times less than one hour";


        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Double expTime = SequenceRule.getExposureTime(config);
            if (expTime != null && expTime > 3600) {
                return new Problem(WARNING, PREFIX + "EXP_SPECTROSCOPIC_RULE", MESSAGE,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
            }
            return null;
        }

        public IConfigMatcher getMatcher() {
            return SPECTROSCOPY_MATCHER;
        }

    };


    /**
     * WARN  if (Built in FPU == selected) && (fpu == 'IFU Left Slit (blue)')
     */
    private static ScienceRule IFU_LEFT_SLIT_SPECTROSCOPIC_RULE = new ScienceRule(
            new ScienceRule.IScienceChecker() {
                private static final String MESSAGE = "In IFU one slit mode it is recommended to use the red slit";

                public boolean check(Config config, ObservationElements elems) {
                    final FPUnit fpu = getFPU(config, elems);
                    return fpu == FPUnitNorth.IFU_2 ||
                            fpu == FPUnitSouth.IFU_2;
                }

                public String getMessage() {
                    return MESSAGE;
                }

                public String getId() {
                    return PREFIX + "IFU_LEFT_SLIT_SPECTROSCOPIC_RULE";
                }
            }
            , SPECTROSCOPY_MATCHER
    );

    /**
     * WARN  if (Built in FPU == selected) && (fpu == 'IFU 2 Slits') && (Filter == 'none')
     */
    private static ScienceRule IFU_2_SLIT_AND_FILTER_SPECTROSCOPIC_RULE = new ScienceRule(
            new ScienceRule.IScienceChecker() {
                private static final String MESSAGE = "In IFU 2-slit mode, it is recommended to use a " +
                        "filter to prevent spectral overlap";

                public boolean check(Config config, ObservationElements elems) {

                    final Filter filter = getFilter(config);
                    if (!filter.isNone()) return false;
                    final FPUnit fpu = getFPU(config, elems);
                    return fpu == FPUnitNorth.IFU_1 ||
                            fpu == FPUnitSouth.IFU_1;
                }

                public String getMessage() {
                    return MESSAGE;
                }

                public String getId() {
                    return PREFIX + "IFU_2_SLIT_AND_FILTER_SPECTROSCOPIC_RULE";
                }
            }
            , SPECTROSCOPY_MATCHER
    );


    /**
     * WARN if (wavelength < 450) || (wavelength > 900)
     */
    private static ScienceRule WAVELENGTH_SPECTROSCOPIC_RULE = new ScienceRule(
            new ScienceRule.IScienceChecker() {
                private static final String MESSAGE = "The central wavelength is likely too blue or too red";

                public boolean check(Config config, ObservationElements elems) {
                    Double wavelength =
                            (Double) SequenceRule.getInstrumentItem(config, InstGmosCommon.DISPERSER_LAMBDA_PROP);
                    return wavelength != null && (wavelength < 450 || wavelength > 900);
                }

                public String getMessage() {
                    return MESSAGE;
                }

                public String getId() {
                    return PREFIX + "WAVELENGTH_SPECTROSCOPIC_RULE";
                }
            }
            , SPECTROSCOPY_MATCHER
    );


    /**
     * Companion class for the {@link edu.gemini.p2checker.rules.gmos.GmosRule#DISPERSER_WAVELENGTH_SPECTROSCOPIC_RULE)}
     */
    private static class DisperserWavelengthChecker implements ScienceRule.IScienceChecker {

        private static final String MESSAGE = "For the selected central wavelength and disperser it is recommended to " +
                "use a blocking filter to avoid second order overlap";

        private static Map<Disperser, Double> DISPERSER_LIMITS_MAP = new HashMap<>();

        static {
            //north dispersers
            DISPERSER_LIMITS_MAP.put(DisperserNorth.R400_G5305, 710.0);
            DISPERSER_LIMITS_MAP.put(DisperserNorth.R831_G5302, 815.0);
            DISPERSER_LIMITS_MAP.put(DisperserNorth.R600_G5304, 775.0);
            //south dispersers
            DISPERSER_LIMITS_MAP.put(DisperserSouth.R400_G5325, 710.0);
            DISPERSER_LIMITS_MAP.put(DisperserSouth.R831_G5322, 815.0);
            DISPERSER_LIMITS_MAP.put(DisperserSouth.R600_G5324, 775.0);
        }

        private static DisperserWavelengthChecker _instance = new DisperserWavelengthChecker();

        public static DisperserWavelengthChecker getInstance() {
            return _instance;
        }

        public boolean check(Config config, ObservationElements elems) {

            final Filter filter = getFilter(config);
            if (!filter.isNone()) return false;
            final Disperser disperser = getDisperser(config);
            if (disperser == null) return false;
            //the following 2 dispersers generate a warning no matter what wavelength
            if (disperser == DisperserNorth.R150_G5306 || disperser == DisperserSouth.R150_G5326)
                return true;

            final Double limitWavelength = DISPERSER_LIMITS_MAP.get(disperser);
            if (limitWavelength == null) return false;

            final Double centralWavelength = (Double) SequenceRule.getInstrumentItem(config, InstGmosCommon.DISPERSER_LAMBDA_PROP);

            return (centralWavelength != null && centralWavelength > limitWavelength);
        }

        public String getMessage() {
            return MESSAGE;
        }

        public String getId() {
            return PREFIX + "DisperserWavelengthChecker";
        }
    }

    /**
     * WARN if (filter == 'none') && (((disperser == 'R400_G5305') && (central wavelength > 710)) \
     * || ((disperser == 'R831_G5302') && (central wavelength > 815)) \
     * || ((disperser == 'R600_G5304') && (central wavelength > 775)) \
     * || (disperser == 'R150_G5306'))
     */
    private static ScienceRule DISPERSER_WAVELENGTH_SPECTROSCOPIC_RULE =
            new ScienceRule(
                    DisperserWavelengthChecker.getInstance(),
                    SPECTROSCOPY_MATCHER
            );


    /**
     * check correct fpu for N&S
     */
    private static ScienceRule N_S_FPU_SPECTROSCOPIC_RULE = new ScienceRule(
            new ScienceRule.IScienceChecker() {
                private static final String MESSAGE_SOUTH = "For Nod and Shuffle, either a Nod and Shuffle slit, " +
                        "or a Custom mask or Nod and Shuffle IFU option must be selected";

                private static final String MESSAGE_NORTH = "For Nod and Shuffle, either a Nod and Shuffle slit " +
                        "or a Custom mask must be selected";

                private String _message = MESSAGE_NORTH;

                public boolean check(Config config, ObservationElements elems) {
                    if (elems.getInstrument() instanceof InstGmosSouth) {
                        _message = MESSAGE_SOUTH;
                    } else {
                        _message = MESSAGE_NORTH;
                    }
                    final FPUnit fpu = getFPU(config, elems);
                    return !(fpu.isNS() || fpu == FPUnitNorth.CUSTOM_MASK || fpu == FPUnitSouth.CUSTOM_MASK);
                }

                public String getMessage() {
                    return _message;
                }

                public String getId() {
                    return PREFIX + "N_S_FPU_SPECTROSCOPIC_RULE";
                }
            }
            , N_S_SPECTROSCOPY_MATCHER
    );


    /**
     * ERROR if (nod_distance == 0)
     * Comments below from K.Roth on SCT-203
     * The nod distance for Nod and Shuffle must not be equal to zero or otherwise the telescope is not nodding
     * when the data are being taken. This is set in the nod and shuffle tab of the GMOS static component.
     * The offset distance is just the difference between the two offset positions.
     * I believe that for all science data the offset distance has to be non-zero, and I actually think
     * this should produce an ERROR instead of just a WARNING. This should not produce an ERROR or a WARNING
     * if the observation is a Daytime Calibration since DARKS do not nod when they are taken
     * <p/>
     * Bryan on SCT-203: I define nod_distance=sqrt((p2-p1)^2 + (q2-q1)^2)
     */
    private static ScienceRule NOD_DISTANCE_N_S_SPECTROSCOPY_RULE = new ScienceRule(
            new ScienceRule.IScienceChecker() {
                private static final String MESSAGE = "For Nod and Shuffle a nod distance must be set";

                public boolean check(Config config, ObservationElements elems) {
                    InstGmosCommon<?,?,?,?> inst = (InstGmosCommon) elems.getInstrument();
                    //Bryan: I define nod_distance=sqrt((p2-p1)^2 + (q2-q1)^2)
                    if (inst.getPosList().size() < 2)
                        return true; //there is not enough offsets, so there is no nod-distance

                    final Iterator<OffsetPos> it = inst.getPosList().iterator();
                    OffsetPos current = it.next();
                    while (it.hasNext()) {
                        OffsetPos pos = it.next();
                        //if the nod distance is zero, issue an error
                        if (Double.compare(getSquareNodDistance(current, pos), 0.0) == 0) {
                            return true;
                        }
                        current = pos;
                    }
                    return false;
                }

                public String getMessage() {
                    return MESSAGE;
                }

                public String getId() {
                    return PREFIX + "NOD_DISTANCE_N_S_SPECTROSCOPY_RULE";
                }
            }
            , N_S_SPECTROSCOPY_MATCHER,
            ERROR
    );

    /**
     * ERROR if (shuffle_distance == 0),
     * (Comments below from K.Roth on SCT 203)
     * The shuffle distance is indeed set also in the nod and shuffle tab of the GMOS static component.
     * This is set in either the Offset(arcsec) or Offset(detector rows) fields since they are tied
     * together and editing one of them causes the other to change value accordingly.
     * Similarly to above, this must be set to something other than zero or else the detector
     * is not shuffling when the observation is being taken, and I actually think this should also
     * produce an ERROR state and not just a WARNING. Unlike the nod distance, a shuffle distance equal
     * to zero should also produce an ERROR in a daytime calibration since you must shuffle the darks
     * as well as the science.
     * <p/>
     * Last comment forces the use of the N_S_SPECTROSCOPY_SCIENCE_DAYCAL__MATCHER matcher,
     * which is basically the same as the N_S_SPECTROSCOPY_MATCHER but matches for Daytime calibrations
     * as well.
     */
    private static ScienceRule SHUFFLE_DISTANCE_N_S_SPECTROSCOPY_RULE = new ScienceRule(
            new ScienceRule.IScienceChecker() {
                private static final String MESSAGE = "For Nod and Shuffle a shuffle distance must be set";

                public boolean check(Config config, ObservationElements elems) {
                    final InstGmosCommon<?,?,?,?> inst = (InstGmosCommon) elems.getInstrument();
                    if (inst == null) return false;
                    int shuffle_distance = inst.getNsDetectorRows();
                    return shuffle_distance == 0;
                }

                public String getMessage() {
                    return MESSAGE;
                }

                public String getId() {
                    return PREFIX + "SHUFFLE_DISTANCE_N_S_SPECTROSCOPY_RULE";
                }
            },
            N_S_SPECTROSCOPY_SCIENCE_DAYCAL__MATCHER,
            ERROR
    );

    /**
     * WARN if (N&S_cycles == 0)
     */
    private static ScienceRule N_S_CYCLES_N_S_SPECTROSCOPY_RULE = new ScienceRule(
            new ScienceRule.IScienceChecker() {
                private static final String MESSAGE = "For Nod and Shuffle > 0 cycles must be set";

                public boolean check(Config config, ObservationElements elems) {
                    final InstGmosCommon<?,?,?,?> inst = (InstGmosCommon) elems.getInstrument();
                    return inst != null && inst.getNsNumCycles() == 0;
                }

                public String getMessage() {
                    return MESSAGE;
                }

                public String getId() {
                    return PREFIX + "N_S_CYCLES_N_S_SPECTROSCOPY_RULE";
                }
            },
            N_S_SPECTROSCOPY_MATCHER
    );

    /**
     * WARN if (Electronic_Offseting == selected) && (Nod_Distance > 2)
     */
    private static ScienceRule EOFFSET_N_S_SPECTROSCOPY_RULE = new ScienceRule(
            new ScienceRule.IScienceChecker() {
                private static final String MESSAGE = "To use electronic offsetting the Nod Distance must be <= 2";
                private static final int MAX_NOD_DISTANCE = 2;

                public boolean check(Config config, ObservationElements elems) {
                    final InstGmosCommon<?,?,?,?> inst = (InstGmosCommon) elems.getInstrument();
                    if (inst == null) return false;
                    if (!inst.isUseElectronicOffsetting()) return false;
                    final Iterator<OffsetPos> it = inst.getPosList().iterator();
                    if (inst.getPosList().size() < 2)
                        return false; //there is not enough offsets, so there is no nod-distance
                    OffsetPos current = it.next();
                    while (it.hasNext()) {
                        final OffsetPos pos = it.next();
                        //if the nod distance is greater than MAX_NOD_DISTANCE, issue an error
                        //notice we get the square of the nod distance
                        if (getSquareNodDistance(current, pos) > MAX_NOD_DISTANCE * MAX_NOD_DISTANCE) {
                            return true;
                        }
                        current = pos;
                    }
                    return false;
                }

                public String getMessage() {
                    return MESSAGE;
                }

                public String getId() {
                    return PREFIX + "EOFFSET_N_S_SPECTROSCOPY_RULE";
                }
            }
            , N_S_SPECTROSCOPY_MATCHER
    );

    /**
     * WARN if (Built in fpu == selected) && (fpu == N&S slit) \
     * && (shuffle_distance != 1536)
     */
    private static IConfigRule NS_SLIT_SPECTROSCOPY_RULE = new IConfigRule() {
        private static final String MESSAGE = "For long slit Nod and Shuffle the shuffle distance must be %d";

        // This is a bit poor but it returns null if there is no need to issue
        // a warning. Returns a formatted message otherwise.
        private String getWarningMessage(Config config, ObservationElements elems) {
            final FPUnit fpu = getFPU(config, elems);
            final InstGmosCommon<?,?,?,?> inst = (InstGmosCommon) elems.getInstrument();
            int shuffle_distance = inst.getNsDetectorRows();
            if (fpu.isNSslit()) {
                final DetectorManufacturer dm = getDetectorManufacturer(config);
                if (dm == null) return null;
                //int rows = InstGmosCommon.calculateDefaultDetectorRows(dm, 1);
                final int rows = dm.shuffleOffsetPixels();
                if (shuffle_distance != rows) {
                    return String.format(MESSAGE, rows);
                }
            }
            return null;
        }

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            final String msg = getWarningMessage(config, elems);
            if (msg != null) {
                return new Problem(WARNING, PREFIX + "NS_SLIT_SPECTROSCOPY_RULE", msg,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }
            return null;
        }

        public IConfigMatcher getMatcher() {
            return N_S_SPECTROSCOPY_MATCHER;
        }
    };

    /**
     * WARN if (shuffle_distance % ccd_y_binning != 0
     */
    private static ScienceRule Y_BINNING_AND_SHUFFLE_DISTANCE_SPECTROSCOPY_RULE = new ScienceRule(
            new ScienceRule.IScienceChecker() {
                private static final String MESSAGE = "The shuffle distance must be a multiple of the CCD Y binning";

                public boolean check(Config config, ObservationElements elems) {
                    final InstGmosCommon<?,?,?,?> inst = (InstGmosCommon) elems.getInstrument();
                    if (inst == null) return false;
                    final int shuffle_distance = inst.getNsDetectorRows();

                    final Binning binningY =
                            (Binning) SequenceRule.getInstrumentItem(config, InstGmosCommon.CCD_Y_BIN_PROP);
                    return binningY != null && (shuffle_distance % binningY.getValue() != 0);
                }

                public String getMessage() {
                    return MESSAGE;
                }

                public String getId() {
                    return PREFIX + "Y_BINNING_AND_SHUFFLE_DISTANCE_SPECTROSCOPY_RULE";
                }
            }
            , N_S_SPECTROSCOPY_MATCHER, ERROR
    );


    /**
     * REQUIREMENTS: There must be an OT phase-2 check which gives a warning when individual GMOS exposure times are
     * longer than 60 minutes for the eev/e2v detectors or 45 minutes for the Hamamatsu detectors.
     * <p/>
     * CONTEXT: There are recommended maximum exposure times for the GMOS instruments due to excessive contamination of
     * the image due to cosmic rays. We request a (yellow) warning appear when the requested exposure time exceeds this
     * limit. This refers not to the total exposure time in the observation but to the exposure time of the individual
     * exposures.
     * <p/>
     * Update: REL-176, 21nov2011
     * GMOS-S EEV: 60 min
     * GMOS-N EEV: 40 min
     * GMOS-N Hamamatsu: 20 min
     */
    private static IConfigRule MAX_EXPOSURE_TIME_RULE = new AbstractConfigRule() {
        private static final String msg = "Exposure time exceeds recommended maximum for the GMOS instruments due to excessive contamination of the image due to cosmic rays";

        @Override
        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            final InstGmosCommon<?,?,?,?> inst = (InstGmosCommon) elems.getInstrument();

            final DetectorManufacturer detectorManufacturer = getDetectorManufacturer(config);

            final double maxExp;
            if (detectorManufacturer == DetectorManufacturer.E2V) {
                maxExp = 60.0 * 60;
            } else if (detectorManufacturer == DetectorManufacturer.E2V) {
                maxExp = 40.0 * 60;
            } else if (detectorManufacturer == DetectorManufacturer.HAMAMATSU) {
                maxExp = 20.0 * 60;
            } else {
                return null;
            }
            final Double expTime = getExposureTime(inst, config);
            if (expTime != null && expTime > maxExp) {
                return new Problem(WARNING, PREFIX + "MAX_EXPOSURE_TIME_RULE", msg,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }
            return null;
        }
    };

    /**
     * REL-387:
     * REQUIREMENT: The OT must generate an error if any exposure time (whether in the GMOS-N/S static
     * component, in a GMOS-N/S iterator, or in a dark or Flat/Arc Observe) is a non-integer.
     * <p/>
     * CONTEXT: The GMOS DC does not support fractional exposure times. In practice if someone enters a
     * fractional exposure time the DC rounds down to the nearest integer, but currently the OT does not
     * give any warning that this will happen so it confuses people. Also, if someone enters an exposure
     * time less than 1 sec the GMOS DC rounds down to 0 sec but it still opens and closes the shutter.
     * There is no way to calibrate such data since we don't really know how long the shutter was open for.
     */
    private static IConfigRule INTEGER_EXPOSURE_TIME_RULE = new AbstractConfigRule() {
        private static final String msg = "The GMOS DC does not support fractional exposure times";

        @Override
        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            InstGmosCommon<?,?,?,?> inst = (InstGmosCommon) elems.getInstrument();
            Double expTime = getExposureTime(inst, config);
            if (expTime != null && expTime.doubleValue() != (int) expTime.doubleValue()) {
                return new Problem(ERROR, PREFIX + "INTEGER_EXPOSURE_TIME_RULE", msg,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }
            return null;
        }
    };

    private static IConfigRule NON_ZERO_EXPOSURE_TIME_RULE = new AbstractConfigRule() {
        private static final String msg = "Exposure time must be greater than 0";

        @Override
        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            if (!InstConstants.BIAS_OBSERVE_TYPE.equals(config.getItemValue(GeneralRule.OBSTYPE_KEY))) {
                InstGmosCommon<?,?,?,?> inst = (InstGmosCommon) elems.getInstrument();
                Double expTime = getExposureTime(inst, config);
                if( expTime != null && expTime.doubleValue() <= 0.0 ) {
                    return new Problem(ERROR, PREFIX + "NON_ZERO_EXPOSURE_TIME_RULE", msg,
                            SequenceRule.getInstrumentOrSequenceNode(step, elems));
                }
            }
            return null;
        }
    };


    private static class MdfMaskNameRule extends AbstractConfigRule {
        private final Problem.Type problemType;

        public MdfMaskNameRule(Problem.Type type) {
            this.problemType = type;
        }

        @Override
        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            final SPInstObsComp spInstObsComp = elems.getInstrument();
            if (spInstObsComp.getType() == InstGmosNorth.SP_TYPE || spInstObsComp.getType() == InstGmosSouth.SP_TYPE) {
                final Option<Problem> problemOption = MdfConfigRule.checkMaskName(FPUnitMode.CUSTOM_MASK, config, step, elems, state);
                if (problemOption.isDefined() && problemOption.get().getType() == problemType)
                    return problemOption.get();
                else
                    return null;
            }
            else
                return null;
        }
    }

    /**
     * ERROR: If GMOS-S is used with E2V after semester 2014A.
     * This can happen when PIs copy parts of old programs into new ones. PIs will not be able to change the detector
     * settings themselves and will have to have a staff member help them. This is not ideal, but since this is only
     * a transitional problem this seems to be the simplest solution.
     * (Note that very soon we will have to apply this rule to GMOS-N, too.)
     */
    private static final Semester SEMESTER_2014A = new Semester(2014, Semester.Half.A);
    private static IConfigRule POST_2014A_GMOS_S_WITH_E2V = new AbstractConfigRule() {
        private static final String msg = "Starting with 2014B GMOS-S must use the Hamamatsu CCDs. Please create a new observation or ask your contact scientist to update the CCD settings.";

        private Option<Semester> semester(final ObservationElements elems) {
            return Optional.ofNullable(elems)                       // null safe access chain
                    .map(ObservationElements::getObservationNode)
                    .map(ISPObservation::getProgramID)
                    .map(RichSpProgramId$.MODULE$::apply)
                    .map(RichSpProgramId::semester)                 // result of this is a Scala Option
                    .orElse(Option.empty());                        // turn the Java None result into a Scala None
        }

        @Override public Problem check(Config config, int step, ObservationElements elems, Object state) {
            // apply this rule to GMOS-S
            final SPInstObsComp instrument = elems.getInstrument();
            if (instrument.getType() == InstGmosSouth.SP_TYPE) {
                // apply only if semester is known
                final Option<Semester> semester = semester(elems);
                if (semester.isDefined()) {
                    // apply it to observations for 2016A or later
                    if (semester.get().compareTo(SEMESTER_2014A) > 0) {
                        // apply if E2V is selected
                        final DetectorManufacturer ccd = ((InstGmosSouth) instrument).getDetectorManufacturer();
                        if (ccd == DetectorManufacturer.E2V) {
                            return new Problem(ERROR, PREFIX + "POST_2014A_GMOS_S_WITH_E2V_RULE", msg, SequenceRule.getInstrumentOrSequenceNode(step, elems));
                        }
                    }
                }
            }
            return null;
        }
    };

    private static final class MultiKey {
        final Filter filter;
        final SkyBackground sb;

        private MultiKey(Filter filter, SkyBackground sb) {
            this.filter = filter;
            this.sb = sb;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final MultiKey multiKey = (MultiKey) o;

            if (filter != multiKey.filter)
                return false;

            return sb == multiKey.sb;
        }

        @Override
        public int hashCode() {
            int result = (filter != null ? filter.hashCode() : 0);
            result = 31 * result + (sb != null ? sb.hashCode() : 0);
            return result;
        }
    }

    private static final Map<MultiKey, Double> E2V_EXPOSURE_LIMITS = new HashMap<>();
    private static final Map<MultiKey, Double> HAM_EXPOSURE_LIMITS = new HashMap<>();

    private static Double getLimit(final DetectorManufacturer dm, final Filter filter, final AmpGain gain, final SkyBackground sb, final Binning binning) {
        // Currently we only apply these rules for AmpGain.LOW. Note that the amp gain was originally part of
        // the MultiKey but as long as only one value is relevant it seems overkill to have it there.
        if (gain != AmpGain.LOW) return Double.MAX_VALUE;

        // check if we have a max value defined
        final MultiKey key = new MultiKey(filter, sb);
        final Double storedLimit = getLimits(dm).get(key);
        if (storedLimit == null) return Double.MAX_VALUE;

        // if so return it, taking binning into account
        switch (binning) {
            case ONE:       return storedLimit;
            case TWO:       return storedLimit / 4.0;
            case FOUR:      return storedLimit / 16.0;
            default:        throw new Error();
        }
    }

    private static Map<MultiKey, Double> getLimits(final DetectorManufacturer dm) {
        switch (dm) {
            case E2V:       return E2V_EXPOSURE_LIMITS;
            case HAMAMATSU: return HAM_EXPOSURE_LIMITS;
            default:        throw new Error();
        }
    }

    static {

// ==================
// HAMAMATSU GMOS-N: REL-3057
// Times here are updated exposure times that give 50% full well.
// We multiply by 2 to get the saturation times in all cases as per this REL task.

// GMOS-N g-band (Hamamatsu Blue CCD) 1x1 binning (unbinned)
// BG20: 3.38 hours
// BG50: 1.39 hours
// BG80: 25 minutes
// BGAny: 3.5 minutes
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.g_G0301, SkyBackground.PERCENT_20), 2 * 3.38 * 60 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.g_G0301, SkyBackground.PERCENT_50), 2 * 1.39 * 60 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.g_G0301, SkyBackground.PERCENT_80), 2 * 25.0 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.g_G0301, SkyBackground.ANY), 2 * 3.5 * 60);
// GMOS-N r-band (Hamamatsu Red CCD) 1x1 binning (unbinned)
// BG20: 1.61 hours
// BG50: 53.3 minutes
// BG80: 21.7 minutes
// BGAny: 3.8 minutes
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.r_G0303, SkyBackground.PERCENT_20), 2 * 1.61 * 60 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.r_G0303, SkyBackground.PERCENT_50), 2 * 53.3 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.r_G0303, SkyBackground.PERCENT_80), 2 * 21.7 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.r_G0303, SkyBackground.ANY), 2 * 3.8 * 60);
// GMOS-N i-band (Hamamatsu Red CCD) 1x1 binning (unbinned)
// BG20: 53.5 minutes
// BG50: 35 minutes
// BG80: 19.2 minutes
// BGAny: 4.7 minutes
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.i_G0302, SkyBackground.PERCENT_20), 2 * 53.3 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.i_G0302, SkyBackground.PERCENT_50), 2 * 35.0 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.i_G0302, SkyBackground.PERCENT_80), 2 * 19.2 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.i_G0302, SkyBackground.ANY), 2 * 4.7 * 60);
// GMOS-N z-band (Hamamatsu Red CCD) 1x1 binning (unbinned)
// BG20: 10.7 minutes
// BG50: 10.3 minutes
// BG80: 10.0 minutes
// BGAny: 7.5 minutes
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.z_G0304, SkyBackground.PERCENT_20), 2 * 10.7 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.z_G0304, SkyBackground.PERCENT_50), 2 * 10.3 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.z_G0304, SkyBackground.PERCENT_80), 2 * 10.0 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.z_G0304, SkyBackground.ANY), 2 * 7.5 * 60);
// GMOS-N Z-band (Hamamatsu Red CCD) 1x1 binning (unbinned)
// BG20: 30 minutes
// BG50: 26.7 minutes
// BG80: 22.2 minutes
// BGAny: 11.5 minutes
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.Z_G0322, SkyBackground.PERCENT_20), 2 * 30.0 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.Z_G0322, SkyBackground.PERCENT_50), 2 * 26.7 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.Z_G0322, SkyBackground.PERCENT_80), 2 * 22.2 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.Z_G0322, SkyBackground.ANY), 2 * 11.5 * 60);
// GMOS-N Y-band (Hamamatsu Red CCD) 1x1 binning (unbinned)
// BG20: 44.2 minutes
// BG50: 44.2 minutes
// BG80: 44.2 minutes
// BGAny: 44.2 minutes
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.Y_G0323, SkyBackground.PERCENT_20), 2 * 44.2 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.Y_G0323, SkyBackground.PERCENT_50), 2 * 44.2 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.Y_G0323, SkyBackground.PERCENT_80), 2 * 44.2 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.Y_G0323, SkyBackground.ANY), 2 * 44.2 * 60);

// HAMAMATSU GMOS-S
// Unlike above, these are saturation times and thus we do not multiply by 2.

// GMOS-S g-band (Hamamatsu Blue CCD) 1x1 binning (unbinned)
// BG20: 4.35 hours (longer than the maximum exposure time due to cosmic rays)
// BG50: 1.83 hours (longer than the maximum exposure time due to cosmic rays)
// BG80: 32.5 minutes
// BGAny: 4.5 minutes
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.g_G0325, SkyBackground.PERCENT_20), 4.35 * 60 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.g_G0325, SkyBackground.PERCENT_50), 1.83 * 60 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.g_G0325, SkyBackground.PERCENT_80), 32.5 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.g_G0325, SkyBackground.ANY), 4.5 * 60);
// GMOS-S r-band (Hamamatsu Red CCD) 1x1 binning (unbinned)
// BG20: 1.83 hours (longer than the maximum exposure time due to cosmic rays)
// BG50: 1.02 hours (longer than the maximum exposure time due to cosmic rays)
// BG80: 25 minutes
// BGAny: 4.3 minutes
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.r_G0326, SkyBackground.PERCENT_20), 1.83 * 60 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.r_G0326, SkyBackground.PERCENT_50), 1.02 * 60 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.r_G0326, SkyBackground.PERCENT_80), 25.0 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.r_G0326, SkyBackground.ANY), 4.3 * 60);
// GMOS-S i-band (Hamamatsu Red CCD) 1x1 binning (unbinned)
// BG20: 1.05 hours (longer than the maximum exposure time due to cosmic rays)
// BG50: 41 minutes (may end up being longer than the maximum exposure time due to cosmic rays)
// BG80: 22.3 minutes
// BGAny: 5.5 minutes
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.i_G0327, SkyBackground.PERCENT_20), 1.05 * 60 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.i_G0327, SkyBackground.PERCENT_50), 41.0 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.i_G0327, SkyBackground.PERCENT_80), 22.3 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.i_G0327, SkyBackground.ANY), 5.5 * 60);
// GMOS-S z-band (Hamamatsu Red CCD) 1x1 binning (unbinned)
// BG20: 12.3 minutes
// BG50: 12 minutes
// BG80: 11.5 minutes
// BGAny: 8.7 minutes
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.z_G0328, SkyBackground.PERCENT_20), 12.3 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.z_G0328, SkyBackground.PERCENT_50), 12.0 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.z_G0328, SkyBackground.PERCENT_80), 11.5 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.z_G0328, SkyBackground.ANY), 8.7 * 60);
// GMOS-S Z-band (Hamamatsu Red CCD) 1x1 binning (unbinned)
// BG20: 35 minutes (may end up being longer than the maximum exposure time due to cosmic rays)
// BG50: 31.1 minutes (may end up being longer than the maximum exposure time due to cosmic rays)
// BG80: 25.8 minutes
// BGAny: 13.3 minutes
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.Z_G0343, SkyBackground.PERCENT_20), 35.0 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.Z_G0343, SkyBackground.PERCENT_50), 31.1 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.Z_G0343, SkyBackground.PERCENT_80), 25.8 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.Z_G0343, SkyBackground.ANY), 13.3 * 60);
// GMOS-S Y-band (Hamamatsu Red CCD) 1x1 binning (unbinned)
// BG20: 52.6 minutes (may end up being longer than the maximum exposure time due to cosmic rays)
// BG50: 52.7 minutes (clearly there is something not quite right with the ITC...)
// BG80: 52.8 minutes (clearly there is something not quite right with the ITC...)
// BGAny: 52.8 minutes (I suggest we call all of these 53 minutes, we already know there is an approximation with the ITC because it has no dependence of background counts on sky background in the nearIR)
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.Y_G0344, SkyBackground.PERCENT_20), 52.6 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.Y_G0344, SkyBackground.PERCENT_50), 52.7 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.Y_G0344, SkyBackground.PERCENT_80), 52.8 * 60);
        HAM_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.Y_G0344, SkyBackground.ANY), 52.8 * 60);

        // ==================
        //E2V GMOS-N

        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.g_G0301, SkyBackground.PERCENT_20), 4.35 * 60 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.g_G0301, SkyBackground.PERCENT_50), 1.83 * 60 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.g_G0301, SkyBackground.PERCENT_80), 32.5 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.g_G0301, SkyBackground.ANY), 4.5 * 60);

        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.r_G0303, SkyBackground.PERCENT_20), 1.83 * 60 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.r_G0303, SkyBackground.PERCENT_50), 1.02 * 60 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.r_G0303, SkyBackground.PERCENT_80), 25.0 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.r_G0303, SkyBackground.ANY), 4.3 * 60);

        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.i_G0302, SkyBackground.PERCENT_20), 1.05 * 60 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.i_G0302, SkyBackground.PERCENT_50), 41.0 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.i_G0302, SkyBackground.PERCENT_80), 22.3 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.i_G0302, SkyBackground.ANY), 5.5 * 60);

        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.z_G0304, SkyBackground.PERCENT_20), 12.3 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.z_G0304, SkyBackground.PERCENT_50), 12.0 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.z_G0304, SkyBackground.PERCENT_80), 11.5 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.z_G0304, SkyBackground.ANY), 8.7 * 60);

        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.Z_G0322, SkyBackground.PERCENT_20), 35.0 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.Z_G0322, SkyBackground.PERCENT_50), 31.1 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.Z_G0322, SkyBackground.PERCENT_80), 25.8 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.Z_G0322, SkyBackground.ANY), 13.3 * 60);

        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.Y_G0323, SkyBackground.PERCENT_20), 52.6 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.Y_G0323, SkyBackground.PERCENT_50), 52.7 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.Y_G0323, SkyBackground.PERCENT_80), 52.8 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterNorth.Y_G0323, SkyBackground.ANY), 52.8 * 60);

        //E2V GMOS-S

        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.u_G0332, SkyBackground.PERCENT_20), 48.6 * 60 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.u_G0332, SkyBackground.PERCENT_50), 21.1 * 60 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.u_G0332, SkyBackground.PERCENT_80), 5.78 * 60 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.u_G0332, SkyBackground.ANY), 45.0 * 60);

        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.g_G0325, SkyBackground.PERCENT_20), 4.35 * 60 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.g_G0325, SkyBackground.PERCENT_50), 1.83 * 60 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.g_G0325, SkyBackground.PERCENT_80), 32.5 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.g_G0325, SkyBackground.ANY), 4.5 * 60);

        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.r_G0326, SkyBackground.PERCENT_20), 2.56 * 60 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.r_G0326, SkyBackground.PERCENT_50), 1.43 * 60 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.r_G0326, SkyBackground.PERCENT_80), 34.0 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.r_G0326, SkyBackground.ANY), 5.9 * 60);

        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.i_G0327, SkyBackground.PERCENT_20), 2.13 * 60 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.i_G0327, SkyBackground.PERCENT_50), 1.37 * 60 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.i_G0327, SkyBackground.PERCENT_80), 43.6 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.i_G0327, SkyBackground.ANY), 10.4 * 60);

        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.z_G0328, SkyBackground.PERCENT_20), 1.06 * 60 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.z_G0328, SkyBackground.PERCENT_50), 1.0 * 60 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.z_G0328, SkyBackground.PERCENT_80), 55.0 * 60);
        E2V_EXPOSURE_LIMITS.put(new MultiKey(FilterSouth.z_G0328, SkyBackground.ANY), 35.8 * 60);

    }

    private static IConfigRule FILTER_MAX_EXPOSURE_TIME_RULE = new IConfigRule() {
        private static final String warnMsg = "The exposure time will cause the background to exceed 50% full well for the configuration and conditions";
        private static final String errMsg = "The exposure time may cause the background to saturate for the configuration and conditions";


        @Override
        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            final InstGmosCommon<?,?,?,?> inst  = (InstGmosCommon) elems.getInstrument();
            final DetectorManufacturer dm       = getDetectorManufacturer(config);
            final Filter filter                 = getFilter(config);
            final Binning xBinning              = getXBinning(config);//for imaging, binning is 1x1, 2x2 or 4x4

            for (SPSiteQuality sq : elems.getSiteQuality()) {

                final SkyBackground sb  = sq.getSkyBackground();
                final AmpGain gain      = getGain(config);
                final Double expTime    = getExposureTime(inst, config);
                final Double maxExpTime = getLimit(dm, filter, gain, sb, xBinning);

                if (expTime != null && expTime > maxExpTime) {
                    return new Problem(ERROR, PREFIX + "E_FILTER_MAX_EXPOSURE_TIME_RULE", errMsg,
                            SequenceRule.getInstrumentOrSequenceNode(step, elems));
                } else if (expTime != null && expTime > maxExpTime / 2.0) {
                    return new Problem(WARNING, PREFIX + "W_FILTER_MAX_EXPOSURE_TIME_RULE", warnMsg,
                            SequenceRule.getInstrumentOrSequenceNode(step, elems));
                }
            }
            return null;
        }

        @Override
        public IConfigMatcher getMatcher() {
            return IMAGING_MATCHER;
        }
    };

    /**
     * Auxiliary methods
     */

    // Works around a bug (?) in InstGmosCommon.getSysConfig where the FPU
    // parameter isn't added unless using a "builtin" FPU option.  Seems like
    // CUSTOM should be set in this case.
    private static FPUnit getFPU(Config config, ObservationElements elems) {
        final FPUnitMode mode = (FPUnitMode) SequenceRule.getItem(config, FPUnitMode.class, FPU_MODE_KEY);

        final FPUnit fpu;
        switch (mode) {
            case BUILTIN:
                fpu = (FPUnit) SequenceRule.getItem(config, FPUnit.class, FPU_KEY);
                break;
            case CUSTOM_MASK:
                // Okay custom mask "FPU" but *which* one. :-/
                final SPComponentType type = elems.getInstrumentNode().getType();
                fpu = InstGmosNorth.SP_TYPE.equals(type) ? FPUnitNorth.CUSTOM_MASK
                                                         : FPUnitSouth.CUSTOM_MASK;
                break;
            default:
                final String msg = String.format("New unaccounted for FPUnitMode type: %s", mode.displayValue());
                LOG.severe(msg);
                throw new RuntimeException(msg);
        }
        return fpu;
    }

    private static Disperser getDisperser(Config config) {
        return (Disperser) SequenceRule.getItem(config, Disperser.class, DISPERSER_KEY);
    }

    private static Filter getFilter(Config config) {
        return (Filter) SequenceRule.getItem(config, Filter.class, FILTER_KEY);
    }

    public static DetectorManufacturer getDetectorManufacturer(Config config) {
        return (DetectorManufacturer) SequenceRule.getItem(config, DetectorManufacturer.class, DETECTOR_KEY);
    }

    private static AmpGain getGain(Config config) {
        return (AmpGain) SequenceRule.getItem(config, AmpGain.class, GAIN_KEY);
    }

    private static Binning getXBinning(Config config) {
        return (Binning) SequenceRule.getItem(config, Binning.class, CCD_X_BINNING_KEY);
    }

    private static Binning getYBinning(Config config) {
        return (Binning) SequenceRule.getItem(config, Binning.class, CCD_Y_BINNING_KEY);
    }

    private static DTAX getDtaXOffset(Config config) {
        return (DTAX) SequenceRule.getInstrumentItem(config, InstGmosCommon.DTAX_OFFSET_PROP);
    }


    private static Double getExposureTime(InstGmosCommon<?,?,?,?> inst, Config config) {
        // REL-196.  If there are no observes, there will be no observe exposure time.
        final Double obsExp = SequenceRule.getExposureTime(config);
        if (obsExp == null) return null;

        if (inst.getUseNS() == UseNS.TRUE) {
            return obsExp * inst.getNsNumCycles() * inst.getPosList().size() + (inst.isUseElectronicOffsetting() ? 11 : 25) * inst.getNsNumCycles();
        } else {
            return obsExp;
        }
    }

    private static boolean isSpecFPUnselected(Config config, ObservationElements elems) {
        final FPUnitMode fpuMode = (FPUnitMode) SequenceRule.getInstrumentItem(config, InstGmosCommon.FPU_MODE_PROP);
        if (fpuMode == FPUnitMode.CUSTOM_MASK) return true;
        if (fpuMode != FPUnitMode.BUILTIN) return false;
        return !getFPU(config, elems).isImaging();
    }

    // From Bryan on SCT-203: I define nod_distance=sqrt((p2-p1)^2 + (q2-q1)^2)
    // Since I don't play with square roots, just return the square nod distance.
    private static double getSquareNodDistance(OffsetPos pos1, OffsetPos pos2) {
        if (pos1 == null || pos2 == null) return 0;
        double diffX = pos1.getXaxis() - pos2.getXaxis();
        double diffY = pos1.getYaxis() - pos2.getYaxis();
        return diffX * diffX + diffY * diffY;
    }

    private static IConfigRule MAX_ROI_RULE = new IConfigRule() {
        private static final String errMsgE2V = "E2V CCDs support up to 4 custom ROIs";
        private static final String errMsgHamamatsu = "Hamamatsu CCDs support up to 5 custom ROIs";

        @Override
        public Problem check(final Config config, final int step, final ObservationElements elems, final Object state) {
            final InstGmosCommon<?,?,?,?> inst = (InstGmosCommon) elems.getInstrument();
            DetectorManufacturer dm = getDetectorManufacturer(config);
            if (dm == null) {
                dm = inst.getDetectorManufacturer();
            }

            final CustomROIList customROIList = inst.getCustomROIs();
            if (customROIList != null && customROIList.size() > dm.getMaxROIs()) {
                final String prefix, msg;
                switch (dm) {
                    case HAMAMATSU:
                        prefix = PREFIX + "HAMAMATSU_MAX_ROI_RULE";
                        msg = errMsgHamamatsu;
                        break;
                    case E2V:
                        prefix = PREFIX + "E2V_MAX_ROI_RULE";
                        msg = errMsgE2V;
                        break;
                    default:
                        throw new IllegalArgumentException("unknown detector");
                }
                return new Problem(ERROR, prefix, msg, SequenceRule.getInstrumentOrSequenceNode(step, elems));
            } else {
                return null;
            }
        }

        @Override
        public IConfigMatcher getMatcher() {
            return IConfigMatcher.ALWAYS;
        }
    };

    private static IConfigRule CUSTOM_ROI_NOT_DECLARED_RULE = new IConfigRule() {
        //private static final String warnMsg = "Custom ROIs are declared but not used in any step";
        private static final String errMsg = "Custom ROIs are not declared but are used in a step";


        @Override
        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            final InstGmosCommon<?,?,?,?> inst = (InstGmosCommon) elems.getInstrument();
            BuiltinROI roi = (BuiltinROI) SequenceRule.getInstrumentItem(config, InstGmosCommon.BUILTIN_ROI_PROP);
            if (roi == null) roi = inst.getBuiltinROI();
            if (roi.equals(BuiltinROI.CUSTOM) && inst.getCustomROIs().isEmpty()) {
                return new Problem(ERROR, PREFIX + "CUSTOM_ROI_NOT_DECLARED_RULE", errMsg,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }

            return null;
        }

        @Override
        public IConfigMatcher getMatcher() {
            return IConfigMatcher.ALWAYS;
        }
    };

    private static IConfigRule ROI_OVERLAP_RULE = new IConfigRule() {
        private static final String errMsg = "The custom ROIs must not overlap";


        @Override
        public Problem check(final Config config, final int step, final ObservationElements elems, final Object state) {
            final InstGmosCommon<?,?,?,?> inst = (InstGmosCommon) elems.getInstrument();
            final DetectorManufacturer dm = inst.getDetectorManufacturer();
            final boolean overlaps;
            switch (dm) {
                case E2V:
                    overlaps = inst.getCustomROIs().rowOverlap();
                    break;
                case HAMAMATSU:
                    overlaps = inst.getCustomROIs().pixelOverlap();
                    break;
                default:
                    throw new IllegalArgumentException("unknown detector");
            }
            if (overlaps) {
                return new Problem(ERROR, PREFIX + "ROI_OVERLAP_RULE", errMsg,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            } else {
                return null;
            }
        }

        @Override
        public IConfigMatcher getMatcher() {
            return IConfigMatcher.ALWAYS;
        }
    };

    /**
     * REL-2057: It is possible to invalidate custom ROIs by changing the detector. This rule detects ROIs that
     * have been invalidated by doing so.
     */
    private static IConfigRule ROI_INVALID_RULE = new IConfigRule() {
        private static final String errMsg = "One or several custom ROIs are invalid";

        @Override
        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            final InstGmosCommon<?,?,?,?> inst = (InstGmosCommon) elems.getInstrument();
            if (!inst.validateCustomROIs()) {
                return new Problem(ERROR, PREFIX + "ROI_INVALID_RULE", errMsg,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            } else {
                return null;
            }
        }

        @Override
        public IConfigMatcher getMatcher() {
            return IConfigMatcher.ALWAYS;
        }
    };

    /**
     * REL-1811: Warn if there are P-offsets for a slit spectroscopy observation.
     * Warn for FPUs = (*arcsec or Custom Mask).
     */
    private static IConfigRule NO_P_OFFSETS_WITH_SLIT_SPECTROSCOPY_RULE = new NoPOffsetWithSlitRule(
        PREFIX,
        new AbstractFunction2<Config, ObservationElements, Boolean>() {
            public Boolean apply(Config config, ObservationElements elems) {
                return isCustomMask(config) || isSlitMask(config, elems);
            }

            private boolean isCustomMask(final Config config) {
                final FPUnitMode fpuMode =
                        (FPUnitMode) SequenceRule.getInstrumentItem(config, InstGmosCommon.FPU_MODE_PROP);
                return fpuMode == FPUnitMode.CUSTOM_MASK;
            }

            private boolean isSlitMask(final Config config, final ObservationElements elems) {
                final FPUnit fpu = getFPU(config, elems);
                return fpu.isSpectroscopic() || fpu.isNSslit();
            }
        }

    );

    private static IRule UNUSED_CUSTOM_ROI_RULE = new IRule() {
        private static final String warnMsg = "Custom ROIs are declared but not used in any step";
        private IConfigRule rule = new AbstractConfigRule() {

            @Override
            public Problem check(Config config, int step, ObservationElements elems, Object state) {
                final InstGmosCommon<?,?,?,?> inst = (InstGmosCommon) elems.getInstrument();
                BuiltinROI roi = (BuiltinROI) SequenceRule.getInstrumentItem(config, InstGmosCommon.BUILTIN_ROI_PROP);
                if (roi == null) roi = inst.getBuiltinROI();
                if (!roi.equals(BuiltinROI.CUSTOM) && !inst.getCustomROIs().isEmpty()) {
                    return new Problem(WARNING, PREFIX + "CUSTOM_ROI_NOT_DECLARED", warnMsg,
                            SequenceRule.getInstrumentOrSequenceNode(step, elems));
                }

                return null;
            }
        };

        @Override
        public IP2Problems check(ObservationElements elements) {
            final List<Problem> probs = new ArrayList<>();

            // Walk through ever config in the sequence, checking each rule.  If
            // a rule matches, remove it from the set so it won't be reported twice.
            int step = 0;
            ConfigSequence seq = elements.getSequence();
            for (Iterator<Config> it = seq.iterator(); it.hasNext(); ++step) {
                final Config config = it.next();
                Problem prob = rule.check(config, step, elements, null);
                if (prob != null) {
                    probs.add(prob);
                }
            }
            IP2Problems problems = new P2Problems();

            if (probs.size() == step) {
                problems.append(probs.get(probs.size() - 1));
            }
            return problems;
        }
    };

    /**
     * REL-1249: Warn if IFU observations have a spatial binning.
     * This rules fires for any IFU observations that do not use the mirror and has a y binning != 1.
     */
    private static IConfigRule IFU_NO_SPATIAL_BINNING_RULE = new IConfigRule() {
        private static final String errMsg = "IFU observations generally should not be binned in the spatial direction (y)";

        @Override
        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            if (getFPU(config, elems).isIFU() && !getDisperser(config).isMirror() && getYBinning(config) != Binning.ONE) {
                return new Problem(WARNING, PREFIX + "IFU_NO_SPATIAL_BINNING_RULE", errMsg,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }

            return null;
        }

        @Override
        public IConfigMatcher getMatcher() {
            return IConfigMatcher.ALWAYS;
        }
    };

    public static final String DTA_X_Y_MULTIPLE_BINNING_RULE_ID = PREFIX + "DTA_X_Y_MULTIPLE_BINNING_RULE";

    /**
     * REL-2456: We request a Phase II check to give a warning if the DTA-X
     * position is not a multiple of the y-pixel binning.
     */
    private static IConfigRule DTA_X_Y_MULTIPLE_BINNING_RULE = new IConfigRule() {
        private static final String errMsg = "The DTA X offset (%d) should be multiple of the Y-binning (%d)";

        @Override
        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            final int y = getYBinning(config).getValue();
            final int dtaX = getDtaXOffset(config).intValue();

            if (dtaX % y != 0) {
                final String msg = String.format(errMsg, dtaX, y);
                return new Problem(WARNING, DTA_X_Y_MULTIPLE_BINNING_RULE_ID, msg,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }

            return null;
        }

        @Override
        public IConfigMatcher getMatcher() {
            return IConfigMatcher.ALWAYS;
        }
    };

    /**
     * Register all the GMOS rules to apply
     */
    static {
//        GMOS_RULES.add(SequenceRule.DUMP_CONFIG_RULE);
        GMOS_RULES.add(DISPERSER_AND_MIRROR_RULE);
        GMOS_RULES.add(BAD_AMP_COUNT_RULE);
        GMOS_RULES.add(CHECK_3_AMP_MODE);
        GMOS_RULES.add(ACQUISITION_RULE);
//        GMOS_RULES.add(AMPLIFIER_SCIENCE_RULE);
        GMOS_RULES.add(GAIN_SCIENCE_RULE);
        GMOS_RULES.add(READMODE_SCIENCE_RULE);
        GMOS_RULES.add(GAIN_READMODE_RULE);
        GMOS_RULES.add(BINNING_RULE);
        GMOS_RULES.add(GMOS_S_DTA_X_RULE);
        GMOS_RULES.add(SPATIAL_DITHER_IMAGING_RULE);
        GMOS_RULES.add(CCD_BIN_AND_IQ_IMAGING_RULE);
        GMOS_RULES.add(CCD_BIN_AND_ALTAIR_IMAGING_RULE);
        GMOS_RULES.add(GRATING_NO_SLIT_SCIENCE_RULE);
        GMOS_RULES.add(B600_G5303_RULE);
        GMOS_RULES.add(SPECTROSCOPIC_ELEMENT_IN_FPU_SCIENCE_RULE);
        GMOS_RULES.add(N_S_NO_SLIT_OR_GRATING_SCIENCE_RULE);
        GMOS_RULES.add(EXP_SPECTROSCOPIC_RULE);
        GMOS_RULES.add(IFU_LEFT_SLIT_SPECTROSCOPIC_RULE);
        GMOS_RULES.add(IFU_2_SLIT_AND_FILTER_SPECTROSCOPIC_RULE);
        GMOS_RULES.add(WAVELENGTH_SPECTROSCOPIC_RULE);
        GMOS_RULES.add(DISPERSER_WAVELENGTH_SPECTROSCOPIC_RULE);
        GMOS_RULES.add(N_S_FPU_SPECTROSCOPIC_RULE);
        GMOS_RULES.add(NOD_DISTANCE_N_S_SPECTROSCOPY_RULE);
        GMOS_RULES.add(SHUFFLE_DISTANCE_N_S_SPECTROSCOPY_RULE);
        GMOS_RULES.add(N_S_CYCLES_N_S_SPECTROSCOPY_RULE);
        GMOS_RULES.add(EOFFSET_N_S_SPECTROSCOPY_RULE);
        GMOS_RULES.add(NS_SLIT_SPECTROSCOPY_RULE);
        GMOS_RULES.add(Y_BINNING_AND_SHUFFLE_DISTANCE_SPECTROSCOPY_RULE);
        GMOS_RULES.add(MAX_EXPOSURE_TIME_RULE);
        GMOS_RULES.add(FILTER_MAX_EXPOSURE_TIME_RULE);
        GMOS_RULES.add(INTEGER_EXPOSURE_TIME_RULE);
        GMOS_RULES.add(NON_ZERO_EXPOSURE_TIME_RULE);
        GMOS_RULES.add(MAX_ROI_RULE);
        GMOS_RULES.add(ROI_OVERLAP_RULE);
        GMOS_RULES.add(ROI_INVALID_RULE);
        GMOS_RULES.add(CUSTOM_ROI_NOT_DECLARED_RULE);
        GMOS_RULES.add(IFU_NO_SPATIAL_BINNING_RULE);
        GMOS_RULES.add(DTA_X_Y_MULTIPLE_BINNING_RULE);
        GMOS_RULES.add(NO_P_OFFSETS_WITH_SLIT_SPECTROSCOPY_RULE);
        GMOS_RULES.add(new MdfMaskNameRule(Problem.Type.ERROR));
        GMOS_RULES.add(new MdfMaskNameRule(Problem.Type.WARNING));
        GMOS_RULES.add(POST_2014A_GMOS_S_WITH_E2V);
    }

    public IP2Problems check(ObservationElements elems) {
        GmosSpatialDitherState state = new GmosSpatialDitherState();

        IP2Problems probs = (new CompositeRule(
                new IRule[]{new GmosOiwfsStarRule(),
                        new SequenceRule(GMOS_RULES, state),
                        AltairRule.INSTANCE, // Altair checks (See REL-386)
                        UNUSED_CUSTOM_ROI_RULE,
                },
                CompositeRule.Type.all
        )).check(elems);

        state.addWarning(probs, elems);

        final GmosOffsetIteratorRule goir = new GmosOffsetIteratorRule();
        probs.append(goir.check(elems));
        return probs;
    }
}
