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

  val allTargets = (tipTiltTargets ::: flexureTargets).map(t => t.name -> {
    val spTarget = new SPTarget(t.coordinates.ra.toAngle.toDegrees, t.coordinates.dec.toDegrees)
    spTarget.setName(t.name)
    spTarget
  }).toMap

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
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, GemsStrehl(0.05231804627006496,0.011337133122578858,0.05000389997714641,0.0532100083852756), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, GemsStrehl(0.052176793623035525,0.011726866031501528,0.05018454623964478,0.05318191692306813), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, GemsStrehl(0.05210217247669559,0.013290367896092599,0.049532412499442555,0.05318072627159335), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, GemsStrehl(0.05191277077105155,0.014462889647845374,0.048672787192984776,0.0531458791224915), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, GemsStrehl(0.05231804627006496,0.011337133122578858,0.05000389997714641,0.0532100083852756), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, GemsStrehl(0.052176793623035525,0.011726866031501528,0.05018454623964478,0.05318191692306813), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, GemsStrehl(0.05210217247669559,0.013290367896092599,0.049532412499442555,0.05318072627159335), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, GemsStrehl(0.05191277077105155,0.014462889647845374,0.048672787192984776,0.0531458791224915), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, GemsStrehl(0.05231804627006496,0.011337133122578858,0.05000389997714641,0.0532100083852756), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, GemsStrehl(0.052176793623035525,0.011726866031501528,0.05018454623964478,0.05318191692306813), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, GemsStrehl(0.05210217247669559,0.013290367896092599,0.049532412499442555,0.05318072627159335), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, GemsStrehl(0.05191277077105155,0.014462889647845374,0.048672787192984776,0.0531458791224915), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, GemsStrehl(0.045517860011822175,0.096765492555885,0.03135552974235063,0.053126152912588744), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, GemsStrehl(0.04547186789961546,0.10063002721182644,0.029326824856075295,0.05314651412884718), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, GemsStrehl(0.04528574329580283,0.09672829555972023,0.03168635327939009,0.05302534707223639), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030635"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, GemsStrehl(0.0451267644229083,0.10579153648406678,0.03045491940412582,0.0531332706339168), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, GemsStrehl(0.045517860011822175,0.096765492555885,0.03135552974235063,0.053126152912588744), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, GemsStrehl(0.04547186789961546,0.10063002721182644,0.029326824856075295,0.05314651412884718), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, GemsStrehl(0.04528574329580283,0.09672829555972023,0.03168635327939009,0.05302534707223639), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030635"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, GemsStrehl(0.0451267644229083,0.10579153648406678,0.03045491940412582,0.0531332706339168), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, GemsStrehl(0.045517860011822175,0.096765492555885,0.03135552974235063,0.053126152912588744), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, GemsStrehl(0.04547186789961546,0.10063002721182644,0.029326824856075295,0.05314651412884718), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, GemsStrehl(0.04528574329580283,0.09672829555972023,0.03168635327939009,0.05302534707223639), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030635"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, GemsStrehl(0.0451267644229083,0.10579153648406678,0.03045491940412582,0.0531332706339168), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, GemsStrehl(0.04254013733574182,0.143003118138703,0.02069256520462188,0.0530695278733036), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, GemsStrehl(0.04254013733574182,0.143003118138703,0.02069256520462188,0.0530695278733036), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, GemsStrehl(0.04254013733574182,0.143003118138703,0.02069256520462188,0.0530695278733036), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, GemsStrehl(0.034604389983833986,0.17319612048434452,0.018843402900172633,0.05279871751855406), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, GemsStrehl(0.034604389983833986,0.17319612048434452,0.018843402900172633,0.05279871751855406), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, GemsStrehl(0.034604389983833986,0.17319612048434452,0.018843402900172633,0.05279871751855406), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, GemsStrehl(0.033466228162697356,0.16225493607214872,0.014024980573824872,0.052709824878354926), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030635"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, GemsStrehl(0.033466228162697356,0.16225493607214872,0.014024980573824872,0.052709824878354926), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030635"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, GemsStrehl(0.033466228162697356,0.16225493607214872,0.014024980573824872,0.052709824878354926), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030635"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, GemsStrehl(0.032270649114148925,0.17643312967807567,0.012716542037639974,0.05304395967257655), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, GemsStrehl(0.032270649114148925,0.17643312967807567,0.012716542037639974,0.05304395967257655), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, GemsStrehl(0.032270649114148925,0.17643312967807567,0.012716542037639974,0.05304395967257655), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, GemsStrehl(0.025377719063930253,0.1818532595907574,0.009787976873492513,0.05241935271886613), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030696"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, GemsStrehl(0.025377719063930253,0.1818532595907574,0.009787976873492513,0.05241935271886613), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030696"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, GemsStrehl(0.025377719063930253,0.1818532595907574,0.009787976873492513,0.05241935271886613), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030696"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(0.0), CanopusWfs.Group.instance, GemsStrehl(0.02439946963915216,0.18356137564541936,0.008637870354262105,0.05249034855434164), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(180.0), CanopusWfs.Group.instance, GemsStrehl(0.02439946963915216,0.18356137564541936,0.008637870354262105,0.05249034855434164), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw3, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(270.0), CanopusWfs.Group.instance, GemsStrehl(0.02439946963915216,0.18356137564541936,0.008637870354262105,0.05249034855434164), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw2, "042-030698")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, GemsStrehl(0.05231804627006496,0.011337133122578858,0.05000389997714641,0.0532100083852756), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, GemsStrehl(0.052176793623035525,0.011726866031501528,0.05018454623964478,0.05318191692306813), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, GemsStrehl(0.05210217247669559,0.013290367896092599,0.049532412499442555,0.05318072627159335), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, GemsStrehl(0.05191277077105155,0.014462889647845374,0.048672787192984776,0.0531458791224915), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs1, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, GemsStrehl(0.045517860011822175,0.096765492555885,0.03135552974235063,0.053126152912588744), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, GemsStrehl(0.04547186789961546,0.10063002721182644,0.029326824856075295,0.05314651412884718), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, GemsStrehl(0.04528574329580283,0.09672829555972023,0.03168635327939009,0.05302534707223639), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030635"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, GemsStrehl(0.0451267644229083,0.10579153648406678,0.03045491940412582,0.0531332706339168), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030622"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, GemsStrehl(0.04254013733574182,0.143003118138703,0.02069256520462188,0.0530695278733036), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030635"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, GemsStrehl(0.034604389983833986,0.17319612048434452,0.018843402900172633,0.05279871751855406), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs2, "042-030696"), guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, GemsStrehl(0.033466228162697356,0.16225493607214872,0.014024980573824872,0.052709824878354926), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030635"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, GemsStrehl(0.032270649114148925,0.17643312967807567,0.012716542037639974,0.05304395967257655), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030622"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, GemsStrehl(0.025377719063930253,0.1818532595907574,0.009787976873492513,0.05241935271886613), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030696"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)),
    GemsGuideStars(Angle.fromDegrees(90.0), CanopusWfs.Group.instance, GemsStrehl(0.02439946963915216,0.18356137564541936,0.008637870354262105,0.05249034855434164), GuideGroup.create(JNone.instance[String], List(guideProbeTargetScn(CanopusWfs.cwfs3, "042-030698"), guideProbeTargetScn(GsaoiOdgw.odgw1, "042-030622")).asImList)))
}

