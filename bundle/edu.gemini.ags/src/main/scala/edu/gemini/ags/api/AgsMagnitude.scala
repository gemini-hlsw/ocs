package edu.gemini.ags.api

import edu.gemini.catalog.api.MagnitudeConstraints
import edu.gemini.spModel.core.{Magnitude, MagnitudeBand}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions.{BEST, WORST}
import edu.gemini.spModel.guide.{GuideSpeed, GuideProbe}
import edu.gemini.spModel.guide.GuideSpeed.{FAST, SLOW}
import edu.gemini.spModel.obs.context.ObsContext

import edu.gemini.ags.impl._

/**
 * Types and methods for calculating magnitude limits.
 */
object AgsMagnitude {
  // This is extremely difficult (impossible?) to use from Java ...
//  type MagnitudeCalc  = (Conditions, GuideSpeed) => MagnitudeLimits
//  type MagnitudeTable = (Site, GuideProbe) => Option[MagnitudeCalc]
  trait MagnitudeCalc {
    def apply(c: Conditions, gs: GuideSpeed): MagnitudeConstraints
  }

  trait MagnitudeTable {
    def apply(ctx: ObsContext, gp: GuideProbe): Option[MagnitudeCalc]
  }


  /**
   * Gets the widest possible range limits incorporating the given conditions
   * and speeds.
   */
  def rangeLimits(mc: MagnitudeCalc, c1: (Conditions, GuideSpeed), c2: (Conditions, GuideSpeed)): MagnitudeConstraints =
    mc(c1._1, c1._2).union(mc(c2._1, c2._2)).getOrElse(???)

  /**
   * Manual search limits provide the faintest possible limit for the best
   * conditions with the slowest guide speed and the brightest limit for the
   * worst conditions with the fastest guide speed.  These limits can be sent
   * to a catalog server to find all possible candidates under any conditions
   * or guide speed.
   */
  def manualSearchLimits(mc: MagnitudeCalc): MagnitudeConstraints =
    rangeLimits(mc, (BEST, SLOW), (WORST, FAST))

  /**
   * Automatic search limits provide the faintest possible limit for the
   * slowest guide speed and the brightest limit for fastest guide speed.  Guide
   * stars which fall within these limits can be automatically assigned to
   * guiders by the AGS system.
   */
  def autoSearchLimitsCalc(mc: MagnitudeCalc, c: Conditions): MagnitudeConstraints =
    rangeLimits(mc, (c, SLOW), (c, FAST))

  /**
   * Determines the fastest possible guide speed (if any) that may be used for
   * guiding given a star with the indicated magnitude.
   */
  def fastestGuideSpeed(mc: MagnitudeCalc, m: Magnitude, c: Conditions): Option[GuideSpeed] =
    GuideSpeed.values().find { gs => // assumes the values are sorted fast to slow
      mc(c, gs).contains(m)
    }

  def band(mc: MagnitudeCalc): MagnitudeBand = mc(BEST, FAST).band
}
