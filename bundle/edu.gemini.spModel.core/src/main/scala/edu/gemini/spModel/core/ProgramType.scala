package edu.gemini.spModel.core

import scala.collection.JavaConverters._

sealed trait ProgramType extends Ordered[ProgramType] {
  def name: String
  def abbreviation: String
  override def toString = abbreviation
  def isScience: Boolean = true
  def typeEnum: ProgramTypeEnum

  def compare(that : ProgramType): Int = abbreviation.compare(that.abbreviation)
}

object ProgramType {

  case object Calibration extends ProgramType {
    val name = "Calibration"
    val abbreviation = "CAL"
    override val isScience = false
    val typeEnum = ProgramTypeEnum.CAL
  }

  /** Access from Java code. */
  def CAL: ProgramType = Calibration

  case object Classical extends ProgramType {
    val name = "Classical"
    val abbreviation = "C"
    val typeEnum = ProgramTypeEnum.C
  }

  /** Access from Java code. */
  def C: ProgramType = Classical

  case object DemoScience extends ProgramType {
    val name = "Demo Science"
    val abbreviation = "DS"
    val typeEnum = ProgramTypeEnum.DS
  }

  /** Access from Java code. */
  def DS: ProgramType = DemoScience

  case object DirectorsTime extends ProgramType {
    val name = "Director's Time"
    val abbreviation = "DD"
    val typeEnum = ProgramTypeEnum.DD
  }

  /** Access from Java code. */
  def DD: ProgramType = DirectorsTime

  case object Engineering extends ProgramType {
    val name = "Engineering"
    val abbreviation = "ENG"
    override val isScience = false
    val typeEnum = ProgramTypeEnum.ENG
  }

  /** Access from Java code. */
  def ENG: ProgramType = Engineering

  case object FastTurnaround extends ProgramType {
    val name = "Fast Turnaround"
    val abbreviation = "FT"
    val typeEnum = ProgramTypeEnum.FT
  }

  /** Access from Java code. */
  def FT: ProgramType = FastTurnaround

  case object LargeProgram extends ProgramType {
    val name = "Large Program"
    val abbreviation = "LP"
    val typeEnum = ProgramTypeEnum.LP
  }

  /** Access from Java code. */
  def LP: ProgramType = LargeProgram

  case object Queue extends ProgramType {
    val name = "Queue"
    val abbreviation = "Q"
    val typeEnum = ProgramTypeEnum.Q
  }

  /** Access from Java code. */
  def Q: ProgramType = Queue

  case object SystemVerification extends ProgramType {
    val name = "System Verification"
    val abbreviation = "SV"
    val typeEnum = ProgramTypeEnum.SV
  }

  /** Access from Java code. */
  def SV: ProgramType = SystemVerification

  val All: List[ProgramType] = List(Calibration, Classical, DemoScience, DirectorsTime, Engineering, FastTurnaround, LargeProgram, Queue, SystemVerification)
  val AllAsJava: java.util.List[ProgramType] = All.asJava

  def read(s: String): Option[ProgramType] = All.find(_.abbreviation == s)

  // Java convenience ...
  def readOrNull(id: SPProgramID): ProgramType = ProgramId.parse(id.toString).ptype.orNull
}
