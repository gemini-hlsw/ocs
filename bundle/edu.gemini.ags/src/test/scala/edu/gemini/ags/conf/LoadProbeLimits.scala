package edu.gemini.ags.conf

import edu.gemini.spModel.gemini.obscomp.SPSiteQuality._
import edu.gemini.spModel.guide.GuideSpeed

import scalaz.{-\/, \/-}

/**
 * Utility for exercising the parser
 */
object LoadProbeLimits {
  def dump(m: Map[MagLimitsId, ProbeLimitsCalc]): Unit =
    m.foreach { case (id, c) =>
        println(s"${id.name}, ${c.band}, ${c.saturationAdjustment}")
        val rows = for {
          iq <- ImageQuality.values()
          sb <- SkyBackground.values()
          gs <- GuideSpeed.values()
        } yield s"$iq, $sb, $gs, ${c(new Conditions(CloudCover.PERCENT_50, iq, sb, WaterVapor.PERCENT_20), gs)}"
        rows.foreach(println)
        println()
    }

  def main(args: Array[String]): Unit =
    ProbeLimitsParser.read(this.getClass.getResourceAsStream("ProbeLimits.txt")) match {
      case \/-(m) => dump(m)
      case -\/(m) => println(m)
    }
}
