package edu.gemini.ags.impl

import edu.gemini.catalog.api.RadiusConstraint
import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.guide.PatrolField
import edu.gemini.spModel.obs.context.ObsContext
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import scala.math._

import edu.gemini.spModel.rich.shared.immutable._

import scalaz._
import Scalaz._

// SW: I pulled this out from the spModel GuideProbeUtil. It makes my head
// hurt so I'll just assume it is correct
object RadiusLimitCalc {
  private val Zero: Point2D = new Point2D.Double(0, 0)

  def getAgsQueryRadiusLimits(guideProbe: GuideProbe, ctx: ObsContext): Option[RadiusConstraint] =
    getAgsQueryRadiusLimits(guideProbe.getCorrectedPatrolField(ctx).asScalaOpt, ctx)

  def getAgsQueryRadiusLimits(pf: Option[PatrolField], ctx: ObsContext): Option[RadiusConstraint] = {
    // This gets a rectangle expressed in arcsec but in screen coordinates
    // as if the base position were the upper left corner (0,0).
    // x increases to the right, and y towards the bottom. :-(
    // So it would need to be flipped around to work with intuitively,
    // but we just need the distance from the base to the farthest corner
    // of the probe limits.
    val r2d = pf.map(_.outerLimitOffsetIntersection(ctx.getSciencePositions).getBounds2D) | new Rectangle2D.Double(0, 0, 0, 0)

    // All the offset positions are far enough apart that the area of
    // this rectangle is 0.  It's impossible to find any guide stars in
    // range at all positions.  We'll just go ahead and do a search
    // limited by the probe ranges as if there were no offset positions.
    // It may find candidates, but will ultimately fail in the
    // analysis.
    (r2d.getWidth > 0 && r2d.getHeight > 0) option {
      // We need the farthest corner, which will differ with port flips
      // so just get the biggest absolute x and y.
      val maxx = max(abs(r2d.getMinX), abs(r2d.getMaxX))
      val maxy = max(abs(r2d.getMinY), abs(r2d.getMaxY))
      val maxr = sqrt(maxx * maxx + maxy * maxy)

      val maxAngle = Angle.fromArcsecs(maxr)
      val minAngle = if (r2d.contains(0, 0)) Angle.zero else Angle.fromArcsecs(shortestDistance(r2d))

      RadiusConstraint.between(maxAngle, minAngle)
    }
  }

  private def shortestDistance(r: Rectangle2D): Double = {
    val (minx, miny, maxx, maxy) = (r.getMinX, r.getMinY, r.getMaxX, r.getMaxY)

    val dh = if (abs(miny) < abs(maxy)) shortestDistanceToHorizontalLine(new Line2D.Double(minx, miny, maxx, miny)) else shortestDistanceToHorizontalLine(new Line2D.Double(minx, maxy, maxx, maxy))
    val dv= if (abs(minx) < abs(maxx)) shortestDistanceToVerticalLine(new Line2D.Double(minx, miny, minx, maxy)) else shortestDistanceToVerticalLine(new Line2D.Double(maxx, miny, maxx, maxy))
    min(dh, dv)
  }

  private def shortestDistanceToHorizontalLine(l: Line2D): Double = {
    shortestDistance(l) {(a, b) => (a.getX <= 0) && (0 <= b.getX)} {_.getY}
  }

  private def shortestDistanceToVerticalLine(l: Line2D): Double = {
    shortestDistance(l) {(a, b) => (a.getY <= 0) && (0 <= b.getY)} {_.getX}
  }

  private def shortestDistance(l: Line2D)(condition: (Point2D, Point2D) => Boolean)(result: Point2D => Double): Double = {
    val p1 = l.getP1
    val p2 = l.getP2
    if (condition(p1, p2)) {
      // If the center falls between the vertical extremes of the
      // segment, it's the horizontal distance to the line.
      abs(result(p1))
    } else {
      min(Zero.distance(p1), Zero.distance(p2))
    }
  }
}