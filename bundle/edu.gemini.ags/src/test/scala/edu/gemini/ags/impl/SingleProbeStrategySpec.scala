package edu.gemini.ags.impl

import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.catalog.votable.TestVoTableBackend
import edu.gemini.spModel.ags.AgsStrategyKey.AltairAowfsKey
import edu.gemini.spModel.core.{Declination, Site, Angle}
import edu.gemini.shared.util.immutable.Some
import edu.gemini.spModel.gemini.altair.{AltairAowfsGuider, AltairParams, InstAltair}
import edu.gemini.spModel.gemini.niri.InstNIRI
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import org.specs2.time.NoTimeConversions
import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.duration._

class SingleProbeStrategySpec extends Specification with NoTimeConversions {
  private val magTable = ProbeLimitsTable.loadOrThrow()

  "SingleProbeStrategy" should {
    "find a target for NIRI+NGS, OCSADV-245" in {
      // zeta Gem target
      val ra = Angle.fromHMS(7, 4, 6.531).getOrElse(Angle.zero)
      val dec = Angle.fromDMS(20, 34, 13.070).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      env.addActive(AltairAowfsGuider.instance)
      val inst = new InstNIRI
      inst.setPosAngle(0.0)

      val strategy = SingleProbeStrategy(AltairAowfsKey, SingleProbeStrategyParams.AltairAowfsParams, TestVoTableBackend("/ocsadv245.xml"))
      val aoComp = new InstAltair
      aoComp.setMode(AltairParams.Mode.NGS)
      val ctx = ObsContext.create(env, inst, new Some(Site.GN), SPSiteQuality.Conditions.BEST, null, aoComp)

      Await.result(strategy.select(ctx, magTable), 20.seconds) should beSome
    }.pendingUntilFixed
    "find a target for NIRI+LGS, OCSADV-245" in {
      // Pal 12 target
      val ra = Angle.fromHMS(21, 46, 38.840).getOrElse(Angle.zero)

      val dec = Declination.fromAngle(Angle.fromDegrees(338.747389)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      env.addActive(AltairAowfsGuider.instance)
      val inst = new InstNIRI
      inst.setPosAngle(0.0)

      val strategy = SingleProbeStrategy(AltairAowfsKey, SingleProbeStrategyParams.AltairAowfsParams, TestVoTableBackend("/ocsadv-245-lgs.xml"))
      val aoComp = new InstAltair
      aoComp.setMode(AltairParams.Mode.LGS)
      val ctx = ObsContext.create(env, inst, new Some(Site.GN), SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY), null, aoComp)

      Await.result(strategy.select(ctx, magTable), 20.seconds) should beSome
    }.pendingUntilFixed
  }
}
