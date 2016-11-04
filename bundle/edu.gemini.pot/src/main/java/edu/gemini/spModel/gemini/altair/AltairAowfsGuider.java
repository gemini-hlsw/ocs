package edu.gemini.spModel.gemini.altair;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.skycalc.Offset;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;

import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.Set;

/**
 *
 */
public enum AltairAowfsGuider implements OffsetValidatingGuideProbe, ValidatableGuideProbe {
    instance;

    private static final PatrolField patrolField;
    private static final int ALTAIR_P_PLUS = 25;
    private static final int ALTAIR_P_MINUS = -25;
    private static final int ALTAIR_Q_PLUS = 27;
    private static final int ALTAIR_Q_MINUS = -23;
    static {
        // Define the oval to represent Altair patrol field.
        // The oval is composed of 2 arcs, whose limits are defined by p and q.
        double widthTop = - 2 * ALTAIR_P_MINUS;
        double heightTop = 2 * ALTAIR_Q_PLUS;

        // The boundaries of the rectangle that will enclose the bottom arc that forms the oval
        double widthBottom =  2 * ALTAIR_P_PLUS;
        double heightBottom = - 2 * ALTAIR_Q_MINUS;

        // * The FOV figure *
        Rectangle2D.Double rightRectangleFOV = new Rectangle2D.Double(-ALTAIR_P_PLUS, -ALTAIR_Q_PLUS, widthTop, heightTop);
        Rectangle2D.Double leftRectangleFOV = new Rectangle2D.Double(ALTAIR_P_MINUS, ALTAIR_Q_MINUS, widthBottom, heightBottom);
        Arc2D.Double aowfsTFOV = new Arc2D.Double(rightRectangleFOV, 0., 180.0, Arc2D.OPEN);
        Arc2D.Double aowfsBFOV = new Arc2D.Double(leftRectangleFOV, 180., 180.0, Arc2D.OPEN);
        GeneralPath figureFOV = new GeneralPath(aowfsBFOV);
        figureFOV.append(aowfsTFOV, true);

        // * The FOV inner figure: 2 arcsec smaller than FOV figure *
        Rectangle2D.Double rightRectangleFOVIn = new Rectangle2D.Double(rightRectangleFOV.x + 2.0, rightRectangleFOV.y + 2.0,
                                                                        rightRectangleFOV.width - 4.0, rightRectangleFOV.height - 4.0);
        Rectangle2D.Double leftRectangleFOVIn  = new Rectangle2D.Double(leftRectangleFOV.x + 2.0, leftRectangleFOV.y + 2.0,
                                                                        leftRectangleFOV.width - 4.0, leftRectangleFOV.height - 4.0);
        Arc2D.Double aowfsTFOVIn = new Arc2D.Double(rightRectangleFOVIn,  0., 180., Arc2D.OPEN);
        Arc2D.Double aowfsBFOVIn = new Arc2D.Double(leftRectangleFOVIn, 180., 180., Arc2D.OPEN);
        GeneralPath figureFOVIn = new GeneralPath(aowfsBFOVIn);
        figureFOVIn.append(aowfsTFOVIn, true);

        // * The FOV outer figure: 2 arcsec larger than FOV figure *
        Rectangle2D.Double rightRectangleFOVOut = new Rectangle2D.Double(rightRectangleFOV.x - 2.0, rightRectangleFOV.y - 2.0,
                                                                         rightRectangleFOV.width + 4.0, rightRectangleFOV.height + 4.0);
        Rectangle2D.Double leftRectangleFOVOut  = new Rectangle2D.Double(leftRectangleFOV.x - 2.0, leftRectangleFOV.y - 2.0,
                                                                         leftRectangleFOV.width + 4.0, leftRectangleFOV.height + 4.0);
        Arc2D.Double aowfsTFOVOut = new Arc2D.Double(rightRectangleFOVOut,  0., 180., Arc2D.OPEN);
        Arc2D.Double aowfsBFOVOut = new Arc2D.Double(leftRectangleFOVOut, 180., 180., Arc2D.OPEN);
        GeneralPath figureFOVOut = new GeneralPath(aowfsBFOVOut);
        figureFOVOut.append(aowfsTFOVOut, true);

        patrolField = new PatrolField(figureFOV, figureFOVIn, figureFOVOut);
    }

    public String getKey() {
        return "Altair AOWFS";
    }

    public String toString() {
        return getKey();
    }

    public Type getType() {
        return Type.AOWFS;
    }

    public String getDisplayName() {
        return "Altair WFS";
    }

    public String getSequenceProp() {
        return "guideWithAOWFS";
    }

    public GuideOptions getGuideOptions() {
        return StandardGuideOptions.instance;
    }

    public Option<GuideProbeGroup> getGroup() {
        return None.instance();
    }

    @Override public PatrolField getPatrolField() {
        return patrolField;
    }

    public Option<BoundaryPosition> checkBoundaries(SPTarget guideStar, ObsContext ctx) {
        return guideStar
            .getSkycalcCoordinates(ctx.getSchedulingBlockStart())
            .flatMap(coords -> checkBoundaries(coords, ctx));
    }

    public Option<BoundaryPosition> checkBoundaries(final Coordinates coords, final ObsContext ctx) {
        return ctx.getBaseCoordinates().map(baseCoordinates -> {
            final Angle positionAngle = ctx.getPositionAngleJava();
            final Set<Offset> sciencePositions = ctx.getSciencePositions();

            // check positions against corrected patrol field
            return getCorrectedPatrolField(ctx).map(pf ->
                    pf.checkBoundaries(coords, baseCoordinates, positionAngle, sciencePositions)
            ).getOrElse(BoundaryPosition.outside);
        });
    }

    @Override public boolean inRange(ObsContext ctx, Offset offset) {
        return GuideProbeUtil.instance.inRange(this, ctx, offset);
    }

    @Override public GuideStarValidation validate(SPTarget guideStar, ObsContext ctx) {
        return GuideProbeUtil.instance.validate(guideStar, this, ctx);
    }

    @Override public Option<PatrolField> getCorrectedPatrolField(final ObsContext ctx) {
        return ctx.getAOComponent().flatMap(ado -> {
            if (ado instanceof InstAltair) {
                // for NIFS we must correct the patrol
                // (this is SCT-361 related and has been moved here from Altair_WFS_Feature class)
                if (ctx.getInstrument() instanceof InstNIFS) {
                    AffineTransform correction = AffineTransform.getTranslateInstance(-InstNIFS.ALTAIR_P_OFFSET, -InstNIFS.ALTAIR_Q_OFFSET);
                    return new Some<>(getPatrolField().getTransformed(correction));
                }
                // for all other instruments Altair does not have any correction (offsets) we need to apply to the patrol field
                return new Some<>(patrolField);
            } else {
                return None.instance();
            }
        });
    }
}
