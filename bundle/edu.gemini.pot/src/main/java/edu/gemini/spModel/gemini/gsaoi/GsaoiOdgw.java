package edu.gemini.spModel.gemini.gsaoi;

import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.gems.GemsGuideProbeGroup;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.OptionsList.UpdateOps;
import edu.gemini.spModel.target.env.OptionsListImpl;
import edu.gemini.spModel.target.env.TargetEnvironment;

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

        public TargetEnvironment add(SPTarget guideStar, ObsContext ctx) {
            // Select the appropriate guider, if any.
            TargetEnvironment env = ctx.getTargets();
            Option<GuideProbe> probeOpt = select(guideStar.getTarget().getSkycalcCoordinates(), ctx);
            GuideProbe probe;
            if (probeOpt.isEmpty()) {
                // Just use ODGW1 since we're adding a target that is off the
                // valid range.
                probe = GsaoiOdgw.odgw1;
            } else {
                probe = probeOpt.getValue();
            }

            // Return an updated target environment that incorporates this
            // guide star.
            GuideGroup grp = env.getOrCreatePrimaryGuideGroup();

            GuideProbeTargets gpt;
            Option<GuideProbeTargets> gptOpt = grp.get(probe);
            if (gptOpt.isEmpty()) {
                gpt = GuideProbeTargets.create(probe, guideStar);
            } else {
                gpt = gptOpt.getValue();
                if (gpt.containsTarget(guideStar)) return env;
                // Requested to always make new guide stars primary, whether
                // they fall on the detector or not.  Adding as primary.
                gpt = gpt.update(UpdateOps.appendAsPrimary(guideStar));
            }
            grp = grp.put(gpt);

            return env.setPrimaryGuideGroup(grp);
        }

        // Sort the targets in the current context into a map keyed by
        // GsaoiOdgw.  Targets in the obs context that land on the detector will
        // appear in the list associated with appropriate guide window.
        // Targets that don't land on the array are kept with whatever
        // guide window they were previously associated with.
        private Map<GsaoiOdgw, List<SPTarget>> sortTargets(ObsContext ctx) {

            // Initialize the map with empty lists.
            Map<GsaoiOdgw, List<SPTarget>> map = new HashMap<GsaoiOdgw, List<SPTarget>>();
            for (GsaoiOdgw odgw : GsaoiOdgw.values()) {
                map.put(odgw, new ArrayList<SPTarget>());
            }

            // Sort each ODGW target into a map of lists where each list is
            // keyed by the ODGW type.  Sort according to which dector array
            // that they fall into.
            TargetEnvironment env = ctx.getTargets();
            for (GsaoiOdgw odgw : GsaoiOdgw.values()) {
                Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(odgw);
                if (gtOpt.isEmpty()) continue;

                for (SPTarget target : gtOpt.getValue().getOptions()) {
                    Option<GuideProbe> opt = select(target.getTarget().getSkycalcCoordinates(), ctx);
                    if (opt.isEmpty()) {
                        // Doesn't fall on the detector, so keep it with
                        // whichever ODGW it was associated with.
                        map.get(odgw).add(target);
                    } else {
                        // Does fall on the detector, so put it with that
                        // detector be-it the same as before or a new one.
                        GsaoiOdgw newOdgw = (GsaoiOdgw) opt.getValue();
                        map.get(newOdgw).add(target);
                    }
                }
            }

            return map;
        }

        // Gets the mapping of GsaoiOdgw to the guide star that is marked
        // as primary for that guider, if any.
        private Map<GsaoiOdgw, SPTarget> getOldPrimaryMap(ObsContext ctx) {
            TargetEnvironment env = ctx.getTargets();

            Map<GsaoiOdgw, SPTarget> res = new HashMap<GsaoiOdgw, SPTarget>();
            for (GsaoiOdgw odgw : GsaoiOdgw.values()) {
                Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(odgw);
                if (gtOpt.isEmpty()) continue; // no targets for this guider

                Option<SPTarget> primary = gtOpt.getValue().getPrimary();
                if (primary.isEmpty()) continue; // no primary for this guider

                res.put(odgw, primary.getValue());
            }

            return res;
        }

        private SPTarget findNewPrimary(GsaoiOdgw odgw, SPTarget oldPrimary, Set<SPTarget> oldPrimarySet, List<SPTarget> newTargetList) {
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
            for (SPTarget target : newTargetList) {
                if (oldPrimarySet.contains(target)) return target;
            }

            // Just choose the first option since there is no logical choice.
            return (newTargetList.size() > 0) ? newTargetList.get(0) : null;
        }

        // Figures out, for each GsaoiOdgw, which guide star should be marked
        // as primary in the new assignment of guider stars to guiders.
        private Map<GsaoiOdgw, SPTarget> getNewPrimaryMap(ObsContext ctx, Map<GsaoiOdgw, List<SPTarget>> newMap) {
            Map<GsaoiOdgw, SPTarget> newPrimaryMap = new HashMap<GsaoiOdgw, SPTarget>();
            Map<GsaoiOdgw, SPTarget> oldPrimaryMap = getOldPrimaryMap(ctx);
            Set<SPTarget> oldPrimarySet = new HashSet<SPTarget>(oldPrimaryMap.values());

            for (GsaoiOdgw odgw : GsaoiOdgw.values()) {
                SPTarget oldPrimary = oldPrimaryMap.get(odgw);
                SPTarget newPrimary = findNewPrimary(odgw, oldPrimary, oldPrimarySet, newMap.get(odgw));
                if (newPrimary != null) newPrimaryMap.put(odgw, newPrimary);
            }
            return newPrimaryMap;
        }

        public Option<TargetEnvironment> optimize(ObsContext ctx) {
            // Sort the targets in the current context.
            Map<GsaoiOdgw, List<SPTarget>> sortedMap = sortTargets(ctx);

            // Now figure out what the primary guide star should be in the
            // new context.
            Map<GsaoiOdgw, SPTarget> primaryMap = getNewPrimaryMap(ctx, sortedMap);

            // Map all the old GuideTargets in the old target environment, keyed
            // by their guider.  This will include all guiders in use, not just
            // GsaoiOdgw.
//            boolean enabled = true;
            TargetEnvironment env = ctx.getTargets();
            Map<GuideProbe, GuideProbeTargets> gtMap = new HashMap<GuideProbe, GuideProbeTargets>();

            GuideGroup grp = env.getOrCreatePrimaryGuideGroup();
            for (GuideProbeTargets gt : grp) {
                gtMap.put(gt.getGuider(), gt);

                // All ODGW should be disabled if any are disabled.
                // TODO: GuideProbeTargets.isEnabled
//                if (enabled && (gt.getGuider() instanceof GsaoiOdgw)) {
//                    enabled = gt.isEnabled();
//                }
            }

            // Create the optimized target environment.
            boolean updated = false;
            for (GsaoiOdgw odgw : GsaoiOdgw.values()) {
                List<SPTarget> lst = sortedMap.get(odgw);
                if (lst.size() == 0) {
                    // No guide stars for this guide window, so remove it from
                    // the map if it is there.
                    GuideProbeTargets old = gtMap.remove(odgw);
                    if ((old != null) && !old.getOptions().isEmpty()) {
                        updated = true;
                    }
                } else {
                    // Create a new GuideTargets instance for this ODGW.  The
                    // primary was decided above, so just look it up in the
                    // primaryMap.
                    ImList<SPTarget> imLst = DefaultImList.create(lst);
                    SPTarget primary = primaryMap.get(odgw);
                    int primaryIndex = imLst.indexOf(primary);
                    Option<Integer> primaryOpt = (primaryIndex == -1) ? None.INTEGER : new Some<Integer>(primaryIndex);
                    GuideProbeTargets old;
                    old = gtMap.put(odgw, GuideProbeTargets.create(odgw, OptionsListImpl.create(primaryOpt, imLst)));

                    if (!updated && ((old == null) || targetsUpdated(imLst, old.getOptions()))) {
                        updated = true;
                    }
                }
            }

            Option<TargetEnvironment> res = None.instance();
            if (updated) {
                ImList<GuideProbeTargets> gtList = DefaultImList.create(gtMap.values());
                res = new Some<TargetEnvironment>(env.setPrimaryGuideGroup(grp.setAll(gtList)));
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

    public boolean validate(SPTarget guideStar, ObsContext ctx) {
        Coordinates coords = guideStar.getTarget().getSkycalcCoordinates();
        // Get the id of the detector in which the guide star lands, if any
        Option<GsaoiDetectorArray.Id> idOpt = GsaoiDetectorArray.instance.getId(coords, ctx);
        if (idOpt.isEmpty()) return false;
        return idOpt.getValue() == id;
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
