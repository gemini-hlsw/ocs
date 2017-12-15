package edu.gemini.spModel.gemini.gems;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.CoordinateDiff;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.skycalc.Offset;
import edu.gemini.spModel.core.BandsList;
import edu.gemini.spModel.core.RBandsList;
import edu.gemini.spModel.gems.GemsGuideProbeGroup;
import edu.gemini.spModel.guide.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.Asterism;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.telescope.IssPort;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

/**
 * A description of the relevant features of Canopus.
 */
public enum Canopus {
    instance;

    /**
     * Canopus guide probe options.
     * Vignetting is no longer an issue due to move to NGS2.
     */
    public enum Wfs implements GuideProbe, ValidatableGuideProbe, OffsetValidatingGuideProbe {
        cwfs1(1) {
            @Override
            public Area probeRange(final ObsContext ctx) {
                return Canopus.instance.probeRange1(ctx);
            }
        },

        cwfs2(2) {
            @Override
            public Area probeRange(final ObsContext ctx) {
                return Canopus.instance.probeRange2(ctx);
            }
        },

        cwfs3(3) {
            @Override
            public Area probeRange(final ObsContext ctx) {
                return Canopus.instance.probeRange3(ctx);
            }
        };

        @Override
        public Option<PatrolField> getCorrectedPatrolField(final ObsContext ctx) {
            // Not implemented yet: return an empty area.
            return ctx.getAOComponent().filter(ado -> ado instanceof Gems).map(a -> new PatrolField(new Area()));
        }

        @Override
        public BandsList getBands() {
            return RBandsList.instance();
        }

        /**
         * Gets the group of Canopus guide stars.
         * See OT-21.
         */
        public enum Group implements GemsGuideProbeGroup {
            instance;

            public Angle getRadiusLimits() {
                return new Angle(1, Angle.Unit.ARCMINS);
            }

            public String getKey() {
                return "CWFS";
            }

            public String getDisplayName() {
                return "Canopus Wave Front Sensor";
            }

            public Collection<ValidatableGuideProbe> getMembers() {
                ValidatableGuideProbe[] vals = Wfs.values();
                return Arrays.asList(vals);
            }
        }

        private final int index;

        Wfs(final int index) {
            this.index = index;
        }

        public String getKey() {
            return "CWFS" + index;
        }

        public String toString() {
            return getKey();
        }

        public Type getType() {
            return Type.AOWFS;
        }

        public String getDisplayName() {
            return "Canopus WFS " + index;
        }

        public int getIndex() {
            return index;
        }

        public String getSequenceProp() {
            return "guideWithCWFS" + index;
        }

        public GuideOptions getGuideOptions() {
            return StandardGuideOptions.instance;
        }

        public Option<GuideProbeGroup> getGroup() {
            return new Some<>(Group.instance);
        }

        // not implemented yet, return an empty area
        @Override
        public PatrolField getPatrolField() {
            return new PatrolField(new Area());
        }

        /**
         * Gets the shape of the Canopus guide probe range, which is dependent
         * upon the current choice of the primary CWFS3 star and its position
         * relative to the base.
         *
         * @param ctx context of the given observation
         *
         * @return Shape describing the range of the guide probe
         */
        public abstract Area probeRange(final ObsContext ctx);


        public GuideStarValidation validate(final SPTarget guideStar, final ObsContext ctx) {
            final Option<Long> when = ctx.getSchedulingBlockStart();
            final Wfs self = this;
            return guideStar.getSkycalcCoordinates(when).map(coords ->
                Canopus.instance.getProbesInRange(coords, ctx).contains(self) ?
                        GuideStarValidation.VALID : GuideStarValidation.INVALID
            ).getOrElse(GuideStarValidation.UNDEFINED);
        }

        /**
         * Check if the primary guide star is in range from the given offset
         *
         * @param ctx ObsContext to get guide star and base coordinates from.
         * @param offset to check if the guide star is in range
         * @return true if guide star is in range from the given offset, false otherwise
         */
        public boolean inRange(final ObsContext ctx, final Offset offset) {
            return ctx.getTargets().getPrimaryGuideProbeTargets(this)
                    .flatMap(GuideProbeTargets::getPrimary)
                    .flatMap(gs -> gs
                            .getSkycalcCoordinates(ctx.getSchedulingBlockStart())
                            .flatMap(gscoords -> ctx.getBaseCoordinates()
                                    .map(base -> {
                                        final CoordinateDiff diff = new CoordinateDiff(base, gscoords);

                                        // Get offset and switch it to be defined in the same coordinate
                                        // system as the shape.
                                        final Offset dis = diff.getOffset();
                                        final double p = -dis.p().toArcsecs().getMagnitude();
                                        final double q = -dis.q().toArcsecs().getMagnitude();
                                        final Set<Offset> offsets = new TreeSet<>();
                                        offsets.add(offset);

                                        final Area a = Canopus.instance.offsetIntersection(ctx, offsets);
                                        return a != null && a.contains(p, q);
                                    })))
                    .getOrElse(false);
        }
    }

    public static final double RADIUS_ARCSEC = 62.5;

    // P1 can access a square field of 3.7 x 3.0 armcin, but the center is at (+0.11,+0.3)
    // P2 can access a square field of 4.2 x 3.0 arcmin, but the center is at (0.0,-0.33)
    private static final Point2D.Double PROBE1_CENTER = new Point2D.Double(0.11 * 60.0, 0.3 * 60.0);
    private static final Point2D.Double PROBE2_CENTER = new Point2D.Double(0.0, -0.33 * 60.0);
    private static final Tuple2<Double, Double> PROBE1_DIM = new Pair<>(3.7 * 60.0, 3.0 * 60.0);
    private static final Tuple2<Double, Double> PROBE2_DIM = new Pair<>(4.2 * 60.0, 2.5 * 60.0);
    private static final Ellipse2D AO_PORT = new Ellipse2D.Double(-RADIUS_ARCSEC, -RADIUS_ARCSEC, RADIUS_ARCSEC*2, RADIUS_ARCSEC*2);

    /**
     * Returns a Shape that defines the AO port in arcsecs, centered at
     * <code>(0,0)</code>.
     */
    private static final Shape AO_PORT_SHAPE = new Ellipse2D.Double(AO_PORT.getX(), AO_PORT.getY(), AO_PORT.getWidth(), AO_PORT.getHeight());

    // Gets the primary CWFS 3 guide star, if any.
    private Option<SPTarget> getPrimaryCwfs3(final ObsContext ctx) {
        return ImOption.apply(ctx.getTargets())
                .flatMap(env -> env.getPrimaryGuideProbeTargets(Wfs.cwfs3))
                .flatMap(GuideProbeTargets::getPrimary);
    }

    // Gets the coordinates of the primary CWFS guide star relative to the
    // base position, translated to screen coordinates.
    private Option<Point2D> getPrimaryCwfs3Offset(final ObsContext ctx) {
        return getPrimaryCwfs3(ctx).flatMap(target -> {
            final Asterism asterism = ctx.getTargets().getAsterism();
            final Option<Long> when = ctx.getSchedulingBlockStart();

            return
                    asterism.getSkycalcCoordinates(when).flatMap(bc ->
                            target.getSkycalcCoordinates(when).map(tc -> {
                                final CoordinateDiff diff = new CoordinateDiff(bc, tc);
                                final Offset o = diff.getOffset();
                                final double p = -o.p().toArcsecs().getMagnitude();
                                final double q = -o.q().toArcsecs().getMagnitude();
                                return new Point2D.Double(p, q);
                            }));
                }

        );
    }

    private static final Map<IssPort,Angle> rotation = new HashMap<IssPort,Angle>(IssPort.values().length) {{
        put(IssPort.SIDE_LOOKING, Angle.ANGLE_PI_OVER_2);
        put(IssPort.UP_LOOKING, Angle.ANGLE_0DEGREES);
    }};

    public synchronized static Angle getRotationConfig(final IssPort port) {
        return rotation.get(port);
    }

    public synchronized static void setRotationConfig(final IssPort port, final Angle angle) {
        Canopus.rotation.put(port, angle);
    }

    // Returns the probe range given the context, mid point and dimensions in arcsec
    private Area probe3DependentRange(final ObsContext ctx, final Point2D.Double mid, final Tuple2<Double, Double> dim) {
        // Gets a rectangle that covers the whole AO port size in width, and
        // has the proper height.
        final Area rect = new Area(new Rectangle2D.Double(
                mid.x - dim._1() / 2.,
                -mid.y - dim._2() / 2.,
                dim._1(), dim._2()));

        // Get the position angle and a transform that rotates in the direction
        // of the position angle, and one in the opposite direction.  Recall
        // that positive y is down and a positive rotation rotates the positive
        // x axis toward the positive y axis.  Position angle is expressed as
        // an angle east of north.
        final double t = ctx.getPositionAngle().toRadians()
                + getRotationConfig(ctx.getIssPort()).toRadians().getMagnitude();
        final AffineTransform rotWithPosAngle = AffineTransform.getRotateInstance(-t);
        final AffineTransform rotAgainstPosAngle = AffineTransform.getRotateInstance(t);

        // Get the valid range of the CWFS 3 probe at all offset positions and
        // compute the center of that range.
        final Area p3range = new Area(probeRange3(ctx));
        final Rectangle2D b = p3range.getBounds2D();
        final Point2D center = new Point2D.Double(b.getCenterX(), b.getCenterY());

        // We have to translate the rectangle.  If there is a primary CWFS3
        // star, then we translate the rectangle to there.  Otherwise, we assume
        // the center of the probe 3 range computed above.
        final Point2D xlat = getPrimaryCwfs3Offset(ctx).map(cwfs3 -> {
            // Rotate both the cwfs position and the center of the probe 3
            // range to 0 degrees.
            rotAgainstPosAngle.transform(center, center);
            rotAgainstPosAngle.transform(cwfs3, cwfs3);

            // Gets a point using the y value of the cwfs star and x value of
            // the center position.  We could just translate to the cwfs star
            // position, but then the width of the rectangle wouldn't be in
            // position to cover the probe 3 range.
            final Point2D p = new Point2D.Double(cwfs3.getX(), cwfs3.getY());
            return rotWithPosAngle.transform(p, p);
        }).getOrElse(center);

        rect.transform(rotWithPosAngle);
        rect.transform(AffineTransform.getTranslateInstance(xlat.getX(), xlat.getY()));
        rect.intersect(p3range);
        return rect;
    }

    public Area probeRange1(final ObsContext ctx) {
        return probe3DependentRange(ctx, PROBE1_CENTER, PROBE1_DIM);
    }

    public Area probeRange2(final ObsContext ctx) {
        return probe3DependentRange(ctx, PROBE2_CENTER, PROBE2_DIM);
    }

    public Area probeRange3(final ObsContext ctx) {
        return offsetIntersection(ctx, ctx.getSciencePositions());
    }

    /**
     * Gets the intersection of the FOV and the offsets.
     *
     * @param offsets positions to include in the intersection
     */
    public Area offsetIntersection(final ObsContext ctx, final Set<Offset> offsets) {
        // Note: If offsets are ever empty, this will return null, and this is not checked for anywhere.
        Area res = null;

        final double t = ctx.getPositionAngle().toRadians();

        for (final Offset pos : offsets) {
            final Area cur = new Area(AO_PORT_SHAPE);

            final double p = pos.p().toArcsecs().getMagnitude();
            final double q = pos.q().toArcsecs().getMagnitude();

            final AffineTransform xform = new AffineTransform();
            if (t != 0.0) xform.rotate(-t);
            xform.translate(-p, -q);
            cur.transform(xform);

            if (res == null) {
                res = cur;
            } else {
                res.intersect(cur);
            }
        }

        return res;
    }

    /**
     * Gets the set of guide probes that can reach the provided coordinates
     * in the given observing context (if any).
     */
    public Set<Wfs> getProbesInRange(Coordinates coords, ObsContext ctx) {
        Set<Wfs> res = new HashSet<>();

        ctx.getBaseCoordinates().foreach(bcs -> {
            // Calculate the difference between the coordinate and the observation's
            // base position.
            CoordinateDiff diff;
            diff = new CoordinateDiff(bcs, coords);

            // Get offset and switch it to be defined in the same coordinate
            // system as the shape.
            Offset dis = diff.getOffset();
            double p = -dis.p().toArcsecs().getMagnitude();
            double q = -dis.q().toArcsecs().getMagnitude();

            if (probeRange1(ctx).contains(p, q)) res.add(Wfs.cwfs1);
            if (probeRange2(ctx).contains(p, q)) res.add(Wfs.cwfs2);
            if (probeRange3(ctx).contains(p, q)) res.add(Wfs.cwfs3);
        });

        return res;
    }
}
