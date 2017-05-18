package edu.gemini.p2checker.rules.general;

import edu.gemini.p2checker.api.IP2Problems;
import edu.gemini.p2checker.api.IRule;
import edu.gemini.p2checker.api.ObservationElements;
import edu.gemini.p2checker.api.P2Problems;
import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.core.ProperMotion;
import edu.gemini.spModel.core.SiderealTarget;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.core.Target;
import edu.gemini.spModel.gemini.altair.AltairAowfsGuider;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffsetBase;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.Asterism;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.GuideSequence;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.template.TemplateParameters;
import edu.gemini.spModel.too.Too;
import edu.gemini.spModel.too.TooType;


import java.util.*;
import java.util.stream.Collectors;

/**
 * General Rules are rules that are not instrument-specific
 */
public class GeneralRule implements IRule {

    private static final String PREFIX = "GeneralRule_";
    private static List<IRule> GENERAL_RULES = new ArrayList<IRule>();
    public static final ItemKey OBSTYPE_KEY = new ItemKey("observe:observeType");
    public static final ItemKey OBSCLASS_KEY = new ItemKey("observe:class");
    public static final ItemKey OBSLABEL_KEY = new ItemKey("observe:dataLabel");

    private static final GeneralRuleHelper helper = new GeneralRuleHelper();

    public IP2Problems check(final ObservationElements elements)  {
        final P2Problems problems = new P2Problems();
        GENERAL_RULES.stream().forEach(rule -> problems.append(rule.check(elements)));
        return problems;
    }

    // Currently Altair is only supported on NIRI, GNIRS and NIFS, and GMOS-N
    private static boolean isAltairSupported(final ObservationElements elements){
        final SPInstObsComp instObsComp = elements.getInstrument();
        if (instObsComp == null) return false;
        final SPComponentType type = instObsComp.getType();
        return (type.equals(InstNIRI.SP_TYPE) || type.equals(InstNIFS.SP_TYPE)
            || type.equals(InstGNIRS.SP_TYPE) || type.equals(SPComponentType.INSTRUMENT_GMOS));
    }

    /**
     * AO general related rule. See REL-386.
     */
    private static IRule AO_RULE = new IRule() {
        private static final String MESSAGE = "Altair is currently only commissioned for use with NIRI, NIFS, GNIRS and GMOS-N";

        public IP2Problems check(final ObservationElements elements)  {
            if (!elements.hasAltair()){
                return null;
            }

            if (!isAltairSupported(elements)) {
                final P2Problems problems = new P2Problems();
                problems.addError(PREFIX + "AO_RULE", MESSAGE, elements.getAOComponentNode().getValue());
                return problems;
            }
            return null;
        }
    };


    /**
     * Rules involving WFS guide star
     */
    private static IRule WFS_RULE = new IRule() {
        private static final String DISABLED_GUIDER = "%s is not available in this observation.";

        private static final String WFS_EMPTY_NAME_TEMPLATE = "%s name field should not be empty";
        private static final String P2_PREFERRED = "PWFS2 is the preferred peripheral wavefront sensor.";
        private static final String NO_AO_OTHER = "Altair cannot use %s and %s.";
        private static final String NGS_AOWFS = "Altair NGS must have an AOWFS target.";
        private static final String LGS_WFS = "Altair %s must have %s tip-tilt star.";
        private static final String LGS_OI_NO_GMOS = "Altair LGS + OI mode requires GMOS-N.";
        private static final String COORD_CLASH_MESSAGE =
            "Objects with the same name must have the same coordinates.";
        private static final String NAME_CLASH_MESSAGE =
                "Objects with the same coordinates must have the same name.";
        private static final String TAG_CLASH_MESSAGE =
                "Objects with the same name must have the same type and coordinates.";

        private String formatGuiderString(final Collection<GuideProbe> probes) {
            final String startDelim = probes.size() >= 2 ? "[" : "";
            final String endDelim   = probes.size() >= 2 ? "]" : "";
            final String sep        = ",";
            final List<String> probeKeys = probes.stream().map(GuideProbe::getKey).collect(Collectors.toList());
            return startDelim + String.join(sep, probeKeys) + endDelim;
        }

        private void reportAltairLgsGuideIssues(final P2Problems problems, final Set<GuideProbe> usedGuiders,
                                                final AltairParams.Mode mode, final ISPObsComponent targetComp) {
            // The "bad guiders", i.e. the used guiders that cannot be used with the mode.
            final Set<GuideProbe> badGuiders = new TreeSet<>(GuideProbe.KeyComparator.instance);
            badGuiders.addAll(usedGuiders);
            mode.guiders().foreach(badGuiders::remove);

            // We must be using the TTF guider, which is the head of the mode guider list.
            final GuideProbe ttfGuider = mode.guiders().head();
            if (!usedGuiders.contains(ttfGuider)) {
                problems.addError(PREFIX + "LGS_WFS", String.format(LGS_WFS, mode.displayValue(), ttfGuider), targetComp);
            } else if (!badGuiders.isEmpty()) {
                // The good guiders, i.e. the ones that can be used with the mode and are in use.
                final Set<GuideProbe> goodGuiders = new TreeSet<>(GuideProbe.KeyComparator.instance);
                usedGuiders.stream().filter(mode.guiders()::contains).forEach(goodGuiders::add);

                // Format the guider names
                final String goodGuiderNames = formatGuiderString(goodGuiders);
                final String badGuiderNames  = formatGuiderString(badGuiders);
                problems.addError(PREFIX+"NO_AO_OTHER", String.format(NO_AO_OTHER, goodGuiderNames, badGuiderNames), targetComp);
            }
        }

        public IP2Problems check(final ObservationElements elements)  {
            if (elements.getTargetObsComp().isEmpty()) return null; //can't check

            final TargetEnvironment env = elements.getTargetObsComp().getValue().getTargetEnvironment();

            final P2Problems problems = new P2Problems();

            final boolean hasAltairComp = elements.hasAltair();
            boolean isLgs = false;

            // Don't do Altair checks if Altair is not supported
            final boolean altairSupported = isAltairSupported(elements);

            if (hasAltairComp && altairSupported) {
                final ISPObsComponent targetComp = elements.getTargetObsComponentNode().getValue();
                final GuideGroup guideGroup      = env.getGuideEnvironment().getPrimary();
                final Set<GuideProbe> guiders    = guideGroup.getReferencedGuiders();
                final InstAltair altair          = (InstAltair) elements.getAOComponent().getValue();
                final AltairParams.Mode mode     = altair.getMode();
                isLgs = mode.guideStarType() == AltairParams.GuideStarType.LGS;
                switch (mode) {
                    case NGS:
                    case NGS_FL:
                        if (!guiders.contains(AltairAowfsGuider.instance)) {
                            problems.addError(PREFIX+"NGS_AOWFS", NGS_AOWFS, targetComp);
                        }
                        break;
                    case LGS:
                    case LGS_P1:
                        reportAltairLgsGuideIssues(problems, guiders, mode, targetComp);
                        break;
                    case LGS_OI:
                        reportAltairLgsGuideIssues(problems, guiders, mode, targetComp);
                        final SPInstObsComp inst = elements.getInstrument();
                        if ((inst == null) || inst.getType() != SPComponentType.INSTRUMENT_GMOS) {
                            problems.addError(PREFIX+"LSG_OI_NO_GMOS", LGS_OI_NO_GMOS, elements.getAOComponentNode().getValue());
                        }
                        break;
                }
            }

            for (final GuideProbeTargets guideTargets : env.getPrimaryGuideGroup()) {
                final GuideProbe guider = guideTargets.getGuider();

                final boolean dis = elements.getObsContext().exists(c -> !GuideProbeUtil.instance.isAvailable(c, guider) &&
                                                                         (guideTargets.getTargets().size() > 0));
                if (dis) {
                    problems.addError(PREFIX+"DISABLED_GUIDER", String.format(DISABLED_GUIDER, guider.getKey()),
                            elements.getTargetObsComponentNode().getValue());
                    continue;
                }

                if (guider == PwfsGuideProbe.pwfs1) {
                    if (guideTargets.getTargets().size() > 0) {
                        final SPInstObsComp instrument = elements.getInstrument();
                        if (instrument!=null && instrument.getSite().contains(Site.GN) && !isLgs) {
                            problems.addWarning(PREFIX+"P2_PREFERRED", P2_PREFERRED,elements.getTargetObsComponentNode().getValue());
                        }
                    }
                    continue;
                }

                final Set<String> errorSet = new TreeSet<>();
                for (final SPTarget target : guideTargets) {
                    //Check for empty name
                    if (target.getName() == null || target.getName().trim().isEmpty()) {
                        errorSet.add(String.format(WFS_EMPTY_NAME_TEMPLATE, guider.getKey()));
                    }

                    // If a WFS has the same name as a science position, make sure the have the same
                    // type (i.e., tag) and position; or, if they have the same tag and position,
                    // make sure they have the same name.
                    for (final SPTarget base : env.getAsterism().allSpTargetsJava()) {
                      final Target t1 = base.getTarget();
                      final Target t2 = target.getTarget();

                      final boolean sameName = t1.name().equals(t2.name());
                      final boolean sameTag  = t1.getClass().getName().equals(t2.getClass().getName());

                      if (sameName) {
                          if (sameTag) {
                              if (!helper.samePosition(t1, t2)) {
                                  // same name and tag, but positions don't match
                                  errorSet.add(COORD_CLASH_MESSAGE);
                              }
                          } else {
                              // same name but different tags
                              errorSet.add(TAG_CLASH_MESSAGE);
                          }
                      } else if (sameTag && helper.samePosition(t1, t2)) {
                          // same tag and position, but different name
                          errorSet.add(NAME_CLASH_MESSAGE);
                      }
                    }

                }
                errorSet.stream().forEach(error -> {
                    final String id = error.equals(WFS_EMPTY_NAME_TEMPLATE) ? "WFS_EMPTY_NAME_TEMPLATE" :
                            (error.equals(NAME_CLASH_MESSAGE) ? "NAME_CLASH_MESSAGE" : "COORD_CLASH_MESSAGE");
                    problems.addError(PREFIX+id, error, elements.getTargetObsComponentNode().getValue());
                });
            }

            return problems;
        }
    };

    private static IRule EXTRA_ADVANCED_GUIDERS_RULE = new IRule() {
        private static final String EXTRA_ADVANCED_GUIDER_MSG = "Advanced guiding is configured for %s, but it hasn't been assigned a guide star.";

        @Override public IP2Problems check(final ObservationElements elements)  {
            if (elements.getSeqComponentNode() == null) return null;
            if (elements.getTargetObsComp().isEmpty()) return null;

            // Get the set of guiders which have been assigned guide stars.
            final Option<TargetEnvironment> env = elements.getTargetObsComp().map(TargetObsComp::getTargetEnvironment);
            final ImList<GuideProbe> guiders = GuideSequence.getRequiredGuiders(env);
            final Set<GuideProbe> guiderSet = new HashSet<>();
            guiders.foreach(guiderSet::add);

            // Get all the offset position lists in the sequence.
            final List<OffsetPosList<?>> posListList = getPosLists(elements.getSeqComponentNode());

            // If any position list has an advanced guider that doesn't have
            // an assigned guide star, remember it in the "extraSet".
            final Set<GuideProbe> extraSet = new TreeSet<>(GuideProbe.KeyComparator.instance);
            for (final OffsetPosList<?> posList : posListList) {
                posList.getAdvancedGuiding().stream().filter(adv -> !guiderSet.contains(adv)).forEach(extraSet::add);
            }

            // Generate an error for each extra guider.
            final P2Problems problems = new P2Problems();
            extraSet.stream().forEach(guider -> {
                final String message = String.format(EXTRA_ADVANCED_GUIDER_MSG, guider.getKey());
                problems.addError(PREFIX+"EXTRA_ADV_GUIDING", message, elements.getSeqComponentNode());
            });
            return problems;
        }

        private List<OffsetPosList<?>> getPosLists(final ISPSeqComponent root)  {
            final List<OffsetPosList<?>> res = new ArrayList<>();
            addOffsetPosLists(root, res);
            return res;
        }

        private void addOffsetPosLists(final ISPSeqComponent root, final List<OffsetPosList<?>> lst)  {
            final Object dataObj = root.getDataObject();
            if (dataObj instanceof SeqRepeatOffsetBase) {
                lst.add(((SeqRepeatOffsetBase) dataObj).getPosList());
            }
            root.getSeqComponents().stream().forEach(child -> addOffsetPosLists(child, lst));
        }
    };


    /**
     * Rule for the science target
     */
    private static IRule SCIENCE_TARGET_RULE = new IRule() {

        private static final String EMPTY_TARGET_NAME_MSG = "TARGET name field should not be empty";
        private static final String NO_SCIENCE_TARGET_MSG = "No science target was found";

        public IP2Problems check(final ObservationElements elements)  {
            if (elements.getTargetObsComp().isEmpty()) return null; // can't perform this check without a target environment

            final P2Problems problems = new P2Problems();

            if (!hasScienceObserves(elements.getSequence())) return null; //if there are not observes, ignore this check (SCT-260)

            for (SPTarget t: elements.getTargetObsComp().getValue().getAsterism().allSpTargetsJava()) {
              if ("".equals(t.getName().trim())) {
                  problems.addError(PREFIX+"EMPTY_TARGET_NAME_MSG", EMPTY_TARGET_NAME_MSG, elements.getTargetObsComponentNode().getValue());
              }
            }

            return problems;

        }
    };

    /**
     * Returns true if a sequence contains at least one science observe type.
     */
    private static boolean hasScienceObserves(final ConfigSequence sequence) {
        final Object[] objs = sequence.getDistinctItemValues(OBSTYPE_KEY);
        for (final Object o: objs)
            if (InstConstants.SCIENCE_OBSERVE_TYPE.equals(o))
                return true;
        return false;
    }


    /**
     * WARN  if TARGET_PROPER_MOTION > 1000.0
     */
    private static IRule TARGET_PM_RULE = new IRule() {
        private static final String MESSAGE = "Very large proper motion. Please double check your proper motion";
        private static final double MAX_PM = 1000.0; //Max PM is 1000 milli-arcsecs. W
        public IP2Problems check(final ObservationElements elements)  {
            if (elements.getTargetObsComp().isEmpty()) return null; // can't perform this check without a target environment

            final P2Problems problems = new P2Problems();
            for (SPTarget spt: elements.getTargetObsComp().getValue().getAsterism().allSpTargetsJava()) {
              final scala.Option<ProperMotion> opm = spt.getProperMotion();
              if (opm.isDefined()) {
                  final ProperMotion pm = opm.get();

                  final double pm_ra = pm.deltaRA().velocity();
                  final double pm_dec = pm.deltaDec().velocity();
                  final double total = pm_ra * pm_ra + pm_dec * pm_dec;

                  if (total > MAX_PM * MAX_PM) { //to avoid sqrt call
                      problems.addWarning(PREFIX + "TARGET_PM_RULE", MESSAGE, elements.getTargetObsComponentNode().getValue());
                  }
              }
            }
            return problems;
          }
      };

    // targets are equal only if positions are defined and within _getMinDistance
    private static boolean _areTargetsEquals(final SPTarget p1Target, final SPTarget target, final ObservationElements elems) {

        final Option<Long> when = elems.getSchedulingBlockStart();

        final Option<Double> spRA = target.getRaHours(when);
        final Option<Double> spDec = target.getDecDegrees(when);

        final Option<Double> p1RA = p1Target.getRaHours(when);
        final Option<Double> p1Dec = p1Target.getDecDegrees(when);

        double minDistance = _getMinDistance(elems);

        return _close(spRA,  p1RA,  minDistance) &&
               _close(spDec, p1Dec, minDistance);

    }

    // Consider it a match if the real observation is a ToO observation and the
    // P1Target is at (0,0).  This will likely match triplets in the template
    // that aren't really associated with the generated observation but it won't
    // miss any.
    private static boolean _isTooTarget(final SPTarget p1Target, final ObservationElements elems) {
        final ISPObservation obs = elems.getObservationNode();
        if (obs == null || !Too.isToo(obs)) return false;
        return p1Target.isTooTarget();
    }

    // true if both defined and diff <= tolerance
    private static boolean _close(final Option<Double> a, final Option<Double> b, final double tolerance) {
        return a.exists(a0 -> b.exists(b0 -> Math.abs(a0 - b0) <= tolerance));
    }


    private static double _getMinDistance(final ObservationElements elems) {
        final SPInstObsComp inst = elems.getInstrument();
        final double[] scienceArea = inst.getScienceArea();

        final double minDistance = scienceArea[1] / 2.0;
        return Math.max(minDistance, 10.0) / 3600;
    }


    /**
     * Rule to check that If Band=3 then Band 3 minimum time != 0
     */
    private static IRule BAND3TIME_RULE = new IRule() {

        private static final String ZERO_MESSAGE = "The Band 3 minimum time must not be 0";
        private static final String ALLOC_MESSAGE = "The Band 3 minimum time must be less than or equal to the allocated time";

        public IP2Problems check(final ObservationElements elements) {
            try {
                final P2Problems probs = new P2Problems();
                if (elements.getProgram().getQueueBand().equals("3")) {
                    final double t = elements.getProgram().getMinimumTime().getTimeAmount();
                    if (t == 0.0) {
                        probs.addError(PREFIX+"BAND3TIME_RULE_ZERO_MESSAGE", ZERO_MESSAGE, elements.getProgramNode()); // REL-337
                    }
                    if (t > elements.getProgram().getAwardedProgramTime().getTimeAmount()) {
                        probs.addError(PREFIX+"BAND3TIME_RULE_ALLOC_MESSAGE", ALLOC_MESSAGE, elements.getProgramNode()); // REL-336
                    }
                    if (probs.getProblemCount() != 0) {
                        return probs;
                    }
                }
            } catch (NullPointerException ex) {
            }
            return null;
        }
    };

    /**
     * Rule involving missing timing windows for ready TOO observations.
     */
    private static final IRule TOO_TIMING_WINDOW_RULE = new IRule() {
        private static final String TEMPLATE =
                "A default timing window of %s (starting when the TOO is triggered) " +
                "will be applied if none is specified.";

        public IP2Problems check(final ObservationElements elements)  {
            if (elements == null) return null;

            // Make sure this is a TOO observation.
            final ISPObservation obs = elements.getObservationNode();
            final TooType too = (obs == null) ? TooType.none : Too.get(obs);
            // SCI-0162: Only warn for rapid TOO observations.
            if (too != TooType.rapid) return null;

            // Make sure it is ready.
            if (ObservationStatus.computeFor(elements.getObservationNode()) != ObservationStatus.READY) {
                return null;
            }

            // Make sure there are no timing windows.
            final Option<SPSiteQuality> sqOpt = elements.getSiteQuality();
            if (!sqOpt.isEmpty()) {
                final List<SPSiteQuality.TimingWindow> windows = sqOpt.getValue().getTimingWindows();
                if ((windows != null) && (windows.size() > 0)) return null;
            }

            if (elements.getSiteQualityNode().isEmpty()) {
                // Cannot add a problem directly to the observation node, so return null.
                return null;
            }

            // Now figure out how long the default window will be.
            final String timePeriod = too.getDurationDisplayString();
            final String message = String.format(TEMPLATE, timePeriod);

            final P2Problems probs = new P2Problems();
            probs.addWarning(PREFIX+"TOO_TIMING_WINDOW_RULE", message,  elements.getSiteQualityNode().getValue());
            return probs;
        }
    };

    /**
     * Rule checking there are no daytime calibrations that are mixed into nighttime operations.
     * See UX-645.
     */
    private static final IRule NO_DAYTIME_CALS_AT_NIGHT_RULE = new IRule() {
        private static final String MESSAGE =
                "Day time calibrations in step %s must not be mixed with night time operations.";

        public IP2Problems check(final ObservationElements elements)  {
            if (elements == null) return null;

            // Get overall obs class
            final ObsClass overall = ObsClassService.lookupObsClass(elements.getObservationNode());
            if (overall == ObsClass.DAY_CAL) return null; // no problem by definition; anything goes during the day

            // Now if any step has class DAY_CAL then we have a problem.
            final P2Problems probs = new P2Problems();
            for (final Config config : elements.getSequence().getAllSteps()) {
                if (ObsClass.DAY_CAL.sequenceValue().equals(config.getItemValue(OBSCLASS_KEY))) {
                    probs.addError(PREFIX+"NO_DAYTIME_CALS_AT_NIGHT_RULE", String.format(MESSAGE, (String)config.getItemValue(OBSLABEL_KEY)), elements.getSeqComponentNode());
                }
            }

            return probs.getProblemCount() > 0 ? probs : null;
        }
    };

    private static final IRule TEMPLATE_RULE = new IRule() {

        private static final String BOGUS_TARGET_MESSAGE =
                "The target position does not match the Phase 1 coordinates. " +
                "Sometimes offsets may be needed to reach a guide star. A new target " +
                "must be approved by the local Gemini Head of Science Operations";

        private static final String BOGUS_CONDS_MESSAGE = "Conditions have to be the same (or worse) than defined in the phase 1 proposal.";

        public IP2Problems check(final ObservationElements elements)  {
            final P2Problems ps = new P2Problems();
            final ISPTemplateFolder templateFolderNode = elements.getProgramNode().getTemplateFolder();
            if (templateFolderNode != null) {

                final SPTarget obsTarget = getObsTarget(elements);
                if (obsTarget != null) {

                    // We're going to drive this off the target, so we first want to narrow it
                    // down to the TemplateParameters that correspond with this target.
                    final List<TemplateParameters> matchingParams = new ArrayList<>();
                    TemplateParameters.foreach(templateFolderNode, tp -> {
                        final SPTarget p1Target = tp.getTarget();
                        if (_areTargetsEquals(p1Target, obsTarget, elements) || _isTooTarget(p1Target, elements)) {
                            matchingParams.add(tp);
                        }
                    });

                    // If there are none, the target is bogus
                    if (matchingParams.isEmpty()) {
                        // REL-1113
                        ps.addError(PREFIX + "TEMPLATE_RULE", BOGUS_TARGET_MESSAGE, elements.getTargetObsComponentNode().getOrNull());
                        // UX-1583. Since there was no matching p1 target, we
                        // don't know what conditions to check so skip the
                        // test for bogus conditions.
                    } else {
                        // Now see if we can't find a match for conditions
                        outer:
                        for (; ; ) {
                            for (final TemplateParameters t : matchingParams) {
                                final SPSiteQuality expected = t.getSiteQuality();
                                final SPSiteQuality actual = elements.getSiteQuality().getOrNull();
                                if (isAcceptable(expected, actual))
                                    break outer;
                            }
                            ps.addError(PREFIX + "TEMPLATE_RULE", BOGUS_CONDS_MESSAGE, elements.getSiteQualityNode().getOrNull());
                            break;
                        }
                    }

                }
            }
            return ps;
        }

        private SPTarget getObsTarget(final ObservationElements elements)  {
            final TargetObsComp targetEnv = elements.getTargetObsComp().getOrNull();
            if (targetEnv != null) {
                final ObsClass obsClass = ObsClassService.lookupObsClass(elements.getObservationNode());
                if (obsClass == ObsClass.SCIENCE) {
                    // TODO:ASTERISM: this needs to handle multiple targets … also why only sidereal?
                    final SPTarget target = targetEnv.getArbitraryTargetFromAsterism();
                    if (target.isSidereal())
                        return target;
                }
            }
            return null;
        }

        private boolean isAcceptable(final SPSiteQuality expected, final SPSiteQuality actual) {
            if (actual == null || expected == null) return true;
            return expected.getCloudCover().getPercentage() <= actual.getCloudCover().getPercentage() &&
                    expected.getImageQuality().getPercentage() <= actual.getImageQuality().getPercentage() &&
                    expected.getSkyBackground().getPercentage() <= actual.getSkyBackground().getPercentage() &&
                    expected.getWaterVapor().getPercentage() <= actual.getWaterVapor().getPercentage();
        }


    };


    /**
     * Register all the general rules
     */
    static  {
        GENERAL_RULES.add(SCIENCE_TARGET_RULE);
        GENERAL_RULES.add(TARGET_PM_RULE);
        GENERAL_RULES.add(WFS_RULE);
        GENERAL_RULES.add(EXTRA_ADVANCED_GUIDERS_RULE);
        GENERAL_RULES.add(AO_RULE);
        GENERAL_RULES.add(TOO_TIMING_WINDOW_RULE);
        GENERAL_RULES.add(BAND3TIME_RULE);
        GENERAL_RULES.add(NO_DAYTIME_CALS_AT_NIGHT_RULE);
        GENERAL_RULES.add(TEMPLATE_RULE);
    }

}
