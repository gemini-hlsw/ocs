//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link ParamSet} implementation for a group of targets.
 */
public final class TargetGroupConfig extends ParamSet {
    public static final String TYPE_VALUE="targetgroup";

    public static TargetGroupConfig createBaseGroup(final TargetEnvironment env) {
        final ImList<SPTarget> targets = env.getUserTargets().cons(env.getBase());
        return new TargetGroupConfig(TccNames.BASE, targets, GuideProbeTargets.NO_TARGET,
                env.getBase().getTarget().getName());
    }

    public static TargetGroupConfig createGuideGroup(final GuideProbeTargets gt) {
        final GuideProbe guider = gt.getGuider();
        final ImList<SPTarget> manualTargets = gt.getManualTargets();
        final Option<SPTarget> primaryOpt = gt.getPrimary();
        final Option<SPTarget> bagsTargetOpt = gt.getBAGSTarget();

        // SW: no longer always setting a primary target.
//        if ((primary == null) && (targets.size() > 0)) primary = targets.head();

        final String primaryTargetName = primaryOpt.map(p -> p.getTarget().getName()).getOrNull();
        final String tag = TargetConfig.getTag(guider);
        return new TargetGroupConfig(tag, manualTargets, bagsTargetOpt, primaryTargetName);
    }

    private TargetGroupConfig(final String name, final ImList<SPTarget> targets,
                              final Option<SPTarget> bagsTarget, final String primaryTargetName) {
        super(name);

        addAttribute(TYPE, TYPE_VALUE);

        if ((primaryTargetName != null) && !"".equals(primaryTargetName)) {
            putParameter(TccNames.PRIMARY, primaryTargetName);
        }

        final List<String> targetNames = new ArrayList<String>(targets.size());
        targets.foreach(t -> targetNames.add(t.getTarget().getName()));
        putParameter(TccNames.TARGETS, targetNames);

        bagsTarget.foreach(t -> putParameter(TccNames.BAGSTARGET, t.getTarget().getName()));
    }
}
