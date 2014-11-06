//
// $
//

package edu.gemini.spModel.guide;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.CoordinateDiff;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;

import java.awt.geom.AffineTransform;

import java.util.*;

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
     *         this observation
     */
    public Set<GuideProbe> getAvailableGuiders(ISPObservation obs) {
        final List<ISPObsComponent> obsComponents = obs.getObsComponents();
        final List<AbstractDataObject> dataObjs = new ArrayList<>(obsComponents.size());
        for (ISPObsComponent obsComp : obsComponents) {
            dataObjs.add((AbstractDataObject) obsComp.getDataObject());
        }
        return getAvailableGuiders(dataObjs);
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

        res.removeAll(anti);

        return res;
    }

    public boolean validate(SPTarget guideStar, PatrolField correctedPatrolField, ObsContext ctx) {
        return validate(guideStar.getSkycalcCoordinates(), correctedPatrolField, ctx);
    }

    public boolean validate(SkyObject guideStar, PatrolField correctedPatrolField, ObsContext ctx) {
        final HmsDegCoordinates coords = guideStar.getCoordinates().toHmsDeg(0);
        final Coordinates c = new Coordinates(coords.getRa(), coords.getDec());
        return validate(c, correctedPatrolField, ctx);
    }

    public boolean validate(Coordinates coords, PatrolField correctedPatrolField, ObsContext ctx) {
        final Angle positionAngle = ctx.getPositionAngle();
        final Set<Offset> sciencePositions = ctx.getSciencePositions();
        final Coordinates baseCoordinates = ctx.getBaseCoordinates();

        final BoundaryPosition bp = correctedPatrolField.checkBoundaries(coords, baseCoordinates, positionAngle, sciencePositions);
        return !(bp == BoundaryPosition.outside || bp == BoundaryPosition.outerBoundary);
    }

    public boolean inRange(GuideProbe guideProbe, ObsContext ctx, Offset offset) {
        // get primary guide star
        final Option<GuideProbeTargets> gptOpt = ctx.getTargets().getPrimaryGuideProbeTargets(guideProbe);
        if (gptOpt.isEmpty()) return false;
        final GuideProbeTargets gpt = gptOpt.getValue();

        final Option<SPTarget> guideStarOpt = gpt.getPrimary();
        if (guideStarOpt.isEmpty()) return false;
        final SPTarget guideStar = guideStarOpt.getValue();

        return inRange(guideStar, guideProbe.getCorrectedPatrolField(ctx), ctx, offset);
    }

    private boolean inRange(SPTarget guideStar, PatrolField correctedPatrolField, ObsContext ctx, Offset offset) {
        // offset position -> we must move the corrected patrol field by this offset
        final double xOffset = -offset.p().toArcsecs().getMagnitude();
        final double yOffset = -offset.q().toArcsecs().getMagnitude();
        final PatrolField offsetPatrolField = correctedPatrolField.getTransformed(AffineTransform.getTranslateInstance(xOffset, yOffset));
        // and we must rotate the patrol field according to position angle
        final PatrolField rotatedPatrolField = offsetPatrolField.getTransformed(AffineTransform.getRotateInstance(-ctx.getPositionAngle().toRadians().getMagnitude()));
        // find distance of base position to the guide star
        final Coordinates baseCoordinates = ctx.getBaseCoordinates();
        final CoordinateDiff diff = new CoordinateDiff(baseCoordinates, guideStar.getSkycalcCoordinates());
        final Offset dis = diff.getOffset();
        final double p = -dis.p().toArcsecs().getMagnitude();
        final double q = -dis.q().toArcsecs().getMagnitude();
        // and now check if that guide star is inside the correctly transformed/rotated patrol field
        return rotatedPatrolField.getArea().contains(p, q);
    }
}
