package edu.gemini.ags.gems

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
import edu.gemini.spModel.target.system.{ITarget, HmsDegTarget}
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

/**
 * Utility methods for Java classes to access scala classes/methods
 */
object GemsUtils4Java {

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

}
