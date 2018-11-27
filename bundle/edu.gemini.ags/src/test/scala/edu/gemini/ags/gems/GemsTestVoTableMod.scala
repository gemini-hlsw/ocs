package edu.gemini.ags.gems

import edu.gemini.spModel.core.{ Magnitude, RBandsList, SiderealTarget }
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions

import scalaz._
import Scalaz._

/**
 * Modification function for test VO table backend that transforms catalog
 * results so that they continue to work with REL-3550 magnitude limit changes.
 */
object GemsTestVoTableMod {

  def forCwfsMagnitudeLimitChange(
    conds: Conditions
  ): SiderealTarget => SiderealTarget = t => {

    // In general, we now require stars 1.3 brighter in R but we also ignore
    // sky background which was previously taken into account.
    val diff = 1.3 + conds.sb.getAdjustment(RBandsList)

    // Now for every R value, we artifically make the star `diff` brighter so
    // that it continues to correspond to the test cases
    val mags = t.magnitudes.map { m =>
      if (RBandsList.bandSupported(m.band))
        Magnitude.value.mod(_ - diff, m)
      else
        m
    }

    SiderealTarget.magnitudes.set(t, mags)

  }

}
