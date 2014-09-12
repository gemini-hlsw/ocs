package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.shared.skyobject.SkyObject
import edu.gemini.skycalc.CoordinateDiff
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.ags.api.AgsMagnitude
import edu.gemini.catalog.api.MagnitudeLimits


/**
 * Math on a list of candidates with a given set of constraints.  The idea is
 * that one set of candidates and constraints can be applied to differing
 * observation contexts (different position angles, guide speeds, etc.)
 */
class CandidateValidator(params: SingleProbeStrategyParams, mt: MagnitudeTable, candidates: List[SkyObject]) {
  /**
   * Produces a predicate for testing whether a candidate is valid in an
   * established context.
   */
  def isValid(ctx: ObsContext): (SkyObject) => Boolean = {
    val magLimits = params.magnitudeCalc(ctx, mt).map(AgsMagnitude.autoSearchLimitsCalc(_, ctx.getConditions)).getOrElse(MagnitudeLimits.empty(params.band))

    (so: SkyObject) => {
      // Do not use any candidates that are too close to science target / base
      // position (i.e. don't use science target as guide star)
      def farEnough =
        params.minDistance.forall { min =>
          val soCoords = so.getHmsDegCoordinates.asSkycalc
          val diff = new CoordinateDiff(ctx.getBaseCoordinates, soCoords)
          diff.getDistance.compareToAngle(min) >= 0
        }

      // Only keep candidates that fall within the magnitude limits.
      def brightnessOk = so.getMagnitude(params.band).asScalaOpt.exists(magLimits.contains)

      // Only keep those that are in range of the guide probe.
      def inProbeRange = params.validator(ctx).validate(new SPTarget(so), ctx)

      farEnough && brightnessOk && inProbeRange
    }
  }

  def filter(ctx: ObsContext): List[SkyObject]   = candidates.filter(isValid(ctx))

  def exists(ctx: ObsContext): Boolean           = candidates.exists(isValid(ctx))

  def select(ctx: ObsContext): Option[SkyObject] = brightest(filter(ctx), params.band)(identity)
}
