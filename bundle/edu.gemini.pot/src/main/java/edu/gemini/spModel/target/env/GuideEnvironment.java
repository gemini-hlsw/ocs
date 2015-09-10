//
// $
//

package edu.gemini.spModel.target.env;

import static edu.gemini.spModel.target.env.OptionsList.UpdateOps.appendAsPrimary;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.SPTarget;

import java.io.Serializable;
import java.util.*;

// NOTE: Caching the active guiders here feels wrong.  It is expensive to
// calculate though (see GuideSync).  This information was previously stored
// in each GuideProbeTargets instance, via an enabled/disabled flag so this
// seems like an improvement at least.

// NOTE2: It's debatable whether this object should exist or just be merged
// with TargetEnvironment.  On the one hand, it is nice to have a single
// container for all the guide target information.  On the other, it is
// inconvenient whenever the information is accessed or updated to go through
// another layer of hierarchy that is, after all, pretty thin.


/**
 * A GuideEnvironment tracks the active guiders and a collection of
 * {@link GuideGroup} options.  It is meant to contain all target information
 * related to guiding.
 */
public final class GuideEnvironment implements Serializable, TargetContainer, OptionsList<GuideGroup> {

    /**
     * An empty GuideEnvironment.
     */
    public static GuideEnvironment EMPTY = create(OptionsListImpl.create(GuideGroup.EMPTY_LIST));

    /**
     * Creates a GuideEnvironment with the given set of GuideGroups.
     */
    public static GuideEnvironment create(OptionsList<GuideGroup> guideGroups) {
        return new GuideEnvironment(guideGroups);
    }

    private final OptionsList<GuideGroup> guideGroups;

    private GuideEnvironment(OptionsList<GuideGroup> guideGroups) {
        if (guideGroups == null) {
            throw new IllegalArgumentException("guideGroups == null");
        }
        this.guideGroups   = guideGroups;
    }

    /**
     * Gets all the {@link GuideProbe}s referenced by all the
     * {@link GuideProbeTargets} in all the {@link GuideGroup}s in this
     * environment.  They are sorted using the
     * {@link edu.gemini.spModel.guide.GuideProbe.KeyComparator}.
     */
    public SortedSet<GuideProbe> getReferencedGuiders() {
        final SortedSet<GuideProbe> res = new TreeSet<>(GuideProbe.KeyComparator.instance);
        guideGroups.getOptions().foreach(guideGroup -> res.addAll(guideGroup.getReferencedGuiders()));
        return res;
    }

    /**
     * Gets the subset of referenced guiders that are actually selected, if any.
     */
    public SortedSet<GuideProbe> getPrimaryReferencedGuiders() {
        return guideGroups.getPrimary().map(GuideGroup::getPrimaryReferencedGuiders).getOrElse(new TreeSet<>());
    }

    @Override
    public ImList<SPTarget> getTargets() {
        return guideGroups.getOptions().flatMap(TargetContainer::getTargets);
    }

    @Override
    public boolean containsTarget(SPTarget target) {
        return guideGroups.getOptions().exists(gg -> gg.containsTarget(target));
    }

    private GuideEnvironment updateGuideGroups(UpdateOp<GuideGroup> f) {
        final ImList<GuideGroup> updatedList = guideGroups.getOptions().map(f);
        final Option<Integer> primary = guideGroups.getPrimaryIndex();
        return new GuideEnvironment(OptionsListImpl.create(primary, updatedList));
    }

    @Override
    public GuideEnvironment cloneTargets() {
        return updateGuideGroups(GuideGroup::cloneTargets);
    }

    @Override
    public GuideEnvironment removeTarget(SPTarget target) {
        return updateGuideGroups(g -> g.removeTarget(target));
    }

    public GuideEnvironment removeGroup(GuideGroup group) {
        return setOptions(getOptions().remove(group));
    }

    @Override
    public Iterator<GuideGroup> iterator() {
        return guideGroups.iterator();
    }

    @Override
    public Option<GuideGroup> getPrimary() {
        return guideGroups.getPrimary();
    }

    @Override
    public GuideEnvironment selectPrimary(Option<GuideGroup> primary) {
        return new GuideEnvironment(guideGroups.selectPrimary(primary));
    }

    @Override
    public GuideEnvironment selectPrimary(GuideGroup primary) {
        return new GuideEnvironment(guideGroups.selectPrimary(primary));
    }

    @Override
    public GuideEnvironment setPrimary(GuideGroup primary) {
        return new GuideEnvironment(guideGroups.setPrimary(primary));
    }

    @Override
    public Option<Integer> getPrimaryIndex() {
        return guideGroups.getPrimaryIndex();
    }

    @Override
    public GuideEnvironment setPrimaryIndex(Option<Integer> primary) {
        return new GuideEnvironment(guideGroups.setPrimaryIndex(primary));
    }

    @Override
    public GuideEnvironment setPrimaryIndex(int primary) {
        return new GuideEnvironment(guideGroups.setPrimaryIndex(primary));
    }

    @Override
    public ImList<GuideGroup> getOptions() {
        return guideGroups.getOptions();
    }

    @Override
    public GuideEnvironment setOptions(ImList<GuideGroup> newList) {
        return new GuideEnvironment(guideGroups.setOptions(newList));
    }

    @Override
    public GuideEnvironment update(Option<Integer> primaryIndex, ImList<GuideGroup> options) {
        return new GuideEnvironment(guideGroups.update(primaryIndex, options));
    }

    @Override
    public GuideEnvironment update(Op<GuideGroup> op) {
        return new GuideEnvironment(guideGroups.update(op));
    }

    public String mkString(String prefix, String sep, String suffix) {
        return guideGroups.mkString(prefix, sep, suffix);
    }

    @Override
    public String toString() {
        return mkString("[", ", ", "]");
    }

    /**
     * A convenience method for updating a guide group's GuideProbeTargets.
     * Returns a copy of this GuideEnvironment after having included or updated
     * the given GuideProbeTargets in the indicated GuideGroup.
     */
    public GuideEnvironment putGuideProbeTargets(GuideGroup grp, GuideProbeTargets gpt) {
        // Find the index of the group in the list of options, if it exists in
        // the guide environment.  Otherwise, will be -1.
        int index = getOptions().indexOf(grp);

        // Update the group with the new guide probe targets.
        grp = grp.put(gpt);

        // Update the guide environment.
        return (index == -1) ? update(appendAsPrimary(grp)) :
                setOptions(getOptions().updated(index, grp));
    }

    public static final String PARAM_SET_NAME = "guideEnv";

    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);

        // Guide groups.
        Option<Integer> primary = guideGroups.getPrimaryIndex();
        if (!primary.isEmpty()) {
            Pio.addIntParam(factory, paramSet, "primary", primary.getValue());
        }
        for (GuideGroup grp : guideGroups) {
            paramSet.addParamSet(grp.getParamSet(factory));
        }

        return paramSet;
    }

    public static GuideEnvironment fromParamSet(ParamSet parent) {
        // Guide groups.
        final int primary = Pio.getIntValue(parent, "primary", -1);
        final Option<Integer> primaryOpt = (primary < 0) ? None.INTEGER : new Some<>(primary);

        final List<GuideGroup> groups = new ArrayList<>();
        for (ParamSet gps : parent.getParamSets()) {
            groups.add(GuideGroup.fromParamSet(gps));
        }

        return create(OptionsListImpl.create(primaryOpt, DefaultImList.create(groups)));
    }

}
