package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsStrategy.Estimate
import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.ags.gems._
import edu.gemini.catalog.api.{RadiusConstraint, SaturationConstraint, FaintnessConstraint, MagnitudeConstraints}
import edu.gemini.catalog.votable.TestVoTableBackend
import edu.gemini.shared.util.immutable.{None, Some}
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.AngleSyntax._
import edu.gemini.spModel.gemini.gems.Canopus.Wfs
import edu.gemini.spModel.gemini.gems.Canopus
import edu.gemini.spModel.gemini.gsaoi.{Gsaoi, GsaoiOdgw}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gems.{GemsGuideStarType, GemsTipTiltMode}
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe
import edu.gemini.spModel.telescope.IssPort
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

object TestGemsStrategy extends GemsStrategy {
  override val backend = TestVoTableBackend("/gemsstrategyquery.xml")
}

class GemsStrategySpec extends Specification with NoTimeConversions {
  "GemsStrategy" should {
    "support estimate" in {
      val ra = Angle.fromHMS(3, 19, 48.2341).getOrElse(Angle.zero)
      val dec = Angle.fromDMS(41, 30, 42.078).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi
      inst.setPosAngle(0.0)
      inst.setIssPort(IssPort.SIDE_LOOKING)
      val gsaoi = GsaoiOdgw.values().toList
      val canopus = Canopus.Wfs.values().toList
      val pwfs1 = List(PwfsGuideProbe.pwfs1)
      val guiders:List[GuideProbe] = gsaoi ::: canopus ::: pwfs1

      val ctx = ObsContext.create(env.setActiveGuiders(guiders.toSet.asJava), inst, new Some(Site.GS), SPSiteQuality.Conditions.BEST, null, null)

      val estimate = TestGemsStrategy.estimate(ctx, ProbeLimitsTable.loadOrThrow())
      Await.result(estimate, 20.seconds) should beEqualTo(Estimate.GuaranteedSuccess)
    }.pendingUntilFixed
    "support search" in {
      val ra = Angle.fromHMS(3, 19, 48.2341).getOrElse(Angle.zero)
      val dec = Angle.fromDMS(41, 30, 42.078).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi
      inst.setPosAngle(0.0)
      inst.setIssPort(IssPort.SIDE_LOOKING)
      val ctx = ObsContext.create(env, inst, None.instance[Site], SPSiteQuality.Conditions.BEST, null, null)
      val tipTiltMode = GemsTipTiltMode.instrument

      val posAngles = Set.empty[Angle]

      val results = Await.result(TestGemsStrategy.search(tipTiltMode, ctx, posAngles, scala.None), 20.seconds)
      results should be size 2

      results(0).criterion should beEqualTo(GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, GsaoiOdgw.Group.instance), CatalogSearchCriterion("On-detector Guide Window tiptilt", scala.Option(MagnitudeConstraints(MagnitudeBand.H, FaintnessConstraint(14.5), scala.Option(SaturationConstraint(7.3)))), RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01666666666665151)), scala.Option(Offset((0.0014984027777700248).degrees[OffsetP], (0.0014984027777700248).degrees[OffsetQ])), scala.None)))
      results(1).criterion should beEqualTo(GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.flexure, Wfs.Group.instance), CatalogSearchCriterion("Canopus Wave Front Sensor flexure", scala.Option(MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(16.0), scala.Option(SaturationConstraint(8.5)))), RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01666666666665151)), scala.Option(Offset((0.0014984027777700248).degrees[OffsetP], (0.0014984027777700248).degrees[OffsetQ])), scala.None)))
      results(0).results should be size 5
      results(1).results should be size 3
    }.pendingUntilFixed
  }
}
