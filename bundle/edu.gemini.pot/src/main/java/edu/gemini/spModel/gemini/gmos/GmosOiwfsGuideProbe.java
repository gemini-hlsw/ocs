//
// $
//

package edu.gemini.spModel.gemini.gmos;

import edu.gemini.shared.util.immutable.MapOp;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.telescope.IssPort;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Set;

/**
 * GMOS on-instrument guide probe.
 */
public enum GmosOiwfsGuideProbe implements ValidatableGuideProbe, OffsetValidatingGuideProbe {
    instance;

    private static final PatrolField patrolField;
    static {
        Rectangle2D.Double fov = new Rectangle2D.Double(-11.4, -34.92, 212.7, 249.6);
        Rectangle2D.Double fovIn = new Rectangle2D.Double(fov.x + 2.0,
                fov.y + 2.0,
                fov.width - 4.0,
                fov.height - 4.0);
        Rectangle2D.Double fovOut = new Rectangle2D.Double(fov.x - 2.0,
                fov.y - 2.0,
                fov.width + 4.0,
                fov.height + 4.0);
        patrolField = new PatrolField(fov, fovIn, fovOut);
    }

    public String getKey() {
        return "GMOS OIWFS";
    }

    public String toString() {
        return getKey();
    }

    public Type getType() {
        return Type.OIWFS;
    }

    public String getDisplayName() {
        return "GMOS On-instrument WFS";
    }

    public String getSequenceProp() {
        return "guideWithOIWFS";
    }

    public GuideOptions getGuideOptions() {
        return StandardGuideOptions.instance;
    }

    public Option<GuideProbeGroup> getGroup() {
        return None.instance();
    }

    public BoundaryPosition checkBoundaries(SPTarget guideStar, ObsContext ctx) {
        return checkBoundaries(guideStar.getSkycalcCoordinates(), ctx);
    }

    public BoundaryPosition checkBoundaries(final Coordinates coords, final ObsContext ctx) {
        final Coordinates baseCoordinates = ctx.getBaseCoordinates();
        final Angle positionAngle = ctx.getPositionAngle();
        final Set<Offset> sciencePositions = ctx.getSciencePositions();

        // check positions against corrected patrol field
        return getCorrectedPatrolField(ctx).map(new MapOp<PatrolField, BoundaryPosition>() {
            @Override public BoundaryPosition apply(PatrolField patrolField) {
                return patrolField.checkBoundaries(coords, baseCoordinates, positionAngle, sciencePositions);
            }
        }).getOrElse(BoundaryPosition.outside);
    }

    /**
     * Check if the primary guide star is in range from the given offset
     *
     * @param ctx    ObsContext to get guide star and base coordinates from.
     * @param offset to check if the guide star is in range
     * @return true if guide star is in range from the given offset, false otherwise
     */
    public boolean inRange(ObsContext ctx, Offset offset) {
        return GuideProbeUtil.instance.inRange(this, ctx, offset);
    }

    @Override
    public boolean validate(SPTarget guideStar, ObsContext ctx) {
        return GuideProbeUtil.instance.validate(guideStar.getSkycalcCoordinates(), this, ctx);
    }

    public boolean validate(SkyObject guideStar, ObsContext ctx) {
        return GuideProbeUtil.instance.validate(guideStar, this, ctx);
    }

    public boolean validate(Coordinates guideStar, ObsContext ctx) {
        return GuideProbeUtil.instance.validate(guideStar, this, ctx);
    }

    @Override
    public PatrolField getPatrolField() {
        return GmosOiwfsGuideProbe.patrolField;
    }

    @Override
    public Option<PatrolField> getCorrectedPatrolField(ObsContext ctx) {
        if (ctx.getInstrument() instanceof InstGmosCommon) {
            return new Some<>(getCorrectedPatrolField(ctx, getPatrolField()));
        } else {
            return None.instance();
        }
    }

    private PatrolField getCorrectedPatrolField(ObsContext ctx, PatrolField patrolField) {
        final AffineTransform sideLooking = transformForPort(ctx.getIssPort());

        // calculate and apply GMOS specific IFU offsets and flip coordinates
        double ifuXOffset = ((GmosCommonType.FPUnit) ((InstGmosCommon) ctx.getInstrument()).getFPUnit()).getWFSOffset();
        AffineTransform offsetAndFlipTransform = AffineTransform.getTranslateInstance(ifuXOffset, 0.0);
        offsetAndFlipTransform.concatenate(sideLooking);

        // now get corrected patrol field
        return patrolField.getTransformed(offsetAndFlipTransform);
    }

    // take care of transformations needed for different ports
    private AffineTransform transformForPort(IssPort port) {
        if (port == IssPort.SIDE_LOOKING) {
            // if on side looking port flip y coordinates
            return AffineTransform.getScaleInstance(1.0, -1.0);
        } else {
            // return the identity transform
            return new AffineTransform();
        }
    }

}
