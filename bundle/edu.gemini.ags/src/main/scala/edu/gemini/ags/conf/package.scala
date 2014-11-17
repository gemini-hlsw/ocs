package edu.gemini.ags

import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{SkyBackground, ImageQuality}
import edu.gemini.spModel.guide.GuideSpeed

import scalaz._
import Scalaz._

package object conf {

  sealed case class MagLimitsId(name: String)

  val AltairLgs  = MagLimitsId("Altair LGS")
  val AltairNgs  = MagLimitsId("Altair NGS")
  val F2Oiwfs    = MagLimitsId("F2 OIWFS")
  val GmosNOiwfs = MagLimitsId("GMOS-N OIWFS")
  val GmosSOiwfs = MagLimitsId("GMOS-S OIWFS")
  val GnirsOiwfs = MagLimitsId("GNIRS OIWFS")
  val GnPwfs1    = MagLimitsId("GN PWFS1")
  val GnPwfs2    = MagLimitsId("GN PWFS2")
  val GsPwfs1    = MagLimitsId("GS PWFS1")
  val GsPwfs2    = MagLimitsId("GS PWFS2")
  val NiciOiwfs  = MagLimitsId("NICI OIWFS")
  val NifsOiwfs  = MagLimitsId("NIFS OIWFS")
  val NiriOiwfs  = MagLimitsId("NIRI OIWFS")

  val AllLimitsIds = List(
    AltairLgs,
    AltairNgs,
    F2Oiwfs,
    GmosNOiwfs,
    GmosSOiwfs,
    GnirsOiwfs,
    GnPwfs1,
    GnPwfs2,
    GsPwfs1,
    GsPwfs2,
    NiciOiwfs,
    NifsOiwfs,
    NiriOiwfs
  )

  case class FaintnessKey(iq: ImageQuality, sb: SkyBackground, gs: GuideSpeed) {
    override def toString: String = s"($iq, $sb, $gs)"
  }
  type FaintnessMap = Map[FaintnessKey, Double]

  type CalcMap = Map[MagLimitsId, ProbeLimitsCalc]

  // The parser validates most aspects of the CalcMap, but doesn't catch missing
  // tables or duplicate faintness keys.
  def validate(cm: CalcMap): String \/ Unit = {
    def missingIds: String \/ Unit =
      AllLimitsIds.filterNot(cm.contains).map(_.name) match {
        case Nil => ().right
        case ids => ids.mkString("Missing table for: ", ", ", "").left
      }

    // Each ProbeLimitsCalc entry should have all these keys.
    val allKeys =
      (for {
        iq <- ImageQuality.values()
        sb <- SkyBackground.values()
        gs <- GuideSpeed.values()
      } yield FaintnessKey(iq, sb, gs)).toSet

    def incompleteTables: String \/ Unit = {
      val errors = cm.map { case (MagLimitsId(name), ProbeLimitsCalc(_, _, fm)) =>
        name -> (allKeys &~ fm.keySet)
      }.filterNot(_._2.isEmpty).map { case (name, keys) =>
        s"$name is missing entries: ${keys.mkString(", ")}"
      }
      if (errors.isEmpty) ().right
      else errors.mkString(", ").left
    }

    for {
      _ <- missingIds
      _ <- incompleteTables
    } yield cm
  }

}
