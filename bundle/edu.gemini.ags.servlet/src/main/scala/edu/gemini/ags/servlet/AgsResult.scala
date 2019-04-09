package edu.gemini.ags.servlet

import edu.gemini.spModel.core.{Angle, SiderealTarget}

final case class AgsResult(posAngle: Angle, target: SiderealTarget)
