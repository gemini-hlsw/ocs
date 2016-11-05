package edu.gemini.ags.impl

import edu.gemini.ags.api.{AgsAnalysis, AgsGuideQuality, AgsStrategy}
import edu.gemini.ags.api.AgsStrategy.Estimate
import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.ags.gems.{GemsCatalogSearchCriterion, GemsCatalogSearchKey, CatalogSearchCriterion}
import edu.gemini.catalog.api._
import edu.gemini.catalog.votable.TestVoTableBackend
import edu.gemini.shared.util.immutable.{Some => JSome, None => JNone }
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.AngleSyntax._
import edu.gemini.pot.ModelConverters._
import edu.gemini.spModel.gemini.gems.Canopus.Wfs
import edu.gemini.spModel.gemini.gems.{Gems, Canopus}
import edu.gemini.spModel.gemini.gsaoi.{Gsaoi, GsaoiOdgw}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gems.{GemsGuideStarType, GemsTipTiltMode}
import edu.gemini.spModel.guide.GuideSpeed
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe
import edu.gemini.spModel.telescope.IssPort
import org.specs2.mutable.Specification
import AlmostEqual.AlmostEqualOps

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import scalaz._
import Scalaz._

case class TestGemsStrategy(file: String) extends GemsStrategy {
  override val backend = TestVoTableBackend(file)
}

class GemsStrategySpec extends Specification {

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

      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), SPSiteQuality.Conditions.BEST, null, new Gems, JNone.instance())

      val estimate = TestGemsStrategy("/gemsstrategyquery.xml").estimate(ctx, ProbeLimitsTable.loadOrThrow())(implicitly)
      Await.result(estimate, 20.seconds) should beEqualTo(Estimate.GuaranteedSuccess)
    }
    "support search" in {
      val ra = Angle.fromHMS(3, 19, 48.2341).getOrElse(Angle.zero)
      val dec = Angle.fromDMS(41, 30, 42.078).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi <| {_.setPosAngle(0.0)} <| {_.setIssPort(IssPort.UP_LOOKING)}
      val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), conditions , null, new Gems, JNone.instance())
      val tipTiltMode = GemsTipTiltMode.instrument

      val posAngles = Set.empty[Angle]

      val results = Await.result(TestGemsStrategy("/gemsstrategyquery.xml").search(tipTiltMode, ctx, posAngles, scala.None)(implicitly), 20.seconds)
      results should be size 2

      results.head.criterion should beEqualTo(GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, GsaoiOdgw.Group.instance), CatalogSearchCriterion("On-detector Guide Window tiptilt", RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01666666666665151)), MagnitudeConstraints(SingleBand(MagnitudeBand.H), FaintnessConstraint(14.5), scala.Option(SaturationConstraint(7.3))), scala.Option(Offset(0.0014984027777700248.degrees[OffsetP], 0.0014984027777700248.degrees[OffsetQ])), scala.None)))
      results(1).criterion should beEqualTo(GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.flexure, Wfs.Group.instance), CatalogSearchCriterion("Canopus Wave Front Sensor flexure", RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01666666666665151)), MagnitudeConstraints(RBandsList, FaintnessConstraint(15.8), scala.Option(SaturationConstraint(8.3))), scala.Option(Offset(0.0014984027777700248.degrees[OffsetP], 0.0014984027777700248.degrees[OffsetQ])), scala.None)))
      results.head.results should be size 5
      results(1).results should be size 4
    }
    "support search/select and analyze on Pal 1 gives guide stars on the northern hemisphere" in {
      val ra = Angle.fromHMS(3, 33, 20.040).getOrElse(Angle.zero)
      val dec = Angle.fromDMS(79, 34, 51.80).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi <| {_.setPosAngle(0.0)} <| {_.setIssPort(IssPort.UP_LOOKING)}
      val conditions = SPSiteQuality.Conditions.NOMINAL
      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), conditions, null, new Gems, JNone.instance())
      val tipTiltMode = GemsTipTiltMode.canopus

      val posAngles = Set(ctx.getPositionAngle, Angle.zero, Angle.fromDegrees(90), Angle.fromDegrees(180), Angle.fromDegrees(270))

      testSearchOnNominal("/gems_pal1.xml", ctx, tipTiltMode, posAngles, 2, 2)

      val gemsStrategy = TestGemsStrategy("/gems_pal1.xml")
      val selection = Await.result(gemsStrategy.select(ctx, ProbeLimitsTable.loadOrThrow())(implicitly), 2.minutes)
      selection.map(_.posAngle) should beSome(Angle.zero)
      val assignments = ~selection.map(_.assignments)
      assignments should be size 3

      assignments.exists(_.guideProbe == Canopus.Wfs.cwfs2) should beTrue
      val cwfs2 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs2).map(_.guideStar)
      assignments.exists(_.guideProbe == Canopus.Wfs.cwfs3) should beTrue
      val cwfs3 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs3).map(_.guideStar)
      assignments.exists(_.guideProbe == GsaoiOdgw.odgw4) should beTrue
      val odgw4 = assignments.find(_.guideProbe == GsaoiOdgw.odgw4).map(_.guideStar)

      // Check coordinates
      cwfs2.map(_.name) should beSome("848-004584")
      cwfs3.map(_.name) should beSome("848-004582")
      odgw4.map(_.name) should beSome("848-004582")

      val cwfs2x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(3, 33, 21.838).getOrElse(Angle.zero)), Declination.fromAngle(Angle.fromDMS(79, 35, 38.20).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs2.map(_.coordinates ~= cwfs2x) should beSome(true)
      val cwfs3x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(3, 33, 20.936).getOrElse(Angle.zero)), Declination.fromAngle(Angle.fromDMS(79, 34, 56.93).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs3.map(_.coordinates ~= cwfs3x) should beSome(true)
      val odgw4x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(3, 33, 20.936).getOrElse(Angle.zero)), Declination.fromAngle(Angle.fromDMS(79, 34, 56.93).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      odgw4.map(_.coordinates ~= odgw4x) should beSome(true)

      // Check magnitudes are sorted correctly
      val mag2 = cwfs2.flatMap(RBandsList.extract).map(_.value)
      val mag3 = cwfs3.flatMap(RBandsList.extract).map(_.value)
      (mag3 < mag2) should beTrue

      // Analyze as a whole
      val newCtx = selection.map(_.applyTo(ctx)).getOrElse(ctx)
      val analysis = gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow())
      analysis.collect {
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs2, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs2                         => Canopus.Wfs.cwfs2
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs3, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs3                         => Canopus.Wfs.cwfs3
        case AgsAnalysis.Usable(GsaoiOdgw.odgw4, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, SingleBand(MagnitudeBand.H)) if st.some == odgw4 => GsaoiOdgw.odgw4
      } should beEqualTo(List(Canopus.Wfs.cwfs2, Canopus.Wfs.cwfs3, GsaoiOdgw.odgw4))

      // Analyze per probe
      cwfs2.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs2, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs2, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      cwfs3.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs3, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs3, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      odgw4.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), GsaoiOdgw.odgw4, s).exists(_ == AgsAnalysis.Usable(GsaoiOdgw.odgw4, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, SingleBand(MagnitudeBand.H)))
      } should beSome(true)

      // Test estimate
      val estimate = gemsStrategy.estimate(ctx, ProbeLimitsTable.loadOrThrow())(implicitly)
      Await.result(estimate, 20.seconds) should beEqualTo(Estimate.GuaranteedSuccess)
    }
    "support search/select and analyze on SN-1987A" in {
      val ra = Angle.fromHMS(5, 35, 28.020).getOrElse(Angle.zero)
      val dec = Angle.zero - Angle.fromDMS(69, 16, 11.07).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi <| {_.setPosAngle(0.0)} <| {_.setIssPort(IssPort.UP_LOOKING)}
      val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), conditions, null, new Gems, JNone.instance())
      val tipTiltMode = GemsTipTiltMode.canopus

      val posAngles = Set(ctx.getPositionAngle, Angle.zero, Angle.fromDegrees(90), Angle.fromDegrees(180), Angle.fromDegrees(270))

      testSearchOnStandardConditions("/gems_sn1987A.xml", ctx, tipTiltMode, posAngles, 9, 9)

      val gemsStrategy = TestGemsStrategy("/gems_sn1987A.xml")
      val selection = Await.result(gemsStrategy.select(ctx, ProbeLimitsTable.loadOrThrow())(implicitly), 2.minutes)
      selection.map(_.posAngle) should beSome(Angle.zero)
      val assignments = ~selection.map(_.assignments)
      assignments should be size 4

      assignments.exists(_.guideProbe == Canopus.Wfs.cwfs1) should beTrue
      val cwfs1 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs1).map(_.guideStar)
      assignments.exists(_.guideProbe == Canopus.Wfs.cwfs2) should beTrue
      val cwfs2 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs2).map(_.guideStar)
      assignments.exists(_.guideProbe == Canopus.Wfs.cwfs3) should beTrue
      val cwfs3 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs3).map(_.guideStar)
      assignments.exists(_.guideProbe == GsaoiOdgw.odgw2) should beTrue
      val odgw2 = assignments.find(_.guideProbe == GsaoiOdgw.odgw2).map(_.guideStar)

      // Check coordinates
      cwfs1.map(_.name) should beSome("104-014597")
      cwfs2.map(_.name) should beSome("104-014608")
      cwfs3.map(_.name) should beSome("104-014547")
      odgw2.map(_.name) should beSome("104-014556")

      val cwfs1x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(5, 35, 32.630).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(69, 15, 48.64).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs1.map(_.coordinates ~= cwfs1x) should beSome(true)
      val cwfs2x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(5, 35, 36.409).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(69, 16, 24.17).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs2.map(_.coordinates ~= cwfs2x) should beSome(true)
      val cwfs3x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(5, 35, 18.423).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(69, 16, 30.67).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs3.map(_.coordinates ~= cwfs3x) should beSome(true)
      val odgw2x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(5, 35, 23.887).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(69, 16, 18.20).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      odgw2.map(_.coordinates ~= odgw2x) should beSome(true)

      // Check magnitudes are sorted correctly
      val mag1 = cwfs1.flatMap(RBandsList.extract).map(_.value)
      val mag2 = cwfs2.flatMap(RBandsList.extract).map(_.value)
      val mag3 = cwfs3.flatMap(RBandsList.extract).map(_.value)
      (mag3 < mag1 && mag2 < mag1) should beTrue

      // Analyze as a whole
      val newCtx = selection.map(_.applyTo(ctx)).getOrElse(ctx)
      val analysis = gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow())
      analysis.collect {
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs1, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs1                         => Canopus.Wfs.cwfs1
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs2, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs2                         => Canopus.Wfs.cwfs2
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs3, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs3                         => Canopus.Wfs.cwfs3
        case AgsAnalysis.Usable(GsaoiOdgw.odgw2, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, SingleBand(MagnitudeBand.H)) if st.some == odgw2 => GsaoiOdgw.odgw2
      } should beEqualTo(List(Canopus.Wfs.cwfs1, Canopus.Wfs.cwfs2, Canopus.Wfs.cwfs3, GsaoiOdgw.odgw2))

      // Analyze per probe
      cwfs1.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs1, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs1, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      cwfs2.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs2, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs2, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      cwfs3.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs3, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs3, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      odgw2.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), GsaoiOdgw.odgw2, s).exists(_ == AgsAnalysis.Usable(GsaoiOdgw.odgw2, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, SingleBand(MagnitudeBand.H)))
      } should beSome(true)

      // Test estimate
      val estimate = gemsStrategy.estimate(ctx, ProbeLimitsTable.loadOrThrow())(implicitly)
      Await.result(estimate, 20.seconds) should beEqualTo(Estimate.GuaranteedSuccess)
    }
    "support search/select and analyze on SN-1987A part 2" in {
      val ra = Angle.fromHMS(5, 35, 28.020).getOrElse(Angle.zero)
      val dec = Angle.zero - Angle.fromDMS(69, 16, 11.07).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi <| {_.setPosAngle(0.0)} <| {_.setIssPort(IssPort.UP_LOOKING)}
      val conditions = SPSiteQuality.Conditions.BEST.cc(SPSiteQuality.CloudCover.PERCENT_50)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), conditions, null, new Gems, JNone.instance())
      val tipTiltMode = GemsTipTiltMode.canopus

      val posAngles = Set(ctx.getPositionAngle, Angle.zero, Angle.fromDegrees(90), Angle.fromDegrees(180), Angle.fromDegrees(270))

      testSearchOnBestConditions("/gems_sn1987A.xml", ctx, tipTiltMode, posAngles, 12, 9)

      val gemsStrategy = TestGemsStrategy("/gems_sn1987A.xml")
      val selection = Await.result(gemsStrategy.select(ctx, ProbeLimitsTable.loadOrThrow())(implicitly), 2.minutes)
      selection.map(_.posAngle) should beSome(Angle.zero)
      val assignments = ~selection.map(_.assignments)
      assignments should be size 4

      val cwfs1 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs1).map(_.guideStar)
      val cwfs2 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs2).map(_.guideStar)
      val cwfs3 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs3).map(_.guideStar)
      val odgw2 = assignments.find(_.guideProbe == GsaoiOdgw.odgw2).map(_.guideStar)
      cwfs1.map(_.name) should beSome("104-014597")
      cwfs2.map(_.name) should beSome("104-014608")
      cwfs3.map(_.name) should beSome("104-014547")
      odgw2.map(_.name) should beSome("104-014556")

      val cwfs1x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(5, 35, 32.630).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(69, 15, 48.64).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs1.map(_.coordinates ~= cwfs1x) should beSome(true)
      val cwfs2x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(5, 35, 36.409).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(69, 16, 24.17).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs2.map(_.coordinates ~= cwfs2x) should beSome(true)
      val cwfs3x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(5, 35, 18.423).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(69, 16, 30.67).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs3.map(_.coordinates ~= cwfs3x) should beSome(true)
      val odgw2x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(5, 35, 23.887).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(69, 16, 18.20).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      odgw2.map(_.coordinates ~= odgw2x) should beSome(true)

      val newCtx = selection.map(_.applyTo(ctx)).getOrElse(ctx)
      // Analyze as a whole
      val analysis = gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow())
      analysis.collect {
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs1, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs1                         => Canopus.Wfs.cwfs1
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs2, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs2                         => Canopus.Wfs.cwfs2
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs3, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs3                         => Canopus.Wfs.cwfs3
        case AgsAnalysis.Usable(GsaoiOdgw.odgw2, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, SingleBand(MagnitudeBand.H)) if st.some == odgw2 => GsaoiOdgw.odgw2
      } should beEqualTo(List(Canopus.Wfs.cwfs1, Canopus.Wfs.cwfs2, Canopus.Wfs.cwfs3, GsaoiOdgw.odgw2))
      // Analyze per probe
      cwfs1.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs1, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs1, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      cwfs2.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs2, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs2, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      cwfs3.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs3, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs3, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      odgw2.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), GsaoiOdgw.odgw2, s).exists(_ == AgsAnalysis.Usable(GsaoiOdgw.odgw2, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, SingleBand(MagnitudeBand.H)))
      } should beSome(true)

      // Test estimate
      val estimate = gemsStrategy.estimate(ctx, ProbeLimitsTable.loadOrThrow())(implicitly)
      Await.result(estimate, 20.seconds) should beEqualTo(Estimate.GuaranteedSuccess)
    }
    "support search/select and analyze on TYC 8345-1155-1" in {
      val ra = Angle.fromHMS(17, 25, 27.529).getOrElse(Angle.zero)
      val dec = Angle.zero - Angle.fromDMS(48, 27, 24.02).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi <| {_.setPosAngle(0.0)} <| {_.setIssPort(IssPort.UP_LOOKING)}
      val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY).wv(SPSiteQuality.WaterVapor.ANY)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), conditions, null, new Gems, JNone.instance())
      val tipTiltMode = GemsTipTiltMode.canopus

      val posAngles = Set(ctx.getPositionAngle, Angle.zero, Angle.fromDegrees(90), Angle.fromDegrees(180), Angle.fromDegrees(270))

      testSearchOnStandardConditions("/gems_TYC_8345_1155_1.xml", ctx, tipTiltMode, posAngles, 25, 28)

      val gemsStrategy = TestGemsStrategy("/gems_TYC_8345_1155_1.xml")
      val selection = Await.result(gemsStrategy.select(ctx, ProbeLimitsTable.loadOrThrow())(implicitly), 2.minutes)
      selection.map(_.posAngle) should beSome(Angle.zero)
      val assignments = ~selection.map(_.assignments)
      assignments should be size 4

      assignments.exists(_.guideProbe == Canopus.Wfs.cwfs1) should beTrue
      val cwfs1 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs1).map(_.guideStar)
      assignments.exists(_.guideProbe == Canopus.Wfs.cwfs2) should beTrue
      val cwfs2 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs2).map(_.guideStar)
      assignments.exists(_.guideProbe == Canopus.Wfs.cwfs3) should beTrue
      val cwfs3 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs3).map(_.guideStar)
      assignments.exists(_.guideProbe == GsaoiOdgw.odgw4) should beTrue
      val odgw4 = assignments.find(_.guideProbe == GsaoiOdgw.odgw4).map(_.guideStar)
      cwfs1.map(_.name) should beSome("208-152095")
      cwfs2.map(_.name) should beSome("208-152215")
      cwfs3.map(_.name) should beSome("208-152039")
      odgw4.map(_.name) should beSome("208-152102")

      val cwfs1x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(17, 25, 27.151).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(48, 28, 07.67).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs1.map(_.coordinates ~= cwfs1x) should beSome(true)
      val cwfs2x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(17, 25, 32.541).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(48, 27, 30.06).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs2.map(_.coordinates ~= cwfs2x) should beSome(true)
      val cwfs3x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(17, 25, 24.719).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(48, 26, 58.00).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs3.map(_.coordinates ~= cwfs3x) should beSome(true)
      val odgw2x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(17, 25, 27.552).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(48, 27, 23.86).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      odgw4.map(_.coordinates ~= odgw2x) should beSome(true)

      // Check magnitudes are sorted correctly
      val mag1 = cwfs1.flatMap(RBandsList.extract).map(_.value)
      val mag2 = cwfs2.flatMap(RBandsList.extract).map(_.value)
      val mag3 = cwfs3.flatMap(RBandsList.extract).map(_.value)
      (mag3 < mag1 && mag2 < mag1) should beTrue

      // Analyze as a whole
      val newCtx = selection.map(_.applyTo(ctx)).getOrElse(ctx)
      val analysis = gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow())
      analysis.collect {
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs1, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs1 => Canopus.Wfs.cwfs1
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs2, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs2 => Canopus.Wfs.cwfs2
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs3, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs3 => Canopus.Wfs.cwfs3
        case AgsAnalysis.Usable(GsaoiOdgw.odgw4, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == odgw4   => GsaoiOdgw.odgw4
      } should beEqualTo(List(Canopus.Wfs.cwfs1, Canopus.Wfs.cwfs2, Canopus.Wfs.cwfs3, GsaoiOdgw.odgw4))
      // Analyze per probe
      cwfs1.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs1, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs1, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      cwfs2.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs2, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs2, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      cwfs3.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs3, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs3, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      odgw4.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), GsaoiOdgw.odgw4, s).exists(_ == AgsAnalysis.Usable(GsaoiOdgw.odgw4, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, SingleBand(MagnitudeBand.H)))
      } should beSome(true)

      // Test estimate
      val estimate = gemsStrategy.estimate(ctx, ProbeLimitsTable.loadOrThrow())(implicitly)
      Await.result(estimate, 20.seconds) should beEqualTo(Estimate.GuaranteedSuccess)
    }
    "support search/select and analyze on M6" in {
      val ra = Angle.fromHMS(17, 40, 20.0).getOrElse(Angle.zero)
      val dec = Angle.zero - Angle.fromDMS(32, 15, 12.0).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi <| {_.setPosAngle(0.0)} <| {_.setIssPort(IssPort.UP_LOOKING)}
      val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY).wv(SPSiteQuality.WaterVapor.ANY)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), conditions, null, new Gems, JNone.instance())
      val tipTiltMode = GemsTipTiltMode.canopus

      val posAngles = Set(ctx.getPositionAngle, Angle.zero, Angle.fromDegrees(90), Angle.fromDegrees(180), Angle.fromDegrees(270))

      testSearchOnStandardConditions("/gems_m6.xml", ctx, tipTiltMode, posAngles, 5, 7)

      val gemsStrategy = TestGemsStrategy("/gems_m6.xml")
      val selection = Await.result(gemsStrategy.select(ctx, ProbeLimitsTable.loadOrThrow())(implicitly), 2.minutes)
      selection.map(_.posAngle) should beSome(Angle.fromDegrees(90))
      val assignments = ~selection.map(_.assignments)
      assignments should be size 4

      assignments.exists(_.guideProbe == Canopus.Wfs.cwfs1) should beTrue
      val cwfs1 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs1).map(_.guideStar)
      assignments.exists(_.guideProbe == Canopus.Wfs.cwfs2) should beTrue
      val cwfs2 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs2).map(_.guideStar)
      assignments.exists(_.guideProbe == Canopus.Wfs.cwfs3) should beTrue
      val cwfs3 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs3).map(_.guideStar)
      assignments.exists(_.guideProbe == GsaoiOdgw.odgw2) should beTrue
      val odgw2 = assignments.find(_.guideProbe == GsaoiOdgw.odgw2).map(_.guideStar)
      cwfs1.map(_.name) should beSome("289-128909")
      cwfs2.map(_.name) should beSome("289-128878")
      cwfs3.map(_.name) should beSome("289-128908")
      odgw2.map(_.name) should beSome("289-128891")

      val cwfs1x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(17, 40, 21.743).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(32, 14, 54.04).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs1.map(_.coordinates ~= cwfs1x) should beSome(true)
      val cwfs2x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(17, 40, 16.855).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(32, 15, 55.83).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs2.map(_.coordinates ~= cwfs2x) should beSome(true)
      val cwfs3x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(17, 40, 21.594).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(32, 15, 50.38).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs3.map(_.coordinates ~= cwfs3x) should beSome(true)
      val odgw2x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(17, 40, 19.295).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(32, 14, 58.34).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      odgw2.map(_.coordinates ~= odgw2x) should beSome(true)

      // Check magnitudes are sorted correctly
      val mag1 = cwfs1.flatMap(RBandsList.extract).map(_.value)
      val mag2 = cwfs2.flatMap(RBandsList.extract).map(_.value)
      val mag3 = cwfs3.flatMap(RBandsList.extract).map(_.value)
      (mag3 < mag1 && mag2 < mag1) should beTrue

      // Analyze as a whole
      val newCtx = selection.map(_.applyTo(ctx)).getOrElse(ctx)
      val analysis = gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow())
      analysis.collect {
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs1, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs1 => Canopus.Wfs.cwfs1
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs2, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs2 => Canopus.Wfs.cwfs2
        case AgsAnalysis.NotReachable(Canopus.Wfs.cwfs3, st, _) if st.some == cwfs3                                               => Canopus.Wfs.cwfs3
        case AgsAnalysis.Usable(GsaoiOdgw.odgw2, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == odgw2   => GsaoiOdgw.odgw2
      } should beEqualTo(List(Canopus.Wfs.cwfs1, Canopus.Wfs.cwfs2, Canopus.Wfs.cwfs3, GsaoiOdgw.odgw2))

      // Analyze per probe
      cwfs1.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs1, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs1, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      cwfs2.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs2, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs2, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      cwfs3.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs3, s).exists(_ == AgsAnalysis.NotReachable(Canopus.Wfs.cwfs3, s, RBandsList))
      } should beSome(true)
      odgw2.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), GsaoiOdgw.odgw2, s).exists(_ == AgsAnalysis.Usable(GsaoiOdgw.odgw2, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, SingleBand(MagnitudeBand.H)))
      } should beSome(true)

      // Test estimate
      val estimate = gemsStrategy.estimate(ctx, ProbeLimitsTable.loadOrThrow())(implicitly)
      Await.result(estimate, 20.seconds) should beEqualTo(Estimate.GuaranteedSuccess)
    }
    "support search/select and analyze on BPM 37093" in {
      val ra = Angle.fromHMS(12, 38, 49.820).getOrElse(Angle.zero)
      val dec = Angle.zero - Angle.fromDMS(49, 48, 0.20).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi <| {_.setPosAngle(0.0)} <| {_.setIssPort(IssPort.UP_LOOKING)}
      val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), conditions, null, new Gems, JNone.instance())
      val tipTiltMode = GemsTipTiltMode.canopus

      val posAngles = Set(ctx.getPositionAngle, Angle.zero, Angle.fromDegrees(90), Angle.fromDegrees(180), Angle.fromDegrees(270))

      testSearchOnStandardConditions("/gems_bpm_37093.xml", ctx, tipTiltMode, posAngles, 4, 5)

      val gemsStrategy = TestGemsStrategy("/gems_bpm_37093.xml")
      val selection = Await.result(gemsStrategy.select(ctx, ProbeLimitsTable.loadOrThrow())(implicitly), 2.minutes)
      selection.map(_.posAngle) should beSome(Angle.zero)
      val assignments = ~selection.map(_.assignments)
      assignments should be size 4

      val cwfs1 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs1).map(_.guideStar)
      val cwfs2 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs2).map(_.guideStar)
      val cwfs3 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs3).map(_.guideStar)
      val odgw4 = assignments.find(_.guideProbe == GsaoiOdgw.odgw4).map(_.guideStar)
      cwfs1.map(_.name) should beSome("202-067216")
      cwfs2.map(_.name) should beSome("202-067200")
      cwfs3.map(_.name) should beSome("201-071218")
      odgw4.map(_.name) should beSome("201-071218")

      val cwfs1x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(12, 38, 50.130).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(49, 47, 38.07).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs1.map(_.coordinates ~= cwfs1x) should beSome(true)
      val cwfs2x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(12, 38, 44.500).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(49, 47, 58.38).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs2.map(_.coordinates ~= cwfs2x) should beSome(true)
      val cwfs3x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(12, 38, 50.005).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(49, 48, 00.89).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs3.map(_.coordinates ~= cwfs3x) should beSome(true)
      val odgw4x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(12, 38, 50.005).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(49, 48, 00.89).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      odgw4.map(_.coordinates ~= odgw4x) should beSome(true)

      val newCtx = selection.map(_.applyTo(ctx)).getOrElse(ctx)
      // Analyze all the probes at once
      gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow()).collect {
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs1, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs1                         => Canopus.Wfs.cwfs1
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs2, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs2                         => Canopus.Wfs.cwfs2
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs3, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs3                         => Canopus.Wfs.cwfs3
        case AgsAnalysis.Usable(GsaoiOdgw.odgw4, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, SingleBand(MagnitudeBand.H)) if st.some == odgw4 => GsaoiOdgw.odgw4
      } should beEqualTo(List(Canopus.Wfs.cwfs1, Canopus.Wfs.cwfs2, Canopus.Wfs.cwfs3, GsaoiOdgw.odgw4))
      // Analyze per probe
      cwfs1.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs1, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs1, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      cwfs2.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs2, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs2, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      cwfs3.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs3, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs3, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      odgw4.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), GsaoiOdgw.odgw4, s).exists(_ == AgsAnalysis.Usable(GsaoiOdgw.odgw4, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, SingleBand(MagnitudeBand.H)))
      } should beSome(true)

      // Test estimate
      val estimate = gemsStrategy.estimate(ctx, ProbeLimitsTable.loadOrThrow())(implicitly)
      Await.result(estimate, 20.seconds) should beEqualTo(Estimate.GuaranteedSuccess)
    }
    "support search/select and analyze on BPM 37093 part 2" in {
      val ra = Angle.fromHMS(12, 38, 49.820).getOrElse(Angle.zero)
      val dec = Angle.zero - Angle.fromDMS(49, 48, 0.20).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new Gsaoi <| {_.setPosAngle(0.0)} <| {_.setIssPort(IssPort.UP_LOOKING)}
      val conditions = SPSiteQuality.Conditions.BEST.cc(SPSiteQuality.CloudCover.PERCENT_50)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), conditions, null, new Gems, JNone.instance())
      val tipTiltMode = GemsTipTiltMode.canopus

      val posAngles = Set(ctx.getPositionAngle, Angle.zero, Angle.fromDegrees(90), Angle.fromDegrees(180), Angle.fromDegrees(270))

      testSearchOnBestConditions("/gems_bpm_37093.xml", ctx, tipTiltMode, posAngles, 5, 5)

      val gemsStrategy = TestGemsStrategy("/gems_bpm_37093.xml")
      val selection = Await.result(gemsStrategy.select(ctx, ProbeLimitsTable.loadOrThrow())(implicitly), 2.minutes)
      selection.map(_.posAngle) should beSome(Angle.zero)
      val assignments = ~selection.map(_.assignments)
      assignments should be size 4

      val cwfs1 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs1).map(_.guideStar)
      val cwfs2 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs2).map(_.guideStar)
      val cwfs3 = assignments.find(_.guideProbe == Canopus.Wfs.cwfs3).map(_.guideStar)
      val odgw4 = assignments.find(_.guideProbe == GsaoiOdgw.odgw4).map(_.guideStar)
      cwfs1.map(_.name) should beSome("202-067216")
      cwfs2.map(_.name) should beSome("202-067200")
      cwfs3.map(_.name) should beSome("201-071218")
      odgw4.map(_.name) should beSome("201-071218")

      val cwfs1x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(12, 38, 50.130).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(49, 47, 38.07).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs1.map(_.coordinates ~= cwfs1x) should beSome(true)
      val cwfs2x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(12, 38, 44.500).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(49, 47, 58.38).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs2.map(_.coordinates ~= cwfs2x) should beSome(true)
      val cwfs3x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(12, 38, 50.005).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(49, 48, 00.89).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      cwfs3.map(_.coordinates ~= cwfs3x) should beSome(true)
      val odgw4x = Coordinates(RightAscension.fromAngle(Angle.fromHMS(12, 38, 50.005).getOrElse(Angle.zero)), Declination.fromAngle(Angle.zero - Angle.fromDMS(49, 48, 00.89).getOrElse(Angle.zero)).getOrElse(Declination.zero))
      odgw4.map(_.coordinates ~= odgw4x) should beSome(true)

      val newCtx = selection.map(_.applyTo(ctx)).getOrElse(ctx)
      // Analyze all the probes at once
      gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow()).collect {
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs1, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs1                         => Canopus.Wfs.cwfs1
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs2, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs2                         => Canopus.Wfs.cwfs2
        case AgsAnalysis.Usable(Canopus.Wfs.cwfs3, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, _) if st.some == cwfs3                         => Canopus.Wfs.cwfs3
        case AgsAnalysis.Usable(GsaoiOdgw.odgw4, st, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, SingleBand(MagnitudeBand.H)) if st.some == odgw4 => GsaoiOdgw.odgw4
      } should beEqualTo(List(Canopus.Wfs.cwfs1, Canopus.Wfs.cwfs2, Canopus.Wfs.cwfs3, GsaoiOdgw.odgw4))
      // Analyze per probe
      cwfs1.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs1, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs1, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      cwfs2.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs2, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs2, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      cwfs3.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), Canopus.Wfs.cwfs3, s).exists(_ == AgsAnalysis.Usable(Canopus.Wfs.cwfs3, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, RBandsList))
      } should beSome(true)
      odgw4.map { s =>
        gemsStrategy.analyze(newCtx, ProbeLimitsTable.loadOrThrow(), GsaoiOdgw.odgw4, s).exists(_ == AgsAnalysis.Usable(GsaoiOdgw.odgw4, s, GuideSpeed.FAST, AgsGuideQuality.DeliversRequestedIq, SingleBand(MagnitudeBand.H)))
      } should beSome(true)

      // Test estimate
      val estimate = gemsStrategy.estimate(ctx, ProbeLimitsTable.loadOrThrow())(implicitly)
      Await.result(estimate, 20.seconds) should beEqualTo(Estimate.GuaranteedSuccess)
    }
  }

  def testSearchOnStandardConditions(file: String, ctx: ObsContext, tipTiltMode: GemsTipTiltMode, posAngles: Set[Angle], expectedTipTiltResultsCount: Int, expectedFlexureResultsCount: Int): Unit = {
    val results = Await.result(TestGemsStrategy(file).search(tipTiltMode, ctx, posAngles, scala.None)(implicitly), 20.seconds)
    results should be size 2

    results.head.criterion.key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, Wfs.Group.instance))
    results.head.criterion should beEqualTo(GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, Wfs.Group.instance), CatalogSearchCriterion("Canopus Wave Front Sensor tiptilt", RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01666666666665151)), MagnitudeConstraints(RBandsList, FaintnessConstraint(15.8), scala.Option(SaturationConstraint(8.3))), scala.Option(Offset(0.0014984027777700248.degrees[OffsetP], 0.0014984027777700248.degrees[OffsetQ])), scala.Some(Angle.zero))))
    results(1).criterion.key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance))
    results(1).criterion should beEqualTo(GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance), CatalogSearchCriterion("On-detector Guide Window flexure", RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01666666666665151)), MagnitudeConstraints(SingleBand(MagnitudeBand.H), FaintnessConstraint(17.0), scala.Option(SaturationConstraint(8.0))), scala.Option(Offset(0.0014984027777700248.degrees[OffsetP], 0.0014984027777700248.degrees[OffsetQ])), scala.Some(Angle.zero))))
    results.head.results should be size expectedTipTiltResultsCount
    results(1).results should be size expectedFlexureResultsCount
  }

  def testSearchOnNominal(file: String, ctx: ObsContext, tipTiltMode: GemsTipTiltMode, posAngles: Set[Angle], expectedTipTiltResultsCount: Int, expectedFlexureResultsCount: Int): Unit = {
    val results = Await.result(TestGemsStrategy(file).search(tipTiltMode, ctx, posAngles, scala.None)(implicitly), 20.seconds)
    results should be size 2

    results.head.criterion.key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, Wfs.Group.instance))
    results.head.criterion should beEqualTo(GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, Wfs.Group.instance), CatalogSearchCriterion("Canopus Wave Front Sensor tiptilt", RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01666666666665151)), MagnitudeConstraints(RBandsList, FaintnessConstraint(16.3), scala.Option(SaturationConstraint(8.8))), scala.Option(Offset(0.0014984027777700248.degrees[OffsetP], 0.0014984027777700248.degrees[OffsetQ])), scala.Some(Angle.zero))))
    results(1).criterion.key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance))
    results(1).criterion should beEqualTo(GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance), CatalogSearchCriterion("On-detector Guide Window flexure", RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01666666666665151)), MagnitudeConstraints(SingleBand(MagnitudeBand.H), FaintnessConstraint(17.0), scala.Option(SaturationConstraint(8.0))), scala.Option(Offset(0.0014984027777700248.degrees[OffsetP], 0.0014984027777700248.degrees[OffsetQ])), scala.Some(Angle.zero))))
    results.head.results should be size expectedTipTiltResultsCount
    results(1).results should be size expectedFlexureResultsCount
  }

  def testSearchOnBestConditions(file: String, ctx: ObsContext, tipTiltMode: GemsTipTiltMode, posAngles: Set[Angle], expectedTipTiltResultsCount: Int, expectedFlexureResultsCount: Int): Unit = {
    val results = Await.result(TestGemsStrategy(file).search(tipTiltMode, ctx, posAngles, scala.None)(implicitly), 20.seconds)
    results should be size 2

    results.head.criterion.key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, Wfs.Group.instance))
    results.head.criterion should beEqualTo(GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, Wfs.Group.instance), CatalogSearchCriterion("Canopus Wave Front Sensor tiptilt", RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01666666666665151)), MagnitudeConstraints(RBandsList, FaintnessConstraint(16.8), scala.Option(SaturationConstraint(9.3))), scala.Option(Offset(0.0014984027777700248.degrees[OffsetP], 0.0014984027777700248.degrees[OffsetQ])), scala.Some(Angle.zero))))
    results(1).criterion.key should beEqualTo(GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance))
    results(1).criterion should beEqualTo(GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance), CatalogSearchCriterion("On-detector Guide Window flexure", RadiusConstraint.between(Angle.zero, Angle.fromDegrees(0.01666666666665151)), MagnitudeConstraints(SingleBand(MagnitudeBand.H), FaintnessConstraint(17.0), scala.Option(SaturationConstraint(8.0))), scala.Option(Offset(0.0014984027777700248.degrees[OffsetP], 0.0014984027777700248.degrees[OffsetQ])), scala.Some(Angle.zero))))
    results.head.results should be size expectedTipTiltResultsCount
    results(1).results should be size expectedFlexureResultsCount
  }
}
