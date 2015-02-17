//
// $
//

package edu.gemini.spModel.guide;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.CoordinateDiff;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.inst.ArmAdjustment;
import edu.gemini.spModel.inst.FeatureGeometry$;
import edu.gemini.spModel.inst.ProbeArmGeometry;
import edu.gemini.spModel.inst.ScienceAreaGeometry;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.telescope.IssPort;

import java.awt.*;
import java.awt.geom.AffineTransform;

import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

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

    public boolean validate(final SPTarget guideStar, final GuideProbe guideProbe, final ObsContext ctx) {
        return validate(guideStar.getTarget().getSkycalcCoordinates(), guideProbe, ctx);
    }

    public boolean validate(final SkyObject guideStar, final GuideProbe guideProbe, final ObsContext ctx) {
        final HmsDegCoordinates coords = guideStar.getCoordinates().toHmsDeg(0);
        final Coordinates c = new Coordinates(coords.getRa(), coords.getDec());
        return validate(c, guideProbe, ctx);
    }

    public boolean validate(final Coordinates coords, final GuideProbe guideProbe, final ObsContext ctx) {
        final Angle positionAngle = ctx.getPositionAngle();
        final Set<Offset> sciencePositions = ctx.getSciencePositions();
        final Coordinates baseCoordinates = ctx.getBaseCoordinates();

        return guideProbe.getCorrectedPatrolField(ctx).exists(new PredicateOp<PatrolField>() {
            @Override public Boolean apply(PatrolField patrolField) {
                final BoundaryPosition bp = patrolField.checkBoundaries(coords, baseCoordinates, positionAngle, sciencePositions);
                return !(bp == BoundaryPosition.outside || bp == BoundaryPosition.outerBoundary);
            }
        });
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
        return guideProbe.getCorrectedPatrolField(ctx).exists(new PredicateOp<PatrolField>() {
            @Override public Boolean apply(PatrolField patrolField) {
                // offset position -> we must move the corrected patrol field by this offset
                final double xOffset = -offset.p().toArcsecs().getMagnitude();
                final double yOffset = -offset.q().toArcsecs().getMagnitude();
                final PatrolField offsetPatrolField = patrolField.getTransformed(AffineTransform.getTranslateInstance(xOffset, yOffset));
                // and we must rotate the patrol field according to position angle
                final PatrolField rotatedPatrolField = offsetPatrolField.getTransformed(AffineTransform.getRotateInstance(-ctx.getPositionAngle().toRadians().getMagnitude()));
                // find distance of base position to the guide star
                final Coordinates baseCoordinates = ctx.getBaseCoordinates();
                final CoordinateDiff diff = new CoordinateDiff(baseCoordinates, guideStar.getTarget().getSkycalcCoordinates());
                final Offset dis = diff.getOffset();
                final double p = -dis.p().toArcsecs().getMagnitude();
                final double q = -dis.q().toArcsecs().getMagnitude();
                // and now check if that guide star is inside the correctly transformed/rotated patrol field
                return rotatedPatrolField.getArea().contains(p, q);
            }
        });
    }


    public double calculateVignetting(final ObsContext ctx,
                                      final edu.gemini.spModel.core.Coordinates guideStarCoordinates,
                                      final ScienceAreaGeometry scienceAreaGeometry,
                                      final ProbeArmGeometry probeArmGeometry) {
        if (ctx == null || scienceAreaGeometry == null || probeArmGeometry == null)
            return 0.0;

        final double        flip = ctx.getIssPort() == IssPort.SIDE_LOOKING ? -1.0 : 1.0;
        final SPInstObsComp inst = ctx.getInstrument();
        final ImList<Shape> paShapes = probeArmGeometry.geometryAsJava();

        // Configure the science area components for the context and combine into a single area.
        final ImList<Shape> saShapes = FeatureGeometry$.MODULE$.transformScienceAreaForContextAsJava(scienceAreaGeometry.geometry(inst), ctx);
        final Area scienceArea       = new Area();
        saShapes.foreach(new ApplyOp<Shape>() {
            @Override
            public void apply(final Shape shape) {
                scienceArea.add(new Area(shape));
            }
        });

        final ImList<Offset> sciencePositions = DefaultImList.create(ctx.getSciencePositions());
        final double sum = sciencePositions.foldLeft(0.0, new Function2<Double, Offset, Double>() {
            @Override
            public Double apply(final Double currentSum, final Offset offset) {
                // Find the probe arm adjustment: we need the probe arm angle and the location of the guide star
                // in arcseconds.
                final Option<ArmAdjustment> adjOpt = probeArmGeometry.armAdjustmentAsJava(ctx, guideStarCoordinates, offset);

                // If an adjustment exists, calculate the vignetting for this adjustment.
                final double vignetting = adjOpt.map(new MapOp<ArmAdjustment, Double>() {
                    @Override
                    public Double apply(final ArmAdjustment adj) {
                        final double angle      = adj.angle();
                        final Point2D guideStar = adj.guideStarCoords();

                        // Adjust the science area for the offset.
                        final double x = -offset.p().toArcsecs().getMagnitude();
                        final double y = -offset.q().toArcsecs().getMagnitude() * flip;
                        final AffineTransform trans = AffineTransform.getTranslateInstance(x, y);
                        final Area adjScienceArea = scienceArea.createTransformedArea(trans);

                        final Area probeArmArea = new Area();
                        paShapes.foreach(new ApplyOp<Shape>() {
                            @Override
                            public void apply(final Shape s) {
                                final Shape st = FeatureGeometry$.MODULE$.transformProbeArmForContext(s, angle, guideStar);
                                probeArmArea.add(new Area(st));
                            }
                        });

                        // Take the intersection of the probe arm with the science area and calculate the approximate
                        // ratio of the length of the probe arm falling inside the science area to the total length
                        // of the probe arm.
                        final Area vignettingArea = new Area(adjScienceArea);
                        vignettingArea.intersect(probeArmArea);

                        // We take, as our approximation, the ratio of the length of the diagonal of the bounding
                        // box of the vignetting area to the length of the diagonal of the bounding box of the entire
                        // probe arm.
                        final Rectangle2D vignetteBounds = vignettingArea.getBounds2D();
                        final double vWidth              = vignetteBounds.getWidth();
                        final double vHeight             = vignetteBounds.getHeight();
                        final double vLengthSquared      = vWidth * vWidth + vHeight * vHeight;

                        final Rectangle2D probeArmBounds = probeArmArea.getBounds2D();
                        final double pWidth              = probeArmBounds.getWidth();
                        final double pHeight             = probeArmBounds.getHeight();
                        final double pLengthSquared      = pWidth * pWidth + pHeight * pHeight;

                        return (pLengthSquared > 0 ? Math.sqrt(vLengthSquared / pLengthSquared) : 0.0);
                    }
                }).getOrElse(0.0);

                return currentSum + vignetting;
            }
        });

        // Now average out the vignetting across the science positions.
        return sciencePositions.isEmpty() ? 0.0 : sum / sciencePositions.size();
    }
}
