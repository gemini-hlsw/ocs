package edu.gemini.spModel.guide;

import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.CoordinateDiff;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.util.immutable.Trio;
import edu.gemini.shared.util.immutable.Tuple3;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.InstGmosCommon;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.telescope.IssPort;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Set;

/**
 * Encapsulates the shared algorithm for validating a guide probe
 */
public enum BoundaryValidator {
    instance;

    public boolean validate(Coordinates guideStar, ObsContext ctx, IBoundaryChecker probe) {
        final Angle positionAngle = ctx.getPositionAngle();
        final Set<Offset> sciencePositions = ctx.getSciencePositions();
        final IssPort issPort = ctx.getIssPort();
        final InstGmosCommon instrument = (InstGmosCommon) ctx.getInstrument();
        final Offset offset = new CoordinateDiff(guideStar, ctx.getBaseCoordinates()).getOffset();


        final Tuple3<AffineTransform, Double, Double> xformT = getTransform(positionAngle, instrument, offset);


        if ((checkBoundaries(probe, xformT, sciencePositions, issPort) == BoundaryPosition.outside) ||
                (checkBoundaries(probe, xformT, sciencePositions, issPort) == BoundaryPosition.outerBoundary)) {
            return false;
        } else {
            return true;
        }
    }


    public BoundaryPosition checkBoundaries(IBoundaryChecker probe, Tuple3<AffineTransform, Double, Double> xformT, Set<Offset> offsets, IssPort issPort) {

        AffineTransform xform = xformT._1();
        double p = xformT._2().doubleValue();
        double q = xformT._3().doubleValue();

        Area a = new Area(offsetIntersection(offsets, probe.getFovIn(), issPort));
        a.transform(xform);
        if (a.contains(p, q)) return BoundaryPosition.inside;

        a = new Area(offsetIntersection(offsets, probe.getFov(), issPort));
        a.transform(xform);
        if (a.contains(p, q)) return BoundaryPosition.innerBoundary;

        a = new Area(offsetIntersection(offsets, probe.getFovOut(), issPort));
        a.transform(xform);
        if (a.contains(p, q)) return BoundaryPosition.outerBoundary;

        return BoundaryPosition.outside;
    }

    private Tuple3<AffineTransform, Double, Double> getTransform(Angle positionAngle, InstGmosCommon instrument, Offset dis) {
        double p = -dis.p().toArcsecs().getMagnitude();
        double q = -dis.q().toArcsecs().getMagnitude();


        // Get a rotation to transform the shape to the position angle.
        AffineTransform xform = new AffineTransform();
        xform.rotate(-positionAngle.toRadians().getMagnitude());

        //apply IFU offset
        Point2D.Double ifuOffset = getIfuOffset(instrument);

        xform.deltaTransform(ifuOffset, ifuOffset);
        p += ifuOffset.getX();
        q += ifuOffset.getY();

        return new Trio<AffineTransform, Double, Double>(xform, p, q);
    }

    private Point2D.Double getIfuOffset(InstGmosCommon instrument) {
        return new Point2D.Double(-((GmosCommonType.FPUnit) instrument.getFPUnit()).getWFSOffset(), 0);
    }

    /**
     * Gets the intersection of the FOV and the offsets.
     *
     * @param offsets positions to include in the intersection
     */
    public Shape offsetIntersection(Collection<Offset> offsets, Rectangle2D shape, IssPort port) {
        Rectangle2D r = (port == IssPort.SIDE_LOOKING) ? flip(shape) : shape;

        Area area = new Area(r);
        if ((offsets == null) || (offsets.size() == 0)) {
            return area;
        }
        Area intersection = null;
        for (Offset off : offsets) {
            double x = -off.p().toArcsecs().getMagnitude();
            double y = -off.q().toArcsecs().getMagnitude();
            AffineTransform xform = AffineTransform.getTranslateInstance(x, y);
            if (intersection == null) {
                intersection = new Area(area);
                intersection.transform(xform);
            } else {
                Area tmp = new Area(area);
                tmp.transform(xform);
                intersection.intersect(tmp);
            }
        }

        return intersection;
    }

    private static Rectangle2D flip(Rectangle2D rect) {
        Rectangle2D r = rect.getBounds2D();
        double y = -r.getBounds2D().getY() - r.getBounds2D().getHeight();
        return new Rectangle2D.Double(r.getX(), y, r.getWidth(), r.getHeight());
    }

}
