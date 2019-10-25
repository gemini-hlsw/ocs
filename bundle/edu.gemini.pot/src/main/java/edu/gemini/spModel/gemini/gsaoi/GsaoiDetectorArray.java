//
// $
//

package edu.gemini.spModel.gemini.gsaoi;

import edu.gemini.skycalc.CoordinateDiff;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.telescope.IssPort;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * The GSAOI detector array singleton.
 */
public enum GsaoiDetectorArray {
    instance;

    // Define the 2 x 2 detector array.  There is a small gap between each
    // detector as defined below.

    // We use the Shape class here where the dimensions are specified in
    // arcsec.  Using screen coordinates to make working with shapes possible,
    // so y is increasing from top to bottom.

    public static final double DETECTOR_SIZE_MM = 36.72;   // mm
    public static final double DETECTOR_GAP_MM  =  2.5;    // mm
    public static final double SCALE            =  1.1154; // arcsec per mm

    public static final double DETECTOR_SIZE_ARCSEC = DETECTOR_SIZE_MM * SCALE;
    public static final double DETECTOR_GAP_ARCSEC  = DETECTOR_GAP_MM * SCALE;

    // Account for ODGW hotspot offset (in arcsec)
    public static final double ODGW_HOTSPOT_OFFSET_P = -5.0;
    public static final double ODGW_HOTSPOT_OFFSET_Q =  8.0;

    private static final Rectangle2D TOP_LEFT_SHAPE;
    static {
        double x = -1 * (DETECTOR_GAP_ARCSEC /2 + DETECTOR_SIZE_ARCSEC) + ODGW_HOTSPOT_OFFSET_P;
        double y = -1 * (DETECTOR_GAP_ARCSEC /2 + DETECTOR_SIZE_ARCSEC) + ODGW_HOTSPOT_OFFSET_Q;
        TOP_LEFT_SHAPE = new Rectangle2D.Double(x, y, DETECTOR_SIZE_ARCSEC, DETECTOR_SIZE_ARCSEC);
    }

    private static final Rectangle2D TOP_RIGHT_SHAPE;
    static {
        double x = DETECTOR_GAP_ARCSEC /2 + ODGW_HOTSPOT_OFFSET_P;
        double y = -1 * (DETECTOR_GAP_ARCSEC /2 + DETECTOR_SIZE_ARCSEC) + ODGW_HOTSPOT_OFFSET_Q;
        TOP_RIGHT_SHAPE = new Rectangle2D.Double(x, y, DETECTOR_SIZE_ARCSEC, DETECTOR_SIZE_ARCSEC);
    }

    private static final Rectangle2D BOTTOM_RIGHT_SHAPE;
    static {
        double x = DETECTOR_GAP_ARCSEC /2 + ODGW_HOTSPOT_OFFSET_P;
        double y = DETECTOR_GAP_ARCSEC /2 + ODGW_HOTSPOT_OFFSET_Q;
        BOTTOM_RIGHT_SHAPE = new Rectangle2D.Double(x, y, DETECTOR_SIZE_ARCSEC, DETECTOR_SIZE_ARCSEC);
    }

    private static final Rectangle2D BOTTOM_LEFT_SHAPE;
    static {
        double x = -1 * (DETECTOR_GAP_ARCSEC /2 + DETECTOR_SIZE_ARCSEC) + ODGW_HOTSPOT_OFFSET_P;
        double y = DETECTOR_GAP_ARCSEC /2 + ODGW_HOTSPOT_OFFSET_Q;
        BOTTOM_LEFT_SHAPE = new Rectangle2D.Double(x, y, DETECTOR_SIZE_ARCSEC, DETECTOR_SIZE_ARCSEC);
    }

    /**
     * Ids identifying each detector in the array 1 - 4.
     */
    public enum Id {
        one(1),
        two(2),
        three(3),
        four(4),
        ;

        private final int id;

        Id(int id) { this.id = id; }

        /**
         * @return numeric id, 1 - 4
         */
        public int index() { return id; }

        public Id next() {
            return values()[(ordinal() + 1) % 4];
        }

        public Id prev() {
            return this == one ? four : values()[ ordinal() - 1];
        }

        public String toString() { return String.valueOf(id); }
    }

    /**
     * Quadrant configuration.  The configuration is specified by identifying
     * the Id of the quadrant in the top left corner and the rotation direction
     * of the Ids.
     */
    public static final class Config {
        public enum Direction {
            clockwise()        { Id next(Id cur) { return cur.next(); }},
            counterClockwise() { Id next(Id cur) { return cur.prev(); }},
            ;
            abstract Id next(Id cur);

        }

        private final Id[] ids = new Id[4];

        public Config(Id tl, Direction rot) {
            Id cur = tl;
            for (int i=0; i<4; ++i) {
                this.ids[i] = cur;
                cur = rot.next(cur);
            }
        }

        public Id getId(Quadrant q) { return ids[q.ordinal()]; }
    }

    private static final Config[] quadConfig = new Config[IssPort.values().length];

    public static synchronized Config getQuadrantConfig(IssPort iss) {
        return quadConfig[iss.ordinal()];
    }

    public static synchronized void setQuadrantConfig(IssPort iss, Config config) {
        quadConfig[iss.ordinal()] = config;
    }

    static {
        setQuadrantConfig(IssPort.SIDE_LOOKING, new Config(Id.four, Config.Direction.counterClockwise));
        setQuadrantConfig(IssPort.UP_LOOKING,   new Config(Id.four, Config.Direction.counterClockwise));
    }

    /**
     * Detector array quadrants.  Each quadrant is specified relative to a
     * 0 degree position angle.  The {@link Id} for each quadrant changes
     * depending upon the IssPort.
     */
    public enum Quadrant {

        topLeft(TOP_LEFT_SHAPE),
        topRight(TOP_RIGHT_SHAPE),
        bottomRight(BOTTOM_RIGHT_SHAPE),
        bottomLeft(BOTTOM_LEFT_SHAPE),
        ;

        private final Rectangle2D shape;

        Quadrant(Rectangle2D shape) {
            this.shape         = shape;
        }

        /**
         * Returns a Shape that defines the detector array quadrant, relative
         * the center of the array at <code>(0, 0)</code>.
         */
        public Shape shape() {
            return (Shape) shape.clone();
        }

        /**
         * Gets the {@link Id} of this quadrant relative to the given
         * ISS port.
         */
        public Id id(IssPort port) {
            return getQuadrantConfig(port).getId(this);
        }
    }

    private static final GeneralPath ARRAY_SHAPE;

    static {
        ARRAY_SHAPE = new GeneralPath();
        for (Quadrant q : Quadrant.values()) ARRAY_SHAPE.append(q.shape(), false);
    }

    /**
     * Returns a Shape that defines the detector array in arcsecs, centered at
     * <code>(0,0)</code>, in screen coordinates.  This shape can be scaled to
     * pixels and rotated etc. for display.
     */
    public Shape shape() {
        return (Shape) ARRAY_SHAPE.clone();
    }

    /**
     * Returns a Shape that defines the combination of the detector array at
     * all the given offset positions.  This defines the sky coverage of the
     * offset positions.  The shape is expressed in arcsecs, centered at
     * <code>(0,0)</code> using screen coordinates.  This shape can be scaled
     * to pixels and rotated etc. for display.
     *
     * @param offsets offset positions of interest
     */
    public Shape coverage(List<Offset> offsets) {
        if ((offsets == null) || (offsets.size() == 0)) return shape();

        Area combined = new Area();
        for (Offset off : offsets) {
            double x = -off.p().toArcsecs().getMagnitude();
            double y = -off.q().toArcsecs().getMagnitude();

            Area tmp = new Area(shape());
            tmp.transform(AffineTransform.getTranslateInstance(x, y));
            combined.add(tmp);
        }

        return combined;
    }


    private List<Tuple2<Quadrant, Shape>> defaultQuadrantShapes() {
        List<Tuple2<Quadrant, Shape>> res = new ArrayList<>();
        for (Quadrant q : Quadrant.values()) {
            res.add(new Pair<>(q, q.shape()));
        }
        return res;
    }

    /**
     * Gets a list of pairs of detector array quadrant and a shape, where each
     * shape represents the intersection of the area that the quadrant covers
     * at each offset position.
     *
     * @param offsets positions to include in the intersection
     */
    public List<Tuple2<Quadrant, Shape>> quadrantIntersection(Collection<Offset> offsets) {
        if ((offsets == null) || (offsets.size() == 0)) {
            return defaultQuadrantShapes();
        }

        final Map<Quadrant, Area> map = new HashMap<>();
        for (final Offset off : offsets) {
            double x = -off.p().toArcsecs().getMagnitude();
            double y = -off.q().toArcsecs().getMagnitude();
            AffineTransform xform = AffineTransform.getTranslateInstance(x, y);

            for (Quadrant q : Quadrant.values()) {
                Area tmp = new Area(q.shape());
                tmp.transform(xform);

                Area intersection = map.get(q);
                if (intersection == null) {
                    map.put(q, tmp);
                } else {
                    intersection.intersect(tmp);
                }
            }
        }

        List<Tuple2<Quadrant, Shape>> res = new ArrayList<>();
        for (Quadrant q : Quadrant.values()) {
            res.add(new Pair<>(q, map.get(q)));
        }
        return res;
    }

    /**
     * Determines the Id associated with the detector array quadrant in which
     * the given coordinates land.  If the coordinate does not land on the
     * detector array for all offset positions, {@link None} is returned.
     *
     * @param coords sky coordinates whose position on the detector array is
     * sought
     *
     * @param ctx context of the observation -- its base position, position
     * angle, {@link IssPort}, and science positions
     *
     * @return {@link Some}<{@link Id}> associated with the detector array
     * quadrant in which the <code>coords</code> fall, or {@link None} if
     * off the array or base coordinates are unknown
     */
    public Option<Id> getId(Coordinates coords, ObsContext ctx) {
        return ctx.getBaseCoordinates().flatMap(base -> {
            // Calculate the difference between the coordinate and the observation's
            // base position.
            CoordinateDiff diff;
            diff = new CoordinateDiff(base, coords);

            // Get offset and switch it to be defined in the same coordinate
            // system as the shape.
            Offset offset = diff.getOffset();
            double p = -offset.p().toArcsecs().getMagnitude();
            double q = -offset.q().toArcsecs().getMagnitude();

            // Get a rotation to transform the shape to the position angle.
            AffineTransform xform = new AffineTransform();
            xform.rotate(-ctx.getPositionAngle().toRadians());

            // Check each quadrant to see if it contains the point, returning
            // a new Some if so.
            Set<Offset> offsets = ctx.getSciencePositions();
            for (Tuple2<Quadrant, Shape> t : quadrantIntersection(offsets)) {
                Area a = new Area(t._2());
                a.transform(xform);
                if (a.contains(p, q)) return new Some<>(t._1().id(ctx.getIssPort()));
            }

            // Not on the detector for all offset positions.
            return None.instance();
        });
    }
}
