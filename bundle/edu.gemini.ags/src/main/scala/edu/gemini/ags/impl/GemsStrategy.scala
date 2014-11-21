package edu.gemini.ags.impl

import edu.gemini.ags.api.{AgsAnalysis, AgsMagnitude, AgsStrategy}
import edu.gemini.ags.api.AgsStrategy.{Assignment, Estimate, Selection}
import edu.gemini.ags.gems._
import edu.gemini.ags.gems.mascot.{Strehl, MascotProgress}
import edu.gemini.catalog.api._
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.shared.skyobject.{SkyObject, Magnitude}
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates
import edu.gemini.skycalc.{Angle, Offset}
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
import edu.gemini.spModel.core.Site


object GemsStrategy extends AgsStrategy {
  override def key = GemsKey

  // Since the constraints are run in parallel, we need a way to identify them after
  // they are done, so we create IDs for each. This is a pretty nasty way to do things, but
  // since we cannot predict in what order the results will return, we need to be able
  // to pick them out somehow.
  private val CanopusTipTiltId = 0
  private val OdgwFlexureId    = 1

  // Catalog results with search keys to avoid having to recompute search key info on the fly.
  private case class CatalogResultWithKey(catalogResult: CatalogResult, searchKey: GemsCatalogSearchKey)


  // Query the catalog for each constraint and compile a list of results with the necessary
  // information for GeMS.
  private def catalogResult(ctx: ObsContext, mt: MagnitudeTable): Future[List[CatalogResultWithKey]] = future {
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

    val adjustedConstraints = queryConstraints(ctx, mt).map { constraint =>
      // Adjust the magnitude limits for the conditions.
      val adjustedMagLimit = constraint.magnitudeLimits.mapMagnitudes(ctx.getConditions.magAdjustOp())
      constraint.copy(adjustedMagLimit)
    }

    val server = CatalogServerInstances.STANDARD
    ParallelCatalogQuery.instance.query(server, adjustedConstraints.asImList).asScala.map { result =>
      val id = result.constraint.id
      CatalogResultWithKey(result, new GemsCatalogSearchKey(GuideStarTypeMap(id), GuideProbeGroupMap(id)))
    }.toList
  }


  // Convert from catalog results to GeMS-specific results.
  private def toGemsCatalogSearchResults(ctx: ObsContext, futureAgsCatalogResults: Future[List[CatalogResultWithKey]]): Future[List[GemsCatalogSearchResults]] = {
    val anglesToTry = (0 until 360 by 45).map(x => new Angle(x, Angle.Unit.DEGREES))
    val none: Option[Offset] = None

    futureAgsCatalogResults.map { agsCatalogResults =>
      for {
        result <- agsCatalogResults
        angle <- anglesToTry
      } yield {
        val constraint = result.catalogResult.constraint
        val catalogSearchCriterion = new CatalogSearchCriterion("ags", constraint.magnitudeLimits, constraint.radiusLimits, none.asGeminiOpt, Some(angle).asGeminiOpt)
        val gemsCatalogSearchCriterion = new GemsCatalogSearchCriterion(result.searchKey, catalogSearchCriterion)
        new GemsCatalogSearchResults(gemsCatalogSearchCriterion, result.catalogResult.candidates.toList)
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

      val probeAnalysis = grp.getMembers.asScala.toList.map { analysis(ctx, mt, _) }
      probeAnalysis.filter(hasGuideStarForProbe) match {
        case Nil => List(NoGuideStarForGroup(grp))
        case lst => lst
      }
    }

    mapGroup(Canopus.Wfs.Group.instance) ++ mapGroup(GsaoiOdgw.Group.instance)
  }

  override def candidates(ctx: ObsContext, mt: MagnitudeTable): Future[List[(GuideProbe, List[SkyObject])]] = {

    // Extract something we can understand from the GemsCatalogSearchResults.
    def simplifiedResult(results: List[GemsCatalogSearchResults]): List[(GuideProbe, List[SkyObject])] =
      results.flatMap { result =>
        val so = result.getResults.asScala.toList  // extract the sky objects from this thing
        // For each guide probe associated with these sky objects, add a tuple
        // (guide probe, sky object list) to the results
        result.getCriterion.getKey.getGroup.getMembers.asScala.toList.map { guideProbe =>
          (guideProbe, so)
        }
      }

    // why do we need multiple position angles?  catalog results are given in
    // a ring (limited by radius limits) around a base position ... confusion
    val posAngles   = (ctx.getPositionAngle :: (0 until 360 by 90).map(a => new Angle(a, Angle.Unit.DEGREES)).toList).toSet
    val emptyResult = List.empty[(GuideProbe, List[SkyObject])]
    future {
      search(GemsGuideStarSearchOptions.DEFAULT_CATALOG,
        GemsGuideStarSearchOptions.DEFAULT_CATALOG,
        GemsTipTiltMode.canopus, ctx, posAngles, None)
    }.map {
      _.fold(emptyResult)(simplifiedResult)
    }
  }

  override def estimate(ctx: ObsContext, mt: MagnitudeTable): Future[Estimate] = {
    // Get the query results and convert them to GeMS-specific ones.
    val results = toGemsCatalogSearchResults(ctx, catalogResult(ctx, mt))

    // Create a set of the angles to try.
    val anglesToTry = (0 until 360 by 45).map(x => new Angle(x, Angle.Unit.DEGREES)).toSet.asJava


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
    val gemsCatalogResults = results.map(result => new GemsCatalogResults().analyzeGoodEnough(ctx, anglesToTry, result.asJava, progressMeasurer).asScala)

    // Filter out the 1-star asterisms. If anything is left, we are good to go; otherwise, no.
    gemsCatalogResults.map { x =>
      if (x.filter(_.getGuideGroup.getTargets.size() >= 3).isEmpty)
        AgsStrategy.Estimate.CompleteFailure
      else
        AgsStrategy.Estimate.GuaranteedSuccess
    }
  }


  private def search(opticalCatalog: String, nirCatalog: String, tipTiltMode: GemsTipTiltMode, ctx: ObsContext, posAngles: Set[Angle], nirBand: Option[Magnitude.Band]): Option[List[GemsCatalogSearchResults]] = {
    // Get the instrument: F2 or GSAOI?
    val gemsInstrument = {
      if (ctx.getInstrument.getType == SPComponentType.INSTRUMENT_GSAOI)
        GemsInstrument.gsaoi
      else
        GemsInstrument.flamingos2
    }
    val gemsOptions = new GemsGuideStarSearchOptions(opticalCatalog, nirCatalog, gemsInstrument, tipTiltMode, posAngles.asJava)

    // Create the base position.
    val baseCoords = ctx.getBaseCoordinates
    val basePos = new HmsDegCoordinates.Builder(baseCoords.getRa, baseCoords.getDec).build

    // Perform the catalog search.
    val results = new GemsCatalog().search(ctx, basePos, gemsOptions, nirBand.asGeminiOpt, null).asScala.toList

    // Now check that the results are valid: there must be a valid tip-tilt and flexure star each.
    val checker = results.foldRight(Map[String, Boolean]())((result, resultMap) => {
      val key = result.getCriterion.getKey.getGroup.getKey
      if (!result.getResults.isEmpty)    resultMap.updated(key, true)
      else if (!resultMap.contains(key)) resultMap.updated(key, false)
      else                               resultMap
    })
    if (checker.values.exists(!_))
      None
    else
      Some(results)
  }

  private def findGuideStars(ctx: ObsContext, posAngles: Set[Angle], results: List[GemsCatalogSearchResults]): Option[GemsGuideStars] = {
    // Passing in null to say we don't want a ProgressMeter.
    val gemsResults = new GemsCatalogResults().analyze(ctx, posAngles.asJava, results.asJava, null).asScala
    if (gemsResults.isEmpty)
      None
    else
      Some(gemsResults.head)
  }

  override def select(ctx: ObsContext, mt: MagnitudeTable): Future[Option[Selection]] = future {
    val posAngles = (ctx.getPositionAngle :: (0 until 360 by 90).map(a => new Angle(a, Angle.Unit.DEGREES)).toList).toSet
    val results = search(GemsGuideStarSearchOptions.DEFAULT_CATALOG,
      GemsGuideStarSearchOptions.DEFAULT_CATALOG,
      GemsTipTiltMode.canopus, ctx, posAngles, None)
    val gemsGuideStars = results.map(x => findGuideStars(ctx, posAngles, x)).flatten

    // Now we must convert from an Option[GemsGuideStars] to a Selection.
    gemsGuideStars.map { x =>
      val posAngle = x.getPa.toDegrees
      val assignments = x.getGuideGroup.getAll.asScalaList.map(targets => {
        val guider = targets.getGuider
        targets.getTargets.asScalaList.map(target => Assignment(guider, skyObjectFromScienceTarget(target)))
      }).flatten
      Selection(posAngle, assignments)
    }
  }

  def queryConstraints(ctx: ObsContext, mt: MagnitudeTable): List[QueryConstraint] = {
    import AgsMagnitude._
    val cond = ctx.getConditions
    val mags = magnitudes(ctx, mt).toMap
    def lim(gp: GuideProbe): MagnitudeLimits = autoSearchLimitsCalc(mags(gp), cond)

    val odgwMagLimits = (lim(GsaoiOdgw.odgw1)/:GsaoiOdgw.values().drop(1)) { (ml, odgw) =>
      ml.union(lim(odgw)).getValue
    }
    val canMagLimits = (lim(Canopus.Wfs.cwfs1)/:Canopus.Wfs.values().drop(1)) { (ml, can) =>
      ml.union(lim(can)).getValue
    }

    val canopusConstraint = new QueryConstraint(CanopusTipTiltId, ctx.getBaseCoordinates, new RadiusLimits(Canopus.Wfs.Group.instance.getRadiusLimits), canMagLimits)
    val odgwConstaint     = new QueryConstraint(OdgwFlexureId,    ctx.getBaseCoordinates, new RadiusLimits(GsaoiOdgw.Group.instance.getRadiusLimits),   odgwMagLimits)
    List(canopusConstraint, odgwConstaint)
  }

  override val guideProbes: List[GuideProbe] =
    Flamingos2OiwfsGuideProbe.instance :: (GsaoiOdgw.values() ++ Canopus.Wfs.values()).toList
}
