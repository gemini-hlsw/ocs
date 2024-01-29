package edu.gemini.model.p1.visibility

import edu.gemini.model.p1.immutable._
import edu.gemini.model.p1.immutable.TargetVisibility._
import edu.gemini.model.p1.immutable.AoLgs
import edu.gemini.model.p1.immutable.SemesterOption.{A, B}
import edu.gemini.model.p1.immutable.Site._
import edu.gemini.spModel.core.Coordinates

object TargetVisibilityCalc {
  /**
   * Obtains a visibility determination for the given semester and observation,
   * or None if the observation is missing its blueprint and/or target or if
   * the target position isn't defined at the midpoint of the semester.
   */
  def get(sem: Semester, obs: Observation): Option[TargetVisibility] =
    for {
      bp     <- obs.blueprint
      target <- obs.target
      coords <- target.coords(sem.midPoint)
    } yield nonSiderealAdjustment(target, visibility(Key(sem.half, geminiSite(bp), GuideType(bp)), coords))

  def getOnDec(sem: Semester, obs: Observation): Option[TargetVisibility] =
    for {
      bp     <- obs.blueprint
      target <- obs.target
      coords <- target.coords(sem.midPoint)
    } yield decVisibility(Key(sem.half, geminiSite(bp), GuideType(bp)), coords)

  // Treat Keck and Subaru the same as GN.
  private def geminiSite(bp: BlueprintBase): Site = bp.site match {
    case GN     => GN
    case GS     => GS
    case Keck   => GN
    case Subaru => GN
    case CFH    => GN
  }

  private def visibility(key: Key, coords: Coordinates): TargetVisibility = {
    raVisibility(key, coords) & decVisibility(key, coords)
  }

  // REL-2284 For Non sidereal targets, reduce errors into warnings
  private def nonSiderealAdjustment(target: Target, visibility: TargetVisibility): TargetVisibility = {
    (target, visibility) match {
       case (_:NonSiderealTarget, TargetVisibility.Bad) => TargetVisibility.Limited
       case _                                           => visibility
     }
  }

  private def decVisibility(key: Key, c: Coordinates): TargetVisibility = decMap(key).visibility(c.dec)

  private def raVisibility(key: Key, c: Coordinates): TargetVisibility = raMap(key).visibility(c.ra)

  private sealed trait GuideType
  private case object Lgs extends GuideType
  private case object Ngs extends GuideType

  private object GuideType {
    def apply(b: BlueprintBase): GuideType = b match {
      case g: GeminiBlueprintBase => if (g.ao == AoLgs) Lgs else Ngs
      case _                      => Ngs  // exchange instruments
    }
  }

  private case class Key(sem: SemesterOption, site: Site, lgs: GuideType)

  import VisibilityRangeList.{deg, hr}

  private val EQUALITY_LIMIT = 0.0001/3600 // 0.001 seconds precision

  private val raMap: Map[Key, VisibilityRangeList] = Map(
    Key(A, GN, Ngs) -> hr( 1.0 -> Bad,     (4.0  + EQUALITY_LIMIT) -> Limited, ( 7.0 + EQUALITY_LIMIT) -> Good,    (22.0 + EQUALITY_LIMIT) -> Limited),
    Key(A, GN, Lgs) -> hr( 0.0 -> Bad,     (5.0  + EQUALITY_LIMIT) -> Limited, ( 8.0 + EQUALITY_LIMIT) -> Good,     21.0 -> Limited),
    Key(A, GS, Ngs) -> hr( 2.0 -> Bad,     (5.0  + EQUALITY_LIMIT) -> Limited, ( 7.0 + EQUALITY_LIMIT) -> Good,    (23.0 + EQUALITY_LIMIT) -> Limited),
    Key(A, GS, Lgs) -> hr(20.0 -> Bad,     (6.0  + EQUALITY_LIMIT) -> Limited, ( 7.0 + EQUALITY_LIMIT) -> Good,    (18.0 + EQUALITY_LIMIT) -> Limited),
    Key(B, GN, Ngs) -> hr(11.0 -> Limited,  13.5 -> Bad,                       (17.0 + EQUALITY_LIMIT) -> Limited, (19.0 + EQUALITY_LIMIT) -> Good),
    Key(B, GN, Lgs) -> hr(10.0 -> Limited,  12.5 -> Bad,                       (18.0 + EQUALITY_LIMIT) -> Limited, (20.0 + EQUALITY_LIMIT) -> Good),
    Key(B, GS, Ngs) -> hr( 9.0 -> Limited,  12.0 -> Bad,                       (16.0 + EQUALITY_LIMIT) -> Limited, (19.0 + EQUALITY_LIMIT) -> Good),
    Key(B, GS, Lgs) -> hr( 8.0 -> Limited,  11.0 -> Bad,                       (19.0 + EQUALITY_LIMIT) -> Limited, (20.0 + EQUALITY_LIMIT) -> Good)
  )

  private val decGnNgs = deg( -90.0 -> Bad,                       -(37.0 - EQUALITY_LIMIT) -> Limited, -(30.0 - EQUALITY_LIMIT) -> Good, 73.0 -> Limited)
  private val decGnLgs = deg(-(27.0 - EQUALITY_LIMIT) -> Limited, -(22.0 - EQUALITY_LIMIT) -> Good,      65.0 -> Limited,                68.0 -> Bad)
  private val decGsNgs = deg(-(87.0 - EQUALITY_LIMIT) -> Good,      22.0 -> Limited,                     28.0 -> Bad,                    90.0 -> Limited)
  private val decGsLgs = deg(-(75.0 - EQUALITY_LIMIT) -> Limited,  -70.0 -> Good,                        10.0 -> Limited,                15.0 -> Bad)

  private val decMap: Map[Key, VisibilityRangeList] = Map(
    Key(A, GN, Ngs) -> decGnNgs,
    Key(A, GN, Lgs) -> decGnLgs,
    Key(A, GS, Ngs) -> decGsNgs,
    Key(A, GS, Lgs) -> decGsLgs,
    Key(B, GN, Ngs) -> decGnNgs,
    Key(B, GN, Lgs) -> decGnLgs,
    Key(B, GS, Ngs) -> decGsNgs,
    Key(B, GS, Lgs) -> decGsLgs)
}
