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
import java.util.List;

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

    // According to NGS2, guide stars should be centered in square 4" windows.
    private static final double      GS_WINDOW_SIZE_ARCSEC = 4.0;
    private static final Rectangle2D GS_WINDOW_SHAPE       = new Rectangle2D.Double(-GS_WINDOW_SIZE_ARCSEC / 2.0, -GS_WINDOW_SIZE_ARCSEC / 2.0, GS_WINDOW_SIZE_ARCSEC, GS_WINDOW_SIZE_ARCSEC);
    private static final Area        GS_WINDOW             = new Area(GS_WINDOW_SHAPE);

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

        // Ensure that targets are not more than MAGNITUDE_DIFFERENCE_LIMIT apart in magnitude in R.
        public static boolean checkAsterismMagnitude(final ImList<SiderealTarget> targets) {
            final ImList<Double> sortedMags = extractRMagnitudes(targets);
            if (sortedMags.isEmpty() || sortedMags.size() != targets.size())
                return false;
            return (sortedMags.size() == 1) || (sortedMags.last() - sortedMags.head() <= MAGNITUDE_DIFFERENCE_LIMIT);
        }

        // Guide windows cannot overlap.
        // As these are square, we need a position angle to determine if they overlap or not.
        @Override
        public boolean asterismFilter(final ObsContext ctx, final ImList<SiderealTarget> targets) {
            return findOverlappingGuideWindows(ctx, targets).isEmpty();
        }

        // Given an ObsContext (we need a position angle) and a list of targets, find the list of pairs of targets
        // whose guide windows overlap.
        public static ImList<Pair<SiderealTarget, SiderealTarget>> findOverlappingGuideWindows(final ObsContext ctx, final ImList<SiderealTarget> targets) {
            final double posAngle = ctx.getPositionAngle().toRadians();

            // Check each pair of guide windows to make sure they do not overlap.
            final List<Pair<SiderealTarget, SiderealTarget>> overlappingGuideWindows = new ArrayList<>(3);

            // Must use indices here to avoid double pairs of the form (t1,t2) and (t2,t1).
            for (int i=0; i < targets.size(); ++i) {
                final SiderealTarget t1 = targets.get(i);
                final Area a1 = windowFromCoordinates(posAngle, t1.coordinates());
                for (int j=i+1; j < targets.size(); ++j) {
                    final SiderealTarget t2 = targets.get(j);
                    final Area a2 = windowFromCoordinates(posAngle, t2.coordinates());

                    // To check overlap, take a1 xor a2 and compare to a1 union a2.
                    // If they are not equal, they overlap.
                    final Area xorArea = (Area) a1.clone();
                    xorArea.exclusiveOr(a2);

                    final Area combinedArea = (Area) a1.clone();
                    combinedArea.add(a2);

                    if (!xorArea.equals(combinedArea))
                        overlappingGuideWindows.add(new Pair<>(t1, t2));
                }
            }

            return DefaultImList.create(overlappingGuideWindows);
        }

        // Convenience method to create a transformed guide window from a position angle and a set of coordinates.
        private static Area windowFromCoordinates(final double posAngle, final edu.gemini.spModel.core.Coordinates c) {
            final AffineTransform trans = AffineTransform.getTranslateInstance(
                    c.ra().toAngle().toArcsecs(),
                    c.dec().toAngle().toArcsecs());
            trans.rotate(-posAngle);
            return new Area(trans.createTransformedShape(GS_WINDOW_SHAPE));
        }

        // Convenience method to extract the R magnitudes from a list of targets.
        public static ImList<Double> extractRMagnitudes(final ImList<SiderealTarget> targets) {
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

    public static Area getGuideStarWindow() {
        return (Area) GS_WINDOW.clone();
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
            final Area offsetInt = offsetIntersection(ctx, offsets);

            // Calculate the difference between the coordinate and the observation's base position.
            final CoordinateDiff diff = new CoordinateDiff(bcs, coords);

            // Get offset and switch it to be defined in the same coordinate system as the shape.
            final Offset dis = diff.getOffset();
            final double p = -dis.p().toArcsecs().getMagnitude();
            final double q = -dis.q().toArcsecs().getMagnitude();

            // We need the window around the coordinates to be fully in the Canopus patrol field.
            // Calculate the patrol field + the window, and ensure it is the same as the patrol field.
            final AffineTransform trans = AffineTransform.getTranslateInstance(p, q);
            trans.rotate(-ctx.getPositionAngle().toRadians());

            final Area offsetIntWithWindow = (Area) offsetInt.clone();
            offsetIntWithWindow.add(new Area(trans.createTransformedShape(GS_WINDOW_SHAPE)));

            return offsetInt.equals(offsetIntWithWindow);
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
