package edu.gemini.ags.gems

import edu.gemini.catalog.api.MagnitudeConstraints
import edu.gemini.pot.ModelConverters._
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.ags.api._
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gems.Canopus
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.target.SPTarget
import edu.gemini.shared.skyobject
import edu.gemini.skycalc
import edu.gemini.spModel.target.env.GuideProbeTargets
import edu.gemini.spModel.target.system.{ITarget, HmsDegTarget}
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

/**
 * Utility methods for Java classes to access scala classes/methods
 */
object GemsUtils4Java {
  // by value only
  implicit val MagnitudeValueOrdering: scala.math.Ordering[Magnitude] =
    scala.math.Ordering.by(_.value)

  // comparison on Option[Magnitude] that reverses the way that None is treated, i.e. None is always > Some(Magnitude).
  // Comparison of RLike bands is done by value alone
  val MagnitudeOptionOrdering: scala.math.Ordering[Option[Magnitude]] = new scala.math.Ordering[Option[Magnitude]] {
    override def compare(x: Option[Magnitude], y: Option[Magnitude]): Int = (x,y) match {
      case (Some(m1), Some(m2)) if List(m1.band, m2.band).forall(RLikeBands.contains) => MagnitudeValueOrdering.compare(m1, m2)
      case (Some(m1), Some(m2))                                                       => Magnitude.MagnitudeOrdering.compare(m1, m2) // Magnitude.MagnitudeOrdering is probably incorrect, you cannot sort on different bunds
      case (None,     None)                                                           => 0
      case (_,        None)                                                           => -1
      case (None,     _)                                                              => 1
    }
  }

  // Returns true if the target magnitude is within the given limits
  def containsMagnitudeInLimits(target: SiderealTarget, magLimits: MagnitudeConstraints): Boolean =
    target.magnitudeIn(magLimits.band).map(m => magLimits.contains(m)).getOrElse(true)

  /**
   * Returns a list of unique targets in the given search results.
   */
  def uniqueTargets(list: java.util.List[GemsCatalogSearchResults]): java.util.List[Target.SiderealTarget] = {
    import collection.breakOut
    new java.util.ArrayList(list.asScala.flatMap(_.results).groupBy(_.name).map(_._2.head)(breakOut).asJava)
  }

  /**
   * Outputs the target magnitudes used by the Asterism table on the Manual Search for GEMS
   */
  def probeMagnitudeInUse(guideProbe: GuideProbe, referenceBand: skyobject.Magnitude.Band, target: ITarget): String = {
    val availableMagnitudes = target.getMagnitudes.asScalaList.map(_.toNewModel)
    val probeBand = if (Canopus.Wfs.Group.instance.getMembers.contains(guideProbe)) {
        MagnitudeBand.R
      } else {
        referenceBand.toNewModel
      }
    val r = defaultProbeBands(probeBand).flatMap {b => availableMagnitudes.find(_.band === b)}.headOption
    ~r.map(m => s"${m.value} (${m.band.name})")
  }

  def toOldBand(band: MagnitudeBand): skyobject.Magnitude.Band = band.toOldModel

  def toNewBand(band: skyobject.Magnitude.Band): MagnitudeBand = band.toNewModel

  def toNewAngle(angle: skycalc.Angle): Angle = angle.toNewModel

  def toOldAngle(angle: Angle): skycalc.Angle = angle.toOldModel

  def toSPTarget(siderealTarget: SiderealTarget):SPTarget = new SPTarget(HmsDegTarget.fromSkyObject(siderealTarget.toOldModel))
}
