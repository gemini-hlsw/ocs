package edu.gemini.spModel.gemini.gems;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.CoordinateDiff;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.skycalc.Offset;
import edu.gemini.spModel.core.*;
import edu.gemini.spModel.gems.GemsGuideProbeGroup;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;

import java.awt.geom.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Canopus WFS guide probes.
 */
public enum CanopusWfs implements GuideProbe, ValidatableGuideProbe, OffsetValidatingGuideProbe {
    cwfs1(1),
    cwfs2(2),
    cwfs3(3),;

    private static final double RADIUS_ARCSEC = 62.5;
    private static final PatrolField patrolField;
    static {
        final Ellipse2D AO_PORT = new Ellipse2D.Double(-RADIUS_ARCSEC, -RADIUS_ARCSEC, RADIUS_ARCSEC * 2, RADIUS_ARCSEC * 2);
        patrolField = new PatrolField(AO_PORT);
    }

    @Override
    public BandsList getBands() {
        return RBandsList.instance();
    }

    /**
     * Gets the group of Canopus guide probes and their properties.
     */
    public enum Group implements GemsGuideProbeGroup {
        instance;

        @Override
        public Angle getRadiusLimits() {
            return new Angle(1, Angle.Unit.ARCMINS);
        }

        @Override
        public String getKey() {
            return "CWFS";
        }

        @Override
        public String getDisplayName() {
            return "Canopus Wave Front Sensor";
        }

        @Override
        public Collection<ValidatableGuideProbe> getMembers() {
            return Arrays.asList(CanopusWfs.values());
        }

        // Stars selected for CWFS must be at most mag 3 apart.
        public static final double MAGNITUDE_DIFFERENCE_LIMIT = 3.0;

        @Override
        public boolean asterismPreFilter(final ImList<SiderealTarget> targets) {
            return checkAsterismMagnitude(targets);
        }

        public boolean checkAsterismMagnitude(final ImList<SiderealTarget> targets) {
            final ImList<Double> sortedMags = extractRMagnitudes(targets);
            if (sortedMags.isEmpty() || sortedMags.size() != targets.size())
                return false;
            return (sortedMags.size() == 1) || (sortedMags.last() - sortedMags.head() <= MAGNITUDE_DIFFERENCE_LIMIT);
        }

        public ImList<Double> extractRMagnitudes(final ImList<SiderealTarget> targets) {
            return targets
                    .map(t -> ImOption.fromScalaOpt(RBandsList.extract(t)))
                    .filter(Option::isDefined)
                    .map(m -> m.getValue().value())
                    .sort(Comparator.naturalOrder());
        }
    }

    private final int index;

    CanopusWfs(final int index) {
        this.index = index;
    }

    @Override
    public String getKey() {
        return "CWFS" + index;
    }

    @Override
    public String toString() {
        return getKey();
    }

    @Override
    public Type getType() {
        return Type.AOWFS;
    }

    @Override
    public String getDisplayName() {
        return "Canopus WFS " + index;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String getSequenceProp() {
        return "guideWithCWFS" + index;
    }

    @Override
    public GuideOptions getGuideOptions() {
        return StandardGuideOptions.instance;
    }

    @Override
    public Option<GuideProbeGroup> getGroup() {
        return new Some<>(Group.instance);
    }

    @Override
    public PatrolField getPatrolField() {
        return patrolField;
    }

    // The patrol field for Canopus is simply a fully symmetric circle, so we don't need to apply any rotations
    // to it to correct it.
    @Override
    public Option<PatrolField> getCorrectedPatrolField(final ObsContext ctx) {
        return ctx.getAOComponent().filter(ado -> ado instanceof Gems).map(a -> patrolField);
    }

    @Override
    public GuideStarValidation validate(final SPTarget guideStar, final ObsContext ctx) {
        final Option<Long> when = ctx.getSchedulingBlockStart();
        return guideStar.getSkycalcCoordinates(when).map(coords ->
                areProbesInRange(coords, ctx) ? GuideStarValidation.VALID : GuideStarValidation.INVALID
        ).getOrElse(GuideStarValidation.UNDEFINED);
    }

    /**
     * Returns an Area representing the probe range. This is used in drawing the probe range.
     */
    public static Area probeRange(final ObsContext ctx) {
        return offsetIntersection(ctx, ctx.getSciencePositions());
    }

    /**
     * Check if the primary guide star is in range from the given offset
     */
    @Override
    public boolean inRange(final ObsContext ctx, final Offset offset) {
        return ctx.getTargets().getPrimaryGuideProbeTargets(this)
                .flatMap(GuideProbeTargets::getPrimary)
                .flatMap(gs -> gs.getSkycalcCoordinates(ctx.getSchedulingBlockStart())
                        .map(gscoords -> areProbesInRangeWithOffsets(gscoords, ctx, Collections.singleton(offset)))
                ).getOrElse(false);
    }

    /**
     * Determines if the guide probe can reach the provided coordinates in the given observing context (if any).
     */
    public static boolean areProbesInRange(final Coordinates coords, final ObsContext ctx) {
        return areProbesInRangeWithOffsets(coords, ctx, ctx.getSciencePositions());
    }

    /**
     * Helper method to extract the common code from inRange and areProbesInRange.
     */
    private static boolean areProbesInRangeWithOffsets(final Coordinates coords, final ObsContext ctx, final Set<Offset> offsets) {
        return ctx.getBaseCoordinates().map(bcs -> {
            // Calculate the difference between the coordinate and the observation's base position.
            final CoordinateDiff diff = new CoordinateDiff(bcs, coords);

            // Get offset and switch it to be defined in the same coordinate system as the shape.
            final Offset dis = diff.getOffset();
            final double p = -dis.p().toArcsecs().getMagnitude();
            final double q = -dis.q().toArcsecs().getMagnitude();

            return offsetIntersection(ctx, offsets).contains(p, q);
        }).getOrElse(false);
    }

    /**
     * Gets the intersection of the FOV and the specified offsets.
     */
    private static Area offsetIntersection(final ObsContext ctx, final Set<Offset> offsets) {
        final edu.gemini.spModel.core.Angle pa = ctx.getPositionAngle();

        return offsets.stream().map(pos -> {
            final double p = pos.p().toArcsecs().getMagnitude();
            final double q = pos.q().toArcsecs().getMagnitude();

            final AffineTransform xform = new AffineTransform();
            if (!pa.equals(Angle$.MODULE$.zero())) xform.rotate(-pa.toRadians());
            xform.translate(-p, -q);

            return patrolField.getArea().createTransformedArea(xform);
        }).reduce((a1, a2) -> {
            final Area result = (Area) a1.clone();
            result.intersect(a2);
            return result;
        }).orElse(patrolField.getArea());
    }
}
