package edu.gemini.spModel.core

import scala.collection.JavaConverters._

sealed trait ProgramType extends Ordered[ProgramType] {
  def name: String
  def abbreviation: String
  override def toString = abbreviation

  def compare(that : ProgramType): Int = abbreviation.compare(that.abbreviation)
}

object ProgramType {
  case object Calibration extends ProgramType {
    val name = "Calibration"
    val abbreviation = "CAL"
  }
  case object Classical extends ProgramType {
    val name = "Classical"
    val abbreviation = "C"
  }
  case object DemoScience extends ProgramType {
    val name = "Demo Science"
    val abbreviation = "DS"
  }
  case object DirectorsTime extends ProgramType {
    val name = "Director's Time"
    val abbreviation = "DD"
  }
  case object Engineering extends ProgramType {
    val name = "Engineering"
    val abbreviation = "ENG"
  }
  case object FastTurnaround extends ProgramType {
    val name = "Fast Turnaround"
    val abbreviation = "FT"
  }
  case object LargeProgram extends ProgramType {
    val name = "Large Program"
    val abbreviation = "LP"
  }
  case object Queue extends ProgramType {
    val name = "Queue"
    val abbreviation = "Q"
  }
  case object SystemVerification extends ProgramType {
    val name = "System Verification"
    val abbreviation = "SV"
  }

  val All: List[ProgramType] = List(Calibration, Classical, DemoScience, DirectorsTime, Engineering, FastTurnaround, LargeProgram, Queue, SystemVerification)
  val AllAsJava: java.util.List[ProgramType] = All.asJava

  def read(s: String): Option[ProgramType] = All.find(_.abbreviation == s)

  // Java convenience ...
  def readOrNull(id: SPProgramID): ProgramType = ProgramId.parse(id.toString).ptype.orNull
}
