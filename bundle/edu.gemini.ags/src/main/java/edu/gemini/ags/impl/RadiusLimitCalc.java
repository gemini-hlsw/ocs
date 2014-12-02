package edu.gemini.ags.impl;

import edu.gemini.catalog.api.RadiusLimits;
import edu.gemini.shared.util.immutable.MapOp;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.skycalc.Angle;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.PatrolField;
import edu.gemini.spModel.obs.context.ObsContext;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// SW: I pulled this out from the spModel GuideProbeUtil.  It makes my head
// hurt so I'll just assume it is correct and not try to reimplement it in
// Scala.

public final class RadiusLimitCalc {
    private RadiusLimitCalc() {}

    public static Option<RadiusLimits> getAgsQueryRadiusLimits(GuideProbe guideProbe, ObsContext ctx) {
        return getAgsQueryRadiusLimits(guideProbe.getCorrectedPatrolField(ctx), ctx);
    }

    public static Option<RadiusLimits> getAgsQueryRadiusLimits(final Option<PatrolField> pf, final ObsContext ctx) {
        Angle min = Angle.ANGLE_0DEGREES;

        // This gets a rectangle expressed in arcsec but in screen coordinates
        // as if the base position were the upper left corner (0,0).
        // x increases to the right, and y towards the bottom. :-(
        // So it would need to be flipped around to work with intuitively,
        // but we just need the distance from the base to the farthest corner
        // of the probe limits.
        final Rectangle2D r2d = pf.map(new MapOp<PatrolField, Rectangle2D>() {
            @Override public Rectangle2D apply(PatrolField pf) {
                return pf.outerLimitOffsetIntersection(ctx.getSciencePositions()).getBounds2D();
            }
        }).getOrElse(new Rectangle2D.Double(0,0,0,0));

        if ((r2d.getWidth() <= 0) || (r2d.getHeight() <= 0)) {
            // All the offset positions are far enough apart that the area of
            // this rectangle is 0.  It's impossible to find any guide stars in
            // range at all positions.  We'll just go ahead and do a search
            // limited by the probe ranges as if there were no offset positions.
            // It may find candidates, but will ultimately fail in the
            // analysis.
            return None.instance();
        }

        // We need the fartherest corner, which will differ with port flips
        // so just get the biggest absolute x and y.
        final double maxx = Math.max(Math.abs(r2d.getMinX()), Math.abs(r2d.getMaxX()));
        final double maxy = Math.max(Math.abs(r2d.getMinY()), Math.abs(r2d.getMaxY()));
        final double maxr = Math.sqrt(maxx * maxx + maxy * maxy);
        final Angle max = new Angle(maxr, Angle.Unit.ARCSECS);

        if (!r2d.contains(0, 0)) {
            min = new Angle(shortestDistance(r2d), Angle.Unit.ARCSECS);
        }

        return new Some<>(new RadiusLimits(max, min));
    }

    private static double shortestDistance(Rectangle2D r) {
        double minx = r.getMinX();
        double miny = r.getMinY();
        double maxx = r.getMaxX();
        double maxy = r.getMaxY();

        // shortest distance to a horizontal line segment
        double dh = Math.abs(miny) < Math.abs(maxy) ?
                shortestDistanceToHorizontalLine(new Line2D.Double(minx, miny, maxx, miny)) :
                shortestDistanceToHorizontalLine(new Line2D.Double(minx, maxy, maxx, maxy));

        // shortest distance to a vertical line segment
        double dv = Math.abs(minx) < Math.abs(maxx) ?
                shortestDistanceToVerticalLine(new Line2D.Double(minx, miny, minx, maxy)) :
                shortestDistanceToVerticalLine(new Line2D.Double(maxx, miny, maxx, maxy));

        return Math.min(dh, dv);
    }

    private static final Point2D ZERO = new Point2D.Double(0, 0);

    private static double shortestDistanceToHorizontalLine(Line2D l) {
        Point2D p1 = l.getP1();
        Point2D p2 = l.getP2();
        if ((p1.getX() <= 0) && (0 <= p2.getX())) {
            // If the center falls between the horizontal extremes of the
            // segment, it's the veritical distance to the line.
            return Math.abs(p1.getY());
        } else {
            return Math.min(ZERO.distance(p1), ZERO.distance(p2));
        }
    }

    private static double shortestDistanceToVerticalLine(Line2D l) {
        Point2D p1 = l.getP1();
        Point2D p2 = l.getP2();
        if ((p1.getY() <= 0) && (0 <= p2.getY())) {
            // If the center falls between the vertical extremes of the
            // segment, it's the horizontal distance to the line.
            return Math.abs(p1.getX());
        } else {
            return Math.min(ZERO.distance(p1), ZERO.distance(p2));
        }
    }

}
