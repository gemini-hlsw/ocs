package edu.gemini.spModel.target.obsComp;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.config.ConfigPostProcessor;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;

import java.util.*;
import java.util.stream.Collectors;

public final class GuideSequence implements ConfigPostProcessor {
    private static final String SYSTEM_NAME       = TargetObsCompConstants.CONFIG_NAME;
    private static final ItemKey PARENT_KEY       = new ItemKey(SYSTEM_NAME);
    public static final String  GUIDE_STATE_PARAM = "tmpGuideState";
    public static final ItemKey GUIDE_STATE_KEY   = new ItemKey(PARENT_KEY, GUIDE_STATE_PARAM);

    /**
     * An explicit override for a particular guide probe.  In general, the
     * default setting is applied to all guide probes at a given step.  The
     * user may wish to override the default setting for a particular probe,
     * however.
     */
    public static final class ExplicitGuideSetting {
        public final GuideProbe probe;
        public final GuideOption option;

        public ExplicitGuideSetting(GuideProbe probe, GuideOption option) {
            if (probe == null) throw new IllegalArgumentException("probe == null");
            if (option == null) throw new IllegalArgumentException("option == null");

            this.probe  = probe;
            this.option = option;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final ExplicitGuideSetting that = (ExplicitGuideSetting) o;
            if (!option.equals(that.option)) return false;
            return probe.equals(that.probe);
        }

        @Override public int hashCode() {
            int result = probe.hashCode();
            result = 31 * result + option.hashCode();
            return result;
        }

        @Override public String toString() {
            return String.format("(%s, %s)", probe.getKey(), option.name());
        }
    }

    /**
     * The guide state for a particular step.  It includes the default value
     * for any probes that are not mentioned along with any guide configuration
     * for explicitly overridden probes.
     */
    public static final class GuideState {
        public static final GuideState DEFAULT_ON  = new GuideState(DefaultGuideOptions.Value.on);
        public static final GuideState DEFAULT_OFF = new GuideState(DefaultGuideOptions.Value.off);

        public static GuideState forDefaultOption(DefaultGuideOptions.Value val) {
            return (val == DEFAULT_ON.defaultState) ? DEFAULT_ON : DEFAULT_OFF;
        }

        public final DefaultGuideOptions.Value defaultState;
        public final ImList<ExplicitGuideSetting> overrides;

        public GuideState(DefaultGuideOptions.Value defaultState) {
            this(defaultState, ImCollections.emptyList());
        }

        public GuideState(DefaultGuideOptions.Value defaultState, ImList<ExplicitGuideSetting> overrides) {
            if (defaultState == null) throw new IllegalArgumentException("defaultState == null");
            if (overrides    == null) throw new IllegalArgumentException("overrides == null");
            this.defaultState = defaultState;
            this.overrides    = overrides;
        }


        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final GuideState that = (GuideState) o;
            if (defaultState != that.defaultState) return false;
            return overrides.equals(that.overrides);
        }

        @Override public int hashCode() {
            int result = defaultState.hashCode();
            result = 31 * result + overrides.hashCode();
            return result;
        }

        @Override public String toString() {
            return String.format("(%s, %s)", defaultState, overrides.mkString("(", ", ", ")"));
        }
    }

    /**
     * Gets the collection of guiders that are required in order to perform the
     * observation.  A guide probe is required if there is a guide star marked
     * "primary" assigned to it in the primary guide group.
     */
    public static ImList<GuideProbe> getRequiredGuiders(Option<TargetEnvironment> envOpt) {
        return envOpt.map(env -> env.getPrimaryGuideGroup().getAll().
                        filter(gpt -> gpt.getPrimary().isDefined()).
                        map(GuideProbeTargets::getGuider)).getOrElse(ImCollections.emptyList());
    }

    /**
     * Gets all the guiders that should be parked for the duration of this
     * observation.  Those would be the guiders available in the current
     * context and yet not assigned to any guide star.  This is a hack-around
     * for the brain dead seqexec which should be handling this itself.
     */
    public static ImList<GuideProbe> getPermanentlyParkedGuiders(Option<ObsContext> ctxOpt, ImList<GuideProbe> req) {
        final Set<GuideProbe> required  = new TreeSet<>(GuideProbe.KeyComparator.instance);
        for (GuideProbe g : req) required.add(g);

        // What's available in this environment?
        final Set<GuideProbe> available = new TreeSet<>(GuideProbe.KeyComparator.instance);
        ctxOpt.foreach(c -> available.addAll(GuideProbeUtil.instance.getAvailableGuiders(c)));

        // But we only care about parkable guiders.
        available.stream().filter(g -> !(g.getGuideOptions() instanceof StandardGuideOptions)).forEach(available::remove);

        // Always have to park the PWFS? even if not available.
        available.addAll(Arrays.asList(PwfsGuideProbe.values()));

        // Finally, if avaliable but unused, park it.
        final Collection<GuideProbe> parked = new ArrayList<>(available.size());
        parked.addAll(available.stream().filter(g -> !required.contains(g)).collect(Collectors.toList()));

        return DefaultImList.create(parked);
    }


    public static Map<ItemKey, GuideOption> getGuideWithParked(Option<ObsContext> ctx, ImList<GuideProbe> requiredGuiders) {
        final ImList<GuideProbe> parked = getPermanentlyParkedGuiders(ctx, requiredGuiders);

        // Remember any sequence props whose values will be set because they
        // involved required guiders.
        final Set<ItemKey> handled = new HashSet<>();
        for (GuideProbe g : requiredGuiders) {
            handled.add(new ItemKey(PARENT_KEY, g.getSequenceProp()));
        }

        // Now for any unused guiders whose sequence property doesn't appear in
        // the handled set, record it as parked / off.
        final Map<ItemKey, GuideOption> res = new HashMap<>();
        for (GuideProbe g : parked) {
            final ItemKey key = new ItemKey(PARENT_KEY, g.getSequenceProp());
            if (!handled.contains(key)) {
                res.put(key, g.getGuideOptions().getDefaultOff());
            }
        }

        return res;
    }

    // Another seqexec hack here -- it cannot use the Altair+PWFS1 setting and
    // figure out that it needs to have the AOWFS guide on a LGS so we have to
    // hack this into the sequence for it. So, when we find that something has
    // already set a guideWith* parameter, we'll respect it (since the Altair
    // component will have to do this for the seqexec).
    private static Map<ItemKey, GuideOption> seqexecHack(Map<ItemKey, GuideOption> parked, Config step) {
        // filter out any existing guideWith* parameters that appear in the
        // config step.
        final Set<ItemKey> rmSet = parked.keySet().stream().filter(step::containsItem).collect(Collectors.toSet());
        if (rmSet.size() == 0) {
            return parked;
        } else {
            final Map<ItemKey, GuideOption> updated = new HashMap<>(parked);
            rmSet.forEach(updated::remove);
            return updated;
        }
    }

    public static Map<ItemKey, GuideOption> getGuideWith(GuideState state, ImList<GuideProbe> requiredGuiders) {
        final Map<ItemKey, GuideOption> res = new HashMap<>();

        // First, set all to the default value.
        for (GuideProbe g : requiredGuiders) {
            final ItemKey key = new ItemKey(PARENT_KEY, g.getSequenceProp());
            res.put(key, g.getGuideOptions().fromDefaultGuideOption(state.defaultState));
        }

        // Then override any as necessary.
        for (ExplicitGuideSetting o : state.overrides) {
            if (!requiredGuiders.contains(o.probe)) continue;
            final ItemKey key = new ItemKey(PARENT_KEY, o.probe.getSequenceProp());
            res.put(key, o.option);
        }

        return res;
    }


    private final Option<ObsContext> ctx;

    public GuideSequence(Option<ObsContext> ctx) {
        this.ctx = ctx;
    }


    @Override public ConfigSequence postProcessSequence(ConfigSequence in) {
        final ImList<GuideProbe> requiredGuiders = getRequiredGuiders(ctx.flatMap(c -> ImOption.apply(c.getTargets())));
        final Config[] steps = in.getAllSteps();

        // Handle parking unused probes for the brain-dead seqexec.
        if (steps.length > 0) {
            Map<ItemKey, GuideOption> parked = seqexecHack(getGuideWithParked(ctx, requiredGuiders), steps[0]);
            applyGuideWith(parked, steps[0]);
            gmosNsHack(steps[0], requiredGuiders);
        }

        // Now go through each step, remove the guide state value and replace it
        // with the appropriate settings for guideWith
        for (final Config step : steps) {
            final GuideState guideState = (GuideState) step.remove(GUIDE_STATE_KEY);
            if (guideState == null) continue;
            applyGuideWith(getGuideWith(guideState, requiredGuiders), step);
        }

        return new ConfigSequence(steps);
    }

    private static void applyGuideWith(Map<ItemKey, GuideOption> guideWith, Config step) {
        for (final Map.Entry<ItemKey, GuideOption> me : guideWith.entrySet()) {
            step.putItem(me.getKey(), me.getValue());
        }
    }

    // FR 30444: GMOS N&S offset position guideWith hack.
    private static final ItemKey GMOS_NS_A = new ItemKey(SeqConfigNames.INSTRUMENT_KEY, "nsBeamA-guideWithOIWFS");
    private static final ItemKey GMOS_NS_B = new ItemKey(SeqConfigNames.INSTRUMENT_KEY, "nsBeamB-guideWithOIWFS");
    private void gmosNsHack(Config step, ImList<GuideProbe> guiders) {
        // First, if we aren't dealing with GMOS N&S there is nothing to do.
        Object optA = step.getItemValue(GMOS_NS_A);
        if (optA == null) return;

        boolean hasOIWFS = guiders.contains(GmosOiwfsGuideProbe.instance);
        Object optB = step.getItemValue(GMOS_NS_B);
        step.putItem(GMOS_NS_A, hasOIWFS ? optA : StandardGuideOptions.Value.park);
        step.putItem(GMOS_NS_B, hasOIWFS ? optB : StandardGuideOptions.Value.park);
    }
}
