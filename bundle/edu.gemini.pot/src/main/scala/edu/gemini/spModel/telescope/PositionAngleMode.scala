package edu.gemini.spModel.telescope

import edu.gemini.pot.sp.ISPObservation
import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.obs.ObsTargetCalculatorService

import scalaz._
import Scalaz._

// Different modes supported by instruments.
// 1. Fixed PA
// 2. Fixed PA and fixed PA + 180 flip
// 3. Fixed PA, fixed PA + 180 flip, and parallactic angle
// 4. Unbounded PA
// 5. Unbounded PA, and parallactic angle
sealed trait PositionAngleMode {
  def positionAngle: Angle
}

sealed trait FixedMode extends PositionAngleMode
object FixedMode {
  def default: FixedMode = PositionAngleMode.Fixed(Angle.zero)
}

sealed trait FixedFlipMode extends FixedMode
object FixedFlipMode {
  def default: FixedFlipMode = PositionAngleMode.FixedFlip(Angle.zero, flipped = false)
}


sealed trait ParallacticMode extends FixedFlipMode
object ParallacticMode {
  def default: ParallacticMode = PositionAngleMode.FixedFlip(Angle.zero, flipped = false)
}

sealed trait UnboundedMode extends PositionAngleMode
object UnboundedMode {
  def default: UnboundedMode = PositionAngleMode.Fixed(Angle.zero)
}

//sealed trait ParallacticUnboundedMode extends ParallacticMode with UnboundedMode
//object ParallacticUnboundedMode {
//  def default: ParallacticUnboundedMode = PosAngleMode.FixedFlip(Angle.zero, flipped = false)
//}

object PositionAngleMode {
  case class Fixed(angle: Angle) extends PositionAngleMode with FixedMode with FixedFlipMode with UnboundedMode {
    override def positionAngle = angle
  }
  case class FixedFlip(angle: Angle, flipped: Boolean) extends PositionAngleMode with FixedFlipMode with ParallacticMode with UnboundedMode {
    override def positionAngle = flipped ? angle.flip | angle
  }
  case class Parallactic(flipped: Boolean, obs: Option[ISPObservation]) extends PositionAngleMode with ParallacticMode with UnboundedMode /*with ParallacticUnboundedMode*/ {
    override def positionAngle = {
      val angle = parallacticAngle.getOrElse(Angle.zero)
      flipped ? angle.flip | angle
    }
    private def parallacticAngle: Option[Angle] = for {
      o <- obs
      t <- ObsTargetCalculatorService.targetCalculation(o)
      a <- t.weightedMeanParallacticAngle
    } yield Angle.fromDegrees(a)
  }

  case class Unbounded(angle: Angle) extends PositionAngleMode with UnboundedMode /*with ParallacticUnboundedMode*/ {
    override def positionAngle = angle
  }
}