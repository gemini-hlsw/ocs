package edu.gemini.ags.impl

import java.security.SecureRandom

import edu.gemini.ags.gems.mascot.Star
import edu.gemini.ags.gems.{GemsCatalogSearchCriterion, GemsCatalogResults, GemsCatalogSearchResults, GemsGuideStars}
import edu.gemini.spModel.core.{Site, Angle, MagnitudeBand}
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.gemini.gems.Gems
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{Conditions, SkyBackground, WaterVapor}
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.{TargetEnvironment, GuideProbeTargets}
import edu.gemini.spModel.target.system.CoordinateParam
import jsky.coords.WorldCoords
import edu.gemini.shared.util.immutable.{Some => JSome}
import edu.gemini.spModel.core.AlmostEqual._

import scala.collection.JavaConverters._
import edu.gemini.pot.ModelConverters._

import scalaz._
import Scalaz._

object UCAC3Regression {
  // Convert targets from having band R to r', R or UC
  def replaceRBands(targets: List[SiderealTarget]): List[SiderealTarget] = {
    targets.zipWithIndex.collect {
      case (t, i) if i < targets.size / 3=>
        val mags = t.magnitudes.collect {
          case m if m.band === MagnitudeBand.R => m.copy(band = MagnitudeBand._r)
          case m                               => m
        }
        t.copy(magnitudes = mags)
      case (t, i) if i < targets.size * 2 / 3 && i >= targets.size / 3=>
        val mags = t.magnitudes.collect {
          case m if m.band === MagnitudeBand.R => m.copy(band = MagnitudeBand.UC)
          case m                               => m
        }
        t.copy(magnitudes = mags)
      case (t, _) => t
    }
  }

}
trait UCAC3Regression {

  type GuideProbeTargetsFinder = (GuideProbe, String) => GuideProbeTargets

  private val defaultTarget = new SPTarget()

  private val random = new SecureRandom()

  def runAnalysis(ra: String, dec: String, conditions: Conditions, tipTiltCriterion: GemsCatalogSearchCriterion, flexureCriterion: GemsCatalogSearchCriterion, tipTiltTargets: List[SiderealTarget], flexureTargets: List[SiderealTarget], expectedGuideStars: List[GemsGuideStars]):Boolean = {
    val results = List(GemsCatalogSearchResults(tipTiltCriterion, tipTiltTargets), GemsCatalogSearchResults(flexureCriterion, flexureTargets))
    val posAngles = Set(Angle.zero, Angle.fromDegrees(180), Angle.fromDegrees(270), Angle.fromDegrees(90))

    val conditions = SPSiteQuality.Conditions.NOMINAL.wv(WaterVapor.ANY).sb(SkyBackground.ANY)
    val coords = new WorldCoords(ra, dec)
    val baseTarget = new SPTarget(coords.getRaDeg, coords.getDecDeg)
    val env = TargetEnvironment.create(baseTarget)
    val offsets = new java.util.HashSet[edu.gemini.skycalc.Offset]

    val obsContext = ObsContext.create(env, new Gsaoi, new JSome(Site.GS), conditions, offsets, new Gems).withPositionAngle(Angle.zero.toOldModel)
    val gemsResults = new GemsCatalogResults().analyze(obsContext, posAngles.asJava, results.asJava, null)
    areGuideStarListsEqual(expectedGuideStars, gemsResults.asScala.toList)
  }

  // Build a GuideProbeTargets class out of a list of targets
  def guideProbeTargetsGenerator(targets: Map[String, SPTarget]):GuideProbeTargetsFinder = (guider: GuideProbe, name: String) => {
    GuideProbeTargets.create(guider, targets.getOrElse(name, defaultTarget))
  }

  // Converts targets with R magnitude to r', R or UC discarding duplicates
  // It is important to discard duplicates or the same target could be assigned to different bands
  // breaking the tests
  private def assignRLikeMagnitudes(targets: List[SiderealTarget]):Map[String, SiderealTarget] =
    targets.distinct.zipWithIndex.collect {
        case (t, i) if i % 3 == 0 =>
          val mags = t.magnitudes.collect {
            case m if m.band === MagnitudeBand.R => m.copy(band = MagnitudeBand._r)
            case m                               => m
          }
          t.copy(magnitudes = mags)
        case (t, i) if i % 3 == 1 =>
          val mags = t.magnitudes.collect {
            case m if m.band === MagnitudeBand.R => m.copy(band = MagnitudeBand.UC)
            case m                               => m
          }
          t.copy(magnitudes = mags)
        case (t, _) => t
    }.map(t => t.name -> t).toMap

  private def translateTargets(targets: List[SiderealTarget], targetsMap: Map[String, SiderealTarget]) = targets.map { t =>
    targetsMap.getOrElse(t.name, t)
  }

  // Convert targets from having band R to r', R or UC
  def replaceRBands(targets: List[SiderealTarget]): List[SiderealTarget] = {
    translateTargets(targets, assignRLikeMagnitudes(targets))
  }

  // Convert tip/tilt and flexure targets from having band R to r', R or UC
  def replaceRBands(tipTiltTargets: List[SiderealTarget], flexureTargets: List[SiderealTarget]): (List[SiderealTarget], List[SiderealTarget]) = {
    // Combine tha targets to assign them consistently
    val targets = tipTiltTargets ::: flexureTargets
    val targetsMap = assignRLikeMagnitudes(targets)
    (translateTargets(tipTiltTargets, targetsMap), translateTargets(flexureTargets, targetsMap))
  }

  // Compare two lists of guide stars, equal comparison doesn't work directly
  def areGuideStarListsEqual(expectedGuideStars: List[GemsGuideStars], actualGuideStars: List[GemsGuideStars]): Boolean = {
    // Same size
    val equalSize = actualGuideStars.size == expectedGuideStars.size
    if (equalSize) {
      // Compare each guide star
      expectedGuideStars.zip(actualGuideStars).map { case (expected, actual) =>
        val paEqual = expected.getPa == actual.getPa
        val tipTiltGroupEqual = expected.getTiptiltGroup == actual.getTiptiltGroup
        val expectedGuideGroup = expected.getGuideGroup.getAll.asScala
        val actualGuideGroup = actual.getGuideGroup.getAll.asScala

        val guideGroupSizeEqual = expectedGuideGroup.size == actualGuideGroup.size
        val guideGroupEqual = if (guideGroupSizeEqual) {
          val equalGroup = expectedGuideGroup.zip(actualGuideGroup).map {
            case (egg, acc) =>
              val sameGuider = egg.getGuider == acc.getGuider
              val sameRa = egg.getPrimary.getValue.getTarget.getRa.getAs(CoordinateParam.Units.DEGREES) == acc.getPrimary.getValue.getTarget.getRa.getAs(CoordinateParam.Units.DEGREES)
              val sameDec = egg.getPrimary.getValue.getTarget.getDec.getAs(CoordinateParam.Units.DEGREES) == acc.getPrimary.getValue.getTarget.getDec.getAs(CoordinateParam.Units.DEGREES)
              sameGuider && sameRa && sameDec
          }
          val sameStrehl = math.abs(expected.getStrehl.getAvg - actual.getStrehl.getAvg) < 0.0001
          sameStrehl && equalGroup.forall(_ == true)
        } else {
          guideGroupSizeEqual
        }
        paEqual && tipTiltGroupEqual && guideGroupEqual
      }.forall(_ == true)
    } else {
      equalSize
    }
  }

  // Compare two lists of guide stars, equal comparison doesn't work directly
  def areAsterismStarsTheSame(actualStars: List[Star], expectedStars: List[Star]):Boolean = {
    val equalSize = actualStars.size == expectedStars.size
    if (equalSize) {
      actualStars.zip(expectedStars).map { case (r, e) =>
        val sameName = r.target.name == e.target.name
        val sameCoordinates = r.target.coordinates ~= e.target.coordinates
        val sameMagnitudes = r.target.magnitudes == e.target.magnitudes
        val sameM = math.abs(r.m - e.m) < 0.001
        val sameR = math.abs(r.r - e.r) < 0.001
        val sameX = math.abs(r.x - e.x) < 0.001
        val sameY = math.abs(r.y - e.y) < 0.001
        sameName && sameCoordinates && sameMagnitudes && sameM && sameX && sameY && sameR
      }.forall(_ == true)
    } else {
      equalSize
    }
  }
}
