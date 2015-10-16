package edu.gemini.ags

import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core.{Magnitude, Coordinates}

trait TargetsHelper {
  def target(name: String, c: Coordinates, mags: List[Magnitude]):SiderealTarget = SiderealTarget.empty.copy(name = name, coordinates = c, magnitudes = mags)
}
