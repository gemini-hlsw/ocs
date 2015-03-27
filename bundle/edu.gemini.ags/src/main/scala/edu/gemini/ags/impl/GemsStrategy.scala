package edu.gemini.ags.impl

import edu.gemini.ags.api.{AgsAnalysis, AgsMagnitude, AgsStrategy}
import edu.gemini.ags.api.AgsStrategy.{Assignment, Estimate, Selection}
import edu.gemini.ags.gems._
import edu.gemini.ags.gems.mascot.{Strehl, MascotProgress}
import edu.gemini.catalog.api._
import edu.gemini.catalog.votable._
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.core.Target.SiderealTarget

import edu.gemini.spModel.ags.AgsStrategyKey.GemsKey
import edu.gemini.spModel.gemini.flamingos2.Flamingos2OiwfsGuideProbe
import edu.gemini.spModel.gemini.gems.{GemsInstrument, Canopus}
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw
import edu.gemini.spModel.gems.{GemsTipTiltMode, GemsGuideProbeGroup, GemsGuideStarType}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._
import scala.concurrent._
import edu.gemini.ags.api.AgsMagnitude.{MagnitudeCalc, MagnitudeTable}
import edu.gemini.spModel.guide.{GuideProbeGroup, GuideProbe}
import edu.gemini.spModel.core.{Angle, MagnitudeBand}

import scalaz._
import Scalaz._

trait GemsStrategy extends AgsStrategy {
  // By default use the remote backend but it can be overriden in tests
  private [impl] def backend:VoTableBackend

  override def key = GemsKey

  // Since the constraints are run in parallel, we need a way to identify them after
  // they are done, so we create IDs for each. This is a pretty nasty way to do things, but
  // since we cannot predict in what order the results will return, we need to be able
  // to pick them out somehow.
  private val CanopusTipTiltId = 0
  private val OdgwFlexureId    = 1

  // Catalog results with search keys to avoid having to recompute search key info on the fly.
  private case class CatalogResultWithKey(query: CatalogQuery, catalogResult: CatalogQueryResult, searchKey: GemsCatalogSearchKey)


  // Query the catalog for each constraint and compile a list of results with the necessary
  // information for GeMS.
  private def catalogResult(ctx: ObsContext, mt: MagnitudeTable): Future[List[CatalogResultWithKey]] = {
    // Maps for IDs to types needed by GeMS.
    def GuideStarTypeMap = Map[Int, GemsGuideStarType](
      CanopusTipTiltId -> GemsGuideStarType.tiptilt,
      OdgwFlexureId    -> GemsGuideStarType.flexure
    )

    // Maps from IDs to guide probe groups.
    def GuideProbeGroupMap = Map[Int, GemsGuideProbeGroup](
      CanopusTipTiltId -> Canopus.Wfs.Group.instance,
      OdgwFlexureId    -> GsaoiOdgw.Group.instance
    )

    val adjustedConstraints = catalogQueries(ctx, mt).map { constraint =>
      // Adjust the magnitude limits for the conditions.
      val adjustedMagConstraints = constraint.magnitudeConstraints.map(c => c.map(m => ctx.getConditions.magAdjustOp().apply(m.toOldModel).toNewModel))
      constraint.withMagnitudeConstraints(adjustedMagConstraints)
    }

    VoTableClient.catalogs(adjustedConstraints, backend).flatMap {
      case result if result.exists(_.result.containsError) => Future.failed(CatalogException(result.map(_.result.problems).flatten))
      case result                                          => Future.successful {
        result.map { r =>
          val id = r.query.id
          id.map(x => CatalogResultWithKey(r.query, r.result, GemsCatalogSearchKey(GuideStarTypeMap(x), GuideProbeGroupMap(x))))
        }.flatten
      }
    }
  }


  // Convert from catalog results to GeMS-specific results.
  private def toGemsCatalogSearchResults(ctx: ObsContext, futureAgsCatalogResults: Future[List[CatalogResultWithKey]]): Future[List[GemsCatalogSearchResults]] = {
    val anglesToTry = (0 until 360 by 45).map(Angle.fromDegrees(_))

    futureAgsCatalogResults.map { agsCatalogResults =>
      for {
        result <- agsCatalogResults
        angle <- anglesToTry
      } yield {
        val constraint = result.query
        val radiusConstraint = constraint.radiusConstraint
        val catalogSearchCriterion = CatalogSearchCriterion("ags", constraint.magnitudeConstraints, radiusConstraint, None, angle.some)
        val gemsCatalogSearchCriterion = new GemsCatalogSearchCriterion(result.searchKey, catalogSearchCriterion)
        new GemsCatalogSearchResults(gemsCatalogSearchCriterion, result.catalogResult.targets.rows)
      }
    }
  }

  override def magnitudes(ctx: ObsContext, mt: MagnitudeTable): List[(GuideProbe, MagnitudeCalc)] = {
    val cans = Canopus.Wfs.values().map { cwfs => mt(ctx, cwfs).map(cwfs -> _) }.toList.flatten
    val odgw = GsaoiOdgw.values().map { odgw => mt(ctx, odgw).map(odgw -> _) }.toList.flatten
    cans ++ odgw
  }

  override def analyze(ctx: ObsContext, mt: MagnitudeTable): List[AgsAnalysis] = {
    import AgsAnalysis._

    def mapGroup(grp: GuideProbeGroup): List[AgsAnalysis] = {
      def hasGuideStarForProbe(a: AgsAnalysis): Boolean = a match {
        case NoGuideStarForProbe(_) => false
        case _                      => true
      }

      val probeAnalysis = grp.getMembers.asScala.toList.map{ analysis(ctx, mt, _, probeBands) }.flatten
      probeAnalysis.filter(hasGuideStarForProbe) match {
        case Nil => List(NoGuideStarForGroup(grp))
        case lst => lst
      }
    }

    mapGroup(Canopus.Wfs.Group.instance) ++ mapGroup(GsaoiOdgw.Group.instance)
  }

  override def candidates(ctx: ObsContext, mt: MagnitudeTable): Future[List[(GuideProbe, List[SiderealTarget])]] = {

    // Extract something we can understand from the GemsCatalogSearchResults.
    def simplifiedResult(results: List[GemsCatalogSearchResults]): List[(GuideProbe, List[SiderealTarget])] =
      results.flatMap { result =>
        val so = result.results  // extract the sky objects from this thing
        // For each guide probe associated with these sky objects, add a tuple
        // (guide probe, sky object list) to the results
        result.criterion.key.group.getMembers.asScala.toList.map { guideProbe =>
          (guideProbe, so)
        }
      }

    // why do we need multiple position angles?  catalog results are given in
    // a ring (limited by radius limits) around a base position ... confusion
    val posAngles   = (ctx.getPositionAngle.toNewModel :: (0 until 360 by 90).map(Angle.fromDegrees(_)).toList).toSet
    search(GemsGuideStarSearchOptions.DEFAULT_CATALOG,
      GemsGuideStarSearchOptions.DEFAULT_CATALOG,
      GemsTipTiltMode.canopus, ctx, posAngles, None).map(simplifiedResult)
  }

  override def estimate(ctx: ObsContext, mt: MagnitudeTable): Future[Estimate] = {
    // Get the query results and convert them to GeMS-specific ones.
    val results = toGemsCatalogSearchResults(ctx, catalogResult(ctx, mt))

    // Create a set of the angles to try.
    val anglesToTry = (0 until 360 by 45).map(Angle.fromDegrees(_)).toSet

    // A way to terminate the Mascot algorithm immediately in the following cases:
    // 1. A usable 2 or 3-star asterism is found; or
    // 2. If no asterisms were found.
    // This is unfortunately a hideous way to do anything, which could be avoided if we
    // rewrote the GemsCatalogResults.analyzeGoodEnough method (which is only used here)
    // to return something like a Buffer.
    val progressMeasurer = new MascotProgress {
      override def progress(s: Strehl, count: Int, total: Int, usable: Boolean): Boolean = {
        !((usable && s.stars.size >= 2) || (s.stars.size < 2))
      }
      override def setProgressTitle(s: String): Unit = {}
    }

    // Iterate over 45 degree position angles if no asterism is found at PA = 0.
    val gemsCatalogResults = results.map(result => new GemsCatalogResults().analyzeGoodEnough(ctx, anglesToTry.asJava, result.asJava, progressMeasurer).asScala)

    // Filter out the 1-star asterisms. If anything is left, we are good to go; otherwise, no.
    gemsCatalogResults.map { x =>
      x.filter(_.getGuideGroup.getTargets.size() >= 3).isEmpty? AgsStrategy.Estimate.CompleteFailure | AgsStrategy.Estimate.GuaranteedSuccess
    }
  }

  protected [impl] def search(opticalCatalog: String, nirCatalog: String, tipTiltMode: GemsTipTiltMode, ctx: ObsContext, posAngles: Set[Angle], nirBand: Option[MagnitudeBand]): Future[List[GemsCatalogSearchResults]] = {
    // Get the instrument: F2 or GSAOI?
    val gemsInstrument =
      (ctx.getInstrument.getType == SPComponentType.INSTRUMENT_GSAOI) ? GemsInstrument.gsaoi | GemsInstrument.flamingos2
    // Search options
    val gemsOptions = new GemsGuideStarSearchOptions(opticalCatalog, nirCatalog, gemsInstrument, tipTiltMode, posAngles.asJava)

    // Perform the catalog search, using GemsStrategy's backend
    val results = GemsVoTableCatalog(backend).search(ctx, ctx.getBaseCoordinates.toNewModel, gemsOptions, nirBand, null)

    // Now check that the results are valid: there must be a valid tip-tilt and flexure star each.
    results.map { r =>
      val AllKeys:List[GemsGuideProbeGroup] = List(Canopus.Wfs.Group.instance, GsaoiOdgw.Group.instance)
      val containedKeys = r.map(_.criterion.key.group)
      // Return a list only if both guide probes returned a value
      ~(containedKeys.forall(AllKeys.contains) option r)
    }
  }

  private def findGuideStars(ctx: ObsContext, posAngles: Set[Angle], results: List[GemsCatalogSearchResults]): Option[GemsGuideStars] = {
    // Passing in null to say we don't want a ProgressMeter.
    val gemsResults = new GemsCatalogResults().analyze(ctx, posAngles.asJava, results.asJava, null).asScala
    gemsResults.headOption
  }

  override def select(ctx: ObsContext, mt: MagnitudeTable): Future[Option[Selection]] = {
    val posAngles = (ctx.getPositionAngle.toNewModel :: (0 until 360 by 90).map(Angle.fromDegrees(_)).toList).toSet
    val results = search(GemsGuideStarSearchOptions.DEFAULT_CATALOG,
      GemsGuideStarSearchOptions.DEFAULT_CATALOG,
      GemsTipTiltMode.canopus, ctx, posAngles, None)
    results.map { r =>
      val gemsGuideStars = findGuideStars(ctx, posAngles, r)

      // Now we must convert from an Option[GemsGuideStars] to a Selection.
      gemsGuideStars.map { x =>
        val assignments = x.getGuideGroup.getAll.asScalaList.map(targets => {
          val guider = targets.getGuider
          targets.getTargets.asScalaList.map(target => Assignment(guider, target.toNewModel))
        }).flatten
        Selection(x.getPa, assignments)
      }
    }
  }

  def catalogQueries(ctx: ObsContext, mt: MagnitudeTable): List[CatalogQuery] = {
    import AgsMagnitude._
    val cond = ctx.getConditions
    val mags = magnitudes(ctx, mt).toMap
    def lim(gp: GuideProbe): Option[MagnitudeConstraints] = {
        val r = autoSearchLimitsCalc(mags(gp), cond)
        // FIXME, this should use MagnitudeRange
        Some(MagnitudeConstraints(MagnitudeBand.R, r.faintnessConstraint, r.saturationConstraint))
      }

    val odgwMagLimits = (lim(GsaoiOdgw.odgw1) /: GsaoiOdgw.values().drop(1)) { (ml, odgw) =>
      (ml |@| lim(odgw))(_ union _).flatten
    }
    val canMagLimits = (lim(Canopus.Wfs.cwfs1) /: Canopus.Wfs.values().drop(1)) { (ml, can) =>
      (ml |@| lim(can))(_ union _).flatten
    }

    val canopusConstraint = canMagLimits.map(c => CatalogQuery.catalogQueryForGems(CanopusTipTiltId, ctx.getBaseCoordinates.toNewModel, RadiusConstraint.between(Angle.zero, Canopus.Wfs.Group.instance.getRadiusLimits.toNewModel), c.some))
    val odgwConstaint     = odgwMagLimits.map(c => CatalogQuery.catalogQueryForGems(OdgwFlexureId,    ctx.getBaseCoordinates.toNewModel, RadiusConstraint.between(Angle.zero, GsaoiOdgw.Group.instance.getRadiusLimits.toNewModel), c.some))
    List(canopusConstraint, odgwConstaint).flatten
  }

  override val probeBands: List[MagnitudeBand] = List(MagnitudeBand.R)

  override val guideProbes: List[GuideProbe] =
    Flamingos2OiwfsGuideProbe.instance :: (GsaoiOdgw.values() ++ Canopus.Wfs.values()).toList
}

object GemsStrategy extends GemsStrategy {
  override private [impl] val backend = RemoteBackend
}