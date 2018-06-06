package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.Function1;
import edu.gemini.shared.util.immutable.ImCollections;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.SPCoordinates;
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
    public static TargetEnvironmentDiff all(final TargetEnvironment oldEnv, final TargetEnvironment newEnv) {
        return new TargetEnvironmentDiff(oldEnv, newEnv,
                oldEnv.getTargets(), newEnv.getTargets(),
                oldEnv.getCoordinates(), newEnv.getCoordinates());
    }

    /**
     * Gets the diffeence between two target environments considering only the
     * guide stars associated with the given guider in the primary guide group
     * of each environment.
     */
    public static TargetEnvironmentDiff guideProbe(final TargetEnvironment oldEnv, final TargetEnvironment newEnv, final GuideProbe guider) {
        final ImList<SPTarget> empty = ImCollections.emptyList();
        return primaryGuideGroupExtraction(oldEnv, newEnv,
                g -> g.get(guider).map(GuideProbeTargets::getTargets).getOrElse(empty));
    }

    private static TargetEnvironmentDiff primaryGuideGroupExtraction(final TargetEnvironment oldEnv, final TargetEnvironment newEnv,
                                                                     final Function1<GuideGroup, ImList<SPTarget>> f) {
        final ImList<SPTarget> oldList = f.apply(oldEnv.getGuideEnvironment().getPrimary());
        final ImList<SPTarget> newList = f.apply(newEnv.getGuideEnvironment().getPrimary());
        return new TargetEnvironmentDiff(oldEnv, newEnv, oldList, newList,
                ImCollections.emptyList(), ImCollections.emptyList());
    }

    private final TargetEnvironment oldEnv;
    private final TargetEnvironment newEnv;

    private final Collection<SPTarget> removedTargets;
    private final Collection<SPTarget> addedTargets;

    private final Collection<SPCoordinates> removedCoordinates;
    private final Collection<SPCoordinates> addedCoordinates;

    // Return an ImList<T> containing the elements of list1 \ list2.
    private static<T> Collection<T> calcDifference(final TargetEnvironment env1, final TargetEnvironment env2,
                                                   final ImList<T> list1, final ImList<T> list2) {
        final Set<T> set1 = Collections.newSetFromMap(new IdentityHashMap<>());
        if (env1 != null)
            set1.addAll(list1.toList());
        if (env2 != null)
            set1.removeAll(list2.toList());
        return Collections.unmodifiableCollection(set1);
    }

    private TargetEnvironmentDiff(final TargetEnvironment oldEnv, final TargetEnvironment newEnv,
                                  final ImList<SPTarget> oldTargets, final ImList<SPTarget> newTargets,
                                  final ImList<SPCoordinates> oldCoords, final ImList<SPCoordinates> newCoords) {
        this.oldEnv = oldEnv;
        this.newEnv = newEnv;
        this.removedTargets     = calcDifference(oldEnv, newEnv, oldTargets, newTargets);
        this.addedTargets       = calcDifference(newEnv, oldEnv, newTargets, oldTargets);
        this.removedCoordinates = calcDifference(oldEnv, newEnv, oldCoords, newCoords);
        this.addedCoordinates   = calcDifference(newEnv, oldEnv, newCoords, oldCoords);
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

    public Collection<SPCoordinates> getRemovedCoordinates() {
        return removedCoordinates;
    }

    public Collection<SPCoordinates> getAddedCoordinates() {
        return addedCoordinates;
    }
}
