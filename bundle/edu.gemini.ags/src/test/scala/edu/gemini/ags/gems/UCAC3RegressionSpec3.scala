package edu.gemini.ags.gems

import edu.gemini.ags.TargetsHelper
import edu.gemini.ags.gems.mascot.{MascotCat, Star}
import edu.gemini.catalog.api._
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.shared.util.immutable.{None => JNone}
import edu.gemini.spModel.core.AngleSyntax._
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gems.CanopusWfs
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{SkyBackground, WaterVapor}
import edu.gemini.spModel.gems.GemsGuideStarType
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.GuideGroup
import org.specs2.mutable.Specification

/**
 * Regression test on target SN 1987A with UCAC3 data and nominal conditions
 */
class UCAC3RegressionSpec3 extends Specification with UCAC3Regression with TargetsHelper {
  val conditions = SPSiteQuality.Conditions.NOMINAL.wv(WaterVapor.ANY).sb(SkyBackground.ANY)

  "Gems Analyze" should {
    "work with legacy UCAC3 values in nominal conditions" in {
      runAnalysis("05:35:28.020", "-69:16:11.07", conditions, tipTiltCriterion, flexureCriterion, tipTiltTargets, flexureTargets, expectedGuideStarsScn1) should beTrue
    }
    "work with legacy UCAC3 values in nominal conditions with random R-like bands" in {
      val replacedTargets = replaceRBands(tipTiltTargets, flexureTargets)
      runAnalysis("05:35:28.020", "-69:16:11.07", conditions, tipTiltCriterion, flexureCriterion, replacedTargets._1, replacedTargets._2, expectedGuideStarsScn1) should beTrue
    }
  }
  "MascotCat" should {
    "produce the correct stars from mascot with legacy UCAC3 values" in {
      val asterisms = MascotCat.findBestAsterism(targetsToMascot, 83.86675000000002, -69.26974166666668, 0.06)
      asterisms._2 should be size averageStrehl.size
      asterisms._2.zip(averageStrehl).foreach { case (a, e) =>
        a.avgstrehl should beEqualTo(e)
      }
      areAsterismStarsTheSame(asterisms._1, mascotStars) should beTrue
    }
    "produce the correct stars from mascot with legacy UCAC3 values and R-like magnitudes" in {
      val replacedTargets = replaceRBands(targetsToMascot)
      replacedTargets should be size targetsToMascot.size
      val targetsMap = replacedTargets.map(t => t.name -> t).toMap
      // Replace the bands on the mascot stars
      val replacedMascotStars = mascotStars.map { s =>
        val replacedTarget = targetsMap.getOrElse(s.target.name, s.target)
        s.copy(target = replacedTarget)
      }
      val asterisms = MascotCat.findBestAsterism(replacedTargets, 83.86675000000002, -69.26974166666668, 0.06)
      asterisms._2 should be size averageStrehl.size
      asterisms._2.zip(averageStrehl).foreach { case (a, e) =>
        a.avgstrehl should beEqualTo(e)
      }
      areAsterismStarsTheSame(asterisms._1, replacedMascotStars) should beTrue
    }
  }

  val tipTiltCriterion = GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.tiptilt, CanopusWfs.Group.instance), CatalogSearchCriterion("Canopus Wave Front Sensor", RadiusConstraint.between(Angle.zero, Angle.fromArcmin(1.0)), MagnitudeConstraints(RBandsList, FaintnessConstraint(15.0), Some(SaturationConstraint(7.5))), Some(Offset(5.394250.arcsecs[OffsetP], 5.394250.arcsecs[OffsetQ])), None))
  val tipTiltTargets = List(
    target("042-030622", Coordinates(RightAscension.fromAngle(Angle.parseHMS("5:35:23.888").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-69:16:18.2").getOrElse(Angle.zero)).getOrElse(Declination.zero)), List(new Magnitude(15.787, MagnitudeBand.J),new Magnitude(15.156, MagnitudeBand.K),new Magnitude(15.114, MagnitudeBand.H),new Magnitude(14.273, MagnitudeBand.R))),
    target("042-030635", Coordinates(RightAscension.fromAngle(Angle.parseHMS("5:35:24.997").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-69:16:04.77").getOrElse(Angle.zero)).getOrElse(Declination.zero)), List(new Magnitude(15.71, MagnitudeBand.J),new Magnitude(14.602, MagnitudeBand.K),new Magnitude(14.589, MagnitudeBand.H),new Magnitude(14.721, MagnitudeBand.R))),
    target("042-030696", Coordinates(RightAscension.fromAngle(Angle.parseHMS("5:35:36.179").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-69:16:09.21").getOrElse(Angle.zero)).getOrElse(Declination.zero)), List(new Magnitude(14.673, MagnitudeBand.J),new Magnitude(14.834, MagnitudeBand.K),new Magnitude(14.539, MagnitudeBand.H),new Magnitude(17.103, MagnitudeBand.B),new Magnitude(14.977, MagnitudeBand.R),new Magnitude(14.403, MagnitudeBand.I))),
    target("042-030698", Coordinates(RightAscension.fromAngle(Angle.parseHMS("5:35:36.405").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-69:16:24.08").getOrElse(Angle.zero)).getOrElse(Declination.zero)), List(new Magnitude(14.428, MagnitudeBand.J),new Magnitude(13.691, MagnitudeBand.K),new Magnitude(14.29, MagnitudeBand.H),new Magnitude(18.086, MagnitudeBand.B),new Magnitude(14.249, MagnitudeBand.R),new Magnitude(14.632, MagnitudeBand.I))),
    target("042-030696", Coordinates(RightAscension.fromAngle(Angle.parseHMS("5:35:36.179").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-69:16:09.21").getOrElse(Angle.zero)).getOrElse(Declination.zero)), List(new Magnitude(14.673, MagnitudeBand.J),new Magnitude(14.834, MagnitudeBand.K),new Magnitude(14.539, MagnitudeBand.H),new Magnitude(17.103, MagnitudeBand.B),new Magnitude(14.977, MagnitudeBand.R),new Magnitude(14.403, MagnitudeBand.I))),
    target("042-030698", Coordinates(RightAscension.fromAngle(Angle.parseHMS("5:35:36.405").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-69:16:24.08").getOrElse(Angle.zero)).getOrElse(Declination.zero)), List(new Magnitude(14.428, MagnitudeBand.J),new Magnitude(13.691, MagnitudeBand.K),new Magnitude(14.29, MagnitudeBand.H),new Magnitude(18.086, MagnitudeBand.B),new Magnitude(14.249, MagnitudeBand.R),new Magnitude(14.632, MagnitudeBand.I))))

  val flexureCriterion = GemsCatalogSearchCriterion(GemsCatalogSearchKey(GemsGuideStarType.flexure, GsaoiOdgw.Group.instance), CatalogSearchCriterion("On-detector Guide Window", RadiusConstraint.between(Angle.zero, Angle.fromArcmin(1.0)), MagnitudeConstraints(SingleBand(MagnitudeBand.H), FaintnessConstraint(17.0), Some(SaturationConstraint(8.0))), Some(Offset(OffsetP(Angle.fromDegrees(5.394250)), OffsetQ(Angle.fromDegrees(5.394250)))), None))

  val flexureTargets = List(
    target("042-030622", Coordinates(RightAscension.fromAngle(Angle.parseHMS("5:35:23.888").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-69:16:18.2").getOrElse(Angle.zero)).getOrElse(Declination.zero)), List(new Magnitude(15.787, MagnitudeBand.J),new Magnitude(15.156, MagnitudeBand.K),new Magnitude(15.114, MagnitudeBand.H),new Magnitude(14.273, MagnitudeBand.R))),
    target("042-030635", Coordinates(RightAscension.fromAngle(Angle.parseHMS("5:35:24.997").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-69:16:04.77").getOrElse(Angle.zero)).getOrElse(Declination.zero)), List(new Magnitude(15.71, MagnitudeBand.J),new Magnitude(14.602, MagnitudeBand.K),new Magnitude(14.589, MagnitudeBand.H),new Magnitude(14.721, MagnitudeBand.R))),
    target("042-030696", Coordinates(RightAscension.fromAngle(Angle.parseHMS("5:35:36.179").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-69:16:09.21").getOrElse(Angle.zero)).getOrElse(Declination.zero)), List(new Magnitude(14.673, MagnitudeBand.J),new Magnitude(14.834, MagnitudeBand.K),new Magnitude(14.539, MagnitudeBand.H),new Magnitude(17.103, MagnitudeBand.B),new Magnitude(14.977, MagnitudeBand.R),new Magnitude(14.403, MagnitudeBand.I))),
    target("042-030698", Coordinates(RightAscension.fromAngle(Angle.parseHMS("5:35:36.405").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-69:16:24.08").getOrElse(Angle.zero)).getOrElse(Declination.zero)), List(new Magnitude(14.428, MagnitudeBand.J),new Magnitude(13.691, MagnitudeBand.K),new Magnitude(14.29, MagnitudeBand.H),new Magnitude(18.086, MagnitudeBand.B),new Magnitude(14.249, MagnitudeBand.R),new Magnitude(14.632, MagnitudeBand.I))),
    target("042-030626", Coordinates(RightAscension.fromAngle(Angle.parseHMS("5:35:24.59").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-69:16:56.27").getOrElse(Angle.zero)).getOrElse(Declination.zero)), List(new Magnitude(15.578, MagnitudeBand.J),new Magnitude(15.095, MagnitudeBand.K),new Magnitude(15.089, MagnitudeBand.H),new Magnitude(18.729, MagnitudeBand.B),new Magnitude(16.806, MagnitudeBand.R),new Magnitude(15.382, MagnitudeBand.I))),
    target("042-030669", Coordinates(RightAscension.fromAngle(Angle.parseHMS("5:35:30.429").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-69:16:18.68").getOrElse(Angle.zero)).getOrElse(Declination.zero)), List(new Magnitude(16.322, MagnitudeBand.J),new Magnitude(16.721, MagnitudeBand.K),new Magnitude(15.804, MagnitudeBand.H),new Magnitude(18.65, MagnitudeBand.B),new Magnitude(16.314, MagnitudeBand.R),new Magnitude(15.653, MagnitudeBand.I))),
    target("042-030678", Coordinates(RightAscension.fromAngle(Angle.parseHMS("5:35:32.629").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-69:15:48.64").getOrElse(Angle.zero)).getOrElse(Declination.zero)), List(new Magnitude(13.801, MagnitudeBand.J),new Magnitude(13.414, MagnitudeBand.K),new Magnitude(13.459, MagnitudeBand.H),new Magnitude(18.186, MagnitudeBand.B),new Magnitude(15.455, MagnitudeBand.R),new Magnitude(14.221, MagnitudeBand.I))),
    target("042-030696", Coordinates(RightAscension.fromAngle(Angle.parseHMS("5:35:36.179").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-69:16:09.21").getOrElse(Angle.zero)).getOrElse(Declination.zero)), List(new Magnitude(14.673, MagnitudeBand.J),new Magnitude(14.834, MagnitudeBand.K),new Magnitude(14.539, MagnitudeBand.H),new Magnitude(17.103, MagnitudeBand.B),new Magnitude(14.977, MagnitudeBand.R),new Magnitude(14.403, MagnitudeBand.I))),
    target("042-030698", Coordinates(RightAscension.fromAngle(Angle.parseHMS("5:35:36.405").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-69:16:24.08").getOrElse(Angle.zero)).getOrElse(Declination.zero)), List(new Magnitude(14.428, MagnitudeBand.J),new Magnitude(13.691, MagnitudeBand.K),new Magnitude(14.29, MagnitudeBand.H),new Magnitude(18.086, MagnitudeBand.B),new Magnitude(14.249, MagnitudeBand.R),new Magnitude(14.632, MagnitudeBand.I))),
    target("042-030702", Coordinates(RightAscension.fromAngle(Angle.parseHMS("5:35:38.009").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-69:15:47.48").getOrElse(Angle.zero)).getOrElse(Declination.zero)), List(new Magnitude(16.205, MagnitudeBand.J),new Magnitude(15.558, MagnitudeBand.K),new Magnitude(15.887, MagnitudeBand.H),new Magnitude(18.36, MagnitudeBand.B),new Magnitude(15.88, MagnitudeBand.R),new Magnitude(15.425, MagnitudeBand.I))))

  val allTargets = (tipTiltTargets ::: flexureTargets).map(t => t.name -> new SPTarget(t.coordinates.ra.toAngle.toDegrees, t.coordinates.dec.toDegrees)).toMap
  val guideProbeTargetScn = guideProbeTargetsGenerator(allTargets)

  val targetsToMascot = List("042-030622",
        "042-030635",
        "042-030696",
        "042-030698",
        "042-030696",
        "042-030698",
        "042-030622",
        "042-030635",
        "042-030696",
        "042-030698",
        "042-030696",
        "042-030698",
        "042-030622",
        "042-030635",
        "042-030696",
        "042-030698",
        "042-030696",
        "042-030698",
        "042-030622",
        "042-030635",
        "042-030696",
        "042-030698",
        "042-030696",
        "042-030698").flatMap(n => (tipTiltTargets ::: flexureTargets).find(_.name == n))

  val averageStrehl = List(
    0.05231804627006496,
    0.052176793623035525,
    0.05210217247669559,
    0.05191277077105155,
    0.045517860011822175,
    0.04547186789961546,
    0.04528574329580283,
    0.0451267644229083,
    0.04254013733574182,
    0.034604389983833986,
    0.033466228162697356,
    0.032270649114148925,
    0.025377719063930253,
    0.02439946963915216
  )

  val mascotStars = List(
    Star(target("042-030698",Coordinates(RightAscension.fromAngle(Angle.fromDegrees(83.90168749999998)),Declination.fromAngle(Angle.fromDegrees(290.72664444444445)).getOrElse(Declination.zero)), List(new Magnitude(14.428,MagnitudeBand.J), new Magnitude(13.691,MagnitudeBand.K), new Magnitude(14.29,MagnitudeBand.H), new Magnitude(18.086,MagnitudeBand.B), new Magnitude(14.249,MagnitudeBand.R), new Magnitude(14.632,MagnitudeBand.I))),-44.51300737797379,-13.009999999962929,2.0,14.249),
    Star(target("042-030622",Coordinates(RightAscension.fromAngle(Angle.fromDegrees(83.84953333333334)),Declination.fromAngle(Angle.fromDegrees(290.72827777777775)).getOrElse(Declination.zero)), List(new Magnitude(15.787,MagnitudeBand.J), new Magnitude(15.156,MagnitudeBand.K), new Magnitude(15.114,MagnitudeBand.H), new Magnitude(14.273,MagnitudeBand.R))),21.936983034367834,-7.130000000074688,2.0,14.273),
    Star(target("042-030635",Coordinates(RightAscension.fromAngle(Angle.fromDegrees(83.85415416666666)),Declination.fromAngle(Angle.fromDegrees(290.73200833333334)).getOrElse(Declination.zero)), List(new Magnitude(15.71,MagnitudeBand.J), new Magnitude(14.602,MagnitudeBand.K), new Magnitude(14.589,MagnitudeBand.H), new Magnitude(14.721,MagnitudeBand.R))),16.052010976423862,6.300000000055661,2.0,14.721),
    Star(target("042-030696",Coordinates(RightAscension.fromAngle(Angle.fromDegrees(83.9007458333333)),Declination.fromAngle(Angle.fromDegrees(290.730775)).getOrElse(Declination.zero)), List(new Magnitude(14.673,MagnitudeBand.J), new Magnitude(14.834,MagnitudeBand.K), new Magnitude(14.539,MagnitudeBand.H), new Magnitude(17.103,MagnitudeBand.B), new Magnitude(14.977,MagnitudeBand.R), new Magnitude(14.403,MagnitudeBand.I))),-43.32150491056957,1.8600000000105865,2.0,14.977)
  )

  val expectedGuideStarsScn1 = List(
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, new GemsStrehl(0.052317982350356916, 0.01133819573512042, 0.05000345834757953, 0.053210007576294995), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, new GemsStrehl(0.05217670305093062, 0.011727993348960632, 0.05018441929854678, 0.05318192003906985), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, new GemsStrehl(0.05210255186378177, 0.013284466960358938, 0.04953389414118685, 0.053180724570543056), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, new GemsStrehl(0.05191301490041002, 0.014460123562554062, 0.048673600549975125, 0.053145879759821116), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, new GemsStrehl(0.052317982350356916, 0.01133819573512042, 0.05000345834757953, 0.053210007576294995), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, new GemsStrehl(0.05217670305093062, 0.011727993348960632, 0.05018441929854678, 0.05318192003906985), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, new GemsStrehl(0.05210255186378177, 0.013284466960358938, 0.04953389414118685, 0.053180724570543056), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, new GemsStrehl(0.05191301490041002, 0.014460123562554062, 0.048673600549975125, 0.053145879759821116), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, new GemsStrehl(0.05217670305093062, 0.011727993348960632, 0.05018441929854678, 0.05318192003906985), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, new GemsStrehl(0.05210255186378177, 0.013284466960358938, 0.04953389414118685, 0.053180724570543056), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, new GemsStrehl(0.05191301490041002, 0.014460123562554062, 0.048673600549975125, 0.053145879759821116), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, new GemsStrehl(0.04551798657885312, 0.0967617404250638, 0.031356279966573336, 0.053126232489091034), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, new GemsStrehl(0.04547182991668181, 0.1006333979349688, 0.029325604745680082, 0.05314651052137516), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, new GemsStrehl(0.04528561178513762, 0.09673089315380243, 0.03168645463443433, 0.05302541114557695), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030635"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, new GemsStrehl(0.04512680041654095, 0.10579093992197398, 0.030453484756441402, 0.05313326754928353), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, new GemsStrehl(0.04551798657885312, 0.0967617404250638, 0.031356279966573336, 0.053126232489091034), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, new GemsStrehl(0.04547182991668181, 0.1006333979349688, 0.029325604745680082, 0.05314651052137516), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, new GemsStrehl(0.04528561178513762, 0.09673089315380243, 0.03168645463443433, 0.05302541114557695), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030635"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, new GemsStrehl(0.04512680041654095, 0.10579093992197398, 0.030453484756441402, 0.05313326754928353), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, new GemsStrehl(0.04551798657885312, 0.0967617404250638, 0.031356279966573336, 0.053126232489091034), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, new GemsStrehl(0.04547182991668181, 0.1006333979349688, 0.029325604745680082, 0.05314651052137516), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, new GemsStrehl(0.04528561178513762, 0.09673089315380243, 0.03168645463443433, 0.05302541114557695), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030635"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, new GemsStrehl(0.04512680041654095, 0.10579093992197398, 0.030453484756441402, 0.05313326754928353), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, new GemsStrehl(0.04254016820789503, 0.14300055452840363, 0.020693214106825192, 0.05306951564558715), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, new GemsStrehl(0.04254016820789503, 0.14300055452840363, 0.020693214106825192, 0.05306951564558715), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, new GemsStrehl(0.04254016820789503, 0.14300055452840363, 0.020693214106825192, 0.05306951564558715), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, new GemsStrehl(0.034605259856144176, 0.17319994484868476, 0.01884152333550868, 0.05279871540715392), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, new GemsStrehl(0.034605259856144176, 0.17319994484868476, 0.01884152333550868, 0.05279871540715392), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, new GemsStrehl(0.034605259856144176, 0.17319994484868476, 0.01884152333550868, 0.05279871540715392), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, new GemsStrehl(0.03346630831985282, 0.1622541013761052, 0.014024971439891017, 0.0527099353266302), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030635"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, new GemsStrehl(0.03346630831985282, 0.1622541013761052, 0.014024971439891017, 0.0527099353266302), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030635"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, new GemsStrehl(0.03346630831985282, 0.1622541013761052, 0.014024971439891017, 0.0527099353266302), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030635"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, new GemsStrehl(0.03227148097305104, 0.17642585871117816, 0.012717491511504754, 0.053043819130914285), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, new GemsStrehl(0.03227148097305104, 0.17642585871117816, 0.012717491511504754, 0.053043819130914285), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, new GemsStrehl(0.03227148097305104, 0.17642585871117816, 0.012717491511504754, 0.053043819130914285), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, new GemsStrehl(0.025378440351829658, 0.18185643770585588, 0.009787843566658863, 0.05241989239503134), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030696"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, new GemsStrehl(0.025378440351829658, 0.18185643770585588, 0.009787843566658863, 0.05241989239503134), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030696"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, new GemsStrehl(0.025378440351829658, 0.18185643770585588, 0.009787843566658863, 0.05241989239503134), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030696"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, new GemsStrehl(0.024398134308257116, 0.18355801167992475, 0.008637215957687594, 0.05248960501173948), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, new GemsStrehl(0.024398134308257116, 0.18355801167992475, 0.008637215957687594, 0.05248960501173948), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, new GemsStrehl(0.024398134308257116, 0.18355801167992475, 0.008637215957687594, 0.05248960501173948), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, new GemsStrehl(0.052317982350356916, 0.01133819573512042, 0.05000345834757953, 0.053210007576294995), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, new GemsStrehl(0.05217670305093062, 0.011727993348960632, 0.05018441929854678, 0.05318192003906985), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, new GemsStrehl(0.05210255186378177, 0.013284466960358938, 0.04953389414118685, 0.053180724570543056), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, new GemsStrehl(0.05191301490041002, 0.014460123562554062, 0.048673600549975125, 0.053145879759821116), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, new GemsStrehl(0.04551798657885312, 0.0967617404250638, 0.031356279966573336, 0.053126232489091034), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, new GemsStrehl(0.04547182991668181, 0.1006333979349688, 0.029325604745680082, 0.05314651052137516), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, new GemsStrehl(0.04528561178513762, 0.09673089315380243, 0.03168645463443433, 0.05302541114557695), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030635"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, new GemsStrehl(0.04512680041654095, 0.10579093992197398, 0.030453484756441402, 0.05313326754928353), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, new GemsStrehl(0.04254016820789503, 0.14300055452840363, 0.020693214106825192, 0.05306951564558715), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, new GemsStrehl(0.034605259856144176, 0.17319994484868476, 0.01884152333550868, 0.05279871540715392), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, new GemsStrehl(0.03346630831985282, 0.1622541013761052, 0.014024971439891017, 0.0527099353266302), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030635"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, new GemsStrehl(0.03227148097305104, 0.17642585871117816, 0.012717491511504754, 0.053043819130914285), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, new GemsStrehl(0.025378440351829658, 0.18185643770585588, 0.009787843566658863, 0.05241989239503134), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030696"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, new GemsStrehl(0.024398134308257116, 0.18355801167992475, 0.008637215957687594, 0.05248960501173948), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)))
}

