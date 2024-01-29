package edu.gemini.model.p1.visibility

import edu.gemini.model.p1.immutable.TargetVisibility
import edu.gemini.spModel.core.Angle.{DMS, HMS}
import edu.gemini.spModel.core.{Declination, RightAscension}

private case class Degrees(raw: Double) extends Ordered[Degrees] {
  val deg: Double = ((raw % 360.0) + 360.0) % 360.0  // [0, 360)
  def compare(that: Degrees): Int = deg.compare(that.deg)
}

private sealed trait CoordinateDegrees[T] {
  def toDegrees(obj: T): Degrees
}

private object CoordinateDegrees {
  implicit object RaDegrees extends CoordinateDegrees[RightAscension] {
    def toDegrees(hms: RightAscension) = Degrees(hms.toAngle.toDegrees)
  }
  implicit object DecDegrees extends CoordinateDegrees[Declination] {
    def toDegrees(dms: Declination) = Degrees(dms.toDegrees)
  }
}

private case class DegRange(start: Degrees, end: Degrees) {
  def this(s: Double, e: Double) = this(Degrees(s), Degrees(e))
  private val comp: Degrees => Boolean =
    if (start < end)
      d => (start <= d) && (d < end)
    else
      d => (start <= d) || (0 <= d.deg && d < end)

  def includes[T](coord: T)(implicit ev: CoordinateDegrees[T]): Boolean =
    comp(ev.toDegrees(coord))
}

private case class VisibilityRange(v: TargetVisibility, r: DegRange)

private[visibility] case class VisibilityRangeList(ranges: List[VisibilityRange]) {
  def visibility[T : CoordinateDegrees](coord: T): TargetVisibility =
      ranges.find(_.r.includes(coord)).map(_.v).getOrElse(TargetVisibility.Bad)
}

private[visibility] object VisibilityRangeList {
  // Folded across a specification to produce a List[VisibilityRange].
  private case class F(lst: List[VisibilityRange], start: Double, vis: TargetVisibility)

  /** Creates a range list that covers the full 0-360 range according to specification. */
  def deg(visSeq: (Double, TargetVisibility)*): VisibilityRangeList =
    visSeq.toList match {
      case Nil    => VisibilityRangeList(Nil)
      case h :: t =>
        val (startDeg, startVis) = h
        val res = (F(Nil, startDeg, startVis)/:t) {
          (f, tup) => {
            val (curDeg, curVis) = tup
            val range = VisibilityRange(f.vis, new DegRange(f.start, curDeg))
            F(range :: f.lst, curDeg, curVis)
          }
        }
        VisibilityRangeList(VisibilityRange(res.vis, new DegRange(res.start, startDeg)) :: res.lst)
    }

  /**
   * Creates a range list from 0 to 24 hours. Converts the hours to degrees.
   */
  def hr(vis: (Double, TargetVisibility)*): VisibilityRangeList =
    deg(vis.map { case (h, v) => (h*15.0, v) }: _*)
}
