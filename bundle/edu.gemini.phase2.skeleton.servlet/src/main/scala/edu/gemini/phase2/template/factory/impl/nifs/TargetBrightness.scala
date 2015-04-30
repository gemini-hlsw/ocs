package edu.gemini.phase2.template.factory.impl.nifs


// TARGET BRIGHTNESS = TB
// Use K magnitude from target information if available:
// IF      K <= 9  then BT = True   # Bright Target
// IF  9 < K <= 13 then MT = True   # Moderate Target
// IF 13 < K <= 20 then FT = True   # Faint Target
// IF 20 < K       then BAT = True  # Blind acquisition target

sealed trait TargetBrightness
case object BT extends TargetBrightness
case object MT extends TargetBrightness
case object FT extends TargetBrightness
case object BAT extends TargetBrightness

object TargetBrightness {
  def apply(K: Double): TargetBrightness =
         if (K <=  9.0) BT
    else if (K <= 13.0) MT
    else if (K <= 20.0) FT
    else                BAT
}

