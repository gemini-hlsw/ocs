package edu.gemini.p2checker.rules.general;

import edu.gemini.p2checker.api.IRule;
import edu.gemini.p2checker.api.IP2Problems;
import edu.gemini.p2checker.api.ObservationElements;
import edu.gemini.p2checker.api.P2Problems;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obsclass.ObsClass;



/**
 * Structure Rules are kind of fundamental problems that are checked
 * before any other rule. If any of this problems is found, then
 * no more checkings are performed on the observation before they
 * are fixed by the user.
 */
public class StructureRule implements IRule {
    private static final String PREFIX = "StructureRule_";

    public static StructureRule INSTANCE = new StructureRule();

    private static final String INSTANCE_MSG = "All science observations need %s component";
    private static final String TEMPLATE_MSG = "Template science/aquisition observations should not have %s component; it will not be replaced when the template is applied.";

    public IP2Problems check(ObservationElements elements)  {
        P2Problems problems = new P2Problems();
        final ObsClass obsClass = ObsClassService.lookupObsClass(elements.getObservationNode());
        if (elements.isTemplate()) {
            if (obsClass == ObsClass.SCIENCE || obsClass == ObsClass.ACQ) {
                checkTemplate(elements, problems);
                checkCommon(elements, problems);
            }
        } else {
            // RCN: old rule doesn't look at ACQ; leaving as-is
            if (obsClass == ObsClass.SCIENCE) {
                checkInstance(elements, problems);
                checkCommon(elements, problems);
            }
        }
        return problems;

    }

    private static void checkCommon(ObservationElements elements, P2Problems problems) {
        if (elements.getInstrumentNode() == null) {
            problems.addError(PREFIX + "instrument", String.format(INSTANCE_MSG, "an instrument"), elements.getObservationNode());
        }
    }

    private static void checkInstance(ObservationElements elements, P2Problems problems) {
        if (elements.getTargetObsComponentNode().isEmpty()) {
            problems.addError(PREFIX + "target", String.format(INSTANCE_MSG, "a target"), elements.getObservationNode());
        }
        if (elements.getSiteQualityNode().isEmpty()) {
            problems.addError(PREFIX + "observing_conditions", String.format(INSTANCE_MSG, "an observing conditions"), elements.getObservationNode());
        }
    }

    private static void checkTemplate(ObservationElements elements, P2Problems problems) {
        if (!elements.getTargetObsComponentNode().isEmpty()) {
            problems.addWarning(PREFIX + "target", String.format(TEMPLATE_MSG, "a target"), elements.getObservationNode());
        }
        if (!elements.getSiteQualityNode().isEmpty()) {
            problems.addWarning(PREFIX + "observing_conditions", String.format(TEMPLATE_MSG, "an observing conditions"), elements.getObservationNode());
        }
    }

}
