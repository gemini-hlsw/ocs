package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsStrategy
import edu.gemini.ags.api.AgsStrategy.Estimate
import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.ags.gems._
import edu.gemini.catalog.api._
import edu.gemini.catalog.votable.TestVoTableBackend
import edu.gemini.shared.util.immutable.{None => JNone, Some => JSome}
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.AngleSyntax._
import edu.gemini.spModel.gemini.gems.Canopus.Wfs
import edu.gemini.spModel.gemini.gems.{Gems, Canopus}
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
import AlmostEqual.AlmostEqualOps

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

import scalaz._
import Scalaz._

case class TestGemsStrategy(file: String) extends GemsStrategy {
  override val backend = TestVoTableBackend(file)
}

class GemsStrategySpec extends Specification with NoTimeConversions {

  private def applySelection(ctx: ObsContext, sel: AgsStrategy.Selection): ObsContext = {
    // Make a new TargetEnvironment with the guide probe assignments.
    sel.applyTo(ctx.getTargets) |> {ctx.withTargets}
  }

  "GemsStrategy" should {
    "support estimate" in {
      val ra = Angle.fromHMS(3, 19, 48.2341).getOrElse(Angle.zero)
      val dec = Angle.fromDMS(41, 30, 42.078).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi <| {_.setPosAngle(0.0)} <| {_.setIssPort(IssPort.UP_LOOKING)}
      val gsaoi = GsaoiOdgw.values().toList
      val canopus = Canopus.Wfs.values().toList
      val pwfs1 = List(PwfsGuideProbe.pwfs1)
      val guiders:List[GuideProbe] = gsaoi ::: canopus ::: pwfs1

      val ctx = ObsContext.create(env.setActiveGuiders(guiders.toSet.asJava), inst, new JSome(Site.GS), SPSiteQuality.Conditions.BEST, null, new Gems)

      val estimate = TestGemsStrategy("/gemsstrategyquery.xml").estimate(ctx, ProbeLimitsTable.loadOrThrow())
      Await.result(estimate, 20.seconds) should beEqualTo(Estimate.GuaranteedSuccess)
    }
    "support search" in {
      val ra = Angle.fromHMS(3, 19, 48.2341).getOrElse(Angle.zero)
      val dec = Angle.fromDMS(41, 30, 42.078).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi <| {_.setPosAngle(0.0)} <| {_.setIssPort(IssPort.UP_LOOKING)}
      val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), conditions , null, new Gems)
      val tipTiltMode = GemsTipTiltMode.instrument

      val posAngles = Set.empty[Angle]

      val results = Await.result(TestGemsStrategy("/gemsstrategyquery.xml").search(tipTiltMode, ctx, posAngles, scala.None), 20.seconds)
      results should be size 2

      results.head.criterion should beEqualTo(GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, GsaoiOdgw.Group.instance), CatalogSearchCriterion("On-detector Guide Window tiptilt", MagnitudeBand.H, MagnitudeRange(FaintnessConstraint(14.5), scala.Option(SaturationConstraint(7.3))), RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01666666666665151)), scala.Option(Offset(0.0014984027777700248.degrees[OffsetP], 0.0014984027777700248.degrees[OffsetQ])), scala.None)))
      results(1).criterion should beEqualTo(GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.flexure, Wfs.Group.instance), CatalogSearchCriterion("Canopus Wave Front Sensor flexure", MagnitudeBand.R, MagnitudeRange(FaintnessConstraint(15.0), scala.Option(SaturationConstraint(7.5))), RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01666666666665151)), scala.Option(Offset(0.0014984027777700248.degrees[OffsetP], 0.0014984027777700248.degrees[OffsetQ])), scala.None)))
      results.head.results should be size 5
      results(1).results should be size 5
    }
    "support search/select and analyze on SN-1987A" in {
      val ra = Angle.fromHMS(5, 35, 28.020).getOrElse(Angle.zero)
      val dec = Angle.zero - Angle.fromDMS(69, 16, 11.07).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi <| {_.setPosAngle(0.0)} <| {_.setIssPort(IssPort.UP_LOOKING)}
      val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), conditions, null, new Gems)
      val tipTiltMode = GemsTipTiltMode.canopus

      val posAngles = Set(GemsUtils4Java.toNewAngle(ctx.getPositionAngle), Angle.zero)

      val results = Await.result(TestGemsStrategy("/gems_sn1987A.xml").search(tipTiltMode, ctx, posAngles, scala.None), 20.seconds)
      results should be size 2

      results.head.criterion.key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, Wfs.Group.instance))
      results.head.criterion should beEqualTo(GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, Wfs.Group.instance), CatalogSearchCriterion("Canopus Wave Front Sensor tiptilt", MagnitudeBand.R, MagnitudeRange(FaintnessConstraint(15.0), scala.Option(SaturationConstraint(7.5))), RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01666666666665151)), scala.Option(Offset(0.0014984027777700248.degrees[OffsetP], 0.0014984027777700248.degrees[OffsetQ])), scala.Some(Angle.zero))))
      results(1).criterion.key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance))
      results(1).criterion should beEqualTo(GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance), CatalogSearchCriterion("On-detector Guide Window flexure", MagnitudeBand.H, MagnitudeRange(FaintnessConstraint(17.0), scala.Option(SaturationConstraint(8.0))), RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01666666666665151)), scala.Option(Offset(0.0014984027777700248.degrees[OffsetP], 0.0014984027777700248.degrees[OffsetQ])), scala.Some(Angle.zero))))
      results.head.results should be size 5
      results(1).results should be size 9

      val selection = Await.result(TestGemsStrategy("/gems_sn1987A.xml").select(ctx, ProbeLimitsTable.loadOrThrow()), 2.minutes)
      selection.map(_.posAngle) should beSome(Angle.zero)
      val assignments = ~selection.map(_.assignments)
      assignments should be size 4

      val gp1 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs1).map(_.guideProbe)
      val cwfs1 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs1).map(_.guideStar)
      val gp2 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs2).map(_.guideProbe)
      val cwfs2 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs2).map(_.guideStar)
      val gp3 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs3).map(_.guideProbe)
      val cwfs3 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs3).map(_.guideStar)
      val gp4 = assignments.find(_.guideProbe == GsaoiOdgw.odgw2).map(_.guideProbe)
      val odgw2 = assignments.find(_.guideProbe == GsaoiOdgw.odgw2).map(_.guideStar)
      cwfs1.map(_.name) should beSome("104-014597")
      cwfs2.map(_.name) should beSome("104-014547")
      cwfs3.map(_.name) should beSome("104-014608")
      odgw2.map(_.name) should beSome("104-014556")

      val cwfs1x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(5, 35, 32.630).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(69, 15, 48.64).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs1.map(_.coordinates ~= cwfs1x) should beSome(true)
      val cwfs2x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(5, 35, 18.423).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(69, 16, 30.67).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs2.map(_.coordinates ~= cwfs2x) should beSome(true)
      val cwfs3x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(5, 35, 36.409).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(69, 16, 24.17).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs3.map(_.coordinates ~= cwfs3x) should beSome(true)
      val odgw2x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(5, 35, 23.887).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(69, 16, 18.20).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      odgw2.map(_.coordinates ~= odgw2x) should beSome(true)

      val newCtx = selection.map(applySelection(ctx, _)).getOrElse(ctx)
      println(TestGemsStrategy("/gems_sn1987A.xml").magnitudes(newCtx, ProbeLimitsTable.loadOrThrow()))
     // TestGemsStrategy("/gems_sn1987A.xml").analyze(newCtx, ProbeLimitsTable.loadOrThrow()).forall(a => a, _.quality == )

    }
  }
}
