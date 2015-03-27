package edu.gemini.ags.impl

import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.catalog.votable.TestVoTableBackend
import edu.gemini.spModel.ags.AgsStrategyKey._
import edu.gemini.spModel.core.{Declination, Site, Angle}
import edu.gemini.shared.util.immutable.Some
import edu.gemini.spModel.gemini.altair.{AltairAowfsGuider, AltairParams, InstAltair}
import edu.gemini.spModel.gemini.flamingos2.{Flamingos2, Flamingos2OiwfsGuideProbe}
import edu.gemini.spModel.gemini.gmos.{GmosOiwfsGuideProbe, InstGmosNorth}
import edu.gemini.spModel.gemini.niri.{NiriOiwfsGuideProbe, InstNIRI}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe
import org.specs2.time.NoTimeConversions
import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

class SingleProbeStrategySpec extends Specification with NoTimeConversions {
  private val magTable = ProbeLimitsTable.loadOrThrow()

  "SingleProbeStrategy" should {
    "find a guide star for NIRI+NGS, OCSADV-245" in {
      // zeta Gem target
      val ra = Angle.fromHMS(7, 4, 6.531).getOrElse(Angle.zero)
      val dec = Angle.fromDMS(20, 34, 13.070).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target) |> {_.addActive(AltairAowfsGuider.instance)}
      val inst = new InstNIRI <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(AltairAowfsKey, SingleProbeStrategyParams.AltairAowfsParams, TestVoTableBackend("/ocsadv245.xml"))
      val aoComp = new InstAltair <| {_.setMode(AltairParams.Mode.NGS)}
      val ctx = ObsContext.create(env, inst, new Some(Site.GN), SPSiteQuality.Conditions.BEST, null, aoComp)

      Await.result(strategy.select(ctx, magTable), 5.seconds) should beSome
    }
    "find a guide star for NIRI+LGS, OCSADV-245" in {
      // Pal 12 target
      val ra = Angle.fromHMS(21, 46, 38.840).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.fromDegrees(338.747389)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target) |> {_.addActive(AltairAowfsGuider.instance)}
      val inst = new InstNIRI <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(AltairAowfsKey, SingleProbeStrategyParams.AltairAowfsParams, TestVoTableBackend("/ocsadv-245-lgs.xml"))
      val aoComp = new InstAltair <| {_.setMode(AltairParams.Mode.LGS)}
      val ctx = ObsContext.create(env, inst, new Some(Site.GN), SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY), null, aoComp)

      Await.result(strategy.select(ctx, magTable), 5.seconds) should beSome
    }
    "find a guide star for NIRI+PWFS1, OCSADV-255" in {
      // HIP 1000 target
      val ra = Angle.fromHMS(0, 12, 30.286).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(22, 4, 2.34).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](NiriOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target) |> {_.setActiveGuiders(guiders.asJava)}
      val inst = new InstNIRI <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs1NorthKey, SingleProbeStrategyParams.PwfsParams(Site.GN, PwfsGuideProbe.pwfs1), TestVoTableBackend("/niri_pwfs1.xml"))

      val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY).cc(SPSiteQuality.CloudCover.PERCENT_80).iq(SPSiteQuality.ImageQuality.PERCENT_85)
      val ctx = ObsContext.create(env, inst, new Some(Site.GN), conditions, null, null)

      Await.result(strategy.select(ctx, magTable), 5.seconds) should beSome
    }
    "find a guide star for NIRI+PWFS2, OCSADV-255" in {
      // HIP 1024 target
      val ra = Angle.fromHMS(0, 12, 45.821).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.fromDMS(1, 18, 34.79).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](NiriOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target) |> {_.setActiveGuiders(guiders.asJava)}
      val inst = new InstNIRI <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs2NorthKey, SingleProbeStrategyParams.PwfsParams(Site.GN, PwfsGuideProbe.pwfs2), TestVoTableBackend("/niri_pwfs2.xml"))

      val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY).cc(SPSiteQuality.CloudCover.PERCENT_80).iq(SPSiteQuality.ImageQuality.PERCENT_85)
      val ctx = ObsContext.create(env, inst, new Some(Site.GN), conditions, null, null)

      Await.result(strategy.select(ctx, magTable), 5.seconds) should beSome
    }
    "find a guide star for GMOS-N+OIWFS, OCSADV-255" in {
      // NGC 101 target
      val ra = Angle.fromHMS(0, 23, 54.614).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(32, 32, 10.34).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](GmosOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target) |> {_.setActiveGuiders(guiders.asJava)}
      val inst = new InstGmosNorth <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(GmosNorthOiwfsKey, SingleProbeStrategyParams.GmosOiwfsParams(Site.GN), TestVoTableBackend("/gmosn_oiwfs.xml"))

      val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY)
      val ctx = ObsContext.create(env, inst, new Some(Site.GN), conditions, null, null)

      Await.result(strategy.select(ctx, magTable), 5.seconds) should beSome
    }
    "find a guide star for GMOS-N+PWFS2, OCSADV-255" in {
      // M1 target
      val ra = Angle.fromHMS(5, 34, 31.940).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.fromDMS(22, 0, 52.20).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](GmosOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target) |> {_.setActiveGuiders(guiders.asJava)}
      val inst = new InstGmosNorth <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs2NorthKey, SingleProbeStrategyParams.PwfsParams(Site.GN, PwfsGuideProbe.pwfs2), TestVoTableBackend("/gmosn_pwfs2.xml"))

      val conditions = SPSiteQuality.Conditions.WORST
      val ctx = ObsContext.create(env, inst, new Some(Site.GN), conditions, null, null)

      Await.result(strategy.select(ctx, magTable), 5.seconds) should beSome
    }
    "find a guide star for Flamingos2+OIWFS, OCSADV-255" in {
      // RMC 136 target
      val ra = Angle.fromHMS(5, 38, 42.396).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(69, 6, 3.36).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](Flamingos2OiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target) |> {_.setActiveGuiders(guiders.asJava)}
      val inst = new Flamingos2 <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Flamingos2OiwfsKey, SingleProbeStrategyParams.Flamingos2OiwfsParams, TestVoTableBackend("/f2_oiwfs.xml"))

      val conditions = SPSiteQuality.Conditions.WORST
      val ctx = ObsContext.create(env, inst, new Some(Site.GS), conditions, null, null)

      Await.result(strategy.select(ctx, magTable), 5.seconds) should beSome
    }
    "find a guide star for Flamingos2+PWFS2, OCSADV-255" in {
      // RMC 136 target
      val ra = Angle.fromHMS(5, 38, 42.396).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(69, 6, 3.36).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](Flamingos2OiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target) |> {_.setActiveGuiders(guiders.asJava)}
      val inst = new Flamingos2 <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs2SouthKey, SingleProbeStrategyParams.PwfsParams(Site.GS, PwfsGuideProbe.pwfs2), TestVoTableBackend("/f2_pwfs2.xml"))

      val conditions = SPSiteQuality.Conditions.WORST.cc(SPSiteQuality.CloudCover.PERCENT_70)
      val ctx = ObsContext.create(env, inst, new Some(Site.GS), conditions, null, null)

      Await.result(strategy.select(ctx, magTable), 5.seconds) should beSome
    }
    "find a guide star for Flamingos2+PWFS2, OCSADV-255" in {
      // RMC 136 target
      val ra = Angle.fromHMS(5, 38, 42.396).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(69, 6, 3.36).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](Flamingos2OiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target) |> {_.setActiveGuiders(guiders.asJava)}
      val inst = new Flamingos2 <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs2SouthKey, SingleProbeStrategyParams.PwfsParams(Site.GS, PwfsGuideProbe.pwfs2), TestVoTableBackend("/f2_pwfs2.xml"))

      val conditions = SPSiteQuality.Conditions.WORST.cc(SPSiteQuality.CloudCover.PERCENT_70)
      val ctx = ObsContext.create(env, inst, new Some(Site.GS), conditions, null, null)

      Await.result(strategy.select(ctx, magTable), 5.seconds) should beSome
    }
  }
}
