//
// $
//

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
 * An immutable pairing of a {@link GuideProbe} with an {@link OptionsList}
 * &lt;{@link SPTarget}&gt;, which is a list of targets and an optional
 * target designated as the primary target.
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
     * Creates a {@link PredicateOp} that can be used with an {@link ImList} of
     * GuideTargets to match on {@link GuideProbe}.  For example, it can be
     * used to find a GuideTargets instance with a particular
     * {@link GuideProbe}:
     * <pre>
     *     ImList<GuideTargets> lst = ...
     *     GuideTargets result = lst.find(match(guider));
     * </pre>
     *
     * @param guider guide probe to match on
     *
     * @return {@link PredicateOp} matching {@link GuideProbeTargets} with the given
     * {@link GuideProbe}
     */
    public static PredicateOp<GuideProbeTargets> match(final GuideProbe guider) {
        return new PredicateOp<GuideProbeTargets>() {
            @Override public Boolean apply(GuideProbeTargets targets) {
                return targets.getGuider() == guider;
            }
        };
    }

    /**
     * Creates a {@link PredicateOp} that can be used with an {@link ImList} of
     * GuideTargets to match on {@link GuideProbe.Type}.  For example, it can
     * be used to filter a list of GuideTargets with a particular
     * {@link GuideProbe.Type}:
     * <pre>
     *     ImList<GuideTargets> lst = ...
     *     ImList<GuideTargets> result = lst.filter(new GuiderTypeMatch(OIWFS));
     * </pre>
     *
     * @param type guide probe type to match on
     *
     * @return {@link PredicateOp} matching {@link GuideProbeTargets} with the given
     * {@link GuideProbe.Type}
     */
    public static PredicateOp<GuideProbeTargets> match(final GuideProbe.Type type) {
        return new PredicateOp<GuideProbeTargets>() {
            @Override public Boolean apply(GuideProbeTargets guideTargets) {
                return guideTargets.getGuider().getType() == type;
            }
        };
    }

    /**
     * A Comparator that sorts GuideProbeTargets based upon the
     * {@link GuideProbe} that they contain.
     */
    public static enum GuiderComparator implements Serializable, Comparator<GuideProbeTargets> {
        instance;

        @Override public int compare(GuideProbeTargets gt1, GuideProbeTargets gt2) {
            GuideProbe g1 = gt1.getGuider();
            GuideProbe g2 = gt2.getGuider();
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
    public static ImList<GuideProbeTargets> sortByGuider(ImList<GuideProbeTargets> lst) {
        return lst.sort(GuiderComparator.instance);
    }


    /**
     * A "function" that extracts the guider from a GuideProbeTarget.  Intended
     * for use with the {@link ImList#map} method.
     */
    public static final Function1<GuideProbeTargets, GuideProbe> EXTRACT_PROBE = new Function1<GuideProbeTargets, GuideProbe>() {
        @Override public GuideProbe apply(GuideProbeTargets gpt) {
            return gpt.getGuider();
        }
    };

    /**
     * A {@link PredicateOp} that matches on GuideProbeTargets instances that
     * have at least one contained {@link SPTarget}.
     */
    public static final PredicateOp<GuideProbeTargets> MATCH_NON_EMPTY = new PredicateOp<GuideProbeTargets>() {
        @Override public Boolean apply(GuideProbeTargets gpt) {
            return gpt.targetOptions.getOptions().size() > 0;
        }
    };

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
    public static GuideProbeTargets create(GuideProbe guider, SPTarget... targets) {
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
    public static GuideProbeTargets create(GuideProbe guider, ImList<SPTarget> targetList) {
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
    public static GuideProbeTargets create(GuideProbe guider, OptionsList<SPTarget> targets) {
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

    /**
     * Gets the guider associated with the list of targets.
     */
    public GuideProbe getGuider() {
        return guider;
    }

    @Override
    public boolean containsTarget(SPTarget target) {
        return targetOptions.getOptions().contains(target);
    }

    @Override
    public ImList<SPTarget> getTargets() {
        return targetOptions.getOptions();
    }

    @Override
    public GuideProbeTargets cloneTargets() {
        Option<Integer>   primaryIndex = targetOptions.getPrimaryIndex();
        ImList<SPTarget> clonedTargets = targetOptions.getOptions().map(SPTarget.CLONE_FUNCTION);
        OptionsListImpl<SPTarget> clone = OptionsListImpl.create(primaryIndex, clonedTargets);
        return new GuideProbeTargets(guider, clone);
    }

    @Override
    public GuideProbeTargets removeTarget(SPTarget target) {
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
    public GuideProbeTargets selectPrimary(Option<SPTarget> primary) {
        return new GuideProbeTargets(guider, targetOptions.selectPrimary(primary));
    }

    @Override
    public GuideProbeTargets selectPrimary(SPTarget primary) {
        return new GuideProbeTargets(guider, targetOptions.selectPrimary(primary));
    }

    @Override
    public GuideProbeTargets setPrimary(SPTarget primary) {
        return new GuideProbeTargets(guider, targetOptions.setPrimary(primary));
    }

    @Override
    public Option<Integer> getPrimaryIndex() {
        return targetOptions.getPrimaryIndex();
    }

    @Override
    public GuideProbeTargets setPrimaryIndex(Option<Integer> primary) {
        return new GuideProbeTargets(guider, targetOptions.setPrimaryIndex(primary));
    }

    @Override
    public GuideProbeTargets setPrimaryIndex(int primary) {
        return new GuideProbeTargets(guider, targetOptions.setPrimaryIndex(primary));
    }

    @Override
    public ImList<SPTarget> getOptions() {
        return targetOptions.getOptions();
    }

    @Override
    public GuideProbeTargets setOptions(ImList<SPTarget> newList) {
        return new GuideProbeTargets(guider, targetOptions.setOptions(newList));
    }

    @Override
    public GuideProbeTargets update(Option<Integer> primaryIndex, ImList<SPTarget> list) {
        return new GuideProbeTargets(guider, targetOptions.update(primaryIndex, list));
    }

    @Override
    public GuideProbeTargets update(Op<SPTarget> op) {
        return new GuideProbeTargets(guider, targetOptions.update(op));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GuideProbeTargets that = (GuideProbeTargets) o;

        if (!guider.equals(that.guider)) return false;
        return targetOptions.equals(that.targetOptions);
    }

    @Override
    public int hashCode() {
        int result = guider.hashCode();
        result = 31 * result + targetOptions.hashCode();
        return result;
    }


    public static final String PARAM_SET_NAME = "guider";

    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);

        Pio.addParam(factory, paramSet, "key", getGuider().getKey());
        if (!targetOptions.getPrimaryIndex().isEmpty()) {
            Pio.addIntParam(factory, paramSet, "primary", targetOptions.getPrimaryIndex().getValue());
        }
        for (SPTarget target : this) {
            paramSet.addParamSet(target.getParamSet(factory));
        }

        return paramSet;
    }

    public static GuideProbeTargets fromParamSet(ParamSet parent) {
        String key = Pio.getValue(parent, "key");
        GuideProbe probe = GuideProbeMap.instance.get(key);
        if (probe == null) return null;

        int primaryIndex = Pio.getIntValue(parent, "primary", -1);
        Option<Integer> primary = (primaryIndex>=0) ? new Some<>(primaryIndex) : None.INTEGER;

        final List<SPTarget> lst = new ArrayList<>();
        for (ParamSet ps : parent.getParamSets()) {
            SPTarget target = SPTarget.fromParamSet(ps);
            lst.add(target);
        }
        ImList<SPTarget> targets = DefaultImList.create(lst);

        return new GuideProbeTargets(probe, OptionsListImpl.create(primary, targets));
    }

    public String mkString(String prefix, String sep, String suffix) {
        return prefix + "guider=" + guider + sep + "targets=" + targetOptions.mkString(prefix, sep, suffix) + suffix;
    }

    public String toString() {
        return mkString("[", ", ", "]");
    }
}
