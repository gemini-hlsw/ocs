package edu.gemini.p2checker.rules.trecs;

import edu.gemini.p2checker.api.*;
import edu.gemini.p2checker.util.AbstractConfigRule;
import edu.gemini.p2checker.util.SequenceRule;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.gemini.trecs.InstTReCS;
import edu.gemini.spModel.gemini.trecs.TReCSParams.Disperser;
import edu.gemini.spModel.gemini.trecs.TReCSParams.Filter;
import edu.gemini.spModel.gemini.trecs.TReCSParams.Mask;
import edu.gemini.spModel.gemini.trecs.TReCSParams.WindowWheel;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import jsky.coords.WorldCoords;


import java.util.*;

/**
 * Rule set for TReCS
 */
public class TrecsRule implements IRule {

    private static final String PREFIX = "TrecsRule_";
    private static Collection<IConfigRule> TRECS_RULES = new ArrayList<IConfigRule>();

    /**
     * Class to implement this set of rules
     * % It is preferable to leave the the 'window' option in the OT at 'auto'
     * % some filters are clearly incompatible with the ZnSe window
     * WARN if window != 'auto', "we recommend that you leave Window WWheel at 'auto'"
     * ERROR if window = 'ZnSe' && filter == 'Qa' , "Please set Window Wheel to 'auto' or use KRS-5."
     * ERROR if window = 'ZnSe' && filter == 'Qb' , "Please set Window Wheel to 'auto' or use KRS-5."
     * ERROR if window = 'ZnSe' && filter == 'Qbroad', "Please set Window Wheel to AUTO or use KRS-5."
     */
    private static final class WindowRule extends AbstractConfigRule {
        private static final String ERROR_MESSAGE = "Please set Window Wheel to 'auto' or use KRS-5";
        private static final String WARN_MESSAGE = "We recommend that you leave the Window Wheel at 'auto'";

        private static Set<Filter> INCOMPAT_FILTERS = EnumSet.noneOf(Filter.class);

        //define the set of filters that are incompatible with the ZnSe Window
        static {
            INCOMPAT_FILTERS.add(Filter.QA);
            INCOMPAT_FILTERS.add(Filter.QB);
            INCOMPAT_FILTERS.add(Filter.Q);
        }

        private static final WindowRule INSTANCE = new WindowRule();

        private WindowRule() {
        }

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            WindowWheel windowWheel = (WindowWheel) SequenceRule.getInstrumentItem(config, InstTReCS.WINDOW_WHEEL_PROP);
            Filter filter = (Filter) SequenceRule.getInstrumentItem(config, InstTReCS.FILTER_PROP);

            if (windowWheel == WindowWheel.ZNSE
                    && INCOMPAT_FILTERS.contains(filter)) {
                return new Problem(ERROR, PREFIX+"AbstractConfigRuleError", ERROR_MESSAGE,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }

            if (windowWheel != WindowWheel.AUTO) {
                return new Problem(WARNING, PREFIX+"AbstractConfigRuleWarning", WARN_MESSAGE,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }

            return null;
        }
    }

    private static IConfigMatcher IMAGING_MATCHER = new IConfigMatcher() {

        public boolean matches(Config config, int step, ObservationElements elems) {
            Mask mask = (Mask) SequenceRule.getInstrumentItem(config, InstTReCS.MASK_PROP);
            return mask == Mask.MASK_IMAGING;
        }
    };

    /**
     * % There's no point in using a disperser without a slit
     * ERROR if mask == 'imaging' && disperser == 'lo-res-10_G5401',"Can't use a disperser without a slit"
     * ERROR if mask == 'imaging' && disperser == 'hi-res-10_G5403',"Can't use a disperser without a slit"
     * ERROR if mask == 'imaging' && disperser == 'lo-res-20_G5402',"Can't use a disperser without a slit"
     */
    private static IConfigRule DISPERSER_NO_SLIT_RULE = new IConfigRule() {
        private static final String MESSAGE = "Can't use a disperser without a slit";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Disperser disperser = (Disperser) SequenceRule.getInstrumentItem(config, InstTReCS.DISPERSER_PROP);
            switch (disperser) {
                case LOW_RES_10:
                case HIGH_RES:
                case LOW_RES_20:
                    return new Problem(ERROR, PREFIX+"DISPERSER_NO_SLIT_RULE", MESSAGE,
                            SequenceRule.getInstrumentOrSequenceNode(step, elems));
                default:
                    return null;
            }
        }

        public IConfigMatcher getMatcher() {
            return IMAGING_MATCHER;
        }
    };

    private static IConfigRule CHOP_THROW_RULE = new AbstractConfigRule() {
        private static final String MESSAGE = "The chop throw cannot be larger than 15 arcseconds";
        private static final double MAX_CHOPTHROW = 15.0;

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Double chopThrow = (Double) SequenceRule.getInstrumentItem(config, InstTReCS.CHOP_THROW_PROP);
            if (chopThrow == null) return null;

            if (chopThrow > MAX_CHOPTHROW) {
                //the UI doens't allow to set the chopthrow to something over 15, so this error
                //only can be triggered by the sequence
                return new Problem(ERROR, PREFIX+"CHOP_THROW_RULE", MESSAGE, elems.getSeqComponentNode());
            }
            return null;
        }
    };

    /**
     * Implements the rule:
     * % We should discuss CC conditions and how they apply to mid-IR
     * % observations. There's no way to separate 'fluffy clouds' from 'thin cirrus'
     * % in the OT. CC=ANY is probably useless for all programs.
     */

    private static IRule CC_RULE = new IRule() {
        private static final String MESSAGE = "T-ReCS cannot be used in CC=ANY conditions";

        public IP2Problems check(ObservationElements elements)  {
            for (SPSiteQuality sq : elements.getSiteQuality()) {


                if (sq.getCloudCover() == SPSiteQuality.CloudCover.ANY) {
                    IP2Problems prob = new P2Problems();
                    prob.addError(PREFIX + "CC_RULE", MESSAGE, elements.getSiteQualityNode().getValue());
                    return prob;
                }
            }
            return null;
        }
    };


    /**
     * Rule for:
     * % Qbroad and N filters are the only ones likely to be used in spectroscopy
     * WARN if disperser == 'lo-res-10_G5401' && filter != 'N', "N filter is normally used with this grating"
     * WARN if disperser == 'hi-res-10_G5401' && filter != 'N', "N filter is normally used with this grating"
     * WARN if disperser == 'lo-res-20_G5402' && filter != 'Qbroad', "Qbroad filter is normally used with this grating"
     */
    private static class SpectroscopyFiltersRule extends AbstractConfigRule {

        private static final String MESSAGE = "%s filter is normally used with this grating";

        private static final Map<Disperser, Filter> configs = new HashMap<Disperser, Filter>();

        private static final SpectroscopyFiltersRule INSTANCE = new SpectroscopyFiltersRule();

        private SpectroscopyFiltersRule() {
        }

        static {
            configs.put(Disperser.LOW_RES_10, Filter.N);
            configs.put(Disperser.HIGH_RES, Filter.N);
            configs.put(Disperser.LOW_RES_20, Filter.Q);
        }


        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Disperser disperser = (Disperser) SequenceRule.getInstrumentItem(config, InstTReCS.DISPERSER_PROP);

            if (disperser == null) return null;

            Filter validFilter = configs.get(disperser);

            if (validFilter == null) return null; //we don't care about this disperser config.

            Filter filter = (Filter) SequenceRule.getInstrumentItem(config, InstTReCS.FILTER_PROP);

            if (validFilter != filter) {
                return new Problem(WARNING, PREFIX+"SpectroscopyFiltersRule",
                        String.format(MESSAGE, validFilter.displayValue()),
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }
            return null;
        }
    }

    /**
     * Rule for
     * % K, L, and M filters are only used for engineering
     * WARN filter == 'K', "The K filter should only be used for engineering."
     * WARN filter == 'L', "The L filter should only be used for engineering."
     * WARN filter == 'M', "The M filter should only be used for engineering."
     */

    private static final IConfigRule ENGINEERING_FILTERS_RULE = new AbstractConfigRule() {
        private static final String MESSAGE = "The %s filter should only be used for engineering";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Filter filter = (Filter) SequenceRule.getInstrumentItem(config, InstTReCS.FILTER_PROP);
            switch (filter) {
                case K:
                case L:
                case M:
                    return new Problem(WARNING, PREFIX+"ENGINEERING_FILTERS_RULE",
                            String.format(MESSAGE, filter.displayValue()),
                            SequenceRule.getInstrumentOrSequenceNode(step, elems));
                default:
                    return null;
            }
        }
    };

    /**
     * Some combinations of dispersers and filters are clearly wrong
     * ERROR if disperser == 'hi-res-10_G5403' && filter == 'Qa', "This grating is used only in the 8-13 micron domain"
     * ERROR if disperser == 'hi-res-10_G5403' && filter == 'Qb', "This grating is used only in the 8-13 micron domain"
     * ERROR if disperser == 'hi-res-10_G5403' && filter == 'Qbroad', "This grating is used only in the 8-13 micron domain"
     * ERROR if disperser == 'hi-res-10_G5403' && filter == 'K', "This grating is used only in the 8-13 micron domain"
     * ERROR if disperser == 'hi-res-10_G5403' && filter == 'L', "This grating is used only in the 8-13 micron domain"
     * ERROR if disperser == 'hi-res-10_G5403' && filter == 'M', "This grating is used only in the 8-13 micron domain"
     */


    private static IConfigRule DISPERSER_WRONG_FILTER_RULE = new AbstractConfigRule() {
        private static final String MESSAGE = "This grating is used only in the 8-13 micron domain";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Disperser disperser = (Disperser) SequenceRule.getInstrumentItem(config, InstTReCS.DISPERSER_PROP);
            if (disperser != Disperser.HIGH_RES) return null;
            Filter filter = (Filter) SequenceRule.getInstrumentItem(config, InstTReCS.FILTER_PROP);
            switch (filter) {
                case QA:
                case QB:
                case Q:
                case K:
                case L:
                case M:
                    return new Problem(ERROR, PREFIX+"DISPERSER_WRONG_FILTER_RULE", MESSAGE,
                            SequenceRule.getInstrumentOrSequenceNode(step, elems));
                default:
                    return null;
            }
        }
    };

    /**
     * More invalid configurations
     * ERROR if disperser != 'mirror' && filter == 'K', "No spectroscopy in K band"
     * ERROR if disperser != 'mirror' && filter == 'L', "No spectroscopy in L band"
     * ERROR if disperser != 'mirror' && filter == 'M', "No spectroscopy in M band"
     */
    private static IConfigRule SPECTROSCOPY_WRONG_BAND_RULE = new AbstractConfigRule() {
        private static final String MESSAGE = "No spectroscopy in %s band";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Disperser disperser = (Disperser) SequenceRule.getInstrumentItem(config, InstTReCS.DISPERSER_PROP);
            if (disperser == Disperser.MIRROR) return null; //not spectroscopy
            Filter filter = (Filter) SequenceRule.getInstrumentItem(config, InstTReCS.FILTER_PROP);
            switch (filter) {
                case K:
                case L:
                case M:
                    return new Problem(ERROR, PREFIX+"SPECTROSCOPY_WRONG_BAND_RULE",
                            String.format(MESSAGE, filter.displayValue()),
                            SequenceRule.getInstrumentOrSequenceNode(step, elems));
                default:
                    return null;
            }
        }
    };

    /**
     * Rule for Sky Background and TReCS
     */
    private static IRule SKY_BG_RULE = new IRule() {
        private static final String SKY_BG_MESSAGE =
            "MID-IR observations are not affected by the moon. " +
            "Only in the case of a very faint guide star should moon constraints " +
            "be necessary.";

        public P2Problems check(ObservationElements elements)  {
            if (elements == null || elements.getSiteQualityNode().isEmpty()) return null; // can't check
            P2Problems problems = new P2Problems();
            for (SPSiteQuality sq : elements.getSiteQuality()) {
                SPSiteQuality.SkyBackground bg = sq.getSkyBackground();
                if (bg != SPSiteQuality.SkyBackground.ANY) {
                    problems.addWarning(PREFIX + "SKY_BG_RULE", SKY_BG_MESSAGE, elements.getSiteQualityNode().getValue());
                }
            }
            return problems;
        }
    };

    /**
     * Rule for guide stars
     * 1) Error if guidestar is located >6.95 arcmin in radius, and warning if <4.5 arcmin.
     * 2) Warning if guidestar fainter than 12.5, error if fainter than 14.
     *
     * We won't implement number 2 until brightness could be specified in a more precise way.
     * Currently the brightness is an arbitrary string. See SCT-261 for details
     */
    private static IRule GUIDE_STARS_RULE = new IRule() {

//        private static final String FAINTER_WARN = "%s guidestar is fainter than 12.5";
//        private static final String FAINTER_ERROR = "%s guidestar is fainter than 14.0";
        private static final double MIN_DISTANCE = 4.5;
        private final double MAX_DISTANCE = PwfsGuideProbe.PWFS_RADIUS.toArcmins().getMagnitude();
        private final String FAR_MESSAGE = "%s guidestar is farther than " + MAX_DISTANCE + " arcmin in radius from the base position";
        private static final String CLOSE_MESSAGE = "%s guidestar is closer than " + MIN_DISTANCE + " arcmin in radius from the base position";

        public IP2Problems check(ObservationElements elements)  {

            IP2Problems problems = new P2Problems();
            for (TargetObsComp targetObsComp : elements.getTargetObsComp()) {

                TargetEnvironment env = targetObsComp.getTargetEnvironment();

              // We only consider the first science target because a multi-target asterism is
              // a configuration error that will raise a further P2 warning.
              SPTarget baseTarget = env.getAsterism().allSpTargets().head();

              for (GuideProbeTargets guideTargets : env.getPrimaryGuideGroup()) {
                    for (SPTarget target : guideTargets) {

                        // Calculate the distance to the base position in arcmin
                        final Option<Long> when = elements.getSchedulingBlock().map(b -> b.start());

                        Option<WorldCoords> oBasePos = _getWorldCoords(baseTarget, when);
                        Option<WorldCoords> oPos = _getWorldCoords(target, when);

                        oBasePos.foreach(basePos ->
                            oPos.foreach(pos -> {
                                double dist = Math.abs(basePos.dist(pos));

                                if (dist > MAX_DISTANCE) {
                                    problems.addError(PREFIX + "FAR_MESSAGE", String.format(FAR_MESSAGE, ""), elements.getTargetObsComponentNode().getValue());
                                }

                                if (dist < MIN_DISTANCE) {
                                    problems.addWarning(PREFIX + "CLOSE_MESSAGE", String.format(CLOSE_MESSAGE, ""), elements.getTargetObsComponentNode().getValue());
                                }
                            }
                        ));

                    }
                }
            }
            return problems;  //To change body of implemented methods use File | Settings | File Templates.
        }

        // Return the world coordinates for the give target
        private Option<WorldCoords> _getWorldCoords(SPTarget tp, Option<Long> time) {
            return tp.getRaDegrees(time).flatMap( ra ->
                   tp.getDecDegrees(time).map(dec ->
                     new WorldCoords(ra, dec, 2000.)));
        }
    };


    /**
     * Register all the GMOS rules to apply
     */
    static {
//        TRECS_RULES.add(SequenceRule.DUMP_CONFIG_RULE);
        TRECS_RULES.add(WindowRule.INSTANCE);
        TRECS_RULES.add(DISPERSER_NO_SLIT_RULE);
        TRECS_RULES.add(CHOP_THROW_RULE);
        TRECS_RULES.add(SpectroscopyFiltersRule.INSTANCE);
        TRECS_RULES.add(ENGINEERING_FILTERS_RULE);
        TRECS_RULES.add(DISPERSER_WRONG_FILTER_RULE);
        TRECS_RULES.add(SPECTROSCOPY_WRONG_BAND_RULE);

    }

    public IP2Problems check(ObservationElements elements)  {
        IP2Problems problems = new SequenceRule(TRECS_RULES, null).check(elements);
        problems.append(CC_RULE.check(elements));
        problems.append(SKY_BG_RULE.check(elements));
        problems.append(GUIDE_STARS_RULE.check(elements));
        return problems;
    }
}
