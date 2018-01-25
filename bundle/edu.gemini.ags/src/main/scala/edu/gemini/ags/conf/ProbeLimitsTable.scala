package edu.gemini.ags.conf

import edu.gemini.ags.api.AgsMagnitude.{MagnitudeCalc, MagnitudeTable}
import edu.gemini.ags.gems.GemsMagnitudeTable
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.core.Site.{GN, GS}

import edu.gemini.spModel.gemini.altair.AltairParams.Mode._
import edu.gemini.spModel.gemini.altair.{InstAltair, AltairAowfsGuider}
import edu.gemini.spModel.gemini.flamingos2.Flamingos2OiwfsGuideProbe
import edu.gemini.spModel.gemini.gems.Canopus
import edu.gemini.spModel.gemini.gmos.GmosOiwfsGuideProbe
import edu.gemini.spModel.gemini.gnirs.GnirsOiwfsGuideProbe
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw
import edu.gemini.spModel.gemini.nici.NiciOiwfsGuideProbe
import edu.gemini.spModel.gemini.nifs.NifsOiwfsGuideProbe
import edu.gemini.spModel.gemini.niri.NiriOiwfsGuideProbe
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe

import scalaz._
import Scalaz._

object ProbeLimitsTable {

  // Okay, for now load the configuration from the classpath.  At some point
  // this will become user-changeable and supplied as an argument..
  private val ConfFile  = "Guide Limits - OT Config.csv"

  def load(): String \/ MagnitudeTable = {
    val is = Option(this.getClass.getResourceAsStream(ConfFile))
    try {
      for {
        s <- is.toRightDisjunction(s"Could not find $ConfFile")
        t <- new ProbeLimitsParser().read(s).map(ProbeLimitsTable(_))
      } yield t
    } finally {
      is.foreach(_.close())
    }
  }

  def loadOrThrow(): MagnitudeTable =
    load().fold(msg => throw new RuntimeException(msg), identity)
}

case class ProbeLimitsTable(tab: CalcMap) extends MagnitudeTable {

  def apply(ctx: ObsContext, probe: GuideProbe): Option[MagnitudeCalc] =
    // Deferring GeMS to the old implementation until we understand
    // what is supposed to happen.
    probe match {
      case _: GsaoiOdgw   => GemsMagnitudeTable(ctx, probe)
      case _: Canopus.Wfs => GemsMagnitudeTable(ctx, probe)
      case _              =>
        for {
          s  <- ctx.getSite.asScalaOpt
          id <- lookup(s, ctx, probe)
          ct <- tab.get(id)
        } yield ct
    }

  private def lookup(site: Site, ctx: ObsContext, probe: GuideProbe): Option[MagLimitsId] = {
    (site, probe) match {
      case (GN, AltairAowfsGuider.instance) =>
        ctx.getAOComponent.asScalaOpt.filter(_.isInstanceOf[InstAltair]).flatMap { ado =>
          ado.asInstanceOf[InstAltair].getMode match {
            case NGS | NGS_FL => Some(AltairNgs)
            case LGS          => Some(AltairLgs)
            case _            => None
          }
        }

      case (GS, Flamingos2OiwfsGuideProbe.instance) => Some(F2Oiwfs)
      case (GN, GmosOiwfsGuideProbe.instance)       => Some(GmosNOiwfs)
      case (GS, GmosOiwfsGuideProbe.instance)       => Some(GmosSOiwfs)
      case (GN, GnirsOiwfsGuideProbe.instance)      => Some(GnirsOiwfs)
      case (GN, NifsOiwfsGuideProbe.instance)       => Some(NifsOiwfs)
      case (GN, NiriOiwfsGuideProbe.instance)       => Some(NiriOiwfs)

      case (GN, PwfsGuideProbe.pwfs1)               => Some(GnPwfs1)
      case (GN, PwfsGuideProbe.pwfs2)               => Some(GnPwfs2)
      case (GS, PwfsGuideProbe.pwfs1)               => Some(GsPwfs1)
      case (GS, PwfsGuideProbe.pwfs2)               => Some(GsPwfs2)
      case (GS, NiciOiwfsGuideProbe.instance)       => Some(NiciOiwfs)
      case _                                        => None
    }
  }
}
