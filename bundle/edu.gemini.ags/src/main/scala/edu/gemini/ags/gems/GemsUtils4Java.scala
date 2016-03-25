package edu.gemini.ags.gems

import edu.gemini.pot.ModelConverters._
import edu.gemini.shared.util.immutable.ImList
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.{Magnitude, MagnitudeBand, SingleBand, RBandsList, SiderealTarget}
import edu.gemini.spModel.gemini.gems.Canopus
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.shared.skyobject
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
  def uniqueTargets(list: java.util.List[GemsCatalogSearchResults]): java.util.List[SiderealTarget] = {
    import collection.breakOut
    new java.util.ArrayList(list.asScala.flatMap(_.results).groupBy(_.name).map(_._2.head)(breakOut).asJava)
  }

  /**
   * Outputs the target magnitudes used by the Asterism table on the Manual Search for GEMS
   */
  def probeMagnitudeInUse(guideProbe: GuideProbe, referenceBand: MagnitudeBand, mags: ImList[Magnitude]): String = {
    val availableMagnitudes = mags.asScalaList
    // TODO Use GemsMagnitudeTable
    val bandsList = if (Canopus.Wfs.Group.instance.getMembers.contains(guideProbe)) {
        RBandsList
      } else {
        SingleBand(referenceBand)
      }
    val r = availableMagnitudes.find(m => bandsList.bandSupported(m.band))
    ~r.map(m => s"${m.value} (${m.band.name})")
  }

}
