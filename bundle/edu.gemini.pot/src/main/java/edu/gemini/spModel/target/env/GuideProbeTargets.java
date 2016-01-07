package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeMap;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.SPTargetPio;

import java.io.Serializable;
import java.util.*;



/**
 * An immutable pairing of a {@link GuideProbe} with an {@link OptionsList}
 * &lt;{@link SPTarget}&gt;, which is a list of targets and an optional
 * target designated as the primary target.
 *
 * GuideTargets are enabled when the associated {@link GuideProbe} is
 * provided by a {@link edu.gemini.spModel.guide.GuideProbeProvider} in the
 * context of the observation, and disabled otherwise.
 *
 * <p>Note, the guide targets list itself is immutable but unfortunately the
 * {@link SPTarget} objects it contains are mutable.
 */
public final class GuideProbeTargets implements Serializable, TargetContainer, OptionsList<SPTarget> {

    /**
     * An empty list of GuideProbeTargets.  This is the same object as
     * {@link edu.gemini.shared.util.immutable.ImCollections#EMPTY_LIST}, but
     * typed for convenience.
     */
    public static final ImList<GuideProbeTargets> EMPTY_LIST = ImCollections.emptyList();

    /**
     * A Comparator that sorts GuideProbeTargets based upon the
     * {@link GuideProbe} that they contain.
     */
    public enum GuiderComparator implements Serializable, Comparator<GuideProbeTargets> {
        instance;

        @Override public int compare(final GuideProbeTargets gt1, final GuideProbeTargets gt2) {
            return GuideProbe.KeyComparator.instance.compare(gt1.getGuider(), gt2.getGuider());
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
     * Creates a GuideTargets instance associated with the given
     * {@link GuideProbe}.  It has no primary star if no targets are provided,
     * otherwise it selects the first target in the list.
     *
     * @param guider guide probe to associate with the GuideTargets
     * @param targets zero or more targets to associate with the guide probe
     *
     * @return a new GuideTargets object with the given targets
     */
    public static GuideProbeTargets create(final GuideProbe guider, final SPTarget... targets) {
        return new GuideProbeTargets(guider, OptionsListImpl.create(targets));
    }

    /**
     * Creates a GuideTargets instance associated with the given
     * {@link GuideProbe}.  It has no primary star if no targets are provided
     * (i.e., <code>targetList.isEmpty()</code>, otherwise it selects the first
     * target in the list.
     *
     * @param guider guider to associate with these targets
     * @param targetList zero or more targets to associate with the guide probe
     *
     * @return a new GuideTargets object with the given targets
     */
    public static GuideProbeTargets create(final GuideProbe guider, final ImList<SPTarget> targetList) {
        return new GuideProbeTargets(guider, OptionsListImpl.create(targetList));
    }

    /**
     * Creates a GuideTargets instance associated with the given
     * {@link GuideProbe} and target options.
     *
     * @param guider guider to associate with these targets
     * @param targets target options to associate with the guide probe
     *
     * @return a new GuideTargets object with the given targets
     */
    public static GuideProbeTargets create(final GuideProbe guider, final OptionsList<SPTarget> targets) {
        return new GuideProbeTargets(guider, targets);
    }


    private final GuideProbe guider;
    private final OptionsList<SPTarget> targetOptions;

    private GuideProbeTargets(GuideProbe guider, OptionsList<SPTarget> targetOptions) {
        if (guider == null) {
            throw new IllegalArgumentException("missing guider");
        }
        if (targetOptions == null) {
            throw new IllegalArgumentException("missing target options");
        }

        this.guider = guider;
        this.targetOptions = targetOptions;
    }

    public GuideProbe getGuider() {
        return guider;
    }

    public boolean containsTargets() {
        return targetOptions.getOptions().nonEmpty();
    }

    @Override
    public boolean containsTarget(final SPTarget target) {
        return targetOptions.getOptions().contains(target);
    }

    @Override
    public ImList<SPTarget> getTargets() {
        return targetOptions.getOptions();
    }

    @Override
    public GuideProbeTargets cloneTargets() {
        Option<Integer>   primaryIndex = targetOptions.getPrimaryIndex();
        ImList<SPTarget> clonedTargets = targetOptions.getOptions().map(SPTarget::clone);
        OptionsListImpl<SPTarget> clone = OptionsListImpl.create(primaryIndex, clonedTargets);
        return new GuideProbeTargets(guider, clone);
    }

    @Override
    public GuideProbeTargets removeTarget(final SPTarget target) {
        return update(UpdateOps.remove(target));
    }

    @Override
    public Iterator<SPTarget> iterator() {
        return targetOptions.iterator();
    }

    public Option<SPTarget> getPrimary() {
        return targetOptions.getPrimary();
    }

    @Override
    public GuideProbeTargets selectPrimary(final Option<SPTarget> primary) {
        return new GuideProbeTargets(guider, targetOptions.selectPrimary(primary));
    }

    @Override
    public GuideProbeTargets selectPrimary(final SPTarget primary) {
        return new GuideProbeTargets(guider, targetOptions.selectPrimary(primary));
    }

    @Override
    public GuideProbeTargets setPrimary(final SPTarget primary) {
        return new GuideProbeTargets(guider, targetOptions.setPrimary(primary));
    }

    @Override
    public Option<Integer> getPrimaryIndex() {
        return targetOptions.getPrimaryIndex();
    }

    @Override
    public GuideProbeTargets setPrimaryIndex(final Option<Integer> primary) {
        return new GuideProbeTargets(guider, targetOptions.setPrimaryIndex(primary));
    }

    @Override
    public GuideProbeTargets setPrimaryIndex(final int primary) {
        return new GuideProbeTargets(guider, targetOptions.setPrimaryIndex(primary));
    }

    @Override
    public ImList<SPTarget> getOptions() {
        return targetOptions.getOptions();
    }

    @Override
    public GuideProbeTargets setOptions(final ImList<SPTarget> newList) {
        return new GuideProbeTargets(guider, targetOptions.setOptions(newList));
    }

    @Override
    public GuideProbeTargets update(final Option<Integer> primaryIndex, final ImList<SPTarget> list) {
        return new GuideProbeTargets(guider, targetOptions.update(primaryIndex, list));
    }

    @Override
    public GuideProbeTargets update(final Op<SPTarget> op) {
        return new GuideProbeTargets(guider, targetOptions.update(op));
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final GuideProbeTargets that = (GuideProbeTargets) o;

        if (!guider.equals(that.guider)) return false;
        return targetOptions.equals(that.targetOptions);
    }

    @Override public int hashCode() {
        int result = guider.hashCode();
        result = 31 * result + targetOptions.hashCode();
        return result;
    }


    public static final String PARAM_SET_NAME = "guider";

    public ParamSet getParamSet(final PioFactory factory) {
        final ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);
        Pio.addParam(factory, paramSet, "key", getGuider().getKey());
        targetOptions.getPrimaryIndex().foreach(idx -> Pio.addIntParam(factory, paramSet, "primary", idx));
        targetOptions.getOptions().foreach(target -> paramSet.addParamSet(target.getParamSet(factory)));
        return paramSet;
    }

    public static GuideProbeTargets fromParamSet(final ParamSet parent) {
        final String key = Pio.getValue(parent, "key");
        final GuideProbe probe = GuideProbeMap.instance.get(key);
        if (probe == null) return null;

        final int primaryIndex = Pio.getIntValue(parent, "primary", -1);
        final Option<Integer> primary = (primaryIndex >= 0) ? new Some<>(primaryIndex) : None.INTEGER;

        final List<SPTarget> lst = new ArrayList<>();
        parent.getParamSets(SPTargetPio.PARAM_SET_NAME).forEach(ps -> lst.add(SPTarget.fromParamSet(ps)));
        final ImList<SPTarget> targets = DefaultImList.create(lst);

        return new GuideProbeTargets(probe, OptionsListImpl.create(primary, targets));
    }

    public String mkString(final String prefix, final String sep, final String suffix) {
        return prefix + "guider=" + guider + sep + "targets=" + targetOptions.mkString(prefix, sep, suffix) + suffix;
    }

    public String toString() {
        return mkString("[", ", ", "]");
    }
}