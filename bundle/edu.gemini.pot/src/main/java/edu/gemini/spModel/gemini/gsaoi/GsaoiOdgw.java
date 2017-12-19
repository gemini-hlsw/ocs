package edu.gemini.spModel.gemini.gsaoi;

import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.BandsList;
import edu.gemini.spModel.core.RBandsList;
import edu.gemini.spModel.gems.GemsGuideProbeGroup;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.*;

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
     * Group of GsaoiOdgw, with support for selecting and optimizing
     * {@link TargetEnvironment target environments}.
     */
    public enum Group implements SelectableGuideProbeGroup, OptimizableGuideProbeGroup, GemsGuideProbeGroup {
        instance;

        @Override public String getKey() {
            return "ODGW";
        }

        @Override public String getDisplayName() {
            return "On-detector Guide Window";
        }

        @Override public Collection<ValidatableGuideProbe> getMembers() {
            return Arrays.asList(GsaoiOdgw.values());
        }

        @Override public Option<GuideProbe> select(Coordinates guideStar, ObsContext ctx) {
            // Get the id of the detector in which the guide star lands, if any
            final Option<GsaoiDetectorArray.Id> idOpt = GsaoiDetectorArray.instance.getId(guideStar, ctx);
            if (idOpt.isEmpty()) return None.instance();

            // Return a new Some with this instance in it.
            return new Some<>(lookup(idOpt.getValue()));
        }

        @Override public TargetEnvironment add(final SPTarget guideStar, final ObsContext ctx) {
            // Select the appropriate guider, if any.
            final TargetEnvironment env = ctx.getTargets();
            final Option<GuideProbe> probeOpt = guideStar
                .getSkycalcCoordinates(ctx.getSchedulingBlockStart())
                .flatMap(cs -> select(cs, ctx));

            // If no probe is defined, just use ODGW1 since we're adding a target that is off the valid range.
            final GuideProbe probe = probeOpt.getOrElse(GsaoiOdgw.odgw1);

            // Return an updated target environment that incorporates this
            // guide star.
            final GuideGroup grp = env.getPrimaryGuideGroup();
            final Option<GuideProbeTargets> gptOpt = grp.get(probe);

            if (gptOpt.exists(gpt -> gpt.containsTarget(guideStar)))
                return env;

            final GuideProbeTargets gptNew = gptOpt.map(gpt ->
                    gpt.update(OptionsList.UpdateOps.appendAsPrimary(guideStar)))
                    .getOrElse(GuideProbeTargets.create(probe, guideStar));
            final GuideGroup grpNew = grp.put(gptNew);
            return env.setPrimaryGuideGroup(grpNew);
        }

        @Override public Angle getRadiusLimits() {
            return new Angle(1, Angle.Unit.ARCMINS);
        }

    }

    private final GsaoiDetectorArray.Id id;

    GsaoiOdgw(GsaoiDetectorArray.Id id) {
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
        return new Some<>(Group.instance);
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
        final Option<Long> when = ctx.getSchedulingBlockStart();
        return guideStar.getSkycalcCoordinates(when).map(coords -> {
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
        return (ctx.getInstrument() instanceof Gsaoi) ? new Some<>(patrolField) : None.instance();
    }

    @Override
    public BandsList getBands() {
        return RBandsList.instance();
    }
}
