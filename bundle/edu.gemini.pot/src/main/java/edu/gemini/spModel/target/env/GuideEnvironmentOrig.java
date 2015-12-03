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
 * A GuideEnvironmentOrig tracks the active guiders and a collection of
 * {@link GuideGroup} options.  It is meant to contain all target information
 * related to guiding.
 */
public final class GuideEnvironmentOrig implements Serializable, TargetContainer, OptionsList<GuideGroupOrig> {

    /**
     * An empty GuideEnvironmentOrig.
     */
    public static GuideEnvironmentOrig EMPTY = create(OptionsListImpl.create(GuideGroupOrig.EMPTY_LIST));

    /**
     * Creates a GuideEnvironmentOrig with the given set of GuideGroups.
     */
    public static GuideEnvironmentOrig create(OptionsList<GuideGroupOrig> guideGroups) {
        return new GuideEnvironmentOrig(guideGroups);
    }

    private final OptionsList<GuideGroupOrig> guideGroups;

    private GuideEnvironmentOrig(OptionsList<GuideGroupOrig> guideGroups) {
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
        return guideGroups.getPrimary().map(GuideGroupOrig::getPrimaryReferencedGuiders).getOrElse(new TreeSet<>());
    }

    @Override
    public ImList<SPTarget> getTargets() {
        return guideGroups.getOptions().flatMap(TargetContainer::getTargets);
    }

    @Override
    public boolean containsTarget(SPTarget target) {
        return guideGroups.getOptions().exists(gg -> gg.containsTarget(target));
    }

    private GuideEnvironmentOrig updateGuideGroups(UpdateOp<GuideGroupOrig> f) {
        final ImList<GuideGroupOrig> updatedList = guideGroups.getOptions().map(f);
        final Option<Integer> primary = guideGroups.getPrimaryIndex();
        return new GuideEnvironmentOrig(OptionsListImpl.create(primary, updatedList));
    }

    @Override
    public GuideEnvironmentOrig cloneTargets() {
        return updateGuideGroups(GuideGroupOrig::cloneTargets);
    }

    @Override
    public GuideEnvironmentOrig removeTarget(SPTarget target) {
        return updateGuideGroups(g -> g.removeTarget(target));
    }

    public GuideEnvironmentOrig removeGroup(GuideGroupOrig group) {
        return setOptions(getOptions().remove(group));
    }

    @Override
    public Iterator<GuideGroupOrig> iterator() {
        return guideGroups.iterator();
    }

    @Override
    public Option<GuideGroupOrig> getPrimary() {
        return guideGroups.getPrimary();
    }

    @Override
    public GuideEnvironmentOrig selectPrimary(Option<GuideGroupOrig> primary) {
        return new GuideEnvironmentOrig(guideGroups.selectPrimary(primary));
    }

    @Override
    public GuideEnvironmentOrig selectPrimary(GuideGroupOrig primary) {
        return new GuideEnvironmentOrig(guideGroups.selectPrimary(primary));
    }

    @Override
    public GuideEnvironmentOrig setPrimary(GuideGroupOrig primary) {
        return new GuideEnvironmentOrig(guideGroups.setPrimary(primary));
    }

    @Override
    public Option<Integer> getPrimaryIndex() {
        return guideGroups.getPrimaryIndex();
    }

    @Override
    public GuideEnvironmentOrig setPrimaryIndex(Option<Integer> primary) {
        return new GuideEnvironmentOrig(guideGroups.setPrimaryIndex(primary));
    }

    @Override
    public GuideEnvironmentOrig setPrimaryIndex(int primary) {
        return new GuideEnvironmentOrig(guideGroups.setPrimaryIndex(primary));
    }

    @Override
    public ImList<GuideGroupOrig> getOptions() {
        return guideGroups.getOptions();
    }

    @Override
    public GuideEnvironmentOrig setOptions(ImList<GuideGroupOrig> newList) {
        return new GuideEnvironmentOrig(guideGroups.setOptions(newList));
    }

    @Override
    public GuideEnvironmentOrig update(Option<Integer> primaryIndex, ImList<GuideGroupOrig> options) {
        return new GuideEnvironmentOrig(guideGroups.update(primaryIndex, options));
    }

    @Override
    public GuideEnvironmentOrig update(Op<GuideGroupOrig> op) {
        return new GuideEnvironmentOrig(guideGroups.update(op));
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
     * Returns a copy of this GuideEnvironmentOrig after having included or updated
     * the given GuideProbeTargets in the indicated GuideGroup.
     */
    public GuideEnvironmentOrig putGuideProbeTargets(GuideGroupOrig grp, GuideProbeTargets gpt) {
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
        for (GuideGroupOrig grp : guideGroups) {
            paramSet.addParamSet(grp.getParamSet(factory));
        }

        return paramSet;
    }

    public static GuideEnvironmentOrig fromParamSet(ParamSet parent) {
        // Guide groups.
        final int primary = Pio.getIntValue(parent, "primary", -1);
        final Option<Integer> primaryOpt = (primary < 0) ? None.INTEGER : new Some<>(primary);

        final List<GuideGroupOrig> groups = new ArrayList<>();
        for (ParamSet gps : parent.getParamSets()) {
            groups.add(GuideGroupOrig.fromParamSet(gps));
        }

        return create(OptionsListImpl.create(primaryOpt, DefaultImList.create(groups)));
    }

}
