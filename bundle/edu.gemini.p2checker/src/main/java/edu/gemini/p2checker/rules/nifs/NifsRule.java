package edu.gemini.p2checker.rules.nifs;

import edu.gemini.p2checker.api.*;
import edu.gemini.p2checker.rules.altair.AltairRule;
import edu.gemini.p2checker.util.AbstractConfigRule;
import edu.gemini.p2checker.util.SequenceRule;
import edu.gemini.pot.sp.ISPProgramNode;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.gemini.altair.AltairAowfsGuider;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.nifs.NIFSParams;
import edu.gemini.spModel.gemini.nifs.NifsOiwfsGuideProbe;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
public final class NifsRule implements IRule {
    private static final String PREFIX = "NifsRule_";
    private static Collection<IConfigRule> NIFS_RULES = new ArrayList<IConfigRule>();

    // Check for "imaging mirror is used in the science exposure".
    private static IConfigRule MIRROR_RULE = new AbstractConfigRule() {
        private static final String MESSAGE = "Imaging mirror is used in the science exposure";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            // This rule only applies to science observations.  Ignore all others.
            ObsClass obsClass = SequenceRule.getObsClass(config);
            if (obsClass != ObsClass.SCIENCE) return null;

            // Okay, it is a science obs.  Get the state of the imaging mirror.
            NIFSParams.ImagingMirror im;
            im = (NIFSParams.ImagingMirror) SequenceRule.getInstrumentItem(config, InstNIFS.IMAGING_MIRROR_PROP);

            // If the mirror isn't in, then we're okay -- no problem.
            if ((im == null) || (im != NIFSParams.ImagingMirror.IN)) return null;

            // mirror in science obs
            return new Problem(ERROR, PREFIX+"MIRROR_RULE", MESSAGE,
                         SequenceRule.getInstrumentOrSequenceNode(step, elems));
        }
    };

    // Exposure time checks

    private static Double getExposureTime(Config config) {
        return SequenceRule.getExposureTime(config);
    }

    //A matcher for non-Arc/Flat observes
    private static IConfigMatcher NON_FLAT_ARC_MATCHER = new IConfigMatcher() {
        public boolean matches(Config config, int step, ObservationElements elems) {
            String obsType = SequenceRule.getObserveType(config);
            return !(InstConstants.FLAT_OBSERVE_TYPE.equals(obsType) ||
                    InstConstants.ARC_OBSERVE_TYPE.equals(obsType));
        }
    };


    private static IConfigRule MIN_EXPOSURE_TIME_RULE = new AbstractConfigRule() {

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Double xtime = getExposureTime(config);
            if(xtime == null) return null;

            NIFSParams.ReadMode readMode = getReadMode(config);

            if ((readMode == NIFSParams.ReadMode.MEDIUM_OBJECT_SPEC) && (xtime < 21)) {
                return new Problem(ERROR, PREFIX+"MIN_EXPOSURE_TIME_RULE_1",
                        "Minimum exposure for medium object spectroscopy is 21 sec.",
                        SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
            } else if ((readMode == NIFSParams.ReadMode.FAINT_OBJECT_SPEC)  && (xtime < 85)) {
                return new Problem(ERROR, PREFIX+"MIN_EXPOSURE_TIME_RULE_2",
                        "Minimum exposure for faint object spectroscopy is 85 sec.",
                        SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
            } else if (xtime < 5.3) {
                return new Problem(ERROR, PREFIX+"MIN_EXPOSURE_TIME_RULE_3",
                        "Minimum exposure for NIFS is 5.3 sec.",
                        SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
            }

            return null;
        }

        //Do not apply this rule for ARC/Flat observations (SCT-303)

        public IConfigMatcher getMatcher() {
            return NON_FLAT_ARC_MATCHER;
        }

    };

    /**
     * Do not want to take a too LONG near-IR observation.
     * WARN if exposure > 900.0, "Very long exposures with NIFS are not
     * recommended because of cosmic ray contamination."
     */
    private static IConfigRule MAX_EXPOSURE_TIME_RULE = new AbstractConfigRule() {
        private static final String MESSAGE = "Very long exposure with NIFS is not recommended because of cosmic ray contamination";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Double xtime = getExposureTime(config);
            if ((xtime != null) && (xtime > 900)) {
                return new Problem(WARNING, PREFIX+"MAX_EXPOSURE_TIME_RULE", MESSAGE,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
            }
            return null;
        }
    };

    // Read mode checks

    private static NIFSParams.ReadMode getReadMode(Config config) {
        return (NIFSParams.ReadMode) SequenceRule.getInstrumentItem(config, InstNIFS.READMODE_PROP);
    }

/*  //integrated MEDIUM_READMODE_RULE and FAINT_READMODE_RULE into MIN_EXPOSURE_TIME_RULE
    // for SCI-0152 : OT NIFS minimum exposure time error
    private static IConfigRule MEDIUM_READMODE_RULE = new AbstractConfigRule() {
        private static final String MESSAGE = "Minimum exposure for medium object spectroscopy is 21 sec.";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Double xtime = getExposureTime(config);
            if ((xtime == null) || (xtime >= 21)) return null;

            NIFSParams.ReadMode readMode = getReadMode(config);
            if (readMode != NIFSParams.ReadMode.MEDIUM_OBJECT_SPEC) return null;

            return new Problem(ERROR, MESSAGE,
                    SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
        }

        //Do not apply this rule for ARC/Flat observations (SCT-303)
        public IConfigMatcher getMatcher() {
            return NON_FLAT_ARC_MATCHER;
        }
    };

    private static IConfigRule FAINT_READMODE_RULE = new AbstractConfigRule() {
        private static final String MESSAGE = "Minimum exposure for faint object spectroscopy is 85 sec.";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Double xtime = getExposureTime(config);
            if ((xtime == null) || (xtime >= 85)) return null;

            NIFSParams.ReadMode readMode = getReadMode(config);
            if (readMode != NIFSParams.ReadMode.FAINT_OBJECT_SPEC) return null;

            return new Problem(ERROR, MESSAGE,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
        }
        //Do not apply this rule for ARC/Flat observations (SCT-303)
        public IConfigMatcher getMatcher() {
            return NON_FLAT_ARC_MATCHER;
        }

    };
*/

    /**
     * ERROR if (TARGET == 'AOWFS' && TARGET_COORD == 'OIWFS_COORD')
     * Catch the case of the science target being simultaneously AOWFS and OIWFS stars.
     * To check this condition, we can compare coordinates of "Base",
     * "AOWFS", and "OIWFS". If all three coords are the same,
     * then we can raise an error.
     */

    private static IRule OI_AO_SAME_AS_SCIENCE_TARGET_RULE = new IRule() {
        private static final String MESSAGE = "For on-axis guiding, target cannot be used as OIWFS b/c of vignetting";

        P2Problems prob = new P2Problems();
        public IP2Problems check(ObservationElements elements)  {
            for (ObsContext ctx : elements.getObsContext()) {

              // We only consider the first sciencer target because a multi-target asterism is
              // a configuration error that will raise a further P2 warning.
              SPTarget baseTarget = ctx.getTargets().getAsterism().allSpTargets().head();

                final Option<SPTarget> oiTarget = getOITarget(ctx);
                final Option<SPTarget> aoTarget = getAOTarget(ctx);

                if (baseTarget == null || oiTarget.isEmpty() || aoTarget.isEmpty()) return null;
                //now, let's compare coordinates. If they are the same, raise an error

                final Option<Long> when = elements.getSchedulingBlockStart();

                baseTarget.getRaHours(when).foreach(baseC1 ->
                baseTarget.getDecDegrees(when).foreach(baseC2 ->

                oiTarget.getValue().getRaHours(when).foreach(oiC1 ->
                oiTarget.getValue().getDecDegrees(when).foreach(oiC2 ->

                aoTarget.getValue().getRaHours(when).foreach(aoC1 ->
                aoTarget.getValue().getDecDegrees(when).foreach(aoC2 -> {
                    if (Double.compare(baseC1, oiC1) == 0
                            &&
                            Double.compare(baseC2, oiC2) == 0
                            &&
                            Double.compare(oiC1, aoC1) == 0
                            &&
                            Double.compare(oiC2, aoC2) == 0) {
                        prob.addError(PREFIX + "OI_AO_SAME_AS_SCIENCE_TARGET_RULE", MESSAGE, elements.getTargetObsComponentNode().getValue());
                    }
                }

                ))))));

            }
            return prob;
        }

        private Option<SPTarget> getAOTarget(ObsContext oc) {
            return getPrimaryTarget(oc, AltairAowfsGuider.instance);
        }

        private Option<SPTarget> getOITarget(ObsContext oc) {
            return getPrimaryTarget(oc, NifsOiwfsGuideProbe.instance);
        }

        private Option<SPTarget> getPrimaryTarget(ObsContext oc, GuideProbe guider) {
            final TargetEnvironment env = oc.getTargets();
            if (!GuideProbeUtil.instance.isAvailable(oc, guider)) return None.instance();
            final Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
            if (gtOpt.isEmpty()) return None.instance();
            final GuideProbeTargets gt = gtOpt.getValue();
            return gt.getPrimary();
        }
    };


//    private static IConfigRule COADDS_RULE = new AbstractConfigRule() {
//        private static final String MESSAGE = "Do you really need to use coadd? NIFS readout overhead is quite large";
//
//        public Problem check(Config config, int step, ObservationElements elems, Object state) {
//            Double xtime = getExposureTime(config);
//            if ((xtime == null) || (xtime >= 60)) return null;
//
//            // Check for coadds and < 60 exp time.
//            Integer coadds = SequenceRule.getCoadds(config);
//            if ((coadds == null) || (coadds <= 1)) return null;
//
//            return new Problem(WARNING, MESSAGE,
//                            SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
//        }
//    };



    /**
     * SCT-203 # Check if WFS are freezed properly for sky offsets.
     *  WARN  if offset > 5.0 && WFS != 'freeze'
     * ---
     * SCT-259
     * # Check if WFS are freezed properly for sky offsets.
     *   if (guidestar == 'AOWFS') {
     * ERROR if (offset > 7.0 && Altair_Field_Lens == 'Out' && WFS != 'freeze'),\
     *  "Offsets to sky are large with no AO freezing and no field lens. Freeze
     *  the guiding or input the field lens to prevent poor AO correction."
     *    ERROR if (offset > 23.0 && Altair_Field_Lens == 'In' && WFS != 'freeze'),\
     *  "Offsets to sky are large with no AO freezing. Freeze the guiding
     * to prevent poor AO correction."
     *
     * if (guidestar == 'PWFS2') {
     *   WARN if (offset > 25.0 && WFS != 'freeze'),\
     *  "Offsets to sky are large with no guide freezing. Consider freezing
     * the guiding to prevent poor correction."
     *   }
     *
     */
    //TODO: Implement changes as defined on SCT-259. Commenting out the following rule for now
    /*
    private static final String TELESCOPE_KEY = "telescope:";
    private static final ItemKey GUIDE_P1_KEY = new ItemKey(TELESCOPE_KEY + TargetEnvConstants.GUIDE_WITH_PWFS1_PROP);
    private static final ItemKey GUIDE_P2_KEY = new ItemKey(TELESCOPE_KEY + TargetEnvConstants.GUIDE_WITH_PWFS2_PROP);
    private static final ItemKey GUIDE_OI_KEY = new ItemKey(TELESCOPE_KEY + TargetEnvConstants.GUIDE_WITH_OIWFS_PROP);
    private static final ItemKey GUIDE_AO_KEY = new ItemKey(TELESCOPE_KEY + TargetEnvConstants.GUIDE_WITH_AOWFS_PROP);

    private static IConfigRule SKY_OFFSET_RULE = new AbstractConfigRule() {
        private static final String MESSAGE =  "WFS needs to be 'freeze' for sky offset positions";
        private static final double OFFSET_LIMIT = 5.0;
        private static final String FREEZE_STATE = "freeze";
        private static final String PARK_STATE = "park";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {

            Double offsetP = SequenceRule.getPOffset(config);
            Double offsetQ = SequenceRule.getQOffset(config);

            if (offsetP == null || offsetQ == null) return null; //can't check

            offsetP = Math.abs(offsetP);
            offsetQ = Math.abs(offsetQ);

            if (offsetP <= OFFSET_LIMIT && offsetQ <= OFFSET_LIMIT) return null; //no problems with small offsets

            String guideP1 = (String)SequenceRule.getItem(config, String.class, GUIDE_P1_KEY);
            String guideP2 = (String)SequenceRule.getItem(config, String.class, GUIDE_P2_KEY);
            String guideOI = (String)SequenceRule.getItem(config, String.class, GUIDE_OI_KEY);
            String guideAO = (String)SequenceRule.getItem(config, String.class, GUIDE_AO_KEY);

            if (isFrozenOrParked(guideP1) && isFrozenOrParked(guideP2) && isFrozenOrParked(guideOI) && isFrozenOrParked(guideAO)) {
                return null; //all the guiders are freeze, so no error
            }
            return new Problem(WARNING, MESSAGE, elems.getSeqComponentNode()); //always the error in the seq. node
        }

        private boolean isFrozenOrParked(String guide) {
            return (guide == null || FREEZE_STATE.equals(guide) || PARK_STATE.equals(guide));
        }
    };
    */

    // Disperser/filter/wavelength checks.

    private static NIFSParams.Disperser getDisperser(Config config) {
        return (NIFSParams.Disperser) SequenceRule.getInstrumentItem(config, InstNIFS.DISPERSER_PROP);
    }

    private static NIFSParams.Filter getFilter(Config config) {
        return (NIFSParams.Filter) SequenceRule.getInstrumentItem(config, InstNIFS.FILTER_PROP);
    }

    public static Double getCentralWavelength(Config config) {
        return (Double) SequenceRule.getInstrumentItem(config, InstNIFS.CENTRAL_WAVELENGTH_PROP);
    }

    private static class GratingMatcher implements IConfigMatcher {
        private NIFSParams.Disperser[] _dispersers;

        GratingMatcher(NIFSParams.Disperser[] dispersers) {
            _dispersers  = dispersers;
        }

        public boolean matches(Config config, int step, ObservationElements elems) {
            NIFSParams.Disperser disp = getDisperser(config);
            if (disp == null) return false;

            for (NIFSParams.Disperser cur : _dispersers) {
                if (cur == disp) return true;
            }
            return false;
        }
    }

    private static GratingMatcher K_GRATINGS_MATCHER = new GratingMatcher(
            new NIFSParams.Disperser[] { NIFSParams.Disperser.K, NIFSParams.Disperser.K_SHORT, NIFSParams.Disperser.K_LONG }
    );

    private static GratingMatcher H_GRATING_MATCHER = new GratingMatcher(
            new NIFSParams.Disperser[] { NIFSParams.Disperser.H }
    );

    private static GratingMatcher J_GRATING_MATCHER = new GratingMatcher(
            new NIFSParams.Disperser[] { NIFSParams.Disperser.J }
    );

    private static GratingMatcher Z_GRATING_MATCHER = new GratingMatcher(
            new NIFSParams.Disperser[] { NIFSParams.Disperser.Z }
    );


    private static class GratingFilterRule implements IConfigRule {
        private NIFSParams.Filter[] _filters;
        private IConfigMatcher _matcher;
        private Double _wavelength;

        private GratingFilterRule(NIFSParams.Filter[] filts, IConfigMatcher matcher) {
            this(filts, null, matcher);
        }

        private GratingFilterRule(NIFSParams.Filter[] filts, Double wavelength, IConfigMatcher matcher) {
            _filters = filts;
            _matcher = matcher;
            _wavelength = wavelength;

        }


        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            boolean match = false;
            NIFSParams.Filter filt = getFilter(config);
            for (NIFSParams.Filter cur : _filters) {
                if (filt == cur) {
                    match = true;
                    break;
                }
            }

            if (!match) {
                StringBuilder buf = new StringBuilder("Wrong filter. ");
                if (_wavelength != null) {
                    Double wavelength = getCentralWavelength(config);
                    if (wavelength == null) return null; //can't check without a central wavelength
                    if (wavelength <= _wavelength) {
                        buf.append("For selected central wavelength should use '");
                    } else {
                        return null; //no errors in this case
                    }
                } else {// we don't consider the wavelength
                    buf.append("Has to be '");
                }
                buf.append(_filters[0].displayValue()).append("'");
                for (int i=1; i<(_filters.length-1); ++i) {
                    buf.append(", '").append(_filters[i].displayValue()).append("'");
                }
                if (_filters.length > 1) {
                    buf.append(" or '").append(_filters[_filters.length-1].displayValue()).append("'");
                }
                return new Problem(ERROR, PREFIX+"check", buf.toString(),
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }
            return null;
        }

        public IConfigMatcher getMatcher() {
            return _matcher;
        }
    }


    private static class GratingFilterWavelengthRule implements IConfigRule {
            private NIFSParams.Filter _filter;
            private IConfigMatcher _matcher;
            private double _wavelength;
            private static final String MESSAGE = "Wrong filter. For selected central wavelength should use %s";

            private GratingFilterWavelengthRule(NIFSParams.Filter filter, double wavelength, IConfigMatcher matcher) {
                _filter = filter;
                _matcher = matcher;
                _wavelength = wavelength;
            }

            public Problem check(Config config, int step, ObservationElements elems, Object state) {
                NIFSParams.Filter filt = getFilter(config);

                if (filt == _filter) return null; //no error if filter matches
                Double wavel = getCentralWavelength(config);
                if (wavel == null) return null; //can't check if can't find central wavelength

                if (wavel > _wavelength) {
                    return new Problem(ERROR, PREFIX+"GratingFilterWavelengthRule",
                            String.format(MESSAGE, _filter.displayValue()),
                            SequenceRule.getInstrumentOrSequenceNode(step, elems));
                }
                return null;
            }

            public IConfigMatcher getMatcher() {
                return _matcher;
            }
        }


    private static class GratingWavelengthRule implements IConfigRule {
        private static final String MESSAGE = "Central Wavelength setting is outside of the allowed range for the " +
                "selected grating.";

        private double _minWavelength;
        private double _maxWavelength;
        private IConfigMatcher _matcher;

        private GratingWavelengthRule(double minwl, double maxwl, IConfigMatcher matcher) {
            _minWavelength = minwl;
            _maxWavelength = maxwl;
            _matcher = matcher;
        }

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Double wavel = getCentralWavelength(config);
            if (wavel == null) return null;

            if ((wavel < _minWavelength) || (wavel > _maxWavelength)) {
                return new Problem(ERROR, PREFIX+"GratingWavelengthRule", MESSAGE,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }

            return null;
        }

        public IConfigMatcher getMatcher() {
            return _matcher;
        }
    }

    private static IConfigRule K_GRATING_FILTER_RULE = new GratingFilterRule(
            new NIFSParams.Filter[] {NIFSParams.Filter.HK_FILTER, NIFSParams.Filter.SAME_AS_DISPERSER},
            K_GRATINGS_MATCHER
    );

    private static IConfigRule K_GRATING_WAVELENGTH_RULE = new GratingWavelengthRule(
            1.98, 2.41, K_GRATINGS_MATCHER
    );

    private static IConfigRule H_GRATING_FILTER_RULE = new GratingFilterRule(
            new NIFSParams.Filter[] {NIFSParams.Filter.JH_FILTER, NIFSParams.Filter.SAME_AS_DISPERSER},
            1.70,
            H_GRATING_MATCHER
    );

    private static IConfigRule H_GRATING_FILTER_WAVELENGTH_RULE = new GratingFilterWavelengthRule(
            NIFSParams.Filter.HK_FILTER, 1.70, H_GRATING_MATCHER
    );

    private static IConfigRule H_GRATING_WAVELENGTH_RULE = new GratingWavelengthRule(
            1.48, 1.82, H_GRATING_MATCHER
    );

    private static IConfigRule J_GRATING_FILTER_RULE = new GratingFilterRule(
            new NIFSParams.Filter[] {NIFSParams.Filter.ZJ_FILTER, NIFSParams.Filter.SAME_AS_DISPERSER},
            1.30,
            J_GRATING_MATCHER
    );

    private static IConfigRule J_GRATING_FILTER_WAVELENGTH_RULE = new GratingFilterWavelengthRule(
                NIFSParams.Filter.JH_FILTER, 1.30, J_GRATING_MATCHER
    );


    private static IConfigRule J_GRATING_WAVELENGTH_RULE = new GratingWavelengthRule(
            1.14, 1.36, J_GRATING_MATCHER
    );

    private static IConfigRule Z_GRATING_FILTER_RULE = new GratingFilterRule(
            new NIFSParams.Filter[] {NIFSParams.Filter.ZJ_FILTER, NIFSParams.Filter.SAME_AS_DISPERSER},
            Z_GRATING_MATCHER
    );

    private static IConfigRule Z_GRATING_WAVELENGTH_RULE = new GratingWavelengthRule(
            0.94, 1.16, Z_GRATING_MATCHER
    );

    /**
     * Class to apply set of rules related to the ReadMode and the exposure times. The actual rules are defined as
     * ReadModeChecker objects later, registered with this class.
     */
    private static class ReadModeRules extends AbstractConfigRule {

        interface ReadModeChecker {
            Problem check(Double expTime, NIFSParams.ReadMode readMode, ISPProgramNode node);
        }

        public static ReadModeRules INSTANCE = new ReadModeRules();

        public List<ReadModeChecker> checkers = new ArrayList<ReadModeChecker>();

        public void registerChecker(ReadModeChecker checker) {
            if (checker == null) return;
            checkers.add(checker);
        }

        public Problem check(Config config, int step, ObservationElements elems, Object state) {

            Double expTime = getExposureTime(config);
            if (expTime == null) return null;

            NIFSParams.ReadMode readMode = getReadMode(config);

            if (readMode == null) return null;

            //apply all the rules associated to exposure times. If one matches, return that problem

            for (ReadModeChecker checker : checkers) {
                Problem p = checker.check(expTime, readMode, SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
                if (p != null) {
                    return p;
                }
            }
            return null;
        }

        //Do not apply these rules for ARC/Flat observations (SCT-303)
        public IConfigMatcher getMatcher() {
            return NON_FLAT_ARC_MATCHER;
        }

    }

    /**
     * #Readmode checks for long exposures
     * WARN if (exposure > 180.0 && Readmode = 'Bright Object')
     * "Exposure time is long but readmode is for a bright object (high
     * noise), are you sure you do not want a lower noise readout?"
     */
    private static ReadModeRules.ReadModeChecker LONG_EXPOSURE_CHECKER = new ReadModeRules.ReadModeChecker() {
        private static final String MESSAGE = "Exposure time is long but readmode is for a bright object (high " +
                "noise), are you sure you do not want a lower noise readout?";

        public Problem check(Double expTime, NIFSParams.ReadMode readMode, ISPProgramNode node) {
            if (expTime > 180 && readMode == NIFSParams.ReadMode.BRIGHT_OBJECT_SPEC) {
                return new Problem(WARNING, PREFIX+"LONG_EXPOSURE_CHECKER", MESSAGE, node);
            }
            return null;
        }
    };

    /**
     * #Readmode checks for short exposures
     * WARN if (exposure < 100.0 && Readmode = 'Faint Object')
     * "Exposure time is relatively short but readmode is "Faint Object".
     * Observing efficiency will be on the order of 50%. are you sure you
     * do not want a higher noise readout to increase efficiency?"
     */
    private static ReadModeRules.ReadModeChecker SHORT_EXPOSURE_FAINT_OBJECT_CHECKER = new ReadModeRules.ReadModeChecker() {
        private static final String MESSAGE = "Exposure time is relatively short but readmode is 'Faint Object'. " +
                "Observing efficiency will be on the order of 50%. " +
                "Are you sure you do not want a higher noise readout to increase efficiency?";

        public Problem check(Double expTime, NIFSParams.ReadMode readMode, ISPProgramNode node) {
            if (expTime < 100 && readMode == NIFSParams.ReadMode.FAINT_OBJECT_SPEC) {
                return new Problem(WARNING, PREFIX+"SHORT_EXPOSURE_FAINT_OBJECT_CHECKER", MESSAGE, node);
            }
            return null;
        }
    };

    /**
     * WARN if (exposure < 30.0 && Readmode = 'Medium')
     * "Exposure time is short and readmode is 'Medium Noise'.
     * Observing efficiency excluding offsets will be on the order of 50%.
     * Are you sure you do not want the high noise readout to
     * increase efficiency?"
     */
    private static ReadModeRules.ReadModeChecker SHORT_EXPOSURE_MEDIUM_NOISE_CHECKER = new ReadModeRules.ReadModeChecker() {
        private static final String MESSAGE = "Exposure time is short and readmode is 'Medium Noise'. " +
                "Observing efficiency excluding offsets will be on the order of 50%. " +
                "Are you sure you do not want the high noise readout to " +
                "increase efficiency?";

        public Problem check(Double expTime, NIFSParams.ReadMode readMode, ISPProgramNode node) {
            if (expTime < 30 && readMode == NIFSParams.ReadMode.MEDIUM_OBJECT_SPEC) {
                return new Problem(WARNING, PREFIX+"SHORT_EXPOSURE_MEDIUM_NOISE_CHECKER", MESSAGE, node);
            }
            return null;
        }
    };

    //Set up the ReadMode Rules
    static {
        ReadModeRules.INSTANCE.registerChecker(LONG_EXPOSURE_CHECKER);
        ReadModeRules.INSTANCE.registerChecker(SHORT_EXPOSURE_FAINT_OBJECT_CHECKER);
        ReadModeRules.INSTANCE.registerChecker(SHORT_EXPOSURE_MEDIUM_NOISE_CHECKER);
    }


    private static NIFSParams.Mask getMask(Config config) {
        return (NIFSParams.Mask) SequenceRule.getInstrumentItem(config, InstNIFS.MASK_PROP);
    }

    /**
     * ERROR if (Mask =="0.1 Pinhole"),\
     * "The defined focal plane mask is used for Engineering purposes only."
     * ERROR if (Mask =="0.2 Pinhole Array"),\
     * "The defined focal plane mask is used for Engineering purposes only."
     * ERROR if (Mask =="0.2 Slit"),\
     * "The defined focal plane mask is used for Engineering purposes only."
     *
     * and
     *   ERROR if (Mask =="0.1 Occulting Disk"),\
     * "Focal Plane Mask Unit is set to 0.1 Occulting Disk which is not
     * used because the PSF with NIFS HAS FWHM >0.1"
     */
    private static IConfigRule ENG_MASK_RULES = new AbstractConfigRule() {
        private static final String MESSAGE = "The %s focal plane mask is used for Engineering purposes only";
        private static final String UNUSED_MASK = "Focal Plane Mask Unit is set to 0.1 arcsec Occulting Disk which is not " +
                "used because the PSF with NIFS has FWHM > 0.1\"";
        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            NIFSParams.Mask mask = getMask(config);
            if (mask == null) return null;
            switch (mask) {
                case PINHOLE:
                case PINHOLE_ARRAY:
                case SLIT:
                    return new Problem(ERROR, PREFIX+"ENG_MASK_RULES_MESSAGE", String.format(MESSAGE, mask.displayValue()),
                            SequenceRule.getInstrumentOrSequenceNode(step,elems));
                case OD_1:
                    return new Problem(ERROR, PREFIX+"ENG_MASK_RULES_UNUSED_MASK", UNUSED_MASK,
                            SequenceRule.getInstrumentOrSequenceNode(step,elems));
                default:
                    return null;
            }
        }
    };

    /**
     * ERROR if (Mask =="Ronche_Cal_Mask" && !="Daytime Calibration"),\
     * "The Ronchi Calibration Mask is used only for Daytime Calibration
     * Observations."
     */
    private static IConfigRule DAYCAL_MASK_RULE = new AbstractConfigRule() {
        private static final String MESSAGE = "The Ronchi Calibration Mask is used only for Daytime Calibration " +
                "Observations";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            ObsClass obsClass = SequenceRule.getObsClass(config);

            if (obsClass == ObsClass.DAY_CAL) return null; //no need to check in this case

            NIFSParams.Mask mask = getMask(config);

            if (mask == NIFSParams.Mask.RONCHE) {
                return new Problem(ERROR, PREFIX+"DAYCAL_MASK_RULE",
                        MESSAGE, SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }

            return null;
        }
    };

    /**
     * ERROR if (Mask =="Blocked" && observe !="Dark"),\
     * "The focal plane mask unit is blocked though the defined
     *  observation is not a Dark."
     */
    private static IConfigRule DARK_MASK_RULE = new AbstractConfigRule() {

        private static final String MESSAGE = "The focal plane mask unit is blocked though the defined " +
                " observation is not a Dark";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {

            NIFSParams.Mask mask = getMask(config);
            if (mask != NIFSParams.Mask.BLOCKED) return null;

            //the mask is set to blocked. Let's make sure this is for
            //a Dark observe
            String obsType = SequenceRule.getObserveType(config);
            if (!InstConstants.DARK_OBSERVE_TYPE.equals(obsType)) {
                return new Problem(ERROR, PREFIX+"DARK_MASK_RULE", MESSAGE,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }
            return null;
        }
    };

    /**
     * NIRI + Altair
     */
    private static IConfigMatcher ALTAIR_MATCHER = new IConfigMatcher() {

        public boolean matches(Config config, int step, ObservationElements elems) {
            return elems.hasAltair();
        }
    };

    /**
     * Rules for NIFS + Altair.
     * See REL-386.
     */
    private static IConfigRule ALTAIR_RULE = new AbstractConfigRule() {

        private static final String MESSAGE = "Altair typically provides Strehls of 5% or less for wavelengths shorter than 1.3 microns";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            NIFSParams.Disperser disperser = (NIFSParams.Disperser) SequenceRule.getInstrumentItem(config, InstNIFS.DISPERSER_PROP);
            if (disperser != null && disperser.getWavelength() < 1.3) {
                return new Problem(WARNING, PREFIX + "ALTAIR_RULE", MESSAGE, SequenceRule.getInstrumentOrSequenceNode(step,elems));
            }
            return null;
        }

        public IConfigMatcher getMatcher() {
            return ALTAIR_MATCHER;
        }
    };


    static {
//        NIFS_RULES.add(SequenceRule.DUMP_CONFIG_RULE);
        NIFS_RULES.add(MIRROR_RULE);
        NIFS_RULES.add(MIN_EXPOSURE_TIME_RULE);
        NIFS_RULES.add(MAX_EXPOSURE_TIME_RULE);
   //     NIFS_RULES.add(MEDIUM_READMODE_RULE);
   //     NIFS_RULES.add(FAINT_READMODE_RULE);
        NIFS_RULES.add(K_GRATING_FILTER_RULE);
        NIFS_RULES.add(K_GRATING_WAVELENGTH_RULE);
        NIFS_RULES.add(H_GRATING_FILTER_RULE);
        NIFS_RULES.add(H_GRATING_FILTER_WAVELENGTH_RULE);
        NIFS_RULES.add(H_GRATING_WAVELENGTH_RULE);
        NIFS_RULES.add(J_GRATING_FILTER_RULE);
        NIFS_RULES.add(J_GRATING_FILTER_WAVELENGTH_RULE);
        NIFS_RULES.add(J_GRATING_WAVELENGTH_RULE);
        NIFS_RULES.add(Z_GRATING_FILTER_RULE);
        NIFS_RULES.add(Z_GRATING_WAVELENGTH_RULE);
      //  NIFS_RULES.add(SKY_OFFSET_RULE);
        NIFS_RULES.add(ReadModeRules.INSTANCE);
        NIFS_RULES.add(ENG_MASK_RULES);
        NIFS_RULES.add(DAYCAL_MASK_RULE);
        NIFS_RULES.add(DARK_MASK_RULE);
        NIFS_RULES.add(ALTAIR_RULE);
    }

    public IP2Problems check(ObservationElements elements)  {
        IP2Problems prob = (new SequenceRule(NIFS_RULES, null)).check(elements);
        prob.append(OI_AO_SAME_AS_SCIENCE_TARGET_RULE.check(elements));

        // Altair checks (See REL-386)
        prob.append(AltairRule.INSTANCE.check(elements));

        return prob;
    }
}
