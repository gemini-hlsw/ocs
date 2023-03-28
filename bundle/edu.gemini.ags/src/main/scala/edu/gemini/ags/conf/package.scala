package edu.gemini.ags

import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{SkyBackground, ImageQuality}
import edu.gemini.spModel.guide.GuideSpeed

package object conf {

  val AltairLgs: MagLimitsId = MagLimitsId("Altair LGS")
  val AltairNgs: MagLimitsId = MagLimitsId("Altair NGS")
  val F2Oiwfs: MagLimitsId = MagLimitsId("F2 OIWFS")
  val GmosNOiwfs: MagLimitsId = MagLimitsId("GMOS-N OIWFS")
  val GmosSOiwfs: MagLimitsId = MagLimitsId("GMOS-S OIWFS")
  val GnirsOiwfs: MagLimitsId = MagLimitsId("GNIRS OIWFS")
  val GnPwfs1: MagLimitsId = MagLimitsId("GN PWFS1")
  val GnPwfs2: MagLimitsId = MagLimitsId("GN PWFS2")
  val GsPwfs1: MagLimitsId = MagLimitsId("GS PWFS1")
  val GsPwfs2: MagLimitsId = MagLimitsId("GS PWFS2")
  val NiciOiwfs: MagLimitsId = MagLimitsId("NICI OIWFS")
  val NifsOiwfs: MagLimitsId = MagLimitsId("NIFS OIWFS")
  val NiriOiwfs: MagLimitsId = MagLimitsId("NIRI OIWFS")

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

  type FaintnessMap = Map[FaintnessKey, Double]

  val AllFaintnessKeys: Set[FaintnessKey] =
    (for {
      iq <- ImageQuality.values()
      sb <- SkyBackground.values()
      gs <- GuideSpeed.values()
    } yield FaintnessKey(iq, sb, gs)).toSet

  type CalcMap = Map[MagLimitsId, ProbeLimitsCalc]
}
