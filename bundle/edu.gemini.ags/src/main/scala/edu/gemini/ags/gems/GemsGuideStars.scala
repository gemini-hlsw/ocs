package edu.gemini.ags.gems

import edu.gemini.spModel.core.{Angle, RBandsList, SiderealTarget}
import edu.gemini.spModel.gemini.gems.CanopusWfs
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw
import edu.gemini.spModel.gems.GemsGuideProbeGroup
import edu.gemini.spModel.target.env.GuideGroup
import edu.gemini.spModel.target.env.GuideProbeTargets
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.shared.util.immutable.{None => JNone}
import edu.gemini.pot.ModelConverters._

import scala.annotation.tailrec
import scalaz._
import Scalaz._

/**
  * An NGS2 result comprises a classical GeMS catalog search result and a set of PWFS1 candidates.
  */
case class NGS2Result(gemsCatalogSearchResult: java.util.List[GemsCatalogSearchResults],
                      pwfs1Results: java.util.List[SiderealTarget])

case class GemsStrehl(avg: Double = .0, rms: Double = .0, min: Double = .0, max: Double = .0)

/**
 * The guideGroup should contain the designation of guide stars to guiders for the tip tilt
 * asterism computed by mascot and the flexure star.
 * There must be 1 to 3 tip tilt guide stars all designated for guiders in the same group
 * (e.g., all Canopus or all GSAOI On Detector Guide Window).
 * There must be one flexure star taken from the opposite group. For example, if a Canopus
 * asterism is used for tiptilt, then the flexure star is a GSAOI ODGW star if using GSAOI
 * or an F2 OIWFS star if using Flamingos2.
 * Flamingos 2 OIWFS can only ever be used for the flexure star.
 * The GemsCatalogSearchCriterion available in the input will contain all the options that
 * need be considered (i.e., the F2 OIWFS "group" will never appear with "tiptilt" type).
 *
 * @param pa position angle that applies to the result
 * @param tiptiltGroup the guide group used for tip tilt correction
 * @param strehl calculated by the mascot algorithm
 * @param guideGroup guide group Contents
 */
case class GemsGuideStars(pa: Angle, tiptiltGroup: GemsGuideProbeGroup, strehl: GemsStrehl, guideGroup: GuideGroup) extends Comparable[GemsGuideStars] {

  /**
   * From OT-27: Ranking Results
   *
   * The first order ranking of results is by best (highest) average Strehl ratio. In addition:
   *
   * When searching ODGW asterisms over different PAs, among configurations that
   * give equivalent average Strehls ratios the ones that include ODGW1 stars must
   * excluded. GSAOI detector 1 has many bad pixels and must be avoided when possible.
   *
   * When searching over different PAs preference must be given to orientations along
   * the cardinal directions (PA=0,90,180,270). If all orientations are equivalent
   * then PA=0 must be selected. It is more important however to avoid ODGW1.
   *
   * In these rules, an "equivalent" average strehl is defined as anything within 2% average strehl.
   */
  override def compareTo(that: GemsGuideStars): Int = {
    val thisContainsOdgw1 = this.guideGroup.contains(GsaoiOdgw.odgw1)
    val thatContainsOdgw1 = that.guideGroup.contains(GsaoiOdgw.odgw1)
    if (thisContainsOdgw1 == thatContainsOdgw1) {
      val thisStrel  = this.strehl.avg
      val thatStrehl = that.strehl.avg

      val strehlDifference = (Math.abs(thisStrel - thatStrehl) / ((thisStrel + thatStrehl) / 2.0)) * 100
      if (strehlDifference > 2) {
        thisStrel.compareTo(thatStrehl)
      } else {
        val thisPa = this.pa
        val thatPa = that.pa

        @tailrec
        def matchesCardinalDirection(d: List[Angle]): Int = d match {
          case Nil                       => thisStrel.compareTo(thatStrehl)
          case x :: tail if thisPa === x => 1
          case x :: tail if thatPa === x => -1
          case x :: tail                 => matchesCardinalDirection(tail)
        }

        if (thisPa =/= thatPa) {
          matchesCardinalDirection(GemsGuideStars.cardinalDirections)
        } else {
          thisStrel.compareTo(thatStrehl)
        }
      }
    } else {
      thatContainsOdgw1.compareTo(thisContainsOdgw1)
    }
  }

  override def toString: String = {
    import scala.collection.JavaConverters._

    val NoTime = JNone.instance[java.lang.Long]

    val guiders = guideGroup.getReferencedGuiders.asScala.map { gp =>
      val target = guideGroup.get(gp).getValue.getPrimary.getValue
      s"$gp[${target.getName},${target.getRaString(NoTime).asScalaOpt},${target.getDecString(NoTime).asScalaOpt}]"
    }
    s"GemsGuideStars{pa=$pa, tiptilt=${tiptiltGroup.getKey}, avg Strehl=${strehl.avg * 100}, guiders=${guiders.mkString(" ")}}"
  }

}
object GemsGuideStars {
  val cardinalDirections = List(Angle.zero, Angle.fromDegrees(90.0), Angle.fromDegrees(180.0), Angle.fromDegrees(270.0))

  implicit val GemsGuideStarsOrdering:scala.Ordering[GemsGuideStars] = new scala.math.Ordering[GemsGuideStars] {
    override def compare(x: GemsGuideStars, y: GemsGuideStars) = x.compareTo(y)
  }
}