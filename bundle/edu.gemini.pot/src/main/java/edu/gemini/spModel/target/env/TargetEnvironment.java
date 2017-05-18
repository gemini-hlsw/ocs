package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.SPTarget;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.*;

/**
 * Gets the collection of targets associated with an observation.  The target
 * enviornment houses the asterism of the observation, which contains the
 * science targets themselves, the collection of guide stars organized by
 * {@link GuideProbe} and grouped, and a list of optional "user" targets.
 *
 * <p>User targets are used for blind offseting in the TCC (better support for
 * which is needed here).
 *
 * <p>A TargetEnvironment always has an asterism, but may or may not have
 * guide stars and user targets.
 *
 * <p>A TargetEnvironment is immutable, but the {@link SPTarget}s it contains
 * are unfortunately mutable.
 */
public final class TargetEnvironment implements Serializable, Iterable<SPTarget>, TargetContainer {

    public static final String BASE_NAME = "Base";
    public static final String USER_NAME = "User";

    /**
     * Creates a TargetEnvironment with a single-target asterism but no other
     * associated stars.
     *
     * @param base the science or main target for the observation
     */
    public static TargetEnvironment create(SPTarget base) {
        return create(new Asterism.Single(base));
    }

  /**
   * Creates a TargetEnvironment with a single-target asterism but no other
   * associated stars.
   *
   * @param asterism the asterism for the observation
   */
    public static TargetEnvironment create(Asterism asterism) {
      ImList<SPTarget> user = ImCollections.emptyList();
      return new TargetEnvironment(asterism, GuideEnvironment$.MODULE$.Initial(), user);
    }

  /**
     * Creates a new TargetEnvironment with the given single-target asterism, guide
     * stars, and user targets. No references to the guide or user targets are
     * maintained, though it will share the references to the {@link SPTarget}s
     * themselves.
     *
     * @param base science or main target for the observation
     *
     * @param guide collection of guide targets for the environment
     *
     * @param user list of user targets
     */
    @Deprecated
    public static TargetEnvironment create(SPTarget base, GuideEnvironment guide, ImList<SPTarget> user) {
        return new TargetEnvironment(new Asterism.Single(base), guide, user);
    }

    private final Asterism asterism;
    private final GuideEnvironment guide;
    private final ImList<SPTarget> user;

    // Cached list of all the targets in this environment.  As a
    // SoftReference, this cache may disappear when memory is tight
    private transient SoftReference<ImList<SPTarget>> allTargets;

    private TargetEnvironment(Asterism asterism, GuideEnvironment guide, ImList<SPTarget> user) {
        if (asterism == null) throw new IllegalArgumentException("asterism = null");
        if (guide    == null) throw new IllegalArgumentException("guide = null");
        if (user     == null) throw new IllegalArgumentException("user targets = null");
        this.asterism = asterism;
        this.guide    = guide;
        this.user     = user;
    }

    /** Returns an arbitrary target from the asterism. This method will be removed for 18A. */
    @Deprecated
    public SPTarget getArbitraryTargetFromAsterism() {
      return getAsterism().allSpTargets().head();
    }

    /**
     * Gets the asterism, or science targets in this environment.  This method
     * will never return <code>null</code> because the environment always has
     * to contain an asterism.
     */
    public Asterism getAsterism() {
      return asterism;
    }

    /**
     * Gets the collection of {@link GuideGroup}s in this target environment.
     *
     * @return the {@link GuideEnvironment} in this target environment
     */
    public GuideEnvironment getGuideEnvironment() {
        return guide;
    }

    /**
     * Extracts the primary guide group from this target environment, if any,
     * returning an empty {@link GuideGroup} if none. This is a convenience
     * method and is equivalent to
     * <code>getGuideEnvironment().getPrimary().getOrElse(GuideGroup.EMPTY)</code>.
     */
    public GuideGroup getPrimaryGuideGroup() {
        return guide.getPrimary();
    }

    /**
     * Returns an equivalent TargetEnvironment that uses the given group as its
     * primary guide group, replacing any existing primary guide group that it
     * might have.  This is a convenience method that is equivalent to
     * <pre>
     *     TargetEnvironment env = ...
     *     env.setGuideEnvironment(env.getGuideEnvironment().withManualPrimary(grp))
     * </pre>
     *
     * @param grp new primary guide group
     *
     * @return a new TargetEnvironment that is identical to this one, but that
     * uses the provided {@link GuideGroup} as its primary guide group
     */
    public TargetEnvironment setPrimaryGuideGroup(GuideGroup grp) {
        return setGuideEnvironment(guide.setPrimary(grp));
    }

    /**
     * Extracts the {@link GuideProbeTargets} associated with the given guider
     * in the primary {@link GuideGroup}, if any.  This is a convenience method
     * equivalent to
     * <pre>
     *     TargetEnvironment env = ...
     *     env.getPrimaryGuideGroup().get(guider)
     * </pre>
     */
    public Option<GuideProbeTargets> getPrimaryGuideProbeTargets(final GuideProbe guider) {
        return getPrimaryGuideGroup().get(guider);
    }

    /**
     * Returns an equivalent TargetEnvironment that contains the given
     * GuideProbeTargets in its primary group, in place of any existing
     * GuideProbeTargets associated with the {@link GuideProbe} in
     * <code>gpt</code>.  This is a convenience method equivalent to
     * <pre>
     *     TargetEnvironment env = ...
     *     env.setPrimaryGuideGroup(env.getPrimaryGuideGroup().put(gpt))
     * </pre>
     *
     * @param gpt guide probe targets to add or update in the primary guide
     * group
     *
     * @return a new TargetEnvironment with the given guide probe targets in
     * its primary group, but otherwise identical to <code>this</code>
     */
    public TargetEnvironment putPrimaryGuideProbeTargets(GuideProbeTargets gpt) {
        return setPrimaryGuideGroup(getPrimaryGuideGroup().put(gpt));
    }

    /**
     * Returns an equivalent TargetEnvironment that contains the given
     * collection of GuideProbeTargets in its primary group, replacing any and
     * all targets that may already be there.  This is a convenience method
     * that tis equivalent to
     * <pre>
     *     TargetEnvironment env = ...
     *     env.setPrimaryGuideGroup(env.getPrimaryGuideGroup().setAll(lst))
     * </pre>
     *
     * @param lst new list of GuideProbeTargets to apply to the primary guide
     * group of this target environment
     *
     * @return a new TargetEnvironment with the given guide probe targets in
     * its primary group, but otherwise identical to <code>this</code>
     */
    public TargetEnvironment setAllPrimaryGuideProbeTargets(ImList<GuideProbeTargets> lst) {
        return setPrimaryGuideGroup(getPrimaryGuideGroup().setAll(lst));
    }

    /**
     * Gets the list of user targets in this environment if any.
     *
     * @return user targets in the environment or an empty list if none
     */
    public ImList<SPTarget> getUserTargets() {
        return user;
    }

    @Override
    public boolean containsTarget(SPTarget target) {
        return getTargets().contains(target);
    }

    /**
     * Gets a list of all the targets in this environment, in the order
     *
     * <ol>
     * <li>base position</li>
     * <li>guide targets, sorted by guide target key</li>
     * <li>user targets</li>
     * </ol>
     *
     * @return ordered list of all the {@link SPTarget}s in this environment
     */
    @Override
    public ImList<SPTarget> getTargets() {
        // First try to use the cached value.
        ImList<SPTarget> res = null;
        if (allTargets != null) res = allTargets.get();

        // Doesn't exist yet/anymore so initialize it.
        if (res == null) {
            res = asterism.allSpTargetsJava().append(guide.getTargets()).append(user);
            allTargets = new SoftReference<>(res);
        }
        return res;
    }

    /**
     * Gets a list of all the guide groups in this environment
     */
    public ImList<GuideGroup> getGroups() {
        return guide.getOptions();
    }

    /**
     * Iterates over all the {@link SPTarget}s in this environment.
     */
    public Iterator<SPTarget> iterator() {
        return getTargets().iterator();
    }

    /**
     * Returns <code>true</code> if the given target is a user position in this
     * environment.
     */
    public boolean isUserPosition(SPTarget target) {
        return user.contains(target);
    }

    /**
     * Returns <code>true</code> if the given target is a guide star in this
     * environment.
     */
    public boolean isGuidePosition(final SPTarget target) {
        return guide.containsTarget(target);
    }

    /**
     * Creates an identical TargetEnvironment but with cloned {@link SPTarget}s.
     * This can be important in some contexts because the SPTargets are
     * mutable.
     */
    @Override
    public TargetEnvironment cloneTargets() {
        final Asterism clonedAsterism = asterism.copyWithClonedTargets();
        final GuideEnvironment clonedGuide = guide.cloneTargets();
        final ImList<SPTarget> clonedUser = user.map(SPTarget::clone);
        return new TargetEnvironment(clonedAsterism, clonedGuide, clonedUser);
    }

    /**
     * Returns a TargetEnvironment equivalent to this one, but without the
     * given target.
     */
    @Override
    public TargetEnvironment removeTarget(SPTarget target) {
        return setGuideEnvironment(guide.removeTarget(target)).setUserTargets(user.remove(target));
    }

    public TargetEnvironment removeGroup(int groupIndex) {
        return setGuideEnvironment(guide.removeGroup(groupIndex));
    }

    public TargetEnvironment setGroup(int groupIndex, GuideGroup grp) {
        return setGuideEnvironment(guide.setGroup(groupIndex, grp));
    }

    /**
     * Creates an identical TargetEnvironment but with a single-target asterism
     * in the place of this environment's asterism.
     */
    @Deprecated
    public TargetEnvironment setBasePosition(SPTarget target) {
        return setAsterism(new Asterism.Single(target));
    }

    /**
     * Creates an identical TargetEnvironment but with the given asterism
     * in the place of this environment's asterism.
     */
    public TargetEnvironment setAsterism(Asterism asterism) {
      return new TargetEnvironment(asterism, guide, user);
    }

  /**
     * Creates an identical TargetEnvironment but with the given
     * GuideEnvironment.
     */
    public TargetEnvironment setGuideEnvironment(GuideEnvironment genv) {
        return new TargetEnvironment(asterism, genv, user);
    }

    /**
     * Creates an identical TargetEnvironment but with the given user target
     * list.
     */
    public TargetEnvironment setUserTargets(ImList<SPTarget> userTargets) {
        if (userTargets == this.user) return this;
        return new TargetEnvironment(asterism, guide, userTargets);
    }


    public static final String PARAM_SET_NAME = "targetEnv";

    public ParamSet getParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);

        // Add the asterism.
        paramSet.addParamSet(
          Asterism$.MODULE$.AsterismParamSetCodec().encode("asterism", getAsterism())
        );

        // Add each guider list
        paramSet.addParamSet(guide.getParamSet(factory));

        // Add the user targets.
        if (user.size() > 0) {
            ParamSet userPset = factory.createParamSet("userTargets");
            for (SPTarget target : user) {
                userPset.addParamSet(target.getParamSet(factory));
            }
            paramSet.addParamSet(userPset);
        }

        return paramSet;
    }

    private static GuideEnvironment parseGuideEnvironment(ParamSet parent) {
        // Add guide information
        final ParamSet guidePset = parent.getParamSet(GuideEnvironment.ParamSetName());
        if (guidePset != null) return GuideEnvironment.fromParamSet(guidePset);

        // Parse the old pre-2010B information into a GuideEnvironment
        final List<ParamSet> guideProbeTargets = parent.getParamSets("guider");
        final List<GuideProbeTargets> lst = new ArrayList<>();
        for (ParamSet ps : guideProbeTargets) {
            final GuideProbeTargets gpt = GuideProbeTargets.fromParamSet(ps);
            if (gpt != null) lst.add(gpt);
        }

        final GuideGroup grp = GuideGroup.create(None.STRING, DefaultImList.create(lst));
        return GuideEnvironment.create(OptionsListImpl.create(new GuideGroup(AutomaticGroup.Disabled$.MODULE$), grp).setPrimaryIndex(1));
    }


    public static TargetEnvironment fromParamSet(ParamSet parent) {

        // Get the asterism
        ParamSet astPset = parent.getParamSet("asterism");
        Asterism asterism = Asterism$.MODULE$.AsterismParamSetCodec().unsafeDecode(astPset);

        GuideEnvironment guide = parseGuideEnvironment(parent);

        // Get the user targets.
        List<SPTarget> userTargets = new ArrayList<>();
        ParamSet userPset = parent.getParamSet("userTargets");
        if (userPset != null) {
            for (ParamSet ps : userPset.getParamSets()) {
                userTargets.add(SPTarget.fromParamSet(ps));
            }
        }

        return new TargetEnvironment(asterism, guide, DefaultImList.create(userTargets));
    }

    public String mkString(String prefix, String sep, String suffix) {
        StringBuilder buf = new StringBuilder();
        buf.append("asterism=").append(asterism);

        buf.append(sep).append("guide:primary=").append(guide.getPrimaryIndex());
        buf.append(sep).append("guide:auto=").append(guide.guideEnv().auto());
        buf.append(sep).append("guide:manual=").append(guide.getOptions().tail().mkString(prefix, sep, suffix));

        if (user.size() > 0) {
            buf.append(sep).append("user=");
            buf.append(user.mkString(prefix, sep, suffix));
        }

        buf.append(suffix);
        return buf.toString();
    }

    public String toString() {
        return mkString("[", ", ", "]");
    }
}
