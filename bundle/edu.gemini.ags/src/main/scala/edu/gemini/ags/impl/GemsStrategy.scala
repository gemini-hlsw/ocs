package edu.gemini.ags.impl

import edu.gemini.ags.api.{AgsAnalysis, AgsMagnitude, AgsStrategy}
import edu.gemini.ags.api.AgsStrategy.{Assignment, Estimate, Selection}
import edu.gemini.ags.gems._
import edu.gemini.ags.gems.mascot.Strehl
import edu.gemini.catalog.api._
import edu.gemini.catalog.votable._
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.pot.ModelConverters._
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
import edu.gemini.spModel.guide.{ValidatableGuideProbe, GuideProbeGroup, GuideProbe}
import edu.gemini.spModel.core._

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

    VoTableClient.catalogs(catalogQueries(ctx, mt), backend).flatMap {
      case result if result.exists(_.result.containsError) => Future.failed(CatalogException(result.flatMap(_.result.problems)))
      case result                                          => Future.successful {
        result.flatMap { qr =>
            val id = qr.query.id
            id.map(x => CatalogResultWithKey(qr.query, qr.result, GemsCatalogSearchKey(GuideStarTypeMap(x), GuideProbeGroupMap(x))))
        }
      }
    }
  }

  // Convert from catalog results to GeMS-specific results.
  private def toGemsCatalogSearchResults(ctx: ObsContext, futureAgsCatalogResults: Future[List[CatalogResultWithKey]]): Future[List[GemsCatalogSearchResults]] = {
    val anglesToTry = (0 until 360 by 45).map(Angle.fromDegrees(_))

    futureAgsCatalogResults.map { agsCatalogResults =>
        for {
          result                                                    <- agsCatalogResults
          angle                                                     <- anglesToTry
        } yield {
          val ConeSearchCatalogQuery(_, _, radiusConstraint, mc, _) = result.query
          val catalogSearchCriterion     = CatalogSearchCriterion("ags", radiusConstraint, mc.head, None, angle.some)
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

  override def analyze(ctx: ObsContext, mt: MagnitudeTable, guideProbe: ValidatableGuideProbe, guideStar: SiderealTarget): Option[AgsAnalysis] =
    AgsAnalysis.analysis(ctx, mt, guideProbe, guideStar, probeBands(guideProbe))

  override def analyze(ctx: ObsContext, mt: MagnitudeTable): List[AgsAnalysis] = {
    import AgsAnalysis._

    def mapGroup(grp: GuideProbeGroup): List[AgsAnalysis] = {
      def hasGuideStarForProbe(a: AgsAnalysis): Boolean = a match {
        case NoGuideStarForProbe(_, _) => false
        case _                         => true
      }

      val probeAnalysis = grp.getMembers.asScala.toList.flatMap { p => analysis(ctx, mt, p, probeBands(p)) }
      probeAnalysis.filter(hasGuideStarForProbe) match {
        case Nil =>
          // Pick the first guide probe as representative, since we are called with either Canopus or GsaoiOdwg
          ~grp.getMembers.asScala.headOption.map {gp => List(NoGuideStarForGroup(grp, probeBands(gp)))}
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
    search(GemsTipTiltMode.canopus, ctx, posAngles, None).map(simplifiedResult)
  }

  override def estimate(ctx: ObsContext, mt: MagnitudeTable): Future[Estimate] = {
    // Get the query results and convert them to GeMS-specific ones.
    val results = toGemsCatalogSearchResults(ctx, catalogResult(ctx, mt))

    // Create a set of the angles to try.
    val anglesToTry = (0 until 360 by 45).map(Angle.fromDegrees(_)).toSet

    // A way to terminate the Mascot algorithm immediately in the following cases:
    // 1. A usable 2 or 3-star asterism is found; or
    // 2. If no asterisms were found.
    // Returning false will stop the search
    def progress(s: Strehl, usable: Boolean): Boolean = {
      !((usable && s.stars.size >= 2) || (s.stars.size < 2))
    }

    // Iterate over 45 degree position angles if no asterism is found at PA = 0.
    val gemsCatalogResults = results.map(result => GemsResultsAnalyzer.analyzeGoodEnough(ctx, anglesToTry, result, progress))

    // Filter out the 1-star asterisms. If anything is left, we are good to go; otherwise, no.
    gemsCatalogResults.map { x =>
      !x.exists(_.guideGroup.getTargets.size() >= 3) ? AgsStrategy.Estimate.CompleteFailure | AgsStrategy.Estimate.GuaranteedSuccess
    }
  }

  protected [impl] def search(tipTiltMode: GemsTipTiltMode, ctx: ObsContext, posAngles: Set[Angle], nirBand: Option[MagnitudeBand]): Future[List[GemsCatalogSearchResults]] =
    ctx.getBaseCoordinates.asScalaOpt.fold(Future.successful(List.empty[GemsCatalogSearchResults])) { base =>
    // Get the instrument: F2 or GSAOI?
    val gemsInstrument =
      (ctx.getInstrument.getType == SPComponentType.INSTRUMENT_GSAOI) ? GemsInstrument.gsaoi | GemsInstrument.flamingos2
    // Search options
    val gemsOptions = new GemsGuideStarSearchOptions(gemsInstrument, tipTiltMode, posAngles.asJava)

    // Perform the catalog search, using GemsStrategy's backend
    val results = GemsVoTableCatalog(backend, UCAC4).search(ctx, base.toNewModel, gemsOptions, nirBand)

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
    val gemsResults = GemsResultsAnalyzer.analyze(ctx, posAngles.asJava, results.asJava, None).asScala
    gemsResults.headOption
  }

  override def select(ctx: ObsContext, mt: MagnitudeTable): Future[Option[Selection]] = {
    val posAngles = Set(ctx.getPositionAngle.toNewModel, Angle.zero, Angle.fromDegrees(90), Angle.fromDegrees(180), Angle.fromDegrees(270))
    val results = search(GemsTipTiltMode.canopus, ctx, posAngles, None)
    results.map { r =>
      val gemsGuideStars = findGuideStars(ctx, posAngles, r)

      // Now we must convert from an Option[GemsGuideStars] to a Selection.
      gemsGuideStars.map { x =>
        val assignments = x.guideGroup.getAll.asScalaList.flatMap(targets => {
          val guider = targets.getGuider
          targets.getTargets.asScalaList.map(target => Assignment(guider, target.toNewModel))
        })
        Selection(x.pa, assignments)
      }
    }
  }

  override def catalogQueries(ctx: ObsContext, mt: MagnitudeTable): List[CatalogQuery] =
    ctx.getBaseCoordinates.asScalaOpt.fold(List.empty[CatalogQuery]) { base =>
      import AgsMagnitude._
      val cond = ctx.getConditions
      val mags = magnitudes(ctx, mt).toMap

      def lim(gp: GuideProbe): Option[MagnitudeConstraints] = autoSearchConstraints(mags(gp), cond)

      val odgwMagLimits = (lim(GsaoiOdgw.odgw1) /: GsaoiOdgw.values().drop(1)) { (ml, odgw) =>
        (ml |@| lim(odgw))(_ union _).flatten
      }
      val canMagLimits = (lim(Canopus.Wfs.cwfs1) /: Canopus.Wfs.values().drop(1)) { (ml, can) =>
        (ml |@| lim(can))(_ union _).flatten
      }

      val canopusConstraint = canMagLimits.map(c => CatalogQuery(CanopusTipTiltId, base.toNewModel, RadiusConstraint.between(Angle.zero, Canopus.Wfs.Group.instance.getRadiusLimits.toNewModel), List(ctx.getConditions.adjust(c)), UCAC4))
      val odgwConstraint    = odgwMagLimits.map(c => CatalogQuery(OdgwFlexureId,   base.toNewModel, RadiusConstraint.between(Angle.zero, GsaoiOdgw.Group.instance.getRadiusLimits.toNewModel), List(ctx.getConditions.adjust(c)), UCAC4))
      List(canopusConstraint, odgwConstraint).flatten
    }

  override val probeBands = RBandsList

  // Return the band used for each probe
  // TODO Delegate to GemsMagnitudeTable
  private def probeBands(guideProbe: GuideProbe): BandsList = if (Canopus.Wfs.Group.instance.getMembers.contains(guideProbe)) RBandsList else SingleBand(MagnitudeBand.H)

  override val guideProbes: List[GuideProbe] =
    Flamingos2OiwfsGuideProbe.instance :: (GsaoiOdgw.values() ++ Canopus.Wfs.values()).toList
}

object GemsStrategy extends GemsStrategy {
  override private [impl] val backend = ConeSearchBackend
}
