package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.spModel.core.Coordinates
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.ags.api.AgsMagnitude
import edu.gemini.catalog.api.MagnitudeRange
import edu.gemini.pot.ModelConverters._
import edu.gemini.spModel.target.system.HmsDegTarget

import scalaz._
import Scalaz._

/**
 * Math on a list of candidates with a given set of constraints.  The idea is
 * that one set of candidates and constraints can be applied to differing
 * observation contexts (different position angles, guide speeds, etc.)
 */
protected case class CandidateValidator(params: SingleProbeStrategyParams, mt: MagnitudeTable, candidates: List[SiderealTarget]) {
  /**
   * Produces a predicate for testing whether a candidate is valid in an
   * established context.
   */
  private def isValid(ctx: ObsContext): (SiderealTarget) => Boolean = {
    val magLimits:Option[MagnitudeRange] = params.magnitudeCalc(ctx, mt).map(AgsMagnitude.autoSearchLimitsCalc(_, ctx.getConditions))

    (st: SiderealTarget) => {
      // Do not use any candidates that are too close to science target / base
      // position (i.e. don't use science target as guide star)
      def farEnough =
        params.minDistance.forall { min =>
          val soCoords = st.coordinates
          val diff = Coordinates.difference(ctx.getBaseCoordinates.toNewModel, soCoords)
          diff.distance >= min
        }

      // Only keep candidates that fall within the magnitude limits.
      def brightnessOk = (magLimits |@| params.referenceMagnitude(st))(_ contains _.value) | false

      // Only keep those that are in range of the guide probe.
      def inProbeRange = params.validator(ctx).validate(new SPTarget(HmsDegTarget.fromSkyObject(st.toOldModel)), ctx)

      val fe = farEnough
      val bo = brightnessOk
      val rg = inProbeRange
      farEnough && brightnessOk && inProbeRange
    }
  }

  def filter(ctx: ObsContext): List[SiderealTarget]   = candidates.filter(isValid(ctx))

  def exists(ctx: ObsContext): Boolean                = candidates.exists(isValid(ctx))

  def select(ctx: ObsContext): Option[SiderealTarget] = params.brightest(filter(ctx))(identity)
}
