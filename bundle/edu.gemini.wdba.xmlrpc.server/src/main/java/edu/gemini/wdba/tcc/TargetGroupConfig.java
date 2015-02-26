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

    public static TargetGroupConfig createBaseGroup(TargetEnvironment env) {
        ImList<SPTarget> targets = env.getUserTargets();
        targets = targets.cons(env.getBase());
        return new TargetGroupConfig(TccNames.BASE, targets, env.getBase().getTarget().getName());
    }

    public static TargetGroupConfig createGuideGroup(GuideProbeTargets gt) {
        GuideProbe guider = gt.getGuider();

        ImList<SPTarget> targets = gt.getOptions();
        Option<SPTarget> primaryOpt = gt.getPrimary();

        // SW: no longer always setting a primary target.
//        if ((primary == null) && (targets.size() > 0)) primary = targets.head();

        String primaryTargetName = null;
        if (!primaryOpt.isEmpty()) primaryTargetName = primaryOpt.getValue().getTarget().getName();

        String tag = TargetConfig.getTag(guider);
        return new TargetGroupConfig(tag, targets, primaryTargetName);
    }

    private TargetGroupConfig(String name, ImList<SPTarget> targets, String primaryTargetName) {
        super(name);

        addAttribute(TYPE, TYPE_VALUE);

        if ((primaryTargetName != null) && !"".equals(primaryTargetName)) {
            putParameter(TccNames.PRIMARY, primaryTargetName);
        }

        List<String> targetNames = new ArrayList<String>(targets.size());
        for (SPTarget target : targets) {
            targetNames.add(target.getTarget().getName());
        }

        putParameter(TccNames.TARGETS, targetNames);
    }
}
