package edu.gemini.ags.gems

import edu.gemini.catalog.api.MagnitudeConstraints
import edu.gemini.ags.impl._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.shared.util.immutable.ScalaConverters._

/**
 * Utility methods for Java classes to access scala classes/methods
 */
object GemsUtils4Java {
  // Returns true if the target magnitude is within the given limits
  def containsMagnitudeInLimits(target: SPTarget, magLimits: MagnitudeConstraints): Boolean =
    // TODO This method has too many conversions, should be simplified
    target.getMagnitude(magLimits.band.toOldModel).asScalaOpt.map(m => magLimits.contains(m.toNewModel)).getOrElse(true)

}