//
// $
//

package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.Function1;
import edu.gemini.shared.util.immutable.ImCollections;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.SPTarget;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * A helper class used to obtain the difference between two TargetEnvironment
 * instances.  This class is used to obtain lists of {@link SPTarget}s that
 * were removed and added in the new version.
 */
public final class TargetEnvironmentDiff implements Serializable {

    /**
     * Gets the difference between two target environments considering all the
     * targets in each environment.
     */
    public static TargetEnvironmentDiff all(TargetEnvironment oldEnv, TargetEnvironment newEnv) {
        return new TargetEnvironmentDiff(oldEnv, newEnv, oldEnv.getTargets(), newEnv.getTargets());
    }

    /**
     * Gets the difference between two target environments considering only the
     * guide stars in the primary guide group of each environment.
     */
    public static TargetEnvironmentDiff primaryGuideGroup(TargetEnvironment oldEnv, TargetEnvironment newEnv) {
        return primaryGuideGroupExtraction(oldEnv, newEnv, GuideGroup::getTargets);
    }

    /**
     * Gets the diffeence between two target environments considering only the
     * guide stars associated with the given guider in the primary guide group
     * of each environment.
     */
    public static TargetEnvironmentDiff guideProbe(TargetEnvironment oldEnv, TargetEnvironment newEnv, final GuideProbe guider) {
        final ImList<SPTarget> empty = ImCollections.emptyList();
        return primaryGuideGroupExtraction(oldEnv, newEnv,
                g -> g.get(guider).map(GuideProbeTargets::getTargets).getOrElse(empty));
    }

    private static TargetEnvironmentDiff primaryGuideGroupExtraction(TargetEnvironment oldEnv, TargetEnvironment newEnv, Function1<GuideGroup, ImList<SPTarget>> f) {
        final ImList<SPTarget> empty = ImCollections.emptyList();
        final ImList<SPTarget> oldList = oldEnv.getGuideEnvironment().getPrimary().map(f).getOrElse(empty);
        final ImList<SPTarget> newList = newEnv.getGuideEnvironment().getPrimary().map(f).getOrElse(empty);
        return new TargetEnvironmentDiff(oldEnv, newEnv, oldList, newList);
    }

    private final TargetEnvironment oldEnv;
    private final TargetEnvironment newEnv;

    private final Collection<SPTarget> removedTargets;
    private final Collection<SPTarget> addedTargets;

    private TargetEnvironmentDiff(TargetEnvironment oldEnv, TargetEnvironment newEnv,
                                  ImList<SPTarget> oldTargets, ImList<SPTarget> newTargets) {
        this.oldEnv = oldEnv;
        this.newEnv = newEnv;

        Set<SPTarget> oldSet = Collections.newSetFromMap(new IdentityHashMap<>());
        if (oldEnv != null) oldSet.addAll(oldTargets.toList());

        Set<SPTarget> newSet = Collections.newSetFromMap(new IdentityHashMap<>());
        if (newEnv != null) newSet.addAll(newTargets.toList());

        if (oldEnv != null) newSet.removeAll(oldTargets.toList());
        if (newEnv != null) oldSet.removeAll(newTargets.toList());

        removedTargets = Collections.unmodifiableCollection(oldSet);
        addedTargets   = Collections.unmodifiableCollection(newSet);
    }

    public TargetEnvironment getOldEnvironment() {
        return oldEnv;
    }

    public TargetEnvironment getNewEnvironment() {
        return newEnv;
    }

    public Collection<SPTarget> getRemovedTargets() {
        return removedTargets;
    }

    public Collection<SPTarget> getAddedTargets() {
        return addedTargets;
    }
}
