package edu.gemini.ags.gems

import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.ags.impl.RadiusLimitCalc
import edu.gemini.ags.gems.GemsMagnitudeTable.CanopusWfsMagnitudeLimitsCalculator
import edu.gemini.catalog.api._
import edu.gemini.catalog.votable._
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.SiderealTarget
import edu.gemini.spModel.core.{Angle, MagnitudeBand, Coordinates}
import edu.gemini.spModel.gems.GemsGuideStarType.tiptilt
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.guide.GuideSpeed
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe
import scala.concurrent.{ExecutionContext, Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.JavaConverters._
import scala.math._

import scalaz._
import Scalaz._

import jsky.util.gui.StatusLogger

import GemsVoTableCatalog._

/**
 * Implements GeMS guide star search. The catalog search will provide the inputs
 * to the analysis phase, which actually assigns guide stars to guiders.
 */
final case class GemsVoTableCatalog(
  catalog: CatalogName,
  backend: Option[VoTableBackend]
) {

  /**
   * Searches for candidate stars that may serve as either Canopus WFS guide
   * stars or else PWFS1 slow focus sensor stars. This method is synchronous
   * and can be used from Java.
   *
   * @param ctx     observation context
   * @param timeout timeout in seconds
   * @return list of candidate guide stars
   */
  def search4Java(
    ctx:     ObsContext,
    mt:      MagnitudeTable,
    timeout: Int = 10,
    ec:      ExecutionContext
  ): java.util.List[SiderealTarget] =
    Await.result(search(ctx, mt)(ec), timeout.seconds).asJava

  /**
   * Searches for candidate stars that may serve as either Canopus WFS guide
   * stars or else PWFS1 slow focus sensor stars.
   *
   * @param ctx the context of the observation
   * @return Future list of candidate guide stars
   */
  def search(
    ctx: ObsContext,
    mt:  MagnitudeTable
  )(
    ec: ExecutionContext
  ): Future[List[SiderealTarget]] =

    catalogQuery(ctx, mt, catalog).fold(Future.successful(List.empty[SiderealTarget])) { q =>
      VoTableClient.catalog(q, backend)(ec).map(_.result.targets.rows)
    }

}

object GemsVoTableCatalog {

  // TODO-NGS2: remove SingleProbeStrategyParams.Pwfs1NGS2Params

  // Magnitude adjustment for the nominal faintness limit of the PWFS1 guide
  // probe. Since it is not being used for guiding, we can tolerate stars 2.5
  // mags fainter than normal.
  val Pwfs1SlowFocusSensorAdjustment = 2.5

  /**
   * Calculates the range of magnitude constraints required for CWFS candidates.
   * @param ctx observation context
   * @return magnitude constraints for CWFS candidates in the given observing
   *         conditions
   */
  def cwfsMagnitudeConstraints(ctx: ObsContext): MagnitudeConstraints =
    CanopusWfsMagnitudeLimitsCalculator
      .adjustGemsMagnitudeConstraintForJava(tiptilt, None, ctx.getConditions)

  /**
   * Calculates the range of magnitude constraints required for PWFS1 SFS
   * candidates.
   * @param ctx observation context
   * @return magnitude constraints for PWFS1 SFS candidates in the given
   *         observing conditions
   */
  def pwfsMagnitudeConstraints(
    ctx: ObsContext,
    mt:  MagnitudeTable
  ): Option[MagnitudeConstraints] =

    mt(ctx, PwfsGuideProbe.pwfs1).map { f =>
      f(ctx.getConditions, GuideSpeed.FAST)
        .adjust(_ + Pwfs1SlowFocusSensorAdjustment, identity)
    }

  /**
   * Calculates the range of magnitude constraints required to include both
   * PWFS1 as SFS and Canopus WFS in the given observing conditions.
   *
   * @param ctx observation context
   *
   * @return broadest necessary magnitude constraints for a candidate that may
   *         serve as a PWFS1 SFS star or a Canopus WFS star
   */
  def magnitudeConstraints(
    ctx: ObsContext,
    mt:  MagnitudeTable
  ): MagnitudeConstraints = {
    val c = cwfsMagnitudeConstraints(ctx)
    pwfsMagnitudeConstraints(ctx, mt).flatMap(_.union(c)).getOrElse(c)
  }

  /**
   * Calculates the radius constraints required to include both PWFS1 and
   * Canopus WFS stars, taking into account offset positions.
   */
  def radiusConstraint(ctx: ObsContext): RadiusConstraint =

    RadiusLimitCalc
      .getAgsQueryRadiusLimits(PwfsGuideProbe.pwfs1, ctx)
      .getOrElse(RadiusConstraint.empty)

  /**
   * Returns the catalog query necessary to include both PWFS1 SFS and Canopus
   * WFS candidates in a single query.
   */
  def catalogQuery(
    ctx:     ObsContext,
    mt:      MagnitudeTable,
    catalog: CatalogName
  ): Option[CatalogQuery] =

    for {
      b0 <- ctx.getBaseCoordinates.asScalaOpt
      b1 <- b0.toCoreCoordinates.asScalaOpt
    } yield CatalogQuery(
      b1,
      radiusConstraint(ctx),
      magnitudeConstraints(ctx, mt),
      catalog
    )

}
