package edu.gemini.spModel.gemini.gsaoi;

import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.gems.GemsGuideProbeGroup;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.SchedulingBlock;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.OptionsList.UpdateOps;
import edu.gemini.spModel.target.env.OptionsListImpl;
import edu.gemini.spModel.target.env.TargetEnvironment;
import scalaz.Alpha;

import java.awt.geom.Area;
import java.util.*;

/**
 * On-detector guide window guiders.
 */
public enum GsaoiOdgw implements ValidatableGuideProbe {
    odgw1(GsaoiDetectorArray.Id.one),
    odgw2(GsaoiDetectorArray.Id.two),
    odgw3(GsaoiDetectorArray.Id.three),
    odgw4(GsaoiDetectorArray.Id.four),
    ;

    /**
     * Guide options for GSAOI ODGW.  The can be turned on or off, but there
     * is no equivalent of the standard guide probe option of
     * {@link edu.gemini.spModel.guide.StandardGuideOptions} freeze.
     */
    /*
    public enum State implements GuideOption {
        active() {
            public boolean isFrozen() { return false; }
            public boolean isActive() { return true;  }
        },
        inactive() {
            public boolean isFrozen() { return false; }
            public boolean isActive() { return false; }
        },
        ;

        public static List<GuideOption> getOptions() {
            return Arrays.asList((GuideOption[]) values());
        }
    }
    */

    /**
     * Group of GsaoiOdgw, with support for selecting and optimizing
     * {@link TargetEnvironment target environments}.
     */
    public enum Group implements SelectableGuideProbeGroup, OptimizableGuideProbeGroup, GemsGuideProbeGroup {
        instance;

        public String getKey() {
            return "ODGW";
        }

        public String getDisplayName() {
            return "On-detector Guide Window";
        }

        public Collection<ValidatableGuideProbe> getMembers() {
            ValidatableGuideProbe[] vals = GsaoiOdgw.values();
            return Arrays.asList(vals);
        }

        public Option<GuideProbe> select(Coordinates guideStar, ObsContext ctx) {
            // Get the id of the detector in which the guide star lands, if any
            Option<GsaoiDetectorArray.Id> idOpt = GsaoiDetectorArray.instance.getId(guideStar, ctx);
            if (idOpt.isEmpty()) return None.instance();

            // Return a new Some with this instance in it.
            return new Some<GuideProbe>(lookup(idOpt.getValue()));
        }

        public TargetEnvironment add(final SPTarget guideStar, final boolean isBAGS, final ObsContext ctx) {
            // Select the appropriate guider, if any.
            final TargetEnvironment env = ctx.getTargets();
            final Option<GuideProbe> probeOpt = select(guideStar.getTarget().getSkycalcCoordinates(), ctx);

            // If no probe is defined, just use ODGW1 since we're adding a target that is off the valid range.
            final GuideProbe probe = probeOpt.getOrElse(GsaoiOdgw.odgw1);

            // Return an updated target environment that incorporates this
            // guide star.
            final GuideGroup grp = env.getOrCreatePrimaryGuideGroup();

            final Option<GuideProbeTargets> gptOpt = grp.get(probe);

            // If the target is already defined, ignore it, even if it is a BAGS target. We do not want any
            // overlap between manual and BAGS, and manual overrides BAGS.
            if (gptOpt.exists(gpt -> gpt.containsTarget(guideStar)))
                return env;

            final GuideProbeTargets gptNew = isBAGS
                    ? gptOpt.map(gpt -> gpt.setBAGSTarget(guideStar).setPrimaryToBAGSTarget()).
                        getOrElse(GuideProbeTargets.create(probe, guideStar).selectPrimary(guideStar))
                    : gptOpt.getOrElse(GuideProbeTargets.create(probe, guideStar)).selectPrimary(guideStar);
            final GuideGroup grpNew = grp.put(gptNew);
            return env.setPrimaryGuideGroup(grpNew);
        }

        // Sort the targets in the current context into a map keyed by
        // GsaoiOdgw.  Targets in the obs context that land on the detector will
        // appear in the list associated with appropriate guide window.
        // Targets that don't land on the array are kept with whatever
        // guide window they were previously associated with.
        private Map<GsaoiOdgw, List<SPTarget>> sortTargets(final ObsContext ctx) {

            // Initialize the map with empty lists.
            final Map<GsaoiOdgw, List<SPTarget>> map = new HashMap<>();
            for (final GsaoiOdgw odgw : GsaoiOdgw.values()) {
                map.put(odgw, new ArrayList<>());
            }

            // Sort each ODGW target into a map of lists where each list is
            // keyed by the ODGW type.  Sort according to which dector array
            // that they fall into.
            final TargetEnvironment env = ctx.getTargets();
            for (final GsaoiOdgw odgw : GsaoiOdgw.values()) {
                final Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(odgw);
                if (gtOpt.isEmpty()) continue;

                for (final SPTarget target : gtOpt.getValue().getTargets()) {
                    final Option<GuideProbe> opt = select(target.getTarget().getSkycalcCoordinates(), ctx);
                    if (opt.isEmpty()) {
                        // Doesn't fall on the detector, so keep it with
                        // whichever ODGW it was associated with.
                        map.get(odgw).add(target);
                    } else {
                        // Does fall on the detector, so put it with that
                        // detector be-it the same as before or a new one.
                        final GsaoiOdgw newOdgw = (GsaoiOdgw) opt.getValue();
                        map.get(newOdgw).add(target);
                    }
                }
            }

            return map;
        }

        // Gets the mapping of GsaoiOdgw to the guide star that is marked
        // as primary for that guider, if any.
        private Map<GsaoiOdgw, SPTarget> getOldPrimaryMap(ObsContext ctx) {
            final TargetEnvironment env = ctx.getTargets();

            final Map<GsaoiOdgw, SPTarget> res = new HashMap<GsaoiOdgw, SPTarget>();
            for (final GsaoiOdgw odgw : GsaoiOdgw.values()) {
                final Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(odgw);
                gtOpt.foreach(gt -> gt.getPrimary().foreach(p -> res.put(odgw, p)));
            }

            return res;
        }

        private SPTarget findNewPrimary(final SPTarget oldPrimary, final Set<SPTarget> oldPrimarySet, final List<SPTarget> newTargetList) {
            // If the old primary guide star for this guide window is still
            // in the same detector array, then select it.
            if ((oldPrimary != null) && newTargetList.contains(oldPrimary)) {
                return oldPrimary;
            }

            // If the old primary set was empty, then don't choose any new
            // primary.
            if (oldPrimarySet.size() == 0) return null;

            // If one of the new guide stars for this guide window was
            // primary in its old home, then make it primary here.
            return newTargetList.stream().
                    filter(oldPrimarySet::contains).findFirst().
                    orElse(newTargetList.stream().findFirst().orElse(null));
        }

        // Figures out, for each GsaoiOdgw, which guide star should be marked
        // as primary in the new assignment of guider stars to guiders.
        private Map<GsaoiOdgw, SPTarget> getNewPrimaryMap(final ObsContext ctx, final Map<GsaoiOdgw, List<SPTarget>> newMap) {
            final Map<GsaoiOdgw, SPTarget> newPrimaryMap = new HashMap<>();
            final Map<GsaoiOdgw, SPTarget> oldPrimaryMap = getOldPrimaryMap(ctx);
            final Set<SPTarget> oldPrimarySet = new HashSet<>(oldPrimaryMap.values());

            for (final GsaoiOdgw odgw : GsaoiOdgw.values()) {
                final SPTarget oldPrimary = oldPrimaryMap.get(odgw);
                final SPTarget newPrimary = findNewPrimary(oldPrimary, oldPrimarySet, newMap.get(odgw));
                if (newPrimary != null) newPrimaryMap.put(odgw, newPrimary);
            }
            return newPrimaryMap;
        }

        public Option<TargetEnvironment> optimize(ObsContext ctx) {
            // Sort the targets in the current context.
            final Map<GsaoiOdgw, List<SPTarget>> sortedMap = sortTargets(ctx);

            // Now figure out what the primary guide star should be in the
            // new context.
            final Map<GsaoiOdgw, SPTarget> primaryMap = getNewPrimaryMap(ctx, sortedMap);

            // Map all the old GuideTargets in the old target environment, keyed
            // by their guider.  This will include all guiders in use, not just
            // GsaoiOdgw.
//            boolean enabled = true;
            final TargetEnvironment env = ctx.getTargets();
            final Map<GuideProbe, GuideProbeTargets> gtMap = new HashMap<>();

            final GuideGroup grp = env.getOrCreatePrimaryGuideGroup();
            for (final GuideProbeTargets gt : grp) {
                gtMap.put(gt.getGuider(), gt);

                // All ODGW should be disabled if any are disabled.
                // TODO: GuideProbeTargets.isEnabled
//                if (enabled && (gt.getGuider() instanceof GsaoiOdgw)) {
//                    enabled = gt.isEnabled();
//                }
            }

            // Create the optimized target environment.
            boolean updated = false;
            for (final GsaoiOdgw odgw : GsaoiOdgw.values()) {
                final List<SPTarget> lst = sortedMap.get(odgw);
                if (lst.size() == 0) {
                    // No guide stars for this guide window, so remove it from
                    // the map if it is there.
                    final GuideProbeTargets old = gtMap.remove(odgw);
                    if ((old != null) && old.containsTargets()) {
                        updated = true;
                    }
                } else {
                    // Create a new GuideTargets instance for this ODGW.  The
                    // primary was decided above, so just look it up in the
                    // primaryMap.
                    final ImList<SPTarget> imLst = DefaultImList.create(lst);
                    final SPTarget primary = primaryMap.get(odgw);
                    final int primaryIndex = imLst.indexOf(primary);

                    final Option<Integer> primaryOpt = (primaryIndex == -1) ? None.INTEGER : new Some<>(primaryIndex);

                    // TODO: PROBLEM HERE: is primary BAGS or not?
                    final GuideProbeTargets gptOld = gtMap.get(odgw);
                    if (gptOld != null) {
                        final boolean primaryIsBags = gptOld.getBAGSTarget().exists(primary::equals);
                        final Option<SPTarget> bagsTarget = primaryIsBags ? new Some<>(primary) : GuideProbeTargets.NO_TARGET;
                        final GuideProbeTargets gptNew = GuideProbeTargets.create(odgw, bagsTarget, new Some<>(primary), imLst);
                        gtMap.put(odgw, gptNew);

                        if (!updated && (targetsUpdated(imLst, gptOld.getManualTargets()) || !gptOld.getBAGSTarget().equals(bagsTarget))) {
                            updated = true;
                        }
                    }
                }
            }

            final Option<TargetEnvironment> res;
            if (updated) {
                final ImList<GuideProbeTargets> gtList = DefaultImList.create(gtMap.values());
                res = new Some<>(env.setPrimaryGuideGroup(grp.setAll(gtList)));
            } else {
                res = None.instance();
            }
            return res;
        }

        private boolean targetsUpdated(ImList<SPTarget> lst1, ImList<SPTarget> lst2) {
            Set<SPTarget> targets1 = new HashSet<SPTarget>(lst1.toList());
            Set<SPTarget> targets2 = new HashSet<SPTarget>(lst2.toList());
            return !targets1.equals(targets2);
        }

        public Angle getRadiusLimits() {
            return new Angle(1, Angle.Unit.ARCMINS);
        }

    }

    private final GsaoiDetectorArray.Id id;

    private GsaoiOdgw(GsaoiDetectorArray.Id id) {
        this.id = id;
    }

    public String getKey() {
        return "ODGW" + getIndex();
    }

    public String toString() {
        return getKey();
    }

    public Type getType() {
        return Type.OIWFS;
    }

    /**
     * Gets the index of the ODGW, from 1 to 4.
     */
    public int getIndex() {
        return id.ordinal() + 1;
    }

    public String getDisplayName() {
        return "On-detector Guide Window " + getIndex();
    }

    public String getSequenceProp() {
        return "guideWithODGW" + getIndex();
    }

    public GuideOptions getGuideOptions() {
        return StandardGuideOptions.instance;
        // Requested to change to standard guide options.  Parking an ODGW
        // means setting it to the corner.
//        return OnDetectorGuideOptions.instance;
    }

    public Option<GuideProbeGroup> getGroup() {
        return new Some<GuideProbeGroup>(Group.instance);
    }

    /**
     * Finds the ODGW with the given id.
     * @return corresponding GsaoiOdgw
     */
    public static GsaoiOdgw lookup(GsaoiDetectorArray.Id id) {
        assert GsaoiDetectorArray.Id.values().length == values().length;
        return values()[id.index() - 1];
    }

    public GuideStarValidation validate(SPTarget guideStar, ObsContext ctx) {
        final Option<Long> when = ctx.getSchedulingBlock().map(SchedulingBlock::start);
        return guideStar.getTarget().getSkycalcCoordinates(when).map(coords -> {
            // Get the id of the detector in which the guide star lands, if any

            Option<GsaoiDetectorArray.Id> idOpt = GsaoiDetectorArray.instance.getId(coords, ctx);
            if (idOpt.isEmpty()) return GuideStarValidation.INVALID;
            return idOpt.getValue() == id ? GuideStarValidation.VALID : GuideStarValidation.INVALID;
        }).getOrElse(GuideStarValidation.UNDEFINED);
    }

    // not implemented yet, return empty area
    final private static PatrolField patrolField = new PatrolField(new Area());
    @Override public PatrolField getPatrolField() {
        return patrolField;
    }

    @Override public Option<PatrolField> getCorrectedPatrolField(ObsContext ctx) {
        return (ctx.getInstrument() instanceof Gsaoi) ? new Some<>(patrolField) : None.<PatrolField>instance();
    }
}
