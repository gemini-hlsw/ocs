//
//$Id: GeneralRule.java 46768 2012-07-16 18:58:53Z rnorris $
//
package edu.gemini.p2checker.rules.general;

import edu.gemini.p2checker.api.IP2Problems;
import edu.gemini.p2checker.api.IRule;
import edu.gemini.p2checker.api.ObservationElements;
import edu.gemini.p2checker.api.P2Problems;
import edu.gemini.pot.sp.*;
import edu.gemini.shared.util.immutable.ApplyOp;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.MapOp;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.altair.AltairAowfsGuider;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffsetBase;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obs.ObservationStatus;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.GuideSequence;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.target.system.CoordinateParam;
import edu.gemini.spModel.target.system.ITarget;
import edu.gemini.spModel.target.system.NonSiderealTarget;
import edu.gemini.spModel.template.TemplateParameters;
import edu.gemini.spModel.too.Too;
import edu.gemini.spModel.too.TooType;


import java.util.*;

/**
 * General Rules are rules that are not instrument-specific
 */
public class GeneralRule implements IRule {

    private static final String PREFIX = "GeneralRule_";
    private static List<IRule> GENERAL_RULES = new ArrayList<IRule>();
    public static final ItemKey OBSTYPE_KEY = new ItemKey("observe:observeType");
    public static final ItemKey OBSCLASS_KEY = new ItemKey("observe:class");
    public static final ItemKey OBSLABEL_KEY = new ItemKey("observe:dataLabel");

    public IP2Problems check(ObservationElements elements)  {
        P2Problems problems = new P2Problems();
        for (IRule rule : GENERAL_RULES) {
            problems.append(rule.check(elements));
        }
        return problems;
    }

    // Currently Altair is only supported on NIRI, GNIRS and NIFS, and GMOS-N
    private static boolean isAltairSupported(ObservationElements elements){
        SPInstObsComp instObsComp = elements.getInstrument();
        if (instObsComp == null) return false;
        SPComponentType type = instObsComp.getType();
        return (type.equals(InstNIRI.SP_TYPE) || type.equals(InstNIFS.SP_TYPE)
            || type.equals(InstGNIRS.SP_TYPE) || type.equals(SPComponentType.INSTRUMENT_GMOS));
    }

    /**
     * AO general related rule. See REL-386.
     */
    private static IRule AO_RULE = new IRule() {
        private static final String MESSAGE = "Altair is currently only commissioned for use with NIRI, NIFS, GNIRS and GMOS-N";

        public IP2Problems check(ObservationElements elements)  {
            if (!elements.hasAltair()){
                return null;
            }

            if (!isAltairSupported(elements)) {
                P2Problems problems = new P2Problems();
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
        private static final String NO_AO_OTHER = "Altair cannot use both %s and %s.";
        private static final String NGS_AOWFS = "Altair NGS must have an AOWFS target.";
        private static final String LGS_WFS = "Altair %s must have %s tip-tilt star.";

        private static final String LGS_OI_NO_GMOS = "Altair LGS + OI mode requires GMOS-N.";

        // SCT-346: changes the checking and message below to make it more general
        // Now applies to any WFS
//        private static final String AOWFS_MESSAGE = "Please make sure that the AOWFS coordinates, proper motions, and " +
//                "tracking details are exactly the same as those of the target";
        private static final String NAME_CLASH_MESSAGE =
            "Objects with the same name must have the same coordinates.";
        private static final String COORD_CLASH_MESSAGE =
            "Objects with the same coordinates must have the same name.";

        private static final String WFS_DEFINED_MESSAGE = "WFS should be defined";
        //TODO: Rule to check if the WFS is in the patrol field, whatever that means
        //private static final String WFS_OUT_PATROL_FIELD_TEMPLATE = "%s outside of the patrol field";

        private void reportAltairLgsGuideIssues(P2Problems problems, Set<GuideProbe> guiders, AltairParams.Mode mode, ISPObsComponent targetComp) {
            final GuideProbe guider = mode.guider();
            if (!guiders.contains(guider)) {
                problems.addError(PREFIX+"LGS_WFS", String.format(LGS_WFS, mode.displayValue(), guider), targetComp);
            } else if (guiders.size() > 1) {
                final Set<GuideProbe> otherGuiders = new TreeSet<GuideProbe>(GuideProbe.KeyComparator.instance);
                otherGuiders.addAll(guiders);
                otherGuiders.remove(guider);

                // Format the guider names
                final StringBuilder buf = new StringBuilder();
                final Iterator<GuideProbe> it = otherGuiders.iterator();
                buf.append(it.next().getKey());
                while (it.hasNext()) {
                    buf.append(", ").append(it.next().getKey());
                }

                final String msg = String.format(NO_AO_OTHER, guider.getKey(), buf.toString());
                problems.addError(PREFIX+"NO_AO_OTHER", msg, targetComp);
            }
        }

        public IP2Problems check(ObservationElements elements)  {
            if (elements.getTargetObsComp().isEmpty()) return null; //can't check

            TargetEnvironment env = elements.getTargetObsComp().getValue().getTargetEnvironment();

            P2Problems problems = new P2Problems();
            SPTarget baseTarget = env.getBase();

            final boolean hasAltairComp = elements.hasAltair();
            boolean isLgs = false;

            // Don't do Altair checks if Altair is not supported
            boolean altairSupported = isAltairSupported(elements);

            if (hasAltairComp && altairSupported) {
                final ISPObsComponent targetComp = elements.getTargetObsComponentNode().getValue();
                final GuideGroup guideGroup      = env.getGuideEnvironment().getPrimary().getOrNull();
                final Set<GuideProbe> guiders    = (guideGroup == null) ? Collections.<GuideProbe>emptySet() : guideGroup.getReferencedGuiders();
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

            for (GuideProbeTargets guideTargets : env.getOrCreatePrimaryGuideGroup()) {
                GuideProbe guider = guideTargets.getGuider();
                // TODO: GuideProbeTargets.isEnabled
                if (!env.isActive(guider) && (guideTargets.getOptions().size() > 0)) {
                    problems.addError(PREFIX+"DISABLED_GUIDER", String.format(DISABLED_GUIDER, guider.getKey()),
                            elements.getTargetObsComponentNode().getValue());
                    continue;
                }

                if (guider == PwfsGuideProbe.pwfs1) {
                    if (guideTargets.getOptions().size() > 0) {
                        SPInstObsComp instrument = elements.getInstrument();
                        if(instrument!=null && instrument.getSite().contains(Site.GN) && !isLgs){
                            problems.addWarning(PREFIX+"P2_PREFERRED", P2_PREFERRED,elements.getTargetObsComponentNode().getValue());
                        }
                    }
                    continue;
                }

                Set<String> errorSet = new TreeSet<String>();
                for (SPTarget target : guideTargets) {
                    //Check for empty name
                    if ("".equals(target.getTarget().getName().trim())) {
                        errorSet.add(String.format(WFS_EMPTY_NAME_TEMPLATE, guider.getKey()));
                    }

                    // If a WFS has the same name as the base position, make sure
                    // that it has the same position.  If they have the same
                    // position, make sure they have the same name.
                    boolean sameName = baseTarget.getTarget().getName().equals(target.getTarget().getName());
                    boolean samePos;
                    if (baseTarget.getTarget() instanceof NonSiderealTarget) {
                        samePos = sameNonSiderealPosition(baseTarget, target);
                    } else {
                        samePos = samePosition(baseTarget, target);
                    }
                    if (sameName && !samePos) {
                        errorSet.add(NAME_CLASH_MESSAGE);
                    }
                    if (samePos && !sameName) {
                        errorSet.add(COORD_CLASH_MESSAGE);
                    }
                }
                for (String error : errorSet) {
                    String id = error.equals(WFS_EMPTY_NAME_TEMPLATE) ? "WFS_EMPTY_NAME_TEMPLATE" :
                            (error.equals(NAME_CLASH_MESSAGE) ? "NAME_CLASH_MESSAGE" : "COORD_CLASH_MESSAGE");
                    problems.addError(PREFIX+id, error, elements.getTargetObsComponentNode().getValue());
                }
            }

            return problems;
        }

        private boolean hasAltair(TargetEnvironment env) {
            return hasPrimary(env, AltairAowfsGuider.instance);
        }

        private boolean hasPrimary(TargetEnvironment env, GuideProbe guider) {
            // TODO: GuideProbeTargets.isEnabled
            if (!env.isActive(guider)) return false;
            Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
            return (!gtOpt.isEmpty()) && !gtOpt.getValue().getPrimary().isEmpty();
        }
        private boolean sameNonSiderealPosition(SPTarget base, SPTarget guide) {
            ITarget baseTarget = base.getTarget();
            ITarget guideTarget = guide.getTarget();
            return baseTarget.equals(guideTarget);
        }

        private boolean samePosition(SPTarget base, SPTarget guide) {
            return hasSameCoordinates(base, guide) &&
                   hasSameProperMotion(base, guide) &&
                   hasSameTrackingDetails(base, guide);
        }

        private boolean hasSameCoordinates(SPTarget base, SPTarget guide) {

            // Okay, this kind of sucks, but I want to compare the coordinates
            // in the same way they are externalized and displayed.  That is,
            // ignore any extra precision that we end up throwing away.
            String baseC1 = base.getTarget().getRa().toString();
            String baseC2 = base.getTarget().getDec().toString();

            String guideC1 = guide.getTarget().getRa().toString();
            String guideC2 = guide.getTarget().getDec().toString();

            return baseC1.equals(guideC1) && baseC2.equals(guideC2);
        }

        private boolean hasSameProperMotion(SPTarget base, SPTarget guide) {

            double pmDecBase = base.getPropMotionDec();
            double pmRaBase = base.getPropMotionRA();

            double pmDecGuide = guide.getPropMotionDec();
            double pmRaGuide = guide.getPropMotionRA();

            return pmRaBase == pmRaGuide && pmDecBase == pmDecGuide;
        }

        private boolean hasSameTrackingDetails(SPTarget base, SPTarget guide) {

            if (base.getTarget().getTag() != guide.getTarget().getTag()) return false;
            if (base.getTrackingEpoch() != guide.getTrackingEpoch()) return false;
            if (base.getTrackingParallax() !=  guide.getTrackingParallax()) return false;
            if (base.getTrackingRadialVelocity() != guide.getTrackingRadialVelocity()) return false;
            //everything is the same
            return true;
        }
    };

    private static IRule EXTRA_ADVANCED_GUIDERS_RULE = new IRule() {
        private static final String EXTRA_ADVANCED_GUIDER_MSG = "Advanced guiding is configured for %s, but it hasn't been assigned a guide star.";

        @Override public IP2Problems check(ObservationElements elements)  {
            if (elements.getSeqComponentNode() == null) return null;
            if (elements.getTargetObsComp().isEmpty()) return null;

            // Get the set of guiders which have been assigned guide stars.
            final Option<TargetEnvironment> env = elements.getTargetObsComp().map(new MapOp<TargetObsComp, TargetEnvironment>() {
                @Override public TargetEnvironment apply(TargetObsComp toc) {
                    return toc.getTargetEnvironment();
                }
            });
            final ImList<GuideProbe> guiders = GuideSequence.getRequiredGuiders(env);
            final Set<GuideProbe> guiderSet = new HashSet<GuideProbe>();
            for (GuideProbe gp : guiders) guiderSet.add(gp);

            // Get all the offset position lists in the sequence.
            final List<OffsetPosList> posListList = getPosLists(elements.getSeqComponentNode());

            // If any position list has an advanced guider that doesn't have
            // an assigned guide star, remember it in the "extraSet".
            final Set<GuideProbe> extraSet = new TreeSet<GuideProbe>(GuideProbe.KeyComparator.instance);
            for (OffsetPosList posList : posListList) {
                Set<GuideProbe> advancedSet = posList.getAdvancedGuiding();
                for (GuideProbe adv : advancedSet) {
                    if (!guiderSet.contains(adv)) extraSet.add(adv);
                }
            }

            // Generate an error for each extra guider.
            P2Problems problems = new P2Problems();
            for (GuideProbe guider : extraSet) {
                String message = String.format(EXTRA_ADVANCED_GUIDER_MSG, guider.getKey());
                problems.addError(PREFIX+"EXTRA_ADV_GUIDING", message, elements.getSeqComponentNode());
            }
            return problems;
        }

        private List<OffsetPosList> getPosLists(ISPSeqComponent root)  {
            List<OffsetPosList> res = new ArrayList<OffsetPosList>();
            addOffsetPosLists(root, res);
            return res;
        }

        private void addOffsetPosLists(ISPSeqComponent root, List<OffsetPosList> lst)  {
            final Object dataObj = root.getDataObject();
            if (dataObj instanceof SeqRepeatOffsetBase) {
                lst.add(((SeqRepeatOffsetBase) dataObj).getPosList());
            }
            for (ISPSeqComponent child : root.getSeqComponents()) {
                addOffsetPosLists(child, lst);
            }
        }
    };


    /**
     * Rule for the science target
     */
    private static IRule SCIENCE_TARGET_RULE = new IRule() {

        private static final String EMPTY_TARGET_NAME_MSG = "TARGET name field should not be empty";
        private static final String NO_SCIENCE_TARGET_MSG = "No science target was found";

        public IP2Problems check(ObservationElements elements)  {

            //TargetObsComp targetEnv = elements.getTargetObsComp();
            if (elements.getTargetObsComp().isEmpty()) return null; // can't perform this check without a target environment

            P2Problems problems = new P2Problems();
            SPTarget scienceTarget = elements.getTargetObsComp().getValue().getBase();

            if (!hasScienceObserves(elements.getSequence())) return null; //if there are not observes, ignore this check (SCT-260)

            if (scienceTarget == null) { //really unlikely
                problems.addError(PREFIX+"NO_SCIENCE_TARGET_MSG", NO_SCIENCE_TARGET_MSG, elements.getTargetObsComponentNode().getValue());
            } else {
                if ("".equals(scienceTarget.getTarget().getName().trim())) {
                    problems.addError(PREFIX+"EMPTY_TARGET_NAME_MSG", EMPTY_TARGET_NAME_MSG, elements.getTargetObsComponentNode().getValue());
                }
            }
            return problems;

        }
    };

    /**
     * Returns true if a sequence contains at least one science observe type.
     */
    public static boolean hasScienceObserves(ConfigSequence sequence) {
        Object[] objs = sequence.getDistinctItemValues(OBSTYPE_KEY);
        for (Object o: objs)
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
        public IP2Problems check(ObservationElements elements)  {
            if (elements.getTargetObsComp().isEmpty()) return null; // can't perform this check without a target environment

            SPTarget baseTarget = elements.getTargetObsComp().getValue().getBase();

            if (baseTarget == null) return null; //can't check;

            double pm_ra  = baseTarget.getPropMotionRA();
            double pm_dec = baseTarget.getPropMotionDec();
            double total = pm_ra * pm_ra + pm_dec * pm_dec;

            if (total > MAX_PM * MAX_PM) { //to avoid sqrt call
                P2Problems problems = new P2Problems();
                problems.addWarning(PREFIX+"TARGET_PM_RULE", MESSAGE, elements.getTargetObsComponentNode().getValue());
                return problems;
            }

            return null;
        }
    };

    private static boolean _areTargetsEquals(SPTarget p1Target, SPTarget target, ObservationElements elems) {

        double spRA = target.getTarget().getRa().getAs(CoordinateParam.Units.HMS);
        double spDec = target.getTarget().getDec().getAs(CoordinateParam.Units.DEGREES);

        double p1RA = p1Target.getTarget().getRa().getAs(CoordinateParam.Units.HMS);
        double p1Dec = p1Target.getTarget().getDec().getAs(CoordinateParam.Units.DEGREES);

        return _closeEnough(elems, spRA, spDec, p1RA, p1Dec);
    }

    // Consider it a match if the real observation is a ToO observation and the
    // P1Target is at (0,0).  This will likely match triplets in the template
    // that aren't really associated with the generated observation but it won't
    // miss any.
    private static boolean _isTooTarget(SPTarget p1Target, ObservationElements elems) {
        final ISPObservation obs = elems.getObservationNode();
        if (obs == null || !Too.isToo(obs)) return false;

        double p1RA = p1Target.getTarget().getRa().getAs(CoordinateParam.Units.HMS);
        double p1Dec = p1Target.getTarget().getDec().getAs(CoordinateParam.Units.DEGREES);
        return (p1RA == 0.0) && (p1Dec == 0.0);
    }

    private static boolean _closeEnough(ObservationElements elems, double spRA, double spDec, double p1RA, double p1Dec) {
        double minDistance = _getMinDistance(elems);

        return Math.abs(spRA - p1RA) <= minDistance
                &&
                Math.abs(spDec - p1Dec) <= minDistance;
    }


    private static double _getMinDistance(ObservationElements elems) {
        SPInstObsComp inst = elems.getInstrument();
        double[] scienceArea = inst.getScienceArea();

        double minDistance = scienceArea[1] / 2.0;

        if (minDistance < 10.0) {
            minDistance = 10.0;
        }
        return minDistance / 3600;
    }


    /**
     * Rule to check that If Band=3 then Band 3 minimum time != 0
     */
    private static IRule BAND3TIME_RULE = new IRule() {

        private static final String ZERO_MESSAGE = "The Band 3 minimum time must not be 0";
        private static final String ALLOC_MESSAGE = "The Band 3 minimum time must be less than or equal to the allocated time";

        public IP2Problems check(ObservationElements elements) {
            try {
                P2Problems probs = new P2Problems();
                if (elements.getProgram().getQueueBand().equals("3")) {
                    double t = elements.getProgram().getMinimumTime().getTimeAmount();
                    if (t == 0.0) {
                        probs.addError(PREFIX+"BAND3TIME_RULE_ZERO_MESSAGE", ZERO_MESSAGE, elements.getProgramNode()); // REL-337
                    }
                    if (t > elements.getProgram().getAwardedTime().getTimeAmount()) {
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

        public IP2Problems check(ObservationElements elements)  {
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
            Option<SPSiteQuality> sqOpt = elements.getSiteQuality();
            if (!sqOpt.isEmpty()) {
                List<SPSiteQuality.TimingWindow> windows = sqOpt.getValue().getTimingWindows();
                if ((windows != null) && (windows.size() > 0)) return null;
            }

            // Generate the warning message.  First figure out which node to
            // apply it to.  Prefer the site quality node, if present.
            if ( elements.getSiteQualityNode().isEmpty()) {
                // TODO: I don't think we can add a problem directly to the
                // TODO: observation node, seems to screw up the summary that
                // TODO: is otherwise shown on the obs node.  So, since there
                // TODO: is no where to put the warning, returning null ...
                //n = elements.getObservationNode();
                return null;
            }

            // Now figure out how long the default window will be.
            String timePeriod = too.getDurationDisplayString();
            String message = String.format(TEMPLATE, timePeriod);

            P2Problems probs = new P2Problems();
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

        public IP2Problems check(ObservationElements elements)  {
            if (elements == null) return null;

            // Get overall obs class
            ObsClass overall = ObsClassService.lookupObsClass(elements.getObservationNode());
            if (overall == ObsClass.DAY_CAL) return null; // no problem by definition; anything goes during the day

            // Now if any step has class DAY_CAL then we have a problem.
            P2Problems probs = new P2Problems();
            for (Config config : elements.getSequence().getAllSteps()) {
                if (ObsClass.DAY_CAL.sequenceValue().equals(config.getItemValue(OBSCLASS_KEY))) {
                    probs.addError(PREFIX+"NO_DAYTIME_CALS_AT_NIGHT_RULE", String.format(MESSAGE, (String)config.getItemValue(OBSLABEL_KEY)), elements.getSeqComponentNode());
                }
            }

            if (probs.getProblemCount() > 0) {
                return probs;
            } else {
                return null;
            }
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
                    final List<TemplateParameters> matchingParams = new ArrayList<TemplateParameters>();
                    TemplateParameters.foreach(templateFolderNode, new ApplyOp<TemplateParameters>() {
                        @Override public void apply(TemplateParameters tp) {
                            final SPTarget p1Target = tp.getTarget();
                            if (_areTargetsEquals(p1Target, obsTarget, elements) || _isTooTarget(p1Target, elements)) {
                                matchingParams.add(tp);
                            }
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

        private SPTarget getObsTarget(ObservationElements elements)  {
            TargetObsComp targetEnv = elements.getTargetObsComp().getOrNull();
            if (targetEnv != null) {
                ObsClass obsClass = ObsClassService.lookupObsClass(elements.getObservationNode());
                if (obsClass == ObsClass.SCIENCE) {
                    SPTarget target = targetEnv.getBase();
                    if (!(target.getTarget() instanceof NonSiderealTarget))
                        return target;
                }
            }
            return null;
        }

        private boolean isAcceptable(SPSiteQuality expected, SPSiteQuality actual) {
            if (actual == null || expected == null) return true; // I guess
            return expected.getCloudCover().getPercentage() <= actual.getCloudCover().getPercentage() &&
                    expected.getImageQuality().getPercentage() <= actual.getImageQuality().getPercentage() &&
                    expected.getSkyBackground().getPercentage() <= actual.getSkyBackground().getPercentage() &&
                    expected.getWaterVapor().getPercentage() <= actual.getWaterVapor().getPercentage();
        }

        class Triple {
            final String blueprintId;
            final String targetId;
            final String siteQualityId;

            private Triple(String blueprintId, String targetId, String condsId) {
                this.blueprintId = blueprintId;
                this.targetId = targetId;
                this.siteQualityId = condsId;
            }
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
