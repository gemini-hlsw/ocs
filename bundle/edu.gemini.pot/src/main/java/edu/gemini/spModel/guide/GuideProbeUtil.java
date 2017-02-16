package edu.gemini.spModel.guide;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.CoordinateDiff;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.skycalc.Offset;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.TargetObsComp;

import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility methods for working with guide probes.
 */
public enum GuideProbeUtil {
    instance;

    /**
     * Turns the given array of guide probes into a
     * <code>java.util.Collection</code>. Used to simplify the implementation
     * of {@link GuideProbeProvider} and {@link GuideProbeConsumer}.
     *
     * @param probes the probes to add to the collection
     * @return collection of {@link GuideProbe} formed from the given probes
     */
    public Collection<GuideProbe> createCollection(GuideProbe... probes) {
        return Collections.unmodifiableCollection(Arrays.asList(probes));
    }

    /**
     * Computes the available guiders in the context of the given observation.
     * This method cycles through the children of the observation finding all
     * {@link GuideProbeProvider}s and all {@link GuideProbeConsumer}s.  It
     * collections all the provided guiders, removes the consumed ones, and
     * returns the result.
     *
     * @param obs the observation whose available guiders are sought
     * @return set of {@link GuideProbe} that are available in the context of
     * this observation
     */
    public Set<GuideProbe> getAvailableGuiders(ISPObservation obs) {
        final List<ISPObsComponent> obsComponents = obs.getObsComponents();
        final List<AbstractDataObject> dataObjs = new ArrayList<>(obsComponents.size());
        dataObjs.addAll(obsComponents.stream().map(obsComp -> (AbstractDataObject) obsComp.getDataObject()).collect(Collectors.toList()));
        return getAvailableGuiders(dataObjs);
    }

    // TODO: for some reason, this is not returning GeMS AGS
    public Set<GuideProbe> getAvailableGuiders(ObsContext ctx) {
        System.out.println("+++ GuideProbeUtil: getAvailableGuiders");
        final List<AbstractDataObject> dataObjects = new ArrayList<>(3);
        final TargetEnvironment env = ctx.getTargets();
        if (env != null) {
            System.out.println("+++ GuideProbeUtil: getAvailableGuiders env != null");
            final TargetObsComp toc = new TargetObsComp();
            toc.setTargetEnvironment(env);
            dataObjects.add(toc);
        }
        final SPInstObsComp inst = ctx.getInstrument();
        if (inst != null) {
            System.out.println("+++ GuideProbeUtil: getAvailableGuiders inst != null");
            dataObjects.add(inst);
        }
        ctx.getAOComponent().foreach(dataObjects::add);
        System.out.println("+++ GuideProbeUtil: getAvailableGuiders, size now=" + dataObjects.size());

        return getAvailableGuiders(dataObjects);
    }

    public Set<GuideProbe> getAvailableGuiders(Collection<? extends ISPDataObject> dataObjects) {
        final Set<GuideProbe> res = new HashSet<>();
        final Set<GuideProbe> anti = new HashSet<>();

        for (ISPDataObject dataObj : dataObjects) {
            if (dataObj instanceof GuideProbeProvider) {
                res.addAll(((GuideProbeProvider) dataObj).getGuideProbes());
            }
            if (dataObj instanceof GuideProbeConsumer) {
                anti.addAll(((GuideProbeConsumer) dataObj).getConsumedGuideProbes());
            }
        }
        res.forEach(gp -> {
            System.out.println("=== res guide probe=" + gp.getDisplayName());
        });
        anti.forEach(gp -> {
            System.out.println("=== anti guide probe=" + gp.getDisplayName());
        });
        res.removeAll(anti);

        return res;
    }

    public boolean isAvailable(ISPObservation obs, GuideProbe guider) {
       return getAvailableGuiders(obs).contains(guider);
    }

    public boolean isAvailable(ObsContext ctx, GuideProbe guider) {
        return getAvailableGuiders(ctx).contains(guider);
    }

    public GuideStarValidation validate(final SPTarget guideStar, final GuideProbe guideProbe, final ObsContext ctx) {
        final Option<Long> when = ctx.getSchedulingBlockStart();
        return guideStar.getSkycalcCoordinates(when).map(coords ->
            validate(coords, guideProbe, ctx)).getOrElse(GuideStarValidation.UNDEFINED);
    }

    public GuideStarValidation validate(final Coordinates coords, final GuideProbe guideProbe, final ObsContext ctx) {
        final Angle positionAngle = ctx.getPositionAngleJava();
        final Set<Offset> sciencePositions = ctx.getSciencePositions();
        return ctx.getBaseCoordinates().map(bcs ->
            guideProbe.getCorrectedPatrolField(ctx).exists(patrolField -> {
               final BoundaryPosition bp = patrolField.checkBoundaries(coords, bcs, positionAngle, sciencePositions);
               return !(bp == BoundaryPosition.outside || bp == BoundaryPosition.outerBoundary);
            }) ? GuideStarValidation.VALID : GuideStarValidation.INVALID
        ).getOrElse(GuideStarValidation.UNDEFINED);
    }

    public boolean inRange(final GuideProbe guideProbe, final ObsContext ctx, final Offset offset) {
        // get primary guide star
        final Option<GuideProbeTargets> gptOpt = ctx.getTargets().getPrimaryGuideProbeTargets(guideProbe);
        if (gptOpt.isEmpty()) return false;
        final GuideProbeTargets gpt = gptOpt.getValue();

        final Option<SPTarget> guideStarOpt = gpt.getPrimary();
        if (guideStarOpt.isEmpty()) return false;
        final SPTarget guideStar = guideStarOpt.getValue();

        return inRange(guideStar, guideProbe, ctx, offset);
    }

    private boolean inRange(final SPTarget guideStar, final GuideProbe guideProbe, final ObsContext ctx, final Offset offset) {
        return guideProbe.getCorrectedPatrolField(ctx).exists(patrolField -> {
            // offset position -> we must move the corrected patrol field by this offset
            final double xOffset = -offset.p().toArcsecs().getMagnitude();
            final double yOffset = -offset.q().toArcsecs().getMagnitude();
            final PatrolField offsetPatrolField = patrolField.getTransformed(AffineTransform.getTranslateInstance(xOffset, yOffset));
            // and we must rotate the patrol field according to position angle
            final PatrolField rotatedPatrolField = offsetPatrolField.getTransformed(AffineTransform.getRotateInstance(-ctx.getPositionAngle().toRadians()));
            // find distance of base position to the guide star
            return
                guideStar.getSkycalcCoordinates(ctx.getSchedulingBlockStart()).flatMap(gcs ->
                ctx.getBaseCoordinates().map(baseCoordinates -> {
                    final CoordinateDiff diff = new CoordinateDiff(baseCoordinates, gcs);
                    final Offset dis = diff.getOffset();
                    final double p = -dis.p().toArcsecs().getMagnitude();
                    final double q = -dis.q().toArcsecs().getMagnitude();
                    // and now check if that guide star is inside the correctly transformed/rotated patrol field
                    return rotatedPatrolField.getArea().contains(p, q);
                })).getOrElse(false);
        });
    }
}
