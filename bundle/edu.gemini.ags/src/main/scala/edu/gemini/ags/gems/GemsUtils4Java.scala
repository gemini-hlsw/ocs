package edu.gemini.ags.gems

import edu.gemini.shared.util.immutable.ImList
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.{Magnitude, MagnitudeBand, SingleBand, RBandsList, SiderealTarget}
import edu.gemini.spModel.gemini.gems.Canopus
import edu.gemini.spModel.guide.GuideProbe
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

/**
 * Utility methods for Java classes to access scala classes/methods
 */
object GemsUtils4Java {
  private val equalByName = Equal.equal[SiderealTarget](_.name === _.name)

  /**
   * Returns a list of unique targets in the given search results.
   */
  def uniqueTargets(list: java.util.List[GemsCatalogSearchResults]): java.util.List[SiderealTarget] = {
    // Find distinct targets by name as there may be duplicates on the search results
    val k = list.asScala.toList.flatMap(_.results).distinctE(equalByName)
    k.toList.asJava
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
