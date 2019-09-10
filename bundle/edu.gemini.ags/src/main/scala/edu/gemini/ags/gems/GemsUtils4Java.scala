package edu.gemini.ags.gems

import edu.gemini.shared.util.immutable.ImList
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.{Magnitude, MagnitudeBand, SingleBand, RBandsList, SiderealTarget}
import edu.gemini.spModel.gemini.gems.CanopusWfs
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
   * Outputs the target magnitudes used by the Asterism table on the Manual Search for GEMS
   */
  def probeMagnitudeInUse(guideProbe: GuideProbe, referenceBand: MagnitudeBand, mags: ImList[Magnitude]): String = {
    val availableMagnitudes = mags.asScalaList
    // TODO Use GemsMagnitudeTable
    val bandsList = if (CanopusWfs.Group.instance.getMembers.contains(guideProbe)) {
        RBandsList
      } else {
        SingleBand(referenceBand)
      }
    val r = availableMagnitudes.find(m => bandsList.bandSupported(m.band))
    ~r.map(m => f"${m.value}%5.2f (${m.band.name}%s)")
  }

}
