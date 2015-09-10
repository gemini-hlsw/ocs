//
// $
//

package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.SPTarget;
import static edu.gemini.spModel.target.env.GuideProbeTargets.sortByGuider;
import edu.gemini.spModel.target.env.OptionsList.UpdateOps;

import java.io.Serializable;
import java.util.*;

/**
 * An immutable collection of {@link GuideProbeTargets} with an optional name.
 * GuideGroup holds a collection of {@link GuideProbeTargets}.  It can be
 * thought of and used as a map of {@link GuideProbe} to
 * {@link OptionsListImpl}&lt;{@link SPTarget}&gt;. It forms a
 * configuration of guide targets and the probes that will be used to track
 * them. The idea is to treat the various {@link GuideProbeTargets} that make
 * up the group as a whole, in order to be able to switch one group for another
 * on demand.
 *
 * <p>For example, a GuideGroup might consist of a stars for CWFS[1-3]
 * and a GSAOI ODGW star.  The user might want to use this group for
 * their observation or else second option with a different collection of
 * choices for these probes. It isn't sufficient to have multiple independent
 * options for each probe, since the pairing of the targets themselves is
 * important.
 */
public final class GuideGroup implements Serializable, Iterable<GuideProbeTargets>, TargetContainer {

    /**
     * A singleton empty GuideGroup.
     */
    public static final GuideGroup EMPTY = create(null);

    /**
     * An empty list of {@link GuideGroup}.  This is the same instance as
     * {@link ImCollections#emptyList()}, but typed for convenience.
     */
    public static final ImList<GuideGroup> EMPTY_LIST = ImCollections.emptyList();

    /**
     * Creates a GuideGroup containing zero or more {@link GuideProbeTargets}.
     *
     * @param name name of the group (may be null)
     * @param targets guide probe targets to associate with the environment
     * (if any)
     *
     * @return a GuideGroup with the given {@link GuideProbeTargets}
     */
    public static GuideGroup create(String name, GuideProbeTargets... targets) {
        return create(name, DefaultImList.create(targets));
    }

    /**
     * Creates a named GuideGroup with the given list of {@link GuideProbeTargets}
     *
     * @param name name of the group (may be null)
     *
     * @param targets collection of {@link GuideProbeTargets} in this
     * environment (may be empty)
     *
     * @return a named GuideGroup with the given {@link GuideProbeTargets}
     */
    public static GuideGroup create(String name, ImList<GuideProbeTargets> targets) {
        final Option<String> nameOpt = name == null ? None.<String>instance() : new Some<>(name);
        return create(nameOpt, targets);
    }

    /**
     * Creates a GuideGroup with the given list of {@link GuideProbeTargets} and
     * optional name.
     *
     * @param name optional name of this group
     *
     * @param targets collection of {@link GuideProbeTargets} in this
     * environment
     *
     * @return a GuideGroup with the given {@link GuideProbeTargets} and name
     */
    public static GuideGroup create(Option<String> name, ImList<GuideProbeTargets> targets) {
        return new GuideGroup(name, normalize(targets));
    }

    // GuideGroup maintains an ImList of GuideProbeTargets.  They are kept
    // sorted with no two GuideProbeTargets instances containing the same
    // GuideProbe.  This method "normalizes" an arbitrary list of
    // GuideProbeTargets to satisfy these requirements.  GuideProbeTargets that
    // repeat the same GuideProbe will replace GuideProbeTargets that occur
    // earlier in the provided list of targets.
    private static ImList<GuideProbeTargets> normalize(ImList<GuideProbeTargets> targets) {
        final SortedMap<GuideProbe, GuideProbeTargets> m = new TreeMap<>(GuideProbe.KeyComparator.instance);
        for (GuideProbeTargets gpt : targets) m.put(gpt.getGuider(), gpt);
        return DefaultImList.create(m.values());
    }

    private final ImList<GuideProbeTargets> guideTargets;
    private final Option<String> name;

    private GuideGroup(Option<String> name, ImList<GuideProbeTargets> guideTargets) {
        if (name == null) throw new IllegalArgumentException("name = null");
        if (guideTargets == null) throw new IllegalArgumentException("guideTargets = null");
        this.guideTargets = guideTargets;
        this.name         = name;
    }

    /**
     * Gets the name of the GuideGroup, if any.
     *
     * @return name of the GuideGroup wrapped in a
     * {@link edu.gemini.shared.util.immutable.Some} instance;
     * {@link edu.gemini.shared.util.immutable.None} if none
     */
    public Option<String> getName() { return name; }

    /**
     * Sets (or removes) the name of the GuideGroup, returning a copy of this
     * group with the new name but with the same list of
     * {@link GuideProbeTargets}.
     *
     * @param name new name for the group, if specified; may be
     * {@link None} but not null
     *
     * @return new GuideGroup with the indicated name
     */
    public GuideGroup setName(Option<String> name) {
        return new GuideGroup(name, guideTargets);
    }

    /**
     * Sets (or removes) the name of the GuideGroup, returning a copy of this
     * group with the new name but with the same list of
     * {@link GuideProbeTargets}.
     *
     * @param name new name for the group, if specified; may be
     * <code>null</code>
     *
     * @return new GuideGroup with the indicated name
     */
    public GuideGroup setName(String name) {
        return setName(name == null ? None.STRING : new Some<>(name));
    }


    /**
     * @param guider guide probe whose presence is sought in the group
     * @return <code>true</code> if there is a matching {@link GuideProbeTargets}
     * instance; <code>false</code> otherwise
     */
    public boolean contains(GuideProbe guider) {
        return guideTargets.exists(gpt -> gpt.getGuider() == guider);
    }

    /**
     * Gets the {@link GuideProbeTargets} associated with the given guider, if
     * any.
     *
     * @param guider guider whose targets are sought
     *
     * @return the {@link GuideProbeTargets} associated with this guider
     * wrapped in {@link edu.gemini.shared.util.immutable.Some}, or
     * {@link edu.gemini.shared.util.immutable.None} if none
     */
    public Option<GuideProbeTargets> get(GuideProbe guider) {
        return guideTargets.find(gpt -> gpt.getGuider() == guider);
    }

    /**
     * Creates an identical GuideGroup but with the given GuideProbeTargets.
     * If this GuideGroup already contains guide targets for the
     * associated {@link GuideProbe}, they are replaced with the provided
     * <code>targets</code>.  Otherwise it is added to the TargetEnvironment
     * that is created and returned.
     *
     * @param targets new set of target options with which to add or update this
     * GuideGroup
     *
     * @return new GuideGroup, identical to this one, except containing the
     * given guide probe target options
     */
    public GuideGroup put(GuideProbeTargets targets) {
        final GuideProbe guideProbe = targets.getGuider();
        return new GuideGroup(name, sortByGuider(guideTargets.remove(gpt -> gpt.getGuider() == guideProbe).cons(targets)));
    }

    /**
     * Creates an identical GuideGroup but that contains no
     * {@link GuideProbeTargets entry} associated with the given
     * {@link GuideProbe}.
     *
     * @param guider guide probe whose corresponding GuideProbeTargets entry
     * will be removed in the GuideGroup that is returned
     *
     * @return a new GuideGroup, identical to this one, except without a
     * {@link GuideProbeTargets} entry associated with {@link GuideProbe}
     */
    public GuideGroup remove(GuideProbe guider) {
        final ImList<GuideProbeTargets> lst = guideTargets.remove(gpt -> gpt.getGuider() == guider);
        if (lst.size() == guideTargets.size()) return this;
        return new GuideGroup(name, lst);
    }

    /**
     * @return an empty GuideGroup with the same name as this one
     */
    public GuideGroup clear() {
        if (guideTargets.size() == 0) return this;
        return new GuideGroup(name, GuideProbeTargets.EMPTY_LIST);
    }

    /**
     * Gets the guide probe targets contained in this GuideConfig.
     * @return list of {@link GuideProbeTargets} contained in this GuideConfig
     */
    public ImList<GuideProbeTargets> getAll() {
        return guideTargets;
    }

    /**
     * Creates a GuideGroup with the same name and targets as this one, except
     * that any {@link GuideProbeTargets} contained in the <code>target</code>
     * argument are added to the new GuideGroup, or used to update their
     * counterparts with the same {@link GuideProbe}.  If there are multiple
     * {@link GuideProbeTargets} with the same {@link GuideProbe}, the last
     * one in the list is used.  Use {@link #setAll} to completely replace all
     * targets in the group that is returned.
     *
     * @param targets targets with which to add or update this GuideGroup
     *
     * @return a new GuideGroup, identical to this one, except updated with the
     * given <code>targets</code>
     */
    public GuideGroup putAll(ImList<GuideProbeTargets> targets) {
        return new GuideGroup(name, normalize(guideTargets.append(targets)));
    }

    /**
     * Creates a GuideGroup with the same name as this one, but with the
     * given list of {@link GuideProbeTargets}.  If there are multiple
     * {@link GuideProbeTargets} with the same {@link GuideProbe}, the last
     * one in the list is used.  Use {@link #putAll} to simply add or update
     * the targets in the group.  This method is equivalent to a call to
     * {@link #clear}, followed by a call to {@link #putAll}.
     *
     * @param targets guide probe targets that should be available in the
     * returned GuideGroup
     *
     * @return a new GuideGroup with the same name as this one, but with the
     * given <code>targets</code>
     */
    public GuideGroup setAll(ImList<GuideProbeTargets> targets) {
        return new GuideGroup(name, normalize(targets));
    }

    @Override
    public Iterator<GuideProbeTargets> iterator() {
        return guideTargets.iterator();
    }

    /**
     * Gets the {@link GuideProbeTargets} in which the given <code>target</code>
     * resides, if any.
     *
     * @param target target whose containing GuideProbeTargets are sought
     *
     * @return {@link edu.gemini.shared.util.immutable.Some}<{@link GuideProbeTargets}>
     * if <code>target</code> is a guide target in this environment;
     * <code>{@link edu.gemini.shared.util.immutable.None}</code> otherwise
     */
    public ImList<GuideProbeTargets> getAllContaining(SPTarget target) {
        return guideTargets.filter(gt -> gt.containsTarget(target));
    }

    /**
     * Gets all the {@link GuideProbeTargets} associated with the given type of
     * {@link edu.gemini.spModel.guide.GuideProbe}.
     *
     * @param type guider type of interest
     *
     * @return {@link ImList} of {@link GuideProbeTargets} in this environment
     * with the given <code>type</code>, or an empty collection if none
     */
    public ImList<GuideProbeTargets> getAllMatching(GuideProbe.Type type) {
        return guideTargets.filter(gpt -> gpt.getGuider().getType() == type);
    }


    /**
     * Gets the collection of guiders that have one or more associated targets
     * in this group, sorted using the
     * {@link edu.gemini.spModel.guide.GuideProbe.KeyComparator}.
     *
     * @return list of guiders with one or more associated targets or an empty
     * list if there are none
     */
    private ImList<GuideProbe> getReferencedGuiderList() {
        return guideTargets.filter(GuideProbeTargets::containsTargets).map(GuideProbeTargets::getGuider);
    }

    /**
     * Gets all the {@link GuideProbe guiders} associated with the given type
     * that are used in this TargetEnvironment (if any).
     *
     * @param type guider type of interest
     *
     * @return the {@link GuideProbe guiders} in this environment with the
     * given <code>type</code>, or an empty collection if none.
     */
    private ImList<GuideProbe> getReferencedGuiderList(final GuideProbe.Type type) {
        return getReferencedGuiderList().filter(new GuideProbe.TypeMatcher(type));
    }

    private SortedSet<GuideProbe> toSet(ImList<GuideProbe> probeList) {
        final SortedSet<GuideProbe> res = new TreeSet<>(GuideProbe.KeyComparator.instance);
        res.addAll(probeList.toList());
        return res;
    }

    /**
     * Gets all the {@link GuideProbe guiders} that have one or more associated
     * targets in this group, sorted using the
     * {@link edu.gemini.spModel.guide.GuideProbe.KeyComparator}.
     *
     * @return set of {@link GuideProbe} with one or more associated targets,
     * or an empty set if there are none
     */
    public SortedSet<GuideProbe> getReferencedGuiders() {
        return toSet(getReferencedGuiderList());
    }

    /**
     * Gets the subset of referenced guiders that actually have an associated
     * selected target.
     */
    public SortedSet<GuideProbe> getPrimaryReferencedGuiders() {
        return toSet(guideTargets.filter(gpt -> gpt.getPrimary().isDefined()).map(GuideProbeTargets::getGuider));
    }

    /**
     * Gets all the {@link GuideProbe guiders} associated with the given type
     * that are used in this group (if any).
     *
     * @param type guider type of interest
     *
     * @return the {@link GuideProbe guiders} in this group with the
     * given <code>type</code>, or an empty collection if none.
     */
    public SortedSet<GuideProbe> getReferencedGuiders(GuideProbe.Type type) {
        return toSet(getReferencedGuiderList(type));
    }


    @Override
    public ImList<SPTarget> getTargets() {
        return guideTargets.flatMap(TargetContainer::getTargets);
    }

    @Override
    public boolean containsTarget(SPTarget target) {
        return guideTargets.exists(gt -> gt.containsTarget(target));
    }

    @Override
    public GuideGroup removeTarget(final SPTarget target) {
        final ImList<GuideProbeTargets> updated = guideTargets.map(gpt -> gpt.removeTargetSelectPrimary(target));
        return new GuideGroup(name, updated);
    }

    @Override
    public GuideGroup cloneTargets() {
        final ImList<GuideProbeTargets> cloned = guideTargets.map(GuideProbeTargets::cloneTargets);
        return new GuideGroup(name, cloned);
    }

    public Iterator<SPTarget> iterateAllTargets() {
        return new Iterator<SPTarget>() {
            private Iterator<GuideProbeTargets> it = guideTargets.iterator();
            private Iterator<SPTarget> cur;

            {
                advanceCur();
            }

            private void advanceCur() {
                while (it.hasNext() && !hasNext()) {
                    cur = it.next().iterator();
                }
            }

            @Override public boolean hasNext() {
                return (cur != null) && cur.hasNext();
            }

            @Override public SPTarget next() {
                SPTarget res = cur.next();
                advanceCur();
                return res;
            }

            @Override public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static final String PARAM_SET_NAME = "guideGroup";

    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);

        if (!name.isEmpty()) {
            Pio.addParam(factory, paramSet, "name", name.getValue());
        }
        for (GuideProbeTargets targets : guideTargets) {
            paramSet.addParamSet(targets.getParamSet(factory));
        }

        return paramSet;
    }

    public static GuideGroup fromParamSet(ParamSet parent) {
        String name = Pio.getValue(parent, "name"); // may be null

        List<GuideProbeTargets> lst = new ArrayList<>();
        for (ParamSet ps : parent.getParamSets()) {
            lst.add(GuideProbeTargets.fromParamSet(ps));
        }
        return create(name, DefaultImList.create(lst));
    }

    public String mkString(String prefix, String sep, String suffix) {
        return prefix + "name=\"" + (name.isEmpty() ? "" : name.getValue()) + "\"" + sep +
                        "entries=" + guideTargets.mkString(prefix, sep, suffix) + suffix;
    }

    public String toString() {
        return mkString("[", ", ", "]");
    }
}