package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeMap;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.SPTarget;

import java.io.Serializable;
import java.util.*;

//* GuideTargets are enabled when the associated {@link GuideProbe} is
//* provided by a {@link edu.gemini.spModel.guide.GuideProbeProvider} in the
//* context of the observation, and disabled otherwise.

/**
 * An immutable pairing of a {@link GuideProbe} with a collection of {@link SPTarget}.
 * One target may be marked as having been picked by background AGS (BAGS), or a status of such a background AGS lookup
 * is stored.
 * One target may be marked as the primary target for the guider.
 *
 * <p>Note that while this object and the list of targets itself is immutable, the actual SPTargets themselves are
 * mutable.
 */
public final class GuideProbeTargets implements Serializable, TargetContainer, Iterable<SPTarget> {

    /**
     * An empty list of GuideProbeTargets.  This is the same object as
     * {@link edu.gemini.shared.util.immutable.ImCollections#EMPTY_LIST}, but
     * typed for convenience.
     */
    public static final ImList<GuideProbeTargets> EMPTY_LIST = ImCollections.emptyList();

    /**
     * An empty SPTarget. Ths is the same object as
     * {@link edu.gemini.shared.util.immutable.None#INSTANCE}, but typed
     * for convenience.
     */
    public static final Option<SPTarget> NO_TARGET = None.instance();

    /**
     * A Comparator that sorts GuideProbeTargets based upon the
     * {@link GuideProbe} that they contain.
     */
    public enum GuiderComparator implements Serializable, Comparator<GuideProbeTargets> {
        instance;

        @Override public int compare(final GuideProbeTargets gt1, final GuideProbeTargets gt2) {
            final GuideProbe g1 = gt1.getGuider();
            final GuideProbe g2 = gt2.getGuider();
            return GuideProbe.KeyComparator.instance.compare(g1, g2);
        }
    }

    /**
     * Sorts a list of GuideProbeTargets comparing on the contained
     * {@link GuideProbe} using the {@link GuiderComparator}.
     *
     * @param lst list of GuideProbeTargets to sort
     *
     * @return sorted list of GuideProbeTargets
     */
    public static ImList<GuideProbeTargets> sortByGuider(final ImList<GuideProbeTargets> lst) {
        return lst.sort(GuiderComparator.instance);
    }

    /**
     * Given an Option<SPTarget> and an SPTarget, if the optional SPTarget is the target, return None, otherwise
     * return the optional SPTarget. This is used in SPTarget removals to set the bagsTarget and primaryTarget
     * appropriately.
     */
    static private final Function2<Option<SPTarget>, SPTarget, Option<SPTarget>> possiblyRemoveTarget = ((o,t) ->
            o.exists(ot -> ot.getTarget().equals(t.getTarget())) ? NO_TARGET : o
    );


    /**
     * Compare targets by their ITarget component so as to ignore watchers.
     */
    static private boolean compareTargets(final Option<SPTarget> t1, final Option<SPTarget> t2) {
        return (t1.isEmpty() && t2.isEmpty()) || t1.exists(spt1 -> t2.exists(spt2 -> spt1.getTarget().equals(spt2.getTarget())));
    }

    /**
     * Creates a GuideTargets instance associated with the given
     * {@link GuideProbe}.
     * It marks no BAGS guide star yet found, and no primary guide star.
     *
     * @param guider guide probe to associate with the GuideTargets
     * @param manualTargets zero or more manual targets
     *
     * @return a new GuideTargets object with the given targets
     */
    public static GuideProbeTargets create(final GuideProbe guider, final SPTarget... manualTargets) {
        return new GuideProbeTargets(guider, BagsLookupPending$.MODULE$, NO_TARGET, DefaultImList.create(manualTargets));
    }

    /**
     * Creates a GuideTargets instance associated with the given
     * {@link GuideProbe}.
     * It marks no BAGS guide star yet found, and no primary guide star.
     *
     * @param guider guide probe to associate with the GuideTargets
     * @param manualTargets zero or more manual targets
     *
     * @return a new GuideTargets object with the given targets
     */
    public static GuideProbeTargets create(final GuideProbe guider, final ImList<SPTarget> manualTargets) {
        return new GuideProbeTargets(guider, BagsLookupPending$.MODULE$, NO_TARGET, manualTargets);
    }

    /**
     * Creates a GuideTargets instance associated with the given
     * {@link GuideProbe}.
     *
     * @param guider guide probe to associate with the GuideTargets
     * @param bagsStatus the status for BAGS
     * @param primaryTarget the primary target, which must be either the BAGS target or in the targets list
     * @param manualTargets zero or more manual targets
     *
     * @return a new GuideTargets object with the given targets
     */
    public static GuideProbeTargets create(final GuideProbe guider, final BagsStatus bagsStatus,
                                           final Option<SPTarget> primaryTarget, final SPTarget... manualTargets) {
        return new GuideProbeTargets(guider, bagsStatus, primaryTarget, DefaultImList.create(manualTargets));
    }

    /**
     * Creates a GuideTargets instance associated with the given
     * {@link GuideProbe}.  It has no primary star if no targets are provided
     * (i.e., <code>targetList.isEmpty()</code>, otherwise it selects the first
     * target in the list.
     *
     * @param guider guider to associate with these targets
     * @param bagsStatus the status for BAGS
     * @param primaryTarget the primary target, which must be either the BAGS target or in the targets list
     * @param manualTargets a list of the manual targets
     *
     * @return a new GuideTargets object with the given targets
     */
    public static GuideProbeTargets create(final GuideProbe guider, final BagsStatus bagsStatus,
                                           final Option<SPTarget> primaryTarget, final ImList<SPTarget> manualTargets) {
        return new GuideProbeTargets(guider, bagsStatus, primaryTarget, manualTargets);
    }


    private final GuideProbe guider;
    private final ImList<SPTarget> manualTargets;

    // The targets selected as the primary target and the target chosen by bags.
    // Note that these can be the same.
    private final Option<SPTarget> primaryTarget;
    private final BagsStatus       bagsStatus;


    private GuideProbeTargets(final GuideProbe guider, final BagsStatus bagsStatus,
                              final Option<SPTarget> primaryTarget, final ImList<SPTarget> manualTargets) {
        if (guider == null)
            throw new IllegalArgumentException("missing guider");
        if (bagsStatus == null)
            throw new IllegalArgumentException("missing BAGS status");
        if (primaryTarget == null)
            throw new IllegalArgumentException("missing primary target");
        if (manualTargets == null)
            throw new IllegalArgumentException("missing target options");

        final boolean primaryIsBags = compareTargets(bagsStatus.bagsStarAsJava(), primaryTarget);
        if (!primaryIsBags && !primaryTarget.forall(manualTargets::contains))
            throw new IllegalArgumentException("primary target must be either BAGS target or in manual targets");

        this.guider = guider;
        this.bagsStatus = bagsStatus;
        this.primaryTarget = primaryTarget;
        this.manualTargets = manualTargets;
    }

    /**
     * Gets the guider associated with the list of targets.
     */
    public GuideProbe getGuider() {
        return guider;
    }

    /**
     * Quick boolean method to determine if this object contains any targets, as calling getTargets().nonEmpty
     * is inefficient since it creates a new list.
     * @return true if there are any targets in this object, and false otherwise
     */
    public boolean containsTargets() {
        return bagsStatus.bagsStarAsJava().isDefined() || manualTargets.nonEmpty();
    }

    /**
     * Determine if the targets contain the specified target.
     * This is the case if the given target has been selected by BAGS or manually.
     * @param target the target in question
     * @return true if either the BAGS target or a manual target, and false otherwise
     */
    @Override
    public boolean containsTarget(final SPTarget target) {
        return target != null && (bagsStatus.bagsStarAsJava().exists(target::equals) || manualTargets.contains(target));
    }

    /**
     * Get a list of the targets. If a BAGS target has been specified, it will be prepended to the list of
     * manual targets
     * @return a list of all targets
     */
    @Override
    public ImList<SPTarget> getTargets() {
        if (bagsStatus.bagsStarAsJava().isDefined())
            return manualTargets.cons(bagsStatus.bagsStarAsJava().getValue());
        else
            return manualTargets;
    }

    /**
     * Get a list of the manual targets only.
     * @return a list of the manual targets
     */
    public ImList<SPTarget> getManualTargets() {
        return manualTargets;
    }

    /**
     * Create a deep clone of this instance.
     * @return the deep clone
     */
    @Override
    public GuideProbeTargets cloneTargets() {
        final BagsStatus       bagsStatusClone    = bagsStatus.clone();
        final ImList<SPTarget> manualTargetsClone = manualTargets.map(SPTarget::clone);

        // Cases:
        // 1. Primary is None
        // 2. Primary is BAGS
        // 3. Primary is in manual list
        final Option<SPTarget> primaryTargetClone = bagsStatus.bagsStarAsJava().equals(primaryTarget) ?
                bagsStatusClone.bagsStarAsJava() :
                primaryTarget.map(t -> manualTargetsClone.get(manualTargets.indexOf(t)));

        return new GuideProbeTargets(guider, bagsStatusClone, primaryTargetClone, manualTargetsClone);
    }


    /**
     * Remove a target.
     * If it is the BAGS target, then create a new copy with BAGS status set to pending.
     * If it is the primary target, then create a new copy with no primary target.
     * @param target the target to remove
     * @return a copy of this GuideProbeTargets with the target removed
     */
    @Override
    public GuideProbeTargets removeTarget(final SPTarget target) {
        if (!bagsStatus.bagsStarAsJava().exists(target::equals) && !manualTargets.contains(target))
            return this;

        final BagsStatus bagsStatusNew = bagsStatus.bagsStarAsJava().exists(target::equals) ? BagsLookupPending$.MODULE$ : bagsStatus;
        final Option<SPTarget> primaryTargetNew = possiblyRemoveTarget.apply(primaryTarget, target);
        final ImList<SPTarget> manualTargetsNew = manualTargets.remove(target);
        return new GuideProbeTargets(guider, bagsStatusNew, primaryTargetNew, manualTargetsNew);
    }

    /**
     * Remove a target. If is the primary target, we want to maintain a primary target, either as
     * the next candidate in the list if one is defined, or the previous candidate if one isn't (otherwise
     * None).
     * @param target the target to remove
     * @return a copy of this GuideProbeTargets with the target removed and a primary selected as per the description
     */
    public GuideProbeTargets removeTargetSelectPrimary(final SPTarget target) {
        // If no primary, or target is not primary, then regular removal is sufficient.
        if (!primaryTarget.exists(target::equals))
            return removeTarget(target);

        final BagsStatus bagsStatusNew = bagsStatus.bagsStarAsJava().exists(target::equals) ? BagsLookupPending$.MODULE$ : bagsStatus;
        final ImList<SPTarget> manualTargetsNew = manualTargets.remove(target);

        // We are removing the primary target, so determine the new primary if one can be chosen.
        final ImList<SPTarget> oldTargets = getTargets();
        final int oldIndex = oldTargets.indexOf(target);
        final int newIndex = oldIndex+1 < oldTargets.size() ? oldIndex+1
                : (oldIndex-1 >= 0 ? oldIndex-1 : -1);
        final Option<SPTarget> primaryTargetNew = newIndex == -1 ? NO_TARGET : new Some<>(oldTargets.get(newIndex));
        return new GuideProbeTargets(guider, bagsStatusNew, primaryTargetNew, manualTargetsNew);
    }

    /**
     * Iterator to all of the SPTargets in this object, including the BAGS target (which leads).
     */
    @Override
    public Iterator<SPTarget> iterator() {
        return getTargets().iterator();
    }

    public BagsStatus getBagsStatus() {
        return bagsStatus;
    }

    public Option<SPTarget> getBagsTarget() {
        return bagsStatus.bagsStarAsJava();
    }

    public Option<SPTarget> getPrimary() {
        return primaryTarget;
    }

    /**
     * Determine if the primary guide star is the guide star as selected by BAGS.
     * If the primary guide star is not selected, this returns false.
     * @return returns true if the primary guide star is the BAGS star, else false
     */
    public boolean primaryIsBagsTarget() {
        return bagsStatus.bagsStarAsJava().exists(bt -> primaryTarget.exists(bt::equals));
    }

    /**
     * Set the BAGS target. If the primary is the old BAGS target or unset, then the primary is set to the
     * new BAGS target automatically; otherwise, it is maintained.
     * @param targetOption the new BAGS target
     * @return a new GuideProbeTargets with the primary target and BAGS target as described
     */
    public GuideProbeTargets withBagsTarget(final Option<SPTarget> targetOption) {
        if (targetOption.equals(bagsStatus.bagsStarAsJava()))
            return this;

        final BagsStatus bagsStatusNew = targetOption.map(t -> (BagsStatus)BagsSuccessWithTarget$.MODULE$.apply(t)).getOrElse(BagsLookupPending$.MODULE$);
        final Option<SPTarget> primaryTargetNew = primaryIsBagsTarget() || primaryTarget.isEmpty() ? targetOption : primaryTarget;
        return new GuideProbeTargets(guider, bagsStatusNew, primaryTargetNew, manualTargets);
    }

    /**
     * Set the BAGS target. If the primary is the old BAGS target or unset, then the primary is set to the
     * new BAGS target automatically; otherwise, it is maintained.
     * @param target the new BAGS target
     * @return a new GuideProbeTargets with the primary target and BAGS target as described
     */
    public GuideProbeTargets withBagsTarget(final SPTarget target) {
        return withBagsTarget(ImOption.apply(target));
    }

    /**
     * Set the BAGS status. If the primary is the old BAGS target or unset, then clear out the primary
     * if the new BAGS status is anything other than one with a guide star; otherwise, maintain.
     */
    public GuideProbeTargets withBagsStatus(final BagsStatus status) {
        if (status.equals(bagsStatus) && status.bagsStarAsJava().equals(bagsStatus.bagsStarAsJava()))
            return this;

        final Option<SPTarget> primaryTargetNew = primaryIsBagsTarget() || primaryTarget.isEmpty() ? status.bagsStarAsJava() : primaryTarget;
        return new GuideProbeTargets(guider, status, primaryTargetNew, manualTargets);
    }

    /**
     * Select the primary guide star. This must already be either the BAGS guide star or in the
     * list of manual targets if it is defined.
     * @param targetOption the primary target to select, or None
     * @return a new GuideProbeTargets with the primary target as indicated
     */
    public GuideProbeTargets withExistingPrimary(final Option<SPTarget> targetOption) {
        if (targetOption.equals(primaryTarget))
            return this;
        return new GuideProbeTargets(guider, bagsStatus, targetOption, manualTargets);
    }

    /**
     * Select the primary guide star. This must already be either the BAGS guide star or in the
     * list of manual targets.
     * @param target the primary target to select
     * @return a new GuideProbeTargets with the primary target as indicated
     */
    public GuideProbeTargets withExistingPrimary(final SPTarget target) {
        final Option<SPTarget> targetOption = target == null ? NO_TARGET : new Some<>(target);
        return withExistingPrimary(targetOption);
    }

    /**
     * Set the primary guide star. If it is not in the list, add it to the end of the list and mark it as the primary.
     * @param target the primary guide star.
     * @return the new GuideProbeTargets with the primary target as indicated.
     */
    public GuideProbeTargets withManualPrimary(final SPTarget target) {
        if (target == null)
            return new GuideProbeTargets(guider, bagsStatus, NO_TARGET, manualTargets);

        final boolean alreadyInList = manualTargets.contains(target);
        if (alreadyInList && primaryTarget.exists(target::equals))
            return this;

        final ImList<SPTarget> manualTargetsNew = alreadyInList ? manualTargets : manualTargets.append(target);
        return new GuideProbeTargets(guider, bagsStatus, new Some<>(target), manualTargetsNew);
    }

    /**
     * Return the index of the primary target, if one exists, or None if there is no primary target.
     * Note that if a BAGS target exists, we count the BAGS target as index 0 and then begin counting the manual targets at index 1.
     * Otherwise, we begin counting the manual targets at index 0.
     * @return the index as described above, or None if no primary
     */
    public Option<Integer> getPrimaryIndex() {
        if (primaryIsBagsTarget())
            return new Some<>(0);

        final int bagsIncrementor = bagsStatus.bagsStarAsJava().isEmpty() ? 0 : 1;
        return primaryTarget.map(t -> manualTargets.indexOf(t) + bagsIncrementor);
    }

    /**
     * Set the primary target by index.
     * If no index is specified, return a new GuideProbeTargets with no primary.
     * If there is a BAGS star, we begin counting it as 0, and then the manual targets at index 1.
     * Otherwise, we begin counting the manual targets at index 0.
     * @param indexOption the optional index to use as the primary
     * @return the new GuideProbeTargets with the new primary as described above
     */
    public GuideProbeTargets withPrimaryByIndex(final Option<Integer> indexOption) {
        // If the current primary is already as set, nothing to do.
        final Option<Integer> oldPrimaryIndex = getPrimaryIndex();
        if (indexOption.exists(i -> oldPrimaryIndex.exists(i::equals))
                || (indexOption.isEmpty() && oldPrimaryIndex.isEmpty()))
            return this;

        if (indexOption.isEmpty())
            return new GuideProbeTargets(guider, bagsStatus, NO_TARGET, manualTargets);

        return withPrimaryByIndex(indexOption.getValue());
    }

    /**
     * Set the primary target by index.
     * If there is a BAGS star, we begin counting it as 0, and then the manual targets at index 1.
     * Otherwise, we begin counting the manual targets at index 0.
     * @param index the index to use as the primary
     * @throws java.lang.IndexOutOfBoundsException if the index does not indicate a valid guide star
     * @return the new GuideProbeTargets with the new primary as described above
     */
    public GuideProbeTargets withPrimaryByIndex(final int index) {
        if (index == 0 && bagsStatus.bagsStarAsJava().isDefined())
            return new GuideProbeTargets(guider, bagsStatus, bagsStatus.bagsStarAsJava(), manualTargets);

        final int bagsAdjustment = bagsStatus.bagsStarAsJava().isEmpty() ? 0 : 1;
        final SPTarget primaryNew = manualTargets.get(index - bagsAdjustment);
        return new GuideProbeTargets(guider, bagsStatus, new Some<>(primaryNew), manualTargets);
    }

    /**
     * Toggles the primary element:
     * 1. If target is not the primary, it is set as the primary.
     * 2. If target is set as the primary, the primary is set to NONE.
     * @param target the primary target to toggle
     * @return the new GuideProbeTargets with the primary as described above
     * @throws java.lang.IllegalArgumentException if <code>primary</code> is not in the list of targets.
     */
    public GuideProbeTargets togglePrimary(final SPTarget target) {
        if (!getTargets().contains(target))
            throw new IllegalArgumentException("not a member of the list");
        final Option<SPTarget> newPrimary = primaryTarget.exists(target::equals) ? None.instance() : new Some<>(target);
        return new GuideProbeTargets(guider, bagsStatus, newPrimary, manualTargets);
    }

    /**
     * Add a target to the list of manual targets. Does not change the primary target.
     * @param target the manual target to add.
     * @return a new GuideProbeTargets with the manual target added.
     */
    public GuideProbeTargets addManualTarget(final SPTarget target) {
        if (manualTargets.contains(target))
            return this;
        return new GuideProbeTargets(guider, bagsStatus, primaryTarget, manualTargets.append(target));
    }

    /**
     * Set the manual targets.
     * If a primary guide star was specified before that is no longer in the manual targets, then set it to None.
     * @param manualTargetsNew the new list of manual targets to use
     * @return a new GuideProbeTargets as described above.
     */
    public GuideProbeTargets withManualTargets(final ImList<SPTarget> manualTargetsNew) {
        // If the primary target is None, or the primary is the BAGS, then just return with the new list.
        if (bagsStatus.bagsStarAsJava().forall(bt -> primaryTarget.forall(bt::equals)))
            return new GuideProbeTargets(guider, bagsStatus, primaryTarget, manualTargetsNew);

        final Option<SPTarget> primaryTargetNew = primaryTarget.forall(manualTargetsNew::contains) ? primaryTarget : NO_TARGET;
        return new GuideProbeTargets(guider, bagsStatus, primaryTargetNew, manualTargetsNew);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final GuideProbeTargets that = (GuideProbeTargets) o;
        if (!guider.equals(that.guider)) return false;
        if (!bagsStatus.equals(that.bagsStatus)) return false;
        if (!primaryTarget.equals(that.primaryTarget)) return false;
        return manualTargets.equals(that.manualTargets);
    }

    @Override
    public int hashCode() {
        int result = guider.hashCode();
        result = 31 * result + bagsStatus.hashCode();
        result = 31 * result + primaryTarget.hashCode();
        result = 31 * result + manualTargets.hashCode();
        return result;
    }



    public static final String GUIDER_PARAM_SET_NAME = "guider";


    public ParamSet getParamSet(final PioFactory factory) {
        final ParamSet paramSet = factory.createParamSet(GUIDER_PARAM_SET_NAME);

        Pio.addParam(factory, paramSet, "key", getGuider().getKey());

        // Store the bags status.
        bagsStatus.getParamSet(factory);

        // If a bags target is set, store it.
//        bagsTarget.foreach(t -> {
//            final ParamSet bagsParamSet = factory.createParamSet(BAGSTARGET_PARAM_SET_NAME);
//            bagsParamSet.addParamSet(t.getParamSet(factory));
//        });

        // If a primary target is set, store the index.
        getPrimaryIndex().foreach(i ->
                        Pio.addIntParam(factory, paramSet, "primary", i)
        );

        getManualTargets().foreach(t -> paramSet.addParamSet(t.getParamSet(factory)));

        return paramSet;
    }

    public static GuideProbeTargets fromParamSet(final ParamSet parent) {
        final String key = Pio.getValue(parent, "key");
        final GuideProbe probe = GuideProbeMap.instance.get(key);
        if (probe == null) return null;


        // Read in the bagsTarget if there is one.
//        final ParamSet bagsParamSet = parent.getParamSet(BAGSTARGET_PARAM_SET_NAME);
//        final Option<SPTarget> bagsTarget = bagsParamSet == null || bagsParamSet.getParamSetCount() != 1
//                ? NO_TARGET
//                : new Some<>(SPTarget.fromParamSet(bagsParamSet.getParamSets().get(0)));
        final BagsStatus bagsStatus = BagsStatus$.MODULE$.fromParamSet(parent);

        // Primary index.
        final int primaryIndex = Pio.getIntValue(parent, "primary", -1);
        final Option<Integer> primary = (primaryIndex>=0) ? new Some<>(primaryIndex) : None.INTEGER;


        final List<SPTarget> lst = new ArrayList<>();
        parent.getParamSets().stream()
                .filter(ps -> !BagsStatus$.MODULE$.BagsStatusParamSetName().equals(ps.getName()))
                .forEach(ps -> lst.add(SPTarget.fromParamSet(ps)));
        final ImList<SPTarget> manualTargets = DefaultImList.create(lst);

        final GuideProbeTargets gpt = new GuideProbeTargets(probe, bagsStatus, NO_TARGET, manualTargets);
        return gpt.withPrimaryByIndex(primary);
    }

    public String mkString(final String prefix, final String sep, final String suffix) {
        final StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("guider=").append(guider).append(sep);
        sb.append(bagsStatus.id());
        bagsStatus.bagsStarAsJava().foreach(t -> sb.append("(").append(t.toString()).append(")"));
        sb.append(sep);
        primaryTarget.foreach(t -> sb.append("primaryTarget").append(t.toString()).append(sep));
        sb.append(manualTargets.mkString(prefix, sep, suffix)).append(suffix);
        return sb.toString();
    }

    public String toString() {
        return mkString("[", ", ", "]");
    }
}
