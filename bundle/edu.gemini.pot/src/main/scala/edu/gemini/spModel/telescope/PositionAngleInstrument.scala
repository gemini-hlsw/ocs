package edu.gemini.spModel.telescope

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.obscomp.SPInstObsComp

import scalaz._
import Scalaz._


// Simplified interface for Java for accessing the position angle.
abstract sealed class PositionAngleInstrument[M <: PositionAngleMode](spType: SPComponentType, default: M) extends SPInstObsComp(spType) {
  private var mode: M = default

  def positionAngleMode: M = mode
  def positionAngle: Angle = positionAngleMode.positionAngle
  def setPositionAngleMode(m: M): Unit = {
    mode = m
  }

  override def restoreScienceDetails(oldInst: SPInstObsComp): Unit = {
    super.restoreScienceDetails(oldInst)
    oldInst match {
      case oldM: PositionAngleInstrument[_ <: M] => setPositionAngleMode(oldM.positionAngleMode)
      case _                                     =>
    }
  }
}

abstract class FixedPositionAngleInstrument(spType: SPComponentType) extends PositionAngleInstrument[FixedMode](spType, FixedMode.default)
abstract class FixedFlipPositionAngleInstrument(spType: SPComponentType) extends PositionAngleInstrument[FixedFlipMode](spType, FixedFlipMode.default)
abstract class ParallacticAngleInstrument(spType: SPComponentType) extends PositionAngleInstrument[ParallacticMode](spType, ParallacticMode.default) {
  // Determine if the current instrument configuration is compoatible with parallactic angle mode.
  def isCompatibleWithMeanParallacticAngleMode: Boolean = true
}