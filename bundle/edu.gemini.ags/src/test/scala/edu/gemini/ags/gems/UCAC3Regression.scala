package edu.gemini.ags.gems

import edu.gemini.ags.gems.mascot.Star
import edu.gemini.pot.ModelConverters._
import edu.gemini.shared.util.immutable.{Some => JSome, None => JNone }
import edu.gemini.spModel.core.AlmostEqual._
import edu.gemini.spModel.core.SiderealTarget
import edu.gemini.spModel.core.{Angle, MagnitudeBand, Site}
import edu.gemini.spModel.gemini.gems.Gems
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{Conditions, SkyBackground, WaterVapor}
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.{GuideProbeTargets, TargetEnvironment}
import edu.gemini.shared.util.immutable.{ None => JNone, Option => JOption }
import edu.gemini.shared.util.immutable.ScalaConverters._

import jsky.coords.WorldCoords

import scala.collection.JavaConverters._
import scalaz.Scalaz._

/**
 * Some useful functions to run regressions tests on GemsCatalogSearchResult
 */
trait UCAC3Regression {

  type GuideProbeTargetsFinder = (GuideProbe, String) => GuideProbeTargets

  private val defaultTarget = new SPTarget()

  def runAnalysis(ra: String, dec: String, conditions: Conditions, tipTiltCriterion: GemsCatalogSearchCriterion, flexureCriterion: GemsCatalogSearchCriterion, tipTiltTargets: List[SiderealTarget], flexureTargets: List[SiderealTarget], expectedGuideStars: List[GemsGuideStars]):Boolean = {
    val results = List(GemsCatalogSearchResults(tipTiltCriterion, tipTiltTargets), GemsCatalogSearchResults(flexureCriterion, flexureTargets))
    val posAngles = Set(Angle.zero, Angle.fromDegrees(180), Angle.fromDegrees(270), Angle.fromDegrees(90))

    val conditions = SPSiteQuality.Conditions.NOMINAL.wv(WaterVapor.ANY).sb(SkyBackground.ANY)
    val coords = new WorldCoords(ra, dec)
    val baseTarget = new SPTarget(coords.getRaDeg, coords.getDecDeg)
    val env = TargetEnvironment.create(baseTarget)
    val offsets = new java.util.HashSet[edu.gemini.skycalc.Offset]

    val obsContext = ObsContext.create(env, new Gsaoi, new JSome(Site.GS), conditions, offsets, new Gems, JNone.instance()).withPositionAngle(Angle.zero)
    val gemsResults = GemsResultsAnalyzer.analyze(obsContext, posAngles.asJava, results.asJava, None)
    areGuideStarListsEqual(expectedGuideStars, gemsResults.asScala.toList)
  }

  // Build a GuideProbeTargets class out of a list of targets
  def guideProbeTargetsGenerator(targets: Map[String, SPTarget]):GuideProbeTargetsFinder = (guider: GuideProbe, name: String) => {
    val target = targets.getOrElse(name, defaultTarget)
    GuideProbeTargets.create(guider, target)
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
        val paEqual = expected.pa == actual.pa
        val tipTiltGroupEqual = expected.tiptiltGroup == actual.tiptiltGroup
        val expectedGuideGroup = expected.guideGroup.getAll.asScala
        val actualGuideGroup = actual.guideGroup.getAll.asScala

        val guideGroupSizeEqual = expectedGuideGroup.size == actualGuideGroup.size
        val guideGroupEqual = if (guideGroupSizeEqual) {
          val equalGroup = expectedGuideGroup.zip(actualGuideGroup).map {
            case (egg, acc) =>
              val sameGuider = egg.getGuider == acc.getGuider
              sameGuider && sameCoordinates(egg.getPrimary.getValue, acc.getPrimary.getValue)
          }
          val sameStrehl = math.abs(expected.strehl.avg - actual.strehl.avg) < 0.0001
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


  // Compare if two targets have the same RA/Dec ... they are assumed to be sidereal
  def sameCoordinates(a: SPTarget, b: SPTarget): Boolean = {
    val when = JNone.instance[java.lang.Long]
    same(a.getRaDegrees(when),  b.getRaDegrees(when)) &&
    same(a.getDecDegrees(when), b.getDecDegrees(when))
  }

  // True if both are defined and values are ==
  def same[A](a: JOption[A], b: JOption[A]): Boolean =
    a.asScalaOpt.flatMap(a0 => b.asScalaOpt.map(b0 => a0 == b0)).getOrElse(false)

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

object UCAC3Regression extends UCAC3Regression
