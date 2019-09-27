package edu.gemini.ags.gems

import java.util.logging.Logger

import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.ags.gems.mascot.{MascotCat, MascotProgress, Strehl}
import edu.gemini.ags.gems.mascot.MascotCat.StrehlResults
import edu.gemini.catalog.api.MagnitudeConstraints
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.gems.CanopusWfs
import edu.gemini.spModel.gemini.gsaoi.{GsaoiOdgw, Gsaoi}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gems.GemsGuideProbeGroup
import edu.gemini.spModel.gems.GemsGuideStarType.tiptilt
import edu.gemini.spModel.guide.{GuideProbe, PatrolField, ValidatableGuideProbe}
import edu.gemini.spModel.guide.GuideStarValidation.VALID
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe.pwfs1
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.shared.util.immutable.{None => JNone, Option => JOption}
import edu.gemini.pot.ModelConverters._
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.{GuideGroup, GuideProbeTargets}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scalaz._
import Scalaz._


object GemsResultsAnalyzer {
  val instance = this
  val Log = Logger.getLogger(GemsResultsAnalyzer.getClass.getSimpleName)

  // Java interfacing methods
  // ========================
  def getStrehlFactor(obsContext: JOption[ObsContext]): Double =
    strehlFactor(obsContext.asScalaOpt)

  // Used by Mascot to discard pairs and triplets where one star differs in
  // magnitude from another by more than n.  The CWFS are guide windows on
  // the same detector so they cannot tolerate widely differing magnitudes.
  private def asterismPreFilter(ts: List[SiderealTarget]): Boolean =
    CanopusWfs.Group.instance.asterismPreFilter(ts.asImList)

  /**
   * Analyze the given position angles and search results to select tip tilt
   * asterisms.
   *
   * @param obsContext observation context
   * @param posAngles position angles to try
   * @param catalogSearch results of catalog search
   * @param mascotProgress used to report progress of Mascot Strehl calculations
   *                       and interrupt if requested
   * @return a sorted List of GemsGuideStars
   */
  def analyze(
    obsContext:     ObsContext,
    posAngles:      Set[Angle],
    candidates:     List[SiderealTarget],
    mascotProgress: Option[MascotProgress]
  ): List[GemsGuideStars] =
    analyzeEither(obsContext, posAngles, candidates, -\/(mascotProgress))

  /**
   * Analyze the given position angles and search results to select tip tilt
   * asterisms.  This version simply calls `analyze` converting to/from Scala
   * for convenient access from Java clients.
   *
   * @param obsContext observation context
   * @param posAngles position angles to try
   * @param catalogSearch results of catalog search
   * @param mascotProgress used to report progress of Mascot Strehl calculations
   *                       and interrupt if requested
   * @return a sorted List of GemsGuideStars
   */
  def analyzeForJava(
    obsContext:     ObsContext,
    posAngles:      java.util.Set[Angle],
    candidates:     java.util.List[SiderealTarget],
    mascotProgress: JOption[MascotProgress]
  ): java.util.List[GemsGuideStars] =
    analyze(
      obsContext,
      posAngles.asScala.toSet,
      candidates.asScala.toList,
      mascotProgress.asScalaOpt
    ).asJava

  /**
   * Analyze the given position angles and search results to select tip tilt
   * asterisms. This version allows the progress argument to stop the strehl
   * algorithm when a "good enough" asterism has been found and use the results
   * up until that point. Hint: this is useful for Phase 1 "estimations" of
   * success probability where it is not important to find the absolute best
   * asterism.
   *
   * @param obsContext observation context
   * @param posAngles position angles to try
   * @param catalogSearch results of catalog search
   * @param shouldContinue function to determine if the search should continue:
   *                       early termination possible if sufficient asterism
   *                       found
   * @return a sorted List of GemsGuideStars
   */
  def analyzeGoodEnough(
    obsContext:     ObsContext,
    posAngles:      Set[Angle],
    candidates:     List[SiderealTarget],
    shouldContinue: Strehl => Boolean
  ): List[GemsGuideStars] =
    analyzeEither(obsContext, posAngles, candidates, \/-(shouldContinue))

  /**
   * Combines the CWFS candidates for a set of position angles with the best
   * SFS for those angles (if any).  If there is no SFS star for a given set
   * of candidates, the candidates are removed.
   */
  private def combine(
    cwfs: Map[Set[Angle], List[SiderealTarget]],
    sfs:  Map[Set[Angle], SiderealTarget]
  ): Map[Set[Angle], (List[SiderealTarget], SiderealTarget)] =
    cwfs.collect {
      case (as, cs) if sfs.isDefinedAt(as) => (as, (cs, sfs(as)))
    }


  // Performs the actual analysis for the `analyze*` methods, which only differ
  // in how they interact with the world.
  private def analyzeEither(
    obsContext:   ObsContext,
    posAngles:    Set[Angle],
    candidates:   List[SiderealTarget],
    mascotOption: Option[MascotProgress] \/ (Strehl => Boolean)
  ): List[GemsGuideStars] = {

    val gemsCandidates = GemsCandidates.groupAndValidate(obsContext, posAngles, candidates)
    val factor         = strehlFactor(new Some(obsContext))

    // tell the UI to update (ugh)
    mascotOption.swap.toOption.flatten.foreach(_.setProgressTitle(s"Finding asterisms for CWFS"))

    gemsCandidates.flatMap { gc =>
      CanopusWfs
        .centerOfProbeRange(obsContext.withPositionAngle(gc.posAngle))
        .asScalaOpt.toList
        .map(_.toNewModel)
        .flatMap { b =>
          mascot(mascotOption)(
            gc.cwfsCandidates,
            b.ra.toAngle.toDegrees,
            b.dec.toDegrees,
            factor
          ).strehlList.map(toGemsGuideStars(_, gc.slowFocusSensor, gc.posAngle))
        }
    }.sorted

  }

  // Sorry. There are two mascot variants that are identical except for a single
  // argument.
  private def mascot(
    mascotVariant: Option[MascotProgress] \/ (Strehl => Boolean)
  ): (List[SiderealTarget], Double, Double, Double) => StrehlResults =
    mascotVariant.fold(
      p => MascotCat.findBestAsterismInTargetsList(_, _, _, RBandsList, _, p, asterismPreFilter(_)),
      c => MascotCat.findBestAsterismInTargetsList(_, _, _, RBandsList,_ , c, asterismPreFilter(_))
    )

  private def toGemsGuideStars(strehl: Strehl, sfs: SiderealTarget, posAngle: Angle): GemsGuideStars = {

    // All the CWFS guide windows are equivalent.  Assign them in any order.
    val guideProbeTargets =
      GuideProbeTargets.create(pwfs1, toSPTarget(sfs)) ::
        targetListFromStrehl(strehl).zip(CanopusWfs.values).map { case (t, w) =>
          GuideProbeTargets.create(w, toSPTarget(t))
        }

    GemsGuideStars(
      posAngle,
      CanopusWfs.Group.instance,
      GemsStrehl(strehl.avgstrehl, strehl.rmsstrehl, strehl.minstrehl, strehl.maxstrehl),
      GuideGroup.create(JNone.instance[String], guideProbeTargets.asImList)
    )

  }

  // Returns the stars in the given asterism sorted by R mag, brightest first.
  private def targetListFromStrehl(strehl: Strehl): List[SiderealTarget] =
    sortTargetsByBrightness(strehl.stars.map(_.target))

  // REL-426: Multiply the average, min, and max Strehl values reported by Mascot by the following scale
  // factors depending on the filter used in the instrument component of the observation (GSAOI, F2 in the future):
  //   0.2 in J,
  //   0.3 in H,
  //   0.4 in K
  // See OT-22 for the mapping of GSAOI filters to JHK equivalent
  //
  // Update for REL-1321:
  // Multiply the average, min, and max Strehl values reported by Mascot by the following scale factors depending
  // on the filter used in the instrument component of the observation (GSAOI, F2 and GMOS-S in the future) and
  // the conditions:
  //  J: IQ20=0.12 IQ70=0.06 IQ85=0.024 IQAny=0.01
  //  H: IQ20=0.18 IQ70=0.14 IQ85=0.06 IQAny=0.01
  //  K: IQ20=0.35 IQ70=0.18 IQ85=0.12 IQAny=0.01
  private def strehlFactor(obsContext: Option[ObsContext]): Double = {
    obsContext.map(o => (o, o.getInstrument)).collect {
      case (ctx, gsaoi: Gsaoi) =>
        val band = gsaoi.getFilter.getCatalogBand.asScalaOpt
        val iq = Option(ctx.getConditions).map(_.iq)
        (band, iq) match {
          case (Some(SingleBand(MagnitudeBand.J)), Some(SPSiteQuality.ImageQuality.PERCENT_20)) => 0.12
          case (Some(SingleBand(MagnitudeBand.J)), Some(SPSiteQuality.ImageQuality.PERCENT_70)) => 0.06
          case (Some(SingleBand(MagnitudeBand.J)), Some(SPSiteQuality.ImageQuality.PERCENT_85)) => 0.024
          case (Some(SingleBand(MagnitudeBand.J)), None)                                        => 0.01
          case (Some(SingleBand(MagnitudeBand.H)), Some(SPSiteQuality.ImageQuality.PERCENT_20)) => 0.18
          case (Some(SingleBand(MagnitudeBand.H)), Some(SPSiteQuality.ImageQuality.PERCENT_70)) => 0.14
          case (Some(SingleBand(MagnitudeBand.H)), Some(SPSiteQuality.ImageQuality.PERCENT_85)) => 0.06
          case (Some(SingleBand(MagnitudeBand.H)), None)                                        => 0.01
          case (Some(SingleBand(MagnitudeBand.K)), Some(SPSiteQuality.ImageQuality.PERCENT_20)) => 0.35
          case (Some(SingleBand(MagnitudeBand.K)), Some(SPSiteQuality.ImageQuality.PERCENT_70)) => 0.18
          case (Some(SingleBand(MagnitudeBand.K)), Some(SPSiteQuality.ImageQuality.PERCENT_85)) => 0.12
          case (Some(SingleBand(MagnitudeBand.K)), None)                                        => 0.01
          case _                                                                                => 0.3
        }
    }.getOrElse(0.3)
  }

  /**
   * Sorts the targets list, putting the brightest stars first and returns the sorted array.
   */
  protected [ags] def sortTargetsByBrightness(targetsList: List[SiderealTarget]): List[SiderealTarget] =
    targetsList.sortBy(RBandsList.extract)(Magnitude.MagnitudeOptionValueOrdering)

  def toSPTarget(siderealTarget: SiderealTarget): SPTarget =
    new SPTarget(siderealTarget)

}


