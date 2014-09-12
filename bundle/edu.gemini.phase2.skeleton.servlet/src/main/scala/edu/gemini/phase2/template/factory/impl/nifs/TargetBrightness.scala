package edu.gemini.phase2.template.factory.impl.nifs


// TARGET BRIGHTNESS = TB
// Use H mag from target information if available
//     Bright target (H <= 9) = BT
//     Moderate target (9 < H <= 12) = MT
//     Faint target (12 < H <= 20) = FT
//     Blind acquisition target (H > 20) = BAT

sealed trait TargetBrightness
case object BT extends TargetBrightness
case object MT extends TargetBrightness
case object FT extends TargetBrightness
case object BAT extends TargetBrightness

object TargetBrightness {
  def apply(H:Double):TargetBrightness =
    if (H <=  9.0) BT
    else if (H <= 12.0) MT
    else if (H <= 20.0) FT
    else               BAT
}

