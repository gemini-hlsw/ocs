package edu.gemini.ags.impl

import edu.gemini.ags.api.{AgsGuideQuality, AgsStrategy}
import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.catalog.votable.TestVoTableBackend
import edu.gemini.spModel.ags.AgsStrategyKey._
import edu.gemini.spModel.core.{Declination, Site, Angle}
import edu.gemini.shared.util.immutable.Some
import edu.gemini.spModel.gemini.altair.{AltairAowfsGuider, AltairParams, InstAltair}
import edu.gemini.spModel.gemini.flamingos2.{Flamingos2, Flamingos2OiwfsGuideProbe}
import edu.gemini.spModel.gemini.gmos.{InstGmosSouth, GmosOiwfsGuideProbe, InstGmosNorth}
import edu.gemini.spModel.gemini.gnirs.{GnirsOiwfsGuideProbe, InstGNIRS}
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

  private def applySelection(ctx: ObsContext, sel: AgsStrategy.Selection): ObsContext = {
    // Make a new TargetEnvironment with the guide probe assignments.
    sel.applyTo(ctx.getTargets) |> {ctx.withTargets}
  }

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

      val selection = Await.result(strategy.select(ctx, magTable), 10.seconds)

      // One guide star found
      selection.map(_.assignments.size) should beSome(1)
      selection.flatMap(_.assignments.headOption.map(_.guideProbe)) should beSome(AltairAowfsGuider.instance)
      val guideStar = selection.flatMap(_.assignments.headOption.map(_.guideStar))
      guideStar.map(_.name) should beSome("553-036128")
      // Add GS to targets
      val newCtx = selection.map(applySelection(ctx, _))
      val analyzedSelection = ~newCtx.map(strategy.analyze(_, magTable))
      analyzedSelection should be size 1
      analyzedSelection.headOption.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)

      // Analyze Single Guide Star
      val analyzedGS = (newCtx |@| guideStar){ strategy.analyze(_, magTable, AltairAowfsGuider.instance, _) }.flatten
      analyzedGS.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)
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

      val selection = Await.result(strategy.select(ctx, magTable), 10.seconds)

      // One guide star found
      selection.map(_.assignments.size) should beSome(1)
      selection.flatMap(_.assignments.headOption.map(_.guideProbe)) should beSome(AltairAowfsGuider.instance)
      val guideStar = selection.flatMap(_.assignments.headOption.map(_.guideStar))
      guideStar.map(_.name) should beSome("344-198748")
      // Add GS to targets
      val newCtx = selection.map(applySelection(ctx, _))
      val analyzedSelection = ~newCtx.map(strategy.analyze(_, magTable))
      analyzedSelection should be size 1
      analyzedSelection.headOption.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)

      // Analyze Single Guide Star
      val analyzedGS = (newCtx |@| guideStar){ strategy.analyze(_, magTable, AltairAowfsGuider.instance, _) }.flatten
      analyzedGS.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)
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

      val selection = Await.result(strategy.select(ctx, magTable), 10.seconds)

      // One guide star found
      selection.map(_.assignments.size) should beSome(1)
      selection.flatMap(_.assignments.headOption.map(_.guideProbe)) should beSome(PwfsGuideProbe.pwfs1)
      val guideStar = selection.flatMap(_.assignments.headOption.map(_.guideStar))
      guideStar.map(_.name) should beSome("340-000202")
      // Add GS to targets
      val newCtx = selection.map(applySelection(ctx, _))
      val analyzedSelection = ~newCtx.map(strategy.analyze(_, magTable))
      analyzedSelection should be size 1
      analyzedSelection.headOption.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)

      // Analyze Single Guide Star
      val analyzedGS = (newCtx |@| guideStar){ strategy.analyze(_, magTable, PwfsGuideProbe.pwfs1, _) }.flatten
      analyzedGS.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)
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

      val selection = Await.result(strategy.select(ctx, magTable), 10.seconds)

      // One guide star found
      selection.map(_.assignments.size) should beSome(1)
      selection.flatMap(_.assignments.headOption.map(_.guideProbe)) should beSome(PwfsGuideProbe.pwfs2)
      val guideStar = selection.flatMap(_.assignments.headOption.map(_.guideStar))
      guideStar.map(_.name) should beSome("458-000297")
      // Add GS to targets
      val newCtx = selection.map(applySelection(ctx, _))
      val analyzedSelection = ~newCtx.map(strategy.analyze(_, magTable))
      analyzedSelection should be size 1
      analyzedSelection.headOption.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)

      // Analyze Single Guide Star
      val analyzedGS = (newCtx |@| guideStar){ strategy.analyze(_, magTable, PwfsGuideProbe.pwfs2, _) }.flatten
      analyzedGS.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)
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

      val selection = Await.result(strategy.select(ctx, magTable), 10.seconds)

      // One guide star found
      selection.map(_.assignments.size) should beSome(1)
      selection.flatMap(_.assignments.headOption.map(_.guideProbe)) should beSome(GmosOiwfsGuideProbe.instance)
      val guideStar = selection.flatMap(_.assignments.headOption.map(_.guideStar))
      guideStar.map(_.name) should beSome("288-000439")
      // Add GS to targets
      val newCtx = selection.map(applySelection(ctx, _))
      val analyzedSelection = ~newCtx.map(strategy.analyze(_, magTable))
      analyzedSelection should be size 1
      analyzedSelection.headOption.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)

      // Analyze Single Guide Star
      val analyzedGS = (newCtx |@| guideStar){ strategy.analyze(_, magTable, GmosOiwfsGuideProbe.instance, _) }.flatten
      analyzedGS.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)
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

      val selection = Await.result(strategy.select(ctx, magTable), 10.seconds)

      // One guide star found
      selection.map(_.assignments.size) should beSome(1)
      selection.flatMap(_.assignments.headOption.map(_.guideProbe)) should beSome(PwfsGuideProbe.pwfs2)
      val guideStar = selection.flatMap(_.assignments.headOption.map(_.guideStar))
      guideStar.map(_.name) should beSome("560-017530")
      // Add GS to targets
      val newCtx = selection.map(applySelection(ctx, _))
      val analyzedSelection = ~newCtx.map(strategy.analyze(_, magTable))
      analyzedSelection should be size 1
      analyzedSelection.headOption.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)

      // Analyze Single Guide Star
      val analyzedGS = (newCtx |@| guideStar){ strategy.analyze(_, magTable, PwfsGuideProbe.pwfs2, _) }.flatten
      analyzedGS.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)
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

      val selection = Await.result(strategy.select(ctx, magTable), 10.seconds)

      // One guide star found
      selection.map(_.assignments.size) should beSome(1)
      selection.flatMap(_.assignments.headOption.map(_.guideProbe)) should beSome(Flamingos2OiwfsGuideProbe.instance)
      val guideStar = selection.flatMap(_.assignments.headOption.map(_.guideStar))
      guideStar.map(_.name) should beSome("105-014127")
      // Add GS to targets
      val newCtx = selection.map(applySelection(ctx, _))
      val analyzedSelection = ~newCtx.map(strategy.analyze(_, magTable))
      analyzedSelection should be size 1
      analyzedSelection.headOption.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)

      // Analyze Single Guide Star
      val analyzedGS = (newCtx |@| guideStar){ strategy.analyze(_, magTable, Flamingos2OiwfsGuideProbe.instance, _) }.flatten
      analyzedGS.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)
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

      val selection = Await.result(strategy.select(ctx, magTable), 10.seconds)

      // One guide star found
      selection.map(_.assignments.size) should beSome(1)
      selection.flatMap(_.assignments.headOption.map(_.guideProbe)) should beSome(PwfsGuideProbe.pwfs2)
      val guideStar = selection.flatMap(_.assignments.headOption.map(_.guideStar))
      guideStar.map(_.name) should beSome("105-014476")
      // Add GS to targets
      val newCtx = selection.map(applySelection(ctx, _))
      val analyzedSelection = ~newCtx.map(strategy.analyze(_, magTable))
      analyzedSelection should be size 1
      analyzedSelection.headOption.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)

      // Analyze Single Guide Star
      val analyzedGS = (newCtx |@| guideStar){ strategy.analyze(_, magTable, PwfsGuideProbe.pwfs2, _) }.flatten
      analyzedGS.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)
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

      val selection = Await.result(strategy.select(ctx, magTable), 10.seconds)

      // One guide star found
      selection.map(_.assignments.size) should beSome(1)
      selection.flatMap(_.assignments.headOption.map(_.guideProbe)) should beSome(PwfsGuideProbe.pwfs2)
      val guideStar = selection.flatMap(_.assignments.headOption.map(_.guideStar))
      guideStar.map(_.name) should beSome("105-014476")
      // Add GS to targets
      val newCtx = selection.map(applySelection(ctx, _))
      val analyzedSelection = ~newCtx.map(strategy.analyze(_, magTable))
      analyzedSelection should be size 1
      analyzedSelection.headOption.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)

      // Analyze Single Guide Star
      val analyzedGS = (newCtx |@| guideStar){ strategy.analyze(_, magTable, PwfsGuideProbe.pwfs2, _) }.flatten
      analyzedGS.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)
    }
    "find a guide star for GMOS-S+OIWFS, OCSADV-255" in {
      // LMC 1 target
      val ra = Angle.fromHMS(5, 25, 1.110).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(62, 28, 48.90).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](GmosOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target) |> {_.setActiveGuiders(guiders.asJava)}
      val inst = new InstGmosSouth <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(GmosSouthOiwfsKey, SingleProbeStrategyParams.GmosOiwfsParams(Site.GS), TestVoTableBackend("/gmoss_oiwfs.xml"))

      val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY)
      val ctx = ObsContext.create(env, inst, new Some(Site.GS), conditions, null, null)

      val selection = Await.result(strategy.select(ctx, magTable), 10.seconds)

      // One guide star found
      selection.map(_.assignments.size) should beSome(1)
      selection.flatMap(_.assignments.headOption.map(_.guideProbe)) should beSome(GmosOiwfsGuideProbe.instance)
      val guideStar = selection.flatMap(_.assignments.headOption.map(_.guideStar))
      guideStar.map(_.name) should beSome("138-005574")
      // Add GS to targets
      val newCtx = selection.map(applySelection(ctx, _))
      val analyzedSelection = ~newCtx.map(strategy.analyze(_, magTable))
      analyzedSelection should be size 1
      analyzedSelection.headOption.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)

      // Analyze Single Guide Star
      val analyzedGS = (newCtx |@| guideStar){ strategy.analyze(_, magTable, GmosOiwfsGuideProbe.instance, _) }.flatten
      analyzedGS.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)
    }
    "find a guide star for GMOS-S+PWFS2, OCSADV-255" in {
      // Blanco 1 target
      val ra = Angle.fromHMS(0, 4, 7).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(29, 50, 0).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](GmosOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target) |> {_.setActiveGuiders(guiders.asJava)}
      val inst = new InstGmosSouth <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs2SouthKey, SingleProbeStrategyParams.PwfsParams(Site.GS, PwfsGuideProbe.pwfs2), TestVoTableBackend("/gmoss_pwfs2.xml"))

      val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY)
      val ctx = ObsContext.create(env, inst, new Some(Site.GS), conditions, null, null)

      val selection = Await.result(strategy.select(ctx, magTable), 10.seconds)

      // One guide star found
      selection.map(_.assignments.size) should beSome(1)
      selection.flatMap(_.assignments.headOption.map(_.guideProbe)) should beSome(PwfsGuideProbe.pwfs2)
      val guideStar = selection.flatMap(_.assignments.headOption.map(_.guideStar))
      guideStar.map(_.name) should beSome("302-000084")
      // Add GS to targets
      val newCtx = selection.map(applySelection(ctx, _))
      val analyzedSelection = ~newCtx.map(strategy.analyze(_, magTable))
      analyzedSelection should be size 1
      analyzedSelection.headOption.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)

      // Analyze Single Guide Star
      val analyzedGS = (newCtx |@| guideStar){ strategy.analyze(_, magTable, PwfsGuideProbe.pwfs2, _) }.flatten
      analyzedGS.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)
    }
    "find a guide star for GNIRS, OCSADV-255" in {
      // Pleiades target
      val ra = Angle.fromHMS(3, 47, 0).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.fromDMS(24, 7, 0).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](GnirsOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target) |> {_.setActiveGuiders(guiders.asJava)}
      val inst = new InstGNIRS <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs2NorthKey, SingleProbeStrategyParams.PwfsParams(Site.GN, PwfsGuideProbe.pwfs2), TestVoTableBackend("/gnirs_1.xml"))

      val conditions = SPSiteQuality.Conditions.NOMINAL.cc(SPSiteQuality.CloudCover.PERCENT_70).sb(SPSiteQuality.SkyBackground.ANY)
      val ctx = ObsContext.create(env, inst, new Some(Site.GN), conditions, null, null)

      val selection = Await.result(strategy.select(ctx, magTable), 10.seconds)

      // One guide star found
      selection.map(_.assignments.size) should beSome(1)
      selection.flatMap(_.assignments.headOption.map(_.guideProbe)) should beSome(PwfsGuideProbe.pwfs2)
      val guideStar = selection.flatMap(_.assignments.headOption.map(_.guideStar))
      guideStar.map(_.name) should beSome("571-008701")
      // Add GS to targets
      val newCtx = selection.map(applySelection(ctx, _))
      val analyzedSelection = ~newCtx.map(strategy.analyze(_, magTable))
      analyzedSelection should be size 1
      analyzedSelection.headOption.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)

      // Analyze Single Guide Star
      val analyzedGS = (newCtx |@| guideStar){ strategy.analyze(_, magTable, PwfsGuideProbe.pwfs2, _) }.flatten
      analyzedGS.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)
    }
    "find a guide star for GNIRS Part II, OCSADV-255" in {
      // Orion target
      val ra = Angle.fromHMS(5, 35, 17.3).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(5, 23, 28).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](GnirsOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target) |> {_.setActiveGuiders(guiders.asJava)}
      val inst = new InstGNIRS <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs2NorthKey, SingleProbeStrategyParams.PwfsParams(Site.GN, PwfsGuideProbe.pwfs2), TestVoTableBackend("/gnirs_2.xml"))

      val conditions = SPSiteQuality.Conditions.NOMINAL.cc(SPSiteQuality.CloudCover.PERCENT_70).sb(SPSiteQuality.SkyBackground.ANY)
      val ctx = ObsContext.create(env, inst, new Some(Site.GN), conditions, null, null)

      val selection = Await.result(strategy.select(ctx, magTable), 10.seconds)

      // One guide star found
      selection.map(_.assignments.size) should beSome(1)
      selection.flatMap(_.assignments.headOption.map(_.guideProbe)) should beSome(PwfsGuideProbe.pwfs2)
      val guideStar = selection.flatMap(_.assignments.headOption.map(_.guideStar))
      guideStar.map(_.name) should beSome("424-010170")
      // Add GS to targets
      val newCtx = selection.map(applySelection(ctx, _))
      val analyzedSelection = ~newCtx.map(strategy.analyze(_, magTable))
      analyzedSelection should be size 1
      analyzedSelection.headOption.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)

      // Analyze Single Guide Star
      val analyzedGS = (newCtx |@| guideStar){ strategy.analyze(_, magTable, PwfsGuideProbe.pwfs2, _) }.flatten
      analyzedGS.map(_.quality) should beSome(AgsGuideQuality.DeliversRequestedIq)
    }

  }
}
