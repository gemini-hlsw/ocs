//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The {@link ParamSet} implementation for a group of targets.
 */
public final class TargetGroupConfig extends ParamSet {
    public static final String TYPE_VALUE="targetgroup";

    public static TargetGroupConfig createBaseGroup(final TargetEnvironment env) {
        final ImList<SPTarget> targets = env.getUserTargets().cons(env.getBase());
        return new TargetGroupConfig(TccNames.BASE, targets, ImOption.apply(env.getBase()));
    }

    public static TargetGroupConfig createGuideGroup(final GuideProbeTargets gt) {
        final String tag = TargetConfig.getTag(gt.getGuider());
        final ImList<SPTarget> targets = gt.getTargets();
        final Option<SPTarget> primaryOpt = gt.getPrimary();
        return new TargetGroupConfig(tag, targets, primaryOpt);
    }

    private TargetGroupConfig(final String name, final ImList<SPTarget> targets, final Option<SPTarget> primaryTarget) {
        super(name);

        addAttribute(TYPE, TYPE_VALUE);

        primaryTarget.map(t -> t.getTarget().getName())
                .filter(n -> !"".equals(n))
                .foreach(n -> putParameter(TccNames.PRIMARY, n));

        final List<String> targetNames = targets.toList().stream().map(t -> t.getTarget().getName()).collect(Collectors.toList());
        putParameter(TccNames.TARGETS, targetNames);
    }
}
