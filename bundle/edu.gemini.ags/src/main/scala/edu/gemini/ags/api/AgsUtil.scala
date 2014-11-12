package edu.gemini.ags.api

import edu.gemini.pot.sp.ISPObservation
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import scala.concurrent.Future

object AgsUtil {
  private def lookupAndThen[A](obs: ISPObservation, default: => A)(op: (AgsStrategy, ObsContext) => Future[A]): Future[A] =
    (for {
      ctx      <- ObsContext.create(obs).asScalaOpt
      strategy <- AgsRegistrar.currentStrategy(ctx)
    } yield op(strategy, ctx)).getOrElse(Future.successful(default))

  def lookupAndEstimate(obs: ISPObservation): Future[AgsStrategy.Estimate] =
    lookupAndThen(obs, AgsStrategy.Estimate.CompleteFailure)((s,c) => s.estimate(c, DefaultMagnitudeTable))

  def lookupAndSelect(obs: ISPObservation): Future[Option[AgsStrategy.Selection]] =
    lookupAndThen(obs, Option.empty[AgsStrategy.Selection])((s,c) => s.select(c, DefaultMagnitudeTable))

  def currentStrategy(obs: ISPObservation): Option[AgsStrategy] =
    ObsContext.create(obs).asScalaOpt.flatMap(AgsRegistrar.currentStrategy)
}
