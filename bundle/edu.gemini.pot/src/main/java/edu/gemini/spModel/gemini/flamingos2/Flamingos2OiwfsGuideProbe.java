package edu.gemini.spModel.gemini.flamingos2;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.BandsList;
import edu.gemini.spModel.core.RBandsList;
import edu.gemini.spModel.gems.GemsGuideProbeGroup;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.SchedulingBlock;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;

import java.awt.geom.*;
import java.util.Arrays;
import java.util.Collection;

/**
 * The Flamingos II OIWFS guider.
 */
public enum Flamingos2OiwfsGuideProbe implements GuideProbe, ValidatableGuideProbe, OffsetValidatingGuideProbe, VignettingGuideProbe {
    instance;

    private static final PatrolField patrolField;

    // Definition of patrol field according to REL-285
    // The diameter of the circle centered over the base position, in mm
    private static final double ENTRANCE_WINDOW_RADIUS = 139.7;
    // The offsets of the patrol area circles, in mm
    private static final double BASE_PIVOT_POINT = 250.5934;
    private static final double PICK_OFF_PIVOT_POINT = BASE_PIVOT_POINT - 77.6653;
    // The diameter of the upper (smaller) patrol area circle, in mm
    private static final double UPPER_PATROL_AREA_RADIUS = 191.0286;
    // The diameter of the lower (larger) patrol area circle, in mm
    private static final double LOWER_PATROL_AREA_RADIUS = 268.6939;
    // The high limit of the bounding box to the right
    private static final double PATROL_AREA_HI_LIMIT = 113.0;

    static{
        // define the "upper" and "lower" half-circles defining the patrol are
        // -- use full circle for upper smaller one (using only half circle for upper part of figure can
        // -- end in two disjoint areas due to calculation imprecisions, we have to make sure areas overlap
        // -- properly to yield the figure we want).
        Ellipse2D.Double upperPa = new Ellipse2D.Double(
                PICK_OFF_PIVOT_POINT - UPPER_PATROL_AREA_RADIUS, -UPPER_PATROL_AREA_RADIUS,
                UPPER_PATROL_AREA_RADIUS*2., UPPER_PATROL_AREA_RADIUS*2.);
        // -- and combine with lower half-circle of bigger one
        Arc2D.Double lowerPa = new Arc2D.Double(
                BASE_PIVOT_POINT - LOWER_PATROL_AREA_RADIUS, -LOWER_PATROL_AREA_RADIUS,
                LOWER_PATROL_AREA_RADIUS*2., LOWER_PATROL_AREA_RADIUS*2.,
                180, 180, Arc2D.CHORD);

        // define the two bounding shapes (one circle and a box)
        Ellipse2D.Double ew = new Ellipse2D.Double(
                -ENTRANCE_WINDOW_RADIUS, -ENTRANCE_WINDOW_RADIUS,
                ENTRANCE_WINDOW_RADIUS*2., ENTRANCE_WINDOW_RADIUS*2.0);

        Rectangle2D.Double paLimit = new Rectangle2D.Double(
                -ENTRANCE_WINDOW_RADIUS, -ENTRANCE_WINDOW_RADIUS,
                ENTRANCE_WINDOW_RADIUS+PATROL_AREA_HI_LIMIT, 2*ENTRANCE_WINDOW_RADIUS);

        // combination of lower and upper patrol areas (two half-circles)...
        Area pfArea = new Area(upperPa);
        pfArea.add(new Area(lowerPa));
        // .. intersected with bounding areas gives the patrol field
        pfArea.intersect(new Area(ew));
        pfArea.intersect(new Area(paLimit));

        // there we go...
        patrolField = new PatrolField(pfArea);
    }

    public String getKey() {
        return "FII OIWFS";
    }

    public String toString() {
        return getKey();
    }

    public Type getType() {
        return Type.OIWFS;
    }

    public String getDisplayName() {
        return "Flamingos-2 OIWFS";
    }

    public String getSequenceProp() {
        return "guideWithOIWFS";
    }

    public GuideOptions getGuideOptions() {
        return StandardGuideOptions.instance;
    }

    public Option<GuideProbeGroup> getGroup() {
        return new Some<GuideProbeGroup>(Group.instance);
    }

    @Override
    public GuideStarValidation validate(SPTarget guideStar, ObsContext ctx) {
        return GuideProbeUtil.instance.validate(guideStar, this, ctx);
    }

    @Override
    public boolean inRange(ObsContext ctx, Offset offset) {
        return GuideProbeUtil.instance.inRange(this, ctx, offset);
    }

    /**
     * Gets the group of Flamingos guide stars.
     * See OT-21.
     */
    public static enum Group implements GemsGuideProbeGroup {
        instance;

        public Angle getRadiusLimits() {
            return new Angle(3.05, Angle.Unit.ARCMINS);
        }

        public String getKey() {
            return "FII OIWFS";
        }

        public String getDisplayName() {
            return "Flamingos-2 OIWFS";
        }

        public Collection<ValidatableGuideProbe> getMembers() {
            ValidatableGuideProbe[] vals = Flamingos2OiwfsGuideProbe.values();
            return Arrays.asList(vals);
        }

        public String toString() {
            return getKey();
        }
    }

    @Override
    public PatrolField getPatrolField() {
        return patrolField;
    }

    @Override
    public Option<PatrolField> getCorrectedPatrolField(ObsContext ctx) {
        if (ctx.getInstrument() instanceof Flamingos2) {
            // Although normally this function only returns flips and offsets, I'm going to do the scaling to the LyotWheel plate scale here
            final Flamingos2 f2 = (Flamingos2) ctx.getInstrument();
            final double plateScale = f2.getLyotWheel().getPlateScale();
            final AffineTransform xform = AffineTransform.getScaleInstance(plateScale, plateScale);

            // Flip it for the port?
            // Validate(ctx.getAoComponent() == null) -> getFlipConfig(false)
            final boolean flip = f2.getFlipConfig(false);
            if (flip) {
                //Flip in X only
                xform.concatenate(AffineTransform.getScaleInstance(-1.0, 1.0));
            }

            final int sign = flip ? -1 : 1;
            final double rotationAngleInRadians = f2.getRotationConfig(false).toRadians().getMagnitude();
            final AffineTransform rotateForPortAndFlip = AffineTransform.getRotateInstance(rotationAngleInRadians * sign);
            xform.concatenate(rotateForPortAndFlip);

            // Apply complete transform
            return new Some<>(new PatrolField(xform.createTransformedShape(getPatrolField().getArea())));
        } else {
            return None.instance();
        }
    }

    @Override
    public VignettingCalculator calculator(ObsContext ctx) {
        return VignettingCalculator$.MODULE$.apply(ctx,
                F2OiwfsProbeArm$.MODULE$,
                F2ScienceAreaGeometry$.MODULE$);
    }

    @Override
    public BandsList getBands() { return RBandsList.instance(); }
}
