package jsky.app.ot.gemini.editor.targetComponent;

import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.OptionsList;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;

/**
 * Handles toggling the primary state of a guide star.
 */
public enum PrimaryTargetToggle {
    instance;

    /**
     * Toggles the primary state of a guide star within the target component.
     * If the given <code>target</code> is not a guide star, then nothing is
     * done.  If it is a guide star and is not already the primary guide star
     * for its guider, then it becomes the primary guide star.  If it is already
     * the primary guide star, then there is no primary guide star for its
     * guider after this call.
     *
     * @param obsComp target component
     * @param target target whose primary state should be toggled on or off
     */
    public void toggle(TargetObsComp obsComp, SPTarget target) {
        if ((obsComp == null) || (target == null)) return;

        final TargetEnvironment env = obsComp.getTargetEnvironment();
        final GuideGroup grp = env.getPrimaryGuideGroup();
        final ImList<GuideProbeTargets> lst = grp.getAllContaining(target);
        if (lst.isEmpty()) return; // target not associated with any guider

        final TargetEnvironment finalEnv = lst.foldLeft(env,
                (e, gt) -> e.putPrimaryGuideProbeTargets(gt.update(OptionsList.UpdateOps.togglePrimary(target))));
        obsComp.setTargetEnvironment(finalEnv);
    }
}
